package com.fxzs.lingxiagent;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;

import com.fxzs.lingxiagent.crash.CrashReportUploader;
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.TrackerUtils;
import com.fxzs.lingxiagent.lingxi.translate.util.LanguageUtils;
import com.fxzs.lingxiagent.model.auth.AuthHelper;
import com.fxzs.lingxiagent.model.chat.callback.SoftCallback;
import com.fxzs.lingxiagent.model.chat.dto.EventBusShareCancelModel;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.upgrade.UpgradeHelper;
import com.fxzs.lingxiagent.receiver.LingxiGUIWidgetProvider;
import com.fxzs.lingxiagent.receiver.WidgetDataHelper;
import com.fxzs.lingxiagent.service.WakeUpService;
import com.fxzs.lingxiagent.util.AppManager;
import com.fxzs.lingxiagent.util.GMapHelper;
import com.fxzs.lingxiagent.util.WakeUpPermissionHelper;
import com.fxzs.lingxiagent.util.ZUtil.SuperAgentUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.util.audio.MediaPlayerUtils;
import com.fxzs.lingxiagent.util.audio.OnPlayerListener;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.agent.AgentFragment;
import com.fxzs.lingxiagent.view.aiwork.AiWorkFragment;
import com.fxzs.lingxiagent.view.auth.OneClickLoginActivity;
import com.fxzs.lingxiagent.view.chat.SuperChatFragment;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.drawing.DrawingNewFragment;
import com.fxzs.lingxiagent.view.meeting.MeetingFragment;
import com.fxzs.lingxiagent.view.user.UserAppSettingsActivity;
import com.fxzs.lingxiagent.view.user.UserFragment;
import com.fxzs.lingxiagent.viewmodel.main.VMMain;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class MainActivity extends BaseActivity<VMMain> {
    private static final String TAG = "MainActivity";
    // 底部导航栏
    private LinearLayout navTabGui;
    private LinearLayout navTabPhone;
    private LinearLayout navTabLingxi;
    private LinearLayout navTabJob;
    private LinearLayout navTabAgent;

    // Fragment实例
    private SuperChatFragment chatFragment;
    private AgentFragment agentFragment;
    private DrawingNewFragment drawingFragment;
    private MeetingFragment meetingFragment;
    private AiWorkFragment aiWorkFragment;
    private UserFragment userFragment;

    // 当前选中的导航项
    private LinearLayout currentNavItem;
    private Fragment currentFragment;

    // Tab索引常量
    private static final int TAB_GUI = 0;
    private static final int TAB_AI_PHONE = 1;
    public static final int TAB_LINGXI = 2;
    private static final int TAB_AI_JOB = 3;
    private static final int TAB_AGENT = 4;
    private View bottomNavigation;
    private boolean isShowBottomNav = true;
    private View rootView;
    public static final int REQUEST_LOCATION_PERMISSION = 0;
    public static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;
    private boolean isBottomAnimating = false;
    private boolean lastKeyboardVisible = false;

    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 100;
    private MotionLayout motionLayout;


    private BroadcastReceiver wakeupReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("ACTION_PLAY_TTS".equals(action)) {
                // 播放TTS
                String ttsText = intent.getStringExtra("tts_text");
                boolean isShowFloat = intent.getBooleanExtra("isShowFloat", false);
                playTTS(ttsText, isShowFloat);
            }
        }
    };

    private void playTTS(String text, boolean isShowFloat) {
        try {
            TTSManager ttsManager = TTSManager.Companion.getInstance();
            TTSManager.getInstance().setOnPlayerListener(new OnPlayerListener() {
                @Override
                public void playerStart() {
                    Timber.tag(TAG).e("播放TTS开始: %s", text);

                }

                @Override
                public void playerStop() {
                    Timber.tag(TAG).e("播放TTS结束: %s", text);
                    if (!isShowFloat) {
                        return;
                    }
                    try {
                        Intent ttsIntent = new Intent("ACTION_STOP_TTS");
                        ttsIntent.setPackage(getPackageName());
                        ttsIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        sendBroadcast(ttsIntent);
                        Log.d(TAG, "已发送完成TTS广播：" + text);
                    } catch (Exception e) {
                        Log.e(TAG, "发送完成TTS广播失败: " + e.getMessage());
                    }
                }
            });

            if (ttsManager != null) {
                ttsManager.textForceToAudio(text);
            }
        } catch (Exception e) {
            Timber.tag(TAG).e("播放TTS失败: %s", e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EventBus.getDefault().register(this);
        // 检查登录状态
        if (!AuthHelper.getInstance().isLogin()) {
            // 未登录，跳转到一键登录页面
            Intent intent = new Intent(this, OneClickLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        // 通过广播刷新桌面卡片
        Intent intent = new Intent(this, LingxiGUIWidgetProvider.class);
        intent.setAction(LingxiGUIWidgetProvider.ACTION_REFRESH_WIDGET);
        sendBroadcast(intent);

        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_WAKEUP_SUCCESS");
        filter.addAction("ACTION_PLAY_TTS");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要指定导出标志
            registerReceiver(wakeupReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            // Android 12 及以下版本
            registerReceiver(wakeupReceiver, filter);
        }
        // 检查位置权限
        checkLocationPermission();

        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.schedule(() -> {
            // 打印签名信息（用于极光后台配置）暂时无用
            // SignatureUtil.logSignatureInfo(MainActivity.this);

            // 注意：viewModel 操作若涉及 UI/LiveData，需切换到主线程
            runOnUiThread(() -> viewModel.fetchAppUpgradeInfo(MainActivity.this));

            new CrashReportUploader().uploadAll(MainActivity.this);

            LanguageUtils.getInstance().requestAllLanguages();

            WidgetDataHelper.loadAndBindWidgetData(null);

        }, 1000, TimeUnit.MILLISECONDS); // 延迟 1000 毫秒（1 秒）

        // 关键：页面销毁时关闭线程池，避免内存泄漏
        getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            if (event == Lifecycle.Event.ON_DESTROY) {
                if (!scheduledExecutor.isShutdown()) {
                    scheduledExecutor.shutdownNow(); // 关闭线程池，取消未执行任务
                }
            }
        });
//        checkOverlayPermission();
        tryRestoreWakeUp();
    }

    private void tryRestoreWakeUp() {
        boolean userEnabled = WakeUpPermissionHelper.isWakeUpEnabled(this);

        if (!userEnabled) {
            Timber.tag(TAG).d("语音唤醒功能未启用，跳过恢复");
            return;
        }

        if (WakeUpPermissionHelper.isWakeUpServiceRunning(this)) {
            Timber.tag(TAG).d("WakeUpService 已在运行，无需重复启动");
            return;
        }

        // 尝试恢复：如果权限全，直接开；否则引导
        if (WakeUpPermissionHelper.checkAllPermissionsGranted(this)) {
            Timber.tag(TAG).d("权限已授予，启动 WakeUpService");
            WakeUpPermissionHelper.toggleWakeUpService(this, true);
        } else {
            //权限手动关闭时，自动关闭唤醒
            Timber.tag(TAG).w("权限不足，关闭语音唤醒功能");
            WakeUpPermissionHelper.setWakeUpEnabled(this,false);
            CommonDialog.showConfirmDialog(this, "语音唤醒权限未开启", "语音唤醒需要开启【录音权限、通知权限、悬浮窗权限】才能正常使用，请前往设置页面开启", "去设置", new CommonDialog.OnDialogClickListener() {
                @Override
                public void onConfirm() {
                    Intent settingsIntent = new Intent(MainActivity.this, UserAppSettingsActivity.class);
                    startActivity(settingsIntent);
                }

                @Override
                public void onCancel() {

                }
            });
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            GMapHelper.getInstance().initLocation(this);
            GMapHelper.getInstance().getLocation();
        }
    }

    /**
     * 处理权限请求结果（兼容原有单个权限申请的回调）
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.tag("GMapHelper").i("定位权限申请成功");
                GMapHelper.getInstance().initLocation(this);
                GMapHelper.getInstance().getLocation();
            }
        }
//        if (requestCode == WakeUpPermissionHelper.REQUEST_CODE_RECORD_AUDIO ||
//                requestCode == WakeUpPermissionHelper.REQUEST_CODE_POST_NOTIFICATIONS) {
//
//            boolean allGranted = true;
//            for (int result : grantResults) {
//                if (result != PackageManager.PERMISSION_GRANTED) {
//                    allGranted = false;
//                    break;
//                }
//            }
//
//            if (allGranted) {
//                // 继续引导下一项（或启动）
//                WakeUpPermissionHelper.promptAndEnableWakeUpIfPossible(this, null);
//            }
//            // 如果拒绝，不做处理（用户放弃）
//        }
    }

    // ========== 以下原有代码保持不变 ==========
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    @Override
    protected Class<VMMain> getViewModelClass() {
        return VMMain.class;
    }

    @Override
    protected void setupDataBinding() {
        // Fragment模式下暂时不需要特定的数据绑定
    }

    @Override
    protected void initializeViews() {
        // 初始化底部导航栏
        navTabGui = findViewById(R.id.nav_tab_gui);
        navTabPhone = findViewById(R.id.nav_tab_phone);
        navTabLingxi = findViewById(R.id.nav_tab_lingxi);
        navTabJob = findViewById(R.id.nav_tab_job);
        navTabAgent = findViewById(R.id.nav_tab_agent);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        motionLayout = findViewById(R.id.motionLayout);

        // Shared Element 动画结束后启动 MotionLayout 内部动画
        motionLayout.post(() -> motionLayout.transitionToEnd());

        // 设置点击事件
        navTabGui.setOnClickListener(v -> selectTab(TAB_GUI));
        navTabPhone.setOnClickListener(v -> selectTab(TAB_AI_PHONE));
        navTabLingxi.setOnClickListener(v -> {
            selectTab(TAB_LINGXI);
            TrackerUtils.trackBottomNavigationClickEvent("灵犀");
        });
        navTabJob.setOnClickListener(v -> {
            selectTab(TAB_AI_JOB);
            TrackerUtils.trackBottomNavigationClickEvent("AI办公");
        });
        navTabAgent.setOnClickListener(v -> {
            selectTab(TAB_AGENT);
            TrackerUtils.trackBottomNavigationClickEvent("智能体");
        });


        // 动画监听，展开完成后加载 Fragment
        motionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                if (currentId == R.id.end) {
                    // 初始化Fragment
                    initFragments();

                    // 检查Intent是否指定了要选中的Tab
                    int selectedTab = getIntent().getIntExtra("selected_tab", TAB_LINGXI);
                    selectTab(selectedTab);
                    setBottomViewVisible();
                    AppManager.finishActivity(WakeVoiceActivity.class);

                }
            }

            // 其他回调可忽略
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
            }

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {
            }
        });

//        // 初始化Fragment
//        initFragments();
//
//        // 检查Intent是否指定了要选中的Tab
//        int selectedTab = getIntent().getIntExtra("selected_tab", TAB_LINGXI);
//        selectTab(selectedTab);
//        setBottomViewVisible();
//
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setBottomNavigationVisible(true);
            toggleShowBotMenu();
        }, 1000);

    }

    @Override
    protected void setupObservers() {
        // 观察版本信息
        viewModel.getVersionInfo().observe(this, versionInfo -> {
            if (versionInfo != null) {
                // 有可升级版本
                if (!TextUtils.isEmpty(versionInfo.getDownloadUrl())) {
                    UpgradeHelper.showUpgradeDialog(this, versionInfo);
                }
            }
        });
    }

    @Override
    protected void handleLoadingState(boolean loading) {
        // Fragment模式下的加载状态处理
    }

    /**
     * 初始化所有Fragment
     */
    private void initFragments() {
        chatFragment = new SuperChatFragment(SuperChatFragment.TYPE_HOME);
        agentFragment = new AgentFragment();
        drawingFragment = new DrawingNewFragment();
        aiWorkFragment = new AiWorkFragment();
//        meetingFragment = new MeetingFragment();
        userFragment = new UserFragment();
    }

    /**
     * 选择Tab
     *
     * @param tabIndex Tab索引
     */
    private void selectTab(int tabIndex) {
        // 切换Tab时停止TTS播放
        try {
            TTSManager.Companion.getInstance().stop();
        } catch (Exception e) {
            Timber.tag(TAG).w("切换Tab时停止TTS播放失败" + e);
        }
        // 检查登录状态 - 除了对话Tab，其他Tab都需要登录
        if (tabIndex != TAB_LINGXI && !AuthHelper.getInstance().isLogin()) {
            // 未登录，跳转到一键登录页面
            Intent intent = new Intent(this, OneClickLoginActivity.class);
            intent.putExtra("from_home", true);
            intent.putExtra("selected_tab", tabIndex);
            startActivity(intent);
            return;
        }
//        if(tabIndex != TAB_MEETING && Constant.isLoadMeetingExchange){
//
//            CommonDialog.showConfirmDialog(MainActivity.this, "将不保存会议内容",
//                    "请确认是否退出", "退出",
//                    new CommonDialog.OnDialogClickListener() {
//                        @Override
//                        public void onConfirm() {
//                            EventBus.getDefault().post(new MessageEvent());
//                        }
//
//                        @Override
//                        public void onCancel() {
//                            // 用户点击不同意，不做任何操作
//                        }
//                    });
//            return;
//        }

        // 重置所有Tab状态
        resetTabState();

        // 根据选中的Tab设置状态和显示对应Fragment
        LinearLayout selectedNavItem = null;
        Fragment selectedFragment = null;

        switch (tabIndex) {
            case TAB_GUI:
                selectedNavItem = navTabGui;
                selectedFragment = drawingFragment;
                break;
            case TAB_AI_PHONE:
                selectedNavItem = navTabPhone;
                selectedFragment = userFragment;
                break;
            case TAB_LINGXI:
                selectedNavItem = navTabLingxi;
                selectedFragment = chatFragment;
                break;
            case TAB_AI_JOB:
                selectedNavItem = navTabJob;
                selectedFragment = aiWorkFragment;
                break;
            case TAB_AGENT:
                selectedNavItem = navTabAgent;
                selectedFragment = agentFragment;
                break;
        }

        if (selectedNavItem != null && selectedFragment != null) {
            // 设置选中状态
            setNavItemSelected(selectedNavItem, true);
            currentNavItem = selectedNavItem;

            // 切换Fragment
            switchFragment(selectedFragment);
        }
    }

    /**
     * 重置所有Tab状态
     */
    private void resetTabState() {
        setNavItemSelected(navTabGui, false);
        setNavItemSelected(navTabPhone, false);
        setNavItemSelected(navTabLingxi, false);
        setNavItemSelected(navTabJob, false);
        setNavItemSelected(navTabAgent, false);
    }

    /**
     * 设置导航项选中状态
     *
     * @param navItem  导航项
     * @param selected 是否选中
     */
    private void setNavItemSelected(LinearLayout navItem, boolean selected) {
        ImageView icon = (ImageView) navItem.getChildAt(0);
        TextView text = (TextView) navItem.getChildAt(1);

        // 设置选中状态，让selector自动切换图标和文字颜色
        icon.setSelected(selected);
        if (text != null) {
            text.setSelected(selected);
        }
    }

    /**
     * 切换Fragment
     *
     * @param fragment 要显示的Fragment
     */
    private void switchFragment(Fragment fragment) {
        if (currentFragment == fragment) {
            return;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // 隐藏当前Fragment
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }

        // 如果Fragment未添加则添加，否则显示
        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment);
        } else {
            transaction.show(fragment);
        }

        transaction.commit();
        currentFragment = fragment;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ZUtils.print("MainActivity onActivityResult ====== > requestCode = " + requestCode + ". resultCode = " + resultCode);

        if (currentFragment != null && currentFragment instanceof SuperChatFragment) {
            ((SuperChatFragment) currentFragment).onActivityResult(requestCode, resultCode, data);
        }
    }

    public void toggleShowBotMenu() {
        if (bottomNavigation == null) {
            return;
        }
        if (isBottomAnimating) {
            return;
        }

        Runnable ensureHeightThen = () -> {
            if (isShowBottomNav) {
                // 显示：先让View可见，占位，再自下而上动画
                if (bottomNavigation.getVisibility() != View.VISIBLE) {
                    bottomNavigation.setVisibility(View.VISIBLE);
                    bottomNavigation.setTranslationY(bottomNavigation.getHeight());
                    isBottomAnimating = true;
                    bottomNavigation.animate()
                            .translationY(0f)
                            .setDuration(200)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .withEndAction(() -> {
                                isBottomAnimating = false;
                            })
                            .start();
                }
            } else {
                // 隐藏：立即设为GONE，避免键盘弹起时的视觉冲突
                if (bottomNavigation.getVisibility() == View.VISIBLE) {
                    // 取消任何正在进行的动画
                    bottomNavigation.animate().cancel();
                    bottomNavigation.setVisibility(View.VISIBLE);
                    bottomNavigation.setTranslationY(0f);
                    isBottomAnimating = false;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> bottomNavigation.setVisibility(View.GONE), 100);
                }
            }
        };

        if (bottomNavigation.getHeight() == 0) {
            bottomNavigation.post(ensureHeightThen);
        } else {
            ensureHeightThen.run();
        }
    }

    public void setBottomViewVisible() {
        // 监听键盘弹出/隐藏
        rootView = findViewById(R.id.root_view_main);

        // 使用Handler延迟处理，避免频繁触发
        final Handler keyboardHandler = new Handler(Looper.getMainLooper());
        final Runnable[] pendingKeyboardAction = {null};

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            double predictHeight = screenHeight * 0.15;

            // 键盘高度大于屏幕高度的15%，认为键盘弹出
            boolean keyboardVisible = keypadHeight > predictHeight;

            if (keyboardVisible != lastKeyboardVisible) {
                lastKeyboardVisible = keyboardVisible;

                // 取消之前的待处理任务
                if (pendingKeyboardAction[0] != null) {
                    keyboardHandler.removeCallbacks(pendingKeyboardAction[0]);
                }

                // 创建新的任务
                pendingKeyboardAction[0] = () -> {
                    if (keyboardVisible) {
                        ZUtils.print("隐藏底部导航");
                        // 键盘弹出时，立即隐藏底部导航
                        setBottomNavigationVisible(false);
                        toggleShowBotMenu();
                    } else {
                        ZUtils.print("显示底部导航");
                        // 键盘隐藏时，延迟显示底部导航，让界面先稳定
                        setBottomNavigationVisible(true);
                        toggleShowBotMenu();
                    }
                    pendingKeyboardAction[0] = null;
                };

                // 键盘弹出时立即执行，键盘隐藏时稍微延迟
                if (keyboardVisible) {
                    pendingKeyboardAction[0].run();
                } else {
                    keyboardHandler.postDelayed(pendingKeyboardAction[0], 50);
                }
            }
        });

        SuperAgentUtil superAgentUtil = new SuperAgentUtil(this, (LinearLayout) rootView, null);
        superAgentUtil.setOnListenSoft(rootView, new SoftCallback() {
            @Override
            public void show() {
                // 延迟滚动，等待布局稳定
                rootView.postDelayed(() -> {
                    // 空态界面可见时不自动滚动，避免空内容文案抖动
                    View emptyView = rootView.findViewById(R.id.ll_empty);
                    if (emptyView == null || emptyView.getVisibility() != View.VISIBLE) {
                        nestedScrollBottom();
                    }
                }, 100);
            }

            @Override
            public void hide() {
            }
        });
    }

    public void setBottomNavigationVisible(boolean visible) {
        isShowBottomNav = visible;
    }

    public void nestedScrollBottom() {
        NestedScrollView svChatList = rootView.findViewById(R.id.sv_chat_list);
        if (svChatList != null) {
            svChatList.smoothScrollTo(0, svChatList.getChildAt(0).getBottom());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!isShowBottomNav) {
            EventBus.getDefault().post(new EventBusShareCancelModel(true, MainActivity.class.getSimpleName()));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 切换界面时停止TTS播放
        try {
            TTSManager.Companion.getInstance().stop();
        } catch (Exception e) {
            Timber.tag(TAG).w("暂停时停止TTS播放失败" + e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        stopWakeupService();
        // 确保销毁时停止TTS播放
        try {
            unregisterReceiver(wakeupReceiver);
            TTSManager.Companion.getInstance().stop();
            MediaPlayerUtils.Companion.getInstance().release();
        } catch (Exception e) {
            Timber.tag(TAG).w("销毁时停止TTS播放失败" + e);
        }

//        EventBus.getDefault().unregister(this);
    }


    private void stopWakeupService() {
        Intent serviceIntent = new Intent(this, WakeUpService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
//        super.onSaveInstanceState(outState);
//        super.onSaveInstanceState(null);
    }

    @Override
    protected void onNewIntent(Intent intent) {//唤醒调用
        super.onNewIntent(intent);
        String formFloatContent = intent.getStringExtra(Constants.WAKE_CONTENT);
        boolean isRefresh = intent.getBooleanExtra(Constants.REFRESH_STATUS, false);
        Timber.tag(TAG).e("唤醒onNewIntent = " + formFloatContent + "  isRefresh = " + isRefresh);
        if (!TextUtils.isEmpty(formFloatContent)) {
            notifyFragment(formFloatContent);
            return;
        }

        if (isRefresh) {
            notifyFragment();
        }


    }


    private void notifyFragment(String content) {
        SuperChatFragment fragment = getLingXiFragment();
        if (fragment != null) {
            fragment.onFloatContent(content);
        }
    }

    private void notifyFragment() {
        SuperChatFragment fragment = getLingXiFragment();
        if (fragment != null) {
            fragment.refreshChatHistory();
        }
    }

    private SuperChatFragment getLingXiFragment() {
        selectTab(TAB_LINGXI);
        TrackerUtils.trackBottomNavigationClickEvent("灵犀");
        if (currentFragment instanceof SuperChatFragment) {
            return (SuperChatFragment) currentFragment;
        }
        return null;
    }

    // 语音唤醒当前处于MainActivity直接传递数据
    public void onReceiveFloatContent(String formFloatContent) {
        // 处理传过来的数据
        runOnUiThread(() -> {
            // 更新 UI 或执行业务逻辑
            if (!TextUtils.isEmpty(formFloatContent)) {
                notifyFragment(formFloatContent);
            }
        });
    }

}