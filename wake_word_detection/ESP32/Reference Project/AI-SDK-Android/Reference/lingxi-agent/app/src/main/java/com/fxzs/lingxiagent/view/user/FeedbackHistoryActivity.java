package com.fxzs.lingxiagent.view.user;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.viewmodel.user.VMFeedbackHistory;

public class FeedbackHistoryActivity extends BaseActivity<VMFeedbackHistory> {
    
    private ImageView ivBack;
    private ImageView ivClose;
    private LinearLayout layoutEmpty;
    private RecyclerView rvFeedbackList;
    private FeedbackHistoryAdapter adapter;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_feedback_history;
    }
    
    @Override
    protected Class<VMFeedbackHistory> getViewModelClass() {
        return VMFeedbackHistory.class;
    }
    
    @Override
    protected void initializeViews() {
        // 设置状态栏颜色为白色，与背景一致，并保证内容不被遮挡
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            getWindow().getDecorView().postDelayed(() -> {
                getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            }, 100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        // 初始化控件
        ivBack = findViewById(R.id.iv_back);
        ivClose = findViewById(R.id.iv_close);
        layoutEmpty = findViewById(R.id.layout_empty);
        rvFeedbackList = findViewById(R.id.rv_feedback_list);
        
        // 设置RecyclerView
        rvFeedbackList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FeedbackHistoryAdapter(this);
        adapter.setOnItemClickListener(feedback -> {
            // 跳转到反馈详情页面
            viewModel.selectFeedback(feedback);
        });
        rvFeedbackList.setAdapter(adapter);
        
        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());
        ivClose.setOnClickListener(v -> finish());
    }
    
    @Override
    protected void setupDataBinding() {
        // 绑定空状态和列表显示
        DataBindingUtils.bindVisibility(layoutEmpty, viewModel.getHasData(), this, true); // 反转显示逻辑
        DataBindingUtils.bindVisibility(rvFeedbackList, viewModel.getHasData(), this);
    }
    
    @Override
    protected void setupObservers() {
        // 观察反馈列表
        viewModel.getFeedbackList().observe(this, feedbackList -> {
            if (adapter != null) {
                adapter.setFeedbackList(feedbackList);
            }
        });
        
        // 观察选中的反馈
        viewModel.getSelectedFeedback().observe(this, feedback -> {
            if (feedback != null) {
                // 打开反馈详情页面
                Intent intent = new Intent(this, FeedbackDetailActivity.class);
                intent.putExtra("feedback_id", feedback.getId());
                startActivity(intent);
            }
        });
    }
    
    @Override
    protected void handleLoadingState(boolean loading) {
        // 可以自定义加载状态显示
        super.handleLoadingState(loading);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 设置状态栏颜色为白色，与背景一致，并保证内容不被遮挡
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            getWindow().getDecorView().postDelayed(() -> {
                getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            }, 100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }
}