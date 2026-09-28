package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BenchmarkRepository
import com.example.data.BenchmarkSessionEntity
import com.example.data.PreferencesManager
import com.example.engine.FpsTracker
import com.example.model.FpsMetrics
import com.example.model.HudConfig
import com.example.overlay.FpsOverlayService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager.getInstance(application)
    private val repository = BenchmarkRepository(AppDatabase.getInstance(application).benchmarkDao())

    // In-app tracker ensures telemetry is active even before system overlay is triggered
    private val inAppTracker = FpsTracker(application)

    val hudConfig: StateFlow<HudConfig> = preferencesManager.hudConfig

    val isOverlayServiceRunning: StateFlow<Boolean> = FpsOverlayService.isOverlayRunning

    private val _liveMetrics = MutableStateFlow(FpsMetrics())
    val liveMetrics: StateFlow<FpsMetrics> = _liveMetrics.asStateFlow()

    val benchmarkHistory: StateFlow<List<BenchmarkSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Benchmark recording state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    private var recordingJob: Job? = null
    private var benchmarkStartMetrics: FpsMetrics? = null
    private var benchmarkTargetGame: String = "Stress Test Sandbox"

    // Stress testing sandbox states
    var particleCount = MutableStateFlow(800)
    var isPhysicsHeavy = MutableStateFlow(true)
    var selectedStressFpsLimit = MutableStateFlow(0) // 0 = unlocked

    init {
        inAppTracker.start()

        // Sync with overlay tracker if active, otherwise in-app tracker
        viewModelScope.launch {
            while (true) {
                val overlayTracker = FpsOverlayService.activeTracker
                if (overlayTracker != null && FpsOverlayService.isOverlayRunning.value) {
                    overlayTracker.metrics.collectLatest {
                        _liveMetrics.value = it
                    }
                } else {
                    inAppTracker.metrics.collectLatest {
                        _liveMetrics.value = it
                    }
                }
                delay(500)
            }
        }

        viewModelScope.launch {
            hudConfig.collectLatest { cfg ->
                inAppTracker.setTargetFps(cfg.targetFps)
                FpsOverlayService.activeTracker?.setTargetFps(cfg.targetFps)
            }
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun toggleOverlay(context: Context) {
        if (!hasOverlayPermission(context)) {
            openOverlaySettings(context)
            return
        }

        if (isOverlayServiceRunning.value) {
            FpsOverlayService.stopService(context)
        } else {
            FpsOverlayService.startService(context)
        }
    }

    fun resetStats() {
        inAppTracker.resetSessionStats()
        FpsOverlayService.activeTracker?.resetSessionStats()
    }

    fun updateConfig(config: HudConfig) {
        preferencesManager.updateConfig(config)
    }

    fun startRecording(gameName: String = "Game Benchmark", limitSeconds: Int? = null) {
        if (_isRecording.value) return
        resetStats()
        _isRecording.value = true
        _recordingDuration.value = 0
        benchmarkTargetGame = gameName
        benchmarkStartMetrics = liveMetrics.value

        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            var seconds = 0
            while (_isRecording.value) {
                delay(1000)
                seconds++
                _recordingDuration.value = seconds
                if (limitSeconds != null && seconds >= limitSeconds) {
                    stopRecording()
                    break
                }
            }
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        _isRecording.value = false
        recordingJob?.cancel()

        val current = liveMetrics.value
        val start = benchmarkStartMetrics ?: current

        val session = BenchmarkSessionEntity(
            gameName = benchmarkTargetGame,
            durationSeconds = _recordingDuration.value.toLong().coerceAtLeast(1L),
            avgFps = current.avgFps,
            minFps = current.minFps,
            maxFps = current.maxFps,
            onePercentLow = current.onePercentLowFps,
            stutterCount = current.stutterCount,
            targetFps = current.targetFps,
            batteryTempStart = start.batteryTempCelsius,
            batteryTempEnd = current.batteryTempCelsius,
            notes = "Recorded at ${current.refreshRate}Hz screen refresh"
        )

        viewModelScope.launch {
            repository.saveSession(session)
        }
    }

    fun deleteSession(session: BenchmarkSessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun injectArtificialStutter(durationMs: Long = 120L) {
        viewModelScope.launch {
            // Artificial frame drop on render thread
            SystemClock.sleep(durationMs)
        }
    }

    override fun onCleared() {
        super.onCleared()
        inAppTracker.stop()
    }
}
