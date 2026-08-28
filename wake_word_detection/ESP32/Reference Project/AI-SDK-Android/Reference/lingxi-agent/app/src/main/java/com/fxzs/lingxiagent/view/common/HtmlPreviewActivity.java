package com.fxzs.lingxiagent.view.common;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.ZUtil.MarkdownUtils;


public class HtmlPreviewActivity extends AppCompatActivity {

    private static final String DATA = "data";
    private static final String EXTRA_TITLE = "extra_title";
    private TextView tv_html_content;
    private ImageView ivBack;
    private TextView tvTitle;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_html_preview);

        tv_html_content = findViewById(R.id.tv_html_content);
        ivBack = findViewById(R.id.iv_back);
        tvTitle = findViewById(R.id.tv_title);

        // 设置返回按钮
        ivBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 获取传递的数据
        String data = getIntent().getStringExtra(DATA);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        tvTitle.setText(title);
        MarkdownUtils.renderSmart(this,data,tv_html_content);
    }

    /**
     * 启动WebView Activity的静态方法
     */
    public static void start(Context context, String data, String title) {
        Intent intent = new Intent(context, HtmlPreviewActivity.class);
        intent.putExtra(DATA, data);
        intent.putExtra(EXTRA_TITLE, title);
        context.startActivity(intent);
    }
}
