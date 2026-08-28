package com.fxzs.lingxiagent.view.chat.delegate;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.util.ZDpUtils;
import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;

import timber.log.Timber;

/**
 * 绘画消息委托
 * 负责处理AI绘画消息显示，包括：
 * - 绘画图片的尺寸计算和适配
 * - 绘画进度显示和动画
 * - 绘画完成状态的UI切换
 * - 绘画操作按钮处理（查看、下载、继续生成、重新生成）
 */
public class DrawingMessageDelegate extends BaseViewTypeDelegate {
    
    private static final String TAG = "DrawingMessageDelegate";
    
    public DrawingMessageDelegate() {
        super(ChatAdapter.TYPE_AI_DRAWING, R.layout.item_ai_drawing);
    }
    
    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }
    
    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder drawingHolder = (ChatAdapter.ChatViewHolder) holder;
        
        Timber.tag(TAG).d( "setDrawingMessage: position=" + position + ", progress=" + message.getProgress());
        
        // 设置绘画图片尺寸和布局
        setupDrawingImageLayout(drawingHolder, message, context);
        
        // 根据进度状态设置UI显示
        setupProgressState(drawingHolder, message, context);
        
        // 设置绘画操作按钮
        setupDrawingActions(drawingHolder, message, position, context);
    }
    
    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
    
    /**
     * 设置绘画图片的尺寸和布局
     */
    private void setupDrawingImageLayout(ChatAdapter.ChatViewHolder holder, ChatMessage message, ChatAdapterContext context) {
        int targetWidth = 0;
        int targetHeight = 0;
        
        // 计算目标尺寸，完全保持与原有逻辑一致
        if (message.getDrawingImageDto() != null &&
                message.getDrawingImageDto().getWidth() != 0 && message.getDrawingImageDto().getHeight() != 0) {
            
            targetWidth = message.getDrawingImageDto().getWidth();
            targetHeight = message.getDrawingImageDto().getHeight();
            
            // 输出调试信息，保持与原有代码一致
            ZUtils.print("getDrawingImageDto targetWidth  = " + targetWidth);
            ZUtils.print("getDrawingImageDto targetHeight  = " + targetHeight);
            ZUtils.print("getDrawingImageDto getAspectRatio  = " + message.getDrawingImageDto().getAspectRatio());
            
            // 根据宽高比计算实际显示尺寸
            float aspectRatio = (float) targetHeight / targetWidth;
            targetWidth = ZDpUtils.dpToPx((Activity) context.getContext(), 180);
            targetHeight = (int) (targetWidth * aspectRatio);
            
            ZUtils.print(" targetWidth  = " + targetWidth);
            ZUtils.print(" targetHeight  = " + targetHeight);
            
            // 设置容器布局参数
            if (holder.rl_container != null) {
                ViewGroup.LayoutParams params = holder.rl_container.getLayoutParams();
                ZUtils.print(" params.width  = " + params.width);
                ZUtils.print(" params.height  = " + params.height);
                params.width = targetWidth;
                params.height = targetHeight;
                holder.rl_container.setLayoutParams(params);
                ZUtils.print(" after======= ");
                ZUtils.print(" params.width  = " + params.width);
                ZUtils.print(" params.height  = " + params.height);
                
                // 确保绘画ImageView跟随容器尺寸
                if (holder.iv_drawing != null) {
                    ViewGroup.LayoutParams imageParams = holder.iv_drawing.getLayoutParams();
                    imageParams.width = targetWidth;
                    imageParams.height = targetHeight;
                    holder.iv_drawing.setLayoutParams(imageParams);
                }
            }
        }
        
        // 将计算好的尺寸传递给进度状态处理方法
        setupProgressStateWithSize(holder, message, context, targetWidth, targetHeight);
    }
    
    /**
     * 根据进度状态设置UI显示
     */
    private void setupProgressState(ChatAdapter.ChatViewHolder holder, ChatMessage message, ChatAdapterContext context) {
        // 调用带尺寸参数的方法，保持代码结构清晰
        setupProgressStateWithSize(holder, message, context, 0, 0);
    }
    
    /**
     * 根据进度状态设置UI显示（包含尺寸参数）
     */
    private void setupProgressStateWithSize(ChatAdapter.ChatViewHolder holder, ChatMessage message, 
                                           ChatAdapterContext context, int targetWidth, int targetHeight) {
        if (message.getProgress() == 100) {
            // 绘画完成状态
            if (holder.rl_progress != null) {
                holder.rl_progress.setVisibility(View.GONE);
            }
            if (holder.ll_actions_drawing != null) {
                holder.ll_actions_drawing.setVisibility(View.VISIBLE);
            }
            
            // 加载完成的绘画图片
            if (holder.iv_drawing != null && message.getUrl() != null) {
                if (targetWidth > 0 && targetHeight > 0) {
                    ImageUtil.netRadiusXY(context.getContext(), message.getUrl(), holder.iv_drawing, targetWidth, targetHeight);
                } else {
                    ImageUtil.netRadius(context.getContext(), message.getUrl(), holder.iv_drawing);
                }
            }
        } else {
            // 绘画进行中状态
            if (holder.rl_progress != null) {
                holder.rl_progress.setVisibility(View.VISIBLE);
                // 动态调整进度显示位置：垂直居中基础上向下偏移30dp
                if (targetWidth > 0 && targetHeight > 0) {
                    android.widget.RelativeLayout.LayoutParams progressParams = 
                        (android.widget.RelativeLayout.LayoutParams) holder.rl_progress.getLayoutParams();
                    if (progressParams == null) {
                        progressParams = new android.widget.RelativeLayout.LayoutParams(
                            android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
                    }
                    
                    // 先清除之前的规则
                    progressParams.removeRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
                    progressParams.removeRule(android.widget.RelativeLayout.CENTER_HORIZONTAL);
                    progressParams.removeRule(android.widget.RelativeLayout.CENTER_VERTICAL);
                    
                    // 设置水平居中
                    progressParams.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL);
                    
                    // 计算垂直居中位置并向下偏移30dp
                    // 使用容器高度的一半加上偏移量作为top margin
                    int centerY = targetHeight / 2;
                    int offsetY = ZDpUtils.dpToPx((Activity) context.getContext(), 30);
                    progressParams.topMargin = centerY + offsetY - ZDpUtils.dpToPx((Activity) context.getContext(), 10); // 减去进度文本高度的一半
                    progressParams.leftMargin = 0;
                    progressParams.rightMargin = 0;
                    progressParams.bottomMargin = 0;
                    
                    holder.rl_progress.setLayoutParams(progressParams);
                }
            }
            if (holder.tv_progress != null) {
                holder.tv_progress.setText(message.getProgress() + "%");
            }
            if (holder.ll_actions_drawing != null) {
                holder.ll_actions_drawing.setVisibility(View.GONE);
            }
            
            // 显示加载动画，确保与最终图片尺寸一致
            if (holder.iv_drawing != null) {
                holder.iv_drawing.setImageDrawable(null);
                
                // 如果有目标尺寸，使用目标尺寸加载GIF，确保生成中的背景图与最终图片尺寸一致
                if (targetWidth > 0 && targetHeight > 0) {
                    // 首先设置ImageView的布局参数，确保容器尺寸正确
                    ViewGroup.LayoutParams params = holder.iv_drawing.getLayoutParams();
                    params.width = targetWidth;
                    params.height = targetHeight;
                    holder.iv_drawing.setLayoutParams(params);
                    
                    // 然后加载GIF，使用与最终图片相同的尺寸
                    ImageUtil.loadGifXY(context.getContext(), R.drawable.bg_imagine_loading, 
                                       holder.iv_drawing, targetWidth, targetHeight);
                } else {
                    // 没有明确尺寸时的备用加载方式
                    ImageUtil.loadGif(context.getContext(), R.drawable.bg_imagine_loading, holder.iv_drawing);
                }
            }
        }
    }
    
    /**
     * 设置绘画操作按钮
     */
    private void setupDrawingActions(ChatAdapter.ChatViewHolder holder, ChatMessage message, int position, ChatAdapterContext context) {
        // 查看绘画按钮
        if (holder.iv_drawing != null) {
            holder.iv_drawing.setOnClickListener(view -> {
                if (context.getMsgActionCallback() != null) {
                    context.getMsgActionCallback().viewDrawing(message);
                }
            });
            
            // 为图片添加长按监听器，使其可以触发删除菜单
            // 注意：这里不直接设置 OnLongClickListener，而是让 ChatAdapter 的统一长按处理机制生效
            // 通过设置 itemView 的长按监听器来触发
        }
        
        // 为整个 itemView 设置长按监听器，确保长按图片也能触发删除菜单
        if (holder.itemView != null) {
            // 让 ChatAdapter 的 mOnLongClickListener 能够处理这个 itemView
            // 这样长按图片区域也能触发删除菜单
        }
        
        // 下载绘画按钮
        if (holder.iv_chat_draw_download != null) {
            holder.iv_chat_draw_download.setOnClickListener(view -> {
                if (context.getMsgActionCallback() != null) {
                    context.getMsgActionCallback().downloadDrawing(message);
                }
            });
        }
        
        // 继续生成按钮
        if (holder.iv_chat_draw_continue != null) {
            holder.iv_chat_draw_continue.setOnClickListener(view -> {
                if (context.getMsgActionCallback() != null) {
                    context.getMsgActionCallback().continueDrawing(message);
                }
            });
        }
        
        // 重新生成按钮
        if (holder.iv_chat_refresh != null) {
            holder.iv_chat_refresh.setOnClickListener(view -> {
                if (context.getMsgActionCallback() != null) {
                    context.getMsgActionCallback().regenerateDrawing(message);
                }
            });
        }
        
        Timber.tag(TAG).d( "setupDrawingActions: Set up all drawing action buttons for position " + position);
    }
}