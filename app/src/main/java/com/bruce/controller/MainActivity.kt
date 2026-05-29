package com.bruce.controller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bruce.controller.ble.BruceBleManager
import com.bruce.controller.cli.BruceCliHandler
import com.bruce.controller.data.model.BruceCategory
import com.bruce.controller.ui.screens.FeatureListScreen
import com.bruce.controller.ui.screens.MainMenuScreen
import com.bruce.controller.ui.screens.ScanScreen
import com.bruce.controller.ui.screens.TerminalScreen
import com.bruce.controller.ui.theme.BruceTheme

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: BruceBleManager
    private lateinit var cliHandler: BruceCliHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bleManager = BruceBleManager(applicationContext)
        cliHandler = BruceCliHandler(bleManager)

        setContent {
            BruceTheme {
                // Запускаем сбор ответов из BLE при первой композиции
                LaunchedEffect(Unit) { cliHandler.startCollecting() }

                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "scan") {
                    composable("scan") {
                        ScanScreen(
                            bleManager = bleManager,
                            onDeviceConnected = {
                                navController.navigate("menu") {
                                    popUpTo("scan") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("menu") {
                        MainMenuScreen(
                            bleManager = bleManager,
                            cliHandler = cliHandler,
                            onTerminalClick = { navController.navigate("terminal") },
                            onCategoryClick = { cat ->
                                navController.navigate("features/${cat.name}")
                            },
                            onDisconnect = {
                                bleManager.disconnect()
                                navController.navigate("scan") {
                                    popUpTo(0)
                                }
                            }
                        )
                    }
                    composable("features/{category}") { entry ->
                        val catName = entry.arguments?.getString("category") ?: return@composable
                        val cat = runCatching { BruceCategory.valueOf(catName) }.getOrNull() ?: return@composable
                        FeatureListScreen(
                            category = cat,
                            cliHandler = cliHandler,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("terminal") {
                        TerminalScreen(
                            bleManager = bleManager,
                            cliHandler = cliHandler,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cliHandler.stopCollecting()
        bleManager.disconnect()
    }
}
