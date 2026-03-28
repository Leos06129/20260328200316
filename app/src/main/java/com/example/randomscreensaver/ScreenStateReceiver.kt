package com.example.randomscreensaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log

class ScreenStateReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "ScreenStateReceiver"
        const val ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF"
        const val ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON"
        const val ACTION_USER_PRESENT = "android.intent.action.USER_PRESENT"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SCREEN_OFF -> {
                Log.d(TAG, "屏幕关闭/锁屏")
                handleScreenOff(context)
            }
            
            ACTION_SCREEN_ON -> {
                Log.d(TAG, "屏幕打开")
                handleScreenOn(context)
            }
            
            ACTION_USER_PRESENT -> {
                Log.d(TAG, "用户解锁")
                handleUserPresent(context)
            }
        }
    }
    
    private fun handleScreenOff(context: Context) {
        // 锁屏状态
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isServiceRunning = prefs.getBoolean("service_running", false)
        
        if (isServiceRunning) {
            // 在锁屏状态下启动全屏显示
            val message = prefs.getString(PREF_MESSAGE, context.getString(R.string.default_message)) ?: context.getString(R.string.default_message)
            val maxInterval = prefs.getInt(PREF_MAX_INTERVAL, DEFAULT_MAX_INTERVAL)
            val minInterval = prefs.getInt(PREF_MIN_INTERVAL, DEFAULT_MIN_INTERVAL)
            
            // 使用Handler延迟启动，避免立即显示
            android.os.Handler(context.mainLooper).postDelayed({
                FullscreenActivity.start(context as android.app.Activity, message, true, maxInterval, minInterval)
            }, 1000) // 延迟1秒显示
        }
    }
    
    private fun handleScreenOn(context: Context) {
        // 屏幕打开但可能还未解锁
        // 可以在这里记录状态
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("screen_on", true)
        editor.apply()
    }
    
    private fun handleUserPresent(context: Context) {
        // 用户已解锁，恢复常规显示模式
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("screen_locked", false)
        editor.apply()
        
        // 停止当前的锁屏显示
        // 实际应用中可能需要更复杂的逻辑来管理多个Activity实例
    }
}