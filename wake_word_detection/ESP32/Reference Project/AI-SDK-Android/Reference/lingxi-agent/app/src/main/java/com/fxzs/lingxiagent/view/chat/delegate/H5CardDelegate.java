package com.fxzs.lingxiagent.view.chat.delegate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.model.chat.dto.EventBusCardLoadNotifyModel;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.lingxi.cardhelper.CardView;

import org.greenrobot.eventbus.EventBus;

import timber.log.Timber;

/**
 * H5卡片委托
 * 负责处理H5动态卡片的显示，包括：
 * - H5卡片内容的数据绑定
 * - 卡片模板ID和内容的处理
 * - 回复事件监听器的设置
 * - CardView事件注册和处理
 */
public class H5CardDelegate extends CardMessageDelegate {

    private static final String TAG = "H5CardDelegate";

    public H5CardDelegate() {
        super(ChatAdapter.TYPE_ASSISTANT_H5_CARD, R.layout.lingxi_card_web);
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message,
                                            int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder h5Holder = (ChatAdapter.ChatViewHolder) holder;

        Timber.tag(TAG).d("setH5CardUI: position=" + position);

        // 检查消息是否为空
        if (message == null) {
            Timber.tag(TAG).w("ChatMessage is null, cannot bind H5 card");
            return;
        }

        // 绑定H5卡片数据，完全保持与原有逻辑一致
//        if (h5Holder.cardView != null) {
        h5Holder.bindData(message.getH5CardContent(), message.getH5TemplateId(), position);

        // 设置回复监听器
        h5Holder.cardView.getEventRegistry().setOnReplyListener(replyText -> {
            if (context instanceof ChatAdapter) {
                ChatAdapter chatAdapter = (ChatAdapter) context;
                if (chatAdapter.replyClickListener != null) {
                    chatAdapter.replyClickListener.onReplyClick(replyText);
                    Timber.tag(TAG).d("H5 card reply clicked: " + replyText);
                }
            } else {
                Timber.tag(TAG).w("Context is not ChatAdapter, cannot handle reply click");
            }
        });
//        h5Holder.cardView.getEventRegistry().setOnHeightChangedListener((i, i1) -> {
//            Log.d("==========", String.format("i=%s,i1=%s", i, i1));
////            if (Math.abs(i1 - i) > 10) {
////
////            }
//        });
        h5Holder.cardView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int oldHeight = 0;
            if (h5Holder.cardView.getTag() != null) {
                oldHeight = (int) h5Holder.cardView.getTag();
            }
            if (oldHeight != 0 && h5Holder.cardView.getHeight() - oldHeight > 50) {
                EventBus.getDefault().post(new EventBusCardLoadNotifyModel(true));
//                Log.d("==========", h5Holder.cardView.toString());
//                Log.d("==========", String.format("oldH = %s newH = %s", oldHeight, h5Holder.cardView.getHeight()));
            }
            h5Holder.cardView.setTag(h5Holder.cardView.getHeight());
        });


        h5Holder.cardView.setOnErrorListener((i, s) -> {
            if (!(context instanceof ChatAdapter)) {
                return;
            }
            ChatAdapter chatAdapter = (ChatAdapter) context;
            switch (i) {
                case CardView.ERROR_ACTIVITY_NOT_FOUND:
                    if(!TextUtils.isEmpty(s) && s.contains("com.greenpoint")){
                        GlobalToast.show((Activity) chatAdapter.getContext(), "没有安装中国移动app，请去应用商店下载安装", GlobalToast.Type.ERROR);
                    }else if(!TextUtils.isEmpty(s) && s.contains("miguvideo")){
                        GlobalToast.show((Activity) chatAdapter.getContext(), "没有安装咪咕视频app，请去应用商店下载安装", GlobalToast.Type.ERROR);
                    }else {
                        GlobalToast.show((Activity) chatAdapter.getContext(), "打开APP失败，请检查是否下载", GlobalToast.Type.ERROR);
                    }
                    break;
                case CardView.ERROR_JSON_INJECTION_FAILED:
                    GlobalToast.show((Activity) chatAdapter.getContext(), "处理JSON注入失败", GlobalToast.Type.ERROR);
                    break;
                case CardView.ERROR_INVALID_URL:
                    // 处理无效URL错误
                    GlobalToast.show((Activity) chatAdapter.getContext(), "无效URL", GlobalToast.Type.ERROR);
                    break;
                case CardView.ERROR_WEBVIEW_INIT_FAILED:
                    // 处理WebView初始化失败错误
                    GlobalToast.show((Activity) chatAdapter.getContext(), "WebView初始化失败", GlobalToast.Type.ERROR);
                    break;
                case CardView.ERROR_LOAD_VIEW_FAILED:
                    // 处理视图加载失败错误
                    GlobalToast.show((Activity) chatAdapter.getContext(), "视图加载失败", GlobalToast.Type.ERROR);
                    break;
            }

        });

        h5Holder.cardView.setOnLongClickListener(context.getOnLongClickListener());
        Timber.tag(TAG).d("setH5CardUI: Bound H5 card with template ID: " + message.getH5TemplateId());
//        } else {
//            Timber.tag(TAG).w((TAG, "setH5CardUI: cardView is null");
//        }
    }

    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
}