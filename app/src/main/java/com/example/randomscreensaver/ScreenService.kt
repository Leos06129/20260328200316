package com.example.randomscreensaver

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import kotlin.random.Random

class ScreenService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var notificationHelper: NotificationHelper
    private var mediaPlayer: MediaPlayer? = null
    private var cuckooRunnable: Runnable? = null

    companion object {
        private const val TAG = "ScreenService"
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_MESSAGE2 = "message2"

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

        // 启动布谷鸟叫声定时器
        startCuckooSoundTimer()

        startDisplayLoop()
    }

    private fun stopScreenService() {
        if (!isServiceRunning) return

        isServiceRunning = false
        Log.d(TAG, "停止屏保服务")

        // 停止布谷鸟叫声
        stopCuckooSoundTimer()

        handler.removeCallbacksAndMessages(null)
        stopForeground(true)
        stopSelf()
    }

    private fun startDisplayLoop() {
        if (!isServiceRunning) return
        
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val message = prefs.getString(MainActivity.PREF_MESSAGE, getString(R.string.default_message))
            ?: getString(R.string.default_message)
        val message2 = prefs.getString(MainActivity.PREF_MESSAGE2, null)
        val maxInterval = prefs.getInt(MainActivity.PREF_MAX_INTERVAL, MainActivity.DEFAULT_MAX_INTERVAL)
        val minInterval = prefs.getInt(MainActivity.PREF_MIN_INTERVAL, MainActivity.DEFAULT_MIN_INTERVAL)
        val isScreenLocked = prefs.getBoolean("screen_locked", false)

        // 计算显示持续时间（在用户设定范围内随机）
        val displayDuration = Random.nextInt(minInterval, maxInterval + 1) * 1000L

        Log.d(TAG, "显示持续时间: ${displayDuration / 1000}秒, 锁屏状态: $isScreenLocked")

        // 立即显示文字内容
        showFullscreenMessage(message, message2, isScreenLocked, maxInterval, minInterval, displayDuration)
        
        // 在显示持续时间结束后，开始下一个显示循环（先黑屏，后等待最短间隔）
        handler.postDelayed({
            if (isServiceRunning) {
                // 文字显示结束，进入黑屏状态
                // 等待最短间隔后开始下一个显示循环
                Log.d(TAG, "文字显示结束，进入黑屏状态")
                handler.postDelayed({
                    if (isServiceRunning) {
                        startDisplayLoop()
                    }
                }, minInterval * 1000L)
            }
        }, displayDuration)
    }

    private fun showFullscreenMessage(
        message: String,
        message2: String?,
        isLocked: Boolean,
        maxInterval: Int,
        minInterval: Int,
        displayDuration: Long
    ) {
        val intent = Intent(this, FullscreenActivity::class.java).apply {
            putExtra(FullscreenActivity.EXTRA_MESSAGE, message)
            putExtra(EXTRA_MESSAGE2, message2)
            putExtra(FullscreenActivity.EXTRA_IS_LOCKED, isLocked)
            putExtra(FullscreenActivity.EXTRA_MAX_INTERVAL, maxInterval)
            putExtra(FullscreenActivity.EXTRA_MIN_INTERVAL, minInterval)
            putExtra(FullscreenActivity.EXTRA_DISPLAY_DURATION, displayDuration)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 启动布谷鸟叫声定时器
     * 每隔2~5分钟随机播放一次布谷鸟叫声
     */
    private fun startCuckooSoundTimer() {
        scheduleNextCuckooSound()
    }

    /**
     * 安排下一次布谷鸟叫声
     */
    private fun scheduleNextCuckooSound() {
        if (!isServiceRunning) return

        // 随机等待2~5分钟（120000~300000毫秒）
        val nextInterval = Random.nextLong(120000, 300001)

        Log.d(TAG, "布谷鸟叫声定时器: ${nextInterval / 1000}秒后播放")

        cuckooRunnable = Runnable {
            if (isServiceRunning) {
                playReminderSound()
                // 播放完后继续安排下一次
                scheduleNextCuckooSound()
            }
        }

        handler.postDelayed(cuckooRunnable!!, nextInterval)
    }

    /**
     * 播放提醒声音
     */
    private fun playReminderSound() {
        try {
            val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
            val soundType = prefs.getString(MainActivity.PREF_SOUND, MainActivity.SOUND_CUCKOO)
                ?: MainActivity.SOUND_CUCKOO

            mediaPlayer?.release()

            if (soundType == MainActivity.SOUND_CUCKOO) {
                // 播放布谷鸟叫声
                mediaPlayer = MediaPlayer.create(this, R.raw.cuckoo_sound)
                if (mediaPlayer != null) {
                    mediaPlayer?.setOnCompletionListener {
                        Log.d(TAG, "布谷鸟叫声播放完成")
                    }
                    mediaPlayer?.start()
                    Log.d(TAG, "播放布谷鸟叫声")
                } else {
                    Log.w(TAG, "未找到布谷鸟叫声音频文件")
                }
            } else {
                // 播放系统默认通知声音
                val notification = android.media.RingtoneManager.getDefaultUri(
                    android.media.RingtoneManager.TYPE_NOTIFICATION
                )
                val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, notification)
                ringtone?.play()
                Log.d(TAG, "播放系统默认通知声音")
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放提醒声音失败: ${e.message}")
        }
    }

    /**
     * 停止布谷鸟叫声定时器
     */
    private fun stopCuckooSoundTimer() {
        cuckooRunnable?.let {
            handler.removeCallbacks(it)
        }
        cuckooRunnable = null
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "服务销毁")
        stopScreenService()
    }
}
