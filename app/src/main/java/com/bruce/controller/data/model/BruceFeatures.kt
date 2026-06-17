package com.bruce.controller.data.model

/**
 * РЕАЛЬНЫЕ функции Bruce, проверенные по исходникам прошивки.
 *
 * Подход:
 *  - Запуск категорий через CLI команду  `loader open <MenuName>`
 *    Имена меню берутся из исходников прошивки (src core menu_items, см. MenuItemInterface("<Name>"))
 *  - Прямые CLI команды (rf rx, wifi on, ir tx, reboot и т.д.)
 *  - После открытия меню — навигация через `nav up/down/select/esc` либо
 *    выбор пункта по номеру через `options run <N>` (см. NavPadScreen / OptionsListScreen)
 */
object BruceFeatures {

    val allFeatures: List<BruceFeature> = listOf(

        // ── WiFi ──
        BruceFeature(
            id = "wifi", name = "WiFi", category = BruceCategory.WIFI,
            available = true, cliCommand = "loader open WiFi",
            description = "WiFi атаки, сканирование, AP, evil portal, sniffer",
            subFeatures = listOf(
                BruceFeature("wifi_on",       "Connect WiFi",   BruceCategory.WIFI, true,  cliCommand = "wifi on"),
                BruceFeature("wifi_off",      "Disconnect",     BruceCategory.WIFI, true,  cliCommand = "wifi off"),
                BruceFeature("wifi_webui",    "Web UI",         BruceCategory.WIFI, true,  cliCommand = "webui"),
                BruceFeature("wifi_arp",      "ARP Scan",       BruceCategory.WIFI, true,  cliCommand = "arp"),
                BruceFeature("wifi_sniffer",  "Sniffer",        BruceCategory.WIFI, true,  cliCommand = "sniffer"),
                BruceFeature("wifi_listen",   "TCP Listen",     BruceCategory.WIFI, true,  cliCommand = "listen"),
                BruceFeature("wifi_menu",     "Open WiFi Menu", BruceCategory.WIFI, true,  cliCommand = "loader open WiFi"),
            )
        ),

        // ── BLE ──
        BruceFeature(
            id = "ble", name = "BLE", category = BruceCategory.BLE,
            available = true, cliCommand = "loader open BLE",
            description = "Bluetooth Low Energy: spam, jammer, scanner",
            subFeatures = listOf(
                BruceFeature("ble_menu", "Open BLE Menu", BruceCategory.BLE, true, cliCommand = "loader open BLE"),
            )
        ),

        // ── RF (Sub-GHz) ──
        BruceFeature(
            id = "rf", name = "RF", category = BruceCategory.RF,
            available = true, requiredHardware = "CC1101",
            cliCommand = "loader open RF",
            description = "Sub-GHz приём/передача (433/315/868 МГц)",
            subFeatures = listOf(
                BruceFeature("rf_rx",    "RF Receive",  BruceCategory.RF, false, requiredHardware = "CC1101", cliCommand = "rf rx"),
                BruceFeature("rf_tx",    "RF Transmit", BruceCategory.RF, false, requiredHardware = "CC1101", cliCommand = "rf tx"),
                BruceFeature("rf_scan",  "RF Scan",     BruceCategory.RF, false, requiredHardware = "CC1101", cliCommand = "rf scan"),
                BruceFeature("rf_file",  "TX from file",BruceCategory.RF, false, requiredHardware = "CC1101 + SD", cliCommand = "rf tx_from_file"),
                BruceFeature("rf_menu",  "Open RF Menu",BruceCategory.RF, false, requiredHardware = "CC1101", cliCommand = "loader open RF"),
            )
        ),

        // ── RFID / NFC ──
        BruceFeature(
            id = "rfid", name = "RFID", category = BruceCategory.RFID,
            available = false, requiredHardware = "PN532 / RC522",
            cliCommand = "loader open RFID",
            description = "RFID/NFC чтение, эмуляция, запись",
            subFeatures = listOf(
                BruceFeature("rfid_menu", "Open RFID Menu", BruceCategory.RFID, false, requiredHardware = "PN532/RC522", cliCommand = "loader open RFID"),
            )
        ),

        // ── IR ──
        BruceFeature(
            id = "ir", name = "IR", category = BruceCategory.IR,
            available = true, // у M5Stick S3 есть встроенный IR
            cliCommand = "loader open IR",
            description = "ИК пульт, TV-B-Gone, чтение IR",
            subFeatures = listOf(
                BruceFeature("ir_rx",   "IR Receive",   BruceCategory.IR, true, cliCommand = "ir rx"),
                BruceFeature("ir_tx",   "IR Transmit",  BruceCategory.IR, true, cliCommand = "ir tx"),
                BruceFeature("ir_raw",  "IR TX Raw",    BruceCategory.IR, true, cliCommand = "ir tx_raw"),
                BruceFeature("ir_file", "TX from file", BruceCategory.IR, true, cliCommand = "ir tx_from_file"),
                BruceFeature("ir_menu", "Open IR Menu", BruceCategory.IR, true, cliCommand = "loader open IR"),
            )
        ),

        // ── BadUSB ──
        BruceFeature(
            id = "badusb", name = "BadUSB", category = BruceCategory.BADUSB,
            available = true, // S3 имеет USB OTG
            cliCommand = "loader open badusb",
            description = "Эмуляция HID-клавиатуры через USB-C (DuckyScript)",
            subFeatures = listOf(
                BruceFeature("bu_open",   "Open BadUSB",    BruceCategory.BADUSB, true, cliCommand = "loader open badusb"),
                BruceFeature("bu_file",   "Run script",     BruceCategory.BADUSB, true, cliCommand = "bu run_from_file"),
            )
        ),

        // ── Files ──
        BruceFeature(
            id = "files", name = "Files", category = BruceCategory.FILES,
            available = true, cliCommand = "loader open Files",
            description = "Файловый менеджер (SD / LittleFS)",
            subFeatures = listOf(
                BruceFeature("files_ls",      "List files",   BruceCategory.FILES, true, cliCommand = "ls /"),
                BruceFeature("files_littlefs","LittleFS",     BruceCategory.FILES, true, cliCommand = "loader open LittleFS"),
                BruceFeature("files_menu",    "Open Files",   BruceCategory.FILES, true, cliCommand = "loader open Files"),
            )
        ),

        // ── Scripts (JS Interpreter) ──
        BruceFeature(
            id = "scripts", name = "Scripts", category = BruceCategory.SCRIPTS,
            available = true, cliCommand = "loader open JS Interpreter",
            description = "JavaScript интерпретатор для Bruce-скриптов",
            subFeatures = listOf(
                BruceFeature("js_menu", "Open Scripts", BruceCategory.SCRIPTS, true, cliCommand = "loader open JS Interpreter"),
            )
        ),

        // ── Clock ──
        BruceFeature(
            id = "clock", name = "Clock", category = BruceCategory.CLOCK,
            available = true, cliCommand = "loader open Clock",
            description = "Часы, будильник, таймер",
            subFeatures = listOf(
                BruceFeature("clock_show", "Show clock",  BruceCategory.CLOCK, true, cliCommand = "clock"),
                BruceFeature("clock_menu", "Open Clock",  BruceCategory.CLOCK, true, cliCommand = "loader open Clock"),
            )
        ),

        // ── Connect ──
        BruceFeature(
            id = "connect", name = "Connect", category = BruceCategory.CONNECT,
            available = true, cliCommand = "loader open Connect",
            description = "WebUI, ESP-NOW, файлообмен",
            subFeatures = listOf(
                BruceFeature("conn_menu", "Open Connect", BruceCategory.CONNECT, true, cliCommand = "loader open Connect"),
            )
        ),

        // ── Config ──
        BruceFeature(
            id = "config", name = "Config", category = BruceCategory.CONFIG,
            available = true, cliCommand = "loader open Config",
            description = "Настройки устройства",
            subFeatures = listOf(
                BruceFeature("cfg_menu",   "Open Config",   BruceCategory.CONFIG, true, cliCommand = "loader open Config"),
                BruceFeature("cfg_reboot", "Reboot",        BruceCategory.CONFIG, true, cliCommand = "reboot"),
                BruceFeature("cfg_sleep",  "Sleep",         BruceCategory.CONFIG, true, cliCommand = "sleep"),
                BruceFeature("cfg_poweroff","Poweroff",     BruceCategory.CONFIG, true, cliCommand = "poweroff"),
                BruceFeature("cfg_bright", "Brightness 50", BruceCategory.CONFIG, true, cliCommand = "screen brightness 50"),
                BruceFeature("cfg_info",   "Device info",   BruceCategory.CONFIG, true, cliCommand = "info"),
                BruceFeature("cfg_uptime", "Uptime",        BruceCategory.CONFIG, true, cliCommand = "uptime"),
                BruceFeature("cfg_free",   "Free RAM",      BruceCategory.CONFIG, true, cliCommand = "free"),
            )
        ),

        // ── GPS ──
        BruceFeature(
            id = "gps", name = "GPS", category = BruceCategory.GPS,
            available = true, requiredHardware = "GPS модуль (UART)",
            cliCommand = "loader open GPS",
            description = "GPS координаты и навигация",
            subFeatures = listOf(
                BruceFeature("gps_menu", "Open GPS Menu", BruceCategory.GPS, false, requiredHardware = "GPS модуль", cliCommand = "loader open GPS"),
            )
        ),

        // ── LoRa ──
        BruceFeature(
            id = "lora", name = "LoRa", category = BruceCategory.LORA,
            available = true, requiredHardware = "SX1276/SX1262",
            cliCommand = "loader open LoRa",
            description = "LoRa приём/передача",
            subFeatures = listOf(
                BruceFeature("lora_menu", "Open LoRa Menu", BruceCategory.LORA, false, requiredHardware = "SX1276/SX1262", cliCommand = "loader open LoRa"),
            )
        ),

        // ── NRF24 ──
        BruceFeature(
            id = "nrf24", name = "NRF24", category = BruceCategory.NRF24,
            available = true, requiredHardware = "NRF24 module",
            cliCommand = "loader open NRF24",
            description = "2.4 ГГц атаки (Mousejack, Crazyradio)",
            subFeatures = listOf(
                BruceFeature("nrf_menu", "Open NRF24 Menu", BruceCategory.NRF24, false, requiredHardware = "NRF24L01", cliCommand = "loader open NRF24"),
            )
        ),

        // ── FM Radio ──
        BruceFeature(
            id = "fm", name = "FM", category = BruceCategory.FM,
            available = true, requiredHardware = "RDA5807M",
            cliCommand = "loader open FM",
            description = "FM-радио",
            subFeatures = listOf(
                BruceFeature("fm_menu", "Open FM Menu", BruceCategory.FM, false, requiredHardware = "RDA5807M", cliCommand = "loader open FM"),
            )
        ),

        // ── Others ──
        BruceFeature(
            id = "others", name = "Others", category = BruceCategory.OTHERS,
            available = true, cliCommand = "loader open Others",
            description = "GPIO, I2C, утилиты, beep",
            subFeatures = listOf(
                BruceFeature("oth_menu",  "Open Others", BruceCategory.OTHERS, true, cliCommand = "loader open Others"),
                BruceFeature("oth_beep",  "Beep",        BruceCategory.OTHERS, true, cliCommand = "tone 1000 200"),
                BruceFeature("oth_i2c",   "I2C scan",    BruceCategory.OTHERS, true, cliCommand = "i2c"),
                BruceFeature("oth_gpio_r","GPIO read 0", BruceCategory.OTHERS, true, cliCommand = "gpio read 0"),
                BruceFeature("oth_date",  "Date",        BruceCategory.OTHERS, true, cliCommand = "date"),
            )
        ),
    )

    fun getByCategory(category: BruceCategory): List<BruceFeature> =
        allFeatures.filter { it.category == category }

    fun getFeatureById(id: String): BruceFeature? =
        allFeatures.flatMap { listOf(it) + it.subFeatures }.find { f -> f.id == id }
}
