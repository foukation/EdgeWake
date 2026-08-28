@file:Suppress("unused")

package com.fxzs.lingxiagent.lingxi.core.api

import android.os.Build
import androidx.annotation.RequiresApi
import com.fxzs.lingxiagent.lingxi.core.utils.times
import com.fxzs.lingxiagent.lingxi.core.viewfinder.AcsNode
import com.fxzs.lingxiagent.lingxi.core.viewfinder.ConditionGroup
import com.fxzs.lingxiagent.lingxi.core.viewfinder.SF
import com.fxzs.lingxiagent.lingxi.core.viewfinder.SmartFinder
import com.fxzs.lingxiagent.lingxi.core.viewfinder.ViewFinder
import com.fxzs.lingxiagent.lingxi.core.viewfinder.containsText
import com.fxzs.lingxiagent.lingxi.core.viewfinder.desc
import com.fxzs.lingxiagent.lingxi.core.viewfinder.editable
import com.fxzs.lingxiagent.lingxi.core.viewfinder.focused
import com.fxzs.lingxiagent.lingxi.core.viewfinder.id
import com.fxzs.lingxiagent.lingxi.core.viewfinder.matchText
import com.fxzs.lingxiagent.lingxi.core.viewfinder.text
import com.fxzs.lingxiagent.lingxi.core.viewfinder.type
import com.fxzs.lingxiagent.lingxi.core.viewnode.ViewNode
import timber.log.Timber

/**
 * # apis
 * 快捷api
 */


/**
 * id 搜索
 * @param id String
 * @return ViewFindBuilder
 */

fun withId(id: String): ConditionGroup {
    return SF.id(id)
}

/**
 * 文本全匹配
 * @param text Array<out String>
 * @return ViewFindBuilder
 */
fun withText(vararg text: String): ConditionGroup {
    return SF.text(*text)
}

/**
 * class 类型
 * eg:
 * 1. TextView
 * 2. android.widget.TextView
 *
 * @param types Array<out String>
 * @return ViewFindBuilder
 */
fun withType(vararg types: String): ConditionGroup {
    return SF.type(*types)
}

/**
 * desc
 * @param desc Array<out String>
 * @return ViewFindBuilder
 */
fun withDesc(vararg desc: String): ConditionGroup {
    return SF.desc(*desc)
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
fun editor(): ConditionGroup {
    return SF.editable(true)
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
fun editored(): ConditionGroup {
    return SF.editable(true).focused(true)
}

/**
 * 视图深度
 * @param depths Array<Int>
 * @return ViewFindBuilder
 */
suspend fun withDepths(vararg depths: Int): ViewNode? {
    return ViewFinder.findByDepths(*depths)
}

/**
 * 文本包含
 * @param text String
 * @return ViewFindBuilder
 */
fun containsText(vararg text: String): ConditionGroup {
    return SF.containsText(*text)
}


/**
 * 正则匹配
 * @param reg Array<out String>
 * @return ViewFindBuilder
 */
fun matchesText(reg: String): ConditionGroup {
    return SF.matchText(reg)
}

/**
 * 输出布局
 */
fun printLayoutInfo(includeInvisible: Boolean = true) {
    ViewNode.getRoot().printWithChild(0, 0, includeInvisible)
}

private fun ViewNode?.printWithChild(
    index: Int,
    dep: Int,
    includeInvisible: Boolean
) {
    if (this == null) {
        Timber.tag("ViewNode").d("*" * dep + "[" + index + "] null")
        return
    }
    if (!includeInvisible && !isVisibleToUser) {
        Timber.tag("ViewNode").w("*" * dep + "[" + index + "] " + "InVisible")
        return
    }
    if (isVisibleToUser) {
        Timber.tag("ViewNode").d("*" * dep + "[" + index + "] " + toString())
    } else {
        Timber.tag("ViewNode").w("*" * dep + "[" + index + "] " + toString())
    }
    children.forEachIndexed { i, it ->
        it.printWithChild(i, dep + 1, includeInvisible)
    }
}

suspend fun findWith(
    includeInvisible: Boolean = false,
    predicate: (AcsNode) -> Boolean
): ViewNode? {
    return SF.where(predicate).findFirst(includeInvisible)
}

suspend fun findAllWith(
    includeInvisible: Boolean = false,
    predicate: (AcsNode) -> Boolean
): Array<ViewNode> {
    return SF.where(predicate).findAll(includeInvisible)
}

suspend fun ViewNode.findWith(
    includeInvisible: Boolean = false,
    predicate: (AcsNode) -> Boolean
): ViewNode? {
    return SmartFinder(this).where(predicate).findFirst(includeInvisible)
}

suspend fun ViewNode.findAllWith(
    includeInvisible: Boolean = false,
    predicate: (AcsNode) -> Boolean
): Array<ViewNode> {
    return SmartFinder(this).where(predicate).findAll(includeInvisible)
}
