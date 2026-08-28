package com.cmdc.ai.assist.constraint

import org.json.JSONObject

/**
 * ASR 智能对话响应体
 * */
data class DialogueResult(

    /**
     * 用于标识某个特定的问题或查询
     *
     * */
    val qid: String,

    /**
     * 定义一个名为 is_end 的整型变量，用于表示是否结束，0通常表示未结束状态
     * */
    val is_end: Int = 0,

    /**
     * 智能助手完整的回答内容
     * */
    val assistant_answer_content: String? = null,

    /**
     * 返回通用渲染指令
     * */
    val header: JSONObject? = null,

    /**
     * 返回具体指令对应的字段信息
     * */
    val payload: JSONObject? = null,

    val completion_tokens: Int = 0,

    val debug: Any? = null

)