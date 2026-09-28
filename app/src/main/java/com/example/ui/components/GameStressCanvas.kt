package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.isActive
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-load game particle vortex & physics simulation.
 * Stresses the render thread and Choreographer so gamers can see real-time FPS variance,
 * min FPS drops, stutters, and 1% lows.
 */
@Composable
fun GameStressCanvas(
    particleCount: Int,
    isPhysicsHeavy: Boolean,
    modifier: Modifier = Modifier
) {
    // Generate particle state arrays
    val maxParticles = 5000
    val positions = remember { FloatArray(maxParticles * 2) }
    val velocities = remember { FloatArray(maxParticles * 2) }
    val colors = remember { IntArray(maxParticles) }

    val random = remember { Random(42) }

    // Initialize particles
    remember(particleCount) {
        val count = particleCount.coerceIn(50, maxParticles)
        for (i in 0 until count) {
            positions[i * 2] = (random.nextFloat() * 1000f)
            positions[i * 2 + 1] = (random.nextFloat() * 1000f)
            velocities[i * 2] = (random.nextFloat() - 0.5f) * 8f
            velocities[i * 2 + 1] = (random.nextFloat() - 0.5f) * 8f
            colors[i] = if (i % 3 == 0) 0xFF00FF66.toInt() else if (i % 3 == 1) 0xFF00F0FF.toInt() else 0xFFFF9900.toInt()
        }
    }

    var frameTick by remember { mutableLongStateOf(0L) }

    // Game loop running on vsync
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (isActive) {
            withFrameNanos { now ->
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now

                // Update particle positions
                val count = particleCount.coerceIn(50, maxParticles)
                val centerX = 500f
                val centerY = 500f

                for (i in 0 until count) {
                    val px = positions[i * 2]
                    val py = positions[i * 2 + 1]

                    if (isPhysicsHeavy) {
                        // Gravitational vortex math
                        val dx = centerX - px
                        val dy = centerY - py
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(10f)
                        val force = 400f / dist
                        velocities[i * 2] += (dx / dist) * force * dt
                        velocities[i * 2 + 1] += (dy / dist) * force * dt

                        // Extra CPU stress: trigonometric transform
                        val angle = sin(now * 0.000000002) * 2f
                        val rotX = px * cos(angle) - py * sin(angle)
                        if (rotX.isNaN()) positions[i * 2] = centerX
                    }

                    positions[i * 2] += velocities[i * 2]
                    positions[i * 2 + 1] += velocities[i * 2 + 1]

                    // Wrap around boundaries
                    if (positions[i * 2] < 0f) positions[i * 2] = 1000f
                    if (positions[i * 2] > 1000f) positions[i * 2] = 0f
                    if (positions[i * 2 + 1] < 0f) positions[i * 2 + 1] = 1000f
                    if (positions[i * 2 + 1] > 1000f) positions[i * 2 + 1] = 0f
                }

                frameTick++
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF06090F)),
                    radius = 800f
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0 || h <= 0) return@Canvas

            val scaleX = w / 1000f
            val scaleY = h / 1000f
            val count = particleCount.coerceIn(50, maxParticles)

            val points = ArrayList<Offset>(count)
            for (i in 0 until count) {
                val x = positions[i * 2] * scaleX
                val y = positions[i * 2 + 1] * scaleY
                points.add(Offset(x, y))
            }

            drawPoints(
                points = points,
                pointMode = PointMode.Points,
                color = Color(0xFF00F0FF),
                strokeWidth = if (particleCount > 2000) 3f else 5f,
                cap = StrokeCap.Round
            )

            // Draw center vortex beacon
            val center = Offset(w / 2f, h / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00FF66).copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = 120f
                ),
                radius = 120f,
                center = center
            )
        }
    }
}
