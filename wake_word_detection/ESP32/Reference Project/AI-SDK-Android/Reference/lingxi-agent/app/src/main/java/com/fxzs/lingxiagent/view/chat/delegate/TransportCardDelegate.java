package com.fxzs.lingxiagent.view.chat.delegate;

import android.net.Uri;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardPlandEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardTrainEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;

import java.util.List;

import timber.log.Timber;

/**
 * 交通工具卡片委托
 * 负责处理机票和火车票卡片的显示，包括：
 * - 机票/火车票列表的RecyclerView布局设置
 * - 缓存适配器的动态切换和数据更新
 * - 底部提示文本的动态设置
 * - 交通票务和更多链接的点击处理
 */
public class TransportCardDelegate extends CardMessageDelegate {
    
    private static final String TAG = "TransportCardDelegate";
    
    public TransportCardDelegate(int viewType) {
        super(viewType, R.layout.lingxi_card_travel_pland);
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder transportHolder = (ChatAdapter.ChatViewHolder) holder;
        
        if (getViewType() == ChatAdapter.TYPE_ASSISTANT_PLANE_CARD) {
            setupPlaneCard(transportHolder, message, context);
        } else if (getViewType() == ChatAdapter.TYPE_ASSISTANT_TRAIN_CARD) {
            setupTrainCard(transportHolder, message, context);
        }
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
    
    /**
     * 设置机票卡片
     */
    private void setupPlaneCard(ChatAdapter.ChatViewHolder holder, ChatMessage message, ChatAdapterContext context) {
        Timber.tag(TAG).d( "setAssistantPlandCard: setting up plane card");
        
        // 设置底部提示文本
        if (holder.tvPlandBottom != null) {
            String bottomText = String.format(context.getContext().getString(R.string.txt_plant_card_bottom_toast), "机");
            holder.tvPlandBottom.setText(bottomText);
        }
        
        // 获取机票数据
        List<ChatCardPlandEntity> plandEntities = message.getPlandEntities();
        if (plandEntities == null) {
            Timber.tag(TAG).w( "Plane entities list is null");
            return;
        }
        
        // 使用缓存的适配器，只更新数据，完全保持与原有逻辑一致
        if (holder.cachedPlandAdapter != null) {
            // 确保使用正确的适配器
            if (holder.plandRecyclerView.getAdapter() != holder.cachedPlandAdapter) {
                holder.plandRecyclerView.setAdapter(holder.cachedPlandAdapter);
            }
            
            holder.cachedPlandAdapter.setNewData(plandEntities);
            holder.cachedPlandAdapter.setOnItemClickListener(entity -> {
                if (entity != null && entity.getH5Url() != null) {
                    Uri uri = Uri.parse(entity.getH5Url());
                    String dDate = uri.getQueryParameter("ddate");
                    Boolean isExpired  = ZUtils.compareFormatDate(dDate + " " + entity.getDepTime());
                    if (Boolean.TRUE.equals(isExpired)) {
                        startWebViewActivity(context.getContext(), entity.getH5Url(), "灵犀-出行规划");
                    } else {
                        ZUtils.showToast("当前机票日期已过期，请选择最新机票日期");
                    }
                    Timber.tag(TAG).d( "Plane item clicked: " + entity.getH5Url());
                }
            });
            
            // 设置更多按钮
            setupMoreButton(holder, plandEntities, context);
            
            Timber.tag(TAG).d( "setupPlaneCard: Updated plane list with " + plandEntities.size() + " items");
        } else {
            Timber.tag(TAG).w("setupPlaneCard: cachedPlandAdapter is null");
        }
    }
    
    /**
     * 设置火车票卡片
     */
    private void setupTrainCard(ChatAdapter.ChatViewHolder holder, ChatMessage message, ChatAdapterContext context) {
        Timber.tag(TAG).d( "setAssistantTrainCard: setting up train card");
        
        // 设置底部提示文本
        if (holder.tvPlandBottom != null) {
            String bottomText = String.format(context.getContext().getString(R.string.txt_plant_card_bottom_toast), "火车");
            holder.tvPlandBottom.setText(bottomText);
        }
        
        // 获取火车票数据
        List<ChatCardTrainEntity> trainEntities = message.getTrainEntities();
        if (trainEntities == null) {
            Timber.tag(TAG).w( "Train entities list is null");
            return;
        }
        
        // 使用缓存的适配器，只更新数据，完全保持与原有逻辑一致
        if (holder.cachedTrainAdapter != null) {
            // 确保使用正确的适配器
            if (holder.plandRecyclerView.getAdapter() != holder.cachedTrainAdapter) {
                holder.plandRecyclerView.setAdapter(holder.cachedTrainAdapter);
            }
            
            holder.cachedTrainAdapter.setNewData(trainEntities);
            holder.cachedTrainAdapter.setOnItemClickListener(entity -> {
                String startDate = entity.getStartDate();
                Boolean isExpired  = ZUtils.compareFormatDate(startDate + " " + entity.getStart_time());
                if (Boolean.TRUE.equals(isExpired)) {
                    startWebViewActivity(context.getContext(), entity.getH5Url(), "灵犀-出行规划");
                } else {
                    ZUtils.showToast("当前车票日期已过期，请选择最新车票日期");
                }
            });
            
            // 设置更多按钮（火车票）
            setupTrainMoreButton(holder, trainEntities, context);
            
            Timber.tag(TAG).d( "setupTrainCard: Updated train list with " + trainEntities.size() + " items");
        } else {
            Timber.tag(TAG).w( "setupTrainCard: cachedTrainAdapter is null");
        }
    }
    
    /**
     * 设置机票更多按钮
     */
    private void setupMoreButton(ChatAdapter.ChatViewHolder holder, List<ChatCardPlandEntity> plandEntities, ChatAdapterContext context) {
        if (holder.tvPlandMore != null) {
            holder.tvPlandMore.setOnClickListener(view -> {
                if (plandEntities != null && !plandEntities.isEmpty()) {
                    String moreUrl = plandEntities.get(0).getMoreUrl();
                    if (moreUrl != null) {
                        Uri uri = Uri.parse(moreUrl);
                        String dDate = uri.getQueryParameter("ddate");
                        if (dDate!= null && !dDate.isEmpty()) {
                            Boolean isExpired  = ZUtils.compareFormatDate(dDate + " 12:00");
                            if (Boolean.TRUE.equals(isExpired)) {
                                startWebViewActivity(context.getContext(), moreUrl, "灵犀-出行规划");
                            } else {
                                ZUtils.showToast("当前机票日期已过期，请选择最新机票日期");
                            }
                        }
                        Timber.tag(TAG).d( "Plane more button clicked: " + moreUrl);
                    }
                }
            });
        }
    }
    
    /**
     * 设置火车票更多按钮
     */
    private void setupTrainMoreButton(ChatAdapter.ChatViewHolder holder, List<ChatCardTrainEntity> trainEntities, ChatAdapterContext context) {
        if (holder.tvPlandMore != null) {
            holder.tvPlandMore.setOnClickListener(view -> {
                if (trainEntities != null && !trainEntities.isEmpty()) {
                    String moreUrl = trainEntities.get(0).getMoreUrl();
                    if (moreUrl != null) {
                        Uri uri = Uri.parse(moreUrl);
                        String dDate = uri.getQueryParameter("dDate");
                        if (dDate!= null && !dDate.isEmpty()) {
                            Boolean isExpired  = ZUtils.compareFormatDate(dDate + " 12:00");
                            if (Boolean.TRUE.equals(isExpired)) {
                                startWebViewActivity(context.getContext(), moreUrl, "灵犀-出行规划");
                            } else {
                                ZUtils.showToast("当前车票日期已过期，请选择最新车票日期");
                            }
                        }
                        Timber.tag(TAG).d( "Train more button clicked: " + moreUrl);
                    }
                }
            });
        }
    }
}