package com.fxzs.lingxiagent.view.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fxzs.lingxiagent.R;

public class PasswordResetSuccessActivity extends AppCompatActivity {
    
    // 顶部栏
    private ImageView ivBack;
    
    // 输入框
    private EditText etPhone;
    private EditText etPassword;
    
    // 底部
    private Button btnLogin;
    private CheckBox cbAgreement;
    private TextView tvAgreement;
    
    // 手机号
    private String phoneNumber;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset_success);
        
        // 获取传递的手机号
        phoneNumber = getIntent().getStringExtra("phone");
        
        initializeViews();
    }
    
    private void initializeViews() {
        // 顶部栏
        ivBack = findViewById(R.id.iv_back);
        
        // 输入框
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        
        // 底部
        btnLogin = findViewById(R.id.btn_login);
        cbAgreement = findViewById(R.id.cb_agreement);
        tvAgreement = findViewById(R.id.tv_agreement);
        
        // 设置手机号（只读）
        if (phoneNumber != null) {
            etPhone.setText(phoneNumber);
        }
        
        // 设置点击事件
        ivBack.setOnClickListener(v -> {
            // 返回登录页面
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        
        btnLogin.setOnClickListener(v -> {
            String password = etPassword.getText().toString().trim();
            if (password.isEmpty()) {
                etPassword.setError("请输入密码");
                return;
            }
            
            if (!cbAgreement.isChecked()) {
                // 提示需要同意协议
                return;
            }
            
            // 跳转到登录页面，并传递手机号
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.putExtra("phone", phoneNumber);
            intent.putExtra("password", password);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}