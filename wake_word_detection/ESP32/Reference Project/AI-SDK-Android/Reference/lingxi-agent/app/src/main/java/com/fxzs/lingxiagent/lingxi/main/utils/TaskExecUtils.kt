package com.fxzs.lingxiagent.lingxi.main.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.fxzs.lingxiagent.lingxi.core.api.click
import com.fxzs.lingxiagent.lingxi.core.api.longClick
import com.fxzs.lingxiagent.lingxi.core.viewnode.ViewNode
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream


object TaskExecUtils {

    fun clickByNodeRect(selectNode: ViewNode, isLong: Boolean = false, isTwice: Boolean = false) {
        runBlocking {
            val rect = selectNode.bounds
            val centerX = rect.left + rect.width() / 2
            val centerY = rect.top + rect.height() / 2
            if (isLong) {
               longClick(centerX, centerY)
            } else if (isTwice) {
                click(centerX, centerY)
                click(centerX, centerY)
            } else {
                click(centerX, centerY)
            }
        }
    }

    fun getBase64FromLocalImage(imagePath: String): String {
        val bitmap = BitmapFactory.decodeFile(imagePath)

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()

        return Base64.encodeToString(imageBytes, Base64.NO_WRAP)
    }
}