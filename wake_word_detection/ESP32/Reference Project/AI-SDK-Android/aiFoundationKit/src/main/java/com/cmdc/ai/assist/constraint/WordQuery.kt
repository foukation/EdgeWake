package com.cmdc.ai.assist.constraint

/**
 * WordQuery API 请求体参数数据类
 */
data class ChineseCharacterRequest(

    val word: String

)

/**
 * WordQuery API 响应数据结构
 */
data class ChineseCharacterResponse(

    val code: Int,
    val data: CharacterData,
    val msg: String

)

/**
 * 汉字详细信息
 */
data class CharacterData(
    /**
     * 汉字文字
     * */
    val name: String,
    /**
     * 汉字结构
     * */
    val struct: String,
    /**
     * 汉字拼音，由于可能有多音字，所以这里是多个，如下 voice、definition 字段也是一样的
     * */
    val pinyin: List<String>,
    /**
     * 读音音频地址
     * */
    val voice: List<String>,
    /**
     * 名词释义
     * */
    val definition: List<String>
)