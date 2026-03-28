package com.example.randomscreensaver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationUtils {
    
    private const val CHANNEL_ID = "screen_saver_channel"
    private const val CHANNEL_NAME = "屏保通知"
    private const val NOTIFICATION_ID = 1001
    
    // 创建通知渠道（Android 8.0+）
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "屏保服务运行状态通知"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    // 创建前台服务通知
    fun createForegroundNotification(context: Context): Notification {
        // 创建点击通知的Intent
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 创建停止服务的Intent
        val stopIntent = Intent(context, ScreenService::class.java).apply {
            action = ScreenService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("随机屏保")
            .setContentText("屏保服务正在运行中")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, android.R.drawable.ic_dialog_info))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "停止服务",
                stopPendingIntent
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("屏保服务正在后台运行，随机显示您设置的语句。\n点击通知可返回应用主界面。")
                    .setBigContentTitle("随机屏保服务")
            )
            .build()
    }
    
    // 显示通知
    fun showNotification(context: Context) {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            val notification = createForegroundNotification(context)
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
    
    // 取消通知
    fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
    
    // 检查通知权限
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}