package com.fxzs.lingxiagent.view.user;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.EventConstants;
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.TrackerUtils;
import com.fxzs.lingxiagent.model.auth.AuthHelper;
import com.fxzs.lingxiagent.receiver.LingxiAskWidgetProvider;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.WakeUpPermissionHelper;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.guide.GuideActivity;
import com.fxzs.lingxiagent.viewmodel.user.VMUserSettings;

import timber.log.Timber;

public class UserAppSettingsActivity extends BaseActivity<VMUserSettings> {

    // 顶部栏
    private ImageView ivBack;

    // 设置项
    private LinearLayout layoutModel;
    private LinearLayout layoutSecurity;
    private LinearLayout layoutLanguage;
    private TextView tvModelName;
    private TextView tvLanguage;
    private View layout_voice;

    // 开关控件（修正拼写：switchKeyboardWakeup）
    private SwitchCompat switchWakeup, switchPowerWakeup, switchKeyboardWakeup;

    // 标识当前处理中的开关（用于权限回调）
    private SwitchCompat currentProcessingSwitch;
    private ImageView ivUserGuide;

    private static final String PREF_NAME = "user_settings";
    private static final String KEY_WAKEUP_ENABLED = "wakeup_enabled";

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_user_app_settings;
    }

    @Override
    protected Class<VMUserSettings> getViewModelClass() {
        return VMUserSettings.class;
    }

    @Override
    protected void initializeViews() {
        // 绑定基础控件
        ivBack = findViewById(R.id.iv_back);
        layoutModel = findViewById(R.id.layout_model);
        layoutSecurity = findViewById(R.id.layout_security);
        layoutLanguage = findViewById(R.id.layout_language);
        tvModelName = findViewById(R.id.tv_model_name);
        tvLanguage = findViewById(R.id.tv_language);
        layout_voice = findViewById(R.id.layout_voice);
        ivUserGuide = findViewById(R.id.iv_user_guide);

        // 基础点击事件
        ivBack.setOnClickListener(v -> finish());
        layoutModel.setOnClickListener(v -> startActivity(new Intent(this, ModelSelectionActivity.class)));
        layoutSecurity.setOnClickListener(v -> startActivity(new Intent(this, AccountSafetyActivity.class)));
        layoutLanguage.setOnClickListener(v -> startActivity(new Intent(this, LanguageSettingsActivity.class)));
        layout_voice.setOnClickListener(v -> startActivity(new Intent(this, VoiceSettingsActivity.class)));
        ivUserGuide.setOnClickListener(v -> {
            Intent intent = new Intent(this, GuideActivity.class);
            intent.putExtra("from","setting");
            startActivity(intent);
        });

        // 绑定开关控件
        switchWakeup = findViewById(R.id.switch_wakeup);
        switchPowerWakeup = findViewById(R.id.switch_power_wakeup);
        switchKeyboardWakeup = findViewById(R.id.switch_keybord_wakeup); // 兼容xml中的拼写错误
        if (!AuthHelper.getInstance().isLogin()) {
            layout_voice.setVisibility(View.GONE);
            layoutLanguage.setVisibility(View.GONE);
            layoutSecurity.setVisibility(View.GONE);
        } else {
            layout_voice.setVisibility(View.VISIBLE);
            layoutLanguage.setVisibility(View.VISIBLE);
            layoutSecurity.setVisibility(View.VISIBLE);
        }
        // ========== 主唤醒开关（需要通知权限） ==========
        switchWakeup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentProcessingSwitch = switchWakeup;
                checkPermissionsAndEnable(switchWakeup, true); // 带通知权限检查
            } else {
                WakeUpPermissionHelper.setWakeUpEnabled(this, false);
                WakeUpPermissionHelper.toggleWakeUpService(this, false);
                TrackerUtils.trackWakeAbortEvent(false, EventConstants.WakeUpManagement.VOICE_WAKE_CREATE);
            }
        });

        // ========== 电源键唤醒开关（无需通知权限） ==========
        switchPowerWakeup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentProcessingSwitch = switchPowerWakeup;
                checkPermissionsAndEnable(switchPowerWakeup, false); // 不带通知权限检查
            } else {
                WakeUpPermissionHelper.setWakeUpPowerEnabled(this, false);
                TrackerUtils.trackWakeAbortEvent(false, EventConstants.WakeUpManagement.POWER_WAKE_CREATE);

            }
        });

        // ========== 键盘唤醒开关（无需通知权限） ==========
        switchKeyboardWakeup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentProcessingSwitch = switchKeyboardWakeup;
                checkPermissionsAndEnable(switchKeyboardWakeup, false); // 不带通知权限检查
            } else {
                WakeUpPermissionHelper.setKeyWakeupKeybordEnabled(this, false);
                TrackerUtils.trackWakeAbortEvent(false, EventConstants.WakeUpManagement.KEYBOARD_WAKE_CREATE);

            }
        });

        // 初始化所有开关状态（带权限校验）
        initSwitchStates();
    }

    /**
     * 初始化开关状态：只有权限满足时才显示"开启"状态
     */
    private void initSwitchStates() {
        // 主唤醒开关（检查全部权限：录音+悬浮窗+通知）
        boolean isMainWakeupEnabled = WakeUpPermissionHelper.isWakeUpEnabled(this);
        switchWakeup.setChecked(isMainWakeupEnabled && checkAllPermissions(true));

        // 电源键唤醒开关（检查基础权限：录音+悬浮窗）
        boolean isPowerWakeupEnabled = WakeUpPermissionHelper.isWakeUpPowerEnabled(this);
        switchPowerWakeup.setChecked(isPowerWakeupEnabled && checkAllPermissions(false));

        // 键盘唤醒开关（检查基础权限：录音+悬浮窗）
        boolean isKeyboardWakeupEnabled = WakeUpPermissionHelper.isWakeUpKeyBordEnabled(this);
        switchKeyboardWakeup.setChecked(isKeyboardWakeupEnabled && checkAllPermissions(false));
    }

    /**
     * 权限检查通用方法
     * @param needNotification 是否需要检查通知权限（仅主唤醒开关需要）
     */
    private boolean checkAllPermissions(boolean needNotification) {
        // 1. 必检：录音权限
        if (!WakeUpPermissionHelper.checkRecordAudioPermission(this)) {
            return false;
        }

        // 2. 可选：通知权限（仅主唤醒开关需要）
        if (needNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!WakeUpPermissionHelper.checkPostNotifications(this)) {
                return false;
            }
        }

        // 3. 必检：悬浮窗权限
        if (!WakeUpPermissionHelper.checkFloatingWindowPermission(this)) {
            return false;
        }

        return true;
    }

    /**
     * 权限引导+开关启用通用逻辑
     * @param targetSwitch 目标开关
     * @param needNotification 是否需要通知权限
     */
    private void checkPermissionsAndEnable(SwitchCompat targetSwitch, boolean needNotification) {
        // 前置校验：如果开关已被取消勾选，直接返回
//        if (!targetSwitch.isChecked()) {
//            return;
//        }

        // ========== 步骤1：检查录音权限 ==========
        if (!WakeUpPermissionHelper.checkRecordAudioPermission(this)) {
            AppPermissionRequestManager.requestAudioPermission(
                    this,
                    WakeUpPermissionHelper.REQUEST_CODE_RECORD_AUDIO,
                    AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_WAKEUP,
                    targetSwitch
            );
            return;
        }

        // ========== 步骤2：检查通知权限（仅主唤醒开关需要） ==========
        if (needNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!WakeUpPermissionHelper.checkPostNotifications(this)) {
                AppPermissionRequestManager.requestNotificationsPermission(
                        this,
                        WakeUpPermissionHelper.REQUEST_CODE_POST_NOTIFICATIONS,
                        "请授权通知权限，以保证唤醒服务正常使用"
                );
                return;
            }
        }

        // ========== 步骤3：检查悬浮窗权限 ==========
        if (!WakeUpPermissionHelper.checkFloatingWindowPermission(this)) {
            AppPermissionRequestManager.requestOverlayPermissionDialog(this, new CommonDialog.OnDialogClickListener() {
                @Override
                public void onConfirm() {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, WakeUpPermissionHelper.REQUEST_CODE_FLOATING);
                }

                @Override
                public void onCancel() {
                    targetSwitch.setChecked(false); // 权限取消，开关回退
                }
            });
            return;
        }

        // ========== 所有权限满足，启用对应功能 ==========
        enableSwitchFunction(targetSwitch);
    }

    /**
     * 根据开关类型启用对应功能
     */
    private void enableSwitchFunction(SwitchCompat targetSwitch) {
        if (targetSwitch == switchWakeup) {
            // 主唤醒开关：保存状态+启动服务
            WakeUpPermissionHelper.setWakeUpEnabled(this, true);

            // 检查服务是否已在运行，避免重复启动
            if (!WakeUpPermissionHelper.isWakeUpServiceRunning(this)) {
                WakeUpPermissionHelper.toggleWakeUpService(this, true);
                TrackerUtils.trackWakeAbortEvent(true, EventConstants.WakeUpManagement.VOICE_WAKE_CREATE);

            } else {
                Timber.tag("UserSettings").d("WakeUpService 已在运行，无需重复启动");
            }
        } else if (targetSwitch == switchPowerWakeup) {
            // 电源键唤醒：仅保存状态
            WakeUpPermissionHelper.setWakeUpPowerEnabled(this, true);
            TrackerUtils.trackWakeAbortEvent(true, EventConstants.WakeUpManagement.POWER_WAKE_CREATE);

        } else if (targetSwitch == switchKeyboardWakeup) {
            // 键盘唤醒：仅保存状态
            WakeUpPermissionHelper.setKeyWakeupKeybordEnabled(this, true);
            TrackerUtils.trackWakeAbortEvent(true, EventConstants.WakeUpManagement.KEYBOARD_WAKE_CREATE);
        }
    }

    /**
     * 同步唤醒服务状态：只要有一个开关开启，就保持服务运行
     */
    private void syncWakeupServiceState() {
        boolean anySwitchEnabled = switchWakeup.isChecked();

        // 如果要启动，先检查是否已在运行
        if (anySwitchEnabled && WakeUpPermissionHelper.isWakeUpServiceRunning(this)) {
            return;
        }

        WakeUpPermissionHelper.toggleWakeUpService(this, anySwitchEnabled);
    }

    @Override
    protected void setupDataBinding() {
        DataBindingUtils.bindTextView(tvModelName, viewModel.getSelectedModel(), this);
        DataBindingUtils.bindTextView(tvLanguage, viewModel.getSelectedLanguage(), this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 刷新数据+重置状态栏
        viewModel.refreshDisplayData();
        resetStatusBar();

        // 每次返回页面都重新校验权限和开关状态
        initSwitchStates();
    }

    /**
     * 重置状态栏样式（白色背景+黑色文字）
     */
    public void resetStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));

            // 延迟确保生效
            getWindow().getDecorView().postDelayed(() -> {
                getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            }, 100);

            // Android M+ 适配状态栏文字颜色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    @Override
    protected void setupObservers() {
        // 模型选择弹窗监听
        viewModel.getShowModelDialog().observe(this, show -> {
            if (show != null && show) {
                showModelSelectionDialog();
            }
        });

        // 语言选择弹窗监听
        viewModel.getShowLanguageDialog().observe(this, show -> {
            if (show != null && show) {
                showLanguageSelectionDialog();
            }
        });

        // 导航事件监听
        viewModel.getNavigationTarget().observe(this, target -> {
            if (target == null) return;
            if (target == VMUserSettings.NAV_SECURITY) {
                startActivity(new Intent(this, AccountSafetyActivity.class));
            }
            viewModel.clearNavigationTarget();
        });
    }

    /**
     * 显示模型选择弹窗
     */
    private void showModelSelectionDialog() {
        String[] models = viewModel.getAvailableModels();
        String currentModel = viewModel.getSelectedModel().get();
        int checkedItem = -1;

        for (int i = 0; i < models.length; i++) {
            if (models[i].equals(currentModel)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("选择大模型")
                .setSingleChoiceItems(models, checkedItem, (dialog, which) -> {
                    viewModel.selectModel(models[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示语言选择弹窗
     */
    private void showLanguageSelectionDialog() {
        String[] languages = viewModel.getAvailableLanguages();
        String currentLanguage = viewModel.getSelectedLanguage().get();
        int checkedItem = -1;

        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(currentLanguage)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("选择语音识别语言")
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    viewModel.selectLanguage(languages[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 权限请求回调（处理录音/通知权限）
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 检查权限是否全部授予
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        // 权限被拒绝：关闭当前处理的开关
        if (!allGranted && currentProcessingSwitch != null) {
            currentProcessingSwitch.setChecked(false);
            return;
        }

        // 权限被授予：继续检查下一个权限
        if (currentProcessingSwitch == switchWakeup) {
            checkPermissionsAndEnable(switchWakeup, true);
        } else if (currentProcessingSwitch == switchPowerWakeup) {
            checkPermissionsAndEnable(switchPowerWakeup, false);
        } else if (currentProcessingSwitch == switchKeyboardWakeup) {
            checkPermissionsAndEnable(switchKeyboardWakeup, false);
        }
    }

    /**
     * 悬浮窗权限设置回调
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == WakeUpPermissionHelper.REQUEST_CODE_FLOATING) {
            LingxiAskWidgetProvider.refreshAllWidgets(this);
            // 回到页面后重新检查权限并尝试启用开关
            if (currentProcessingSwitch == switchWakeup) {
                checkPermissionsAndEnable(switchWakeup, true);
            } else if (currentProcessingSwitch == switchPowerWakeup) {
                checkPermissionsAndEnable(switchPowerWakeup, false);
            } else if (currentProcessingSwitch == switchKeyboardWakeup) {
                checkPermissionsAndEnable(switchKeyboardWakeup, false);
            }
        }
    }
}