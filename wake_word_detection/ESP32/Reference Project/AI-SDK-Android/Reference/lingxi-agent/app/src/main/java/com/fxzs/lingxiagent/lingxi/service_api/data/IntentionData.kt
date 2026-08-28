package com.example.service_api.data

data class ActionResponse(
    var intents: ArrayList<IntentContent>
)

data class IntentContent (
    var intent: String,
    var slots: SlotsContent,
)

data class SlotsContent (
    val product: String?,
    val item: String?,
    val recipient: String?,
    val location: String?,
    val messageContent: String?,
    val destination: String?,
    val drinkName: String?,
    val shop: String?,
    val receiver: String?,
    val content: String?,
    var store: String?,
    var times: String?,
    var hotel: String?
)

