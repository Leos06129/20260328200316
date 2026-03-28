package com.example.randomscreensaver

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.Random

object Utils {
    
    private const val TAG = "Utils"
    
    // 检查悬浮窗权限
    fun checkOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    // 生成随机颜色
    fun getRandomColor(): Int {
        val random = Random()
        return android.graphics.Color.rgb(
            random.nextInt(256),
            random.nextInt(256),
            random.nextInt(256)
        )
    }
    
    // 生成随机字体大小 (20-100sp)
    fun getRandomTextSize(): Float {
        return Random().nextInt(81) + 20f // 20-100
    }
    
    // 生成随机位置 (0-100%)
    fun getRandomPosition(max: Int): Float {
        return Random().nextFloat() * max
    }
    
    // 获取锁屏状态下的间隔时间 (10-12秒)
    fun getLockedScreenInterval(): Long {
        return (Random().nextInt(3) + 10) * 1000L // 10-12秒
    }
    
    // 获取常规状态下的间隔时间
    fun getNormalScreenInterval(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val min = prefs.getInt(PREF_MIN_INTERVAL, DEFAULT_MIN_INTERVAL)
        val max = prefs.getInt(PREF_MAX_INTERVAL, DEFAULT_MAX_INTERVAL)
        
        return Random().nextInt(max - min + 1) + min * 1000L
    }
    
    // 获取当前显示的消息
    fun getDisplayMessage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_MESSAGE, context.getString(R.string.default_message))
            ?: context.getString(R.string.default_message)
    }
    
    // 检查服务是否正在运行
    fun isServiceRunning(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("service_running", false)
    }
    
    // 设置服务运行状态
    fun setServiceRunning(context: Context, isRunning: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("service_running", isRunning).apply()
    }
    
    // 播放提示音（可选功能）
    fun playNotificationSound(context: Context) {
        try {
            val mediaPlayer = MediaPlayer.create(context, Settings.System.DEFAULT_NOTIFICATION_URI)
            mediaPlayer?.apply {
                setOnCompletionListener { release() }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放提示音失败", e)
        }
    }
    
    // 检查是否应该显示（根据设置和当前状态）
    fun shouldDisplayNow(context: Context, isScreenLocked: Boolean): Boolean {
        // 检查服务是否运行
        if (!isServiceRunning(context)) {
            return false
        }
        
        // 这里可以添加更多逻辑，比如：
        // - 检查免打扰模式
        // - 检查特定时间段
        // - 检查电量状态等
        
        return true
    }
    
    // 获取下一次显示的时间
    fun getNextDisplayTime(context: Context, isScreenLocked: Boolean): Long {
        return if (isScreenLocked) {
            getLockedScreenInterval()
        } else {
            getNormalScreenInterval(context)
        }
    }
    
    // 记录日志
    fun logEvent(context: Context, event: String) {
        Log.d(TAG, event)
        // 这里可以添加日志记录到文件的功能
    }
}