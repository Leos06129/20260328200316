package com.example.randomscreensaver

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.*

/**
 * 多样式时钟 View
 * 支持文字类和指针表盘类，每秒自动刷新。
 */
class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // ── 样式枚举 ─────────────────────────────────────────────────────────
    enum class Style {
        // 文字类
        DIGITAL_LARGE,     // 大数字      23:07
        MINIMAL_COLON,     // 极简点      23·07
        RETRO_LCD,         // 复古LCD     [23:07]
        DATE_TIME,         // 日期+时间   04-07 / 23:07
        AMPM_ENGLISH,      // AM/PM英文   11:07 PM
        SECONDS_TICKER,    // 含秒        23:07:45
        FUZZY_TIME,        // 模糊时间    快到午夜了

        // 指针表盘类
        ANALOG_CLASSIC,    // 经典表盘：刻度圆+时分秒针
        ANALOG_MINIMAL,    // 极简表盘：无刻度，只有三根细针
        ANALOG_NEON,       // 霓虹表盘：发光描边+彩色指针
    }

    var style: Style = Style.DIGITAL_LARGE
    var clockColor: Int = Color.WHITE          // 主色（针、文字）
    var accentColor: Int = Color.CYAN          // 强调色（秒针、高亮刻度）

    // ── 固定尺寸 ─────────────────────────────────────────────────────────
    // 文字类：宽由 desiredW/desiredH 驱动；表盘类固定正方形
    private val dialSize = (context.resources.displayMetrics.density * 130).toInt()  // 130dp

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ── 用于绘制文字时钟的 Paint ─────────────────────────────────────────
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isAnalog()) {
            setMeasuredDimension(dialSize, dialSize)
        } else {
            // 文字类：测量文字宽高
            val (w, h) = measureTextClock()
            setMeasuredDimension(w, h)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (isAnalog()) {
            drawAnalog(canvas)
        } else {
            drawTextClock(canvas)
        }
    }

    // ── 每秒调用触发重绘 ────────────────────────────────────────────────
    fun tick() {
        invalidate()
    }

    // ════════════════════════════════════════════════════════════════
    //  文字时钟
    // ════════════════════════════════════════════════════════════════

    private fun isAnalog() = style in listOf(
        Style.ANALOG_CLASSIC, Style.ANALOG_MINIMAL, Style.ANALOG_NEON
    )

    private data class TextLayout(val lines: List<Pair<String, Float>>) // text, textSizeSp

    private fun getTextLayout(): TextLayout {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val s = cal.get(Calendar.SECOND)
        val mon = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dp = context.resources.displayMetrics.density

        return when (style) {
            Style.DIGITAL_LARGE -> TextLayout(listOf("%02d:%02d".format(h, m) to 64f))
            Style.MINIMAL_COLON -> TextLayout(listOf("%02d·%02d".format(h, m) to 52f))
            Style.RETRO_LCD     -> TextLayout(listOf("[%02d:%02d]".format(h, m) to 44f))
            Style.DATE_TIME     -> TextLayout(listOf(
                "%02d-%02d".format(mon, day) to 28f,
                "%02d:%02d".format(h, m) to 38f
            ))
            Style.AMPM_ENGLISH  -> {
                val h12 = if (h % 12 == 0) 12 else h % 12
                val ampm = if (h < 12) "AM" else "PM"
                TextLayout(listOf("%02d:%02d %s".format(h12, m, ampm) to 46f))
            }
            Style.SECONDS_TICKER -> TextLayout(listOf("%02d:%02d:%02d".format(h, m, s) to 40f))
            Style.FUZZY_TIME    -> TextLayout(listOf(toFuzzyTime(h, m) to 28f))
            else -> TextLayout(listOf("" to 24f))
        }
    }

    private fun measureTextClock(): Pair<Int, Int> {
        val layout = getTextLayout()
        val density = context.resources.displayMetrics.density
        var maxW = 0f
        var totalH = 0f
        layout.lines.forEach { (text, sp) ->
            textPaint.textSize = sp * density
            val bounds = Rect()
            textPaint.getTextBounds(text, 0, text.length, bounds)
            if (bounds.width() > maxW) maxW = bounds.width().toFloat()
            totalH += bounds.height() + 8 * density
        }
        return (maxW + 48 * density).toInt() to (totalH + 24 * density).toInt()
    }

    private fun drawTextClock(canvas: Canvas) {
        val layout = getTextLayout()
        val density = context.resources.displayMetrics.density
        val cx = width / 2f
        var y = 12 * density

        layout.lines.forEach { (text, sp) ->
            textPaint.color = clockColor
            textPaint.textSize = sp * density
            textPaint.setShadowLayer(10f * density, 0f, 0f, accentColor)
            val bounds = Rect()
            textPaint.getTextBounds(text, 0, text.length, bounds)
            y += bounds.height()
            canvas.drawText(text, cx, y, textPaint)
            y += 8 * density
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  指针表盘
    // ════════════════════════════════════════════════════════════════

    private fun drawAnalog(canvas: Canvas) {
        when (style) {
            Style.ANALOG_CLASSIC -> drawClassic(canvas)
            Style.ANALOG_MINIMAL -> drawMinimal(canvas)
            Style.ANALOG_NEON    -> drawNeon(canvas)
            else -> {}
        }
    }

    // ── 公共：获取当前时间角度（弧度，12点为0，顺时针） ──────────────────
    private fun timeAngles(): Triple<Float, Float, Float> {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY) % 12
        val m = cal.get(Calendar.MINUTE)
        val s = cal.get(Calendar.SECOND)
        val secAngle  = Math.toRadians((s * 6 - 90).toDouble()).toFloat()
        val minAngle  = Math.toRadians((m * 6 + s * 0.1 - 90).toDouble()).toFloat()
        val hourAngle = Math.toRadians((h * 30 + m * 0.5 - 90).toDouble()).toFloat()
        return Triple(hourAngle, minAngle, secAngle)
    }

    private fun drawHand(canvas: Canvas, cx: Float, cy: Float,
                         angle: Float, length: Float, width: Float, color: Int,
                         shadow: Boolean = false) {
        paint.reset()
        paint.isAntiAlias = true
        paint.color = color
        paint.strokeWidth = width
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        if (shadow) {
            paint.setShadowLayer(width * 2, 0f, 0f, accentColor)
        }
        val ex = cx + cos(angle) * length
        val ey = cy + sin(angle) * length
        canvas.drawLine(cx, cy, ex, ey, paint)
        paint.clearShadowLayer()
    }

    // ── 经典表盘 ────────────────────────────────────────────────────────
    private fun drawClassic(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(cx, cy) - 4f
        val (hourA, minA, secA) = timeAngles()

        // 外圆
        paint.reset()
        paint.isAntiAlias = true
        paint.color = Color.argb(60, Color.red(clockColor), Color.green(clockColor), Color.blue(clockColor))
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, r, paint)

        paint.color = clockColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        canvas.drawCircle(cx, cy, r, paint)

        // 刻度
        for (i in 0 until 60) {
            val a = Math.toRadians((i * 6 - 90).toDouble()).toFloat()
            val isMajor = i % 5 == 0
            val inner = if (isMajor) r * 0.82f else r * 0.91f
            val outer = r * 0.97f
            paint.strokeWidth = if (isMajor) 2.5f else 1f
            paint.color = if (isMajor) clockColor else Color.argb(
                120, Color.red(clockColor), Color.green(clockColor), Color.blue(clockColor)
            )
            canvas.drawLine(
                cx + cos(a) * inner, cy + sin(a) * inner,
                cx + cos(a) * outer, cy + sin(a) * outer,
                paint
            )
        }

        // 时针
        drawHand(canvas, cx, cy, hourA, r * 0.52f, 5f, clockColor)
        // 分针
        drawHand(canvas, cx, cy, minA,  r * 0.72f, 3f, clockColor)
        // 秒针（强调色）
        drawHand(canvas, cx, cy, secA,  r * 0.85f, 1.5f, accentColor)
        // 中心圆点
        paint.reset()
        paint.isAntiAlias = true
        paint.color = accentColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, 5f, paint)
    }

    // ── 极简表盘 ────────────────────────────────────────────────────────
    private fun drawMinimal(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(cx, cy) - 4f
        val (hourA, minA, secA) = timeAngles()

        // 细圆圈
        paint.reset()
        paint.isAntiAlias = true
        paint.color = Color.argb(100, Color.red(clockColor), Color.green(clockColor), Color.blue(clockColor))
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawCircle(cx, cy, r, paint)

        // 只有4个点标记12/3/6/9
        for (i in 0 until 4) {
            val a = Math.toRadians((i * 90 - 90).toDouble()).toFloat()
            val dotR = 3f
            paint.color = clockColor
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx + cos(a) * r * 0.88f, cy + sin(a) * r * 0.88f, dotR, paint)
        }

        // 时针（粗）
        drawHand(canvas, cx, cy, hourA, r * 0.50f, 4f, clockColor)
        // 分针
        drawHand(canvas, cx, cy, minA,  r * 0.70f, 2.5f, clockColor)
        // 秒针（细，强调色）
        drawHand(canvas, cx, cy, secA,  r * 0.82f, 1.2f, accentColor)

        // 中心点
        paint.reset()
        paint.isAntiAlias = true
        paint.color = clockColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, 3.5f, paint)
    }

    // ── 霓虹表盘 ────────────────────────────────────────────────────────
    private fun drawNeon(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(cx, cy) - 4f
        val (hourA, minA, secA) = timeAngles()

        // 霓虹外圆（双层发光）
        for (glow in listOf(12f, 5f)) {
            paint.reset()
            paint.isAntiAlias = true
            paint.color = Color.argb((255 * 0.3f * (1f - glow / 14f)).toInt(),
                Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = glow
            canvas.drawCircle(cx, cy, r, paint)
        }
        paint.color = accentColor
        paint.strokeWidth = 1.5f
        canvas.drawCircle(cx, cy, r, paint)

        // 霓虹刻度（仅12个小时刻度）
        for (i in 0 until 12) {
            val a = Math.toRadians((i * 30 - 90).toDouble()).toFloat()
            val inner = r * 0.80f
            val outer = r * 0.94f
            paint.color = if (i == 0) Color.WHITE else accentColor
            paint.strokeWidth = if (i == 0) 3f else 1.5f
            paint.setShadowLayer(6f, 0f, 0f, accentColor)
            canvas.drawLine(
                cx + cos(a) * inner, cy + sin(a) * inner,
                cx + cos(a) * outer, cy + sin(a) * outer,
                paint
            )
            paint.clearShadowLayer()
        }

        // 时针（clockColor 发光）
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = clockColor
        paint.strokeWidth = 5f
        paint.setShadowLayer(10f, 0f, 0f, clockColor)
        canvas.drawLine(cx, cy,
            cx + cos(hourA) * r * 0.50f, cy + sin(hourA) * r * 0.50f, paint)

        // 分针
        paint.strokeWidth = 3f
        paint.setShadowLayer(8f, 0f, 0f, clockColor)
        canvas.drawLine(cx, cy,
            cx + cos(minA) * r * 0.70f, cy + sin(minA) * r * 0.70f, paint)

        // 秒针（accentColor）
        paint.color = accentColor
        paint.strokeWidth = 1.5f
        paint.setShadowLayer(12f, 0f, 0f, accentColor)
        canvas.drawLine(cx, cy,
            cx + cos(secA) * r * 0.85f, cy + sin(secA) * r * 0.85f, paint)

        // 中心发光点
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = accentColor
        paint.setShadowLayer(10f, 0f, 0f, accentColor)
        canvas.drawCircle(cx, cy, 4f, paint)
    }

    // ── 模糊时间工具 ───────────────────────────────────────────────────
    private fun toFuzzyTime(h: Int, m: Int): String {
        return when {
            m < 5  -> when (h) {
                0, 24 -> "午夜刚过"; 1 -> "凌晨一点"; 2 -> "深夜两点"; 3 -> "夜深三点"
                4 -> "黎明前夕"; 5 -> "天快亮了"; 6 -> "清晨六点"; 7 -> "早上七点"
                8 -> "上午八点"; 9 -> "上午九点"; 10 -> "上午十点"; 11 -> "快到中午了"
                12 -> "正午时分"; 13 -> "下午一点"; 14 -> "下午两点"; 15 -> "下午三点"
                16 -> "下午四点"; 17 -> "傍晚五点"; 18 -> "傍晚六点"; 19 -> "晚上七点"
                20 -> "晚上八点"; 21 -> "夜里九点"; 22 -> "夜深十点"; 23 -> "快到午夜了"
                else -> "此刻"
            }
            m < 15 -> "刚过${hourCn(h)}点"
            m < 30 -> "${hourCn(h)}点一刻"
            m < 45 -> "${hourCn(h)}点半"
            else   -> "快到${hourCn((h + 1) % 24)}点了"
        }
    }

    private fun hourCn(h: Int): String {
        val names = listOf("零","一","二","三","四","五","六","七","八","九",
            "十","十一","十二","一","二","三","四","五","六","七","八","九","十","十一")
        return names.getOrElse(h) { h.toString() }
    }
}
