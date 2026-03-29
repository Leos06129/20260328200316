package com.example.randomscreensaver

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import kotlin.random.Random

class FullscreenActivity : AppCompatActivity() {
    
    private lateinit var textView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var isLockedScreen = false
    private var currentMessage: String = ""
    private var maxInterval: Int = MainActivity.DEFAULT_MAX_INTERVAL
    private var minInterval: Int = MainActivity.DEFAULT_MIN_INTERVAL
    private var displayDuration: Long = 10000L
    
    companion object {
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_IS_LOCKED = "is_locked"
        const val EXTRA_MAX_INTERVAL = "max_interval"
        const val EXTRA_MIN_INTERVAL = "min_interval"
        const val EXTRA_DISPLAY_DURATION = "display_duration"
        const val AUTHENTICATION_REQUEST_CODE = 1002

        fun start(activity: Activity, message: String, isLocked: Boolean, maxInterval: Int, minInterval: Int, displayDuration: Long) {
            val intent = Intent(activity, FullscreenActivity::class.java).apply {
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_IS_LOCKED, isLocked)
                putExtra(EXTRA_MAX_INTERVAL, maxInterval)
                putExtra(EXTRA_MIN_INTERVAL, minInterval)
                putExtra(EXTRA_DISPLAY_DURATION, displayDuration)
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
        
        // 设置锁定屏幕
        lockScreen()
        
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
        
        // 设置触摸事件监听器
        textView.setOnClickListener {
            requestAuthentication()
        }
        
        // 获取参数
        currentMessage = intent.getStringExtra(EXTRA_MESSAGE) ?: getString(R.string.default_message)
        isLockedScreen = intent.getBooleanExtra(EXTRA_IS_LOCKED, false)
        maxInterval = intent.getIntExtra(EXTRA_MAX_INTERVAL, MainActivity.DEFAULT_MAX_INTERVAL)
        minInterval = intent.getIntExtra(EXTRA_MIN_INTERVAL, MainActivity.DEFAULT_MIN_INTERVAL)
        displayDuration = intent.getLongExtra(EXTRA_DISPLAY_DURATION, 10000L)
        
        // 显示随机文字（循环）
        showRandomMessage(currentMessage)
    }
    
    private fun lockScreen() {
        // 锁定屏幕，防止用户操作
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        
        // 对于Android 8.0+，使用setShowWhenLocked
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }
    
    private fun showRandomMessage(message: String) {
        // 生成随机颜色
        val randomColor = Color.rgb(
            Random.nextInt(256),
            Random.nextInt(256),
            Random.nextInt(256)
        )
        
        // 生成随机大小 (16-40sp，减小字体大小范围以避免显示不全)
        val randomSize = Random.nextInt(16, 41).toFloat()
        
        // 获取屏幕尺寸
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // 边距
        val padding = 40f * displayMetrics.density
        val maxTextWidth = screenWidth - padding * 2
        
        // 先设置TextView的基本属性
        textView.apply {
            text = message
            setTextColor(randomColor)
            textSize = randomSize
            maxWidth = maxTextWidth.toInt() // 限制最大宽度
            setPadding(padding.toInt(), padding.toInt(), padding.toInt(), padding.toInt())
            visibility = View.VISIBLE
            alpha = 0f
        }
        
        // 强制测量实际文字尺寸
        textView.measure(
            View.MeasureSpec.makeMeasureSpec(maxTextWidth.toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        
        // 调用 layout 使测量尺寸生效
        textView.layout(0, 0, textView.measuredWidth, textView.measuredHeight)
        
        // 获取测量后的实际尺寸
        val textWidth = textView.measuredWidth.toFloat()
        val textHeight = textView.measuredHeight.toFloat()
        
        Log.d("FullscreenActivity", "文字尺寸: ${textWidth}x${textHeight}, 屏幕: ${screenWidth}x${screenHeight}")
        
        // 确保文字完全显示在屏幕内
        val availableWidth = (screenWidth - textWidth - padding * 2).coerceAtLeast(0f)
        val availableHeight = (screenHeight - textHeight - padding * 2).coerceAtLeast(0f)
        
        // 生成随机位置
        val randomX = if (availableWidth > 0) padding + Random.nextFloat() * availableWidth else padding
        val randomY = if (availableHeight > 0) padding + Random.nextFloat() * availableHeight else padding
        
        // 设置位置
        textView.x = randomX
        textView.y = randomY
        
        // 淡入动画
        textView.animate()
            .alpha(1f)
            .setDuration(500)
            .start()
        
        // 显示持续时间后淡出
        handler.postDelayed({
            textView.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    // 文字淡出后，保持黑屏，等待随机间隔后显示新内容
                    waitAndShowNextMessage()
                }
                .start()
        }, displayDuration - 500)
    }
    
    private fun waitAndShowNextMessage() {
        // 生成随机间隔 (minInterval ~ maxInterval，单位毫秒)
        val nextInterval = Random.nextLong(
            minInterval.toLong() * 1000,
            maxInterval.toLong() * 1000
        )
        
        Log.d("FullscreenActivity", "等待下一条消息: ${nextInterval}ms")
        
        // 等待随机间隔后显示新消息
        handler.postDelayed({
            // 重新生成随机消息（使用相同的消息池）
            showRandomMessage(currentMessage)
        }, nextInterval)
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
    
    private var currentBiometricPrompt: BiometricPrompt? = null
    private var currentDeviceCredentialIntent: Intent? = null
    
    private fun requestAuthentication() {
        // 取消可能存在的之前的消息显示
        handler.removeCallbacksAndMessages(null)
        
        // 检查生物识别认证是否可用
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // 生物识别认证可用，显示认证对话框
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("解锁屏保")
                    .setSubtitle("需要输入密码或使用指纹解锁")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
                
                val executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = BiometricPrompt(this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            Log.d("FullscreenActivity", "生物识别认证成功")
                            // 认证成功，解锁屏幕并关闭Activity
                            unlockScreenAndFinish()
                        }
                        
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Log.d("FullscreenActivity", "生物识别认证错误: $errorCode, $errString")
                            // 用户取消时直接退出，不要重新显示
                            if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || 
                                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                errorCode == BiometricPrompt.ERROR_CANCELED) {
                                finish()
                            } else {
                                // 其他错误，5秒后自动回到屏保循环
                                handler.postDelayed({
                                    restartDisplayCycle()
                                }, 5000)
                            }
                        }
                        
                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Log.d("FullscreenActivity", "生物识别认证失败")
                            // 认证失败，不取消，等待用户重试或超时
                        }
                    })
                
                currentBiometricPrompt = biometricPrompt
                biometricPrompt.authenticate(promptInfo)
                
                // 5秒超时，取消认证并回到屏保循环
                handler.postDelayed({
                    if (biometricPrompt.isStarted) {
                        biometricPrompt.cancelAuthentication()
                    }
                    restartDisplayCycle()
                }, 5000)
            }
            else -> {
                // 生物识别不可用，使用传统的Keyguard解锁
                requestKeyguardWithTimeout()
            }
        }
    }
    
    private fun requestKeyguardWithTimeout() {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "解锁屏保", "需要解锁才能使用设备")
            if (intent != null) {
                currentDeviceCredentialIntent = intent
                startActivityForResult(intent, AUTHENTICATION_REQUEST_CODE)
                
                // 5秒超时
                handler.postDelayed({
                    // 结束当前Activity，重新启动以回到屏保循环
                    finish()
                    // 这里不会重新启动，因为Activity销毁后ScreenService会继续运行
                }, 5000)
            } else {
                // 无法创建Keyguard，直接解锁
                unlockScreenAndFinish()
            }
        } else {
            // 旧版本Android，直接解锁
            unlockScreenAndFinish()
        }
    }
    
    private fun restartDisplayCycle() {
        // 重新开始显示循环
        showRandomMessage(currentMessage)
    }
    
    private fun unlockScreenAndFinish() {
        Log.d("FullscreenActivity", "unlockScreenAndFinish 被调用")
        
        try {
            // 清除锁定标志
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
            
            // 停止服务
            val serviceIntent = Intent(this, ScreenService::class.java).apply {
                action = ScreenService.ACTION_STOP
            }
            stopService(serviceIntent)
            
            // 强制关闭Activity
            finish()
            Log.d("FullscreenActivity", "Activity 已关闭")
        } catch (e: Exception) {
            Log.e("FullscreenActivity", "关闭Activity失败: ${e.message}")
        }
    }
    
    private fun unlockWithKeyguard() {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "解锁屏保", "需要解锁才能使用设备")
            if (intent != null) {
                startActivityForResult(intent, AUTHENTICATION_REQUEST_CODE)
            } else {
                // 无法创建Keyguard，直接解锁
                unlockScreenAndFinish()
            }
        } else {
            // 旧版本Android，直接解锁
            unlockScreenAndFinish()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTHENTICATION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Keyguard认证成功
                unlockScreenAndFinish()
            }
            // 认证失败则保持锁定状态
        }
    }
    
}