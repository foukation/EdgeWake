package com.fxzs.lingxiagent.actions

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.service_api.data.Action
import com.fxzs.lingxiagent.lingxi.accessibility_api.AccessibilityApi.Companion.isBaseServiceEnable
import com.fxzs.lingxiagent.lingxi.main.actions.LineTaskSocket
import com.fxzs.lingxiagent.lingxi.main.helper.FloatHelper
import com.fxzs.lingxiagent.lingxi.main.utils.Utils.showToastTop
import timber.log.Timber
import java.lang.Thread.sleep

/**
 *
 */
class LineTaskOrchestrate(
    private val context: Activity,
    private val actions: ArrayList<Action>,
    private val slots: Map<String,String>,
    private val execNum: Int,
) {
    private lateinit var taskHelper: LineTaskSocket

    private fun openMenu() {
        FloatHelper.openFloatMenu(close = { close() })
    }

    private fun closeMenu() {
        FloatHelper.closeFloatMenu()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun taskRun(){
        openMenu()
        taskHelper.run()
        sleep(2000L)
        showToastTop(context,"点击悬浮球可关闭任务")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun start() {
        taskHelper = LineTaskSocket(
            context,
            actions,
            slots,
            execNum,
            closeMenu = { closeMenu() },
        )

        if (isBaseServiceEnable) {
            Timber.tag("测试测试").d("taskRun")

            taskRun()
        }
    }


    fun stop() {
        taskHelper.stop()
    }

    fun close() {
        taskHelper.close()
    }
}
