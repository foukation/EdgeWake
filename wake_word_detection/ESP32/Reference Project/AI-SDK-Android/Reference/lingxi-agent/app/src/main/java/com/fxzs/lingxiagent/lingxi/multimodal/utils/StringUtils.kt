package com.ai.multimodal.utils

import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/**
 * 将JSON格式的药品信息转换为特定格式的字符串
 *
 * @param jsonContent JSON格式的药品信息字符串
 * @return 格式化后的药品信息字符串，每个字段以【字段名】开头
 */
fun formatDrugInformation(jsonContent: String): String {
    if (jsonContent.isBlank()) {
        Timber.tag("DrugInfoFormatter").w("输入的JSON内容为空")
        return "无法获取药品信息"
    }

    return try {
        val jsonObject = JSONObject(jsonContent)
        val keys = jsonObject.keys()
        val stringBuilder = StringBuilder()

        // 临时存储所有字段，用于后续排序或处理
        val formattedFields = mutableListOf<Pair<String, String>>()

        // 处理所有字段
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.optString(key, "")
            formattedFields.add(Pair(key, value))
        }

        // 如果有特定的字段顺序要求，添加排序逻辑
        // formattedFields.sortBy { ... }

        // 构建格式化字符串
        for ((index, field) in formattedFields.withIndex()) {
            // 添加字段名
            stringBuilder.append("【${field.first}】")

            // 处理字段值：如果有内容则换行后添加内容
            if (field.second.isNotBlank()) {
                stringBuilder.append("\n${field.second}")
            }

            // 如果不是最后一个字段，添加换行
            if (index < formattedFields.size - 1) {
                stringBuilder.append("\n")
            }
        }

        stringBuilder.toString()
    } catch (e: JSONException) {
        Timber.tag("DrugInfoFormatter").e(e, "JSON解析错误")
        jsonContent
    } catch (e: Exception) {
        Timber.tag("DrugInfoFormatter").e(e, "格式化药品信息时发生未知错误")
        // 捕获任何其他可能的异常
        "处理药品信息时出错，请稍后再试"
    }
}