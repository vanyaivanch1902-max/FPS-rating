package com.example.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.WindowManager
import com.example.model.FpsMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * High-precision Choreographer-based frame rate tracker for Android gameplay.
 * Accurately measures:
 * - Real-time Live FPS
 * - Session Average FPS
 * - Session Minimum FPS
 * - Session Maximum FPS
 * - 1% Low FPS (Micro-stutter indicator)
 * - 0.1% Low FPS
 * - Frame Time (ms)
 * - Stutter frequency
 */
class FpsTracker(private val context: Context) : Choreographer.FrameCallback {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val systemMonitor = SystemMonitor(context)

    private val _metrics = MutableStateFlow(FpsMetrics())
    val metrics: StateFlow<FpsMetrics> = _metrics.asStateFlow()

    @Volatile
    private var isTracking = false

    private var lastFrameTimeNanos: Long = 0L
    private var sessionStartTimeNanos: Long = 0L

    // Window for calculating real-time live FPS (1 second rolling window)
    private val frameTimestamps = ArrayDeque<Long>(150)
    // Recent frame times (in ms) for sparkline graph (last 45 frames)
    private val frameTimeHistory = ArrayDeque<Float>(45)
    // Full list of sampled FPS values for true 1% Low & Average calculation
    private val sampledFpsList = ArrayList<Int>(5000)

    private var totalFramesInSession: Long = 0L
    private var sessionMinFps: Int = Int.MAX_VALUE
    private var sessionMaxFps: Int = 0
    private var stutterCount: Int = 0

    private var refreshRateHz: Int = 60
    private var targetFps: Int = 60

    // Periodic emitter for telemetry (every 250ms for smooth live updates without thrashing)
    private var lastUiUpdateNanos: Long = 0L
    private val updateIntervalNanos = 250_000_000L // 250ms

    init {
        detectDisplayRefreshRate()
    }

    private fun detectDisplayRefreshRate() {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val display = wm?.defaultDisplay
            val rate = display?.refreshRate?.roundToInt() ?: 60
            refreshRateHz = if (rate in 30..240) rate else 60
            targetFps = refreshRateHz
        } catch (_: Exception) {
            refreshRateHz = 60
            targetFps = 60
        }
    }

    fun setTargetFps(target: Int) {
        targetFps = target
        _metrics.value = _metrics.value.copy(targetFps = target)
    }

    fun start() {
        if (isTracking) return
        isTracking = true
        systemMonitor.start()
        detectDisplayRefreshRate()
        resetSessionStats()

        mainHandler.post {
            lastFrameTimeNanos = 0L
            sessionStartTimeNanos = System.nanoTime()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun stop() {
        isTracking = false
        mainHandler.post {
            Choreographer.getInstance().removeFrameCallback(this)
        }
        systemMonitor.stop()
    }

    fun resetSessionStats() {
        synchronized(this) {
            sessionStartTimeNanos = System.nanoTime()
            totalFramesInSession = 0L
            sessionMinFps = Int.MAX_VALUE
            sessionMaxFps = 0
            stutterCount = 0
            sampledFpsList.clear()
            frameTimestamps.clear()
            frameTimeHistory.clear()
        }
        val (ramPercent, usedMb, totalMb) = systemMonitor.getMemoryStats()
        _metrics.value = FpsMetrics(
            currentFps = 0,
            avgFps = 0f,
            minFps = 0,
            maxFps = 0,
            onePercentLowFps = 0f,
            pointOnePercentLowFps = 0f,
            currentFrameTimeMs = 1000f / targetFps,
            targetFps = targetFps,
            refreshRate = refreshRateHz,
            stutterCount = 0,
            ramUsedPercent = ramPercent,
            ramUsedMb = usedMb,
            totalRamMb = totalMb,
            batteryTempCelsius = systemMonitor.batteryTempCelsius
        )
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isTracking) return

        // Schedule next frame callback immediately
        Choreographer.getInstance().postFrameCallback(this)

        if (lastFrameTimeNanos == 0L) {
            lastFrameTimeNanos = frameTimeNanos
            return
        }

        val frameDeltaNanos = frameTimeNanos - lastFrameTimeNanos
        lastFrameTimeNanos = frameTimeNanos

        if (frameDeltaNanos <= 0) return

        val frameDeltaMs = frameDeltaNanos / 1_000_000f
        totalFramesInSession++

        // Stutter detection: frame time exceeds 1.8x expected frame time or > 33.3ms
        val expectedMs = 1000f / targetFps
        if (frameDeltaMs > (expectedMs * 1.8f) || frameDeltaMs > 33.3f) {
            stutterCount++
        }

        // Add to recent frame history for the sparkline graph
        synchronized(frameTimeHistory) {
            if (frameTimeHistory.size >= 45) {
                frameTimeHistory.removeFirst()
            }
            frameTimeHistory.add(frameDeltaMs)
        }

        // 1-second rolling window for live FPS calculation
        frameTimestamps.add(frameTimeNanos)
        val cutoff = frameTimeNanos - 1_000_000_000L
        while (!frameTimestamps.isEmpty() && frameTimestamps.first() < cutoff) {
            frameTimestamps.removeFirst()
        }

        val currentRollingFps = frameTimestamps.size

        // Only begin recording min/max/avg after warm-up (first 10 frames)
        if (totalFramesInSession > 10 && currentRollingFps in 1..240) {
            synchronized(sampledFpsList) {
                // Record sample periodically
                if (totalFramesInSession % 4 == 0L) {
                    sampledFpsList.add(currentRollingFps)
                    if (sampledFpsList.size > 10000) {
                        sampledFpsList.subList(0, 2000).clear()
                    }
                }
            }

            if (currentRollingFps < sessionMinFps) {
                sessionMinFps = currentRollingFps
            }
            if (currentRollingFps > sessionMaxFps) {
                sessionMaxFps = currentRollingFps
            }
        }

        // Update UI state periodically to maintain optimal performance
        if (frameTimeNanos - lastUiUpdateNanos >= updateIntervalNanos) {
            lastUiUpdateNanos = frameTimeNanos
            publishMetrics(currentRollingFps, frameDeltaMs, frameTimeNanos)
        }
    }

    private fun publishMetrics(currentFps: Int, currentFrameTimeMs: Float, nowNanos: Long) {
        val durationSeconds = max(1L, (nowNanos - sessionStartTimeNanos) / 1_000_000_000L)
        val avgFps = if (durationSeconds > 0) {
            (totalFramesInSession.toFloat() / durationSeconds.toFloat()).coerceAtMost(refreshRateHz.toFloat())
        } else {
            currentFps.toFloat()
        }

        var onePercentLow = 0f
        var pointOnePercentLow = 0f
        synchronized(sampledFpsList) {
            if (sampledFpsList.size >= 10) {
                val sorted = sampledFpsList.sorted()
                val idx1 = ((sorted.size - 1) * 0.01f).toInt().coerceIn(0, sorted.size - 1)
                val idx01 = ((sorted.size - 1) * 0.001f).toInt().coerceIn(0, sorted.size - 1)
                onePercentLow = sorted[idx1].toFloat()
                pointOnePercentLow = sorted[idx01].toFloat()
            } else if (sessionMinFps != Int.MAX_VALUE) {
                onePercentLow = sessionMinFps.toFloat()
                pointOnePercentLow = sessionMinFps.toFloat()
            }
        }

        val displayMin = if (sessionMinFps == Int.MAX_VALUE) currentFps else sessionMinFps
        val displayMax = if (sessionMaxFps == 0) currentFps else sessionMaxFps
        val (ramPercent, usedMb, totalMb) = systemMonitor.getMemoryStats()

        val historyCopy: List<Float>
        synchronized(frameTimeHistory) {
            historyCopy = frameTimeHistory.toList()
        }

        _metrics.value = FpsMetrics(
            currentFps = currentFps,
            avgFps = avgFps,
            minFps = displayMin,
            maxFps = displayMax,
            onePercentLowFps = onePercentLow,
            pointOnePercentLowFps = pointOnePercentLow,
            currentFrameTimeMs = currentFrameTimeMs,
            targetFps = targetFps,
            refreshRate = refreshRateHz,
            stutterCount = stutterCount,
            totalFramesTracked = totalFramesInSession,
            sessionDurationSeconds = durationSeconds,
            ramUsedPercent = ramPercent,
            ramUsedMb = usedMb,
            totalRamMb = totalMb,
            batteryTempCelsius = systemMonitor.batteryTempCelsius,
            frameTimesHistory = historyCopy
        )
    }
}
