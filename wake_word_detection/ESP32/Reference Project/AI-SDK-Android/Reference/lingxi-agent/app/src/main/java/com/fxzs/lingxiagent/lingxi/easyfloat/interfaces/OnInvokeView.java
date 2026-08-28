package com.fxzs.lingxiagent.lingxi.easyfloat.interfaces;

import android.view.View;

/**
 * @function: 设置浮窗内容的接口，由于kotlin暂不支持SAM，所以使用Java接口
 */
public interface OnInvokeView {

    /**
     * 设置浮窗布局的具体内容
     *
     * @param view 浮窗布局
     */
    void invoke(View view);
}
