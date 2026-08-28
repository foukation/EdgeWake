package com.fxzs.lingxiagent.view.drawing;

import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSampleDto;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.user.UserProfileActivity;
import com.fxzs.lingxiagent.viewmodel.drawing.VMDrawingSamples;

import java.util.List;

/**
 * AI绘画样本画廊界面
 */
public class DrawingSamplesActivity extends BaseActivity<VMDrawingSamples> {
    
    // Tab相关
    private TextView tabSelected;
    private TextView tabPortrait;
    private TextView tabArt;
    private TextView tabChineseComic;
    private TextView tabAnime;
    private View tabIndicator;
    
    // 图片展示区域
    private ImageView imgCatSunglasses;
    private ImageView imgCatCity;
    private ImageView imgGirlStudent;
    private ImageView imgGirlPortrait;
    
    // 做同款按钮
    private TextView btnCatSunglasses;
    private TextView btnCatCity;
    private TextView btnGirlStudent;
    private TextView btnGirlPortrait;
    
    // 按住说话按钮
    private TextView btnVoiceInput;
    private ImageView ivMore;
    
    // 底部导航
    private LinearLayout navChat;
    private LinearLayout navAgent;
    private LinearLayout navDrawing;
    private LinearLayout navMeeting;
    private LinearLayout navUser;
    
    // 语音相关
    private boolean isVoiceRecording = false;
    private float initialTouchY;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_drawing_samples;
    }
    
    @Override
    protected Class<VMDrawingSamples> getViewModelClass() {
        return VMDrawingSamples.class;
    }
    
    @Override
    protected void initializeViews() {
        // 初始化Tab
        tabSelected = findViewById(R.id.tab_selected);
        tabPortrait = findViewById(R.id.tab_portrait);
        tabArt = findViewById(R.id.tab_art);
        tabChineseComic = findViewById(R.id.tab_chinese_comic);
        tabAnime = findViewById(R.id.tab_anime);
        tabIndicator = findViewById(R.id.tab_indicator);
        
        // 初始化图片
        imgCatSunglasses = findViewById(R.id.img_cat_sunglasses);
        imgCatCity = findViewById(R.id.img_cat_city);
        imgGirlStudent = findViewById(R.id.img_girl_student);
        imgGirlPortrait = findViewById(R.id.img_girl_portrait);
        
        // 初始化做同款按钮
        btnCatSunglasses = findViewById(R.id.btn_cat_sunglasses);
        btnCatCity = findViewById(R.id.btn_cat_city);
        btnGirlStudent = findViewById(R.id.btn_girl_student);
        btnGirlPortrait = findViewById(R.id.btn_girl_portrait);
        
        // 初始化按钮
        btnVoiceInput = findViewById(R.id.btn_voice_input);
        // ivMore = findViewById(R.id.iv_more); // 暂时注释，稍后处理
        
        // 初始化底部导航
        navChat = findViewById(R.id.nav_chat);
        navAgent = findViewById(R.id.nav_agent);
        navDrawing = findViewById(R.id.nav_drawing);
        navMeeting = findViewById(R.id.nav_meeting);
        navUser = findViewById(R.id.nav_user);
        
        // 设置Tab点击事件
        setupTabListeners();
        
        // 设置做同款按钮点击事件
        setupSameStyleButtons();
        
        // 设置语音按钮
        setupVoiceButton();
        
        // 设置底部导航
        setupBottomNavigation();
        
        // 更多选项 - 点击下方图标跳转到绘画聊天界面
        View moreView = findViewById(R.id.iv_more_horizontal);
        if (moreView != null) {
            moreView.setOnClickListener(v -> {
                // 跳转到绘画聊天界面
                Intent intent = new Intent(this, DrawingActivity.class);
                startActivity(intent);
            });
        }
    }
    
    @Override
    protected void setupDataBinding() {
        // 这里可以绑定数据，如果需要的话
    }
    
    @Override
    protected void setupObservers() {
        // 观察样本数据
        viewModel.getSamples().observe(this, samples -> {
            updateSampleImages(samples);
        });
        
        // 观察选中的分类
        viewModel.getSelectedCategory().observe(this, category -> {
            updateTabIndicator(category);
            // 加载对应分类的样本
            viewModel.loadSamplesByCategory(category);
        });
    }
    
    // 设置Tab监听器
    private void setupTabListeners() {
        tabSelected.setOnClickListener(v -> selectTab(0));
        tabPortrait.setOnClickListener(v -> selectTab(1));
        tabArt.setOnClickListener(v -> selectTab(2));
        tabChineseComic.setOnClickListener(v -> selectTab(3));
        tabAnime.setOnClickListener(v -> selectTab(4));
    }
    
    // 选择Tab
    private void selectTab(int position) {
        // 重置所有Tab颜色
        tabSelected.setTextColor(getResources().getColor(R.color.text_secondary));
        tabPortrait.setTextColor(getResources().getColor(R.color.text_secondary));
        tabArt.setTextColor(getResources().getColor(R.color.text_secondary));
        tabChineseComic.setTextColor(getResources().getColor(R.color.text_secondary));
        tabAnime.setTextColor(getResources().getColor(R.color.text_secondary));
        
        // 设置选中的Tab颜色和指示器位置
        TextView selectedTab = null;
        String category = "";
        
        switch (position) {
            case 0:
                selectedTab = tabSelected;
                category = "selected";
                break;
            case 1:
                selectedTab = tabPortrait;
                category = "portrait";
                break;
            case 2:
                selectedTab = tabArt;
                category = "art";
                break;
            case 3:
                selectedTab = tabChineseComic;
                category = "chinese_comic";
                break;
            case 4:
                selectedTab = tabAnime;
                category = "anime";
                break;
        }
        
        if (selectedTab != null) {
            selectedTab.setTextColor(getResources().getColor(R.color.text_primary));
            // 更新ViewModel中的选中分类
            viewModel.getSelectedCategory().setValue(category);
        }
    }
    
    // 更新Tab指示器位置
    private void updateTabIndicator(String category) {
        // 计算指示器位置
        int tabWidth = getResources().getDisplayMetrics().widthPixels / 5;
        int position = 0;
        
        switch (category) {
            case "selected":
                position = 0;
                break;
            case "portrait":
                position = 1;
                break;
            case "art":
                position = 2;
                break;
            case "chinese_comic":
                position = 3;
                break;
            case "anime":
                position = 4;
                break;
        }
        
        // 动画移动指示器
        tabIndicator.animate()
                .translationX(position * tabWidth + (tabWidth - tabIndicator.getWidth()) / 2)
                .setDuration(200)
                .start();
    }
    
    // 设置做同款按钮
    private void setupSameStyleButtons() {
        btnCatSunglasses.setOnClickListener(v -> 
            onSampleClick("戴着墨镜的猫咪在游艇上喝橙汁", "精选"));
            
        btnCatCity.setOnClickListener(v -> 
            onSampleClick("橘猫在上海外滩夜景前自拍", "精选"));
            
        btnGirlStudent.setOnClickListener(v -> 
            onSampleClick("日系校园风格的短发女生笑容", "人像摄影"));
            
        btnGirlPortrait.setOnClickListener(v -> 
            onSampleClick("唯美人像摄影风格的女生侧脸", "艺术"));
    }
    
    // 点击样本
    private void onSampleClick(String prompt, String style) {
        // 跳转到绘画界面，传递prompt
        Intent intent = new Intent(this, DrawingActivity.class);
        intent.putExtra("prompt", prompt);
        intent.putExtra("style", style);
        startActivity(intent);
    }
    
    // 设置语音按钮
    private void setupVoiceButton() {
        btnVoiceInput.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchY = event.getRawY();
                    startVoiceRecording();
                    return true;
                    
                case MotionEvent.ACTION_MOVE:
                    float deltaY = initialTouchY - event.getRawY();
                    if (deltaY > 100) {
                        btnVoiceInput.setText("松手取消");
                    } else {
                        btnVoiceInput.setText("松手发送");
                    }
                    return true;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float finalDeltaY = initialTouchY - event.getRawY();
                    if (finalDeltaY > 100) {
                        cancelVoiceRecording();
                    } else {
                        stopVoiceRecording();
                    }
                    btnVoiceInput.setText("按住说话");
                    return true;
            }
            return false;
        });
    }
    
    // 开始语音录制
    private void startVoiceRecording() {
        isVoiceRecording = true;
        btnVoiceInput.setBackgroundResource(R.drawable.bg_voice_recording_active);
        // TODO: 实际的语音录制逻辑
        showToast("开始录音");
    }
    
    // 停止语音录制
    private void stopVoiceRecording() {
        isVoiceRecording = false;
        btnVoiceInput.setBackgroundResource(R.drawable.bg_voice_recording);
        // TODO: 处理录音结果，跳转到绘画界面
        showToast("录音结束");
        
        // 模拟跳转到绘画界面
        Intent intent = new Intent(this, DrawingActivity.class);
        intent.putExtra("prompt", "语音输入的内容");
        startActivity(intent);
    }
    
    // 取消语音录制
    private void cancelVoiceRecording() {
        isVoiceRecording = false;
        btnVoiceInput.setBackgroundResource(R.drawable.bg_voice_recording);
        showToast("录音已取消");
    }
    
    // 设置底部导航
    private void setupBottomNavigation() {
        navChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigation", "chat");
            startActivity(intent);
            finish();
        });
        
        navAgent.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigation", "agent");
            startActivity(intent);
            finish();
        });
        
        navDrawing.setOnClickListener(v -> {
            // 当前页面，不做操作
        });
        
        navMeeting.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("selected_tab", 3); // Meeting tab index
            startActivity(intent);
            finish();
        });
        
        navUser.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserProfileActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    // 更新样本图片
    private void updateSampleImages(List<DrawingSampleDto> samples) {
        // TODO: 根据样本数据更新图片
        // 这里可以使用Glide加载网络图片
    }
}