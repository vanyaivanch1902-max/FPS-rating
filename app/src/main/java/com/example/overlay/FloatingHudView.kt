package com.example.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import com.example.model.FpsMetrics
import com.example.model.HudConfig
import com.example.model.HudTheme
import kotlin.math.abs
import kotlin.math.max

/**
 * Custom high-performance gaming HUD view for the floating WindowManager overlay.
 * Renders PC-style live telemetry with live FPS, min/max/avg, 1% low, and frame time sparkline.
 */
@SuppressLint("ViewConstructor")
class FloatingHudView(
    context: Context,
    private val onDrag: (dx: Int, dy: Int) -> Unit,
    private val onSnapEnd: () -> Unit,
    private val onToggleExpand: () -> Unit,
    private val onResetStats: () -> Unit,
    private val onOpenApp: () -> Unit,
    private val onClose: () -> Unit
) : View(context) {

    private var metrics = FpsMetrics()
    private var config = HudConfig()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val graphLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val graphTargetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val bgRect = RectF()
    private val graphRect = RectF()
    private val graphPath = Path()

    // Interactive button rects inside the HUD
    private val resetBtnRect = RectF()
    private val appBtnRect = RectF()
    private val closeBtnRect = RectF()
    private val headerRect = RectF()

    // Touch dragging tracking
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val touchSlop = 12f

    init {
        isClickable = true
        isFocusable = false
    }

    fun updateMetrics(newMetrics: FpsMetrics) {
        this.metrics = newMetrics
        postInvalidate()
    }

    fun updateConfig(newConfig: HudConfig) {
        this.config = newConfig
        requestLayout()
        postInvalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val scale = config.scaleFactor

        val widthDp = if (config.isExpanded) 230f * scale else 115f * scale
        val heightDp = if (config.isExpanded) {
            var h = 100f
            if (config.showFrameGraph) h += 36f
            if (config.showSystemTelemetry) h += 24f
            h * scale
        } else {
            38f * scale
        }

        val w = (widthDp * density).toInt()
        val h = (heightDp * density).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val scale = config.scaleFactor
        val theme = config.theme

        val w = width.toFloat()
        val h = height.toFloat()
        val cornerRadius = 14f * density * scale

        // Background
        val bgAlpha = (config.opacity * 255).toInt().coerceIn(40, 255)
        val baseBg = theme.backgroundColorHex.toInt()
        bgPaint.color = Color.argb(bgAlpha, Color.red(baseBg), Color.green(baseBg), Color.blue(baseBg))
        bgRect.set(2f, 2f, w - 2f, h - 2f)
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)

        // Accent Border
        val primaryCol = theme.primaryColorHex.toInt()
        val secondaryCol = theme.secondaryColorHex.toInt()
        val accentCol = theme.accentColorHex.toInt()

        borderPaint.color = Color.argb(bgAlpha, Color.red(primaryCol), Color.green(primaryCol), Color.blue(primaryCol))
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, borderPaint)

        if (!config.isExpanded) {
            drawCompactMode(canvas, density, scale, primaryCol, secondaryCol)
        } else {
            drawExpandedMode(canvas, density, scale, primaryCol, secondaryCol, accentCol)
        }
    }

    private fun drawCompactMode(canvas: Canvas, density: Float, scale: Float, primaryCol: Int, secondaryCol: Int) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Glowing live FPS text
        textPaint.color = primaryCol
        textPaint.textSize = 18f * density * scale
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

        val fpsStr = "${metrics.currentFps}"
        val fpsTextW = textPaint.measureText(fpsStr)
        val fpsY = (h / 2f) + (textPaint.textSize / 3f)

        // Status dot
        val dotRadius = 4f * density * scale
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (metrics.currentFps >= config.warningFpsThreshold) primaryCol else Color.RED
            style = Paint.Style.FILL
        }
        val dotX = 14f * density * scale
        canvas.drawCircle(dotX, h / 2f, dotRadius, dotPaint)

        // Draw "FPS" label + value
        val textStartX = dotX + 8f * density * scale
        canvas.drawText(fpsStr, textStartX, fpsY, textPaint)

        subTextPaint.color = secondaryCol
        subTextPaint.textSize = 10f * density * scale
        subTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FPS", textStartX + fpsTextW + 3f, fpsY - 4f, subTextPaint)

        // Tiny avg display
        if (config.showAverageFps && metrics.avgFps > 0) {
            subTextPaint.color = Color.LTGRAY
            subTextPaint.textSize = 9f * density * scale
            canvas.drawText(String.format("ø%.0f", metrics.avgFps), textStartX + fpsTextW + 3f, fpsY + 8f, subTextPaint)
        }
    }

    private fun drawExpandedMode(canvas: Canvas, density: Float, scale: Float, primaryCol: Int, secondaryCol: Int, accentCol: Int) {
        val w = width.toFloat()
        val pad = 10f * density * scale

        // Header: Live FPS (Large) + Frame Time
        textPaint.color = primaryCol
        textPaint.textSize = 24f * density * scale
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

        val fpsVal = "${metrics.currentFps}"
        val fpsValW = textPaint.measureText(fpsVal)
        val headerY = pad + textPaint.textSize
        canvas.drawText(fpsVal, pad, headerY, textPaint)

        // FPS Unit Tag
        subTextPaint.color = primaryCol
        subTextPaint.textSize = 11f * density * scale
        subTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FPS", pad + fpsValW + 4f, headerY - 7f, subTextPaint)

        // Frame time pill (e.g. "16.6 ms")
        if (config.showFrameTimeMs) {
            val ftText = String.format("%.1f ms", metrics.currentFrameTimeMs)
            subTextPaint.color = secondaryCol
            subTextPaint.textSize = 11f * density * scale
            val ftW = subTextPaint.measureText(ftText)
            val ftX = w - pad - ftW
            canvas.drawText(ftText, ftX, headerY - 5f, subTextPaint)
        }

        headerRect.set(0f, 0f, w, headerY + 6f)

        // Divider
        val divY = headerY + 6f * density * scale
        val divPaint = Paint().apply {
            color = Color.argb(60, 255, 255, 255)
            strokeWidth = 1f
        }
        canvas.drawLine(pad, divY, w - pad, divY, divPaint)

        var curY = divY + 14f * density * scale

        // Metrics Row 1: Average, Min, Max FPS
        if (config.showAverageFps || config.showMinMaxFps) {
            subTextPaint.textSize = 10.5f * density * scale
            subTextPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

            val avgStr = String.format("AVG:%.1f", metrics.avgFps)
            val minStr = "MIN:${metrics.minFps}"
            val maxStr = "MAX:${metrics.maxFps}"

            subTextPaint.color = secondaryCol
            canvas.drawText(avgStr, pad, curY, subTextPaint)

            subTextPaint.color = Color.WHITE
            val avgW = subTextPaint.measureText(avgStr) + 10f * density * scale
            canvas.drawText(minStr, pad + avgW, curY, subTextPaint)

            val minW = subTextPaint.measureText(minStr) + 10f * density * scale
            canvas.drawText(maxStr, pad + avgW + minW, curY, subTextPaint)

            curY += 13f * density * scale
        }

        // Metrics Row 2: 1% Low, 0.1% Low or Stutters
        if (config.showOnePercentLow) {
            subTextPaint.textSize = 10.5f * density * scale
            subTextPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

            val lowStr = String.format("1%%LOW:%.0f", metrics.onePercentLowFps)
            val stutterStr = "STUT:${metrics.stutterCount}"
            val stabStr = "${metrics.stabilityPercent}% STAB"

            subTextPaint.color = accentCol
            canvas.drawText(lowStr, pad, curY, subTextPaint)

            val lowW = subTextPaint.measureText(lowStr) + 8f * density * scale
            subTextPaint.color = if (metrics.stutterCount > 5) Color.RED else Color.LTGRAY
            canvas.drawText(stutterStr, pad + lowW, curY, subTextPaint)

            val stutW = subTextPaint.measureText(stutterStr) + 8f * density * scale
            subTextPaint.color = Color.LTGRAY
            canvas.drawText(stabStr, pad + lowW + stutW, curY, subTextPaint)

            curY += 13f * density * scale
        }

        // Live Frame Time Sparkline Graph
        if (config.showFrameGraph) {
            val graphH = 26f * density * scale
            val graphW = w - (pad * 2)
            graphRect.set(pad, curY, pad + graphW, curY + graphH)

            // Graph background
            val gBgPaint = Paint().apply {
                color = Color.argb(80, 0, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(graphRect, 4f, 4f, gBgPaint)

            // Target frame time guideline (e.g. 16.6ms for 60fps)
            val expectedMs = 1000f / metrics.targetFps
            val maxMs = max(33.3f, expectedMs * 2f)
            val targetLineY = graphRect.bottom - ((expectedMs / maxMs) * graphH)

            graphTargetPaint.color = Color.argb(120, 255, 255, 255)
            canvas.drawLine(graphRect.left, targetLineY, graphRect.right, targetLineY, graphTargetPaint)

            // Draw line graph
            val history = metrics.frameTimesHistory
            if (history.size >= 2) {
                graphPath.reset()
                val stepX = graphW / (history.size - 1)
                for (i in history.indices) {
                    val ms = history[i].coerceIn(0f, maxMs)
                    val px = graphRect.left + (i * stepX)
                    val py = (graphRect.bottom - ((ms / maxMs) * graphH)).coerceIn(graphRect.top, graphRect.bottom)
                    if (i == 0) graphPath.moveTo(px, py) else graphPath.lineTo(px, py)
                }
                graphLinePaint.color = primaryCol
                canvas.drawPath(graphPath, graphLinePaint)
            }

            curY += graphH + 8f * density * scale
        }

        // System Telemetry Row: RAM % and Battery Temp
        if (config.showSystemTelemetry) {
            subTextPaint.textSize = 9.5f * density * scale
            subTextPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            subTextPaint.color = Color.LTGRAY

            val ramStr = "RAM:${metrics.ramUsedPercent}%"
            val tempStr = String.format("TEMP:%.1f°C", metrics.batteryTempCelsius)
            val hzStr = "${metrics.refreshRate}Hz"

            canvas.drawText("$ramStr  $tempStr  $hzStr", pad, curY, subTextPaint)
            curY += 12f * density * scale
        }

        // Action Buttons Row: [🔄 Reset] [⚙️ App] [✖ Close]
        val btnH = 18f * density * scale
        val btnW = 46f * density * scale
        val btnRadius = 4f * density * scale
        btnPaint.color = Color.argb(90, 255, 255, 255)
        btnPaint.style = Paint.Style.FILL

        val btnY = curY + 2f * density * scale

        // Reset Button
        resetBtnRect.set(pad, btnY, pad + btnW, btnY + btnH)
        canvas.drawRoundRect(resetBtnRect, btnRadius, btnRadius, btnPaint)
        drawButtonText(canvas, "RESET", resetBtnRect, density * scale)

        // App Button
        val appX = pad + btnW + 6f * density * scale
        appBtnRect.set(appX, btnY, appX + btnW, btnY + btnH)
        canvas.drawRoundRect(appBtnRect, btnRadius, btnRadius, btnPaint)
        drawButtonText(canvas, "APP", appBtnRect, density * scale)

        // Close Button
        val closeW = 28f * density * scale
        val closeX = w - pad - closeW
        closeBtnRect.set(closeX, btnY, closeX + closeW, btnY + btnH)
        val closeBgPaint = Paint().apply {
            color = Color.argb(120, 239, 68, 68) // Crimson
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(closeBtnRect, btnRadius, btnRadius, closeBgPaint)
        drawButtonText(canvas, "✕", closeBtnRect, density * scale)
    }

    private fun drawButtonText(canvas: Canvas, text: String, rect: RectF, densityScale: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f * densityScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val y = rect.centerY() - ((p.descent() + p.ascent()) / 2f)
        canvas.drawText(text, rect.centerX(), y, p)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()

                if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    isDragging = true
                }

                if (isDragging) {
                    onDrag(dx, dy)
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    onSnapEnd()
                } else {
                    // Check button clicks
                    val localX = event.x
                    val localY = event.y

                    if (config.isExpanded) {
                        if (resetBtnRect.contains(localX, localY)) {
                            onResetStats()
                            return true
                        }
                        if (appBtnRect.contains(localX, localY)) {
                            onOpenApp()
                            return true
                        }
                        if (closeBtnRect.contains(localX, localY)) {
                            onClose()
                            return true
                        }
                    }
                    // Tap on body or header toggles expanded / compact
                    onToggleExpand()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
