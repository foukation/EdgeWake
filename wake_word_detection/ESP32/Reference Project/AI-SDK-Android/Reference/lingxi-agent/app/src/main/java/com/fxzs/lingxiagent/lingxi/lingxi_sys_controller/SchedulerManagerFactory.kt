package com.example.device_control

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.device_control.data.AppData
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.lang.reflect.Type;


/**
 *创建者：ZyOng
 *描述：
 *创建时间：2025/7/4 上午11:31
 */
class SchedulerManagerFactory (active: Activity){
    private var context = active
    private var intentStr = ""
    private var domain:String = ""
    private var schedulerManager: SchedulerManager? = null
    private var mediaManager :MediaManager? = null

    var appInfoList: ArrayList<AppData>? = null
    fun setAppList(appInfo: String){
        val gson = GsonBuilder().setPrettyPrinting().create()
        val listType: Type = object : TypeToken<ArrayList<AppData?>?>() {}.type
        this.appInfoList = gson.fromJson(appInfo, listType)
    }
    fun updateIntent(intentStr:String,domain:String){
        this.intentStr = intentStr
        this.domain = domain
    }
    fun start():AgentResult{
        Timber.d("系统控制_意图内容: $intentStr")
         if (domain == "Media"){
            if (mediaManager == null){
                mediaManager = MediaManager(context)
            }
            mediaManager?.updateIntent(intentStr)
            return mediaManager?.start() ?:AgentResult(false)

        }else{
             if (schedulerManager == null){
                 schedulerManager = SchedulerManager(context)
                 schedulerManager?.setAppList(appInfoList)
             }
             schedulerManager?.updateIntent(intentStr)
             return schedulerManager?.start() ?:AgentResult(false)
        }
    }
}