package com.fxzs.lingxiagent.lingxi.help;


import static com.fxzs.lingxiagent.lingxi.help.FunctionHelpConstants.FUNCTION_HELP_CONFIG_JSON;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.main.utils.GlobalUtils;
import com.fxzs.lingxiagent.lingxi.main.utils.ScreenUtils;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.main.VMEmpty;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import timber.log.Timber;

public class FunctionHelpActivity extends BaseActivity {
    private static final String TAG = "FunctionHelpActivity";
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private HelpTabsConfig tabsConfig = new HelpTabsConfig(new ArrayList<>());
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private int mClickNum = 0;
    //调试功能，是否主动进行文件访问权限申请并加载手机存储的json文件
    private boolean READ_EXTERNAL =true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_function_help;
    }

    @Override
    protected Class getViewModelClass() {
        return VMEmpty.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {
        // Load tabs configuration from JSON
        tabsConfig = loadFunctionTabsConfig(false);
        // Setup UI
        initView();
        ScreenUtils.INSTANCE.immersiveShow(this);
    }

    @Override
    protected void setupObservers() {

    }

    private HelpTabsConfig loadFunctionTabsConfig(boolean readExternal) {
        try {
            String function_help = "";
            if(readExternal) {
                function_help = GlobalUtils.readExternalJson(FUNCTION_HELP_CONFIG_JSON);
                Timber.tag(TAG).d("readExternalJson %s", function_help);
            }
            if(TextUtils.isEmpty(function_help)) {
                InputStream is = getAssets().open(FUNCTION_HELP_CONFIG_JSON);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if(!TextUtils.isEmpty(line)) {
                        sb.append(line);
                    }
                }
                Timber.tag(TAG).d("loadAssetsTabsConfig %s", sb.toString());
                return HelpTabsConfig.fromJson(new JSONObject(sb.toString()));
            } else {
                return HelpTabsConfig.fromJson(new JSONObject(function_help));
            }
        } catch (IOException | JSONException e) {
            Timber.tag(TAG).e("loadAssetsTabsConfig Exception %s", e.toString());
            return new HelpTabsConfig(new ArrayList<>());
        }
    }

    private void hasStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
        ) {
            AppPermissionRequestManager.requestExternalStoragePermission(this, PERMISSION_REQUEST_CODE,"请授权手机存储，以便功能说明的存储");
        } else {
            // Load tabs configuration from JSON
            refreshTabsConfig();
        }
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Load tabs configuration from JSON
            refreshTabsConfig();
        }
    }

    // 权限更新后加载布局
    private void refreshTabsConfig() {
        tabsConfig = loadFunctionTabsConfig(true);
        // 更新 ViewPager
        HelpTabPagerAdapter newAdapter = new HelpTabPagerAdapter(this, tabsConfig.getTabs());
        viewPager.setAdapter(newAdapter);

        // 更新 TabLayout 标题（需重新绑定 Mediator）
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(tabsConfig.getTabs().get(position).getTitle());
        }).attach();
    }


    private void initView() {
        ImageView closeIcon = findViewById(R.id.iv_back);
        closeIcon.setOnClickListener((view) -> finish());

        TextView title = findViewById(R.id.tv_title);
        title.setOnClickListener(view -> {
            mClickNum ++;
            if (READ_EXTERNAL && mClickNum == 10) {
                hasStoragePermission();
            }
        });
        setupViewPager();
        setupTabLayout();
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.viewPager);
        RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
        //完全禁用过度滚动
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        HelpTabPagerAdapter adapter = new HelpTabPagerAdapter(this, tabsConfig.getTabs());
        viewPager.setAdapter(adapter);
    }

    private void setupTabLayout() {
        tabLayout = findViewById(R.id.tabLayout);
        // 设置下滑线颜色
        tabLayout.setSelectedTabIndicatorColor(getColor(R.color.tag_indicator_selected));
        // 设置文本颜色选择器
        tabLayout.setTabTextColors(
                getColor(R.color.tag_text_unselected), // 未选中颜色
                getColor(R.color.tag_text_selected) // 选中颜色
        );
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(tabsConfig.getTabs().get(position).getTitle());
        }).attach();
    }
}