package com.fxzs.lingxiagent.viewmodel.chat.flow;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.cmdc.ai.assist.constraint.DialogueResult;
import com.fxzs.lingxiagent.conversation.AIConversationManager;
import com.fxzs.lingxiagent.lingxi.config.ChatFlowCallback;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.ChatDataFormat;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.ChatLingXiAdapter;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.LocalModule;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.PlanProgressType;
import com.fxzs.lingxiagent.lingxi.service_api.data.FoodList;
import com.fxzs.lingxiagent.lingxi.service_api.data.MusicData;
import com.fxzs.lingxiagent.model.chat.callback.DeepResearchCallback;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardHotelModel;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardOrderEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardPlanEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardPlandEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardTrainEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean;
import com.fxzs.lingxiagent.model.deepresearch.dto.DeepResearchBean;
import com.fxzs.lingxiagent.model.honor.dto.HtmlInfo;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.viewmodel.chat.service.StreamingService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.disposables.Disposable;

/**
 * ChatFlowController
 * 职责：
 * - 统一封装两种聊天流（标准流/灵犀流）的启动、回调派发与中断控制
 * - 在灵犀路径下，根据策略B统一触发历史记录（用户首条、助手结束JSON）
 * - 保持 VMChat 接口不变，VMChat 仅负责 UI 状态与消息列表更新
 * 用法：
 * - 通过 new ChatFlowController(request, streamingService, aiConversationManager, chatDataFormat)
 * - 配置 setHistoryRecorder(...) 可选的历史记录落库实现
 * - 调用 start(params, callback) 启动，返回 Disposable 供外层关闭
 */
public class ChatFlowController {

    public interface Callback {
        void onReceiveContent(String content);

        void onReceiveReason(String reason);

        void onReceiveH5Card(String templateId, String content);

        void onImages(ArrayList<String> imageUrls);

        void onHtmlCard(HtmlInfo cardInfo);

        void onFoodCard(FoodList foodList);

        void onOrderCard(ChatCardOrderEntity orderEntity);

        void onHotelCard(List<ChatCardHotelModel> hotelModels);

        void onPlaneCard(List<ChatCardPlandEntity> plandEntities);

        void onPlanCard(PlanProgressType planProgressType, ChatCardPlanEntity.ContentBean planEntities, String reqUrl);

        void onTrainCard(List<ChatCardTrainEntity> trainEntities);

        //void onFloatPermissionCard();

        void onGUiPermissionCard();

        void onDeepResearch(DeepResearchBean deepResearchBeans);

        void onEnd();

        void onError(String message);

        void onBillError(String message);

        // 新增方法：处理消息ID
        void onMessageIds(Integer sendId, Integer receiveId);

        void onMusicCard(MusicData musicData);

        void addGuiUserMsg(String content);

        void addGuiAiMsg(String content);
    }

    // 历史记录门面，由上层注入，控制何时落库。
    public interface HistoryRecorder {
        void recordUser(long conversationId, String message);

        void recordAssistant(long conversationId, String assistantJson);
    }

    // Assistant JSON 提供器，避免控制器依赖具体 ChatMessage 结构
    public interface AssistantJsonProvider {
        String get();
    }

    public static class Params {
        public long conversationId;
        public String title;
        public Long modelId; // 用于标准流与 initHonor
        public boolean thinkingEnabled;
        public @Nullable List<ChatFileBean> files;
        public boolean isLingxi; // 是否走灵犀路径
        public @Nullable LocalModule agentModel; // 灵犀智能体模块（聚餐/出行等）；非灵犀可为 null
        public @Nullable AssistantJsonProvider assistantJsonProvider; // 灵犀结束时提供 AI 消息 JSON
    }

    private final HttpRequest request;
    private final StreamingService streamingService;
    private final AIConversationManager aiConversationManager;
    private final ChatDataFormat chatDataFormat;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private @Nullable HistoryRecorder historyRecorder;

    public ChatFlowController(HttpRequest request,
                              StreamingService streamingService,
                              AIConversationManager aiConversationManager,
                              ChatDataFormat chatDataFormat) {
        this.request = request;
        this.streamingService = streamingService;
        this.aiConversationManager = aiConversationManager;
        this.chatDataFormat = chatDataFormat;
    }

    public void setHistoryRecorder(@Nullable HistoryRecorder recorder) {
        this.historyRecorder = recorder;
    }

    /**
     * 启动聊天流。根据 isLingXi/agentModel 选择策略。
     * 返回当前 SSE 的 Disposable，供上层关闭。
     */
    public Disposable start(Params params, Callback cb,boolean isSaveHistory) {
        if (params.isLingxi) {
            if (historyRecorder != null && isSaveHistory) {
                historyRecorder.recordUser(params.conversationId, params.title);
            }
            if (params.agentModel != null) {
                return startLingXiAgentFlow(params, cb);
            } else {
                // B 策略：仅灵犀路径落库：用户消息（开始时）+ 助手 JSON（结束时）
                return startLingXiFlow(params, cb);
            }
        } else {
            return startStandardFlow(params, cb);
        }
    }

    private Disposable startStandardFlow(Params p, Callback cb) {
        TTSManager.getInstance().setStop(false);
        streamingService.startStandardStream(
                p.conversationId,
                p.modelId == null ? 0L : p.modelId,
                p.title,
                p.files,
                p.thinkingEnabled,
                new StreamingService.Callback() {
                    @Override
                    public void onReceive(String content, boolean isReason, Integer sendId, Integer receiveId) {
                        // 传递消息ID
                        if (cb != null) {
                            cb.onMessageIds(sendId, receiveId);
                        }

                        if (isReason) {
                            if (cb != null) cb.onReceiveReason(content);
                        } else {
                            if (cb != null) cb.onReceiveContent(content);
                        }
                    }

                    @Override
                    public void onEnd() {
                        if (cb != null) cb.onEnd();
                    }
                }
        );
        return streamingService.getCurrentDisposable();
    }

    private Disposable startLingXiFlow(Params p, Callback cb) {
        final String requestId = UUID.randomUUID().toString();
        chatDataFormat.init(p.agentModel, p.title);
        final ChatLingXiAdapter chatLingXiAdapter = new ChatLingXiAdapter(aiConversationManager, requestId);
        TTSManager.getInstance().setStop(false);
        return chatLingXiAdapter.insideRcChat(p.title, (Object result) -> {

            if (result == null) {
                if (cb != null) cb.onError("生成失败");
                return null;
            }

            if (result instanceof String) {
                String errorMsg = (String) result;
                if (cb != null) {
                    cb.onBillError(errorMsg);
                }
                return null;
            }

            DialogueResult dialogueResult = (DialogueResult) result;

            mainHandler.post(() -> {
                chatDataFormat.initHonor(p.conversationId, p.modelId == null ? 0L : p.modelId, p.files);
                chatDataFormat.startFlow(request, dialogueResult, new ChatFlowCallback() {
                    @Override
                    public void receiveChat(String content) {
                        if (cb != null) cb.onReceiveContent(content);
                    }

                    @Override
                    public void receiveCot(String content) {
                        if (cb != null) cb.onReceiveReason(content);
                    }

                    @Override
                    public void receiveH5Card(String templateId, String content) {
                        if (cb != null) cb.onReceiveH5Card(templateId, content);
                    }

                    @Override
                    public void receiveImages(ArrayList<String> imageList) {
                        if (cb != null) cb.onImages(imageList);
                    }

                    @Override
                    public void receiveHtmlCard(HtmlInfo cardInfo) {
                        if (cb != null) cb.onHtmlCard(cardInfo);
                    }

                    @Override
                    public void receiveFoodCard(FoodList foodList) {
                        if (cb != null) cb.onFoodCard(foodList);
                    }

                    @Override
                    public void receiveOrderCard(ChatCardOrderEntity orderEntity) {
                        if (cb != null) cb.onOrderCard(orderEntity);
                    }

                    @Override
                    public void receiveHotelCard(List<ChatCardHotelModel> hotelModel) {
                        if (cb != null) cb.onHotelCard(hotelModel);
                    }

                    @Override
                    public void receivePlaneCard(List<ChatCardPlandEntity> planeEntities) {
                        if (cb != null) cb.onPlaneCard(planeEntities);
                    }

                    @Override
                    public void receivePlanCard(PlanProgressType planProgressType, ChatCardPlanEntity.ContentBean planEntities, String reqUrl) {
                        if (cb != null) cb.onPlanCard(planProgressType, planEntities, reqUrl);
                    }


                    @Override
                    public void receiveTrainCard(List<ChatCardTrainEntity> trainEntities) {
                        if (cb != null) cb.onTrainCard(trainEntities);
                    }

                    @Override
                    public void receiveGUIPermissionCard() {
                        if (cb != null) cb.onGUiPermissionCard();
                    }

//                    @Override
//                    public void receiveAccessCard() {
//                        if (cb != null) cb.onAccessPermissionCard();
//                    }
//
//                    @Override
//                    public void receiveFloatCard() {
//                        if (cb != null) cb.onFloatPermissionCard();
//                    }

                    @Override
                    public void receiveDeepResearchCard(DeepResearchBean deepResearchBean) {

                    }

                    @Override
                    public void receiveMusic(MusicData musicData) {
                        if (cb != null) cb.onMusicCard(musicData);
                    }

                    @Override
                    public void end() {
                        if (cb != null) cb.onEnd();
                        // B 策略：灵犀结束时写入助手消息 JSON
                        if (historyRecorder != null && p.assistantJsonProvider != null) {
                            historyRecorder.recordAssistant(p.conversationId, p.assistantJsonProvider.get());
                        }
                    }

                    @Override
                    public void addGuiUserMsg(String content) {
                        if (cb != null) cb.addGuiUserMsg(content);
                    }

                    @Override
                    public void addGuiAiMsg(String content) {
                        if (cb != null) cb.addGuiAiMsg(content);
                    }
                });
            });
            return null;
        });
    }

    public Disposable startDeepResearchAgentFlow(boolean isHistory, Params params, DeepResearchCallback cb) {
        if (historyRecorder != null && !isHistory) {
            historyRecorder.recordUser(params.conversationId, params.title);
        }
//        mainHandler.post(() -> {
        Disposable disposable = chatDataFormat.startAgentFlow(request, params.title, false, new DeepResearchCallback() {
            @Override
            public void onDeepResearch(DeepResearchBean deepResearchBean) {
                if (cb != null) cb.onDeepResearch(deepResearchBean);
            }

            @Override
            public void onDeepResearchError(String error) {
                if (cb != null) cb.onDeepResearchError(error);
            }

            @Override
            public void onDeepResearchComplete() {
                if (cb != null) cb.onDeepResearchComplete();
            }
        });

//        });
        return disposable;
    }

    private Disposable startLingXiAgentFlow(Params p, Callback cb) {
        chatDataFormat.init(p.agentModel, p.title);
        TTSManager.getInstance().setStop(false);
        mainHandler.post(() -> {
            chatDataFormat.initHonor(p.conversationId, p.modelId == null ? 0L : p.modelId, p.files);
            chatDataFormat.startFlow(request, p.title, new ChatFlowCallback() {
                @Override
                public void receiveChat(String content) {
                    if (cb != null) cb.onReceiveContent(content);
                }

                @Override
                public void receiveCot(String content) {
                    if (cb != null) cb.onReceiveReason(content);
                }

                @Override
                public void receiveH5Card(String templateId, String content) {
                    if (cb != null) cb.onReceiveH5Card(templateId, content);
                }

                @Override
                public void receiveImages(ArrayList<String> imageList) {
                    if (cb != null) cb.onImages(imageList);
                }

                @Override
                public void receiveHtmlCard(HtmlInfo cardInfo) {
                    if (cb != null) cb.onHtmlCard(cardInfo);
                }

                @Override
                public void receiveFoodCard(FoodList foodList) {
                    if (cb != null) cb.onFoodCard(foodList);
                }

                @Override
                public void receiveOrderCard(ChatCardOrderEntity orderEntity) {
                    if (cb != null) cb.onOrderCard(orderEntity);
                }

                @Override
                public void receiveHotelCard(List<ChatCardHotelModel> hotelModel) {
                    if (cb != null) cb.onHotelCard(hotelModel);
                }

                @Override
                public void receivePlaneCard(List<ChatCardPlandEntity> planeEntities) {
                    if (cb != null) cb.onPlaneCard(planeEntities);
                }

                @Override
                public void receivePlanCard(PlanProgressType planProgressType, ChatCardPlanEntity.ContentBean planEntities, String reqUrl) {
                    if (cb != null) cb.onPlanCard(planProgressType, planEntities, reqUrl);
                }

                @Override
                public void receiveTrainCard(List<ChatCardTrainEntity> trainEntities) {
                    if (cb != null) cb.onTrainCard(trainEntities);
                }

                @Override
                public void receiveGUIPermissionCard() {
                    if (cb != null) cb.onGUiPermissionCard();
                }

//                @Override
//                public void receiveAccessCard() {
//                    if (cb != null) cb.onAccessPermissionCard();
//                }
//
//                @Override
//                public void receiveFloatCard() {
//                    if (cb != null) cb.onFloatPermissionCard();
//                }

                @Override
                public void receiveDeepResearchCard(DeepResearchBean deepResearchBean) {
                    if (cb != null) cb.onDeepResearch(deepResearchBean);
                }

                @Override
                public void receiveMusic(MusicData musicData) {

                }

                @Override
                public void end() {
                    if (cb != null) cb.onEnd();
                    if (historyRecorder != null && p.assistantJsonProvider != null) {
                        historyRecorder.recordAssistant(p.conversationId, p.assistantJsonProvider.get());
                    }
                }

                @Override
                public void addGuiUserMsg(String content) {
                    if (cb != null) cb.addGuiUserMsg(content);
                }

                @Override
                public void addGuiAiMsg(String content) {
                    if (cb != null) cb.addGuiAiMsg(content);
                }
            });
        });
        return request.getSseDisposable();
    }
}

