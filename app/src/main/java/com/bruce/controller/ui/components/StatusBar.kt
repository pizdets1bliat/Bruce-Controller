package com.bruce.controller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bruce.controller.ble.BruceBleManager
import com.bruce.controller.ble.ConnectionState
import com.bruce.controller.ui.theme.BruceColors

/**
 * Bruce-style верхняя строка статуса: имя устройства, BLE-индикатор, батарея.
 */
@Composable
fun StatusBar(
    bleManager: BruceBleManager,
    title: String,
    modifier: Modifier = Modifier
) {
    val connectionState by bleManager.connectionState.collectAsStateWithLifecycle()
    val battery by bleManager.batteryLevel.collectAsStateWithLifecycle()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BruceColors.Background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Имя/статус слева
        Text(
            text = title.uppercase(),
            color = BruceColors.Primary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // BLE-индикатор
        val (bleIcon, bleColor, bleText) = when (connectionState) {
            is ConnectionState.Connected -> Triple(Icons.Filled.BluetoothConnected, BruceColors.Success, "BLE")
            is ConnectionState.Connecting -> Triple(Icons.Filled.Bluetooth, BruceColors.Warning, "…")
            is ConnectionState.Error -> Triple(Icons.Filled.BluetoothDisabled, BruceColors.Error, "ERR")
            ConnectionState.Disconnected -> Triple(Icons.Filled.BluetoothDisabled, BruceColors.TextDim, "OFF")
        }
        Icon(bleIcon, contentDescription = null, tint = bleColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(2.dp))
        Text(
            text = bleText,
            color = bleColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )

        Spacer(Modifier.width(12.dp))

        // Батарея
        val (batIcon: androidx.compose.ui.graphics.vector.ImageVector, batColor: Color) = when {
            battery == null -> Icons.Filled.BatteryAlert to BruceColors.TextDim
            (battery ?: 0) > 70 -> Icons.Filled.BatteryFull to BruceColors.Success
            (battery ?: 0) > 30 -> Icons.Filled.Battery5Bar to BruceColors.Warning
            else -> Icons.Filled.Battery3Bar to BruceColors.Error
        }
        Icon(batIcon, contentDescription = null, tint = batColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(2.dp))
        Text(
            text = battery?.let { "$it%" } ?: "--%",
            color = batColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}
