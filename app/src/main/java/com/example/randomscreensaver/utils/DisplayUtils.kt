package com.example.randomscreensaver.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.randomscreensaver.FullscreenActivity
import com.example.randomscreensaver.MainActivity
import kotlin.random.Random

object DisplayUtils {
    
    fun showRandomMessage(context: Context, isScreenLocked: Boolean) {
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)

        // 获取设置
        val message = prefs.getString(
            MainActivity.PREF_MESSAGE,
            "今天也要加油哦！"
        ) ?: "今天也要加油哦！"

        val message2 = prefs.getString(MainActivity.PREF_MESSAGE2, null)

        val minInterval = prefs.getInt(
            MainActivity.PREF_MIN_INTERVAL,
            MainActivity.DEFAULT_MIN_INTERVAL
        )

        val maxInterval = prefs.getInt(
            MainActivity.PREF_MAX_INTERVAL,
            MainActivity.DEFAULT_MAX_INTERVAL
        )

        // 启动全屏显示
        val intent = Intent(context, FullscreenActivity::class.java).apply {
            putExtra(FullscreenActivity.EXTRA_MESSAGE, message)
            putExtra(FullscreenActivity.EXTRA_MESSAGE2, message2)
            putExtra(FullscreenActivity.EXTRA_IS_LOCKED, isScreenLocked)
            putExtra(FullscreenActivity.EXTRA_MAX_INTERVAL, maxInterval)
            putExtra(FullscreenActivity.EXTRA_MIN_INTERVAL, minInterval)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    fun getRandomColor(): Int {
        return android.graphics.Color.rgb(
            Random.nextInt(256),
            Random.nextInt(256),
            Random.nextInt(256)
        )
    }
    
    fun getRandomSize(min: Float = 20f, max: Float = 100f): Float {
        return Random.nextFloat() * (max - min) + min
    }
    
    fun getRandomPosition(maxX: Float, maxY: Float): Pair<Float, Float> {
        return Pair(
            Random.nextFloat() * maxX,
            Random.nextFloat() * maxY
        )
    }
    
    fun getDisplayInterval(isScreenLocked: Boolean, minInterval: Int, maxInterval: Int): Int {
        return if (isScreenLocked) {
            // 锁屏时：10-12秒
            Random.nextInt(10, 13)
        } else {
            // 常规时：用户设置的范围
            Random.nextInt(minInterval, maxInterval + 1)
        }
    }
}