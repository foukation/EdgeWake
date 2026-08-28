package com.fxzs.lingxiagent.lingxi.main.utils

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException


/**
 * 解决Map解析时，int转double的问题
 */
class MapTypeAdapter : TypeAdapter<Any?>() {
    private val delegate: TypeAdapter<Any> = Gson().getAdapter(Any::class.java)


    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Any? {
        val token = `in`.peek()
        when (token) {
            JsonToken.BEGIN_ARRAY -> {
                val list: MutableList<Any?> = ArrayList()
                `in`.beginArray()
                while (`in`.hasNext()) {
                    list.add(read(`in`))
                }
                `in`.endArray()
                return list
            }

            JsonToken.BEGIN_OBJECT -> {
                val map: MutableMap<String, Any?> = LinkedTreeMap()
                `in`.beginObject()
                while (`in`.hasNext()) {
                    map[`in`.nextName()] = read(`in`)
                }
                `in`.endObject()
                return map
            }

            JsonToken.STRING -> return `in`.nextString()

            JsonToken.NUMBER -> {
                /**
                 * 改写数字的处理逻辑，将数字值分为整型与浮点型。
                 */
                val s = `in`.nextString()
                return if (s.contains(".")) {
                    s.toDouble()
                } else {
                    try {
                        s.toInt()
                    } catch (e: Exception) {
                        s.toLong()
                    }
                }
            }

            JsonToken.BOOLEAN -> return `in`.nextBoolean()

            JsonToken.NULL -> {
                `in`.nextNull()
                return null
            }

            else -> throw IllegalStateException()
        }
    }

    @Throws(IOException::class)
    override fun write(out: JsonWriter?, value: Any?) {
        delegate.write(out, value)
    }
}