package com.fxzs.lingxiagent.view.meeting;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.viewmodel.meeting.VMStream;

public class StreamFragment extends BaseFragment<VMStream> {

    // UI控件
    private TextView tvStreamStatus;
    private ProgressBar progressStream;
    private TextView tvProgressText;
    private Button btnStartStream;
    private Button btnApiStream;
    private Button btnStopStream;
    private Button btnClear;
    private TextView tvStreamContent;
    private ProgressBar progressLoading;

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_stream;
    }

    @Override
    protected Class<VMStream> getViewModelClass() {
        return VMStream.class;
    }

    @Override
    protected void initializeViews(View view) {
        // 初始化控件
        tvStreamStatus = view.findViewById(R.id.tv_stream_status);
        progressStream = view.findViewById(R.id.progress_stream);
        tvProgressText = view.findViewById(R.id.tv_progress_text);
        btnStartStream = view.findViewById(R.id.btn_start_stream);
        btnApiStream = view.findViewById(R.id.btn_api_stream);
        btnStopStream = view.findViewById(R.id.btn_stop_stream);
        btnClear = view.findViewById(R.id.btn_clear);
        tvStreamContent = view.findViewById(R.id.tv_stream_content);
        progressLoading = view.findViewById(R.id.progress_loading);

        // 设置点击事件
        btnStartStream.setOnClickListener(v -> viewModel.startStreaming());
        btnApiStream.setOnClickListener(v -> viewModel.simulateApiStream());
        btnStopStream.setOnClickListener(v -> viewModel.stopStreaming());
        btnClear.setOnClickListener(v -> viewModel.clearStream());
    }

    @Override
    protected void setupDataBinding() {
        // 绑定状态文本
        DataBindingUtils.bindTextView(tvStreamStatus, viewModel.getStreamStatus(), this);
        
        // 绑定流式内容
        DataBindingUtils.bindTextView(tvStreamContent, viewModel.getStreamContent(), this);
        
        // 绑定按钮启用状态
        DataBindingUtils.bindButtonEnabled(btnStartStream, viewModel.getStartButtonEnabled(), this);
        DataBindingUtils.bindButtonEnabled(btnStopStream, viewModel.getStopButtonEnabled(), this);
        DataBindingUtils.bindButtonEnabled(btnClear, viewModel.getClearButtonEnabled(), this);
        
        // 绑定API流按钮状态（与开始按钮状态相同）
        DataBindingUtils.bindButtonEnabled(btnApiStream, viewModel.getStartButtonEnabled(), this);
    }

    @Override
    protected void setupObservers() {
        // 观察进度变化
        viewModel.getStreamProgress().observeForever(progress -> {
            if (progress != null && getActivity() != null) {
                progressStream.setProgress(progress);
                tvProgressText.setText(progress + "%");
            }
        });
        
        // 观察流状态，控制按钮样式
        viewModel.getIsStreaming().observeForever(isStreaming -> {
            if (isStreaming != null && getActivity() != null) {
                // 更新按钮文本和样式
                if (isStreaming) {
                    btnStartStream.setText("传输中...");
                    btnApiStream.setText("API流中...");
                } else {
                    btnStartStream.setText("开始流传输");
                    btnApiStream.setText("API流模拟");
                }
                
                // 重置进度条样式
                if (!isStreaming && viewModel.getStreamProgress().get() != null && 
                    viewModel.getStreamProgress().get() == 100) {
                    // 完成状态保持绿色
                    progressStream.setProgressTintList(
                        getResources().getColorStateList(R.color.success_color, null));
                } else if (isStreaming) {
                    // 进行中状态使用主色
                    progressStream.setProgressTintList(
                        getResources().getColorStateList(R.color.color_primary, null));
                }
            }
        });
        
        // 观察内容变化，自动滚动到底部
        viewModel.getStreamContent().observeForever(content -> {
            if (content != null && !content.isEmpty() && getActivity() != null && isAdded()) {
                // 延迟滚动，确保内容已更新
                tvStreamContent.post(() -> {
                    // 滚动到底部
                    View parent = (View) tvStreamContent.getParent();
                    if (parent instanceof android.widget.ScrollView) {
                        ((android.widget.ScrollView) parent).fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
        
        // 观察错误状态
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && getActivity() != null) {
                showToast(error);
                // 错误时重置进度条颜色
                progressStream.setProgressTintList(
                    getResources().getColorStateList(R.color.error_color, null));
            }
        });
        
        // 观察成功状态
        viewModel.getSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && getActivity() != null) {
                showToast(success);
                // 成功时设置进度条为绿色
                if (viewModel.getStreamProgress().get() != null && 
                    viewModel.getStreamProgress().get() == 100) {
                    progressStream.setProgressTintList(
                        getResources().getColorStateList(R.color.success_color, null));
                }
            }
        });
        
        // 观察加载状态
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (progressLoading != null) {
                progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 确保停止流式传输
        if (viewModel != null) {
            viewModel.stopStreaming();
        }
    }
}