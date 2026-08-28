package com.fxzs.lingxiagent.view.user;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.HistoryMenuPopup;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int VIEW_TYPE_DATE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_LOADING = 2;
    
    private List<HistoryItem> items = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    private boolean isDrawingTab = false;
    private OnMoreActionListener onMoreActionListener;
    
    public void setItems(List<HistoryItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }
    
    public void setIsDrawingTab(boolean isDrawingTab) {
        this.isDrawingTab = isDrawingTab;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    public interface OnMoreActionListener {
        void onMoreAction(View anchor, HistoryItem item, int actionType);
    }
    
    public void setOnMoreActionListener(OnMoreActionListener listener) {
        this.onMoreActionListener = listener;
    }
    
    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        if (viewType == VIEW_TYPE_DATE_HEADER) {
            View view = inflater.inflate(R.layout.item_history_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            View view = inflater.inflate(R.layout.item_history_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_history, parent, false);
            return new ItemViewHolder(view, onItemClickListener, onMoreActionListener);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HistoryItem item = items.get(position);
        
        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).bind(item);
        } else if (holder instanceof LoadingViewHolder) {
            // Loading view holder doesn't need binding
        } else if (holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).bind(item, isDrawingTab);
        }
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDate;
        
        DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
        
        void bind(HistoryItem item) {
            tvDate.setText(item.getTitle());
        }
    }
    
    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
    
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivAvatar;
        private TextView tvTitle;
        private ImageView ivMore;
        private OnItemClickListener clickListener;
        private OnMoreActionListener moreActionListener;
        
        ItemViewHolder(@NonNull View itemView, OnItemClickListener clickListener, OnMoreActionListener moreActionListener) {
            super(itemView);
            this.clickListener = clickListener;
            this.moreActionListener = moreActionListener;
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            ivMore = itemView.findViewById(R.id.ivMore);
        }
        
        void bind(HistoryItem item, boolean isDrawingTab) {
            tvTitle.setText(item.getTitle());
            
            // 设置点击事件
            itemView.setOnClickListener(v -> {
                Timber.tag("HistoryAdapter").e( "=== 历史记录项被点击 ===");
                Timber.tag("HistoryAdapter").e( "标题: " + item.getTitle());
                Timber.tag("HistoryAdapter").e( "SessionId: " + item.getSessionId());
                Timber.tag("HistoryAdapter").e( "ConversationId: " + item.getConversationId());
                Timber.tag("HistoryAdapter").e( "MeetingId: " + item.getMeetingId());
                Timber.tag("HistoryAdapter").e( "PptSessionId: " + item.getPptSessionId());
                Timber.i("=== HistoryAdapter: 点击历史记录 - " + item.getTitle() + " ===");

                if (clickListener != null) {
                    // 根据不同的数据类型判断是否可以点击
                    boolean canClick = (item.getSessionId() != null) ||
                                     (item.getConversationId() != null) ||
                                     (item.getMeetingId() != null) ||
                                     (item.getPptSessionId() != null);  // 添加PPT支持

                    Timber.tag("HistoryAdapter").e( "canClick: " + canClick);

                    if (canClick) {
                        Timber.tag("HistoryAdapter").e( "=== 触发点击监听器 ===");
                        clickListener.onItemClick(item);
                    } else {
                        Timber.tag("HistoryAdapter").e( "=== 点击被过滤，没有有效ID ===");
                    }
                }
            });
            
            // 显示图片
            ivAvatar.setVisibility(View.VISIBLE);

            // 处理图片显示
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                // 加载网络图片（智能体头像、AI绘画图片等）
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .transform(new CenterCrop(), new RoundedCorners(8))
                        .placeholder(isDrawingTab ? R.drawable.ic_nav_drawing : R.drawable.ic_app_logo)
                        .error(isDrawingTab ? R.drawable.ic_nav_drawing : R.drawable.ic_app_logo)
                        .into(ivAvatar);
            } else if (item.getAvatarResId() != 0) {
                // 加载本地资源图片
                ivAvatar.setImageResource(item.getAvatarResId());
            } else {
                // 设置默认图片
                if (isDrawingTab) {
                    ivAvatar.setImageResource(R.drawable.ic_nav_drawing);
                } else {
                    // 对话和智能体Tab使用ic_app_logo作为默认头像
                    ivAvatar.setImageResource(R.drawable.ic_app_logo);
                }
            }
            
            ivMore.setOnClickListener(v -> {
                Timber.tag("HistoryAdapter").d( "ivMore clicked, isDrawingTab=" + isDrawingTab + ", moreActionListener=" + (moreActionListener != null));
                if (moreActionListener != null) {
                    // 使用自定义弹窗
                    HistoryMenuPopup menuPopup =
                        new HistoryMenuPopup(
                            itemView.getContext(),
                            item,
                            new HistoryMenuPopup.OnMenuItemClickListener() {
                                @Override
                                public void onViewDetail(HistoryItem item) {
                                    moreActionListener.onMoreAction(ivMore, item, 0);
                                }

                                @Override
                                public void onRename(HistoryItem item) {
                                    moreActionListener.onMoreAction(ivMore, item, 1);
                                }

                                @Override
                                public void onDelete(HistoryItem item) {
                                    moreActionListener.onMoreAction(ivMore, item, 2);
                                }
                            }
                        );
                    menuPopup.showAsDropDown(ivMore);
                }
            });
        }
    }
    
    public interface OnItemClickListener {
        void onItemClick(HistoryItem item);
    }
}