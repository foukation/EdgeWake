package com.fxzs.lingxiagent.lingxi.gui_agent.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.callback.NexusVoiceCallback;
import com.fxzs.lingxiagent.util.NexusVoice;

import timber.log.Timber;


public class WaitingReplyDialogActivity extends Activity {

    // 回调接口
    public interface OnActionListener {
        void onStopClick();
        void onInputClick(String content);
        void onMicClick();
        void onCloseClick();
        void onCompleteClick();
        void onBackClick();
        void onStop(boolean clickAble);
    }

    // Intent参数键名
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_CONTENT = "content";
    private static final String EXTRA_TYPE = "type";

    // 弹窗类型
    public static final int TYPE_DEFAULT = 0;
    public static final int TYPE_WITH_COMPLETE = 1;
    public static final int TYPE_WITH_TASK_COMPLETE = 2;
    public static final int TYPE_WITH_ERROR = 3;

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
    private TextView tvError;
    private OnActionListener actionListener;
    private String title;
    private String content;
    private int dialogType;
    private boolean ClickAble = false;
    private NexusVoiceCallback callback = new NexusVoiceCallback() {
        @Override
        public void onSuccessMsg(String content) {
            if (actionListener != null) {
                ClickAble = true;
                actionListener.onInputClick(content);
            }
            dismiss();
        }

        @Override
        public void onErrorMsg(String error) {
            // 处理语音识别失败
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 必须先设置窗口属性，再设置内容视图
        setupWindow();

        // 设置内容视图
        setContentView(R.layout.dialog_waiting_reply_activity);

        // 初始化参数
        initParams();

        // 初始化视图
        initViews();

        // 设置监听器
        setupListeners();

        // 更新显示
        updateDisplay();
    }

    /**
     * 设置窗口属性
     */
    private void setupWindow() {
        // 无标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 全屏半透明
        Window window = getWindow();

        // 强制设置全屏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.TOP | Gravity.START; // 从左上角开始
        params.dimAmount = 0f; // 去除背景变暗

        // 设置完全透明背景
        window.setBackgroundDrawableResource(android.R.color.transparent);

        // 关键：设置窗口完全透明
        window.setAttributes(params);

        // 移除所有可能导致变暗的标志
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // 只保留必要的标志
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        window.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

        // 确保布局扩展到状态栏下面
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        // 确保内容可以显示在系统栏后面
        window.getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }


    private void initViews() {
        ivAvatar = findViewById(R.id.iv_avatar);
        tvTitle = findViewById(R.id.tv_title);
        tvContent = findViewById(R.id.tv_content);
        llInputContainer = findViewById(R.id.ll_input_container);
        nexusVoice = findViewById(R.id.nexus_voice);
        btnStop = findViewById(R.id.btn_stop);
        ivClose = findViewById(R.id.iv_close);
        btnComplete = findViewById(R.id.btn_complete);
        rl_complete = findViewById(R.id.rl_complete);
        tvDone = findViewById(R.id.tv_done);
        btnBack = findViewById(R.id.btn_back);
        tvError = findViewById(R.id.tv_error);
        nexusVoice.setCallback(callback);
    }

    private void setupListeners() {
        // 关闭按钮
        ivClose.setOnClickListener(v -> {
            if (actionListener != null) {
                ClickAble = true;
                actionListener.onCloseClick();
            }
            dismiss();
        });

        // 停止按钮
        btnStop.setOnClickListener(v -> {
            if (actionListener != null) {
                ClickAble = true;
                actionListener.onStopClick();
            }
            dismiss();
        });

        // 操作完成按钮
        rl_complete.setOnClickListener(v -> {
            if (actionListener != null) {
                ClickAble = true;
                actionListener.onCompleteClick();
            }
            dismiss();
        });

        // 返回应用按钮
        btnBack.setOnClickListener(v -> {
            if (actionListener != null) {
                ClickAble = true;
                actionListener.onBackClick();
            }
            dismiss();
        });

        // 输入框容器
        llInputContainer.setOnClickListener(v -> {
            if (actionListener != null) {
                ClickAble = true;
                actionListener.onInputClick(content);
            }
        });
    }

    private void updateDisplay() {
        if (tvTitle != null) {
            tvTitle.setText(title != null ? title : "等待答复");
        }
        if (tvContent != null) {
            tvContent.setText(content != null ? content : "");
        }

        // 根据类型显示/隐藏组件
        switch (dialogType) {
            case TYPE_WITH_TASK_COMPLETE:
                if (tvTitle != null) tvTitle.setVisibility(View.GONE);
                if (tvDone != null) {
                    tvDone.setVisibility(View.VISIBLE);
                    tvDone.setTextColor(getResources().getColor(R.color.color_0BBF6E));
                }
                if (btnBack != null) btnBack.setVisibility(View.VISIBLE);
                if (btnStop != null) btnStop.setVisibility(View.GONE);
                if (rl_complete != null) rl_complete.setVisibility(View.GONE);
                if (llInputContainer != null) llInputContainer.setVisibility(View.GONE);
                if (tvError != null) tvError.setVisibility(View.GONE);

                break;

            case TYPE_WITH_COMPLETE:
                if (rl_complete != null) rl_complete.setVisibility(View.VISIBLE);
                if (llInputContainer != null) llInputContainer.setVisibility(View.VISIBLE);
                if (nexusVoice != null) nexusVoice.setVisibility(View.GONE);
                break;
            case TYPE_WITH_ERROR:
                if (tvTitle != null) tvTitle.setVisibility(View.GONE);
                if (tvDone != null) tvDone.setVisibility(View.GONE);
                if (btnBack != null) btnBack.setVisibility(View.VISIBLE);
                if (btnStop != null) btnStop.setVisibility(View.GONE);
                if (rl_complete != null) rl_complete.setVisibility(View.GONE);
                if (llInputContainer != null) llInputContainer.setVisibility(View.GONE);
                if (tvError != null) {
                    tvError.setVisibility(View.VISIBLE);
                    tvDone.setTextColor(getResources().getColor(R.color.color_EE3636));
                }


            case TYPE_DEFAULT:
                if (rl_complete != null) rl_complete.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * 显示弹窗
     */
    public static void show(Context context, String title, String content, int type) {
        Intent intent = new Intent(context, WaitingReplyDialogActivity.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_CONTENT, content);
        intent.putExtra(EXTRA_TYPE, type);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 关闭弹窗
     */
    public void dismiss() {
        overridePendingTransition(0, 0); // 无动画
        finish();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 点击外部区域不关闭
        return true;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // 禁用返回键
    }

    /**
     * 设置操作监听器
     */
    public void setOnActionListener(OnActionListener listener) {
        this.actionListener = listener;
    }

    /**
     * Builder模式创建实例
     */
    public static Builder with(Context context) {
        return new Builder(context);
    }

    /**
     * Builder类 - 提供链式调用方式
     */
    public static class Builder {
        private final Context context;
        private String title = "等待答复";
        private String content;
        private int type = TYPE_DEFAULT;
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

        public Builder actionListener(OnActionListener listener) {
            this.actionListener = listener;
            return this;
        }

        /**
         * 构建并立即显示
         */
        public void show() {
            // 创建Intent
            Intent intent = new Intent(context, WaitingReplyDialogActivity.class);
            intent.putExtra(EXTRA_TITLE, title);
            intent.putExtra(EXTRA_CONTENT, content);
            intent.putExtra(EXTRA_TYPE, type);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 启动Activity
            context.startActivity(intent);

            // 设置监听器（通过静态变量传递）
            if (actionListener != null) {
                // 这里需要通过其他机制传递监听器，比如EventBus或静态变量
                setStaticActionListener(actionListener);
            }
        }
    }

    // 静态变量用于传递监听器
    private static OnActionListener staticActionListener;

    private static void setStaticActionListener(OnActionListener listener) {
        staticActionListener = listener;
    }

    private static void clearStaticActionListener() {
        staticActionListener = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Timber.tag("HandlerLlm").d("onStop ClickAble: " + ClickAble);
        // 添加空指针检查
        if (actionListener != null) {
            actionListener.onStop(ClickAble);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理静态监听器
        clearStaticActionListener();
    }

    // 在initParams中设置监听器
    private void initParams() {
        Intent intent = getIntent();
        title = intent.getStringExtra(EXTRA_TITLE);
        content = intent.getStringExtra(EXTRA_CONTENT);
        dialogType = intent.getIntExtra(EXTRA_TYPE, TYPE_DEFAULT);

        // 从静态变量获取监听器
        actionListener = staticActionListener;
    }
}