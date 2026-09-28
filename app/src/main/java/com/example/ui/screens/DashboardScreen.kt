package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.FpsMetrics
import com.example.model.HudConfig
import com.example.ui.MainViewModel
import com.example.ui.components.HudPreviewCard

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToStress: () -> Unit,
    onNavigateToCustomizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val liveMetrics by viewModel.liveMetrics.collectAsStateWithLifecycle()
    val hudConfig by viewModel.hudConfig.collectAsStateWithLifecycle()
    val isOverlayActive by viewModel.isOverlayServiceRunning.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.recordingDuration.collectAsStateWithLifecycle()

    var hasPermission by remember { mutableStateOf(viewModel.hasOverlayPermission(context)) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Hero Header with Gaming Branding
        HeroHeaderCard(isOverlayActive = isOverlayActive)

        Spacer(modifier = Modifier.height(14.dp))

        // Overlay Permission Banner if not granted
        if (!hasPermission) {
            PermissionRequiredCard(
                onGrantClick = {
                    viewModel.openOverlaySettings(context)
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Master Floating Overlay Switch Card
        OverlayMasterSwitchCard(
            isOverlayActive = isOverlayActive,
            hasPermission = hasPermission,
            onToggle = {
                viewModel.toggleOverlay(context)
                hasPermission = viewModel.hasOverlayPermission(context)
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Live HUD Preview Card
        Text(
            text = "LIVE HUD OVERLAY PREVIEW",
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
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

        Spacer(modifier = Modifier.height(16.dp))

        // Real-Time Metrics Breakdown Cards (Live, Avg, Min, Max, 1% Low)
        Text(
            text = "GAMEPLAY PERFORMANCE METRICS",
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryMetricCard(
                title = "LIVE FPS",
                value = "${liveMetrics.currentFps}",
                unit = "FPS",
                subtitle = "Frame: ${String.format("%.1f", liveMetrics.currentFrameTimeMs)}ms",
                color = Color(0xFF00FF66),
                modifier = Modifier.weight(1f)
            )

            TelemetryMetricCard(
                title = "AVERAGE FPS",
                value = String.format("%.1f", liveMetrics.avgFps),
                unit = "AVG",
                subtitle = "${liveMetrics.totalFramesTracked} frames tracked",
                color = Color(0xFFFF9900),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryMetricCard(
                title = "MIN / MAX FPS",
                value = "${liveMetrics.minFps} / ${liveMetrics.maxFps}",
                unit = "SPREAD",
                subtitle = "Target: ${liveMetrics.targetFps} FPS",
                color = Color(0xFF00F0FF),
                modifier = Modifier.weight(1f)
            )

            TelemetryMetricCard(
                title = "1% LOW FPS",
                value = String.format("%.0f", liveMetrics.onePercentLowFps),
                unit = "LOW",
                subtitle = "${liveMetrics.stutterCount} micro-stutters",
                color = if (liveMetrics.stutterCount > 5) Color(0xFFFF003C) else Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Session Benchmark Recorder
        BenchmarkRecorderCard(
            isRecording = isRecording,
            durationSeconds = recordingDuration,
            onStart = { viewModel.startRecording("Live Game Session") },
            onStop = { viewModel.stopRecording() },
            onReset = { viewModel.resetStats() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Navigation Action Buttons (Stress Simulator & Customizer)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onNavigateToStress,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("launch_stress_test_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = Color(0xFF00FF66),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stress Sandbox", color = Color.White, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onNavigateToCustomizer,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("customize_hud_button"),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color(0xFF00FF66), Color(0xFF00F0FF))))
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = Color(0xFF00F0FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("HUD Settings", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun HeroHeaderCard(isOverlayActive: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_fps_hero),
                contentDescription = "Gaming HUD Banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xEE0F172A))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .align(Alignment.BottomStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PC FPS OVERLAY",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Live telemetry • Min/Avg/Max • 1% Low",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                // Active badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isOverlayActive) Color(0xFF004D1F) else Color(0xFF1E293B))
                        .border(
                            1.dp,
                            if (isOverlayActive) Color(0xFF00FF66) else Color(0xFF475569),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isOverlayActive) "LIVE ON SCREEN" else "STANDBY",
                        color = if (isOverlayActive) Color(0xFF00FF66) else Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayMasterSwitchCard(
    isOverlayActive: Boolean,
    hasPermission: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverlayActive) Color(0xFF0D2818) else Color(0xFF111827)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (isOverlayActive) listOf(Color(0xFF00FF66), Color(0xFF00F0FF))
                else listOf(Color(0xFF334155), Color(0xFF1E293B))
            ),
            width = if (isOverlayActive) 2.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOverlayActive) "Floating Overlay Active" else "Enable Game FPS Overlay",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isOverlayActive)
                        "HUD is floating above all apps. Drag to reposition, tap to expand."
                    else
                        "Display live FPS, average, min/max over any Android game.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = isOverlayActive,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("overlay_master_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00FF66),
                    checkedTrackColor = Color(0xFF004D1F),
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFF1E293B)
                )
            )
        }
    }
}

@Composable
private fun PermissionRequiredCard(onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B0E)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(Color(0xFFFF9900), Color(0xFFFF5500))),
            width = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Permission Warning",
                tint = Color(0xFFFF9900),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Overlay Permission Required",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Grant 'Display over other apps' to project the FPS meter into gameplay.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("grant_permission_button")
            ) {
                Text("Grant", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TelemetryMetricCard(
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.5f), Color.Transparent)),
            width = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = unit,
                        color = color,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                color = color,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun BenchmarkRecorderCard(
    isRecording: Boolean,
    durationSeconds: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRecording) "RECORDING BENCHMARK" else "GAME BENCHMARK SESSION",
                    color = if (isRecording) Color(0xFFFF003C) else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isRecording)
                        "Logging min/max/avg FPS: ${durationSeconds}s elapsed"
                    else
                        "Log gameplay performance & save FPS session report.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("reset_session_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Stats",
                        tint = Color(0xFF94A3B8)
                    )
                }

                Button(
                    onClick = { if (isRecording) onStop() else onStart() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFFF003C) else Color(0xFF00FF66)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("record_benchmark_button")
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRecording) "Stop" else "Record",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
