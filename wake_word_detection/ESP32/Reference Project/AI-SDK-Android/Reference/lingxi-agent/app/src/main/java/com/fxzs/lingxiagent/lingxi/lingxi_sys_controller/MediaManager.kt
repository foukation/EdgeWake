package com.example.device_control

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.device_control.data.Extra
import com.example.device_control.data.MiGuData
import com.example.device_control.data.Params
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 *创建者：ZyOng
 *描述：
 *创建时间：2025/7/4 上午11:28
 */
class MediaManager(active: Activity)  :BaseManager(){

    private var context = active
    private var intentStr = ""
    fun updateIntent(intentStr:String){
        this.intentStr = intentStr
    }

    override fun start(): AgentResult {
        val intentResult = GsonBuilder().create()?.fromJson("{\"intents\":${intentStr}}", IntentsContent::class.java)
        val intent = intentResult?.intents?.get(0)
        val domain = intent?.domain ?:""
        val title = intent?.slotsSourceMap?.Title ?:""
        when (intent?.intent) {
            MediaIntent.MEDIA_MUSICPLAY.intent -> {
               return AgentResult(false,"暂时不支持相关操作")
            }
            MediaIntent.MEDIA_VIDEOPLAY.intent -> {
                val extra = Extra(true,title,"Home_Default_Search_Text")
                val params = Params(extra,"HOME_SEARCH")
                val miGuData = MiGuData(params, type = "JUMP_INNER_NEW_PAGE")
                val gson = Gson()
                val result = gson.toJson(miGuData)
                return openMiGuv(result)
            }
            MediaIntent.MEDIA_UNICASTPLAY.intent -> {
                return AgentResult(false,"暂时不支持相关操作")
            }
        }
        return AgentResult(false)
    }


    /*
    * 打开咪咕
    * */
    fun openMiGuv(action:String): AgentResult {
        if (!isChinaMobileInstalled("miguvideo://miguvideo",context)){
            return AgentResult(false,"当前需要咪咕视频才能继续哦～去应用商店下载后，就能正常使用啦")
        }
        val url = "miguvideo://miguvideo?action="+PullTerminalUtils.urlEncode(action)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        return AgentResult(true, sucMsg = "已为您打开咪咕视频App")

    }

}