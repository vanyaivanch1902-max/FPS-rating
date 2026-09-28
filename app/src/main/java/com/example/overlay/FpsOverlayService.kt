package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesManager
import com.example.engine.FpsTracker
import com.example.model.FpsMetrics
import com.example.model.HudConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FpsOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var hudView: FloatingHudView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private lateinit var preferencesManager: PreferencesManager
    private var currentConfig = HudConfig()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager.getInstance(this)
        currentConfig = preferencesManager.hudConfig.value

        if (activeTracker == null) {
            activeTracker = FpsTracker(this)
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Initializing FPS Meter..."))

        if (Settings.canDrawOverlays(this)) {
            initOverlayView()
        }

        observeMetricsAndConfig()
        activeTracker?.start()
        _isOverlayRunning.value = true
    }

    private fun initOverlayView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val displayMetrics = resources.displayMetrics
        val initialX = 40
        val initialY = (displayMetrics.heightPixels * 0.15f).toInt()

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        hudView = FloatingHudView(
            context = this,
            onDrag = { dx, dy ->
                windowParams?.let { params ->
                    params.x += dx
                    params.y += dy
                    try {
                        windowManager?.updateViewLayout(hudView, params)
                    } catch (_: Exception) {}
                }
            },
            onSnapEnd = {
                snapToNearestEdge()
            },
            onToggleExpand = {
                val newConfig = currentConfig.copy(isExpanded = !currentConfig.isExpanded)
                preferencesManager.updateConfig(newConfig)
            },
            onResetStats = {
                activeTracker?.resetSessionStats()
            },
            onOpenApp = {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            },
            onClose = {
                stopSelf()
            }
        )

        hudView?.updateConfig(currentConfig)
        try {
            windowManager?.addView(hudView, windowParams)
        } catch (_: Exception) {}
    }

    private fun snapToNearestEdge() {
        if (!currentConfig.snapToEdges) return
        val params = windowParams ?: return
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val viewWidth = hudView?.width ?: 200

        val targetX = if (params.x + (viewWidth / 2) < screenWidth / 2) {
            20 // Left dock
        } else {
            screenWidth - viewWidth - 20 // Right dock
        }

        params.x = targetX
        params.y = params.y.coerceIn(50, displayMetrics.heightPixels - 150)
        try {
            windowManager?.updateViewLayout(hudView, params)
        } catch (_: Exception) {}
    }

    private fun observeMetricsAndConfig() {
        serviceScope.launch {
            activeTracker?.metrics?.collectLatest { metrics ->
                hudView?.updateMetrics(metrics)
                updateNotification(metrics)
            }
        }

        serviceScope.launch {
            preferencesManager.hudConfig.collectLatest { config ->
                currentConfig = config
                hudView?.updateConfig(config)
                activeTracker?.setTargetFps(config.targetFps)
                windowParams?.let { params ->
                    try {
                        windowManager?.updateViewLayout(hudView, params)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun updateNotification(metrics: FpsMetrics) {
        val notifyText = "FPS: ${metrics.currentFps} | Avg: ${String.format("%.1f", metrics.avgFps)} | Min: ${metrics.minFps} | Max: ${metrics.maxFps}"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, buildNotification(notifyText))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FPS Overlay Telemetry",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live game frame rates and performance metrics overlay."
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FpsOverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, FpsOverlayService::class.java).apply { action = ACTION_RESET_STATS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FPS Meter Active")
            .setContentText(content)
            .setSmallIcon(R.drawable.img_fps_icon)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(0, "Reset Stats", resetIntent)
            .addAction(0, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RESET_STATS -> {
                activeTracker?.resetSessionStats()
            }
            ACTION_TOGGLE_EXPAND -> {
                val newConfig = currentConfig.copy(isExpanded = !currentConfig.isExpanded)
                preferencesManager.updateConfig(newConfig)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        _isOverlayRunning.value = false
        serviceScope.cancel()
        activeTracker?.stop()

        if (hudView != null && windowManager != null) {
            try {
                windowManager?.removeView(hudView)
            } catch (_: Exception) {}
            hudView = null
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.action.START_OVERLAY"
        const val ACTION_STOP = "com.example.action.STOP_OVERLAY"
        const val ACTION_RESET_STATS = "com.example.action.RESET_STATS"
        const val ACTION_TOGGLE_EXPAND = "com.example.action.TOGGLE_EXPAND"

        private const val CHANNEL_ID = "fps_overlay_channel"
        private const val NOTIFICATION_ID = 9001

        private val _isOverlayRunning = MutableStateFlow(false)
        val isOverlayRunning = _isOverlayRunning.asStateFlow()

        var activeTracker: FpsTracker? = null

        fun startService(context: Context) {
            val intent = Intent(context, FpsOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FpsOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun resetStats(context: Context) {
            val intent = Intent(context, FpsOverlayService::class.java).apply {
                action = ACTION_RESET_STATS
            }
            context.startService(intent)
        }
    }
}
