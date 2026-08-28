package com.fxzs.lingxiagent.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.os.Handler;
import android.os.Looper;

import timber.log.Timber;

public class FixTouchWebView extends WebView {
    private boolean isMultiTouch = false;
    private boolean isResetting = false; // 标记是否正在重置状态
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public FixTouchWebView(Context context) {
        super(context);
        init();
    }

    public FixTouchWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
        // 开启JS支持（确保网页点击事件能被处理）
        getSettings().setJavaScriptEnabled(true);
        // 允许网页缩放（部分点击失效与缩放限制有关）
        getSettings().setSupportZoom(true);
    }

    /**
     * 优先在事件拦截阶段判断触摸状态，避免父容器干扰
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        updateMultiTouchStatus(event);
        // 多点触摸时不拦截，确保系统处理截屏；单点时正常拦截（避免父容器抢事件）
        if (isMultiTouch) {
            return false;
        }
        // 修复：单点触摸时强制不拦截，确保事件能传递到WebView内部
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        updateMultiTouchStatus(event);
        Timber.tag("PptPreview").d(
                "触摸事件：%s，触摸点：%d，是否多点：%s, isResetting = %s, event.getAction()",
                getActionName(event.getAction()),
                event.getPointerCount(),
                isMultiTouch,isResetting
        );

        // 多点触摸结束后，强制重置WebView内核状态
        if ((event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL || (getActionName(event.getAction()).toUpperCase()).equals("UNKNOWN"))
                && isMultiTouch
                && !isResetting) {
            isResetting = true;
            resetWebViewState();
            isMultiTouch = false;
            // 必须返回true，否则系统会认为事件未处理，阻断后续点击
            return true;
        }

        // 核心修复：单点触摸时，强制让WebView消费事件（返回true）
        // 确保ACTION_DOWN -> ACTION_UP的完整链路被WebView处理
        boolean handled = super.onTouchEvent(event);
        return isMultiTouch ? false : (handled || isClickable());
    }

    /**
     * 重置WebView状态，重点唤醒JS事件系统
     */
    private void resetWebViewState() {
        Timber.tag("PptPreview").d("resetWebViewState");
        // 1. 刷新焦点（解决原生焦点丢失）
        post(() -> {
            clearFocus();
            requestFocus(View.FOCUS_DOWN);
            setFocusableInTouchMode(true);
        });

        // 2. 强制唤醒JS内核（关键：让网页重新响应点击）
        postDelayed(() -> {
            this.reload();// 触发网页全局点击事件失败，暂时先重新load
            isResetting = false;
//            evaluateJavascript("(function() { " +
//                    // 触发网页全局点击事件（唤醒监听）
//                    "var event = new MouseEvent('click', {bubbles: true, cancelable: true});" +
//                    "document.dispatchEvent(event);" +
//                    "return true;" +
//                    "})();", value -> {
//                Timber.tag("PptPreview").d("JS唤醒结果：%s", value);
//                isResetting = false;
//            });
        }, 200); // 延迟确保系统截屏窗口已关闭

        // 3. 兜底：强制刷新WebView绘制（避免视觉与交互不同步）
        postDelayed(this::invalidate, 300);
    }

    /**
     * 辅助方法：转换事件类型为字符串（便于日志调试）
     */
    private String getActionName(int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN: return "ACTION_DOWN";
            case MotionEvent.ACTION_UP: return "ACTION_UP";
            case MotionEvent.ACTION_CANCEL: return "ACTION_CANCEL";
            case MotionEvent.ACTION_MOVE: return "ACTION_MOVE";
            default: return "UNKNOWN";
        }
    }

    private void updateMultiTouchStatus(MotionEvent event) {
        isMultiTouch = event.getPointerCount() >= 3;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mainHandler.removeCallbacksAndMessages(null);
    }
}