package com.fxzs.lingxiagent.view.meeting;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.view.common.ConfirmDialog;
import com.fxzs.lingxiagent.view.common.LoadingProgressDialog;
import com.fxzs.lingxiagent.viewmodel.meeting.VMMeeting;

import java.io.File;

public class AudioTranscriptionFragment extends BaseFragment<VMMeeting> {
    
    private static final int REQUEST_CODE_SELECT_FILE = 1001;
    
    // UI Components - Step Indicators
    private FrameLayout step1Circle, step2Circle, step3Circle;
    private TextView step1Text, step2Text;
    private ImageView step1Check, step2Check, step3Check;
    private TextView step1Label, step2Label, step3Label;
    
    // UI Components - Content Containers
    private LinearLayout step1Content, step2Content;
    
    // UI Components - Language Selection
    private FrameLayout cardChineseMandarin;
    private FrameLayout cardMainlandDialects;
    private FrameLayout cardEnglish;
    private FrameLayout cardCantonese;
    private FrameLayout cardJapanese;
    private FrameLayout cardMixedEngine;
    private CardView btnNextToUpload;
    
    // UI Components - Upload area
    private CardView cardUploadArea;
    private TextView tvUploadTitle;
    private TextView tvUploadSubtitle;
    private LinearLayout layoutFileInfo;
    private TextView tvFileName;
    private ImageView ivRemoveFile;
    private CardView btnBackToLanguage;
    private CardView btnStartTranscription;
    
    // State variables
    private int currentStep = 1;
    private String selectedLanguage = "zh-CN";
    private Uri selectedFileUri = null;
    private LoadingProgressDialog loadingDialog;
    
    // File size limit (50MB)
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_audio_transcription;
    }
    
    @Override
    protected Class<VMMeeting> getViewModelClass() {
        return VMMeeting.class;
    }
    
    @Override
    protected void initializeViews(View view) {
        // Initialize step indicators
        step1Circle = findViewById(R.id.step1_circle);
        step2Circle = findViewById(R.id.step2_circle);
        step3Circle = findViewById(R.id.step3_circle);
        step1Text = findViewById(R.id.step1_text);
        step2Text = findViewById(R.id.step2_text);
        // step3Text = findViewById(R.id.step3_text); // Step 3 doesn't have text, only check icon
        step1Check = findViewById(R.id.step1_check);
        step2Check = findViewById(R.id.step2_check);
        step3Check = findViewById(R.id.step3_check);
        step1Label = findViewById(R.id.step1_label);
        step2Label = findViewById(R.id.step2_label);
        step3Label = findViewById(R.id.step3_label);
        
        // Initialize content containers
        step1Content = findViewById(R.id.step1_content);
        step2Content = findViewById(R.id.step2_content);
        
        // Initialize language selection cards
        cardChineseMandarin = findViewById(R.id.card_chinese_mandarin);
        cardMainlandDialects = findViewById(R.id.card_mainland_dialects);
        cardEnglish = findViewById(R.id.card_english);
        cardCantonese = findViewById(R.id.card_cantonese);
        cardJapanese = findViewById(R.id.card_japanese);
        cardMixedEngine = findViewById(R.id.card_mixed_engine);
        btnNextToUpload = findViewById(R.id.btn_next_to_upload);
        
        // Initialize upload area
        cardUploadArea = findViewById(R.id.card_upload_area);
        tvUploadTitle = findViewById(R.id.tv_upload_title);
        tvUploadSubtitle = findViewById(R.id.tv_upload_subtitle);
        layoutFileInfo = findViewById(R.id.layout_file_info);
        tvFileName = findViewById(R.id.tv_file_name);
        ivRemoveFile = findViewById(R.id.iv_remove_file);
        btnBackToLanguage = findViewById(R.id.btn_back_to_language);
        btnStartTranscription = findViewById(R.id.btn_start_transcription);
        
        // Set click listeners
        setupClickListeners();
        
        // Initialize UI state
        updateStepIndicator(1);
        selectedLanguage = "zh-CN";
    }
    
    private void setupClickListeners() {
        // Language selection click listeners
        cardChineseMandarin.setOnClickListener(v -> selectLanguage("zh-CN", cardChineseMandarin));
        cardMainlandDialects.setOnClickListener(v -> selectLanguage("zh-dialect", cardMainlandDialects));
        cardEnglish.setOnClickListener(v -> selectLanguage("en-US", cardEnglish));
        cardCantonese.setOnClickListener(v -> selectLanguage("zh-HK", cardCantonese));
        cardJapanese.setOnClickListener(v -> selectLanguage("ja-JP", cardJapanese));
        cardMixedEngine.setOnClickListener(v -> selectLanguage("mixed", cardMixedEngine));
        
        // Navigation buttons
        btnNextToUpload.setOnClickListener(v -> moveToStep(2));
        btnBackToLanguage.setOnClickListener(v -> moveToStep(1));
        btnStartTranscription.setOnClickListener(v -> handleTranscriptionButtonClick());
        
        // Upload area
        cardUploadArea.setOnClickListener(v -> selectFile());
        ivRemoveFile.setOnClickListener(v -> removeSelectedFile());
    }
    
    @Override
    protected void setupDataBinding() {
        // Data binding for audio transcription
    }
    
    @Override
    protected void setupObservers() {
        // Observers for audio transcription
    }
    
    /**
     * Move to specific step
     */
    private void moveToStep(int step) {
        currentStep = step;
        updateStepIndicator(step);
        
        // Show/hide content based on step
        step1Content.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        step2Content.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
    }
    
    /**
     * Update step indicator UI
     */
    private void updateStepIndicator(int activeStep) {
        // Update step 1
        if (activeStep > 1) {
            step1Circle.setBackgroundResource(R.drawable.bg_step_circle_inactive);
            step1Text.setVisibility(View.GONE);
            step1Check.setVisibility(View.VISIBLE);
            step1Label.setTextColor(getResources().getColor(R.color.text_gray));
            step1Label.setTypeface(step1Label.getTypeface(), Typeface.NORMAL);
        } else {
            step1Circle.setBackgroundResource(R.drawable.bg_step_circle_active);
            step1Text.setVisibility(View.VISIBLE);
            step1Check.setVisibility(View.GONE);
            step1Label.setTextColor(getResources().getColor(R.color.text_primary));
            step1Label.setTypeface(step1Label.getTypeface(), Typeface.BOLD);
        }
        
        // Update step 2
        if (activeStep > 2) {
            step2Circle.setBackgroundResource(R.drawable.bg_step_circle_inactive);
            step2Text.setVisibility(View.GONE);
            step2Check.setVisibility(View.VISIBLE);
            step2Label.setTextColor(getResources().getColor(R.color.text_gray));
            step2Label.setTypeface(step2Label.getTypeface(), Typeface.NORMAL);
        } else if (activeStep == 2) {
            step2Circle.setBackgroundResource(R.drawable.bg_step_circle_active);
            step2Text.setVisibility(View.VISIBLE);
            step2Check.setVisibility(View.GONE);
            step2Label.setTextColor(getResources().getColor(R.color.text_primary));
            step2Label.setTypeface(step2Label.getTypeface(), Typeface.BOLD);
        } else {
            step2Circle.setBackgroundResource(R.drawable.bg_step_circle_inactive);
            step2Text.setVisibility(View.VISIBLE);
            step2Check.setVisibility(View.GONE);
            step2Label.setTextColor(getResources().getColor(R.color.text_gray));
            step2Label.setTypeface(step2Label.getTypeface(), Typeface.NORMAL);
        }
        
        // Step 3 is always showing check icon in inactive state
        step3Circle.setBackgroundResource(R.drawable.bg_step_circle_inactive);
        step3Check.setVisibility(View.VISIBLE);
        step3Label.setTextColor(getResources().getColor(R.color.text_gray));
        step3Label.setTypeface(step3Label.getTypeface(), Typeface.NORMAL);
    }
    
    /**
     * Select language and update UI
     */
    private void selectLanguage(String languageCode, FrameLayout selectedCard) {
        selectedLanguage = languageCode;
        
        // Reset all cards to unselected state
        resetAllLanguageCards();
        
        // Set selected card style  
        selectedCard.setBackgroundResource(R.drawable.bg_language_card_selected);
        
        // Update text style for selected card
        TextView textView = (TextView) selectedCard.getChildAt(0);
        textView.setTextColor(getResources().getColor(R.color.text_primary));
        textView.setTypeface(null, Typeface.BOLD);
        
        // Update check icon visibility
        updateCheckIcons(selectedCard);
    }
    
    /**
     * Reset all language cards to unselected state
     */
    private void resetAllLanguageCards() {
        FrameLayout[] cards = {cardChineseMandarin, cardMainlandDialects, cardEnglish, 
                           cardCantonese, cardJapanese, cardMixedEngine};
        
        for (FrameLayout card : cards) {
            card.setBackgroundResource(R.drawable.bg_language_card_normal);
            // Reset text style
            TextView textView = (TextView) card.getChildAt(0);
            textView.setTextColor(getResources().getColor(R.color.text_primary));
            textView.setTypeface(null, Typeface.NORMAL);
            // Hide check icon
            ImageView checkIcon = (ImageView) card.getChildAt(1);
            if (checkIcon != null) {
                checkIcon.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Update check icons for all language cards
     */
    private void updateCheckIcons(FrameLayout selectedCard) {
        // Show check icon for selected card
        ImageView checkIcon = (ImageView) selectedCard.getChildAt(1);
        if (checkIcon != null) {
            checkIcon.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Get language display name
     */
    private String getLanguageName(String languageCode) {
        switch (languageCode) {
            case "zh-CN": return "中文普通话";
            case "zh-dialect": return "大陆方言";
            case "en-US": return "English";
            case "zh-HK": return "粤语（简体）";
            case "ja-JP": return "日本語";
            case "mixed": return "中英粤混合";
            default: return "未知语言";
        }
    }
    
    
    /**
     * Handle transcription button click
     */
    private void handleTranscriptionButtonClick() {
        if (selectedFileUri == null) {
            showErrorDialog("请先选择音视频文件");
            return;
        }
        
        // Check file size
        long fileSize = getFileSize(selectedFileUri);
        if (fileSize > MAX_FILE_SIZE) {
            showFileSizeErrorDialog();
            return;
        }
        
        // Check file format
        String mimeType = getActivity().getContentResolver().getType(selectedFileUri);
        if (!isValidFileFormat(mimeType)) {
            showFormatErrorDialog();
            return;
        }
        
        // Start transcription
        startTranscription();
    }
    
    /**
     * Check if file format is valid
     */
    private boolean isValidFileFormat(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.startsWith("audio/") || mimeType.startsWith("video/");
    }
    
    /**
     * Get file size from Uri
     */
    private long getFileSize(Uri uri) {
        long fileSize = 0;
        try {
            ContentResolver contentResolver = getActivity().getContentResolver();
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileSize;
    }
    
    /**
     * Show file size error dialog
     */
    private void showFileSizeErrorDialog() {
        new ConfirmDialog(getActivity())
            .setTitle("文件大小超出限制")
            .setSubtitle("请选择小于50MB的文件")
            .setCancelText("")
            .setConfirmText("确定")
            .setOnConfirmDialogListener(new ConfirmDialog.OnConfirmDialogListener() {
                @Override
                public void onConfirm() {
                    // Dialog dismissed
                }
                
                @Override
                public void onCancel() {
                    // No cancel button
                }
            })
            .show();
    }
    
    /**
     * Show format error dialog
     */
    private void showFormatErrorDialog() {
        new ConfirmDialog(getActivity())
            .setTitle("不支持该文件格式")
            .setSubtitle("请选择音频或视频文件")
            .setCancelText("")
            .setConfirmText("确定")
            .setOnConfirmDialogListener(new ConfirmDialog.OnConfirmDialogListener() {
                @Override
                public void onConfirm() {
                    // Dialog dismissed
                }
                
                @Override
                public void onCancel() {
                    // No cancel button
                }
            })
            .show();
    }
    
    /**
     * Show general error dialog
     */
    private void showErrorDialog(String message) {
        new ConfirmDialog(getActivity())
            .setTitle("提示")
            .setSubtitle(message)
            .setCancelText("")
            .setConfirmText("确定")
            .setOnConfirmDialogListener(new ConfirmDialog.OnConfirmDialogListener() {
                @Override
                public void onConfirm() {
                    // Dialog dismissed
                }
                
                @Override
                public void onCancel() {
                    // No cancel button
                }
            })
            .show();
    }
    
    /**
     * Select file for transcription
     */
    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] mimeTypes = {"audio/*", "video/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, "选择音视频文件"), REQUEST_CODE_SELECT_FILE);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                selectedFileUri = data.getData();
                displaySelectedFile();
            }
        }
    }
    
    /**
     * Display selected file information
     */
    private void displaySelectedFile() {
        if (selectedFileUri != null) {
            String fileName = getFileName(selectedFileUri);
            tvFileName.setText(fileName);
            
            // Update UI to show file info
            tvUploadTitle.setVisibility(View.GONE);
            tvUploadSubtitle.setVisibility(View.GONE);
            layoutFileInfo.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Remove selected file
     */
    private void removeSelectedFile() {
        selectedFileUri = null;
        
        // Reset UI
        tvUploadTitle.setVisibility(View.VISIBLE);
        tvUploadSubtitle.setVisibility(View.VISIBLE);
        layoutFileInfo.setVisibility(View.GONE);
        tvFileName.setText("");
    }
    
    /**
     * Get file name from Uri
     */
    private String getFileName(Uri uri) {
        String fileName = "未知文件";
        if (uri != null) {
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                File file = new File(uri.getPath());
                fileName = file.getName();
            } else {
                ContentResolver contentResolver = getActivity().getContentResolver();
                try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return fileName;
    }
    
    /**
     * Start transcription process
     */
    private void startTranscription() {
        if (selectedFileUri == null || selectedLanguage == null) {
            showErrorDialog("请选择文件和语言");
            return;
        }
        
        // Show loading dialog
        showUploadingDialog();
        
        // TODO: Implement actual transcription logic
        // 1. Upload file to server
        // 2. Start transcription with selected language
        // 3. Navigate to results page when complete
        
        // Simulate upload delay
        getView().postDelayed(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            showToast("转写请求已提交");
            // Navigate to results or next step
        }, 3000);
    }
    
    /**
     * Show uploading dialog
     */
    private void showUploadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new LoadingProgressDialog(getActivity());
        }
        loadingDialog.setMessage("上传中请稍等...");
        loadingDialog.setCancelable(false);
        loadingDialog.show();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // 切换界面时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        
        // 确保销毁时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }
}