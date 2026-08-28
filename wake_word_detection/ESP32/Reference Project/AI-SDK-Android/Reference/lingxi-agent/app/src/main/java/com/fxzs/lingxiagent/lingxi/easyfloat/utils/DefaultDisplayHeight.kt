package com.lzf.easyfloat.utils

import android.content.Context
import com.fxzs.lingxiagent.lingxi.easyfloat.interfaces.OnDisplayHeight

/**
 * @function: 获取屏幕有效高度的实现类
 */
internal class DefaultDisplayHeight : OnDisplayHeight {

    override fun getDisplayRealHeight(context: Context) = DisplayUtils.rejectedNavHeight(context)

}