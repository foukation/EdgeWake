package com.cmdc.ai.assist.demo.adapter

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
)

data class AgentResult (
    val result: Boolean,
    val errMsg: String? = "",
    val sucMsg: String? = "",
)


