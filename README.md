# Bruce Controller

> Android-приложение для удалённого управления **M5StickC Plus 2 / M5Stick S3** с прошивкой [**Bruce firmware**](https://bruce.computer) через BLE (Bluetooth Low Energy).

<p align="left">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white">
  <img alt="Language" src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white">
  <img alt="UI" src="https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="minSdk" src="https://img.shields.io/badge/minSdk-26-orange">
  <img alt="targetSdk" src="https://img.shields.io/badge/targetSdk-35-orange">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-green">
</p>

---

## О проекте

Bruce Controller повторяет интерфейс прошивки Bruce прямо на телефоне и позволяет
запускать функции устройства по BLE — без необходимости тыкать в крошечные кнопки
самого Stick'а. Приложение находит устройство, показывает сетку категорий
(WiFi, BLE, RF, RFID, IR, BadUSB и др.) и отправляет CLI-команды на железо.

## Возможности

- **Сканирование BLE** — поиск устройств с Bruce (имя `Bruc` или `Bruce`)
- **Интерфейс как в Bruce** — сетка из 16 категорий функций
- **Индикация доступности** — 🟢 зелёный = функция доступна, 🟡 жёлтый = требуется внешний модуль
- **Запуск функций с телефона** — отправка CLI-команд на устройство по BLE
- **CLI терминал** — ручной ввод команд с историей ответов
- **Мониторинг батареи** — уровень заряда Stick S3

## Технологии

| Слой | Технология |
|---|---|
| Язык | Kotlin 2.1.0 |
| UI | Jetpack Compose + Material 3 |
| Навигация | Navigation Compose |
| Асинхронность | Kotlin Coroutines |
| Связь | Android BLE (GATT) |
| Сборка | Gradle 8.9 (AGP 8.7.3) |

## Архитектура BLE

Bruce firmware использует:

| Параметр | Значение |
|---|---|
| Service UUID | `4371ec0b-3d43-49f9-b731-7c72a4a7bb91` |
| Characteristic UUID (Serial) | `d555ed97-bf2a-4f46-b3eb-d1fcdd7325e9` |
| Advertised name | `Bruc` (укороченное) или `Bruce` |

Команды передаются как текст `команда\r\n` через BLE Write, ответы приходят через Notify.

## Структура проекта

```
Bruce-Controller/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/bruce/controller/
│       │   ├── MainActivity.kt          # точка входа, навигация
│       │   ├── ble/
│       │   │   └── BruceBleManager.kt   # сканирование, GATT, отправка команд
│       │   ├── cli/
│       │   │   └── BruceCliHandler.kt   # формирование CLI-команд
│       │   ├── data/model/
│       │   │   ├── BruceFeature.kt      # модель функции
│       │   │   └── BruceFeatures.kt     # каталог категорий и команд
│       │   └── ui/
│       │       ├── components/StatusBar.kt
│       │       ├── screens/             # Scan, MainMenu, FeatureList, Terminal
│       │       └── theme/               # цвета и тема в стиле Bruce
│       └── res/                         # иконки, строки, цвета, темы
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
```

## Доступные функции на M5Stick S3

### Доступны без доп. модулей
- **WiFi** — сканирование, deauth, beacon spam, evil portal, сниффер
- **BLE** — сканирование, спам, Apple spam, Ninebot, HFP exploit
- **IR** — отправка/приём ИК-сигналов, TV-B-Gone, брутфорс
- **BadUSB** — эмуляция BLE-клавиатуры, запуск скриптов
- **Files** — работа с файловой системой
- **Scripts** — JavaScript интерпретатор (mquickjs)
- **Clock** — часы, будильник
- **Connect** — WiFi, MQTT
- **Config** — настройки яркости, темы, поворота экрана
- **GPIO, Crypto, System Info**

### Требуют внешние модули
- **RF/Sub-GHz** — нужен CC1101 модуль (Grove)
- **RFID/NFC** — нужен PN532 или ST25R3916 (Grove)
- **GPS** — нужен GPS модуль (Grove)
- **LoRa** — нужен LoRa модуль (Grove)
- **NRF24** — нужен NRF24L01 модуль (Grove)
- **FM Radio** — нужен FM чип

## Сборка

Требуется JDK 17 и Android SDK (через Android Studio Ladybug+).

```bash
# Клонировать репозиторий
git clone https://github.com/pizdets1bliat/Bruce-Controller.git
cd Bruce-Controller

# Собрать debug APK
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
```

Готовый APK будет в `app/build/outputs/apk/debug/`.

Либо просто откройте папку проекта в **Android Studio** и нажмите ▶ Run.

## Использование

1. Прошейте M5Stick S3 прошивкой Bruce (через [M5Burner](https://docs.m5stack.com/en/download) или [Web Flasher](https://bruce.computer))
2. Включите BLE на Stick S3 (в меню Bruce: **Connect → BLE API**)
3. Откройте приложение Bruce Controller и выдайте разрешения Bluetooth/Location
4. Нажмите **"Scan for Devices"**
5. Выберите найденное устройство `Bruc`
6. Выбирайте категории и запускайте функции

## CLI команды (примеры)

```
wifi scan              — сканировать WiFi сети
wifi deauth -t <bssid> — деаутентификация
wifi beacon_spam       — спам beacon-пакетами
ble scan               — сканировать BLE устройства
ble spam               — BLE спам
ir receive             — принимать ИК сигнал
power battery          — уровень батареи
storage list           — список файлов
util info              — информация о системе
help                   — справка по командам
```

## Разрешения

Приложение запрашивает `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` и `ACCESS_FINE_LOCATION`
(необходимы Android для BLE-сканирования).

## Дисклеймер

Проект предназначен **только** для образовательных целей и тестирования
безопасности на собственном оборудовании. Используйте ответственно и в рамках
законодательства вашей страны.

## Лицензия

[MIT](LICENSE)
