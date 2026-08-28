package com.fxzs.lingxiagent.view.aifile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.excel.PhotoUtils;

public class AiFilePickImageActivity extends BaseActivity<AiFileToolViewModel> {

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_ai_file_pick_image;
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
        TextView title = findViewById(R.id.tv_header_title);
        if (title != null) {
            title.setText(AiFileToolTypes.getTitle(getToolType()));
        }
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        // 选图入口已前置到介绍页（AiFileIntroImageActivity）
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
            // 相机拍摄的图片路径保存在 PhotoUtils.PATH_PHOTO
            String path = PhotoUtils.PATH_PHOTO;
            if (path == null) return;
            startActivity(new Intent(this, AiFileImageCropActivity.class)
                    .putExtra(AiFileToolTypes.EXTRA_TOOL_TYPE, getToolType())
                    .putExtra(AiFileImageCropActivity.EXTRA_IMAGE_PATH, path));
        }
    }
}
