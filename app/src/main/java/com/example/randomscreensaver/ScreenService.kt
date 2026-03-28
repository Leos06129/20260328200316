package com.example.randomscreensaver

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import kotlin.random.Random

class ScreenService : Service() {
    
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private lateinit var notificationHelper: NotificationHelper
    
    companion object {
        private const val TAG = "ScreenService"
        const val ACTION_STOP_SERVICE = "stop_service"
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
            ACTION_STOP_SERVICE -> {
                stopService()
                return START_NOT_STICKY
            }
        }
        
        if (!isRunning) {
            startService()
        }
        
        return START_STICKY
    }
    
    private fun startService() {
        isRunning = true
        Log.d(TAG, "开始屏保服务")
        
        // 启动前台服务
        val notification = notificationHelper.createServiceNotification()
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        
        // 开始显示循环
        startDisplayLoop()
    }
    
    private fun stopService() {
        if (!isRunning) {
            return
        }
        
        isRunning = false
        Log.d(TAG, "停止屏保服务")
        
        // 移除所有回调
        handler.removeCallbacksAndMessages(null)
        
        // 停止服务
        stopForeground(true)
        stopSelf()
    }
    
    private fun startDisplayLoop() {
        if (!isRunning) return
        
        // 从设置中获取参数
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val message = prefs.getString(MainActivity.PREF_MESSAGE, getString(R.string.default_message)) 
            ?: getString(R.string.default_message)
        val maxInterval = prefs.getInt(MainActivity.PREF_MAX_INTERVAL, MainActivity.DEFAULT_MAX_INTERVAL)
        val minInterval = prefs.getInt(MainActivity.PREF_MIN_INTERVAL, MainActivity.DEFAULT_MIN_INTERVAL)
        
        // 获取锁屏状态
        val isScreenLocked = prefs.getBoolean("screen_locked", false)
        
        // 计算显示间隔
        val interval = if (isScreenLocked) {
            // 锁屏时：10-12秒
            Random.nextInt(10, 13) * 1000L
        } else {
            // 常规时：用户设置的范围
            Random.nextInt(minInterval, maxInterval + 1) * 1000L
        }
        
        Log.d(TAG, "下次显示间隔: ${interval/1000}秒, 锁屏状态: $isScreenLocked")
        
        // 延迟显示
        handler.postDelayed({
            if (isRunning) {
                showFullscreenMessage(message, isScreenLocked, maxInterval, minInterval)
                // 继续下一次循环
                startDisplayLoop()
            }
        }, interval)
    }
    
    private fun showFullscreenMessage(message: String, isLocked: Boolean, maxInterval: Int, minInterval: Int) {
        // 启动全屏Activity
        val intent = Intent(this, FullscreenActivity::class.java).apply {
            putExtra(FullscreenActivity.EXTRA_MESSAGE, message)
            putExtra(FullscreenActivity.EXTRA_IS_LOCKED, isLocked)
            putExtra(FullscreenActivity.EXTRA_MAX_INTERVAL, maxInterval)
            putExtra(FullscreenActivity.EXTRA_MIN_INTERVAL, minInterval)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "服务销毁")
        stopService()
    }
}