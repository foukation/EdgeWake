package com.fxzs.lingxiagent.view.drawing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingMessageDto;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * AI绘画对话适配器
 */
public class DrawingChatAdapter extends RecyclerView.Adapter<DrawingChatAdapter.MessageViewHolder> {
    
    private List<DrawingMessageDto> messages = new ArrayList<>();
    private OnMessageActionListener listener;
    private OnScrollToBottomListener scrollToBottomListener;
    
    public interface OnMessageActionListener {
        void onDownloadClick(DrawingMessageDto message);
        void onContinueEditClick(DrawingMessageDto message);
        void onImageClick(DrawingMessageDto message);
    }
    
    public interface OnScrollToBottomListener {
        void onScrollToBottom();
    }
    
    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.listener = listener;
    }
    
    public void setOnScrollToBottomListener(OnScrollToBottomListener scrollToBottomListener) {
        this.scrollToBottomListener = scrollToBottomListener;
    }
    
    public void setMessages(List<DrawingMessageDto> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }
    
    public void addMessage(DrawingMessageDto message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
        
        // 自动滚动到底部
        if (scrollToBottomListener != null) {
            scrollToBottomListener.onScrollToBottom();
        }
    }
    
    public void updateLastMessage(DrawingMessageDto message) {
        if (!messages.isEmpty()) {
            messages.set(messages.size() - 1, message);
            notifyItemChanged(messages.size() - 1);
            
            // 如果更新的是图片完成状态，自动滚动到底部
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty() && scrollToBottomListener != null) {
                scrollToBottomListener.onScrollToBottom();
            }
        }
    }
    
    public DrawingMessageDto getLastMessage() {
        if (!messages.isEmpty()) {
            return messages.get(messages.size() - 1);
        }
        return null;
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drawing_message, parent, false);
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        DrawingMessageDto message = messages.get(position);
        holder.bind(message);
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserMessage;
        LinearLayout llAiMessage;
        TextView tvAiMessage;
        LinearLayout llProgress;
        TextView tvProgressPercentage;
        ProgressBar progressBar;
        CardView cvImage;
        ImageView ivGeneratedImage;
        LinearLayout llActions;
        ImageButton btnDownload;
        TextView tvContinueEdit;
        ImageView ivSparkleBig;
        ImageView ivSparkleSmall;
        
        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserMessage = itemView.findViewById(R.id.tv_user_message);
            llAiMessage = itemView.findViewById(R.id.ll_ai_message);
            tvAiMessage = itemView.findViewById(R.id.tv_ai_message);
            llProgress = itemView.findViewById(R.id.ll_progress);
            tvProgressPercentage = itemView.findViewById(R.id.tv_progress_percentage);
            progressBar = itemView.findViewById(R.id.progress_bar);
            cvImage = itemView.findViewById(R.id.cv_image);
            ivGeneratedImage = itemView.findViewById(R.id.iv_generated_image);
            llActions = itemView.findViewById(R.id.ll_actions);
            btnDownload = itemView.findViewById(R.id.btn_download);
            tvContinueEdit = itemView.findViewById(R.id.tv_continue_edit);
            ivSparkleBig = itemView.findViewById(R.id.iv_sparkle_big);
            ivSparkleSmall = itemView.findViewById(R.id.iv_sparkle_small);
        }
        
        void bind(DrawingMessageDto message) {
            // 用户消息
            if (message.isUserMessage()) {
                tvUserMessage.setVisibility(View.VISIBLE);
                tvUserMessage.setText(message.getText());
                llAiMessage.setVisibility(View.GONE);
            } else {
                // AI消息
                tvUserMessage.setVisibility(View.GONE);
                llAiMessage.setVisibility(View.VISIBLE);
                
                // 文字回复
                if (message.getText() != null && !message.getText().isEmpty()) {
                    tvAiMessage.setVisibility(View.VISIBLE);
                    tvAiMessage.setText(message.getText());
                } else {
                    tvAiMessage.setVisibility(View.GONE);
                }
                
                // 根据消息中的比例信息调整容器尺寸
                adjustContainerSize(message);
                
                // 进度显示
                if (message.isGenerating()) {
                    llProgress.setVisibility(View.VISIBLE);
                    tvProgressPercentage.setText(message.getProgress() + "%");
                    if (progressBar != null) {
                        progressBar.setProgress(message.getProgress());
                    }
                    
                    // 动态调整进度显示位置：垂直居中基础上向下偏移30dp
                    adjustProgressPosition();
                    
                    // 可以在这里添加星星动画
                } else {
                    llProgress.setVisibility(View.GONE);
                }
                
                // 图片显示
                if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    cvImage.setVisibility(View.VISIBLE);
                    llActions.setVisibility(View.VISIBLE);
                    
                    Glide.with(itemView.getContext())
                            .load(message.getImageUrl())
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(ivGeneratedImage);
                    
                    // 设置点击事件
                    ivGeneratedImage.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onImageClick(message);
                        }
                    });
                    
                    btnDownload.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onDownloadClick(message);
                        }
                    });
                    
                    tvContinueEdit.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onContinueEditClick(message);
                        }
                    });
                } else {
                    cvImage.setVisibility(View.GONE);
                    llActions.setVisibility(View.GONE);
                }
            }
        }
        
        /**
         * 根据图片比例调整容器尺寸
         */
        private void adjustContainerSize(DrawingMessageDto message) {
            if (message.getAspectRatio() == null && message.getRatio() == null) {
                return; // 没有比例信息，使用默认尺寸
            }
            
            // 解析比例信息
            String ratioStr = message.getAspectRatio() != null ? message.getAspectRatio() : message.getRatio();
            float aspectRatio = parseAspectRatio(ratioStr);
            
            if (aspectRatio <= 0) {
                return; // 无效的比例，使用默认尺寸
            }
            
            // 计算容器尺寸
            int baseWidth = dpToPx(240); // 基准宽度
            int containerWidth, containerHeight;
            
            if (aspectRatio >= 1.0f) {
                // 横向图片
                containerWidth = baseWidth;
                containerHeight = (int) (baseWidth / aspectRatio);
            } else {
                // 竖向图片
                int baseHeight = dpToPx(320); // 基准高度
                containerHeight = baseHeight;
                containerWidth = (int) (baseHeight * aspectRatio);
            }
            
            // 限制最小和最大尺寸
            containerWidth = Math.max(dpToPx(180), Math.min(containerWidth, dpToPx(300)));
            containerHeight = Math.max(dpToPx(200), Math.min(containerHeight, dpToPx(400)));
            
            // 应用尺寸到进度容器
            ViewGroup.LayoutParams progressParams = llProgress.getLayoutParams();
            progressParams.width = containerWidth;
            progressParams.height = containerHeight;
            llProgress.setLayoutParams(progressParams);
            
            // 应用尺寸到图片容器
            ViewGroup.LayoutParams imageParams = cvImage.getLayoutParams();
            imageParams.width = containerWidth;
            imageParams.height = containerHeight;
            cvImage.setLayoutParams(imageParams);
        }
        
        /**
         * 解析宽高比字符串
         */
        private float parseAspectRatio(String ratioStr) {
            if (ratioStr == null || ratioStr.isEmpty()) {
                return 0f;
            }
            
            try {
                if (ratioStr.contains(":")) {
                    // 格式如 "9:16", "16:9" 等
                    String[] parts = ratioStr.split(":");
                    if (parts.length == 2) {
                        float width = Float.parseFloat(parts[0]);
                        float height = Float.parseFloat(parts[1]);
                        return width / height;
                    }
                } else {
                    // 格式如 "1.5", "0.75" 等
                    return Float.parseFloat(ratioStr);
                }
            } catch (NumberFormatException e) {
                Timber.tag("DrawingChatAdapter").e( "Failed to parse aspect ratio: " + ratioStr, e);
            }
            
            return 0f;
        }
        
        /**
         * 动态调整进度显示位置：垂直居中基础上向下偏移30dp
         */
        private void adjustProgressPosition() {
            if (llProgress != null) {
                ViewGroup.LayoutParams params = llProgress.getLayoutParams();
                
                // 获取当前消息，用于计算容器高度
                int containerHeight = params.height;
                if (containerHeight <= 0) {
                    containerHeight = dpToPx(320); // 默认高度
                }
                
                if (params instanceof android.widget.RelativeLayout.LayoutParams) {
                    android.widget.RelativeLayout.LayoutParams relativeParams = 
                        (android.widget.RelativeLayout.LayoutParams) params;
                    
                    // 先清除之前的规则  
                    relativeParams.removeRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
                    relativeParams.removeRule(android.widget.RelativeLayout.CENTER_HORIZONTAL);
                    relativeParams.removeRule(android.widget.RelativeLayout.CENTER_VERTICAL);
                    
                    // 设置水平居中
                    relativeParams.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL);
                    
                    // 计算垂直居中位置并向下偏移30dp
                    int centerY = containerHeight / 2;
                    int offsetY = dpToPx(30);
                    relativeParams.topMargin = centerY + offsetY - dpToPx(20); // 减去进度内容高度的一半
                    relativeParams.leftMargin = 0;
                    relativeParams.rightMargin = 0;
                    relativeParams.bottomMargin = 0;
                    
                    llProgress.setLayoutParams(relativeParams);
                }
            }
        }

        /**
         * dp转px
         */
        private int dpToPx(int dp) {
            float density = itemView.getContext().getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }
}