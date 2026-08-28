package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.util.TimberUtils;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;
import com.fxzs.lingxiagent.util.ZUtil.MarkdownUtils;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.util.markdown.ChatMarkdownRenderer;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.fxzs.lingxiagent.view.chat.SuperChatFragment;

import io.noties.markwon.Markwon;

/**
 * AI消息委托
 * 负责处理AI回复消息的显示和交互，包括：
 * - Markdown内容渲染
 * - 思考状态显示
 * - 操作按钮处理（播放、复制、分享等）
 * - 流式更新支持
 * - TTS功能（继承自TTSAwareDelegate）
 */
public class AIMessageDelegate extends TTSAwareDelegate {

    private static final String TAG = "AIMessageDelegate";

    // Markdown相关
    private ChatMarkdownRenderer chatMarkdownRenderer;
    private Markwon markwon;
    private Markwon markwonForLx;

    // TTS状态管理已移至基类 TTSAwareDelegate

    public AIMessageDelegate() {
        super(ChatAdapter.TYPE_AI, R.layout.item_ai_message);
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }

    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message,
                                            int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder aiHolder = (ChatAdapter.ChatViewHolder) holder;

//        Timber.tag(TAG).d( "setAIMessage: position=" + position + ", message length=" +
//                (message.getMessage() != null ? message.getMessage().length() : 0));

        // 设置思考内容
        setupThinkingContent(aiHolder, message, context);

        // 设置思考状态显示
        setupThinkingState(aiHolder, message);

        // 设置状态相关UI
        setupStatusUI(aiHolder, message, position, context);

        // 设置操作按钮
        setupActionButtons(aiHolder, message, position, context);

        // 渲染Markdown内容
        setupMarkdownContent(aiHolder, message, context,position);

        if (message.isTTSPlaying()) {
            startPlayAnimation(aiHolder);
            aiHolder.iv_chat_play.setClickable(true);
        } else {
            resetPlayButton(aiHolder);
            aiHolder.iv_chat_play.setClickable(true);
        }

    }

    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }

    /**
     * 设置思考内容
     */
    private void setupThinkingContent(ChatAdapter.ChatViewHolder holder, ChatMessage message, ChatAdapterContext context) {
        holder.tv_thinking_title.setText(message.getThinkMessageTitle());

        ensureMarkwon(context);
        String cotMessage = message.getThinkMessage();
        holder.tv_thinking.setOnLongClickListener(context.getOnLongClickListener());
        if (cotMessage != null ) {

            if (cotMessage.contains("灵犀智能体") || cotMessage.contains("同城出行")) {
                String cotMessageInt = cotMessage.replaceAll("\n{2,}", "\n");
                String filteredMarkdown = cotMessageInt.replaceAll("#+", "\n");
                String filteredMdn = filteredMarkdown.replaceAll("(?m)^- \\*\\*(.+?)\\*\\*", "- $1");
                TimberUtils.logLong("tv_thinking",filteredMdn);
                markwonForLx.setMarkdown(holder.tv_thinking, filteredMdn);
            } else {
                markwon.setMarkdown(holder.tv_thinking, cotMessage);
            }
        }

        ImageUtil.load(context.getContext(), R.mipmap.thinking_dash, holder.iv_thinking_dash);
    }

    /**
     * 设置思考状态显示
     */
    private void setupThinkingState(ChatAdapter.ChatViewHolder holder, ChatMessage message) {
        holder.iv_chat_refresh.setVisibility(View.GONE);
        holder.iv_chat_think_start.setVisibility(View.GONE);
        holder.iv_thinking_arrow.setVisibility(View.VISIBLE);
        holder.iv_thinking_dash.setVisibility(View.VISIBLE);

        // 思考中时强制显示思考栏；结束后根据是否有思考内容决定显隐
        if (message.getStatus() == Constant.ThinkState.START) {
            holder.ll_thinking.setVisibility(View.VISIBLE);
        } else if (!"".equals(message.getThinkMessage())) {
            holder.ll_thinking.setVisibility(View.VISIBLE);
        } else {
            holder.ll_thinking.setVisibility(View.GONE);
        }

        // 控制思考内容显示/隐藏
        if (message.isHideThinking()) {
            holder.tv_thinking.setVisibility(View.GONE);
            ZUtils.setIvBg(holder.itemView.getContext(), holder.iv_thinking_arrow, R.mipmap.home_down_arrow);
        } else {
            holder.tv_thinking.setVisibility(View.VISIBLE);
            ZUtils.setIvBg(holder.itemView.getContext(), holder.iv_thinking_arrow, R.mipmap.home_up_arrow);
        }
    }

    /**
     * 设置状态相关UI
     */
    private void setupStatusUI(ChatAdapter.ChatViewHolder holder, ChatMessage message, int position, ChatAdapterContext context) {
        // 处理刷新按钮显示
        if (message.isHideActionRefresh() || position != (getItemCount(context) - 1)) {
            holder.iv_chat_refresh.setVisibility(View.GONE);
        } else {
            holder.iv_chat_refresh.setVisibility(View.VISIBLE);
        }

        // 根据状态设置UI
        switch (message.getStatus()) {
            case Constant.ThinkState.START:
                // START状态时，始终显示"正在思考中"作为加载指示器
                holder.tv_thinking_title.setText("正在思考中");
                holder.iv_chat_think_start.setVisibility(View.VISIBLE);
                holder.iv_thinking_arrow.setVisibility(View.GONE);
                holder.ll_actions.setVisibility(View.GONE);
                break;
            case Constant.ThinkState.THINKING:
                String thinkingTitle = message.getThinkMessageTitle();
                if (thinkingTitle != null && !thinkingTitle.isEmpty()) {
                    holder.tv_thinking_title.setText(thinkingTitle);
                } else {
                    holder.tv_thinking_title.setText("思考中");
                }
                holder.iv_thinking_arrow.setVisibility(View.GONE);
                holder.ll_actions.setVisibility(View.GONE);
                break;
            case Constant.ThinkState.END:
                if (position == (getItemCount(context) - 1) && !context.isSelectable()) {
                    holder.ll_actions.setVisibility(View.VISIBLE);
                } else {
                    ChatMessage endMessage = context.getMessage(getItemCount(context) - 1);
                    boolean condition = endMessage.getH5CardContent() != null
                            || endMessage.getUrl() != null
                            || endMessage.getHotelModels() != null
                            || endMessage.getOrderEntity() != null
                            || endMessage.getTrainEntities() != null
                            || endMessage.getPlandEntities() != null
                            || endMessage.getFoodList() != null
                            || endMessage.getPlanContent() != null;
                    if (position == (getItemCount(context) - 2) && condition) {
                        holder.ll_actions.setVisibility(View.VISIBLE);
                    } else {
                        holder.ll_actions.setVisibility(View.GONE);
                    }
                }

                // 检查是否有思考内容
                String thinkMessage = message.getThinkMessage();
                String thinkTitle = message.getThinkMessageTitle();
                boolean hasThinkingContent = (thinkMessage != null && !thinkMessage.isEmpty()) &&
                        (thinkTitle != null && !thinkTitle.isEmpty());

                if (hasThinkingContent) {
                    // 有思考内容时显示思考相关UI
                    holder.tv_thinking_title.setText(thinkTitle);
                    holder.iv_thinking_arrow.setVisibility(View.VISIBLE);
                } else {
                    // 没有思考内容时隐藏思考相关UI
                    holder.tv_thinking_title.setText("");
                    holder.iv_thinking_arrow.setVisibility(View.GONE);
                }

                holder.iv_thinking_dash.setVisibility(View.GONE);
                holder.iv_chat_think_start.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }

    /**
     * 设置操作按钮
     */
    private void setupActionButtons(ChatAdapter.ChatViewHolder holder, ChatMessage message, int position, ChatAdapterContext context) {
        // 针对会议类型隐藏分享按钮
        if (context.getChatType() == SuperChatFragment.TYPE_MEETING ||
                context.getChatType() == SuperChatFragment.TYPE_MEETING_QA) {
            holder.iv_chat_share.setVisibility(View.GONE);
        }

        // 播放按钮 - 使用基类的TTS功能
        setupTTSFeatures(holder, message, context);

        // 复制按钮
        setupCopyButton(holder, message, context);

        // 分享按钮
        setupShareButton(holder, message, position, context);

        // 刷新按钮
        setupRefreshButton(holder, message, position, context);

        // 导出按钮
        setupExportButton(holder, message, context);

        // 思考箭头点击事件
        setupThinkingArrowButton(holder, message);
    }

    // 播放按钮设置已移至基类 TTSAwareDelegate

    /**
     * 设置复制按钮
     */
    private void setupCopyButton(ChatAdapter.ChatViewHolder holder, ChatMessage message, ChatAdapterContext context) {
        // 设置复制按钮的正确状态
        if (holder.isCopyState) {
            holder.iv_chat_copy.setBackgroundResource(R.mipmap.chat_copy_success);
        } else {
            holder.iv_chat_copy.setBackgroundResource(R.mipmap.chat_copy);
        }

        holder.iv_chat_copy.setOnClickListener(view -> {
            if (holder.isCopyState) {
                return;
            }
            holder.isCopyState = true;
            ZUtils.copy(context.getContext(), message.getMessage());
            holder.iv_chat_copy.setBackgroundResource(R.mipmap.chat_copy_success);

            // 清除之前的重置任务
            if (holder.copyResetRunnable != null) {
                holder.itemView.removeCallbacks(holder.copyResetRunnable);
            }

            // 创建新的重置任务
            holder.copyResetRunnable = () -> {
                holder.isCopyState = false;
                holder.iv_chat_copy.setBackgroundResource(R.mipmap.chat_copy);
                holder.copyResetRunnable = null;
            };

            // 2秒后重置状态
            holder.itemView.postDelayed(holder.copyResetRunnable, 2000);
        });
    }

    /**
     * 设置分享按钮
     */
    private void setupShareButton(ChatAdapter.ChatViewHolder holder, ChatMessage message, int position, ChatAdapterContext context) {
        holder.iv_chat_share.setOnClickListener(view -> {
            context.onShareClick(holder, position);
        });
    }

    /**
     * 设置刷新按钮
     */
    private void setupRefreshButton(ChatAdapter.ChatViewHolder holder, ChatMessage message, int position, ChatAdapterContext context) {
        holder.iv_chat_refresh.setOnClickListener(view -> {
            if (context.getMsgActionCallback() != null) {
                // 查找对应的用户消息，与原来ChatAdapter的逻辑保持一致
                String userMessage = findUserMessageForRefresh(position, context);
//                if (userMessage != null) {
                if (message.isTranslationMsg()) {
                    context.getMsgActionCallback().refreshTranslation(userMessage, message.getFromLang(), message.getToLang());
                    return;
                }
                context.getMsgActionCallback().refresh(userMessage);
                if (message != null && message.getId() != null){
                    context.getMessageActionCallback().onDeleteMessage(message.getId().longValue(),false);
                }
//                }
            }
        });
    }

    /**
     * 查找用于刷新的用户消息
     * 从当前AI消息位置向前查找最近的用户消息
     */
    private String findUserMessageForRefresh(int position, ChatAdapterContext context) {
        // 从当前位置向前查找最近的用户消息
        for (int i = position - 1; i >= 0; i--) {
            ChatMessage msg = context.getMessage(i);
            if (msg != null) {
                int msgType = msg.getMsgType();
                // 检查是否为用户消息类型
                if (msgType == ChatAdapter.TYPE_USER ||
                        msgType == ChatAdapter.TYPE_USER_FILE_IMAGE ||
                        msgType == ChatAdapter.TYPE_USER_FILE) {

                    // 如果是文件消息，需要找到对应的文本消息
                    if (msgType == ChatAdapter.TYPE_USER_FILE_IMAGE ||
                            msgType == ChatAdapter.TYPE_USER_FILE) {
                        // 文件消息的文本内容通常在文件消息的前一个位置
                        if (i > 0) {
                            ChatMessage textMessage = context.getMessage(i - 1);
                            if (textMessage != null && textMessage.getMsgType() == ChatAdapter.TYPE_USER) {
                                return textMessage.getMessage();
                            } else {
                                // 如果没有找到对应的文本消息，使用文件消息本身的消息内容
                                return msg.getMessage() != null ? msg.getMessage() : "";
                            }
                        } else {
                            return msg.getMessage() != null ? msg.getMessage() : "";
                        }
                    } else {
                        // 普通文本消息
                        return msg.getMessage();
                    }
                }
            }
        }
        return null; // 没有找到用户消息
    }

    /**
     * 设置导出按钮
     */
    private void setupExportButton(ChatAdapter.ChatViewHolder holder, ChatMessage message, ChatAdapterContext context) {
        holder.iv_chat_export.setOnClickListener(view -> {
            // TODO: 实现导出功能
            context.showToast("导出功能开发中");
        });
    }

    /**
     * 设置思考箭头按钮
     */
    private void setupThinkingArrowButton(ChatAdapter.ChatViewHolder holder, ChatMessage message) {
        holder.iv_thinking_arrow.setOnClickListener(view -> {
            if (message.isHideThinking()) {
                message.setHideThinking(false);
                holder.tv_thinking.setVisibility(View.VISIBLE);
                ZUtils.setIvBg(holder.itemView.getContext(), holder.iv_thinking_arrow, R.mipmap.home_up_arrow);
            } else {
                message.setHideThinking(true);
                holder.tv_thinking.setVisibility(View.GONE);
                ZUtils.setIvBg(holder.itemView.getContext(), holder.iv_thinking_arrow, R.mipmap.home_down_arrow);
            }
        });
    }

    /**
     * 设置Markdown内容
     */
    private void setupMarkdownContent(ChatAdapter.ChatViewHolder holder, ChatMessage message, ChatAdapterContext context,int position) {
        // 新的直出容器渲染路径（替代 Markwon 的 RecyclerView）
        String messageContent = message.getMessage();
        if (messageContent != null && !messageContent.isEmpty()) {
            ensureMarkdownRenderer(context,position);
            if (holder.markdownContainer != null) {
                chatMarkdownRenderer.renderInto(holder.markdownContainer, messageContent);
            }
        } else {
            if (holder.markdownContainer != null) {
                holder.markdownContainer.removeAllViews();
            }
        }
    }

    // TTS播放状态处理已移至基类 TTSAwareDelegate

    /**
     * 确保Markwon实例存在
     */
    private void ensureMarkwon(ChatAdapterContext context) {
        if (markwon == null) {
            markwon = MarkdownUtils.createMarkwon(context.getContext());
        }
        if (markwonForLx == null) {
            markwonForLx = MarkdownUtils.createMdForLx(context.getContext());
        }
    }

    /**
     * 确保Markdown渲染器存在
     */
    private void ensureMarkdownRenderer(ChatAdapterContext context,int position) {

        if (chatMarkdownRenderer == null) {
            chatMarkdownRenderer = new ChatMarkdownRenderer(context.getContext());
            // 设置长按监听器，用于显示操作对话框
            ChatMessage message = context.getMessage( position- 1);
            if (message != null && message.getMsgType() == ChatAdapter.TYPE_USER_HEAD_AGENT){
                chatMarkdownRenderer.setOnLongClickListener(null);
            }else {
                chatMarkdownRenderer.setOnLongClickListener(context.getOnLongClickListener());
            }

        }
    }

    /**
     * 获取消息列表大小（用于判断是否是最后一条消息）
     */
    private int getItemCount(ChatAdapterContext context) {
        return context.getItemCount();
    }
}