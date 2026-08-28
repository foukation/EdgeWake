package com.fxzs.lingxiagent.view.aiwork;

import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.translate.TranslateDetailActivity;
import com.fxzs.lingxiagent.model.aiwork.AiWorkFilterBean;
import com.fxzs.lingxiagent.model.auth.AuthHelper;
import com.fxzs.lingxiagent.model.chat.dto.DrawingToChatBean;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSessionDto;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepositoryImpl;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingDto;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepository;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepositoryImpl;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.util.ShadowUtils;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.auth.OneClickLoginActivity;
import com.fxzs.lingxiagent.view.chat.SuperChatContainActivity;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.EditInfoDialog;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.drawing.DrawingContainActivity;
import com.fxzs.lingxiagent.view.meeting.MeetingActivity;
import com.fxzs.lingxiagent.view.meeting.MeetingContainActivity;
import com.fxzs.lingxiagent.view.ppt.PptTopicInputActivity;
import com.fxzs.lingxiagent.view.user.HistoryItem;
import com.fxzs.lingxiagent.view.user.UserActivity;
import com.fxzs.lingxiagent.viewmodel.aiwork.VMAiWork;
import com.fxzs.lingxiagent.viewmodel.history.VMHistory;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

/*老版本*/
public class AiWorkFragmentV1 extends BaseFragment<VMAiWork> {
    private TextView tv_title;
    private View iv_filter;
    private RecyclerView rv;
    private SwipeRefreshLayout swipeRefreshLayout;
    AiWorkAdapter adapter;
    private View ll_empty;
    private int selectFilter;
    private ImageView iv_avatar;
    private AiWorkFilterBean selectAiWorkFilterBean;
    private View ll_translate_btn;
    private TextView tv_listen;
    private TextView tv_dialog;
    String currentTranslateType = "1";//1- 聆听 2-对话
    boolean needRefresh = true;
    private boolean isUserPullRefresh = false; // 追踪是否为用户下拉刷新

    @Override
    protected int getLayoutResource() {
        return R.layout.ai_work;
    }

    @Override
    protected Class<VMAiWork> getViewModelClass() {
        return VMAiWork.class;
    }

    @Override
    protected void initializeViews(View view) {

        tv_title = view.findViewById(R.id.tv_title);
        iv_filter = view.findViewById(R.id.iv_filter);
        rv = view.findViewById(R.id.rv);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        ll_empty = view.findViewById(R.id.ll_empty);
        iv_avatar = view.findViewById(R.id.iv_avatar);
        ll_translate_btn = view.findViewById(R.id.ll_translate_btn);
        tv_listen = view.findViewById(R.id.tv_listen);
        tv_dialog = view.findViewById(R.id.tv_dialog);

        // Quick action buttons
        View btnMeeting = view.findViewById(R.id.ll_meeting);
        View btnPpt = view.findViewById(R.id.ll_ppt);
        View btnDrawing = view.findViewById(R.id.ll_drawing);
        View btnTranslate = view.findViewById(R.id.ll_translate);

        // Apply unified border + blur (shadow) with preserved paddings
        applyQuickButtonStyle(btnMeeting);
        applyQuickButtonStyle(btnPpt);
        applyQuickButtonStyle(btnDrawing);
        applyQuickButtonStyle(btnTranslate);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(layoutManager);
        List<HistoryItem> list = new ArrayList<>();
//        list.add(new )
        adapter = new AiWorkAdapter(getActivity(),list);
        rv.setAdapter(adapter);
        
        // Setup SwipeRefreshLayout
        setupSwipeRefresh();
        // 设置更多操作监听器
        adapter.setOnMoreActionListener((anchor, item, actionType) -> {
            if (actionType == 0) { // 查看详情
//                handleViewDetail(item);
            } else if (actionType == 1) { // 重命名
                // 弹出输入框，确认后重命名
                showEditNameDialog(item);
            } else if (actionType == 2) { // 删除
                // 弹出确认对话框
                CommonDialog.showWarningDialog(getContext(), "是否删除该内容？", "删除后，内容无法恢复，请谨慎操作。",
                        "删除", new CommonDialog.OnDialogClickListener() {
                            @Override
                            public void onConfirm() {

                                deleteHistory(item);
                            }

                            @Override
                            public void onCancel() {

                            }
                        });
            }
        });
        adapter.setOnItemClickListener(item -> {
            int type = adapter.getType();
            if(type == AiWorkAdapter.TYPE_ALL){
                Object st = item.getExtraData("sourceType");
                if(st instanceof Integer){
                    type = (Integer) st;
                } else {
                    type = AiWorkAdapter.TYPE_MEETING; // 兜底
                }
            }

            long id = 0;
            switch (type) {
                case AiWorkAdapter.TYPE_DRAWING:
                    // AI绘画tab，调用详情接口并跳转
                    loadDrawingSessionDetail(item.getSessionId());
                    break;
                case AiWorkAdapter.TYPE_MEETING:
                    // 会议tab，跳转到会议详情页面
                    jumpToMeetingDetail(item.getMeetingId(), item.getMeetingType());
                    break;
                case AiWorkAdapter.TYPE_PPT:
                    // PPT tab，跳转到PPT详情页面
                    handlePptHistoryClick(item);
                    break;
                case AiWorkAdapter.TYPE_TRANSLATE:
                    //同传详情页面
                    Intent intent = new Intent(getActivity(), TranslateDetailActivity.class);
                    intent.putExtra(Constant.INTENT_ID,item.getConversationId());
                    intent.putExtra(Constant.INTENT_TYPE,currentTranslateType);
                    startActivity(intent);
                    break;
            }

            needRefresh = false;
            // 其他tab的处理可以在这里添加
        });

        iv_filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ZUtils.showAIWorkFilterPopup(getActivity(), view, selectAiWorkFilterBean, new AiWorkFilterAdapter.OnOptionSelectedListener() {
                    @Override
                    public void onOptionSelected(AiWorkFilterBean option) {
                        selectAiWorkFilterBean = option;
                        selectFilter = option.getType();
                        ll_translate_btn.setVisibility(View.GONE);
                        viewModel.getVmHistory().setAll(false);
                        
                        // 先清空当前数据，避免显示不匹配的历史数据
                        if (adapter != null) {
                            adapter.setItems(new ArrayList<>());
                        }
                        
                        String title = "历史记录";
                        if(option.getType() == AiWorkAdapter.TYPE_ALL){
                            viewModel.getVmHistory().setAll(true);
                            // 混合模式：请求全部接口
                            viewModel.getVmHistory().loadAllHistory(true);
                        }else if(option.getType() == AiWorkAdapter.TYPE_TRANSLATE){
                            viewModel.loadHistoryTranslate("1");
                            ll_translate_btn.setVisibility(View.VISIBLE);
                        }else{
                            viewModel.loadHistory(selectFilter);
                        }
                        title = option.getName()+title;

                        adapter.setType(option.getType());

                        tv_title.setText(title);
                    }
                });
            }
        });

        viewModel.setActivity(requireActivity());
        
        // 默认筛选为"全部"
        selectFilter = AiWorkAdapter.TYPE_ALL;
        adapter.setType(AiWorkAdapter.TYPE_ALL);
        viewModel.getVmHistory().setAll(true);
        
        // 延迟加载初始数据，避免数据闪现
        rv.post(() -> {
            viewModel.getVmHistory().loadAllHistory(true);
        });

        findViewById(R.id.iv_avatar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AuthHelper.getInstance().isLogin()) {
                    // 跳转到设置界面
                    Intent intent = new Intent(getActivity(), UserActivity.class);
                    startActivity(intent);
                } else {
                    // 未登录，跳转到一键登录页面
                    Intent intent = new Intent(getActivity(), OneClickLoginActivity.class);
                    getActivity().startActivity(intent);
                }
            }
        });
        findViewById(R.id.ll_meeting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), MeetingContainActivity.class));
            }
        });

        findViewById(R.id.ll_ppt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 跳转到PPT主题输入页面
                startActivity(new Intent(getActivity(), PptTopicInputActivity.class));
            }
        });

        findViewById(R.id.ll_drawing).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), DrawingContainActivity.class));
            }
        });

        findViewById(R.id.ll_translate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), com.fxzs.lingxiagent.lingxi.translate.SimultaneousTranslateActivity.class));
            }
        });
        tv_listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentTranslateType = "1";
                
                // 清空当前数据，避免显示对话模式的数据
                if (adapter != null) {
                    adapter.setItems(new ArrayList<>());
                }
                
                viewModel.loadHistoryTranslate("1");
                ZUtils.setViewBg(getActivity(),tv_listen,R.drawable.bg_stoke_blue_r16);
                ZUtils.setViewBg(getActivity(),tv_dialog,R.drawable.bg_stoke_e0_r16);

                ZUtils.setTextColor(getActivity(),tv_listen,R.color.figma_primary_blue);
                ZUtils.setTextColor(getActivity(),tv_dialog,R.color.figma_text_hint);
            }
        });
        tv_dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentTranslateType = "2";
                
                // 清空当前数据，避免显示聆听模式的数据
                if (adapter != null) {
                    adapter.setItems(new ArrayList<>());
                }
                
                viewModel.loadHistoryTranslate("2");
                ZUtils.setViewBg(getActivity(),tv_listen,R.drawable.bg_stoke_e0_r16);
                ZUtils.setViewBg(getActivity(),tv_dialog,R.drawable.bg_stoke_blue_r16);

                ZUtils.setTextColor(getActivity(),tv_listen,R.color.figma_text_hint);
                ZUtils.setTextColor(getActivity(),tv_dialog,R.color.figma_primary_blue);
            }
        });
        // 添加滚动监听器实现分页加载
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // 只在向下滚动时检查
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    // 当滚动到接近底部时（剩余3个item时）触发加载更多
                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 3) {
                        // 检查是否正在加载或刷新，以及是否还有更多数据，避免重复请求
                        Boolean isLoading = viewModel.getVmHistory().getIsLoading().getValue();
                        Boolean isRefreshing = viewModel.getVmHistory().getIsRefreshing().getValue();
                        boolean hasMoreData = viewModel.getVmHistory().hasMoreData();

                        if (!Boolean.TRUE.equals(isLoading) && !Boolean.TRUE.equals(isRefreshing) && hasMoreData) {
                            Timber.tag("HistoryBottomSheet").d( "触发加载更多数据");

                            // 根据当前筛选类型进行分页加载
                            if(selectFilter == AiWorkAdapter.TYPE_ALL){
                                // 混合模式：请求全部接口的下一页
                                viewModel.getVmHistory().loadAllHistory(false);
                            }
                            else if(selectFilter == AiWorkAdapter.TYPE_TRANSLATE){
                                // 翻译模式：加载下一页数据
                                viewModel.getVmHistory().loadTranslateHistory(false, currentTranslateType);
                            }
                            else{
                                // 其他单一类型：调用通用的loadMoreHistory
                                viewModel.getVmHistory().loadMoreHistory();
                            }
                        } else if (!hasMoreData) {
                            Timber.tag("HistoryBottomSheet").d( "已加载全部数据，无更多数据");
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void setupObservers() {

        viewModel.getVmHistory().getHistoryItems().observe(getViewLifecycleOwner(), items -> {
            ZUtils.print("setupObservers == "+items.size());
            if(items.size() > 0){
                ll_empty.setVisibility(View.GONE);
            }else {
                ll_empty.setVisibility(View.VISIBLE);
            }
            if (adapter != null) {
                adapter.setItems(items);
            }
        });

        // 观察刷新状态 - 只在用户下拉刷新时显示动画
        viewModel.getVmHistory().getIsRefreshing().observe(getViewLifecycleOwner(), isRefreshing -> {
            if (swipeRefreshLayout != null) {
                // 只有用户下拉刷新时才显示loading动画
                if (isUserPullRefresh) {
                    swipeRefreshLayout.setRefreshing(isRefreshing);
                    Timber.tag("AiWorkFragment").d( "用户下拉刷新状态更新: " + isRefreshing);
                    // 刷新完成后重置标识
                    if (!isRefreshing) {
                        isUserPullRefresh = false;
                    }
                } else {
                    // 非下拉刷新时，确保不显示loading动画
                    swipeRefreshLayout.setRefreshing(false);
                    Timber.tag("AiWorkFragment").d( "非下拉刷新，隐藏loading动画");
                }
            }
        });

        // 观察加载状态
        viewModel.getVmHistory().getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Timber.tag("AiWorkFragment").d( "加载状态更新: " + isLoading);
            // 这里可以处理其他loading UI，如果有的话
        });

        viewModel.getVmUserProfile().getAvatarUrl().observe(getViewLifecycleOwner(), avatarUrl ->
//                    loadAvatarUrl(avatarUrl)
                        ImageUtil.netCircle(getActivity(),avatarUrl,iv_avatar)
        );
    }


    private void showEditNameDialog(HistoryItem item) {
        EditInfoDialog editDialog = new EditInfoDialog(getActivity())
                .setTitle("重命名")
                .setHint("请输入对话名称")
                .setText(item.getTitle())
                .setCancelText("取消")
                .setConfirmText("保存")
                .setConfirmTextColor(R.color.dialog_save)
                .setMaxLength(20)
                .setOnEditInfoDialogListener(new EditInfoDialog.OnEditInfoDialogListener() {
                    @Override
                    public void onConfirm(String inputText) {
                        // 空值验证已在 EditInfoDialog 中处理
//                        if(selectFilter == AiWorkAdapter.TYPE_TRANSLATE){
//                            viewModel.getVmHistory().updateTranslateName(item,currentTranslateType,inputText);
//                        } else
                            if (selectFilter == AiWorkAdapter.TYPE_ALL) {
                            int type = adapter.getType();

                            Object st = item.getExtraData("sourceType");
                            if(st instanceof Integer){
                                type = (Integer) st;
                            } else {
                                type = AiWorkAdapter.TYPE_MEETING; // 兜底
                            }
                           int currentTab = VMHistory.TAB_MEETING;
                            switch (type) {
                                case AiWorkAdapter.TYPE_DRAWING:
                                    currentTab = VMHistory.TAB_DRAWING;
                                    break;
                                case AiWorkAdapter.TYPE_MEETING:
                                    currentTab = VMHistory.TAB_MEETING;
                                    break;
                                case AiWorkAdapter.TYPE_PPT:
                                    currentTab = VMHistory.TAB_PPT;
                                    break;
                                case AiWorkAdapter.TYPE_TRANSLATE:
                                    currentTab = VMHistory.TAB_TRANSLATE;
                                    break;
                            }
                            viewModel.getVmHistory().updateName(item,inputText,currentTab);
                        } else {
                            viewModel.getVmHistory().updateName(item,inputText);
                        }
                    }

                    @Override
                    public void onCancel() {
                        // 取消编辑
                    }
                });
        editDialog.show();
    }


    private void deleteHistory(HistoryItem item) {
        int type = adapter.getType();

        long id = 0;
        if (selectFilter == AiWorkAdapter.TYPE_ALL) {
            Object st = item.getExtraData("sourceType");
            if(st instanceof Integer){
                type = (Integer) st;
            } else {
                type = AiWorkAdapter.TYPE_MEETING; // 兜底
            }
        }
        switch (type) {
            case AiWorkAdapter.TYPE_DRAWING:
                id = item.getSessionId();
                deleteDrawing(id);
                break;
            case AiWorkAdapter.TYPE_MEETING:
                id = item.getMeetingId();
                deleteMeeting(id + "");
                break;
            case AiWorkAdapter.TYPE_PPT:
                id = item.getPptSessionId();
                deletePPT(id + "");
                break;
            case AiWorkAdapter.TYPE_TRANSLATE:
                id = item.getConversationId();
                deleteTranslate(id + "");
                break;
        }
    }
    public void deleteMeeting(String meetingId) {
//        setLoading(true);
        MeetingRepository repository = new MeetingRepositoryImpl();
        repository.deleteMeeting(meetingId).observeForever(result -> {
//            setLoading(false);
            if (result != null && result.isSuccess()) {
                GlobalToast.show(getActivity(),"删除成功",GlobalToast.Type.SUCCESS);
                viewModel.getVmHistory().refreshHistory();
            } else {
//                setError(result != null ? result.getError() : "删除失败");
            }
        });
    }
    public void deletePPT(String pptId) {
//        setLoading(true);
        HttpRequest request = new HttpRequest();

        request.deletePPT(pptId, new Observer<ApiResponse<Integer>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<Integer> result) {
                if (result != null && result.getData() == 1) {
                    GlobalToast.show(getActivity(),"删除成功",GlobalToast.Type.SUCCESS);
                    viewModel.getVmHistory().refreshHistory();
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
    public void deleteTranslate(String translateId) {
//        setLoading(true);
        HttpRequest request = new HttpRequest();

        request.deleteById(translateId,currentTranslateType, new Observer<ApiResponse<String>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<String> result) {
                if (result != null && result.getData().equals("success")) {
                    GlobalToast.show(getActivity(),"删除成功",GlobalToast.Type.SUCCESS);
                    viewModel.getVmHistory().refreshHistory();
//                    viewModel.getVmHistory().loadTranslateHistory(true,currentTranslateType);
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
    private void deleteDrawing(long id) {
        DrawingRepository drawingRepository = DrawingRepositoryImpl.getInstance();
        drawingRepository.deleteAllSessions(id).observeForever(result -> {
            if (result != null && result.isSuccess() && result.getData() != null) {
                GlobalToast.show(getActivity(),"删除成功",GlobalToast.Type.SUCCESS);
                viewModel.getVmHistory().refreshHistory();
            } else {
                GlobalToast.show(getActivity(),"删除失败", GlobalToast.Type.ERROR);
            }
        });
    }

    private void loadDrawingSessionDetail(Long sessionId) {
        // 显示加载框
//        LoadingProgressDialog loadingDialog =
//                new LoadingProgressDialog(getContext())
//                        .setMessage("加载中...")
//                        .setCancelable(false);
//        loadingDialog.show();

        // 调用API获取会话详情
        DrawingRepository drawingRepository = DrawingRepositoryImpl.getInstance();
        drawingRepository.getSessionDetailById(sessionId).observeForever(result -> {
//            loadingDialog.dismiss();

            if (result.isSuccess() && result.getData() != null) {
                DrawingSessionDto sessionDetail = result.getData();

                // 跳转到DrawingChatActivity
//                android.content.Intent intent = new android.content.Intent(getContext(),
//                    com.fxzs.drawing.view.lingxiagent.DrawingChatActivity.class);
//                intent.putExtra("sessionId", sessionId);
//                intent.putExtra("sessionDetail", sessionDetail);
                Intent intent = new Intent(getContext(),
                        SuperChatContainActivity.class);
                intent.putExtra(Constant.INTENT_TYPE, SuperChatContainActivity.TYPE_DRAWING);

                DrawingToChatBean drawingToChatBean = new DrawingToChatBean();
                drawingToChatBean.setSessionId(sessionId);
                drawingToChatBean.setSessionDetail(sessionDetail);

                intent.putExtra(Constant.INTENT_DATA, drawingToChatBean);
                startActivity(intent);
            } else {
                // 显示错误提示
                GlobalToast.show(getActivity(),
                        "获取详情失败：" + (result.getError() != null ? result.getError() : "未知错误"),
                        GlobalToast.Type.ERROR);
            }
        });
    }



    /**
     * 跳转到会议详情页面
     */
    private void jumpToMeetingDetail(Long meetingId, Integer meetingType) {
        Timber.tag("HistoryBottomSheet").d( "跳转到会议详情，meetingId: " + meetingId + ", meetingType: " + meetingType);

        // 显示加载提示
//        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
//        progressDialog.setMessage("正在加载会议详情...");
//        progressDialog.setCancelable(false);
//        progressDialog.show();

        try {
            // 先查询会议详情获取转写内容
            MeetingRepository meetingRepository = new MeetingRepositoryImpl();
            meetingRepository.getMeetingDetail(meetingId.toString()).observeForever(result -> {
                // 隐藏加载提示
//                if (progressDialog.isShowing()) {
//                    progressDialog.dismiss();
//                }

                if (result != null && result.isSuccess() && result.getData() != null) {
                    MeetingDto meeting = result.getData();
                    String transcriptionResult = meeting.getMeetingText(); // content字段就是转写结果

                    // 获取会议标题，优先使用name，其次使用title
                    String meetingTitle = meeting.getName();
                    if (meetingTitle == null || meetingTitle.trim().isEmpty()) {
                        meetingTitle = meeting.getTitle();
                    }
                    if (meetingTitle == null || meetingTitle.trim().isEmpty()) {
                        meetingTitle = "会议详情";
                    }

                    Timber.tag("HistoryBottomSheet").d( "获取到会议转写内容，长度: " +
                            (transcriptionResult != null ? transcriptionResult.length() : 0) +
                            ", 会议标题: " + meetingTitle);

                    // 使用获取到的转写内容和标题跳转
                    Intent intent = MeetingActivity.createIntent(
                            getContext(),
                            meetingId.toString(), // 会议ID
                            transcriptionResult != null ? transcriptionResult : "", // 实际的转写结果
                            0, // tabType，会议内容（默认显示第一个tab）
                            meetingTitle // 会议标题
                    );
                    startActivity(intent);

                } else {
                    // 查询失败，仍然跳转但不传递转写内容
                    Timber.tag("HistoryBottomSheet").w("获取会议详情失败: " +
                            (result != null ? result.getError() : "未知错误"));

                    Intent intent = MeetingActivity.createIntent(
                            getContext(),
                            meetingId.toString(), // 会议ID
                            "", // 转写结果为空，让详情页自己加载
                            0, // tabType，会议内容
                            "会议详情" // 默认标题
                    );
                    startActivity(intent);

                    GlobalToast.show(getActivity(),
                            "会议详情加载失败，但仍可查看",
                            GlobalToast.Type.NORMAL);
                }
            });

        } catch (Exception e) {
            // 隐藏加载提示
//            if (progressDialog.isShowing()) {
//                progressDialog.dismiss();
//            }

            Timber.tag("HistoryBottomSheet").e( "跳转到会议详情失败", e);
            GlobalToast.show(getActivity(),
                    "打开会议详情失败",
                    GlobalToast.Type.ERROR);
        }
    }


    /**
     * 处理PPT历史记录点击
     */
    private void handlePptHistoryClick(HistoryItem item) {
        Timber.tag("HistoryBottomSheet").d( "处理PPT历史记录点击 - SessionId: " + item.getPptSessionId());

        com.fxzs.lingxiagent.model.ppt.dto.PptSessionDto session =
                (com.fxzs.lingxiagent.model.ppt.dto.PptSessionDto) item.getExtraData("session");

        if (session == null) {
            Timber.tag("HistoryBottomSheet").e( "PPT会话数据为空");
//            GlobalToast.show(getActivity(), "PPT会话数据异常", GlobalToast.Type.ERROR);
            GlobalToast.show(getActivity(), "ppt不存在", GlobalToast.Type.ERROR);
            return;
        }

        Timber.tag("HistoryBottomSheet").d( "PPT会话数据 - ID: " + session.getId() + ", Title: " + session.getTitle() +
                ", Tasks数量: " + (session.getPptTasks() != null ? session.getPptTasks().size() : 0));

        if (session.getPptTasks() != null && !session.getPptTasks().isEmpty()) {
            com.fxzs.lingxiagent.model.ppt.dto.PptSessionDto.PptTask firstTask = session.getPptTasks().get(0);
            jumpToPptPreview(firstTask.getPptUrl(), session.getTitle(), session.getTitle(), session.getId(), firstTask.getTaskId());
        }else{
            GlobalToast.show(getActivity(), "ppt不存在", GlobalToast.Type.ERROR);
        }
        // 优先检查是否有PPT任务，如果有则直接跳转到预览页面
//        if (session.getPptTasks() != null && !session.getPptTasks().isEmpty()) {
//            com.fxzs.lingxiagent.model.ppt.dto.PptSessionDto.PptTask firstTask = session.getPptTasks().get(0);
//
//            // 无论pptUrl是否为空，都跳转到预览页面，让预览页面处理
//            jumpToPptPreview(firstTask.getPptUrl(), firstTask.getPptTitle(), session.getTitle(), session.getId());
//            return;
//        }

        // 如果没有PPT任务，检查是否有大纲，跳转到大纲编辑页面
//        if (session.getPptCatalogs() != null && !session.getPptCatalogs().isEmpty()) {
//            jumpToPptOutlineEdit(session);
//            return;
//        }

        // 否则跳转到主题输入页面，预填充主题
//        jumpToPptTopicInput(session.getTitle());
    }

    /**
     * 跳转到PPT预览页面
     */
    private void jumpToPptPreview(String pptUrl, String pptTitle, String topic, int sessionId, String taskId) {
        try {
            Intent intent = new Intent(getActivity(), com.fxzs.lingxiagent.view.ppt.PptPreviewActivity.class);

            // 传递PPT URL（可能为null或空字符串）
            if (pptUrl != null && !pptUrl.isEmpty()) {
                intent.putExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_PPT_URL, pptUrl);
            }

            // 传递主题信息
            String displayTopic = topic != null ? topic : pptTitle;
            if (displayTopic != null && !displayTopic.isEmpty()) {
                intent.putExtra(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_TOPIC, displayTopic);
            }

            // 传递会话ID和任务ID，用于标识这是从历史记录跳转的
            intent.putExtra("session_id", sessionId);
            if (taskId != null && !taskId.isEmpty()) {
                intent.putExtra("task_id", taskId);
            }
            intent.putExtra("from_history", true);

            Timber.tag("HistoryBottomSheet").d( "跳转PPT预览页面 - URL: " + pptUrl + ", Topic: " + displayTopic + ", SessionId: " + sessionId + ", TaskId: " + taskId);

            startActivity(intent);
        } catch (Exception e) {
            Timber.tag("HistoryBottomSheet").e( "跳转PPT预览页面失败", e);
            GlobalToast.show(getActivity(), "跳转失败", GlobalToast.Type.ERROR);
        }
    }

    /**
     * 配置下拉刷新功能
     */
    private void setupSwipeRefresh() {
        // 初始时隐藏loading动画
        swipeRefreshLayout.setRefreshing(false);
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Timber.tag("AiWorkFragment").d( "用户下拉刷新被触发");
            isUserPullRefresh = true; // 标记为用户下拉刷新
            refreshCurrentData();
        });
        
        // 设置下拉刷新的颜色主题
        swipeRefreshLayout.setColorSchemeColors(
            0xFF1976D2, // 蓝色
            0xFF388E3C, // 绿色
            0xFFFF5722, // 橙色
            0xFF7B1FA2  // 紫色
        );
        
        // 设置背景颜色
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(0xFFFFFFFF);
        
        // 设置刷新圆圈的大小
        swipeRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        
        Timber.tag("AiWorkFragment").d( "SwipeRefreshLayout 配置完成，初始loading已隐藏");
    }

    /**
     * 刷新当前数据
     */
    private void refreshCurrentData() {
        if (selectFilter == AiWorkAdapter.TYPE_ALL) {
            viewModel.getVmHistory().loadAllHistory(true);
        } else if (selectFilter == AiWorkAdapter.TYPE_TRANSLATE) {
            viewModel.loadHistoryTranslate(currentTranslateType);
        } else {
            viewModel.loadHistory(selectFilter);
        }
    }

    /**
     * 为顶部四个快捷入口按钮应用统一描边 + 阴影效果，同时保留原有内边距
     */
    private void applyQuickButtonStyle(View v) {
        if (v == null) return;
        // 确保父容器不裁剪阴影
        if (v.getParent() instanceof ViewGroup) {
            ((ViewGroup) v.getParent()).setClipToPadding(false);
            ((ViewGroup) v.getParent()).setClipChildren(false);
        }

        // 保存原padding
        int pL = v.getPaddingLeft();
        int pT = v.getPaddingTop();
        int pR = v.getPaddingRight();
        int pB = v.getPaddingBottom();

        // 应用阴影（轻量柔和）与8dp圆角；添加1dp描边
        ShadowUtils.applyShadow(
                v,
                requireContext(),
                4, // elevation 与历史卡片一致，阴影更自然
                ContextCompat.getColor(requireContext(), R.color.color_606F8B), // 阴影色
                8, // 圆角与历史卡片一致
                false, // 不需要描边
                Color.TRANSPARENT, // 无描边颜色
                0, // 无描边
                Color.WHITE // 背景色
        );

        // 还原原padding
        v.setPadding(pL, pT, pR, pB);
    }


    @Override
    public void onResume() {
        super.onResume();
        if(needRefresh){
            if( selectFilter == AiWorkAdapter.TYPE_ALL) {
                viewModel.getVmHistory().loadAllHistory(true);
            }else if( selectFilter == AiWorkAdapter.TYPE_TRANSLATE){
                viewModel.loadHistoryTranslate(currentTranslateType);
            }else {
                viewModel.loadHistory(selectFilter);
            }
            viewModel.getVmUserProfile().loadUserProfile();
        }else {
            needRefresh = true;
        }
    }
}
