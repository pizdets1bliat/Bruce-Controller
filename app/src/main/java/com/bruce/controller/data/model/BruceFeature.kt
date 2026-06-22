package com.bruce.controller.data.model

/**
 * Модель функции Bruce.
 * @param id — уникальный идентификатор
 * @param name — название функции (как в меню Bruce)
 * @param category — категория (WiFi, BLE, RF, RFID, IR, BadUSB, Files, Settings, etc.)
 * @param available — доступна ли на текущем устройстве (M5Stick S3)
 * @param requiredHardware — какое оборудование нужно, если недоступна
 * @param cliCommand — команда CLI для запуска
 * @param description — описание функции
 * @param iconRes — ресурс иконки (опционально)
 */
data class BruceFeature(
    val id: String,
    val name: String,
    val category: BruceCategory,
    val available: Boolean,
    val requiredHardware: String = "",
    val cliCommand: String = "",
    val description: String = "",
    val subFeatures: List<BruceFeature> = emptyList()
)

enum class BruceCategory(
    val displayName: String,
    val icon: String,
    val bruceColor: Long
) {
    WIFI("WiFi", "wifi", 0xFF5E81AC),
    BLE("Bluetooth", "bluetooth", 0xFF81A1C1),
    RF("RF/Sub-GHz", "radio", 0xFFD08770),
    RFID("RFID/NFC", "nfc", 0xFFBF616A),
    IR("Infrared", "remove_red_eye", 0xFFEBCB8B),
    BADUSB("BadUSB", "keyboard", 0xFFB48EAD),
    FILES("Files", "folder", 0xFFA3BE8C),
    SCRIPTS("Scripts", "code", 0xFF88C0D0),
    CLOCK("Clock", "schedule", 0xFF81A1C1),
    CONNECT("Connect", "link", 0xFF8FBCBB),
    CONFIG("Config", "settings", 0xFFD8DEE9),
    GPS("GPS", "gps_fixed", 0xFFA3BE8C),
    LORA("LoRa", "satellite_alt", 0xFF5E81AC),
    NRF24("NRF24", "sensors", 0xFFD08770),
    FM("FM Radio", "radio", 0xFFEBCB8B),
    ETHERNET("Ethernet", "lan", 0xFF81A1C1),
    OTHERS("Others", "more_horiz", 0xFFD8DEE9)
}
