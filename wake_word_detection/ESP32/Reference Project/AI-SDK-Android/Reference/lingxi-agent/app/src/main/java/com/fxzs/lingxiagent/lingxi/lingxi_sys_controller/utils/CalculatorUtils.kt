package com.example.device_control.utils

import android.content.Context
import android.content.Intent
import com.example.device_control.AgentResult

object CalculatorUtils {

    /**
     * 打开系统计算器
     */
    fun openSystemCalculator(context: Context):AgentResult {
        val intents = listOf(
            // 原生 Android
            Intent().setClassName("com.android.calculator2", "com.android.calculator2.Calculator"),

            // 小米 / 红米
            Intent().setClassName("com.miui.calculator", "com.miui.calculator.cal.CalculatorActivity"),

            // OPPO / realme
            Intent().setClassName("com.coloros.calculator", "com.coloros.calculator.CalculatorActivity"),

            // Vivo / iQOO
            Intent().setClassName("com.android.bbkcalculator", "com.android.bbkcalculator.Calculator"),

            // 华为 / 荣耀
            Intent().setClassName("com.huawei.calculator", "com.huawei.calculator.Calculator"),

            // 三星
            Intent().setClassName("com.sec.android.app.popupcalculator", "com.sec.android.app.popupcalculator.Calculator"),

            // ASUS
            Intent().setClassName("com.asus.calculator", "com.asus.calculator.Calculator")
        )

        // 逐个尝试启动
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return AgentResult(true,sucMsg ="已为您开启系统计算器")
            } catch (_: Exception) {
                // 继续尝试下一个
            }
        }
        return AgentResult(true,sucMsg ="未能打开系统计算器")
    }
}
