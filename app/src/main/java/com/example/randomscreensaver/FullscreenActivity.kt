package com.example.randomscreensaver

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
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
    
    companion object {
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_IS_LOCKED = "is_locked"
        const val EXTRA_MAX_INTERVAL = "max_interval"
        const val EXTRA_MIN_INTERVAL = "min_interval"
        const val EXTRA_DISPLAY_DURATION = "display_duration"

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
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: getString(R.string.default_message)
        isLockedScreen = intent.getBooleanExtra(EXTRA_IS_LOCKED, false)
        val maxInterval = intent.getIntExtra(EXTRA_MAX_INTERVAL, MainActivity.DEFAULT_MAX_INTERVAL)
        val minInterval = intent.getIntExtra(EXTRA_MIN_INTERVAL, MainActivity.DEFAULT_MIN_INTERVAL)
        val displayDuration = intent.getLongExtra(EXTRA_DISPLAY_DURATION, 10000L)
        
        // 显示随机文字
        showRandomMessage(message, maxInterval, minInterval, displayDuration)
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
    
    private fun showRandomMessage(message: String, maxInterval: Int, minInterval: Int, displayDuration: Long) {
        // 生成随机颜色
        val randomColor = Color.rgb(
            Random.nextInt(256),
            Random.nextInt(256),
            Random.nextInt(256)
        )
        
        // 生成随机大小 (20-100sp)
        val randomSize = Random.nextInt(20, 101).toFloat()
        
        // 获取屏幕尺寸
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // 计算文本宽度（估算）
        val textPaint = textView.paint
        textPaint.textSize = randomSize * displayMetrics.scaledDensity
        val textWidth = textPaint.measureText(message)
        
        // 确保文字完全显示在屏幕内
        val maxX = screenWidth - textWidth * 1.2f // 留20%边距
        val maxY = screenHeight - randomSize * 2 * displayMetrics.scaledDensity // 留一定高度
        
        // 生成随机位置，确保在屏幕内
        val randomX = if (maxX > 0) Random.nextInt(0, maxX.toInt()).toFloat() else 0f
        val randomY = if (maxY > 0) Random.nextInt(0, maxY.toInt()).toFloat() else 0f
        
        // 设置TextView属性
        textView.apply {
            text = message
            setTextColor(randomColor)
            textSize = randomSize
            x = randomX
            y = randomY
            alpha = 0f
        }
        
        // 淡入动画
        textView.animate()
            .alpha(1f)
            .setDuration(500)
            .start()
        
        // 使用传入的显示持续时间
        val interval = displayDuration
        
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
    
    private fun requestAuthentication() {
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
                            // 认证成功，解锁屏幕并关闭Activity
                            unlockScreenAndFinish()
                        }
                        
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            // 认证错误，保持锁定状态
                        }
                        
                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            // 认证失败，保持锁定状态
                        }
                    })
                
                biometricPrompt.authenticate(promptInfo)
            }
            else -> {
                // 生物识别不可用，使用传统的Keyguard解锁
                unlockWithKeyguard()
            }
        }
    }
    
    private fun unlockScreenAndFinish() {
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
        
        // 关闭Activity
        finish()
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
    
    companion object {
        const val AUTHENTICATION_REQUEST_CODE = 1002
    }
    
}