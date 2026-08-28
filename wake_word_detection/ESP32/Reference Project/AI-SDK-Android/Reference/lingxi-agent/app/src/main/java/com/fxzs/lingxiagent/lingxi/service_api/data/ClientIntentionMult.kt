package com.example.service_api.data

enum class RespResultStr (val alias: String) {
    ING("InProgress"),
    DOWN("Completed"),
}

data class ClientApiActionsResMul(
    val code: Int,
    val msg: String,
    val data: AgentTaskDataMul,
)

data class AgentTaskDataMul(
    val id: Int,
    val desc: String,
    val appId: String,
    val typeId: Int,
    val appName: String,
    val queryIntent: String,
    val actionList: String?,
    var execNum: Int,
    val intents: ArrayList<IntentData>?,
    val mulIntent: MulIntentData
)

data class MulIntentData(
    val ResponseText: String,
    val SessionID: String,
    val IsSingleTurn: Boolean,
    val IntentCategory: String,
    val StandardSchema: StandardSchemaData,
    val RecognizedJSON: RecognizedJSON,
    val Status: String,
    val Error: String
)

data class StandardSchemaData(
    val intents: ArrayList<IntentsData>,
)

data class RecognizedJSON(
    val intents: ArrayList<IntentsData>,
)

data class  IntentsData(
    val slots: SlotsDataContent,
    val intent: String,
)

data class  SlotsDataContent(
    val 出发地: String?,
    val 目的地: String?,
    val 出发日期: String?,
    val 车次类型: String?,
)

