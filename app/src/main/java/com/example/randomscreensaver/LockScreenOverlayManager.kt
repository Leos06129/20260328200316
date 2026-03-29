package com.example.randomscreensaver

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.random.Random

/**
 * 锁屏覆盖层管理器
 * 在锁屏界面上显示全屏文字覆盖层
 */
class LockScreenOverlayManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    companion object {
        private const val TAG = "LockScreenOverlayMgr"

        /**
         * 显示锁屏覆盖层
         * @param context 上下文
         * @param message1 第一条文字
         * @param message2 第二条文字（可选）
         * @param displayDuration 显示持续时间（毫秒）
         * @param onDismiss 点击关闭时的回调
         */
        fun show(
            context: Context,
            message1: String,
            message2: String?,
            displayDuration: Long = 5000L,
            onDismiss: (() -> Unit)? = null
        ) {
            val manager = LockScreenOverlayManager(context)
            manager.display(message1, message2, displayDuration, onDismiss)
        }

        /**
         * 隐藏所有覆盖层
         */
        fun hideAll(context: Context) {
            try {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                // 尝试移除可能的残留覆盖层（这里简化处理）
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 显示覆盖层
     */
    private fun display(
        message1: String,
        message2: String?,
        displayDuration: Long,
        onDismiss: (() -> Unit)?
    ) {
        // 选择显示的文字（随机或唯一）
        val message = when {
            message2.isNullOrEmpty() -> message1
            message1.isEmpty() -> message2
            else -> {
                // 随机选择一条显示
                if (Random.nextBoolean()) message1 else message2
            }
        }

        try {
            // 获取 WindowManager
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // 创建覆盖层布局参数（可点击）
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                getWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )

            // 设置布局参数
            layoutParams.gravity = Gravity.CENTER
            layoutParams.screenOrientation = Activity.SCREEN_ORIENTATION_PORTRAIT

            // 创建覆盖层视图
            overlayView = createOverlayView(message)

            // 添加到窗口
            windowManager?.addView(overlayView, layoutParams)

            // 设置定时自动关闭
            dismissRunnable = Runnable {
                dismiss()
                onDismiss?.invoke()
            }
            handler.postDelayed(dismissRunnable!!, displayDuration)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 创建覆盖层视图
     */
    private fun createOverlayView(message: String): View {
        // 创建 FrameLayout 作为根容器
        val rootLayout = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            // 设置可点击以关闭
            setOnClickListener {
                dismiss()
            }
        }

        // 创建文字显示 TextView
        val textView = TextView(context).apply {
            text = message
            setTextColor(getRandomColor())
            textSize = Random.nextInt(24, 48).toFloat()
            gravity = Gravity.CENTER
            setPadding(60, 40, 60, 40)

            // 设置随机位置
            val randomX = Random.nextInt(-100, 101)
            val randomY = Random.nextInt(-200, 201)
            x = randomX.toFloat()
            y = randomY.toFloat()
        }

        rootLayout.addView(textView)

        // 添加提示文字
        val hintView = TextView(context).apply {
            text = "点击任意位置关闭"
            setTextColor(Color.parseColor("#888888"))
            textSize = 12f
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, 100)
        }
        rootLayout.addView(hintView)

        return rootLayout
    }

    /**
     * 获取随机颜色
     */
    private fun getRandomColor(): Int {
        return Color.rgb(
            Random.nextInt(128, 256),  // 较亮的颜色
            Random.nextInt(128, 256),
            Random.nextInt(128, 256)
        )
    }

    /**
     * 获取窗口类型
     * 根据 Android 版本选择合适的窗口类型
     */
    private fun getWindowType(): Int {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    /**
     * 关闭覆盖层
     */
    fun dismiss() {
        try {
            // 移除定时关闭任务
            dismissRunnable?.let {
                handler.removeCallbacks(it)
            }

            // 移除视图
            overlayView?.let {
                windowManager?.removeView(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            overlayView = null
            windowManager = null
        }
    }

    /**
     * 检查是否有 Overlay 权限
     */
    fun hasOverlayPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
}
