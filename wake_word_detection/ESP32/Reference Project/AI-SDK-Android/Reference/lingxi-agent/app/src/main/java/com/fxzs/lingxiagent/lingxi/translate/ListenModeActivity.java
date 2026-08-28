package com.fxzs.lingxiagent.lingxi.translate;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.translate.adapter.TranslationItemAdapter;
import com.fxzs.lingxiagent.lingxi.translate.api.TranslationRecordApiService;
import com.fxzs.lingxiagent.lingxi.translate.model.LanguageOption;
import com.fxzs.lingxiagent.lingxi.translate.model.TranslationItem;
import com.fxzs.lingxiagent.lingxi.translate.ui.LanguageDropdownPopup;
import com.fxzs.lingxiagent.lingxi.translate.util.LanguageUtils;
import com.fxzs.lingxiagent.network.RetrofitClient;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.DocumentHelper;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.WakeUpPermissionHelper;
import com.fxzs.lingxiagent.util.WordExportUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.LoadingProgressDialog;
import com.fxzs.lingxiagent.viewmodel.translate.VMListenMode;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * 聆听模式页面 - 单向翻译
 */
public class ListenModeActivity extends BaseActivity<VMListenMode> {

    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1001;
    private ImageView ivBack;
    private ImageView ivSettings;
    private TextView tvSourceLanguage;
    private TextView tvTargetLanguage;
    private View topBar;
    private View languageBar;

    private RecyclerView rvResults;
    private View btnMicrophone;
//    private ImageView ivMicMask;
    private ImageView iv_pause_mask;
    private ImageView ivMic;
    private TextView tvHint;
    private ImageView btnPause;  // 播放/暂停按钮
    private View btn_pause_container;  // 播放/暂停按钮
    private LanguageDropdownPopup popupSource;
    private LanguageDropdownPopup popupTarget;


    private TranslationItemAdapter adapter;
    private WebSocketTranslationManager translationManager;
    private boolean isRecording = false;
    private boolean isPaused = false;  // 是否处于暂停状态
    private boolean wakeupPausedForListenMode = false;

    private ObjectAnimator maskAnimator;

    private BroadcastReceiver translationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.fxzs.lingxiagent.KEY_MID_RESULT".equals(action)) {
                String midResult = intent.getStringExtra("mid_result");
                if (!TextUtils.isEmpty(midResult)) {
                    adapter.updateMidResult(midResult);
                }
            } else if ("com.fxzs.lingxiagent.CACHE_LIST_UPDATE".equals(action)) {
                // 更新缓存列表显示
                updateCacheListDisplay();
            } else if ("com.fxzs.lingxiagent.complete".equals(action)) {
                stopRecording();
            }
        }
    };
    private View ll_source_language;
    private View ll_target_language;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.fxzs.lingxiagent.KEY_MID_RESULT");
        filter.addAction("com.fxzs.lingxiagent.CACHE_LIST_UPDATE");
        filter.addAction("com.fxzs.lingxiagent.complete");
        LocalBroadcastManager.getInstance(this).registerReceiver(translationReceiver, filter);

        // 不在进入页面时创建会话，改为在点击麦克风时创建
        // try { viewModel.addListenTranslationRecord(); } catch (Exception ignore) {}

        // 初始化WebSocket翻译管理器
        translationManager = new WebSocketTranslationManager(this, new WebSocketTranslationManager.TranslationCallback() {
            @Override
            public void onTranslationStarted() {
                runOnUiThread(() -> {
                    isRecording = true;
                    isPaused = false;
                    btnMicrophone.setSelected(true);
//                    if (ivMicMask != null) {
//                        ivMicMask.setVisibility(View.VISIBLE);
//                        ivMicMask.setAlpha(0.2f);
                        iv_pause_mask.setVisibility(View.VISIBLE);
                        iv_pause_mask.setAlpha(0.2f);
                        if (maskAnimator != null) maskAnimator.start();
//                    }
                    // 开始录音时显示停止图标
                    if (ivMic != null) {
                        ivMic.setImageResource(R.drawable.ic_listen_stop);
                    }
                    // 显示暂停按钮
                    if (btnPause != null) {
                        btnPause.setVisibility(View.VISIBLE);
                        btn_pause_container.setVisibility(View.VISIBLE);
                        btnPause.setImageResource(R.drawable.ic_listen_pause);
                    }
                    if (adapter != null) adapter.setMicActive(true); // 开启三点
                    tvHint.setText("正在录音中，请说话...");
                    tvHint.setVisibility(View.GONE);

                    // 录音开始时隐藏导航栏与语言栏，内容区自动占满
                    if (topBar != null) topBar.setVisibility(View.GONE);
                    if (languageBar != null) languageBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onTranslationStopped() {
                runOnUiThread(() -> {
                    // 只有在非暂停状态下才完全停止
                    if (!isPaused) {
                        isRecording = false;
                        btnMicrophone.setSelected(false);
//                        if (ivMicMask != null) {
                            if (maskAnimator != null) maskAnimator.cancel();
//                            ivMicMask.setVisibility(View.GONE);
                            iv_pause_mask.setVisibility(View.GONE);
//                        }
                        // 停止录音时显示开始图标
                        if (ivMic != null) {
                            ivMic.setImageResource(R.drawable.ic_listen_mic);
                        }
                        // 隐藏暂停按钮
                        if (btnPause != null) {
                            btnPause.setVisibility(View.GONE);
                            btn_pause_container.setVisibility(View.GONE);
                        }
                        if (adapter != null) adapter.setMicActive(false); // 关闭三点
                        tvHint.setText("点击麦克风按钮即可开始翻译。");

                        // 恢复显示导航栏与语言栏
                        if (topBar != null) topBar.setVisibility(View.VISIBLE);
                        if (languageBar != null) languageBar.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isRecording = false;
                    btnMicrophone.setSelected(false);
                    // 出错时也恢复开始图标
                    if (ivMic != null) {
                        ivMic.setImageResource(R.drawable.ic_listen_mic);
                    }
                    tvHint.setText("翻译出错: " + error);
                    if (topBar != null) topBar.setVisibility(View.VISIBLE);
                    if (languageBar != null) languageBar.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_listen_mode;
    }

    @Override
    protected Class<VMListenMode> getViewModelClass() {
        return VMListenMode.class;
    }

    @Override
    protected void setupDataBinding() {
        // 暂不需要数据绑定
    }

    @Override
    protected void initializeViews() {
        ivBack = findViewById(R.id.iv_back);
        ivSettings = findViewById(R.id.iv_settings);
        tvSourceLanguage = findViewById(R.id.tv_source_language);
        ll_source_language = findViewById(R.id.ll_source_language);
        tvTargetLanguage = findViewById(R.id.tv_target_language);
        ll_target_language = findViewById(R.id.ll_target_language);
        topBar = findViewById(R.id.top_bar);
        languageBar = findViewById(R.id.language_bar);
        rvResults = findViewById(R.id.rv_results);
        btnMicrophone = findViewById(R.id.btn_microphone);
//        ivMicMask = findViewById(R.id.iv_mic_mask);
        iv_pause_mask = findViewById(R.id.iv_pause_mask);
        ivMic = findViewById(R.id.iv_mic);
        tvHint = findViewById(R.id.tv_hint);
        btnPause = findViewById(R.id.btn_pause);
        btn_pause_container = findViewById(R.id.btn_pause_container);

        // 录音时的呼吸灯动画
//        maskAnimator = ObjectAnimator.ofFloat(ivMicMask, View.ALPHA, 0.2f, 0.85f, 0.2f);
        maskAnimator = ObjectAnimator.ofFloat(iv_pause_mask, View.ALPHA, 0.2f, 0.85f, 0.2f);
        maskAnimator.setDuration(1200);
        maskAnimator.setRepeatCount(ValueAnimator.INFINITE);
        maskAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        // 设置RecyclerView
        adapter = new TranslationItemAdapter(new ArrayList<>());
        // 首次进入默认不显示底部三个点
        adapter.setShowMidResultItem(false);
        
        // 设置播放按钮点击监听器
        adapter.setOnPlayClickListener((item, position) -> {
            // 播放译文
            String translatedText = item.getTargetText();
            if (translatedText != null && !translatedText.isEmpty()) {
                TTSManager.Companion.getInstance().textForceToAudio(translatedText);
                Toast.makeText(this, "正在播放译文", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "没有可播放的内容", Toast.LENGTH_SHORT).show();
            }
        });

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);

        // 点击事件
        ivBack.setOnClickListener(v -> {
            if (isRecording) {
                showStopRecordingConfirmDialog();
            } else {
                finish();
            }
        });

        ivSettings.setOnClickListener(v -> showExportMenu(ivSettings));

        ll_source_language.setOnClickListener(v -> showLanguageSelectionDialog(true));
        ll_target_language.setOnClickListener(v -> showLanguageSelectionDialog(false));

        btnMicrophone.setOnClickListener(v -> {
            if (isRecording || isPaused) {
                // 录音中或暂停中，点击麦克风按钮表示结束并保存
                showStopRecordingConfirmDialog();
            } else {
                // 未录音，点击麦克风按钮开始录音
                Timber.tag("ListenModeActivity").d("用户点击麦克风开启录音，准备创建新会话");
                startRecording();
            }
        });
        
        // 播放/暂停按钮点击事件
        btnPause.setOnClickListener(v -> {
            if (isPaused) {
                // 当前是暂停状态，点击恢复录音
                resumeRecording();
            } else if (isRecording) {
                // 当前正在录音，点击暂停
                pauseRecording();
            }
        });
        btn_pause_container.setOnClickListener(v -> {
            if (isPaused) {
                // 当前是暂停状态，点击恢复录音
                resumeRecording();
            } else if (isRecording) {
                // 当前正在录音，点击暂停
                pauseRecording();
            }
        });
    }

    @Override
    protected void setupObservers() {
        // 观察源语言
        viewModel.getSourceLanguage().observe(this, language -> {
            if (language != null) {
                tvSourceLanguage.setText(LanguageUtils.getInstance().getSourceLanguageName(language));
                // 移除语言限制，支持所有语言互译
            }
        });

        // 观察目标语言
        viewModel.getTargetLanguage().observe(this, language -> {
            if (language != null) {
                tvTargetLanguage.setText(LanguageUtils.getInstance().getTargetLanguageName(language));
            }
        });
    }

    /**
     * 更新缓存列表显示
     */
    private void updateCacheListDisplay() {
        if (translationManager != null) {
            List<TranslationItem> cacheList = translationManager.getLocalCacheList();
            adapter.updateItems(cacheList);
            if (!cacheList.isEmpty()) {
                rvResults.smoothScrollToPosition(cacheList.size() - 1);
            }
        }
    }

    private LoadingProgressDialog loadingDialog;

    @Override
    protected void handleLoadingState(boolean loading) {
        if (loading) {
            if (loadingDialog == null)
                loadingDialog = new LoadingProgressDialog(this);
            loadingDialog.setMessage("保存中...").setCancelable(false).show();
        } else {
            if (loadingDialog != null) loadingDialog.dismiss();
        }
    }

    // 记录本次会话的开始时间，供批量保存使用
    private String sessionStartTime;

    private void startRecording() {
        // 检查录音权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // 权限未授予，请求权限
            AppPermissionRequestManager.requestAudioPermission(this, PERMISSION_REQUEST_RECORD_AUDIO,AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_SI);
            return;
        }

        String sourceLanguage = viewModel.getSourceLanguage().getValue();
        String targetLanguage = viewModel.getTargetLanguage().getValue();

        // 移除语言限制，支持所有语言互译

        if (sourceLanguage != null && targetLanguage != null) {
            // 每次开启麦克风时创建新的会话
            try {
                viewModel.addListenTranslationRecord();
            } catch (Exception ignore) {
            }

            // 清空上一次录音的识别消息列表（后端缓存）
            if (translationManager != null) {
                translationManager.clearLocalCache();
                Timber.tag("ListenModeActivity").d("Cleared local cache before starting new recording");
            }
            
            // 立即清空界面显示的消息列表
            if (adapter != null) {
                adapter.updateItems(new ArrayList<>());
                Timber.tag("ListenModeActivity").d("Cleared UI message list immediately");
            }

            // 进入录音时记录开始时间
            sessionStartTime = formatNow();
            pauseWakeUpForListenRecording();
            translationManager.startTranslation(sourceLanguage, targetLanguage);
        } else {
            tvHint.setText("请先选择源语言和目标语言");
        }
    }


    /**
     * 显示结束录音确认弹窗
     */
    private void showStopRecordingConfirmDialog() {
        new CommonDialog.Builder(this)
                .setTitle("结束同声传译吗？")
                .setMessage("目前正在录音中，确定要结束吗")
                .setConfirmText("结束")
                .setConfirmTextRed(true)
                .setCancelText("取消")
                .setOnClickListener(new CommonDialog.OnDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        ivMic.setImageResource(R.drawable.ic_listen_mic);
                        stopRecording();
                    }

                    @Override
                    public void onCancel() {
                        // 用户取消，不做任何操作
                    }
                })
                .show();
    }

    private void stopRecording() {
        translationManager.stopTranslation();
        resumeWakeUpAfterListenRecording();

        // 停止录音后，批量保存所有消息
        try {
            String endTime = formatNow();
            List<TranslationItem> items = translationManager != null ? translationManager.getLocalCacheList() : new ArrayList<>();
            String sourceLanguage = viewModel.getSourceLanguage().getValue();
            String targetLanguage = viewModel.getTargetLanguage().getValue();

            // 保存当前会话的消息
            viewModel.batchSaveListenMessages(items,
                    sourceLanguage == null ? "" : sourceLanguage,
                    targetLanguage == null ? "" : targetLanguage,
                    sessionStartTime == null ? endTime : sessionStartTime,
                    endTime);

            // 注意：不再清空缓存和界面，保留历史记录供用户查看
            // 用户可以继续录音，新的翻译结果会追加到列表中

        } catch (Exception e) {
            Timber.tag("ListenModeActivity").e("停止录音时保存消息异常: " + e.getMessage());
        }
        
        // 重置状态
        isPaused = false;
        isRecording = false;
        
        // 隐藏暂停按钮
        if (btnPause != null) {
            btnPause.setVisibility(View.GONE);
            btn_pause_container.setVisibility(View.GONE);
        }
        topBar.setVisibility(View.VISIBLE);
        languageBar.setVisibility(View.VISIBLE);
    }
    
    private void pauseWakeUpForListenRecording() {
        if (wakeupPausedForListenMode) {
            return;
        }
        WakeUpPermissionHelper.pauseWakeUpForListenMode(this);
        wakeupPausedForListenMode = true;
        Timber.tag("ListenModeActivity").d("Paused wake service for listen recording");
    }

    private void resumeWakeUpAfterListenRecording() {
        if (!wakeupPausedForListenMode) {
            return;
        }
        WakeUpPermissionHelper.resumeWakeUpAfterListenMode(this);
        wakeupPausedForListenMode = false;
        Timber.tag("ListenModeActivity").d("Resumed wake service after listen recording");
    }

    /**
     * 暂停录音
     * 断开 WebSocket 连接，停止录音，但不保存消息
     */
    private void pauseRecording() {
        if (!isRecording) return;
        
        Timber.tag("ListenModeActivity").d("暂停录音");

        // 更新状态
        isPaused = true;
        isRecording = false;
        // 停止翻译（断开 WebSocket）
        translationManager.stopTranslation();

        
        // 更新 UI
        runOnUiThread(() -> {
            // 更新暂停按钮图标为播放图标，并确保可见
            if (btnPause != null) {
                btnPause.setVisibility(View.VISIBLE);
                btn_pause_container.setVisibility(View.VISIBLE);
                btnPause.setImageResource(R.drawable.ic_listen_mic);
            }
            
            // 更新麦克风按钮为结束图标
            if (ivMic != null) {
                ivMic.setImageResource(R.drawable.ic_listen_stop);
            }
            
            // 停止呼吸灯动画
            if (maskAnimator != null) {
                maskAnimator.cancel();
            }
//            if (ivMicMask != null) {
//                ivMicMask.setVisibility(View.GONE);
                iv_pause_mask.setVisibility(View.GONE);
//            }

            if (adapter != null) adapter.setMicActive(false); // 关闭三点

            // 隐藏提示文字，避免遮挡翻译消息
            tvHint.setVisibility(View.GONE);
        });
    }
    
    /**
     * 恢复录音
     * 重新连接 WebSocket，继续录音
     */
    private void resumeRecording() {
        if (!isPaused) return;
        
        Timber.tag("ListenModeActivity").d("恢复录音");
        
        String sourceLanguage = viewModel.getSourceLanguage().getValue();
        String targetLanguage = viewModel.getTargetLanguage().getValue();
        
        if (sourceLanguage != null && targetLanguage != null) {
            // 重新开始翻译（重新连接 WebSocket）
            translationManager.startTranslation(sourceLanguage, targetLanguage);
            
            // 更新状态
            isPaused = false;
            isRecording = true;
            
            // 更新 UI
            runOnUiThread(() -> {
                // 更新暂停按钮图标为暂停图标
                if (btnPause != null) {
                    btnPause.setImageResource(R.drawable.ic_listen_pause);
                }
                
                // 更新麦克风按钮为录音中图标
                if (ivMic != null) {
                    ivMic.setImageResource(R.drawable.ic_listen_stop);
                }
                
                // 启动呼吸灯动画
//                if (ivMicMask != null) {
//                    ivMicMask.setVisibility(View.VISIBLE);
//                    ivMicMask.setAlpha(0.2f);
                    iv_pause_mask.setVisibility(View.VISIBLE);
                    iv_pause_mask.setAlpha(0.2f);
                    if (maskAnimator != null) {
                        maskAnimator.start();
                    }
//                }
                if (adapter != null) adapter.setMicActive(true); // 开启三点
                
                // 隐藏提示文字，避免遮挡翻译消息
                tvHint.setVisibility(View.GONE);
            });
        }
    }

    private String formatNow() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void showLanguageSelectionDialog(boolean isSourceLanguage) {
        LanguageDropdownPopup popup;
        // 改为无蒙层下拉，锚定在对应按钮下方
        if (isSourceLanguage){
            if(popupTarget != null){ popupTarget.dismiss();}
            if(popupSource == null){
                popupSource = new LanguageDropdownPopup(this);
            }
            popup = popupSource;
        }else{
            if(popupSource != null){ popupSource.dismiss();}
            if(popupTarget == null){
                popupTarget = new LanguageDropdownPopup(this);
            }
            popup = popupTarget;
        }
        popup.setTitle(isSourceLanguage ? "我的语言" :"对方的语言" );

        // 使用 LanguageUtils 获取所有支持的语言
        List<LanguageOption> list = new ArrayList<>();
        List<String> allLanguageNames;
        List<String> allLanguageCodes;
        if(isSourceLanguage){
            allLanguageNames =  LanguageUtils.getInstance().getSourceLanguagesNames();
            allLanguageCodes = LanguageUtils.getInstance().getSourceLanguageCodes();
        }else{
            allLanguageNames =  LanguageUtils.getInstance().getTargetLanguagesNames();
            allLanguageCodes = LanguageUtils.getInstance().getTargetLanguageCodes();
        }
        
        // 显示所有语言，不过滤
        for (int i = 0; i < allLanguageCodes.size(); i++) {
            String code = allLanguageCodes.get(i);
            String name = allLanguageNames.get(i);
            list.add(new LanguageOption(name, code));
        }

        String curCode = isSourceLanguage ? viewModel.getSourceLanguage().getValue() : viewModel.getTargetLanguage().getValue();
        String curName = isSourceLanguage? LanguageUtils.getInstance().getSourceLanguagesName(curCode == null ? "" : curCode) : LanguageUtils.getInstance().getTargetLanguageName(curCode == null ? "" : curCode);

        for (LanguageOption op : list) {
            op.setSelected(op.getName().equals(curName));
        }

        popup.setData(list);

        popup.setOnSelect(option -> {
            // 点击即选中并更新 ViewModel，同时同步显示文本
            String code;

            if(isSourceLanguage){
                code = LanguageUtils.getInstance().getSourceLanguageCode(option.getName());
            }else{
                code = LanguageUtils.getInstance().getTargetLanguageCode(option.getName());
            }

            // 检查是否选择了与另一侧相同的语言
            String otherSideCode = isSourceLanguage ? viewModel.getTargetLanguage().getValue() : viewModel.getSourceLanguage().getValue();
            
            if (code != null && code.equals(otherSideCode)) {
                // 如果选择了相同的语言，自动将另一侧切换为不同的语言
                String newOtherSideCode = getAlternativeLanguage(code, isSourceLanguage);
                
                if (isSourceLanguage) {
                    // 修改了源语言，自动调整目标语言
                    viewModel.setTargetLanguage(newOtherSideCode);
                    tvTargetLanguage.setText(LanguageUtils.getInstance().getTargetLanguageName(newOtherSideCode));
                } else {
                    // 修改了目标语言，自动调整源语言
                    viewModel.setSourceLanguage(newOtherSideCode);
                    tvSourceLanguage.setText(LanguageUtils.getInstance().getSourceLanguagesName(newOtherSideCode));
                }
            }

            // 更新当前选择的语言
            if (isSourceLanguage) {
                viewModel.setSourceLanguage(code);
                tvSourceLanguage.setText(LanguageUtils.getInstance().getSourceLanguagesName(code));
            } else {
                viewModel.setTargetLanguage(code);
                tvTargetLanguage.setText(LanguageUtils.getInstance().getTargetLanguagesName(code));
            }
            popup.dismiss();
        });

        View anchor = isSourceLanguage ? ll_source_language : tvTargetLanguage;
        popup.showBelow(anchor);
    }
    
    /**
     * 获取替代语言
     * 当两个语言相同时，返回一个不同的语言
     * @param currentCode 当前选择的语言代码
     * @param isSourceLanguage 是否是源语言选择
     * @return 替代语言代码
     */
    private String getAlternativeLanguage(String currentCode, boolean isSourceLanguage) {
        // 默认的替代语言对：中文 <-> 英文
        if ("zh".equals(currentCode)) {
            return "en";
        } else if ("en".equals(currentCode)) {
            return "zh";
        }
        
        // 对于其他语言，默认切换到中文
        // 如果当前已经是中文，则切换到英文
        return "zh";
    }

    /**
     * 获取一个与当前语言不同的语言代码
     *
     * @param currentCode 当前语言代码
     * @return 不同的语言代码
     */
    private String getDifferentLanguageCode(String currentCode,boolean isSourceLanguage) {
        // 使用 LanguageUtils 获取所有支持的语言
        List<String> allLanguageCodes ;
        if(isSourceLanguage){
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
        // 停止TTS播放
        TTSManager.Companion.getInstance().stop();
        
        // 取消注册广播接收器
        LocalBroadcastManager.getInstance(this).unregisterReceiver(translationReceiver);

        // 释放翻译管理器资源
        if (translationManager != null) {
            translationManager.release();
        }
        resumeWakeUpAfterListenRecording();
    }

    private void showExportMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, "复制文本");
        // 通过后端接口导出Word（.docx）
        menu.getMenu().add(0, 2, 1, "导出Word");
        // 本地生成RTF（Word可打开）
        menu.getMenu().add(0, 3, 2, "生成RTF");
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    copyAllMessagesToClipboard();
                    return true;
                case 2:
                    exportWordViaApi();
                    return true;
                case 3:
                    exportMessagesAsDoc();
                    return true;
            }
            return false;
        });
        menu.show();
    }

    private String buildExportText() {
        StringBuilder sb = new StringBuilder();
        List<TranslationItem> items =
                translationManager != null ? translationManager.getLocalCacheList() : new ArrayList<>();
        
        Timber.tag("ListenModeActivity").d("开始构建导出文本，共 " + items.size() + " 条记录");
        
        for (int i = 0; i < items.size(); i++) {
            TranslationItem it = items.get(i);
            if (it == null) continue;
            
            String src = it.getSourceText() == null ? "" : it.getSourceText();
            String tgt = it.getTargetText() == null ? "" : it.getTargetText();
            
            // 添加序号和原文
            if (!src.isEmpty()) {
                sb.append("[原文] ").append(src).append("\n\n");
            }
            
            // 添加译文
            if (!tgt.isEmpty()) {
                sb.append("[译文] ").append(tgt).append("\n\n");
            }
            
            // 每组之间添加分隔线（除了最后一组）
            if (i < items.size() - 1 && (!src.isEmpty() || !tgt.isEmpty())) {
                sb.append("---\n\n");
            }
        }
        
        String result = sb.toString();
        Timber.tag("ListenModeActivity").d("导出文本构建完成，长度: " + result.length());
        Timber.tag("ListenModeActivity").d("导出文本预览: " + (result.length() > 200 ? result.substring(0, 200) + "..." : result));
        
        return result;
    }

    private void copyAllMessagesToClipboard() {
        String text = buildExportText();
        try {
            // 优先使用项目通用的复制方法
            ZUtils.copy(this, text);
        } catch (Throwable t) {
            // 兜底使用系统剪贴板
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text", text);
            if (clipboard != null) clipboard.setPrimaryClip(clip);
            showToast("文本已复制");
        }
    }

    private void exportMessagesAsTxt() {
        String text = buildExportText();
        DocumentHelper helper = new DocumentHelper(this);
        helper.generateTextFile(text, "listen_export_" + System.currentTimeMillis(), new DocumentHelper.OnDocumentGeneratedListener() {
            @Override
            public void onSuccess(String filePath) {
                File file = new File(filePath);
                if (!isFinishing()) runOnUiThread(() -> showOpenDocumentDialog(file));
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> showToast("TXT 导出失败: " + errorMessage));
            }
        });
    }

    private void exportMessagesAsDoc() {
        String text = buildExportText();
        WordExportUtil.exportToWord(this,
                "聆听模式记录",
                text,
                new WordExportUtil.ExportCallback() {
                    @Override
                    public void onSuccess(File file) {
                        if (!isFinishing()) {
                            runOnUiThread(() -> showOpenDocumentDialog(file));
                        }
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> showToast("DOC 导出失败: " + error));
                    }
                });
    }

    /**
     * 调用后端接口导出Word（DOCX），根据当前同传会话ID获取下载链接并保存
     */
    private void exportWordViaApi() {
        try {
            // 获取translationId（优先ViewModel，其次SharedPreferences）
            Long idLive = viewModel.getTranslationIdLive().getValue();
            long translationId = (idLive != null ? idLive : 0L);
            if (translationId <= 0) {
                translationId = SharedPreferencesUtil.getTranslationId();
            }

            if (translationId <= 0) {
                showToast("未获取到会话ID，无法导出");
                return;
            }

            // 提示开始
            showToast("正在生成并下载Word文档...");

            TranslationRecordApiService api =
                    RetrofitClient.getInstance()
                            .create(TranslationRecordApiService.class);

            // 按接口定义传入路径参数 id（Long），而非 Map body
            Call<ResponseBody> call = api.exportWordById(translationId);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> showToast("导出失败: 服务返回" + response.code()));
                        return;
                    }
                    try {
                        ResponseBody rb = response.body();
                        String json = rb != null ? rb.string() : null;
                        if (json == null || json.isEmpty()) {
                            runOnUiThread(() -> showToast("导出失败: 响应为空"));
                            return;
                        }
                        JSONObject obj = new JSONObject(json);
                        int code = obj.optInt("code", -1);
                        String dataUrl = obj.optString("data", "");
                        String msg = obj.optString("msg", "");
                        if (code == 0 && dataUrl != null && !dataUrl.isEmpty()) {
                            // 下载DOCX到应用文档目录
                            downloadDocxToAppDir(dataUrl, "聆听模式记录");
                        } else {
                            String err = (msg != null && !msg.isEmpty()) ? msg : "导出失败";
                            runOnUiThread(() -> showToast(err));
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> showToast("导出失败: " + e.getMessage()));
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    runOnUiThread(() -> showToast("导出失败: " + t.getMessage()));
                }
            });
        } catch (Exception e) {
            showToast("导出异常: " + e.getMessage());
        }
    }

    /**
     * 将docx下载到 app 专属Documents/exports目录，完成后弹窗打开
     */
    private void downloadDocxToAppDir(String fileUrl, String title) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream input = null;
            FileOutputStream output = null;
            File outFile = null;
            try {
                URL url = new URL(fileUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> showToast("下载失败: " + responseCode));
                    return;
                }

                // 生成保存路径：/Android/data/<pkg>/files/Documents/exports
                File documentsDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports");
                if (!documentsDir.exists()) {
                    documentsDir.mkdirs();
                }

                String fileName = buildDocxFileName(fileUrl, title);
                outFile = new File(documentsDir, fileName);

                input = connection.getInputStream();
                output = new FileOutputStream(outFile);
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                output.flush();

                File finalFile = outFile;
                runOnUiThread(() -> showOpenDocumentDialog(finalFile));
            } catch (Exception e) {
                runOnUiThread(() -> showToast("下载异常: " + e.getMessage()));
            } finally {
                try { if (input != null) input.close(); } catch (Exception ignore) {}
                try { if (output != null) output.close(); } catch (Exception ignore) {}
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private String buildDocxFileName(String url, String baseTitle) {
        String nameFromUrl = null;
        try {
            int idx = url.lastIndexOf('/');
            if (idx >= 0 && idx < url.length() - 1) {
                nameFromUrl = url.substring(idx + 1);
                int q = nameFromUrl.indexOf('?');
                if (q > 0) nameFromUrl = nameFromUrl.substring(0, q);
            }
        } catch (Exception ignore) {}

        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String safeBase = (baseTitle == null || baseTitle.isEmpty()) ? "listen_export" : baseTitle;
        safeBase = safeBase.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
        String fallback = safeBase + "_" + ts + ".docx";
        if (nameFromUrl != null && nameFromUrl.toLowerCase().endsWith(".docx")) {
            return nameFromUrl;
        }
        return fallback;
    }

    private void showOpenDocumentDialog(File file) {
        new CommonDialog.Builder(this)
                .setTitle("导出成功")
                .setMessage("文档已保存到:\n" + file.getName() + "\n\n是否立即打开？")
                .setConfirmText("打开")
                .setCancelText("稍后")
                .setOnClickListener(new CommonDialog.OnDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        WordExportUtil.openDocument(ListenModeActivity.this, file);
                    }

                    @Override
                    public void onCancel() {
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ZUtils.setStatusBarWhite(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，重新开始录音
                startRecording();
            } else {
                // 权限被拒绝
                ZUtils.showToast("需要录音权限才能使用翻译功能");
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isRecording) {
            showStopRecordingConfirmDialog();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 切换界面时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }
}
