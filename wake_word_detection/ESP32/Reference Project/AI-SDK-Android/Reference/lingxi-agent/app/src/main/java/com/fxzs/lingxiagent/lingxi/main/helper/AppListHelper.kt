package com.fxzs.lingxiagent.helper

import com.example.service_api.IntentionApi
import com.fxzs.lingxiagent.lingxi.service_api.data.AppData
import com.fxzs.lingxiagent.lingxi.service_api.data.ClientApiAppListRes


object  AppListHelper {
    var appInfoList: ArrayList<AppData>? = null

    fun getAppList() {
        IntentionApi.handlerRequestClientAppList(
            onSuccess = fun (response: ClientApiAppListRes) {
                appInfoList = response.data.list
            },
            onError = fun (_: String) {
            }
        )
    }

    fun setAppList(appInfoList: ArrayList<AppData>){
        this.appInfoList = appInfoList
    }
}