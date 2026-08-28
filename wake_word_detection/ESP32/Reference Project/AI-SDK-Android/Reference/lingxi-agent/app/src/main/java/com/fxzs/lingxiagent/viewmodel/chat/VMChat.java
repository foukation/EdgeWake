package com.fxzs.lingxiagent.viewmodel.chat;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.conversation.AIConversationManager;
import com.fxzs.lingxiagent.helper.AppListHelper;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.ChatDataFormat;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.HonorQueueManager;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.LocalModule;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.PlanProgressType;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.TabEntity;
import com.fxzs.lingxiagent.lingxi.main.actions.HandlerLlm;
import com.fxzs.lingxiagent.lingxi.main.utils.GsonUtils;
import com.fxzs.lingxiagent.lingxi.main.utils.JsonUtil;
import com.fxzs.lingxiagent.lingxi.service_api.data.AppData;
import com.fxzs.lingxiagent.lingxi.service_api.data.FoodList;
import com.fxzs.lingxiagent.lingxi.service_api.data.MusicData;
import com.fxzs.lingxiagent.lingxi.translate.TranslationManager;
import com.fxzs.lingxiagent.model.chat.callback.CreateMyCallback;
import com.fxzs.lingxiagent.model.chat.callback.DeepResearchCallback;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardHotelModel;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardOrderEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardPlanEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardPlandEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardTrainEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.model.chat.dto.ConversationDetailDto;
import com.fxzs.lingxiagent.model.chat.dto.DrawingToChatBean;
import com.fxzs.lingxiagent.model.chat.dto.OptionModel;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepository;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepositoryImpl;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.deepresearch.dto.DeepResearchBean;
import com.fxzs.lingxiagent.model.drawing.dto.AspectRatioDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSessionDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingStyleDto;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepositoryImpl;
import com.fxzs.lingxiagent.model.honor.dto.HtmlInfo;
import com.fxzs.lingxiagent.model.honor.repository.HonorRepositoryImpl;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean;
import com.fxzs.lingxiagent.util.BillDialogHelper;
import com.fxzs.lingxiagent.util.GlobalSettings;
import com.fxzs.lingxiagent.util.NetworkStateManager;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.ZUtil.SuperEditAITranslateUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.util.audio.OnPlayerListener;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.fxzs.lingxiagent.view.chat.SuperChatFragment;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.viewmodel.chat.flow.ChatFlowController;
import com.fxzs.lingxiagent.viewmodel.chat.flow.DrawingFlow;
import com.fxzs.lingxiagent.viewmodel.chat.service.ConversationService;
import com.fxzs.lingxiagent.viewmodel.chat.service.DrawingGenerationService;
import com.fxzs.lingxiagent.viewmodel.chat.service.MessageRenderService;
import com.fxzs.lingxiagent.viewmodel.chat.service.StreamingService;
import com.fxzs.lingxiagent.viewmodel.chat.service.TTSAudioService;
import com.fxzs.lingxiagent.viewmodel.chat.service.TranslationInteractor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class VMChat extends BaseViewModel {
    private static final String TAG = "VMChat";

    private final MutableLiveData<List<ChatMessage>> chatMessages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> aiResponse = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> streamEnd = new MutableLiveData<>(true);
    private final MutableLiveData<String> thinkMessage = new MutableLiveData<>("");
    private final MutableLiveData<String> thinkMessageTitle = new MutableLiveData<>("");
    private final MutableLiveData<Integer> thinkStatus = new MutableLiveData<>(Constant.ThinkState.END);
    private final MutableLiveData<Boolean> isAutoPlay = new MutableLiveData<>(true);
    private final MutableLiveData<String> testCot = new MutableLiveData<>("");
    public MutableLiveData<Boolean> deepResearchStreamEnd = new MutableLiveData<>(false);
    public MutableLiveData<String> sendMsg = new MutableLiveData<>();
    public MutableLiveData<String> resendMsg = new MutableLiveData<>();

    private final HttpRequest request;
    private ConversationService conversationService;
    private OptionModel selectOptionModel;
    private getCatDetailListBean selectAgentBean;
    private DrawingToChatBean selectDrawingToChatBean;
    private DrawingStyleDto selectDrawingStyleDto;
    // 思考模式（深度思考开关）
    private boolean thinkingModeEnabled = false;
    //    private long conversationId;
    public boolean isStreamEnd = false;
    private String ResponseThink = "";
    private String fullResponse = "";
    private boolean hasAddHistory = false;
    private int currentIndex = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable typewriterRunnable;
    private Disposable sseDisposable;
    private long startTime = 0;
    private long endTime = 0;
    private String LING_XI_MODEL = "10086";

    //drawing
    private String selectedRatio = "1:1";
    private final ObservableField<Long> conversationId = new ObservableField<>(0l);
    private final ObservableField<Integer> progress = new ObservableField<>(0);
    private final ObservableField<String> progressText = new ObservableField<>("");

    // 业务状态
    private final MutableLiveData<List<DrawingStyleDto>> styles = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<AspectRatioDto>> aspectRatios = new MutableLiveData<>();
    private final MutableLiveData<DrawingImageDto> generatedImage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showResult = new MutableLiveData<>(false);
    private final ObservableField<Boolean> isGenerating = new ObservableField<>(false);
    private final MutableLiveData<DrawingSessionDto> currentSession = new MutableLiveData<>();
    private DrawingStyleDto selectedStyle = null;
    private DrawingRepository repository = null;
    private DrawingGenerationService drawingService;
    // 新增：绘画流程协调器
    private DrawingFlow drawingFlow;
    private String initialStyle = null;
    private String referenceImageUrl = null; // 参考图片URL
    private String hiddenPrompt = null; // 继续编辑模式下的隐藏prompt，用于关联但不显示
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isCreatingSession = false; // 是否正在创建会话
    private boolean pendingGeneration = false; // 是否有待处理的生成请求
    private boolean isContinueEditMode = false; // 是否是继续编辑模式
    private ChatMessage aiDrawingMsg;//正在生成的ai图片
    private ChatMessage deepResearchMsg;
    List<ChatFileBean> mFiles = new ArrayList<>();//选取的图片和文件
    List<String> mFileAnalyseUrl = new ArrayList<>();//选取的图片和文件url

    // 性能优化：根据设备内存动态调整的消息数量限制
    private int MAX_MESSAGES_IN_MEMORY;

    private String meetingId;
    private String transcriptionResult;

    private ChatDataFormat chatDataFormat;
    private AIConversationManager aiConversationManager;
    private WeakReference<Activity> activityRef;

    private MessageRenderService messageRenderService;

    private StreamingService streamingService;

    // 新增：统一管理聊天两种流式方案的门面控制器
    private ChatFlowController chatFlowController;

    private SuperEditAITranslateUtil superEditAITranslateUtil;

    private TranslationInteractor translationInteractor;
    private TTSAudioService ttsAudioService;
    private TranslationManager translationManager;
    public boolean isDeepResearchStreaming = false;

    public boolean netIsConnect = true;


    private final Gson gson = new Gson();
    private final Type type = new TypeToken<ChatMessage>() {
    }.getType();
    private NetworkStateManager networkStateManager;
    private ChatMessage networkError;
    private ChatRepository chatRepository ;
    private boolean isRefreshSend = false;

    private final MutableLiveData<String> benefitErrorLiveData = new MutableLiveData<>();

    public LiveData<String> getBenefitErrorLiveData() {
        return benefitErrorLiveData;
    }

    // 删除消息定义一个回调接口
    public interface DataCallback {
        void onSuccess(Long messageId);
    }


    public VMChat(@NonNull Application application) {
        super(application);

        // 性能优化：根据设备内存自动调整消息数量限制
        MAX_MESSAGES_IN_MEMORY = getMaxMessagesBasedOnDevice(application);
        Timber.tag(TAG).d("性能优化：根据设备内存设置消息限制为 %d 条", MAX_MESSAGES_IN_MEMORY);

        request = new HttpRequest();
        repository = DrawingRepositoryImpl.getInstance();
        chatRepository = new ChatRepositoryImpl();
        drawingService = new DrawingGenerationService(repository);
        initAIConversationManager();
        conversationService = new ConversationService(application, request, conversationId);
        streamingService = new StreamingService(request);
        // 初始化绘画流程协调器
        drawingFlow = new DrawingFlow(repository, drawingService);
        // 初始化翻译与 TTS 门面
        translationInteractor = new TranslationInteractor(application.getApplicationContext());
        ttsAudioService = new TTSAudioService();
        initNetStateManager();
    }

    /**
     * 根据设备内存自动调整消息数量限制
     *
     * @param application 应用上下文
     * @return 最大消息数量
     */
    private static int getMaxMessagesBasedOnDevice(Application application) {
        try {
            android.app.ActivityManager activityManager =
                    (android.app.ActivityManager) application.getSystemService(android.content.Context.ACTIVITY_SERVICE);

            if (activityManager != null) {
                android.app.ActivityManager.MemoryInfo memInfo = new android.app.ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memInfo);

                // 获取设备总内存（MB）
                long totalMemoryMB = memInfo.totalMem / (1024 * 1024);

                Timber.tag(TAG).d("设备总内存: %d MB", totalMemoryMB);

                // 根据内存大小返回不同的限制
                if (totalMemoryMB < 3000) {
                    // 低端设备（< 3GB）
                    Timber.tag(TAG).d("检测到低端设备，使用限制: %d", Constants.MAX_MESSAGES_LOW_END);
                    return Constants.MAX_MESSAGES_LOW_END;
                } else if (totalMemoryMB < 5000) {
                    // 中端设备（3-5GB）
                    Timber.tag(TAG).d("检测到中端设备，使用限制: %d", Constants.MAX_MESSAGES_MID_RANGE);
                    return Constants.MAX_MESSAGES_MID_RANGE;
                } else {
                    // 高端设备（> 5GB）
                    Timber.tag(TAG).d("检测到高端设备，使用限制: %d", Constants.MAX_MESSAGES_HIGH_END);
                    return Constants.MAX_MESSAGES_HIGH_END;
                }
            }
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "获取设备内存失败");
        }

        // 默认返回中端设备的限制
        Timber.tag(TAG).d("使用默认限制: %d", Constants.MAX_MESSAGES_MID_RANGE);
        return Constants.MAX_MESSAGES_MID_RANGE;
    }

    // ===== 分页状态（会话消息） =====
    private long paginationConversationId = 0L;
    private int pageNo = 1;
    private int pageSize = 10;
    private boolean hasMore = false;
    private boolean isLoadingPage = false;

    public boolean isLoadingPage() {
        return isLoadingPage;
    }

    public boolean hasMorePages() {
        return hasMore;
    }

    public int getCurrentPageNo() {
        return pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 重置并加载第一页
     */
    public void resetAndLoadFirstPage(long conversationId) {
        this.paginationConversationId = conversationId;
        this.pageNo = 1;
        this.hasMore = false;
        if (paginationConversationId <= 0L){
            return;
        }
        loadPageInternal(false);
    }

    /**
     * 加载下一页（追加）
     */
    public void loadNextPage() {
        if (isLoadingPage || !hasMore || paginationConversationId <= 0) return;
        loadPageInternal(true);
    }

    private void loadPageInternal(boolean append) {
        isLoadingPage = true;
        ChatRepository chatRepository = new ChatRepositoryImpl();
        int requestPage = pageNo;
        chatRepository.getPageByConversationId(paginationConversationId, requestPage, pageSize,
                new ChatRepository.Callback<List<ConversationDetailDto>>() {
                    @Override
                    public void onSuccess(List<ConversationDetailDto> data) {

                        try {
                            List<ChatMessage> updated = applyConversationDetailsToMessages(data, append);
                            chatMessages.postValue(updated);

                            // 分页游标更新
                            hasMore = (data != null && data.size() >= pageSize);
                            if (hasMore) {
                                pageNo = requestPage + 1;
                            }
                        } finally {
                            isLoadingPage = false;
                        }
                    }

                    @Override
                    public void onError(String error) {
                        isLoadingPage = false;
                        Timber.tag(TAG).w("分页加载失败: " + error);
                    }
                });
    }

    // 参考 SuperChatFragment.handleOfflineConversationHistory 的现有逻辑，将后端列表应用到本地消息列表
    private List<ChatMessage> applyConversationDetailsToMessages(List<ConversationDetailDto> list, boolean append) {
        List<ChatMessage> current = chatMessages.getValue();
        if (current == null) {
            current = new ArrayList<>();
            chatMessages.setValue(current);
        }
        List<ChatMessage> preservedHeaders = null;
        if (!append && !current.isEmpty()) {
            preservedHeaders = new ArrayList<>();
            for (ChatMessage message : current) {
                if (message != null && message.getMsgType() == ChatAdapter.TYPE_USER_HEAD_AGENT) {
                    preservedHeaders.add(message);
                }
            }
        }
        if (!append) {
            current.clear();
//            if (preservedHeaders != null && !preservedHeaders.isEmpty()) {
//                current.addAll(preservedHeaders);
//            }
        }
        if (list == null || list.isEmpty()) {
            return current;
        }

        final com.google.gson.Gson gsonLocal = gson;
        final java.lang.reflect.Type FILE_LIST_TYPE = new com.google.gson.reflect.TypeToken<java.util.List<com.fxzs.lingxiagent.model.chat.dto.ChatFileListJsonBean>>() {
        }.getType();

        int sizeBefore = current.size();
        for (int i = 0; i < list.size(); i++) {
            ConversationDetailDto dto = list.get(i);
            if (dto == null || dto.getContent() == null || dto.getContent().isEmpty()) {
                continue;
            }
            if ("assistant".equals(dto.getType())) {
                ChatMessage aiMsg = null;
                if ("10086".equals(dto.getModel())
                        || com.fxzs.lingxiagent.model.common.Constants.AGENT_TRAVEL.equals(dto.getModel())
                        || com.fxzs.lingxiagent.model.common.Constants.AGENT_TRIP.equals(dto.getModel())
                        || com.fxzs.lingxiagent.model.common.Constants.AGENT_MGVIDOE.equals(dto.getModel())
                        || com.fxzs.lingxiagent.model.common.Constants.AGENT_FINANCE.equals(dto.getModel())
                        || com.fxzs.lingxiagent.model.common.Constants.AGENT_COMMUNICATION.equals(dto.getModel())) {
                    aiMsg = addAIMsgLingxiHistory(dto.getContent());
                } else if ("深度研究".equals(dto.getModel())) {
                    com.fxzs.lingxiagent.model.deepresearch.dto.DeepResearchBean resp = gsonLocal.fromJson(dto.getContent(), com.fxzs.lingxiagent.model.deepresearch.dto.DeepResearchBean.class);
                    if (resp.getModel() != null) {
                        addDeepResearchCompleteCardHistory(resp);
                    }
                } else {
                    aiMsg = addAIMsgHistory(dto.getContent(), dto.getThinkText());
                }
                if (aiMsg != null) {
                    aiMsg.setId((long) dto.getId());
                }
            } else if ("user".equals(dto.getType())) {
                sendMessageHistory(dto.getContent());
                List<ChatMessage> msgs = chatMessages.getValue();
                if (msgs != null && !msgs.isEmpty()) {
                    ChatMessage lastUserMsg = null;
                    for (int j = msgs.size() - 1; j >= 0; j--) {
                        ChatMessage msg = msgs.get(j);
                        if (msg.getMsgType() == com.fxzs.lingxiagent.view.chat.ChatAdapter.TYPE_USER) {
                            lastUserMsg = msg;
                            break;
                        }
                    }
                    if (lastUserMsg != null) {
                        lastUserMsg.setId((long) dto.getId());
                    }
                }

                java.util.List<com.fxzs.lingxiagent.model.chat.dto.ChatFileBean> files = new java.util.ArrayList<>();
                if (dto.getFileListJson() != null) {
                    java.util.List<com.fxzs.lingxiagent.model.chat.dto.ChatFileListJsonBean> eventList = gsonLocal.fromJson(dto.getFileListJson(), FILE_LIST_TYPE);
                    if (eventList != null && eventList.size() > 0) {
                        for (int j = 0; j < eventList.size(); j++) {
                            com.fxzs.lingxiagent.model.chat.dto.ChatFileListJsonBean bean = eventList.get(j);
                            com.fxzs.lingxiagent.model.chat.dto.ChatFileBean chatFileBean = new com.fxzs.lingxiagent.model.chat.dto.ChatFileBean(bean.getName(), bean.getFileUrl(), false);
                            chatFileBean.setFileType(bean.getType());
                            files.add(chatFileBean);
                        }
                    }
                } else if (dto.getImages() != null) {
                    String[] results = dto.getImages().split(",");
                    if (results != null && results.length > 0) {
                        for (int j = 0; j < results.length; j++) {
                            String url = results[j];
                            com.fxzs.lingxiagent.model.chat.dto.ChatFileBean chatFileBean = new com.fxzs.lingxiagent.model.chat.dto.ChatFileBean(url, true);
                            chatFileBean.setPath(url);
                            chatFileBean.setPercent(100);
                            files.add(chatFileBean);
                        }
                    }
                }
                addUserMsgWithFile(files);
            }
        }
        // 若为加载更多（append=true），则将本次新增的片段挪到列表头部，保持“旧消息在上方”
        if (append) {
            List<ChatMessage> msgsNow = chatMessages.getValue();
            if (msgsNow != null && msgsNow.size() > sizeBefore) {
                List<ChatMessage> segment = new ArrayList<>(msgsNow.subList(sizeBefore, msgsNow.size()));
                // 先移除尾部新增
                msgsNow.subList(sizeBefore, msgsNow.size()).clear();
                // 再整体插入到头部，确保顺序保持
                msgsNow.addAll(0, segment);
            }
        }
        return chatMessages.getValue();
    }

    private void initNetStateManager() {
        networkStateManager = new NetworkStateManager(getApplication());
        networkStateManager.addNetworkStateListener(new NetworkStateManager.NetworkStateListener() {
            @Override
            public void onNetworkAvailable() {

            }

            @Override
            public void onNetworkLost() {

            }

            @Override
            public void onNetworkChanged(boolean isConnected) {
                netIsConnect = isConnected;
                if (!netIsConnect) {
                    new Handler(Looper.getMainLooper()).post(() -> stopThinkingAndGeneration("网络终断，请恢复后重试。"));
                }
            }
        });
    }

    public void netStateUnregister() {
        if (networkStateManager != null) {
            networkStateManager.destroy();
            networkStateManager = null;
        }
    }

    public void setContext(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
        initChatManager();
        setIsAutoPlay(SharedPreferencesUtil.getBoolean(Constants.KEY_IS_AUTO, true));
        getAppList();
    }

    public void setSuperEditAITranslateUtil(SuperEditAITranslateUtil superEditAITranslateUtil) {
        this.superEditAITranslateUtil = superEditAITranslateUtil;
    }

    public MutableLiveData<List<ChatMessage>> getChatMessages() {
        return chatMessages;
    }

    public LiveData<String> getAiResponse() {
        return aiResponse;
    }

    public LiveData<Boolean> getDeepResearchStreamEnd() {
        return deepResearchStreamEnd;
    }
    public LiveData<String> getSendMsg() {
        return sendMsg;
    }

    public MutableLiveData<Boolean> getLoading() {
        return loading;
    }

    public MutableLiveData<Boolean> getStreamEnd() {
        return streamEnd;
    }

    public LiveData<String> getThinkMessage() {
        return thinkMessage;
    }

    public LiveData<String> getThinkMessageTitle() {
        return thinkMessageTitle;
    }

    public MutableLiveData<Integer> getThinkStatus() {
        return thinkStatus;
    }

    public ObservableField<Long> getConversationId() {
        return conversationId;
    }

    private void initChatManager() {
        if (chatDataFormat == null) {
            HonorRepositoryImpl honorHttp = new HonorRepositoryImpl(activityRef.get());
            chatDataFormat = new ChatDataFormat(activityRef.get(), honorHttp);
            chatDataFormat.setVmChat(this);
        }
        if (messageRenderService == null) {
            messageRenderService = new MessageRenderService(new MessageRenderService.ChatListProvider() {
                @Override
                public List<ChatMessage> getMessages() {
                    return chatMessages.getValue();
                }

                @Override
                public void postMessages(List<ChatMessage> list) {
                    chatMessages.postValue(list);
                }
            });
        }
        // 初始化聊天流控制器（需在 chatDataFormat 已创建后）
        if (chatFlowController == null) {
            chatFlowController = new ChatFlowController(request, streamingService, aiConversationManager, chatDataFormat);
            // 策略B：仅灵犀路径落库由控制器统一调度
            chatFlowController.setHistoryRecorder(new ChatFlowController.HistoryRecorder() {
                @Override
                public void recordUser(long cid, String message) {
                    if (isRefreshSend){
                        return;
                    }
                    addConversationHistory(message, "user", true, messageId -> {
                        List<ChatMessage> currentMessages = chatMessages.getValue();
                        if (currentMessages != null && currentMessages.size() >= 2) {
                            ChatMessage messageItem = currentMessages.get(currentMessages.size() - 2);
                            messageItem.setId(messageId);
                        }
                    });
                }

                @Override
                public void recordAssistant(long cid, String assistantJson) {

                }
            });
        }
    }

    private void initAIConversationManager() {
        aiConversationManager = new AIConversationManager();
        aiConversationManager.setAllowInterrupt(false);
    }

    public void setSelectOptionModel(OptionModel option) {
        this.selectOptionModel = option;
        if (option == null || option.getModel() == null) {
            return;
        }
        GlobalSettings.getInstance().setSelectedModel(getApplication(), option.getModel(), option.getName());
        Constant.isUseLingXiTranslation = "10086".equals(option.getModel());
    }

    @Nullable
    private String resolveCreateMyModel() {
        if (selectOptionModel != null && !TextUtils.isEmpty(selectOptionModel.getModel())) {
            return selectOptionModel.getModel();
        }
        String savedModel = GlobalSettings.getInstance().getSelectedModelCode();
        return TextUtils.isEmpty(savedModel) ? null : savedModel;
    }

    public void setSelectAgentBean(getCatDetailListBean selectAgentBean) {
        this.selectAgentBean = selectAgentBean;
    }

    public getCatDetailListBean getSelectAgentBean() {
        return this.selectAgentBean;
    }

    public MutableLiveData<Boolean> getIsAutoPlay() {
        return isAutoPlay;
    }

    public void setIsAutoPlay(boolean isAutoPlay) {
        this.isAutoPlay.setValue(isAutoPlay);
    }

    public void setSelectDrawingStyleDto(DrawingStyleDto selectDrawingStyleDto) {
        this.selectDrawingStyleDto = selectDrawingStyleDto;
    }

    public DrawingStyleDto getSelectDrawingStyleDto() {
        return selectDrawingStyleDto;
    }

    public void setSelectDrawingToChatBean(DrawingToChatBean selectDrawingToChatBean) {
        this.selectDrawingToChatBean = selectDrawingToChatBean;
    }

    public DrawingToChatBean getSelectDrawingToChatBean() {
        return selectDrawingToChatBean;
    }

    public MutableLiveData<DrawingImageDto> getGeneratedImage() {
        return generatedImage;
    }


    public void setContinueEditMode(boolean continueEditMode) {
        isContinueEditMode = continueEditMode;
    }

    public void setReferenceImageUrl(String referenceImageUrl) {
        this.referenceImageUrl = referenceImageUrl;
    }

    public String getReferenceImageUrl() {
        return referenceImageUrl;
    }

    public void setHiddenPrompt(String hiddenPrompt) {
        this.hiddenPrompt = hiddenPrompt;
    }

    public ObservableField<Boolean> getIsGenerating() {
        return isGenerating;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public void setTranscriptionResult(String transcriptionResult) {
        this.transcriptionResult = transcriptionResult;
    }

    public void setSelectedRatio(String selectedRatio) {
        this.selectedRatio = selectedRatio;
    }

    public String getSelectedRatio() {
        return selectedRatio;
    }

    public List<ChatFileBean> getmFiles() {
        return mFiles;
    }

    String lastInput;
    public void reSendMessage() {
        Timber.tag(TAG).d("sendStream lastInput:"+lastInput);
        sendStream(conversationId.getValue(), lastInput,false,false);
    }

    public void sendMessage(String input) {
        sendMsg(input, true);
    }

    public void sendMessageHistory(String input) {
        sendMsg(input, false);
    }

    public void sendMsg(String input, boolean isSendStream) {
        if (input == null || input.trim().isEmpty()) return;
        loading.setValue(true);
        addUserMsg(input);
        mFiles.clear();//普通文本消息把文件信息清除
        if (isSendStream && selectOptionModel != null) {
            createMy(selectOptionModel.getModel(), input, new CreateMyCallback() {
                @Override
                public void back() {
                    sendStream(conversationId.getValue(), input,true,false);
                }
            });
        }
    }

    //发送带图片的消息
    public void sendMessageWithFile(String input, List<ChatFileBean> files) {
        if (input == null || input.trim().isEmpty() || files == null) return;
//        {"id":136,"keyId":null,"name":"腾讯混元","model":"hunyuan-turbo-latest","
        mFiles = files;
        mFileAnalyseUrl.clear();
        if (files != null && files.size() > 0) {
            for (int i = 0; i < files.size(); i++) {
                mFileAnalyseUrl.add(files.get(i).getPath());
            }
        }
//        selectOptionModel = new OptionModel();
//        selectOptionModel.setId(132);
//        selectOptionModel.setName("腾讯混元");
//        selectOptionModel.setModel("hunyuan-t1-vision");
        loading.setValue(true);
        addUserMsg(input);
        addUserMsgWithFile();
        boolean isImage = mFiles.get(0).isImage();
//        "新对话 | 详细总结文档内容"

        String newInput = input;
        if (!isImage) {
            newInput = "新对话 | " + input;
        }
        createMy(selectOptionModel.getModel(), newInput, new CreateMyCallback() {
            @Override
            public void back() {
                sendStream(conversationId.get(), input,true,false);
            }
        });
    }

    //重发图片/文件信息
    public void resendMessageWithFile(String input) {
//        sendMessageWithFile(input, mFiles);
        sendStream(conversationId.get(), input,true,false);
    }

    //发送绘画消息
    public void sendDrawingMessage(String input) {

        if (isGenerating.getValue()) {
            ZUtils.showToast("请等待生成成功");
//            GlobalToast.show(IYAApplication.getInstance(),"请等待生成成功", GlobalToast.Type.NORMAL);
            return;
        }
        if (selectDrawingToChatBean == null) {
            selectDrawingToChatBean = new DrawingToChatBean();
        }
        selectDrawingToChatBean.setPrompt(input);
        sendDrawingMessage(input, true);
    }

    //发送绘画消息(重新生成)
    public void sendDrawingMessage(DrawingImageDto imageDto) {

        if (isGenerating.getValue()) {
            ZUtils.showToast("请等待生成成功");
//            GlobalToast.show(IYAApplication.getInstance(),"请等待生成成功", GlobalToast.Type.NORMAL);
            return;
        }
        if (imageDto == null) {
            return;
        }
        String input = imageDto.getPrompt();
        selectDrawingToChatBean = new DrawingToChatBean();
        selectDrawingToChatBean.setPrompt(input);
        selectDrawingToChatBean.setReference_image_url(imageDto.getPicUrl());

        selectDrawingStyleDto = new DrawingStyleDto();
        selectDrawingStyleDto.setId(imageDto.getStyleId());

        selectDrawingToChatBean.setRatio(imageDto.getWidth() + ":" + imageDto.getHeight());
        selectedRatio = imageDto.getWidth() + ":" + imageDto.getHeight();
        AspectRatioDto aspectRatioDto = new AspectRatioDto();
        aspectRatioDto.setWidth(imageDto.getWidth());
        aspectRatioDto.setHeight(imageDto.getHeight());
        selectDrawingToChatBean.setAspectRatioDto(aspectRatioDto);
        sendDrawingMessage(input, true);
    }

    public void sendDrawingMessage(String input, String referenceImageUrl) {

        if (isGenerating.getValue()) {
            ZUtils.showToast("请等待生成成功");
//            GlobalToast.show(IYAApplication.getInstance(),"请等待生成成功", GlobalToast.Type.NORMAL);
            return;
        }
        selectDrawingToChatBean = new DrawingToChatBean();
        selectDrawingToChatBean.setPrompt(input);
        selectDrawingToChatBean.setReference_image_url(referenceImageUrl);
        sendDrawingMessage(input, true);
    }

    //发送绘画消息（历史记录）
    public void sendDrawingMessageHistory(String input) {
        sendDrawingMessage(input, false);
    }

    public void sendDrawingMessage(String input, boolean isGenerate) {
//        if (input == null || input.trim().isEmpty() || selectOptionModel == null) return;
        loading.setValue(true);
        addUserMsg(input);
        aiDrawingMsg = addAIDrawingMsg();
        if (isGenerate) {
            generateImage();
        }
    }

    public void refreshSendMessage(String input, int type) {
        if (input == null || input.trim().isEmpty()) return;
        if (type == SuperChatFragment.TYPE_HOME || type == SuperChatFragment.TYPE_WAKE ) {
            if (selectOptionModel == null) return;
            chatMessages.getValue().remove(chatMessages.getValue().size() - 1);
            loading.setValue(true);
//        addUserMsg(input);

            // 查找最近的用户消息类型来决定重发方式
            boolean isFileMessage = false;
            List<ChatMessage> messages = chatMessages.getValue();
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage msg = messages.get(i);
                if (msg.getMsgType() == ChatAdapter.TYPE_USER ||
                        msg.getMsgType() == ChatAdapter.TYPE_USER_FILE_IMAGE ||
                        msg.getMsgType() == ChatAdapter.TYPE_USER_FILE) {
                    isFileMessage = (msg.getMsgType() == ChatAdapter.TYPE_USER_FILE_IMAGE ||
                            msg.getMsgType() == ChatAdapter.TYPE_USER_FILE);
                    break;
                }
            }

            if (isFileMessage && mFiles != null && !mFiles.isEmpty()) {
                resendMessageWithFile(input);
            } else {
                createMy(selectOptionModel.getModel(), input, new CreateMyCallback() {
                    @Override
                    public void back() {
                        sendStream(conversationId.get(), input,true,true);
                    }
                });
            }
        } else if (type == SuperChatFragment.TYPE_AGENT) {
//            sendAgentMessage(input);
            chatMessages.getValue().remove(chatMessages.getValue().size() - 1);
            sendStream(conversationId.get(), input,true,true);
        } else if (type == SuperChatFragment.TYPE_MEETING_QA) {
            chatMessages.getValue().remove(chatMessages.getValue().size() - 1);
            Long cid = conversationId.get();
            if (cid == null || cid == 0L) {
                loading.setValue(false);
                setError("会议初始化中，请稍后再试");
                Timber.tag(TAG).w("sendMeetingMessage skipped: conversationId is null/0");
                return;
            }
            sendStream(cid, input,true,true);
//            sendMeetingMessage(input);
        }
    }

    //智能体消息
    public void sendAgentMessage(String input) {
        if (input == null || input.trim().isEmpty() || selectAgentBean == null) return;
        loading.setValue(true);
        addUserMsg(input);
        String model = selectAgentBean.getBotId();

        createMyAgent(model, selectAgentBean.getModelName(), selectAgentBean.getMenuId() + "", new CreateMyCallback() {
            @Override
            public void back() {
                sendStream(conversationId.get(), input,true,false);
            }
        });
    }

    //本地没有缓存ID，通过接口获取智能体conversationId
    public void getAgentConversationId(CreateMyCallback callback) {
        if (selectAgentBean == null) return;
        String model = selectAgentBean.getBotId();
        createMyAgent(model, selectAgentBean.getModelName(), selectAgentBean.getMenuId() + "", new CreateMyCallback() {
            @Override
            public void back() {
                if (callback != null) {
                    callback.back();
                }
            }
        });
    }

    //会议-智能问答消息
    public void sendMeetingMessage(String input) {
        if (input == null || input.trim().isEmpty()) return;
        loading.setValue(true);
        // 防御：会议会话未就绪时避免空指针闪退
        Long cid = conversationId.get();
        if (cid == null || cid == 0L) {
            loading.setValue(false);
            setError("会议初始化中，请稍后再试");
            Timber.tag(TAG).w("sendMeetingMessage skipped: conversationId is null/0");
            return;
        }
        addUserMsg(input);
        sendStream(cid, input,true,false);
    }

    //会议-智能问答初始化
    public void initMeeting() {
        String model = "bot-20250307112049-znnjx";
        String title = "智能问答";
        String systemMessage = "你是由中国的深度求索（DeepSeek）公司开发的智能助手DeepSeek-R1。如您有任何任何问题，我会尽我所能为您提供帮助。";
        createMyMeeting(model, title, systemMessage, new CreateMyCallback() {
            @Override
            public void back() {

                bindMeetingAndConversationId(meetingId, conversationId.get() + "", new CreateMyCallback() {
                    @Override
                    public void back() {

//                        String transcription = Constant.transcription;

                        String systemMessage = transcriptionResult;//TODO 填充会议内容
                        updateMyMeeting(conversationId.get() + "", systemMessage, new CreateMyCallback() {
                            @Override
                            public void back() {

                            }
                        });
                    }
                });
            }
        });
    }

    public void sendAIWritingMessage(String input) {
        List<ChatMessage> list;
        ZUtils.print("conversationId = " + conversationId.get());
        if (conversationId.get() == 0) {
//            list = chatMessages.getValue();
//            list.clear();
//            chatMessages.postValue(list);
        } else {
            list = chatMessages.getValue();
            chatMessages.postValue(list);
        }
//        ZUtils.print(" chatMessages.getValue() = "+ chatMessages.getValue().size());
        if (conversationId.get() == 0) {
            String model = resolveCreateMyModel();
            Timber.tag(TAG).d("模型 model =  %s", model);
            if (model == null) {
                setError("模型未加载，请稍后再试");
                return;
            }
            addUserMsg(input);
            createMy(model, input, new CreateMyCallback() {
                @Override
                public void back() {
                    sendStream(conversationId.get(), input,true,false);
//                    conversationId.setValue(0l);
                }
            });
        } else {
            addUserMsg(input);
            sendStream(conversationId.get(), input,true,false);
        }
    }

    public void sendTranslateMessage(String content, String prompt) {
        List<ChatMessage> list;
        ZUtils.print("conversationId = " + conversationId.get());
        if (conversationId.get() == 0) {
//            list = chatMessages.getValue();
//            list.clear();
//            chatMessages.postValue(list);
        } else {
            list = chatMessages.getValue();
            chatMessages.postValue(list);
        }
//        chatMessages.getValue().clear();
//        List<ChatMessage> list = chatMessages.getValue();
//        chatMessages.postValue(list);

        if (conversationId.get() == 0) {
            String model = resolveCreateMyModel();
            Timber.tag(TAG).d("模型 model =  %s", model);
            if (model == null) {
                setError("模型未加载，请稍后再试");
                return;
            }
            addUserMsg(content);
            createMy(model, content, new CreateMyCallback() {
                @Override
                public void back() {
                    sendStream(conversationId.get(), content,true,false);

//                    conversationId.setValue(0l);
                }
            });
        } else {
            addUserMsg(content);
            sendStream(conversationId.get(), content,true,false);
        }
    }

    public void sendTranslateMessage(String content, String fromLang, String toLang) {
        List<ChatMessage> list;
        ZUtils.print("conversationId = " + conversationId.get());
        if (conversationId.get() == 0) {
//            list = chatMessages.getValue();
//            list.clear();
//            chatMessages.postValue(list);
        } else {
            list = chatMessages.getValue();
            chatMessages.postValue(list);
        }
//        chatMessages.getValue().clear();
//        List<ChatMessage> list = chatMessages.getValue();
//        chatMessages.postValue(list);

        addUserMsg(content);
        textTranslate(content, fromLang, toLang);
    }

    private void addUserMsg(String input) {
        List<ChatMessage> list = chatMessages.getValue();
        if (list == null) list = new ArrayList<>();
        ChatMessage userMsg = new ChatMessage(input, true);
        list.add(userMsg);
        ZUtils.print("addUserMsg = " + list.size());
        chatMessages.postValue(list);
    }

    private void addH5CardMsg(String id, String con) {
        ChatMessage aiMsg;
        if (messageRenderService != null) {
            aiMsg = messageRenderService.addH5CardMsg(id, con);
        } else {
            // fallback 保留原逻辑
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            aiMsg = new ChatMessage(con, id, ChatAdapter.TYPE_ASSISTANT_H5_CARD);
            list.add(aiMsg);
            chatMessages.postValue(list);
        }
        addConversationHistory(GsonUtils.toJson(aiMsg), "assistant", true, aiMsg::setId);

    }

    private void addUserMsgWithFile() {
        List<ChatMessage> list = chatMessages.getValue();
        if (list == null) list = new ArrayList<>();
//        ChatMessage userMsg = new ChatMessage(input, true);

        boolean isImage = mFiles.get(0).isImage();
        ChatMessage userMsg = new ChatMessage(mFiles, isImage ? ChatAdapter.TYPE_USER_FILE_IMAGE : ChatAdapter.TYPE_USER_FILE);
        list.add(userMsg);
        ZUtils.print("addUserMsg = " + list.size());
        chatMessages.postValue(list);
    }

    public void addUserMsgWithFile(List<ChatFileBean> mFiles) {
        if (mFiles == null || mFiles.size() == 0) {
            return;
        }
        List<ChatMessage> list = chatMessages.getValue();
        if (list == null) list = new ArrayList<>();
//        ChatMessage userMsg = new ChatMessage(input, true);

        boolean isImage = mFiles.get(0).isImage();
        ChatMessage userMsg = new ChatMessage(mFiles, isImage ? ChatAdapter.TYPE_USER_FILE_IMAGE : ChatAdapter.TYPE_USER_FILE);
        list.add(userMsg);
        ZUtils.print("addUserMsg = " + list.size());
        chatMessages.postValue(list);
    }

    public ChatMessage addAIMsg() {
        List<ChatMessage> list = chatMessages.getValue();
        if (list == null) list = new ArrayList<>();
        ChatMessage aiMsg = new ChatMessage("", false);
        list.add(aiMsg);
        chatMessages.postValue(list);
        return aiMsg;
    }

    public ChatMessage addAIMsgHistory(String input) {
        List<ChatMessage> list = chatMessages.getValue();
        if (list == null) list = new ArrayList<>();
        ChatMessage aiMsg = new ChatMessage(input, false);
        aiMsg.setStatus(Constant.ThinkState.END);
        aiMsg.setThinkMessage("");
        list.add(aiMsg);
        chatMessages.postValue(list);
        return aiMsg;
    }

    // 历史AI消息（含思考内容）
    public ChatMessage addAIMsgHistory(String content, String think) {
        List<ChatMessage> list = chatMessages.getValue();
        if (list == null) list = new ArrayList<>();
        ChatMessage aiMsg = new ChatMessage(TextUtils.isEmpty(content) || content.contains("未完整生成") ? "已暂停生成" : content, false);
        aiMsg.setStatus(Constant.ThinkState.END);
        if (!TextUtils.isEmpty(content)) {
            aiMsg.setThinkMessage(think == null ? "" : think);
            aiMsg.setThinkMessageTitle("思考过程");
        } else {
            aiMsg.setThinkMessage("");
        }
        list.add(aiMsg);
        chatMessages.postValue(list);
        return aiMsg;
    }

    public ChatMessage addAIMsgLingxiHistory(String input) {
        ChatMessage aiMsg;
        try {
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
//            Gson gson = new Gson();
//            Type type = new TypeToken<ChatMessage>() {
//            }.getType();
            aiMsg = gson.fromJson(input, type);
            aiMsg.setTTSPlaying(false);
            list.add(aiMsg);
            chatMessages.postValue(list);
        } catch (Exception e) {
            Timber.tag(TAG).d("addAIMsgLingxiHistory Exception " + e.toString());
            aiMsg = addAIMsgHistory(input);
        }
        return aiMsg;
    }

    //添加AI绘画回复
    public ChatMessage addAIDrawingMsg() {
        List<ChatMessage> list = chatMessages.getValue();
        if (list == null) list = new ArrayList<>();
        ChatMessage aiMsg = new ChatMessage("", ChatAdapter.TYPE_AI_DRAWING);
        list.add(aiMsg);
        chatMessages.postValue(list);
        return aiMsg;
    }

    // 添加灵犀图片集
    public void addAIImages(ArrayList<String> imageList) {
        ChatMessage aiMsg;
        if (messageRenderService != null) {
            aiMsg = messageRenderService.addAIImages(imageList);
        } else {
            // fallback 保留原逻辑
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            else if (!list.isEmpty()) list.remove(list.size() - 1);
            aiMsg = new ChatMessage(imageList, ChatAdapter.TYPE_ASSISTANT_IMG);
            list.add(aiMsg);
            chatMessages.postValue(list);
        }
        addConversationHistory(GsonUtils.toJson(aiMsg), "assistant", true, aiMsg::setId);
    }

    // 添加灵犀智能体JS卡片
    public void addAICard(HtmlInfo cardInfo) {
        ChatMessage aiMsg;
        if (messageRenderService != null) {
            aiMsg = messageRenderService.addAICard(cardInfo);
        } else {
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            aiMsg = new ChatMessage(cardInfo, ChatAdapter.TYPE_ASSISTANT_CARD);
            list.add(aiMsg);
            chatMessages.postValue(list);
        }
        addConversationHistory(GsonUtils.toJson(aiMsg), "assistant", true, aiMsg::setId);
    }

    // 添加灵犀聚餐智能体餐厅JS卡片
    public void addAIFoodCard(FoodList foodList) {
        ChatMessage aiMsg;
        if (messageRenderService != null) {
            aiMsg = messageRenderService.addAIFoodCard(foodList);
        } else {
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            aiMsg = new ChatMessage(foodList, ChatAdapter.TYPE_ASSISTANT_FOOD_CARD);
            list.add(aiMsg);
            chatMessages.postValue(list);
        }
        addConversationHistory(GsonUtils.toJson(aiMsg), "assistant", true, aiMsg::setId);
    }

    public void addAIHotelCard(List<ChatCardHotelModel> hotelModels) {
        ChatMessage aiMsg;
        if (messageRenderService != null) {
            aiMsg = messageRenderService.addAIHotelCard(hotelModels);
        } else {
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            aiMsg = new ChatMessage(ChatAdapter.TYPE_ASSISTANT_HOTEL_CARD);
            aiMsg.setHotelModels(hotelModels);
            list.add(aiMsg);
            chatMessages.postValue(list);
        }
        addConversationHistory(GsonUtils.toJson(aiMsg), "assistant", true, aiMsg::setId);
    }

    public void addAIPlanCard(PlanProgressType planProgressType, ChatCardPlanEntity.ContentBean planEntities, String reqUrl) {
        ChatMessage aiMsg;
        if (planProgressType == PlanProgressType.Loading) {
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            aiMsg = new ChatMessage(planProgressType.getAlias(), reqUrl, planEntities, ChatAdapter.TYPE_ASSISTANT_PLAN_CARD);
            list.add(aiMsg);
            chatMessages.postValue(list);
        } else {
            if (messageRenderService != null) {
                aiMsg = messageRenderService.addAIPlanCard(planProgressType, planEntities, reqUrl);
            } else {
                List<ChatMessage> list = chatMessages.getValue();
                if (list == null || list.isEmpty()) return;
                aiMsg = list.get(list.size() - 1);
                if (!TextUtils.isEmpty(aiMsg.getPlanProgressType()) &&
                        TextUtils.equals(aiMsg.getPlanProgressType(), PlanProgressType.Loading.getAlias())) {
                    aiMsg.setPlanContent(planEntities);
                    aiMsg.setPlanReqUrl(reqUrl);
                    aiMsg.setPlanProgressType(planProgressType.getAlias());
                    chatMessages.postValue(list);
                }
            }
            if (planProgressType == PlanProgressType.Success)
                addConversationHistory(GsonUtils.toJson(aiMsg), "assistant", true, aiMsg::setId);
        }
    }

    public void addAIPlandCard(List<ChatCardPlandEntity> plandEntities) {
        ChatMessage aiMsg;
        if (messageRenderService != null) {
            aiMsg = messageRenderService.addAIPlandCard(plandEntities);
        } else {
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            aiMsg = new ChatMessage(ChatAdapter.TYPE_ASSISTANT_PLANE_CARD);
            aiMsg.setPlandEntities(plandEntities);
            list.add(aiMsg);
            chatMessages.postValue(list);
        }
        addConversationHistory(GsonUtils.toJson(aiMsg), "assistant", true, aiMsg::setId);
    }

    public void addAITrainCard(List<ChatCardTrainEntity> trainEntities) {
        ChatMessage aiMsg;
        if (messageRenderService != null) {
            aiMsg = messageRenderService.addAITrainCard(trainEntities);
        } else {
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            aiMsg = new ChatMessage(ChatAdapter.TYPE_ASSISTANT_TRAIN_CARD);
            aiMsg.setTrainEntities(trainEntities);
            list.add(aiMsg);
            chatMessages.postValue(list);
        }
        addConversationHistory(GsonUtils.toJson(aiMsg), "assistant", true, aiMsg::setId);
    }

    public void addAIOrderCard(ChatCardOrderEntity orderEntities) {
        ChatMessage aiMsg;
        if (messageRenderService != null) {
            aiMsg = messageRenderService.addAIOrderCard(orderEntities);
        } else {
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            aiMsg = new ChatMessage(ChatAdapter.TYPE_ASSISTANT_ORDER_CARD);
            aiMsg.setOrderEntity(orderEntities);
            list.add(aiMsg);
            chatMessages.postValue(list);
        }
        addConversationHistory(GsonUtils.toJson(aiMsg), "assistant", true, aiMsg::setId);
    }

    public void addDeepResearchCard(DeepResearchBean deepResearchBean) {

        if (messageRenderService != null) {
            deepResearchMsg = messageRenderService.addDeepResearchCard(deepResearchBean);
        } else {
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            deepResearchMsg = new ChatMessage(deepResearchBean, ChatAdapter.TYPE_DEEP_RESEARCH);
            list.add(deepResearchMsg);
            chatMessages.postValue(list);
        }
        addConversationHistory(GsonUtils.toJson(deepResearchMsg), "assistant", true);
    }

    public void addDeepResearchCompleteCard(DeepResearchBean deepResearchBean) {

        if (messageRenderService != null) {
            deepResearchMsg = messageRenderService.addDeepResearchCompleteCard(deepResearchBean);
        } else {
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            deepResearchMsg = new ChatMessage(deepResearchBean, ChatAdapter.TYPE_DEEP_RESEARCH_COMPLETE);
            list.add(deepResearchMsg);
            chatMessages.postValue(list);
        }
        addConversationHistory(GsonUtils.toJson(deepResearchBean), "assistant", true);
    }

    public void addDeepResearchCompleteCardHistory(DeepResearchBean deepResearchBean) {

        if (messageRenderService != null) {
            deepResearchMsg = messageRenderService.addDeepResearchCompleteCard(deepResearchBean);
        } else {
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            deepResearchMsg = new ChatMessage(deepResearchBean, ChatAdapter.TYPE_DEEP_RESEARCH_COMPLETE);
            list.add(deepResearchMsg);
            chatMessages.postValue(list);
        }
    }

    public void addNetworkErrorCard() {
        if (messageRenderService != null) {
            networkError = messageRenderService.addNetworkErrorCard();
        } else {
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            networkError = new ChatMessage(ChatAdapter.TYPE_NETWORK_ERROR);
            list.add(networkError);
            chatMessages.postValue(list);
        }
    }

    public void updateDeepResearchMsg(DeepResearchBean deepResearchBean) {
        deepResearchMsg.setDeepResearch(deepResearchBean);
        chatMessages.postValue(chatMessages.getValue());
    }

    public void updateAIDrawingMsg(int progress) {
        aiDrawingMsg.setProgress(progress);

        chatMessages.postValue(chatMessages.getValue());
    }

    public void updateAIDrawingMsg(String url) {
        aiDrawingMsg.setUrl(url);

        chatMessages.postValue(chatMessages.getValue());
    }

    public void updateAIDrawingMsgId(long id) {
        aiDrawingMsg.setId(id);

        // 同步将上一条消息（通常为用户发起的绘画请求）也设置相同的 id
        List<ChatMessage> list = chatMessages.getValue();
        if (list != null && !list.isEmpty()) {
            int indexOfAi = list.indexOf(aiDrawingMsg);
            if (indexOfAi > 0) {
                ChatMessage prevMsg = list.get(indexOfAi - 1);
                if (prevMsg != null) {
                    prevMsg.setId(id);
                }
            } else if (indexOfAi == -1) {
                // 兜底：若引用不在列表中，则将最后一条视为当前绘画消息的上一条
                if (list.size() >= 2) {
                    ChatMessage prevMsg = list.get(list.size() - 2);
                    if (prevMsg != null) {
                        prevMsg.setId(id);
                    }
                }
            }
        }

        chatMessages.postValue(list);
    }

    public void updateAIDrawingMsg(DrawingImageDto imageDto) {
        aiDrawingMsg.setDrawingImageDto(imageDto);

        chatMessages.postValue(chatMessages.getValue());
    }
    
    /**
     * 开始轮询图片状态（用于从历史记录恢复生成中的图片）
     */
    public void startPollingImageStatus(Long imageId) {
        if (imageId == null) {
            Timber.tag("VMDrawing").w("startPollingImageStatus: imageId is null");
            return;
        }
        
        Timber.tag("VMDrawing").d("开始轮询图片状态，imageId: " + imageId);
        
        // 使用 Timer 轮询图片状态
        Timer pollingTimer = new Timer();
        final int[] pollCount = {0};
        final int maxPolls = 60;
        final int[] uiProgress = {0};

        pollingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                pollCount[0]++;
                if (pollCount[0] > maxPolls) {
                    pollingTimer.cancel();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        updateAIDrawingMsg(100);
                        setError("图片生成超时");
                    });
                    return;
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    DrawingImageDto queryDto = new DrawingImageDto();
                    queryDto.setId(imageId);
                    repository.getImageDetail(queryDto).observeForever(result -> {
                        if (result.isSuccess() && result.getData() != null) {
                            DrawingImageDto image = result.getData();
                            Integer status = image.getStatus();

                            if (status != null && status == 20) {
                                // 生成成功
                                pollingTimer.cancel();
                                updateAIDrawingMsg(100);
                                updateAIDrawingMsg(image.getImageUrl());
                                updateAIDrawingMsg(image);
                                Timber.tag("VMDrawing").d("图片生成成功，imageId: " + imageId);
                            } else if (status != null && status == 30) {
                                // 生成失败
                                pollingTimer.cancel();
                                updateAIDrawingMsg(100);
                                setError(image.getErrorMsg() != null ? image.getErrorMsg() : "图片生成失败");
                                Timber.tag("VMDrawing").e("图片生成失败，imageId: " + imageId);
                            } else if (status != null && status == 10) {
                                // 生成中：缓慢推进进度，不超过90%
                                if (uiProgress[0] < 90) {
                                    uiProgress[0] = Math.min(90, uiProgress[0] + 5);
                                }
                                updateAIDrawingMsg(uiProgress[0]);
                                Timber.tag("VMDrawing").d("图片生成中，进度: " + uiProgress[0] + "%");
                            }
                        } else {
                            if (pollCount[0] > 10 && !result.isSuccess()) {
                                pollingTimer.cancel();
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    updateAIDrawingMsg(100);
                                    setError("查询图片状态失败");
                                });
                            }
                        }
                    });
                });
            }
        }, 1000, 2000);
    }

    public ChatMessage addAIMsg(String msg) {
        List<ChatMessage> list = chatMessages.getValue();
        if (list == null) list = new ArrayList<>();
        ChatMessage aiMsg = new ChatMessage(msg, false, Constant.ThinkState.END);
        aiMsg.setThinkMessage("");
        list.add(aiMsg);
        chatMessages.postValue(list);
        return aiMsg;
    }

    public void addNexusHeadMsg(List<String> nexusList) {
        List<ChatMessage> list = chatMessages.getValue();
        if (list == null) list = new ArrayList<>();
        ChatMessage aiMsg = new ChatMessage(selectAgentBean.getDescription(), ChatAdapter.TYPE_USER_HEAD_AGENT, selectAgentBean.getIcon());
        aiMsg.setNexusPilotList(nexusList);
        list.add(aiMsg);
        chatMessages.postValue(list);
    }

    /**
     * 批量添加历史消息，减少LiveData更新次数
     * 用于历史记录加载场景，一次性添加多条消息，避免频繁触发UI刷新
     *
     * @param messages 要添加的消息列表
     */
    public void addMessagesInBatch(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return;

        List<ChatMessage> currentMessages = chatMessages.getValue();
        if (currentMessages == null) {
            currentMessages = new ArrayList<>();
        }

        // 批量添加所有消息
        currentMessages.addAll(messages);

        // 只触发一次LiveData更新
        chatMessages.postValue(currentMessages);
    }

    public void createMy(String model, String title, CreateMyCallback callback) {
        conversationService.createMy(model, title, callback);
    }

    public void createMyWithFile(String model, String title, CreateMyCallback callback) {
        conversationService.createMyWithFile(model, title, callback);
    }

    public void createMyAgent(String model, String title, String aiMenuId, CreateMyCallback callback) {
        conversationService.createMyAgent(model, title, aiMenuId, callback);
    }

    public void createMyMeeting(String model, String title, String systemMessage, CreateMyCallback callback) {
        conversationService.createMyMeeting(model, title, systemMessage, callback);
    }

    public void bindMeetingAndConversationId(String meetingId, String conversionId, CreateMyCallback callback) {
        conversationService.bindMeetingAndConversationId(meetingId, conversionId, callback);
    }

    public void updateMyMeeting(String id, String systemMessage, CreateMyCallback callback) {
        conversationService.updateMyMeeting(id, systemMessage, callback);
    }

    private String LastFullResponse;
    private String ttsContent;
    // 新增灵犀数据增量返回处理变量
    private Handler ttsHandler = new Handler(Looper.getMainLooper());
    private Runnable ttsRunnable;
    private static final int MIN_LENGTH = 30; // 最小播报长度
    private static final long DELAY_MS = 20; // 延迟时间
    // 新增中文逗号“，”和英文逗号“,”，根据需要选择是否保留
    private static final Pattern END_PUNCT_PATTERN = Pattern.compile("[。,!?，！？…；;:：]+\\s?");
    private String cachedTtsContent = "";
    // 记录最后一次接收内容的时间（用于超时判断）
    private long lastReceiveTime = 0;
    // 超时阈值（例如3秒无新内容则强制播放）
    private static final long TIMEOUT_MS = 3000;
    private long lastStreamReceiveTime = 0; // 最后一次收到流式数据的时间戳
    private Runnable streamTimeoutRunnable; // 流式超时检查任务
    private static final long STREAM_TIMEOUT_MS = 1000; // 超时阈值：1秒

    public MutableLiveData<String> getTestCot() {
        return testCot;
    }

    public void sendStream(long conversationId, String title,boolean isSaveHistory,boolean isRefreshSend) {
        // 兜底：确保 chatFlowController 已初始化，避免 NPE
        Timber.tag(TAG).d("sendStream title:"+title);
        lastInput = title;
        this.isRefreshSend = isRefreshSend;
        if (chatFlowController == null) {
            initChatManager();
            if (chatFlowController == null) {
                setError("聊天引擎未就绪，请稍后重试");
                loading.setValue(false);
                return;
            }
        }
        closeSSE();
        if (selectAgentBean != null && selectAgentBean.getModelName() != null) {
            if (selectAgentBean.getModelName().equals("深度研究")) {
                ChatFlowController.Params params = new ChatFlowController.Params();
                params.conversationId = conversationId;
                params.title = title;
                deepResearchStreamEnd.postValue(false);
                isDeepResearchStreaming = true;
                Timber.tag(TAG).d("isDeepResearchStreaming %s", isDeepResearchStreaming);
                sseDisposable = chatFlowController.startDeepResearchAgentFlow(false, params, new DeepResearchCallback() {
                    @Override
                    public void onDeepResearch(DeepResearchBean deepResearchBeans) {
//                        Timber.tag(TAG).d("onDeepResearch %s", deepResearchBeans.getTaskStatus());
                        if (deepResearchBeans.getTaskStatus() == 1) {
                            deepResearchBeans.setTaskStatus(2);
                            addDeepResearchCard(deepResearchBeans);
                        } else if (deepResearchBeans.getTaskStatus() == 4) {
                            deepResearchStreamEnd.postValue(true);
                            String result = GsonUtils.toJson(deepResearchBeans.getList().get(deepResearchBeans.getList().size() - 1));
                            Timber.tag(TAG).d("onDeepResearch TaskStatus = 4 data = " + result);
                            updateDeepResearchMsg(deepResearchBeans);
                            //添加一个完成的卡片
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                addDeepResearchCompleteCard(deepResearchBeans);
                            }, 2000);
                            //创建文件
//                            DocumentHelper helper = new DocumentHelper(activityRef.get());
//                            String content = deepResearchBeans.getReportContent();
//                            helper.createWordWithText(content, title);
                        } else {
//                            Timber.tag(TAG).d("updateDeepResearchMsg");
                            updateDeepResearchMsg(deepResearchBeans);
                        }
                    }

                    @Override
                    public void onDeepResearchError(String error) {
                        isDeepResearchStreaming = false;
                        addNetworkErrorCard();
                        deepResearchStreamEnd.postValue(true);
                    }

                    @Override
                    public void onDeepResearchComplete() {
                        isDeepResearchStreaming = false;
                    }
                });
                return;
            }
        }

        fullResponse = "";
        ResponseThink = "";
        thinkMessage.postValue("");
        startTime = System.currentTimeMillis();
        ChatMessage aiMessage = addAIMsg();
        aiMessage.setThinkMessage("");
        aiMessage.setThinkMessageTitle("正在思考中"); // START状态显示"正在思考中"作为加载指示器
        aiMessage.setStatus(Constant.ThinkState.START);
        thinkMessageTitle.postValue("正在思考中");
        thinkStatus.postValue(Constant.ThinkState.START);
        isStreamEnd = false;
        streamEnd.postValue(false);

	    LocalModule agentModel = getLocalModule();
	    boolean lingXiModel = selectOptionModel != null && Objects.equals(selectOptionModel.getModel(), LING_XI_MODEL);
        boolean isLingxiPath = lingXiModel || agentModel != null;

        if (isLingxiPath) {
            hasAddHistory = false;
        }

        // 统一参数
        ChatFlowController.Params params = new ChatFlowController.Params();
        params.conversationId = conversationId;
        params.title = title;
        params.modelId = selectOptionModel != null ? selectOptionModel.getId() : (selectAgentBean != null ? selectAgentBean.getModelId() : null);
        params.thinkingEnabled = isThinkingModeEnabled();
        params.files = mFiles;
        params.isLingxi = isLingxiPath;
        params.agentModel = agentModel;
        // 结束时由控制器通过 Provider 写历史
        params.assistantJsonProvider = () -> GsonUtils.toJson(aiMessage);

        String requestId = UUID.randomUUID().toString();
        LastFullResponse = "";
        ttsContent = "";
        sseDisposable = chatFlowController.start(params, new ChatFlowController.Callback() {
            @Override
            public void onReceiveContent(String content) {
                // 修改原有逻辑
                ZUtils.print("onReceiveContent：" + "content ==" + content + ",isLingxiPath==" + isLingxiPath);
                if (TextUtils.isEmpty(content)) {
                    return;
                }
                if (isLingxiPath) {
                    ZUtils.print("onReceiveContent" + "LastFullResponse ==" + LastFullResponse);
                    if (LastFullResponse == null || LastFullResponse.isEmpty()) {
                        ttsContent = content;
                        LastFullResponse = content;
                        fullResponse = content;
                    } else {
                        if (content.equals(LastFullResponse)) {
                            return;
                        }
                        boolean isIncrement = content.startsWith(LastFullResponse) && content.length() > LastFullResponse.length();
                        if (isIncrement) {
                            ttsContent += content.substring(LastFullResponse.length());
                            LastFullResponse = content;
                            fullResponse = content;
                        } else {
                            ttsContent = content; // 非增量时直接覆盖（原逻辑用+=会重复，导致内容过长）
                            LastFullResponse = content;
                            fullResponse = content;
                        }
                    }
                } else {
                    fullResponse += content;
                    ttsContent += content;
                }
                lastStreamReceiveTime = System.currentTimeMillis();
                if (ttsRunnable != null) {
                    ttsHandler.removeCallbacks(ttsRunnable);
                }
                // 创建新的延迟任务
                ttsRunnable = () -> {
                    // 检查是否包含中英文句末符号
                    Matcher punctMatcher = END_PUNCT_PATTERN.matcher(ttsContent);
                    boolean hasEndPunct = punctMatcher.find();
                    boolean isLongEnough = ttsContent.length() >= MIN_LENGTH;

                    ZUtils.print("onReceiveContent" + "play ttsContent ==" + ttsContent
                            + " isLongEnough:" + isLongEnough + ":hasEndPunct:" + hasEndPunct);

                    // 处理有标点符号的情况：截取到最后一个标点处播放
                    if (hasEndPunct) {
                        // 找到最后一个标点的位置
                        int lastPunctPos = -1;
                        do {
                            lastPunctPos = punctMatcher.end(); // 记录当前标点的结束位置
                        } while (punctMatcher.find()); // 循环找到最后一个标点

                        // 截取到最后一个标点处的内容进行播放
                        String contentToPlay = ttsContent.substring(0, lastPunctPos);
                        // 剩余内容继续缓存
                        ttsContent = ttsContent.substring(lastPunctPos);
                        playTTSContent(aiMessage, contentToPlay, requestId, false);
                        ZUtils.print("onReceiveContentplay 播放到标点位置: " + contentToPlay + ", 剩余缓存: " + ttsContent);
                    } else if (isLongEnough) {
                        playTTSContent(aiMessage, ttsContent, requestId, false);
                        ttsContent = "";
                        ZUtils.print("onReceiveContentplay 达到最小长度，整段播放: " + ttsContent);
                    }
                };
                // 延迟执行
                ttsHandler.postDelayed(ttsRunnable, DELAY_MS);

                initStreamTimeoutCheck(aiMessage, requestId);

                aiMessage.setThinkMessage(ResponseThink);
                aiMessage.setMessage(fullResponse);
                aiMessage.setStatus(Constant.ThinkState.THINKING);
                // 当大模型开始返回内容时，清除"正在思考中"的标题
                aiMessage.setThinkMessageTitle("");
                thinkMessageTitle.postValue("");
                thinkMessage.postValue(ResponseThink);
                aiResponse.postValue(fullResponse);
                thinkStatus.postValue(Constant.ThinkState.THINKING);
                currentIndex = fullResponse.length();
                chatMessages.postValue(chatMessages.getValue());
//                testCot.postValue(fullResponse);
            }

            @Override
            public void onReceiveReason(String reason) {
                // 标准流：增量片段 -> 累加；灵犀流：回调为完整思考 -> 覆盖
                if (isLingxiPath) {
                    ResponseThink = reason;
                } else {
                    ResponseThink += reason;
                }
                aiMessage.setThinkMessage(ResponseThink);

                // 收到思考内容时，更新标题为"思考中"
                aiMessage.setThinkMessageTitle("思考中");
                thinkMessageTitle.postValue("思考中");

                aiMessage.setStatus(Constant.ThinkState.THINKING);
                thinkMessage.postValue(ResponseThink);
                thinkStatus.postValue(Constant.ThinkState.THINKING);
                chatMessages.postValue(chatMessages.getValue());
//                testCot.postValue(reason);
            }

            @Override
            public void onReceiveH5Card(String templateId, String content) {
                addH5CardMsg(templateId, content);
//                testCot.postValue(content);
            }

            @Override
            public void onImages(ArrayList<String> imageUrls) {
                addAIImages(imageUrls);
                //图片生成不再需要保存进度信息
                hasAddHistory = true;
            }

            @Override
            public void onHtmlCard(HtmlInfo cardInfo) {
                addAICard(cardInfo);
            }

            @Override
            public void onFoodCard(FoodList foodList) {
                addAIFoodCard(foodList);
            }

            @Override
            public void onOrderCard(ChatCardOrderEntity orderEntity) {
                addAIOrderCard(orderEntity);
            }

            @Override
            public void onHotelCard(List<ChatCardHotelModel> hotelModels) {
                addAIHotelCard(hotelModels);
            }

            @Override
            public void onPlaneCard(List<ChatCardPlandEntity> planeEntities) {
                addAIPlandCard(planeEntities);
            }

            @Override
            public void onPlanCard(PlanProgressType planProgressType, ChatCardPlanEntity.ContentBean planEntities, String reqUrl) {
                addAIPlanCard(planProgressType, planEntities, reqUrl);
            }

            @Override
            public void onTrainCard(List<ChatCardTrainEntity> trainEntities) {
                addAITrainCard(trainEntities);
            }

            @Override
            public void onGUiPermissionCard() {
                messageRenderService.addGuiPermissionCard(ChatAdapter.TYPE_ASSISTANT_FLOAT_PERM_CARD);
            }

//            @Override
//            public void onFloatPermissionCard() {
//                messageRenderService.addGuiPermissionCard(ChatAdapter.TYPE_ASSISTANT_FLOAT_PERM_CARD);
//            }
//
//            @Override
//            public void onAccessPermissionCard() {
//                messageRenderService.addGuiPermissionCard(ChatAdapter.TYPE_ASSISTANT_ACC_PERM_CARD);
//            }

            @Override
            public void onDeepResearch(DeepResearchBean deepResearchBeans) {

            }

            @Override
            public void onEnd() {
                isStreamEnd = true;
                endTime = System.currentTimeMillis();
                long second = (endTime - startTime) / 1000;

                // 只有当有思考内容时才设置思考标题，否则清空标题隐藏思考布局
                if (ResponseThink != null && !ResponseThink.isEmpty()) {
                    aiMessage.setThinkMessageTitle("思考过程（用时" + second + "秒）");
                    thinkMessageTitle.postValue("思考过程（用时" + second + "秒）");
                } else {
                    aiMessage.setThinkMessageTitle("");
                    thinkMessageTitle.postValue("");
                }

                aiMessage.setStatus(Constant.ThinkState.END);
                thinkStatus.postValue(Constant.ThinkState.END);
                chatMessages.postValue(chatMessages.getValue());
                new Handler(Looper.getMainLooper()).postDelayed(() -> streamEnd.postValue(true),1500);
                if (isLingxiPath && !TextUtils.isEmpty(fullResponse) && !hasAddHistory) {
                    hasAddHistory = true;
                    boolean ttsPlaying = aiMessage.isTTSPlaying();
                    aiMessage.setTTSPlaying(false);
					if (params.agentModel == LocalModule.ACTION) {
						addConversationHistory("任务已执行", "assistant", true, aiMessage::setId);
					} else {
                        addConversationHistory(GsonUtils.toJson(aiMessage), "assistant", true, aiMessage::setId);
					}
                    aiMessage.setTTSPlaying(ttsPlaying);
                }

            }

            @Override
            public void onBillError(String message) {
                benefitErrorLiveData.postValue(message);
                isStreamEnd = true;
                aiMessage.setStatus(Constant.ThinkState.END);
                chatMessages.postValue(chatMessages.getValue());
                aiMessage.setMessage(message);
                streamEnd.postValue(true);
            }

            @Override
            public void onError(String message) {
                setError(message);
            }

            @Override
            public void onMessageIds(Integer sendId, Integer receiveId) {
                // 处理消息ID匹配
                handleMessageIds(sendId, receiveId);
            }

            @Override
            public void onMusicCard(MusicData musicData) {
                if (messageRenderService != null) {
                     messageRenderService.addMusicCard(musicData);
                }
                if (TextUtils.isEmpty(musicData.getName())){
                    addConversationHistory( "儿童故事暂不支持播放", "assistant", true);
                }else {
                    addConversationHistory(musicData.getName(), "assistant", true);
                }
            }

            @Override
            public void addGuiUserMsg(String content) {
                addUserMsg(content);
                addConversationHistory(content, "user", true);
            }

            @Override
            public void addGuiAiMsg(String content) {
                addAIMsg(content);
                addConversationHistory(content, "assistant", true);
            }
        }, isSaveHistory);
    }

	@Nullable
	private LocalModule getLocalModule() {
		LocalModule agentModel = null;
		if (selectAgentBean != null && selectAgentBean.getModelName() != null) {
			agentModel = selectAgentBean.getModelName().equals(Constants.AGENT_TRIP) ? LocalModule.TRIP
					: selectAgentBean.getModelName().equals(Constants.AGENT_TRAVEL) ? LocalModule.TRAVEL
					: selectAgentBean.getModelName().equals(Constants.AGENT_MGVIDOE) ? LocalModule.MGVIDOE
					: selectAgentBean.getModelName().equals(Constants.AGENT_FINANCE) ? LocalModule.FINANCE
					: selectAgentBean.getModelName().equals(Constants.AGENT_GUI) ? LocalModule.ACTION
					: selectAgentBean.getModelName().equals(Constants.AGENT_COMMUNICATION) ? LocalModule.COMMUNICATION : null;
		}
		return agentModel;
	}

	public void closeSSE() {
        if (sseDisposable != null && !sseDisposable.isDisposed()) {
            sseDisposable.dispose();
        }
        handleStreamTimeout();
    }

    // 初始化流式超时检查任务
    private void initStreamTimeoutCheck(ChatMessage message, String conversationId) {
        // 若任务已存在，先移除避免重复
        if (streamTimeoutRunnable != null) {
            ttsHandler.removeCallbacks(streamTimeoutRunnable);
        }
        // 定义超时任务：检查是否超过1秒未收到新数据
        streamTimeoutRunnable = () -> {
            if (!TextUtils.isEmpty(ttsContent)) {
                long currentTime = System.currentTimeMillis();
                // 计算与最后接收时间的差值
                if (currentTime - lastStreamReceiveTime >= STREAM_TIMEOUT_MS) {
                    // 超时：强制播放剩余内容
                    ZUtils.print("流式数据超时1秒，强制播放剩余内容：" + ttsContent);
                    playTTSContent(message, ttsContent, conversationId, false);
                    ttsContent = "";
                } else {
                    // 未超时：继续检查（每隔500ms轮询一次）
                    ttsHandler.postDelayed(streamTimeoutRunnable, 500);
                }
            }
        };
        // 首次延迟500ms启动检查（避免频繁触发）
        ttsHandler.postDelayed(streamTimeoutRunnable, 500);
    }

    // 处理空内容（流式结束时强制播放）
    private void handleStreamTimeout() {
        if (!TextUtils.isEmpty(ttsContent)) {
            ZUtils.print("收到流式结束标识，播放剩余内容：" + ttsContent);
            TTSManager.Companion.getInstance().stop();
            ttsContent = "";
        }
        // 移除超时任务
        if (streamTimeoutRunnable != null) {
            ttsHandler.removeCallbacks(streamTimeoutRunnable);
        }
    }

    /**
     * 停止当前的思考和生成过程，并更新最后一条AI消息的状态
     */
    public void stopThinkingAndGeneration(String messageCon) {
        // 关闭SSE连接
        closeSSE();
        clearLingxiFlowCallbacks();
        if (thinkStatus.getValue() == Constant.ThinkState.END) {
            return;
        }
        if (chatDataFormat != null) {
            chatDataFormat.setBreakDialog(true);
            chatDataFormat.stopTypingTask();
        }
        // 设置全局状态
        thinkStatus.setValue(Constant.ThinkState.END);
        streamEnd.postValue(true);

        // 更新最后一条AI消息的状态
        List<ChatMessage> messages = chatMessages.getValue();
        if (messages != null && !messages.isEmpty()) {
            // 从后往前查找最后一条AI消息
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage message = messages.get(i);
                if (message.getMsgType() == ChatAdapter.TYPE_AI) {
                    // 设置消息状态为结束
                    message.setStatus(Constant.ThinkState.END);
                    // 如果没有思考内容，清空思考消息标题以隐藏思考布局
//                    if (message.getThinkMessage() == null || message.getThinkMessage().isEmpty()) {
                    message.setThinkMessageTitle("");
                    message.setThinkMessage("");
//                    }
                    message.setMessage(messageCon);
                    // 通知数据变化
                    chatMessages.postValue(messages);
                    if (selectOptionModel != null && TextUtils.equals("10086", selectOptionModel.getModel())) {
                        // addConversationHistory(GsonUtils.toJson(message), "assistant", true);
                    }
                    break;
                }
            }
        }
    }

    @Override
    protected void onCleared() {
        netStateUnregister();
        closeSSE();
        HandlerLlm.INSTANCE.close();
        clearLingxiFlowCallbacks();
        handler.removeCallbacksAndMessages(null);
    }

    private void clearLingxiFlowCallbacks() {
        HandlerLlm.INSTANCE.clearCallback();
        HonorQueueManager.clearCallback();
        if (chatDataFormat != null) {
            chatDataFormat.stopTypingTask();
        }
    }

    public void resendMsg() {
        List<ChatMessage> list = chatMessages.getValue();
        ChatMessage msg = getResendMsg();

    }

    public void removeLast2Msg() {
        List<ChatMessage> list = chatMessages.getValue();
        list.remove(list.size() - 1);
        list.remove(list.size() - 1);
        chatMessages.postValue(list);

    }

    public ChatMessage getResendMsg() {
        List<ChatMessage> list = chatMessages.getValue();
        if (list.size() > 1) {
            return list.get(list.size() - 2);
        }
        return null;
    }

    public ChatMessage getResendImageFileMsg() {
        List<ChatMessage> list = chatMessages.getValue();
        if (list.size() > 1) {
            ChatMessage lastUserMsg = list.get(list.size() - 2);
            if (lastUserMsg != null && (lastUserMsg.getMsgType() == ChatAdapter.TYPE_USER_FILE_IMAGE ||
                    lastUserMsg.getMsgType() == ChatAdapter.TYPE_USER_FILE)) {
                return list.get(list.size() - 3);//文件或图片，获取倒数第三个
            } else {
                return lastUserMsg;
            }
        }
        return null;
    }


    // 生成图片（后续重构）
    public void generateImage() {
        Timber.tag("VMDrawing").d("=== generateImage called ===");
//        Timber.tag("VMDrawing").d( "Call stack: " + android.util.Log.getStackTraceString(new Throwable()));
//        Timber.tag("VMDrawing").d( "generateEnabled: " + generateEnabled.get());
//        Timber.tag("VMDrawing").d( "isGenerating: " + isGenerating.get());
//        Timber.tag("VMDrawing").d( "prompt: " + prompt.get());
//        Timber.tag("VMDrawing").d( "selectedStyle: " + (selectedStyle != null ? selectedStyle.getName() + " (ID: " + selectedStyle.getId() + ")" : "null"));
//        Timber.tag("VMDrawing").d( "selectedRatio: " + selectedRatio.get());
//        Timber.tag("VMDrawing").d( "referenceImageUrl: " + referenceImageUrl);
//        Timber.tag("VMDrawing").d( "isContinueEditMode: " + isContinueEditMode);


        if (/*!generateEnabled.get() ||*/ isGenerating.get()) {
//            android.util.Timber.tag(TAG).w(("VMDrawing", "Generation blocked: generateEnabled=" + generateEnabled.get() + ", isGenerating=" + isGenerating.get());
            return;
        }

        // 准备请求参数（直接使用本地变量与 DrawingFlow.Params 传递，无需中间请求对象）

        if (selectDrawingToChatBean == null) {
            return;
        }
        // 组合提示词
        String prompt = selectDrawingToChatBean.getPrompt().trim();
        String fullPrompt = selectDrawingToChatBean.getPrompt().trim();
        // 腾讯生图 编辑效果太差 去除
        if (!isContinueEditMode) {
            referenceImageUrl = null;
        } else {
            referenceImageUrl = selectDrawingToChatBean.getReference_image_url();
        }
        DrawingStyleDto selectedStyle = selectDrawingStyleDto;

//        Timber.tag("VMDrawing").d( "Original prompt: " + fullPrompt);
//
//        // 如果有hiddenPrompt（继续编辑模式），则将其与用户输入组合
        if (hiddenPrompt != null && !hiddenPrompt.isEmpty()) {
            Timber.tag("VMDrawing").d("Hidden prompt: " + hiddenPrompt);
            // 继续编辑模式：组合隐藏的prompt和新输入
            if (!fullPrompt.isEmpty()) {
                fullPrompt = hiddenPrompt + ", " + fullPrompt;
            } else {
                fullPrompt = hiddenPrompt;
            }
            Timber.tag("VMDrawing").d("Combined with hidden prompt: " + fullPrompt);
        }

        // 如果有参考图片（做同款/继续编辑），只使用用户输入的提示词，不追加风格描述
        // 因为用户通常只想修改局部内容（如"头发换成红色"）
        if (referenceImageUrl == null || referenceImageUrl.isEmpty()) {
            // 只有在没有参考图片时，才追加风格提示词
            if (selectedStyle != null && selectedStyle.getPrompt() != null) {
                Timber.tag("VMDrawing").d("Adding style prompt: " + selectedStyle.getPrompt());
                fullPrompt += ", " + selectedStyle.getPrompt();
                Timber.tag("VMDrawing").d("Final prompt with style: " + fullPrompt);
            }
        }
        // 设置宽高：使用 AspectRatioDto 中的尺寸
        String selectedRatioStr = selectedRatio;
        Timber.tag("VMDrawing").d("Getting selected ratio: " + selectedRatioStr);
        int width = 0;
        int height = 0;
        if (selectDrawingToChatBean.getAspectRatioDto() != null) {
            width = selectDrawingToChatBean.getAspectRatioDto().getWidth();
            height = selectDrawingToChatBean.getAspectRatioDto().getHeight();
            Timber.tag("VMDrawing").d("Calculated dimensions - width: " + width + ", height: " + height);
        }

        // 设置风格ID（仅用于日志与后续 Params）
        if (selectedStyle != null) {
            Timber.tag("VMDrawing").w("Setting style: " + selectedStyle.getName() + " (ID: " + selectedStyle.getId() + ")");
        } else {
            Timber.tag("VMDrawing").w("No style selected!");
        }

        // 添加调试日志
        Timber.tag("VMDrawing").w("Final prompt: " + fullPrompt);
        Timber.tag("VMDrawing").w("Final dimensions - width: " + width + ", height: " + height);
        Timber.tag("VMDrawing").w("Final styleId: " + (selectedStyle != null ? selectedStyle.getId() : null));

        // 设置参考图片URL（交由 Flow Params 传递）
        isGenerating.set(true);
        showResult.postValue(false);
        updateAIDrawingMsg(0);

        DrawingFlow.Params p = new DrawingFlow.Params();
        p.userPrompt = prompt;
        p.finalPrompt = fullPrompt;
        p.referenceImageUrl = referenceImageUrl;
        p.style = selectedStyle;
        p.width = width;
        p.height = height;
        p.sessionId = conversationId.get(); // 若为0则由 DrawingFlow 创建
        p.continueEditMode = isContinueEditMode;
        p.hiddenPrompt = hiddenPrompt;
        p.aspectRatioDto = selectDrawingToChatBean.getAspectRatioDto();

        drawingFlow.startGeneration(p, new DrawingFlow.Callback() {
            @Override
            public void onSessionCreated(DrawingSessionDto sessionDto) {
                // Flow 创建会话后，同步 VM 的当前会话
                conversationId.setValue(sessionDto.getId());
                currentSession.postValue(sessionDto);
                clearError();
            }

            @Override
            public void onInitProgress(DrawingImageDto preview, int initProgress, String initText) {
                progress.set(initProgress);
                progressText.set(initText);
                updateAIDrawingMsg(preview);
            }

            @Override
            public void onProgress(int pValue, String text) {
                progress.set(pValue);
                progressText.set(text);
                updateAIDrawingMsg(pValue);
            }

            @Override
            public void onComplete(DrawingImageDto imageDto, String finalImageUrl) {
                mainHandler.post(() -> {
                    generatedImage.postValue(imageDto);

                    isGenerating.set(false);
                    progress.set(100);
                    DrawingImageDto msgDto = aiDrawingMsg.getDrawingImageDto();
                    msgDto.setPicUrl(finalImageUrl);
                    updateAIDrawingMsg(msgDto);
                    updateAIDrawingMsg(100);
                    updateAIDrawingMsg(finalImageUrl);
                    progressText.set("生成完成");
                    showResult.postValue(true);
                    setSuccess("图片生成成功");
                    clearReferenceImageUrl();

                    List<ChatMessage> list = chatMessages.getValue();
                    if (list != null && list.size() - 2 >= 0) {
                        list.get(list.size() - 1).setId(imageDto.getId());
                        list.get(list.size() - 2).setId(imageDto.getId());
                        chatMessages.postValue(list);
                    }
                });
            }

            @Override
            public void onBillError(String errorCode, String errorMsg) {
                mainHandler.post(() -> {
                    isGenerating.set(false);
                    updateAIDrawingMsg(0);
                    BillDialogHelper.showBillDialog(activityRef.get(), errorMsg, () -> activityRef.get().finish());
                    clearReferenceImageUrl();
                });
            }

            @Override
            public void onError(String errorMsg) {
                mainHandler.post(() -> {
                    isGenerating.set(false);
                    updateAIDrawingMsg(0);
                    setError(errorMsg != null ? errorMsg : "生成失败");
                    clearReferenceImageUrl();
                });
            }
        });
    }


    // 已迁移：比例计算与会话创建由 DrawingFlow 承担

    /**
     * 清除参考图片URL
     */
    public void clearReferenceImageUrl() {
        this.referenceImageUrl = null;
    }

    /**
     * 获取云端app 信息
     */
    private void getAppList() {
        try {
            Type listType = new TypeToken<ArrayList<AppData>>() {
            }.getType();
            ArrayList<AppData> appList = JsonUtil.parseJson(getApplication().getApplicationContext(), "app_list.json", listType);
            AppListHelper.INSTANCE.setAppList(appList);
        } catch (Exception e) {
            Timber.d("app列表解析失败%s", e.getMessage());
            e.printStackTrace();
        }
    }

    //主页最近对话列表
    public void saveList() {
        List<ChatMessage> messages = chatMessages.getValue();
        Gson gson = new Gson();
        String jsonString = gson.toJson(messages);
        SharedPreferencesUtil.saveString(Constants.PREF_RECENT_CONVERSATION_LIST, jsonString);
    }

    /**
     * 播报 TTS 语音内容
     *
     * @param content    需要播报的内容
     * @param isComplete 是否是完整内容播报（true表示完整内容，false表示流式内容片段）
     */
    public void playTTSContent(ChatMessage message, String content, String conversationId, boolean isComplete) {
        if (!Boolean.TRUE.equals(isAutoPlay.getValue())) return;
//        Markwon markwon = MarkdownUtils.createMarkwon(getApplication());
//        Spanned markdown = markwon.toMarkdown(content);
        // message.setTTSPlaying(true);
        TTSManager.getInstance().setOnPlayerListener(new OnPlayerListener() {
            @Override
            public void playerStart() {
                message.setTTSPlaying(true);
                List<ChatMessage> list = chatMessages.getValue();
                chatMessages.postValue(list);
            }

            @Override
            public void playerStop() {
                message.setTTSPlaying(false);
                List<ChatMessage> list = chatMessages.getValue();
                chatMessages.postValue(list);
            }
        });
        if (isComplete) {
            ttsAudioService.playText(content, conversationId, true, isAutoPlay.getValue());
        } else {
            ttsAudioService.playText(content, conversationId, false, isAutoPlay.getValue());
        }
        /*TTSUtils.getInstance().ttsText(res.getData().getReceive().getContent(),false);*/
        /*TTSUtils.getInstance().ttsText("",true);*/
        /*TTSUtils.getInstance().ttsStart();*/
        /*TTSUtils.getInstance().ttsStop();*/
    }

    /**
     * 播报 TTS 语音内容
     *
     * @param url        需要播报的内容
     * @param isComplete 是否是完整内容播报（true表示完整内容，false表示流式内容片段）
     */
    public void playTTSUrl(String url, String conversationId, boolean isComplete) {
        if (!Boolean.TRUE.equals(isAutoPlay.getValue())) return;
        if (isComplete) {
            ttsAudioService.playUrl(url, conversationId, true, isAutoPlay.getValue());
        } else {
            ttsAudioService.playUrl(url, conversationId, false, isAutoPlay.getValue());
        }
        /*TTSUtils.getInstance().ttsText(res.getData().getReceive().getContent(),false);*/
        /*TTSUtils.getInstance().ttsText("",true);*/
        /*TTSUtils.getInstance().ttsStart();*/
        /*TTSUtils.getInstance().ttsStop();*/
    }

    /**
     * 文本翻译功能
     * <p>
     * 该方法实现文本翻译功能，将指定内容从源语言翻译为目标语言。
     * 翻译过程是异步的，结果通过回调函数处理。
     * 翻译完成后会将结果作为AI消息添加到聊天记录中，并播放TTS语音。
     *
     * @param content  待翻译的文本内容
     * @param fromLang 源语言，例如"中文"、"英语"等
     * @param toLang   目标语言，例如"中文"、"英语"等
     */
    private void textTranslate(String content, String fromLang, String toLang) {
        addConversationHistory(content, "user", true);
        startTime = System.currentTimeMillis();
        ChatMessage aiMessage = addAIMsg();
        aiMessage.setFromLang(fromLang);
        aiMessage.setToLang(toLang);
        aiMessage.setTranslationMsg(true);
        aiMessage.setThinkMessage("");
        aiMessage.setThinkMessageTitle("正在思考中"); // START状态显示"正在思考中"作为加载指示器
        aiMessage.setStatus(Constant.ThinkState.START);
        thinkMessage.postValue("");
        thinkMessageTitle.postValue("正在思考中");
        thinkStatus.postValue(Constant.ThinkState.START);
        isStreamEnd = false;
        streamEnd.postValue(false);
        translationInteractor.textTranslate(content, fromLang, toLang, response -> {
            String translatedText = response.getData().getTranslateText();
            fullResponse = translatedText;

            aiMessage.setThinkMessage(ResponseThink);
            aiMessage.setMessage(fullResponse);
            aiMessage.setStatus(Constant.ThinkState.THINKING);
            thinkMessage.postValue(ResponseThink);
            aiResponse.postValue(fullResponse);

            isStreamEnd = true;
            endTime = System.currentTimeMillis();
            long second = (endTime - startTime) / 1000;

            // 只有当有思考内容时才设置思考标题，否则清空标题隐藏思考布局
            if (ResponseThink != null && !ResponseThink.isEmpty()) {
                aiMessage.setThinkMessageTitle("思考过程（用时" + second + "秒）");
                thinkMessageTitle.postValue("思考过程（用时" + second + "秒）");
            } else {
                aiMessage.setThinkMessageTitle("");
                thinkMessageTitle.postValue("");
            }

            aiMessage.setStatus(Constant.ThinkState.END);
            thinkStatus.postValue(Constant.ThinkState.END);
            chatMessages.postValue(chatMessages.getValue());
            streamEnd.postValue(true);
            Timber.tag(TAG).i("Translation 翻译结果: %s", translatedText);
            if (!TTSManager.getInstance().getTargetLanguageList().contains(aiMessage.getToLang())) {
                mainHandler.post(() -> GlobalToast.show(activityRef.get(), "暂不支持语音朗读", GlobalToast.Type.ERROR));
            } else {
                playTTSContent(aiMessage, fullResponse, "", true);
            }
            TabEntity.agentType = TabEntity.TabType.CHAT;
            addConversationHistory(GsonUtils.toJson(aiMessage), "assistant", true);
            return null;
        },msg -> {
            benefitErrorLiveData.postValue(msg);
            isStreamEnd = true;
            thinkStatus.postValue(Constant.ThinkState.END);
            aiMessage.setStatus(Constant.ThinkState.END);
            chatMessages.postValue(chatMessages.getValue());
            aiMessage.setMessage(msg);
            streamEnd.postValue(true);
        });

    }

    /**
     * 处理按下触发的业务流程
     * <p>
     * 该方法在用户按下按钮时调用，用于启动实时翻译功能。
     * 在执行前会检查是否应该跳过翻译，如果不应该跳过则启动实时翻译。
     *
     * @return boolean 返回 true 表示成功启动翻译功能，返回 false 表示跳过了翻译启动
     * 当 shouldSkipTranslation() 返回 true 时会跳过翻译启动
     */
    public boolean pressDownBusinessProcessFlow() {
        if (shouldSkipTranslation()) return false;
        realTimeTranslation();
        return true;
    }

    /**
     * 处理按下释放的业务流程
     * <p>
     * 该方法在用户释放按钮时调用，用于停止实时翻译功能并释放相关资源。
     * 在执行前会检查是否应该跳过翻译以及翻译管理器是否存在。
     *
     * @return boolean 返回 true 表示成功执行释放操作，返回 false 表示跳过了释放操作
     * 当 shouldSkipTranslation() 返回 true 时会跳过，或者 translationManager 为 null 时也会跳过
     */
    public boolean pressUpBusinessProcessFlow() {
        if (shouldSkipTranslation()) return false;
        if (translationInteractor == null) return false;
        translationInteractor.stopRealTime();
        return true;
    }

    /**
     * 实时翻译功能
     * <p>
     * 该方法实现语音实时翻译功能，包括语音识别(ASR)和翻译两个过程。
     * 创建 TranslationManager 实例，并注册回调来处理识别和翻译结果。
     * 当翻译完成后，会将结果作为 AI消息添加到聊天记录中，并播放 TTS 语音。
     * <p>
     * 主要流程：
     * 1. 初始化 TranslationManager 并设置回调监听器
     * 2. 启动翻译过程，根据指定的语言进行语音识别和翻译
     * 3. 在回调中处理中间结果、最终结果和翻译结果
     * 4. 翻译完成后构建AI消息并更新UI状态
     * 5. 播放TTS语音内容
     */
    public void realTimeTranslation() {

        fullResponse = "";

        translationManager = new TranslationManager(getApplication().getApplicationContext(), new TranslationManager.TranslationCallback() {

            ChatMessage aiMessage = null;
            String asrFinalResult = "";

            @Override
            public void onAsrMidResult(@NonNull String midResult) {
                // 处理 ASR 中间结果
                Timber.tag(TAG).i("语音识别的中间结果: %s", midResult);
            }

            @Override
            public void onAsrFinalResult(@NonNull String finalResult) {
                // 处理 ASR 最终结果
                if (TextUtils.isEmpty(finalResult)) return;
                asrFinalResult += finalResult;
                Timber.tag(TAG).i("语音识别结果: %s", asrFinalResult);
            }

            @Override
            public void onTranslationResult(@NonNull String translationResult) {
                // 处理翻译结果
                if (TextUtils.isEmpty(translationResult)) return;
                fullResponse += translationResult;

                Timber.tag(TAG).i("Translation 翻译结果: %s", fullResponse);
            }

            @Override
            public void onComplete() {
                // 处理完成事件
                Timber.tag(TAG).i("onComplete");
                if (TextUtils.isEmpty(fullResponse)) return;
                List<ChatMessage> list;
                if (conversationId.get() != 0) {
                    list = chatMessages.getValue();
                    chatMessages.postValue(list);
                }
                addUserMsg(asrFinalResult);

                startTime = System.currentTimeMillis();
                aiMessage = addAIMsg();
                aiMessage.setThinkMessage("");
                aiMessage.setThinkMessageTitle("正在思考中"); // START状态显示"正在思考中"作为加载指示器
                aiMessage.setStatus(Constant.ThinkState.START);
                thinkMessage.postValue("");
                thinkMessageTitle.postValue("正在思考中");
                thinkStatus.postValue(Constant.ThinkState.START);
                isStreamEnd = false;
                streamEnd.postValue(false);

                aiMessage.setThinkMessage(ResponseThink);
                aiMessage.setMessage(fullResponse);
                aiMessage.setStatus(Constant.ThinkState.THINKING);
                thinkMessage.postValue(ResponseThink);
                aiResponse.postValue(fullResponse);

                isStreamEnd = true;
                endTime = System.currentTimeMillis();
                long second = (endTime - startTime) / 1000;

                // 只有当有思考内容时才设置思考标题，否则清空标题隐藏思考布局
                if (ResponseThink != null && !ResponseThink.isEmpty()) {
                    aiMessage.setThinkMessageTitle("思考过程（用时" + second + "秒）");
                    thinkMessageTitle.postValue("思考过程（用时" + second + "秒）");
                } else {
                    aiMessage.setThinkMessageTitle("");
                    thinkMessageTitle.postValue("");
                }

                aiMessage.setStatus(Constant.ThinkState.END);
                thinkStatus.postValue(Constant.ThinkState.END);
                chatMessages.postValue(chatMessages.getValue());
                streamEnd.postValue(true);

                playTTSContent(aiMessage, fullResponse, "", true);
                TabEntity.agentType = TabEntity.TabType.CHAT;

            }
        }, benefitErrorLiveData::postValue);

        // 启动实时翻译由用例负责
        translationInteractor.startRealTime(superEditAITranslateUtil.getSleet1(), superEditAITranslateUtil.getSleet2(), new TranslationManager.TranslationCallback() {
            ChatMessage aiMessage = null;
            String asrFinalResult = "";

            @Override
            public void onAsrMidResult(@NonNull String midResult) {
                Timber.tag(TAG).i("语音识别的中间结果: %s", midResult);
            }

            @Override
            public void onAsrFinalResult(@NonNull String finalResult) {
                if (TextUtils.isEmpty(finalResult)) return;
                asrFinalResult += finalResult;
                Timber.tag(TAG).i("语音识别结果: %s", asrFinalResult);
            }

            @Override
            public void onTranslationResult(@NonNull String translationResult) {
                if (TextUtils.isEmpty(translationResult)) return;
                fullResponse += translationResult;
                Timber.tag(TAG).i("Translation 翻译结果: %s", fullResponse);
            }

            @Override
            public void onComplete() {
                Timber.tag(TAG).i("onComplete");
                if (TextUtils.isEmpty(fullResponse)) return;
                if (conversationId.get() != 0) {
                    chatMessages.postValue(chatMessages.getValue());
                }
                addUserMsg(asrFinalResult);
                startTime = System.currentTimeMillis();
                aiMessage = addAIMsg();
                aiMessage.setThinkMessage("");
                aiMessage.setThinkMessageTitle("正在思考中"); // START状态显示"正在思考中"作为加载指示器
                aiMessage.setStatus(Constant.ThinkState.START);
                thinkMessage.postValue("");
                thinkMessageTitle.postValue("正在思考中");
                thinkStatus.postValue(Constant.ThinkState.START);
                isStreamEnd = false;
                streamEnd.postValue(false);
                aiMessage.setThinkMessage(ResponseThink);
                aiMessage.setMessage(fullResponse);
                aiMessage.setStatus(Constant.ThinkState.THINKING);
                thinkMessage.postValue(ResponseThink);
                aiResponse.postValue(fullResponse);
                isStreamEnd = true;
                endTime = System.currentTimeMillis();
                long second = (endTime - startTime) / 1000;

                // 只有当有思考内容时才设置思考标题，否则清空标题隐藏思考布局
                if (ResponseThink != null && !ResponseThink.isEmpty()) {
                    aiMessage.setThinkMessageTitle("思考过程（用时" + second + "秒）");
                    thinkMessageTitle.postValue("思考过程（用时" + second + "秒）");
                } else {
                    aiMessage.setThinkMessageTitle("");
                    thinkMessageTitle.postValue("");
                }

                aiMessage.setStatus(Constant.ThinkState.END);
                thinkStatus.postValue(Constant.ThinkState.END);
                chatMessages.postValue(chatMessages.getValue());
                streamEnd.postValue(true);
                playTTSContent(aiMessage, fullResponse, "", true);
                TabEntity.agentType = TabEntity.TabType.CHAT;
            }
        }, benefitErrorLiveData::postValue);
    }

    /**
     * 判断是否应该跳过翻译处理
     * <p>
     * 在以下情况下会跳过翻译：
     * 1. 未选择任何选项模型
     * 2. 选择的模型不是"10086"
     * 3. 当前标签页不是翻译类型
     *
     * @return true 表示应该跳过翻译，false 表示需要进行翻译处理
     */
    private boolean shouldSkipTranslation() {
        if (selectOptionModel == null) return true;
        if (!Objects.equals(selectOptionModel.getModel(), "10086")) return true;
        return TabEntity.agentType != TabEntity.TabType.TRANSLATE;
    }

    /**
     * 添加/批量添加历史数据
     *
     * @param message 添加的参数（对话的信息）
     * @param type    消息的类型（user表示发送消息, assistant表示回复的消息）
     */
    public void addConversationHistory(String message, String type, boolean needCreate) {
        addConversationHistory(message, type, needCreate, null);
    }

    /**
     * 添加/批量添加历史数据
     *
     * @param message 添加的参数（对话的信息）
     * @param type    消息的类型（user表示发送消息, assistant表示回复的消息）
     */
    public void addConversationHistory(String message, String type, boolean needCreate, DataCallback callback) {
        String conversationID = String.valueOf(conversationId.get());
        if (TextUtils.isEmpty(conversationID)) {
            Timber.tag(TAG).d("addConversationHistory error");
            return;
        }
        Timber.tag(TAG).d("addConversationHistory conversationId " + conversationID + " message " + message + " type " + type);
        // 创建 messages 列表
        List<Map<String, Object>> messages = new ArrayList<>();
        // 创建第一条消息
        Map<String, Object> message1 = new HashMap<>();
        message1.put("type", type);
        message1.put("content", message);
        message1.put("model", selectOptionModel != null ? selectOptionModel.getModel() : selectAgentBean.getModelName());
        message1.put("modelId", selectOptionModel != null ? selectOptionModel.getId() : selectAgentBean.getModelId());
        message1.put("createTime", System.currentTimeMillis());
        // 将消息添加到 messages 列表
        messages.add(message1);

        ChatRepository chatRepository = new ChatRepositoryImpl();
        chatRepository.addConversationHistory(conversationID, messages, new ChatRepository.Callback<ArrayList<Integer>>() {
            @Override
            public void onSuccess(ArrayList<Integer> data) {
                for (int i = 0; i < data.size(); i++) {
                    Timber.tag("addConversationHistory1").d("data: %s", data.get(i));
                    if (callback != null) {
                        callback.onSuccess(Long.valueOf(data.get(i)));
                    }
                }
            }

            @Override
            public void onError(String error) {
                Timber.tag(TAG).d("addConversationHistory onError " + error);
                if (!TextUtils.isEmpty(error) && error.contains("对话不存在") && needCreate) {
                    conversationId.setValue(0l);
                    if (selectOptionModel != null) {
                        createMy(selectOptionModel.getModel(), message, () -> addConversationHistory(message, type, false));
                    } else if (selectAgentBean != null) {
                        String model = selectAgentBean.getBotId();
                        createMyAgent(model, selectAgentBean.getModelName(), selectAgentBean.getMenuId() + "", () -> addConversationHistory(message, type, false));
                    }
                }
            }
        });
    }

    public void setThinkingModeEnabled(boolean enabled) {
        this.thinkingModeEnabled = enabled;
    }

    public boolean isThinkingModeEnabled() {
        return thinkingModeEnabled;
    }

    /**
     * 处理流式返回中的消息ID匹配
     *
     * @param sendId    用户发送消息的ID
     * @param receiveId AI回复消息的ID
     */
    private void handleMessageIds(Integer sendId, Integer receiveId) {
        List<ChatMessage> currentMessages = chatMessages.getValue();
        if (currentMessages == null || currentMessages.isEmpty()) {
            return;
        }

        // 设置用户消息ID
        if (sendId != null) {
            // 从后往前查找最近的用户消息
            for (int i = currentMessages.size() - 1; i >= 0; i--) {
                ChatMessage message = currentMessages.get(i);
                if (message.getMsgType() == ChatAdapter.TYPE_USER && message.getId() == null) {
                    message.setId((long) sendId.intValue());
                    Timber.tag(TAG).d("设置用户消息ID: " + sendId + ", 位置: " + i);
                    break;
                }
            }
        }

        // 设置AI回复消息ID
        if (receiveId != null) {
            // 从后往前查找最近的AI消息
            for (int i = currentMessages.size() - 1; i >= 0; i--) {
                ChatMessage message = currentMessages.get(i);
                if (message.getMsgType() == ChatAdapter.TYPE_AI && message.getId() == null) {
                    message.setId((long) receiveId.intValue());
                    Timber.tag(TAG).d("设置AI消息ID: " + receiveId + ", 位置: " + i);
                    break;
                }
            }
        }

        // 通知UI更新（如果需要的话）
        chatMessages.postValue(currentMessages);
    }

    /**
     * 删除消息
     *
     * @param messageId 消息ID
     */
    public void deleteMessage(long messageId, ChatRepository.Callback<Boolean> callback) {
        Timber.tag("VMDrawing").w("开始删除消息，ID: " + messageId);

        // 先查找消息类型
        List<ChatMessage> currentMessages = chatMessages.getValue();
        ChatMessage targetMessage = null;
        if (currentMessages != null) {
            for (ChatMessage msg : currentMessages) {
                if (msg != null && msg.getId() != null && msg.getId().equals(messageId)) {
                    targetMessage = msg;
                    break;
                }
            }
        }

        // 如果是绘画消息，调用绘画删除接口
        if (targetMessage != null && targetMessage.getMsgType() == ChatAdapter.TYPE_AI_DRAWING) {
            Timber.tag(TAG).d("检测到绘画消息，使用绘画删除接口，ID: " + messageId);
            repository.deleteImage(messageId).observeForever(result -> {
                if (result.getData() != null && result.getData()) {
                    Timber.tag(TAG).d("绘画消息删除成功，ID: " + messageId);
                    GlobalToast.show(activityRef.get(), "删除成功", GlobalToast.Type.SUCCESS);
                    // 从本地列表中移除消息
                    removeMessageFromList(messageId);
                    if (callback != null){
                        callback.onSuccess(true);
                    }

                } else {
                    if (callback != null){
                        callback.onSuccess(false);
                    }
                    Timber.tag(TAG).w("绘画消息删除失败，ID: " + messageId + ", 错误: " + result.getError());
                    GlobalToast.show(activityRef.get(), "删除失败", GlobalToast.Type.ERROR);
                }
            });
            return;
        }

        // 普通消息使用ChatRepository来删除
        ChatRepository chatRepository = new ChatRepositoryImpl();
        chatRepository.deleteChatMessage(messageId, new ChatRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    Timber.tag(TAG).d("消息删除成功，ID: " + messageId);
                    // 从本地列表中移除消息
                    removeMessageFromList(messageId);
                    if (callback != null){
                        GlobalToast.show(activityRef.get(), "删除成功", GlobalToast.Type.SUCCESS);
                        callback.onSuccess(true);
                    }

                } else {
                    if (callback != null){
                        callback.onSuccess(false);
                    }
                    Timber.tag(TAG).w("消息删除失败，服务器返回false，ID: " + messageId);
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null){
                    callback.onSuccess(false);
                }
                Timber.tag(TAG).e("删除消息时发生错误，ID: " + messageId + ", 错误: " + error);
            }
        });
    }

    /**
     * 删除图片
     */
    public void deleteImage(long messageId) {
        if (messageId == 0) {
            return;
        }

        setLoading(true);

        repository.deleteImage(messageId).observeForever(result -> {
            setLoading(false);

            if (result.getData()) {
                // 从列表中移除
                // 从本地列表中移除消息
                removeMessageFromList(messageId);
                setSuccess("删除成功");
            } else {
                setError(result.getError() != null ? result.getError() : "删除失败");
            }
        });
    }

    /**
     * 从本地消息列表中移除指定ID的消息
     *
     * @param messageId 消息ID
     */
    private void removeMessageFromList(long messageId) {
        List<ChatMessage> currentMessages = chatMessages.getValue();
        if (currentMessages == null || currentMessages.isEmpty()) {
            return;
        }

        // 找到被删除消息的位置
        int targetIndex = -1;
        for (int i = 0; i < currentMessages.size(); i++) {
            ChatMessage msg = currentMessages.get(i);
            if (msg != null && msg.getId() != null && msg.getId().equals(messageId)) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) {
            Timber.tag(TAG).w("在本地列表中未找到要删除的消息，ID: " + messageId);
            return;
        }

        // 记录被删除消息类型
        ChatMessage targetMsg = currentMessages.get(targetIndex);

        // 先删除目标消息
        currentMessages.remove(targetIndex);

        // 如果删除的是用户消息，检查下一条是否是绘画消息，如果是则一起删除
        boolean isUserMessage = targetMsg.getMsgType() == ChatAdapter.TYPE_USER;
        if (isUserMessage && targetIndex < currentMessages.size()) {
            ChatMessage nextMsg = currentMessages.get(targetIndex);
            if (nextMsg != null && nextMsg.getMsgType() == ChatAdapter.TYPE_AI_DRAWING) {
                Long drawingMsgId = nextMsg.getId();
                // 从本地移除绘画消息
                currentMessages.remove(targetIndex);
                Timber.tag(TAG).d("联动删除绘画消息，本地已移除。drawingMsgId=" + drawingMsgId);
                
                // 如果绘画消息有服务器ID，则请求后端删除
                if (drawingMsgId != null) {
                    repository.deleteImage(drawingMsgId).observeForever(result -> {
                        if (result.getData() == null ){
                            chatMessages.postValue(currentMessages);
                            Timber.tag(TAG).d("从本地列表中移除消息成功，ID: " + messageId + ", 当前剩余: " + currentMessages.size());
                            return;
                        }
                        if (result.getData()) {
                            Timber.tag(TAG).d("联动删除绘画消息-后端结果: 成功");
                        } else {
                            Timber.tag(TAG).e("联动删除绘画消息-后端错误: " + result.getError());
                        }
                    });
                }
            }
        }
        
        // 如果删除的是绘画消息，检查上一条是否是用户消息，如果是则一起删除
        boolean isDrawingMessage = targetMsg.getMsgType() == ChatAdapter.TYPE_AI_DRAWING;
        if (isDrawingMessage && targetIndex > 0) {
            ChatMessage prevMsg = currentMessages.get(targetIndex - 1);
            if (prevMsg != null && prevMsg.getMsgType() == ChatAdapter.TYPE_USER) {
                Long userMsgId = prevMsg.getId();
                // 从本地移除用户消息
                currentMessages.remove(targetIndex - 1);
                Timber.tag(TAG).d("联动删除用户消息，本地已移除。userMsgId=" + userMsgId);
                
                // 如果用户消息有服务器ID，则请求后端删除
                if (userMsgId != null) {
                    ChatRepository chatRepository = new ChatRepositoryImpl();
                    chatRepository.deleteChatMessage(userMsgId, new ChatRepository.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            Timber.tag(TAG).d("联动删除用户消息-后端结果: " + result);
                        }

                        @Override
                        public void onError(String error) {
                            Timber.tag(TAG).e("联动删除用户消息-后端错误: " + error);
                        }
                    });
                }
            }
        }

        // 如是用户文本问题，则尝试联动删除其紧邻的图片/文件消息（发送时紧随其后）
         /* boolean isUserTextQuestion = targetMsg.getMsgType() == ChatAdapter.TYPE_USER;
        boolean isH5Card = targetMsg.getH5CardContent() != null;
        boolean isDrawingMessage = targetMsg.getMsgType() == ChatAdapter.TYPE_DRAWING;
        if (isUserTextQuestion && targetIndex < currentMessages.size()) {
            ChatMessage neighbor = currentMessages.get(targetIndex); // 删除后，原 targetIndex 位置即为后一个元素
            if (neighbor != null && (neighbor.getMsgType() == ChatAdapter.TYPE_USER_FILE_IMAGE
                    || neighbor.getMsgType() == ChatAdapter.TYPE_USER_FILE)) {
                Long neighborId = neighbor.getId();
                // 先从本地移除
                currentMessages.remove(targetIndex);
                Timber.tag(TAG).d("联动删除图片/文件消息，本地已移除。neighborId=" + neighborId);
                // 若图片/文件消息存在服务器ID，则请求后端删除
                if (neighborId != null) {
                    ChatRepository chatRepository = new ChatRepositoryImpl();
                    chatRepository.deleteChatMessage(neighborId, new ChatRepository.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            Timber.tag(TAG).d("联动删除图片/文件消息-后端结果: " + result);
                        }

                        @Override
                        public void onError(String error) {
                            Timber.tag(TAG).e("联动删除图片/文件消息-后端错误: " + error);
                        }
                    });
                }
            }
        } else if (targetIndex < currentMessages.size()) {
            if (!isH5Card) {
                // 非用户文本：按需求删除本条和下一条
                ChatMessage neighbor = currentMessages.get(targetIndex);
                currentMessages.remove(targetIndex);
                Long neighborId = neighbor != null ? neighbor.getId() : null;
                /* if (neighborId != null) {
                    ChatRepository chatRepository = new ChatRepositoryImpl();
                    chatRepository.deleteChatMessage(neighborId, new ChatRepository.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            android.util.Log.d("VMChat", "联动删除下一条消息-后端结果: " + result);
                        }

                        @Override
                        public void onError(String error) {
                            android.util.Log.e("VMChat", "联动删除下一条消息-后端错误: " + error);
                        }
                    });
                }
            }
        }

        // 如是绘画消息，则联动移除缓存中同 id 的消息（例如与之成对的上一条）
        if (isDrawingMessage) {
            for (int i = currentMessages.size() - 1; i >= 0; i--) {
                ChatMessage msg = currentMessages.get(i);
                if (msg != null && msg.getId() != null && msg.getId().equals(messageId)) {
                    currentMessages.remove(i);
                }
            }
        } */

        chatMessages.postValue(currentMessages);
        Timber.tag(TAG).d("从本地列表中移除消息成功，ID: " + messageId + ", 当前剩余: " + currentMessages.size());
    }

    public void setDisposable(Disposable disposable) {
        sseDisposable = disposable;
    }


    // 添加儿童故事播放
    public void addMusic(MusicData musicData) {
        ChatMessage aiMsg;
        if (messageRenderService != null) {
            aiMsg = messageRenderService.addMusicCard(musicData);
        } else {
            List<ChatMessage> list = chatMessages.getValue();
            if (list == null) list = new ArrayList<>();
            aiMsg = new ChatMessage(musicData, ChatAdapter.TYPE_MUSIC);
            list.add(aiMsg);
            chatMessages.postValue(list);
        }
        addConversationHistory(GsonUtils.toJson(aiMsg), "assistant", true, aiMsg::setId);
    }

    /**
     * 删除智能体绘画id，防止历史记录存在
     */
    public void deleteConversation(long conversation,ChatRepository.Callback<Boolean> callback) {
        long id = conversationId.get();
        if (id == 0) {
            id = conversation;
        }
        Timber.tag(TAG).i("删除会话id =%s", id);
        if (id == 0) {
            return;
        }
        chatRepository.deleteConversation(id, new ChatRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                Timber.tag(TAG).i("删除会话成功 =%s", data);
                if (callback != null){
                    callback.onSuccess(true);
                }

            }

            @Override
            public void onError(String error) {
                Timber.tag(TAG).i("删除会话失败 =%s", error);
                if (callback != null){
                    callback.onError("");
                }
            }
        });
    }
}