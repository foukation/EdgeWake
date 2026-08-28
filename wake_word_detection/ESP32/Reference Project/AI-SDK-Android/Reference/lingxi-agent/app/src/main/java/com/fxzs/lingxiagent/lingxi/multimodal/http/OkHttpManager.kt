package com.ai.multimodal.http

import okhttp3.Callback
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class OkHttpManager {

    private val TAG = OkHttpManager::class.simpleName.toString()

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
    })

    private val sslContext = SSLContext.getInstance("SSL").apply {
        init(null, trustAllCerts, SecureRandom())
    }

    val jsonRequestBody = fun(json: String): RequestBody {
        val mediaJson = "application/json; charset=utf-8".toMediaType()
        return json.toRequestBody(mediaJson)
    }

    private val defaultClient = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.TIMEOUT, TimeUnit.SECONDS)
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    private fun createHeaders(): Headers {
        return Headers.Builder()
            .add("Content-Type", "application/json; charset=utf-8")
            .add("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOiJmcm9tLXp4IiwiZXhwIjoxNzczOTc0NzM5LCJpc3MiOiJibHVlLWFnZW50In0.s8esQu5PEoBIb5EfiSbU7Xv0Kc9hS6UXRmou_ywudMM")
            .build()
    }

    fun <T> get(
        api: String,
        baseUrl: String,
        header: Headers? = createHeaders(),
        params: T?,
        callback: Callback
    ) {
        var httpUrl = "${baseUrl}${api}"
        val urlBuilder = httpUrl.toHttpUrlOrNull()?.newBuilder()
        if (params != null) {
            for ((key, value) in params as Map<*, *>) {
                urlBuilder?.addQueryParameter(key.toString(), value.toString())
            }
        }
        val finalUrl = urlBuilder?.build()?.toString() ?: httpUrl

        var headers: Headers? = header

        /*if (isGateway) {
            headers = createGateWayHeaders(finalUrl)
        } else if (ApiConfig.useAgent) {
            headers = createHeaders(finalUrl)
        }*/

        val request = Request.Builder()
            .url(finalUrl)
            .headers(headers!!)
            .get()
            .build()

        Timber.tag(TAG).d("request finalUrl : $finalUrl")
        Timber.tag(TAG).d("request headers : $headers")

        defaultClient.newCall(request).enqueue(callback)
    }

    fun <T> post(
        api: String,
        baseUrl: String,
        params: T,
        header: Headers? = createHeaders(),
        callback: Callback
    ) {

        var httpUrl = "${baseUrl}${api}"

        var requestBody = jsonRequestBody(params.toString())

        val request = Request.Builder()
            .url(httpUrl)
            .headers(header!!)
            .post(requestBody)
            .build()

        Timber.tag(TAG).d("request httpUrl : $httpUrl")
        Timber.tag(TAG).d("request headers : $header")
        Timber.tag(TAG).d("request params : $params")

        defaultClient.newCall(request).enqueue(callback)
    }

}
