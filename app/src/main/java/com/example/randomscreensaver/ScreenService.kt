package com.example.randomscreensaver

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import kotlin.random.Random

class ScreenService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var notificationHelper: NotificationHelper
    private var mediaPlayer: MediaPlayer? = null
    private var cuckooRunnable: Runnable? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

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

        // 获取音频管理器
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // 获取电源管理器
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RandomScreensaver::CuckooSoundLock"
        )
        wakeLock?.setReferenceCounted(false)
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

        // 释放音频焦点
        abandonAudioFocus()

        // 释放唤醒锁
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }

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
                playReminderSoundWithRetry()
                // 播放完后继续安排下一次
                scheduleNextCuckooSound()
            }
        }

        handler.postDelayed(cuckooRunnable!!, nextInterval)
    }

    /**
     * 播放提醒声音（带重试机制）
     */
    private fun playReminderSoundWithRetry() {
        // 获取唤醒锁以确保播放完成
        wakeLock?.let {
            if (!it.isHeld) {
                it.acquire(10000) // 最多持有10秒
            }
        }

        // 请求音频焦点
        if (requestAudioFocus()) {
            // 延迟一点确保获取到焦点
            handler.postDelayed({
                playReminderSound()
            }, 100)
        } else {
            // 直接播放
            playReminderSound()
        }
    }

    /**
     * 请求音频焦点
     */
    private fun requestAudioFocus(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setOnAudioFocusChangeListener { focusChange ->
                        when (focusChange) {
                            AudioManager.AUDIOFOCUS_LOSS -> {
                                Log.d(TAG, "音频焦点丢失")
                                stopPlaying()
                            }
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                                Log.d(TAG, "音频焦点暂时丢失")
                                pausePlaying()
                            }
                            AudioManager.AUDIOFOCUS_GAIN -> {
                                Log.d(TAG, "音频焦点获取")
                                resumePlaying()
                            }
                        }
                    }
                    .build()

                audioFocusRequest = focusRequest
                val result = audioManager?.requestAudioFocus(focusRequest)
                result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager?.requestAudioFocus(
                    { focusChange ->
                        when (focusChange) {
                            AudioManager.AUDIOFOCUS_LOSS -> stopPlaying()
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pausePlaying()
                            AudioManager.AUDIOFOCUS_GAIN -> resumePlaying()
                        }
                    },
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
                result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            Log.e(TAG, "请求音频焦点失败: ${e.message}")
            false
        }
    }

    /**
     * 放弃音频焦点
     */
    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager?.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "放弃音频焦点失败: ${e.message}")
        }
    }

    private fun stopPlaying() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun pausePlaying() {
        mediaPlayer?.pause()
    }

    private fun resumePlaying() {
        mediaPlayer?.start()
    }

    /**
     * 播放提醒声音
     */
    private fun playReminderSound() {
        try {
            val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
            val soundType = prefs.getString(MainActivity.PREF_SOUND, MainActivity.SOUND_CUCKOO)
                ?: MainActivity.SOUND_CUCKOO

            // 确保释放之前的 MediaPlayer
            mediaPlayer?.release()
            mediaPlayer = null

            if (soundType == MainActivity.SOUND_CUCKOO) {
                // 播放布谷鸟叫声
                mediaPlayer = MediaPlayer.create(this, R.raw.cuckoo_sound)
                if (mediaPlayer != null) {
                    // 设置音频属性
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mediaPlayer?.setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    }

                    // 设置音量（最大音量）
                    mediaPlayer?.setVolume(1.0f, 1.0f)

                    mediaPlayer?.setOnCompletionListener {
                        Log.d(TAG, "布谷鸟叫声播放完成")
                        // 播放完成后释放资源
                        mediaPlayer?.release()
                        mediaPlayer = null
                        // 释放音频焦点
                        abandonAudioFocus()
                        // 释放唤醒锁
                        wakeLock?.let { wakeLock ->
                            if (wakeLock.isHeld) {
                                wakeLock.release()
                            }
                        }
                    }

                    mediaPlayer?.setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer 错误: what=$what, extra=$extra")
                        // 错误时重试一次
                        handler.postDelayed({
                            retryPlaySound()
                        }, 500)
                        true
                    }

                    mediaPlayer?.start()
                    Log.d(TAG, "播放布谷鸟叫声")
                } else {
                    Log.w(TAG, "未找到布谷鸟叫声音频文件，尝试重试")
                    // 创建失败时重试
                    handler.postDelayed({
                        retryPlaySound()
                    }, 500)
                }
            } else {
                // 播放系统默认通知声音
                val notification = android.media.RingtoneManager.getDefaultUri(
                    android.media.RingtoneManager.TYPE_NOTIFICATION
                )
                val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, notification)
                ringtone?.play()
                Log.d(TAG, "播放系统默认通知声音")

                // 释放音频焦点和唤醒锁
                handler.postDelayed({
                    abandonAudioFocus()
                    wakeLock?.let { wakeLock ->
                        if (wakeLock.isHeld) {
                            wakeLock.release()
                        }
                    }
                }, 2000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放提醒声音失败: ${e.message}")
            // 异常时重试
            handler.postDelayed({
                retryPlaySound()
            }, 1000)
        }
    }

    /**
     * 重试播放声音
     */
    private fun retryPlaySound() {
        if (!isServiceRunning) return

        Log.d(TAG, "重试播放声音")
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.cuckoo_sound)
            if (mediaPlayer != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mediaPlayer?.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                mediaPlayer?.setVolume(1.0f, 1.0f)
                mediaPlayer?.setOnCompletionListener {
                    mediaPlayer?.release()
                    mediaPlayer = null
                    abandonAudioFocus()
                    wakeLock?.let { wakeLock ->
                        if (wakeLock.isHeld) {
                            wakeLock.release()
                        }
                    }
                }
                mediaPlayer?.start()
                Log.d(TAG, "重试播放成功")
            } else {
                Log.e(TAG, "重试播放失败")
                // 最终失败，释放资源
                abandonAudioFocus()
                wakeLock?.let { wakeLock ->
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "重试播放异常: ${e.message}")
            abandonAudioFocus()
            wakeLock?.let { wakeLock ->
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
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
