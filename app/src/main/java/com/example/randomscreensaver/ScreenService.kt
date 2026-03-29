package com.example.randomscreensaver

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import kotlin.random.Random

class ScreenService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var notificationHelper: NotificationHelper

    companion object {
        private const val TAG = "ScreenService"
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"

        // 供 MainActivity 查询服务是否运行
        var isServiceRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "服务创建")
        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "服务启动命令, action: ${intent?.action}")

        when (intent?.action) {
            ACTION_STOP -> {
                stopScreenService()
                return START_NOT_STICKY
            }
        }

        if (!isServiceRunning) {
            startScreenService()
        }

        return START_STICKY
    }

    private fun startScreenService() {
        isServiceRunning = true
        Log.d(TAG, "开始屏保服务")

        val notification = notificationHelper.createServiceNotification()
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)

        startDisplayLoop()
    }

    private fun stopScreenService() {
        if (!isServiceRunning) return

        isServiceRunning = false
        Log.d(TAG, "停止屏保服务")

        handler.removeCallbacksAndMessages(null)
        stopForeground(true)
        stopSelf()
    }

    private fun startDisplayLoop() {
        if (!isServiceRunning) return

        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val message = prefs.getString(MainActivity.PREF_MESSAGE, getString(R.string.default_message))
            ?: getString(R.string.default_message)
        val maxInterval = prefs.getInt(MainActivity.PREF_MAX_INTERVAL, MainActivity.DEFAULT_MAX_INTERVAL)
        val minInterval = prefs.getInt(MainActivity.PREF_MIN_INTERVAL, MainActivity.DEFAULT_MIN_INTERVAL)
        val isScreenLocked = prefs.getBoolean("screen_locked", false)

        val interval = if (isScreenLocked) {
            Random.nextInt(10, 13) * 1000L
        } else {
            Random.nextInt(minInterval, maxInterval + 1) * 1000L
        }

        Log.d(TAG, "下次显示间隔: ${interval / 1000}秒, 锁屏状态: $isScreenLocked")

        handler.postDelayed({
            if (isServiceRunning) {
                showFullscreenMessage(message, isScreenLocked, maxInterval, minInterval)
                startDisplayLoop()
            }
        }, interval)
    }

    private fun showFullscreenMessage(
        message: String,
        isLocked: Boolean,
        maxInterval: Int,
        minInterval: Int
    ) {
        val intent = Intent(this, FullscreenActivity::class.java).apply {
            putExtra(FullscreenActivity.EXTRA_MESSAGE, message)
            putExtra(FullscreenActivity.EXTRA_IS_LOCKED, isLocked)
            putExtra(FullscreenActivity.EXTRA_MAX_INTERVAL, maxInterval)
            putExtra(FullscreenActivity.EXTRA_MIN_INTERVAL, minInterval)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "服务销毁")
        stopScreenService()
    }
}
