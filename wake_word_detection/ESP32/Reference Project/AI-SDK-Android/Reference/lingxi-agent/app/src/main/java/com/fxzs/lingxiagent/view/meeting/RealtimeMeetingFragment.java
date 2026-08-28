package com.fxzs.lingxiagent.view.meeting;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.meeting.dto.LanguageDto;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.viewmodel.meeting.VMMeeting;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

public class RealtimeMeetingFragment extends BaseFragment<VMMeeting> {
    
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    // UI Components
    private EditText etMeetingTitle;
    private TextView tvMeetingTitleDisplay;
    private TextView tvSelectedLanguage;
    private CardView cardLanguageSelection;
    private CardView cardMeetingTitle;
    private LinearLayout layoutStartButton;
    private LinearLayout layoutStopButton;
    private ImageView ivEditTitle;
    
    // Audio recording
    private AudioRecord audioRecord;
    private ExecutorService executorService;
    private boolean isRecording = false;
    private int bufferSize;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_realtime_meeting;
    }
    
    @Override
    protected Class<VMMeeting> getViewModelClass() {
        return VMMeeting.class;
    }
    
    @Override
    protected void initializeViews(View view) {
        // Initialize UI components
        etMeetingTitle = findViewById(R.id.et_meeting_title);
        tvMeetingTitleDisplay = findViewById(R.id.tv_meeting_title_display);
        tvSelectedLanguage = findViewById(R.id.tv_selected_language);
        cardLanguageSelection = findViewById(R.id.frame_language_selection);
        cardMeetingTitle = findViewById(R.id.frame_meeting_title);
        layoutStartButton = findViewById(R.id.layout_start_button);
        layoutStopButton = findViewById(R.id.layout_stop_button);
        ivEditTitle = findViewById(R.id.iv_edit_title);
        
        // Set click listeners
        layoutStartButton.setOnClickListener(v -> startRecording());
        layoutStopButton.setOnClickListener(v -> stopRecording());
        cardLanguageSelection.setOnClickListener(v -> showLanguageSelectionDialog());
        cardMeetingTitle.setOnClickListener(v -> toggleTitleEdit());
        ivEditTitle.setOnClickListener(v -> toggleTitleEdit());
        
        // Initialize audio
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        executorService = Executors.newSingleThreadExecutor();
        
        // Check permissions
        checkAudioPermission();
        
        // Set default meeting title
        setDefaultMeetingTitle();
    }
    
    @Override
    protected void setupDataBinding() {
        // Bind meeting title
        DataBindingUtils.bindEditText(etMeetingTitle, viewModel.getMeetingTitle(), this);
        
        // Sync display text with EditText
        viewModel.getMeetingTitle().observeForever(title -> {
            if (title != null && !title.equals(tvMeetingTitleDisplay.getText().toString())) {
                tvMeetingTitleDisplay.setText(title);
            }
        });
        
        // Bind recording status
        DataBindingUtils.bindEnabled(layoutStartButton, viewModel.getStartButtonEnabled(), this);
        viewModel.getIsRecording().observeForever(isRecording -> {
            if (isRecording) {
                findViewById(R.id.frame_start_button).setVisibility(View.GONE);
                findViewById(R.id.card_stop_button).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.frame_start_button).setVisibility(View.VISIBLE);
                findViewById(R.id.card_stop_button).setVisibility(View.GONE);
            }
        });

//        DataBindingUtils.requestNextFocus(etMeetingTitle,cardLanguageSelection);

        etMeetingTitle.setOnKeyListener((v, keyCode, event) -> {

            if (event.getAction() == KeyEvent.ACTION_DOWN) {

                if (keyCode == KeyEvent.KEYCODE_ENTER
                        || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    startRecording();

                    return true;
                }

            }
            return false;
        });
    }
    
    @Override
    protected void setupObservers() {
        // Observe languages
        viewModel.getRealTimeLanguages().observe(getViewLifecycleOwner(), languages -> {
            if (languages != null && !languages.isEmpty()) {
                setupLanguageDisplay(languages);
            } else {
                new Handler(Looper.getMainLooper()).postDelayed(this::setupEmptyLanguageDisplay, 300);
            }
        });

        // Observe selected language change
        viewModel.getSelectedLanguage().observeForever(languageCode -> {
            updateLanguageDisplay(languageCode);
        });
        
        // Observe meeting creation
        viewModel.getCurrentMeeting().observe(getViewLifecycleOwner(), meeting -> {
            if (meeting != null && viewModel.getIsRecording().get()) {
                startAudioRecording();
            }
        });
    }
    
    private void setDefaultMeetingTitle() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd日HH:mm分的会议", Locale.getDefault());
        String defaultTitle = sdf.format(new Date());
        viewModel.getMeetingTitle().set(defaultTitle);
    }
    
    private List<LanguageDto> availableLanguages = new ArrayList<>();
    
    private void setupLanguageDisplay(List<LanguageDto> languages) {
        this.availableLanguages = languages;
        cardLanguageSelection.setEnabled(true);
        cardLanguageSelection.setClickable(true);
        
        Timber.tag("RealtimeMeetingFragment").d( "设置语言列表，数量: " + languages.size());
        
        // Look for Chinese language variants
        boolean foundChinese = false;
        for (LanguageDto language : languages) {
            Timber.tag("RealtimeMeetingFragment").d( "语言: " + language.getName() + " (" + language.getCode() + ")");
            
            // Check for various Chinese language codes
            if ("zh-CN".equals(language.getCode()) || 
                "16k_zh".equals(language.getCode()) || 
                "16k_zh_large".equals(language.getCode()) ||
                language.getName().contains("中文") ||
                language.getName().contains("普通话")) {
                viewModel.getSelectedLanguage().set(language.getCode());
                tvSelectedLanguage.setText(language.getName());
                foundChinese = true;
                Timber.tag("RealtimeMeetingFragment").d( "默认选择中文: " + language.getName());
                break;
            }
        }
        
        // If Chinese not found, select first available
        if (!foundChinese && !languages.isEmpty()) {
            LanguageDto firstLanguage = languages.get(0);
            viewModel.getSelectedLanguage().set(firstLanguage.getCode());
            tvSelectedLanguage.setText(firstLanguage.getName());
            Timber.tag("RealtimeMeetingFragment").d( "未找到中文，选择第一个语言: " + firstLanguage.getName());
        }
    }
    
    private void setupEmptyLanguageDisplay() {
        Timber.tag("RealtimeMeetingFragment").e( "语言列表为空，使用默认语言");
        
        // Provide default language options as fallback
        availableLanguages.clear();
        
        // Add default Chinese language
        LanguageDto defaultChinese = new LanguageDto();
        defaultChinese.setCode("16k_zh_large");
        defaultChinese.setName("中文普通话");
        availableLanguages.add(defaultChinese);
        
        // Add English as alternative
        LanguageDto defaultEnglish = new LanguageDto();
        defaultEnglish.setCode("16k_en");
        defaultEnglish.setName("英语");
        availableLanguages.add(defaultEnglish);
        
        // Set Chinese as default
        viewModel.getSelectedLanguage().set(defaultChinese.getCode());
        tvSelectedLanguage.setText(defaultChinese.getName());
        cardLanguageSelection.setEnabled(true);
        cardLanguageSelection.setClickable(true);
        
        // showToast("网络连接失败，使用默认语言选项");
    }
    
    private void updateLanguageDisplay(String languageCode) {
        for (LanguageDto language : availableLanguages) {
            if (language.getCode().equals(languageCode)) {
                tvSelectedLanguage.setText(language.getName());
                break;
            }
        }
    }
    
    private void showLanguageSelectionDialog() {
        Timber.tag("RealtimeMeetingFragment").d( "点击语言选择，当前语言数量: " + availableLanguages.size());

        if (availableLanguages.isEmpty()) {
            showToast("语言列表加载失败，请检查网络连接");
            // 尝试重新加载语言列表
            viewModel.loadLanguages();
            return;
        }

        String currentLanguageCode = viewModel.getSelectedLanguage().get();
        LanguageDto currenLanguageDto = viewModel.getSelectedLanguageDto().get();

        // 使用自定义语言选择弹窗
        LanguageSelectionDialog dialog = new LanguageSelectionDialog(
            getContext(),
            availableLanguages,
//            currentLanguageCode,
                currenLanguageDto,
            selectedLanguage -> {
                // 用户选择语言的回调
                viewModel.getSelectedLanguage().set(selectedLanguage.getCode());
                viewModel.getSelectedLanguageDto().set(selectedLanguage);
                tvSelectedLanguage.setText(selectedLanguage.getName());
                Timber.tag("RealtimeMeetingFragment").d( "选择语言: " + selectedLanguage.getName() + " (" + selectedLanguage.getCode() + ")");
            }
        );

        dialog.show();
    }
    
    
    private boolean isEditingTitle = false;
    
    private void toggleTitleEdit() {
        if (isEditingTitle) {
            // Switch to display mode
            String currentText = etMeetingTitle.getText().toString().trim();
            if (!currentText.isEmpty()) {
                viewModel.getMeetingTitle().set(currentText);
                tvMeetingTitleDisplay.setText(currentText);
            }
            
            tvMeetingTitleDisplay.setVisibility(View.VISIBLE);
            etMeetingTitle.setVisibility(View.GONE);
            isEditingTitle = false;
            
            // Hide keyboard
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etMeetingTitle.getWindowToken(), 0);
            }
        } else {
            // Switch to edit mode
            etMeetingTitle.setText(tvMeetingTitleDisplay.getText().toString());
            tvMeetingTitleDisplay.setVisibility(View.GONE);
            etMeetingTitle.setVisibility(View.VISIBLE);
            etMeetingTitle.requestFocus();
            etMeetingTitle.setSelection(etMeetingTitle.getText().length());
            isEditingTitle = true;
            
            // Show keyboard
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etMeetingTitle, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }
    
    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            AppPermissionRequestManager.requestAudioPermission(getActivity(), PERMISSION_REQUEST_RECORD_AUDIO,AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_MEETING);
        }
    }
    
    private void startRecording() {
        // 添加空指针检查
        if (getActivity() == null || getContext() == null) {
            Timber.tag("RealtimeMeetingFragment").e( "Activity或Context为空，无法启动会议");
            return;
        }
        
        if (viewModel == null) {
            Timber.tag("RealtimeMeetingFragment").e( "ViewModel为空，无法启动会议");
            showToast("初始化失败，请重试");
            return;
        }
        
        String meetingTitle = viewModel.getMeetingTitle().get();
        String selectedLanguageCode = viewModel.getSelectedLanguage().get();
        
        Timber.tag("RealtimeMeetingFragment").d( "开始录音 - 会议标题: " + meetingTitle);
        Timber.tag("RealtimeMeetingFragment").d( "选择的语言代码: " + selectedLanguageCode);
        
        // Validate inputs
        if (meetingTitle == null || meetingTitle.trim().isEmpty()) {
            showToast("请输入会议名称");
            return;
        }
        
        if (selectedLanguageCode == null || selectedLanguageCode.isEmpty()) {
            showToast("请选择语音识别语言");
            return;
        }
        
        try {
            // Launch RealtimeMeetingActivity
            Intent intent = new Intent(getActivity(), RealtimeMeetingActivity.class);
            intent.putExtra("meeting_title", meetingTitle);
            intent.putExtra("selected_language", selectedLanguageCode);
            startActivity(intent);
        } catch (Exception e) {
            Timber.tag("RealtimeMeetingFragment").e( "启动会议Activity失败: " + e.getMessage(), e);
            showToast("启动会议失败，请重试");
        }
    }
    
    private void stopRecording() {
        stopAudioRecording();
        viewModel.toggleRecording();
    }
    
    private void startAudioRecording() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);
        
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            showToast("音频录制初始化失败");
            return;
        }
        
        audioRecord.startRecording();
        isRecording = true;
        
        executorService.execute(() -> {
            byte[] buffer = new byte[bufferSize];
            
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    byte[] audioData = new byte[read];
                    System.arraycopy(buffer, 0, audioData, 0, read);
                    viewModel.processAudioData(audioData);
                }
            }
        });
    }
    
    private void stopAudioRecording() {
        isRecording = false;
        
        if (audioRecord != null) {
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
                audioRecord.release();
            }
            audioRecord = null;
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // 切换界面时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAudioRecording();
        if (executorService != null) {
            executorService.shutdown();
        }
        
        // 确保销毁时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }
}