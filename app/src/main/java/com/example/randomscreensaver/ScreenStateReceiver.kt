package com.example.randomscreensaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlin.random.Random

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
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val isServiceRunning = prefs.getBoolean("service_running", false)

        if (isServiceRunning) {
            // 获取两条文字
            val message1 = prefs.getString(
                MainActivity.PREF_MESSAGE,
                context.getString(R.string.default_message)
            ) ?: context.getString(R.string.default_message)

            val message2 = prefs.getString(
                MainActivity.PREF_MESSAGE2,
                null
            )

            val maxInterval = prefs.getInt(MainActivity.PREF_MAX_INTERVAL, MainActivity.DEFAULT_MAX_INTERVAL)
            val minInterval = prefs.getInt(MainActivity.PREF_MIN_INTERVAL, MainActivity.DEFAULT_MIN_INTERVAL)

            // 计算显示持续时间（随机）
            val displayDuration = Random.nextInt(minInterval, maxInterval + 1) * 1000L

            Log.d(TAG, "显示覆盖层，间隔: ${displayDuration / 1000}秒")

            // 延迟显示覆盖层（避免立即显示）
            android.os.Handler(context.mainLooper).postDelayed({
                // 显示锁屏覆盖层
                LockScreenOverlayManager.show(
                    context = context,
                    message1 = message1,
                    message2 = message2,
                    displayDuration = displayDuration
                )
            }, 500) // 延迟0.5秒显示
        }
    }

    private fun handleScreenOn(context: Context) {
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("screen_on", true).apply()
    }

    private fun handleUserPresent(context: Context) {
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("screen_locked", false).apply()
    }
}
