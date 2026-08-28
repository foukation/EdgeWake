package com.fxzs.lingxiagent.lingxi.core.viewfinder

import com.fxzs.lingxiagent.lingxi.core.viewnode.ViewNode

/**
 * # ScreenTextFinder
 */
class ScreenTextFinder(
    node: ViewNode? = null
) : ViewFinder<ScreenTextFinder>(node) {
    override fun finderInfo() = "ScreenTextFinder"

    var isWeb = false

    override fun findCondition(node: AcsNode): Boolean {
        if (node.className?.endsWith("WebView", ignoreCase = true) == true) {
            isWeb = true
            return false
        }
        return ((node.childCount == 0) && (node.text != null && node.text.trim() != "")
            || (isWeb && (node.contentDescription ?: "") != ""))
    }

}