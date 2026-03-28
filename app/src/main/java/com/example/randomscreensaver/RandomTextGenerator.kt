package com.example.randomscreensaver

import android.graphics.Color
import kotlin.random.Random

object RandomTextGenerator {
    
    // 生成随机颜色
    fun generateRandomColor(): Int {
        return Color.rgb(
            Random.nextInt(256), // R
            Random.nextInt(256), // G
            Random.nextInt(256)  // B
        )
    }
    
    // 生成随机字体大小 (20-100sp)
    fun generateRandomTextSize(): Float {
        return Random.nextInt(20, 101).toFloat()
    }
    
    // 生成随机X坐标 (相对于屏幕宽度的百分比)
    fun generateRandomXPosition(screenWidth: Int): Float {
        // 生成0-100%的随机位置，确保文字不会超出屏幕
        val percentage = Random.nextFloat() * 0.8f + 0.1f // 10%-90%范围
        return screenWidth * percentage
    }
    
    // 生成随机Y坐标 (相对于屏幕高度的百分比)
    fun generateRandomYPosition(screenHeight: Int): Float {
        // 生成0-100%的随机位置，确保文字不会超出屏幕
        val percentage = Random.nextFloat() * 0.8f + 0.1f // 10%-90%范围
        return screenHeight * percentage
    }
    
    // 生成随机显示间隔（秒）
    fun generateRandomInterval(isLockedScreen: Boolean, minInterval: Int, maxInterval: Int): Long {
        return if (isLockedScreen) {
            // 锁屏时：固定10-12秒
            Random.nextInt(10, 13) * 1000L
        } else {
            // 常规时：用户设置的范围
            Random.nextInt(minInterval, maxInterval + 1) * 1000L
        }
    }
    
    // 生成随机透明度 (0.5-1.0)
    fun generateRandomAlpha(): Float {
        return Random.nextFloat() * 0.5f + 0.5f // 50%-100%
    }
    
    // 生成随机阴影颜色
    fun generateRandomShadowColor(): Int {
        return Color.rgb(
            Random.nextInt(256),
            Random.nextInt(256),
            Random.nextInt(256)
        )
    }
    
    // 生成随机阴影半径
    fun generateRandomShadowRadius(): Float {
        return Random.nextFloat() * 10f + 5f // 5-15
    }
    
    // 生成随机阴影偏移
    fun generateRandomShadowOffset(): Float {
        return Random.nextFloat() * 10f - 5f // -5到5
    }
    
    // 生成随机动画持续时间
    fun generateRandomAnimationDuration(): Long {
        return Random.nextLong(500, 2000) // 0.5-2秒
    }
    
    // 生成随机文字旋转角度
    fun generateRandomRotation(): Float {
        return Random.nextFloat() * 30f - 15f // -15到15度
    }
    
    // 生成随机文字缩放比例
    fun generateRandomScale(): Float {
        return Random.nextFloat() * 0.5f + 0.8f // 0.8-1.3
    }
    
    // 获取预设颜色列表（避免生成过于暗淡的颜色）
    private val presetColors = listOf(
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.CYAN,
        Color.MAGENTA,
        Color.rgb(255, 165, 0), // 橙色
        Color.rgb(128, 0, 128), // 紫色
        Color.rgb(0, 128, 128), // 青色
        Color.rgb(255, 192, 203) // 粉色
    )
    
    // 从预设颜色中随机选择
    fun generateRandomColorFromPreset(): Int {
        return presetColors[Random.nextInt(presetColors.size)]
    }
    
    // 生成随机颜色（混合预设和随机）
    fun generateMixedRandomColor(): Int {
        return if (Random.nextBoolean()) {
            generateRandomColorFromPreset()
        } else {
            generateRandomColor()
        }
    }
}