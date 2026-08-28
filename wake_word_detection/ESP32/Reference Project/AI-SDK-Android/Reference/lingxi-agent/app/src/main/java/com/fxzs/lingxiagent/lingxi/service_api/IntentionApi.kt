package com.example.service_api

import android.os.StrictMode
import com.example.service_api.config.ApiUrl
import com.example.service_api.data.ClientApiActionsRes
import com.example.service_api.data.ClientApiActionsResMul
import com.example.service_api.data.ClientApiGetTokenRes
import com.example.service_api.data.ClientTimeData
import com.example.service_api.data.ClientTimeRes
import com.example.service_api.data.IsOcrResult
import com.example.service_api.data.LLmQueryParams
import com.example.service_api.data.LlmQueryResult
import com.example.service_api.data.OcrResult
import com.example.service_api.data.QueryParams
import com.example.service_api.data.TripContentRes
import com.example.service_api.data.TripCreateRes
import com.example.service_api.data.TripDelRes
import com.fxzs.lingxiagent.lingxi.service_api.data.ClientApiAppListRes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object IntentionApi {

    private const val SIGN_CODE = "blue-agent"
    private val TAG = IntentionApi.javaClass.simpleName
    val gson: Gson = GsonBuilder()
        .create()

    /*
    * 获取公网IP
    * */
    fun handlerRequestPublicIp() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        try {
            val url = URL("http://checkip.amazonaws.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            OkHttpManager.publicIp = reader.readLine().trim { it <= ' ' }
            reader.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /*
    * 获取设备Id
    * */
    fun handlerRequestDeviceId() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        try {
            val url = URL("http://checkip.amazonaws.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            OkHttpManager.publicIp = reader.readLine().trim { it <= ' ' }
            reader.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /*
    * 获取客户端token
    * */
    fun handlerRequestClientToken(
        onSuccess: () -> Unit,
    ) {
        val paramsStr = "?clientId=$SIGN_CODE"
        OkHttpManager.get(
            ApiUrl.INTENTION_CLIENT_TOKEN,
            paramsStr,
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val respString = response.body?.string()!!
                    try {
                        val resp = gson.fromJson(respString, ClientApiGetTokenRes::class.java)
                        if (resp.code == 200) {
                            OkHttpManager.clientToken = resp.data
                            onSuccess()
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e.message.toString())
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    e.message?.let { Timber.tag(TAG).e(it) }
                }
            })
    }

    /*
     * 获取客户端适配App信息
     * */
    fun handlerRequestClientAppList(
        onSuccess: (ClientApiAppListRes) -> Unit,
        onError: (errMsg: String) -> Unit
    ) {
        val paramsStr = "?pageIndex=1&pageSize=50&status=1"
        OkHttpManager.get(
            ApiUrl.INTENTION_CLIENT_APP,
            paramsStr,
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val respString = response.body?.string()!!
                    try {
                        val resp = gson.fromJson(respString, ClientApiAppListRes::class.java)
                        if (resp.code == 200) {
                            onSuccess(resp)
                        } else {
                            onError(resp.msg)
                        }
                    } catch (e: Exception) {
                        onError("Apps数据解析错误，请检查数据")
                        Timber.tag(TAG).e(e.message.toString())
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError("无法连接到服务器，请检查网络")
                    e.message?.let { Timber.tag(TAG).e(it) }
                }
            })
    }

    /*
    * 获取客户端Action信息
    * */
    fun handlerRequestClientActions(
        inputStr: String,
        onSuccess: (ClientApiActionsRes) -> Unit,
        onError: (errMsg: String) -> Unit
    ) {
        val params = JSONObject()
        params.put("text", inputStr)
        OkHttpManager.post(
            ApiUrl.INTENTION_CLIENT_ACTIONS,
            params.toString(),
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val respString = response.body?.string()!!
                    println("actions=$respString")
                    try {
                        val resp = gson.fromJson(respString, ClientApiActionsRes::class.java)
                        if (resp.code == 200 && resp.data.intents != null) {
                            onSuccess(resp)
                        } else {
                            onError(resp.msg)
                        }
                    } catch (e: Exception) {
                        onError("Actions数据解析错误，请检查数据")
                        Timber.tag(TAG).e(e.message.toString())
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError("无法连接到服务器，请检查网络")
                    e.message?.let { Timber.tag(TAG).e(it) }
                }
            })
    }

    /*
    * 获取客户端Action（多轮对话）信息
    * */
    fun handlerRequestClientActionsMul(
        inputStr: String,
        sessionID: String,
        onSuccess: (ClientApiActionsResMul) -> Unit,
        onError: (errMsg: String) -> Unit
    ) {

        val params = JSONObject()
        params.put("text", inputStr)
        if(sessionID != "") {
            params.put("SessionID", sessionID)
        }

        OkHttpManager.post(
            ApiUrl.INTENTION_CLIENT_ACTIONS_MUL,
            params.toString(),
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val respString = response.body?.string()!!
                    println("actions=$respString")
                    try {
                        val resp = gson.fromJson(respString, ClientApiActionsResMul::class.java)
                        if (resp.code == 200) {
                            onSuccess(resp)
                        } else {
                            onError(resp.msg)
                        }
                    } catch (e: Exception) {
                        onError("Actions数据解析错误，请检查数据")
                        Timber.tag(TAG).e(e.message.toString())
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError("无法连接到服务器，请检查网络")
                    e.message?.let { Timber.tag(TAG).e(it) }
                }
            })
    }

    /*
    * 获取泛化时间
    * */
    fun handlerRequestClientNormalizeTime(
        timeDescription: String,
        onSuccess: (ClientTimeData) -> Unit,
        onError: (errMsg: String) -> Unit
    ) {
        val paramsStr = "?timeDescription=${timeDescription}"

        OkHttpManager.get(
            ApiUrl.INTENTION_CLIENT_NORMALIZE_TIME,
            paramsStr,
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val respString = response.body?.string()!!
                    println("timeResp=$respString")
                    try {
                        val resp = gson.fromJson(respString, ClientTimeRes::class.java)
                        if (resp.code == 200) {
                            onSuccess(resp.data)
                        } else {
                            onError(resp.msg)
                        }
                    } catch (e: Exception) {
                        onError("数据解析错误，请检查数据")
                        Timber.tag(TAG).e(e.message.toString())
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError("无法连接到服务器，请检查网络")
                    e.message?.let { Timber.tag(TAG).e(it) }
                }
            })
    }

    /*
    * 行程创建
    * */
    fun createTrip(inputStr: String, onSuccess: (TripCreateRes) -> Unit, onError: (errMsg: String) -> Unit) {
        val params = JSONObject()
        params.put("title", "行程规划")
        params.put("description", "行程规划")
        params.put("command", inputStr)

        OkHttpManager.post(ApiUrl.CREATE_TRIP_URL, params.toString(), object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val respString = response.body?.string()
                println("tripCreateResp=$respString")
                try {
                    val resp = gson.fromJson(respString, TripCreateRes::class.java)
                    onSuccess(resp)
                } catch (e: Exception) {
                    onError("任务创建失败，请稍后重试")
                    Timber.tag(TAG).e(e.message.toString())
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                onError("无法连接到服务器，请检查网络")
                e.message?.let { Timber.tag(TAG).e(it) }
            }
        })
    }

    /*
    * 获取行程列表
    * */
    fun getTripList(onSuccess: (TripContentRes) -> Unit, onError: (errMsg: String) -> Unit) {
        val paramsStr = "?pageNum=1&pageSize=50"

        OkHttpManager.get(ApiUrl.GET_TRIP_URL, paramsStr, object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val respString = response.body?.string()
                println("tripRespString=$respString")
                try {
                    val resp = gson.fromJson(respString, TripContentRes::class.java)
                    onSuccess(resp)
                } catch (e: Exception) {
                    onError("响应数据解析错误，请检查数据格式")
                    Timber.tag(TAG).e(e.message.toString())
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                onError("无法连接到服务器，请检查网络")
                e.message?.let { Timber.tag(TAG).e(it) }
            }
        })
    }

    /*
    * 删除行程
    * */
    fun delTrip(delId: String, onSuccess: (TripDelRes) -> Unit, onError: (errMsg: String) -> Unit) {
        OkHttpManager.delete(ApiUrl.DEL_TRIP_URL, delId, object : Callback {
            override fun onResponse(call: Call, response: Response) {
                println("tripDelRespString=$response")
                val respString = response.body?.string()
                try {
                    val resp = gson.fromJson(respString, TripDelRes::class.java)
                    onSuccess(resp)
                } catch (e: Exception) {
                    onError("响应数据解析错误，请检查数据格式")
                    Timber.tag(TAG).e(e.message.toString())
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                onError("无法连接到服务器，请检查网络")
                e.message?.let { Timber.tag(TAG).e(it) }
            }
        })
    }

    fun handlerOCR(params: QueryParams, onSuccess: (OcrResult) -> Unit, onError: (errMsg: String) -> Unit) {
        OkHttpManager.post(ApiUrl.OCR_URL, gson.toJson(params), object: Callback {
            override fun onResponse(call: Call, response: Response) {
                val respString = response.body?.string()
                println("respString=$respString")
                try {
                    val resp = gson.fromJson(respString , OcrResult::class.java)
                    onSuccess(resp)
                } catch (e: Exception) {
                    onError("响应数据解析错误，请检查数据格式")
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                onError("无法连接到服务器，请检查网络")
            }
        })
    }

    fun isAdditionOcr(params: QueryParams, onSuccess: (IsOcrResult) -> Unit, onError: (errMsg: String) -> Unit) {
        OkHttpManager.post(ApiUrl.OCR_URL, gson.toJson(params), object: Callback {
            override fun onResponse(call: Call, response: Response) {
                val respString = response.body?.string()
                println("respString=$respString")
                try {
                    val resp = gson.fromJson(respString , IsOcrResult::class.java)
                    onSuccess(resp)
                } catch (e: Exception) {
                    onError("响应数据解析错误，请检查数据格式")
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                onError("无法连接到服务器，请检查网络")
            }
        })
    }

    fun handlerLlmAction(params: LLmQueryParams, onSuccess: (LlmQueryResult) -> Unit, onError: (errMsg: String) -> Unit){
        OkHttpManager.llmPost(ApiUrl.LLM_URL, gson.toJson(params), object: Callback {
            override fun onResponse(call: Call, response: Response) {
                val respString = response.body?.string()
                try {
                    val resp = gson.fromJson(respString , LlmQueryResult::class.java)
                    onSuccess(resp)
                } catch (e: Exception) {
                    onError("响应数据解析错误，请检查数据格式")
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                onError("无法连接到服务器，请检查网络")
            }
        })

    }
}