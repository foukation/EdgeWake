package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardHotelModel;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.fxzs.lingxiagent.view.chat.ChatHotelAdapter;

import java.util.List;

import timber.log.Timber;

/**
 * 酒店卡片委托
 * 负责处理酒店推荐卡片的显示，包括：
 * - 酒店列表的RecyclerView布局设置
 * - 缓存适配器的数据更新和管理
 * - 酒店项目和更多链接的点击处理
 * - WebView页面跳转功能
 */
public class HotelCardDelegate extends CardMessageDelegate {
    
    private static final String TAG = "HotelCardDelegate";
    
    public HotelCardDelegate() {
        super(ChatAdapter.TYPE_ASSISTANT_HOTEL_CARD, R.layout.lingxi_card_travel_hotel);
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder hotelHolder = (ChatAdapter.ChatViewHolder) holder;
        
        Timber.tag(TAG).d( "setAssistantHotelCard: position=" + position);
        
        // 获取酒店数据
        List<ChatCardHotelModel> hotelModels = message.getHotelModels();
        if (hotelModels == null) {
            Timber.tag(TAG).w("Hotel models list is null, cannot display hotel card");
            return;
        }
        
        // 设置酒店列表
        setupHotelList(hotelHolder, hotelModels, context);
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
    
    /**
     * 设置酒店列表，使用缓存的适配器
     */
    private void setupHotelList(ChatAdapter.ChatViewHolder holder, List<ChatCardHotelModel> hotelModels, ChatAdapterContext context) {
        // 使用缓存的适配器，只更新数据，完全保持与原有逻辑一致
        if (holder.cachedHotelAdapter != null) {
            holder.cachedHotelAdapter.setNewData(hotelModels);
            
            // 设置点击监听器
            holder.cachedHotelAdapter.setOnItemClickListener(new ChatHotelAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(ChatCardHotelModel hotelModel) {
                    if (hotelModel != null && hotelModel.getH5Url() != null) {
                        startWebViewActivity(context.getContext(), hotelModel.getH5Url(), "灵犀-出行规划");
                        Timber.tag(TAG).d( "onItemClick: Opened hotel details: " + hotelModel.getH5Url());
                    }
                }
                
                @Override
                public void onMoreClick(ChatCardHotelModel hotelModel) {
                    if (hotelModel != null && hotelModel.getMoreUrl() != null) {
                        startWebViewActivity(context.getContext(), hotelModel.getMoreUrl(), "灵犀-出行规划");
                        Timber.tag(TAG).d( "onMoreClick: Opened hotel more info: " + hotelModel.getMoreUrl());
                    }
                }
            });
            
            Timber.tag(TAG).d( "setupHotelList: Updated hotel list with " + hotelModels.size() + " hotels");
        } else {
            Timber.tag(TAG).w( "setupHotelList: cachedHotelAdapter is null");
        }
    }
}