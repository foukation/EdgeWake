package com.fxzs.lingxiagent.lingxi.main.entity

enum class Type (val alias: Int) {
    COMMON(1),   // 普通
    APP(2),      // 打开APP
    TOAST(3),    // 关闭弹框
    TAB(4),      // 切换或跳过首广告
    OCR(5),      // OCR设别
    DEEP_LINK(6),// 系统动作
}

enum class State (val alias: Int) {
    INIT(0),        // 初始状态
    WATCH_ING(1),   // 控件匹配或OCR识别中
    WATCH_SUC(2),   // 控件匹配或OCR识别结束
    WATCH_OUT_TIME(3),  // 控件匹配或OCR识别超时
    EXEC_PAUSE(4),  // 暂停执行
    EXEC_ERR(5),    // 执行错误
    EXEC_SUC(6),    // 执行完成
}

enum class MatchType (val alias: Int) {
    OR(1),   // 条件并集
    AND(2),  // 条件交集
}

enum class MatchMethod (val alias: Int) {
    ID(1),           // id
    TEXT(2),         // text
    DESC(3),         // desc
    TYPE(4),         // className
    CONTAIN_TEXT(5), // 包含text
    CONTAIN_DESC(6), // 包含desc
    EDITOR(7),       // editor
}

enum class ExecWay (val alias: Int) {
    TRY_CLICK(1),             // 尝试点击（捕获）
    CLICK_BY_XY(2),           // 通过坐标点击
    CLICK_BY_NODE(3),         // 通过控件点击 (针对不可点击节点)
    INPUT(4),                 // 输入
    CLICK_BY_NODE_LONG(5),    // 长按
    CLICK_BY_NODE_TWICE(7),   // 双击
    SWIPER(6),                // 滑动
}

enum class SlotType (val alias: Int) {
    LOCATION(1),              // 地区
    USER(2),                  // 联系人
    DATE(5)                   // 日期
}

enum class TaskType (val alias: Int) {
    WZA(1),              // 无障碍
    WZA_MODEL(2),        // 无障碍-模块
    SDK(3)               // SDK
}

