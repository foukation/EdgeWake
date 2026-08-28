package com.fxzs.lingxiagent.lingxi.service_api.data

data class AppData(
    val id: Int,
    val name: String,
    val enName: String,
    val packageName: String,
    val homeActivity: String,
)

data class AppListData(
    val count: Int,
    val pageIndex: Int,
    val pageSize: Int,
    val list: ArrayList<AppData>
)

data class ClientApiAppListRes(
    val code: Int,
    val msg: String,
    val data: AppListData,
)
