package com.fxzs.lingxiagent.lingxi.gui_agent.entity

// CaseItem.kt
data class CaseItem(
    var content: String // var 表示可修改，若只读用 val
)

// CaseCategory.kt
data class CaseCategory(
    var title: String,   // 分类标题
    var items: List<CaseItem> // 分类下的条目列表
)