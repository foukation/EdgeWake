package com.fxzs.lingxiagent.lingxi.lingxi_conversation

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson

/**
 * 简单的键值对存储封装
 */
class Prefs private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: Prefs? = null

        fun getInstance(context: Context): Prefs {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Prefs(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    val gson = Gson()

    // 基础类型存储
    fun putString(key: String, value: String?) {
        sharedPreferences.edit { putString(key, value) }
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    fun putInt(key: String, value: Int) {
        sharedPreferences.edit { putInt(key, value) }
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun putLong(key: String, value: Long) {
        sharedPreferences.edit { putLong(key, value) }
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    fun putFloat(key: String, value: Float) {
        sharedPreferences.edit { putFloat(key, value) }
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    // 对象存储
    fun <T> putObject(key: String, obj: T?) {
        val json = if (obj != null) gson.toJson(obj) else null
        putString(key, json)
    }

    inline fun <reified T> getObject(key: String): T? {
        val json = getString(key) ?: return null
        return try {
            gson.fromJson(json, T::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // 列表存储
    fun <T> putList(key: String, list: List<T>?) {
        val json = if (list != null) gson.toJson(list) else null
        putString(key, json)
    }

    inline fun <reified T> getList(key: String): List<T>? {
        val json = getString(key) ?: return null
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<T>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    // 工具方法
    fun contains(key: String): Boolean = sharedPreferences.contains(key)

    fun remove(key: String) {
        sharedPreferences.edit { remove(key) }
    }

    fun clear() {
        sharedPreferences.edit { clear() }
    }

    fun getAllKeys(): Set<String> = sharedPreferences.all.keys
}