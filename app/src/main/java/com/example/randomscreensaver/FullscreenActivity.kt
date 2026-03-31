package com.example.randomscreensaver

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import kotlin.random.Random

class FullscreenActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout
    private val handler = Handler(Looper.getMainLooper())
    private val activeTextViews = mutableListOf<TextView>()
    private val usedRects = mutableListOf<Rect>()
    private var isLockedScreen = false
    private var currentMessage: String = ""
    private var currentMessage2: String? = null
    private var maxInterval: Int = MainActivity.DEFAULT_MAX_INTERVAL
    private var minInterval: Int = MainActivity.DEFAULT_MIN_INTERVAL
    private var displayDuration: Long = 10000L
    private var screenWidth = 0
    private var screenHeight = 0
    private val padding = 60  // 边距像素

    // 动画类型枚举
    private enum class AnimationType {
        POP,           // 弹出
        ROTATE_IN,     // 旋转入场
        SLIDE_LEFT,    // 从左滑入
        SLIDE_RIGHT,   // 从右滑入
        SLIDE_TOP,     // 从上滑入
        SLIDE_BOTTOM,  // 从下滑入
        SCALE_BOUNCE,  // 弹性缩放
        FLIP,          // 翻转
        FADE_SCALE,    // 淡入缩放
        SHAKE,         // 震颤效果
        WOBBLE,        // 晃动效果
        FLASH,         // 闪光效果
        PULSE          // 脉冲效果
    }

    companion object {
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_MESSAGE2 = "message2"
        const val EXTRA_IS_LOCKED = "is_locked"
        const val EXTRA_MAX_INTERVAL = "max_interval"
        const val EXTRA_MIN_INTERVAL = "min_interval"
        const val EXTRA_DISPLAY_DURATION = "display_duration"
        const val AUTHENTICATION_REQUEST_CODE = 1002

        fun start(activity: Activity, message: String, message2: String?, isLocked: Boolean, maxInterval: Int, minInterval: Int, displayDuration: Long) {
            val intent = Intent(activity, FullscreenActivity::class.java).apply {
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_MESSAGE2, message2)
                putExtra(EXTRA_IS_LOCKED, isLocked)
                putExtra(EXTRA_MAX_INTERVAL, maxInterval)
                putExtra(EXTRA_MIN_INTERVAL, minInterval)
                putExtra(EXTRA_DISPLAY_DURATION, displayDuration)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        }
    }

    // 用于记录已使用区域的矩形
    data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        fun intersects(other: Rect): Boolean {
            return !(right < other.left || left > other.right ||
                    bottom < other.top || top > other.bottom)
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

        // 创建容器布局
        container = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        setContentView(container)

        // 获取屏幕尺寸
        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        // 设置触摸事件监听器
        container.setOnClickListener {
            requestAuthentication()
        }

        // 获取参数
        currentMessage = intent.getStringExtra(EXTRA_MESSAGE) ?: getString(R.string.default_message)
        currentMessage2 = intent.getStringExtra(EXTRA_MESSAGE2)
        isLockedScreen = intent.getBooleanExtra(EXTRA_IS_LOCKED, false)
        maxInterval = intent.getIntExtra(EXTRA_MAX_INTERVAL, MainActivity.DEFAULT_MAX_INTERVAL)
        minInterval = intent.getIntExtra(EXTRA_MIN_INTERVAL, MainActivity.DEFAULT_MIN_INTERVAL)
        displayDuration = intent.getLongExtra(EXTRA_DISPLAY_DURATION, 10000L)

        Log.d("FullscreenActivity", "消息1: $currentMessage, 消息2: $currentMessage2")

        // 开始显示动画文字
        startWordAnimation()
    }

    private fun lockScreen() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    private fun startWordAnimation() {
        // 清空之前的数据
        activeTextViews.clear()
        usedRects.clear()

        // 随机选择显示模式
        val hasSecondMessage = !currentMessage2.isNullOrEmpty()
        val displayMode = if (hasSecondMessage) Random.nextInt(0, 3) else 0

        // 根据显示模式确定显示内容
        val displayText = when (displayMode) {
            0 -> currentMessage
            1 -> currentMessage2 ?: currentMessage
            2 -> "$currentMessage $currentMessage2"
            else -> currentMessage
        }

        Log.d("FullscreenActivity", "显示模式: $displayMode, 内容: $displayText")

        // 将文字分割成显示单元（中文按两个字一组，英文按词）
        val words = splitTextIntoWords(displayText)
        Log.d("FullscreenActivity", "分割成 ${words.size} 个显示单元: $words")

        // 逐个显示词语
        showWordsSequentially(words, 0)

        // 设置显示持续时间后清除所有文字
        handler.postDelayed({
            clearAllWordsAndContinue()
        }, displayDuration)
    }

    private fun splitTextIntoWords(text: String): List<String> {
        val words = mutableListOf<String>()
        val chineseBuffer = StringBuilder()
        val otherBuffer = StringBuilder()

        fun flushChineseBuffer() {
            if (chineseBuffer.isNotEmpty()) {
                words.add(chineseBuffer.toString())
                chineseBuffer.clear()
            }
        }

        fun flushOtherBuffer() {
            if (otherBuffer.isNotEmpty()) {
                words.add(otherBuffer.toString())
                otherBuffer.clear()
            }
        }

        for (char in text) {
            when {
                isChineseCharacter(char) -> {
                    flushOtherBuffer()
                    chineseBuffer.append(char)
                    if (chineseBuffer.length == 2) {
                        flushChineseBuffer()
                    }
                }
                char.isWhitespace() -> {
                    flushChineseBuffer()
                    flushOtherBuffer()
                }
                else -> {
                    flushChineseBuffer()
                    otherBuffer.append(char)
                }
            }
        }

        flushChineseBuffer()
        flushOtherBuffer()

        return words
    }

    private fun isChineseCharacter(char: Char): Boolean {
        return char.code in 0x4E00..0x9FFF || char.code in 0x3400..0x4DBF
    }

    private fun showWordsSequentially(words: List<String>, index: Int) {
        if (index >= words.size) return

        val word = words[index]
        val textView = createAnimatedTextView(word)

        // 找到不重叠的位置
        val position = findNonOverlappingPosition(textView)
        if (position != null) {
            textView.x = position.first
            textView.y = position.second

            // 添加到容器和记录
            container.addView(textView)
            activeTextViews.add(textView)

            // 记录占用区域（加上一些间距）
            val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST)
            val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
            textView.measure(widthMeasureSpec, heightMeasureSpec)
            val width = textView.measuredWidth.toFloat()
            val height = textView.measuredHeight.toFloat()
            usedRects.add(Rect(
                position.first - 20,
                position.second - 20,
                position.first + width + 20,
                position.second + height + 20
            ))

            // 执行入场动画
            val animType = AnimationType.values().random()
            playEntranceAnimation(textView, animType)
        }

        // 随机间隔后显示下一个词（100-400ms）
        val nextDelay = Random.nextLong(100, 401)
        handler.postDelayed({
            showWordsSequentially(words, index + 1)
        }, nextDelay)
    }

    private fun createAnimatedTextView(text: String): TextView {
        // 生成随机颜色（确保在深色背景上可见）
        val randomColor = generateVisibleColor()

        // 生成随机大小 (20-60sp)
        val randomSize = Random.nextInt(20, 61).toFloat()

        return TextView(this).apply {
            this.text = text
            setTextColor(randomColor)
            textSize = randomSize
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16) // 增加内边距防止截断
            alpha = 0f  // 初始不可见
            
            // 设置布局参数，确保不会被父容器裁剪
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            clipToOutline = false
        }
    }

    private fun generateVisibleColor(): Int {
        // 生成在黑色背景上可见的明亮颜色
        val hue = Random.nextFloat() * 360
        val saturation = 0.7f + Random.nextFloat() * 0.3f  // 70-100% 饱和度
        val value = 0.8f + Random.nextFloat() * 0.2f       // 80-100% 明度

        return Color.HSVToColor(floatArrayOf(hue, saturation, value))
    }

    private fun findNonOverlappingPosition(textView: TextView): Pair<Float, Float>? {
        // 强制进行测量
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
        textView.measure(widthMeasureSpec, heightMeasureSpec)
        
        val textWidth = textView.measuredWidth.toFloat()
        val textHeight = textView.measuredHeight.toFloat()

        // 确保文字不会超出屏幕边界（增加安全边距）
        val safePadding = padding * 1.5f
        val availableWidth = screenWidth - safePadding * 2 - textWidth
        val availableHeight = screenHeight - safePadding * 2 - textHeight

        // 如果文字比屏幕还大，强制缩小
        if (availableWidth <= 0 || availableHeight <= 0) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textView.textSize * 0.7f)
            textView.measure(widthMeasureSpec, heightMeasureSpec)
            return Pair(safePadding, safePadding) // 放在左上角安全区域
        }

        // 尝试最多50次找到不重叠的位置
        repeat(50) {
            val x = safePadding + Random.nextFloat() * availableWidth
            val y = safePadding + Random.nextFloat() * availableHeight

            val newRect = Rect(x - 20, y - 20, x + textWidth + 20, y + textHeight + 20)

            // 检查是否与已占用的区域重叠
            val overlaps = usedRects.any { it.intersects(newRect) }

            if (!overlaps) {
                return Pair(x, y)
            }
        }

        // 如果50次都没找到，随机返回一个安全位置（可能会重叠，但不会超出屏幕）
        return Pair(
            safePadding + Random.nextFloat() * availableWidth,
            safePadding + Random.nextFloat() * availableHeight
        )
    }

    private fun playEntranceAnimation(textView: TextView, animType: AnimationType) {
        when (animType) {
            AnimationType.POP -> animatePop(textView)
            AnimationType.ROTATE_IN -> animateRotateIn(textView)
            AnimationType.SLIDE_LEFT -> animateSlideFromLeft(textView)
            AnimationType.SLIDE_RIGHT -> animateSlideFromRight(textView)
            AnimationType.SLIDE_TOP -> animateSlideFromTop(textView)
            AnimationType.SLIDE_BOTTOM -> animateSlideFromBottom(textView)
            AnimationType.SCALE_BOUNCE -> animateScaleBounce(textView)
            AnimationType.FLIP -> animateFlip(textView)
            AnimationType.FADE_SCALE -> animateFadeScale(textView)
            AnimationType.SHAKE -> animateShake(textView)
            AnimationType.WOBBLE -> animateWobble(textView)
            AnimationType.FLASH -> animateFlash(textView)
            AnimationType.PULSE -> animatePulse(textView)
        }
    }

    private fun animatePop(textView: TextView) {
        textView.scaleX = 0f
        textView.scaleY = 0f
        textView.alpha = 1f

        val animator = ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("scaleX", 0f, 1.2f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0f, 1.2f, 1f)
        )
        animator.duration = 400
        animator.interpolator = OvershootInterpolator(1.5f)
        animator.start()
    }

    private fun animateRotateIn(textView: TextView) {
        textView.rotation = -180f
        textView.scaleX = 0f
        textView.scaleY = 0f
        textView.alpha = 1f

        val animator = ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("rotation", -180f, 0f),
            PropertyValuesHolder.ofFloat("scaleX", 0f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0f, 1f)
        )
        animator.duration = 500
        animator.interpolator = AnticipateOvershootInterpolator()
        animator.start()
    }

    private fun animateSlideFromLeft(textView: TextView) {
        val originalX = textView.x
        textView.x = -200f
        textView.alpha = 1f

        textView.animate()
            .x(originalX)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateSlideFromRight(textView: TextView) {
        val originalX = textView.x
        textView.x = screenWidth + 200f
        textView.alpha = 1f

        textView.animate()
            .x(originalX)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateSlideFromTop(textView: TextView) {
        val originalY = textView.y
        textView.y = -200f
        textView.alpha = 1f

        textView.animate()
            .y(originalY)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateSlideFromBottom(textView: TextView) {
        val originalY = textView.y
        textView.y = screenHeight + 200f
        textView.alpha = 1f

        textView.animate()
            .y(originalY)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateScaleBounce(textView: TextView) {
        textView.scaleX = 0f
        textView.scaleY = 0f
        textView.alpha = 1f

        val animator = ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("scaleX", 0f, 1.1f, 0.9f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0f, 1.1f, 0.9f, 1f)
        )
        animator.duration = 600
        animator.interpolator = BounceInterpolator()
        animator.start()
    }

    private fun animateFlip(textView: TextView) {
        textView.rotationY = 90f
        textView.alpha = 1f

        val animator = ObjectAnimator.ofFloat(textView, "rotationY", 90f, 0f)
        animator.duration = 400
        animator.interpolator = DecelerateInterpolator()
        animator.start()
    }

    private fun animateFadeScale(textView: TextView) {
        textView.scaleX = 0.5f
        textView.scaleY = 0.5f
        textView.alpha = 0f

        textView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun animateShake(textView: TextView) {
        textView.alpha = 1f
        val animator = ObjectAnimator.ofFloat(textView, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        animator.duration = 500
        animator.start()
    }

    private fun animateWobble(textView: TextView) {
        textView.alpha = 1f
        val animator = ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 1.2f, 0.9f, 1.1f, 0.95f, 1.05f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 0.8f, 1.1f, 0.9f, 1.05f, 0.95f, 1f),
            PropertyValuesHolder.ofFloat("rotation", 0f, -10f, 10f, -5f, 5f, 0f)
        )
        animator.duration = 600
        animator.start()
    }

    private fun animateFlash(textView: TextView) {
        textView.alpha = 0f
        val animator = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f, 0f, 1f, 0f, 1f)
        animator.duration = 600
        animator.start()
    }

    private fun animatePulse(textView: TextView) {
        textView.alpha = 1f
        val animator = ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("scaleX", 0f, 1.5f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0f, 1.5f, 1f)
        )
        animator.duration = 500
        animator.interpolator = OvershootInterpolator()
        animator.start()
    }

    private fun clearAllWordsAndContinue() {
        // 随机选择出场动画
        val exitAnimType = Random.nextInt(4)

        if (activeTextViews.isEmpty()) {
            waitAndShowNextMessage()
            return
        }

        var completedCount = 0
        val totalCount = activeTextViews.size

        activeTextViews.forEachIndexed { index, textView ->
            handler.postDelayed({
                playExitAnimation(textView, exitAnimType) {
                    completedCount++
                    if (completedCount >= totalCount) {
                        container.removeAllViews()
                        activeTextViews.clear()
                        usedRects.clear()
                        waitAndShowNextMessage()
                    }
                }
            }, index * 50L)  // 依次出场，间隔50ms
        }
    }

    private fun playExitAnimation(textView: TextView, type: Int, onComplete: () -> Unit) {
        when (type) {
            0 -> { // 淡出缩小
                textView.animate()
                    .alpha(0f)
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(300)
                    .withEndAction { onComplete() }
                    .start()
            }
            1 -> { // 向上飞出
                textView.animate()
                    .y(-200f)
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction { onComplete() }
                    .start()
            }
            2 -> { // 向右飞出
                textView.animate()
                    .x(screenWidth + 200f)
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction { onComplete() }
                    .start()
            }
            3 -> { // 旋转消失
                val animator = ObjectAnimator.ofPropertyValuesHolder(
                    textView,
                    PropertyValuesHolder.ofFloat("rotation", 0f, 180f),
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 0f),
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 0f),
                    PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
                )
                animator.duration = 300
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onComplete()
                    }
                })
                animator.start()
            }
        }
    }

    private fun waitAndShowNextMessage() {
        // 生成随机间隔
        val nextInterval = Random.nextLong(
            minInterval.toLong() * 1000,
            maxInterval.toLong() * 1000
        )

        Log.d("FullscreenActivity", "等待下一条消息: ${nextInterval}ms")

        handler.postDelayed({
            startWordAnimation()
        }, nextInterval)
    }

    private fun hideSystemUI() {
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

    // ========== 解锁相关代码 ==========

    private var currentBiometricPrompt: BiometricPrompt? = null
    private var currentDeviceCredentialIntent: Intent? = null
    private var isUnlocking = false
    private var timeoutRunnable: Runnable? = null

    private fun requestAuthentication() {
        if (isUnlocking) {
            Log.d("FullscreenActivity", "正在解锁中，跳过")
            return
        }

        handler.removeCallbacksAndMessages(null)

        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
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
                            unlockScreenAndFinish()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Log.d("FullscreenActivity", "生物识别认证错误: $errorCode, $errString")
                            timeoutRunnable?.let { handler.removeCallbacks(it) }

                            if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                errorCode == BiometricPrompt.ERROR_CANCELED) {
                                finish()
                            } else {
                                isUnlocking = false
                                handler.postDelayed({
                                    restartDisplayCycle()
                                }, 5000)
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Log.d("FullscreenActivity", "生物识别认证失败")
                        }
                    })

                currentBiometricPrompt = biometricPrompt
                biometricPrompt.authenticate(promptInfo)

                timeoutRunnable = Runnable {
                    try {
                        biometricPrompt.cancelAuthentication()
                    } catch (e: Exception) {
                        Log.d("FullscreenActivity", "取消认证失败: ${e.message}")
                    }
                    isUnlocking = false
                    restartDisplayCycle()
                }
                handler.postDelayed(timeoutRunnable!!, 5000)
            }
            else -> {
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

                timeoutRunnable = Runnable {
                    isUnlocking = false
                    finish()
                }
                handler.postDelayed(timeoutRunnable!!, 5000)
            } else {
                unlockScreenAndFinish()
            }
        } else {
            unlockScreenAndFinish()
        }
    }

    private fun restartDisplayCycle() {
        startWordAnimation()
    }

    private fun unlockScreenAndFinish() {
        if (isUnlocking) {
            Log.d("FullscreenActivity", "正在解锁，跳过")
            return
        }
        isUnlocking = true

        handler.removeCallbacksAndMessages(null)
        Log.d("FullscreenActivity", "unlockScreenAndFinish 被调用")

        try {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )

            val serviceIntent = Intent(this, ScreenService::class.java).apply {
                action = ScreenService.ACTION_STOP
            }
            stopService(serviceIntent)

            finish()
            Log.d("FullscreenActivity", "Activity 已关闭")
        } catch (e: Exception) {
            Log.e("FullscreenActivity", "关闭Activity失败: ${e.message}")
            isUnlocking = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        timeoutRunnable?.let { handler.removeCallbacks(it) }

        if (requestCode == AUTHENTICATION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("FullscreenActivity", "密码认证成功")
                unlockScreenAndFinish()
            } else {
                Log.d("FullscreenActivity", "密码认证失败, resultCode: $resultCode")
                isUnlocking = false
                restartDisplayCycle()
            }
        }
    }
}
