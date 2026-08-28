package com.example.device_control

data class IntentsContent(
    var intents: ArrayList<IntentData>,
)

data class IntentData (
    var domain: String,
    var intent: String,
    var rewrite: String,
    var slotsSourceMap: SlotsData,
)

data class SlotsData (
    val Action: String?,
    val Name: String?,
    val Contact: String?,
    val Text: String?,
    val Page: String?,
    val Date:String?,
    val Time:String?,
    val TimeAPM:String?,
    val Repeat:String?,
    val AlarmTimeMinute:Int = 0,
    val AlarmTimeHour:Int = 0,
    val TimeHour :Int = 0,
    val TimeMinute:Int = 0,
    val Function:String?,
    val Light:String?,
    val Title:String?,
    val Amount:String?,
    val PhoneNumber:String?,
    val Brightness:String?

)

data class AgentResult (
    val result: Boolean,
    val errMsg: String? = "",
    val sucMsg: String? = "",
    val permitsStatus:Boolean = true,
    val permitsType:String? = ""
)


