package com.example.randomscreensaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "设备启动完成")
            
            // 检查是否需要在开机时自动启动服务
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean("auto_start", false)
            
            if (autoStart) {
                Log.d(TAG, "自动启动屏保服务")
                // 延迟启动服务，等待系统完全启动
                android.os.Handler(context.mainLooper).postDelayed({
                    startScreenService(context)
                }, 10000) // 延迟10秒启动
            }
        }
    }
    
    private fun startScreenService(context: Context) {
        val serviceIntent = Intent(context, ScreenService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        
        Log.d(TAG, "屏保服务已启动")
    }
}