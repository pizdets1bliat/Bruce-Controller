package com.bruce.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bruce.controller.cli.BruceCliHandler
import com.bruce.controller.cli.MenuOption
import com.bruce.controller.data.model.BruceCategory
import com.bruce.controller.ui.theme.BruceColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Живое меню стика: открываем категорию через `loader open`, потом крутим `optionsJSON`
 * и показываем РЕАЛЬНЫЕ пункты меню. Тап = `options N` — стик сам перейдёт куда нужно.
 *
 * Никакого выдуманного списка sub-features.
 */
@Composable
fun FeatureListScreen(
    category: BruceCategory,
    cliHandler: BruceCliHandler,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snapshot by cliHandler.menuSnapshot.collectAsStateWithLifecycle()
    var loading by remember { mutableStateOf(true) }

    // При входе на экран: открываем категорию на стике и сразу запрашиваем опции.
    LaunchedEffect(category) {
        loading = true
        cliHandler.loaderOpen(category.displayName)
        delay(700) // дать стику время отрисовать меню
        cliHandler.refreshOptions()
        delay(900)
        // Если ответа всё ещё нет — ретраим
        if (cliHandler.menuSnapshot.value == null ||
            cliHandler.menuSnapshot.value?.title.isNullOrEmpty()
        ) {
            cliHandler.refreshOptions()
            delay(900)
        }
        loading = false
    }

    val accent = categoryColor(category)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BruceColors.Background)
    ) {
        // ── Шапка ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BruceColors.Surface)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                scope.launch {
                    cliHandler.nav(com.bruce.controller.cli.NavDirection.ESC)
                    delay(250)
                    onBack()
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = BruceColors.Primary)
            }
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .border(1.dp, accent, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIcon(category), null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = (snapshot?.title ?: category.displayName).uppercase(),
                    color = BruceColors.TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "live menu · ${snapshot?.options?.size ?: 0} items",
                    color = BruceColors.TextDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
            IconButton(onClick = {
                scope.launch { loading = true; cliHandler.refreshOptions(); delay(400); loading = false }
            }) {
                Icon(Icons.Filled.Refresh, "Обновить", tint = BruceColors.Primary)
            }
        }

        HairLine()

        // ── Контент ──
        when {
            loading && snapshot == null -> LoadingState(accent)
            snapshot == null -> EmptyState(
                msg = "Стик не ответил.\nПроверь, что BLE API включён:\nConfig → Toggle BLE API",
                accent = accent,
                onRetry = {
                    scope.launch {
                        loading = true
                        cliHandler.loaderOpen(category.displayName)
                        delay(700)
                        cliHandler.refreshOptions()
                        delay(900)
                        loading = false
                    }
                }
            )
            snapshot!!.options.isEmpty() -> EmptyState(
                msg = "В этом меню нет опций.\nПопробуй обновить или выйти.",
                accent = accent,
                onRetry = {
                    scope.launch { cliHandler.refreshOptions() }
                }
            )
            else -> {
                val snap = snapshot!!
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(snap.options) { opt ->
                        OptionRow(
                            opt = opt,
                            active = opt.n == snap.active,
                            accent = accent,
                            onClick = {
                                scope.launch {
                                    cliHandler.optionsRun(opt.n)
                                    delay(700) // стик может переключить меню
                                    cliHandler.refreshOptions()
                                }
                            }
                        )
                    }
                    item {
                        Spacer(Modifier.height(12.dp))
                        HomeButton(accent) {
                            scope.launch {
                                // вернуться к главному меню стика
                                repeat(4) {
                                    cliHandler.nav(com.bruce.controller.cli.NavDirection.ESC)
                                    delay(120)
                                }
                                cliHandler.refreshOptions()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(opt: MenuOption, active: Boolean, accent: Color, onClick: () -> Unit) {
    val bg = if (active) accent.copy(alpha = 0.18f) else BruceColors.Surface
    val border = if (active) accent else BruceColors.Border
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (active) accent else BruceColors.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${opt.n}",
                color = if (active) BruceColors.Background else BruceColors.TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            opt.label,
            modifier = Modifier.weight(1f),
            color = BruceColors.TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            Icons.Filled.ChevronRight,
            null,
            tint = if (active) accent else BruceColors.TextDim,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LoadingState(accent: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Loading menu…",
                color = accent,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "loader open → optionsJSON",
                color = BruceColors.TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun EmptyState(msg: String, accent: Color, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                msg,
                color = BruceColors.TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent)
                    .clickable { onRetry() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Refresh, null, tint = BruceColors.Background, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "RETRY",
                    color = BruceColors.Background,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HomeButton(accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BruceColors.Surface)
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Home, null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "↩ MAIN MENU",
            color = accent,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HairLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(BruceColors.Border))
}
