package com.fxzs.lingxiagent.view.user;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.user.VMHelpCenter;

public class HelpCenterActivity extends BaseActivity<VMHelpCenter> {
    
    private ImageView ivBack;
    private RecyclerView rvFaqList;
    private HelpFAQAdapter faqAdapter;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_help_center;
    }
    
    @Override
    protected Class<VMHelpCenter> getViewModelClass() {
        return VMHelpCenter.class;
    }
    
    @Override
    protected void initializeViews() {
        // 初始化控件
        ivBack = findViewById(R.id.iv_back);
        rvFaqList = findViewById(R.id.rv_faq_list);
        
        // 设置返回按钮
        ivBack.setOnClickListener(v -> finish());
        
        // 设置FAQ列表
        rvFaqList.setLayoutManager(new LinearLayoutManager(this));
        faqAdapter = new HelpFAQAdapter(this);
        faqAdapter.setOnItemClickListener((faq, position) -> {
            viewModel.onFAQClicked(faq);
        });
        rvFaqList.setAdapter(faqAdapter);
    }
    
    @Override
    protected void setupDataBinding() {
        // 此页面没有双向绑定需求
    }
    
    @Override
    protected void setupObservers() {
        // 观察FAQ列表
        viewModel.getFaqList().observe(this, faqs -> {
            if (faqs != null) {
                faqAdapter.setData(faqs);
            }
        });
        
        // 观察导航事件
        viewModel.getNavigationEvent().observe(this, faq -> {
            if (faq != null) {
                // TODO: 跳转到FAQ详情页
                showToast("查看: " + faq.getTitle());
            }
        });
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