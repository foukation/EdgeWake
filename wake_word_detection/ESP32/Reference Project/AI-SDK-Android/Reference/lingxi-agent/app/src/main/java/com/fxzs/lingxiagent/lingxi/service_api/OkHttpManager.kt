package com.example.service_api

import android.annotation.SuppressLint
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object OkHttpManager {

    var clientToken = ""
    var publicIp = ""

    private val trustAllCerts = arrayOf<TrustManager>(
        @SuppressLint("CustomX509TrustManager")
        object : X509TrustManager {
            override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        }
    )

    private val sslContext = SSLContext.getInstance("SSL").apply {
        init(null, trustAllCerts, SecureRandom())
    }

    val defaultClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    fun post(url: String, params: String, callback: Callback) {
        val headers = Headers.Builder()
            .add("Authorization", "Bearer $clientToken")
            .add("Content-Type", "application/json; charset=utf-8")
            .add("X-Client-Ip", publicIp)
            .build()

        val jsonRequestBody = fun(json: String): RequestBody {
            val mediaJson = "application/json; charset=utf-8".toMediaType()
            return json.toRequestBody(mediaJson)
        }

        val request = Request.Builder()
            .url(url)
            .headers(headers)
            .post(jsonRequestBody(params))
            .build()

        defaultClient.newCall(request).enqueue(callback)
    }

    fun get(url: String, paramsStr: String,  callback: Callback) {
        val headers = Headers.Builder()
            .add("Authorization", "Bearer $clientToken")
            .add("Content-Type", "application/json; charset=utf-8")
            .add("X-Client-Ip", publicIp)
            .build()

        val httpUrl = "${url}${paramsStr}"

        val request = Request.Builder()
            .url(httpUrl)
            .headers(headers)
            .get()
            .build()

        defaultClient.newCall(request).enqueue(callback)
    }

    fun delete(url: String, delId: String,  callback: Callback) {
        val headers = Headers.Builder()
            .add("Authorization", "Bearer $clientToken")
            .add("Content-Type", "application/json; charset=utf-8")
            .add("X-Client-Ip", publicIp)
            .build()

        val httpUrl = "${url}/${delId}"

        val request = Request.Builder()
            .url(httpUrl)
            .headers(headers)
            .delete()
            .build()

        defaultClient.newCall(request).enqueue(callback)
    }

    fun llmPost(url: String, params: String, callback: Callback) {
        val headers = Headers.Builder()
            .add("Content-Type", "application/json; charset=utf-8")
            .build()

        val jsonRequestBody = fun(json: String): RequestBody {
            val mediaJson = "application/json; charset=utf-8".toMediaType()
            return json.toRequestBody(mediaJson)
        }

        val request = Request.Builder()
            .url(url)
            .headers(headers)
            .post(jsonRequestBody(params))
            .build()

        defaultClient.newCall(request).enqueue(callback)
    }

}
