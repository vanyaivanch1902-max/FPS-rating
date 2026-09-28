package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.HudConfig
import com.example.model.HudTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fps_overlay_prefs", Context.MODE_PRIVATE)

    private val _hudConfig = MutableStateFlow(loadConfig())
    val hudConfig: StateFlow<HudConfig> = _hudConfig.asStateFlow()

    private fun loadConfig(): HudConfig {
        val themeName = prefs.getString("hud_theme", HudTheme.RIVA_AFTERBURNER.name) ?: HudTheme.RIVA_AFTERBURNER.name
        val theme = try { HudTheme.valueOf(themeName) } catch (_: Exception) { HudTheme.RIVA_AFTERBURNER }

        return HudConfig(
            isExpanded = prefs.getBoolean("is_expanded", true),
            scaleFactor = prefs.getFloat("scale_factor", 1.0f),
            opacity = prefs.getFloat("opacity", 0.90f),
            theme = theme,
            showAverageFps = prefs.getBoolean("show_avg_fps", true),
            showMinMaxFps = prefs.getBoolean("show_min_max_fps", true),
            showOnePercentLow = prefs.getBoolean("show_one_percent_low", true),
            showFrameTimeMs = prefs.getBoolean("show_frame_time_ms", true),
            showFrameGraph = prefs.getBoolean("show_frame_graph", true),
            showSystemTelemetry = prefs.getBoolean("show_system_telemetry", true),
            targetFps = prefs.getInt("target_fps", 60),
            warningFpsThreshold = prefs.getInt("warning_fps_threshold", 45),
            snapToEdges = prefs.getBoolean("snap_to_edges", true)
        )
    }

    fun updateConfig(config: HudConfig) {
        prefs.edit()
            .putBoolean("is_expanded", config.isExpanded)
            .putFloat("scale_factor", config.scaleFactor)
            .putFloat("opacity", config.opacity)
            .putString("hud_theme", config.theme.name)
            .putBoolean("show_avg_fps", config.showAverageFps)
            .putBoolean("show_min_max_fps", config.showMinMaxFps)
            .putBoolean("show_one_percent_low", config.showOnePercentLow)
            .putBoolean("show_frame_time_ms", config.showFrameTimeMs)
            .putBoolean("show_frame_graph", config.showFrameGraph)
            .putBoolean("show_system_telemetry", config.showSystemTelemetry)
            .putInt("target_fps", config.targetFps)
            .putInt("warning_fps_threshold", config.warningFpsThreshold)
            .putBoolean("snap_to_edges", config.snapToEdges)
            .apply()

        _hudConfig.value = config
    }

    companion object {
        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
