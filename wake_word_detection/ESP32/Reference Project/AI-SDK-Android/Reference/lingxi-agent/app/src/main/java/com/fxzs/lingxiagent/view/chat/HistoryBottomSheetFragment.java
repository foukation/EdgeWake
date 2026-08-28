package com.fxzs.lingxiagent.view.chat;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.translate.TranslateDetailActivity;
import com.fxzs.lingxiagent.model.chat.dto.ConversationHistoryDto;
import com.fxzs.lingxiagent.model.chat.dto.DrawingToChatBean;
import com.fxzs.lingxiagent.model.chat.dto.OptionModel;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepository;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepositoryImpl;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSessionDto;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepositoryImpl;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingHistoryDto;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepository;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepositoryImpl;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.agent.AgentContainActivity;
import com.fxzs.lingxiagent.view.aiwork.AiWorkAdapter;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.EditInfoDialog;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.common.LoadingProgressDialog;
import com.fxzs.lingxiagent.view.drawing.DrawingImageGenerateActivity;
import com.fxzs.lingxiagent.view.drawing.DrawingTransformStyleItem;
import com.fxzs.lingxiagent.view.excel.AiExcelContainActivity;
import com.fxzs.lingxiagent.view.meeting.MeetingActivity;
import com.fxzs.lingxiagent.view.user.HistoryItem;
import com.fxzs.lingxiagent.viewmodel.history.HistoryViewModelFactory;
import com.fxzs.lingxiagent.viewmodel.history.VMHistory;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

/**
 * 历史记录底部抽屉Fragment
 */
public class HistoryBottomSheetFragment extends BottomSheetDialogFragment {
    
    private ImageView ivClose;
    private TextView tvTitle;
    private TextView tvTabChat;
    private TextView tvTabAgent;
    private TextView tvTabDrawing;
    private TextView tvTabMeeting;
    private TextView tvTabPPT;
    private TextView tvTabTranslate;
    private TextView tvTabExcel;
    private RecyclerView rvHistory;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private AiWorkAdapter historyAdapter;
    private VMHistory viewModel;
    private LinearLayoutManager layoutManager;
    
    // Tab颜色
    private static final int COLOR_SELECTED = 0xFF1E1E1E;
    private static final int COLOR_UNSELECTED = 0xFF999999;
    private static int initTab = VMHistory.TAB_CHAT;
    private View ll_translate_btn;
    private TextView tv_listen;
    private TextView tv_dialog;
    String currentTranslateType = "1";//1- 聆听 2-对话

    private View ll_drawing_type_btn;
    private TextView tv_drawing_text2img;
    private TextView tv_drawing_img2img;
    private int currentDrawingType = 0; // 0-文生图（默认） 1-图生图

    private View ll_empty;
    
    // 用于跟踪是否是首次启动，避免初次进入时触发刷新
    private boolean isFirstStart = true;
    
    // 保存滚动位置
    private int savedScrollPosition = -1;
    private int savedScrollOffset = 0;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private boolean is_tab_hide;
    private View scrollViewTabs;

    /**
     * 创建实例并指定默认选中的tab
     * @param defaultTab 默认选中的tab索引
     * @return HistoryBottomSheetFragment实例
     */
    public static HistoryBottomSheetFragment newInstance(int defaultTab) {
        HistoryBottomSheetFragment fragment = new HistoryBottomSheetFragment();
        Bundle args = new Bundle();
        args.putInt("default_tab", defaultTab);
        args.putBoolean("is_tab_hide", false);
        fragment.setArguments(args);
        return fragment;
    }
    public static HistoryBottomSheetFragment newInstance(int defaultTab,boolean is_tab_hide) {
        HistoryBottomSheetFragment fragment = new HistoryBottomSheetFragment();
        Bundle args = new Bundle();
        args.putInt("default_tab", defaultTab);
        args.putBoolean("is_tab_hide", is_tab_hide);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        
        // 设置底部抽屉行为
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                
                // 获取状态栏高度
                int statusBarHeight = getStatusBarHeight();
                
                // 设置高度为屏幕高度减去状态栏高度，让抽屉顶到状态栏下方
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = getResources().getDisplayMetrics().heightPixels - statusBarHeight;
                bottomSheet.setLayoutParams(layoutParams);
            }
        });
        
        return dialog;
    }
    
    /**
     * 获取状态栏高度
     */
    private int getStatusBarHeight() {
        if (getContext() == null) return 0;
        
        int resourceId = getContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getContext().getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 注册 ActivityResultLauncher
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 处理返回的结果
                    if (result.getResultCode() == 1) {
                        Intent data = result.getData();
                        if (data != null) {
                            long conversationId = data.getLongExtra("result_conversationId", 0);
                            int type = data.getIntExtra("type", -1);
                            String resultTitle = data.getStringExtra("result_title");
                            String resultLastMessage = data.getStringExtra("result_last_message");

                            if (conversationId > 0 && (type == SuperChatContainActivity.TYPE_AGENT || type == SuperChatContainActivity.TYPE_HOME)) {
                                // 不刷新列表，避免分页位置丢失；仅更新当前列表中的标题和最后一条消息
                                viewModel.updateConversationPreview(conversationId, resultTitle, resultLastMessage);
                            }
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_bottom_sheet, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initViewModel();
        setupListeners();
        setupRecyclerView();
        setupSwipeRefresh();
        observeViewModel();

        // 获取默认选中的tab，如果没有传递参数则默认选中对话Tab
        int defaultTab = VMHistory.TAB_CHAT;
        if (getArguments() != null) {
            defaultTab = getArguments().getInt("default_tab", VMHistory.TAB_CHAT);
            is_tab_hide = getArguments().getBoolean("is_tab_hide", false);
            if(is_tab_hide){
                scrollViewTabs.setVisibility(View.GONE);
            }
        }
        viewModel.selectTab(defaultTab);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Timber.tag("HistoryBottomSheet").d( "onResume - isFirstStart: " + isFirstStart);
        
        // 当从对话页面返回时，静默刷新历史记录以获取最新的排序
        // 但跳过首次启动时的刷新，避免重复加载
        if (viewModel != null && !isFirstStart) {
            Timber.tag("HistoryBottomSheet").d( "执行静默刷新历史记录");
//            viewModel.silentRefreshHistory();
//
//            // 刷新后恢复滚动位置
//            if (savedScrollPosition >= 0 && rvHistory != null && layoutManager != null) {
//                rvHistory.post(() -> {
//                    layoutManager.scrollToPositionWithOffset(savedScrollPosition, savedScrollOffset);
//                    Timber.tag("HistoryBottomSheet").d("恢复滚动位置: position=" + savedScrollPosition + ", offset=" + savedScrollOffset);
//                });
//            }
        }
        
        // 标记已经不是首次启动
        isFirstStart = false;
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // 保存当前滚动位置
        if (layoutManager != null && rvHistory != null) {
            savedScrollPosition = layoutManager.findFirstVisibleItemPosition();
            View firstVisibleView = layoutManager.findViewByPosition(savedScrollPosition);
            if (firstVisibleView != null) {
                savedScrollOffset = firstVisibleView.getTop();
            } else {
                savedScrollOffset = 0;
            }
            Timber.tag("HistoryBottomSheet").d("保存滚动位置: position=" + savedScrollPosition + ", offset=" + savedScrollOffset);
        }
    }
    
    private void initViews(View view) {
        ivClose = view.findViewById(R.id.ivClose);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvTabChat = view.findViewById(R.id.tvTabChat);
        tvTabAgent = view.findViewById(R.id.tvTabAgent);
        tvTabDrawing = view.findViewById(R.id.tvTabDrawing);
        tvTabMeeting = view.findViewById(R.id.tvTabMeeting);
        tvTabPPT = view.findViewById(R.id.tvTabPPT);
        tvTabTranslate = view.findViewById(R.id.tvTabTranslate);
        tvTabExcel = view.findViewById(R.id.tvTabExcel);
        rvHistory = view.findViewById(R.id.rvHistory);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        ll_translate_btn = view.findViewById(R.id.ll_translate_btn);
        tv_listen = view.findViewById(R.id.tv_listen);
        tv_dialog = view.findViewById(R.id.tv_dialog);

        ll_drawing_type_btn = view.findViewById(R.id.ll_drawing_type_btn);
        tv_drawing_text2img = view.findViewById(R.id.tv_drawing_text2img);
        tv_drawing_img2img = view.findViewById(R.id.tv_drawing_img2img);

        ll_empty = view.findViewById(R.id.ll_empty);
        scrollViewTabs = view.findViewById(R.id.scrollViewTabs);
    }

    private void initViewModel() {
        HistoryViewModelFactory factory = new HistoryViewModelFactory();
        viewModel = new ViewModelProvider(this, factory).get(VMHistory.class);

        // 检查是否有默认选中的tab
        Bundle args = getArguments();
        if (args != null && args.containsKey("default_tab")) {
            int defaultTab = args.getInt("default_tab", VMHistory.TAB_CHAT);
            Timber.tag("HistoryBottomSheet").d( "设置默认tab: " + defaultTab + " (PPT=" + VMHistory.TAB_PPT + ")");
            viewModel.selectTab(defaultTab);
        } else {
            // 默认选择聊天tab
            Timber.tag("HistoryBottomSheet").d("使用默认聊天tab");
            viewModel.selectTab(VMHistory.TAB_CHAT);
        }
    }

    private void observeViewModel() {
        // 观察当前Tab变化
        viewModel.getCurrentTabIndex().observe(getViewLifecycleOwner(), tabIndex -> {
            updateTabSelection(tabIndex);

            ll_translate_btn.setVisibility(View.GONE);
            ll_drawing_type_btn.setVisibility(View.GONE);
            int type = AiWorkAdapter.TYPE_MEETING;
            switch (tabIndex){
                case VMHistory.TAB_CHAT:
                    type = AiWorkAdapter.TYPE_CHAT;
                    break;
                case VMHistory.TAB_AGENT:
                    type = AiWorkAdapter.TYPE_AGENT;
                    break;
                case VMHistory.TAB_DRAWING:
                    type = AiWorkAdapter.TYPE_DRAWING;
                    ll_drawing_type_btn.setVisibility(View.VISIBLE);
                    // 默认文生图
                    if (currentDrawingType != 0) {
                        currentDrawingType = 0;
                    }
                    syncDrawingTypeUI();
                    viewModel.setDrawingType(currentDrawingType);
                    break;
                case VMHistory.TAB_MEETING:
                    type = AiWorkAdapter.TYPE_MEETING;
                    break;
                case VMHistory.TAB_PPT:
                    type = AiWorkAdapter.TYPE_PPT;
                    break;
                case VMHistory.TAB_TRANSLATE:
                    type = AiWorkAdapter.TYPE_TRANSLATE;
                    ll_translate_btn.setVisibility(View.VISIBLE);
                    break;
            }
            historyAdapter.setType(type);
            // 更新Adapter的绘画Tab模式
//            if (historyAdapter != null) {
//                historyAdapter.setIsDrawingTab(tabIndex == VMHistory.TAB_DRAWING);
//            }
        });

        // 观察历史记录数据变化
        viewModel.getHistoryItems().observe(getViewLifecycleOwner(), items -> {

            if(items.size() > 0){
                ll_empty.setVisibility(View.GONE);
            }else {
                if(viewModel.isInit == false){
                    ll_empty.setVisibility(View.VISIBLE);
                }else {
                    ll_empty.setVisibility(View.GONE);
//                    viewModel.isInit = false;
                }
            }
            if(viewModel.isInit == true){
                viewModel.isInit = false;
            }
            if (historyAdapter != null) {
                historyAdapter.setItems(items);
                // 延迟检查是否在底部且没有更多数据，多次检查确保布局完成
                rvHistory.post(() -> checkAndShowBottomHint());
                rvHistory.postDelayed(() -> checkAndShowBottomHint(), 100);
                rvHistory.postDelayed(() -> checkAndShowBottomHint(), 300);
            }
        });

        // 观察加载状态
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // 当加载完成时（从 true 变为 false），检查是否在底部
            if (!Boolean.TRUE.equals(isLoading) && rvHistory != null && layoutManager != null) {
                // 延迟检查，确保 RecyclerView 布局完成
                rvHistory.post(() -> checkAndShowBottomHint());
                rvHistory.postDelayed(() -> checkAndShowBottomHint(), 100);
                rvHistory.postDelayed(() -> checkAndShowBottomHint(), 300);
            }
        });

        // 观察刷新状态
        viewModel.getIsRefreshing().observe(getViewLifecycleOwner(), isRefreshing -> {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(isRefreshing);
                Timber.tag("HistoryBottomSheet").d( "刷新状态更新: " + isRefreshing);
            }
        });

        // 观察错误信息
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                GlobalToast.show(getActivity(), error, GlobalToast.Type.ERROR);
            }
        });
    }
    
    private void setupListeners() {
        ivClose.setOnClickListener(v -> dismiss());

        tvTabChat.setOnClickListener(v -> viewModel.selectTab(VMHistory.TAB_CHAT));
        tvTabAgent.setOnClickListener(v -> viewModel.selectTab(VMHistory.TAB_AGENT));
        tvTabDrawing.setOnClickListener(v -> viewModel.selectTab(VMHistory.TAB_DRAWING));
        tvTabMeeting.setOnClickListener(v -> viewModel.selectTab(VMHistory.TAB_MEETING));
        tvTabPPT.setOnClickListener(v -> viewModel.selectTab(VMHistory.TAB_PPT));
        tvTabTranslate.setOnClickListener(v -> viewModel.selectTab(VMHistory.TAB_TRANSLATE));
        tvTabExcel.setOnClickListener(v -> viewModel.selectTab(VMHistory.TAB_EXCEL));

        tv_listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentTranslateType = "1";
                viewModel.loadTranslateHistory(true,currentTranslateType);
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
                viewModel.loadTranslateHistory(true,currentTranslateType);
                ZUtils.setViewBg(getActivity(),tv_listen,R.drawable.bg_stoke_e0_r16);
                ZUtils.setViewBg(getActivity(),tv_dialog,R.drawable.bg_stoke_blue_r16);

                ZUtils.setTextColor(getActivity(),tv_listen,R.color.figma_text_hint);
                ZUtils.setTextColor(getActivity(),tv_dialog,R.color.figma_primary_blue);
            }
        });

        tv_drawing_text2img.setOnClickListener(v -> {
            currentDrawingType = 0;
            syncDrawingTypeUI();
            viewModel.setDrawingType(currentDrawingType);
        });

        tv_drawing_img2img.setOnClickListener(v -> {
            currentDrawingType = 1;
            syncDrawingTypeUI();
            viewModel.setDrawingType(currentDrawingType);
        });
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Timber.tag("HistoryBottomSheet").d( "下拉刷新被触发");
            viewModel.refreshHistory();
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

        Timber.tag("HistoryBottomSheet").d( "SwipeRefreshLayout 配置完成");
    }

    private void syncDrawingTypeUI() {
        if (currentDrawingType == 0) { // 文生图
            ZUtils.setViewBg(getActivity(), tv_drawing_text2img, R.drawable.bg_stoke_blue_r16);
            ZUtils.setTextColor(getActivity(), tv_drawing_text2img, R.color.figma_primary_blue);
            ZUtils.setViewBg(getActivity(), tv_drawing_img2img, R.drawable.bg_stoke_e0_r16);
            ZUtils.setTextColor(getActivity(), tv_drawing_img2img, R.color.figma_text_hint);
        } else { // 图生图
            ZUtils.setViewBg(getActivity(), tv_drawing_text2img, R.drawable.bg_stoke_e0_r16);
            ZUtils.setTextColor(getActivity(), tv_drawing_text2img, R.color.figma_text_hint);
            ZUtils.setViewBg(getActivity(), tv_drawing_img2img, R.drawable.bg_stoke_blue_r16);
            ZUtils.setTextColor(getActivity(), tv_drawing_img2img, R.color.figma_primary_blue);
        }
    }

    /**
     * 检查是否在底部且没有更多数据，如果是则显示底部提示
     */
    private void checkAndShowBottomHint() {
        if (rvHistory == null || layoutManager == null || historyAdapter == null) {
            return;
        }
        
        int totalItemCount = layoutManager.getItemCount();
        int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
        boolean hasMoreData = viewModel.hasMoreData();
        // 判断列表内容是否足够滚动（数据不满一页时不显示“已滑到底部”）
        boolean isScrollable = rvHistory.canScrollVertically(1) || rvHistory.canScrollVertically(-1);
        
        // 如果已经在底部（最后一个可见项是最后一个item）、没有更多数据，且列表可滚动，才显示提示
        if (!hasMoreData && totalItemCount > 0 && lastVisiblePosition >= totalItemCount - 1 && isScrollable) {
            historyAdapter.setShowBottomHint(true);
            Timber.tag("HistoryBottomSheet").d("显示底部提示 - totalItemCount: " + totalItemCount + ", lastVisiblePosition: " + lastVisiblePosition);
        } else {
            historyAdapter.setShowBottomHint(false);
        }
    }

    private void setupRecyclerView() {
        historyAdapter = new AiWorkAdapter(getActivity());
        layoutManager = new LinearLayoutManager(getContext());
        rvHistory.setLayoutManager(layoutManager);
        rvHistory.setAdapter(historyAdapter);

        
        // 添加滚动监听器实现分页加载
        rvHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();
                int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();

                // 检查是否已经到达底部（最后一个可见项是最后一个item或接近最后一个item）
                boolean isAtBottom = totalItemCount > 0 && lastVisiblePosition >= totalItemCount - 1;

                // 当滚动到接近底部时（剩余2个item时）触发加载更多
                if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 2 && totalItemCount > 0) {
                    // 检查是否正在加载或刷新，以及是否还有更多数据，避免重复请求
                    Boolean isLoading = viewModel.getIsLoading().getValue();
                    Boolean isRefreshing = viewModel.getIsRefreshing().getValue();
                    boolean hasMoreData = viewModel.hasMoreData();

                    if (!Boolean.TRUE.equals(isLoading) && !Boolean.TRUE.equals(isRefreshing) && hasMoreData) {
                        Timber.tag("HistoryBottomSheet").d( "触发加载更多数据 - 当前页: " + viewModel.getCurrentPage() + 
                            ", 总项目数: " + totalItemCount + ", 可见项目数: " + visibleItemCount);
                        if (historyAdapter != null) {
                            historyAdapter.setShowBottomHint(false);
                        }
                        viewModel.loadMoreHistory();
                    } else if (!hasMoreData) {
                        Timber.tag("HistoryBottomSheet").d( "已加载全部数据，无更多数据");
                        // 使用统一方法检查并显示底部提示
                        checkAndShowBottomHint();
                    } else {
                        Timber.tag("HistoryBottomSheet").d( "跳过加载更多 - isLoading: " + isLoading + 
                            ", isRefreshing: " + isRefreshing + ", hasMoreData: " + hasMoreData);
                    }
                } else if (isAtBottom) {
                    // 即使没有触发加载更多，如果已经在底部且没有更多数据，也显示提示
                    checkAndShowBottomHint();
                }
            }
        });

        // 设置点击监听器
        historyAdapter.setOnItemClickListener(item -> {
            Integer currentTab = viewModel.getCurrentTabIndex().getValue();
            if (currentTab == null) return;
            
            Timber.tag("HistoryBottomSheet").e( "=== 点击历史记录项 ===");
            Timber.tag("HistoryBottomSheet").e( "当前Tab: " + currentTab + ", PPT Tab: " + VMHistory.TAB_PPT);
            Timber.tag("HistoryBottomSheet").e( "项目标题: " + item.getTitle() + ", PPT SessionId: " + item.getPptSessionId());
            Timber.i("=== HistoryBottomSheet: 点击历史记录 - " + item.getTitle() + " ===");

            if (currentTab == VMHistory.TAB_CHAT && item.getConversationId() != null) {
                // 对话tab，跳转到对话页面
//                jumpToConversation(item.getConversationId(), item.getModelType(),0);
                jumpToConversation(item);
            } else if (currentTab == VMHistory.TAB_AGENT && item.getConversationId() != null) {
                // 智能体tab，跳转到对话页面
                jumpToConversation(item);
            }else if (currentTab == VMHistory.TAB_EXCEL && item.getConversationId() != null) {
                // ai表格tab，跳转到对话页面
                Intent intent = new Intent(getActivity(), AiExcelContainActivity.class);
                intent.putExtra(Constant.INTENT_ID, item.getConversationId()+"");
                startActivity(intent);
            } else if (currentTab == VMHistory.TAB_DRAWING && item.getSessionId() != null) {
                // AI绘画tab，调用详情接口并跳转
                loadDrawingSessionDetail(item.getSessionId());
            } else if (currentTab == VMHistory.TAB_MEETING && item.getMeetingId() != null) {
                // 会议tab，跳转到会议详情页面
                jumpToMeetingDetail(item.getMeetingId(), item.getMeetingType());
            } else if (currentTab == VMHistory.TAB_PPT && item.getPptSessionId() != null) {
                // PPT tab，处理PPT历史记录点击
                Timber.tag("HistoryBottomSheet").d( "点击PPT历史记录 - SessionId: " + item.getPptSessionId() + ", Title: " + item.getTitle());
                handlePptHistoryClick(item);
            }else if (currentTab == VMHistory.TAB_TRANSLATE && item.getConversationId() != null) {

                Intent intent = new Intent(getActivity(), TranslateDetailActivity.class);
                intent.putExtra(Constant.INTENT_ID,item.getConversationId());
                intent.putExtra(Constant.INTENT_TYPE,currentTranslateType);
                startActivity(intent);
            }
            // 其他tab的处理可以在这里添加
        });
        
        // 设置更多操作监听器
        historyAdapter.setOnMoreActionListener((anchor, item, actionType) -> {
            if (actionType == 0) { // 查看详情
//                handleViewDetail(item);
            } else if (actionType == 1) { // 重命名
                    // 弹出输入框，确认后重命名
                showEditNameDialog(item);
//                    android.widget.EditText editText = new android.widget.EditText(getContext());
//                    editText.setText(item.getTitle());
//                    new androidx.appcompat.app.AlertDialog.Builder(getContext())
//                        .setTitle("重命名")
//                        .setView(editText)
//                        .setPositiveButton("确定", (dialog, which) -> {
//                            String newName = editText.getText().toString().trim();
//                            // TODO: 调用重命名接口
//                            GlobalToast.show(getActivity(), "重命名为: " + newName, GlobalToast.Type.SUCCESS);
//                        })
//                        .setNegativeButton("取消", null)
//                        .show();
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
//                    new androidx.appcompat.app.AlertDialog.Builder(getContext())
//                        .setTitle("删除确认")
//                        .setMessage("确定要删除这条记录吗？")
//                        .setPositiveButton("删除", (dialog, which) -> {
//                            // TODO: 调用删除接口
//                            android.widget.Toast.makeText(getContext(), "已删除", android.widget.Toast.LENGTH_SHORT).show();
//                        })
//                        .setNegativeButton("取消", null)
//                        .show();
                }
//            }
        });
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
                        Integer currentTab = viewModel.getCurrentTabIndex().getValue();
                        if(currentTab == VMHistory.TAB_TRANSLATE){
                            viewModel.updateTranslateName(item,currentTranslateType,inputText);
                        }else {
                            viewModel.updateName(item,inputText);
                        }
                    }

                    @Override
                    public void onCancel() {
                        // 取消编辑
                    }
                });
        editDialog.show();
    }

    /**
     * 处理查看详情操作
     */
    private void handleViewDetail(HistoryItem item) {
        Integer currentTab = viewModel.getCurrentTabIndex().getValue();
        if (currentTab == null) return;

        if (currentTab == VMHistory.TAB_CHAT && item.getConversationId() != null) {
            // 对话tab，跳转到对话页面
            jumpToConversation(item);
        } else if (currentTab == VMHistory.TAB_AGENT && item.getConversationId() != null) {
            // 智能体tab，跳转到对话页面
            jumpToConversation(item);
        } else if (currentTab == VMHistory.TAB_DRAWING && item.getSessionId() != null) {
            // AI绘画tab，调用详情接口并跳转
            loadDrawingSessionDetail(item.getSessionId());
        } else if (currentTab == VMHistory.TAB_MEETING && item.getMeetingId() != null) {
            // 会议tab，跳转到会议详情页面
            jumpToMeetingDetail(item.getMeetingId(), item.getMeetingType());
        } else if (currentTab == VMHistory.TAB_PPT && item.getPptSessionId() != null) {
            // PPT tab，处理PPT历史记录点击
            Timber.tag("HistoryBottomSheet").d( "长按PPT历史记录 - SessionId: " + item.getPptSessionId() + ", Title: " + item.getTitle());
            handlePptHistoryClick(item);
        } else {
            GlobalToast.show(getActivity(), "暂不支持查看此类型详情", GlobalToast.Type.NORMAL);
        }
    }

    private void deleteHistory(HistoryItem item) {
        Integer currentTab = viewModel.getCurrentTabIndex().getValue();
        if (currentTab == null) return;

        long id = 0;
        String conversationId = String.valueOf(item.getConversationId());
        if(currentTab == VMHistory.TAB_CHAT) {
            String idStr = SharedPreferencesUtil.getString(Constants.PREF_CONVERSATION_ID, "0");
            if (!TextUtils.isEmpty(idStr) && idStr.equals(conversationId)) {
                SharedPreferencesUtil.saveString(Constants.PREF_CONVERSATION_ID, "0");
            }
        } else if(currentTab == VMHistory.TAB_AGENT) {
            SharedPreferencesUtil.removeConversationIdRecords(conversationId);
        }
        switch (currentTab) {
            case VMHistory.TAB_CHAT:
            case VMHistory.TAB_AGENT:
            case VMHistory.TAB_EXCEL:
                id = item.getConversationId();
                deleteConversation(id);
                break;
            case VMHistory.TAB_DRAWING:
                id = item.getSessionId();
                deleteDrawing(id);
                break;
            case VMHistory.TAB_MEETING:
                id = item.getMeetingId();
                deleteMeeting(id + "");
                break;
            case VMHistory.TAB_PPT:
                id = item.getPptSessionId();
                deletePPT(id + "");
                break;
            case VMHistory.TAB_TRANSLATE:
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
                viewModel.refreshHistory();
            } else {
//                setError(result != null ? result.getError() : "删除失败");
            }
        });
    }
    private void deleteDrawing(long id) {
        DrawingRepository drawingRepository = DrawingRepositoryImpl.getInstance();
        drawingRepository.deleteAllSessions(id).observeForever(result -> {
            if (result != null && result.isSuccess() && result.getData() != null) {
                GlobalToast.show(getActivity(),"删除成功",GlobalToast.Type.SUCCESS);
                viewModel.refreshHistory();
            } else {
                GlobalToast.show(getActivity(),"删除失败", GlobalToast.Type.ERROR);
            }
        });
    }
    private void deleteConversation(long id) {
        ChatRepository chatRepository = new ChatRepositoryImpl();
        chatRepository.deleteConversation(id, new ChatRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                GlobalToast.show(getActivity(),"删除成功",GlobalToast.Type.SUCCESS);
                viewModel.refreshHistory();
            }

            @Override
            public void onError(String error) {
                GlobalToast.show(getActivity(),"删除失败: " + error, GlobalToast.Type.ERROR);
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
                    viewModel.refreshHistory();
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
                    viewModel.refreshHistory();
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
    /**
     * 更新Tab选中状态（由ViewModel触发）
     */
    private void updateTabSelection(int tabIndex) {
        // 重置所有Tab样式
        resetTabStyles();

        // 设置选中Tab样式
        TextView selectedTab = getTabByIndex(tabIndex);
        if (selectedTab != null) {
            selectedTab.setTextColor(COLOR_SELECTED);
            selectedTab.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }
    
    private void resetTabStyles() {
        TextView[] tabs = {tvTabChat, tvTabAgent, tvTabDrawing, tvTabMeeting, tvTabPPT, tvTabTranslate,tvTabExcel};
        for (TextView tab : tabs) {
            tab.setTextColor(COLOR_UNSELECTED);
            tab.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }
    
    private TextView getTabByIndex(int index) {
        switch (index) {
            case VMHistory.TAB_CHAT: return tvTabChat;
            case VMHistory.TAB_AGENT: return tvTabAgent;
            case VMHistory.TAB_DRAWING: return tvTabDrawing;
            case VMHistory.TAB_MEETING: return tvTabMeeting;
            case VMHistory.TAB_PPT: return tvTabPPT;
            case VMHistory.TAB_TRANSLATE: return tvTabTranslate;
            case VMHistory.TAB_EXCEL: return tvTabExcel;
            default: return null;
        }
    }
    
    // 移除了loadHistoryData方法，现在由ViewModel处理数据加载

    // 移除了loadChatConversationHistory方法，现在由ViewModel处理

    // 移除了loadAgentConversationHistory方法，现在由ViewModel处理

    // 移除了loadMeetingHistory方法，现在由ViewModel处理

    // 移除了loadDrawingHistory方法，现在由ViewModel处理

    private List<HistoryItem> convertDrawingToHistoryItems(List<DrawingImageDto> drawingImages) {
        List<HistoryItem> items = new ArrayList<>();
        String lastDate = "";

        for (DrawingImageDto image : drawingImages) {
            String date = getDateFromTime(image.getCreateTime());
            if (!date.equals(lastDate)) {
                // 添加日期分组头
                items.add(new HistoryItem(HistoryItem.TYPE_DATE_HEADER, date, null, 0));
                lastDate = date;
            }

            // 添加图片项，使用提示词作为标题
            String prompt = image.getPrompt();
            if (prompt != null && prompt.length() > 30) {
                prompt = prompt.substring(0, 30) + "...";
            }
            String imageUrl = image.getThumbnailUrl() != null ? image.getThumbnailUrl() : image.getImageUrl();
            HistoryItem historyItem = new HistoryItem(HistoryItem.TYPE_ITEM, prompt, null, imageUrl);
            historyItem.setSessionId(image.getSessionId());
            items.add(historyItem);
        }

        return items;
    }

    /**
     * 将会议历史转换为HistoryItem列表
     */
    private List<HistoryItem> convertMeetingToHistoryItems(List<MeetingHistoryDto> meetings) {
        List<HistoryItem> items = new ArrayList<>();
        if (meetings == null || meetings.isEmpty()) {
            return items;
        }

        String lastDate = "";

        for (MeetingHistoryDto meeting : meetings) {
            // 使用会议名称中的时间戳或当前时间作为日期
            String date = getDateFromMeetingName(meeting.getCreateTime()+"");
            if (!date.equals(lastDate)) {
                // 添加日期分组头
                items.add(new HistoryItem(HistoryItem.TYPE_DATE_HEADER, date, null, 0));
                lastDate = date;
            }

            // 构建显示标题
            String title = meeting.getName();
            if (title == null || title.trim().isEmpty()) {
                title = "会议记录";
            }
            // 去除分割title的逻辑，直接显示完整name字段

            // 构建副标题（显示会议内容摘要）
            String subtitle = "";
            if (meeting.getAbstractText() != null && !meeting.getAbstractText().trim().isEmpty()) {
                subtitle = meeting.getAbstractText();
                if (subtitle.length() > 50) {
                    subtitle = subtitle.substring(0, 50) + "...";
                }
            } else if (meeting.getContent() != null && !meeting.getContent().trim().isEmpty()) {
                // 提取纯文本内容
                String content = extractTextFromMeetingContent(meeting.getContent());
                if (content.length() > 50) {
                    content = content.substring(0, 50) + "...";
                }
                subtitle = content;
            } else {
                subtitle = "暂无内容";
            }

            // 创建HistoryItem，使用会议图标
            HistoryItem historyItem = new HistoryItem(HistoryItem.TYPE_ITEM, title, subtitle, R.drawable    .ic_nav_meeting);
            historyItem.setMeetingId(meeting.getId());
            historyItem.setMeetingType(meeting.getType());
            items.add(historyItem);
        }

        return items;
    }

    /**
     * 从会议名称中提取日期
     */
    private String getDateFromMeetingName(String timestamp) {
        try {
            if (timestamp == null) {
                return "未知日期";
            }
            return getDateFromTimestamp(Long.valueOf(timestamp));
        } catch (Exception e) {
            return "未知日期";
        }
    }

    /**
     * 从会议内容中提取纯文本
     */
    private String extractTextFromMeetingContent(String content) {
        if (content == null) return "";

        // 移除时间戳格式 [0:1.310,0:2.260,0]
        String text = content.replaceAll("\\[\\d+:\\d+\\.\\d+,\\d+:\\d+\\.\\d+,\\d+\\]\\s*", "");
        // 移除多余的换行符
        text = text.replaceAll("\\n+", " ");
        return text.trim();
    }

    /**
     * 将智能体对话历史转换为HistoryItem列表
     */
    private List<HistoryItem> convertConversationToHistoryItems(List<ConversationHistoryDto> conversations) {
        List<HistoryItem> items = new ArrayList<>();
        if (conversations == null || conversations.isEmpty()) {
            return items;
        }

        String lastDate = "";

        for (ConversationHistoryDto conversation : conversations) {
            // 获取日期字符串
            String date = getDateFromTimestamp(conversation.getCreateTime());
            if (!date.equals(lastDate)) {
                // 添加日期分组头
                items.add(new HistoryItem(HistoryItem.TYPE_DATE_HEADER, date, null, 0));
                lastDate = date;
            }

            // 构建显示标题
            String title = conversation.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = "新对话";
            }

            // 构建副标题（显示角色名称或最后一条消息）
            String subtitle = "";
            if (conversation.getRoleName() != null && !conversation.getRoleName().trim().isEmpty()) {
                subtitle = conversation.getRoleName();
            } else if (conversation.getLastMessage() != null &&
                       conversation.getLastMessage().getContent() != null) {
                String content = conversation.getLastMessage().getContent();
                if (content.length() > 30) {
                    content = content.substring(0, 30) + "...";
                }
                subtitle = content;
            }

            // 获取头像资源
            int avatarRes = getAvatarResourceByRoleName(conversation.getRoleName());

            // 创建HistoryItem
            HistoryItem historyItem = new HistoryItem(HistoryItem.TYPE_ITEM, title, subtitle, avatarRes);
            historyItem.setConversationId(conversation.getId());
            historyItem.setModelType(conversation.getModelType());
            historyItem.setModelType(conversation.getModelType());
            historyItem.setModelId(conversation.getModelId());
            items.add(historyItem);
        }

        return items;
    }

    /**
     * 根据时间戳获取日期字符串
     */
    private String getDateFromTimestamp(Long timestamp) {
        if (timestamp == null) return "";

        try {
            Date date = new Date(timestamp);
            Calendar today = Calendar.getInstance();
            Calendar targetDay = Calendar.getInstance();
            targetDay.setTime(date);

            // 判断是否是今天
            if (isSameDay(today, targetDay)) {
                return "今天";
            }

            // 判断是否是昨天
            today.add(Calendar.DAY_OF_YEAR, -1);
            if (isSameDay(today, targetDay)) {
                return "昨天";
            }

            // 其他日期
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
            return outputFormat.format(date);

        } catch (Exception e) {
            Timber.tag("HistoryBottomSheet").e( "时间戳转换失败: " + timestamp, e);
            return "";
        }
    }

    /**
     * 根据角色名称获取头像资源
     */
    private int getAvatarResourceByRoleName(String roleName) {
        if (roleName == null) {
            return R.drawable.ic_avatar_ai_writer; // 默认头像
        }

        // 根据角色名称匹配头像
        switch (roleName) {
            case "瘦身小天使":
            case "健身教练":
                return R.drawable.ic_avatar_fitness;
            case "千变女友":
            case "恋爱助手":
                return R.drawable.ic_avatar_girlfriend;
            case "AI 写作":
            case "写作助手":
                return R.drawable.ic_avatar_ai_writer;
            default:
                return R.drawable.ic_avatar_ai_writer; // 默认头像
        }
    }

    private String getDateFromTime(String timeStr) {
        if (timeStr == null) return "";

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(timeStr);

            Calendar today = Calendar.getInstance();
            Calendar targetDay = Calendar.getInstance();
            targetDay.setTime(date);

            // 判断是否是今天
            if (isSameDay(today, targetDay)) {
                return "今天";
            }

            // 判断是否是昨天
            today.add(Calendar.DAY_OF_YEAR, -1);
            if (isSameDay(today, targetDay)) {
                return "昨天";
            }

            // 其他日期
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
            return outputFormat.format(date);

        } catch (Exception e) {
            return "";
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void loadDrawingSessionDetail(Long sessionId) {
        // 显示加载框
        LoadingProgressDialog loadingDialog =
            new LoadingProgressDialog(getContext())
                .setMessage("加载中...")
                .setCancelable(false);
//        loadingDialog.show();

        // 无网络：尝试从本地缓存读取
        if (!com.fxzs.lingxiagent.util.NetworkUtils.isNetworkAvailable(getContext())) {
            try {
                String key = com.fxzs.lingxiagent.model.common.Constants.PREF_DRAWING_SESSION_BY_ID_PREFIX + sessionId;
                String json = com.fxzs.lingxiagent.util.SharedPreferencesUtil.getString(key, "");
                if (!android.text.TextUtils.isEmpty(json)) {
                    DrawingSessionDto cached = new com.google.gson.Gson().fromJson(json, DrawingSessionDto.class);
                    if (cached != null) {
                        jumpToDrawingChat(sessionId, cached);
                        return;
                    }
                }
            } catch (Exception ignore) {}
            GlobalToast.show(getActivity(), "当前无网络且本地无缓存", GlobalToast.Type.ERROR);
            return;
        }

        // 调用API获取会话详情
        DrawingRepository drawingRepository = DrawingRepositoryImpl.getInstance();
        drawingRepository.getSessionDetailById(sessionId).observeForever(result -> {
            loadingDialog.dismiss();

            if (result.isSuccess() && result.getData() != null) {
                DrawingSessionDto sessionDetail = result.getData();

                // 成功后写入离线缓存
                try {
                    String key = com.fxzs.lingxiagent.model.common.Constants.PREF_DRAWING_SESSION_BY_ID_PREFIX + sessionId;
                    String json = new com.google.gson.Gson().toJson(sessionDetail);
                    com.fxzs.lingxiagent.util.SharedPreferencesUtil.saveString(key, json);
                } catch (Exception ignore) {}

                jumpToDrawingChat(sessionId, sessionDetail);
            } else {
                // 显示错误提示
                GlobalToast.show(getActivity(),
                    "获取详情失败：" + (result.getError() != null ? result.getError() : "未知错误"),
                    GlobalToast.Type.ERROR);
            }
        });
    }

    private void jumpToDrawingChat(Long sessionId, DrawingSessionDto sessionDetail) {
        if(currentDrawingType == 0){
            android.content.Intent intent = new android.content.Intent(getContext(),
                    SuperChatContainActivity.class);
            intent.putExtra(Constant.INTENT_TYPE, SuperChatContainActivity.TYPE_DRAWING);

            DrawingToChatBean drawingToChatBean = new DrawingToChatBean();
            drawingToChatBean.setSessionId(sessionId);
            drawingToChatBean.setSessionDetail(sessionDetail);

            intent.putExtra(Constant.INTENT_DATA, drawingToChatBean);
            startActivity(intent);
        }else {//图生图

            // 跳转到图片生成页面
            Intent intent = new Intent(getContext(), DrawingImageGenerateActivity.class);
            if(null != sessionDetail.getAiImageList() && sessionDetail.getAiImageList().size() > 0){
                DrawingImageDto last =  sessionDetail.getAiImageList().get(sessionDetail.getAiImageList().size()-1);

                if(last.getPicUrl() != null){
                    intent.putExtra(DrawingImageGenerateActivity.EXTRA_GENERATED_IMAGE_URL, last.getPicUrl().toString());
                }
                if(last.getOptions() != null){
                    DrawingImageDto.Options options = last.getOptions();
                    intent.putExtra(DrawingImageGenerateActivity.EXTRA_ORIGINAL_IMAGE_URI, options.getReferenceImageUrl().toString());
                    intent.putExtra(DrawingImageGenerateActivity.EXTRA_STYLE_DESCRIPTION,options.getRealPrompt());

                    DrawingTransformStyleItem selectedStyle = new DrawingTransformStyleItem();
                    selectedStyle.setId(options.getStyleId());
                    selectedStyle.setName(options.getStylePrompt());
                    selectedStyle.setPrompt("生成" +
                            options.getStylePrompt() +
                            "风格，");
                    intent.putExtra(DrawingImageGenerateActivity.EXTRA_STYLE_ITEM, selectedStyle);

                }

                intent.putExtra(DrawingImageGenerateActivity.EXTRA_SESSION_ID, last.getSessionId());
                intent.putExtra(DrawingImageGenerateActivity.EXTRA_TASK_ID, last.getId());
                intent.putExtra(DrawingImageGenerateActivity.EXTRA_IS_FROM_HISTORY, true);
                startActivity(intent);
            }
//            startActivityForResult(intent, REQUEST_GENERATE);

        }
//        dismiss(); // 关闭底部抽屉
    }

    /**
     * 跳转到对话页面（支持对话和智能体）
     */
    private void jumpToConversation(HistoryItem item
           ) {
        long conversationId = item.getConversationId();
        int modelType = item.getModelType();
        long modeId = item.getModelId();
        Timber.tag("HistoryBottomSheet").d( "跳转到对话页面，conversationId: " + conversationId + ", modelType: " + modelType);

        try {
            // 无网时提示并仍然尝试进入聊天页，由聊天页自行做离线回退
            if (!com.fxzs.lingxiagent.util.NetworkUtils.isNetworkAvailable(getContext())) {
                com.fxzs.lingxiagent.view.common.GlobalToast.show(getActivity(), "当前无网络，尝试从本地缓存打开", com.fxzs.lingxiagent.view.common.GlobalToast.Type.NORMAL);
            }
            // 跳转到SuperChatContainActivity
            Intent intent;
            if (modelType == 1) {
                intent = new Intent(getContext(), AgentContainActivity.class);
            } else {
                intent = new Intent(getContext(), SuperChatContainActivity.class);
            }

            // 根据modelType设置不同的类型
            if (modelType == 1) {
                // 智能体对话
                getCatDetailListBean bean = new getCatDetailListBean();
                bean.setModelId(modeId);
                intent.putExtra(Constant.INTENT_DATA2, bean);
                intent.putExtra(Constant.INTENT_TYPE, SuperChatContainActivity.TYPE_AGENT);
            } else if (modelType == 8) {
                // 普通对话

                OptionModel selectOptionModel = new OptionModel();
                selectOptionModel.setId(item.getModelId());
                selectOptionModel.setName(item.getModelName());
                selectOptionModel.setModel(item.getModel());
                intent.putExtra(Constant.INTENT_DATA1, selectOptionModel);
                intent.putExtra(Constant.INTENT_TYPE, SuperChatContainActivity.TYPE_HOME);
            } else {
                // 默认类型
                intent.putExtra(Constant.INTENT_TYPE, SuperChatContainActivity.TYPE_HOME);
            }

            // 传递对话ID
            intent.putExtra(Constant.INTENT_ID, conversationId);
            intent.putExtra("modelType", modelType);

//            startActivity(intent);
            activityResultLauncher.launch(intent);
//            dismiss(); // 关闭底部抽屉

        } catch (Exception e) {
            Timber.tag("HistoryBottomSheet").e( "跳转到对话页面失败"+ e);
            GlobalToast.show(getActivity(),
                "打开对话失败",
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
            GlobalToast.show(getActivity(), "PPT会话数据异常", GlobalToast.Type.ERROR);
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
//        // 优先检查是否有PPT任务，如果有则直接跳转到预览页面
//        if (session.getPptTasks() != null && !session.getPptTasks().isEmpty()) {
//            com.fxzs.lingxiagent.model.ppt.dto.PptSessionDto.PptTask firstTask = session.getPptTasks().get(0);
//
//            // 无论pptUrl是否为空，都跳转到预览页面，让预览页面处理
//            jumpToPptPreview(firstTask.getPptUrl(), firstTask.getPptTitle(), session.getTitle(), session.getId());
//            return;
//        }
//
//        // 如果没有PPT任务，检查是否有大纲，跳转到大纲编辑页面
//        if (session.getPptCatalogs() != null && !session.getPptCatalogs().isEmpty()) {
//            jumpToPptOutlineEdit(session);
//            return;
//        }
//
//        // 否则跳转到主题输入页面，预填充主题
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
//            dismiss();
        } catch (Exception e) {
            Timber.tag("HistoryBottomSheet").e( "跳转PPT预览页面失败"+ e);
            GlobalToast.show(getActivity(), "跳转失败", GlobalToast.Type.ERROR);
        }
    }

    /**
     * 跳转到PPT大纲编辑页面
     */
    private void jumpToPptOutlineEdit(com.fxzs.lingxiagent.model.ppt.dto.PptSessionDto session) {
        try {
            Intent intent = new Intent(getActivity(), com.fxzs.lingxiagent.view.ppt.PptOutlineEditActivity.class);
            intent.putExtra("session_id", session.getId());
            intent.putExtra("topic", session.getTitle());
            startActivity(intent);
            dismiss();
        } catch (Exception e) {
            Timber.tag("HistoryBottomSheet").e( "跳转PPT大纲编辑页面失败"+e);
            GlobalToast.show(getActivity(), "跳转失败", GlobalToast.Type.ERROR);
        }
    }

    /**
     * 跳转到PPT主题输入页面
     */
    private void jumpToPptTopicInput(String topic) {
        try {
            Intent intent = new Intent(getActivity(), com.fxzs.lingxiagent.view.ppt.PptTopicInputActivity.class);
            if (topic != null && !topic.isEmpty()) {
                intent.putExtra("default_topic", topic);
            }
            startActivity(intent);
            dismiss();
        } catch (Exception e) {
            Timber.tag("HistoryBottomSheet").e( "跳转PPT主题输入页面失败"+ e);
            GlobalToast.show(getActivity(), "跳转失败", GlobalToast.Type.ERROR);
        }
    }

    /**
     * 跳转到会议详情页面
     */
    private void jumpToMeetingDetail(Long meetingId, Integer meetingType) {
        Timber.tag("HistoryBottomSheet").d( "跳转到会议详情，meetingId: " + meetingId + ", meetingType: " + meetingType);

        // 显示加载提示
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
        progressDialog.setMessage("正在加载会议详情...");
        progressDialog.setCancelable(false);
//        progressDialog.show();

        try {
            // 先查询会议详情获取转写内容
            MeetingRepository meetingRepository = new MeetingRepositoryImpl();
            meetingRepository.getMeetingDetail(meetingId.toString()).observeForever(result -> {
                // 隐藏加载提示
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

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
                    android.content.Intent intent = MeetingActivity.createIntent(
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

                    android.content.Intent intent = MeetingActivity.createIntent(
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
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            Timber.tag("HistoryBottomSheet").e( "跳转到会议详情失败"+ e);
            GlobalToast.show(getActivity(),
                "打开会议详情失败",
                GlobalToast.Type.ERROR);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (historyAdapter != null){
            historyAdapter.cancelPopu();
        }
    }
}
