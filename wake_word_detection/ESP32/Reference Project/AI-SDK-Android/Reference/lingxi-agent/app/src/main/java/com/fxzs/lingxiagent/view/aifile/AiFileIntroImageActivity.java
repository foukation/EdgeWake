package com.fxzs.lingxiagent.view.aifile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.excel.PhotoUtils;

public class AiFileIntroImageActivity extends BaseActivity<AiFileToolViewModel> {

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_ai_file_intro;
    }

    @Override
    protected Class<AiFileToolViewModel> getViewModelClass() {
        return AiFileToolViewModel.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {
        View back = findViewById(R.id.back);
        TextView headerTitle = findViewById(R.id.tv_header_title);
        if (headerTitle != null) {
            headerTitle.setText(AiFileToolTypes.getTitle(getToolType()));
        }
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        View headerBg = findViewById(R.id.v_header_bg);
        if (headerBg != null) {
            headerBg.setBackgroundResource(AiFileToolTypes.getHeaderBgRes(getToolType()));
        }

        TextView title = findViewById(R.id.tv_tool_title);
        if (title != null) {
            title.setText(AiFileToolTypes.getTitle(getToolType()));
            title.setTextColor(AiFileToolTypes.getTextColor(getToolType()));
        }

        TextView subTitle = findViewById(R.id.tv_tool_subtitle);
        if (subTitle != null) {
            subTitle.setText(AiFileToolTypes.getSubTitle(getToolType()));
            subTitle.setTextColor(AiFileToolTypes.getTextColor(getToolType()));
        }

        ImageView icon = findViewById(R.id.iv_tool_icon);
        if (icon != null) {
            icon.setImageResource(AiFileToolTypes.getHeaderIconRes(getToolType()));
        }

        TextView bullet1 = findViewById(R.id.tv_bullet_1);
        TextView bullet2 = findViewById(R.id.tv_bullet_2);
        String[] bullets = AiFileToolTypes.getIntroBullets(getToolType());
        if (bullet1 != null && bullets.length > 0) {
            bullet1.setText(bullets[0]);
        }
        if (bullet2 != null && bullets.length > 1) {
            bullet2.setText(bullets[1]);
        }

        View btn = findViewById(R.id.btn_action);
        if (btn instanceof TextView) {
            ((TextView) btn).setText("选择图片");
        }
        if (btn != null) {
            btn.setOnClickListener(v -> {
                AiFilePickImageBottomSheetDialog dialog = new AiFilePickImageBottomSheetDialog();
                dialog.show(getSupportFragmentManager(), "AiFilePickImageBottomSheetDialog");
            });
        }
    }

    @Override
    protected void setupObservers() {

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWhiteStatusBar();
    }

    private int getToolType() {
        return getIntent().getIntExtra(AiFileToolTypes.EXTRA_TOOL_TYPE, AiFileToolTypes.TOOL_IMG_TO_WORD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == PhotoUtils.RESULT_CODE_PHOTO && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            String path = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                path = PhotoUtils.getPath(this, uri);
            }
            if (path == null) {
                path = uri.toString();
            }
            startActivity(new Intent(this, AiFileImageCropActivity.class)
                    .putExtra(AiFileToolTypes.EXTRA_TOOL_TYPE, getToolType())
                    .putExtra(AiFileImageCropActivity.EXTRA_IMAGE_URI, uri.toString())
                    .putExtra(AiFileImageCropActivity.EXTRA_IMAGE_PATH, path));
        } else if (requestCode == PhotoUtils.RESULT_CODE_CAMERA) {
            String path = PhotoUtils.PATH_PHOTO;
            if (path == null) return;
            startActivity(new Intent(this, AiFileImageCropActivity.class)
                    .putExtra(AiFileToolTypes.EXTRA_TOOL_TYPE, getToolType())
                    .putExtra(AiFileImageCropActivity.EXTRA_IMAGE_PATH, path));
        }
    }
}
