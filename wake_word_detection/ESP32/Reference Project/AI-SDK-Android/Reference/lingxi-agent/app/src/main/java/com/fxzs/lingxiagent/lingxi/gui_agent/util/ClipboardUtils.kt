package com.fxzs.lingxiagent.lingxi.gui_agent.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.fxzs.lingxiagent.lingxi.main.utils.CustomToast

object ClipboardUtils {

    /**
     * 将文本复制到剪贴板
     * @param context 上下文
     * @param text 要复制的文本
     * @param showToast 是否显示提示
     */
    @JvmStatic
    fun copyText(context: Context, text: String?, showToast: Boolean = true) {
        if (context == null || text.isNullOrEmpty()) {
            return
        }
        // 获取系统剪贴板服务
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        // 创建ClipData对象
        val clip = ClipData.newPlainText("clipboard_text", text)
        // 设置到剪贴板
        clipboard.setPrimaryClip(clip)
        // 显示提示
        if (showToast) {
            CustomToast.showToast(context, "文本已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 获取剪贴板中的文本内容
     * @param context 上下文
     * @return 剪贴板中的文本，如果没有则返回null
     */
    @JvmStatic
    fun getText(context: Context): String? {
        if (context == null) {
            return null
        }
        // 获取系统剪贴板服务
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null
        // 检查是否有内容
        if (!clipboard.hasPrimaryClip()) {
            return null
        }
        // 获取剪贴板数据
        val clipData = clipboard.primaryClip ?: return null
        if (clipData.itemCount <= 0) {
            return null
        }
        // 获取文本内容
        return clipData.getItemAt(0)?.text?.toString()
    }

    /**
     * 检查剪贴板是否有内容
     * @param context 上下文
     * @return true:有内容，false:无内容
     */
    @JvmStatic
    fun hasContent(context: Context): Boolean {
        if (context == null) {
            return false
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        return clipboard?.hasPrimaryClip() == true
    }

    /**
     * 检查剪贴板内容是否为文本类型
     * @param context 上下文
     * @return true:是文本，false:不是文本或无内容
     */
    @JvmStatic
    fun isTextContent(context: Context): Boolean {
        if (context == null) {
            return false
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        if (!clipboard.hasPrimaryClip()) {
            return false
        }
        val clipData = clipboard.primaryClip ?: return false
        if (clipData.itemCount <= 0) {
            return false
        }
        return clipData.getItemAt(0)?.text != null
    }

}