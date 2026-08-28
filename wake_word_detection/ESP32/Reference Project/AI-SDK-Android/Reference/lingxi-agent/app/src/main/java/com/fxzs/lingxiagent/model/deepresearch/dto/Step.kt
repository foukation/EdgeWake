package com.fxzs.lingxiagent.model.deepresearch.dto

private var step = 0 //1:thinking 2: think complete 3: web_search 4:reporting 5:report complete

enum class Step (val alias: String){
    THINKING("thinking"),
    THINK_COMPLETE("think_complete"),
    WEB_SEARCH("web_search"),
    REPORTING("reporting"),
    REPORT_COMPLETE("report_complete")
}
