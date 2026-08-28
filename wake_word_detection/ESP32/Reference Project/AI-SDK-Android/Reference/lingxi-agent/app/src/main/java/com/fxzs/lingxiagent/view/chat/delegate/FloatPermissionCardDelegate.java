package com.fxzs.lingxiagent.view.chat.delegate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.accessibility_api.AccessibilityApi;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import timber.log.Timber;

/**
 * 悬浮窗权限卡片委托
 * 权限开启后自动刷新UI（传统onActivityResult方案，兼容所有Android版本）
 */
public class FloatPermissionCardDelegate extends CardMessageDelegate {

    private static final String TAG = "FloatPermissionCardDelegate";
    // 权限请求码（自定义，唯一即可）
    public static final int REQ_CODE_FLOAT_PERMISSION = 100001;
    public static final int REQ_CODE_ACC_PERMISSION = 100002;

    public FloatPermissionCardDelegate() {
        super(ChatAdapter.TYPE_ASSISTANT_FLOAT_PERM_CARD, R.layout.lingxi_card_permission_float);
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }

    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message,
                                            int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder permHolder = (ChatAdapter.ChatViewHolder) holder;
        // 初始化/刷新权限状态
        refreshPermissionState(permHolder, context);

        // ===== 悬浮窗权限按钮点击事件 =====
        if (permHolder.goOpenFloatBtn != null) {
            permHolder.goOpenFloatBtn.setOnClickListener(view -> {
                Intent intent = getFloatPermissionIntent(context);
                openPermissionPage(context.getContext(),intent,REQ_CODE_FLOAT_PERMISSION);
            });
        } else {
            Timber.tag(TAG).w("goOpenFloatBtn is null");
        }

        // ===== 无障碍权限按钮点击事件 =====
        if (permHolder.goOpenAccBtn != null) {
            permHolder.goOpenAccBtn.setOnClickListener(view -> {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                // 核心：用Activity调用传统startActivityForResult，绑定请求码
                openPermissionPage(context.getContext(),intent,REQ_CODE_ACC_PERMISSION);
            });
        } else {
            Timber.tag(TAG).w("goOpenAccBtn is null");
        }
    }

    // 统一跳转：可传悬浮窗Intent/无障碍Intent，自动适配
    @SuppressLint("TimberArgCount")
    private void openPermissionPage(Context context, Intent intent, int reqCode) {
        try {
            ((Activity) context).startActivityForResult(intent, reqCode);
            Timber.tag(TAG).d("Opened permission page, reqCode: %d", reqCode);
            // 标记：开始等待权限校验 + 记录当前请求码
            AccessibilityApi.Companion.setWaitingPermission(true);
        } catch (Exception e) {
            Timber.tag(TAG).e("Failed to open permission page", e);
            AccessibilityApi.Companion.setWaitingPermission(false);
        }
    }
    /**
     * 通用方法：刷新权限状态+更新按钮文字
     */
    private void refreshPermissionState(ChatAdapter.ChatViewHolder holder, ChatAdapterContext context) {
        if (!Settings.canDrawOverlays(context.getContext())){//刷新悬浮窗权限按钮
            holder.goOpenFloatBtn.setVisibility(View.VISIBLE);
            holder.goSuccessFloat.setVisibility(View.GONE);
        }else {
            holder.goOpenFloatBtn.setVisibility(View.GONE);
            holder.goSuccessFloat.setVisibility(View.VISIBLE);
        }

        if (!AccessibilityApi.Companion.isBaseServiceEnable() ){//刷新无障碍权限按钮
            holder.goOpenAccBtn.setVisibility(View.VISIBLE);
            holder.goSuccessAcc.setVisibility(View.GONE);
        }else {
            holder.goOpenAccBtn.setVisibility(View.GONE);
            holder.goSuccessAcc.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 适配所有Android版本的悬浮窗权限跳转Intent（解决机型兼容问题）
     */
    private Intent getFloatPermissionIntent(ChatAdapterContext context) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getContext().getPackageName()));
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + context.getContext().getPackageName()));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
}