package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FpsMetrics
import com.example.model.HudConfig

/**
 * Interactive preview of the PC Game FPS Overlay HUD.
 */
@Composable
fun HudPreviewCard(
    metrics: FpsMetrics,
    config: HudConfig,
    onToggleExpand: () -> Unit,
    onResetStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(config.theme.primaryColorHex)
    val secondaryColor = Color(config.theme.secondaryColorHex)
    val accentColor = Color(config.theme.accentColorHex)
    val baseBg = Color(config.theme.backgroundColorHex)
    val bgWithOpacity = baseBg.copy(alpha = config.opacity)

    Card(
        modifier = modifier
            .testTag("hud_preview_card")
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggleExpand() }
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgWithOpacity),
        border = BorderStroke(2.dp, primaryColor.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header Row: Live FPS + Frame Time ms
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (metrics.currentFps >= config.warningFpsThreshold) primaryColor else Color(0xFFFF003C)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${metrics.currentFps}",
                        color = primaryColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "FPS",
                        color = primaryColor.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (config.showFrameTimeMs) {
                    Text(
                        text = String.format("%.1f ms", metrics.currentFrameTimeMs),
                        color = secondaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Expanded Telemetry Sections
            AnimatedVisibility(visible = config.isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.15f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 1: Average, Min, Max FPS
                    if (config.showAverageFps || config.showMinMaxFps) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (config.showAverageFps) {
                                MetricItem(
                                    label = "AVG",
                                    value = String.format("%.1f", metrics.avgFps),
                                    valueColor = secondaryColor
                                )
                            }
                            if (config.showMinMaxFps) {
                                MetricItem(
                                    label = "MIN",
                                    value = "${metrics.minFps}",
                                    valueColor = Color(0xFFFF6B6B)
                                )
                                MetricItem(
                                    label = "MAX",
                                    value = "${metrics.maxFps}",
                                    valueColor = Color(0xFF4ADE80)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Row 2: 1% Low, Stutters, Stability
                    if (config.showOnePercentLow) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MetricItem(
                                label = "1% LOW",
                                value = String.format("%.0f", metrics.onePercentLowFps),
                                valueColor = accentColor
                            )
                            MetricItem(
                                label = "STUTTER",
                                value = "${metrics.stutterCount}",
                                valueColor = if (metrics.stutterCount > 5) Color(0xFFFF003C) else Color(0xFFCBD5E1)
                            )
                            MetricItem(
                                label = "STABILITY",
                                value = "${metrics.stabilityPercent}%",
                                valueColor = primaryColor
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Frame Time Sparkline Graph
                    if (config.showFrameGraph && metrics.frameTimesHistory.isNotEmpty()) {
                        FrameTimeGraph(
                            frameTimes = metrics.frameTimesHistory,
                            targetFps = metrics.targetFps,
                            lineColor = primaryColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // System Telemetry (RAM & Battery Temp)
                    if (config.showSystemTelemetry) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "RAM: ${metrics.ramUsedPercent}% (${metrics.ramUsedMb}MB)",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format("TEMP: %.1f°C", metrics.batteryTempCelsius),
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${metrics.refreshRate}Hz",
                                color = secondaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Quick Actions inside preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { onResetStats() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Stats",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RESET STATS",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "TAP TO COLLAPSE",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
