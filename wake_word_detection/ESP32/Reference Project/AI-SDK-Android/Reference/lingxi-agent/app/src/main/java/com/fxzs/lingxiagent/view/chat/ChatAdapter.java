package com.fxzs.lingxiagent.view.chat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.HomeModelEntity;
import com.fxzs.lingxiagent.lingxi.main.utils.GsonUtils;
import com.fxzs.lingxiagent.model.chat.callback.MsgActionCallback;
import com.fxzs.lingxiagent.model.chat.callback.OnFileItemClick;
import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.model.chat.dto.IconTextItem;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepository;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepositoryImpl;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.DeviceUUIDGenerator;
import com.fxzs.lingxiagent.util.FileCopyUtil;
import com.fxzs.lingxiagent.util.ZDpUtils;
import com.fxzs.lingxiagent.util.ZUtil.CodeBlockPlugin;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.ZUtil.MarkdownRenderer;
import com.fxzs.lingxiagent.util.ZUtil.MarkdownUtils;
import com.fxzs.lingxiagent.util.ZUtil.SimpleSelectEntry;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.util.audio.MediaPlayerUtils;
import com.fxzs.lingxiagent.util.audio.OnPlayerListener;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.util.markdown.ChatMarkdownRenderer;
import com.fxzs.lingxiagent.util.media.MediaPreviewHandler;
import com.fxzs.lingxiagent.view.chat.delegate.ChatAdapterContext;
import com.fxzs.lingxiagent.view.chat.delegate.ViewTypeDelegate;
import com.fxzs.lingxiagent.view.chat.delegate.ViewTypeDelegateFactory;
import com.fxzs.lingxiagent.view.chat.delegate.ViewTypeManager;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.common.HtmlPreviewActivity;
import com.fxzs.lingxiagent.view.common.WebViewActivity;
import com.fxzs.lingxiagent.view.dialog.ChatTextActionDialog;
import com.fxzs.lingxiagent.view.dialog.TextSelectorView;
import com.fxzs.smartassist.util.ZUtil.ScreenUtils;
import com.lingxi.cardhelper.CardView;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.node.FencedCodeBlock;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.noties.markwon.Markwon;
import io.noties.markwon.recycler.MarkwonAdapter;
import timber.log.Timber;
// Individual delegate imports removed - now using ViewTypeDelegateFactory

/**
 * ChatAdapter - 聊天消息适配器
 * <p>
 * 使用委托模式处理不同类型的聊天消息显示和交互。
 * 每种消息类型都有专门的委托类处理其布局创建、数据绑定和交互逻辑。
 * <p>
 * 架构：
 * - ViewTypeManager: 管理所有消息类型委托
 * - ChatAdapterContext: 为委托提供适配器功能访问接口
 * - ViewTypeDelegateFactory: 使用单例模式管理委托实例
 * - BaseViewTypeDelegate: 委托基类，提供通用功能
 * <p>
 * 支持的消息类型：
 * - 用户消息 (UserMessageDelegate)
 * - AI消息 (AIMessageDelegate)
 * - 文件消息 (FileMessageDelegate, ImageMessageDelegate)
 * - 绘画消息 (DrawingMessageDelegate)
 * - 卡片消息 (各种CardDelegate)
 * - 头部消息 (AgentHeadDelegate, MeetingHeadDelegate, HomeHeadDelegate)
 * - 权限消息 (FloatPermissionCardDelegate, AccessibilityPermissionCardDelegate)
 * <p>
 * 扩展指南：参考 .kiro/docs/chatadapter-delegate-guide.md
 *
 * @see ViewTypeDelegate
 * @see ViewTypeManager
 * @see ChatAdapterContext
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> implements ChatAdapterContext {
    private static final String TAG = "ChatAdapter";
    public static final int TYPE_USER = 0;// 用户-普通消息
    public static final int TYPE_AI = 1;// ai-文字消息
    public static final int TYPE_USER_HEAD_AGENT = 2;// 用户-智能体头部（固定头部）
    public static final int TYPE_AI_DRAWING = 3;// ai-绘画消息
    public static final int TYPE_USER_FILE = 4;// 用户-文件
    public static final int TYPE_USER_FILE_IMAGE = 5;// 用户-图片
    public static final int TYPE_USER_HEAD_MEETING = 6;// 用户-智能问答头部（固定头部）
    public static final int TYPE_USER_HEAD_HOME = 7;// 用户-主页头部（固定头部）
    public static final int TYPE_ASSISTANT_IMG = 8; // 助手-灵犀生成图片集
    public static final int TYPE_ASSISTANT_CARD = 9; // 助手-灵犀智能体卡片
    public static final int TYPE_ASSISTANT_FOOD_CARD = 10; // 助手-灵犀智能体餐厅卡片
    public static final int TYPE_ASSISTANT_HOTEL_CARD = 11; // 酒店
    public static final int TYPE_ASSISTANT_PLANE_CARD = 12; // 机票
    public static final int TYPE_ASSISTANT_TRAIN_CARD = 13; // 火车票
    public static final int TYPE_ASSISTANT_ORDER_CARD = 14; // 订票
    public static final int TYPE_ASSISTANT_FLOAT_PERM_CARD = 15; // 悬浮球
    public static final int TYPE_ASSISTANT_ACC_PERM_CARD = 17; // 无障碍
    public static final int TYPE_ASSISTANT_H5_CARD = 16; // 无障碍

    public static final int TYPE_DEEP_RESEARCH = 18;
    public static final int TYPE_DEEP_RESEARCH_COMPLETE = 19;
    public static final int TYPE_ASSISTANT_PLAN_CARD = 20;
    public static final int TYPE_NETWORK_ERROR = 21;
    private static final int REFRESH_DELAY = 3000;
    private static final int POPUP_HEIGHT = 160;
    private static final int POPUP_WIDTH = 128;
    private static final int POPUP_DISTANCE = 16;
    public static final int TYPE_MUSIC = 22;//音乐

    private Markwon markwon = null;
    private MarkdownRenderer markdownRenderer;

    // New markdown renderer
    private ChatMarkdownRenderer chatMarkdownRenderer;

    private List<ChatMessage> chatMessages;
    public boolean isSelectable = false;
    Context context;

    // 保持对renderer的引用以便清理
    private final List<MarkdownRenderer> activeRenderers = new ArrayList<>();

    // 缓存MarkwonAdapter实例以避免重复创建和闪屏
    private MarkwonAdapter cachedMarkwonAdapter;
    private Markwon cachedUnifiedMarkwon;

    // 为每个ViewHolder缓存适配器实例，避免流式更新时的闪屏 - 使用WeakHashMap防止内存泄漏
    private final Map<RecyclerView.ViewHolder, MarkwonAdapter> viewHolderAdapterCache = new WeakHashMap<>();
    private final Map<RecyclerView.ViewHolder, String> lastRenderedContent = new WeakHashMap<>();

    // 缓存已渲染的Spanned内容，用于增量更新 - 使用WeakHashMap防止内存泄漏
    private final Map<RecyclerView.ViewHolder, CharSequence> lastRenderedSpanned = new WeakHashMap<>();

    // 记录每个ViewHolder对应的内部adapter itemCount，用于精准刷新 - 使用WeakHashMap防止内存泄漏
    private final Map<RecyclerView.ViewHolder, Integer> lastItemCounts = new WeakHashMap<>();

    // 共享池：减少子 RecyclerView 的创建/销毁与测量抖动
    private final RecycledViewPool sharedPool = new RecycledViewPool();

    // ViewTypeManager for delegate pattern
    private final ViewTypeManager viewTypeManager;

    MsgActionCallback callback;

    private int deepResearchListSize = 0;
    public int position = -1;

    // 消息操作回调接口
    public interface OnMessageActionCallback {
        void onDeleteMessage(long messageId,boolean isNeedCallBack);
        void onDeleteSuccessCallBack();
    }

    private OnMessageActionCallback messageActionCallback;
    private ChatFileAdapter chatFileAdapter;
    private boolean isCopy = false, isSpeak = false;
    ;
    private View mSelectorView;
    private HomeModelEntity.ModelType modelType;
    private int type;
    private DeepResearchAdapter deepResearchAdapter;
    private String lastReqId = "";
    private MediaPreviewHandler mediaPreviewHandler;

    public ChatAdapter(Context context, List<ChatMessage> chatMessages) {
        Timber.tag(TAG).d("ChatAdapter: Constructor called");
        this.context = context;
        this.chatMessages = chatMessages;

        // Initialize ViewTypeManager and register delegates
        this.viewTypeManager = new ViewTypeManager();
        initializeDelegates();

        markwon = MarkdownUtils.createMarkwon(context);
        markdownRenderer = new MarkdownRenderer(context);
        activeRenderers.add(markdownRenderer);
        // 初始化缓存的Markwon实例，避免重复创建
        initializeCachedMarkwon();

        // 启用稳定ID，减少视图复用导致的错位
        setHasStableIds(true);

        // 优化ViewHolder缓存机制 - 使用共享池提高内存效率
        // ViewHolderPoolManager会在RecyclerView设置时自动应用优化配置
        Timber.tag(TAG).d("ChatAdapter: ViewHolder caching optimization enabled");

        // 注册到内存优化器进行内存管理
        MemoryOptimizer.getInstance().initialize(context);
        MemoryOptimizer.getInstance().registerAdapter(this);

        // 初始化媒体预览处理器
        mediaPreviewHandler = new MediaPreviewHandler(context);

        // 设置当前实例，用于静态方法访问
        setCurrentInstance();

        chatFileAdapter = new ChatFileAdapter(context, null, ChatFileAdapter.TYPE_IMAGE, new OnFileItemClick() {
            @Override
            public void onItemClick(int position) {
                // 使用MediaPreviewHandler处理文件点击
                List<ChatFileBean> dataList = chatFileAdapter.getDataList();
                if (dataList != null && position >= 0 && position < dataList.size()) {
                    ChatFileBean fileBean = dataList.get(position);
                    mediaPreviewHandler.handleFileClick(fileBean);
                }
            }

            @Override
            public void onClose(int position) {
                // Default file close handling
            }
        });
        chatFileAdapter.setShowClose(false);
    }

    /**
     * 初始化所有消息类型委托
     * <p>
     * 使用ViewTypeDelegateFactory获取单例委托实例，提高内存效率。
     * 每种消息类型都注册对应的委托处理器：
     * <p>
     * 基础消息类型：
     * - TYPE_USER: UserMessageDelegate (用户消息)
     * - TYPE_AI: AIMessageDelegate (AI消息，支持Markdown和TTS)
     * - TYPE_USER_FILE: FileMessageDelegate (用户文件)
     * - TYPE_USER_FILE_IMAGE: ImageMessageDelegate (用户图片)
     * - TYPE_AI_DRAWING: DrawingMessageDelegate (AI绘画)
     * - TYPE_ASSISTANT_IMG: AssistantImageDelegate (助手图片集)
     * <p>
     * 头部消息类型：
     * - TYPE_USER_HEAD_AGENT: AgentHeadDelegate (智能体头部)
     * - TYPE_USER_HEAD_MEETING: MeetingHeadDelegate (会议头部)
     * - TYPE_USER_HEAD_HOME: HomeHeadDelegate (主页头部)
     * <p>
     * 卡片消息类型：
     * - TYPE_ASSISTANT_CARD: WebViewCardDelegate (WebView卡片)
     * - TYPE_ASSISTANT_FOOD_CARD: FoodCardDelegate (餐厅卡片)
     * - TYPE_ASSISTANT_HOTEL_CARD: HotelCardDelegate (酒店卡片)
     * - TYPE_ASSISTANT_PLANE_CARD: TransportCardDelegate (机票卡片)
     * - TYPE_ASSISTANT_TRAIN_CARD: TransportCardDelegate (火车票卡片)
     * - TYPE_ASSISTANT_ORDER_CARD: OrderCardDelegate (订票卡片)
     * - TYPE_ASSISTANT_H5_CARD: H5CardDelegate (H5动态卡片)
     * - TYPE_ASSISTANT_FLOAT_PERM_CARD: FloatPermissionCardDelegate (悬浮窗权限)
     * - TYPE_ASSISTANT_ACC_PERM_CARD: AccessibilityPermissionCardDelegate (无障碍权限)
     */
    private void initializeDelegates() {
        try {
            // Register delegates using singleton factory for memory optimization
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getUserMessageDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getAIMessageDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getFileMessageDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getImageMessageDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getDrawingMessageDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getAssistantImageDelegate());

            // Register Head Message Delegates
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getAgentHeadDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getMeetingHeadDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getHomeHeadDelegate());

            // Register Card Message Delegates
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getWebViewCardDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getFoodCardDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getHotelCardDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getPlaneCardDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getTrainCardDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getOrderCardDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getPlanCardDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getH5CardDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getFloatPermissionCardDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getAccessibilityPermissionCardDelegate());
            viewTypeManager.registerDelegate(ViewTypeDelegateFactory.getMusicCardDelegate());

            Timber.tag(TAG).d("Initialized delegates successfully");
        } catch (Exception e) {
            Timber.tag(TAG).e("Failed to initialize delegates" + e);
        }
    }

    private void ensureMarkdownRenderer() {
        if (chatMarkdownRenderer == null) {
            chatMarkdownRenderer = new ChatMarkdownRenderer(context);
            chatMarkdownRenderer.setOnLongClickListener(mOnLongClickListener);
        }
    }

    public void setCallback(MsgActionCallback callback) {
        this.callback = callback;
    }

    public void setMessageActionCallback(OnMessageActionCallback messageActionCallback) {
        this.messageActionCallback = messageActionCallback;
    }

    public List<ChatMessage> getSelectMessages() {
        List<ChatMessage> list = new ArrayList<>();
        if (chatMessages != null) {
            for (int i = 0; i < chatMessages.size(); i++) {
                if (chatMessages.get(i).getIsSelected()) {
                    list.add(chatMessages.get(i));
                }
            }
        }
        return list;
    }

    public List<Integer> getSelectPositions() {
        List<Integer> list = new ArrayList<>();
        if (chatMessages != null) {
            for (int i = 0; i < chatMessages.size(); i++) {
                if (chatMessages.get(i).getIsSelected()) {
                    list.add(i);
                }
            }
        }
        return list;
    }

    public void setSelectState(boolean isSelectable) {
        this.isSelectable = isSelectable;
        if (isSelectable) {
            cleanSelectStatus();
        }
        // 使用更高效的通知方式，只更新可见项目
        notifyItemRangeChanged(0, getItemCount());
    }

    public void deleteSelectData() {
        if (chatMessages != null) {
            // 收集要删除的位置，从后往前删除以避免索引问题
            List<Integer> positionsToDelete = new ArrayList<>();
            for (int i = chatMessages.size() - 1; i >= 0; i--) {
                if (chatMessages.get(i).getIsSelected()) {
                    positionsToDelete.add(i);
					if (chatMessages.get(i).getId() != null) {
						deleteChatMessage(chatMessages.get(i).getId());
					}
                }
            }

            // 从后往前删除项目并通知
            for (int position : positionsToDelete) {
                chatMessages.remove(position);
                notifyItemRemoved(position);
            }

            // 如果有删除操作，更新剩余项目的位置
            if (!positionsToDelete.isEmpty()) {
                int minPosition = Collections.min(positionsToDelete);
                notifyItemRangeChanged(minPosition, getItemCount() - minPosition);
            }

            if (messageActionCallback !=null){
                messageActionCallback.onDeleteSuccessCallBack();
            }
        }
    }

    public void closeSelectView() {
        if (mSelectorView != null && mSelectorView instanceof TextSelectorView) {
            ((TextSelectorView) mSelectorView).setTextIsSelectable(false);
        }
    }

    private void cleanSelectStatus() {
        if (chatMessages != null) {
            for (int i = 0; i < chatMessages.size(); i++) {
                chatMessages.get(i).setIsSelected(false);
            }
        }
    }

    /**
     * 初始化缓存的Markwon实例，避免重复创建导致的闪屏
     */
    private void initializeCachedMarkwon() {
        try {
            // 创建缓存的 MarkwonAdapter
            cachedMarkwonAdapter = MarkwonAdapter
                    .builder(SimpleSelectEntry.create(R.layout.item_default, R.id.text_view, mOnLongClickListener))
                    .include(TableBlock.class, CodeBlockPlugin.createAdvancedTableEntry(context,
                            R.layout.mobile_style_table_final,
                            R.id.mobile_style_table_final,
                            R.layout.mobile_table_cell))
                    .include(FencedCodeBlock.class, CodeBlockPlugin.createCodeBlockEntry(context))
                    .build();

            // 创建缓存的 Markwon 实例
            cachedUnifiedMarkwon = MarkdownUtils.createMarkwon(context);

            Timber.tag(TAG).d("initializeCachedMarkwon: Cached Markwon instances initialized successfully");
        } catch (Exception e) {
            Timber.tag(TAG).e("initializeCachedMarkwon: Failed to initialize cached instances" + e);
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Try to use delegate pattern first
        if (viewTypeManager.hasDelegate(viewType)) {
            try {
                ViewTypeDelegate delegate = viewTypeManager.getDelegate(viewType);
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                RecyclerView.ViewHolder viewHolder = delegate.onCreateViewHolder(parent, inflater);

                // If the delegate returns a non-ChatViewHolder, we need to wrap it
                if (viewHolder instanceof ChatViewHolder) {
                    // 记录委托成功创建的ViewHolder使用情况
                    recordViewHolderUsage(viewType);
                    return (ChatViewHolder) viewHolder;
                } else {
                    Timber.tag(TAG).w("Delegate returned non-ChatViewHolder, using fallback: "
                            + viewHolder.getClass().getSimpleName());
                }
            } catch (Exception e) {
                Timber.tag(TAG).e("Failed to use delegate for viewType: " + viewType, e);
                // Fall back to original logic
            }
        }

        // Original logic as fallback
        int res = 0;
        if (viewType == TYPE_USER) {
            res = R.layout.item_user_message;
        } else if (viewType == TYPE_AI) {
            res = R.layout.item_ai_message;
        } else if (viewType == TYPE_AI_DRAWING) {
            res = R.layout.item_ai_drawing;
        } else if (viewType == TYPE_USER_FILE_IMAGE) {
            res = R.layout.item_chat_file_rv;
        } else if (viewType == TYPE_USER_FILE) {
            res = R.layout.item_chat_file_rv;
        } else if (viewType == TYPE_USER_HEAD_MEETING) {
            res = R.layout.item_meeting_head_message;
        } else if (viewType == TYPE_USER_HEAD_HOME) {
            res = R.layout.lingxi_card_top_describe;
        } else if (viewType == TYPE_ASSISTANT_IMG) {
            res = R.layout.lingxi_chat_pictures;
        } else if (viewType == TYPE_ASSISTANT_CARD) {
            res = R.layout.lingxi_card_webview;
        } else if (viewType == TYPE_ASSISTANT_FOOD_CARD) {
            res = R.layout.lingxi_card_party_restaurant;
        } else if (viewType == TYPE_ASSISTANT_HOTEL_CARD) {
            res = R.layout.lingxi_card_travel_hotel;
        } else if (viewType == TYPE_ASSISTANT_PLANE_CARD) {
            res = R.layout.lingxi_card_travel_pland;
        } else if (viewType == TYPE_ASSISTANT_PLAN_CARD) {
            res = R.layout.lingxi_card_travel_plan;
        } else if (viewType == TYPE_ASSISTANT_TRAIN_CARD) {
            res = R.layout.lingxi_card_travel_pland;
        } else if (viewType == TYPE_ASSISTANT_ORDER_CARD) {
            res = R.layout.lingxi_card_travel_order;
        } else if (viewType == TYPE_ASSISTANT_H5_CARD) {
            res = R.layout.lingxi_card_web;
        } else if (viewType == TYPE_ASSISTANT_FLOAT_PERM_CARD) {
            res = R.layout.lingxi_card_permission_float;
        } else if (viewType == TYPE_ASSISTANT_ACC_PERM_CARD) {
            res = R.layout.lingxi_card_permission_accessibility;
        } else if (viewType == TYPE_DEEP_RESEARCH) {
            res = R.layout.deep_research_chat;
        } else if (viewType == TYPE_DEEP_RESEARCH_COMPLETE) {
            res = R.layout.deep_research_complete_card;
        } else if (viewType == TYPE_NETWORK_ERROR) {
            res = R.layout.network_error_card;
        } else if (viewType == TYPE_MUSIC) {
            res = R.layout.item_message_received_media;
        } else {
            res = R.layout.item_agent_message;
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(res, parent, false);

        // 记录ViewHolder使用情况，用于缓存优化
        recordViewHolderUsage(viewType);

        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position, List<Object> payloads) {

        if (position >=1 && chatMessages.get(position-1).getMsgType() == TYPE_USER_HEAD_AGENT){
            holder.itemView.setOnLongClickListener(null);
        }else {
            holder.itemView.setOnLongClickListener(mOnLongClickListener);
        }

        if (!payloads.isEmpty()) {
            ChatMessage item = chatMessages.get(position);
            // 检查是否为流式更新
            if (isStreamingUpdate(payloads)) {
                boolean immediate = isImmediateUpdate(payloads);
                Timber.tag(TAG).d("onBindViewHolder: Handling streaming update for position " + position + ", immediate="
                        + immediate);

                // 流式更新：先用轻量 TextView 占位显示，完成后一次性替换为完整 Markdown，尽量避免屏闪
                if (item.getMsgType() == ChatAdapter.TYPE_AI) {

                    String newContent = item.getMessage() == null ? "" : item.getMessage();
                    TextView tv = ensureStreamingTextView(holder);
                    if (tv != null) {
                        String old = lastRenderedContent.get(holder);
                        boolean firstShow = TextUtils.isEmpty(old);
                        if (old != null && newContent.startsWith(old)) {
                            tv.append(newContent.substring(old.length()));
                        } else {
                            tv.setText(newContent);
                        }
                        // 流式阶段显示占位 TextView，隐藏 Markdown 容器，避免层叠影响动画观感
                        tv.setVisibility(View.VISIBLE);
                        holder.markdownContainer.setVisibility(View.GONE);

                        // 移除淡入动画，直接显示
                        lastRenderedContent.put(holder, newContent);

                        lastRenderedContent.put(holder, newContent);
                    }
                    // 合并调度真正的 Markdown 渲染（离屏构建一次性替换）：仅在结束时或需要立即更新时进行
                    if (holder.pendingUpdateRunnable != null) {
                        holder.markdownContainer.removeCallbacks(holder.pendingUpdateRunnable);
                    }
                    if (immediate || item.getStatus() == Constant.ThinkState.END) {
                        Runnable r = () -> {
                            if (holder.getBindingAdapterPosition() != position)
                                return;
                            ensureMarkdownRenderer();
                            chatMarkdownRenderer.renderInto(holder.markdownContainer, newContent);
                            // 切回 Markdown 容器，隐藏占位 TextView
                            holder.markdownContainer.setVisibility(View.VISIBLE);
                            TextView tvStreaming = ensureStreamingTextView(holder);
                            if (tvStreaming != null)
                                tvStreaming.setVisibility(View.GONE);
                            holder.markdownContainer.setTag(null);
                        };
                        holder.pendingUpdateRunnable = r;
                        holder.markdownContainer.postDelayed(r, immediate ? 16 : 180);
                    }
                } else {
                    setUI(holder, item, position);
                }
            } else {
                // 其他类型的payload更新
                setUI(holder, item, position);
            }
        } else {
            // 如果没有 payloads，还是返回默认的整个刷新
            onBindViewHolder(holder, position);
        }
        // RecyclerView 点击事件在 setAction 方法中设置
    }

    private void setUI(ChatViewHolder holder, ChatMessage item, int position) {
        // 只处理没有委托的消息类型：TYPE_DEEP_RESEARCH 和 TYPE_DEEP_RESEARCH_COMPLETE
        if (item.getMsgType() == ChatAdapter.TYPE_DEEP_RESEARCH) {
//            Timber.tag(TAG).d("setDeepResearch");
            setDeepResearch(holder, item, position);
        } else if (item.getMsgType() == ChatAdapter.TYPE_DEEP_RESEARCH_COMPLETE) {
            Timber.tag(TAG).d("setDeepResearch complete");
            setDeepResearchComplete(holder, item, position);
        } else {
            // 所有其他类型都应该有对应的委托处理
            Timber.tag(TAG).w("setUI called for message type that should have delegate: " + item.getMsgType());

            // 尝试使用委托模式作为后备
            if (viewTypeManager.hasDelegate(item.getMsgType())) {
                try {
                    ViewTypeDelegate delegate = viewTypeManager.getDelegate(item.getMsgType());
                    delegate.onBindViewHolder(holder, item, position, this);
                    return;
                } catch (Exception e) {
                    Timber.tag(TAG).e("Failed to use delegate for viewType: " + item.getMsgType(), e);
                }
            }

            Timber.tag(TAG).e("No handling available for message type: " + item.getMsgType());
        }
    }



    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingUpdate;

    private void postUpdate(Runnable update) {

        if (pendingUpdate != null) {
            handler.removeCallbacks(pendingUpdate);
        }

        pendingUpdate = () -> {
            update.run();
            pendingUpdate = null;
        };

        handler.postDelayed(pendingUpdate, 2000);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setDeepResearch(ChatViewHolder holder, ChatMessage item, int position) {
        String currentReqId = item.getDeepResearch().getReq_id();
        // 初始化LayoutManager（只需设置一次）
        if (holder.rv_deep_research.getLayoutManager() == null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
            layoutManager.setAutoMeasureEnabled(true);
            layoutManager.setItemPrefetchEnabled(false);
            holder.rv_deep_research.setLayoutManager(layoutManager);
            holder.rv_deep_research.setRecycledViewPool(sharedPool);
            holder.rv_deep_research.setHasFixedSize(false);
            holder.rv_deep_research.setItemViewCacheSize(50);
            holder.rv_deep_research.setItemAnimator(null);
        }

        // 初始化或更新Adapter
        if (holder.deepResearchAdapter == null || !currentReqId.equals(lastReqId)) {
            deepResearchListSize = item.getDeepResearch().getList().size();
            holder.deepResearchAdapter = new DeepResearchAdapter(context, item.getDeepResearch().getList());
            holder.rv_deep_research.setAdapter(holder.deepResearchAdapter);
        } else {
            int currentSize = item.getDeepResearch().getList().size();
            if (deepResearchListSize == currentSize) {
                holder.deepResearchAdapter.updateItems(item.getDeepResearch().getList(), item.getDeepResearch().getStep());
            } else {
                // postUpdate(() -> holder.deepResearchAdapter.notifyItemRangeChanged(currentSize - 2, 2));
                holder.deepResearchAdapter.notifyDataSetChanged();
            }
            deepResearchListSize = currentSize;
        }
        lastReqId = currentReqId;
        if (item.getDeepResearch().getTaskStatus() == 4) {
            holder.tv_task_status.setText("研究已完成");
        } else {
            holder.tv_task_status.setText("正在研究中");
        }
        holder.tv_see.setOnClickListener(v -> {
            String query = item.getDeepResearch().getQuery();
            String req_id = item.getDeepResearch().getReq_id();
            String device_id = DeviceUUIDGenerator.getDeviceUUID(context);
            String url = Constants.BASE_URL_DEEP_RESEARCH_DETAIL + "/ruminateH5/index.html/?query=" + query + "req_id=" + req_id
                    + "device_id=" + device_id + "#/";
            Timber.tag(TAG).d("onClick url: " + url);
            Intent intent = new Intent(context, WebViewActivity.class);
            intent.putExtra("extra_url", url);
            intent.putExtra("extra_title", query);
            context.startActivity(intent);

        });

    }

    private void setDeepResearchComplete(ChatViewHolder holder, ChatMessage item, int position) {
        holder.tv_query.setText(item.getDeepResearch().getQuery());
        holder.tv_content.setText(
                "深度研究报告服务已顺利完成，内容涵盖资料整理、核心原理解析及关键进展的系统梳理与综合分析，请点击下面文档查看。");
        holder.tv_file.setText(item.getDeepResearch().getQuery() + ".docx");
        // 这里需要云端返回文件类型
        // Drawable iconLeft = ContextCompat.getDrawable(context,
        // R.drawable.icon_file_pdf);
        // Drawable iconRight = ContextCompat.getDrawable(context,
        // R.drawable.icon_file_pdf);
        // holder.tv_file.setCompoundDrawablesWithIntrinsicBounds(iconLeft, null,
        // iconRight, null);
        // holder.tv_file.setCompoundDrawables(context.getDrawable(R.drawable.icon_file_pdf),null,null,null);
        FileCopyUtil.CopyListener listener = new FileCopyUtil.CopyListener() {
            @Override
            public void onProgress(int progress) {
                holder.pb_download.setVisibility(View.VISIBLE);
                holder.tv_progress_text.setVisibility(View.VISIBLE);
                holder.pb_download.setProgress(progress);
                holder.tv_progress_text.setText("下载进度" + progress + "%");
            }

            @Override
            public void onSuccess(File destFile) {
                // 复制成功后的操作
            }

            @Override
            public void onFailure(String errorMsg) {

            }
        };
        // /storage/emulated/0/Documents/com.fxzs.lingxiagent/你好.docx
        holder.tv_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                String sourcePath = "/storage/emulated/0/Documents/com.fxzs.lingxiagent/"
//                        + item.getDeepResearch().getQuery() + ".docx";
//                // 源文件路径（要复制的文件）
//                String destDir = "/storage/emulated/0/Documents/";
//                FileCopyUtil copyUtil = new FileCopyUtil(context);
//                copyUtil.setDelayMillis(1000);
//                copyUtil.copyFile(sourcePath, destDir, listener);
                String content = item.getDeepResearch().getReportContent();
//                Timber.tag(TAG).d("onClick report content: " + content);
                HtmlPreviewActivity.start(context, content, item.getDeepResearch().getQuery());
            }
        });
        holder.tv_content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = item.getDeepResearch().getQuery();
                String req_id = item.getDeepResearch().getReq_id();
                String device_id = DeviceUUIDGenerator.getDeviceUUID(context);
                String url = Constants.BASE_URL_DEEP_RESEARCH_DETAIL + "/ruminateH5/index.html/?query=" + query + "req_id=" + req_id
                        + "device_id=" + device_id + "#/";
                Timber.tag(TAG).d("onClick url: " + url);
                Intent intent = new Intent(context, WebViewActivity.class);
                intent.putExtra("extra_url", url);
                intent.putExtra("extra_title", query);
                context.startActivity(intent);
            }
        });
        holder.tv_complete_see.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = item.getDeepResearch().getQuery();
                String req_id = item.getDeepResearch().getReq_id();
                String device_id = DeviceUUIDGenerator.getDeviceUUID(context);
                String url = Constants.BASE_URL_DEEP_RESEARCH_DETAIL + "/ruminateH5/index.html/?query=" + query + "req_id=" + req_id
                        + "device_id=" + device_id + "#/";
                Timber.tag(TAG).d("onClick url: " + url);
                Intent intent = new Intent(context, WebViewActivity.class);
                intent.putExtra("extra_url", url);
                intent.putExtra("extra_title", query);
                context.startActivity(intent);

            }
        });


    }


    public void setType(int type) {
        this.type = type;
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        // Try to use delegate pattern first
        if (viewTypeManager.hasDelegate(message.getMsgType())) {
            try {
                ViewTypeDelegate delegate = viewTypeManager.getDelegate(message.getMsgType());
                delegate.onBindViewHolder(holder, message, position, this);

                // Still handle selection state for delegates that support it
                handleSelectionState(holder, message);
                return;
            } catch (Exception e) {
                Timber.tag(TAG).e("Failed to use delegate for viewType: " + message.getMsgType(), e);
                // Fall back to original logic
            }
        }

        // Original logic as fallback
        setUI(holder, message, position);
        handleSelectionState(holder, message);
    }

    /**
     * Handle selection state for message items
     */
    private void handleSelectionState(ChatViewHolder holder, ChatMessage message) {
        if (holder.radio != null && holder.radioSelected != null) {
            if (!isSelectable) {
                holder.radio.setVisibility(View.GONE);
                holder.radioSelected.setVisibility(View.GONE);
            } else if (message.getIsSelected()) {
                holder.radio.setVisibility(View.GONE);
                holder.radioSelected.setVisibility(View.VISIBLE);
            } else if (!message.getIsSelected()) {
                holder.radio.setVisibility(View.VISIBLE);
                holder.radioSelected.setVisibility(View.GONE);
            }
            if (holder.vwShareEmpty !=null){//分享
                if (isSelectable){
                    holder.vwShareEmpty.setVisibility(View.VISIBLE);
                }else {
                    holder.vwShareEmpty.setVisibility(View.GONE);
                }
                holder.vwShareEmpty.setOnClickListener(v -> switchShareSelect(holder,message));
            }


            holder.radio.setOnClickListener(v -> {
                v.setVisibility(View.GONE);
                holder.radioSelected.setVisibility(View.VISIBLE);
                message.setIsSelected(true);
            });
            holder.radioSelected.setOnClickListener(v -> {
                v.setVisibility(View.GONE);
                holder.radio.setVisibility(View.VISIBLE);
                message.setIsSelected(false);
            });


        }
    }

    @Override
    public void onViewRecycled(@NonNull ChatViewHolder holder) {
        super.onViewRecycled(holder);

        // 首先尝试通过委托模式处理回收
        try {
            int viewType = holder.getItemViewType();
            if (viewTypeManager.hasDelegate(viewType)) {
                viewTypeManager.onViewRecycled(viewType, holder, this);
                Timber.tag(TAG).d("onViewRecycled: Delegate handled recycling for viewType " + viewType);
            }
        } catch (Exception e) {
            Timber.tag(TAG).e("onViewRecycled: Failed to handle recycling through delegate" + e);
        }

        // 保持原有的通用清理逻辑作为后备
        // 取消挂在容器上的延迟更新，避免过期Runnable作用于复用后的Holder
        try {
            if (holder.pendingUpdateRunnable != null && holder.markdownContainer != null) {
                holder.markdownContainer.removeCallbacks(holder.pendingUpdateRunnable);
            }
            if (holder.pendingUpdateRunnable != null && holder.recyclerViewAi != null) {
                holder.recyclerViewAi.removeCallbacks(holder.pendingUpdateRunnable);
            }
        } catch (Exception ignore) {
        }
        holder.pendingUpdateRunnable = null;

        // 清理WebView，防止内存泄漏
        try {
            if (holder.webView != null) {
                // 从父视图中移除以避免异常
                ViewParent parent = holder.webView.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(holder.webView);
                }

                holder.webView.stopLoading();
                holder.webView.setWebViewClient(null);
                holder.webView.setWebChromeClient(null); // 同时清除 chrome client
                holder.webView.clearHistory();
                holder.webView.clearCache(true);
                holder.webView.loadUrl("about:blank");
                holder.webView.onPause();
                holder.webView.removeAllViews();

                // 这是关键步骤 - 完全销毁WebView
                holder.webView.destroy();
                // 注意：由于销毁了WebView，如果需要重用，需要在onCreateViewHolder中重新创建
            }
        } catch (Exception ignore) {
        }

        if (holder.markdownContainer != null) {
            holder.markdownContainer.setTag(null);
            // 可选：清空视图，降低残留风险
            // holder.markdownContainer.removeAllViews();
        }
        holder.clear();
        // 清理TTS相关资源
        try {
            if (holder.iv_chat_play != null) {
                // 停止TTS播放动画
                Drawable drawable = holder.iv_chat_play.getDrawable();
                if (drawable instanceof AnimationDrawable) {
                    ((AnimationDrawable) drawable).stop();
                }
                // 重置播放按钮状态
                holder.iv_chat_play.setImageResource(R.mipmap.chat_play);
            }
        } catch (Exception ignore) {
        }

        // 清理复制状态相关资源
        try {
            if (holder.copyResetRunnable != null && holder.iv_chat_copy != null) {
                holder.iv_chat_copy.removeCallbacks(holder.copyResetRunnable);
                holder.copyResetRunnable = null;
            }
            holder.isCopyState = false;
            if (holder.iv_chat_copy != null) {
                holder.iv_chat_copy.setBackgroundResource(R.mipmap.chat_copy);
            }
        } catch (Exception ignore) {
        }

        // 清理ViewHolder缓存，避免内存泄漏
        clearViewHolderCache(holder);
        Timber.tag(TAG).d("onViewRecycled: Cleared cache and removed pending callbacks for ViewHolder");
    }


    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return chatMessages.get(position).getMsgType();
    }

    @Override
    public long getItemId(int position) {
        ChatMessage msg = chatMessages.get(position);
        if (msg != null) {
            // 优先使用 ChatMessage 的 ID（如果存在）
            if (msg.getId() != null) {
                return msg.getId();
            }
            // 如果没有ID，使用消息内容和类型的哈希值作为稳定标识
            // 这比 System.identityHashCode 更稳定
            String identifier = msg.getMsgType() + "_" +
                    (msg.getMessage() != null ? msg.getMessage() : "") + "_" +
                    (msg.getUrl() != null ? msg.getUrl() : "") + "_" +
                    position; // 添加位置作为最后的区分因子
            return identifier.hashCode();
        }
        return RecyclerView.NO_ID;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {

        public ProgressBar planProgressBar;
        public LinearLayout llPlanLoading;
        public LinearLayout llPlanCard, llPlanLoadFail;
        public ImageView imagePlanTop;
        public LinearLayout llH5;
        public CardView cardView;
        private int mCardPos = -1;
        public TextView tv_thinking_title;
        public TextView tvOrderQuery, tv_thinking;
        public TextSelectorView messageText;
        public ConstraintLayout root_view;
        public LinearLayout ll_thinking;

        public LinearLayout ll_actions;
        public ImageView iv_chat_play;
        public ImageView iv_chat_copy;
        public ImageView iv_chat_share;
        public ImageView iv_chat_refresh;
        public ImageView iv_chat_export;
        public ImageView iv_chat_think_start;
        public ImageView iv_thinking_arrow;

        public ImageView iv_agent;
        public TextView tvPlandBottom, tvPlandMore, tv_agent_hint;
        public ImageView iv_thinking_dash;
        public ImageView iv_drawing;
        public TextView tv_progress;
        public View rl_progress;
        public View ll_actions_drawing;
        public View iv_chat_draw_continue;
        public View iv_chat_draw_download;

        public RecyclerView rv_file;
        public RecyclerView recyclerViewAi; // legacy
        public LinearLayout markdownContainer,llNexusPilot; // new container for direct view rendering

        public final RecyclerView imageListView;
        public final WebView webView;
        public final RecyclerView plandRecyclerView, hotelRecyclerView, foodRecyclerView;
        public final TextView foodMore;

        public final ImageView radio;
        public final ImageView radioSelected;

        // private RecyclerView imageListView;

        // 用于延迟更新的Runnable，避免频繁更新
        public Runnable pendingUpdateRunnable;
        public RelativeLayout rl_container;// 绘画图片的外壳

        public final Button btnRecharge;
        public final Button btnGenerateImage;
        public final Button btnHealth;
        public final Button btnSpring;
        public TextView textHi;

        public LinearLayout goOpenFloatBtn;
        public View goOpenAccBtn;
        public RecyclerView rv_deep_research;
        private TextView tv_query;
        private TextView tv_content;
        private TextView tv_file;
        private ProgressBar pb_download;
        private TextView tv_progress_text;
        private TextView tv_see;
        private TextView tv_complete_see;
        private TextView tv_task_status;
        public TextView tvPlanLoading, tvPlanAddress, tvPlanWhere, tvPlanTime, tvPlanMore, tvPlanTitle;

        // 缓存的适配器实例，避免在onBindViewHolder中重复创建
        public ChatFileAdapter cachedFileAdapter;
        public ChatHotelAdapter cachedHotelAdapter;
        public ChatPlandAdapter cachedPlandAdapter;
        public ChatTrainAdapter cachedTrainAdapter;
        public DeepResearchAdapter deepResearchAdapter;
        // 复制状态跟踪
        public boolean isCopyState = false;
        public Runnable copyResetRunnable;

        public ImageView playIv,goSuccessAcc,goSuccessFloat;
        public TextView albumName;
        public View vwShareEmpty;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_thinking_title = itemView.findViewById(R.id.tv_thinking_title);
            tv_thinking = itemView.findViewById(R.id.tv_thinking);
            tvOrderQuery = itemView.findViewById(R.id.id_tv_query);
            messageText = itemView.findViewById(R.id.messageText);
            root_view = itemView.findViewById(R.id.root_view);
            ll_thinking = itemView.findViewById(R.id.ll_thinking);
            ll_actions = itemView.findViewById(R.id.ll_actions);
            iv_chat_play = itemView.findViewById(R.id.iv_chat_play);
            iv_chat_copy = itemView.findViewById(R.id.iv_chat_copy);
            iv_chat_share = itemView.findViewById(R.id.iv_chat_share);
            iv_chat_refresh = itemView.findViewById(R.id.iv_chat_refresh);
            iv_chat_export = itemView.findViewById(R.id.iv_chat_export);
            iv_chat_think_start = itemView.findViewById(R.id.iv_chat_think_start);
            iv_thinking_arrow = itemView.findViewById(R.id.iv_thinking_arrow);
            iv_agent = itemView.findViewById(R.id.iv_agent);
            tvPlandMore = itemView.findViewById(R.id.id_tv_more);
            tvPlandBottom = itemView.findViewById(R.id.id_tv_card_bottom);
            tv_agent_hint = itemView.findViewById(R.id.tv_agent_hint);
            iv_thinking_dash = itemView.findViewById(R.id.iv_thinking_dash);
            iv_drawing = itemView.findViewById(R.id.iv_drawing);
            tv_progress = itemView.findViewById(R.id.tv_progress);
            rl_progress = itemView.findViewById(R.id.rl_progress);
            ll_actions_drawing = itemView.findViewById(R.id.ll_actions_drawing);
            iv_chat_draw_continue = itemView.findViewById(R.id.iv_chat_draw_continue);
            iv_chat_draw_download = itemView.findViewById(R.id.iv_chat_draw_download);
            rv_file = itemView.findViewById(R.id.rv_file);
            // recyclerViewAi removed in favor of markdownContainer
            recyclerViewAi = null;
            markdownContainer = itemView.findViewById(R.id.markdown_container);
            llNexusPilot = itemView.findViewById(R.id.ll_nexusPilot);

            imageListView = itemView.findViewById(R.id.rvImages);
            webView = itemView.findViewById(R.id.webView);
            rl_container = itemView.findViewById(R.id.rl_container);
            foodMore = itemView.findViewById(R.id.rvMore);
            foodRecyclerView = itemView.findViewById(R.id.rvMenuItems);
            hotelRecyclerView = itemView.findViewById(R.id.rvHotelItems);
            plandRecyclerView = itemView.findViewById(R.id.rvPlandItems);
            radio = itemView.findViewById(R.id.radio);
            radioSelected = itemView.findViewById(R.id.radio_check);
            btnRecharge = itemView.findViewById(R.id.btn_recharge);
            btnGenerateImage = itemView.findViewById(R.id.btn_generate_image);
            btnHealth = itemView.findViewById(R.id.btn_health);
            btnSpring = itemView.findViewById(R.id.btn_spring);
            // imageListView = itemView.findViewById(R.id.rvImages);
            rl_container = itemView.findViewById(R.id.rl_container);
            textHi = itemView.findViewById(R.id.text_hi);

            goOpenFloatBtn = itemView.findViewById(R.id.go_open_float);
            goOpenAccBtn = itemView.findViewById(R.id.go_open_acc);

            // deepresearch
            tv_task_status = itemView.findViewById(R.id.tv_task_status);
            rv_deep_research = itemView.findViewById(R.id.rv_deep_research);
            tv_query = itemView.findViewById(R.id.tv_query);
            tv_content = itemView.findViewById(R.id.tv_content);
            tv_file = itemView.findViewById(R.id.tv_file);
            pb_download = itemView.findViewById(R.id.pb_download);
            tv_progress_text = itemView.findViewById(R.id.tv_progress_text);
            tv_see = itemView.findViewById(R.id.tv_see);
            tv_complete_see = itemView.findViewById(R.id.tv_complete_see);
            llH5 = itemView.findViewById(R.id.ll_h5);
            imagePlanTop = itemView.findViewById(R.id.id_iv_plan);
            tvPlanTitle = itemView.findViewById(R.id.id_tv_plan_title);
            tvPlanMore = itemView.findViewById(R.id.id_tv_plan_more);
            tvPlanTime = itemView.findViewById(R.id.id_tv_plan_time);
            tvPlanWhere = itemView.findViewById(R.id.id_tv_plan_to_where);
            tvPlanAddress = itemView.findViewById(R.id.id_tv_plan_address);
            tvPlanLoading = itemView.findViewById(R.id.id_tv_plan_loading);
            llPlanLoading = itemView.findViewById(R.id.id_ll_plan_loading);
            llPlanLoadFail = itemView.findViewById(R.id.id_ll_plan_load_fail);
            llPlanCard = itemView.findViewById(R.id.id_ll_plan_card);
            planProgressBar = itemView.findViewById(R.id.id_pgb_plan);
            albumName = itemView.findViewById(R.id.album_name);
            playIv = itemView.findViewById(R.id.play_iv);
            vwShareEmpty = itemView.findViewById(R.id.vw_empty);
            goSuccessAcc = itemView.findViewById(R.id.go_success_acc);
            goSuccessFloat = itemView.findViewById(R.id.go_success_float);
            // 初始化缓存的适配器实例
            initializeCachedAdapters();
        }

        private void initializeCachedAdapters() {
            Context context = itemView.getContext();
            // 为文件RecyclerView创建缓存适配器
            if (rv_file != null) {
                cachedFileAdapter = new ChatFileAdapter(context, new ArrayList<>(), ChatFileAdapter.TYPE_IMAGE,
                        new OnFileItemClick() {
                            @Override
                            public void onItemClick(int position) {
                                Timber.tag("ChatAdapter").d("cachedFileAdapter onItemClick: position=" + position);
                                // 使用MediaPreviewHandler处理文件点击
                                List<ChatFileBean> dataList = cachedFileAdapter.getDataList();
                                Timber.tag("ChatAdapter").d("cachedFileAdapter dataList size: " +
                                        (dataList != null ? dataList.size() : "null"));
                                if (dataList != null && position >= 0 && position < dataList.size()) {
                                    ChatFileBean fileBean = dataList.get(position);
                                    Timber.tag("ChatAdapter").d("cachedFileAdapter calling handleFileClickStatic with: " +
                                            (fileBean != null ? fileBean.getName() : "null"));
                                    ChatAdapter.handleFileClickStatic(fileBean);
                                } else {
                                    Timber.tag(TAG).w("cachedFileAdapter invalid position or null dataList");
                                }
                            }

                            @Override
                            public void onClose(int position) {
                                // 处理关闭事件
                            }
                        });
                cachedFileAdapter.setShowClose(false);
                rv_file.setAdapter(cachedFileAdapter);
                rv_file.setLayoutManager(new GridLayoutManager(context, 1, GridLayoutManager.HORIZONTAL, false));
            }

            // 为酒店RecyclerView创建缓存适配器
            if (hotelRecyclerView != null) {
                cachedHotelAdapter = new ChatHotelAdapter(itemView.getContext());
                hotelRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                hotelRecyclerView.setAdapter(cachedHotelAdapter);

            }

            // 为机票RecyclerView创建缓存适配器
            if (plandRecyclerView != null) {
                cachedPlandAdapter = new ChatPlandAdapter(context);
                cachedTrainAdapter = new ChatTrainAdapter(context);
                plandRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                // 注意：机票和火车票共用同一个RecyclerView，所以在使用时需要动态设置适配器
            }
        }

        public void bindData(String data, String templateId, int pos) {
            if (mCardPos != pos) {
                mCardPos = pos;
                llH5.removeAllViews();
                cardView = new CardView(itemView.getContext());
                cardView.setCornerRadius(10);
                cardView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                llH5.addView(cardView);
                cardView.loadView(data);
            }
        }

        public void clear() {
            mCardPos = -1;
        }

    }


    /**
     * 检测消息是否包含表格内容
     */
    private boolean containsTable(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        // 检测Markdown表格格式
        return message.contains("|") && message.contains("---");
    }

    /**
     * 降级渲染方法
     */
    private void renderFallbackContent(ChatViewHolder holder, String message) {
        try {
            MarkwonAdapter fallbackAdapter = MarkwonAdapter
                    .builder(SimpleSelectEntry.create(R.layout.item_default, R.id.text_view, mOnLongClickListener))
                    .build();
            Markwon fallbackMarkwon = Markwon.create(context);
            fallbackAdapter.setMarkdown(fallbackMarkwon, message);

            if (holder.recyclerViewAi.getLayoutManager() == null) {
                holder.recyclerViewAi.setLayoutManager(new LinearLayoutManager(context));
            }

            holder.recyclerViewAi.post(() -> {
                holder.recyclerViewAi.setAdapter(fallbackAdapter);
                Timber.tag(TAG).d("renderFallbackContent: Fallback rendering successful");
            });
        } catch (Exception fallbackError) {
            Timber.tag(TAG).e("renderFallbackContent: Fallback rendering also failed" + fallbackError);
            // 最后的降级：显示错误信息
            try {
                MarkwonAdapter errorAdapter = MarkwonAdapter
                        .builder(SimpleSelectEntry.create(R.layout.item_default, R.id.text_view, mOnLongClickListener))
                        .build();
                errorAdapter.setMarkdown(Markwon.create(context), "**渲染失败**\n\n" + message);
                holder.recyclerViewAi.post(() -> holder.recyclerViewAi.setAdapter(errorAdapter));
            } catch (Exception finalError) {
                Timber.tag(TAG).e("renderFallbackContent: Final fallback also failed" + finalError);
            }
        }
    }


    /**
     * 取消所有正在进行的Markdown渲染
     */
    public void cancelAllMarkdownRendering() {
        Timber.tag(TAG).d("cancelAllMarkdownRendering: Cancelling all markdown rendering");

        // 取消主渲染器
        if (markdownRenderer != null) {
            markdownRenderer.cancel();
        }

        // 取消所有活动的渲染器
        for (MarkdownRenderer renderer : activeRenderers) {
            if (renderer != null) {
                renderer.cancel();
            }
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        Timber.tag(TAG).d("cleanup: Starting adapter cleanup");

        for (MarkdownRenderer renderer : activeRenderers) {
            if (renderer != null) {
                renderer.destroy();
            }
        }
        activeRenderers.clear();

        if (markdownRenderer != null) {
            markdownRenderer.destroy();
            markdownRenderer = null;
        }

        // 清理ChatMarkdownRenderer
        if (chatMarkdownRenderer != null) {
            chatMarkdownRenderer.cleanup();
            chatMarkdownRenderer = null;
        }

        // 清理ViewHolder缓存，避免内存泄漏
        viewHolderAdapterCache.clear();
        lastRenderedContent.clear();
        lastRenderedSpanned.clear();
        lastItemCounts.clear();

        // 清理ViewHolderPoolManager缓存，释放共享池资源
        ViewHolderPoolManager.clearCache();

        Timber.tag(TAG).d("cleanup: Adapter cleanup completed");
    }

    /**
     * 清理特定ViewHolder的缓存
     */
    public void clearViewHolderCache(ChatViewHolder holder) {
        viewHolderAdapterCache.remove(holder);
        lastRenderedContent.remove(holder);
        lastRenderedSpanned.remove(holder);
        lastItemCounts.remove(holder);
    }


    /**
     * 专门用于流式更新的优化方法（带强制更新选项）
     *
     * @param position       消息位置
     * @param newContent     新内容
     * @param forceImmediate 是否强制立即更新，跳过防抖机制
     */
    public void updateStreamingContent(int position, String newContent, boolean forceImmediate) {
        if (position < 0 || position >= chatMessages.size()) {
            return;
        }

        ChatMessage message = chatMessages.get(position);
        String oldContent = message.getMessage();
        message.setMessage(newContent);

        // 如果强制立即更新，使用特殊的payload
        String payload = forceImmediate ? "streaming_update_immediate" : "streaming_update";

        // 使用payload通知特定位置更新，避免整个item重新绑定
        notifyItemChanged(position, payload);

        Timber.tag(TAG).d("updateStreamingContent: Updated position " + position +
                ", increment=" + (newContent.length() - (oldContent != null ? oldContent.length() : 0)) +
                ", immediate=" + forceImmediate);
    }

    /**
     * 检查是否为流式更新的payload
     */
    private boolean isStreamingUpdate(List<Object> payloads) {
        return payloads != null && !payloads.isEmpty() &&
                (payloads.contains("streaming_update") || payloads.contains("streaming_update_immediate"));
    }

    /**
     * 检查是否为立即更新的payload
     */
    private boolean isImmediateUpdate(List<Object> payloads) {
        return payloads != null && !payloads.isEmpty() &&
                payloads.contains("streaming_update_immediate");
    }

    private OnShareClickListener mShareListener;
    private int mSharePos = -1;
    private ChatViewHolder mShareHolder = null;

    public interface OnShareClickListener {
        void onShareIconClick(int  pos);
    }

    public void setShareListener(OnShareClickListener listener) {
        mShareListener = listener;
    }

    public OnReplyClickListener replyClickListener;

    public void setReplyClickListener(OnReplyClickListener listener) {
        replyClickListener = listener;
    }

    public interface OnReplyClickListener {
        void onReplyClick(String reply);
    }

    private void showPopup(View anchor, String content) {
        Context context = anchor.getContext();
        View popupView = LayoutInflater.from(context)
                .inflate(R.layout.popup_refresh_options, null);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true);
        popupWindow.setOutsideTouchable(true);

        RecyclerView rvOptions = popupView.findViewById(R.id.rvOptions);
        rvOptions.setLayoutManager(new LinearLayoutManager(context));

        String[] optionTitles = context.getResources()
                .getStringArray(R.array.refresh_options);
        List<IconTextItem> options = new ArrayList<>();
        options.add(new IconTextItem(R.mipmap.refresh_retry, optionTitles[0]));
        options.add(new IconTextItem(R.mipmap.refresh_simplify, optionTitles[1]));
        options.add(new IconTextItem(R.mipmap.refresh_details, optionTitles[2]));
        options.add(new IconTextItem(R.mipmap.refresh_conversation, optionTitles[3]));

        rvOptions.setAdapter(new IconTextAdapter(options,
                position -> {
                    popupWindow.dismiss();
                    handleOptionClick(position, content);
                }));

        int xOffset = ZDpUtils.dpToPx2(context, POPUP_DISTANCE + POPUP_WIDTH);
        int yOffset = ZDpUtils.dpToPx2(context, POPUP_HEIGHT);
        popupWindow.showAsDropDown(anchor, -xOffset, -yOffset);
    }

    // 轻量占位渲染：在流式更新阶段减少屏闪，保持可滑动
    private TextView ensureStreamingTextView(ChatViewHolder holder) {
        if (holder.markdownContainer == null) {
            return null;
        }
        View v = holder.markdownContainer.getChildCount() > 0 ? holder.markdownContainer.getChildAt(0) : null;
        if (!(v instanceof TextView) || holder.markdownContainer.getTag() == null
                || !"streaming".equals(holder.markdownContainer.getTag())) {
            holder.markdownContainer.removeAllViews();
            TextView tv = new TextView(context);
            tv.setTextSize(16);
            tv.setLineSpacing(0, 1.2f);
            tv.setTextColor(0xFF333333);
            holder.markdownContainer.addView(tv);
            holder.markdownContainer.setTag("streaming");
            return tv;
        }
        return (TextView) v;
    }

    private void handleOptionClick(int position, String content) {
        if (position == 0) {
            if (callback != null) {
                callback.refresh(content);
            }
        } else {
            ZUtils.showToast("功能开发中...");
        }
    }

    private View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (isSelectable) {
                return false;
            }

            closeSelectView();
            mSelectorView = v;

            int[] location = new int[2];
            v.getLocationOnScreen(location);
            int y = location[1];

            // 查找并设置当前消息的位置
            ViewParent parent = v.getParent();
            Timber.tag("ChatAdapter").d("长按消息，位置: " + parent);
            int itemType = 0;
            while (parent != null) {
                if (parent instanceof RecyclerView) {
                    RecyclerView recyclerView = (RecyclerView) parent;
                    RecyclerView.ViewHolder viewHolder = recyclerView.findContainingViewHolder(v);
                    if (viewHolder != null) {
                        itemType = viewHolder.getItemViewType();
                        mSharePos = viewHolder.getAdapterPosition();
                        if (viewHolder instanceof ChatViewHolder) {
                            mShareHolder = (ChatViewHolder) viewHolder;
                        }
                        Timber.tag("ChatAdapter").d("长按消息，位置: " + mSharePos);
                        break;
                    }
                }
                parent = parent.getParent();
            }
            if (context instanceof Activity) {
                if (!((Activity) context).isFinishing() && (Build.VERSION.SDK_INT < 17 || !((Activity) context).isDestroyed())) {
	                ChatTextActionDialog dialog = getChatTextActionDialog(itemType);
	                if (dialog != null) {
						boolean isLandscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
						dialog.showAtLocation((isLandscape ? ScreenUtils.getScreenHeight(context) : ScreenUtils.getScreenWidth(context)) / 2 - ZDpUtils.dpToPx2(context, 130),
								y - (isLandscape ? ScreenUtils.getScreenWidth(context) : ScreenUtils.getScreenHeight(context)) / 2 + ZDpUtils.dpToPx2(context, context.getResources().getBoolean(R.bool.isTablet) ? 160 : 120));
						dialog.setOnActionClickListener(mActionClickListener);
					}
                }
            }
            return true;
        }

	    @Nullable
	    private ChatTextActionDialog getChatTextActionDialog(int itemType) {
		    ChatTextActionDialog dialog = null;
		    if (itemType == TYPE_USER || itemType == TYPE_AI || itemType == TYPE_AI_DRAWING)  {
				dialog = new ChatTextActionDialog(context, false);
			} else if(
				itemType == TYPE_ASSISTANT_ORDER_CARD ||
			    itemType == TYPE_ASSISTANT_TRAIN_CARD ||
			    itemType ==  TYPE_ASSISTANT_PLANE_CARD ||
			    itemType == TYPE_ASSISTANT_HOTEL_CARD ||
			    itemType == TYPE_ASSISTANT_FOOD_CARD ||
			    itemType == TYPE_ASSISTANT_CARD ||
			    itemType == TYPE_ASSISTANT_IMG
	        ) {
			    dialog = new ChatTextActionDialog(context, true);
			}
		    return dialog;
	    }
    };

    private ChatTextActionDialog.OnActionClickListener mActionClickListener = new ChatTextActionDialog.OnActionClickListener() {
        @Override
        public void onLikeClick() {

        }

        @Override
        public void onDislikeClick() {

        }

        @Override
        public void onCopyClick() {
            String message = chatMessages.get(mSharePos).getMessage();
            if (!message.isEmpty()) {
                ZUtils.copy(context, message);
            }
        }

        @Override
        public void onSelectClick() {
            if (mSelectorView != null && mSelectorView instanceof TextSelectorView) {
                ((TextSelectorView) mSelectorView).setSelectionEnabled(true);
                ((TextSelectorView) mSelectorView).setOnSelectionClickListener(mSelectChangeListener);
            }
        }

        @Override
        public void onReadClick() {
            String message = chatMessages.get(mSharePos).getMessage();
            if (!message.isEmpty()) {
                findHodlerTxtSpeakStatus(message);
            }
        }

        @Override
        public void onShareClick() {
            if (chatMessages != null && mSharePos >= 0 && mSharePos < chatMessages.size()) {
                if (mShareListener != null) {
                    mShareListener.onShareIconClick(mSharePos);
                    setSelectState(true);
                    if (mShareHolder != null) {
                        mShareHolder.radio.setVisibility(View.GONE);
                        mShareHolder.radioSelected.setVisibility(View.VISIBLE);
                        if (mShareHolder.vwShareEmpty !=null){
                            mShareHolder.vwShareEmpty.setVisibility(View.VISIBLE);
                        }
                        chatMessages.get(mSharePos).setIsSelected(true);
                    }
                }
            }
        }

        @Override
        public void onCollectClick() {

        }

        @Override
        public void onMoreClick() {

        }

        @Override
        public void onDeleteClick() {
            // 删除聊天消息
            Timber.tag("ChatAdapter").d("点击删除，mSharePos: " + mSharePos + ", chatMessages size: "
                    + (chatMessages != null ? chatMessages.size() : "null"));

            if (chatMessages != null && mSharePos >= 0 && mSharePos < chatMessages.size()) {
                ChatMessage message = chatMessages.get(mSharePos);
                Timber.tag("ChatAdapter").d("准备删除消息，ID: " + (message != null ? message.getId() : "null"));
                Timber.tag("ChatAdapter").d("消息内容: " + (message != null ? message.getMessage() : "null"));
                Timber.tag("ChatAdapter").d("消息类型: " + (message != null ? message.getMsgType() : "null"));

                if (message != null && message.getId() != null) {
                    // 使用回调通知ViewModel删除消息
                    if (messageActionCallback != null) {
                        messageActionCallback.onDeleteMessage(message.getId().longValue(),true);
                    } else {
                        // 如果没有设置回调，保持原有逻辑作为后备
                        deleteChatMessage(message.getId().longValue());
                    }
                } else {
                    // 本地消息：无服务器ID，直接本地删除
                    Log.w("ChatAdapter", "本地删除消息：消息ID为空，直接从列表移除");
                    if (mSharePos >= 0 && mSharePos < chatMessages.size()) {
                        // 删除本条
                        chatMessages.remove(mSharePos);
                        notifyDataSetChanged();
                        closeSelectView();
                        GlobalToast.show((Activity) context, "删除成功", GlobalToast.Type.SUCCESS);
                        if (messageActionCallback !=null){
                            messageActionCallback.onDeleteSuccessCallBack();
                        }
                    } else {
                        GlobalToast.show((Activity) context, "无法确定要删除的消息", GlobalToast.Type.ERROR);
                    }
                }
            } else {
                Timber.tag("ChatAdapter").e("删除条件不满足：mSelectorView=" + mSelectorView + ", chatMessages="
                        + chatMessages + ", mSharePos=" + mSharePos);
                GlobalToast.show((Activity) context, "无法确定要删除的消息", GlobalToast.Type.ERROR);
            }
        }
    };

    public void findHodlerTxtSpeakStatus(String txtSpeak) {
        ChatViewHolder holder = null;
        ViewParent parent = mSelectorView.getParent();
        while (parent != null) {
            if (parent instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) parent;
                RecyclerView.ViewHolder viewHolder = recyclerView.findContainingViewHolder(mSelectorView);
                if (viewHolder instanceof ChatViewHolder) {
                    holder = (ChatViewHolder) viewHolder;
                    break;
                }
            }
            parent = parent.getParent();
        }
        chatTxtSpeakStatus(holder, txtSpeak, false);
    }

    private void chatTxtSpeakStatus(ChatViewHolder finalHolder, String textTTS, boolean speakStatus) {
        Toast toast = GlobalToast.show((Activity) context, "正在生成语音朗读", GlobalToast.Type.LOADING);
        setMediaStatus(getPosition());
        // 使用弱引用避免内存泄漏
        WeakReference<ChatViewHolder> holderRef = new WeakReference<>(finalHolder);

        TTSManager.getInstance().setOnPlayerListener(new OnPlayerListener() {
            @Override
            public void playerStart() {
                if (speakStatus) {
                    isSpeak = true;
                }
                if (toast != null) {
                    toast.cancel();
                }

                ChatViewHolder holder = holderRef.get();
                if (holder != null && holder.iv_chat_play != null) {
                    holder.iv_chat_play.setImageResource(R.drawable.chat_tts_speaking_anim);
                    AnimationDrawable animationDrawable = (AnimationDrawable) holder.iv_chat_play.getDrawable();
                    // 开始动画
                    animationDrawable.start();
                }
            }

            @Override
            public void playerStop() {
                isSpeak = false;
                ChatViewHolder holder = holderRef.get();
                if (holder != null && holder.iv_chat_play != null) {
                    holder.iv_chat_play.setImageResource(R.mipmap.chat_play);
                }
            }
        });
        TTSManager.Companion.getInstance().textForceToAudio(textTTS);
    }

    private TextSelectorView.OnSelectionClickListener mSelectChangeListener = text -> {
        findHodlerTxtSpeakStatus(text);
    };

    /**
     * 删除聊天消息
     *
     * @param messageId 消息ID
     */
    private void deleteChatMessage(long messageId) {
        Timber.tag("ChatAdapter").d("开始删除消息，ID: " + messageId);

        // 显示删除中的提示
        GlobalToast.show((Activity) context, "正在删除...", GlobalToast.Type.LOADING);

        // 使用ChatRepositoryImpl来删除消息
        ChatRepositoryImpl chatRepository = new ChatRepositoryImpl();

        chatRepository.deleteChatMessage(messageId,
                new ChatRepository.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        Timber.tag("ChatAdapter").d("删除API响应，result: " + result);

                        if (result != null && result) {
                            // 删除成功，从列表中移除该消息
                            if (mSharePos >= 0 && mSharePos < chatMessages.size()) {
                                chatMessages.remove(mSharePos);
                                notifyItemRemoved(mSharePos);
                                notifyItemRangeChanged(mSharePos, chatMessages.size());
                                Timber.tag("ChatAdapter").d("界面更新完成，剩余消息数: " + chatMessages.size());
                            }
                            GlobalToast.show((Activity) context, "删除成功", GlobalToast.Type.SUCCESS);
                        } else {
                            Timber.tag("ChatAdapter").e("删除失败，服务器返回false");
                            GlobalToast.show((Activity) context, "删除失败", GlobalToast.Type.ERROR);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Timber.tag("ChatAdapter").e("删除失败: " + error);
                        GlobalToast.show((Activity) context, "删除失败: " + error, GlobalToast.Type.ERROR);
                    }
                });
    }

    public void switchHeadCard(HomeModelEntity.ModelType modelType) {
        this.modelType = modelType;
        notifyItemChanged(0);
    }

    // ========================================================================
    // ChatAdapterContext interface implementation
    // ========================================================================

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public MsgActionCallback getMsgActionCallback() {
        return callback;
    }

    @Override
    public OnFileItemClick getOnFileItemClick() {
        return new OnFileItemClick() {
            @Override
            public void onItemClick(int position) {
                // 获取指定位置的文件信息
                ChatFileBean fileBean = getFileAtPosition(position);
                if (fileBean != null) {
                    // 使用MediaPreviewHandler处理文件点击
                    mediaPreviewHandler.handleFileClick(fileBean);
                } else {
                    Timber.tag(TAG).w("No file found at position: " + position);
                }
            }

            @Override
            public void onClose(int position) {
                // Default file close handling
            }
        };
    }

    /**
     * 获取指定位置的文件信息
     *
     * @param position 文件位置
     * @return ChatFileBean对象，如果未找到返回null
     */
    private ChatFileBean getFileAtPosition(int position) {
        try {
            // 从当前绑定的适配器中获取文件信息
            if (chatFileAdapter != null && chatFileAdapter.getDataList() != null
                    && position >= 0 && position < chatFileAdapter.getDataList().size()) {
                return chatFileAdapter.getDataList().get(position);
            }

            // 如果主适配器没有数据，尝试从缓存适配器获取
            // 这种情况可能发生在ViewHolder复用时
            return getFileFromCurrentViewHolder(position);
        } catch (Exception e) {
            Timber.tag("ChatAdapter").e("Error getting file at position " + position, e);
            return null;
        }
    }

    /**
     * 从当前ViewHolder获取文件信息
     *
     * @param position 文件位置
     * @return ChatFileBean对象，如果未找到返回null
     */
    private ChatFileBean getFileFromCurrentViewHolder(int position) {
        // 这个方法用于处理ViewHolder复用时的特殊情况
        // 在实际使用中，文件信息通常会通过ViewHolder的缓存适配器获取
        // 由于ChatAdapter的复杂性，这里提供一个基础实现
        return null;
    }

    // 静态引用当前活跃的适配器实例，用于ViewHolder访问
    private static ChatAdapter currentInstance;

    /**
     * 设置当前活跃的适配器实例
     */
    private void setCurrentInstance() {
        currentInstance = this;
    }

    /**
     * 静态方法处理文件点击，供ViewHolder使用
     *
     * @param fileBean 文件信息
     */
    public static void handleFileClickStatic(ChatFileBean fileBean) {
        Timber.tag("ChatAdapter").d("handleFileClickStatic called with fileBean: " +
                (fileBean != null ? fileBean.getName() : "null"));
        Timber.tag("ChatAdapter").d("currentInstance: " + (currentInstance != null ? "not null" : "null"));

        if (currentInstance != null && currentInstance.mediaPreviewHandler != null) {
            Timber.tag("ChatAdapter").d("Calling mediaPreviewHandler.handleFileClick");
            currentInstance.mediaPreviewHandler.handleFileClick(fileBean);
        } else {
            Timber.tag("ChatAdapter").e("Cannot handle file click - currentInstance or mediaPreviewHandler is null");
        }
    }

    @Override
    public boolean isSelectable() {
        return isSelectable;
    }

    @Override
    public TTSManager getTTSManager() {
        return TTSManager.getInstance();
    }

    @Override
    public void showToast(String message) {
        GlobalToast.show((Activity) context, message, GlobalToast.Type.NORMAL);
    }

    @Override
    public void startActivity(Intent intent) {
        context.startActivity(intent);
    }

    @Override
    public OnMessageActionCallback getMessageActionCallback() {
        return messageActionCallback;
    }

    @Override
    public int getChatType() {
        return type;
    }

    @Override
    public Object getModelType() {
        return modelType;
    }

    @Override
    public View.OnLongClickListener getOnLongClickListener() {
        return mOnLongClickListener;
    }

    @Override
    public void onShareClick(ChatViewHolder holder, int position) {
        mSharePos = position;
        if (chatMessages != null && mSharePos >= 0 && mSharePos < chatMessages.size()) {
            if (mShareListener != null) {
                mShareListener.onShareIconClick(mSharePos);
                setSelectState(true);
                holder.radio.setVisibility(View.GONE);
                holder.radioSelected.setVisibility(View.VISIBLE);
                if (holder.vwShareEmpty !=null){
                    holder.vwShareEmpty.setVisibility(View.VISIBLE);
                }
                chatMessages.get(position).setIsSelected(true);
            }
        }
    }

    @Override
    public ChatMessage getMessage(int position) {
        if (position >= 0 && position < chatMessages.size()) {
            return chatMessages.get(position);
        }
        return null;
    }

    /**
     * 记录ViewHolder使用情况（仅在DEBUG模式下）
     * 用于性能监控和缓存大小优化
     *
     * @param viewType 消息类型
     */
    public void recordViewHolderUsage(int viewType) {
        ViewHolderPoolManager.recordViewHolderUsage(viewType);
    }

    /**
     * 清理ViewHolder缓存
     * 在内存紧张时可调用此方法释放缓存
     */
    public void clearViewHolderCache() {
        ViewHolderPoolManager.clearCache();
        Timber.tag(TAG).d("clearViewHolderCache: ViewHolder cache cleared");
    }

    /**
     * 基于使用统计动态优化缓存大小
     * 建议在应用空闲时调用，如onPause()或onStop()中
     */
    public void optimizeViewHolderCacheSizes() {
        ViewHolderPoolManager.optimizeCacheSizes();
        Timber.tag(TAG).d("optimizeViewHolderCacheSizes: Cache sizes optimized based on usage");
    }

    public int getPosition(){
        return position;
    }

    public void setPosition(int position){
        this.position = position;
    }

    public void setMediaStatus(int position){
        this.position = position;
        ChatMessage message1 = getMessage(position);
        Timber.tag(TAG).d("下标 = "+position +" data= "+ GsonUtils.toJson(message1));
        if (message1 != null && message1.getMusicData()!=null){
            message1.getMusicData().setPlay(false);
            notifyItemChanged(position);
        }
        MediaPlayerUtils.Companion.getInstance().stop();
    }

    /**
     * 分享 item 选中
     * @param holder
     * @param message
     */
    public void switchShareSelect(ChatViewHolder holder,ChatMessage message){
        if (message == null || !isSelectable){
            return;
        }
        if (!message.getIsSelected()) {
            holder.radio.setVisibility(View.GONE);
            holder.radioSelected.setVisibility(View.VISIBLE);
            message.setIsSelected(true);
        } else  {
            holder.radio.setVisibility(View.VISIBLE);
            holder.radioSelected.setVisibility(View.GONE);
            message.setIsSelected(false);
        }

    }
}