package com.fxzs.lingxiagent.lingxi.marquee

import android.graphics.Color

// MarqueeConfig.kt
data class MarqueeConfig(
    // 边框宽度
    var borderWidth: Int = 4,

    // 发光效果配置
    var glowEnabled: Boolean = true,           // 是否启用发光效果
    var glowRadius: Float = 15f,              // 发光半径
    var glowColor: Int = Color.WHITE,          // 发光颜色
    var glowAlpha: Int = 255,                  // 发光透明度（0-255）

    var doubleGlowEnabled: Boolean = true,    // 是否启用双重发光
    var outerGlowRadius: Float = 15f,        // 外层发光半径
    var outerGlowColor: Int = Color.WHITE,    // 外层发光颜色
    var outerGlowAlpha: Int = 255,            // 外层发光透明度

    var tripleGlowEnabled: Boolean = true,    // 是否启用三重发光
    var farGlowRadius: Float = 15f,          // 最外层发光半径
    var farGlowColor: Int = Color.WHITE,      // 最外层发光颜色
    var farGlowAlpha: Int = 40,               // 最外层发光透明度

    var glowIntensity: Float = 1.0f,          // 发光强度系数 (1.0=正常, >1.0=增强)
    var glowPulseSpeed: Float = 1.0f,         // 脉冲速度 (0.5=慢, 1.0=正常, 2.0=快)
    var glowColorVibrance: Float = 1.0f,      // 色彩鲜艳度 (1.0=正常, >1.0=更鲜艳)

    var isFinish: Int = 0
)