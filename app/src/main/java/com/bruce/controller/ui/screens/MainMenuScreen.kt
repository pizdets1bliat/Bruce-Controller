package com.bruce.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bruce.controller.ble.BruceBleManager
import com.bruce.controller.cli.BruceCliHandler
import com.bruce.controller.data.model.BruceCategory
import com.bruce.controller.data.model.BruceFeature
import com.bruce.controller.data.model.BruceFeatures
import com.bruce.controller.ui.components.StatusBar
import com.bruce.controller.ui.theme.BruceColors

/**
 * Главный экран — сетка категорий в стиле Bruce.
 * Тап по карточке = переход в живое меню стика (категория откроется на стике).
 */
@Composable
fun MainMenuScreen(
    bleManager: BruceBleManager,
    cliHandler: BruceCliHandler,
    onTerminalClick: () -> Unit,
    onCategoryClick: (BruceCategory) -> Unit,
    onDisconnect: () -> Unit
) {
    val features = remember { BruceFeatures.getAllFeatures() }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BruceColors.Background)
    ) {
        StatusBar(bleManager = bleManager, title = "BRUCE")

        // ── Подзаголовок ──
        val context = androidx.compose.ui.platform.LocalContext.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BruceColors.Surface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BruceColors.Primary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                context.getString(com.bruce.controller.R.string.main_menu),
                color = BruceColors.Primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onTerminalClick, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Terminal, "Терминал", tint = BruceColors.Primary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDisconnect, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.LinkOff, "Отключить", tint = BruceColors.Error, modifier = Modifier.size(18.dp))
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(BruceColors.Border))

        // ── Сетка категорий ──
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(features) { feature ->
                CategoryTile(feature = feature) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCategoryClick(feature.category)
                }
            }
        }

        // ── Подсказка снизу ──
        Box(
            Modifier
                .fillMaxWidth()
                .background(BruceColors.Surface)
                .padding(vertical = 6.dp, horizontal = 12.dp)
        ) {
            Text(
                context.getString(com.bruce.controller.R.string.tap_a_tile),
                color = BruceColors.TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun CategoryTile(feature: BruceFeature, onClick: () -> Unit) {
    val tint = categoryColor(feature.category)
    val icon = categoryIcon(feature.category)
    val needsHw = feature.requiredHardware.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(BruceColors.Surface)
                .border(
                    width = 1.dp,
                    color = tint.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onClick() }
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.18f))
                    .border(1.dp, tint, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = feature.name,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = feature.name.uppercase(),
                color = BruceColors.TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Бейдж "needs HW" в углу — информационный, не блокирует клик
        if (needsHw) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BruceColors.Warning.copy(alpha = 0.2f))
                    .border(0.5.dp, BruceColors.Warning, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "HW",
                    color = BruceColors.Warning,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

internal fun categoryIcon(category: BruceCategory): ImageVector = when (category) {
    BruceCategory.WIFI -> Icons.Filled.Wifi
    BruceCategory.BLE -> Icons.Filled.Bluetooth
    BruceCategory.RF -> Icons.Filled.SettingsInputAntenna
    BruceCategory.RFID -> Icons.Filled.Nfc
    BruceCategory.IR -> Icons.Filled.SettingsRemote
    BruceCategory.BADUSB -> Icons.Filled.Keyboard
    BruceCategory.FILES -> Icons.Filled.Folder
    BruceCategory.SCRIPTS -> Icons.Filled.Code
    BruceCategory.CLOCK -> Icons.Filled.Schedule
    BruceCategory.CONNECT -> Icons.Filled.Link
    BruceCategory.CONFIG -> Icons.Filled.Settings
    BruceCategory.GPS -> Icons.Filled.GpsFixed
    BruceCategory.LORA -> Icons.Filled.SatelliteAlt
    BruceCategory.NRF24 -> Icons.Filled.Sensors
    BruceCategory.FM -> Icons.Filled.Radio
    BruceCategory.ETHERNET -> Icons.Filled.Lan
    BruceCategory.OTHERS -> Icons.Filled.MoreHoriz
}

internal fun categoryColor(category: BruceCategory): Color = Color(category.bruceColor.toInt())
