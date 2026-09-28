package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.HudConfig
import com.example.model.HudTheme
import com.example.ui.MainViewModel
import com.example.ui.components.HudPreviewCard
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HudCustomizerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val liveMetrics by viewModel.liveMetrics.collectAsStateWithLifecycle()
    val hudConfig by viewModel.hudConfig.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "HUD OVERLAY SETTINGS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("customizer_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0F172A)
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Live Preview Card
            Text(
                text = "INTERACTIVE HUD PREVIEW",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))

            HudPreviewCard(
                metrics = liveMetrics,
                config = hudConfig,
                onToggleExpand = {
                    viewModel.updateConfig(hudConfig.copy(isExpanded = !hudConfig.isExpanded))
                },
                onResetStats = {
                    viewModel.resetStats()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Color Themes Section
            Text(
                text = "PC GAMING THEME PRESETS",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HudTheme.values().forEach { theme ->
                    val isSelected = hudConfig.theme == theme
                    val themePrimary = Color(theme.primaryColorHex)
                    val themeSecondary = Color(theme.secondaryColorHex)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.updateConfig(hudConfig.copy(theme = theme)) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = if (isSelected) 2.dp else 1.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(themePrimary)
                                        .border(2.dp, themeSecondary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = theme.title,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }

                            if (isSelected) {
                                Text(
                                    text = "ACTIVE",
                                    color = Color(0xFF00FF66),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Metrics Display Toggles
            Text(
                text = "METRICS DISPLAYED IN OVERLAY",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingToggleRow(
                        title = "Show Average FPS",
                        description = "Calculates rolling session average frames per second",
                        checked = hudConfig.showAverageFps,
                        onCheckedChange = { viewModel.updateConfig(hudConfig.copy(showAverageFps = it)) },
                        testTag = "toggle_avg_fps"
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                    SettingToggleRow(
                        title = "Show Min & Max FPS",
                        description = "Displays the minimum drop and peak observed frames",
                        checked = hudConfig.showMinMaxFps,
                        onCheckedChange = { viewModel.updateConfig(hudConfig.copy(showMinMaxFps = it)) },
                        testTag = "toggle_min_max_fps"
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                    SettingToggleRow(
                        title = "Show 1% Low FPS",
                        description = "Measures micro-stutters and frame drops during intense combat",
                        checked = hudConfig.showOnePercentLow,
                        onCheckedChange = { viewModel.updateConfig(hudConfig.copy(showOnePercentLow = it)) },
                        testTag = "toggle_one_percent_low"
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                    SettingToggleRow(
                        title = "Show Frame Time (ms)",
                        description = "Displays instantaneous latency between rendered frames",
                        checked = hudConfig.showFrameTimeMs,
                        onCheckedChange = { viewModel.updateConfig(hudConfig.copy(showFrameTimeMs = it)) },
                        testTag = "toggle_frame_time"
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                    SettingToggleRow(
                        title = "Show Live Frame Graph",
                        description = "Real-time sparkline graph plotting frame delta spikes",
                        checked = hudConfig.showFrameGraph,
                        onCheckedChange = { viewModel.updateConfig(hudConfig.copy(showFrameGraph = it)) },
                        testTag = "toggle_frame_graph"
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                    SettingToggleRow(
                        title = "Show System Telemetry",
                        description = "RAM usage % and battery thermal temperature in °C",
                        checked = hudConfig.showSystemTelemetry,
                        onCheckedChange = { viewModel.updateConfig(hudConfig.copy(showSystemTelemetry = it)) },
                        testTag = "toggle_system_telemetry"
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                    SettingToggleRow(
                        title = "Snap to Screen Edges",
                        description = "Magnetically docks overlay to left or right screen border on drag release",
                        checked = hudConfig.snapToEdges,
                        onCheckedChange = { viewModel.updateConfig(hudConfig.copy(snapToEdges = it)) },
                        testTag = "toggle_snap_edges"
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Scaling & Opacity Sliders
            Text(
                text = "SIZE & TRANSPARENCY",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("HUD Scale", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${(hudConfig.scaleFactor * 100).roundToInt()}%", color = Color(0xFF00FF66), fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = hudConfig.scaleFactor,
                        onValueChange = { viewModel.updateConfig(hudConfig.copy(scaleFactor = it)) },
                        valueRange = 0.8f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00FF66),
                            activeTrackColor = Color(0xFF00FF66)
                        ),
                        modifier = Modifier.testTag("scale_slider")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("HUD Opacity", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${(hudConfig.opacity * 100).roundToInt()}%", color = Color(0xFF00F0FF), fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = hudConfig.opacity,
                        onValueChange = { viewModel.updateConfig(hudConfig.copy(opacity = it)) },
                        valueRange = 0.4f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00F0FF),
                            activeTrackColor = Color(0xFF00F0FF)
                        ),
                        modifier = Modifier.testTag("opacity_slider")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Target FPS Reference
            Text(
                text = "TARGET FPS CAP",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(30, 60, 90, 120, 144).forEach { target ->
                    val isSelected = hudConfig.targetFps == target
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateConfig(hudConfig.copy(targetFps = target)) },
                        label = { Text("${target} FPS", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF004D1F),
                            selectedLabelColor = Color(0xFF00FF66),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("target_fps_${target}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00FF66),
                checkedTrackColor = Color(0xFF004D1F),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E293B)
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
