package com.fxzs.lingxiagent.lingxi.gui_agent.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.callback.NexusVoiceCallback;
import com.fxzs.lingxiagent.util.NexusVoice;

import timber.log.Timber;

public class WaitingReplyPopupWindow {

    // 回调接口
    public interface OnActionListener {
        void onStopClick();
        void onInputClick(String content);
        void onMicClick();
        void onCloseClick();
        void onCompleteClick();
        void onBackClick();
    }

    private OnActionListener actionListener;
    private PopupWindow popupWindow;
    private Context context;
    private View popupView;

    // UI组件
    private ImageView ivAvatar;
    private TextView tvTitle;
    private TextView tvContent;
    private LinearLayout llInputContainer;
    private NexusVoice nexusVoice;
    private TextView btnStop;
    private ImageView ivClose;
    private TextView btnComplete;
    private RelativeLayout rl_complete;
    private TextView tvDone;
    private TextView btnBack;

    // 弹窗类型
    public static final int TYPE_DEFAULT = 0; // 默认类型，不显示"操作完成"按钮
    public static final int TYPE_WITH_COMPLETE = 1; // 显示"操作完成"按钮
    public static final int TYPE_WITH_TASK_COMPLETE = 2; // 显示"任务已完成"按钮
    // 数据
    private String title = "等待答复";
    private String content;
    private int dialogType = TYPE_DEFAULT;
    private boolean isSystemWindow = false;

    // 单例实例
    private static WaitingReplyPopupWindow instance;

    NexusVoiceCallback callback = new NexusVoiceCallback() {
        @Override
        public void onSuccessMsg(String content) {
            // 处理语音识别成功后的逻辑，例如更新UI显示等

            Timber.tag("VoiceCallback").d("语音识别成功，内容：%s", content);
            actionListener.onInputClick(content);
            dismiss();

        }

        @Override
        public void onErrorMsg(String error) {
            // 处理语音识别失败后的逻辑，例如提示用户重新尝试等
            Timber.tag("VoiceCallback").e("语音识别失败，错误信息：%s", error);

        }
    };

    // 私有构造函数
    private WaitingReplyPopupWindow(Context context) {
        this.context = context.getApplicationContext();
        init();
    }

    /**
     * 获取单例实例
     */
    public static synchronized WaitingReplyPopupWindow getInstance(Context context) {
        if (instance == null) {
            instance = new WaitingReplyPopupWindow(context);
        }
        return instance;
    }

    /**
     * 释放单例实例
     */
    public static void releaseInstance() {
        if (instance != null) {
            instance.dismiss();
            instance = null;
        }
    }

    /**
     * Builder模式创建实例
     */
    public static Builder with(Context context) {
        return new Builder(context);
    }

    /**
     * Builder类 - 提供更优雅的创建方式
     */
    public static class Builder {
        private final Context context;
        private String title = "等待答复";
        private String content;
        private int type = TYPE_DEFAULT;
        private boolean systemWindow = false;
        private OnActionListener actionListener;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder type(int type) {
            this.type = type;
            return this;
        }

        public Builder systemWindow(boolean systemWindow) {
            this.systemWindow = systemWindow;
            return this;
        }

        public Builder actionListener(OnActionListener listener) {
            this.actionListener = listener;
            return this;
        }

        public WaitingReplyPopupWindow build() {
            WaitingReplyPopupWindow popup = new WaitingReplyPopupWindow(context);
            popup.setTitle(title);
            if (content != null) {
                popup.setContent(content);
            }
            popup.setDialogType(type);
            popup.setSystemWindow(systemWindow);
            if (actionListener != null) {
                popup.setOnActionListener(actionListener);
            }
            return popup;
        }

        /**
         * 构建并立即显示（底部显示）
         */
        public WaitingReplyPopupWindow show() {
            return build().show();
        }

        /**
         * 构建并立即显示（悬浮显示）
         */
        public WaitingReplyPopupWindow showOverlay() {
            return build().showOverlay();
        }

        /**
         * 构建并立即显示（底部悬浮显示）
         */
        public WaitingReplyPopupWindow showOverlayAtBottom() {
            return build().showOverlayAtBottom();
        }
    }

    private void init() {
        LayoutInflater inflater = LayoutInflater.from(context);
        popupView = inflater.inflate(R.layout.dialog_waiting_reply, null);

        // 初始化PopupWindow（全屏尺寸）
        popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                true
        );

        // 设置PopupWindow属性（全屏半透明背景由布局文件中的FrameLayout提供）
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(false); // 禁用外部点击关闭
        popupWindow.setTouchable(true); // 保持内部可点击
        popupWindow.setFocusable(true);
        popupWindow.setClippingEnabled(false); // 允许内容超出屏幕
        popupWindow.setAnimationStyle(R.style.HintDialogStyle);


        // 设置为系统覆盖窗口，确保覆盖状态栏
        // 使用TYPE_SYSTEM_ERROR窗口类型，这是最高层级的系统窗口
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            popupWindow.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            popupWindow.setWindowLayoutType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
        }

        // 设置触摸拦截器，拦截弹窗外部的点击事件
        popupWindow.setTouchInterceptor((v, event) -> {
            if (!isInPopupArea(event)) {
                return true; // 拦截弹窗外部的点击事件
            }
            return false; // 允许弹窗内部的点击事件
        });

        initViews();
        setupListeners();

        // 保持全屏设置（已在构造函数中设置MATCH_PARENT）
        // 不需要额外的尺寸设置
    }

    private void initViews() {
        ivAvatar = popupView.findViewById(R.id.iv_avatar);
        tvTitle = popupView.findViewById(R.id.tv_title);
        tvContent = popupView.findViewById(R.id.tv_content);
        llInputContainer = popupView.findViewById(R.id.ll_input_container);
        nexusVoice = popupView.findViewById(R.id.nexus_voice);
        btnStop = popupView.findViewById(R.id.btn_stop);
        ivClose = popupView.findViewById(R.id.iv_close);
        btnComplete = popupView.findViewById(R.id.btn_complete);
        rl_complete = popupView.findViewById(R.id.rl_complete);
        tvDone = popupView.findViewById(R.id.tv_done);
        btnBack = popupView.findViewById(R.id.btn_back);
        nexusVoice.setCallback(callback);

    }

    private void setupListeners() {
        // 关闭按钮点击事件
        ivClose.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onCloseClick();
            }
            dismiss();
        });

        // 停止按钮点击事件
        btnStop.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onStopClick();
            }
            dismiss();
        });

        // 输入框容器点击事件
        llInputContainer.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onInputClick(content);
            }
        });

        // 操作完成按钮点击事件
        rl_complete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onCompleteClick();
            }
            dismiss();
        });
        btnBack.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onBackClick();
            }
            dismiss();
        });
    }

    // 链式调用方法

    public WaitingReplyPopupWindow setTitle(String title) {
        this.title = title;
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
        return this;
    }

    public WaitingReplyPopupWindow setContent(String content) {
        this.content = content;
        if (tvContent != null) {
            tvContent.setText(content);
        }
        return this;
    }

    /**
     * 重置弹窗状态（单例模式下重用）
     */
    public WaitingReplyPopupWindow reset() {
        this.title = "等待答复";
        this.content = null;
        this.dialogType = TYPE_DEFAULT;
        this.isSystemWindow = false;
        this.actionListener = null;

        if (popupWindow != null) {
            popupWindow.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL);
        }

        updateDisplay();
        return this;
    }


    public WaitingReplyPopupWindow setDialogType(int type) {
        this.dialogType = type;
        switch (type) {
            case TYPE_WITH_TASK_COMPLETE:
                tvTitle.setVisibility(View.GONE);
                tvDone.setVisibility(View.VISIBLE);
                btnBack.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.GONE);
                rl_complete.setVisibility(View.GONE);
                llInputContainer.setVisibility(View.GONE);
                break;
            case TYPE_WITH_COMPLETE:
                rl_complete.setVisibility(View.VISIBLE);
                llInputContainer.setVisibility(View.VISIBLE);
                nexusVoice.setVisibility(View.GONE);
                break;
            case TYPE_DEFAULT:
                rl_complete.setVisibility(View.GONE);
                break;
        }
        return this;
    }

    public WaitingReplyPopupWindow setOnActionListener(OnActionListener listener) {
        this.actionListener = listener;
        return this;
    }

    public WaitingReplyPopupWindow setBackground(int drawableResId) {
        if (popupView != null) {
            popupView.setBackgroundResource(drawableResId);
        }
        return this;
    }

    public WaitingReplyPopupWindow setWidth(int width) {
        if (popupWindow != null) {
            popupWindow.setWidth(width);
        }
        return this;
    }

    public WaitingReplyPopupWindow setHeight(int height) {
        if (popupWindow != null) {
            popupWindow.setHeight(height);
        }
        return this;
    }

    public WaitingReplyPopupWindow setSystemWindow(boolean systemWindow) {
        this.isSystemWindow = systemWindow;
        if (popupWindow != null && systemWindow) {
            // 设置为系统级窗口
            popupWindow.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        }
        return this;
    }

    // 显示方法

    public WaitingReplyPopupWindow show(View anchorView) {
        if (popupWindow != null && !popupWindow.isShowing()) {
            // 更新数据显示
            updateDisplay();
            popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0);
        }
        return this;
    }

    public WaitingReplyPopupWindow showAsDropDown(View anchorView) {
        if (popupWindow != null && !popupWindow.isShowing()) {
            updateDisplay();
            popupWindow.showAsDropDown(anchorView);
        }
        return this;
    }

    public WaitingReplyPopupWindow showAsDropDown(View anchorView, int xoff, int yoff) {
        if (popupWindow != null && !popupWindow.isShowing()) {
            updateDisplay();
            popupWindow.showAsDropDown(anchorView, xoff, yoff);
        }
        return this;
    }

    public WaitingReplyPopupWindow showAtBottom() {
        if (popupWindow != null && !popupWindow.isShowing()) {
            updateDisplay();

            View anchorView = getAnchorView();

            // 全屏显示，位置由布局文件中的FrameLayout控制
            popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0);
        }
        return this;
    }

    public WaitingReplyPopupWindow showOverlay() {
        if (popupWindow != null && !popupWindow.isShowing()) {
            updateDisplay();

            // 设置为系统级窗口
            popupWindow.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);

            // 使用空视图作为锚点
            View anchorView = new View(context);

            // 显示在屏幕中央
            popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0);
        }
        return this;
    }

    public WaitingReplyPopupWindow showOverlayAtBottom() {
        if (popupWindow != null && !popupWindow.isShowing()) {
            updateDisplay();

            // 设置为系统级窗口
            popupWindow.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);

            // 使用空视图作为锚点
            View anchorView = new View(context);

            // 全屏显示，位置由布局文件中的FrameLayout控制
            popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0);
        }
        return this;
    }

    private View getAnchorView() {
        if (isSystemWindow) {
            // 系统窗口使用空视图
            return new View(context);
        } else {
            // 普通窗口使用PopupWindow的contentView
            return popupView;
        }
    }

    /**
     * 判断点击事件是否在弹窗区域内
     */
    private boolean isInPopupArea(MotionEvent event) {
        if (popupView == null) return false;

        int[] location = new int[2];
        popupView.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        int width = popupView.getWidth();
        int height = popupView.getHeight();

        return event.getRawX() >= x &&
               event.getRawX() <= x + width &&
               event.getRawY() >= y &&
               event.getRawY() <= y + height;
    }

    public WaitingReplyPopupWindow show() {
        if (isSystemWindow) {
            // 系统窗口默认使用悬浮显示
            return showOverlay();
        } else {
            // 普通窗口默认使用底部显示
            return showAtBottom();
        }
    }


    private void updateDisplay() {
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
        if (tvContent != null) {
            tvContent.setText(content != null ? content : "");
        }
//        if (rl_complete != null) {
//            rl_complete.setVisibility(dialogType == TYPE_WITH_COMPLETE ? View.VISIBLE : View.GONE);
//            nexusVoice.setVisibility(dialogType == TYPE_WITH_COMPLETE ? View.GONE : View.VISIBLE);
//        }
    }

    // 关闭方法

    public void dismiss() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        // 单例模式下不释放实例，只关闭窗口
    }

    /**
     * 完全销毁弹窗（释放资源）
     */
    public void destroy() {
        dismiss();
        if (popupWindow != null) {
            popupWindow = null;
        }
        popupView = null;
        // 不释放单例实例，由releaseInstance()处理
    }

    public boolean isShowing() {
        return popupWindow != null && popupWindow.isShowing();
    }

    // Getter方法

    public NexusVoice getNexusVoice() {
        return nexusVoice;
    }

    public RelativeLayout getCompleteButton() {
        return rl_complete;
    }

    public TextView getStopButton() {
        return btnStop;
    }

    public LinearLayout getInputContainer() {
        return llInputContainer;
    }

    public ImageView getCloseButton() {
        return ivClose;
    }

    public PopupWindow getPopupWindow() {
        return popupWindow;
    }
}
