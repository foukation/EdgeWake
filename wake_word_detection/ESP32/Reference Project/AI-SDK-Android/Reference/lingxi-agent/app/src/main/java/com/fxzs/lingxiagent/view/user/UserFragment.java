package com.fxzs.lingxiagent.view.user;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.billing.BillingManager;
import com.fxzs.lingxiagent.model.billing.callback.BillingCallback;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.user.UserUtil;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;
import com.fxzs.lingxiagent.view.bill.BillingWebActivity;
import com.fxzs.lingxiagent.view.chat.HistoryContainActivity;
import com.fxzs.lingxiagent.view.common.BaseFragment;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.common.WebViewActivity;
import com.fxzs.lingxiagent.view.guide.GuideActivity;
import com.fxzs.lingxiagent.viewmodel.user.VMUserProfile;

import java.io.File;

import timber.log.Timber;

public class UserFragment extends BaseFragment<VMUserProfile> {
    
    private FrameLayout layoutUserInfo;
    private ImageView ivAvatar;
    private TextView tvUsername;
    private TextView tvUserId;
    private LinearLayout layoutSettings;
    private LinearLayout layoutPptGeneration;
    private LinearLayout layoutAbout;
    private LinearLayout layoutHelpFeedback;
    private LinearLayout layoutHistory;
    private LinearLayout layoutBill;
    private TextView billText;
    private View viewUpdateDot;
    // private LinearLayout layoutLogout;
    private LinearLayout rlAgreementPri1;
    private LinearLayout rlAgreementPri2;
    private LinearLayout rlAgreementUser;
    private LinearLayout rlAgreementShareList;
    private LinearLayout rlAgreementUserList;
    private LinearLayout layoutUserGuide;

    private String billToken = "";
    private String deviceList = "";

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_user;
    }
    
    @Override
    protected Class<VMUserProfile> getViewModelClass() {
        return VMUserProfile.class;
    }
    
    @Override
    protected void initializeViews(View view) {
        layoutUserInfo = findViewById(R.id.layout_user_info);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvUsername = findViewById(R.id.tv_username);
        tvUserId = findViewById(R.id.tv_user_id);
        layoutSettings = findViewById(R.id.layout_settings);
        layoutPptGeneration = findViewById(R.id.layout_ppt_generation);
        layoutAbout = findViewById(R.id.layout_about);
        layoutHelpFeedback = findViewById(R.id.layout_help_feedback);
        layoutHistory = findViewById(R.id.layout_history);
        layoutBill = findViewById(R.id.layout_bill);
        billText = findViewById(R.id.bill_text);
        viewUpdateDot = findViewById(R.id.view_update_dot);
        layoutUserGuide = findViewById(R.id.layout_user_guide);
      //  layoutLogout = findViewById(R.id.layout_logout);
        
        layoutUserInfo.setOnClickListener(v -> navigateToAccountInfo());
        layoutSettings.setOnClickListener(v -> viewModel.navigateToSettings());
        layoutPptGeneration.setOnClickListener(v -> navigateToPptGeneration());
        layoutAbout.setOnClickListener(v -> viewModel.navigateToAbout());
        layoutHelpFeedback.setOnClickListener(v -> navigateToHelpFeedback());
        layoutHistory.setOnClickListener(v -> navigateToHistory());
        layoutBill.setOnClickListener(v -> navigateToBill());
        layoutUserGuide.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GuideActivity.class);
            intent.putExtra("from","setting");
            startActivity(intent);

        });
     //   layoutLogout.setOnClickListener(v -> performLogout());

        rlAgreementPri1 = findViewById(R.id.rl_agreement_pri1);
        rlAgreementPri2 = findViewById(R.id.rl_agreement_pri2);
        rlAgreementUser = findViewById(R.id.rl_agreement_user);
        rlAgreementUserList = findViewById(R.id.rl_agreement_userlist);
        rlAgreementShareList = findViewById(R.id.rl_agreement_sharelist);
        // 设置点击事件

        rlAgreementPri1.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), WebViewActivity.class);
            intent.putExtra("extra_url", Constants.PRIVACY_POLICY_DETAILED);
            intent.putExtra("extra_title", "隐私政策");
            startActivity(intent);
        });
        rlAgreementPri2.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), WebViewActivity.class);
            intent.putExtra("extra_url", Constants.PRIVACY_POLICY_SUMMARY);
            intent.putExtra("extra_title", "隐私政策");
            startActivity(intent);
        });
        rlAgreementUser.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), WebViewActivity.class);
            intent.putExtra("extra_url", Constants.USER_AGREEMENT);
            intent.putExtra("extra_title", "用户协议");
            startActivity(intent);
        });
        rlAgreementUserList.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), WebViewActivity.class);
            intent.putExtra("extra_url", Constants.PARTY_INFO_SHARE_LIST);
            intent.putExtra("extra_title", "收集个人信息明示清单");
            startActivity(intent);
        });
        rlAgreementShareList.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), WebViewActivity.class);
            intent.putExtra("extra_url", Constants.PERSONAL_INFO_LIST);
            intent.putExtra("extra_title", "第三方信息共享清单");
            startActivity(intent);
        });
    }

    @Override
    protected void setupDataBinding() {
        DataBindingUtils.bindTextView(tvUsername, viewModel.getUsername(), this);
        // UserProfile ViewModel doesn't have getUserId, using phone instead
        DataBindingUtils.bindTextView(tvUserId, viewModel.getPhone(), this);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 每次返回"我的"页面时重新加载用户信息，确保显示最新的信息
        loadLocalUserProfile();
        // 获取最新版本信息
        viewModel.fetchAppUpgradeInfo(getActivity());

        // 初始化权益包模块
        BillingManager.getInstance().start(new BillingCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onSentPackageInfo(String status, String token, String device) {
                billToken = token;
                deviceList = device;

                layoutBill.setVisibility(android.view.View.VISIBLE);
                billText.setText(status);

                int color;

                if (status.contains("已开通")) {
                    color = Color.parseColor("#4A7BFF");
                } else if (status.contains("即将到期")) {
                    color = Color.parseColor("#fa8c16");
                } else if (status.contains("已过期")) {
                    color = Color.parseColor("#FF8F8F");
                } else if (status.contains("未开通")) {
                    color = Color.parseColor("#4A7BFF");
                } else {
                    color = Color.parseColor("#999999");
                }

                billText.setTextColor(color);

                Timber.tag("Billing").d("权益包状态：" + status);
                Timber.tag("Billing").d("权益包token：" + token);
                Timber.tag("Billing").d("权益包设备：" + device);
            }

            @SuppressLint("TimberArgCount")
            @Override
            public void onFail(String msg) {
                Timber.tag("Billing").d("Billing", "User页面调用失败: " + msg);
            }

            @Override
            public void onNoDevice() {
                layoutBill.setVisibility(android.view.View.GONE);
                Timber.tag("Billing").d("设备未注册");
            }
        }, true);
    }

    private void loadLocalUserProfile() {
        // 优先加载本地的数据
        String avatarUrl = SharedPreferencesUtil.getUserAvatar();
        loadAvatarUrl(avatarUrl);
        String userId = SharedPreferencesUtil.getUserIdStr();
        String nickName = SharedPreferencesUtil.getUserName();
        tvUsername.setText(nickName.isEmpty() ? "用户" + userId : nickName);
        String mobile = SharedPreferencesUtil.getUserPhone();
        tvUserId.setText(UserUtil.formatPhone(mobile));
    }
    
    @Override
    protected void setupObservers() {
        // 观察头像URL变化
        viewModel.getAvatarUrl().observe(getViewLifecycleOwner(), avatarUrl ->
            loadAvatarUrl(avatarUrl)
        );
        
        viewModel.getNavigationTarget().observe(getViewLifecycleOwner(), target -> {
            if (target == null) return;
            
            switch (target) {
                case VMUserProfile.NAV_SETTINGS:
                    Intent settingsIntent = new Intent(getActivity(), UserAppSettingsActivity.class);
                    startActivity(settingsIntent);
                    break;
                case VMUserProfile.NAV_ABOUT:
                    Intent aboutIntent = new Intent(getActivity(), AboutAppActivity.class);
                    startActivity(aboutIntent);
                    break;
                case VMUserProfile.NAV_HELP:
                    Intent helpIntent = new Intent(getActivity(), HelpCenterActivity.class);
                    startActivity(helpIntent);
                    break;
            }
            
            viewModel.clearNavigationTarget();
        });
        // 观察版本信息
        viewModel.getVersionInfo().observe(this, versionInfo -> {
            if (versionInfo != null) {
                // 如果需要更新，显示红点而不是弹窗
                if (TextUtils.isEmpty(versionInfo.getDownloadUrl())) {
                    // 隐藏红点
                    viewUpdateDot.setVisibility(android.view.View.GONE);
                } else {
                    // 显示红点
                    viewUpdateDot.setVisibility(android.view.View.VISIBLE);
//                    if (versionInfo.getUpdateMode() == 1) {
//                        UpgradeHelper.showUpgradeDialog(this, versionInfo);
//                    }
                }
            }
        });
    }
    
    private void loadAvatarUrl(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            // 处理本地文件路径
            Object loadUrl = avatarUrl;
            if (avatarUrl.startsWith("file://")) {
                loadUrl = new File(avatarUrl.substring(7));
            }

            if (getContext() != null) {
                ImageUtil.netCircle(getContext(), String.valueOf(loadUrl),ivAvatar);
//                Glide.with(getContext())
//                        .load(loadUrl)
//                        .placeholder(R.drawable.icon_user_head)
//                        .error(R.drawable.icon_user_head)
//                        .transform(new CenterCrop(), new RoundedCorners(UserUtil.dp2px(getContext(), 12.8f)))
//                        .into(ivAvatar);
            }

        }
    }
    
    private void navigateToHelpFeedback() {
        Intent intent = new Intent(getActivity(), FeedbackActivity.class);
        startActivity(intent);
    }
    
    private void navigateToAccountInfo() {
        Intent intent = new Intent(getActivity(), AccountInfoActivity.class);
        startActivity(intent);
    }

    private void navigateToHistory() {
        startActivity(new Intent(getActivity(), HistoryContainActivity.class));
//        Timber.tag("DrawingFragment").d( "showHistoryBottomSheet called");
//        try {
//            HistoryBottomSheetFragment bottomSheet = HistoryBottomSheetFragment.newInstance( VMHistory.TAB_CHAT);
//            // 传递绘画tab索引，默认选中绘画历史
//            bottomSheet.show(getChildFragmentManager(), "HistoryBottomSheet");
//            Timber.tag("DrawingFragment").d( "BottomSheet shown successfully with drawing tab selected");
//        } catch (Exception e) {
//            android.util.Log.e("DrawingFragment", "Error showing bottom sheet", e);
//        }
    }

    private void navigateToBill() {

        if (TextUtils.isEmpty(billToken)) {
            Toast.makeText(getActivity(), "权益平台token正在查询，请稍等", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(deviceList)) {
            Toast.makeText(getActivity(), "权益平台设备信息正在同步，请稍等", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity(), BillingWebActivity.class);
        intent.putExtra("billToken", billToken);
        intent.putExtra("deviceList", deviceList);
        startActivity(intent);
    }

    private void navigateToPptGeneration() {
        Intent intent = new Intent(getActivity(), com.fxzs.lingxiagent.view.ppt.PptTopicInputActivity.class);
        startActivity(intent);
    }
}
