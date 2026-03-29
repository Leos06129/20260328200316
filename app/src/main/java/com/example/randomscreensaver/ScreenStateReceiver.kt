package com.example.randomscreensaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val isServiceRunning = prefs.getBoolean("service_running", false)

        if (isServiceRunning) {
            val message = prefs.getString(
                MainActivity.PREF_MESSAGE,
                context.getString(R.string.default_message)
            ) ?: context.getString(R.string.default_message)
            val maxInterval = prefs.getInt(MainActivity.PREF_MAX_INTERVAL, MainActivity.DEFAULT_MAX_INTERVAL)
            val minInterval = prefs.getInt(MainActivity.PREF_MIN_INTERVAL, MainActivity.DEFAULT_MIN_INTERVAL)

            android.os.Handler(context.mainLooper).postDelayed({
                val fsIntent = Intent(context, FullscreenActivity::class.java).apply {
                    putExtra(FullscreenActivity.EXTRA_MESSAGE, message)
                    putExtra(FullscreenActivity.EXTRA_IS_LOCKED, true)
                    putExtra(FullscreenActivity.EXTRA_MAX_INTERVAL, maxInterval)
                    putExtra(FullscreenActivity.EXTRA_MIN_INTERVAL, minInterval)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fsIntent)
            }, 1000)
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
