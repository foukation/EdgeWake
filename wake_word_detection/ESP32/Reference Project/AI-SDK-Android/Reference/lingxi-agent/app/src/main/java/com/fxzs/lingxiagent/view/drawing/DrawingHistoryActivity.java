package com.fxzs.lingxiagent.view.drawing;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSessionDto;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.viewmodel.drawing.VMDrawingHistory;

import java.util.List;

/**
 * 绘画历史记录页面
 */
public class DrawingHistoryActivity extends BaseActivity<VMDrawingHistory> {
    
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvHistory;
    private LinearLayout llEmpty;
    private TextView tvEmpty;
    private EditText etSearch;
    private ImageView ivClearSearch;
    private TextView tvFilterAll;
    private TextView tvFilterToday;
    private TextView tvFilterWeek;
    private TextView tvFilterMonth;
    
    private DrawingHistoryAdapter historyAdapter;
    private GridLayoutManager layoutManager;
    private boolean isLoadingMore = false;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_drawing_history;
    }
    
    @Override
    protected Class<VMDrawingHistory> getViewModelClass() {
        return VMDrawingHistory.class;
    }
    
    @Override
    protected void initializeViews() {
        // 初始化控件
        swipeRefresh = findViewById(R.id.swipe_refresh);
        rvHistory = findViewById(R.id.rv_history);
        llEmpty = findViewById(R.id.ll_empty);
        tvEmpty = findViewById(R.id.tv_empty);
        etSearch = findViewById(R.id.et_search);
        ivClearSearch = findViewById(R.id.iv_clear_search);
        tvFilterAll = findViewById(R.id.tv_filter_all);
        tvFilterToday = findViewById(R.id.tv_filter_today);
        tvFilterWeek = findViewById(R.id.tv_filter_week);
        tvFilterMonth = findViewById(R.id.tv_filter_month);
        
        // 返回按钮
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        
        // 更多选项按钮（清空历史等）
        // TODO: 添加更多选项按钮到布局文件后启用
        // findViewById(R.id.iv_more).setOnClickListener(v -> showMoreOptionsMenu());
        
        // 设置RecyclerView
        setupRecyclerView();
        
        // 设置过滤器点击事件
        setupFilterButtons();
        
        // 设置搜索功能
        setupSearch();
        
        // 设置下拉刷新
        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }
    
    @Override
    protected void setupDataBinding() {
        // 绑定搜索框
        DataBindingUtils.bindEditText(etSearch, viewModel.getSearchQuery(), this);
        
        // 绑定刷新状态
        viewModel.getIsRefreshing().observe(this, refreshing -> {
            if (refreshing != null) {
                swipeRefresh.setRefreshing(refreshing);
            }
        });
        
        // 绑定空视图消息
        DataBindingUtils.bindTextView(tvEmpty, viewModel.getEmptyMessage(), this);
    }
    
    @Override
    protected void setupObservers() {
        // 观察历史记录列表
        viewModel.getHistoryImages().observe(this, images -> {
            if (images != null) {
                historyAdapter.setImages(images);
                updateEmptyView(images.isEmpty());
            }
        });
        
        // 观察空视图显示状态
        viewModel.getShowEmptyView().observe(this, show -> {
            if (show != null) {
                updateEmptyView(show);
            }
        });
        
        // 观察选中的图片（用于导航到详情页）
        viewModel.getSelectedImage().observe(this, image -> {
            if (image != null) {
                navigateToImageDetail(image);
            }
        });
        
        // 观察过滤器状态
        viewModel.getSelectedFilter().observe(this, filter -> {
            if (filter != null) {
                updateFilterButtonStates(filter);
            }
        });
        
        // 观察加载更多状态
        viewModel.getIsLoadingMore().observe(this, loading -> {
            isLoadingMore = loading != null && loading;
        });
    }
    
    private void setupRecyclerView() {
        // 使用网格布局，每行2列
        layoutManager = new GridLayoutManager(this, 2);
        rvHistory.setLayoutManager(layoutManager);
        
        // 设置适配器
        historyAdapter = new DrawingHistoryAdapter();
        historyAdapter.setOnItemClickListener(image -> viewModel.viewImageDetail(image));
        historyAdapter.setOnDeleteClickListener(image -> showDeleteConfirmDialog(image));
        rvHistory.setAdapter(historyAdapter);
        
        // 设置滚动监听，实现加载更多
        rvHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (dy > 0) { // 向下滚动
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if (!isLoadingMore && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0 && totalItemCount >= 20) {
                        viewModel.loadMore();
                    }
                }
            }
        });
    }
    
    private void setupFilterButtons() {
        tvFilterAll.setOnClickListener(v -> viewModel.setFilter("all"));
        tvFilterToday.setOnClickListener(v -> viewModel.setFilter("today"));
        tvFilterWeek.setOnClickListener(v -> viewModel.setFilter("week"));
        tvFilterMonth.setOnClickListener(v -> viewModel.setFilter("month"));
        
        // 默认选中"全部"
        updateFilterButtonStates("all");
    }
    
    private void setupSearch() {
        // 搜索框文本变化监听
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // 清除搜索按钮
        ivClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            viewModel.getSearchQuery().set("");
        });
    }
    
    private void updateFilterButtonStates(String selectedFilter) {
        // 重置所有按钮状态
        tvFilterAll.setSelected(false);
        tvFilterToday.setSelected(false);
        tvFilterWeek.setSelected(false);
        tvFilterMonth.setSelected(false);
        
        // 设置选中状态
        switch (selectedFilter) {
            case "all":
                tvFilterAll.setSelected(true);
                break;
            case "today":
                tvFilterToday.setSelected(true);
                break;
            case "week":
                tvFilterWeek.setSelected(true);
                break;
            case "month":
                tvFilterMonth.setSelected(true);
                break;
        }
    }
    
    private void updateEmptyView(boolean isEmpty) {
        llEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
    
    private void showDeleteConfirmDialog(DrawingImageDto image) {
        // 显示删除确认对话框
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("删除确认")
                .setMessage("确定要删除这张图片吗？")
                .setPositiveButton("删除", (dialog, which) -> viewModel.deleteImage(image))
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void navigateToImageDetail(DrawingImageDto image) {
        // 导航到图片详情页
        Intent intent = new Intent(this, DrawingDetailActivity.class);
        intent.putExtra("image_id", String.valueOf(image.getId()));
        startActivityForResult(intent, 100);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // 如果从详情页删除了图片，刷新列表
            viewModel.loadHistory();
        }
    }
    
    private void showMoreOptionsMenu() {
        // 创建选项数组
        String[] options = {"清空当前会话", "删除所有历史"};
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("更多选项")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // 清空当前会话
                            showClearSessionConfirmDialog();
                            break;
                        case 1:
                            // 删除所有历史
                            showDeleteAllConfirmDialog();
                            break;
                    }
                })
                .show();
    }
    
    private void showClearSessionConfirmDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("清空会话")
                .setMessage("确定要清空当前会话的所有记录吗？")
                .setPositiveButton("清空", (dialog, which) -> {
                    // 获取当前会话并清空
                    List<DrawingSessionDto> sessions = viewModel.getSessions().getValue();
                    if (sessions != null && !sessions.isEmpty()) {
                        viewModel.clearSession(sessions.get(0)); // 清空第一个会话
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void showDeleteAllConfirmDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("删除所有历史")
                .setMessage("确定要删除所有绘画历史记录吗？此操作不可恢复。")
                .setPositiveButton("删除", (dialog, which) -> viewModel.deleteAllSessions())
                .setNegativeButton("取消", null)
                .show();
    }
}