package com.example.device_control

/**
 *创建者：ZyOng
 *描述：domain 判断
 *创建时间：2025/6/20 3:35 PM
 */
enum class IntentDomain (val alias: String) {
    TELECOM_SERVICE("TelecomService"),
    HEALTHCARE("Healthcare"),
    MEMBERSHIP("Membership"),
    CUSTOMERSERVICE("CustomerService"),
}

enum class TelecomServiceInstruction (val intent: String) {
    TELECOMSERVICE_MOBILEPHONE("TelecomService.MobilePhonePlan"),
    CUSTOMERSERVICE_EXCLUSIVE("CustomerService.Exclusive"),
    MEMBERSHIP_GOTONE("Membership.GoTone"),
    HEALTHCARE_LOVEHOME("Healthcare.LoveHome"),


}

enum class TelecomServiceAction (val ation: String) {
    ACTION_RECHARGEPHONECREDIT("RechargePhoneCredit"),
    ACTION_CHECKBALANCE("CheckBalance"),
    ACTION_CHECKDATAUSAGE("CheckDataUsage"),
    ACTION_SEND("Send"),
    ACTION_CALL("Call"),
    ACTION_VIDEOCALL("VideoCall"),
    ACTION_RIGHTSINQUIRY("RightsInquiry"),
    ACTION_CONSULTATION("Consultation"),
}

enum class AlarmIntent(val intent: String){
    ALARM_MANAGE("Alarm.Manage")
}

enum class MediaIntent(val intent: String){
    MEDIA_VIDEOPLAY("Media.VideoPlay"),
    MEDIA_MUSICPLAY("Media.MusicPlay"),
    MEDIA_UNICASTPLAY("Media.UnicastPlay"),
}