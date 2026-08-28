package com.fxzs.lingxiagent.lingxi.translate;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.translate.adapter.DialogMessageAdapter;
import com.fxzs.lingxiagent.lingxi.translate.model.DialogMessage;
import com.fxzs.lingxiagent.lingxi.translate.model.DialogRecord;
import com.fxzs.lingxiagent.lingxi.translate.model.TranslationItem;
import com.fxzs.lingxiagent.lingxi.translate.util.LanguageUtils;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.viewmodel.translate.VMDialogMode;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

/**
 * 对话模式页面 - 双向翻译
 */
public class DialogModeActivity extends BaseActivity<VMDialogMode> {

    private static final int PERMISSION_REQUEST_RECORD_AUDIO_A = 2001;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO_B = 2002;

    private ImageView ivBack;
    private ImageView ivExport;
    private ImageView ivFlip;
    private TextView tvLanguageA;
    private TextView tvLanguageA2;
    private TextView tvLanguageB;
    private RecyclerView rvMessagesA;
    private RecyclerView rvMessagesB;
    private DialogMessageAdapter adapterA;
    private DialogMessageAdapter adapterB;
    private View topCardContainer;
    private boolean isTopFlipped = false;

    private List<DialogMessage> messagesA;  // A方看到的消息列表
    private List<DialogMessage> messagesB;  // B方看到的消息列表
    
    // 拆分中间态和完成态消息
    private DialogMessage currentMidMessageA;  // A方当前中间态消息（带三点）
    private DialogMessage currentMidMessageB;  // B方当前中间态消息（带三点）
    private List<DialogMessage> finishedMessagesA = new ArrayList<>();  // A方完成态消息列表
    private List<DialogMessage> finishedMessagesB = new ArrayList<>();  // B方完成态消息列表

    // 最新记录与导出
    private List<DialogRecord> records = new ArrayList<>();
    private DialogRecord currentRecord = null;
    private int translationIdCounter = 1;

    // 全局历史消息跟踪，防止被WebSocketTranslationManager清空
    private Set<String> processedMessageIds = new HashSet<>();

    // 播报管理
    private String pendingTtsText = null;

    private ImageView btnMicrophoneA;
    private ImageView btnMicrophoneB;
    private ImageView ivMicMaskA;
    private ImageView ivMicMaskB;
    private ObjectAnimator maskAnimatorA;
    private ObjectAnimator maskAnimatorB;
    
    // 静默检测配置
    private static final long SILENCE_TIMEOUT_MS = 3000;  // 3秒静默超时（可配置）
    private android.os.Handler silenceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable silenceTimeoutRunnable;
    private long lastMessageTimestamp = 0;  // 最后一次收到消息的时间戳
    private ImageView ivPlayA, ivPlayA1;
    private ImageView ivPlayB, ivPlayB1;
    private TextView tvHintA;
    private TextView tvHintB;
    private View ll_language_a;
    private View ll_language_b;

    private WebSocketTranslationManager translationManagerA;
    private WebSocketTranslationManager translationManagerB;
    private boolean isRecordingA = false;
    // 播放按钮的侧标记
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 5011;

    private static final int SIDE_NONE = 0;
    private static final int SIDE_A = 1;
    private static final int SIDE_B = 2;
    private int currentPlaySide = SIDE_NONE;
    private int pendingPlaySide = SIDE_NONE;
    private int activeMicSide = SIDE_NONE; // 当前占用麦克风的侧（A=1/B=2/NONE=0）
    private int lastClickedSide = SIDE_NONE; // 最近一次点击侧，用于权限回调后恢复启动

    private boolean isRecordingB = false;

    // 当前内容缓存和消息管理
    private String currentContentA = "";  // A方当前显示内容
    private String currentContentB = "";  // B方当前显示内容
    
    // 待保存的FIN消息列表（停止录音时批量保存）
    private List<TranslationItem> pendingFinMessagesA = new ArrayList<>();
    private List<TranslationItem> pendingFinMessagesB = new ArrayList<>();
    private String currentSeIdA = "";     // 当前A方消息的seId
    private String currentSeIdB = "";     // 当前B方消息的seId

    // 临时显示的实时识别结果
    private String currentMidResult = "";
    private boolean showingMidResult = false;

    private BroadcastReceiver translationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Timber.tag("DialogModeActivity").d("Received broadcast: " + action);

            if ("com.fxzs.lingxiagent.KEY_MID_RESULT".equals(action)) {
                String midResult = intent.getStringExtra("mid_result");
                Timber.tag("DialogModeActivity").d("MID_RESULT: " + midResult);
                // MID_RESULT用于显示实时识别结果，但不持久化
                updateCurrentMidResult(midResult);
            } else if ("com.fxzs.lingxiagent.CACHE_LIST_UPDATE".equals(action)) {
                // 主要的更新逻辑，获取去重后的TranslationItem列表
                updateFromCacheList();
            } else if ("com.fxzs.lingxiagent.complete".equals(action)) {
                // 不要自动停止，保持连接
                // stopAllRecording();
            }
        }
    };

    /**
     * 平滑滚动到最新消息（正常布局，最新消息在底部）
     * 与聆听模式保持一致
     */
    private void scrollToLatestMessage(RecyclerView rv, RecyclerView.Adapter adapter) {
        if (rv == null || adapter == null) return;
        
        int total = adapter.getItemCount();
        if (total > 0) {
            // 使用 smoothScrollToPosition 平滑滚动到最后一条消息（最新消息）
            rv.smoothScrollToPosition(total - 1);
            Timber.tag("DialogModeActivity").d("平滑滚动到最新消息（位置 " + (total - 1) + "）");
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.fxzs.lingxiagent.KEY_MID_RESULT");
        filter.addAction("com.fxzs.lingxiagent.CACHE_LIST_UPDATE");
        filter.addAction("com.fxzs.lingxiagent.complete");
        LocalBroadcastManager.getInstance(this).registerReceiver(translationReceiver, filter);

        // 进入页面时调用同传记录接口（对话模式 type=2），由 ViewModel 负责网络逻辑
        try {
            viewModel.addDialogTranslationRecord();
        } catch (Exception e) {
            Timber.tag("DialogModeActivity").e("Failed to add translation record: " + e.getMessage());
        }

        // 初始化WebSocket翻译管理器
        translationManagerA = new WebSocketTranslationManager(this, new WebSocketTranslationManager.TranslationCallback() {
            @Override
            public void onTranslationStarted() {
                runOnUiThread(() -> {
                    isRecordingA = true;
                    btnMicrophoneA.setSelected(true);
                    btnMicrophoneA.setImageResource(R.drawable.ic_translate_mic_active);
                    // 显示并启动mask呼吸动画
                    if (ivMicMaskA != null) {
                        ivMicMaskA.setVisibility(View.VISIBLE);
                        ivMicMaskA.setAlpha(0.2f);
                        if (maskAnimatorA != null) maskAnimatorA.start();
                    }
                    tvHintA.setText("正在录音中，请说话...");
                    tvHintA.setVisibility(View.GONE);
                    tvHintB.setVisibility(View.GONE);
                    // 禁用B侧按钮
                    btnMicrophoneB.setEnabled(false);
                    btnMicrophoneB.setAlpha(0.5f);
                    
                    // 禁用语言选择和朗读按钮
                    disableControlsDuringRecording();
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        // A侧WS连接成功，显示加载三点
                        showLoadingDotsOnStart(true);
                    }
                });

            }

            @Override
            public void onTranslationStopped() {
                runOnUiThread(() -> {
                    isRecordingA = false;
                    btnMicrophoneA.setSelected(false);
                    // 恢复静态图标与停止mask动画
                    btnMicrophoneA.setImageResource(R.drawable.ic_translate_mic);
                    if (maskAnimatorA != null) maskAnimatorA.cancel();
                    if (ivMicMaskA != null) ivMicMaskA.setVisibility(View.GONE);
                    tvHintA.setText("点击麦克风按钮即可开始对话。");

                    // 隐藏三点
                    hideAllLoadingDots();

                    // 保存未完成的中间结果（如果有）
                    saveIncompleteMessages(true);

                    // 如果有待播报文本，断开后再播报
                    if (pendingTtsText != null && !pendingTtsText.isEmpty()) {
                        // 开始播放，设置当前播放侧并显示按钮
                        currentPlaySide = pendingPlaySide;
                        updatePlayButtonsVisibility();
                        playTranslationTts(pendingTtsText);
                        pendingTtsText = null;
                    }

                    // A结束时，恢复B侧按钮
                    btnMicrophoneB.setEnabled(true);
                    btnMicrophoneB.setAlpha(1f);
                    activeMicSide = SIDE_NONE;
                    lastClickedSide = SIDE_NONE;
                    
                    // 恢复语言选择和朗读按钮
                    enableControlsAfterRecording();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // 错误时也隐藏三点
                    hideAllLoadingDots();

                    isRecordingA = false;
                    btnMicrophoneA.setSelected(false);
                    tvHintA.setText("翻译出错: " + error);

                    // 保存未完成的中间结果
                    saveIncompleteMessages(true);

                    // 清除临时显示
                    showingMidResult = false;
                    currentMidResult = "";
                    // 出错时释放互斥锁并恢复B侧按钮
                    btnMicrophoneB.setEnabled(true);
                    btnMicrophoneB.setAlpha(1f);
                    activeMicSide = SIDE_NONE;
                    lastClickedSide = SIDE_NONE;
                    
                    // 恢复语言选择和朗读按钮
                    enableControlsAfterRecording();
                });
            }
        });

        translationManagerB = new WebSocketTranslationManager(this, new WebSocketTranslationManager.TranslationCallback() {
            @Override
            public void onTranslationStarted() {
                runOnUiThread(() -> {
                    isRecordingB = true;
                    btnMicrophoneB.setSelected(true);
                    btnMicrophoneB.setImageResource(R.drawable.ic_translate_mic_active);
                    // 显示并启动mask呼吸动画
                    if (ivMicMaskB != null) {
                        ivMicMaskB.setVisibility(View.VISIBLE);
                        ivMicMaskB.setAlpha(0.2f);
                        if (maskAnimatorB != null) maskAnimatorB.start();
                    }
                    // B侧WS连接成功，显示加载三点
                    showLoadingDotsOnStart(false);

                    tvHintB.setText("正在录音中，请说话...");
                    tvHintA.setVisibility(View.GONE);
                    tvHintB.setVisibility(View.GONE);
                    // 禁用A侧按钮
                    btnMicrophoneA.setEnabled(false);
                    btnMicrophoneA.setAlpha(0.5f);
                    
                    // 禁用语言选择和朗读按钮
                    disableControlsDuringRecording();
                });
            }

            @Override
            public void onTranslationStopped() {
                runOnUiThread(() -> {
                    isRecordingB = false;
                    btnMicrophoneB.setSelected(false);
                    // 隐藏三点
                    hideAllLoadingDots();

                    // 恢复静态图标与停止mask动画
                    btnMicrophoneB.setImageResource(R.drawable.ic_translate_mic);
                    if (maskAnimatorB != null) maskAnimatorB.cancel();
                    if (ivMicMaskB != null) ivMicMaskB.setVisibility(View.GONE);
                    tvHintB.setText("点击麦克风按钮即可开始对话。");

                    // 保存未完成的中间结果（如果有）
                    saveIncompleteMessages(false);

                    // 如果有待播报文本，断开后再播报
                    if (pendingTtsText != null && !pendingTtsText.isEmpty()) {
                        // 开始播放，设置当前播放侧并显示按钮
                        currentPlaySide = pendingPlaySide;
                        updatePlayButtonsVisibility();
                        playTranslationTts(pendingTtsText);
                        pendingTtsText = null;
                    }

                    // B结束时，恢复A侧按钮
                    btnMicrophoneA.setEnabled(true);
                    btnMicrophoneA.setAlpha(1f);
                    activeMicSide = SIDE_NONE;
                    lastClickedSide = SIDE_NONE;
                    
                    // 恢复语言选择和朗读按钮
                    enableControlsAfterRecording();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isRecordingB = false;
                    btnMicrophoneB.setSelected(false);
                    tvHintB.setText("翻译出错: " + error);

                    // 初始化播放按钮
                    currentPlaySide = SIDE_NONE;
                    pendingPlaySide = SIDE_NONE;
                    updatePlayButtonsVisibility();

                    // 保存未完成的中间结果
                    saveIncompleteMessages(false);

                    // 清除临时显示
                    showingMidResult = false;
                    currentMidResult = "";
                    // 出错时释放互斥锁并恢复A侧按钮
                    btnMicrophoneA.setEnabled(true);
                    btnMicrophoneA.setAlpha(1f);
                    activeMicSide = SIDE_NONE;
                    lastClickedSide = SIDE_NONE;
                    
                    // 恢复语言选择和朗读按钮
                    enableControlsAfterRecording();
                });
            }
        });
    }


    @Override
    protected int getLayoutResource() {
        return R.layout.activity_dialog_mode;
    }

    @Override
    protected Class<VMDialogMode> getViewModelClass() {
        return VMDialogMode.class;
    }

    @Override
    protected void setupDataBinding() {
        // 暂不需要数据绑定
    }

    @Override
    protected void initializeViews() {
        ivBack = findViewById(R.id.iv_back);
        ivExport = findViewById(R.id.iv_export);
        ivFlip = findViewById(R.id.iv_flip);
        ivPlayA = findViewById(R.id.iv_play_a);
        ivPlayA1 = findViewById(R.id.iv_play_a1);
        ivPlayB = findViewById(R.id.iv_play_b);
        ivPlayB1 = findViewById(R.id.iv_play_b1);
        tvLanguageA = findViewById(R.id.tv_language_a);
        tvLanguageA2 = findViewById(R.id.tv_language_a2);
        tvLanguageB = findViewById(R.id.tv_language_b);
        rvMessagesA = findViewById(R.id.rv_messages_a);  // A方消息列表
        rvMessagesB = findViewById(R.id.rv_messages_b);  // B方消息列表
        btnMicrophoneA = findViewById(R.id.btn_microphone_a);
        btnMicrophoneB = findViewById(R.id.btn_microphone_b);
        ll_language_a = findViewById(R.id.ll_language_a);
        ll_language_b = findViewById(R.id.ll_language_b);
        ivMicMaskA = findViewById(R.id.iv_mic_mask_a);
        ivMicMaskB = findViewById(R.id.iv_mic_mask_b);
        tvHintA = findViewById(R.id.tv_hint_a);
        tvHintB = findViewById(R.id.tv_hint_b);
        topCardContainer = findViewById(R.id.top_card_container);

        // 预创建淡入淡出动画（录音时的呼吸灯效果）
        maskAnimatorA = ObjectAnimator.ofFloat(ivMicMaskA, View.ALPHA, 0.2f, 0.85f, 0.2f);
        maskAnimatorA.setDuration(1200);
        maskAnimatorA.setRepeatCount(ValueAnimator.INFINITE);
        maskAnimatorA.setInterpolator(new AccelerateDecelerateInterpolator());

        maskAnimatorB = ObjectAnimator.ofFloat(ivMicMaskB, View.ALPHA, 0.2f, 0.85f, 0.2f);
        maskAnimatorB.setDuration(1200);
        maskAnimatorB.setRepeatCount(ValueAnimator.INFINITE);
        maskAnimatorB.setInterpolator(new AccelerateDecelerateInterpolator());

        // 初始化RecyclerView和适配器
        setupRecyclerViews();

        // 设置点击事件
        setupClickListeners();
    }

    /**
     * // 播放按钮交互：点击可重新朗读最近一次的译文
     * ivPlayA.setOnClickListener(v -> replayLatestForSide(true));
     * ivPlayB.setOnClickListener(v -> replayLatestForSide(false));
     * ivPlayA.setVisibility(View.GONE);
     * ivPlayB.setVisibility(View.GONE);
     * <p>
     * 初始化RecyclerView和适配器
     */
    private void setupRecyclerViews() {
        // 确保消息列表已初始化
        if (messagesA == null) messagesA = new ArrayList<>();
        if (messagesB == null) messagesB = new ArrayList<>();

        // 初始化适配器
        adapterA = new DialogMessageAdapter(this, messagesA);
        adapterB = new DialogMessageAdapter(this, messagesB);

        // 设置布局管理器和适配器 - 正常布局，新消息在最下方（与聆听模式一致）
        LinearLayoutManager layoutManagerA = new LinearLayoutManager(this);
        rvMessagesA.setLayoutManager(layoutManagerA);
        rvMessagesA.setAdapter(adapterA);

        LinearLayoutManager layoutManagerB = new LinearLayoutManager(this);
        rvMessagesB.setLayoutManager(layoutManagerB);
        rvMessagesB.setAdapter(adapterB);

        Timber.tag("DialogModeActivity").d("RecyclerViews setup complete with stack layout - adapterA: " + adapterA + ", adapterB: " + adapterB);
    }

    /**
     * 设置点击事件
     */
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        ivExport.setOnClickListener(v -> exportRecordsToJson());

        // 右上角翻转按钮：翻转上方显示框
        if (ivFlip != null) {
            ivFlip.setOnClickListener(v -> toggleTopFlip());
        }

        // 播放按钮交互：点击可重新朗读最近一次的译文
        ivPlayA.setOnClickListener(v -> replayLatestForSide(true));
        ivPlayB.setOnClickListener(v -> replayLatestForSide(false));
        ivPlayA1.setOnClickListener(v -> replayLatestForSide(true));
        ivPlayB1.setOnClickListener(v -> replayLatestForSide(false));
        ivPlayA.setVisibility(View.GONE);
        ivPlayB.setVisibility(View.GONE);

        ll_language_a.setOnClickListener(v -> showLanguageSelectionDialog(true));
        ll_language_b.setOnClickListener(v -> showLanguageSelectionDialog(false));
        // 监听 TTS 播放结束，复位播放按钮
        /*TtsXiaDuMediaPlayer.getInstance().setOnPlayerStopListener(() -> runOnUiThread(() -> {
            currentPlaySide = SIDE_NONE;
            updatePlayButtonsVisibility();
        }));*/
        // TTS 监听 TTS 播放结束，复位播放按钮 todo


        btnMicrophoneA.setOnClickListener(v -> {
            // 若B侧占用或正在录音，直接拦截
            if (activeMicSide == SIDE_B || isRecordingB) {
                Toast.makeText(DialogModeActivity.this, "B侧正在录音，请先停止", Toast.LENGTH_SHORT).show();
                return;
            }
            ll_language_a.setClickable(false);
            ll_language_b.setClickable(false);
            lastClickedSide = SIDE_A;
            if (isRecordingA) {
                // 同侧可停止，显示确认弹窗
                showStopRecordingConfirmDialog(true);
            } else {
                // 先加锁并禁用对侧，避免双启动
                activeMicSide = SIDE_A;
                btnMicrophoneB.setEnabled(false);
                btnMicrophoneB.setAlpha(0.5f);
                startRecordingA();
            }
        });

        btnMicrophoneB.setOnClickListener(v -> {
            // 若A侧占用或正在录音，直接拦截
            if (activeMicSide == SIDE_A || isRecordingA) {
                Toast.makeText(DialogModeActivity.this, "A侧正在录音，请先停止", Toast.LENGTH_SHORT).show();
                return;
            }
            ll_language_a.setClickable(false);
            ll_language_b.setClickable(false);
            lastClickedSide = SIDE_B;
            if (isRecordingB) {
                // 同侧可停止，显示确认弹窗
                showStopRecordingConfirmDialog(false);
            } else {
                // 先加锁并禁用对侧，避免双启动
                activeMicSide = SIDE_B;
                btnMicrophoneA.setEnabled(false);
                btnMicrophoneA.setAlpha(0.5f);
                startRecordingB();
            }
        });

        // 录音互斥：当A录音中，B按钮禁用；当B录音中，A按钮禁用
        runOnUiThread(() -> {
            btnMicrophoneA.setEnabled(!isRecordingB);
            btnMicrophoneB.setEnabled(!isRecordingA);
            btnMicrophoneA.setAlpha(isRecordingB ? 0.5f : 1f);
            btnMicrophoneB.setAlpha(isRecordingA ? 0.5f : 1f);
        });
    }

    @Override
    protected void setupObservers() {
        // 观察语言A
        viewModel.getLanguageA().observe(this, language -> {
            if (language != null) {
                tvLanguageA.setText(LanguageUtils.getInstance().getTargetLanguageName(language));
//                tvLanguageA2.setText(language);
            }
        });

        // 观察语言B
        viewModel.getLanguageB().observe(this, language -> {
            if (language != null) {
                tvLanguageB.setText(LanguageUtils.getInstance().getSourceLanguagesName(language));
            }
        });
    }


    /**
     * 将WS断开前最后一条消息定格为稳态并更新UI（原文/译文同时稳态显示，隐藏三点）
     */
    private void finalizeLastMessageAndHideDots(boolean forSideA) {
        WebSocketTranslationManager mgr = forSideA ? translationManagerA : translationManagerB;
        if (mgr == null) return;
        List<TranslationItem> cache = mgr.getLocalCacheList();
        if (cache == null || cache.isEmpty()) return;
        TranslationItem item = cache.get(cache.size() - 1);

        // 原文（稳态）
        DialogMessage originalMsg = new DialogMessage(item.getSeId() + "_orig", item.getSourceText(), DialogMessage.TYPE_ORIGINAL, forSideA ? "A方" : "B方", false);
        // 译文（稳态，展示在对侧）
        DialogMessage translationMsg = new DialogMessage(item.getSeId() + "_trans", item.getTargetText(), DialogMessage.TYPE_TRANSLATION, forSideA ? "A方" : "B方", false);

        if (forSideA) {
            addOrUpdateMessageInList(messagesA, originalMsg);
            addOrUpdateMessageInList(messagesB, translationMsg);
        } else {
            addOrUpdateMessageInList(messagesB, originalMsg);
            addOrUpdateMessageInList(messagesA, translationMsg);
        }

        // 隐藏所有loading三点
        boolean hasChanges = false;
        if (messagesA != null) {
            for (DialogMessage m : messagesA) {
                if (m.isRecognizing()) {
                    m.setRecognizing(false);
                    hasChanges = true;
                }
            }
        }
        if (messagesB != null) {
            for (DialogMessage m : messagesB) {
                if (m.isRecognizing()) {
                    m.setRecognizing(false);
                    hasChanges = true;
                }
            }
        }
        
        // 只在真正有变化时才通知更新
        if (hasChanges) {
            if (adapterA != null && !messagesA.isEmpty()) {
                adapterA.notifyItemChanged(0);
            }
            if (adapterB != null && !messagesB.isEmpty()) {
                adapterB.notifyItemChanged(0);
            }
        }

        showingMidResult = false;
        currentMidResult = "";
    }


    /**
     * 完成未完成的消息（当切换说话人或WS连接中断时）
     */
    private void finalizeIncompleteMessages() {
        WebSocketTranslationManager currentManager = null;

        // 获取当前活动的翻译管理器
        if (isRecordingA && translationManagerA != null) {
            currentManager = translationManagerA;
        } else if (isRecordingB && translationManagerB != null) {
            currentManager = translationManagerB;
        }

        if (currentManager != null) {
            List<TranslationItem> cacheList = currentManager.getLocalCacheList();
            boolean hasIncompleteItems = false;

            // 检查是否有未完成的项目
            for (TranslationItem item : cacheList) {
                if (!item.isEnd()) {
                    hasIncompleteItems = true;
                    Timber.tag("DialogModeActivity").d("Finalizing incomplete item: " + item.getSeId() + " - " + item.getSourceText());

                    // 强制标记为完成状态
                    item.setIsEnd(true);
                }
            }

            // 如果有未完成的项目被强制完成，触发最后一次更新
            if (hasIncompleteItems) {
                Timber.tag("DialogModeActivity").d("Forcing final update for incomplete messages");
                updateFromCacheList();
            }
        }
    }

    /**
     * 更新当前实时识别结果显示
     */
    private void updateCurrentMidResult(String midResult) {
        if (midResult == null || midResult.isEmpty()) {
            return;
        }

        currentMidResult = midResult;
        showingMidResult = true;

        // 在适配器中显示实时结果，只更新第一项避免抖动
        runOnUiThread(() -> {
            if (isRecordingA && adapterA != null && !messagesA.isEmpty()) {
                adapterA.notifyItemChanged(0);
            } else if (isRecordingB && adapterB != null && !messagesB.isEmpty()) {
                adapterB.notifyItemChanged(0);
            }
        });
    }

    /**
     * 从缓存列表更新消息显示
     * 
     * 新逻辑：
     * 1. type="MID" (seId="current_message") - 覆盖更新中间态消息，带三个点
     * 2. type="FIN" (seId="fin_xxx") - 追加到完成态列表队尾（位置0），不更新整个列表
     * 3. 最新消息永远在最上方（栈式布局）
     */
    private void updateFromCacheList() {
        runOnUiThread(() -> {
            WebSocketTranslationManager currentManager = null;
            boolean isFromSideA = false;

            // 获取当前活动的翻译管理器的缓存列表
            if (isRecordingA && translationManagerA != null) {
                currentManager = translationManagerA;
                isFromSideA = true;
            } else if (isRecordingB && translationManagerB != null) {
                currentManager = translationManagerB;
                isFromSideA = false;
            }

            if (currentManager != null) {
                List<TranslationItem> cacheList = currentManager.getLocalCacheList();
                Timber.tag("DialogModeActivity").d("Updating from cache list, size: " + cacheList.size());

                if (cacheList.isEmpty()) {
                    return;
                }

                // 处理每条消息
                boolean hasMidMessage = false;
                boolean hasFinMessage = false;
                
                for (TranslationItem item : cacheList) {
                    String seId = item.getSeId();
                    
                    if ("current_message".equals(seId)) {
                        // 中间态消息 - 覆盖更新，带三个点
                        updateMidMessage(item, isFromSideA);
                        hasMidMessage = true;
                    } else if (seId.startsWith("fin_")) {
                        // 完成态消息 - 追加到队尾（位置0）
                        addFinishedMessage(item, isFromSideA);
                        hasFinMessage = true;
                    }
                }
                
                // 收到MID消息：取消静默倒计时
                if (hasMidMessage) {
                    cancelSilenceTimeout();
                }
                
                // 收到FIN消息：启动3秒倒计时，如果3秒内没有新的MID消息则自动断开
                if (hasFinMessage) {
                    startSilenceTimeout();
                }
            }

            // 根据当前播放状态更新按钮显示
            updatePlayButtonsVisibility();

            // 清除实时结果显示
            showingMidResult = false;
            currentMidResult = "";
        });
    }
    
    /**
     * 更新中间态消息（覆盖更新，带三个点）
     */
    private void updateMidMessage(TranslationItem item, boolean isFromSideA) {
        String originalText = item.getSourceText();
        String translationText = item.getTargetText();
        
        if (originalText.isEmpty() && translationText.isEmpty()) {
            return;
        }
        
        // 创建或更新中间态消息（带三个点）
        DialogMessage originalMsg = new DialogMessage(
            "mid_original",
            originalText,
            DialogMessage.TYPE_ORIGINAL,
            isFromSideA ? "A方" : "B方",
            true  // 显示三点动画
        );
        
        DialogMessage translationMsg = new DialogMessage(
            "mid_translation",
            translationText,
            DialogMessage.TYPE_TRANSLATION,
            isFromSideA ? "A方" : "B方",
            true  // 显示三点动画
        );
        
        // 更新中间态消息
        if (isFromSideA) {
            currentMidMessageA = originalMsg;
            currentMidMessageB = translationMsg;
            
            // 覆盖更新位置0的消息
            updateMidMessageInList(messagesA, originalMsg, adapterA);
            updateMidMessageInList(messagesB, translationMsg, adapterB);
        } else {
            currentMidMessageB = originalMsg;
            currentMidMessageA = translationMsg;
            
            // 覆盖更新位置0的消息
            updateMidMessageInList(messagesB, originalMsg, adapterB);
            updateMidMessageInList(messagesA, translationMsg, adapterA);
        }
        
        Timber.tag("DialogModeActivity").d("Updated MID message: " + originalText);
    }
    
    /**
     * 在列表中覆盖更新中间态消息
     * 新消息在列表末尾（与聆听模式一致）
     */
    private void updateMidMessageInList(List<DialogMessage> messageList, DialogMessage midMessage, DialogMessageAdapter adapter) {
        if (messageList.isEmpty()) {
            // 列表为空，直接添加到末尾
            messageList.add(midMessage);
            if (adapter != null) {
                adapter.notifyItemInserted(messageList.size() - 1);
            }
        } else {
            int lastIndex = messageList.size() - 1;
            DialogMessage lastMessage = messageList.get(lastIndex);
            
            // 检查最后一条消息是否是中间态消息
            if (lastMessage.getSeId().startsWith("mid_")) {
                // 覆盖更新中间态消息
                if (!lastMessage.getText().equals(midMessage.getText())) {
                    lastMessage.setText(midMessage.getText());
                    lastMessage.setRecognizing(true);
                    if (adapter != null) {
                        adapter.notifyItemChanged(lastIndex);
                    }
                }
            } else {
                // 最后一条是完成态消息，在末尾添加新的中间态消息
                messageList.add(midMessage);
                if (adapter != null) {
                    adapter.notifyItemInserted(messageList.size() - 1);
                    scrollToLatestMessage(adapter == adapterA ? rvMessagesA : rvMessagesB, adapter);
                }
            }
        }
    }
    
    /**
     * 添加完成态消息到队尾（位置0），不更新整个列表
     * 平滑转换：如果位置0是中间态消息，直接更新为完成态，避免闪烁
     */
    private void addFinishedMessage(TranslationItem item, boolean isFromSideA) {
        String seId = item.getSeId();
        
        // 检查是否已经添加过这条消息
        List<DialogMessage> finishedList = isFromSideA ? finishedMessagesA : finishedMessagesB;
        for (DialogMessage msg : finishedList) {
            if (msg.getSeId().equals(seId + "_orig") || msg.getSeId().equals(seId + "_trans")) {
                // 已经添加过，跳过
                return;
            }
        }
        
        String originalText = item.getSourceText();
        String translationText = item.getTargetText();
        
        if (originalText.isEmpty() && translationText.isEmpty()) {
            return;
        }
        
        // 创建完成态消息（不带三个点）
        DialogMessage originalMsg = new DialogMessage(
            seId + "_orig",
            originalText,
            DialogMessage.TYPE_ORIGINAL,
            isFromSideA ? "A方" : "B方",
            false  // 不显示三点动画
        );
        
        DialogMessage translationMsg = new DialogMessage(
            seId + "_trans",
            translationText,
            DialogMessage.TYPE_TRANSLATION,
            isFromSideA ? "A方" : "B方",
            false  // 不显示三点动画
        );
        
        // 添加到完成态列表
        if (isFromSideA) {
            finishedMessagesA.add(originalMsg);
            finishedMessagesB.add(translationMsg);
        } else {
            finishedMessagesB.add(originalMsg);
            finishedMessagesA.add(translationMsg);
        }
        
        // 平滑转换：将中间态消息转换为完成态消息（不删除再插入）
        if (isFromSideA) {
            convertMidToFinMessage(messagesA, originalMsg, adapterA);
            convertMidToFinMessage(messagesB, translationMsg, adapterB);
        } else {
            convertMidToFinMessage(messagesB, originalMsg, adapterB);
            convertMidToFinMessage(messagesA, translationMsg, adapterA);
        }
        
        // 收集到待保存列表中
        if (isFromSideA) {
            if (!pendingFinMessagesA.contains(item)) {
                pendingFinMessagesA.add(item);
            }
        } else {
            if (!pendingFinMessagesB.contains(item)) {
                pendingFinMessagesB.add(item);
            }
        }
        
        Timber.tag("DialogModeActivity").d("Added FIN message to position 0: " + originalText);
    }
    
    /**
     * 将中间态消息平滑转换为完成态消息
     * 如果最后一条是中间态消息，直接更新内容和状态；否则插入新消息
     * 新消息在列表末尾（与聆听模式一致）
     */
    private void convertMidToFinMessage(List<DialogMessage> messageList, 
                                       DialogMessage finMessage, 
                                       DialogMessageAdapter adapter) {
        if (messageList.isEmpty()) {
            // 列表为空，直接添加到末尾
            messageList.add(finMessage);
            if (adapter != null) {
                adapter.notifyItemInserted(messageList.size() - 1);
            }
        } else {
            int lastIndex = messageList.size() - 1;
            DialogMessage lastMessage = messageList.get(lastIndex);
            
            if (lastMessage.getSeId().startsWith("mid_")) {
                // 最后一条是中间态消息，平滑转换为完成态
                lastMessage.setSeId(finMessage.getSeId());
                lastMessage.setText(finMessage.getText());
                lastMessage.setRecognizing(false);  // 关闭三点动画
                
                if (adapter != null) {
                    adapter.notifyItemChanged(lastIndex);  // 只更新最后一条，不删除再插入
                }
                
                Timber.tag("DialogModeActivity").d("Smoothly converted MID to FIN at last position");
            } else {
                // 最后一条不是中间态消息，在末尾添加新消息
                messageList.add(finMessage);
                if (adapter != null) {
                    adapter.notifyItemInserted(messageList.size() - 1);
                }
                
                Timber.tag("DialogModeActivity").d("Inserted new FIN message at last position");
            }
        }
    }
    
    /**
     * 从列表中移除中间态消息
     */
    private void removeMidMessageFromList(List<DialogMessage> messageList, DialogMessageAdapter adapter) {
        if (messageList.isEmpty()) {
            return;
        }
        
        DialogMessage topMessage = messageList.get(0);
        if (topMessage.getSeId().startsWith("mid_")) {
            messageList.remove(0);
            if (adapter != null) {
                adapter.notifyItemRemoved(0);
            }
        }
    }
    
    /**
     * 保存已完成的消息到数据库
     */
    private void saveFinishedMessage(TranslationItem item, boolean isFromSideA) {
        if (item == null || item.getSourceText().isEmpty()) {
            return;
        }
        
        try {
            DialogRecord record = new DialogRecord();
            record.setTranslationId((int) com.fxzs.lingxiagent.util.SharedPreferencesUtil.getTranslationId());
            record.setSpeakerId(isFromSideA ? 1 : 2);
            record.setSource(isFromSideA ? getCurrentSourceLangForA() : getCurrentSourceLangForB());
            record.setTarget(isFromSideA ? getCurrentTargetLangForA() : getCurrentTargetLangForB());
            record.setSourceText(item.getSourceText());
            record.setTargetText(item.getTargetText());
            record.setStartTime(formatNow());
            record.setEndTime(formatNow());
            
            // 保存到数据库
            viewModel.saveLatestDialogMessage(record);
            
            Timber.tag("DialogModeActivity").d("Saved FIN message: " + item.getSourceText());
        } catch (Exception e) {
            Timber.tag("DialogModeActivity").e("Failed to save message: " + e.getMessage());
        }
    }
    
    /**
     * 批量保存所有待保存的消息（用户主动停止或服务器断开时）
     * 包括所有收集的FIN消息和未完成的中间结果
     */
    private void saveIncompleteMessages(boolean isFromSideA) {
        List<TranslationItem> pendingMessages = isFromSideA ? pendingFinMessagesA : pendingFinMessagesB;
        
        // 1. 批量保存所有收集的FIN消息
        if (!pendingMessages.isEmpty()) {
            Timber.tag("DialogModeActivity").d("Batch saving " + pendingMessages.size() + " FIN messages");
            
            for (TranslationItem item : pendingMessages) {
                saveFinishedMessage(item, isFromSideA);
            }
            
            // 清空待保存列表
            pendingMessages.clear();
        }
        
        // 2. 检查是否有未完成的中间结果需要保存
        WebSocketTranslationManager manager = isFromSideA ? translationManagerA : translationManagerB;
        if (manager != null) {
            List<TranslationItem> cacheList = manager.getLocalCacheList();
            if (cacheList != null && !cacheList.isEmpty()) {
                // 查找未完成的中间结果
                for (TranslationItem item : cacheList) {
                    if ("current_message".equals(item.getSeId()) && !item.getSourceText().isEmpty()) {
                        // 将中间结果作为最终结果保存
                        saveFinishedMessage(item, isFromSideA);
                        Timber.tag("DialogModeActivity").d("Saved incomplete message as final: " + item.getSourceText());
                        break;
                    }
                }
            }
        }
    }

    /**
     * 在消息列表中添加或更新消息（栈式布局：新消息在位置 0）
     * 由于使用了 reverseLayout，位置 0 会显示在顶部
     * 
     * 注意：此方法已被新的 updateMidMessage 和 addFinishedMessage 替代
     * 保留此方法以兼容其他可能的调用
     */
    @Deprecated
    private void addOrUpdateMessageInList(List<DialogMessage> messageList, DialogMessage newMessage) {
        if (messageList.isEmpty()) {
            // 列表为空，直接添加到位置 0
            messageList.add(0, newMessage);
            Timber.tag("DialogModeActivity").d("Added new message at position 0: " + newMessage.getText());
        } else {
            // 检查位置 0 的消息是否是当前正在识别的消息
            DialogMessage topMessage = messageList.get(0);
            
            if ("display_original".equals(topMessage.getSeId()) || "display_translation".equals(topMessage.getSeId())) {
                // 位置 0 是当前消息，只在内容真正变化时才更新
                if (!topMessage.getText().equals(newMessage.getText()) ||
                    topMessage.isRecognizing() != newMessage.isRecognizing()) {
                    
                    // 直接更新现有消息的内容，避免清空重建
                    topMessage.setText(newMessage.getText());
                    topMessage.setRecognizing(newMessage.isRecognizing());
                    
                    Timber.tag("DialogModeActivity").d("Updated message at position 0: " + newMessage.getText());
                }
                // 如果内容没变化，不做任何操作
            } else {
                // 位置 0 不是当前消息（是历史消息），在位置 0 插入新消息
                messageList.add(0, newMessage);
                Timber.tag("DialogModeActivity").d("Inserted new message at position 0 (stack layout): " + newMessage.getText());
            }
        }
    }

    /**
     * 播放翻译结果的TTS语音
     */
    private void playTranslationTts(String translationText) {
        if (translationText == null || translationText.trim().isEmpty()) {
            return;
        }

        // 检查当前目标语言是否支持TTS
        String currentTargetLang = getCurrentTargetLanguageForTTS();
        if (currentTargetLang != null && !TTSManager.Companion.getInstance().getTargetLanguageList().contains(currentTargetLang)) {
            Toast.makeText(this, "暂不支持该译文朗读", Toast.LENGTH_SHORT).show();
            return;
        }

        Timber.tag("DialogModeActivity").d("playTranslationTts: currentPlaySide=" + currentPlaySide + ", text=" + translationText);
        
        // 开始播放动画
        startPlayAnimation();
        
        // 设置播放监听器
        TTSManager.Companion.getInstance().setOnPlayerListener(new com.fxzs.lingxiagent.util.audio.OnPlayerListener() {
            @Override
            public void playerStart() {
                Timber.tag("DialogModeActivity").d("TTS playerStart callback");
                // 播放开始时的处理（如果需要）
            }
            
            @Override
            public void playerStop() {
                Timber.tag("DialogModeActivity").d("TTS playerStop callback");
                runOnUiThread(() -> {
                    // 停止播放动画，重置按钮
                    stopPlayAnimation();
                    currentPlaySide = SIDE_NONE;
                    updatePlayButtonsVisibility();
                });
            }
        });
        
        TTSManager.Companion.getInstance().textForceToAudio(translationText);
    }

    /**
     * 更新消息列表显示
     */
    private void updateMessageLists() {
        runOnUiThread(() -> {
            if (adapterA != null) {
                adapterA.notifyDataSetChanged();
            }
            if (adapterB != null) {
                adapterB.notifyDataSetChanged();
            }
        });
        // 先检查录音权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkAudioPermission();
            return; // 等待权限回调
        }

    }

    @Override
    protected void handleLoadingState(boolean loading) {
        // 处理加载状态
    }

    private void startRecordingA() {
        // 检查录音权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // 权限未授予，请求权限
            checkAudioPermission();
            return;
        }

        // 停止B的录音
        if (isRecordingB) {
            stopRecordingB();
        }

        Timber.tag("DialogModeActivity").d("Starting recording A");

        // 清空上一次录音的识别消息列表（后端缓存）
        if (translationManagerA != null) {
            translationManagerA.clearLocalCache();
            Timber.tag("DialogModeActivity").d("Cleared A's local cache before starting new recording");
        }
        
        // 清空待保存的消息列表
        pendingFinMessagesA.clear();
        
        // 清空中间态和完成态消息
        currentMidMessageA = null;
        currentMidMessageB = null;
        finishedMessagesA.clear();
        finishedMessagesB.clear();
        
        // 立即清空界面显示的消息列表（原文和译文）
        if (messagesA != null && adapterA != null) {
            messagesA.clear();
            adapterA.notifyDataSetChanged();
            Timber.tag("DialogModeActivity").d("Cleared A's UI message list immediately");
        }
        if (messagesB != null && adapterB != null) {
            messagesB.clear();
            adapterB.notifyDataSetChanged();
            Timber.tag("DialogModeActivity").d("Cleared B's UI message list immediately");
        }
        
        // 重置临时状态
        showingMidResult = false;
        currentMidResult = "";
        
        // 开启新一轮录音时，隐藏所有播放按钮
        currentPlaySide = SIDE_NONE;
        pendingPlaySide = SIDE_NONE;
        updatePlayButtonsVisibility();
        pendingTtsText = null;

        String languageA = viewModel.getLanguageA().getValue();
        String languageB = viewModel.getLanguageB().getValue();

        if (languageA != null && languageB != null) {
            translationManagerA.startTranslation(languageA, languageB);
            // 不在这里显示三点，等待 onTranslationStarted 回调时显示
            // showLoadingDotsOnStart(true);
        } else {
            tvHintA.setText("请先选择源语言和目标语言");
        }
    }

    /**
     * 显示结束录音确认弹窗
     *
     * @param isForSideA 是否是A侧的录音
     */
    private void showStopRecordingConfirmDialog(boolean isForSideA) {
        new CommonDialog.Builder(this).setTitle("结束同声传译吗？").setMessage("目前正在录音中，确定要结束吗").setConfirmText("结束").setConfirmTextRed(true).setCancelText("取消").setOnClickListener(new CommonDialog.OnDialogClickListener() {
            @Override
            public void onConfirm() {
                ll_language_a.setClickable(true);
                ll_language_b.setClickable(true);
                if (isForSideA) {
                    stopRecordingA();
                } else {
                    stopRecordingB();
                }
            }

            @Override
            public void onCancel() {
                // 用户取消，不做任何操作
            }
        }).show();
    }

    private void stopRecordingA() {
        // 取消静默倒计时
        cancelSilenceTimeout();
        
        translationManagerA.stopTranslation();
    }

    private void startRecordingB() {
        // 检查录音权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkAudioPermission();
            return;
        }

        // 停止A的录音
        if (isRecordingA) {
            stopRecordingA();
        }

        Timber.tag("DialogModeActivity").d("Starting recording B");

        // 清空上一次录音的识别消息列表（后端缓存）
        if (translationManagerB != null) {
            translationManagerB.clearLocalCache();
            Timber.tag("DialogModeActivity").d("Cleared B's local cache before starting new recording");
        }
        
        // 清空待保存的消息列表
        pendingFinMessagesB.clear();
        
        // 清空中间态和完成态消息
        currentMidMessageA = null;
        currentMidMessageB = null;
        finishedMessagesA.clear();
        finishedMessagesB.clear();
        
        // 立即清空界面显示的消息列表（原文和译文）
        if (messagesA != null && adapterA != null) {
            messagesA.clear();
            adapterA.notifyDataSetChanged();
            Timber.tag("DialogModeActivity").d("Cleared A's UI message list immediately");
        }
        if (messagesB != null && adapterB != null) {
            messagesB.clear();
            adapterB.notifyDataSetChanged();
            Timber.tag("DialogModeActivity").d("Cleared B's UI message list immediately");
        }
        
        // 重置临时状态
        showingMidResult = false;
        currentMidResult = "";
        
        // 开启新一轮录音时，隐藏所有播放按钮
        currentPlaySide = SIDE_NONE;
        pendingPlaySide = SIDE_NONE;
        updatePlayButtonsVisibility();
        pendingTtsText = null;

        String languageA = viewModel.getLanguageA().getValue();
        String languageB = viewModel.getLanguageB().getValue();

        if (languageA != null && languageB != null) {
            translationManagerB.startTranslation(languageB, languageA);
            // 不在这里显示三点，等待 onTranslationStarted 回调时显示
            // showLoadingDotsOnStart(false);
        } else {
            tvHintB.setText("请先选择源语言和目标语言");
        }
    }

    private void checkAudioPermission() {
        AppPermissionRequestManager.requestAudioPermission(this, PERMISSION_REQUEST_RECORD_AUDIO, AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_SI);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO_A || requestCode == PERMISSION_REQUEST_RECORD_AUDIO_B) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，重新开始录音
                if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO_A) {
                    startRecordingA();
                } else if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO_B) {
                    startRecordingB();
                }
            } else {
                Toast.makeText(this, "需要录音权限才能使用翻译功能", Toast.LENGTH_SHORT).show();
                // 权限拒绝，释放互斥并恢复按钮
                activeMicSide = SIDE_NONE;
                lastClickedSide = SIDE_NONE;
                btnMicrophoneA.setEnabled(true);
                btnMicrophoneA.setAlpha(1f);
                btnMicrophoneB.setEnabled(true);
                btnMicrophoneB.setAlpha(1f);
            }
        }
    }

    private void stopRecordingB() {
        // 取消静默倒计时
        cancelSilenceTimeout();
        
        translationManagerB.stopTranslation();
    }

    private void stopAllRecording() {
        stopRecordingA();
        stopRecordingB();
    }

    private void showLanguageSelectionDialog(boolean isLanguageA) {
        String code = isLanguageA ? viewModel.getLanguageA().getValue() : viewModel.getLanguageB().getValue();
        String otherSideCode = isLanguageA ? viewModel.getLanguageB().getValue() : viewModel.getLanguageA().getValue();
        LanguageSelectionDialog dialog = new LanguageSelectionDialog(this, code, otherSideCode,!isLanguageA);
        dialog.setOnLanguageSelectedListener(languageName -> {
            String selectedCode;
            
            if (isLanguageA) {
                selectedCode = LanguageUtils.getInstance().getTargetLanguageCode(languageName);
                // 设置A侧语言
                viewModel.setLanguageA(selectedCode);
                tvLanguageA.setText(languageName);
                
                // 检查是否与B侧相同
                String languageB = viewModel.getLanguageB().getValue();
                if (selectedCode.equals(languageB)) {
                    // 如果相同，自动切换B侧为不同的语言
                    String newCodeB = getDifferentLanguageCode(selectedCode,isLanguageA);
                    viewModel.setLanguageB(newCodeB);
                    tvLanguageB.setText(LanguageUtils.getInstance().getSourceLanguagesName(newCodeB));
                }
                
                // 恢复颜色和可点击状态
                ZUtils.setTextColor(this, tvLanguageA, R.color.primary_blue);
                ZUtils.setTextColor(this, tvLanguageB, R.color.primary_blue);
                ll_language_a.setClickable(true);
                ll_language_b.setClickable(true);
            } else {
                selectedCode = LanguageUtils.getInstance().getSourceLanguageCode(languageName);
                // 设置B侧语言
                viewModel.setLanguageB(selectedCode);
                tvLanguageB.setText(languageName);
                
                // 检查是否与A侧相同
                String languageA = viewModel.getLanguageA().getValue();
                if (selectedCode.equals(languageA)) {
                    // 如果相同，自动切换A侧为不同的语言
                    String newCodeA = getDifferentLanguageCode(selectedCode,isLanguageA);
                    viewModel.setLanguageA(newCodeA);
                    tvLanguageA.setText(LanguageUtils.getInstance().getSourceLanguageName(newCodeA));
                }
                
                // 恢复颜色和可点击状态
                ZUtils.setTextColor(this, tvLanguageA, R.color.primary_blue);
                ZUtils.setTextColor(this, tvLanguageB, R.color.primary_blue);
                ll_language_a.setClickable(true);
                ll_language_b.setClickable(true);
            }
        });
        dialog.show();
    }
    
    /**
     * 获取一个与当前语言不同的语言代码
     */
    private String getDifferentLanguageCode(String currentCode,boolean isLanguageA) {

        List<String> allLanguageCodes ;
        if(isLanguageA){
            allLanguageCodes = LanguageUtils.getInstance().getTargetLanguageCodes();
        }else{
            allLanguageCodes = LanguageUtils.getInstance().getSourceLanguageCodes();
        }
        // 查找第一个与当前语言不同的语言
        for (String code : allLanguageCodes) {
            if (!code.equals(currentCode)) {
                return code;
            }
        }
        
        // 如果所有语言都相同（理论上不会发生），返回英语作为默认
        return "en";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 取消静默倒计时
        cancelSilenceTimeout();
        
        // 取消注册广播接收器
        LocalBroadcastManager.getInstance(this).unregisterReceiver(translationReceiver);

        // 释放翻译管理器资源
        if (translationManagerA != null) {
            translationManagerA.release();
        }
        if (translationManagerB != null) {
            translationManagerB.release();
        }

    }

    // 导出本地记录为 JSON 并通过系统分享
    private void exportRecordsToJson() {
        try {
            List<DialogRecord> all = getAllDialogRecords();
            if (all == null || all.isEmpty()) {
                Toast.makeText(this, "暂无记录可导出", Toast.LENGTH_SHORT).show();
                return;
            }
            String json = new Gson().toJson(all);
            File dir = new File(getCacheDir(), "exports");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, "dialog_records_" + System.currentTimeMillis() + ".json");
            try (FileWriter fw = new FileWriter(out)) {
                fw.write(json);
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", out);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/json");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "导出对话记录"));
        } catch (Exception e) {
            Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 返回所有对话记录
    private List<DialogRecord> getAllDialogRecords() {
        return records;
    }

    // 获取当前语言代码（A/B）
    private String getCurrentSourceLangForA() {
        String lang = viewModel.getLanguageA().getValue();
        return lang != null ? lang : "";
    }

    private String getCurrentTargetLangForA() {
        String lang = viewModel.getLanguageB().getValue();
        return lang != null ? lang : "";
    }

    private String getCurrentSourceLangForB() {
        String lang = viewModel.getLanguageB().getValue();
        return lang != null ? lang : "";
    }

    // 录音/WS连接开始时，立即显示三个点的加载占位
    // 只在说话人的容器中显示一个三点动画
    private void showLoadingDotsOnStart(boolean forSideA) {
        // 创建中间态消息（带三点）
        DialogMessage loadingMsgOriginal = new DialogMessage(
            "mid_original", 
            "", 
            DialogMessage.TYPE_ORIGINAL, 
            forSideA ? "A方" : "B方", 
            true  // 显示三点动画
        );
        
        DialogMessage loadingMsgTranslation = new DialogMessage(
            "mid_translation", 
            "", 
            DialogMessage.TYPE_TRANSLATION, 
            forSideA ? "A方" : "B方", 
            true  // 显示三点动画
        );
        
        // 在两个容器中都添加中间态消息
        if (forSideA) {
            currentMidMessageA = loadingMsgOriginal;
            currentMidMessageB = loadingMsgTranslation;
            
            // 添加到列表位置0
            messagesA.add(0, loadingMsgOriginal);
            messagesB.add(0, loadingMsgTranslation);
            
            if (adapterA != null) {
                adapterA.notifyItemInserted(0);
            }
            if (adapterB != null) {
                adapterB.notifyItemInserted(0);
            }
        } else {
            currentMidMessageB = loadingMsgOriginal;
            currentMidMessageA = loadingMsgTranslation;
            
            // 添加到列表位置0
            messagesB.add(0, loadingMsgOriginal);
            messagesA.add(0, loadingMsgTranslation);
            
            if (adapterB != null) {
                adapterB.notifyItemInserted(0);
            }
            if (adapterA != null) {
                adapterA.notifyItemInserted(0);
            }
        }
    }

    // 停止录音/WS断开或错误时，隐藏三点
    private void hideAllLoadingDots() {
        boolean hasChanges = false;
        
        if (messagesA != null) {
            for (DialogMessage m : messagesA) {
                if (m.isRecognizing()) {
                    m.setRecognizing(false);
                    hasChanges = true;
                }
            }
        }
        
        if (messagesB != null) {
            for (DialogMessage m : messagesB) {
                if (m.isRecognizing()) {
                    m.setRecognizing(false);
                    hasChanges = true;
                }
            }
        }
        
        // 只在真正有变化时才通知更新，并且只更新第一项
        if (hasChanges) {
            if (adapterA != null && !messagesA.isEmpty()) {
                adapterA.notifyItemChanged(0);
            }
            if (adapterB != null && !messagesB.isEmpty()) {
                adapterB.notifyItemChanged(0);
            }
        }
    }

    /**
     * 确保每个列表中只有第一条消息显示三点动画
     * 其他历史消息不显示三点
     */
    private void ensureOnlyFirstItemShowsDots() {
        boolean needsUpdate = false;
        
        // 处理A侧消息列表
        if (messagesA != null && messagesA.size() > 1) {
            for (int i = 1; i < messagesA.size(); i++) {
                if (messagesA.get(i).isRecognizing()) {
                    messagesA.get(i).setRecognizing(false);
                    needsUpdate = true;
                }
            }
        }
        
        // 处理B侧消息列表
        if (messagesB != null && messagesB.size() > 1) {
            for (int i = 1; i < messagesB.size(); i++) {
                if (messagesB.get(i).isRecognizing()) {
                    messagesB.get(i).setRecognizing(false);
                    needsUpdate = true;
                }
            }
        }
        
        // 如果有历史消息被修改，通知适配器更新
        if (needsUpdate) {
            if (adapterA != null && messagesA.size() > 1) {
                adapterA.notifyItemRangeChanged(1, messagesA.size() - 1);
            }
            if (adapterB != null && messagesB.size() > 1) {
                adapterB.notifyItemRangeChanged(1, messagesB.size() - 1);
            }
        }
    }

    // 根据当前播放状态更新按钮显示与图标（播放时显示动画；未播放显示喇叭图标）
    private void updatePlayButtonsVisibility() {
        if (ivPlayA == null || ivPlayB == null || ivPlayA1 == null || ivPlayB1 == null) return;

        // A侧
        if (currentPlaySide == SIDE_A) {
            // 播放中时不设置图标，由 startPlayAnimation 处理动画
            // 按钮保持当前状态（动画显示，喇叭隐藏）
        } else {
            // 非播放状态，重置：隐藏动画，显示喇叭
            resetPlayButton(ivPlayA, ivPlayA1);
        }

        // B侧
        if (currentPlaySide == SIDE_B) {
            // 播放中时不设置图标，由 startPlayAnimation 处理动画
            // 按钮保持当前状态（动画显示，喇叭隐藏）
        } else {
            // 非播放状态，重置：隐藏动画，显示喇叭
            resetPlayButton(ivPlayB, ivPlayB1);
        }
    }

    private String getCurrentTargetLangForB() {
        String lang = viewModel.getLanguageA().getValue();
        return lang != null ? lang : "";
    }

    // 重新朗读最近一次译文
    private void replayLatestForSide(boolean isSideA) {
        int clickedSide = isSideA ? SIDE_A : SIDE_B;
        
        // 如果点击的是正在播放的侧，则停止播放
        if (currentPlaySide == clickedSide) {
            TTSManager.Companion.getInstance().stop();
            stopPlayAnimation();
            currentPlaySide = SIDE_NONE;
            updatePlayButtonsVisibility();
            return;
        }
        
        // 否则开始播放整个消息列表
        String textToSpeak = null;
        if (isSideA) {
            // 获取A侧所有消息的文本
            textToSpeak = getAllMessagesText(messagesA);
        } else {
            // 获取B侧所有消息的文本
            textToSpeak = getAllMessagesText(messagesB);
        }
        if (textToSpeak == null || textToSpeak.trim().isEmpty()) {
            Toast.makeText(this, "暂无可播放的文本", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 停止之前的播放
        if (currentPlaySide != SIDE_NONE) {
            TTSManager.Companion.getInstance().stop();
            stopPlayAnimation();
        }
        
        // 设置当前播放侧
        currentPlaySide = clickedSide;
        
        // 开始播放（playTranslationTts 内部会调用 startPlayAnimation）
        playTranslationTts(textToSpeak);
    }
    
    /**
     * 获取消息列表中所有消息的文本
     * 按照从旧到新的顺序拼接（正常布局，位置0是最旧的，最后一条是最新的）
     */
    private String getAllMessagesText(List<DialogMessage> messageList) {
        if (messageList == null || messageList.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // 从列表开头开始遍历（最旧的消息）到末尾（最新的消息）
        for (int i = 0; i < messageList.size(); i++) {
            DialogMessage message = messageList.get(i);
            if (message != null && message.getText() != null && !message.getText().trim().isEmpty()) {
                // 跳过正在识别中的消息（带三点的）
                if (!message.isRecognizing()) {
                    if (sb.length() > 0) {
                        sb.append(" "); // 消息之间用空格分隔
                    }
                    sb.append(message.getText());
                }
            }
        }
        
        return sb.toString();
    }

    // 获取某个RecyclerView顶部可见项的文本（若无则回退到内存列表首项）
    private String getTopVisibleText(RecyclerView rv, List<DialogMessage> list) {
        if (rv != null && rv.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
            int pos = lm.findFirstVisibleItemPosition();
            if (pos != RecyclerView.NO_POSITION && pos < list.size()) {
                return list.get(pos).getText();
            }
        }
        return (list != null && !list.isEmpty()) ? list.get(0).getText() : null;
    }


    /**
     * 获取最新一条可见文本（若为空则返回空字符串）。
     * 遍历列表，从后往前找第一条非recognizing且有文本的消息。
     */
    private String getLatestTextFromList(List<DialogMessage> list) {
        if (list == null || list.isEmpty()) return "";
        for (int i = list.size() - 1; i >= 0; i--) {
            DialogMessage m = list.get(i);
            if (!m.isRecognizing() && m.getText() != null && !m.getText().isEmpty()) {
                return m.getText();
            }
        }
        return "";
    }

    /**
     * 进入对话模式页面时，调用同传记录接口
     * curl 示例: POST https://china-mobile.jmkjsh.com/app-api/lt/ai/translation/add
     * header: token
     * body: {"name":"yyyyMMdd日HH:mm同传", "type":2}
     */


    /**
     * 切换上半区显示框的翻转状态
     */
    private void toggleTopFlip() {
        if (topCardContainer == null) return;
        isTopFlipped = !isTopFlipped;
        float target = isTopFlipped ? 180f : 0f;
        topCardContainer.animate().rotation(target).setDuration(200).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    private String formatNow() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 获取当前TTS播放的目标语言
     * 根据当前点击的播放按钮确定目标语言
     */
    private String getCurrentTargetLanguageForTTS() {
        if (currentPlaySide == SIDE_A) {
            // 点击A侧播放按钮，播放A侧显示的文本，目标语言是A侧语言
            String langA = viewModel.getLanguageA().getValue();
            return langA != null ? LanguageUtils.getInstance().getSourceLanguageName(langA) : null;
        } else if (currentPlaySide == SIDE_B) {
            // 点击B侧播放按钮，播放B侧显示的文本，目标语言是B侧语言
            String langB = viewModel.getLanguageB().getValue();
            return langB != null ? LanguageUtils.getInstance().getTargetLanguagesName(langB) : null;
        } else if (pendingPlaySide == SIDE_A) {
            // 待播放A侧，目标语言是A侧语言
            String langA = viewModel.getLanguageA().getValue();
            return langA != null ? LanguageUtils.getInstance().getSourceLanguageName(langA) : null;
        } else if (pendingPlaySide == SIDE_B) {
            // 待播放B侧，目标语言是B侧语言
            String langB = viewModel.getLanguageB().getValue();
            return langB != null ? LanguageUtils.getInstance().getTargetLanguagesName(langB) : null;
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ZUtils.setStatusBarWhite(this);

    }
    
    /**
     * 启动静默倒计时
     * 收到FIN消息后，如果3秒内没有新的MID消息，自动断开录音
     */
    private void startSilenceTimeout() {
        // 先取消之前的倒计时
        cancelSilenceTimeout();
        
        // 创建新的倒计时任务
        silenceTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Timber.tag("DialogModeActivity").d("Silence timeout reached, auto stopping recording");
                
                // 自动停止录音
                if (isRecordingA) {
                    stopRecordingA();
                } else if (isRecordingB) {
                    stopRecordingB();
                }
            }
        };
        
        // 启动3秒倒计时
        silenceHandler.postDelayed(silenceTimeoutRunnable, SILENCE_TIMEOUT_MS);
        
        Timber.tag("DialogModeActivity").d("Started silence timeout: " + SILENCE_TIMEOUT_MS + "ms");
    }
    
    /**
     * 取消静默倒计时
     * 收到新的MID消息时调用
     */
    private void cancelSilenceTimeout() {
        if (silenceTimeoutRunnable != null) {
            silenceHandler.removeCallbacks(silenceTimeoutRunnable);
            silenceTimeoutRunnable = null;
            Timber.tag("DialogModeActivity").d("Cancelled silence timeout");
        }
    }
    
    /**
     * 录音过程中禁用控件
     * 禁用：语言选择按钮、朗读按钮
     */
    private void disableControlsDuringRecording() {
        // 禁用语言选择按钮
        if (ll_language_a != null) {
            ll_language_a.setEnabled(false);
            ll_language_a.setAlpha(0.5f);
        }
        if (ll_language_b != null) {
            ll_language_b.setEnabled(false);
            ll_language_b.setAlpha(0.5f);
        }
        
        // 禁用朗读按钮
        if (ivPlayA != null) {
            ivPlayA.setEnabled(false);
            ivPlayA.setAlpha(0.5f);
        }
        if (ivPlayA1 != null) {
            ivPlayA1.setEnabled(false);
            ivPlayA1.setAlpha(0.5f);
        }
        if (ivPlayB != null) {
            ivPlayB.setEnabled(false);
            ivPlayB.setAlpha(0.5f);
        }
        if (ivPlayB1 != null) {
            ivPlayB1.setEnabled(false);
            ivPlayB1.setAlpha(0.5f);
        }
        
        Timber.tag("DialogModeActivity").d("Disabled controls during recording");
    }
    
    /**
     * 录音结束后恢复控件
     * 恢复：语言选择按钮、朗读按钮
     */
    private void enableControlsAfterRecording() {
        // 恢复语言选择按钮
        if (ll_language_a != null) {
            ll_language_a.setEnabled(true);
            ll_language_a.setAlpha(1f);
            // 确保在自动停止后可点击恢复
            ll_language_a.setClickable(true);
        }
        if (ll_language_b != null) {
            ll_language_b.setEnabled(true);
            ll_language_b.setAlpha(1f);
            // 确保在自动停止后可点击恢复
            ll_language_b.setClickable(true);
        }
        
        // 恢复朗读按钮
        if (ivPlayA != null) {
            ivPlayA.setEnabled(true);
            ivPlayA.setAlpha(1f);
        }
        if (ivPlayA1 != null) {
            ivPlayA1.setEnabled(true);
            ivPlayA1.setAlpha(1f);
        }
        if (ivPlayB != null) {
            ivPlayB.setEnabled(true);
            ivPlayB.setAlpha(1f);
        }
        if (ivPlayB1 != null) {
            ivPlayB1.setEnabled(true);
            ivPlayB1.setAlpha(1f);
        }
        
        Timber.tag("DialogModeActivity").d("Enabled controls after recording");
    }
    
    /**
     * 开始播放动画
     * 根据当前播放侧设置对应按钮的动画
     * 播放时：隐藏喇叭图标（ivPlayA1/ivPlayB1），显示动画（ivPlayA/ivPlayB）
     */
    private void startPlayAnimation() {
        final ImageView animButton;  // 动画按钮
        final ImageView volumeButton;  // 喇叭按钮
        
        // 根据当前播放侧获取对应的按钮
        if (currentPlaySide == SIDE_A) {
            animButton = ivPlayA;
            volumeButton = ivPlayA1;
        } else if (currentPlaySide == SIDE_B) {
            animButton = ivPlayB;
            volumeButton = ivPlayB1;
        } else {
            Timber.tag("DialogModeActivity").w("Cannot start animation: currentPlaySide is NONE");
            return;
        }
        
        if (animButton != null && volumeButton != null) {
            // 确保在 UI 线程中执行
            runOnUiThread(() -> {
                // 隐藏喇叭图标
                volumeButton.setVisibility(View.GONE);
                
                // 显示动画按钮
                animButton.setVisibility(View.VISIBLE);
                
                // 设置为动画资源
                animButton.setImageResource(R.drawable.chat_tts_speaking_anim);
                
                // 需要 post 到下一帧才能获取到 AnimationDrawable
                animButton.post(() -> {
                    Drawable drawable = animButton.getDrawable();
                    if (drawable instanceof AnimationDrawable) {
                        AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
                        animationDrawable.start();
                        Timber.tag("DialogModeActivity").d("Started play animation for side: " + currentPlaySide + ", isRunning: " + animationDrawable.isRunning());
                    } else {
                        Timber.tag("DialogModeActivity").w("Drawable is not AnimationDrawable: " + (drawable != null ? drawable.getClass().getName() : "null"));
                    }
                });
            });
        } else {
            Timber.tag("DialogModeActivity").w("Play button or volume button is null for side: " + currentPlaySide);
        }
    }
    
    /**
     * 停止播放动画
     * 重置所有播放按钮：隐藏动画，显示喇叭图标
     */
    private void stopPlayAnimation() {
        // 停止A侧动画
        resetPlayButton(ivPlayA, ivPlayA1);
        
        // 停止B侧动画
        resetPlayButton(ivPlayB, ivPlayB1);
        
        Timber.tag("DialogModeActivity").d("Stopped play animation");
    }
    
    /**
     * 重置单个播放按钮
     * 停止动画，隐藏动画按钮，显示喇叭图标
     */
    private void resetPlayButton(ImageView animButton, ImageView volumeButton) {
        if (animButton != null && volumeButton != null) {
            runOnUiThread(() -> {
                // 停止动画
                Drawable drawable = animButton.getDrawable();
                if (drawable instanceof AnimationDrawable) {
                    ((AnimationDrawable) drawable).stop();
                }
                
                // 隐藏动画按钮
                animButton.setVisibility(View.GONE);
                
                // 显示喇叭图标
                volumeButton.setVisibility(View.VISIBLE);
            });
        }
    }
    
    /**
     * 重置单个播放按钮（兼容旧方法）
     */
    private void resetPlayButton(ImageView playButton) {
        if (playButton != null) {
            // 停止动画
            Drawable drawable = playButton.getDrawable();
            if (drawable instanceof AnimationDrawable) {
                ((AnimationDrawable) drawable).stop();
            }
            
            // 重置为播放图标
            playButton.setImageResource(R.mipmap.chat_play);
        }
    }
}
