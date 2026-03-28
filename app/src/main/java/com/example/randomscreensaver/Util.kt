package com.example.randomscreensaver

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.Random

object Util {
    
    private const val TAG = "ScreenSaverUtil"
    private var mediaPlayer: MediaPlayer? = null
    
    // 生成随机颜色
    fun randomColor(): Int {
        val random = Random()
        return android.graphics.Color.rgb(
            random.nextInt(256),
            random.nextInt(256),
            random.nextInt(256)
        )
    }
    
    // 生成随机大小 (20-100sp)
    fun randomTextSize(): Float {
        return (Random().nextInt(81) + 20).toFloat() // 20-100
    }
    
    // 生成随机位置 (0-1)
    fun randomPosition(): Float {
        return Random().nextFloat()
    }
    
    // 检查悬浮窗权限
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    // 播放语音提醒
    fun playVoiceReminder(context: Context, text: String = "任务已完成") {
        try {
            // 停止之前的播放
            stopVoiceReminder()
            
            // 创建新的MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                // 这里应该使用TTS或者预录制的音频文件
                // 由于没有音频文件，我们使用系统提示音
                setDataSource("")
                
                setOnCompletionListener {
                    releaseMediaPlayer()
                }
                
                setOnErrorListener { _, _, _ ->
                    releaseMediaPlayer()
                    false
                }
                
                prepare()
                start()
            }
            
            Log.d(TAG, "播放语音提醒: $text")
            
        } catch (e: Exception) {
            Log.e(TAG, "播放语音提醒失败", e)
            releaseMediaPlayer()
        }
    }
    
    // 停止语音提醒
    fun stopVoiceReminder() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }
    
    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
    
    // 获取设置
    fun getSettings(context: Context): Triple<String, Int, Int> {
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val message = prefs.getString(
            MainActivity.PREF_MESSAGE, 
            context.getString(R.string.default_message)
        ) ?: context.getString(R.string.default_message)
        val minInterval = prefs.getInt(MainActivity.PREF_MIN_INTERVAL, MainActivity.DEFAULT_MIN_INTERVAL)
        val maxInterval = prefs.getInt(MainActivity.PREF_MAX_INTERVAL, MainActivity.DEFAULT_MAX_INTERVAL)
        
        return Triple(message, minInterval, maxInterval)
    }
    
    // 保存设置
    fun saveSettings(context: Context, message: String, minInterval: Int, maxInterval: Int) {
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(MainActivity.PREF_MESSAGE, message)
        editor.putInt(MainActivity.PREF_MIN_INTERVAL, minInterval)
        editor.putInt(MainActivity.PREF_MAX_INTERVAL, maxInterval)
        editor.apply()
    }
    
    // 检查服务是否运行
    fun isServiceRunning(context: Context): Boolean {
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("service_running", false)
    }
    
    // 设置服务运行状态
    fun setServiceRunning(context: Context, isRunning: Boolean) {
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("service_running", isRunning)
        editor.apply()
    }
}