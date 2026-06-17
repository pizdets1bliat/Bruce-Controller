package com.bruce.controller.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bruce.controller.ble.BruceBleManager
import com.bruce.controller.ble.ConnectionState
import com.bruce.controller.ui.theme.BruceColors

@Composable
fun ScanScreen(
    bleManager: BruceBleManager,
    onDeviceConnected: () -> Unit
) {
    val context = LocalContext.current
    val scanResults by bleManager.scanResults.collectAsState()
    val connectionState by bleManager.connectionState.collectAsState()
    var isScanning by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            if (bleManager.isBluetoothEnabled()) {
                isScanning = true
                bleManager.startScan()
            } else {
                Toast.makeText(context, context.getString(com.bruce.controller.R.string.please_enable_bluetooth), Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, context.getString(com.bruce.controller.R.string.ble_permissions_required), Toast.LENGTH_LONG).show()
        }
    }

    fun checkAndStartScan() {
        if (!bleManager.isBluetoothEnabled()) {
            Toast.makeText(context, context.getString(com.bruce.controller.R.string.please_enable_bluetooth), Toast.LENGTH_LONG).show()
            return
        }

        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            isScanning = true
            bleManager.startScan()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Connected) {
            isScanning = false
            onDeviceConnected()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BruceColors.Background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Bruce",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = BruceColors.Frost2,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Controller",
                    fontSize = 18.sp,
                    color = BruceColors.SnowStorm3,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "M5Stick S3",
                    fontSize = 13.sp,
                    color = BruceColors.Frost3,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Status
        when (val state = connectionState) {
            is ConnectionState.Disconnected -> {
                ConnectionStatusCard(
                    status = if (isScanning) context.getString(com.bruce.controller.R.string.scanning) else context.getString(com.bruce.controller.R.string.not_connected),
                    color = if (isScanning) BruceColors.Warning else BruceColors.UnavailableText
                )
            }
            is ConnectionState.Connecting -> {
                ConnectionStatusCard(status = context.getString(com.bruce.controller.R.string.connecting), color = BruceColors.Warning)
            }
            is ConnectionState.Connected -> {
                ConnectionStatusCard(status = context.getString(com.bruce.controller.R.string.connected), color = BruceColors.Success)
            }
            is ConnectionState.Error -> {
                ConnectionStatusCard(status = state.message, color = BruceColors.Error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scan button
        Button(
            onClick = {
                if (isScanning) {
                    isScanning = false
                    bleManager.stopScan()
                } else {
                    checkAndStartScan()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isScanning) BruceColors.Red else BruceColors.Frost4
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = if (isScanning) Icons.Filled.Stop else Icons.Filled.BluetoothSearching,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isScanning) context.getString(com.bruce.controller.R.string.stop_scan) else context.getString(com.bruce.controller.R.string.scan_for_devices),
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(scanResults) { result ->
                DeviceCard(
                    name = result.device.name ?: "Unknown",
                    address = result.device.address,
                    rssi = result.rssi,
                    onClick = {
                        bleManager.connect(result.device)
                    }
                )
            }
            if (scanResults.isEmpty() && !isScanning) {
                item {
                    Text(
                        text = context.getString(com.bruce.controller.R.string.no_devices_found),
                        color = BruceColors.UnavailableText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(status: String, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = BruceColors.Surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = status,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun DeviceCard(
    name: String,
    address: String,
    rssi: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BruceColors.Surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Devices,
                contentDescription = null,
                tint = BruceColors.Frost2,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = BruceColors.SnowStorm1,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
                Text(
                    text = address,
                    color = BruceColors.UnavailableText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$rssi dBm",
                    color = if (rssi > -70) BruceColors.Success else BruceColors.Warning,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = BruceColors.Frost3,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
