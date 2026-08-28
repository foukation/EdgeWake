package com.fxzs.lingxiagent.view.chat;

import static android.app.Activity.RESULT_OK;
import static com.fxzs.lingxiagent.model.common.Constants.AGENT_GUI;
import static com.fxzs.lingxiagent.view.chat.delegate.FloatPermissionCardDelegate.REQ_CODE_ACC_PERMISSION;
import static com.fxzs.lingxiagent.view.chat.delegate.FloatPermissionCardDelegate.REQ_CODE_FLOAT_PERMISSION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.accessibility_api.AccessibilityApi;
import com.fxzs.lingxiagent.lingxi.gui_agent.GuiFunctionHelpActivity;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AsrManager;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.ChatDataFormat;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.HomeModelEntity;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.TabEntity;
import com.fxzs.lingxiagent.lingxi.main.helper.FloatHelper;
import com.fxzs.lingxiagent.lingxi.main.utils.GsonUtils;
import com.fxzs.lingxiagent.lingxi.marquee.MarqueeManager;
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.TrackerUtils;
import com.fxzs.lingxiagent.lingxi.translate.SimultaneousTranslateActivity;
import com.fxzs.lingxiagent.model.auth.AuthHelper;
import com.fxzs.lingxiagent.model.billing.BillingManager;
import com.fxzs.lingxiagent.model.billing.callback.BillingCallback;
import com.fxzs.lingxiagent.model.chat.callback.AIMeetingEditCallback;
import com.fxzs.lingxiagent.model.chat.callback.AITranslateEditCallback;
import com.fxzs.lingxiagent.model.chat.callback.DeepResearchCallback;
import com.fxzs.lingxiagent.model.chat.callback.MsgActionCallback;
import com.fxzs.lingxiagent.model.chat.callback.RequestCallback;
import com.fxzs.lingxiagent.model.chat.callback.SoftCallback;
import com.fxzs.lingxiagent.model.chat.callback.SuperEditCallback;
import com.fxzs.lingxiagent.model.chat.callback.SuperShareCallback;
import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean;
import com.fxzs.lingxiagent.model.chat.dto.ChatFileListJsonBean;
import com.fxzs.lingxiagent.model.chat.dto.ChatFunctionBean;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.model.chat.dto.ConversationDetailDto;
import com.fxzs.lingxiagent.model.chat.dto.DrawingToChatBean;
import com.fxzs.lingxiagent.model.chat.dto.EventBusCardLoadNotifyModel;
import com.fxzs.lingxiagent.model.chat.dto.EventBusSendHistoryNotifyMsg;
import com.fxzs.lingxiagent.model.chat.dto.EventBusShareCancelModel;
import com.fxzs.lingxiagent.model.chat.dto.EventBusShareNotifyModel;
import com.fxzs.lingxiagent.model.chat.dto.OptionModel;
import com.fxzs.lingxiagent.model.chat.dto.ShareItem;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepository;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepositoryImpl;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.deepresearch.dto.DeepResearchBean;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSessionDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingStyleDto;
import com.fxzs.lingxiagent.model.honor.repository.BenefitCode;
import com.fxzs.lingxiagent.model.honor.repository.HonorRepositoryImpl;
import com.fxzs.lingxiagent.network.NetworkMonitor;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.network.ZNet.bean.ChatContent;
import com.fxzs.lingxiagent.network.ZNet.bean.GetMenuBean;
import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean;
import com.fxzs.lingxiagent.receiver.LingxiAskWidgetProvider;
import com.fxzs.lingxiagent.service.MyForegroundService;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.BillDialogHelper;
import com.fxzs.lingxiagent.util.DevicePerformanceConfig;
import com.fxzs.lingxiagent.util.DocumentHelper;
import com.fxzs.lingxiagent.util.GlobalDataHolder;
import com.fxzs.lingxiagent.util.GlobalSettings;
import com.fxzs.lingxiagent.util.NetworkUtils;
import com.fxzs.lingxiagent.util.ShadowUtils;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.SuperEditUtil;
import com.fxzs.lingxiagent.util.ZInputMethod;
import com.fxzs.lingxiagent.util.ZUtil.AsrOneUtils;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.ZUtil.DrawingActionUtils;
import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;
import com.fxzs.lingxiagent.util.ZUtil.SuperAgentUtil;
import com.fxzs.lingxiagent.util.ZUtil.SuperEditAITranslateUtil;
import com.fxzs.lingxiagent.util.ZUtil.SuperEditAIWritingUtil;
import com.fxzs.lingxiagent.util.ZUtil.SuperLongPicUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.agent.AgentContainActivity;
import com.fxzs.lingxiagent.view.auth.OneClickLoginActivity;
import com.fxzs.lingxiagent.view.common.AutoRecordView;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.ExportFileDialog;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.drawing.DrawingActivity;
import com.fxzs.lingxiagent.view.drawing.DrawingImageViewerActivity;
import com.fxzs.lingxiagent.view.drawing.DrawingSelectActivity;
import com.fxzs.lingxiagent.view.meeting.MeetingContainActivity;
import com.fxzs.lingxiagent.view.meeting.MeetingSummaryFragment;
import com.fxzs.lingxiagent.view.ppt.PptTopicInputActivity;
import com.fxzs.lingxiagent.view.user.UserActivity;
import com.fxzs.lingxiagent.view.widget.ChatLinearLayoutManager;
import com.fxzs.lingxiagent.viewmodel.chat.VMChat;
import com.fxzs.lingxiagent.viewmodel.meeting.VMMeetingSummary;
import com.fxzs.lingxiagent.viewmodel.user.VMAccountInfo;
import com.fxzs.lingxiagent.viewmodel.user.VMUserProfile;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lingxi.cardhelper.CardView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class SuperChatFragment extends BaseFragment<VMChat> {
    private static final String TAG = "SuperChatFragment";
    public static final int TYPE_HOME = 1;//首页
    public static final int TYPE_AGENT = 2;//智能体
    public static final int TYPE_DRAWING = 3;//绘画-对话界面
    public static final int TYPE_MEETING = 4;//会议-会议摘要
    public static final int TYPE_MEETING_QA = 5;//会议-智能问答

    public static final int TYPE_WAKE = 6;//语音唤醒

    public static final int DEFAULT_LAYOUT = 0;
    public static final int LONG_PIC_LAYOUT = 1;
    public static final int SAVE_FILE_LAYOUT = 2;
    public static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;
    private static final int REQUEST_CONTINUE_EDIT = 2;
    private static final int REQUEST_DRAWING_VIEW = 3;
    private LinearLayout root_view;
    private LinearLayout ll_bottom;
    private LinearLayout ll_edit_writing;
    private LinearLayout ll_edit_translate;
    private LinearLayout ll_edit_main;
    private LinearLayout ll_edit_agent;
    private LinearLayout mShareBottom;
    private LinearLayout mLongPicLayout;
    private SuperEditUtil superEditUtil;
    private SuperAgentUtil superAgentUtil;
    private SuperEditAIWritingUtil superEditAIWritingUtil;
    private SuperEditAITranslateUtil superEditAITranslateUtil;
    private SuperLongPicUtil mLongPicUtil;
    private EditText ed;
    private RecyclerView rv_chat, rv_function;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ChatFunctionAdapter chatFunctionAdapter;
    private ChatAdapter chatAdapter;
    private TextView aiResponse;
    private View ll_stop;
    private View ll_resend;
    private TextView tv_mode;
    private ImageView iv_scroll_down;
    private ImageView iv_history;
    private ImageView iv_top_play;
    private View ll_header;
    private NestedScrollView sv_chat_list;
    private LinearLayout ll_select;
    private VMChat vmChat;
    VMUserProfile vmUserProfile;
    List<ChatFileBean> fileList;
    private OptionModel selectOptionModel;
    private boolean isUserTouch;//返回流时用户是否操作
    private boolean isSoft;
    private int type;//跳转类型
    private boolean mIsUserScrolling = false;
    private ChatMessage currentViewChatMessage;
    private VMMeetingSummary vmMeetingSummary;
    Map<String, Object> meetingMap;

    private AutoRecordView voiceRecordView;
    private List<ChatMessage> mSelectMessages = new ArrayList<>();
    private RecyclerView mShareItemList;
    private ShareItemtAdapter mShareItemAdapter;
    private List<ShareItem> mShareDatas = new ArrayList<>();


    private String curAsrResult = "";
    private final int PRESS_DOWN = 1;
    private final int PRESS_UP = 2;
    private final int PRESS_MOVE = 3;
    private TextView tvHeaderSelectAgent, tvHeaderTitle;
    private ImageView ivHead, ivCreateChat, ivHeaderSelectAgent;
    private LinearLayout mSelectTitleLayout;
    private LinearLayout  mLayoutGuiGuide;
    private TabEntity.TabType agentType;//判断底部输入框类型
    // 用于标记是否已触发底部监听，避免重复触发
    private boolean isScrolledToBottom = false;
    // 用于标记是否是新对话状态，防止滚动监听器错误显示置底按钮

    private CardView cardView;

    private VMAccountInfo vmAccountInfo;

    private boolean isNewConversation = true;

    /**
     * 重置新对话状态标志
     */
    public void resetNewConversationFlag() {
        isNewConversation = false;
    }

    private Disposable sseDisposable;

    // 分页加载相关字段
    private ViewTreeObserver.OnScrollChangedListener scrollChangedListener;
    private boolean isLoadingMore = false;
    private long lastLoadTime = 0;
    private static final long LOAD_DEBOUNCE_MS = 500; // 防抖间隔（毫秒）
    private TextView tv_load_more_hint; // 加载更多提示文本
    // 分页加载 Observer 引用（只注册一次，避免累积泄漏）
    // 使用全限定名避免与 io.reactivex.Observer 冲突
    private androidx.lifecycle.Observer<List<ChatMessage>> loadMoreObserver;
    // 分页加载时记录的滚动位置（供 Observer 回调使用）
    private int scrollYBeforeLoad = 0;
    private int childHeightBeforeLoad = 0;

    // 首次加载标记，用于优化初始渲染，避免闪屏
    private boolean isFirstLoad = true;

    private long id;

    // 必须的无参构造，供系统在进程重建/状态恢复时反射实例化
    public SuperChatFragment() {
    }

    private AsrManager asrManager;
    private HonorRepositoryImpl honorHttp;
    private TextView tv_empty_hi;
    private View ll_empty;
    private boolean hasPositionedEmptyView = false;

    // 保存 observeForever 观察者引用，用于 onDestroyView 时移除，防止内存泄漏
    private androidx.lifecycle.Observer<String> summaryStreamObserver;
    private androidx.lifecycle.Observer<Integer> summaryProgressObserver;

    private NetworkMonitor networkMonitor;
    private Timer autoLoadTimer;
    private boolean isMoveInArea;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    //历史进入
    public SuperChatFragment(int type, String input, long id, OptionModel optionModel) {
        Bundle args = new Bundle();
        args.putInt(Constant.INTENT_TYPE, type);
        args.putLong(Constant.INTENT_ID, id);
        args.putString(Constant.INTENT_DATA, input);
        args.putSerializable(Constant.INTENT_DATA1, optionModel);
        setArguments(args);
    }

    //首页
    public SuperChatFragment(int type, String input, OptionModel optionModel, List<ChatFileBean> list) {
        Bundle args = new Bundle();
        args.putInt(Constant.INTENT_TYPE, type);
        args.putString(Constant.INTENT_DATA, input);
        args.putSerializable(Constant.INTENT_DATA1, optionModel);
        args.putSerializable(Constant.INTENT_DATA2, (Serializable) list);
        setArguments(args);
    }

    //首页
    public SuperChatFragment(int type) {
        Bundle args = new Bundle();
        args.putInt(Constant.INTENT_TYPE, type);
        setArguments(args);
    }

    //智能体
    public SuperChatFragment(int type, long id, getCatDetailListBean bean) {
        Bundle args = new Bundle();
        args.putInt(Constant.INTENT_TYPE, type);
        args.putLong(Constant.INTENT_ID, id);
        args.putSerializable(Constant.INTENT_DATA2, (Serializable) bean);
        setArguments(args);
        honorHttp = new HonorRepositoryImpl(getContext());
    }

    //智能体
    public SuperChatFragment(int type, long id, getCatDetailListBean bean, String query) {
        Bundle args = new Bundle();
        args.putInt(Constant.INTENT_TYPE, type);
        args.putLong(Constant.INTENT_ID, id);
        args.putSerializable(Constant.INTENT_DATA2, (Serializable) bean);
        args.putString(Constant.INTENT_DATA_GUI_QUERY, query);
        setArguments(args);
        honorHttp = new HonorRepositoryImpl(getContext());
    }

    //绘画
    public SuperChatFragment(int type, DrawingToChatBean drawingToChatBean, DrawingStyleDto drawingStyleDto) {
        Bundle args = new Bundle();
        args.putInt(Constant.INTENT_TYPE, type);
        args.putSerializable(Constant.INTENT_DATA, drawingToChatBean);
        args.putSerializable(Constant.INTENT_DATA1, drawingStyleDto);
        setArguments(args);
    }

    //会议摘要
    public SuperChatFragment(int type, Map<String, Object> map) {
        Bundle args = new Bundle();
        args.putInt(Constant.INTENT_TYPE, type);
        args.putSerializable(Constant.INTENT_DATA, (Serializable) map);
        setArguments(args);
    }

    public void init() {
        GlobalDataHolder.init(requireContext()); // 初始化全局共享数据
        vmChat = new ViewModelProvider(requireActivity()).get(VMChat.class);
        vmChat.setContext(requireActivity());

        vmUserProfile = new ViewModelProvider(this).get(VMUserProfile.class);
        vmAccountInfo = new ViewModelProvider(this).get(VMAccountInfo.class);
        sv_chat_list = findViewById(R.id.sv_chat_list);
        cardView = findViewById(R.id.id_card_view);
        cardView.startStreaming();
        ll_bottom = findViewById(R.id.ll_bottom);
        ll_edit_writing = findViewById(R.id.ll_edit_writing);
        ll_edit_translate = findViewById(R.id.ll_edit_translate);
        ll_edit_main = findViewById(R.id.ll_edit_main);
        ll_edit_agent = findViewById(R.id.ll_edit_agent);
        root_view = findViewById(R.id.root_view);
        rv_chat = findViewById(R.id.rv_chat);
        rv_function = findViewById(R.id.rv_function);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        ed = findViewById(R.id.ed);
        aiResponse = findViewById(R.id.aiResponse);
        ll_stop = findViewById(R.id.ll_stop);
        ll_resend = findViewById(R.id.ll_resend);
        tv_mode = findViewById(R.id.tv_mode);
        iv_scroll_down = findViewById(R.id.iv_scroll_down);
        iv_history = findViewById(R.id.iv_history);
        iv_top_play = findViewById(R.id.iv_top_play);
        ll_header = findViewById(R.id.ll_header);
        mShareBottom = findViewById(R.id.ll_share_bottom);
        voiceRecordView = findViewById(R.id.voiceRecordView);
        mLongPicLayout = findViewById(R.id.ll_longpic_layout);
        mShareItemList = findViewById(R.id.share_list);

        tvHeaderSelectAgent = findViewById(R.id.tv_header_select_agent);
        tvHeaderTitle = findViewById(R.id.tv_header_title);
        ivHeaderSelectAgent = findViewById(R.id.iv_header_select_agent);
        ivHead = findViewById(R.id.iv_head);
        ivCreateChat = findViewById(R.id.iv_create_chat);
        mLayoutGuiGuide = findViewById(R.id.layout_gui_guide);
        mSelectTitleLayout = findViewById(R.id.ll_select_title_layout);
        tv_empty_hi = findViewById(R.id.tv_empty_hi);
        ll_empty = findViewById(R.id.ll_empty);
        tv_load_more_hint = findViewById(R.id.tv_load_more_hint);
        ll_select = findViewById(R.id.ll_select);
        // 动态设置 ll_empty 在0.618位置
        setEmptyViewPosition();

        TextView exitText = mSelectTitleLayout.findViewById(R.id.selectCancel);
        exitText.setOnClickListener(v -> updateShareState(false));
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(),
                LinearLayoutManager.HORIZONTAL, false);
        mShareItemList.setLayoutManager(layoutManager);
        mShareItemList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position == 0) {
                    outRect.left = 50;
                }
            }
        });
        mShareItemAdapter = new ShareItemtAdapter(mShareDatas, mItemClickListener);
        mShareItemList.setAdapter(mShareItemAdapter);

        // 启用下拉刷新功能，用于手动加载更多历史记录
        swipeRefreshLayout.setEnabled(true);

        // 下拉刷新监听器：加载更多历史记录
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // 加载更多历史记录
            if (!vmChat.isLoadingPage() && vmChat.hasMorePages()) {
                vmChat.loadNextPage();
                // 加载完成后关闭刷新动画
                mHandler.postDelayed(() -> {
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, 500);
            } else {
                // 没有更多数据时立即关闭刷新动画
                swipeRefreshLayout.setRefreshing(false);
                if (!vmChat.hasMorePages()) {
                    showToast("没有更多历史记录了");

                    updateLoadMoreHint();
                }
            }
        });

        if (getArguments() != null) {
            type = getArguments().getInt(Constant.INTENT_TYPE, SuperChatFragment.TYPE_HOME);
            if (type == TYPE_HOME) {//首页
                agentType = TabEntity.TabType.CHAT;
                ivHead.setVisibility(View.VISIBLE);
                tvHeaderTitle.setVisibility(View.VISIBLE);
//                ivHeaderSelectAgent.setVisibility(View.VISIBLE);
                ivCreateChat.setVisibility(View.VISIBLE);
                mLayoutGuiGuide.setVisibility(View.GONE);
                selectOptionModel = (OptionModel) getArguments().getSerializable(Constant.INTENT_DATA1);
                setFunctionRv(new ArrayList<>());
                requestDataFunctionRv();
                setBottomEdit();
                setHiText(null);
                String input = getArguments().getString(Constant.INTENT_DATA);
                id = getArguments().getLong(Constant.INTENT_ID, 0);
//                vmChat.setSelectOptionModel(selectOptionModel);
                if (id != 0) {
                    ivHead.setVisibility(View.INVISIBLE);
                    ivCreateChat.setVisibility(View.GONE);
                    mLayoutGuiGuide.setVisibility(View.GONE);
                    vmChat.getConversationId().setValue(id);
                    loadConversationHistory(id);
                    if (superEditUtil != null) {
                        superEditUtil.setSelectOptionModel(selectOptionModel);
//                        superEditUtil.setBanSelectModel(true);
                    }
                } else {

//                    vmChat.getChatMessages().getValue().add(new ChatMessage(ChatAdapter.TYPE_USER_HEAD_HOME));
//                    vmChat.setSelectOptionModel(selectOptionModel);

                    fileList = (List<ChatFileBean>) getArguments().getSerializable(Constant.INTENT_DATA2);
                    if (fileList != null) {//带图片的/带文件
                        vmChat.setSelectOptionModel(selectOptionModel);
                        if (input != null) {
                            vmChat.sendMessageWithFile(input, fileList);
                        }
                    } else if (selectOptionModel != null) {//纯文本对话
                        vmChat.setSelectOptionModel(selectOptionModel);
                        if (superEditUtil != null) {
                            superEditUtil.setSelectOptionModel(selectOptionModel);
                        }
                        if (input != null && selectOptionModel != null) {
                            tv_mode.setText(selectOptionModel.getName());
                            vmChat.sendMessage(input);
                        }
                    }
                    ivCreateChat.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (superEditAIWritingUtil != null) {
                                superEditAIWritingUtil.hideBottomView();
                            }
                            vmChat.resetAndLoadFirstPage(0l);
                            vmChat.getChatMessages().getValue().clear();
//                            vmChat.getChatMessages().getValue().add(new ChatMessage(ChatAdapter.TYPE_USER_HEAD_HOME));
//                            vmChat.getChatMessages().postValue(vmChat.getChatMessages().getValue());
                            vmChat.getChatMessages().postValue(vmChat.getChatMessages().getValue());
                            vmChat.getConversationId().set(0l);
                            vmChat.getmFiles().clear();
                            SharedPreferencesUtil.saveString(Constants.PREF_RECENT_CONVERSATION_LIST, "");
                            SharedPreferencesUtil.saveString(Constants.PREF_CONVERSATION_ID, "0");


//                          long  finalId = Long.parseLong(SharedPreferencesUtil.getString(Constants.PREF_CONVERSATION_ID,"0"));
//                        loadConversationHistory(finalId);

                            ll_resend.setVisibility(View.GONE);
                            ll_stop.setVisibility(View.GONE);
                            iv_scroll_down.setVisibility(View.GONE);
                            // 标记为新对话状态，防止滚动监听器显示置底按钮
                            isNewConversation = true;
                            vmChat.closeSSE();
                            vmChat.getStreamEnd().postValue(true);
                            ll_empty.setVisibility(View.VISIBLE);
                            ll_bottom.setVisibility(View.VISIBLE);
                            ll_edit_translate.setVisibility(View.GONE);

                        }
                    });
                    String jsonString = SharedPreferencesUtil.getString(Constants.PREF_RECENT_CONVERSATION_LIST, "");
                    if (!TextUtils.isEmpty(jsonString)) {
                        Gson gson = new Gson();
                        Type type = new TypeToken<List<ChatMessage>>() {
                        }.getType();
                        List<ChatMessage> res = gson.fromJson(jsonString, type);
                        vmChat.getChatMessages().setValue(res);
                    }
                    if (id == 0) {//如果不是从历史过来，就用最近一次存储的
                        ZUtils.print("如果不是从历史过来，就用最近一次存储的 id = " + id);
                        autoLoadTimer = new Timer();
                        autoLoadTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {

                                String idStr = SharedPreferencesUtil.getString(Constants.PREF_CONVERSATION_ID, "0");
                                if (!idStr.equals("") && !idStr.equals("0")) {
                                    long id = Long.parseLong(idStr);
                                    mHandler.post(() -> vmChat.getConversationId().setValue(id));
                                    loadConversationHistory(id);
                                }
                                vmUserProfile.loadUserProfile();
                            }
                        }, 1000);
                    }
                }
            } else if (type == TYPE_AGENT) {//智能体
                getCatDetailListBean bean = (getCatDetailListBean) getArguments().getSerializable(Constant.INTENT_DATA2);
                String query = getArguments().getString(Constant.INTENT_DATA_GUI_QUERY);
                if (!TextUtils.isEmpty(query) && query.startsWith("[")) {
                    List<String> menuBeans = new Gson().fromJson(query, new TypeToken<List<String>>() {
                    });
                    if (menuBeans != null && menuBeans.size() > 0) {
                        Random random = new Random();
                        int i = random.nextInt(100);
                        query = menuBeans.get(i % menuBeans.size());
                    }
                }

                id = getArguments().getLong(Constant.INTENT_ID, 0);
                if (bean != null && bean.getName() != null) {
                    tvHeaderTitle.setText(bean.getName());
                    if (AGENT_GUI.equals(bean.getModelName())) {//GUI 增加重置会话
                        ivCreateChat.setVisibility(View.VISIBLE);
                        mLayoutGuiGuide.setVisibility(View.VISIBLE);
                        mLayoutGuiGuide.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent aboutIntent = new Intent(getActivity(), GuiFunctionHelpActivity.class);
                                startActivity(aboutIntent);
                            }
                        });
                        //清除绘画
                        ivCreateChat.setOnClickListener(v -> {
                            vmChat.getChatMessages().getValue().clear();
                            showAgentGuide();
                        });
                    }
                }
                if (bean != null) {
                    // 统一通过 getAgentHeadInfo 处理，内部会先读缓存再请求网络
                    getAgentHeadInfo(bean.getModelId(), data -> {
                        if (data != null) {
                            if (!TextUtils.isEmpty(data.getName())) {
                                tvHeaderTitle.setText(data.getName());
                            }
//                                vmChat.getChatMessages().getValue().add(new ChatMessage(data.getDescription(), ChatAdapter.TYPE_USER_HEAD_AGENT, data.getIcon()));
//
//                                ChatMessage msg = vmChat.addAIMsg(data.getPreInput().replace("\\n", "\n"));
//                                msg.setHideActionRefresh(true);
                            vmChat.setSelectAgentBean(data);

                            if (id != 0) {//历史进入
                                loadConversationHistory(id);
                            } else {//从缓存拿
                                String idStr = SharedPreferencesUtil.getAgentMap(bean.getBotId());
//                                    String idStr = bean.getId()+"";
                                if (idStr != null && !idStr.equals("") && !idStr.equals("0")) {
                                    id = Long.parseLong(idStr);
                                    long finalId = id;
                                    mHandler.post(() -> vmChat.getConversationId().setValue(finalId));
                                    loadConversationHistory(id);
                                } else {//缓存没有就调用接口（不在回调里触发，以免重复请求）
                                    vmChat.getAgentConversationId(() -> loadConversationHistory(vmChat.getConversationId().get()));
                                }
                            }
                        }
                    });
//                    }
                }

                setAgentBottomEdit(bean, query);
                iv_history.setVisibility(View.GONE);
            } else if (type == TYPE_DRAWING) {//绘画
                tvHeaderTitle.setText("AI绘画");
                iv_history.setVisibility(View.GONE);
                iv_top_play.setVisibility(View.INVISIBLE);
                DrawingToChatBean bean = (DrawingToChatBean) getArguments().getSerializable(Constant.INTENT_DATA);
                DrawingStyleDto styleDto = (DrawingStyleDto) getArguments().getSerializable(Constant.INTENT_DATA1);
                vmChat.setSelectDrawingToChatBean(bean);
                vmChat.setSelectDrawingStyleDto(styleDto);
                if (bean != null) {

                    // 首先检查是否从历史记录进入
                    Long sessionId = bean.getSessionId();
                    vmChat.getConversationId().postValue(Long.parseLong(sessionId.toString()));
                    DrawingSessionDto sessionDetail = bean.getSessionDetail();

                    if (sessionDetail != null && sessionDetail.getAiImageList() != null) {
                        // 从历史记录进入，显示会话详情
                        displaySessionHistory(sessionDetail);

                    } else {

                        // 正常的生成流程
                        String prompt = bean.getPrompt();
                        String style = bean.getStyle();
                        String styleId = bean.getStyle_id();
                        String ratio = bean.getRatio();
                        String referenceImageUrl = bean.getReference_image_url();

                        Timber.tag("DrawingChatActivity").d("Intent data - prompt: " + prompt + ", style: " + style +
                                ", styleId: " + styleId + ", ratio: " + ratio + ", referenceImageUrl: " + referenceImageUrl);

                        vmChat.setSelectedRatio(ratio);
                        if (prompt != null && !prompt.isEmpty()) {
                            // 添加用户消息
                            vmChat.sendDrawingMessage(prompt);
                        }
                    }

                }
//                setAgentBottomEdit();
                ActivityResultLauncher<Intent> launcher;
                launcher = registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK) {
                                Intent data = result.getData();
                                if (data != null) {
                                    ZUtils.print("跳转到绘画聊天界面 StartActivityForResult");
//                                    String returnedData = data.getStringExtra("key");
//                                    resultText.setText(returnedData);
                                    sendDrawingMsgFromSetResult(data);
                                }
                            }
                        });
                ll_edit_agent.setVisibility(View.VISIBLE);
                ed = ll_edit_agent.findViewById(R.id.ed);
                View iv_voice = ll_edit_agent.findViewById(R.id.iv_voice);
                View iv_send = ll_edit_agent.findViewById(R.id.iv_send);
//                iv_voice.setVisibility(View.GONE);
//                iv_send.setVisibility(View.VISIBLE);
                View ll_bottom_edit = ll_edit_agent.findViewById(R.id.ll_bottom_edit);
                ShadowUtils.applyDefaultShadow(ll_bottom_edit, getActivity());
                View ll_edit = ll_edit_agent.findViewById(R.id.ll_edit);
                ed.setHint("描述你想要创作的内容");
                ed.setFocusable(false);

                // 添加文本变化监听器，检测粘贴操作
                ed.addTextChangedListener(new TextWatcher() {
                    private String previousText = "";

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        previousText = s.toString();
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // 检测是否为粘贴操作（一次性输入多个字符）
                        if (count > 1 && before == 0) {
                            String newText = s.toString();
                            if (!TextUtils.isEmpty(newText) && !newText.equals(previousText)) {
                                // 延迟执行跳转，确保文本已完全输入
                                ed.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        String pastedText = ed.getText().toString().trim();
                                        if (!TextUtils.isEmpty(pastedText)) {
                                            ZUtils.print("检测到粘贴文字，跳转到绘画聊天界面: " + pastedText);
                                            // 清空当前输入框
                                            ed.setText("");
                                            // 跳转到绘画聊天界面并传递粘贴的文字
                                            Intent intent = new Intent(getActivity(), DrawingActivity.class);
                                            intent.putExtra("from_chat_send", true);
                                            intent.putExtra("isVoice", false);
                                            intent.putExtra("pasted_text", pastedText);
                                            launcher.launch(intent);
                                        }
                                    }
                                }, 100);
                            }
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        // 不需要处理
                    }
                });

                iv_voice.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 检查网络连接
                        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                            showToast("当前无网络连接，请检查后重试");
                            return;
                        }
                        ZUtils.print("跳转到绘画聊天界面");
                        // 跳转到绘画聊天界面
                        Intent intent = new Intent(getActivity(), DrawingActivity.class);
                        intent.putExtra("from_chat_send", true);
                        intent.putExtra("isVoice", true);
                        launcher.launch(intent);
                    }
                });
                ed.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        ZUtils.print("跳转到绘画聊天界面");
                        // 跳转到绘画聊天界面
                        Intent intent = new Intent(getActivity(), DrawingActivity.class);
                        intent.putExtra("from_chat_send", true);
                        intent.putExtra("isVoice", false);
                        launcher.launch(intent);
                    }
                });

            } else if (type == TYPE_MEETING) {//会议摘要
                ll_header.setVisibility(View.GONE);
                Bundle args = getArguments();
                if (args != null) {
                    Map<String, Object> map = (Map<String, Object>) args.getSerializable(Constant.INTENT_DATA); // 获取 Map
                    // 使用 type 和 map 进行后续逻辑
                    if (map != null) {
                        // 处理 map 数据
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            ZUtils.print("SuperChatFragment == " + "Key: " + entry.getKey() + ", Value: " + entry.getValue().toString());
                        }
                    }

                    initMeetingSummary(map);
                }
            } else if (type == TYPE_WAKE) {//语音唤醒
                swipeRefreshLayout.setEnabled(false);
                swipeRefreshLayout.setRefreshing(false);
                agentType = TabEntity.TabType.CHAT;
                ivHead.setVisibility(View.GONE);
                ll_select.setVisibility(View.GONE);
                tvHeaderSelectAgent.setVisibility(View.GONE);
                ll_bottom.setVisibility(View.GONE);
                ivHeaderSelectAgent.setVisibility(View.GONE);
                ivCreateChat.setVisibility(View.GONE);
                mLayoutGuiGuide.setVisibility(View.GONE);
                selectOptionModel = (OptionModel) getArguments().getSerializable(Constant.INTENT_DATA1);
                id = getArguments().getLong(Constant.INTENT_ID, 0);
                if (id == 0) {//如果不是从历史过来，就用最近一次存储的
                    String idStr = SharedPreferencesUtil.getString(Constants.PREF_CONVERSATION_ID, "0");
                    if (!idStr.equals("") && !idStr.equals("0")) {
                        long id = Long.parseLong(idStr);
                        ZUtils.print("如果不是从历史过来，就用最近一次存储的 id = " + id);
                        mHandler.post(() -> vmChat.getConversationId().setValue(id));
                    }
                }
            }
        }

        setChatRv();
        // 首次进入页面，将位置定位到最新消息
        rv_chat.post(() -> scroll2Last(true));
        ll_stop.setOnClickListener(view -> {
            // 使用VMChat的统一方法处理停止逻辑
            vmChat.stopThinkingAndGeneration("已暂停生成");
            //深度研究点击停止按钮时，将isDeepResearchStreaming设置为false，可以再次发起请求
            vmChat.isDeepResearchStreaming = false;
            //点击停止按钮，不记录深度研究的req_id和query
            SharedPreferencesUtil.saveString(Constants.DEEPRESEARCH_REQ_ID, "0");
            SharedPreferencesUtil.saveString(Constants.DEEPRESEARCH_QUERY, "0");
            // 更新UI状态
            ll_stop.setVisibility(View.GONE);
            ll_resend.setVisibility(View.VISIBLE);

            // 停止TTS播放
            TTSManager.getInstance().stop();

            // 取消正在进行的Markdown渲染
            if (chatAdapter != null) {
                chatAdapter.cancelAllMarkdownRendering();
            }
        });

        iv_scroll_down.setOnClickListener(view -> {
            isUserTouch = false;
            iv_scroll_down.setVisibility(View.GONE);

//            if(vmChat.getStreamEnd().getValue()){//已经结束流，手动调用代码滑动
//            if (type == TYPE_AGENT) {
//                startAutoScroll(KEY_HISTORY);
//            } else {
//                scroll2Last(true);
//            }
            scrollToBottom();
//            }
        });
        ll_resend.setOnClickListener(view -> {
            ChatMessage message = vmChat.getResendMsg();
            if (message != null && message.getMsgType() == ChatAdapter.TYPE_USER) {
                ZUtils.print("message.getMessage() == " + message.getMessage());
                ed.setText(message.getMessage());
                ed.setSelection(message.getMessage().length());
//                vmChat.resendMsg();
                ll_resend.setVisibility(View.GONE);
                if (superEditUtil != null) {
                    superEditUtil.switchMode(0);
                }
                if (superAgentUtil != null) {
                    superAgentUtil.switchMode(0);
                }

                if (type == TYPE_WAKE) {
                    vmChat.resendMsg.postValue(message.getMessage());
                }
            } else if (message != null && (message.getMsgType() == ChatAdapter.TYPE_USER_FILE_IMAGE ||
                    message.getMsgType() == ChatAdapter.TYPE_USER_FILE)) {
                ChatMessage fileTextMsg = vmChat.getResendImageFileMsg();
                ed.setText(fileTextMsg.getMessage());
                ed.setSelection(fileTextMsg.getMessage().length());
                if (superEditUtil != null) {
                    int type = ChatFileAdapter.TYPE_IMAGE;
                    if (message.getMsgType() == ChatAdapter.TYPE_USER_FILE_IMAGE) {
                        type = ChatFileAdapter.TYPE_IMAGE;
                    } else if (message.getMsgType() == ChatAdapter.TYPE_USER_FILE) {
                        type = ChatFileAdapter.TYPE_FILE;
                    }
                    superEditUtil.setFileRv(vmChat.getmFiles(), type);
                    superEditUtil.setList_file(vmChat.getmFiles());
                    superEditUtil.setQuickPromptDefaultUI(type);
                }
            }
        });
        vmChat.getIsAutoPlay().postValue(SharedPreferencesUtil.getBoolean(Constants.KEY_IS_AUTO, true));
        iv_top_play.setOnClickListener(view -> {
            vmChat.setIsAutoPlay(!vmChat.getIsAutoPlay().getValue());
        });
        iv_history.setOnClickListener(view -> {
        });


        rv_chat.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                mIsUserScrolling = (newState == RecyclerView.SCROLL_STATE_DRAGGING);


//                if (recyclerView.canScrollVertically(1) == false) {//的值表示是否能向上滚动，false表示已经滚动到底部
//                    iv_scroll_down.setVisibility(View.GONE);
//                    Timber.tag("test ui").d("addOnScrollListener iv_scroll_down GONE");
//                    // 到达底部，允许后续自动滚动
//                    isUserTouch = false;
//                }

            }
        });
        sv_chat_list.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                // 1. 获取内容总高度
                int contentHeight = v.getChildAt(0).getHeight();
                // 2. 获取NestedScrollView容器的高度
                int containerHeight = v.getHeight();
                // 3. 计算底部临界值（内容高度 - 容器高度）
                int bottomThreshold = contentHeight - containerHeight;
                // 4. 判断是否滚动到底部
                if (contentHeight > containerHeight && scrollY >= bottomThreshold - 10) {
                    if (!isScrolledToBottom) {
                        isScrolledToBottom = true;
                        iv_scroll_down.setVisibility(View.GONE);
//                        Timber.tag("test ui").d("sv_chat_list onScrollChange iv_scroll_down GONE");
                        // 到达底部，允许后续自动滚动
                        isUserTouch = false;
                    }
                } else {
//                    Timber.tag("test ui").d("sv_chat_list onScrollChange iv_scroll_down VISIBLE");
                    // 如果是新对话状态，不显示置底按钮
                    if (!isNewConversation) {
                        iv_scroll_down.setVisibility(View.VISIBLE);
                    }
                    // 当离开底部区域时重置标记
                    isScrolledToBottom = false;
                }
            }
        });

        tvHeaderSelectAgent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 检查网络连接
                if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                    showToast("当前无网络连接，请检查后重试");
                    return;
                }
                if (superEditUtil != null) {
                    superEditUtil.showChooseModelPopup(tvHeaderSelectAgent);
                }
            }
        });
        ivHeaderSelectAgent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 检查网络连接
                if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                    showToast("当前无网络连接，请检查后重试");
                    return;
                }
                if (superEditUtil != null) {
                    superEditUtil.showChooseModelPopup(tvHeaderSelectAgent);
                }
            }
        });

        ivHead.setOnClickListener(v -> {
            if (AuthHelper.getInstance().isLogin()) {
                // 跳转到设置界面
                Intent intent = new Intent(getActivity(), UserActivity.class);
                startActivity(intent);
            } else {
                // 未登录，跳转到一键登录页面
                Intent intent = new Intent(getActivity(), OneClickLoginActivity.class);
                getActivity().startActivity(intent);
            }

//            Intent intent = new Intent(getActivity(), WakeVoiceActivity.class);
//            getActivity().startActivity(intent);
        });

        initAsrManger();
        initNetwork();
    }

    private void setHiText(String nameText) {
        String name = SharedPreferencesUtil.getUserName();
        if (nameText != null) {
            name = nameText;
        }
        tv_empty_hi.setText("您好，" + name);
    }

    private void sendDrawingMsgFromSetResult(Intent data) {

        DrawingToChatBean bean = (DrawingToChatBean) data.getSerializableExtra(Constant.INTENT_DATA);
        DrawingStyleDto styleDto = (DrawingStyleDto) data.getSerializableExtra(Constant.INTENT_DATA1);
        vmChat.setSelectDrawingToChatBean(bean);
        vmChat.setSelectDrawingStyleDto(styleDto);
        // 正常的生成流程
        String prompt = bean.getPrompt();
        String style = bean.getStyle();
        String styleId = bean.getStyle_id();
        String ratio = bean.getRatio();
        String referenceImageUrl = bean.getReference_image_url();

        Timber.tag("DrawingChatActivity").d("Intent data - prompt: " + prompt + ", style: " + style +
                ", styleId: " + styleId + ", ratio: " + ratio + ", referenceImageUrl: " + referenceImageUrl);

        vmChat.setSelectedRatio(ratio);
        if (prompt != null && !prompt.isEmpty()) {
            // 添加用户消息
            vmChat.sendDrawingMessage(prompt);
        }
    }

    private void initMeetingSummary(Map<String, Object> map) {
        // 防止重复初始化
        if (meetingMap != null && meetingMap.equals(map)) {
            return;
        }

        meetingMap = map;
        vmMeetingSummary = new ViewModelProvider(this).get(VMMeetingSummary.class);

        String transcriptionResult = (String) map.get(MeetingSummaryFragment.ARG_TRANSCRIPTION_RESULT);
        int meetingIdInt = (int) map.get(MeetingSummaryFragment.ARG_MEETING_ID);
        String botKey = (String) map.get(MeetingSummaryFragment.ARG_BOTKEY);
        Log.i("MeetingSummaryFragment", "切换到标签: " /*+ getSelectedTagName()*/ + ", botKey: " + botKey);
        vmMeetingSummary.generateMeetingSummaryWithCheck(transcriptionResult, meetingIdInt, botKey);
    }

    private void getAgentHeadInfo(long id, RequestCallback<getCatDetailListBean> callback) {
        final boolean[] hasCachedData = {false};

        // 先尝试本地缓存
        try {
            String key = Constants.PREF_AGENT_HEAD_BY_MODEL_PREFIX + id;
            String json = SharedPreferencesUtil.getString(key, "");
            if (!TextUtils.isEmpty(json)) {
                getCatDetailListBean cached = new Gson().fromJson(json, getCatDetailListBean.class);
                if (cached != null) {
                    Timber.tag(TAG).d("agent head cache hit, modelId=" + id);
                    callback.callback(cached);
                    hasCachedData[0] = true;
                }
            }
        } catch (Exception ignore) {
        }

        // 如果无网络，直接返回（避免重复回调）
        if (!NetworkUtils.isNetworkAvailable(getContext())) {
            Timber.tag(TAG).d("no network, skip network request for agent head");
            return;
        }

        HttpRequest httpRequest = new HttpRequest();
        httpRequest.getDetailByModel(id + "", new Observer<ApiResponse<getCatDetailListBean>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<getCatDetailListBean> getCatDetailListBeanApiResponse) {
                try {
                    String key = Constants.PREF_AGENT_HEAD_BY_MODEL_PREFIX + id;
                    String json = new Gson().toJson(getCatDetailListBeanApiResponse.getData());
                    SharedPreferencesUtil.saveString(key, json);
                    Timber.tag(TAG).d("agent head cache saved, modelId=" + id + ", bytes=" + (json != null ? json.length() : 0));
                } catch (Exception ignore) {
                }

                // 只有在没有缓存数据时才回调，避免重复
                if (!hasCachedData[0]) {
                    callback.callback(getCatDetailListBeanApiResponse.getData());
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.tag(TAG).e("getAgentHeadInfo error: " + (e != null ? e.getMessage() : "null"));
            }

            @Override
            public void onComplete() {

            }
        });
    }

    private void loadConversationHistory(long id) {
        Timber.tag(TAG).d("loadConversationHistory enter, id=" + id +
                ", network=" + NetworkUtils.isNetworkAvailable(getContext()));
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                // 显示加载框
//                LoadingProgressDialog loadingDialog =
//                        new LoadingProgressDialog(getActivity())
//                                .setMessage("加载中...")
//                                .setCancelable(false);
//                loadingDialog.show();
//            }
//        });

        ChatRepository repository = new ChatRepositoryImpl();
        // 无网络时，优先尝试从本地缓存读取
        if (!NetworkUtils.isNetworkAvailable(getContext())) {
            try {
                String key = Constants.PREF_CHAT_HISTORY_BY_ID_PREFIX + id;
                String json = SharedPreferencesUtil.getString(key, "");
                Timber.tag(TAG).d("offline read, key=" + key + ", bytes=" + (json != null ? json.length() : 0));
                if (!TextUtils.isEmpty(json)) {
                    Type listType = new TypeToken<List<ConversationDetailDto>>() {
                    }.getType();
                    List<ConversationDetailDto> list = new Gson().fromJson(json, listType);
                    if (list != null && !list.isEmpty()) {
                        Timber.tag(TAG).d("offline hit, items=" + list.size());
                        // 直接走 onSuccess 流程渲染
                        handleOfflineConversationHistory(list);
                        return;
                    }
                    Timber.tag(TAG).d("offline miss, parsed empty list");
                }
                Timber.tag(TAG).d("offline miss, empty json");
            } catch (Exception ignore) {
            }
        }
        // 通过 VM 统一管理分页
//        vmChat.resetAndLoadFirstPage(id);
//
//        // 设置分页加载监听器（带防抖机制）
//        setupLoadMoreListener();

        // 分页结果在 VM 的 LiveData 观察中统一处理
        // 下面的 onSuccess 缓存逻辑改由 VM 成功后触发时执行
        repository.getPageByConversationId(id, 1, vmChat.getPageSize(), new ChatRepository.Callback<List<ConversationDetailDto>>() {
            @Override
            public void onSuccess(List<ConversationDetailDto> list) {
                Timber.tag(TAG).d("loadConversationHistory onSuccess, id=" + id + ", items=" + (list != null ? list.size() : -1));
                // 成功后写入按ID的离线缓存，限制60轮对话（不依赖 Fragment 生命周期）
                try {
                    String key = Constants.PREF_CHAT_HISTORY_BY_ID_PREFIX + id;

                    // 限制缓存60轮对话，保留最新的60条
                    List<ConversationDetailDto> cacheList = list;
                    if (list != null && list.size() > 60) {
                        cacheList = new ArrayList<>(list.subList(Math.max(0, list.size() - 60), list.size()));
                        Timber.tag(TAG).d("cache limited to 60 items, original=" + list.size() + ", cached=" + cacheList.size());
                    }

                    String json = new Gson().toJson(cacheList);
                    SharedPreferencesUtil.saveString(key, json);
                    String back = SharedPreferencesUtil.getString(key, "");
                    Timber.tag(TAG).d("cache chat by id saved, key=" + key + ", bytes=" + (back != null ? back.length() : 0) + ", items=" + (cacheList != null ? cacheList.size() : 0));
                } catch (Exception e) {
                    Timber.tag(TAG).e("cache chat by id save error: " + e.getMessage());
                }

                if (!isAdded()) {
                    Timber.tag(TAG).d("loadConversationHistory onSuccess skipped, fragment detached");
                    return;
                }

                if (list != null && !list.isEmpty()) {
                    // 通过 VM 统一管理分页
                    vmChat.resetAndLoadFirstPage(id);
                    // 设置分页加载监听器（带防抖机制）
                    setupLoadMoreListener();
                }
//                loadingDialog.dismiss();
//                vmChat.getChatMessages().setValue(new ArrayList<>());
//                vmChat.getChatMessages().postValue(new ArrayList<>());
                //                vmChat.getChatMessages().postValue(vmChat.getChatMessages().getValue());
                ZUtils.print("getListByConversationId list == " + list.size() + list.toString());
                if (type == TYPE_AGENT && (list == null || list.size() == 0)) {
                    String preInput = vmChat.getSelectAgentBean().getPreInput().replace("\\n", "\n");
//                    if (vmChat.getIsAutoPlay().getValue()) {
//                        TTSManager.Companion.getInstance().textForceToAudio(preInput);
//                    }
                    setAgentHead();
                }
                if (type == TYPE_HOME) {
                    vmChat.getChatMessages().getValue().clear();
//                    vmChat.getChatMessages().getValue().add(new ChatMessage(ChatAdapter.TYPE_USER_HEAD_HOME));
                    if (chatAdapter != null) {
                        chatAdapter.notifyDataSetChanged();
                    }
                }
                if (list != null && list.size() > 0) {
                    resetNewConversationFlag();
                    //判断deepresearch 最近一条请求是否完成
                    boolean lastDeepResearchRequset = true;
                    final Gson gson = new Gson();
                    final Type FILE_LIST_TYPE = new TypeToken<List<ChatFileListJsonBean>>() {
                    }.getType();
                    String req_id = SharedPreferencesUtil.getString(Constants.DEEPRESEARCH_REQ_ID, "0");
                    for (int i = 0; i < list.size(); i++) {
                        ConversationDetailDto dto = list.get(i);

                        Timber.tag("ConversationDetailDto").d(dto.getContent());
                        if (!TextUtils.isEmpty(dto.getContent())) {
                            if (dto.getType().equals("assistant")) {
                                ZUtils.print("onSuccess assistant = " + dto.getContent());
                                ChatMessage aiMsg = null;
                                if (TextUtils.equals("10086", dto.getModel()) || TextUtils.equals(Constants.AGENT_TRAVEL, dto.getModel())
                                        || TextUtils.equals(Constants.AGENT_TRIP, dto.getModel()) || TextUtils.equals(Constants.AGENT_MGVIDOE, dto.getModel())
                                        || TextUtils.equals(Constants.AGENT_FINANCE, dto.getModel()) || TextUtils.equals(Constants.AGENT_COMMUNICATION, dto.getModel())) {
                                    aiMsg = vmChat.addAIMsgLingxiHistory(dto.getContent());
                                } else if (TextUtils.equals("深度研究", dto.getModel())) {

                                    DeepResearchBean resp = gson.fromJson(dto.getContent(), DeepResearchBean.class);
                                    if (resp.getModel() != null) {
                                        if (req_id.equals(resp.getReq_id()) || req_id.equals("0")) {
                                            Timber.tag(TAG).d("深度研究请求ID: " + resp.getReq_id());
                                            lastDeepResearchRequset = false;
                                        }
//                                        vmChat.addDeepResearchCard(resp);
                                        vmChat.addDeepResearchCompleteCardHistory(resp);
                                    }
                                } else {
                                    aiMsg = vmChat.addAIMsgHistory(dto.getContent(), dto.getThinkText());
                                }
                                // 设置服务器返回ID
                                if (aiMsg != null) {
                                    aiMsg.setId((long) dto.getId());
//                                    Timber.tag("SuperChatFragment").d( "设置AI消息ID: " + dto.getId());
                                }
                            } else if (dto.getType().equals("user")) {
//                                ZUtils.print("onSuccess user = " + dto.getContent());
                                vmChat.sendMessageHistory(dto.getContent());
                                // 获取刚创建的用户消息并设置ID
                                List<ChatMessage> currentMessages = vmChat.getChatMessages().getValue();
                                if (currentMessages != null && !currentMessages.isEmpty()) {
                                    ChatMessage lastUserMsg = null;
                                    // 从后往前查找最近的用户消息
                                    for (int j = currentMessages.size() - 1; j >= 0; j--) {
                                        ChatMessage msg = currentMessages.get(j);
                                        if (msg.getMsgType() == ChatAdapter.TYPE_USER) {
                                            lastUserMsg = msg;
                                            break;
                                        }
                                    }
                                    if (lastUserMsg != null) {
                                        lastUserMsg.setId((long) dto.getId());
//                                        Timber.tag("SuperChatFragment").d( "设置用户消息ID: " + dto.getId());
                                    }
                                }
                                List<ChatFileBean> files = new ArrayList<>();
                                if (dto.getFileListJson() != null) {
                                    List<ChatFileListJsonBean> eventList = gson.fromJson(dto.getFileListJson(),
                                            FILE_LIST_TYPE);

                                    if (eventList != null && eventList.size() > 0) {
                                        for (int j = 0; j < eventList.size(); j++) {
                                            ChatFileListJsonBean bean = eventList.get(j);
                                            ChatFileBean chatFileBean = new ChatFileBean(bean.getName(), bean.getFileUrl(), false);
                                            chatFileBean.setFileType(bean.getType());
                                            files.add(chatFileBean);
                                        }
                                    }
                                } else if (dto.getImages() != null) {
                                    String[] results = dto.getImages().split(",");

                                    if (results != null && results.length > 0) {
                                        for (int j = 0; j < results.length; j++) {
                                            String url = results[j];
                                            ChatFileBean chatFileBean = new ChatFileBean(url, true);
                                            chatFileBean.setPath(url);
                                            chatFileBean.setPercent(100);
                                            files.add(chatFileBean);
                                        }
                                    }

                                }

                                vmChat.addUserMsgWithFile(files);
                            }
//                        }
                        }
                    }
                    if (lastDeepResearchRequset) {
                        if (vmChat.getSelectAgentBean() != null && vmChat.getSelectAgentBean().getModelName() != null) {
                            if (vmChat.getSelectAgentBean().getModelName().equals("深度研究")) {
                                ChatDataFormat chatDataFormat = new ChatDataFormat(getActivity(), honorHttp);

                                String query = SharedPreferencesUtil.getString(Constants.DEEPRESEARCH_QUERY, "0");
                                Timber.tag(TAG).d("深度研究请求query: " + query);
                                if (!"0".equals(query)) {
                                    vmChat.deepResearchStreamEnd.postValue(false);
                                    vmChat.isDeepResearchStreaming = true;
                                    Timber.tag(TAG).d("isDeepResearchStreaming %s", vmChat.isDeepResearchStreaming);
                                    sseDisposable = chatDataFormat.startAgentFlow(null, query, true, new DeepResearchCallback() {
                                        @Override
                                        public void onDeepResearch(DeepResearchBean deepResearchBean) {

                                            if (deepResearchBean.getTaskStatus() == 1) {
                                                deepResearchBean.setTaskStatus(2);
                                                vmChat.addDeepResearchCard(deepResearchBean);
                                            } else if (deepResearchBean.getTaskStatus() == 4) {
                                                vmChat.deepResearchStreamEnd.postValue(true);
                                                String result = GsonUtils.toJson(deepResearchBean.getList().get(deepResearchBean.getList().size() - 1));
                                                Timber.tag(TAG).d("onDeepResearch TaskStatus = 4 data = " + result);
                                                vmChat.updateDeepResearchMsg(deepResearchBean);
                                                //添加一个完成的卡片
                                                mHandler.postDelayed(() -> {
                                                    vmChat.addDeepResearchCompleteCard(deepResearchBean);
                                                }, 2000);
                                                //创建文件
//                                                DocumentHelper helper = new DocumentHelper(getActivity());
//                                                String content = deepResearchBean.getReportContent();
//                                                helper.createWordWithText(content, query);
                                            } else {
//                                          Timber.tag(TAG).d("updateDeepResearchMsg");
                                                vmChat.updateDeepResearchMsg(deepResearchBean);
                                            }
                                        }

                                        @Override
                                        public void onDeepResearchError(String error) {
                                            vmChat.isDeepResearchStreaming = false;
                                            vmChat.addNetworkErrorCard();
                                            vmChat.deepResearchStreamEnd.postValue(true);
                                        }

                                        @Override
                                        public void onDeepResearchComplete() {
                                            vmChat.isDeepResearchStreaming = false;
                                        }
                                    });
                                    vmChat.setDisposable(sseDisposable);
                                }
                            }
                        }
                    }
                    if (vmChat.getSelectAgentBean() != null && (TextUtils.equals(vmChat.getSelectAgentBean().getModelName(), getString(R.string.txt_migu)) ||
                            TextUtils.equals(vmChat.getSelectAgentBean().getModelName(), getString(R.string.txt_finance)) ||
                            TextUtils.equals(vmChat.getSelectAgentBean().getModelName(), getString(R.string.txt_communication)))) {
                        startAutoScroll(KEY_HISTORY);
                        return;
                    }
                    // 等待消息数据完全加载后再滚动到底部
                    mHandler.postDelayed(() -> {
                        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                            // 通知Adapter数据已改变
                            if (chatAdapter != null) {
                                chatAdapter.notifyDataSetChanged();
                            }
                            // 用高度稳定检测方法滚动到底部，避免 item 未完全 measure 时偏上
                            scrollToBottomWhenStable();
                            Timber.tag(TAG).d("历史记录加载完成，滚动到底部");
                        }
                    }, 300); // 给足够时间让数据加载和渲染完成
                }

            }

            @Override
            public void onError(String error) {
                Timber.tag(TAG).e("loadConversationHistory onError, id=" + id + ", error=" + error);
//                loadingDialog.dismiss();
            }
        });

    }

    /**
     * 设置分页加载监听器（带防抖机制）
     * 当用户滚动到顶部时自动加载更多历史消息
     * 修复：将 Observer 从滚动回调内部提取到外部，只注册一次，
     * 避免每次触发分页加载都新增 Observer 导致累积泄漏
     */
    private void setupLoadMoreListener() {
        // 移除旧的监听器（如果存在）
        if (scrollChangedListener != null && sv_chat_list != null) {
            sv_chat_list.getViewTreeObserver().removeOnScrollChangedListener(scrollChangedListener);
            Timber.tag(TAG).d("移除旧的滚动监听器");
        }

        // 移除旧的分页 Observer（避免重复注册）
        if (loadMoreObserver != null && vmChat != null) {
            vmChat.getChatMessages().removeObserver(loadMoreObserver);
            loadMoreObserver = null;
            Timber.tag(TAG).d("移除旧的分页Observer");
        }

        // 首次加载时不显示提示，等待用户滚动后再判断
        // 避免在首次进入页面时就显示"下拉获取更多历史消息"
        if (tv_load_more_hint != null) {
            tv_load_more_hint.setVisibility(View.GONE);
        }

        // 注册分页数据 Observer（只注册一次，数据变更时自动恢复滚动位置）
        loadMoreObserver = messages -> {
            if (isLoadingMore && messages != null) {
                // 等待RecyclerView更新完成
                sv_chat_list.post(() -> {
                    if (sv_chat_list == null || !isAdded()) return;
                    if (sv_chat_list.getChildAt(0) != null) {
                        int childHeightAfterLoad = sv_chat_list.getChildAt(0).getHeight();
                        int heightDiff = childHeightAfterLoad - childHeightBeforeLoad;

                        // 调整滚动位置，保持用户当前查看的内容不变
                        if (heightDiff > 0) {
                            sv_chat_list.scrollTo(0, scrollYBeforeLoad + heightDiff);
                            Timber.tag(TAG).d("恢复滚动位置，新scrollY=" +
                                    (scrollYBeforeLoad + heightDiff) + ", heightDiff=" + heightDiff);
                        }
                    }
                });
            }
        };
        vmChat.getChatMessages().observe(SuperChatFragment.this, loadMoreObserver);
        Timber.tag(TAG).d("注册分页Observer（单次）");

        // 创建新的滚动监听器
        scrollChangedListener = () -> {
            if (!isAdded() || getView() == null) return; // 安全检查
            // 更新提示文本显示状态
            updateLoadMoreHint();

            // 检查是否在顶部
            if (sv_chat_list.getScrollY() <= 0) {
                // 防抖检查：避免短时间内重复触发
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLoadTime < LOAD_DEBOUNCE_MS) {
                    Timber.tag(TAG).d("防抖拦截：距离上次加载时间过短");
                    return;
                }

                // 检查是否可以加载
                if (!isLoadingMore && !vmChat.isLoadingPage() && vmChat.hasMorePages()) {
                    isLoadingMore = true;
                    lastLoadTime = currentTime;

                    // 记录加载前的滚动位置（使用类字段，供外部 Observer 回调使用）
                    scrollYBeforeLoad = sv_chat_list.getScrollY();
                    childHeightBeforeLoad = sv_chat_list.getChildAt(0) != null ?
                            sv_chat_list.getChildAt(0).getHeight() : 0;

                    Timber.tag(TAG).d("触发加载下一页，当前scrollY=" + scrollYBeforeLoad +
                            ", childHeight=" + childHeightBeforeLoad);

                    vmChat.loadNextPage();
                    // Observer 已在外部注册，数据变更会自动回调恢复滚动位置，无需再新增 observe

                    // 加载完成后重置标志（延迟1秒）
                    mHandler.postDelayed(() -> {
                        isLoadingMore = false;
                        updateLoadMoreHint();
                        Timber.tag(TAG).d("重置加载标志");
                    }, 1000);
                } else {
                    if (isLoadingMore) {
                        Timber.tag(TAG).d("已在加载中，跳过");
                    } else if (vmChat.isLoadingPage()) {
                        Timber.tag(TAG).d("VM正在加载，跳过");
                    } else if (!vmChat.hasMorePages()) {
                        Timber.tag(TAG).d("没有更多页面，跳过");
                    }
                }
            }
        };

        // 注册监听器
        sv_chat_list.getViewTreeObserver().addOnScrollChangedListener(scrollChangedListener);
        Timber.tag(TAG).d("注册新的滚动监听器");
    }

    /**
     * 更新"加载更多"提示文本的显示状态
     * 当滚动到顶部且有更多页面时显示
     */
    private void updateLoadMoreHint() {
        if (tv_load_more_hint == null || sv_chat_list == null) {
            return;
        }

        // 检查是否在顶部附近（允许一定误差）
        boolean isNearTop = sv_chat_list.getScrollY() <= 50;

        // 检查是否有更多页面
        boolean hasMore = vmChat != null && vmChat.hasMorePages();

        // 首次加载时不显示提示（即使在顶部）
        // 只有在用户主动滚动后才显示
        if (isFirstLoad) {
            if (tv_load_more_hint.getVisibility() != View.GONE) {
                tv_load_more_hint.setVisibility(View.GONE);
                Timber.tag(TAG).d("首次加载，隐藏加载更多提示");
            }
            return;
        }

        // 只有在顶部附近且有更多页面时才显示提示
        if (isNearTop && hasMore && !isLoadingMore) {
            if (tv_load_more_hint.getVisibility() != View.VISIBLE) {
                tv_load_more_hint.setVisibility(View.GONE);
                Timber.tag(TAG).d("显示加载更多提示");
            }
        } else {
            if (tv_load_more_hint.getVisibility() != View.GONE) {
                tv_load_more_hint.setVisibility(View.GONE);
                Timber.tag(TAG).d("隐藏加载更多提示");
            }
        }
    }

    // 将离线读取的列表复用现有渲染逻辑
    private void handleOfflineConversationHistory(List<ConversationDetailDto> list) {
        // 直接复制 onSuccess 的主要渲染路径（去掉网络副作用）
        if (list != null && list.size() > 0) {
            resetNewConversationFlag();
            final Gson gson = new Gson();
            final Type FILE_LIST_TYPE = new TypeToken<List<ChatFileListJsonBean>>() {
            }.getType();
            String req_id = SharedPreferencesUtil.getString(Constants.DEEPRESEARCH_REQ_ID, "0");
            for (int i = 0; i < list.size(); i++) {
                ConversationDetailDto dto = list.get(i);
                if (!TextUtils.isEmpty(dto.getContent())) {
                    if (dto.getType().equals("assistant")) {
                        ChatMessage aiMsg = null;
                        if (TextUtils.equals("10086", dto.getModel()) || TextUtils.equals(Constants.AGENT_TRAVEL, dto.getModel())
                                || TextUtils.equals(Constants.AGENT_TRIP, dto.getModel()) || TextUtils.equals(Constants.AGENT_MGVIDOE, dto.getModel())
                                || TextUtils.equals(Constants.AGENT_FINANCE, dto.getModel()) || TextUtils.equals(Constants.AGENT_COMMUNICATION, dto.getModel())) {
                            aiMsg = vmChat.addAIMsgLingxiHistory(dto.getContent());
                        } else if (TextUtils.equals("深度研究", dto.getModel())) {
                            DeepResearchBean resp = gson.fromJson(dto.getContent(), DeepResearchBean.class);
                            if (resp.getModel() != null) {
                                vmChat.addDeepResearchCompleteCardHistory(resp);
                            }
                        } else {
                            aiMsg = vmChat.addAIMsgHistory(dto.getContent(), dto.getThinkText());
                        }
                        if (aiMsg != null) {
                            aiMsg.setId((long) dto.getId());
                        }
                    } else if (dto.getType().equals("user")) {
                        vmChat.sendMessageHistory(dto.getContent());
                        List<ChatMessage> currentMessages = vmChat.getChatMessages().getValue();
                        if (currentMessages != null && !currentMessages.isEmpty()) {
                            ChatMessage lastUserMsg = null;
                            for (int j = currentMessages.size() - 1; j >= 0; j--) {
                                ChatMessage msg = currentMessages.get(j);
                                if (msg.getMsgType() == ChatAdapter.TYPE_USER) {
                                    lastUserMsg = msg;
                                    break;
                                }
                            }
                            if (lastUserMsg != null) {
                                lastUserMsg.setId((long) dto.getId());
                            }
                        }
                        List<ChatFileBean> files = new ArrayList<>();
                        if (dto.getFileListJson() != null) {
                            List<ChatFileListJsonBean> eventList = gson.fromJson(dto.getFileListJson(), FILE_LIST_TYPE);
                            if (eventList != null && eventList.size() > 0) {
                                for (int j = 0; j < eventList.size(); j++) {
                                    ChatFileListJsonBean bean = eventList.get(j);
                                    ChatFileBean chatFileBean = new ChatFileBean(bean.getName(), bean.getFileUrl(), false);
                                    chatFileBean.setFileType(bean.getType());
                                    files.add(chatFileBean);
                                }
                            }
                        } else if (dto.getImages() != null) {
                            String[] results = dto.getImages().split(",");
                            if (results != null && results.length > 0) {
                                for (int j = 0; j < results.length; j++) {
                                    String url = results[j];
                                    ChatFileBean chatFileBean = new ChatFileBean(url, true);
                                    chatFileBean.setPath(url);
                                    chatFileBean.setPercent(100);
                                    files.add(chatFileBean);
                                }
                            }
                        }
                        vmChat.addUserMsgWithFile(files);
                    }
                }
            }
            // 渲染完成后滚动到底
            mHandler.postDelayed(() -> {
                if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                    if (chatAdapter != null) {
                        chatAdapter.notifyDataSetChanged();
                    }
                    // 用高度稳定检测方法滚动到底部
                    scrollToBottomWhenStable();
                }
            }, 300);
        }
    }

    /**
     * 刷新聊天历史记录
     */
    public void refreshChatHistory() {
        Long conversationId = vmChat.getConversationId().getValue();
        if (conversationId != null && conversationId > 0) {
            // 清空当前消息列表
            List<ChatMessage> list = vmChat.getChatMessages().getValue();
            if (list != null) {
                list.clear();
                vmChat.getChatMessages().postValue(list);
            }
            // 重新加载历史记录
            loadConversationHistory(conversationId);
        } else {
            // 新建对话状态，没有历史记录可刷新
            Toast.makeText(getActivity(), "当前为新建对话，暂无历史记录", Toast.LENGTH_SHORT).show();
        }
        // 下拉刷新已禁用，无需停止刷新动画
        // swipeRefreshLayout.setRefreshing(false);
    }

    private ScheduledExecutorService historyExecutorService;
    private ScheduledExecutorService bottomExecutorService;

    private static final String KEY_HISTORY = "history";
    private static final String KEY_BOTTOM = "bottom";

    private void startAutoScroll(String key) {
        switch (key) {
            case KEY_HISTORY:
                if (historyExecutorService == null || historyExecutorService.isShutdown()) {
                    historyExecutorService = Executors.newSingleThreadScheduledExecutor();
                }
                historyExecutorService.scheduleAtFixedRate(() -> performAutoScroll(key),
                        200, 100, TimeUnit.MILLISECONDS);
                break;
            case KEY_BOTTOM:
                if (bottomExecutorService == null || bottomExecutorService.isShutdown()) {
                    bottomExecutorService = Executors.newSingleThreadScheduledExecutor();
                }
                bottomExecutorService.scheduleAtFixedRate(() -> performAutoScroll(key),
                        200, 100, TimeUnit.MILLISECONDS);
                break;
        }
    }

    private void performAutoScroll(String key) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            getActivity().runOnUiThread(() -> {
                if (isAdded() && !isDetached()) {
                    scrollToBottom();
                }
            });
        }
    }

    private void stopAutoScroll(String key) {
        switch (key) {
            case KEY_HISTORY:
                shutdownExecutor(historyExecutorService);
                break;
            case KEY_BOTTOM:
                shutdownExecutor(bottomExecutorService);
                break;
        }
    }




    /**
     * 安全关闭 ScheduledExecutorService，避免 onDestroy 时残留的定时任务泄漏。
     */
    private void shutdownExecutor(ScheduledExecutorService executor) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // 可取消的高度稳定检测轮询：每次新调用覆盖前一次，保证始终使用最新数据滚动
    private Handler scrollStableHandler;
    private Runnable scrollStableRunnable;

    /**
     * 等待 NestedScrollView 内 RecyclerView 高度稳定后，瞬间滚动到底部。
     * 采用可取消轮询方式：每 50ms 检查高度，连续 2 次相同时认为稳定并滚动。
     * 每次新调用会取消前一次检测并重新开始 —— 确保多轮数据加载时最终用完整数据滚动。
     * 2s 超时兜底：超时后不管是否稳定都强制 scrollTo。
     */
    private void scrollToBottomWhenStable() {
        if (sv_chat_list == null || !isAdded()) return;

        if (scrollStableHandler == null) {
            scrollStableHandler = new Handler(Looper.getMainLooper());
        }

        // 每次新调用取消前一次轮询，重新开始 —— 这是关键：
        // loadConversationHistory 中 resetAndLoadFirstPage 和 onSuccess 会先后触发，
        // 后者数据更完整，应覆盖前一次的稳定检测
        if (scrollStableRunnable != null) {
            scrollStableHandler.removeCallbacks(scrollStableRunnable);
            Timber.tag(TAG).d("scrollToBottomWhenStable 取消前一次检测，重新开始");
        }

        final long startTime = System.currentTimeMillis();
        final long MAX_WAIT_MS = 2000;

        scrollStableRunnable = new Runnable() {
            private int lastHeight = -1;
            private int stableCount = 0;

            @Override
            public void run() {
                if (sv_chat_list == null || !isAdded()) {
                    scrollStableRunnable = null;
                    return;
                }
                View child = sv_chat_list.getChildAt(0);
                int h = (child != null) ? child.getHeight() : 0;

                boolean timedOut = (System.currentTimeMillis() - startTime) > MAX_WAIT_MS;

                if (h == lastHeight && h > 0) {
                    stableCount++;
                } else {
                    stableCount = 0;
                    lastHeight = h;
                }

                if (stableCount >= 2 || timedOut) {
                    if (h > 0) {
                        sv_chat_list.scrollTo(0, h);
                    }
                    scrollStableRunnable = null;
                    Timber.tag(TAG).d("scrollToBottomWhenStable 完成"
                            + (timedOut ? "(超时兜底)" : "")
                            + "，高度=" + h + "，stableCount=" + stableCount);
                    return;
                }

                scrollStableHandler.postDelayed(this, 50);
            }
        };
        scrollStableHandler.post(scrollStableRunnable);
    }

    /**
     * 滚动到聊天列表底部方法
     */
    private void scrollToBottom() {
        if (sv_chat_list == null || getActivity() == null || getActivity().isFinishing() || !isAdded()) {
            return;
        }

        // post 确保当前帧 layout 全部完成后再滚动，避免新 item 还未测量时读取 getBottom() 偏小
        sv_chat_list.post(() -> {
            if (sv_chat_list == null || !isAdded()) return;
            try {
                // 使用 scrollTo 而非 fullScroll(FOCUS_DOWN)
                // fullScroll 会触发 focusSearch，将焦点从 EditText 抢走导致软键盘收起
                // scrollTo 只滚动不改变焦点，效果完全一致
                View child = sv_chat_list.getChildAt(0);
                if (child != null) {
                    int scrollRange = child.getHeight() - sv_chat_list.getHeight();
                    if (scrollRange > 0) {
                        sv_chat_list.scrollTo(0, scrollRange);
                    }
                }

                // 同步让 RecyclerView 也滚到最后一项，防止 NestedScrollView 高度未撑开时的兜底
                if (rv_chat != null && rv_chat.getAdapter() != null) {
                    int itemCount = rv_chat.getAdapter().getItemCount();
                    if (itemCount > 0) {
                        rv_chat.scrollToPosition(itemCount - 1);
                    }
                }

                Timber.tag(TAG).d("执行滚动到底部");
            } catch (Exception e) {
                Timber.tag(TAG).e("滚动到底部失败: %s", e.getMessage());
            }
        });
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.act_super_chat;
    }

    @Override
    protected Class getViewModelClass() {
        return VMChat.class;
    }

    @Override
    protected void initializeViews(View view) {
        initializeViews();
    }

    @Override
    protected void setupDataBinding() {

    }

    public VMChat getVMChat() {
        return vmChat;
    }

    protected void initializeViews() {
        EventBus.getDefault().register(this);
        // 设置状态栏为白色，与账号信息页统一
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//                getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
//            }
//        }
        init();

        String avatarUrl = SharedPreferencesUtil.getUserAvatar();
        loadAvatarUrl(avatarUrl);
    }


    private void loadAvatarUrl(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            // 处理本地文件路径
            Object loadUrl = avatarUrl;
            if (avatarUrl.startsWith("file://")) {
                loadUrl = new File(avatarUrl.substring(7));
            }

            if (getContext() != null) {
                ImageUtil.netCircle(getContext(), String.valueOf(loadUrl), ivHead);
            }

        }
    }

    private long lastShowDialogTime = 0;

    @Override
    protected void setupObservers() {
        // 监听 ViewModel 的 LiveData
        vmChat.getChatMessages().observe(this, messages -> {
//
//            rv_chat.scrollToPosition(messages.size() - 1);
            if (messages.size() == 0) {
                if (type == TYPE_HOME) {
                    ll_empty.setVisibility(View.VISIBLE);
                }
                // 当数据变为空时也需要通知适配器刷新并清理选择状态，
                // 否则 RecyclerView 仍显示一条旧数据，导致再次删除时索引校验失败
                if (chatAdapter != null) {
                    chatAdapter.notifyDataSetChanged();
                    chatAdapter.closeSelectView();
                }
                return;
            } else {
                ll_empty.setVisibility(View.GONE);
            }
            ZUtils.print("vmChat observe = " + messages.size());
            int oldCount = chatAdapter != null ? chatAdapter.getItemCount() : 0;

            // 首次加载优化：直接刷新并瞬间滚动到底部，避免闪屏
            if (isFirstLoad && messages.size() > 0) {
                isFirstLoad = false;
                Timber.tag(TAG).d("首次加载数据，消息数量: " + messages.size());
                if (chatAdapter != null) {
                    chatAdapter.notifyDataSetChanged();
                }
                scrollToBottomWhenStable();
                return;
            }

            if (messages.size() <= 1 || oldCount == 0 || messages.size() - oldCount > 1) {
                chatAdapter.notifyDataSetChanged();
            } else {
                ChatMessage chatMessage = messages.get(messages.size() - 1);
                if (chatMessage.getMessage() == null) {
                    chatAdapter.notifyItemChanged(Math.max(0, messages.size() - 2), 0);
                }
                chatAdapter.notifyItemChanged(Math.max(0, messages.size() - 1), 0);
//                startAutoScroll(KEY_BOTTOM);
//                scroll2Last();

                if (!mIsUserScrolling && !isUserTouch) {
                    Timber.tag(TAG).d("执行滚动到底部 = mIsUserScrolling" +mIsUserScrolling);
                    scrollToBottom();
                }
                sv_chat_list.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);

            }
        });

        vmChat.getBenefitErrorLiveData().observe(
                getViewLifecycleOwner(),
                msg -> BillDialogHelper.showBillDialog(
                        requireActivity(),
                        msg
                )
        );

        vmChat.getTestCot().observe(this, s -> {
            if (TextUtils.isEmpty(s)) {
                return;
            }
            cardView.setVisibility(View.VISIBLE);
            if (s.startsWith("[") || s.startsWith("{")) {
//                    cardView.loadView(s);
            } else {
                cardView.appendStreamingData(s);
            }
        });
        vmChat.getDeepResearchStreamEnd().observe(this, end -> {
            if (end) {
                ll_stop.setVisibility(View.GONE);
            } else {
                ll_stop.setVisibility(View.VISIBLE);
            }
        });
        vmChat.getAiResponse().observe(this, response -> aiResponse.setText(response));
        vmChat.getLoading().observe(this, isLoading -> {
            // 可根据 isLoading 显示/隐藏 loading UI
        });
        vmChat.getStreamEnd().observe(this, end -> {
            if (end) ll_stop.setVisibility(View.GONE);
            if (chatAdapter != null) {
                if (agentType == TabEntity.TabType.TRIP_AI_WRITING) {
                    chatAdapter.notifyItemChanged(Math.max(0, vmChat.getChatMessages().getValue().size() - 1), 0);
                } else {
                    chatAdapter.notifyDataSetChanged();
                }
            }
        });
        vmChat.getThinkStatus().observe(this, status -> {
            Timber.tag(TAG).d(" 执行滚动 = %s", status);
            if (status == Constant.ThinkState.START) {
                if (getActivity() instanceof AgentContainActivity && Objects.equals(vmChat.getSelectAgentBean().getModelName(), AGENT_GUI)) {
                    ll_stop.setVisibility(View.GONE);
                } else {
                    ll_stop.setVisibility(View.VISIBLE);
                    ZUtils.startService(getActivity());
                }
                ll_resend.setVisibility(View.GONE);
            } else if (status == Constant.ThinkState.THINKING) {
//                ll_stop.setVisibility(View.VISIBLE);
            } else if (status == Constant.ThinkState.END) {
                ll_stop.setVisibility(View.GONE);
                isUserTouch = false;
                if (getActivity() instanceof MainActivity) {
                    mHandler.postDelayed(this::scrollToBottom, 200);
                } else if (getActivity() instanceof AgentContainActivity) {
                    mHandler.postDelayed(this::scrollToBottom, 800);
                }

                ZUtils.stopService(getActivity());
            }

        });

        vmChat.getIsAutoPlay().observe(this, isAutoPlay -> {


            if (isAutoPlay) {
                iv_top_play.setImageResource(R.mipmap.chat_top_play);
            } else {
                iv_top_play.setImageResource(R.mipmap.chat_top_mute);
                TTSManager.Companion.getInstance().stop();
                stopMediaPlay();
            }
            SharedPreferencesUtil.saveBoolean(Constants.KEY_IS_AUTO, isAutoPlay);

        });
        // 其他 LiveData 监听可按需添加
        vmChat.getConversationId().observe(this, id -> {
            if (superEditUtil != null) {
                superEditUtil.conversationId = id;
            }
        });

        // 观察流式摘要内容变化（保存 Observer 引用以便 onDestroyView 时移除，防止内存泄漏）
        if (vmMeetingSummary != null) {
            // 先移除旧观察者，避免重复注册
            if (summaryStreamObserver != null) {
                vmMeetingSummary.getSummaryStreamContent().removeObserver(summaryStreamObserver);
            }
            if (summaryProgressObserver != null) {
                vmMeetingSummary.getSummaryProgress().removeObserver(summaryProgressObserver);
            }

            summaryStreamObserver = content -> {
                Timber.tag(TAG).d("观察者被触发 - 内容长度: " + (content != null ? content.length() : 0));
                ZUtils.print("vmMeetingSummary = " + vmChat.getChatMessages().getValue().size());
                ZUtils.print("vmMeetingSummary = " + content);
                List<ChatMessage> list = vmChat.getChatMessages().getValue();
                if (list == null) return;

                if (list.size() == 0) {
                    ChatMessage aiMessage = vmChat.addAIMsg();
                    aiMessage.setThinkMessage("");
                    aiMessage.setStatus(Constant.ThinkState.THINKING);
                } else {
                    int lastIndex = list.size() - 1;
                    ChatMessage aiMessage = list.get(lastIndex);
                    aiMessage.setThinkMessage("");
                    aiMessage.setMessage(content);
                    aiMessage.setStatus(Constant.ThinkState.THINKING);

                    // 使用适配器的流式增量更新，避免全量postValue导致闪烁
                    if (chatAdapter != null) {
                        chatAdapter.updateStreamingContent(lastIndex, content, true);
                    }
                    // 流式更新时强制滚动到底部，确保用户能看到最新内容
                    scroll2Last(true);
                }
            };
            vmMeetingSummary.getSummaryStreamContent().observeForever(summaryStreamObserver);

            summaryProgressObserver = progress -> {
                ZUtils.print("vmMeetingSummary = progress = " + progress);
                MutableLiveData<List<ChatMessage>> chatMessages = vmChat.getChatMessages();
                int length = chatMessages.getValue().size();
                ChatMessage aiMessage = chatMessages.getValue().get(length - 1);

                ZUtils.print("vmMeetingSummary = progress getMessage = " + aiMessage.getMessage());

                String message = aiMessage.getMessage();
                if (message.contains("code")) {
                    try {
                        JSONObject jsonObject = new JSONObject(message);
                        int code = jsonObject.getInt("code");
                        String msg = jsonObject.getString("msg");
                        if ( BenefitCode.isBenefitError(String.valueOf(code))) {
                            long now = System.currentTimeMillis();

                            if (now - lastShowDialogTime < 3000) {
                                return;
                            }

                            lastShowDialogTime = now;

                            BillDialogHelper.showBillDialog(
                                    requireActivity(),
                                    msg,
                                    () -> requireActivity().finish()
                            );
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    if (progress == 100) {
                        aiMessage.setStatus(Constant.ThinkState.END);
                        if (chatAdapter != null) {
                            chatAdapter.notifyDataSetChanged();
                        }
                        scroll2Last(true);
                    } else {
                        aiMessage.setStatus(Constant.ThinkState.THINKING);
                    }
                }
            };
            vmMeetingSummary.getSummaryProgress().observeForever(summaryProgressObserver);
        }
        if (vmUserProfile != null) {
            vmUserProfile.getAvatarUrl().observe(getViewLifecycleOwner(), avatarUrl -> {
                        loadAvatarUrl(avatarUrl);
//                        ImageUtil.netCircle(getActivity(), avatarUrl, ivHead);
                        if (chatAdapter != null) {
                            chatAdapter.notifyItemChanged(0);
                        }
                    }

            );
            vmUserProfile.getUsernameMulti().observe(getViewLifecycleOwner(), name -> {
                        Timber.tag("TAG").e("getUsernameMulti = " + name);
                        ZUtils.print("vmUserProfile getUsernameMulti = " + name);
                        setHiText(name);
                    }
            );
        }
        Timber.tag("TAG").e("getUsernameMulti = " + vmAccountInfo);
        ZUtils.print("getUsernameMulti = " + vmAccountInfo);
        if (vmAccountInfo != null) {
            vmAccountInfo.getUsernameMulti().observe(getViewLifecycleOwner(), name -> {
                        Timber.tag("TAG").e("getUsernameMulti = " + name);
                        ZUtils.print("vmAccountInfo getUsernameMulti = " + name);
                        setHiText(name);
                    }
            );
        }
    }


    float lastX;
    float lastY;

    @SuppressLint("ClickableViewAccessibility")
    private void setChatRv() {
        rv_chat.setItemAnimator(null);
        rv_chat.setLayoutManager(new ChatLinearLayoutManager(getActivity()));
        chatAdapter = new ChatAdapter(getActivity(), vmChat.getChatMessages().getValue());
        // 设置消息删除回调
        chatAdapter.setMessageActionCallback(new ChatAdapter.OnMessageActionCallback() {
            @Override
            public void onDeleteMessage(long messageId,boolean isNeedCallBack) {
                // 通过ViewModel删除消息

                if (vmChat != null) {
                    if (type == TYPE_DRAWING) {
                        vmChat.deleteImage(messageId);
                    } else {
                        if (!isNeedCallBack){//不需要回调，重新发送消息
                            vmChat.deleteMessage(messageId,null);
                            return;
                        }
                        vmChat.deleteMessage(messageId, new ChatRepository.Callback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean data) {
                                if (!data) {
                                    return;
                                }
                                List<ChatMessage> list = vmChat.getChatMessages().getValue();
                                if (list != null && list.isEmpty()) {
                                    showAgentGuide();
                                } else if (list != null && list.size() <= 2) {
                                    ChatMessage message = list.get(0);
                                    if (message.getMsgType() == ChatAdapter.TYPE_USER_HEAD_AGENT) {
                                        viewModel.deleteConversation(id, null);
                                    }
                                }
                            }

                            @Override
                            public void onError(String error) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onDeleteSuccessCallBack() {//删除记录回调
                List<ChatMessage> list = vmChat.getChatMessages().getValue();
                if (list.isEmpty()) {
                    showAgentGuide();
                } else if (list != null && list.size() <= 2) {
                    ChatMessage message = list.get(0);
                    if (message.getMsgType() == ChatAdapter.TYPE_USER_HEAD_AGENT) {
                        viewModel.deleteConversation(id, null);
                    }
                }
            }
        });

        chatAdapter.setCallback(new MsgActionCallback() {
            @Override
            public void refresh(String content) {
                if (type == TYPE_MEETING) {
                    if (meetingMap != null) {

                        String transcriptionResult = (String) meetingMap.get(MeetingSummaryFragment.ARG_TRANSCRIPTION_RESULT);
                        int meetingIdInt = (int) meetingMap.get(MeetingSummaryFragment.ARG_MEETING_ID);
                        String botKey = (String) meetingMap.get(MeetingSummaryFragment.ARG_BOTKEY);
                        vmMeetingSummary.forceRegenerateSummary(transcriptionResult, meetingIdInt, botKey);
//                        vmMeetingSummary.generateMeetingSummaryStream(transcriptionResult,meetingIdInt,botKey);
                    }
                } else {
                    vmChat.refreshSendMessage(content, type);
                }
            }


            @Override
            public void refreshTranslation(String content, String fromLanguage, String toLanguage) {
                vmChat.sendTranslateMessage(content, fromLanguage, toLanguage);
            }

            @Override
            public void msgClick() {
                ZInputMethod.closeInputMethod(getActivity(), rv_chat);
            }

            @Override
            public void continueDrawing(ChatMessage message) {
                continueDrawings(message);
            }

            @Override
            public void regenerateDrawing(ChatMessage message) {
                String prompt = message.getMessage();
                vmChat.sendDrawingMessage(message.getDrawingImageDto()); // 直接显示原始prompt
//                vmChat.updateAIDrawingMsg(message.getDrawingImageDto());
                Toast.makeText(getActivity(), "正在重新生成图片...", Toast.LENGTH_SHORT).show();

                // 自动滚动到底部，延迟执行以等待新消息添加到列表
                rv_chat.postDelayed(() -> {
                    if (vmChat.getChatMessages().getValue() != null && vmChat.getChatMessages().getValue().size() > 0) {
                        scroll2Last(true);
                    }
                }, 300);
            }

            @Override
            public void downloadDrawing(ChatMessage message) {
                DrawingActionUtils.performDownload(getActivity(), message.getUrl());
            }

            @Override
            public void viewDrawing(ChatMessage message) {
                currentViewChatMessage = message;
                Intent intent = new Intent(getActivity(), DrawingImageViewerActivity.class);
                intent.putExtra("image_url", message.getUrl());
                DrawingImageDto drawImageDto = vmChat.getGeneratedImage().getValue();
                if (drawImageDto != null) {
                    String prompt = drawImageDto.getPrompt();
                    if (prompt == null || prompt.isEmpty()) {
                        prompt = vmChat.getSelectDrawingToChatBean().getPrompt();
                    }
                    intent.putExtra("prompt", prompt);
                    startActivityForResult(intent, REQUEST_DRAWING_VIEW);
                }

            }

            @Override
            public void sendMsg(String message) {
                MsgActionCallback.super.sendMsg(message);
                getCatDetailListBean selectAgentBean = vmChat.getSelectAgentBean();
                if (selectAgentBean != null) {//判断是否是gui 点击发送数据
                    if (Objects.equals(selectAgentBean.getModelName(), AGENT_GUI)) {
                        vmChat.sendAgentMessage(message);
                    }
                    return;
                }

                // 检查消息是否为空或只包含空格、换行符等空白字符
                if (!message.isEmpty() && !message.trim().isEmpty()) {
                    if (selectOptionModel != null) {
                        ZUtils.print("选中模型: " + selectOptionModel.getName() + ", ID: " + selectOptionModel.getId() + "  发送内容=" + message);
                        vmChat.sendMessage(message);
                    }
                } else {
                    // 显示提示信息
                    GlobalToast.show(getActivity(), "请输入有效内容", GlobalToast.Type.ERROR);
                }
            }
        });
        chatAdapter.setShareListener(mShareClickListener);
        rv_chat.setAdapter(chatAdapter);
        rv_chat.setNestedScrollingEnabled(false);
        rv_chat.setOnTouchListener((view, motionEvent) -> {
            ZUtils.print("rv_chat motionEvent = " + motionEvent.getAction());
            ZUtils.print("rv_chat mChat.getStreamEnd().getValue() = " + vmChat.getStreamEnd().getValue());
            ZInputMethod.closeInputMethod(getActivity(), view);

            chatAdapter.closeSelectView();
//
//            if(type == TYPE_DRAWING){
//                return false;
//            }

            boolean canScroll = rv_chat.canScrollVertically(1) || rv_chat.canScrollVertically(-1);
            float rawX = motionEvent.getRawX();
            float rawY = motionEvent.getRawY();
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                if (!vmChat.getStreamEnd().getValue()) {
//                    if (canScroll) {
//                        Log.d("RecyclerView", "内容可滚动，说明内容高度大于 RecyclerView 高度");
//                        Timber.tag("test ui").d("内容可滚动，说明内容高度大于 RecyclerView 高度 iv_scroll_down visible");
//                        iv_scroll_down.setVisibility(View.VISIBLE);
//                    }
                    isUserTouch = true;
                }

                lastX = motionEvent.getRawX();
                lastY = motionEvent.getRawY();
            } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                Timber.tag("RecyclerView1").d("lastY - rawY " + (lastY - rawY));
                Timber.tag("RecyclerView1").d("lastY " + (lastY));
                Timber.tag("RecyclerView1").d("  rawY " + (rawY));
                if (lastY - rawY > 0) {//向上滑，认为用户主动浏览历史，禁止自动跳转
                    // 保持提示按钮策略，由上层逻辑控制显示
                    isUserTouch = true;
                } else {
//                    if (canScroll && lastY - rawY < -25) {
//                        Timber.tag("RecyclerView1").d( "内容可滚动，说明内容高度大于 RecyclerView 高度");
//                        Timber.tag("test ui").d("内容可滚动，说明内容高度大于 RecyclerView 高度 iv_scroll_down visible");
//                        iv_scroll_down.setVisibility(View.VISIBLE);
//                    }
                    // 仍在交互，维持手势状态，避免自动跳转
                    isUserTouch = true;
                }
            }
            return false;
        });

        sv_chat_list.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ZUtils.print("rv_chat motionEvent = " + motionEvent.getAction());
                ZUtils.print("rv_chat mChat.getStreamEnd().getValue() = " + vmChat.getStreamEnd().getValue());
                stopAutoScroll(KEY_HISTORY);
                ZInputMethod.closeInputMethod(getActivity(), view);

                chatAdapter.closeSelectView();

                boolean canScroll = sv_chat_list.canScrollVertically(1) || sv_chat_list.canScrollVertically(-1);
                float rawX = motionEvent.getRawX();
                float rawY = motionEvent.getRawY();
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!vmChat.getStreamEnd().getValue()) {
//                        if (canScroll) {
//                            Timber.tag("RecyclerView4").d( "内容可滚动，说明内容高度大于 RecyclerView 高度");
//                            Timber.tag("test ui").d("内容可滚动，说明内容高度大于 RecyclerView 高度 iv_scroll_down visible");
//                            iv_scroll_down.setVisibility(View.VISIBLE);
//                        }
                        isUserTouch = true;
                    }

                    lastX = motionEvent.getRawX();
                    lastY = motionEvent.getRawY();
                    Timber.tag("RecyclerView4").d("ACTION_DOWN lastY " + (lastY));
                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    Timber.tag("RecyclerView4").d("lastY - rawY " + (lastY - rawY));
                    Timber.tag("RecyclerView4").d("lastY " + (lastY));
                    Timber.tag("RecyclerView4").d("  rawY " + (rawY));
                    if (lastY - rawY > 0) {//向上滑，认为用户主动浏览历史，禁止自动跳转
                        // 保持提示按钮策略，由上层逻辑控制显示
                        isUserTouch = true;
                    } else {
//                        if (canScroll && lastY - rawY < -25) {
//                            Timber.tag("RecyclerView4").d( "内容可滚动，说明内容高度大于 RecyclerView 高度");
//                            Timber.tag("test ui").d("内容可滚动，说明内容高度大于 RecyclerView 高度 iv_scroll_down visible");
//                            iv_scroll_down.setVisibility(View.VISIBLE);
//                        }
                        // 仍在交互，维持手势状态，避免自动跳转
                        isUserTouch = true;
                    }
                }

                return false;
            }
        });

        chatAdapter.setType(type);
        if (type == TYPE_AGENT) {
            chatAdapter.setReplyClickListener(reply -> {
                getActivity().runOnUiThread(() -> {
                    if (!vmChat.getStreamEnd().getValue()) {
                        ZUtils.showToast("正在输出，稍后。。");
                        return;
                    }
                    if (vmChat.getSelectAgentBean().getModelName().equals("深度研究")) {
                        ll_resend.setVisibility(View.GONE);
                        if (vmChat.isDeepResearchStreaming) {
                            Timber.tag(TAG).d("isDeepResearchStreaming %s", vmChat.isDeepResearchStreaming);
                            ZUtils.showToast("正在输出，稍后。。");
                            return;
                        }
                    }

                    vmChat.sendAgentMessage(reply);
                });
            });
        }

        // 性能优化：根据设备性能配置 RecyclerView 缓存
        optimizeRecyclerViewCache();
    }

    /**
     * 优化 RecyclerView 缓存配置
     * 根据设备性能自动调整缓存大小，提升滚动性能
     */
    private void optimizeRecyclerViewCache() {
        try {
            // 导入设备性能配置工具类
            DevicePerformanceConfig.init(requireActivity().getApplication());

            // 获取根据设备性能调整的缓存大小
            int cacheSize = DevicePerformanceConfig.getRecyclerCacheSize();
            int viewCacheSize = DevicePerformanceConfig.getRecyclerViewCacheSize();

            // 设置 RecyclerView 缓存池大小
            RecyclerView.RecycledViewPool pool = rv_chat.getRecycledViewPool();
            if (pool != null) {
                // 为每种 ViewType 设置缓存大小
                pool.setMaxRecycledViews(ChatAdapter.TYPE_USER, cacheSize);
                pool.setMaxRecycledViews(ChatAdapter.TYPE_AI, cacheSize);
                pool.setMaxRecycledViews(ChatAdapter.TYPE_USER_HEAD_HOME, cacheSize);
                pool.setMaxRecycledViews(ChatAdapter.TYPE_USER_HEAD_AGENT, cacheSize);
                pool.setMaxRecycledViews(ChatAdapter.TYPE_USER_HEAD_MEETING, cacheSize);
                pool.setMaxRecycledViews(ChatAdapter.TYPE_AI_DRAWING, cacheSize);
                pool.setMaxRecycledViews(ChatAdapter.TYPE_USER_FILE, cacheSize);
                pool.setMaxRecycledViews(ChatAdapter.TYPE_USER_FILE_IMAGE, cacheSize);
                pool.setMaxRecycledViews(ChatAdapter.TYPE_ASSISTANT_IMG, cacheSize);
                pool.setMaxRecycledViews(ChatAdapter.TYPE_ASSISTANT_CARD, cacheSize);
                pool.setMaxRecycledViews(ChatAdapter.TYPE_DEEP_RESEARCH, cacheSize);
                pool.setMaxRecycledViews(ChatAdapter.TYPE_DEEP_RESEARCH_COMPLETE, cacheSize);
                pool.setMaxRecycledViews(ChatAdapter.TYPE_NETWORK_ERROR, cacheSize);

                Timber.tag(TAG).d("RecyclerView 缓存池大小设置为: %d", cacheSize);
            }

            // 设置 ViewHolder 缓存数量
            rv_chat.setItemViewCacheSize(viewCacheSize);
            Timber.tag(TAG).d("RecyclerView ViewHolder 缓存数量设置为: %d", viewCacheSize);

            // 打印性能配置摘要
            String configSummary = DevicePerformanceConfig.getConfigSummary();
            Timber.tag(TAG).d("性能配置: %s", configSummary);

        } catch (Exception e) {
            Timber.tag(TAG).e(e, "优化 RecyclerView 缓存失败");
        }
    }

    private void requestDataFunctionRv() {
        HttpRequest request = new HttpRequest();
        request.getMenuList(new Observer<ApiResponse<List<GetMenuBean>>>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(ApiResponse<List<GetMenuBean>> res) {
                if (res.getCode() == 0) {
                    List<GetMenuBean> listApiResponse = res.getData();
                    for (GetMenuBean getMenuBean : listApiResponse) {
                        if ("灵犀智能体".equals(getMenuBean.getName())) {
                            request.getCatDetailList(getMenuBean.getId(), new Observer<ApiResponse<List<getCatDetailListBean>>>() {
                                @Override
                                public void onSubscribe(Disposable d) {
                                }

                                @Override
                                public void onNext(ApiResponse<List<getCatDetailListBean>> res) {
                                    if (res.getCode() == 0) {
                                        List<getCatDetailListBean> listApiResponse = res.getData();
                                        List<ChatFunctionBean> list = new ArrayList<>();
                                        for (getCatDetailListBean getCatDetailListBean : listApiResponse) {
                                            if (Constants.AGENT_TRAVEL.equals(getCatDetailListBean.getModelName())) {
                                                list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_TRAVEL, R.drawable.ic_trive, "出行规划", getCatDetailListBean));
                                            } else if (AGENT_GUI.equals(getCatDetailListBean.getModelName())) {
                                                list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_GUI, R.mipmap.ic_voice, "自动执行", getCatDetailListBean));
                                            } else if (Constants.AGENT_TRIP.equals(getCatDetailListBean.getModelName())) {
                                                list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_PART, R.drawable.ic_part, "同城聚餐", getCatDetailListBean));
                                            } else if (Constants.AGENT_MGVIDOE.equals(getCatDetailListBean.getModelName())) {
                                                list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_PART, R.mipmap.ic_mgvideo, "咪咕视频", getCatDetailListBean));
                                            } else if (Constants.AGENT_FINANCE.equals(getCatDetailListBean.getModelName())) {
                                                list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_PART, R.mipmap.ic_finance, "金融领域助手", getCatDetailListBean));
                                            } else if (Constants.AGENT_COMMUNICATION.equals(getCatDetailListBean.getModelName())) {
                                                list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_PART, R.mipmap.ic_conmunication, "通信助手", getCatDetailListBean));
                                            } else if (Constants.AGENT_DEEP_RESEARCH.equals(getCatDetailListBean.getModelName())) {
                                                list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_PART, R.mipmap.ic_deep_research, "深度研究", getCatDetailListBean));
                                            }
                                        }
                                        setFunctionRv(list);
                                    }
                                }

                                @Override
                                public void onError(Throwable e) {
                                }

                                @Override
                                public void onComplete() {
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });

    }

    /**
     * 初始化网络监听
     */
    private void initNetwork() {

        networkMonitor = new NetworkMonitor(getContext());

        networkMonitor.register(new NetworkMonitor.Listener() {

            @Override
            public void onNetworkAvailable() {

                getActivity().runOnUiThread(() -> {
                    if (selectOptionModel == null && type == TYPE_HOME) {
                        // 网络恢复后仅刷新数据，不可重新调用 init()
                        // init() 会重复 findViewById + EventBus 注册 + 滚动监听绑定，导致 ANR/崩溃
                        refreshChatHistory();
                    }
                });
            }

            @Override
            public void onNetworkLost() {
                getActivity().runOnUiThread(() -> {
                });
            }
        });
    }

    /**
     * 刷新功能列表，在模型切换时调用
     */
    public void refreshFunctionList() {
        if (type == TYPE_HOME && rv_function != null) {
            // 重新请求智能体数据，这会重新设置完整的功能列表
            requestDataFunctionRv();
        }
        closeTranslateView();
    }

    private void closeTranslateView() {
        if (superEditAITranslateUtil == null) return;
        if (superEditAITranslateUtil.getIv_close() == null) return;
        superEditAITranslateUtil.getIv_close().callOnClick();
    }

    /**
     * 检查当前选中的模型是否为豆包模型
     *
     * @return true 如果是豆包模型，false 否则
     */
    private boolean isDoubaoModel() {
        try {
            // 优先检查本地变量 selectOptionModel
            if (selectOptionModel != null) {
                String modelCode = selectOptionModel.getModel();
                String modelName = selectOptionModel.getName();

                return isDoubaoModelByCodeOrName(modelCode, modelName);
            }

            // 使用 GlobalSettings 获取当前选中模型
            GlobalSettings settings = GlobalSettings.getInstance();
            if (settings != null) {
                String modelCode = settings.getSelectedModelCode();
                String modelName = settings.getSelectedModelName();

                return isDoubaoModelByCodeOrName(modelCode, modelName);
            }
        } catch (Exception e) {
            Timber.tag(TAG).w("检查豆包模型时出现异常: " + e.getMessage());
        }

        // 默认情况下不是豆包模型
        return false;
    }

    /**
     * 根据模型代码和名称判断是否为豆包模型
     */
    private boolean isDoubaoModelByCodeOrName(String modelCode, String modelName) {
        if (modelCode != null) {
            // 检查模型代码
            if ("bot-20250715145055-hks84".equals(modelCode) || "doubao".equals(modelCode)) {
                return true;
            }
        }

        if (modelName != null) {
            // 检查模型名称
            if ("豆包大模型".equals(modelName) || "豆包".equals(modelName)) {
                return true;
            }
        }

        // 默认情况下不是豆包模型
        return false;
    }

    private void setFunctionRv(List<ChatFunctionBean> list) {
        // 只有在豆包模型时才添加深度思考功能
        try {
            if (isDoubaoModel()) {
                list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_THINK, R.drawable.ic_think, "深度思考"));
            }
        } catch (Exception e) {
            // 如果判断豆包模型时出现异常，记录日志但不影响其他功能
            Timber.tag(TAG).w("判断豆包模型时出现异常: " + e.getMessage());
        }
//        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_LIFE, R.mipmap.ic_life, "生活"));
//        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_PHONE, R.mipmap.ic_phone, "通话"));
//        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_WORK, R.mipmap.ic_work, "办公"));
//        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_PLAY, R.mipmap.ic_play, "娱乐"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_AI_WRITE, R.mipmap.ic_ai_write, "AI写作"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_AI_TRANSLATE, R.mipmap.ic_ai_translate, "AI翻译"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_PPT, R.mipmap.ic_ppt, "PPT生成"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_AI_PIC, R.mipmap.ic_ai_pic, "AI绘画"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_AI_MEETING, R.mipmap.ic_ai_meeting, "AI会议"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_VOICE, R.mipmap.ic_voice, "同声传译"));
//        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_GUI, R.mipmap.ic_voice, "GUI"));
        rv_function.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.HORIZONTAL, false));
        chatFunctionAdapter = new ChatFunctionAdapter(getActivity(), list, position -> {
            ChatFunctionBean bean = list.get(position);
            if (bean.getId() == Constant.ChatFunction.TYPE_LIFE) {
                // 生活
            } else if (bean.getId() == Constant.ChatFunction.TYPE_PHONE) {
                // 通话
            } else if (bean.getId() == Constant.ChatFunction.TYPE_WORK) {
                // 办公
            } else if (bean.getId() == Constant.ChatFunction.TYPE_PLAY) {
                // 娱乐
            } else if (bean.getId() == Constant.ChatFunction.TYPE_AI_WRITE) {
                // AI写作
                if (!NetworkUtils.isNetworkAvailable(getContext())) {
                    Toast.makeText(getActivity(), "网络链接不可用，请检查网络连接", Toast.LENGTH_SHORT).show();
                    return;
                }
                ll_bottom.setVisibility(View.INVISIBLE);
                ll_edit_writing.setVisibility(View.VISIBLE);
//                vmChat.getConversationId().postValue(0l);
                setBottomAIWritingEdit();
            } else if (bean.getId() == Constant.ChatFunction.TYPE_AI_TRANSLATE) {
                // AI翻译
                if (!NetworkUtils.isNetworkAvailable(getContext())) {
                    Toast.makeText(getActivity(), "网络链接不可用，请检查网络连接", Toast.LENGTH_SHORT).show();
                    return;
                }
//                vmChat.getConversationId().postValue(0l);
                ll_bottom.setVisibility(View.GONE);
                ll_edit_translate.setVisibility(View.VISIBLE);
                setBottomTranslateEdit();
            } else if (bean.getId() == Constant.ChatFunction.TYPE_PPT) {
                // PPT 生成
                startActivity(new Intent(getActivity(), PptTopicInputActivity.class));
            } else if (bean.getId() == Constant.ChatFunction.TYPE_AI_PIC) {
                // AI绘画
                startActivity(new Intent(getActivity(), DrawingSelectActivity.class));
            } else if (bean.getId() == Constant.ChatFunction.TYPE_AI_MEETING) {
                // AI 会议
                startActivity(new Intent(getActivity(), MeetingContainActivity.class));
            } else if (bean.getId() == Constant.ChatFunction.TYPE_VOICE) {
                // 同声传译
                startActivity(new Intent(getActivity(), SimultaneousTranslateActivity.class));
            } else if (bean.getId() == Constant.ChatFunction.TYPE_TRAVEL || bean.getId() == Constant.ChatFunction.TYPE_PART) {
                // 智能体胶囊：出行规划、同城聚餐、咪咕视频、金融领域助手、通信助手、深度研究
                getCatDetailListBean getCatDetailListBean = bean.getCatDetailListBean();
                Intent intent = new Intent(requireContext(), AgentContainActivity.class);
                intent.putExtra(Constant.INTENT_TYPE, AgentContainActivity.TYPE_AGENT);
                intent.putExtra(Constant.INTENT_DATA2, getCatDetailListBean);
                requireContext().startActivity(intent);
            } else if (bean.getId() == Constant.ChatFunction.TYPE_THINK) {
                // 深度思考：切换选中状态
                boolean newSelected = !chatFunctionAdapter.isThinkSelected();
                chatFunctionAdapter.setThinkSelected(newSelected);
                chatFunctionAdapter.notifyDataSetChanged();
                // 通知 ViewModel 选择（用于后续请求参数）
                vmChat.setThinkingModeEnabled(newSelected);
            } else if (bean.getId() == Constant.ChatFunction.TYPE_GUI) {
                getCatDetailListBean guiBean = bean.getCatDetailListBean();
                Intent intent = new Intent(requireContext(), AgentContainActivity.class);
                intent.putExtra(Constant.INTENT_TYPE, AgentContainActivity.TYPE_AGENT);
                intent.putExtra(Constant.INTENT_DATA2, guiBean);
                requireContext().startActivity(intent);
            }
        });
        rv_function.setAdapter(chatFunctionAdapter);
    }

    private void setBottomEdit() {
        TabEntity.agentType = TabEntity.TabType.CHAT;
        ll_edit_main.setVisibility(View.VISIBLE);
        superEditUtil = new SuperEditUtil(getActivity(), ll_edit_main);
//        // 进入对话页即尝试申请相册读取权限（按需）
//        superEditUtil.checkReadFilePermission();
        showVoiceAnimate();
        superEditUtil.setOnListenSoft(root_view, new SoftCallback() {
            @Override
            public void show() {
                stopAutoScroll(KEY_HISTORY);
                // 底部导航的显隐交给 MainActivity 全局监听处理，避免重复触发导致布局抖动
                if (!isSoft) {
//                    isUserTouch = false;
//                    scroll2Last(true);
                    // 空态界面时不执行自动滚动，避免文案随键盘抖动
                    if (ll_empty == null || ll_empty.getVisibility() != View.VISIBLE) {
//                        scrollToBottom();
                    }
                }
                isSoft = true;
            }

            @Override
            public void hide() {
                stopAutoScroll(KEY_HISTORY);
                if (type == TYPE_HOME) {//首页
//                    rv_function.setVisibility(View.VISIBLE);
                }
                if (isSoft) {
//                    isUserTouch = false;
//                    scroll2Last(true);
                    sv_chat_list.post(() -> {
                        // 空态界面时不执行自动滚动，避免文案随键盘抖动
                        if (ll_empty == null || ll_empty.getVisibility() != View.VISIBLE) {
                            scrollToBottom();
                        }
                    });
                }
                isSoft = false;
            }
        });
        superEditUtil.setCallback(new SuperEditCallback() {
            @Override
            public void send(String content, OptionModel optionModel) {
                ZUtils.print("SuperChatActivity send = " + content + " OptionModel = " + optionModel.getName());
                // 检查网络连接
                if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                    showToast("当前无网络连接，请检查后重试");
                    return;
                }
                if (!vmChat.getStreamEnd().getValue()) {
                    ZUtils.showToast("正在输出，稍后。。");
                    return;
                }
                if (!content.isEmpty()) {
                    TTSManager.Companion.getInstance().stop();
                    stopMediaPlay();
                    if (selectOptionModel != null && optionModel != null) {
                        ZUtils.print("选中模型: " + selectOptionModel.getName() + ", ID: " + selectOptionModel.getId());
                        ZUtils.print("选中模型optionModel: " + optionModel.getName() + ", ID: " + optionModel.getId());
                        if (selectOptionModel.getId() != optionModel.getId()) {
                            //如果切换模型，就重置conversationId，重新建对话
//                            vmChat.getConversationId().setValue(0l);
                        }
                    }
                    selectOptionModel = optionModel;
                    vmChat.setSelectOptionModel(optionModel);
                    vmChat.sendMessage(content);

                    // 发送消息后重置新对话状态标志
                    resetNewConversationFlag();

                    scroll2Last(true);
                    scrollToBottom();
                    // 收起键盘
                    ZInputMethod.closeInputMethod(getActivity(), root_view);
                }

            }

            @Override
            public void sendWithFile(String content, OptionModel selectOptionModel, List<ChatFileBean> fileList, boolean isFile) {
                vmChat.setSelectOptionModel(selectOptionModel);
                if (content != null) {
                    List<ChatFileBean> list = new ArrayList<>();
                    list.addAll(fileList);
                    vmChat.sendMessageWithFile(content, list);

                    // 发送消息后重置新对话状态标志
                    resetNewConversationFlag();
                }
            }

            @Override
            public void voice() {
                checkAudioPermission();

            }

            @Override
            public void keyboard() {

            }

            @Override
            public void pressDown() {
                if (!vmChat.getStreamEnd().getValue()) {
                    ZUtils.showToast("正在输出，稍后。。");
                    return;
                }
                if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
//                    Timber.tag(TAG).e("superEditUtil 录音无权限");
//                    if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.RECORD_AUDIO)) {
//                        // 用户拒绝过，需要给出解释
//                        Timber.tag(TAG).e("superEditUtil 录音权限被拒绝");
//                    } else {
//                        // 直接请求权限
//                        ActivityCompat.requestPermissions(requireActivity(),
//                                new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
//                    }
                    AppPermissionRequestManager.requestAudioPermission(getActivity(), PERMISSION_REQUEST_RECORD_AUDIO, AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_ASR);
                } else {
                    // 已经有权限
                    voiceStatusHandle(PRESS_DOWN, false, false);
                }
            }

            @Override
            public void pressUp(boolean isInArea) {
                voiceStatusHandle(PRESS_UP, isInArea, false);

            }

            @Override
            public void voiceMove(boolean status) {
                SuperEditCallback.super.voiceMove(status);
                voiceStatusHandle(PRESS_MOVE, false, status);
            }

            @Override
            public void modeChange(OptionModel model, int size) {
                SuperEditCallback.super.modeChange(model, size);
                if (!isAdded()) {
                    return;
                }
                if (size > 1) {
                    ivHeaderSelectAgent.setVisibility(View.VISIBLE);
                }
                if (model != null && model.getName() != null) {
                    selectOptionModel = model;
                    vmChat.setSelectOptionModel(selectOptionModel);
                    tvHeaderSelectAgent.setText(model.getName());
                    if (model.getModel().equals(Constants.LING_XI_MODEL)) {
                        if (superEditUtil != null) {
                            superEditUtil.hideAddLingXi(View.GONE);
                        }

                        if (chatAdapter != null) {
                            chatAdapter.switchHeadCard(HomeModelEntity.ModelType.LING_XI_MODEL);
                        }
                        tvHeaderTitle.setVisibility(View.VISIBLE);
                        if (getContext() != null){
                            tvHeaderSelectAgent.setTextColor(getContext().getColor(R.color.color_1E1E1E));
                        }
//                        ViewCompat.setBackgroundTintList(ivHeaderSelectAgent, ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
                        ivHeaderSelectAgent.setImageTintList(ColorStateList.valueOf(Color.parseColor("#000000")));
                    } else {
                        if (superEditUtil != null) {
                            superEditUtil.hideAddLingXi(View.VISIBLE);
                        }
                        if (chatAdapter != null) {
                            chatAdapter.switchHeadCard(HomeModelEntity.ModelType.OTHER_MODEL);
                        }
                        tvHeaderTitle.setVisibility(View.VISIBLE);
                        if (getContext() != null){
                            tvHeaderSelectAgent.setTextColor(getContext().getColor(R.color.color_738FB4));
                        }
//                        ViewCompat.setBackgroundTintList(ivHeaderSelectAgent, ColorStateList.valueOf(android.R.color.transparent));
                        ivHeaderSelectAgent.setImageTintList(ColorStateList.valueOf(Color.parseColor("#738FB4")));

                    }
                    // 模型切换后刷新功能列表
                    refreshFunctionList();
                }
            }
        });
    }

    private void setAgentBottomEdit(getCatDetailListBean bean, String query) {
        agentType = TabEntity.TabType.TRIP_AI_AGENT;
        ll_edit_agent.setVisibility(View.VISIBLE);
        ed = ll_edit_agent.findViewById(R.id.ed);
        superAgentUtil = new SuperAgentUtil(getActivity(), ll_edit_agent, bean);
        showVoiceAnimate();
        superAgentUtil.setOnListenSoft(root_view, new SoftCallback() {
            @Override
            public void show() {

                if (!isSoft) {
                    scroll2Last(true);
                    if (ed != null) {
                        ed.postDelayed(() -> {
                            ed.requestFocus();
                            ed.setSelection(ed.getText().length());
                        }, 100);
                    }
                }
                isSoft = true;
            }

            @Override
            public void hide() {
                if (isSoft) {
                    scroll2Last(true);
                }
                isSoft = false;
            }
        });
        superAgentUtil.setCallback(new SuperEditCallback() {
            @Override
            public void send(String content, OptionModel optionModel) {
//                ZUtils.print("SuperChatActivity send = "+content + " OptionModel = "+optionModel.getName());
                stopAutoScroll(KEY_HISTORY);
                if (!content.isEmpty()) {
//                    selectOptionModel = optionModel;
//                    optionModel = new OptionModel();
//                    vmChat.setSelectOptionModel(optionModel);
                    if (type == TYPE_AGENT) {
                        if (!vmChat.getStreamEnd().getValue()) {
                            ZUtils.showToast("正在输出，稍后。。");
                            return;
                        }
                        if (vmChat.getSelectAgentBean() != null && vmChat.getSelectAgentBean().getModelName().equals("深度研究")) {
                            ll_resend.setVisibility(View.GONE);
                            if (vmChat.isDeepResearchStreaming) {
                                Timber.tag(TAG).d("isDeepResearchStreaming %s", vmChat.isDeepResearchStreaming);
                                ZUtils.showToast("正在输出，稍后。。");
                                return;
                            }
                        }
                        vmChat.sendAgentMessage(content);

                    } else if (type == TYPE_DRAWING) {

                        vmChat.sendDrawingMessage(content);
                    }

                    ZInputMethod.closeInputMethod(getActivity(), root_view);   //收起键盘
                }

            }

            @Override
            public void sendWithFile(String content, OptionModel selectOptionModel, List<ChatFileBean> fileList, boolean isFile) {
                //暂时无用
            }

            @Override
            public void voice() {
                checkAudioPermission();

            }

            @Override
            public void keyboard() {

            }

            @Override
            public void pressDown() {
                voiceStatusHandle(PRESS_DOWN, false, false);
            }

            @Override
            public void pressUp(boolean isInArea) {
                voiceStatusHandle(PRESS_UP, isInArea, false);

            }

            @Override
            public void voiceMove(boolean status) {
                SuperEditCallback.super.voiceMove(status);
                voiceStatusHandle(PRESS_MOVE, false, status);
            }
        });
        if (!TextUtils.isEmpty(query)) {
            ed.setText(query);
        }
    }

    private void setBottomAIWritingEdit() {
        agentType = TabEntity.TabType.TRIP_AI_WRITING;
        ll_stop.setVisibility(View.GONE);
        superEditAIWritingUtil = new SuperEditAIWritingUtil(getActivity(), ll_edit_writing);
        superEditAIWritingUtil.setCallback(
                new AIMeetingEditCallback() {
                    @Override
                    public void send(String content) {
                        if (selectOptionModel != null) {
                            vmChat.setSelectOptionModel(selectOptionModel);
                        }
                        vmChat.sendAIWritingMessage(content);
                        ll_bottom.setVisibility(View.INVISIBLE);
//                        ll_edit_writing.setVisibility(View.GONE);
                        agentType = TabEntity.TabType.CHAT;
                    }

                    @Override
                    public void close() {
                        agentType = TabEntity.TabType.CHAT;
                        ll_bottom.setVisibility(View.VISIBLE);
                        ll_edit_writing.setVisibility(View.GONE);
//                        vmChat.getConversationId().postValue(0l);

                        if (vmChat.getThinkStatus().getValue() != Constant.ThinkState.END) {
                            ll_stop.setVisibility(View.VISIBLE);
                        }

                        ZInputMethod.closeInputMethod(getActivity(), superEditAIWritingUtil.getIv_close());   //收起键盘
                    }

                    @Override
                    public void voice() {
                        checkAudioPermission();
                    }

                    @Override
                    public void keyboard() {

                    }

                    @Override
                    public void pressDown() {
                        voiceStatusHandle(PRESS_DOWN, false, false);

//                        checkAudioPermission();
                    }


                    @Override
                    public void pressUp(boolean isInArea) {
                        voiceStatusHandle(PRESS_UP, isInArea, false);

                    }

                    @Override
                    public void voiceMove(boolean status) {
                        AIMeetingEditCallback.super.voiceMove(status);
                        voiceStatusHandle(PRESS_MOVE, false, status);
                    }
                }
        );
    }

    private void setBottomTranslateEdit() {
        agentType = TabEntity.TabType.TRANSLATE;
        ll_stop.setVisibility(View.GONE);
        ll_edit_main.setVisibility(View.VISIBLE);
        superEditAITranslateUtil = new SuperEditAITranslateUtil(getActivity(), ll_edit_translate);
        vmChat.setSuperEditAITranslateUtil(superEditAITranslateUtil);
        showVoiceAnimate();

        superEditAITranslateUtil.setCallback(
                new AITranslateEditCallback() {

                    @Override
                    public void send(String content, String prompt) {
                    }

                    @Override
                    public void send(String content, String prompt, String fromLang, String toLang) {
                        AITranslateEditCallback.super.send(content, prompt, fromLang, toLang);
                        if (Constant.isUseLingXiTranslation) {
                            Timber.tag(TAG).d("翻译内容=" + content + "  原本语言=" + fromLang + " 需要翻译语音=" + toLang);
                            vmChat.sendTranslateMessage(content, fromLang, toLang);
                        } else {
                            vmChat.sendTranslateMessage(content, prompt);
                        }

//                        ll_bottom.setVisibility(View.VISIBLE);
//                        ll_edit_translate.setVisibility(View.GONE);
//                        agentType = TabEntity.TabType.CHAT;
                    }

                    @Override
                    public void close() {
                        agentType = TabEntity.TabType.CHAT;
                        ll_bottom.setVisibility(View.VISIBLE);
                        ll_edit_translate.setVisibility(View.GONE);
//                        vmChat.getConversationId().postValue(0l);

                        if (vmChat.getThinkStatus().getValue() != Constant.ThinkState.END) {
                            ll_stop.setVisibility(View.VISIBLE);
                        }

                        ZInputMethod.closeInputMethod(getActivity(), superEditAITranslateUtil.getIv_close());   //收起键盘
                    }

                    @Override
                    public void voice() {
                        checkAudioPermission();

                    }

                    @Override
                    public void keyboard() {

                    }

                    @Override
                    public void pressDown() {
                        voiceStatusHandle(PRESS_DOWN, false, false);

//                        checkAudioPermission();
                    }


                    @Override
                    public void pressUp(boolean isInArea) {
                        voiceStatusHandle(PRESS_UP, isInArea, false);

                    }

                    @Override
                    public void voiceMove(boolean status) {
                        AITranslateEditCallback.super.voiceMove(status);
                        voiceStatusHandle(PRESS_MOVE, false, status);
                    }
                }
        );
    }


    int lastTextViewHeight = 0;

    private void scroll2Last() {
        scroll2Last(false);
    }

    public void scroll2Last(boolean force) {
        ZUtils.print("scroll2Last isUserTouch = " + isUserTouch + ", force=" + force);
        if (!force && isUserTouch) {
            return;
        }
        ZUtils.print("scroll2Last mIsUserScrolling = " + mIsUserScrolling);
        if (mIsUserScrolling && !force) {
            return;
        }
        ZUtils.print("scroll2Last vmChat.getChatMessages().getValue().size() = " + vmChat.getChatMessages().getValue().size());
        if (vmChat.getChatMessages().getValue().size() <= 0) {
            return;
        }

        // 优先使用新的可靠滚动方法
//        if (force) {
//            // 对于强制滚动（如进入页面时），使用最可靠的方法
//            scrollToBottom();
//            iv_scroll_down.setVisibility(View.GONE);
//            return;
//        }

        LinearLayoutManager layoutManager = (LinearLayoutManager) rv_chat.getLayoutManager();
        if (layoutManager != null) {
            // 仅在接近底部时自动滚动，避免用户稍微上滑就被拉回底部
            int total = vmChat.getChatMessages().getValue().size();
            int lastVisible = layoutManager.findLastVisibleItemPosition();
            if (lastVisible < total - 2) {
                return;
            }

            // 简化滚动逻辑，直接滚动到最后位置
            rv_chat.post(() -> {
                try {
//                    rv_chat.smoothScrollToPosition(total - 1);
//                    sv_chat_list.smoothScrollTo(0, sv_chat_list.getChildAt(0).getBottom());
                    Timber.tag(TAG).d("平滑滚动到位置: " + (total - 1));
                } catch (Exception e) {
                    Timber.tag(TAG).e("平滑滚动失败，使用备用方案");
                    // 备用方案：使用NestedScrollView滚动
//                    scrollToBottom();
                }
            });
        }
        iv_scroll_down.setVisibility(View.GONE);
    }

    private void scrollWhenHolderLoad(RecyclerView.ViewHolder holder, LinearLayoutManager layoutManager) {
        if (holder instanceof ChatAdapter.ChatViewHolder) {
            ConstraintLayout textView = ((ChatAdapter.ChatViewHolder) holder).root_view;
            if (textView != null) {
                // 强制重新布局以确保高度准确
                textView.requestLayout();
                // 延迟获取高度，等待 Markdown 渲染完成
                textView.post(() -> {
                    if (isUserTouch) {
                        return;
                    }
                    if (mIsUserScrolling) {
                        return;
                    }
                    int textViewHeight = textView.getHeight();
                    ZUtils.print("TextView height: " + textViewHeight);
                    ZUtils.print("lastTextViewHeight height: " + lastTextViewHeight);
//                    Timber.tag(TAG).d( "rv_chat height: " + rv_chat.getHeight());
                    // 仅当 TextView 高度超过 RecyclerView 高度时滚动
//                    if(lastTextViewHeight == textViewHeight){
//                        return;
//                    }
//                    lastTextViewHeight = textViewHeight;
                    if (textViewHeight > rv_chat.getHeight()) {
                        layoutManager.scrollToPositionWithOffset(vmChat.getChatMessages().getValue().size() - 1, -textViewHeight);
                    } else {
                        layoutManager.scrollToPosition(vmChat.getChatMessages().getValue().size() - 1);
                    }
                });
            }
        }
    }

    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            AppPermissionRequestManager.requestAudioPermission(getActivity(), PERMISSION_REQUEST_RECORD_AUDIO, AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_ASR);
            Timber.tag(TAG).e("checkAudioPermission == 录音无权限");
        }

    }

    public void continueDrawings(ChatMessage message) {
//        DrawingImageDto currentImage = vmChat.getGeneratedImage().getValue();

        if (vmChat.getIsGenerating().getValue()) {
            ZUtils.showToast("请等待生成成功");
//            GlobalToast.show(IYAApplication.getInstance(),"请等待生成成功", GlobalToast.Type.NORMAL);
            return;
        }
        // 继续编辑
        if (message != null) {
            Intent intent = new Intent(getActivity(), DrawingActivity.class);
            intent.putExtra("continue_edit", true);
            intent.putExtra("from_chat", true);
            intent.putExtra("reference_image_url", message.getUrl());
//            intent.putExtra("original_prompt", currentImage.getPrompt());
            if (vmChat.getSelectDrawingStyleDto() != null) {
                intent.putExtra("style", vmChat.getSelectDrawingStyleDto().getName());
            }
            if (vmChat.getSelectedRatio() != null) {
                intent.putExtra("ratio", vmChat.getSelectedRatio());
            }

            Timber.tag("DrawingChatActivity").d("Starting continue edit activity for result");
            // 使用startActivityForResult等待返回结果
            startActivityForResult(intent, REQUEST_CONTINUE_EDIT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                ZUtils.showToast("需要录音权限才能使用功能");
            }
        }

        if (superEditUtil != null) {
            superEditUtil.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Timber.tag("SuperChatFragment").d("onActivityResult called - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == REQUEST_CONTINUE_EDIT && resultCode == RESULT_OK && data != null) {
            // 从继续编辑页面返回，获取用户输入的编辑要求
            String editPrompt = data.getStringExtra("edit_prompt");
            String referenceImageUrl = data.getStringExtra("reference_image_url");
            String originalPrompt = data.getStringExtra("original_prompt");
            String style = data.getStringExtra("style");
            String ratio = data.getStringExtra("ratio");
            DrawingStyleDto styleDto = (DrawingStyleDto) data.getSerializableExtra("DrawingStyleDto");

            Timber.tag("DrawingChatActivity").d("Received continue edit result:");
            Timber.tag("DrawingChatActivity").d("editPrompt: " + editPrompt);
            Timber.tag("DrawingChatActivity").d("referenceImageUrl: " + referenceImageUrl);
            Timber.tag("DrawingChatActivity").d("originalPrompt: " + originalPrompt);
            Timber.tag("DrawingChatActivity").d("style: " + style);
            Timber.tag("DrawingChatActivity").d("ratio: " + ratio);

            vmChat.setSelectDrawingStyleDto(styleDto);
            vmChat.setSelectedRatio(ratio);

            if (editPrompt != null && !editPrompt.isEmpty()) {
                // 在当前对话页面中继续编辑
                handleContinueEditInConversation(editPrompt, referenceImageUrl, originalPrompt, style);
            }
        } else if (requestCode == REQUEST_DRAWING_VIEW && resultCode == RESULT_OK) {
            //大图浏览-继续编辑
            continueDrawings(currentViewChatMessage);
        }

        if (superEditUtil != null) {
            superEditUtil.onActivityResult(requestCode, resultCode, data);
        }
        Timber.tag(TAG).d("onActivityResult requestCode=%d, resultCode=%d", requestCode, resultCode);
        if (requestCode == REQ_CODE_FLOAT_PERMISSION || requestCode == REQ_CODE_ACC_PERMISSION) {
            LingxiAskWidgetProvider.refreshAllWidgets(getActivity());
            refreshGuiPermission();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void refreshGuiPermission() {
        Timber.tag(TAG).d("refreshGuiPermission");
        if (chatAdapter != null) {
            AccessibilityApi.Companion.setWaitingPermission(false);
            // 刷新UI，按钮文字自动更新
            root_view.postDelayed(() -> {
                chatAdapter.notifyDataSetChanged();
                if (AccessibilityApi.Companion.isAllRequiredPermissionEnable(getContext())) {
                    Timber.tag(TAG).d("onActivityResult canDrawOverlays && isBaseServiceEnable true");
                    vmChat.reSendMessage();
                }
            }, 500);
        }
    }

    /**
     * 在对话页面中处理继续编辑
     */
    private void handleContinueEditInConversation(String editPrompt, String referenceImageUrl, String originalPrompt, String style) {
        Timber.tag("DrawingChatActivity").d("handleContinueEditInConversation called");

        DrawingToChatBean bean = new DrawingToChatBean();
        // 设置继续编辑模式
        vmChat.setContinueEditMode(true);

        // 设置参考图片和原始提示词
        if (referenceImageUrl != null) {
            vmChat.setReferenceImageUrl(referenceImageUrl);
            // 仅在继续编辑时同步赋值到selectDrawingToChatBean
//            DrawingToChatBean bean = vmChat.getSelectDrawingToChatBean();
            if (bean != null) {
                bean.setReference_image_url(referenceImageUrl);
            }
        }
        if (originalPrompt != null) {
            vmChat.setHiddenPrompt(originalPrompt);
        }

        // 添加用户的编辑要求消息
//        DrawingToChatBean bean = vmChat.getSelectDrawingToChatBean();
        if (bean != null) {
            bean.setPrompt(editPrompt);
        }
//        vmChat.setSelectDrawingToChatBean(bean);
        vmChat.sendDrawingMessage(editPrompt, referenceImageUrl);

        // 不再单独添加AI回复文字，将在气泡中一起显示

        // 设置新的prompt并触发生成
//        viewModel.getPrompt().set(editPrompt);

        Timber.tag("DrawingChatActivity").d("About to call generateImage()");
//        vmChat.generateImage();

        Timber.tag("DrawingChatActivity").d("Continue edit in conversation completed");
    }


    // 显示会话历史内容
    private void displaySessionHistory(DrawingSessionDto sessionDetail) {
        // 显示所有历史图片和对话
        if (sessionDetail.getAiImageList() != null && !sessionDetail.getAiImageList().isEmpty()) {
            for (DrawingImageDto imageDto : sessionDetail.getAiImageList()) {
                // 显示每个图片的提示词作为用户消息
                if (imageDto.getPrompt() != null && !imageDto.getPrompt().isEmpty()) {
                    vmChat.sendDrawingMessageHistory(imageDto.getPrompt());
                }

                // 不再单独添加AI回复文字，将在气泡中一起显示

                // 设置当前图片，以便支持继续编辑功能
                vmChat.getGeneratedImage().postValue(imageDto);
                // 直接添加结果图片，不再调用displayResult避免重复预加载
//                addResultImage(imageDto);
//                vmChat.addAIDrawingMsg();
                vmChat.updateAIDrawingMsgId(imageDto.getId());

                // 检查图片状态，如果是生成中（status=10），则继续轮询
                Integer status = imageDto.getStatus();
                if (status != null && status == 10) {
                    // 图片生成中，显示生成动画并开始轮询
                    vmChat.updateAIDrawingMsg(0); // 设置初始进度
                    vmChat.updateAIDrawingMsg(imageDto);
                    // 开始轮询图片状态
                    vmChat.startPollingImageStatus(imageDto.getId());
                } else {
                    // 图片已完成或失败，直接显示结果
                    vmChat.updateAIDrawingMsg(100);
                    vmChat.updateAIDrawingMsg(imageDto.getImageUrl());
                    vmChat.updateAIDrawingMsg(imageDto);
                }
            }

            // 如果有历史图片，获取最后一张图片的URL作为参考图片
            if (!sessionDetail.getAiImageList().isEmpty()) {
                DrawingImageDto lastImage =
                        sessionDetail.getAiImageList().get(sessionDetail.getAiImageList().size() - 1);
                if (lastImage != null && lastImage.getImageUrl() != null) {
                    String referenceImageUrl = lastImage.getImageUrl();
                    String prompt = lastImage.getPrompt();
                    vmChat.setReferenceImageUrl(referenceImageUrl);
                    vmChat.getSelectDrawingToChatBean().setPrompt(prompt);
                    vmChat.getSelectDrawingToChatBean().setReference_image_url(referenceImageUrl);

                }
            }
        }

        // 设置输入框提示文字
        if (ed != null) {
            ed.setHint("继续编辑这个会话...");
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (vmChat != null) {
            vmChat.netStateUnregister();
            vmChat.closeSSE();
            //深度研究判断条件false，可以再次发起请求
            vmChat.isStreamEnd = true;
        }
        AsrOneUtils.getInstance().removeCallBack();
        if (superEditUtil != null) {
            superEditUtil.setCallback(null);
            superEditUtil = null;
        }
        ZUtils.print("SuperChatActivity onDestroy = " + superEditUtil);

        // 清理ChatAdapter资源
        if (chatAdapter != null) {
            chatAdapter.cleanup();
        }

        if (asrManager != null) {
            asrManager.onDestroy();
            asrManager = null;
        }
        EventBus.getDefault().unregister(this);
        if (mHandler != null) mHandler.removeCallbacksAndMessages(null);
        if (sseDisposable != null && !sseDisposable.isDisposed()) sseDisposable.dispose();

        // 清理自动滚动定时器
        if (autoLoadTimer != null) {
            autoLoadTimer.cancel();
            autoLoadTimer = null;
        }

        // 清理滚动稳定检测Handler
        if (scrollStableHandler != null) {
            scrollStableHandler.removeCallbacksAndMessages(null);
            scrollStableHandler = null;
        }

        // 清理自动滚动ScheduledExecutorService（兜底，防止未正常stopAutoScroll）
        shutdownExecutor(historyExecutorService);
        historyExecutorService = null;
        shutdownExecutor(bottomExecutorService);
        bottomExecutorService = null;

    }

    private final ChatAdapter.OnShareClickListener mShareClickListener = new ChatAdapter.OnShareClickListener() {
        @Override
        public void onShareIconClick(int pos) {
            updateShareState(true, pos);
        }
    };

    // 判断adapter类型是否符合复制或导出
    private boolean containsOnlyAllowedTypes(List<ChatMessage> messages, List<Integer> allowedTypes) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getIsSelected()) {
                Log.e("测试", "测试测试选中==" + messages.get(i).getMessage());

            }
        }
        return messages.stream()
                .allMatch(msg -> allowedTypes.contains(msg.getMsgType()));
    }

    private ShareItemtAdapter.OnItemClickListener mItemClickListener = new ShareItemtAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position) {
            boolean closeBottom = true;
            ShareItem item = mShareDatas.get(position);
            mSelectMessages = chatAdapter.getSelectMessages();
            Timber.tag("SuperChatFragment").d("Selected messages count: " + mSelectMessages.size());

            // 详细日志记录选中的消息
            for (int i = 0; i < mSelectMessages.size(); i++) {
                ChatMessage msg = mSelectMessages.get(i);
                Timber.tag("SuperChatFragment").d("Message " + i + ": type=" + msg.getMsgType() +
                        ", hasContent=" + (msg.getMessage() != null) +
                        ", hasImages=" + (msg.getImageList() != null && !msg.getImageList().isEmpty()));
            }

            if (mSelectMessages.isEmpty() || mSelectMessages.size() == 0) {
                ZUtils.showToast("请选择要分享的内容");
                return;
            }

            if (item == ShareItem.COPY_LINK) {
                handleCopyLink(false);
            } else if (item == ShareItem.WECHAT) {
                CommonDialog.showConfirmDialog(
                        getActivity(),
                        "即将打开微信",
                        "您将跳转至微信登录/分享页面，是否继续？",
                        "允许",
                        new CommonDialog.OnDialogClickListener() {
                            @Override
                            public void onConfirm() {
                                handleWechat();
                            }

                            @Override
                            public void onCancel() {
                            }
                        });
            } else if (item == ShareItem.WECHAT_MOMENT) {
                shareImageToWeChatMoments(BitmapFactory.decodeResource(getActivity().getResources(), R.mipmap.share_long_pic));
            } else if (item == ShareItem.LONG_PIC) {
                handleLongPic();
                closeBottom = false;
            } else if (item == ShareItem.COPY_TEXT) {

                // 定义允许的类型集合
                List<Integer> allowedTypes = Arrays.asList(
                        ChatAdapter.TYPE_USER,
                        ChatAdapter.TYPE_ASSISTANT_IMG,
                        ChatAdapter.TYPE_AI
                );

                if (!containsOnlyAllowedTypes(mSelectMessages, allowedTypes)) {
                    GlobalToast.show(requireActivity(), "暂不支持复制卡片类型", GlobalToast.Type.ERROR);
                    return;
                } else {
                    String copyText = getSelectText();
                    ZUtils.copy(getActivity(), copyText);
                }

            } else if (item == ShareItem.SHARE_FILE) {

                // 定义允许的类型集合
                List<Integer> allowedTypes = Arrays.asList(
                        ChatAdapter.TYPE_USER,
                        ChatAdapter.TYPE_AI
                );

                if (!containsOnlyAllowedTypes(mSelectMessages, allowedTypes)) {
                    GlobalToast.show(requireActivity(), "暂不支持导出卡片及图片类型", GlobalToast.Type.ERROR);
                    return;
                } else {
                    handleExportFile();
                }

            } else if (item == ShareItem.SHARE_DELETE) {
                chatAdapter.deleteSelectData();
            } else if (item == ShareItem.SHARE_SETTING) {
                handleCopyLink(true);
            } else if (item == ShareItem.SAVE_PIC) {
                handleSavePic();
            }
            if (closeBottom) {
                updateShareState(false);
                mLongPicLayout.setVisibility(View.GONE);
            }
        }
    };

    private String getSelectText() {
        String copyText = "";

        for (ChatMessage message : mSelectMessages) {
            if (message.getMsgType() == ChatAdapter.TYPE_USER) {
                copyText += "用户：" + "\n" + message.getMessage().trim() + "\n\n";
            } else {
                if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_IMG) {
                    copyText += "灵犀：" + "\n" + message.getImageList().get(0) + "\n\n";
                }
                if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_IMG) {
                    copyText += "灵犀：" + "\n" + message.getImageList().get(0) + "\n\n";
                } else {
                    copyText += "灵犀：" + "\n" + message.getMessage().trim() + "\n\n";
                }
            }
        }
        return copyText;
    }

    private void updateShareState(boolean share) {
        updateShareState(share, -1);
    }

    private void updateShareState(boolean share, int pos) {
        if (share) {
            switchShareDatas(DEFAULT_LAYOUT);
            ll_resend.setVisibility(View.GONE);
        }

        mSelectTitleLayout.setVisibility(share ? View.VISIBLE : View.GONE);
        ll_header.setVisibility(share ? View.GONE : View.VISIBLE);
        mShareBottom.setVisibility(share ? View.VISIBLE : View.GONE);
        chatAdapter.setSelectState(share);
//        EventBus.getDefault().post(new EventBusShareNotifyModel(!share));
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisible(!share);
        }
        if (getActivity() instanceof SuperChatContainActivity || getActivity() instanceof AgentContainActivity) {
            EventBus.getDefault().post(new EventBusShareNotifyModel(share));
        }
        if (pos > 0) {
            mHandler.postDelayed(() -> {
                // 滚动到顶部
//                LinearLayoutManager layoutManager = (LinearLayoutManager) rv_chat.getLayoutManager();
//                layoutManager.scrollToPositionWithOffset(pos, 0);
                rv_chat.scrollToPosition(pos);
            }, 1000);
        }
    }

    private void handleCopyLink(boolean needShare) {
        List<ChatContent> contents = new ArrayList<>();
        boolean isH5Card = true;
        for (ChatMessage message : mSelectMessages) {
            String cardType = null;
            String con = message.getMessage();
            String extraContent = "";
            if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_HOTEL_CARD) {
                cardType = Constant.ShareReqCardTypeParams.HOTEL_CARD;
                con = GsonUtils.toJson(message.getHotelModels());
            } else if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_TRAIN_CARD) {
                cardType = Constant.ShareReqCardTypeParams.TRAIN_CARD;
                con = GsonUtils.toJson(message.getTrainEntities());
            } else if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_PLANE_CARD) {
                cardType = Constant.ShareReqCardTypeParams.PLANE_CARD;
                con = GsonUtils.toJson(message.getPlandEntities());
            } else if (!TextUtils.isEmpty(message.getThinkMessage())) {
                cardType = Constant.ShareReqCardTypeParams.THINK_COT;
                con = message.getThinkMessage();
                extraContent = message.getMessage();
            } else if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_ORDER_CARD) {
                cardType = Constant.ShareReqCardTypeParams.ORDER_CARD;
                con = GsonUtils.toJson(message.getOrderEntity());
            } else if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_FOOD_CARD) {
                cardType = Constant.ShareReqCardTypeParams.FOOD_CARD;
                con = GsonUtils.toJson(message.getFoodList());
            } else if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_IMG) {
                cardType = Constant.ShareReqCardTypeParams.IMG_CARD;
                con = message.getImageList().get(0);
            } else if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_PLAN_CARD) {
                cardType = Constant.ShareReqCardTypeParams.HOME_CARD;
                con = GsonUtils.toJson(message.getPlanContent());
            } else if (message.getMsgType() == ChatAdapter.TYPE_AI_DRAWING) {
                cardType = Constant.ShareReqCardTypeParams.IMG_CARD;
                DrawingImageDto drawingImageDto = message.getDrawingImageDto();
                if (drawingImageDto != null && drawingImageDto.getImageUrl() != null) {
                    con = drawingImageDto.getImageUrl();
                }
            } else if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_H5_CARD) {
                cardType = Constant.ShareReqCardTypeParams.H5_CARD;
                contents.add(new ChatContent(
                        (message.getMsgType() == ChatAdapter.TYPE_USER) ? "user" : "assistant",
                        con, cardType, extraContent)
                );
                break;
            }
            isH5Card = false;
            contents.add(new ChatContent(
                    (message.getMsgType() == ChatAdapter.TYPE_USER) ? "user" : "assistant",
                    con, cardType, extraContent)
            );
        }

        if (contents.isEmpty() || isH5Card) {
            GlobalToast.show(requireActivity(), "无法分享该内容", GlobalToast.Type.ERROR);
            return;
        }

        new HttpRequest().sendChatLink("分享链接", contents, new Observer<ResponseBody>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(ResponseBody responseBody) {
                try {
                    String response = responseBody.string();
                    JSONObject jsonObject = new JSONObject(response);
                    String shortUrl = jsonObject.getString("short_url");
                    if (needShare) {
                        handleShareAction(shortUrl);
                    } else {
                        if (!TextUtils.isEmpty(shortUrl)) {
                            ZUtils.copy(requireActivity(), shortUrl);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    private void handleWechat() {
        List<ChatContent> contents = new ArrayList<>();
        for (ChatMessage message : mSelectMessages) {
            String cardType = null;
            String con = message.getMessage();
            if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_HOTEL_CARD) {
                cardType = Constant.ShareReqCardTypeParams.HOTEL_CARD;
                con = GsonUtils.toJson(message.getHotelModels());
            } else if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_TRAIN_CARD) {
                cardType = Constant.ShareReqCardTypeParams.TRAIN_CARD;
                con = GsonUtils.toJson(message.getTrainEntities());
            } else if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_PLANE_CARD) {
                cardType = Constant.ShareReqCardTypeParams.PLANE_CARD;
                con = GsonUtils.toJson(message.getPlandEntities());
            } else if (!TextUtils.isEmpty(message.getThinkMessage())) {
                cardType = Constant.ShareReqCardTypeParams.THINK_COT;
                con = GsonUtils.toJson(message.getThinkMessage());
            }
//            else if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_ORDER_CARD){
//                cardType = "orderCard";
//                con = GsonUtils.toJson(message.getOrderEntity());
//            }else if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_FOOD_CARD){
//                cardType = "foodCard";
//                con = GsonUtils.toJson(message.getFoodList());
//            }
//            contents.add(new ChatContent(
//                    (message.getMsgType() == ChatAdapter.TYPE_USER) ? "user" : "assistant",
//                    con, cardType));

            if (message.getMsgType() == ChatAdapter.TYPE_ASSISTANT_IMG) {
                contents.add(new ChatContent(
                        (message.getMsgType() == ChatAdapter.TYPE_USER) ? "user" : "assistant",
                        message.getImageList().get(0), null, ""));
            } else if (message.getMsgType() == ChatAdapter.TYPE_AI_DRAWING) {
                DrawingImageDto drawingImageDto = message.getDrawingImageDto();
                if (drawingImageDto != null && drawingImageDto.getImageUrl() != null) {
                    contents.add(new ChatContent("assistant", drawingImageDto.getImageUrl(), null, ""));
                }
            } else if (!TextUtils.isEmpty(con)) {
                contents.add(new ChatContent(
                        (message.getMsgType() == ChatAdapter.TYPE_USER) ? "user" : "assistant",
                        con, cardType, ""));
            }
        }
        if (contents.size() == 0) {
            GlobalToast.show(getActivity(), "无法分享该内容", GlobalToast.Type.ERROR);
            return;
        }
        new HttpRequest().sendChatLink("分享链接", contents, new Observer<ResponseBody>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ResponseBody responseBody) {
                try {
                    String response = responseBody.string();
                    JSONObject jsonObject = new JSONObject(response);
                    String shortUrl = jsonObject.getString("short_url");
                    if (!TextUtils.isEmpty(shortUrl)) {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, "灵犀对话：" + shortUrl);
                        ComponentName component = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI");
                        shareIntent.setComponent(component);
                        getActivity().startActivity(shareIntent);
                    }
                } catch (Exception e) {
                    Toast.makeText(getActivity(), "请先安装微信", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    private void shareImageToWeChatMoments(Bitmap bitmap) {
        try {
            File cachePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "long_pic.png");
            FileOutputStream fOut = new FileOutputStream(cachePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();

            Uri contentUri = FileProvider.getUriForFile(
                    getActivity(),
                    getActivity().getPackageName() + ".fileprovider",
                    cachePath
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            intent.setPackage("com.tencent.mm");
            intent.setClassName("com.tencent.mm",
                    "com.tencent.mm.ui.tools.ShareToTimeLineUI");

            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                getActivity().startActivity(intent);
            } else {
                Toast.makeText(getActivity(), "无法打开微信", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleLongPic() {
        mLongPicUtil = new SuperLongPicUtil(getActivity(), mLongPicLayout, rv_chat);
        mLongPicUtil.setCallback(new SuperShareCallback() {

            @Override
            public List<ChatMessage> getSelectMessages() {
                if (chatAdapter != null) {
                    return chatAdapter.getSelectMessages();
                }
                return null;
            }

            @Override
            public List<Integer> getSelectPositions() {
                if (chatAdapter != null) {
                    return chatAdapter.getSelectPositions();
                }
                return null;
            }

            @Override
            public void closeBottomLayout() {
                mLongPicLayout.setVisibility(View.GONE);
                updateShareState(false);
            }

            @Override
            public void onShareLongPic() {
                switchShareDatas(LONG_PIC_LAYOUT);
            }
        });
        chatAdapter.setSelectState(false);
        mLongPicUtil.createLongImage();
    }

    private void handleExportFile() {
        ExportFileDialog.showExportDialog(getActivity(), new ExportFileDialog.OnExportOptionSelected() {

            @Override
            public void onWordSelected() {
                DocumentHelper helper = new DocumentHelper(getActivity());
                helper.createWordWithTextAndImage(mSelectMessages, String.format("share_word_%s", System.currentTimeMillis()), new DocumentHelper.OnDocumentGeneratedListener() {
                    @Override
                    public void onSuccess(String filePath) {
                        getActivity().runOnUiThread(() -> GlobalToast.show(getActivity(), "导出Word文档成功,目录：" + filePath, GlobalToast.Type.SUCCESS));
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        getActivity().runOnUiThread(() -> GlobalToast.show(getActivity(), errorMessage, GlobalToast.Type.ERROR));
                    }
                });
            }

            @Override
            public void onPdfSelected() {
                DocumentHelper helper = new DocumentHelper(getActivity());
                helper.createPdfWithTextAndImage(mSelectMessages, String.format("share_pdf_%s", System.currentTimeMillis()), new DocumentHelper.OnDocumentGeneratedListener() {
                    @Override
                    public void onSuccess(String filePath) {
                        getActivity().runOnUiThread(() -> GlobalToast.show(getActivity(), "导出PDF文档成功,目录：" + filePath, GlobalToast.Type.SUCCESS));
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        getActivity().runOnUiThread(() -> GlobalToast.show(getActivity(), errorMessage, GlobalToast.Type.ERROR));
                    }
                });
            }

            @Override
            public void onTxtSelected() {
                DocumentHelper helper = new DocumentHelper(getActivity());
                helper.generateTextFile(getSelectText(), String.format("share_txt_%s", System.currentTimeMillis()), new DocumentHelper.OnDocumentGeneratedListener() {
                    @Override
                    public void onSuccess(String filePath) {
                        getActivity().runOnUiThread(() -> GlobalToast.show(getActivity(), "导出txt文档成功,目录：" + filePath, GlobalToast.Type.SUCCESS));
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        getActivity().runOnUiThread(() -> GlobalToast.show(getActivity(), errorMessage, GlobalToast.Type.ERROR));
                    }
                });
            }
        });
    }

    private void handleShareAction(String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        getActivity().startActivity(Intent.createChooser(shareIntent, "分享消息"));
    }

    private void handleSavePic() {
        mLongPicUtil.saveLongPic();
    }

    public void switchShareDatas(int type) {
        mShareDatas.clear();
        mShareDatas = new ArrayList<>();
        switch (type) {
            case DEFAULT_LAYOUT:
                mShareDatas.add(ShareItem.COPY_LINK);
                mShareDatas.add(ShareItem.WECHAT);
//                mShareDatas.add(ShareItem.WECHAT_MOMENT);
//                mShareDatas.add(ShareItem.LONG_PIC);
                mShareDatas.add(ShareItem.COPY_TEXT);
//                mShareDatas.add(ShareItem.SHARE_COLLECT);
                mShareDatas.add(ShareItem.SHARE_FILE);
                mShareDatas.add(ShareItem.SHARE_DELETE);
                mShareDatas.add(ShareItem.SHARE_SETTING);
                break;
            case LONG_PIC_LAYOUT:
                mShareDatas.add(ShareItem.SAVE_PIC);
                mShareDatas.add(ShareItem.WECHAT);
//                mShareDatas.add(ShareItem.WECHAT_MOMENT);
                break;
            case SAVE_FILE_LAYOUT:
                break;
            default:
                break;
        }
        mShareItemAdapter.updateData(mShareDatas);
    }


    private void showVoiceAnimate() {
        if (voiceRecordView == null) {
            voiceRecordView = findViewById(R.id.voiceRecordView);
        }
    }

    private void toggleAsrRecognition() {
        if (asrManager == null) {
            Timber.tag(TAG).d("asrManager = %s", asrManager);
            initAsrManger();
        }
        asrManager.toggleRecognition();
    }


    public void closeAsr() {
        if (asrManager == null) {
            Timber.tag(TAG).d("asrManager = %s", asrManager);
            return;
        }
        asrManager.stopRecognition();

    }

    public void cancelAsr() {
        if (asrManager == null) {
            Timber.tag(TAG).d("asrManager = %s", asrManager);
            return;
        }
        asrManager.cancelRecognition();
    }


    /**
     * 录音按下、移动、松开处理
     *
     * @param type     状态
     * @param isInArea 是否在区域
     * @param status   切换显示
     */
    public void voiceStatusHandle(int type, boolean isInArea, boolean status) {
        if (type == PRESS_DOWN) {
            if (voiceRecordView != null && voiceRecordView.startRecording()) {
                voiceRecordView.show();
                voiceRecordView.switchVoiceStatus(true);
            }
            TTSManager.Companion.getInstance().stop();
            stopMediaPlay();
            if (vmChat.pressDownBusinessProcessFlow()) return;
            toggleAsrRecognition();
        } else if (type == PRESS_MOVE) {
            if (voiceRecordView != null) {
                voiceRecordView.switchVoiceStatus(status);
            }
        } else if (type == PRESS_UP) {
            this.isMoveInArea = isInArea;
            if (voiceRecordView != null) {
                voiceRecordView.stopRecording();
            }
            if (vmChat.pressUpBusinessProcessFlow()) {
                ll_bottom.setVisibility(View.VISIBLE);
                ll_edit_translate.setVisibility(View.GONE);
                return;
            }
            if (!isInArea) {
                closeAsr();
            } else {
                cancelAsr();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        type = getArguments().getInt(Constant.INTENT_TYPE, SuperChatFragment.TYPE_HOME);
        if (type == TYPE_HOME) {
            // 权益包同步设备
            BillingManager.getInstance().start(new BillingCallback() {
                @Override
                public void onSuccess() {}

                @Override
                public void onSentPackageInfo(String status, String token, String device) {
                }

                @SuppressLint("TimberArgCount")
                @Override
                public void onFail(String msg) {
                    Timber.tag("Billing").d("Billing", "chat页面调用失败: " + msg);
                }

                @Override
                public void onNoDevice() {
                    Timber.tag("Billing").d("设备未注册");
                }
            }, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (superEditUtil != null) {
            superEditUtil.refreshSelectModel();
        }
        if (vmUserProfile != null) {
            vmUserProfile.loadUserProfile();
        }
        ZUtils.print("onResume =   ");
        TrackerUtils.trackEnterMainPageEvent();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll(KEY_BOTTOM);
        stopAutoScroll(KEY_HISTORY);
        if (vmChat != null && vmChat.getChatMessages().getValue() != null && type == TYPE_HOME) {
            vmChat.saveList();
        }
        TTSManager.Companion.getInstance().stop();
        stopMediaPlay();
    }

    private void initAsrManger() {
        asrManager = new AsrManager();
        asrManager.setVadTime(1500);
        asrManager.setResultListener(new AsrManager.AsrResultListener() {
            @Override
            public void onFinalResult(String text) {
                curAsrResult = text;
                Timber.tag(TAG).d("Chat_agentType: %s", agentType);
                if (type == TYPE_WAKE) {//半屏弹框走父类
                    vmChat.sendMsg.postValue(curAsrResult);
                    return;
                }
                if (agentType == TabEntity.TabType.CHAT) {
                    if (superEditUtil == null) {
                        Timber.tag(TAG).d("Chat_superEditUtil: %s", superEditUtil);
                        return;
                    }
                    superEditUtil.sendCommon(curAsrResult);
                } else if (agentType == TabEntity.TabType.TRIP_AI_AGENT) {
                    if (superAgentUtil == null) {
                        return;
                    }
                    superAgentUtil.voiceSendText(curAsrResult);
                } else if (agentType == TabEntity.TabType.TRIP_AI_WRITING) {
                    if (superEditAIWritingUtil == null) {
                        return;
                    }
                    superEditAIWritingUtil.sendMsg(curAsrResult);
                } else if (agentType == TabEntity.TabType.TRANSLATE) {
                    if (superEditAITranslateUtil == null) {
                        return;
                    }
                    superEditAITranslateUtil.sendMsg(curAsrResult);
                }
            }

            @Override
            public void onPartialResult(String text) {
                Timber.d("onPartialResult partial: %s", text);

            }

            @Override
            public void onError(String errorMsg) {
                Timber.e("ASR error: %s", errorMsg);
//                getActivity().runOnUiThread(() -> GlobalToast.show(requireActivity(), "未识别到文字", GlobalToast.Type.ERROR));
            }

            @Override
            public void onCloseError(String text) {
                Timber.e("ASR onCloseError: %s", text);
                if (!isMoveInArea){
                    Timber.e("ASR onCloseError: 取消录音");
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(getActivity());
                    if (!isNetworkAvailable) {
                        GlobalToast.show(getActivity(), "网络错误，请检查网络连接", GlobalToast.Type.ERROR);
                    } else {
                        GlobalToast.show(getActivity(), "未识别到文字", GlobalToast.Type.ERROR);
                    }
                });
            }
        });
    }

    private void startService() {
        Timber.tag(TAG).d("开始服务");
        Intent serviceIntent = new Intent(getActivity(), MyForegroundService.class);
        ContextCompat.startForegroundService(getActivity(), serviceIntent);
    }

    private void stopService() {
        Timber.tag(TAG).d("停止服务");
        Intent serviceIntent = new Intent(getActivity(), MyForegroundService.class);
        getActivity().stopService(serviceIntent); // 停止服务
    }


    @Subscribe
    public void cancelShare(EventBusShareCancelModel cancelModel) {
        if (TextUtils.equals(cancelModel.getShareType(), SuperChatContainActivity.class.getSimpleName()) || TextUtils.equals(cancelModel.getShareType(), AgentContainActivity.class.getSimpleName())) {
            if (getActivity() instanceof SuperChatContainActivity || getActivity() instanceof AgentContainActivity) {
                updateShareState(!cancelModel.isCancel());
            }
        }
        if (TextUtils.equals(cancelModel.getShareType(), MainActivity.class.getSimpleName())) {
            if (getActivity() instanceof MainActivity) {
                updateShareState(!cancelModel.isCancel());
            }
        }
    }

    @Subscribe
    public void planHistoryRefresh(EventBusSendHistoryNotifyMsg msg) {
        vmChat.addConversationHistory(msg.getMessage(), "assistant", true);
    }

    @Subscribe
    public void planHistoryRefresh(EventBusCardLoadNotifyModel msg) {
        scroll2Last();
    }

    /**
     * 动态设置 ll_empty 在0.618位置（黄金比例）
     */
    private void setEmptyViewPosition() {
        if (ll_empty == null) return;

        if (hasPositionedEmptyView) return;

        final ViewTreeObserver viewTreeObserver = ll_empty.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewGroup parent = (ViewGroup) ll_empty.getParent();
                if (parent == null) return;

                int parentHeight = parent.getHeight();
                if (parentHeight <= 0) return;

                // 只执行一次并移除监听，避免重复计算
                if (ll_empty.getViewTreeObserver().isAlive()) {
                    ll_empty.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

                hasPositionedEmptyView = true;

                // 首次计算
                repositionEmptyView(parentHeight);

                // 冷启动/首次安装可能在窗口 inset 应用后高度变化，再复算一次
                ll_empty.postDelayed(() -> {
                    ViewGroup p = (ViewGroup) ll_empty.getParent();
                    if (p != null && p.getHeight() > 0) {
                        repositionEmptyView(p.getHeight());
                    }
                }, 300);
            }
        });
    }

    private void repositionEmptyView(int parentHeight) {
        float density = ll_empty.getResources().getDisplayMetrics().density;
        int offsetPx = (int) (50 * density + 0.5f);
        int targetMarginTop = (int) (parentHeight * (1 - 0.618f)) - offsetPx;
        if (targetMarginTop < 0) targetMarginTop = 0;

        ViewGroup.LayoutParams params = ll_empty.getLayoutParams();
        if (params instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) params;
            lp.topMargin = targetMarginTop;
            ll_empty.setLayoutParams(lp);
        } else if (params instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) params;
            lp.topMargin = targetMarginTop;
            ll_empty.setLayoutParams(lp);
        } else if (params instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) params;
            lp.topMargin = targetMarginTop;
            ll_empty.setLayoutParams(lp);
        }
    }

    /**
     * Fragment视图销毁时清理资源
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MarqueeManager.INSTANCE.stopMarquee(getActivity());
        FloatHelper.INSTANCE.closeFloatMenu();
        // 移除滚动监听器，防止内存泄漏
        if (scrollChangedListener != null && sv_chat_list != null) {
            sv_chat_list.getViewTreeObserver().removeOnScrollChangedListener(scrollChangedListener);
            scrollChangedListener = null;
            Timber.tag(TAG).d("onDestroyView: 移除滚动监听器");
        }

        // 移除分页加载 Observer，防止累积泄漏
        if (loadMoreObserver != null && vmChat != null) {
            vmChat.getChatMessages().removeObserver(loadMoreObserver);
            loadMoreObserver = null;
            Timber.tag(TAG).d("onDestroyView: 移除分页加载Observer");
        }

        if (networkMonitor != null) {
            networkMonitor.unregister();
        }

        if (vmMeetingSummary != null) {
            if (summaryStreamObserver != null) {
                vmMeetingSummary.getSummaryStreamContent().removeObserver(summaryStreamObserver);
                summaryStreamObserver = null;
            }
            if (summaryProgressObserver != null) {
                vmMeetingSummary.getSummaryProgress().removeObserver(summaryProgressObserver);
                summaryProgressObserver = null;
            }
        }

        if (mHandler != null){
            mHandler.removeCallbacksAndMessages(null);
        }

    }

    private void stopMediaPlay() {
        if (chatAdapter != null) {
            int position = chatAdapter.getPosition();
            if (position >= 0) {
                chatAdapter.setMediaStatus(position);
            }
        }
    }


    /**
     * 通用一个listener
     */
    ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            // 移除监听，避免重复触发
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                sv_chat_list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            } else {
                // 兼容旧版本
                sv_chat_list.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
            Timber.tag(TAG).d(" 执行滚动 = OnGlobalLayoutListener   mIsUserScrolling %s ,isUserTouch=%s",mIsUserScrolling,isUserTouch );
            // 滚动到底部
            if (!mIsUserScrolling && !isUserTouch) {
                scrollToBottom();
            }
        }
    };

    /**
     * 智能体引导语
     */
    private void showAgentGuide() {
        viewModel.deleteConversation(id, null);
        if (type == TYPE_HOME) {
            if (vmChat.getChatMessages().getValue().size() == 0 && ll_empty != null) {
                ll_empty.setVisibility(View.VISIBLE);
            }
            return;
        }
        setAgentHead();

    }

    private void setAgentHead() {
        getCatDetailListBean selectAgentBean = vmChat.getSelectAgentBean();
        if (selectAgentBean == null) {
            return;
        }

        String preInput = selectAgentBean.getPreInput().replace("\\n", "\n");
        String recommendQuestions = selectAgentBean.getRecommendQuestions();
        List<String> nexusList = GsonUtils.fromJson(recommendQuestions, new TypeToken<List<String>>() {
        }.getType());
        if (Objects.equals(selectAgentBean.getModelName(), AGENT_GUI) && nexusList != null && !nexusList.isEmpty()) {
            vmChat.addNexusHeadMsg(nexusList);
        } else {
            if (selectAgentBean.getPreInput() == null) {
                return;
            }
            List<ChatMessage> mListChatMessage = vmChat.getChatMessages().getValue();
            if (mListChatMessage == null) {
                Timber.tag(TAG).e("获取消息列表为空");
                mListChatMessage = new ArrayList<>();
            }
            mListChatMessage.add(new ChatMessage(selectAgentBean.getDescription(), ChatAdapter.TYPE_USER_HEAD_AGENT, selectAgentBean.getIcon()));
            ChatMessage msg = vmChat.addAIMsg(preInput);
            msg.setHideActionRefresh(true);
        }
    }

    public ChatAdapter getChatAdapter() {
        return chatAdapter;
    }

    public void onFloatContent(String content) {
        Timber.tag(TAG).e("识别结果 == " + content);
        if (TextUtils.isEmpty(content)) {
            return;
        }
        // 与输入框发送保持一致：走 superEditUtil 回调，确保模型同步、TTS 状态重置
        if (superEditUtil != null) {
            superEditUtil.sendCommon(content);
            return;
        }
        sendFloatContentDirect(content);
    }

    private void sendFloatContentDirect(String content) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            showToast("当前无网络连接，请检查后重试");
            return;
        }
        if (!vmChat.getStreamEnd().getValue()) {
            ZUtils.showToast("正在输出，稍后。。");
            return;
        }
        TTSManager.Companion.getInstance().stop();
        stopMediaPlay();
        if (selectOptionModel != null) {
            vmChat.setSelectOptionModel(selectOptionModel);
        }
        vmChat.sendMessage(content);
        resetNewConversationFlag();
        scroll2Last(true);
        scrollToBottom();
    }

    public void onAgentFloatContent(String content) {
        Timber.tag(TAG).e("智能体识别结果 == %s", content + " type = "+type);
        if (type == TYPE_AGENT) {
            vmChat.sendAgentMessage(content);
        }

    }

    public void setAutoRecordView(AutoRecordView view) {
        this.voiceRecordView = view;
    }


}
