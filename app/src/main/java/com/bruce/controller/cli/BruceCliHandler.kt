package com.bruce.controller.cli

import com.bruce.controller.ble.BruceBleManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Обработчик CLI-команд Bruce через BLE.
 *
 * Поток данных:
 *  - Команды отправляются через BleManager (`\n` добавляется автоматически).
 *  - Ответы приходят NOTIFY-чанками, склеиваются в rawBuffer.
 *  - В rawBuffer ищется первый сбалансированный JSON-объект → MenuSnapshot.
 */
class BruceCliHandler(private val bleManager: BruceBleManager) {

    companion object {
        const val MAX_TERMINAL_LINES = 500
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var responseCollector: Job? = null

    private val _terminalLog = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalLog: StateFlow<List<TerminalLine>> = _terminalLog.asStateFlow()

    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    private val _menuSnapshot = MutableStateFlow<MenuSnapshot?>(null)
    val menuSnapshot: StateFlow<MenuSnapshot?> = _menuSnapshot.asStateFlow()

    /** Сырой буфер для парсинга JSON-ответов. */
    private val rawBuffer = StringBuilder()

    /** Счётчик принятых JSON — для requestMenuSnapshot. */
    private val _snapshotCounter = MutableStateFlow(0)
    val snapshotCounter: StateFlow<Int> = _snapshotCounter.asStateFlow()

    fun startCollecting() {
        if (responseCollector?.isActive == true) return
        responseCollector = scope.launch {
            bleManager.receivedData.collect { chunk ->
                appendOutput(chunk)
                tryExtractJson()
            }
        }
    }

    fun stopCollecting() {
        responseCollector?.cancel()
        responseCollector = null
    }

    private fun appendOutput(chunk: String) {
        _lastResponse.value = (_lastResponse.value + chunk).takeLast(8000)
        rawBuffer.append(chunk)
        if (rawBuffer.length > 32_000) {
            val tail = rawBuffer.takeLast(16_000).toString()
            rawBuffer.clear()
            rawBuffer.append(tail)
        }
        val newLines = chunk.split('\n').mapNotNull { line ->
            val trimmed = line.trimEnd('\r')
            if (trimmed.isNotEmpty()) TerminalLine(trimmed, TerminalLineType.OUTPUT) else null
        }
        if (newLines.isNotEmpty()) {
            _terminalLog.value = (_terminalLog.value + newLines).takeLast(MAX_TERMINAL_LINES)
        }
    }

    /** Ищем СБАЛАНСИРОВАННЫЕ JSON-объекты в буфере и парсим каждый. */
    private fun tryExtractJson() {
        while (true) {
            val text = rawBuffer.toString()
            val start = text.indexOf('{')
            if (start < 0) {
                // мусор без JSON — иногда можно почистить, если много накопилось
                if (rawBuffer.length > 4000) {
                    rawBuffer.clear()
                }
                return
            }
            var depth = 0
            var inString = false
            var escape = false
            var foundEnd = -1
            for (i in start until text.length) {
                val c = text[i]
                if (escape) { escape = false; continue }
                when {
                    c == '\\' && inString -> escape = true
                    c == '"' -> inString = !inString
                    !inString && c == '{' -> depth++
                    !inString && c == '}' -> {
                        depth--
                        if (depth == 0) { foundEnd = i; break }
                    }
                }
            }
            if (foundEnd < 0) return // JSON ещё не дочитан до конца — ждём
            val json = text.substring(start, foundEnd + 1)
            rawBuffer.delete(0, foundEnd + 1)
            runCatching { parseMenuJson(json) }.onSuccess { snap ->
                if (snap != null) {
                    _menuSnapshot.value = snap
                    _snapshotCounter.value = _snapshotCounter.value + 1
                }
            }
        }
    }

    private fun parseMenuJson(json: String): MenuSnapshot? {
        val obj = JSONObject(json)
        if (!obj.has("options") || !obj.has("menu_title")) return null
        val title = obj.optString("menu_title", "")
        val type = obj.optString("menu", "regular_menu")
        val active = obj.optInt("active", 0)
        val arr = obj.getJSONArray("options")
        val list = mutableListOf<MenuOption>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += MenuOption(o.optInt("n", i), o.optString("label", "?"))
        }
        return MenuSnapshot(title = title, type = type, options = list, active = active)
    }

    fun clearTerminal() {
        _terminalLog.value = emptyList()
        _lastResponse.value = ""
        rawBuffer.clear()
    }

    /** Отправить сырую CLI-команду. */
    suspend fun execute(command: String): Boolean {
        val cmd = command.trim()
        if (cmd.isEmpty()) return false
        _terminalLog.value = (_terminalLog.value + TerminalLine("> $cmd", TerminalLineType.INPUT)).takeLast(MAX_TERMINAL_LINES)
        val ok = bleManager.sendCommand(cmd)
        if (!ok) {
            _terminalLog.value = (_terminalLog.value +
                TerminalLine("[!] BLE send failed (not connected?)", TerminalLineType.ERROR)).takeLast(MAX_TERMINAL_LINES)
        }
        return ok
    }

    // ── Высокоуровневые команды Bruce ──

    suspend fun loaderOpen(menuName: String) = execute("loader open $menuName")

    /** Запросить опции текущего меню. Ответ JSON приходит асинхронно в [menuSnapshot]. */
    suspend fun refreshOptions() = execute("optionsJSON")

    /**
     * Ждём СВЕЖИЙ JSON-снимок меню. Возвращает первый снимок, появившийся ПОСЛЕ вызова,
     * либо null по таймауту.
     */
    suspend fun requestMenuSnapshot(timeoutMs: Long = 3000L): MenuSnapshot? {
        // Очищаем мусор в буфере, чтобы предыдущие чанки не дали ложно-старый JSON
        rawBuffer.clear()
        val startCounter = _snapshotCounter.value
        refreshOptions()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (_snapshotCounter.value > startCounter) return _menuSnapshot.value
            delay(50)
        }
        return null
    }

    /** Тап по пункту меню стика по индексу. */
    suspend fun optionsRun(index: Int) = execute("options $index")

    /** Эмулировать аппаратную кнопку. */
    suspend fun nav(direction: NavDirection, durationMs: Int = 1) =
        execute("nav ${direction.code} $durationMs")

    // системные
    suspend fun reboot() = execute("reboot")
    suspend fun poweroff() = execute("poweroff")
    suspend fun sleep() = execute("sleep")
    suspend fun info() = execute("info")
    suspend fun uptime() = execute("uptime")
    suspend fun free() = execute("free")
    suspend fun help() = execute("help")
    suspend fun date() = execute("date")
    suspend fun beep(freq: Int = 1000, durationMs: Int = 200) = execute("tone $freq $durationMs")
    suspend fun setBrightness(value: Int) = execute("screen brightness $value")

    companion object {
        private const val MAX_TERMINAL_LINES = 500
    }
}

enum class NavDirection(val code: String) {
    UP("up"), DOWN("down"),
    NEXT("next"), PREV("prev"),
    SELECT("select"), ESC("esc"),
    NEXT_PAGE("nextpage"), PREV_PAGE("prevpage"),
}

data class MenuOption(val n: Int, val label: String)
data class MenuSnapshot(
    val title: String,
    val type: String,
    val options: List<MenuOption>,
    val active: Int
)

data class TerminalLine(val text: String, val type: TerminalLineType)
enum class TerminalLineType { INPUT, OUTPUT, ERROR, INFO }
