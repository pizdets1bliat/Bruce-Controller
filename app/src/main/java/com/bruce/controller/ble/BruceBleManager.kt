package com.bruce.controller.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Менеджер BLE-соединения с Bruce-устройством.
 *
 * Из исходников Bruce (`ble_api.cpp`, `BLESerialService.cpp`):
 *   - Имя в эфире:           "Bruc" (укорочено из "Bruce", чтобы влезло в adv-пакет)
 *   - Serial Service UUID:   4371ec0b-3d43-49f9-b731-7c72a4a7bb91
 *   - Serial Char UUID:      d555ed97-bf2a-4f46-b3eb-d1fcdd7325e9  (READ + NOTIFY + WRITE)
 *   - Battery Service UUID:  0x180F (стандартный)
 *   - Battery Char UUID:     0x2A19 (стандартный)
 *
 * Команды Bruce читаются построчно (`readStringUntil('\n')`), поэтому КАЖДАЯ
 * команда должна заканчиваться одним `\n`. `\r` ломает парсинг (получается "cmd\r").
 */
class BruceBleManager(private val context: Context) {

    companion object {
        val SERIAL_SERVICE_UUID: UUID = UUID.fromString("4371ec0b-3d43-49f9-b731-7c72a4a7bb91")
        val SERIAL_CHAR_UUID: UUID = UUID.fromString("d555ed97-bf2a-4f46-b3eb-d1fcdd7325e9")
        val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_CHAR_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Имена, которые Bruce выставляет в advertising
        val BRUCE_DEVICE_NAMES = setOf("Bruc", "Bruce", "M5Stick")

        private const val REQUESTED_MTU = 247
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private var bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var serialCharacteristic: BluetoothGattCharacteristic? = null
    private var batteryCharacteristic: BluetoothGattCharacteristic? = null

    // Текущий максимальный размер полезной нагрузки в одном BLE write.
    // MTU - 3 байта на ATT header.
    private var maxWriteChunk: Int = 20 // default until MTU exchange

    private val scanResultsMap = java.util.concurrent.ConcurrentHashMap<String, ScanResult>()
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResults: StateFlow<List<ScanResult>> = _scanResults.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    /** Поток всех NOTIFY-данных с serial-характеристики (UTF-8 строки). */
    private val _receivedData = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 256)
    val receivedData: SharedFlow<String> = _receivedData.asSharedFlow()

    // Очередь команд для последовательной отправки
    private val pendingCommands = Channel<String>(Channel.UNLIMITED)

    init {
        // Стартуем consumer-корутину сразу — она ждёт команды и пишет их по одной
        scope.launch {
            for (cmd in pendingCommands) {
                writeAllChunks(cmd)
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Сканирование
    // ──────────────────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    fun startScan() {
        scanResultsMap.clear()
        _scanResults.value = emptyList()
        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(SERIAL_SERVICE_UUID)).build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bluetoothLeScanner?.startScan(filters, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        bluetoothLeScanner?.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val name = device.name
            // Фильтруем по сервису UUID мы уже отфильтровали в ScanFilter,
            // но добавим fallback по имени, если ScanFilter не сработал
            // (некоторые Android-устройства игнорируют service UUID-фильтр).
            val matchesService = result.scanRecord?.serviceUuids?.any { it.uuid == SERIAL_SERVICE_UUID } == true
            val matchesName = name != null && BRUCE_DEVICE_NAMES.any { name.startsWith(it, ignoreCase = true) }
            if (!matchesService && !matchesName) return

            scanResultsMap[device.address] = result
            _scanResults.value = scanResultsMap.values.toList()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Подключение
    // ──────────────────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        stopScan()
        _connectionState.value = ConnectionState.Connecting
        // autoConnect=false, transport=LE
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        serialCharacteristic = null
        batteryCharacteristic = null
        maxWriteChunk = 20
        _connectionState.value = ConnectionState.Disconnected
    }

    fun isConnected(): Boolean = _connectionState.value is ConnectionState.Connected

    // ──────────────────────────────────────────────────────────────
    // Отправка команды
    // ──────────────────────────────────────────────────────────────
    /**
     * Отправить CLI-команду. К строке автоматически добавляется `\n` —
     * вызывающий код передаёт команду БЕЗ перевода строки.
     */
    suspend fun sendCommand(command: String): Boolean {
        if (!isConnected()) return false
        // Bruce CLI ожидает \n как разделитель. \r ломает парсинг.
        val payload = command.trimEnd('\n', '\r') + "\n"
        pendingCommands.send(payload)
        return true
    }

    /** Записать строку, разбивая на куски по MTU при необходимости. */
    @SuppressLint("MissingPermission")
    private suspend fun writeAllChunks(data: String) {
        val char = serialCharacteristic ?: return
        val gatt = bluetoothGatt ?: return
        val bytes = data.toByteArray(Charsets.UTF_8)

        var offset = 0
        while (offset < bytes.size) {
            val end = minOf(offset + maxWriteChunk, bytes.size)
            val chunk = bytes.copyOfRange(offset, end)
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            char.value = chunk
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
            offset = end
            // Небольшая задержка между чанками: write с подтверждением, но Android
            // не всегда дожидается ответа перед следующей записью.
            kotlinx.coroutines.delay(25)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // GATT callback
    // ──────────────────────────────────────────────────────────────
    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        // Перед discovery просим MTU побольше
                        gatt.requestMtu(REQUESTED_MTU)
                    } else {
                        _connectionState.value = ConnectionState.Error("Connect failed: status=$status")
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.Disconnected
                    serialCharacteristic = null
                    batteryCharacteristic = null
                    gatt.close()
                    bluetoothGatt = null
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            // MTU exchange завершён — теперь можно безопасно discovering services
            maxWriteChunk = (mtu - 3).coerceAtLeast(20)
            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = ConnectionState.Error("discoverServices failed: $status")
                return
            }
            val serialService = gatt.getService(SERIAL_SERVICE_UUID)
            val batteryService = gatt.getService(BATTERY_SERVICE_UUID)

            serialCharacteristic = serialService?.getCharacteristic(SERIAL_CHAR_UUID)
            batteryCharacteristic = batteryService?.getCharacteristic(BATTERY_CHAR_UUID)

            val serial = serialCharacteristic
            if (serial == null) {
                _connectionState.value =
                    ConnectionState.Error("Serial characteristic not found — это точно Bruce?")
                return
            }

            // Включаем NOTIFY на serial char (Connected будет выставлен в onDescriptorWrite)
            gatt.setCharacteristicNotification(serial, true)
            val desc = serial.getDescriptor(CCCD_UUID)
            if (desc != null) {
                @Suppress("DEPRECATION")
                desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(desc)
            } else {
                // Если CCCD-дескриптора нет — соединение всё равно функциональное (без NOTIFY)
                _connectionState.value = ConnectionState.Connected
            }

            // И на battery тоже (если есть) — это уже после установки Connected
            batteryCharacteristic?.let { bat ->
                gatt.setCharacteristicNotification(bat, true)
                @Suppress("DEPRECATION")
                gatt.readCharacteristic(bat)
            }
        }

        @Suppress("DEPRECATION")
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            // Помечаем Connected ТОЛЬКО когда CCCD на serial-характеристике успешно записан —
            // именно тогда стик начнёт реально слать NOTIFY. До этого момента ответы теряются.
            if (descriptor.uuid == CCCD_UUID &&
                descriptor.characteristic?.uuid == SERIAL_CHAR_UUID &&
                status == BluetoothGatt.GATT_SUCCESS
            ) {
                _connectionState.value = ConnectionState.Connected

                // Теперь, когда NOTIFY включён — пишем CCCD и на battery (если есть)
                batteryCharacteristic?.let { bat ->
                    val d = bat.getDescriptor(CCCD_UUID)
                    if (d != null) {
                        @Suppress("DEPRECATION")
                        d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(d)
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value ?: return
            when (characteristic.uuid) {
                SERIAL_CHAR_UUID -> {
                    val text = String(data, Charsets.UTF_8)
                    scope.launch { _receivedData.emit(text) }
                }
                BATTERY_CHAR_UUID -> {
                    if (data.isNotEmpty()) _batteryLevel.value = data[0].toInt() and 0xFF
                }
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val data = characteristic.value ?: return
            if (characteristic.uuid == BATTERY_CHAR_UUID && data.isNotEmpty()) {
                _batteryLevel.value = data[0].toInt() and 0xFF
            }
        }
    }
}

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
