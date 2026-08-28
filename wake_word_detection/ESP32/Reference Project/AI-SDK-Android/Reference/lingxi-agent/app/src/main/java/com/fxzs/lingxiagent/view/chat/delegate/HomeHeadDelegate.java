package com.fxzs.lingxiagent.view.chat.delegate;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.HomeModelEntity;
import com.fxzs.lingxiagent.model.chat.callback.MsgActionCallback;
import com.fxzs.lingxiagent.model.chat.dto.ActionType;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.fxzs.lingxiagent.view.common.NoMultiClickListener;

import java.util.List;

import timber.log.Timber;

/**
 * 主页头部消息委托
 * 负责处理主页头部消息的显示，包括：
 * - 用户问候语的个性化显示
 * - 模型实体按钮的动态配置
 * - 按钮点击事件的处理和消息发送
 * - 头部卡片的交互逻辑
 */
public class HomeHeadDelegate extends BaseViewTypeDelegate {
    
    private static final String TAG = "HomeHeadDelegate";
    
    public HomeHeadDelegate() {
        super(ChatAdapter.TYPE_USER_HEAD_HOME, R.layout.lingxi_card_top_describe);
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder homeHolder = (ChatAdapter.ChatViewHolder) holder;
        
        Timber.tag(TAG).d( "setHomeHeadMessage: position=" + position);
        
        // 设置用户问候语，完全保持与原有逻辑一致
        if (homeHolder.textHi != null) {
            String name = SharedPreferencesUtil.getUserName();
            String greetingText = !TextUtils.isEmpty(name) ? "您好，" + name : "您好，我是你的AI助手";
            homeHolder.textHi.setText(greetingText);
            Timber.tag(TAG).d( "Set greeting text: " + greetingText);
        }
        
        // 获取并配置模型实体按钮
        setupModelButtons(homeHolder, context);
        
        // 设置头部卡片按钮点击事件
        setupHeadCardButtons(homeHolder, context);
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
    
    /**
     * 设置模型按钮的文本
     */
    private void setupModelButtons(ChatAdapter.ChatViewHolder holder, ChatAdapterContext context) {
        Object modelTypeObj = context.getModelType();
        if (modelTypeObj instanceof HomeModelEntity.ModelType) {
            HomeModelEntity.ModelType modelType = (HomeModelEntity.ModelType) modelTypeObj;
            
            List<HomeModelEntity> modelEntities = HomeModelEntity.createHeadEntity(modelType);
            if (modelEntities.size() >= 4) {
                if (holder.btnRecharge != null) {
                    holder.btnRecharge.setText(modelEntities.get(0).getName());
                }
                if (holder.btnGenerateImage != null) {
                    holder.btnGenerateImage.setText(modelEntities.get(1).getName());
                }
                if (holder.btnHealth != null) {
                    holder.btnHealth.setText(modelEntities.get(2).getName());
                }
                if (holder.btnSpring != null) {
                    holder.btnSpring.setText(modelEntities.get(3).getName());
                }
                
                Timber.tag(TAG).d( "Set model buttons with " + modelEntities.size() + " entities");
            } else {
                Timber.tag(TAG).w("Model entities size is less than 4: " + modelEntities.size());
            }
        }
    }
    
    /**
     * 设置头部卡片按钮点击事件
     */
    private void setupHeadCardButtons(ChatAdapter.ChatViewHolder holder, ChatAdapterContext context) {
        // 创建统一的点击监听器，完全保持与原有逻辑一致
        View.OnClickListener headListener = new NoMultiClickListener() {
            @Override
            public void onNoMultiClick(View v) {
                MsgActionCallback callback = context.getMsgActionCallback();
                if (callback != null) {
                    Object tag = v.getTag();
                    if (tag instanceof ActionType && v instanceof TextView) {
                        String buttonText = ((TextView) v).getText().toString();
                        callback.sendMsg(buttonText);
                        Timber.tag(TAG).d( "Head button clicked: " + buttonText + " (Action: " + tag + ")");
                    }
                }
            }
        };
        
        // 为每个按钮设置标签和点击监听器
        bindButton(holder.btnHealth, ActionType.HEALTH, headListener);
        bindButton(holder.btnRecharge, ActionType.RECHARGE, headListener);
        bindButton(holder.btnGenerateImage, ActionType.GENERATE_IMAGE, headListener);
        bindButton(holder.btnSpring, ActionType.SPRING, headListener);
    }
    
    /**
     * 为按钮绑定动作类型和点击监听器
     */
    private void bindButton(View button, ActionType actionType, View.OnClickListener listener) {
        if (button != null) {
            button.setTag(actionType);
            button.setOnClickListener(listener);
        }
    }
}