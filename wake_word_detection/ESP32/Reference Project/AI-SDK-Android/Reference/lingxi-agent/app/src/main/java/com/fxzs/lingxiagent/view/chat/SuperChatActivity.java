package com.fxzs.lingxiagent.view.chat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.callback.AIMeetingEditCallback;
import com.fxzs.lingxiagent.model.chat.callback.AITranslateEditCallback;
import com.fxzs.lingxiagent.model.chat.callback.MsgActionCallback;
import com.fxzs.lingxiagent.model.chat.callback.RequestCallback;
import com.fxzs.lingxiagent.model.chat.callback.SoftCallback;
import com.fxzs.lingxiagent.model.chat.callback.SuperEditCallback;
import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean;
import com.fxzs.lingxiagent.model.chat.dto.ChatFileListJsonBean;
import com.fxzs.lingxiagent.model.chat.dto.ChatFunctionBean;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.model.chat.dto.ConversationDetailDto;
import com.fxzs.lingxiagent.model.chat.dto.DrawingToChatBean;
import com.fxzs.lingxiagent.model.chat.dto.OptionModel;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepository;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepositoryImpl;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSessionDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingStyleDto;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.SuperEditUtil;
import com.fxzs.lingxiagent.util.ZInputMethod;
import com.fxzs.lingxiagent.util.ZUtil.AsrOneUtils;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.ZUtil.DrawingActionUtils;
import com.fxzs.lingxiagent.util.ZUtil.SuperAgentUtil;
import com.fxzs.lingxiagent.util.ZUtil.SuperEditAITranslateUtil;
import com.fxzs.lingxiagent.util.ZUtil.SuperEditAIWritingUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.ChatInputLayout;
import com.fxzs.lingxiagent.view.common.LoadingProgressDialog;
import com.fxzs.lingxiagent.view.drawing.DrawingActivity;
import com.fxzs.lingxiagent.view.drawing.DrawingImageViewerActivity;
import com.fxzs.lingxiagent.viewmodel.chat.VMChat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class SuperChatActivity extends BaseActivity {
    public static final int TYPE_HOME = 1;//首页
    public static final int TYPE_AGENT = 2;//智能体
    public static final int TYPE_DRAWING = 3;//绘画-对话界面
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;
    private static final int REQUEST_CONTINUE_EDIT = 2;
    private static final int REQUEST_DRAWING_VIEW = 3;
    private LinearLayout root_view;
    private LinearLayout ll_bottom;
    private LinearLayout ll_edit_writing;
    private LinearLayout ll_edit_translate;
    private LinearLayout ll_edit_main;
    private LinearLayout ll_edit_agent;
    private SuperEditUtil superEditUtil;
    private SuperAgentUtil superAgentUtil;
    private SuperEditAIWritingUtil superEditAIWritingUtil;
    private SuperEditAITranslateUtil superEditAITranslateUtil;
    private EditText ed;
    private RecyclerView rv_chat, rv_function;
    private ChatFunctionAdapter chatFunctionAdapter;
    private ChatAdapter chatAdapter;
    private TextView aiResponse;
    private View ll_stop;
    private View ll_resend;
    private TextView tv_mode;
    private ImageView iv_scroll_down;
    private ImageView iv_history;
    private ImageView iv_top_play;
    private VMChat vmChat;

    List<ChatFileBean> fileList;
    private OptionModel selectOptionModel;
    private boolean isUserTouch;//返回流时用户是否操作
    private boolean isSoft;
    private int type;//跳转类型
    private boolean mIsUserScrolling = false;
    private ChatMessage currentViewChatMessage;
    private ChatInputLayout chatInputLayout;

    private int prevMessageCount = 0;

    public void init() {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.act_super_chat);

        vmChat = new ViewModelProvider(this).get(VMChat.class);

        findViewById(R.id.back).setOnClickListener(view -> finish());
        ll_bottom = findViewById(R.id.ll_bottom);
        ll_edit_writing = findViewById(R.id.ll_edit_writing);
        ll_edit_translate = findViewById(R.id.ll_edit_translate);
        ll_edit_main = findViewById(R.id.ll_edit_main);
        ll_edit_agent = findViewById(R.id.ll_edit_agent);
        root_view = findViewById(R.id.root_view);
        rv_chat = findViewById(R.id.rv_chat);
        rv_function = findViewById(R.id.rv_function);
        ed = findViewById(R.id.ed);
        aiResponse = findViewById(R.id.aiResponse);
        ll_stop = findViewById(R.id.ll_stop);
        ll_resend = findViewById(R.id.ll_resend);
        tv_mode = findViewById(R.id.tv_mode);
        iv_scroll_down = findViewById(R.id.iv_scroll_down);
        iv_history = findViewById(R.id.iv_history);
        chatInputLayout.setFragmentManager(getSupportFragmentManager());


        if (getIntent() != null) {
            type = getIntent().getIntExtra(Constant.INTENT_TYPE,SuperChatActivity.TYPE_HOME);
            if(type == TYPE_HOME){//首页

                setFunctionRv();
                setBottomEdit();

                String input = getIntent().getStringExtra(Constant.INTENT_DATA);
                long id = getIntent().getLongExtra(Constant.INTENT_ID,0);
                selectOptionModel = (OptionModel) getIntent().getSerializableExtra(Constant.INTENT_DATA1);
                vmChat.setSelectOptionModel(selectOptionModel);
                if(id != 0){
                    loadConversationHistory(id);
                    if(superEditUtil!= null){
                        superEditUtil.setSelectOptionModel(selectOptionModel);
//                        superEditUtil.setBanSelectModel(true);
                    }
                }else{
                    fileList = (List<ChatFileBean>) getIntent().getSerializableExtra(Constant.INTENT_DATA2);
                    if(fileList != null){//带图片的/带文件
                        vmChat.setSelectOptionModel(selectOptionModel);
                        if (input != null) {
                            vmChat.sendMessageWithFile(input,fileList);
                        }
                    }else if(selectOptionModel != null){//纯文本对话
                        vmChat.setSelectOptionModel(selectOptionModel);
                        if(superEditUtil!= null){
                            superEditUtil.setSelectOptionModel(selectOptionModel);
                        }
                        if (input != null && selectOptionModel != null) {
                            tv_mode.setText(selectOptionModel.getName());
                            vmChat.sendMessage(input);
                        }
                    }
                }
            }else if(type == TYPE_AGENT){//智能体
                getCatDetailListBean bean = (getCatDetailListBean) getIntent().getSerializableExtra(Constant.INTENT_DATA2);

                if(bean != null){
                    if(bean.getIcon() != null){
                        vmChat.getChatMessages().getValue().add(new ChatMessage(bean.getDescription(),ChatAdapter.TYPE_USER_HEAD_AGENT,bean.getIcon()));

                        ChatMessage msg = vmChat.addAIMsg(bean.getPreInput());
                        msg.setHideActionRefresh(true);
                        vmChat.setSelectAgentBean(bean);
                    }
                    getAgentHeadInfo(bean.getId(), new RequestCallback<getCatDetailListBean>() {
                        @Override
                        public void callback(getCatDetailListBean data) {
                            if(data != null){

                                vmChat.getChatMessages().getValue().add(new ChatMessage(data.getDescription(),ChatAdapter.TYPE_USER_HEAD_AGENT,data.getIcon()));

                                ChatMessage msg = vmChat.addAIMsg(data.getPreInput());
                                msg.setHideActionRefresh(true);
                                vmChat.setSelectAgentBean(data);
                                long id = getIntent().getLongExtra(Constant.INTENT_ID,0);
                                if(id != 0){
                                    loadConversationHistory(id);
                                }
                            }
                        }
                    });
                }



                setAgentBottomEdit();
                iv_history.setVisibility(View.GONE);
            }else if(type == TYPE_DRAWING){//绘画
                DrawingToChatBean bean = (DrawingToChatBean) getIntent().getSerializableExtra(Constant.INTENT_DATA);
                DrawingStyleDto styleDto = (DrawingStyleDto) getIntent().getSerializableExtra(Constant.INTENT_DATA1);
                vmChat.setSelectDrawingToChatBean(bean);
                vmChat.setSelectDrawingStyleDto(styleDto);
                if(bean != null){

                    // 首先检查是否从历史记录进入
                    Long sessionId =  bean.getSessionId();
                    vmChat.getConversationId().postValue(Long.parseLong(sessionId.toString()));
                    DrawingSessionDto sessionDetail = bean.getSessionDetail();

                    if (sessionDetail != null && sessionDetail.getAiImageList() != null) {
                        // 从历史记录进入，显示会话详情
                        displaySessionHistory(sessionDetail);

                    }else{

                        // 正常的生成流程
                        String prompt = bean.getPrompt();
                        String style = bean.getStyle();
                        String styleId = bean.getStyle_id();
                        String ratio = bean.getRatio();
                        String referenceImageUrl = bean.getReference_image_url();

                        Timber.tag("DrawingChatActivity").d( "Intent data - prompt: " + prompt + ", style: " + style +
                                ", styleId: " + styleId + ", ratio: " + ratio + ", referenceImageUrl: " + referenceImageUrl);

                        if (prompt != null && !prompt.isEmpty()) {
                            // 添加用户消息
                            vmChat.sendDrawingMessage(prompt);

                            // 不再单独添加AI回复文字，将在气泡中一起显示

                            // 设置ViewModel的prompt
//                        viewModel.getPrompt().set(prompt);
//                        Timber.tag("DrawingChatActivity").d( "Set prompt to ViewModel: " + prompt);

                            // 设置传递过来的比例，如果没有则使用默认9:16
//                        String selectedRatio = (ratio != null && !ratio.isEmpty()) ? ratio : "9:16";
//                        viewModel.getSelectedRatio().set(selectedRatio);
//                        Timber.tag("DrawingChatActivity").d( "Set ratio to ViewModel: " + selectedRatio);
//
//                        // 设置传递过来的风格
//                        if (style != null && !style.isEmpty() && styleId != null && !styleId.isEmpty()) {
//                            // 需要等待风格列表加载完成后再设置选中的风格
//                            setupStyleFromIntent(style, styleId);
//                        }
//
//                        // 如果有参考图片URL，传递给ViewModel
//                        if (referenceImageUrl != null && !referenceImageUrl.isEmpty()) {
//                            this.referenceImageUrl = referenceImageUrl;
//                            viewModel.setReferenceImageUrl(referenceImageUrl);
//                            Timber.tag("DrawingChatActivity").d( "Set reference image URL: " + referenceImageUrl);
//                        }
//
//                        // 延迟一小段时间再开始生成，确保ViewModel初始化完成
//                        etMessageInput.postDelayed(() -> {
//                            Timber.tag("DrawingChatActivity").d( "Starting image generation...");
//                            viewModel.generateImage();
//                        }, 500);
                        }
//                    vmChat.getChatMessages().getValue().add(new ChatMessage(bean.getDescription(),true,bean.getIcon()));
//
//                    ChatMessage msg = vmChat.addAIMsg(bean.getPreInput());
//                    msg.setHideActionRefresh(true);
//                    vmChat.setSelectAgentBean(bean);

                    }

                }
                setAgentBottomEdit();
                iv_history.setVisibility(View.GONE);
                iv_top_play.setVisibility(View.GONE);
            }
        }

        setChatRv();
        /*TTSUtils.getInstance().init(SuperChatActivity.this);
        TTSUtils.getInstance().process();*/

        ZUtils.print("TTSUtils.getInstance().ttsStart");
        /*TTSUtils.getInstance().ttsInit();*/




        ll_stop.setOnClickListener(view -> {
            // 使用VMChat的统一方法处理停止逻辑
            vmChat.stopThinkingAndGeneration("已暂停生成");
            
            // 更新UI状态
            ll_stop.setVisibility(View.GONE);
            ll_resend.setVisibility(View.VISIBLE);

            // 取消正在进行的Markdown渲染
            if (chatAdapter != null) {
                chatAdapter.cancelAllMarkdownRendering();
            }
        });

        iv_scroll_down.setOnClickListener(view -> {
            isUserTouch = false;
            iv_scroll_down.setVisibility(View.GONE);

            if(vmChat.getStreamEnd().getValue()){//已经结束流，手动调用代码滑动
                scroll2Last();
            }
        });
        ll_resend.setOnClickListener(view -> {
            ChatMessage message = vmChat.getResendMsg();
            if(message != null && message.getMsgType() == ChatAdapter.TYPE_USER){
                ed.setText(message.getMessage());
//                vmChat.resendMsg();
                ll_resend.setVisibility(View.GONE);
            }
        });
        vmChat.getIsAutoPlay().postValue(SharedPreferencesUtil.getBoolean(Constants.KEY_IS_AUTO,true));
        iv_top_play.setOnClickListener(view -> {
            vmChat.getIsAutoPlay().postValue(!vmChat.getIsAutoPlay().getValue());
        });
        iv_history.setOnClickListener(view -> {
        });
        rv_chat.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                // DRAGGING 与 SETTLING（fling）都视为用户滚动，期间不自动跟随
                mIsUserScrolling = (newState == RecyclerView.SCROLL_STATE_DRAGGING
                        || newState == RecyclerView.SCROLL_STATE_SETTLING);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // 当用户向上滚动较多（dy<0）时，认为用户正在浏览历史，不进行跟随
                if (dy < 0) {
                    isUserTouch = true;
                }
            }
        });
    }

    private void getAgentHeadInfo(long id, RequestCallback<getCatDetailListBean> callback) {

        HttpRequest httpRequest = new HttpRequest();
        httpRequest.getDetailByModel(id + "", new Observer<ApiResponse<getCatDetailListBean>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<getCatDetailListBean> getCatDetailListBeanApiResponse) {
                callback.callback(getCatDetailListBeanApiResponse.getData());
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    private void loadConversationHistory(long id) {
        // 使用VMChat的分页加载功能
        // 设置合理的分页大小（首次加载20条）
        vmChat.setPageSize(20);
        
        // 显示加载框
        LoadingProgressDialog loadingDialog =
                new LoadingProgressDialog(SuperChatActivity.this)
                        .setMessage("加载中...")
                        .setCancelable(false);
        loadingDialog.show();
        
        // 重置并加载第一页
        vmChat.resetAndLoadFirstPage(id);
        
        // 延迟关闭加载框，等待数据加载完成
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            loadingDialog.dismiss();
        }, 500);
        
        // 添加滚动监听，实现向上滚动加载更多
        setupLoadMoreListener();
    }
    
    /**
     * 设置加载更多监听器
     * 当用户向上滚动到顶部附近时，自动加载更多历史消息
     */
    private void setupLoadMoreListener() {
        rv_chat.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                // dy < 0 表示向上滚动
                if (dy < 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                        
                        // 当滚动到前5条消息时，触发加载更多
                        if (firstVisiblePosition < 5 && vmChat.hasMorePages() && !vmChat.isLoadingPage()) {
                            Timber.tag("SuperChatActivity").d("触发加载更多历史消息");
                            vmChat.loadNextPage();
                        }
                    }
                }
            }
        });
    }
    
    /**
     * 旧的加载方法（保留作为备用）
     * @deprecated 使用 loadConversationHistory(long id) 替代
     */
    @Deprecated
    private void loadConversationHistoryOld(long id) {
        // 显示加载框
        LoadingProgressDialog loadingDialog =
                new LoadingProgressDialog(SuperChatActivity.this)
                        .setMessage("加载中...")
                        .setCancelable(false);
        loadingDialog.show();

        ChatRepository repository = new ChatRepositoryImpl();
        // 调用API获取会话详情
        final int pageNo = 1;
        final int pageSize = 60;
        repository.getPageByConversationId(id, pageNo, pageSize, new ChatRepository.Callback<List<ConversationDetailDto>>() {
            @Override
            public void onSuccess(List<ConversationDetailDto> list) {
                loadingDialog.dismiss();
                if(list != null && list.size() > 0){
                    // 批量收集所有消息，最后一次性添加
                    List<ChatMessage> historyMessages = new ArrayList<>();
                    
                    for (int i = 0; i < list.size(); i++) {
                        ConversationDetailDto dto =  list.get(i);
                        if(!TextUtils.isEmpty(dto.getContent())){
                            if(dto.getType().equals("assistant")){
                                ZUtils.print("onSuccess assistant = "+dto.getContent());
                                // 创建AI消息但不立即添加到列表
                                ChatMessage aiMsg = new ChatMessage(dto.getContent(), false, com.fxzs.lingxiagent.util.ZUtil.Constant.ThinkState.END);
                                aiMsg.setThinkMessage("");
                                aiMsg.setId((long) dto.getId());
                                historyMessages.add(aiMsg);
                                Timber.tag("SuperChatActivity").d( "收集AI消息ID: " + dto.getId());
                            }else if(dto.getType().equals("user")){
                                ZUtils.print("onSuccess user = "+dto.getContent());
                                // 创建用户消息但不立即添加到列表
                                ChatMessage userMsg = new ChatMessage(dto.getContent(), ChatAdapter.TYPE_USER);
                                userMsg.setId((long) dto.getId());
                                historyMessages.add(userMsg);
                                Timber.tag("SuperChatActivity").d( "收集用户消息ID: " + dto.getId());
                                
                                // 处理文件和图片
                                List<ChatFileBean> files = new ArrayList<>();
                                if(dto.getFileListJson() != null){
                                    List<ChatFileListJsonBean> eventList = new Gson().fromJson(dto.getFileListJson() ,
                                            new TypeToken<List<ChatFileListJsonBean>>() {}.getType());

                                    if(eventList != null && eventList.size() > 0){
                                        for (int j = 0; j < eventList.size(); j++) {
                                            ChatFileListJsonBean bean =  eventList.get(j);
                                            ChatFileBean chatFileBean = new ChatFileBean(bean.getName(),bean.getFileUrl(),false);
                                            chatFileBean.setFileType(bean.getType());
                                            files.add(chatFileBean);
                                        }
                                    }
                                }else if(dto.getImages()!=null){
                                    String[] results = dto.getImages().split(",");
                                    if(results != null && results.length > 0){
                                        for (int j = 0; j < results.length; j++) {
                                            String url =  results[j];
                                            ChatFileBean chatFileBean = new ChatFileBean(url,true);
                                            chatFileBean.setPath(url);
                                            chatFileBean.setPercent(100);
                                            files.add(chatFileBean);
                                        }
                                    }
                                }
                                
                                // 如果有文件，创建文件消息
                                if(files != null && files.size() > 0){
                                    boolean isImage = files.get(0).isImage();
                                    ChatMessage fileMsg = new ChatMessage(
                                        isImage ? ChatAdapter.TYPE_USER_FILE_IMAGE : ChatAdapter.TYPE_USER_FILE
                                    );
                                    fileMsg.setChatFileBeanList(files);
                                    fileMsg.setId((long) dto.getId());
                                    historyMessages.add(fileMsg);
                                }
                            }
                        }
                    }
                    
                    // 批量添加所有消息，只触发一次LiveData更新
                    vmChat.addMessagesInBatch(historyMessages);
                    Timber.tag("SuperChatActivity").d("批量添加历史消息完成，共 " + historyMessages.size() + " 条");
                }
            }

            @Override
            public void onError(String error) {
                loadingDialog.dismiss();
                Timber.tag("SuperChatActivity").e("加载历史消息失败: " + error);
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
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {
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
    }

    @Override
    protected void setupObservers() {
        // 监听 ViewModel 的 LiveData（区分新增与流式增量，精细更新 + 温和自动滚动）
        vmChat.getChatMessages().observe(this, messages -> {
            int count = messages.size();
            if (count == 0) return;
            ZUtils.print("vmChat observe = " + count);

            int last = count - 1;
            ChatMessage lastMsg = messages.get(last);
            boolean isAiStreaming = lastMsg.getMsgType() == ChatAdapter.TYPE_AI
                    && vmChat.getStreamEnd().getValue() != null
                    && !vmChat.getStreamEnd().getValue();

            if (count == 1) {
                chatAdapter.notifyDataSetChanged();
                prevMessageCount = count;
                autoScrollIfNearBottom();
                return;
            }

            if (count > prevMessageCount) {
                // 新增一条（用户发言或新增AI条目）
                chatAdapter.notifyItemInserted(last);
                autoScrollIfNearBottom();
            } else {
                // 条数未变：大概率是最后一条流式内容的增量
                if (isAiStreaming) {
                    chatAdapter.updateStreamingContent(last, lastMsg.getMessage(), false);
                    autoScrollIfNearBottom();
                } else {
                    // 兜底：普通变更
                    chatAdapter.notifyItemChanged(last, 0);
                    autoScrollIfNearBottom();
                }
            }

            prevMessageCount = count;
        });
        vmChat.getAiResponse().observe(this, response -> aiResponse.setText(response));
        vmChat.getLoading().observe(this, isLoading -> {
            // 可根据 isLoading 显示/隐藏 loading UI
        });
        vmChat.getStreamEnd().observe(this, end -> {
            if (end) ll_stop.setVisibility(View.GONE);
            // 流结束后，如果此刻仍接近底部，做一次温和跟随
            autoScrollIfNearBottom();
        });
        vmChat.getThinkStatus().observe(this, status->{
            if(status == Constant.ThinkState.START){
                ll_stop.setVisibility(View.VISIBLE);
                ll_resend.setVisibility(View.GONE);
            }else if(status == Constant.ThinkState.THINKING){
                // no-op
            }else if(status == Constant.ThinkState.END){
                ll_stop.setVisibility(View.GONE);
                isUserTouch = false;
                autoScrollIfNearBottom();
            }
        });

        vmChat.getIsAutoPlay().observe(this, isAutoPlay -> {


            if(isAutoPlay){
                iv_top_play.setBackground(getResources().getDrawable(R.mipmap.chat_top_play));
                /*TTSUtils.getInstance().startPlayer();*/
            }else {
                iv_top_play.setBackground(getResources().getDrawable(R.mipmap.chat_top_mute));
                /*TTSUtils.getInstance().pausePlayer();*/
//                TTSUtils.getInstance().ttsCancel();
            }
            SharedPreferencesUtil.saveBoolean(Constants.KEY_IS_AUTO,isAutoPlay);

        });
        // 其他 LiveData 监听可按需添加
        vmChat.getConversationId().observe(this,id ->{
            if(superEditUtil != null){
                superEditUtil.conversationId = id;
            }
        });
    }


    float lastX;
    float lastY;
    private void setChatRv() {
        // 优化：禁用动画，减少不必要的重绘
        rv_chat.setItemAnimator(null);
        
        LinearLayoutManager lm = new LinearLayoutManager(this);
        // 不使用 stackFromEnd，避免长内容+动态高度时位置被锚定在底部导致跳跃
        rv_chat.setLayoutManager(lm);
        
        // 优化：RecyclerView性能配置
        rv_chat.setHasFixedSize(false); // 消息高度不固定
        rv_chat.setItemViewCacheSize(20); // 优化：减少缓存大小，从50改为20，减少内存占用
        rv_chat.setDrawingCacheEnabled(true); // 启用绘制缓存
        rv_chat.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO); // 自动质量
        
        // 优化：设置RecycledViewPool，提升ViewHolder复用效率
        RecyclerView.RecycledViewPool recycledViewPool = new RecyclerView.RecycledViewPool();
        recycledViewPool.setMaxRecycledViews(ChatAdapter.TYPE_USER, 10);
        recycledViewPool.setMaxRecycledViews(ChatAdapter.TYPE_AI, 10);
        recycledViewPool.setMaxRecycledViews(ChatAdapter.TYPE_USER_FILE_IMAGE, 5);
        recycledViewPool.setMaxRecycledViews(ChatAdapter.TYPE_USER_FILE, 5);
        rv_chat.setRecycledViewPool(recycledViewPool);

        chatAdapter = new ChatAdapter(this, vmChat.getChatMessages().getValue());
        chatAdapter.setCallback(new MsgActionCallback() {
            @Override
            public void refresh(String content) {
                /*TTSUtils.getInstance().cancelAndPlay();*/
                vmChat.refreshSendMessage(content,type);
            }

            @Override
            public void refreshTranslation(String content, String fromLanguage, String toLanguage) {
                vmChat.sendTranslateMessage(content, fromLanguage, toLanguage);
            }

            @Override
            public void msgClick() {
                ZInputMethod.closeInputMethod(SuperChatActivity.this, rv_chat);

//                if(superEditUtil != null){
//                    superEditUtil.
//                }
            }

            @Override
            public void continueDrawing(ChatMessage message) {
                continueDrawings(message);
            }
            @Override
            public void regenerateDrawing(ChatMessage message) {
                String prompt = message.getMessage();
                vmChat.sendDrawingMessage(prompt); // 直接显示原始prompt
                Toast.makeText(SuperChatActivity.this, "正在重新生成图片...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void downloadDrawing(ChatMessage message) {
               DrawingActionUtils.performDownload(SuperChatActivity.this,message.getUrl());
            }

            @Override
            public void viewDrawing(ChatMessage message) {
                currentViewChatMessage = message;
                Intent intent = new Intent(SuperChatActivity.this, DrawingImageViewerActivity.class);
                intent.putExtra("image_url", message.getUrl());

                String prompt = vmChat.getGeneratedImage().getValue().getPrompt();
                if (prompt == null || prompt.isEmpty()) {
                    prompt =  vmChat.getSelectDrawingToChatBean().getPrompt();
                }
                intent.putExtra("prompt", prompt);
                startActivityForResult(intent,REQUEST_DRAWING_VIEW);
            }
        });
        rv_chat.setAdapter(chatAdapter);
        rv_chat.setOnTouchListener((view, motionEvent) -> {
            ZUtils.print("rv_chat motionEvent = "+motionEvent.getAction());
            ZUtils.print("rv_chat mChat.getStreamEnd().getValue() = "+vmChat.getStreamEnd().getValue());
            ZInputMethod.closeInputMethod(SuperChatActivity.this, view);
//
//            if(type == TYPE_DRAWING){
//                return false;
//            }

            float rawX = motionEvent.getRawX();
            float rawY = motionEvent.getRawY();
            if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                if(!vmChat.getStreamEnd().getValue()){
                    iv_scroll_down.setVisibility(View.VISIBLE);
                    isUserTouch = true;
                }

                lastX = motionEvent.getRawX();
                lastY = motionEvent.getRawY();
            }else if(motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                if(lastY-rawY>0){//向上滑，用户在浏览历史
                    isUserTouch = true; // 不要在上滑时被自动拉回
                }else {
                    // 向下滑回到底部区域时，才允许自动跟随
                    if (vmChat.getStreamEnd().getValue()) {
                        isUserTouch = false;
                    }
                    iv_scroll_down.setVisibility(View.VISIBLE);
                }
            }
            return false;
        });
    }

    private void setFunctionRv() {
        List<ChatFunctionBean> list = new ArrayList<>();
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_LIFE, R.mipmap.ic_life, "生活"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_PHONE, R.mipmap.ic_phone, "通话"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_WORK, R.mipmap.ic_work, "办公"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_PLAY, R.mipmap.ic_play, "娱乐"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_AI_WRITE, R.mipmap.ic_ai_write, "AI写作"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_AI_TRANSLATE, R.mipmap.ic_ai_translate, "AI翻译"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_PPT, R.mipmap.ic_ppt, "PPT生成"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_AI_PIC, R.mipmap.ic_ai_pic, "AI绘画"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_AI_MEETING, R.mipmap.ic_ai_meeting, "AI会议"));
        list.add(new ChatFunctionBean(Constant.ChatFunction.TYPE_VOICE, R.mipmap.ic_voice, "同声传译"));
        rv_function.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        chatFunctionAdapter = new ChatFunctionAdapter(this, list, position -> {
            ChatFunctionBean bean = list.get(position);
            if (bean.getId() == Constant.ChatFunction.TYPE_LIFE) {
                // 生活
            } else  if (bean.getId() == Constant.ChatFunction.TYPE_PHONE) {
                // 通话
            } else  if (bean.getId() == Constant.ChatFunction.TYPE_WORK) {
                // 办公
            } else  if (bean.getId() == Constant.ChatFunction.TYPE_PLAY) {
                // 娱乐
            } else if (bean.getId() == Constant.ChatFunction.TYPE_AI_WRITE) {
                // AI写作
                ll_bottom.setVisibility(View.GONE);
                ll_edit_writing.setVisibility(View.VISIBLE);
                vmChat.getConversationId().postValue(0l);
                 setBottomAIWritingEdit();
            } else if (bean.getId() == Constant.ChatFunction.TYPE_AI_TRANSLATE) {
                // AI翻译
                vmChat.getConversationId().postValue(0l);
                ll_bottom.setVisibility(View.GONE);
                ll_edit_translate.setVisibility(View.VISIBLE);
                 setBottomTranslateEdit();
            } else if (bean.getId() == Constant.ChatFunction.TYPE_PPT) {
                // PPT 生成
            } else if (bean.getId() == Constant.ChatFunction.TYPE_AI_PIC) {
                // AI绘画
//                finish();
            } else if (bean.getId() == Constant.ChatFunction.TYPE_AI_MEETING) {
                // AI 会议
            } else if (bean.getId() == Constant.ChatFunction.TYPE_VOICE) {
                // 同声传译
            }
        });
        rv_function.setAdapter(chatFunctionAdapter);
    }

    private void setBottomEdit() {
//        ll_edit_main.setVisibility(View.VISIBLE);
        superEditUtil = new SuperEditUtil(this, ll_edit_main);
        superEditUtil.setOnListenSoft(root_view, new SoftCallback() {
            @Override
            public void show() {
                if(type == TYPE_HOME) {//首页
                    rv_function.setVisibility(View.GONE);
                }
                if(!isSoft){
                    scroll2Last();
                }
                isSoft = true;
            }
            @Override
            public void hide() {
                if(type == TYPE_HOME) {//首页
                    rv_function.setVisibility(View.VISIBLE);
                }
                if(isSoft){
                    scroll2Last();
                }
                isSoft = false;
            }
        });
        superEditUtil.setCallback(new SuperEditCallback() {
            @Override
            public void send(String content, OptionModel optionModel) {
                ZUtils.print("SuperChatActivity send = "+content + " OptionModel = "+optionModel.getName());

                if (!content.isEmpty()) {
                    selectOptionModel = optionModel;
                    vmChat.setSelectOptionModel(optionModel);
                    vmChat.sendMessage(content);

                    /*TTSUtils.getInstance().cancelAndPlay();*/
                    ZInputMethod.closeInputMethod(SuperChatActivity.this,root_view);   //收起键盘
                }

            }

            @Override
            public void sendWithFile(String content, OptionModel selectOptionModel, List<ChatFileBean> fileList, boolean isFile) {
                vmChat.setSelectOptionModel(selectOptionModel);
                if (content != null) {
                    List<ChatFileBean> list = new ArrayList<>();
                    list.addAll(fileList);
                    vmChat.sendMessageWithFile(content,list);
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

            }

            @Override
            public void pressUp(boolean isInArea) {

            }
        });
    }

    private void setAgentBottomEdit() {
        ll_edit_agent.setVisibility(View.VISIBLE);
        superAgentUtil = new SuperAgentUtil(this, ll_edit_agent,null);
        superAgentUtil.setOnListenSoft(root_view, new SoftCallback() {
            @Override
            public void show() {

                if(!isSoft){
                    scroll2Last();
                }
                isSoft = true;
            }
            @Override
            public void hide() {
                if(isSoft){
                    scroll2Last();
                }
                isSoft = false;
            }
        });
        superAgentUtil.setCallback(new SuperEditCallback() {
            @Override
            public void send(String content, OptionModel optionModel) {
//                ZUtils.print("SuperChatActivity send = "+content + " OptionModel = "+optionModel.getName());

                if (!content.isEmpty()) {
//                    selectOptionModel = optionModel;
//                    optionModel = new OptionModel();
//                    vmChat.setSelectOptionModel(optionModel);
                    if(type == TYPE_AGENT){
                        if (!vmChat.getStreamEnd().getValue()) {
                            ZUtils.showToast("正在输出，稍后。。");
                            return;
                        }
                        vmChat.sendAgentMessage(content);

                    }else if(type == TYPE_DRAWING){

                        vmChat.sendDrawingMessage(content);
                    }

                    ZInputMethod.closeInputMethod(SuperChatActivity.this,root_view);   //收起键盘
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

            }

            @Override
            public void pressUp(boolean isInArea) {

            }
        });
    }

    private void setBottomAIWritingEdit() {

        ll_stop.setVisibility(View.GONE);
        superEditAIWritingUtil = new SuperEditAIWritingUtil(SuperChatActivity.this,ll_edit_writing);


        superEditAIWritingUtil.setCallback(
                new AIMeetingEditCallback() {
                    @Override
                    public void send(String content) {
                        if (selectOptionModel != null) {
                            vmChat.setSelectOptionModel(selectOptionModel);
                        }
                        vmChat.sendAIWritingMessage(content);
                        ll_bottom.setVisibility(View.VISIBLE);
                        ll_edit_writing.setVisibility(View.GONE);

                    }

                    @Override
                    public void close() {

                        ll_bottom.setVisibility(View.VISIBLE);
                        ll_edit_writing.setVisibility(View.GONE);
                        vmChat.getConversationId().postValue(0l);

                        if (vmChat.getThinkStatus().getValue() != Constant.ThinkState.END) {
                            ll_stop.setVisibility(View.VISIBLE);
                        }

                        ZInputMethod.closeInputMethod(SuperChatActivity.this,superEditAIWritingUtil.getIv_close());   //收起键盘
                    }

                    @Override
                    public void voice() {

                    }

                    @Override
                    public void keyboard() {

                    }

                    @Override
                    public void pressDown() {

                    }

                    @Override
                    public void pressUp(boolean isInArea) {

                    }

                    @Override
                    public void voiceMove(boolean status) {
                        AIMeetingEditCallback.super.voiceMove(status);
                    }
                }
        );
    }
    private void setBottomTranslateEdit() {

        ll_stop.setVisibility(View.GONE);
        superEditAITranslateUtil = new SuperEditAITranslateUtil(SuperChatActivity.this,ll_edit_translate);


        superEditAITranslateUtil.setCallback(
                new AITranslateEditCallback() {

                    @Override
                    public void send(String content, String prompt) {
                        vmChat.sendTranslateMessage(content,prompt);
                        ll_bottom.setVisibility(View.VISIBLE);
                        ll_edit_translate.setVisibility(View.GONE);
                    }
                    @Override
                    public void close() {

                        ll_bottom.setVisibility(View.VISIBLE);
                        ll_edit_translate.setVisibility(View.GONE);
                        vmChat.getConversationId().postValue(0l);

                        if (vmChat.getThinkStatus().getValue() != Constant.ThinkState.END) {
                            ll_stop.setVisibility(View.VISIBLE);
                        }

                        ZInputMethod.closeInputMethod(SuperChatActivity.this,superEditAITranslateUtil.getIv_close());   //收起键盘
                    }

                    @Override
                    public void voice() {

                    }

                    @Override
                    public void keyboard() {

                    }

                    @Override
                    public void pressDown() {

                    }

                    @Override
                    public void pressUp(boolean isInArea) {

                    }

                    @Override
                    public void voiceMove(boolean status) {
                        AITranslateEditCallback.super.voiceMove(status);
                    }

                }
        );
    }



    int lastTextViewHeight = 0;
    private void scroll2Last() {
        // 旧逻辑可能导致位置跳变，统一使用温和“接近底部才跟随”的策略
        if (mIsUserScrolling || isUserTouch) return;
        autoScrollIfNearBottom();
    }

    private void autoScrollIfNearBottom() {
        if (mIsUserScrolling || isUserTouch) return;
        RecyclerView.LayoutManager lm0 = rv_chat.getLayoutManager();
        if (!(lm0 instanceof LinearLayoutManager)) return;
        LinearLayoutManager lm = (LinearLayoutManager) lm0;
        int lastVisible = lm.findLastVisibleItemPosition();
        int itemCount = chatAdapter != null ? chatAdapter.getItemCount() : 0;
        if (itemCount <= 0) return;
        boolean nearBottom = lastVisible >= itemCount - 2;
        if (nearBottom) {
            rv_chat.smoothScrollToPosition(itemCount - 1);
        }
    }


    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(SuperChatActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            AppPermissionRequestManager.requestAudioPermission(this, PERMISSION_REQUEST_RECORD_AUDIO,AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_ASR);
        }
    }

    public void continueDrawings(ChatMessage message) {
//        DrawingImageDto currentImage = vmChat.getGeneratedImage().getValue();

        if(vmChat.getIsGenerating().getValue()){
            ZUtils.showToast("请等待生成成功");
//            GlobalToast.show(IYAApplication.getInstance(),"请等待生成成功", GlobalToast.Type.NORMAL);
            return;
        }
        // 继续编辑
        if (message != null) {
            Intent intent = new Intent(SuperChatActivity.this, DrawingActivity.class);
            intent.putExtra("continue_edit", true);
            intent.putExtra("from_chat", true);
            intent.putExtra("reference_image_url", message.getUrl());
//            intent.putExtra("original_prompt", currentImage.getPrompt());
            if (vmChat.getSelectDrawingStyleDto() != null) {
                intent.putExtra("style", vmChat.getSelectDrawingStyleDto().getName());
            }

            Timber.tag("DrawingChatActivity").d( "Starting continue edit activity for result");
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
            superEditUtil.onRequestPermissionsResult(requestCode,permissions,grantResults);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Timber.tag("DrawingChatActivity").d( "onActivityResult called - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == REQUEST_CONTINUE_EDIT && resultCode == RESULT_OK && data != null) {
            // 从继续编辑页面返回，获取用户输入的编辑要求
            String editPrompt = data.getStringExtra("edit_prompt");
            String referenceImageUrl = data.getStringExtra("reference_image_url");
            String originalPrompt = data.getStringExtra("original_prompt");
            String style = data.getStringExtra("style");
            DrawingStyleDto styleDto = (DrawingStyleDto) data.getSerializableExtra("DrawingStyleDto");

            Timber.tag("DrawingChatActivity").d( "Received continue edit result:");
            Timber.tag("DrawingChatActivity").d( "editPrompt: " + editPrompt);
            Timber.tag("DrawingChatActivity").d( "referenceImageUrl: " + referenceImageUrl);
            Timber.tag("DrawingChatActivity").d( "originalPrompt: " + originalPrompt);
            Timber.tag("DrawingChatActivity").d( "style: " + style);

            vmChat.setSelectDrawingStyleDto(styleDto);

            if (editPrompt != null && !editPrompt.isEmpty()) {
                // 在当前对话页面中继续编辑
                handleContinueEditInConversation(editPrompt, referenceImageUrl, originalPrompt, style);
            }
        }else if(requestCode == REQUEST_DRAWING_VIEW && resultCode == RESULT_OK){
            //大图浏览-继续编辑
            continueDrawings(currentViewChatMessage);
        }

        if (superEditUtil != null) {
            superEditUtil.onActivityResult(requestCode,resultCode,data);
        }
    }
    /**
     * 在对话页面中处理继续编辑
     */
    private void handleContinueEditInConversation(String editPrompt, String referenceImageUrl, String originalPrompt, String style) {
        Timber.tag("DrawingChatActivity").d( "handleContinueEditInConversation called");

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
        vmChat.sendDrawingMessage(editPrompt,referenceImageUrl);

        // 不再单独添加AI回复文字，将在气泡中一起显示

        // 设置新的prompt并触发生成
//        viewModel.getPrompt().set(editPrompt);

        Timber.tag("DrawingChatActivity").d( "About to call generateImage()");
//        vmChat.generateImage();

        Timber.tag("DrawingChatActivity").d( "Continue edit in conversation completed");
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
                vmChat.updateAIDrawingMsg(100);
                vmChat.updateAIDrawingMsg(imageDto.getImageUrl());
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
    protected void onDestroy() {
        super.onDestroy();
        vmChat.closeSSE();

        AsrOneUtils.getInstance().removeCallBack();
        superEditUtil = null;
        ZUtils.print("SuperChatActivity onDestroy = "+superEditUtil);
        /*TTSUtils.getInstance().stopPlayer();
        TTSUtils.getInstance().ttsCancel();*/

        // 清理ChatAdapter资源
        if (chatAdapter != null) {
            chatAdapter.cleanup();
        }

    }
}
