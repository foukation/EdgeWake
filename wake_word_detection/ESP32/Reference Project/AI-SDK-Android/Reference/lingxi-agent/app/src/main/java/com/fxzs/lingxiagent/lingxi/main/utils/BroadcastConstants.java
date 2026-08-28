package com.fxzs.lingxiagent.lingxi.main.utils;

public class BroadcastConstants {
    // 语音识别相关
    public static final String ACTION_SPEECH_START = "com.fxzs.lingxiagent.KEY_SPEECH_START";

    public static final String ACTION_TRANSLATION_START = "com.fxzs.lingxiagent.KEY_TRANSLATION_START";

    public static final String ACTION_SPEECH_MID_RESULT = "com.fxzs.lingxiagent.KEY_MID_RESULT";
    public static final String ACTION_SPEECH_FINAL_RESULT = "com.fxzs.lingxiagent.KEY_FINAL_RESULT";
    public static final String ACTION_SEND = "com.fxzs.lingxiagent.KEY_SEND";
    public static final String ACTION_SHOW_KEYBOARD = "com.fxzs.lingxiagent.SHOW_KEYBOARD";
    // 多模态相关
    public static final String ACTION_IMAGE_URI_RESPONSE = "com.ai.multimodal.imageUriResponse";
    public static final String ACTION_TEX_RESPONSE = "com.ai.multimodal.textResponse";
    // 通用状态
    public static final String ACTION_COMPLETE = "com.fxzs.lingxiagent.complete";
    public static final String ACTION_COMPLETE_SENDTRIPCHATMESSAGE = "com.fxzs.lingxiagent.sendtripchatmessage";
    public static final String ACTION_COMPLETE_SENDTRIPCARD = "com.fxzs.lingxiagent.sendtripcard";
    public static final String ACTION_CHAT_TYPE_WRITE = "com.fxzs.lingxiagent.chatTypeWrite";
    public static final String ACTION_CHAT_TYPE_COT = "com.fxzs.lingxiagent.sentCot";
    public static final String ACTION_ACCESSIBILITYCARD = "com.fxzs.lingxiagent.accessibilitycard";

    public static final String ACTION_SCROLL_BOTTOM = "com.fxzs.lingxiagent.scrollbottom";
    public static final String ACTION_AUTO_HEIGHT = "com.fxzs.lingxiagent.autoheight";
    public static final String ACTION_CLOSE_ASR = "com.fxzs.lingxiagent.closeasr";

    public static final String ACTION_TEXT_REPLY = "com.fxzs.lingxiagent.textReply";
    public static final String ACTION_IMG_REPLY = "com.fxzs.lingxiagent.imgReply";
    public static final String ACTION_IMGS_REPLY = "com.fxzs.lingxiagent.imgsReply";
    public static final String ACTION_MEDIA_REPLY = "com.fxzs.lingxiagent.mediaReply";
    public static final String ACTION_TRIP_CARD_REPLY = "com.fxzs.lingxiagent.tripCardReply";
    public static final String ACTION_TRAVEL_HTML_REPLY = "com.fxzs.lingxiagent.travelHtmlReply";

    public static final String ACTION_SEND_TEXT = "com.fxzs.lingxiagent.send.action.text";
    public static final String MESSAGE = "message";
    public static final String ACTION_STOP_SENDTRIPCARD = "com.fxzs.lingxiagent.stopHonorCard";
    
    // GUI Agent 任务状态
    public static final String ACTION_TASK_EXECUTING = "com.fxzs.lingxiagent.task.executing";
    public static final String ACTION_TASK_COMPLETED = "com.fxzs.lingxiagent.task.completed";
}
