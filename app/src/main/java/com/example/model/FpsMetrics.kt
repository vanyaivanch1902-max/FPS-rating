package com.example.model

/**
 * Data model holding live frame rate and performance telemetry.
 */
data class FpsMetrics(
    val currentFps: Int = 0,
    val avgFps: Float = 0f,
    val minFps: Int = 0,
    val maxFps: Int = 0,
    val onePercentLowFps: Float = 0f,
    val pointOnePercentLowFps: Float = 0f,
    val currentFrameTimeMs: Float = 16.6f,
    val targetFps: Int = 60,
    val refreshRate: Int = 60,
    val stutterCount: Int = 0,
    val totalFramesTracked: Long = 0L,
    val sessionDurationSeconds: Long = 0L,
    val ramUsedPercent: Int = 0,
    val ramUsedMb: Int = 0,
    val totalRamMb: Int = 0,
    val batteryTempCelsius: Float = 0f,
    val frameTimesHistory: List<Float> = emptyList()
) {
    /**
     * FPS health rating based on target FPS.
     */
    val stabilityPercent: Int
        get() {
            if (targetFps <= 0 || avgFps <= 0f) return 100
            val ratio = (avgFps / targetFps.toFloat()).coerceIn(0f, 1f)
            return (ratio * 100).toInt()
        }
}

/**
 * Color themes for the PC-style HUD.
 */
enum class HudTheme(
    val title: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long,
    val backgroundColorHex: Long
) {
    RIVA_AFTERBURNER(
        title = "RivaTuner / MSI",
        primaryColorHex = 0xFF00FF66, // Classic Neon Green
        secondaryColorHex = 0xFFFF9900, // Vivid Orange
        accentColorHex = 0xFF00E5FF, // Cyan
        backgroundColorHex = 0xCC111827 // Translucent dark slate
    ),
    GEFORCE_EMERALD(
        title = "GeForce Shadow",
        primaryColorHex = 0xFF76B900, // Nvidia Green
        secondaryColorHex = 0xFFFFFFFF, // Pure White
        accentColorHex = 0xFF9AE600, // Lime
        backgroundColorHex = 0xDD0D1117 // Stealth charcoal
    ),
    STEAM_CYBER(
        title = "Steam Neon",
        primaryColorHex = 0xFF00F0FF, // Cyber Cyan
        secondaryColorHex = 0xFFFFE600, // Neon Yellow
        accentColorHex = 0xFFFF0055, // Hot Pink
        backgroundColorHex = 0xD9101622 // Deep navy
    ),
    CYBERPUNK(
        title = "Cyberpunk 2077",
        primaryColorHex = 0xFFFCEE0A, // Cyber Yellow
        secondaryColorHex = 0xFFFF003C, // Cyber Red
        accentColorHex = 0xFF00E5FF, // Neon Blue
        backgroundColorHex = 0xE60A0A10 // Midnight black
    ),
    MONOCHROME_PRO(
        title = "Monochrome Pro",
        primaryColorHex = 0xFFF1F5F9, // Crisp White
        secondaryColorHex = 0xFF94A3B8, // Steel Gray
        accentColorHex = 0xFF38BDF8, // Ice Blue
        backgroundColorHex = 0xE60F172A // Dark navy slate
    )
}

/**
 * Visual layout and visibility settings for the floating HUD.
 */
data class HudConfig(
    val isExpanded: Boolean = true,
    val scaleFactor: Float = 1.0f, // 0.8f to 1.4f
    val opacity: Float = 0.90f, // 0.4f to 1.0f
    val theme: HudTheme = HudTheme.RIVA_AFTERBURNER,
    val showAverageFps: Boolean = true,
    val showMinMaxFps: Boolean = true,
    val showOnePercentLow: Boolean = true,
    val showFrameTimeMs: Boolean = true,
    val showFrameGraph: Boolean = true,
    val showSystemTelemetry: Boolean = true,
    val targetFps: Int = 60,
    val warningFpsThreshold: Int = 45,
    val snapToEdges: Boolean = true
)
