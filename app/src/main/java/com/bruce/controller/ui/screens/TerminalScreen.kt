package com.bruce.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bruce.controller.ble.BruceBleManager
import com.bruce.controller.cli.BruceCliHandler
import com.bruce.controller.cli.TerminalLineType
import com.bruce.controller.ui.components.StatusBar
import com.bruce.controller.ui.theme.BruceColors
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    bleManager: BruceBleManager,
    cliHandler: BruceCliHandler,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val log by cliHandler.terminalLog.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Автопрокрутка вниз при новом сообщении
    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1)
    }

    Scaffold(
        topBar = {
            Column {
                StatusBar(bleManager = bleManager, title = "Terminal")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BruceColors.Surface)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = BruceColors.Primary, modifier = Modifier.size(18.dp))
                    }
                    Text("CLI", color = BruceColors.Primary,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { cliHandler.clearTerminal() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Clear, "Clear",
                            tint = BruceColors.Error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        containerColor = BruceColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Быстрые команды
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BruceColors.SurfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickCmd("info", Icons.Filled.Info) { scope.launch { cliHandler.info() } }
                QuickCmd("help", Icons.AutoMirrored.Filled.Help) { scope.launch { cliHandler.help() } }
                QuickCmd("free", Icons.Filled.Refresh) { scope.launch { cliHandler.free() } }
                QuickCmd("reboot", Icons.Filled.PowerSettingsNew) { scope.launch { cliHandler.reboot() } }
            }

            // Лог
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(BruceColors.Background)
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                if (log.isEmpty()) {
                    item {
                        Text(
                            "// Terminal ready. Type a command below or use the D-pad.",
                            color = BruceColors.TextDim,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                items(log) { line ->
                    val color = when (line.type) {
                        TerminalLineType.INPUT -> BruceColors.Primary
                        TerminalLineType.OUTPUT -> BruceColors.TextPrimary
                        TerminalLineType.ERROR -> BruceColors.Error
                        TerminalLineType.INFO -> BruceColors.Info
                    }
                    Text(
                        line.text,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }

            // Поле ввода
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BruceColors.Surface)
                    .border(1.dp, BruceColors.Border)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "#",
                    color = BruceColors.Primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BruceColors.Background)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    textStyle = TextStyle(
                        color = BruceColors.TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(BruceColors.Primary),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            val cmd = input
                            input = ""
                            scope.launch { cliHandler.execute(cmd) }
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        "Send",
                        tint = BruceColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCmd(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(BruceColors.Surface)
            .border(1.dp, BruceColors.PrimaryDim, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = BruceColors.Primary, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = BruceColors.TextPrimary,
            fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

