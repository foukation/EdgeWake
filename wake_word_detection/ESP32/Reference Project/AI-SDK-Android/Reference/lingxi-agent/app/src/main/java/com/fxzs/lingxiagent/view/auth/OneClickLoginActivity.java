package com.fxzs.lingxiagent.view.auth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.cmic.sso.sdk.auth.TokenListener;
import com.fxzs.lingxiagent.JumpParameterManager;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.SplashActivity;
import com.fxzs.lingxiagent.model.auth.AuthHelper;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.receiver.LingxiGUIWidgetProvider;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.view.ServerSwitchActivity;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.common.LoginPromptDialog;
import com.fxzs.lingxiagent.view.common.WebViewActivity;
import com.fxzs.lingxiagent.viewmodel.auth.VMRegister;

import org.json.JSONObject;

import timber.log.Timber;

public class OneClickLoginActivity extends BaseActivity<VMRegister> {

    private static final int REQUEST_PHONE_PERMISSION = 1001;
    private static final String phonePermission = Manifest.permission.READ_PHONE_STATE;
    private int tabIndex = -1;

    private ImageView btnBack;
    private TextView tvRegister;
    private Button btnOneClickLogin;
    private Button btnSwitchLoginMethod;
    private CheckBox cbAgreement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SharedPreferencesUtil.isAgreePrivacy()) {
            Intent intent = new Intent(this, SplashActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        Intent intent = new Intent(this, LingxiGUIWidgetProvider.class);
        intent.setAction(LingxiGUIWidgetProvider.ACTION_REFRESH_WIDGET);
        sendBroadcast(intent);
        // 检查是否从首页跳转过来
        checkIfFromHomePage();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_one_click_login;
    }

    @Override
    protected Class<VMRegister> getViewModelClass() {
        return VMRegister.class;
    }

    @Override
    protected void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvRegister = findViewById(R.id.tvRegister);
        btnOneClickLogin = findViewById(R.id.btnOneClickLogin);
        btnSwitchLoginMethod = findViewById(R.id.btnSwitchLoginMethod);
        cbAgreement = findViewById(R.id.cb_agreement);

        // 检查并请求权限
//        AppPermissionRequestManager.requestReadPhoneStatePermission(this, REQUEST_PHONE_PERMISSION);


        // 设置点击事件（添加null检查）
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (tvRegister != null) {
            tvRegister.setOnClickListener(v -> {
                if (!SharedPreferencesUtil.getAuthStatus()){
                    GlobalToast.show(this,getString(R.string.device_auth_error),GlobalToast.Type.ERROR);
                    return;
                }
                Intent intent = new Intent(this, RegisterNewActivity.class);
                startActivity(intent);
            });
        }

        if (btnOneClickLogin != null) {
            btnOneClickLogin.setOnClickListener(v -> performOneClickLogin());
        }

        if (btnSwitchLoginMethod != null) {
            btnSwitchLoginMethod.setOnClickListener(v -> {
                if (!SharedPreferencesUtil.getAuthStatus()){
                    GlobalToast.show(this,getString(R.string.device_auth_error),GlobalToast.Type.ERROR);
                    return;
                }
                Intent intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
            });
        }

        // 协议点击事件
        findViewById(R.id.tv_cmcc_clause).setOnClickListener(v -> {
            WebViewActivity.start(this, Constants.CM_CONTACT_URL, getString(R.string.cm_certification_clause));
        });
        findViewById(R.id.tv_user_agreement).setOnClickListener(v -> {
            WebViewActivity.start(this, Constants.USER_AGREEMENT, getString(R.string.user_agreement));
        });

        findViewById(R.id.tv_privacy_policy).setOnClickListener(v -> {
            WebViewActivity.start(this, Constants.PRIVACY_POLICY_DETAILED, getString(R.string.privacy_policy));
        });

        // 检查网络连接
        if (!com.fxzs.lingxiagent.util.NetworkUtils.isNetworkAvailable(this)) {
            showToast("当前无网络连接，请检查后重试");
        }

        ImageView aboutAvatar = findViewById(R.id.ivLogo);
        aboutAvatar.setOnClickListener(v -> handleAvatarClick());
    }
    private int avatarClickCount = 0;
    private void handleAvatarClick() {
        avatarClickCount++;
        if (avatarClickCount == 5) {
            //doTargetAction();
            avatarClickCount = 0; // 重置，支持重复触发
            startActivity(new Intent(this, ServerSwitchActivity.class));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!SharedPreferencesUtil.isAgreePrivacy()) {
            return;
        }
        // 取电话号码掩码，提升用户体验
        tryUmcLoginPre();
    }

    @Override
    protected void setupDataBinding() {
        // 暂无需要绑定的数据
    }

    @Override
    protected void setupObservers() {
        // 观察登录结果
        viewModel.getRegisterResult().observe(this, success -> {
            if (success != null && success) {
                JumpParameterManager.INSTANCE.jumpTargetPage(OneClickLoginActivity.this);
                finish();
            }
        });

        // 观察加载状态
        viewModel.getLoading().observe(this, loading -> {
            if (btnOneClickLogin != null) {
                btnOneClickLogin.setEnabled(!loading);
                if (loading) {
                    btnOneClickLogin.setText("登录中...");
                } else {
                    btnOneClickLogin.setText("一键登录");
                }
            }
        });
    }


    private void performOneClickLogin() {
        if (!SharedPreferencesUtil.getAuthStatus()){
            GlobalToast.show(this,getString(R.string.device_auth_error),GlobalToast.Type.ERROR);
            return;
        }
        // 检查并请求权限
//        if (ContextCompat.checkSelfPermission(this, phonePermission) != PackageManager.PERMISSION_GRANTED) {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, phonePermission)) {
//                ActivityCompat.requestPermissions(this, new String[]{phonePermission, Manifest.permission.ACCESS_NETWORK_STATE}, REQUEST_PHONE_PERMISSION);
//            } else {
//                GlobalToast.show(this, "预取号码失败，请授予获取本机号码权限", GlobalToast.Type.ERROR);
//            }
//            return;
//        }

        // 检查必要的UI组件是否存在
        if (cbAgreement == null) {
            Timber.tag("OneClickLogin").e("cbAgreement is null");
            showToast("页面初始化失败，请重试");
            return;
        }

        // 检查是否同意协议
        if (!cbAgreement.isChecked()) {
            // 使用协议确认弹窗（带有可点击的链接）
            CommonDialog.showAgreementDialog(this, getString(R.string.login_user_agreement), getString(R.string.login_user_agreement_title),
                    new CommonDialog.OnDialogClickListener() {
                        @Override
                        public void onConfirm() {
                            cbAgreement.setChecked(true);
                            performOneClickLogin();
                        }

                        @Override
                        public void onCancel() {
                            // 用户点击不同意，不做任何操作
                        }
                    });
            return;
        }

        // 执行一键登录
        String operatorType = AuthHelper.getInstance().getOperatorType();
        if ("0".equals(operatorType)) {
            GlobalToast.show(this, "登录失败，您可切换其他方式登录", GlobalToast.Type.NORMAL);
            return;
        }
        if (!"1".equals(operatorType)) {
            GlobalToast.show(this, "登录失败，您可切换其他方式登录", GlobalToast.Type.NORMAL);
            return;
        }
        AuthHelper.getInstance().getTokenImp(new AuthListener(2));
    }

    // 检查权限，预取手机号码
    private void tryUmcLoginPre() {
        // 检查并请求权限
//        if (ContextCompat.checkSelfPermission(this, phonePermission) == PackageManager.PERMISSION_GRANTED) {
//            AuthHelper.getInstance().umcLoginPre(new AuthListener(1));
//        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, phonePermission)) {
//            ActivityCompat.requestPermissions(this, new String[]{phonePermission, Manifest.permission.ACCESS_NETWORK_STATE}, REQUEST_PHONE_PERMISSION);
//        } else {
//            GlobalToast.show(this, "预取号码失败，请授予获取本机号码权限", GlobalToast.Type.ERROR);
//        }
        AuthHelper.getInstance().umcLoginPre(new AuthListener(1));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PHONE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 取电话号码掩码，提升用户体验
                AuthHelper.getInstance().umcLoginPre(new AuthListener(1));
            }
        }
    }

    @Override
    protected void showToast(String message) {
        GlobalToast.show(this, message, GlobalToast.Type.NORMAL);
    }

    private void checkIfFromHomePage() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("from_home", false)) {
            // 从首页跳转过来，显示登录提示对话框
//            showLoginPromptDialog();
//            showToast("登录可体验完整功能");
            tabIndex = intent.getIntExtra("selected_tab", -1);
            GlobalToast.show(this, "登录可体验完整功能", GlobalToast.Type.NORMAL);
        }
    }

    private void showLoginPromptDialog() {
        LoginPromptDialog dialog = new LoginPromptDialog(this);
        dialog.show();

        // 2秒后自动关闭
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        }, 2000);
    }

    private final class AuthListener implements TokenListener {
        private final int mFeature;

        public AuthListener(int feature) {
            mFeature = feature;
        }

        /**
         * 认证登录回调接口
         *
         * @param jObj 回调响应参数
         *             resultCode：接口返回码，“103000”为成功
         *             resultDesc：返回码描述
         *             securityphone：电话号码掩码
         *             loginMethod：登录的方法
         *             operatortype：运营商类型：0 未知；1 移动；2 联通；3 电信
         *             usetimes：预取号使用的时间，单位毫秒
         *             traceId：SDK生成的本次会话标识id，用于排查问题，长度32位
         *             imageUrl：品牌图片的URL地址
         *             brand：品牌名称
         *             gsmLevel：品牌等级
         *             token：有效期2min，一次有效，同一用户（手机号）10分钟内获取token且未使用的数量不超过30个
         */
        @Override
        public void onGetTokenComplete(JSONObject jObj) {
            if (jObj != null) {
                switch (mFeature) {
                    case 1:
                        String securityPhone = jObj.optString("securityphone");
                        if (TextUtils.isEmpty(securityPhone)) {
                           // String resultDesc = jObj.optString("resultDesc");
                           // GlobalToast.show(OneClickLoginActivity.this, resultDesc, GlobalToast.Type.ERROR);
                        } else {
                            btnOneClickLogin.setText("使用" + securityPhone + "一键登录");
                        }
                        break;
                    case 2:
                        String token = jObj.optString("token");
                        if (TextUtils.isEmpty(token)) {
                           // String resultDesc = jObj.optString("resultDesc");
                           // GlobalToast.show(OneClickLoginActivity.this, resultDesc, GlobalToast.Type.ERROR);
                        } else {
                            // 使用token调用后端接口进行登录
                            viewModel.performOneClickLogin(token);
                        }
                        break;
                    case 3:
                        break;
                }
            }
        }
    }
}