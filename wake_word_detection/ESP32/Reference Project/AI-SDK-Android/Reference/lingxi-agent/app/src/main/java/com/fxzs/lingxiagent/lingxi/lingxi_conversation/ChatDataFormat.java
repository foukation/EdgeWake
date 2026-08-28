package com.fxzs.lingxiagent.lingxi.lingxi_conversation;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.cmdc.ai.assist.constraint.DialogueResult;
import com.example.device_control.AgentResult;
import com.example.device_control.SchedulerManagerFactory;
import com.fxzs.lingxiagent.BaseSplashActivity;
import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.helper.AppListHelper;
import com.fxzs.lingxiagent.lingxi.config.ChatFlowCallback;
import com.fxzs.lingxiagent.lingxi.main.actions.HandlerLlm;
import com.fxzs.lingxiagent.lingxi.main.utils.CustomToast;
import com.fxzs.lingxiagent.lingxi.main.utils.GsonUtils;
import com.fxzs.lingxiagent.lingxi.service_api.data.MusicData;
import com.fxzs.lingxiagent.lingxi.service_api.data.WeatherContent;
import com.fxzs.lingxiagent.lingxi.service_api.data.WeatherIndexes;
import com.fxzs.lingxiagent.model.chat.callback.DeepResearchCallback;
import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.deepresearch.dto.DeepResearchBean;
import com.fxzs.lingxiagent.model.deepresearch.dto.DeepResearchItem;
import com.fxzs.lingxiagent.model.deepresearch.dto.Step;
import com.fxzs.lingxiagent.model.deepresearch.dto.TripDeepResearchRes;
import com.fxzs.lingxiagent.model.deepresearch.dto.WebSearch;
import com.fxzs.lingxiagent.model.deepresearch.repository.DeepResearchImpl;
import com.fxzs.lingxiagent.model.deepresearch.repository.DeepResearchStreamHandler;
import com.fxzs.lingxiagent.model.honor.dto.BodyData;
import com.fxzs.lingxiagent.model.honor.dto.CardData;
import com.fxzs.lingxiagent.model.honor.dto.CommandsData;
import com.fxzs.lingxiagent.model.honor.dto.MessageData;
import com.fxzs.lingxiagent.model.honor.dto.MessageRole;
import com.fxzs.lingxiagent.model.honor.dto.TripHonorRes;
import com.fxzs.lingxiagent.model.honor.repository.BenefitCode;
import com.fxzs.lingxiagent.model.honor.repository.HonorRepositoryImpl;
import com.fxzs.lingxiagent.model.honor.repository.StreamHandler;
import com.fxzs.lingxiagent.model.scene.dto.SceneResponse;
import com.fxzs.lingxiagent.model.scene.repository.SceneRepositoryImpl;
import com.fxzs.lingxiagent.model.scene.repository.SceneStreamHandler;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.BillDialogHelper;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.TimberUtils;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.util.audio.MediaPlayerUtils;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.viewmodel.chat.VMChat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tencent.cos.xml.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class ChatDataFormat {
    private static final String TAG = "ChatDataFormat";
    private final WeakReference<Activity> activityRef;
    public LocalModule curModule = null;
    public LocalModule mainCurModule = null;
    Gson gson = new GsonBuilder().create();
    //private DialogueResult result = null;
    private SchedulerManagerFactory schedulerManagerFactory = null;
    private String asrResult;
    private long conversationId;
    private VMChat vmChat;
    private DeepResearchBean deepResearchBean;
    private DeepResearchBean retryDeepResearchBean;
    int report_count = 0;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;
    private HonorRepositoryImpl honorHttp;
    private SceneRepositoryImpl sceneHttp;
    boolean isBreak;
    private boolean isRetry = false;
    private boolean isNewData = false;
    private int TaskStatus = 0;
    private String preChatContent = "";
    private volatile HonorQueueManager honorQueueManager = null;

    private MusicData musicData = null;
    public ChatDataFormat(Activity activityRef) {
        this.activityRef = new WeakReference<>(activityRef);
    }

    public ChatDataFormat(Activity activityRef, HonorRepositoryImpl honorHttp) {
        this.activityRef = new WeakReference<>(activityRef);
        this.honorHttp = honorHttp;
        this.isBreak = false;
    }

    public void setVmChat(VMChat vmChat) {
        this.vmChat = vmChat;
    }

	public void init(LocalModule mainCurModule, String asrResult) {
        TTSManager.Companion.getInstance().stop();
        this.curModule = null;
        this.isBreak = false;
	    this.asrResult = asrResult;
        this.mainCurModule = mainCurModule;
        this.honorQueueManager = null;
        this.preChatContent = "";
    }

    public void initHonor(long conversationId, long modelId, List<ChatFileBean> fileAnalyseUrl) {
        this.conversationId = conversationId;
    }

    /**
     * 启动对话流
     * 根据对话结果和当前模块状态，执行相应的操作或关闭对话流
     *
     * @param result 对话结果对象，包含对话的相关信息和状态
     */
    public void startFlow(HttpRequest request, DialogueResult result, ChatFlowCallback callback) {
        TimberUtils.logLong(TAG, String.valueOf(result));
        int isEnd = result.is_end();

       // this.result = result;

        if (isBreak) {
            stopFlow(callback);
            return;
        }

        if (abnormalProcess(callback,result)) return;

        // 提前获取 speak data，确保后续 distribute 不会因为缺少数据而出错
        getSpeakData(result);

        LocalModule module = intentDistribute(result);
        // 明确设定 curModule，在一开始就完成赋值
        if (mainCurModule != null && mainCurModule != LocalModule.CHAT) {
            curModule = mainCurModule;
        } else if (module != null) {
            curModule = module;
        }

        // 单例模式安全地初始化 HonorQueueManager
        if (honorQueueManager == null) {
            synchronized (this) {
                if (honorQueueManager == null) {
                    honorQueueManager = new HonorQueueManager(activityRef.get(), callback);
                    HonorQueueManager.executeTypingQueueSafely(0);
                }
            }
        }
        if(module != null){
            switch (module) {
                case TRIP:
                    execHonor(Constants.HONOR_MEET, asrResult, callback);
                    return;
                case TRAVEL:
                    execHonor(Constants.HONOR_TRIP, asrResult, callback);
                    return;
                case GATHERING:
                    return;
                case SYS_CONTROL:
                    execScheduler();
                    HonorQueueManager.executeEndQueueSafely(callback::end);
                    return;
                case MEDIA://江苏移动需求
//                mainCurModule = curModule;
//                execScene(asrResult, callback);
                    execAction(callback);
                    HonorQueueManager.executeEndQueueSafely(callback::end);
                    return;
                case ACTION:
                    execAction(callback);
                    HonorQueueManager.executeEndQueueSafely(callback::end);
                    return;
                case MUSIC: {//江苏移动需求
//                String answer = activityRef.get() != null ?
//                        activityRef.get().getString(R.string.exec_sys_control_default) : "";
//                honorQueueManager.enqueueTypingTask(HonorDataType.RICH_TEXT, answer, 0);
//                HonorQueueManager.executeEndQueueSafely(callback::end);
                    execAction(callback);
                    HonorQueueManager.executeEndQueueSafely(callback::end);
                    return;
                }
                case ESHOP: {//订外卖
                    execAction(callback);
                    HonorQueueManager.executeEndQueueSafely(callback::end);
                    return;
                }
                case GUI: {//除了出行规划其他走GUI
                    execAction(callback);
                    HonorQueueManager.executeEndQueueSafely(callback::end);
                    return;
                }

            }
        }
        switch (curModule) {
                case IMG: {
                    // String percent = getPercentData();
                    ArrayList<String> imageList = getImgData(result);
//                if (!percent.isEmpty()) {
//                    honorQueueManager.enqueueTypingTask(HonorDataType.RICH_TEXT, percent, 0);
//                }
                    if (imageList != null && !imageList.isEmpty()) {
                        callback.receiveImages(imageList);
                    }
                    break;
                }
                case UNCLEAR: {
                    String lastAnswer = result.getAssistant_answer_content();
                    if (lastAnswer != null && !lastAnswer.isEmpty()) {
                        honorQueueManager.enqueueTypingTask(
                                HonorDataType.RICH_TEXT,
                                lastAnswer,
                                Math.max(preChatContent.length(), 1)
                        );
                        preChatContent = lastAnswer;
                    }
                    break;
                }
                case WEATHER: {
                    WeatherContent weatherData = getWeatherData(result);
                    if (weatherData != null) {
                        String answer = weatherData.getDescription();
                        String answerData = buildLifeTips(answer, weatherData.getWeatherForecast().get(0).getIndexes());
                        honorQueueManager.enqueueTypingTask(HonorDataType.RICH_TEXT, answerData, 0);
                        isBreak = true;
                    } else {
                        String lastAnswer = result.getAssistant_answer_content();
                        honorQueueManager.enqueueTypingTask(HonorDataType.RICH_TEXT, lastAnswer, 0);
                    }
                    break;
                }
                case CHAT: {
                    String answer = getAnswerData(result);
                    if (!answer.isEmpty()) {
                        honorQueueManager.enqueueTypingTask(
                                HonorDataType.RICH_TEXT,
                                answer,
                                Math.max(preChatContent.length(), 1)
                        );
                        preChatContent = answer;
                    }
                    break;
                }
                case UNICASTPLAY: {//讲故事
                    if (isEnd == 0) {
                        musicData = addMusicView(result);
                    }
                    if (isEnd == 1) {
                        if (musicData != null && musicData.isGetPlay() && result.getAssistant_answer_content() != null) {
                            musicData.setName(result.getAssistant_answer_content());
                            musicData.setPlay(Boolean.TRUE.equals(vmChat.getIsAutoPlay().getValue()));
                            callback.receiveMusic(musicData);
                        } else {
                            String lastAnswer = result.getAssistant_answer_content();
                            honorQueueManager.enqueueTypingTask(HonorDataType.RICH_TEXT, lastAnswer, 0);
                        }
                    }
                    break;
                }
                case TRANSLATE: {
                    String answer = getTranslationResponse(result);
                    if (!answer.isEmpty()) {
                        honorQueueManager.enqueueTypingTask(
                                HonorDataType.RICH_TEXT,
                                answer,
                                Math.max(preChatContent.length(), 1)
                        );
                        preChatContent = answer;
                    }
                    break;
                }
            }
        // 如果对话结束，更新状态并执行兜底操作
        if (isEnd == 1) {
            stopFlow(callback);
        }
    }

    /**
     * 主模块是智能体，直接走荣耀
     */
    public void startFlow(HttpRequest request, String asrResult, ChatFlowCallback callback) {

        if (honorQueueManager == null) {
            honorQueueManager = new HonorQueueManager(activityRef.get(), callback);
            HonorQueueManager.executeTypingQueueSafely(0);
        }

        // 根据当前模块执行相应的操作,同城聚会
        if (mainCurModule == LocalModule.TRIP) {
            execHonor(Constants.HONOR_MEET, asrResult, callback);
	        return;
        }

        // 出现规划
        if (mainCurModule == LocalModule.TRAVEL) {
            execHonor(Constants.HONOR_TRIP, asrResult, callback);
	        return;
        }

	    // GUI
	    if (mainCurModule == LocalModule.ACTION) {
		    execAction(callback);
		    HonorQueueManager.executeEndQueueSafely(callback::end);
		    return;
	    }

        // 咪咕视频、金融助手、通信助手
        if (mainCurModule == LocalModule.MGVIDOE || mainCurModule == LocalModule.FINANCE || mainCurModule == LocalModule.COMMUNICATION || mainCurModule == LocalModule.MGVIDOE ) {
            execScene(asrResult, callback);
        }
    }

    public Disposable startAgentFlow(HttpRequest request, String query, Boolean isHistory, DeepResearchCallback callback) {
	    return execDeepResearch(isHistory, query, request, callback);
    }

    private boolean abnormalProcess(ChatFlowCallback callback,DialogueResult result) {
        int isEnd = result.is_end();
        JSONObject header = result.getHeader();
        String name = "";
        if (header != null)
            name = header.optString("name");
        if ((!TextUtils.isEmpty(name)) && name.equals(NameType.RENDERCARD.getAlias()))
            curModule = LocalModule.ABNORMAL_PROCESS;
        if (curModule == LocalModule.ABNORMAL_PROCESS && (!TextUtils.isEmpty(name)) && name.equals(NameType.RENDERCARD.getAlias())) {
            JSONObject payload = result.getPayload();
            callback.receiveChat(payload != null ? payload.optString("content") : null);
            return true;
        }
        if (curModule == LocalModule.ABNORMAL_PROCESS && (!TextUtils.isEmpty(name)) && name.equals(NameType.SPEAK.getAlias())) {
            // speak todo
            return true;
        }
        if (curModule == LocalModule.ABNORMAL_PROCESS && isEnd == 1) {
            callback.end();
            return true;
        }
        return false;
    }

    public void stopFlow(ChatFlowCallback callback) {
        if (honorQueueManager != null) {
            HonorQueueManager.executeEndQueueSafely(()-> {
                callback.end();
                honorQueueManager = null;
                preChatContent = "";
            });
        } else {
            callback.end();
        }
    }

    /**
     * 分发意图处理
     * 该方法根据意图(domain和intent)将处理流程分发到不同的本地模块
     * 主要解析结果中的header和payload，提取name和nlu信息，根据domain和intent决定下一步的处理模块
     *
     * @return LocalModule 根据不同的意图返回对应的本地模块处理类，如果没有匹配的模块则返回null
     */
    private LocalModule intentDistribute(DialogueResult result) {
        try {
            // 获取结果的头部信息
            JSONObject header = result.getHeader();
            if (header == null) {
                return null;
            }
            // 获取头部中的name字段
            String name = header.optString("name");
            // 检查name是否匹配自然语言理解(NLU)的别名
            if (name.equals(NameType.NLU.getAlias())) {
                // 获取结果的负载信息
                JSONObject payload = result.getPayload();
                assert payload != null;
                // 获取负载中的nlu数组
                JSONArray nlu = payload.optJSONArray("nlu");
                if (nlu != null) {
                    // 获取nlu数组中的第一个元素
                    JSONObject nluFir = (JSONObject) nlu.get(0);
                    // 提取domain和intent字段
                    String domain = nluFir.optString("domain");
                    String intent = nluFir.optString("intent");
                    asrResult = nluFir.optString("rewrite");

                    // 根据domain和intent的值决定返回的本地模块
                    if (domain.equals(IntentDomain.CHAT.getAlias())) {
                        // 处理聊天相关的意图
                        if (intent.equals(ChatIntent.TRANSLATION.getAlias())) {
                            return LocalModule.TRANSLATE;
                        } else if (intent.equals(ChatIntent.LLMQA.getAlias())) {
                            return LocalModule.CHAT;
                        } else if (intent.equals(ChatIntent.WEATHER.getAlias())) {
                            return LocalModule.WEATHER;
                        } else if (intent.equals(ChatIntent.BAIDU_BAIKE.getAlias())) {
                            return LocalModule.CHAT;
                        }
                        if (intent.equals(ChatIntent.CALC.getAlias())) {
                            return LocalModule.UNCLEAR;
                        } else {
                            return LocalModule.CHAT;
                        }
                    } else if (domain.equals(IntentDomain.AIGC.getAlias())) {
                        // 处理AI生成内容相关的意图
                        if (intent.equals(ImgIntent.AIGC_DRAW.getAlias())) {
                            return LocalModule.IMG;
                        } else {
                            return LocalModule.CHAT;
                        }
                    } else if (domain.equals(IntentDomain.MEDIA.getAlias())) {
                        // 处理媒体相关的意图
                        if (intent.equals(MediaIntent.MEDIA_VIDEOPLY.getAlias()) ) {
                            if (schedulerManagerFactory == null) {
                                schedulerManagerFactory = new SchedulerManagerFactory(activityRef.get());
                                schedulerManagerFactory.setAppList(Objects.requireNonNull(GsonUtils.toJson(AppListHelper.INSTANCE.getAppInfoList())));
                            }
                            schedulerManagerFactory.updateIntent(nlu.toString(), domain);
                            return LocalModule.MEDIA;
                        } else if (intent.equals(MediaIntent.MEDIA_MUSIC.getAlias())) {
                            return LocalModule.MUSIC;
                        } else if (intent.equals(MediaIntent.MEDIA_UNICAST.getAlias())){//讲故事
                            return LocalModule.UNICASTPLAY;
                        } else {
                            return LocalModule.CHAT;
                        }
                    } else if (
                            domain.equals(IntentDomain.SYSTEM_CONTROL.getAlias()) |
                                    domain.equals(IntentDomain.PHONE.getAlias()) |
                                    domain.equals(IntentDomain.CAR_CONTROL.getAlias()) |
                                    domain.equals(IntentDomain.ALARM.getAlias()) |
                                    domain.equals(IntentDomain.TELECOMSERVICE.getAlias()) |
                                    domain.equals(IntentDomain.HEALTHCARE.getAlias()) |
                                    domain.equals(IntentDomain.CUSTOMERSERVICE.getAlias()) |
                                    domain.equals(IntentDomain.MEMBERSHIP.getAlias())) {
                        // 处理系统控制、电话和汽车控制相关的意图
                        if (schedulerManagerFactory == null) {
                            schedulerManagerFactory = new SchedulerManagerFactory(activityRef.get());
                            schedulerManagerFactory.setAppList(Objects.requireNonNull(GsonUtils.toJson(AppListHelper.INSTANCE.getAppInfoList())));
                        }
                        schedulerManagerFactory.updateIntent(nlu.toString(), domain);
                        return LocalModule.SYS_CONTROL;
                    } else if (domain.equals(IntentDomain.NAVIGATION.getAlias())) {
                        // 处理导航相关的意图
                        if (intent.equals(NavIntent.NAV_AIGuide.getAlias())) {
                            return LocalModule.TRAVEL;
                        } else if (intent.equals(NavIntent.NAV_POI.getAlias())) {
                            return LocalModule.TRIP;
                        } else if (intent.equals(NavIntent.NAV_NAV.getAlias())) {
                            return LocalModule.ACTION;
                        } else {
                            return LocalModule.CHAT;
                        }
                    } else if (domain.equals(IntentDomain.ALARM.getAlias())) {
                        // 处理闹钟相关的意图
                        return LocalModule.ACTION;
                    } else if (domain.equals(IntentDomain.DRINK.getAlias())) {
                        // 处理饮料相关的意图
                        return LocalModule.CHAT;
                    } else if (domain.equals(IntentDomain.UNCLEAR.getAlias())) {
                        // 处理不明确的意图
                        return LocalModule.UNCLEAR;
                    } else if (domain.equals(IntentDomain.TRAVEL.getAlias())) {
                        if (intent.equals(Travel.Travel_PlanTravel.getAlias())){
                            // 处理出行意图
                            return LocalModule.TRAVEL;
                        }
                        return LocalModule.GUI;

                    } else if (domain.equals(IntentDomain.ESHOP.getAlias())){
                        return LocalModule.ESHOP;
                    }
                    else {
                        // 默认处理聊天意图
                        return LocalModule.CHAT;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public void execAction(ChatFlowCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HandlerLlm.INSTANCE.start(activityRef.get(), asrResult, callback, honorQueueManager);
        }
    }

    public void execScheduler() {
            String resultStr;
            AgentResult agentResult = schedulerManagerFactory.start();
            if (agentResult.getResult()) {
                resultStr = !Objects.equals(agentResult.getSucMsg(), "") ? agentResult.getSucMsg() : "指令执行成功";
            } else {
                resultStr = !Objects.equals(agentResult.getErrMsg(), "") ? agentResult.getErrMsg() : "暂时不支持相关操作";
            }
            if (!TextUtils.isEmpty(resultStr)) {
                honorQueueManager.enqueueTypingTask(HonorDataType.RICH_TEXT, resultStr, 0);
            }
    }

    public void execHonor(String apiUrl, String result, ChatFlowCallback callback) {
        if (ActivityCompat.checkSelfPermission(activityRef.get(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && !SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_LOCATION, false)) {
            honorQueueManager.enqueueTypingTask(HonorDataType.RICH_TEXT, "请先授权位置权限，方便为你提供更精准的规划服务", 0);
            if(callback != null){
                callback.end();
            }
            AppPermissionRequestManager.requestLocationPermission(activityRef.get(), MainActivity.REQUEST_LOCATION_PERMISSION);
            return;
        }
        honorHttp.updateRequestInfo(apiUrl, conversationId);
        StringBuilder totalCotText = new StringBuilder();
        StringBuilder totalText = new StringBuilder();

        honorHttp.sendStreamRequest(result, new StreamHandler() {
            @Override
            public void onStreamStop() {
            }

            @Override
            public void onDataChunk(@NonNull TripHonorRes resp) {
                if (resp.getErrorCode().equals("0")) {
                    CommandsData commands = resp.getChoices().getMessage().getHybridContent().getCommands();
                    handleHonorData(commands, totalCotText, callback, totalText, honorQueueManager);
                } else if (BenefitCode.isBenefitError(resp.getErrorCode())) {
                    BillDialogHelper.showBillDialog(activityRef.get(), resp.getErrorMessage(), () -> activityRef.get().finish());
                    honorQueueManager.enqueueTypingTask(HonorDataType.RICH_TEXT, resp.getErrorMessage(), 0);
                }
            }

            @Override
            public void onDataChunk(MessageData resp) {
                CommandsData commands = resp.getHybridContent().getCommands();
                handleHonorData(commands, totalCotText, callback, totalText, honorQueueManager);
            }

            @Override
            public void onStreamComplete() {
                honorHttp.updateMessages(MessageRole.ASSISTANT.getAlias(), String.valueOf(totalText), "text");
                HonorQueueManager.executeEndQueueSafely(callback::end);
            }

            @Override
            public void onError(@NonNull String errMsg) {

                HonorQueueManager.executeEndQueueSafely(callback::end);
            }
        });
    }

    private String think;
    private String content;

    public void execScene(String result, ChatFlowCallback callback) {
        think = "";
        content = "";
        sceneHttp = new SceneRepositoryImpl(activityRef.get());
        sceneHttp.updateRequestInfo(mainCurModule, conversationId);
        sceneHttp.sendStreamRequest(result, new SceneStreamHandler() {
            @Override
            public void onStreamStop() {
                callback.end();
            }

            @Override
            public void onDataChunk(@NonNull SceneResponse resp) {
                if (BenefitCode.isBenefitError(resp.getErrorCode())) {
                    BillDialogHelper.showBillDialog(activityRef.get(), resp.getErrorMessage(), () -> activityRef.get().finish());
                    callback.receiveChat(resp.getErrorMessage());
                } else {
                    if (resp.getChoices() != null && !resp.getChoices().isEmpty()) {
                        if (resp.getChoices().get(0).getMessageData().getThinkData() != null) {
                            think += resp.getChoices().get(0).getMessageData().getThinkData().getThinkText();
                            callback.receiveCot(think);
                        }
                        if (resp.getChoices().get(0).getMessageData().getContentData() != null) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (TextUtils.equals(resp.getChoices().get(0).getMessageData().getContentData().getType(), "3")) {
                                    callback.receiveH5Card(resp.getChoices().get(0).getMessageData().getContentData().getDeltaContentData().getTemplateId(),
                                            GsonUtils.toJson(resp.getChoices().get(0).getMessageData().getContentData().getDeltaContentData()));
                                } else if (TextUtils.equals(resp.getChoices().get(0).getMessageData().getContentData().getType(), "2")) {
                                    callback.receiveH5Card(null,
                                            GsonUtils.toJson(resp.getChoices().get(0).getMessageData().getContentData().getDeltaContentData()));
                                }
                            }, 1500);
                            if (!TextUtils.isEmpty(resp.getChoices().get(0).getMessageData().getContentData().getDeltaContentData().getTextValue())) {
                                Timber.tag(TAG).d( " content String = " + resp.getChoices().get(0).getMessageData().getContentData().getDeltaContentData().getTextValue());
                                content += resp.getChoices().get(0).getMessageData().getContentData().getDeltaContentData().getTextValue();
                            } else if (resp.getChoices().get(0).getMessageData().getContentData().getDeltaContentData().getDeltaContentBean() != null) {
                                content += resp.getChoices().get(0).getMessageData().getContentData().getDeltaContentData().getDeltaContentBean().getContentText();
                            }
                            callback.receiveChat(content);
                        }
                        Timber.tag(TAG).d( "think " + think + " content = " + content);
                    }
                }
            }

            @Override
            public void onStreamComplete() {
                callback.end();
            }

            @Override
            public void onError(@NonNull String errMsg) {
                callback.end();
            }
        });
    }

    public Disposable execDeepResearch(Boolean isHistory, String query, HttpRequest request, DeepResearchCallback callback) {
        String req_id;
        if (isHistory)  {
            req_id = SharedPreferencesUtil.getString(Constants.DEEPRESEARCH_REQ_ID, "");
            if(StringUtils.isEmpty(req_id)){
                req_id = String.valueOf(System.currentTimeMillis() + (System.nanoTime() % 1_000_000));
            }
        } else {
            req_id = String.valueOf(System.currentTimeMillis() + (System.nanoTime() % 1_000_000));
        }
        Timber.tag(TAG).d("execDeepResearch isHistory = %s query = %s req_id = %s", isHistory, query, req_id);
        DeepResearchImpl deepResearchHttp = new DeepResearchImpl(activityRef.get());
        StringBuilder totalCotText = new StringBuilder();
        StringBuilder totalText = new StringBuilder();
        StringBuilder reportText = new StringBuilder();
        deepResearchBean = new DeepResearchBean();
        createBean(query, req_id,deepResearchBean);
        SharedPreferencesUtil.saveString(Constants.DEEPRESEARCH_REQ_ID, req_id);
        SharedPreferencesUtil.saveString(Constants.DEEPRESEARCH_QUERY, query);

        isRetry = false;
        retryCount = 0;
        isNewData = false;
        TaskStatus = 0;
        String finalReq_id = req_id;
        return deepResearchHttp.sendStreamRequest(query, req_id, new DeepResearchStreamHandler() {
            @Override
            public void onStreamStop() {

            }

            @Override
            public void onDataChunk(@NonNull TripDeepResearchRes resp) {
                if (BenefitCode.isBenefitError(resp.getErrorCode())) {
                    BillDialogHelper.showBillDialog(activityRef.get(), resp.getErrorMessage(), () -> activityRef.get().finish());
                    this.onStreamComplete();
                } else {
                    if (isRetry) {
                        if(!isNewData){
                            processData(query, totalCotText, reportText, resp, retryDeepResearchBean);
                            Timber.tag(TAG).d("retryDeepResearchBean : "+retryDeepResearchBean.getList().size() + " deepResearchBean : " + deepResearchBean.getList().size());
                            if(retryDeepResearchBean.getList().size() >= deepResearchBean.getList().size()){
                                isNewData = true;
                            }
                            return;
                        }
                        handleDeepResearchData(query, totalCotText, reportText, callback, totalText, resp, retryDeepResearchBean);
                    } else {
                        handleDeepResearchData(query, totalCotText, reportText, callback, totalText, resp, deepResearchBean);
                    }
                }
            }

            @Override
            public void onDataChunk(MessageData resp) {

            }

            @Override
            public void onStreamComplete() {
                Timber.tag(TAG).d("onStreamComplete ");
                callback.onDeepResearchComplete();
                isRetry = false;
                retryCount = 0;
            }

            @Override
            public void onError(@NonNull String errMsg) {
                isRetry = true;
                Timber.tag(TAG).d("execDeepResearch onError: %s, retryCount: %d", errMsg, retryCount);
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    if(retryCount == 1){
                        // 创建新的数据对象
                        retryDeepResearchBean = new DeepResearchBean();
                        createBean(query, finalReq_id, retryDeepResearchBean);
                        if (TaskStatus == 1) {
                            deepResearchBean.setTaskStatus(2);
                        } else if (TaskStatus == 3) {
                            deepResearchBean.setTaskStatus(TaskStatus);
                        }
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Timber.tag(TAG).d("Retrying deep research... attempt %d", retryCount);
                        // 然后继续新的请求
                        Disposable sseDisposable = deepResearchHttp.sendStreamRequest(query, finalReq_id, this);
                        if (vmChat != null) {  // 假设vmChat是VMChat实例变量
                            vmChat.setDisposable(sseDisposable);
                        } else {
                            Timber.tag(TAG).e("VMChat instance is null when setting sseDisposable");
                            // 可选：处理错误情况，如显示错误信息给用户
                        }
                    }, RETRY_DELAY_MS);
                } else {
                    Timber.tag(TAG).e("Max retries reached for deep research");
                    callback.onDeepResearchError(errMsg);
                }
            }
        });
    }

    private DeepResearchBean createBean(String query, String req_id, DeepResearchBean deepResearchBean) {
        deepResearchBean.setTaskStatus(1);
        deepResearchBean.setQuery(query);
        deepResearchBean.setReq_id(req_id);
        deepResearchBean.setStep(Step.THINKING.getAlias());
        deepResearchBean.setModel("深度研究");
        DeepResearchItem item = new DeepResearchItem();
        WebSearch webSearch = new WebSearch();
        webSearch.setQuery("正在思考中");
        item.setWeb_search(webSearch);
        item.setAnimStatus(0);
        item.setStatus(0);
        List<DeepResearchItem> list = new ArrayList<>();
        list.add(item);
        deepResearchBean.setList(list);
        return deepResearchBean;
    }

    private void handleDeepResearchData(String query, StringBuilder totalCotText, StringBuilder reportText, DeepResearchCallback callback, StringBuilder totalText, TripDeepResearchRes resp, DeepResearchBean deepResearch) {
        processData(query, totalCotText, reportText, resp, deepResearch);

        if (report_count <= 1) {
            callback.onDeepResearch(deepResearch);
        }
    }

    private DeepResearchBean processData(String query, StringBuilder totalCotText, StringBuilder reportText, TripDeepResearchRes resp, DeepResearchBean deepResearch) {
        //think
        if (resp.getThink() != null) {
            deepResearch.getList().get(deepResearch.getList().size() - 1).setStatus(0);
            deepResearch.setStep(Step.THINKING.getAlias());
            totalCotText.append(resp.getThink());
            deepResearch.getList().get(deepResearch.getList().size() - 1).setThink(resp.getThink());
            deepResearch.getList().get(deepResearch.getList().size() - 1).setThinkContent(totalCotText.toString());
        } else if (resp.getWeb_search() != null) {//web_search
            deepResearch.getList().get(0).getWeb_search().setQuery("思考已完成");
            deepResearch.getList().get(deepResearch.getList().size() - 1).setStatus(1);
            totalCotText.setLength(0);
            DeepResearchItem item = new DeepResearchItem();
            item.setAnimStatus(0);
            deepResearch.getList().add(item);
            deepResearch.setStep(Step.WEB_SEARCH.getAlias());
            deepResearch.getList().get(deepResearch.getList().size() - 1).setWeb_search(resp.getWeb_search());
            if (null != resp.getWeb_search()) {
                Timber.tag(TAG).d("web_search query = %s size= %s", deepResearch.getList().get(deepResearch.getList().size() - 1).getWeb_search().getQuery(), deepResearch.getList().get(deepResearch.getList().size() - 1).getWeb_search().getWeb_search().size());
                Timber.tag(TAG).d("web_search content = %s", deepResearch.getList().get(deepResearch.getList().size() - 2).getThinkContent());

            }
        } else if (resp.getReport() != null) {//report

            report_count++;
            deepResearch.setReport_count(report_count);
//			if(resp.getReport().contains("the report is complete.")){
            if (resp.getReport().contains("http://www.baidu.com")) {

//				Timber.tag(TAG).d("report complete deepResearchBean = " + result);
                deepResearch.setStep(Step.REPORT_COMPLETE.getAlias());
                deepResearch.setTaskStatus(4);
                deepResearch.getList().get(deepResearch.getList().size() - 1).setStatus(3);
                String result = GsonUtils.toJson(deepResearch.getList().get(deepResearch.getList().size() - 1));
                Timber.tag(TAG).d("report complete = " + result);
                reportText.setLength(0);
                report_count = 0;
            } else {
                if (deepResearch.getReport_count() == 1) {
                    deepResearch.getList().get(deepResearch.getList().size() - 1).setStatus(1);
                    DeepResearchItem item = new DeepResearchItem();
                    WebSearch webSearch = new WebSearch();
                    webSearch.setQuery("撰写最终的推荐报告");
                    item.setWeb_search(webSearch);
                    item.setQuery(query);
                    item.setStatus(2);
                    item.setAnimStatus(0);
                    deepResearch.getList().add(item);
                    deepResearch.setTaskStatus(3);
                    TaskStatus = 3;
                }
                deepResearch.setStep(Step.REPORTING.getAlias());
                reportText.append(resp.getReport());
                deepResearch.setReport(resp.getReport());
                deepResearch.setReportContent(reportText.toString());
            }
        }
        return deepResearch;
    }

    private void handleHonorData(CommandsData commands, StringBuilder totalCotText, ChatFlowCallback callback, StringBuilder totalText, HonorQueueManager honorQueueManager) {
        String type = commands.getHead().getNamespace();
        BodyData body = commands.getBody();
        String richText = body.getText();
        switch (type) {
            case "think":
                Timber.d("消息进度onDataChunk THINK %s", richText);
                final int curThinkIndex = totalCotText.length();
                totalCotText.append(richText);
                String thinkContent = String.valueOf(totalCotText);
                honorQueueManager.enqueueTypingTask(HonorDataType.THINK, thinkContent, curThinkIndex);
                break;
            case "rich_text":
                Timber.d("消息进度onDataChunk RICH_TEXT %s", richText);
                if (richText != null) {
                    final int curRichIndex = totalText.length();
                    totalText.append(richText);
                    String richContent = String.valueOf(totalText);
                    honorQueueManager.enqueueTypingTask(HonorDataType.RICH_TEXT, richContent, curRichIndex);
                }
                break;
            case "card":
                Timber.d("消息进度onDataChunk CARD %s", richText);
                if (body.getJsCards() != null && !body.getJsCards().isEmpty()) {
                    for (int i = 0; i < body.getJsCards().size(); i++) {
                        CardData cardData = body.getJsCards().get(i);
                        String serviceId = cardData.getServiceId();
                        String templateId = cardData.getTemplateId();
                        if (templateId == null) {
                            if (!Objects.equals(serviceId, "")) {
                                honorQueueManager.enqueueTypingTask(HonorDataType.CARD, cardData);
                            }
                        }
                    }
                }
                break;
        }
    }

    private ArrayList<String> getImgData(DialogueResult result) {
        try {
            JSONObject header = result.getHeader();
            JSONObject payload = result.getPayload();
            if (header != null) {
                String name = header.optString("name");
                if (payload != null && NameType.IMG_CARD.getAlias().equals(name)) {
                    JSONArray urlsArray = payload.optJSONArray("urls");
                    ArrayList<String> addressList = new ArrayList<>();
                    if (urlsArray != null) {
                        for (int i = 0; i < urlsArray.length(); i++) {
                            addressList.add(urlsArray.optString(i));
                        }
                    }
                    return addressList;
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String buildLifeTips(String lastAnswer, List<WeatherIndexes> indexes) {
        if (indexes == null){
            return lastAnswer;
        }
        Map<String, String[]> typeMap = new HashMap<>();
        typeMap.put("CLOTHES", new String[]{"穿衣", "👕"});
        typeMap.put("CAR_WASH", new String[]{"洗车", "🚗"});
        typeMap.put("TRIP", new String[]{"出行", "🚌"});
        typeMap.put("INFLUENZA", new String[]{"健康", "🛡"});
        typeMap.put("UMBRELLA", new String[]{"雨伞", "🌂"});
        typeMap.put("ULTRAVIOLET", new String[]{"辐射", "☀"});

        String[] circleNumbers = {"①","②","③","④","⑤","⑥","⑦","⑧","⑨","⑩"};

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(lastAnswer).append("\n");
        sb.append("\n#### 🔹 生活提示\n\n");

        int i = 0;
        for (WeatherIndexes item : indexes) {
            String[] info = typeMap.get(item.getType());
            if (info != null) {
                String number = (i < circleNumbers.length) ? circleNumbers[i] : (i+1) + "、";
                sb.append(number).append(" **")
                        .append(info[0]).append("** ").append(info[1]).append("\n   ")
                        .append(item.getSuggestion()).append("\n");
                i++;
            }
        }

        return sb.toString();
    }

    private WeatherContent getWeatherData(DialogueResult result) {
        try {
            JSONObject header = result.getHeader();
            JSONObject payload = result.getPayload();
            if (header != null) {
                String name = header.optString("name");
                if (NameType.RENDER_WEATHER.getAlias().equals(name)) {
	                return gson.fromJson(String.valueOf(payload), WeatherContent.class);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

//    private String getPercentData(DialogueResult result) {
//        try {
//            JSONObject header = result.getHeader();
//            JSONObject payload = result.getPayload();
//            if (header != null) {
//                String name = header.optString("name");
//                if (NameType.PROCESS.getAlias().equals(name)) {
//                    assert payload != null;
//                    return "图片生成中：" + payload.optInt("percent") + "%";
//                }
//            }
//            return "";
//        } catch (Exception e) {
//            return "";
//        }
//    }

    private String getAnswerData(DialogueResult result) {
        try {
            JSONObject header = result.getHeader();
            JSONObject payload = result.getPayload();
            if (header != null) {
                String name = header.optString("name");
                if (NameType.RENDER_FLOW.getAlias().equals(name)) {
                    assert payload != null;
                    return payload.optString("answer");
                }
            }

        } catch (Exception e) {
            return "";
        }
        return "";
    }

    private void getSpeakData(DialogueResult result) {
        JSONObject header = result.getHeader();
        JSONObject payload = result.getPayload();
        if (header == null) return;
        if (payload == null) return;
        String name = header.optString("name");
        if (TextUtils.isEmpty(name)) return;
        if (!NameType.SPEAK.getAlias().equals(name)) return;
        String url = payload.optString("url");
        if (TextUtils.isEmpty(url)) return;
        //vmChat.playTTSUrl(url, result.getQid(), false);
    }

    private String getTranslationResponse(DialogueResult result) {
        JSONObject header = result.getHeader();
        JSONObject payload = result.getPayload();
        if (header == null) return "";
        if (payload == null) return "";
        String name = header.optString("name");
        if (TextUtils.isEmpty(name)) return "";
        if (!NameType.SPEAK.getAlias().equals(name)) return "";
        String content = payload.optString("content");
        if (TextUtils.isEmpty(content)) return "";
        return content;
    }

    // 中断打字机动画
    public void stopTypingTask() {
        if (honorQueueManager != null) {
            HonorQueueManager.stopTypingTask();
        }
    }

    public void setBreakDialog(boolean isBreak) {
        if (honorHttp != null) {
            honorHttp.interruptMessage();
        }

        if (sceneHttp != null) {
            sceneHttp.interruptMessage();
        }
    }

    public MusicData addMusicView(DialogueResult result)  {
        Map<String, String> play = getPlayData(result);
        MusicData musicData = new MusicData("","",false,false);
        if (play != null) {
            String url = play.get("url");
            String albumName = play.get("albumName");
            if (albumName != null && url != null) {
                MediaPlayerUtils.Companion.getInstance().create();
                musicData = new MusicData(albumName,url,true,false);
            }

        }
        return musicData;
    }

    private Map<String, String> getPlayData(DialogueResult result) {
        try {
            JSONObject header = result.getHeader();
            JSONObject payload = result.getPayload();
            if (header != null) {
                String name = header.optString("name");
                Map<String, String> scores = new HashMap<>();
                if (NameType.PLAY.getAlias().equals(name)) {
                    assert payload != null;
                    String url = Objects.requireNonNull(Objects.requireNonNull(payload.optJSONObject("audioItem")).optJSONObject("stream")).optString("url");
                    String albumName = Objects.requireNonNull(payload.optJSONObject("audioItem")).optString("extension");
                    scores.put("url", url);
                    scores.put("albumName", albumName);
                    return scores;
                }
                return null;
            }

        } catch (Exception e) {
            return null;
        }
        return null;
    }
}