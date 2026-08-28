package com.fxzs.lingxiagent.view.user;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.viewmodel.user.VMEditName;

public class EditNameActivity extends BaseActivity<VMEditName> {
    
    private ImageView ivBack;
    private TextView tvSave;
    private EditText etNickname;
    private TextView tvCharCount;
    
    @Override
    protected int getLayoutResource() {
        return R.layout.activity_edit_name;
    }
    
    @Override
    protected Class<VMEditName> getViewModelClass() {
        return VMEditName.class;
    }
    
    @Override
    protected void initializeViews() {
        // 设置状态栏颜色为白色，与背景一致，并保证内容不被遮挡
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));
            getWindow().getDecorView().postDelayed(() -> {
                getWindow().setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));
            }, 100);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
        ivBack = findViewById(R.id.iv_back);
        tvSave = findViewById(R.id.tv_save);
        etNickname = findViewById(R.id.et_nickname);
        tvCharCount = findViewById(R.id.tv_char_count);
        
        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());
        tvSave.setOnClickListener(v -> viewModel.saveNickname());
        
        // 设置软键盘完成按钮
        etNickname.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.saveNickname();
                return true;
            }
            return false;
        });
        
        // 添加文本变化监听器以更新字符计数
        etNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + "/20");
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    @Override
    protected void setupDataBinding() {
        // 双向绑定昵称输入框
        DataBindingUtils.bindEditText(etNickname, viewModel.getNickname(), this);
        // 绑定保存按钮启用状态
        DataBindingUtils.bindEnabled(tvSave, viewModel.getSaveEnabled(), this);
    }
    
    @Override
    protected void setupObservers() {
        // 观察保存成功事件
        viewModel.getSaveSuccess().observe(this, success -> {
            if (success != null && success) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }
}