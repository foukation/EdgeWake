package com.fxzs.lingxiagent.view.chat.delegate;

import android.content.Intent;
import android.net.Uri;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.service_api.data.FoodList;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.fxzs.lingxiagent.view.chat.ChatFoodAdapter;

import timber.log.Timber;

/**
 * 餐厅卡片委托
 * 负责处理餐厅推荐卡片的显示，包括：
 * - 餐厅列表的RecyclerView布局设置
 * - FoodItemAdapter的创建和数据绑定
 * - 更多餐厅链接的点击处理
 * - 系统浏览器跳转功能
 */
public class FoodCardDelegate extends CardMessageDelegate {
    
    private static final String TAG = "FoodCardDelegate";
    
    public FoodCardDelegate() {
        super(ChatAdapter.TYPE_ASSISTANT_FOOD_CARD, R.layout.lingxi_card_party_restaurant);
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder foodHolder = (ChatAdapter.ChatViewHolder) holder;
        
        Timber.tag(TAG).d( "setAssistantFoodCard: position=" + position);
        
        // 获取餐厅数据
        FoodList foodList = message.getFoodList();
        if (foodList == null) {
            Timber.tag(TAG).w("FoodList is null, cannot display food card");
            return;
        }
        
        // 获取更多链接URL
        String resultUrl = null;
        try {
            resultUrl = foodList.getMoreLink().getWeb().getUrl();
        } catch (Exception e) {
            Timber.tag(TAG).w("Failed to get more link URL", e);
        }
        
        // 设置餐厅列表
        setupFoodRecyclerView(foodHolder, foodList, context);
        
        // 设置更多按钮
        setupMoreButton(foodHolder, resultUrl, context);
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
    
    /**
     * 设置餐厅列表RecyclerView
     */
    private void setupFoodRecyclerView(ChatAdapter.ChatViewHolder holder, FoodList foodList, ChatAdapterContext context) {
        if (holder.foodRecyclerView != null && foodList.getList() != null) {
            // 设置线性布局管理器，完全保持与原有逻辑一致
            holder.foodRecyclerView.setLayoutManager(new LinearLayoutManager(context.getContext()));
            
            // 创建并设置FoodItemAdapter
            ChatFoodAdapter adapter = new ChatFoodAdapter(foodList.getList());
            holder.foodRecyclerView.setAdapter(adapter);
            
            Timber.tag(TAG).d( "setupFoodRecyclerView: Set up food list with " + foodList.getList().size() + " items");
        } else {
            Timber.tag(TAG).w("setupFoodRecyclerView: foodRecyclerView is null or food list is empty");
        }
    }
    
    /**
     * 设置更多餐厅按钮
     */
    private void setupMoreButton(ChatAdapter.ChatViewHolder holder, String resultUrl, ChatAdapterContext context) {
        if (holder.foodMore != null) {
            holder.foodMore.setOnClickListener(view -> {
                if (resultUrl != null && !resultUrl.isEmpty()) {
                    try {
                        // 使用系统浏览器打开更多链接，保持与原有逻辑一致
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(resultUrl));
                        view.getContext().startActivity(intent);
                        
                        Timber.tag(TAG).d( "setupMoreButton: Opened more link: " + resultUrl);
                    } catch (Exception e) {
                        Timber.tag(TAG).e( "setupMoreButton: Failed to open more link", e);
                    }
                } else {
                    Timber.tag(TAG).w("setupMoreButton: More link URL is empty");
                }
            });
        } else {
            Timber.tag(TAG).w( "setupMoreButton: foodMore button is null");
        }
    }
}