package com.fxzs.lingxiagent.lingxi.core.utils

import com.fxzs.lingxiagent.lingxi.core.viewfinder.ViewFinder

/**
 * # exceptions
 * 异常类合集
 */

/**
 * 视图搜索失败异常
 */
class ViewNodeNotFoundException : Exception {
    constructor(finder: ViewFinder<*>)
        : super("ViewNodeNotFound: ${finder.finderInfo()}")

    constructor(msg: String) : super(msg)
}

class GestureCanceledException(
    val gestureDescription: AutoGestureDescription
) : RuntimeException()

class AutoServiceUnavailableException : RuntimeException() {

    override fun toString(): String {
        return "AutoServiceUnavailableException"
    }
}
