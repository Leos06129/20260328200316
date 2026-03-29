package com.example.randomscreensaver

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class FullscreenActivity : AppCompatActivity() {
    
    private lateinit var textView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var isLockedScreen = false
    
    companion object {
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_IS_LOCKED = "is_locked"
        const val EXTRA_MAX_INTERVAL = "max_interval"
        const val EXTRA_MIN_INTERVAL = "min_interval"

        fun start(activity: Activity, message: String, isLocked: Boolean, maxInterval: Int, minInterval: Int) {
            val intent = Intent(activity, FullscreenActivity::class.java).apply {
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_IS_LOCKED, isLocked)
                putExtra(EXTRA_MAX_INTERVAL, maxInterval)
                putExtra(EXTRA_MIN_INTERVAL, minInterval)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置全屏显示
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // 隐藏状态栏和导航栏
        hideSystemUI()
        
        // 设置黑色背景
        window.decorView.setBackgroundColor(Color.BLACK)
        
        // 创建TextView显示文字
        textView = TextView(this).apply {
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 24f
        }
        
        setContentView(textView)
        
        // 获取参数
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: getString(R.string.default_message)
        isLockedScreen = intent.getBooleanExtra(EXTRA_IS_LOCKED, false)
        val maxInterval = intent.getIntExtra(EXTRA_MAX_INTERVAL, MainActivity.DEFAULT_MAX_INTERVAL)
        val minInterval = intent.getIntExtra(EXTRA_MIN_INTERVAL, MainActivity.DEFAULT_MIN_INTERVAL)
        
        // 显示随机文字
        showRandomMessage(message, maxInterval, minInterval)
    }
    
    private fun showRandomMessage(message: String, maxInterval: Int, minInterval: Int) {
        // 生成随机颜色
        val randomColor = Color.rgb(
            Random.nextInt(256),
            Random.nextInt(256),
            Random.nextInt(256)
        )
        
        // 生成随机大小 (20-100sp)
        val randomSize = Random.nextInt(20, 101).toFloat()
        
        // 生成随机位置
        val randomX = Random.nextInt(-500, 501).toFloat()
        val randomY = Random.nextInt(-500, 501).toFloat()
        
        // 设置TextView属性
        textView.apply {
            text = message
            setTextColor(randomColor)
            textSize = randomSize
            translationX = randomX
            translationY = randomY
            alpha = 0f
        }
        
        // 淡入动画
        textView.animate()
            .alpha(1f)
            .setDuration(500)
            .start()
        
        // 计算下次显示间隔
        val interval = if (isLockedScreen) {
            // 锁屏时：10-12秒
            Random.nextInt(10, 13) * 1000L
        } else {
            // 常规时：minInterval-maxInterval秒
            Random.nextInt(minInterval, maxInterval + 1) * 1000L
        }
        
        // 淡出并关闭Activity
        handler.postDelayed({
            textView.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    finish()
                }
                .start()
        }, interval - 500) // 提前500ms开始淡出
    }
    
    private fun hideSystemUI() {
        // 全屏沉浸模式
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
    
}