package com.cmdc.ai.assist.http.error

import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber

/**
 * 业务异常：表示服务端返回的"权益/计费/鉴权"等业务级错误。
 *
 * 与 [java.io.IOException]、[java.lang.Exception] 等网络/解析异常相区分：
 * - 网络异常：连接超时、DNS 失败、连接被重置等，应提示"网络异常"
 * - 业务异常：服务端按约定返回的失败响应（如套餐用尽 code=6403），应弹出 [msg]
 *
 * 上层调用方在 `onError(e: Exception)` 回调中通过 `if (e is BizException)` 判断分流：
 * ```kotlin
 * onError = { e ->
 *     if (e is BizException && e.code in 6000..6999) {
 *         showBillingDialog(e.code, e.msg)   // 弹窗：套餐用尽等
 *     } else {
 *         toast("网络异常")
 *     }
 * }
 * ```
 *
 * @property code 服务端返回的业务错误码（如 6403）
 * @property msg  服务端返回的可直接面向用户的提示文案
 * @property rawJson 服务端返回的完整 JSON 文本，便于上层按需解析 `data` 等扩展字段
 * @param cause 原始异常（可空），便于排查链路
 */
class BizException(
    val code: Int,
    val msg: String,
    val rawJson: String,
    cause: Throwable? = null
    // 把 rawJson 也拼进父类 message，便于直接通过 Log.e(tag, e) / e.toString() 一并输出完整业务体，
    // 排查 data 中 errorCode、serviceKey 等扩展字段时无需再单独取 [BizException.rawJson]。
) : Exception("biz error: code=$code, msg=$msg, rawJson=$rawJson", cause)

/**
 * 业务错误解析器。
 *
 * ## 背景
 * 当服务端接入"权益（计费/套餐/鉴权）"校验时，可能在以下三种通道里返回业务错误：
 * 1. **普通 HTTP**：以 4xx/5xx 状态 + `Content-Type: application/json` 返回错误体
 *    （如 `{"code":6403,"msg":"套餐用尽","data":{...}}`）
 * 2. **SSE 流式**：在握手或响应阶段返回 `Content-Type: application/json` 而非
 *    `text/event-stream`，OkHttp-SSE 会抛 `IllegalStateException("Invalid content-type: ...")`
 *    并触发 `EventSourceListener.onFailure(es, t, response)`，response.body 即业务错误体
 * 3. **WebSocket**：建立后通过 `onMessage(text)` 推送 `{"code":6403,...}` 再 close
 *
 * 旧代码在各通道的 `onFailure` 中通常只做 `onError(Exception(t))`，把 `response` 整个
 * 丢弃，导致上层无法区分"网络错误"与"业务错误"。本工具用于统一这套识别逻辑：
 *
 * - [parse]      ——  传入 OkHttp [Response]（SSE / 普通 HTTP 通用），命中返回 [BizException]，否则返回 `null`
 * - [parseText]  ——  传入 WebSocket 文本帧，命中返回 [BizException]，否则返回 `null`
 *
 * ## 业务码约定
 * 与服务端统一约定：**`code == 0` 表示成功，任何非零值均视为业务错误**。
 *
 * 已知业务码示例：
 * - `401` 身份认证错误
 * - `403` 权限不足
 * - `6000..6999` 权益/计费/套餐类失败
 *
 * 客户端不维护业务码白名单：后端新增业务码（如 429 限流、
 * 5xxx 服务不可用等）时，本模块**零改动**自动接住，上层只需凭
 * [BizException.code] 自行分流处理。
 */
object BizErrorParser {

    private const val TAG = "BizErrorParser"

    /**
     * 从 OkHttp 响应中尝试解析业务错误。
     *
     * 适用通道：
     * - 普通 HTTP 的 `Callback.onResponse`（建议先判 `!response.isSuccessful` 再调用）
     * - 普通 HTTP 的 `Callback.onFailure`（通常 response 为 null，本方法会安全返回 null）
     * - SSE 的 `EventSourceListener.onFailure(es, t, response)`
     *
     * **注意**：[okhttp3.ResponseBody.string] 只能读取一次，本方法读取后调用方不应再访问 body。
     *
     * @param response OkHttp 响应对象，可空（如普通 HTTP 的 IO 失败回调中 response 不可得）
     * @param cause    原始异常，作为 [BizException.cause] 透传，便于链路排查
     * @return 命中业务码段位时返回 [BizException]；否则返回 `null`，调用方应回退到原网络错误处理
     */
    fun parse(response: Response?, cause: Throwable? = null): BizException? {
        if (response == null) return null
        return try {
            // 仅当响应体声明为 JSON 时才尝试解析，避免把二进制/纯文本错误体当作 JSON
            val contentType = response.header("Content-Type").orEmpty()
            if (!contentType.contains("application/json", ignoreCase = true)) {
                return null
            }
            // ResponseBody.string() 只能读一次，读完后 body 不再可用
            val body = response.body?.string().orEmpty()
            parseJson(body)
        } catch (e: Exception) {
            Timber.tag(TAG).w("[parse] 解析响应体失败: ${e.message}")
            null
        }?.let { biz ->
            // 把原始 cause 透传到 BizException，便于排查
            if (cause != null) BizException(biz.code, biz.msg, biz.rawJson, cause) else biz
        }
    }

    /**
     * 从 WebSocket 文本帧中尝试解析业务错误。
     *
     * 适用通道：
     * - `WebSocketListener.onMessage(webSocket, text)` 入口
     *
     * @param text WebSocket 收到的文本帧内容
     * @return 命中业务码段位时返回 [BizException]；否则返回 `null`
     */
    fun parseText(text: String?): BizException? {
        if (text.isNullOrBlank()) return null
        return try {
            parseJson(text)
        } catch (e: Exception) {
            Timber.tag(TAG).w("[parseText] 解析文本帧失败: ${e.message}")
            null
        }
    }

    /**
     * 内部统一 JSON 解析逻辑：
     * 1. 解析顶层 `code` 字段（缺省 0）
     * 2. 按服务端约定判断：非 0 即业务错误
     * 3. 命中则构造 [BizException]，未命中返回 `null`
     */
    private fun parseJson(bodyText: String): BizException? {
        if (bodyText.isBlank()) return null
        val json = JSONObject(bodyText)
        val code = json.optInt("code", 0)
        if (!isBizCode(code)) return null
        val msg = json.optString("msg")
        /*Timber.tag(TAG).e("[parseJson] 命中业务错误: code=$code, msg=$msg")*/
        return BizException(code = code, msg = msg, rawJson = bodyText)
    }

    /**
     * 判断给定错误码是否属于业务错误。
     *
     * 服务端约定：`code == 0` 和 `code == 200` 表示成功，其余均为业务错误码。
     * 客户端不维护白名单，以适配服务端未来可能新增的业务码。
     */
    private fun isBizCode(code: Int): Boolean = code != 0 && code != 200
}
