package com.example.randomscreensaver

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {
    
    // 检查悬浮窗权限
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    // 请求悬浮窗权限
    fun requestOverlayPermission(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, requestCode)
        }
    }
    
    // 检查忽略电池优化权限
    fun hasIgnoreBatteryOptimizationsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }
    
    // 请求忽略电池优化权限
    fun requestIgnoreBatteryOptimizationsPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        }
    }
    
    // 检查通知权限（Android 13+）
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    // 请求通知权限
    fun requestNotificationPermission(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                requestCode
            )
        }
    }
    
    // 检查所有必要权限
    fun checkAllPermissions(context: Context): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        if (!hasOverlayPermission(context)) {
            missingPermissions.add("悬浮窗权限")
        }
        
        if (!hasIgnoreBatteryOptimizationsPermission(context)) {
            missingPermissions.add("电池优化白名单")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
            !hasNotificationPermission(context)) {
            missingPermissions.add("通知权限")
        }
        
        return missingPermissions
    }
}