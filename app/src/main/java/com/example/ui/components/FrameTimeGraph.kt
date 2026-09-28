package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * High-performance Compose Canvas sparkline graph for frame times (ms).
 */
@Composable
fun FrameTimeGraph(
    frameTimes: List<Float>,
    targetFps: Int,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val expectedMs = 1000f / max(30, targetFps)
    val maxScaleMs = max(33.3f, expectedMs * 2.2f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF070B12))
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            val w = size.width
            val h = size.height

            if (w <= 0 || h <= 0) return@Canvas

            // Draw target guideline (e.g. 16.6ms for 60fps)
            val targetY = (h - ((expectedMs / maxScaleMs) * h)).coerceIn(0f, h)
            drawLine(
                color = Color.White.copy(alpha = 0.25f),
                start = Offset(0f, targetY),
                end = Offset(w, targetY),
                strokeWidth = 1.5f
            )

            // Draw 33.3ms danger line (30fps threshold)
            val dangerY = (h - ((33.3f / maxScaleMs) * h)).coerceIn(0f, h)
            drawLine(
                color = Color.Red.copy(alpha = 0.2f),
                start = Offset(0f, dangerY),
                end = Offset(w, dangerY),
                strokeWidth = 1f
            )

            if (frameTimes.size >= 2) {
                val path = Path()
                val stepX = w / (frameTimes.size - 1)

                for (i in frameTimes.indices) {
                    val ms = frameTimes[i].coerceIn(0f, maxScaleMs)
                    val x = i * stepX
                    val y = (h - ((ms / maxScaleMs) * h)).coerceIn(2f, h - 2f)

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 2.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
