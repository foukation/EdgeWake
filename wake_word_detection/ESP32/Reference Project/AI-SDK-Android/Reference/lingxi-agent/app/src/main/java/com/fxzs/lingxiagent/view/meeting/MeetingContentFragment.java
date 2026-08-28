package com.fxzs.lingxiagent.view.meeting;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.WordExportUtil;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.auth.RegisterActivity;
import com.fxzs.lingxiagent.view.common.AIResponseView;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.viewmodel.meeting.VMMeetingContent;

import timber.log.Timber;

public class MeetingContentFragment extends BaseFragment<VMMeetingContent> {
    
    private static final String TAG = "MeetingContentFragment";
    private static final String ARG_MEETING_ID = "meeting_id";
    private static final String ARG_TRANSCRIPTION_RESULT = "transcription_result";
    
    private String meetingId;
    private String transcriptionResult;
    private AIResponseView aiResponseView;
    
    public static MeetingContentFragment newInstance(String meetingId, String transcriptionResult) {
        MeetingContentFragment fragment = new MeetingContentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEETING_ID, meetingId);
        args.putString(ARG_TRANSCRIPTION_RESULT, transcriptionResult);

//        Log.e("测试","transcriptionResult" +transcriptionResult);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            meetingId = getArguments().getString(ARG_MEETING_ID);
            transcriptionResult = getArguments().getString(ARG_TRANSCRIPTION_RESULT);
        }
    }
    
    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_meeting_content;
    }
    
    @Override
    protected Class<VMMeetingContent> getViewModelClass() {
        return VMMeetingContent.class;
    }
    
    @Override
    protected void initializeViews(View view) {
        aiResponseView = view.findViewById(R.id.ai_response_view);

        setupAIResponseView();
        setDefaultContent();
    }
    
    @Override
    protected void setupDataBinding() {
        if (viewModel != null) {
            // 设置会议ID
            if (meetingId != null) {
                Timber.tag(TAG).d( "setupDataBinding - 设置会议ID: " + meetingId);
                viewModel.setMeetingId(meetingId);
            }

            // 设置原始转写内容
            if (transcriptionResult != null) {
                Timber.tag(TAG).d( "setupDataBinding - 设置转写内容，长度: " + transcriptionResult.length());
                viewModel.setRawTranscriptionContent(transcriptionResult);
            }
        }
    }
    
    @Override
    protected void setupObservers() {
        // 观察转写内容变化
        viewModel.getTranscriptionContent().observeForever(content -> {
            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {
                    Timber.tag(TAG).d( "setupObservers - 转写内容更新，长度: " + 
                        (content != null ? content.length() : "null"));
                    updateContentDisplay(content);
                });
            }
        });
        

        
        // 观察错误状态
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                if (error.contains("账号未登录") || error.contains("请重新登录")) {
                    handleLoginError();
                } else {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // 观察成功状态
        viewModel.getSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                Timber.tag("MeetingContentFragment").i( "操作成功: " + success);
            }
        });
    }
    
    private void setDefaultContent() {
        if (transcriptionResult != null && !transcriptionResult.isEmpty()) {
            aiResponseView.setContent(transcriptionResult.trim());
        } else {
            aiResponseView.setContent("会议内容加载中...");
        }
    }
    
    private void updateContentDisplay(String content) {
        if (aiResponseView != null) {
            if (content != null && !content.trim().isEmpty()) {
                Timber.tag(TAG).d( "updateContentDisplay - 更新内容显示，长度: " + content.length());
                // 显示前50个字符作为预览
                String preview = content.length() > 50 ? content.substring(0, 50) + "..." : content;
                Timber.tag(TAG).d( "updateContentDisplay - 内容预览: " + preview);
                aiResponseView.setContent(content.trim());
            } else {
                Timber.tag(TAG).d( "updateContentDisplay - 内容为空");
                aiResponseView.setContent("暂无会议转写内容");
            }
        }
    }
    
    private void setupAIResponseView() {
        if (aiResponseView != null) {
            // 设置刷新按钮监听器
            aiResponseView.setOnRefreshClickListener(new AIResponseView.OnRefreshClickListener() {
                @Override
                public void onRefreshClick() {
                    refreshContent();
                }
            });
            
            // AIResponseView 已经内置了复制和语音播放功能
            // 显示所有按钮
            aiResponseView.showCopyButton(true);
            aiResponseView.showSpeakButton(true);
            aiResponseView.showDivider(true);
            aiResponseView.setRefreshBtnVisible(View.GONE);
        }
    }
    
    // 复制功能已由 AIResponseView 内部处理
    
    private void refreshContent() {
        if (viewModel == null) {
            Toast.makeText(getContext(), "刷新失败，请重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        viewModel.refreshContent();
    }
    
    // 更多选项功能暂时移除，可在后续版本中集成到 AIResponseView 或其他位置
    
    private void handleLoginError() {
        if (aiResponseView != null) {
            aiResponseView.setContent("登录状态已过期\n\n您的登录已失效，请重新登录后再试。\n\n点击下方按钮返回登录页面。");
        }
        
        // 3秒后自动跳转到登录页面
        if (getView() != null) {
            getView().postDelayed(() -> {
                if (getActivity() != null) {
                    android.content.Intent intent = new android.content.Intent(getActivity(), RegisterActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                }
            }, 3000);
        }
    }

    /**
     * 导出会议内容为Word文档
     */
    public void exportToWord() {
        if (aiResponseView == null) {
            GlobalToast.show(getActivity(), "暂无内容可导出", GlobalToast.Type.ERROR);
            return;
        }

        // 获取AIResponse的内容
        String content = aiResponseView.getContent();
        if (content == null || content.trim().isEmpty()) {
            // 如果AIResponse没有内容，尝试获取原始转写内容
            if (transcriptionResult != null && !transcriptionResult.trim().isEmpty()) {
                content = transcriptionResult;
            } else {
                GlobalToast.show(getActivity(), "暂无内容可导出", GlobalToast.Type.ERROR);
                return;
            }
        }

        // 测试修复后的中文编码（仅在调试模式下）
        if (android.util.Log.isLoggable("MeetingContentFragment", android.util.Log.DEBUG)) {
            WordExportUtil.testFixedChineseEncoding(getContext());
        }

        // 显示导出进度
        GlobalToast.show(getActivity(), "正在导出会议内容文档...", GlobalToast.Type.NORMAL);

        // 导出Word文档
        WordExportUtil.exportToWord(getContext(), "会议内容", content, new WordExportUtil.ExportCallback() {
            @Override
            public void onSuccess(java.io.File file) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // 导出成功后直接显示弹窗，不显示Toast
                        showOpenDocumentDialog(file);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        GlobalToast.show(getActivity(), "导出失败: " + error, GlobalToast.Type.ERROR);
                    });
                }
            }
        });
    }

    /**
     * 显示打开文档的对话框
     */
    private void showOpenDocumentDialog(java.io.File file) {
        if (getContext() == null) return;

        new CommonDialog.Builder(getContext())
                .setTitle("导出成功")
                .setMessage("会议内容文档已保存到:\n" + file.getName() + "\n\n是否立即打开？")
                .setConfirmText("打开")
                .setCancelText("稍后")
                .setOnClickListener(new CommonDialog.OnDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        WordExportUtil.openDocument(getContext(), file);
                    }

                    @Override
                    public void onCancel() {
                        // 用户选择稍后，不做任何操作
                    }
                })
                .show();
    }

    @Override
    public void onPause() {
        super.onPause();
        // 切换界面时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }

    @Override
    public void onDestroyView() {
        // 释放AIResponseView资源
        if (aiResponseView != null) {
            aiResponseView.release();
        }
        
        // 确保销毁时停止TTS播放
        TTSManager.Companion.getInstance().stop();

        super.onDestroyView();
    }

}