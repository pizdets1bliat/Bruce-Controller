package com.bruce.controller.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Точные цвета прошивки Bruce (из src/core/theme.h):
 *   DEFAULT_PRICOLOR = 0xA80F (RGB565)  → #A80078  (магента/фиолет)
 *   DEFAULT_SECCOLOR = 0xCB76 (RGB565)  → #C86CB0  (светлая магента)
 *   bgColor          = 0x0000           → #000000  (чёрный)
 */
object BruceColors {
    // ── Base Bruce theme ──
    val Background = Color(0xFF000000)          // чёрный фон
    val Surface = Color(0xFF0A0A0A)             // карточки чуть светлее
    val SurfaceVariant = Color(0xFF141414)      // ещё светлее
    val Border = Color(0xFF1F1F1F)

    val Primary = Color(0xFFA80078)             // bruce primary (магента)
    val PrimaryDim = Color(0xFF800058)          // приглушённый
    val Secondary = Color(0xFFC86CB0)           // bruce secondary
    val SecondaryDim = Color(0xFF6B3960)

    // Текст
    val TextPrimary = Color(0xFFE8E8E8)         // светло-серый (основной)
    val TextSecondary = Color(0xFF9A9A9A)
    val TextDim = Color(0xFF555555)
    val TextOnAccent = Color(0xFF000000)

    // Статусные акценты (для индикации доступности)
    val Success = Color(0xFF00E676)             // ярко-зелёный (online)
    val Warning = Color(0xFFFFB300)             // оранжевый (HW needed)
    val Error = Color(0xFFFF1744)               // красный (ошибка/отключено)
    val Info = Color(0xFF00B0FF)                // циан (info)

    // Дополнительные категорийные цвета (для иконок)
    val CatWifi = Color(0xFF00B0FF)
    val CatBle = Color(0xFF2979FF)
    val CatRf = Color(0xFFFF1744)
    val CatRfid = Color(0xFFFFB300)
    val CatIr = Color(0xFFFF6E40)
    val CatBadusb = Color(0xFF00E676)
    val CatFiles = Color(0xFFE8E8E8)
    val CatScripts = Color(0xFFAB47BC)
    val CatClock = Color(0xFF80DEEA)
    val CatConnect = Color(0xFF1DE9B6)
    val CatConfig = Color(0xFF9E9E9E)
    val CatGps = Color(0xFF66BB6A)
    val CatLora = Color(0xFF7C4DFF)
    val CatNrf = Color(0xFFFF8A65)
    val CatFm = Color(0xFFFFD54F)
    val CatOthers = Color(0xFFB0BEC5)

    // Алиасы для совместимости со старым кодом
    val OnBackground = TextPrimary
    val OnSurface = TextPrimary
    val Accent = Primary
    val PrimaryVariant = PrimaryDim
    val Available = Success
    val Unavailable = TextDim
    val UnavailableText = TextSecondary
    val SnowStorm1 = TextPrimary
    val SnowStorm2 = TextSecondary
    val SnowStorm3 = TextSecondary
    val Red = Error
    val Frost1 = Secondary
    val Frost2 = Primary
    val Frost3 = Secondary
    val Frost4 = Primary
}
