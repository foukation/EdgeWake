package com.fxzs.lingxiagent.view.user;

import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.drawing.DrawingGalleryActivity;
import com.fxzs.lingxiagent.viewmodel.user.VMUserProfile;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends BaseActivity<VMUserProfile> {
    
    // 用户信息区域
    private CircleImageView ivAvatar;
    private TextView tvUsername;
    private TextView tvPhone;
    private RelativeLayout layoutUserInfo;
    
    // 功能列表
    private RelativeLayout layoutSettings;
    private RelativeLayout layoutAbout;
    private RelativeLayout layoutHelp;
    
    // 底部导航栏
    private LinearLayout navDialog;
    private LinearLayout navAgent;
    private LinearLayout navDrawing;
    private LinearLayout navMeeting;
    private LinearLayout navProfile;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_user_profile;
    }
    
    @Override
    protected Class<VMUserProfile> getViewModelClass() {
        return VMUserProfile.class;
    }
    
    @Override
    protected void initializeViews() {
        // 用户信息区域
        ivAvatar = findViewById(R.id.iv_avatar);
        tvUsername = findViewById(R.id.tv_username);
        tvPhone = findViewById(R.id.tv_phone);
        layoutUserInfo = findViewById(R.id.layout_user_info);
        
        // 功能列表
        layoutSettings = findViewById(R.id.layout_settings);
        layoutAbout = findViewById(R.id.layout_about);
        layoutHelp = findViewById(R.id.layout_help);
        
        // 设置点击事件
        layoutUserInfo.setOnClickListener(v -> {
            Intent accountIntent = new Intent(this, AccountInfoActivity.class);
            startActivity(accountIntent);
        });
        
        layoutSettings.setOnClickListener(v -> viewModel.navigateToSettings());
        layoutAbout.setOnClickListener(v -> viewModel.navigateToAbout());
        layoutHelp.setOnClickListener(v -> viewModel.navigateToHelp());
        
        // 初始化底部导航栏
        setupBottomNavigation();
    }
    
    private void setupBottomNavigation() {
        // 查找底部导航栏控件
        RelativeLayout navDialog = findViewById(R.id.nav_dialog);
        RelativeLayout navAgent = findViewById(R.id.nav_agent);
        RelativeLayout navDrawing = findViewById(R.id.nav_drawing);
        RelativeLayout navMeeting = findViewById(R.id.nav_meeting);
        RelativeLayout navProfile = findViewById(R.id.nav_profile);
        
        if (navDialog != null && navAgent != null && navDrawing != null && navMeeting != null && navProfile != null) {
            
            // 设置当前选中项
            navProfile.setSelected(true);
            
            // 设置点击事件
            navDialog.setOnClickListener(v -> {
                showToast("对话功能开发中");
            });
            
            navAgent.setOnClickListener(v -> {
                showToast("智能体功能开发中");
            });
            
            navDrawing.setOnClickListener(v -> {
                // 导航到绘画画廊页面
                Intent intent = new Intent(this, DrawingGalleryActivity.class);
                startActivity(intent);
                finish();
            });
            
            navMeeting.setOnClickListener(v -> {
                showToast("会议功能开发中");
            });
            
            navProfile.setOnClickListener(v -> {
                // 已经在用户中心页面，不做操作
            });
        }
    }
    
    @Override
    protected void setupDataBinding() {
        // 绑定用户信息
        DataBindingUtils.bindTextView(tvUsername, viewModel.getUsername(), this);
        DataBindingUtils.bindTextView(tvPhone, viewModel.getPhone(), this);
    }
    
    @Override
    protected void setupObservers() {
        // 观察头像URL变化
        viewModel.getAvatarUrl().observe(this, avatarUrl -> {
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                // 处理本地文件路径
                Object loadUrl = avatarUrl;
                if (avatarUrl.startsWith("file://")) {
                    loadUrl = new File(avatarUrl.substring(7));
                }
                
                Glide.with(this)
                    .load(loadUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(ivAvatar);
            }
        });
        
        // 观察导航事件
        viewModel.getNavigationTarget().observe(this, target -> {
            if (target == null) return;
            
            switch (target) {
                case VMUserProfile.NAV_SETTINGS:
                    Intent settingsIntent = new Intent(this, UserAppSettingsActivity.class);
                    startActivity(settingsIntent);
                    break;
                case VMUserProfile.NAV_ABOUT:
                    Intent aboutIntent = new Intent(this, AboutAppActivity.class);
                    startActivity(aboutIntent);
                    break;
                case VMUserProfile.NAV_HELP:
                    Intent helpIntent = new Intent(this, HelpCenterActivity.class);
                    startActivity(helpIntent);
                    break;
            }
            
            // 清除导航事件
            viewModel.clearNavigationTarget();
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 页面恢复时刷新用户信息
        viewModel.loadUserProfile();
    }
}