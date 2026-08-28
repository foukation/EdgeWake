package com.fxzs.lingxiagent.view.meeting;

import android.content.Intent;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.AudioRecognitionManager;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.meeting.VMMeeting;

/**
 * 录音文件上传等待界面
 * 显示上传进度和语音识别进度
 */
public class MeetingUploadWaitingActivity extends BaseActivity<VMMeeting> {
    
    private ImageView ivFileIcon;
    private TextView tvStatus;
    private TextView tvProgress;
    private ProgressBar progressBar;
    private TextView tvTips;
    
    private String audioFilePath;
    private String meetingTitle;
    private String meetingId;
    private AudioRecognitionManager audioRecognitionManager;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_meeting_upload_waiting;
    }
    
    @Override
    protected Class<VMMeeting> getViewModelClass() {
        return VMMeeting.class;
    }
    
    @Override
    protected void initializeViews() {
        ivFileIcon = findViewById(R.id.iv_file_icon);
        tvStatus = findViewById(R.id.tv_status);
        tvProgress = findViewById(R.id.tv_progress);
        progressBar = findViewById(R.id.progress_bar);
        tvTips = findViewById(R.id.tv_tips);
        
        // 获取传递的参数
        Intent intent = getIntent();
        audioFilePath = intent.getStringExtra("audio_file_path");
        meetingTitle = intent.getStringExtra("meeting_title");
        meetingId = intent.getStringExtra("meeting_id");
        
        // 初始化音频识别管理器
        audioRecognitionManager = new AudioRecognitionManager(this);
        
        // 设置初始状态
        updateUI("上传中请稍等...", 0, "正在上传录音文件到云端");
        
        // 开始上传和识别流程
        startAudioRecognition();
    }
    
    @Override
    protected void setupDataBinding() {
        // 不需要数据绑定
    }
    
    @Override
    protected void setupObservers() {
        // 观察ViewModel状态变化
        viewModel.getLoading().observe(this, loading -> {
            if (loading != null && loading) {
                progressBar.setIndeterminate(true);
            }
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                updateUI("上传失败", 0, error);
                // 延迟返回
                tvTips.postDelayed(() -> {
                    setResult(RESULT_CANCELED);
                    finish();
                }, 3000);
            }
        });
    }
    
    /**
     * 开始音频识别流程
     */
    private void startAudioRecognition() {
        if (audioFilePath == null) {
            updateUI("上传失败", 0, "录音文件路径无效");
            return;
        }
        
        Integer meetingIdInt = null;
        try {
            if (meetingId != null && !meetingId.startsWith("temp_")) {
                meetingIdInt = Integer.parseInt(meetingId);
            }
        } catch (NumberFormatException e) {
            // meetingId无效，继续处理但不关联特定会议
        }
        
        audioRecognitionManager.startAudioRecognition(audioFilePath, meetingIdInt, new AudioRecognitionManager.AudioRecognitionCallback() {
            @Override
            public void onUploadProgress(int progress) {
                runOnUiThread(() -> {
                    updateUI("上传中请稍等...", progress, "正在上传录音文件到云端");
                });
            }
            
            @Override
            public void onUploadSuccess(String fileUrl) {
                runOnUiThread(() -> {
                    updateUI("会议生成中...", 100, "文件上传成功，正在进行语音识别");
                });
            }
            
            @Override
            public void onTaskSubmitted(Long taskId) {
                runOnUiThread(() -> {
                    updateUI("会议生成中...", 0, "识别任务已提交，正在处理中");
                });
            }
            
            @Override
            public void onRecognitionProgress(int progress) {
                runOnUiThread(() -> {
                    updateUI("会议生成中...", progress, "正在识别语音内容，请耐心等待");
                });
            }
            
            @Override
            public void onRecognitionCompleted(String result, Integer meetingId) {
                runOnUiThread(() -> {
                    // 处理识别结果为空的情况
                    if (result == null || result.trim().isEmpty()) {
                        updateUI("识别完成", 100, "未识别到有效内容，可能是录音时长太短或无声音");

                        // 保存空结果到ViewModel
                        viewModel.setTranscriptionResult("未识别到有效内容");
                        viewModel.setAudioFilePath(audioFilePath);
                        viewModel.setMeetingId(meetingId); // 保存meetingId

                        // 延迟跳转到会议内容页面，显示空内容提示
                        tvTips.postDelayed(() -> {
                            navigateToMeetingContent("未识别到有效内容");
                        }, 2000);
                    } else {
                        updateUI("识别完成", 100, "语音识别成功，正在跳转到会议内容");

                        // 保存识别结果到ViewModel
                        viewModel.setTranscriptionResult(result);
                        viewModel.setAudioFilePath(audioFilePath);
                        viewModel.setMeetingId(meetingId); // 保存meetingId

                        // 延迟跳转到会议内容页面
                        tvTips.postDelayed(() -> {
                            navigateToMeetingContent(result);
                        }, 1500);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    updateUI("处理失败", 0, error);
                    // 延迟返回
                    tvTips.postDelayed(() -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    }, 3000);
                });
            }
        });
    }
    
    /**
     * 更新UI显示
     */
    private void updateUI(String status, int progress, String tips) {
        tvStatus.setText(status);
        tvTips.setText(tips);
        
        if (progress > 0) {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(progress);
            tvProgress.setText(progress + "%");
            tvProgress.setVisibility(TextView.VISIBLE);
        } else {
            progressBar.setIndeterminate(true);
            tvProgress.setVisibility(TextView.GONE);
        }
    }
    
    /**
     * 跳转到会议内容页面
     */
    private void navigateToMeetingContent(String transcriptionResult) {
        // 设置结果并关闭当前Activity
        setResult(RESULT_OK);
        finish();
    }
    
    @Override
    public void onBackPressed() {
        // 在处理过程中禁用返回键，防止用户误操作
        showToast("正在处理中，请稍候...");
        // 不调用 super.onBackPressed()，阻止返回
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 切换界面时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        if (audioRecognitionManager != null) {
            // AudioRecognitionManager没有提供取消方法，但可以在这里做清理
        }
        
        // 确保销毁时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }
}