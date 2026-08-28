package com.fxzs.lingxiagent.lingxi.main.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.Gravity
import android.widget.Toast
import com.cmdc.ai.assist.api.AIFoundationKit
import com.cmdc.ai.assist.constraint.TextToAudioRequest
import com.fxzs.lingxiagent.IYAApplication
import java.util.UUID


object Utils {
    fun getUUID(): String {
        return UUID.randomUUID().toString()
    }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            IYAApplication.getInstance().startActivity(intent)
        } catch (_: Exception) {
        }
    }
    
    fun openThirdPartyApp(context: Context, packageName: String, activityClassName: String) {
        val intent = Intent()
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
        intent.setComponent(ComponentName(packageName, activityClassName))
        context.startActivity(intent)
    }

    fun showToastTop(context: Activity, text: String) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                val t= CustomToast.showToast(context, text, Toast.LENGTH_SHORT)
                t.setGravity(Gravity.TOP,0,0)
                t.show()
            }
        } else {
            val t= CustomToast.showToast(context, text, Toast.LENGTH_SHORT)
            t.setGravity(Gravity.TOP,0,0)
            t.show();
        }
    }

    fun loadImageAsBitmap(context: Context, fileName: String): Bitmap? {
        return try {
            context.assets.open(fileName).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun String.removeLineBreaks(): String {
        return this.filterNot { it.isWhitespace() }
    }


    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val serviceList = activityManager.getRunningServices(Integer.MAX_VALUE)

        for (service in serviceList) {
            if (serviceClass.name.equals(service.service.className)) {
                return true
            }
        }
        return false
    }

    fun subStr(str:String, len:Int):String {
        return if (str.length > len) {
            str.substring(0, len) + "..."
        } else {
            str
        }
    }

    private val aiFoundationKit by lazy {
        AIFoundationKit()
    }

     fun textToAudio(input: String, textToAudioRequestCallBack: TextToAudioRequestCallBack) {
         aiFoundationKit.textToAudio(
             TextToAudioRequest(
                 text = input,
                 spd = 5,
                 pit = 5,
                 vol = 5,
                 aue = 3
             ),
             { response ->
                 textToAudioRequestCallBack.requestCallBack(
                     UUID.randomUUID().toString(),
                     response.data?.absolutePath ?: ""
                 )
             }, {
                 textToAudioRequestCallBack.requestCallBack(UUID.randomUUID().toString(), "")
             }
         )
     }

    interface TextToAudioRequestCallBack {
        fun requestCallBack(uuid: String, absolutePath: String)
    }

    fun getProcessName(context: Context): String? {
        val pid = Process.myPid()
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (manager != null) {
            for (processInfo in manager.runningAppProcesses) {
                if (processInfo.pid == pid) {
                    return processInfo.processName
                }
            }
        }
        return null
    }
}