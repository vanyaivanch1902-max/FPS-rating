package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GameStressScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HudCustomizerScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen(val title: String) {
    DASHBOARD("Live HUD"),
    STRESS("Sandbox"),
    HISTORY("History"),
    CUSTOMIZER("Settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotifPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotifPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // Keep NavigationBar visible on all screens except full-screen stress test for maximum gaming view
            if (currentScreen != Screen.STRESS) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_navigation_bar"),
                    containerColor = Color(0xFF0F172A),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.DASHBOARD,
                        onClick = { currentScreen = Screen.DASHBOARD },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = {
                            Text(
                                text = "Live HUD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00FF66),
                            selectedTextColor = Color(0xFF00FF66),
                            indicatorColor = Color(0xFF004D1F),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("nav_item_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.STRESS,
                        onClick = { currentScreen = Screen.STRESS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = "Sandbox"
                            )
                        },
                        label = {
                            Text(
                                text = "Sandbox",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00FF66),
                            selectedTextColor = Color(0xFF00FF66),
                            indicatorColor = Color(0xFF004D1F),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("nav_item_stress")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.HISTORY,
                        onClick = { currentScreen = Screen.HISTORY },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History"
                            )
                        },
                        label = {
                            Text(
                                text = "History",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00FF66),
                            selectedTextColor = Color(0xFF00FF66),
                            indicatorColor = Color(0xFF004D1F),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("nav_item_history")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.CUSTOMIZER,
                        onClick = { currentScreen = Screen.CUSTOMIZER },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Settings"
                            )
                        },
                        label = {
                            Text(
                                text = "Settings",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00FF66),
                            selectedTextColor = Color(0xFF00FF66),
                            indicatorColor = Color(0xFF004D1F),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("nav_item_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF090D16))
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToStress = { currentScreen = Screen.STRESS },
                    onNavigateToCustomizer = { currentScreen = Screen.CUSTOMIZER }
                )
                Screen.STRESS -> GameStressScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.DASHBOARD }
                )
                Screen.HISTORY -> HistoryScreen(
                    viewModel = viewModel
                )
                Screen.CUSTOMIZER -> HudCustomizerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.DASHBOARD }
                )
            }
        }
    }
}
