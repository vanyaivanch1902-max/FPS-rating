package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.MainViewModel
import com.example.ui.components.FrameTimeGraph
import com.example.ui.components.GameStressCanvas
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameStressScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val liveMetrics by viewModel.liveMetrics.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.recordingDuration.collectAsStateWithLifecycle()

    var particleCount by remember { mutableFloatStateOf(1000f) }
    var isPhysicsActive by remember { mutableStateOf(true) }
    var selectedCap by remember { mutableStateOf(60) }
    var showBenchmarkResultDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        // High-load interactive Game Simulation Canvas
        GameStressCanvas(
            particleCount = particleCount.roundToInt(),
            isPhysicsHeavy = isPhysicsActive,
            modifier = Modifier.fillMaxSize()
        )

        // Top Navigation Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "STRESS BENCHMARK SANDBOX",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            text = "Interactive game engine to test live FPS variance & drops",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("stress_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xDD0F172A)
                )
            )

            // Live Telemetry Banner (Live, Avg, Min, Max, 1% Low)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE111827)),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Live FPS
                    Column {
                        Text("FPS", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("${liveMetrics.currentFps}", fontSize = 20.sp, color = Color(0xFF00FF66), fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }

                    // Average FPS
                    Column {
                        Text("AVG", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(String.format("%.1f", liveMetrics.avgFps), fontSize = 16.sp, color = Color(0xFFFF9900), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // Min FPS
                    Column {
                        Text("MIN", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("${liveMetrics.minFps}", fontSize = 16.sp, color = Color(0xFFFF003C), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // Max FPS
                    Column {
                        Text("MAX", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("${liveMetrics.maxFps}", fontSize = 16.sp, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // 1% Low FPS
                    Column {
                        Text("1% LOW", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(String.format("%.0f", liveMetrics.onePercentLowFps), fontSize = 16.sp, color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // Reset button
                    IconButton(
                        onClick = { viewModel.resetStats() },
                        modifier = Modifier.size(32.dp).testTag("stress_reset_stats_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Stats",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Bottom Controls Overlay
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xF00F172A))
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                // Live Sparkline Graph
                Text(
                    text = "FRAME TIME LATENCY (${String.format("%.1f", liveMetrics.currentFrameTimeMs)}ms)",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                FrameTimeGraph(
                    frameTimes = liveMetrics.frameTimesHistory,
                    targetFps = liveMetrics.targetFps,
                    lineColor = Color(0xFF00FF66),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Particle Stress Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PARTICLE LOAD: ${particleCount.roundToInt()}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (particleCount > 2500) "HEAVY STRESS" else "NORMAL",
                        color = if (particleCount > 2500) Color(0xFFFF003C) else Color(0xFF00FF66),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Slider(
                    value = particleCount,
                    onValueChange = { particleCount = it },
                    valueRange = 100f..4000f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00FF66),
                        activeTrackColor = Color(0xFF00FF66),
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("particle_slider")
                )

                // Action Row: Inject Stutter & Timed Benchmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Stutter injection button
                    Button(
                        onClick = { viewModel.injectArtificialStutter(140L) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("inject_stutter_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1C1C)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFF003C),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Inject 140ms Hitch",
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Benchmark Recorder button
                    Button(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopRecording()
                                showBenchmarkResultDialog = true
                            } else {
                                viewModel.startRecording("Stress Test Sandbox", limitSeconds = 30)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("stress_benchmark_record_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0xFFFF003C) else Color(0xFF00FF66)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRecording) "Stop (${30 - recordingDuration}s)" else "30s Benchmark",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Benchmark Result Dialog
        if (showBenchmarkResultDialog) {
            AlertDialog(
                onDismissRequest = { showBenchmarkResultDialog = false },
                title = {
                    Text(
                        text = "BENCHMARK COMPLETED",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                },
                text = {
                    Column {
                        Text("Session performance summary:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Average FPS:", color = Color.White)
                            Text(String.format("%.1f FPS", liveMetrics.avgFps), color = Color(0xFFFF9900), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Min FPS:", color = Color.White)
                            Text("${liveMetrics.minFps} FPS", color = Color(0xFFFF003C), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Max FPS:", color = Color.White)
                            Text("${liveMetrics.maxFps} FPS", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1% Low FPS:", color = Color.White)
                            Text(String.format("%.0f FPS", liveMetrics.onePercentLowFps), color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Stability Score:", color = Color.White)
                            Text("${liveMetrics.stabilityPercent}%", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBenchmarkResultDialog = false }) {
                        Text("Save & Close", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }
    }
}
