package com.example.randomscreensaver

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random
import java.util.Calendar

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

    // 当前屏的颜色序列（同屏内强对比）
    private val currentScreenColors = mutableListOf<Int>()

    // ── 时钟相关 ──────────────────────────────────────────────────────────
    private var clockView: ClockView? = null
    private val clockHandler = Handler(Looper.getMainLooper())

    // ── 字体族列表（Android 系统内置字体）──────────────────────────────
    private val fontFamilies = listOf(
        "sans-serif",
        "sans-serif-light",
        "sans-serif-condensed",
        "sans-serif-black",
        "serif",
        "monospace",
        "serif-monospace",
        "casual",
        "cursive"
    )

    // ── 动画类型枚举 ────────────────────────────────────────────────────
    private enum class AnimationType {
        POP,            // 弹出
        ROTATE_IN,      // 旋转入场
        SLIDE_LEFT,     // 从左滑入
        SLIDE_RIGHT,    // 从右滑入
        SLIDE_TOP,      // 从上滑入
        SLIDE_BOTTOM,   // 从下滑入
        SCALE_BOUNCE,   // 弹性缩放
        FLIP,           // 翻转
        FADE_SCALE,     // 淡入缩放
        SHAKE,          // 震颤
        WOBBLE,         // 晃动
        FLASH,          // 闪光
        PULSE,          // 脉冲
        SPRING_DROP,    // 弹簧坠落
        RUBBER_BAND,    // 橡皮筋
        DRUNK,          // 醉酒晃动
        TYPEWRITER,     // 打字机淡入
        SPIRAL,         // 螺旋放大
        JELLO,          // 果冻抖动
        SWING           // 秋千摆动
    }

    companion object {
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_MESSAGE2 = "message2"
        const val EXTRA_IS_LOCKED = "is_locked"
        const val EXTRA_MAX_INTERVAL = "max_interval"
        const val EXTRA_MIN_INTERVAL = "min_interval"
        const val EXTRA_DISPLAY_DURATION = "display_duration"

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

    // 双击检测
    private var lastClickTime = 0L
    private val doubleClickInterval = 500L

    // 退出标志 —— 一旦置为 true，所有动画回调均跳过
    @Volatile
    private var isExiting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        lockScreen()
        hideSystemUI()

        window.decorView.setBackgroundColor(Color.BLACK)

        container = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        setContentView(container)

        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        container.setOnTouchListener { _, _ -> false }

        currentMessage = intent.getStringExtra(EXTRA_MESSAGE) ?: getString(R.string.default_message)
        currentMessage2 = intent.getStringExtra(EXTRA_MESSAGE2)
        isLockedScreen = intent.getBooleanExtra(EXTRA_IS_LOCKED, false)
        maxInterval = intent.getIntExtra(EXTRA_MAX_INTERVAL, MainActivity.DEFAULT_MAX_INTERVAL)
        minInterval = intent.getIntExtra(EXTRA_MIN_INTERVAL, MainActivity.DEFAULT_MIN_INTERVAL)
        displayDuration = intent.getLongExtra(EXTRA_DISPLAY_DURATION, 10000L)

        Log.d("FullscreenActivity", "消息1: $currentMessage, 消息2: $currentMessage2")

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
        if (isExiting) return
        activeTextViews.clear()
        usedRects.clear()
        currentScreenColors.clear()

        val hasSecondMessage = !currentMessage2.isNullOrEmpty()
        val displayMode = if (hasSecondMessage) Random.nextInt(0, 3) else 0

        val displayText = when (displayMode) {
            0 -> currentMessage
            1 -> currentMessage2 ?: currentMessage
            2 -> "$currentMessage $currentMessage2"
            else -> currentMessage
        }

        Log.d("FullscreenActivity", "显示模式: $displayMode, 内容: $displayText")

        val words = splitTextIntoWords(displayText)
        Log.d("FullscreenActivity", "分割成 ${words.size} 个显示单元: $words")

        // 预生成同屏强对比色序列
        buildContrastColorSequence(words.size)

        // 显示随机风格时钟
        showClock()

        showWordsSequentially(words, 0)

        handler.postDelayed({
            clearAllWordsAndContinue()
        }, displayDuration)
    }

    /**
     * 预生成 count 个颜色，保证同屏内相邻颜色 hue 差距大：
     * - 每次新颜色与已有所有颜色的 hue 差都 ≥ minHueDiff
     * - 有 40% 概率直接取上一个颜色的互补色（hue+180°），确保反色感
     */
    private fun buildContrastColorSequence(count: Int) {
        val usedHues = mutableListOf<Float>()
        val minHueDiff = 60f   // 最小色相差
        val sat = 0.85f + Random.nextFloat() * 0.15f  // 高饱和度
        val `val` = 0.85f + Random.nextFloat() * 0.15f // 高亮度

        // 第一个颜色完全随机
        val firstHue = Random.nextFloat() * 360f
        usedHues.add(firstHue)
        currentScreenColors.add(Color.HSVToColor(floatArrayOf(firstHue, sat, `val`)))

        repeat(count - 1) {
            val lastHue = usedHues.last()
            val newHue: Float

            if (Random.nextFloat() < 0.4f) {
                // 40% 直接用互补色
                newHue = (lastHue + 180f) % 360f
            } else {
                // 其余：在全色相空间里找一个与所有已用 hue 差距最大的角度
                var bestHue = (lastHue + 120f) % 360f
                var bestMinDiff = 0f
                // 用16等分采样找最优
                repeat(16) { i ->
                    val candidate = (lastHue + 22.5f * (i + 1)) % 360f
                    val minDiff = usedHues.minOf { used ->
                        val diff = Math.abs(candidate - used)
                        minOf(diff, 360f - diff)
                    }
                    if (minDiff > bestMinDiff) {
                        bestMinDiff = minDiff
                        bestHue = candidate
                    }
                }
                // 在最优角度附近加一点随机扰动（±20°）
                newHue = ((bestHue + Random.nextFloat() * 40f - 20f) + 360f) % 360f
            }

            // 每个词独立随机饱和度/亮度，但都保持高饱和高亮度
            val s = 0.75f + Random.nextFloat() * 0.25f
            val v = 0.80f + Random.nextFloat() * 0.20f
            usedHues.add(newHue)
            currentScreenColors.add(Color.HSVToColor(floatArrayOf(newHue, s, v)))
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  时钟显示（吸附上沿或下沿，随机 X 位置，随机样式）
    // ══════════════════════════════════════════════════════════════════

    /**
     * 随机选一种 ClockView.Style，吸附上沿或下沿，随机 X 位置显示，
     * 每秒刷新一次。
     */
    private fun showClock() {
        removeClock()

        // 随机选样式（共10种，排除 CHINESE_TIME）
        val style = ClockView.Style.values().random()

        // 时钟主色取当前屏颜色序列的互补色（让时钟颜色与提醒词形成对比）
        val baseColor = if (currentScreenColors.isNotEmpty())
            currentScreenColors.random()
        else
            generateVisibleColor()
        val mainColor   = complementColor(baseColor)
        val accentColor = generateVisibleColor()    // 秒针/发光随机亮色

        val cv = ClockView(this).apply {
            this.style       = style
            this.clockColor  = mainColor
            this.accentColor = accentColor
            alpha = 0f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        clockView = cv

        // 先测量尺寸
        cv.measure(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
        )
        val cw = cv.measuredWidth.coerceAtLeast(50)
        val ch = cv.measuredHeight.coerceAtLeast(50)

        // 吸附上沿或下沿
        val snapTop = Random.nextBoolean()
        val edgePad = (padding * 0.6f).toInt()
        val yPos = if (snapTop) {
            edgePad.toFloat()
        } else {
            (screenHeight - ch - edgePad).toFloat()
        }

        // X 轴随机（保证不超出屏幕）
        val maxX = (screenWidth - cw - edgePad).toFloat().coerceAtLeast(edgePad.toFloat())
        val xPos = edgePad + Random.nextFloat() * maxX

        cv.x = xPos
        cv.y = yPos
        container.addView(cv)

        // ── 把时钟区域加入 usedRects，避免文字与时钟重叠 ──
        usedRects.add(Rect(
            xPos - padding,
            yPos - padding,
            xPos + cw + padding,
            yPos + ch + padding
        ))

        // 淡入
        cv.animate().alpha(1f).setDuration(600)
            .setInterpolator(DecelerateInterpolator()).start()

        // 每秒 tick
        scheduleClockTick()
    }

    private fun scheduleClockTick() {
        clockHandler.postDelayed(object : Runnable {
            override fun run() {
                if (isExiting) return
                clockView?.tick() ?: return
                clockHandler.postDelayed(this, 1000L)
            }
        }, 1000L)
    }

    private fun removeClock() {
        clockHandler.removeCallbacksAndMessages(null)
        clockView?.let { cv ->
            cv.animate().alpha(0f).setDuration(300).withEndAction {
                // 双击退出时 container 已被 removeAllViews，这里安全地尝试移除即可
                try { container.removeView(cv) } catch (_: Exception) {}
            }.start()
        }
        clockView = null
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
        if (isExiting) return
        if (index >= words.size) return

        val word = words[index]
        val textView = createAnimatedTextView(word, index)

        val position = findNonOverlappingPosition(textView)
        if (position != null) {
            textView.x = position.first
            textView.y = position.second

            container.addView(textView)
            activeTextViews.add(textView)

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

            val animType = AnimationType.values().random()
            playEntranceAnimation(textView, animType)
        }

        // 词间延迟更随机，有时快有时慢，更调皮
        val nextDelay = when (Random.nextInt(5)) {
            0 -> Random.nextLong(20, 80)    // 极快连发
            1 -> Random.nextLong(80, 200)   // 快速
            2 -> Random.nextLong(200, 400)  // 正常
            3 -> Random.nextLong(400, 700)  // 慢悠悠
            else -> Random.nextLong(50, 600) // 完全随机
        }
        handler.postDelayed({
            if (!isExiting) showWordsSequentially(words, index + 1)
        }, nextDelay)
    }

    // ══════════════════════════════════════════════════════════════════
    //  核心：创建随机样式的 TextView
    // ══════════════════════════════════════════════════════════════════
    private fun createAnimatedTextView(text: String, colorIndex: Int): TextView {
        // 从预生成的对比色序列里取颜色，越界则兜底随机
        val randomColor = if (colorIndex < currentScreenColors.size)
            currentScreenColors[colorIndex]
        else
            generateVisibleColor()

        // 阴影取该词颜色的互补色（hue+180°），形成强烈对比发光效果
        val shadowColor = complementColor(randomColor)

        // 字号：20~72sp，按权重偏向大字
        val sizeCandidates = listOf(20, 24, 28, 32, 36, 42, 48, 56, 64, 72)
        val randomSize = sizeCandidates.random().toFloat()

        // 字体族随机
        val fontFamily = fontFamilies.random()

        // 粗体 / 斜体 / 粗斜体 随机
        val styleIndex = Random.nextInt(4)
        val randomTypeface = when (styleIndex) {
            0 -> Typeface.create(fontFamily, Typeface.NORMAL)
            1 -> Typeface.create(fontFamily, Typeface.BOLD)
            2 -> Typeface.create(fontFamily, Typeface.ITALIC)
            else -> Typeface.create(fontFamily, Typeface.BOLD_ITALIC)
        }

        // 初始旋转倾斜：-35° ~ 35°，概率分布偏向小角度
        val tiltAngle = when (Random.nextInt(4)) {
            0 -> 0f                                    // 正直（较常见）
            1 -> Random.nextFloat() * 15f - 7.5f      // 轻微倾斜
            2 -> Random.nextFloat() * 35f - 17.5f     // 中等倾斜
            else -> if (Random.nextBoolean()) 90f else -90f // 竖排（少见）
        }

        val shadowRadius = Random.nextFloat() * 14f + 4f
        val shadowDx = Random.nextFloat() * 6f - 3f
        val shadowDy = Random.nextFloat() * 6f - 3f

        return TextView(this).apply {
            this.text = text
            setTextColor(randomColor)
            textSize = randomSize
            typeface = randomTypeface
            rotation = tiltAngle
            gravity = Gravity.CENTER
            setPadding(20, 12, 20, 12)
            alpha = 0f

            setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            clipToOutline = false
        }
    }

    /** 返回给定颜色的互补色（HSV hue + 180°） */
    private fun complementColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[0] = (hsv[0] + 180f) % 360f
        // 互补阴影保持高饱和、稍低亮度，避免太亮盖过文字
        hsv[1] = (hsv[1] * 0.9f).coerceIn(0.6f, 1f)
        hsv[2] = (hsv[2] * 0.8f).coerceIn(0.5f, 0.9f)
        return Color.HSVToColor(hsv)
    }

    private fun generateVisibleColor(): Int {
        val hue = Random.nextFloat() * 360
        val saturation = 0.7f + Random.nextFloat() * 0.3f
        val value = 0.8f + Random.nextFloat() * 0.2f
        return Color.HSVToColor(floatArrayOf(hue, saturation, value))
    }

    private fun findNonOverlappingPosition(textView: TextView): Pair<Float, Float>? {
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
        textView.measure(widthMeasureSpec, heightMeasureSpec)

        val textWidth = textView.measuredWidth.toFloat()
        val textHeight = textView.measuredHeight.toFloat()

        val safePadding = padding * 1.5f
        val availableWidth = screenWidth - safePadding * 2 - textWidth
        val availableHeight = screenHeight - safePadding * 2 - textHeight

        if (availableWidth <= 0 || availableHeight <= 0) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textView.textSize * 0.7f)
            textView.measure(widthMeasureSpec, heightMeasureSpec)
            return Pair(safePadding, safePadding)
        }

        repeat(50) {
            val x = safePadding + Random.nextFloat() * availableWidth
            val y = safePadding + Random.nextFloat() * availableHeight
            val newRect = Rect(x - 20, y - 20, x + textWidth + 20, y + textHeight + 20)
            if (usedRects.none { it.intersects(newRect) }) {
                return Pair(x, y)
            }
        }
        return null
    }

    // ══════════════════════════════════════════════════════════════════
    //  入场动画分发
    // ══════════════════════════════════════════════════════════════════
    private fun playEntranceAnimation(textView: TextView, animType: AnimationType) {
        when (animType) {
            AnimationType.POP          -> animatePop(textView)
            AnimationType.ROTATE_IN    -> animateRotateIn(textView)
            AnimationType.SLIDE_LEFT   -> animateSlideFromLeft(textView)
            AnimationType.SLIDE_RIGHT  -> animateSlideFromRight(textView)
            AnimationType.SLIDE_TOP    -> animateSlideFromTop(textView)
            AnimationType.SLIDE_BOTTOM -> animateSlideFromBottom(textView)
            AnimationType.SCALE_BOUNCE -> animateScaleBounce(textView)
            AnimationType.FLIP         -> animateFlip(textView)
            AnimationType.FADE_SCALE   -> animateFadeScale(textView)
            AnimationType.SHAKE        -> animateShake(textView)
            AnimationType.WOBBLE       -> animateWobble(textView)
            AnimationType.FLASH        -> animateFlash(textView)
            AnimationType.PULSE        -> animatePulse(textView)
            AnimationType.SPRING_DROP  -> animateSpringDrop(textView)
            AnimationType.RUBBER_BAND  -> animateRubberBand(textView)
            AnimationType.DRUNK        -> animateDrunk(textView)
            AnimationType.TYPEWRITER   -> animateTypewriter(textView)
            AnimationType.SPIRAL       -> animateSpiral(textView)
            AnimationType.JELLO        -> animateJello(textView)
            AnimationType.SWING        -> animateSwing(textView)
        }
    }

    // ── 原有动画 ────────────────────────────────────────────────────────

    private fun animatePop(textView: TextView) {
        textView.scaleX = 0f
        textView.scaleY = 0f
        textView.alpha = 1f
        ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("scaleX", 0f, 1.3f, 0.9f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0f, 1.3f, 0.9f, 1f)
        ).apply {
            duration = 450
            interpolator = OvershootInterpolator(2f)
            start()
        }
    }

    private fun animateRotateIn(textView: TextView) {
        textView.rotation = -180f
        textView.scaleX = 0f
        textView.scaleY = 0f
        textView.alpha = 1f
        ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("rotation", -180f, 0f),
            PropertyValuesHolder.ofFloat("scaleX", 0f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0f, 1f)
        ).apply {
            duration = 500
            interpolator = AnticipateOvershootInterpolator()
            start()
        }
    }

    private fun animateSlideFromLeft(textView: TextView) {
        val originalX = textView.x
        textView.x = -300f
        textView.alpha = 1f
        textView.animate().x(originalX).setDuration(420)
            .setInterpolator(OvershootInterpolator(1.5f)).start()
    }

    private fun animateSlideFromRight(textView: TextView) {
        val originalX = textView.x
        textView.x = screenWidth + 300f
        textView.alpha = 1f
        textView.animate().x(originalX).setDuration(420)
            .setInterpolator(OvershootInterpolator(1.5f)).start()
    }

    private fun animateSlideFromTop(textView: TextView) {
        val originalY = textView.y
        textView.y = -300f
        textView.alpha = 1f
        textView.animate().y(originalY).setDuration(420)
            .setInterpolator(BounceInterpolator()).start()
    }

    private fun animateSlideFromBottom(textView: TextView) {
        val originalY = textView.y
        textView.y = screenHeight + 300f
        textView.alpha = 1f
        textView.animate().y(originalY).setDuration(420)
            .setInterpolator(OvershootInterpolator(1.5f)).start()
    }

    private fun animateScaleBounce(textView: TextView) {
        textView.scaleX = 0f
        textView.scaleY = 0f
        textView.alpha = 1f
        ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("scaleX", 0f, 1.15f, 0.88f, 1.05f, 0.97f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0f, 1.15f, 0.88f, 1.05f, 0.97f, 1f)
        ).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun animateFlip(textView: TextView) {
        textView.rotationY = 90f
        textView.alpha = 1f
        ObjectAnimator.ofFloat(textView, "rotationY", 90f, -15f, 5f, 0f).apply {
            duration = 450
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun animateFadeScale(textView: TextView) {
        textView.scaleX = 0.3f
        textView.scaleY = 0.3f
        textView.alpha = 0f
        textView.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(400).setInterpolator(OvershootInterpolator(1.2f)).start()
    }

    private fun animateShake(textView: TextView) {
        textView.alpha = 1f
        ObjectAnimator.ofFloat(textView, "translationX",
            0f, 30f, -30f, 25f, -25f, 18f, -18f, 10f, -10f, 5f, 0f).apply {
            duration = 600
            start()
        }
    }

    private fun animateWobble(textView: TextView) {
        textView.alpha = 1f
        ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 1.25f, 0.85f, 1.15f, 0.93f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 0.75f, 1.15f, 0.88f, 1.07f, 1f),
            PropertyValuesHolder.ofFloat("rotation", 0f, -12f, 12f, -7f, 7f, -3f, 0f)
        ).apply {
            duration = 700
            start()
        }
    }

    private fun animateFlash(textView: TextView) {
        textView.alpha = 0f
        ObjectAnimator.ofFloat(textView, "alpha",
            0f, 1f, 0.2f, 1f, 0.5f, 1f).apply {
            duration = 600
            start()
        }
    }

    private fun animatePulse(textView: TextView) {
        textView.alpha = 1f
        ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("scaleX", 0f, 1.6f, 0.9f, 1.1f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0f, 1.6f, 0.9f, 1.1f, 1f)
        ).apply {
            duration = 550
            interpolator = OvershootInterpolator(1.5f)
            start()
        }
    }

    // ── 新增动画 ────────────────────────────────────────────────────────

    /** 弹簧坠落：从上方高处以弹弹的节奏落下 */
    private fun animateSpringDrop(textView: TextView) {
        val originalY = textView.y
        textView.y = -screenHeight * 0.3f
        textView.alpha = 1f
        textView.animate().y(originalY)
            .setDuration(700)
            .setInterpolator(BounceInterpolator())
            .start()
    }

    /** 橡皮筋：水平方向先超出再回弹 */
    private fun animateRubberBand(textView: TextView) {
        textView.alpha = 1f
        ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 1.5f, 0.75f, 1.25f, 0.88f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 0.5f, 1.25f, 0.85f, 1.1f, 1f)
        ).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    /** 醉酒：旋转+位移的随机晃动组合 */
    private fun animateDrunk(textView: TextView) {
        textView.alpha = 1f
        val dx = Random.nextFloat() * 40f - 20f
        val dy = Random.nextFloat() * 40f - 20f
        ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("rotation",
                0f, 20f, -25f, 18f, -12f, 8f, -4f, 0f),
            PropertyValuesHolder.ofFloat("translationX",
                0f, dx, -dx * 0.8f, dx * 0.5f, 0f),
            PropertyValuesHolder.ofFloat("translationY",
                0f, dy, -dy * 0.6f, dy * 0.3f, 0f)
        ).apply {
            duration = 900
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    /** 打字机：先完全透明，然后逐渐淡入同时从小到正常 */
    private fun animateTypewriter(textView: TextView) {
        textView.alpha = 0f
        textView.scaleX = 0.6f
        textView.scaleY = 0.6f
        val delay = Random.nextLong(0, 150)
        handler.postDelayed({
            if (!isExiting) {
                textView.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(350)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        }, delay)
    }

    /** 螺旋：先旋转360°同时从0放大到正常 */
    private fun animateSpiral(textView: TextView) {
        textView.rotation = 360f
        textView.scaleX = 0f
        textView.scaleY = 0f
        textView.alpha = 1f
        ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("rotation", 360f, 0f),
            PropertyValuesHolder.ofFloat("scaleX", 0f, 1.1f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0f, 1.1f, 1f)
        ).apply {
            duration = 600
            interpolator = DecelerateInterpolator(1.5f)
            start()
        }
    }

    /** 果冻：上下轴向交替压缩，像果冻一样颤抖 */
    private fun animateJello(textView: TextView) {
        textView.alpha = 1f
        ObjectAnimator.ofPropertyValuesHolder(
            textView,
            PropertyValuesHolder.ofFloat("scaleX",
                1f, 1.3f, 0.7f, 1.2f, 0.8f, 1.1f, 0.9f, 1.05f, 0.95f, 1f),
            PropertyValuesHolder.ofFloat("scaleY",
                1f, 0.7f, 1.3f, 0.8f, 1.2f, 0.9f, 1.1f, 0.95f, 1.05f, 1f)
        ).apply {
            duration = 900
            interpolator = LinearInterpolator()
            start()
        }
    }

    /** 秋千摆动：绕顶部中心点类似秋千的摇摆入场 */
    private fun animateSwing(textView: TextView) {
        textView.pivotX = textView.measuredWidth / 2f
        textView.pivotY = 0f
        textView.rotation = -60f
        textView.alpha = 1f
        ObjectAnimator.ofFloat(textView, "rotation",
            -60f, 30f, -20f, 12f, -6f, 3f, 0f).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  出场动画（每个词独立随机出场方式）
    // ══════════════════════════════════════════════════════════════════
    private fun clearAllWordsAndContinue() {
        if (isExiting) return

        // 移除时钟
        removeClock()

        if (activeTextViews.isEmpty()) {
            waitAndShowNextMessage()
            return
        }

        var completedCount = 0
        val totalCount = activeTextViews.size

        activeTextViews.forEachIndexed { index, textView ->
            // 每个词独立随机出场类型
            val exitType = Random.nextInt(8)
            // 错开时间出场，更生动
            val exitDelay = index * Random.nextLong(30L, 100L)
            handler.postDelayed({
                if (isExiting) return@postDelayed
                playExitAnimation(textView, exitType) {
                    if (isExiting) return@playExitAnimation
                    completedCount++
                    if (completedCount >= totalCount) {
                        if (isExiting) return@playExitAnimation
                        container.removeAllViews()
                        activeTextViews.clear()
                        usedRects.clear()
                        waitAndShowNextMessage()
                    }
                }
            }, exitDelay)
        }
    }

    private fun playExitAnimation(textView: TextView, type: Int, onComplete: () -> Unit) {
        when (type) {
            0 -> { // 淡出缩小
                textView.animate().alpha(0f).scaleX(0f).scaleY(0f)
                    .setDuration(350).withEndAction { if (!isExiting) onComplete() }.start()
            }
            1 -> { // 向上飞出
                textView.animate().y(-300f).alpha(0f)
                    .setDuration(350).withEndAction { if (!isExiting) onComplete() }.start()
            }
            2 -> { // 向右飞出
                textView.animate().x(screenWidth + 300f).alpha(0f)
                    .setDuration(350).withEndAction { if (!isExiting) onComplete() }.start()
            }
            3 -> { // 旋转消失
                ObjectAnimator.ofPropertyValuesHolder(
                    textView,
                    PropertyValuesHolder.ofFloat("rotation", 0f, 360f),
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 0f),
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 0f),
                    PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
                ).apply {
                    duration = 350
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            if (!isExiting) onComplete()
                        }
                    })
                    start()
                }
            }
            4 -> { // 向左飞出
                textView.animate().x(-300f).alpha(0f)
                    .setDuration(350).withEndAction { if (!isExiting) onComplete() }.start()
            }
            5 -> { // 向下掉落
                textView.animate().y(screenHeight + 200f).alpha(0f)
                    .setInterpolator(AccelerateInterpolator(2f))
                    .setDuration(400).withEndAction { if (!isExiting) onComplete() }.start()
            }
            6 -> { // 爆炸放大消失
                textView.animate().scaleX(3f).scaleY(3f).alpha(0f)
                    .setDuration(350).withEndAction { if (!isExiting) onComplete() }.start()
            }
            7 -> { // 翻转消失
                ObjectAnimator.ofFloat(textView, "rotationY", 0f, 90f).apply {
                    duration = 200
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            textView.alpha = 0f
                            if (!isExiting) onComplete()
                        }
                    })
                    start()
                }
            }
        }
    }

    private fun waitAndShowNextMessage() {
        if (isExiting) return
        val nextInterval = Random.nextLong(
            minInterval.toLong() * 1000,
            maxInterval.toLong() * 1000
        )
        Log.d("FullscreenActivity", "等待下一条消息: ${nextInterval}ms")
        handler.postDelayed({ startWordAnimation() }, nextInterval)
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
        if (hasFocus) hideSystemUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        clockHandler.removeCallbacksAndMessages(null)
    }

    // 双击退出屏保
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < doubleClickInterval) {
                Log.d("FullscreenActivity", "双击退出屏保")
                // ① 先置标志，所有 isExiting 检查点都会跳过
                isExiting = true
                // ② 清两个 handler 的所有挂起任务
                handler.removeCallbacksAndMessages(null)
                clockHandler.removeCallbacksAndMessages(null)
                // ③ 立即取消所有正在运行的 View 动画，防止 withEndAction 回调继续触发
                container.removeAllViews()
                finish()
                return true
            } else {
                lastClickTime = currentTime
            }
        }
        return super.onTouchEvent(event)
    }
}
