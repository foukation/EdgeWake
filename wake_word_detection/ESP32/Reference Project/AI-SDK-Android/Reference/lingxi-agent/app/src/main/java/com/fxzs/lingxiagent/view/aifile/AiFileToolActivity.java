package com.fxzs.lingxiagent.view.aifile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.BaseActivity;

public class AiFileToolActivity extends BaseActivity<AiFileToolViewModel> {

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_ai_file_tool;
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
            title.setText("效率工具");
        }
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        bindTool(R.id.item_word_2_pdf, AiFileToolTypes.TOOL_WORD_TO_PDF);
        bindTool(R.id.item_ppt_2_pdf, AiFileToolTypes.TOOL_PPT_TO_PDF);
        bindTool(R.id.item_word_2_img, AiFileToolTypes.TOOL_WORD_TO_IMG);
        bindTool(R.id.item_ppt_2_img, AiFileToolTypes.TOOL_PPT_TO_IMG);
        bindTool(R.id.item_pdf_2_img, AiFileToolTypes.TOOL_PDF_TO_IMG);

        bindTool(R.id.item_pdf_2_word, AiFileToolTypes.TOOL_PDF_TO_WORD);
        bindTool(R.id.item_pdf_2_ppt, AiFileToolTypes.TOOL_PDF_TO_PPT);
        bindTool(R.id.item_img_2_word, AiFileToolTypes.TOOL_IMG_TO_WORD);
        bindTool(R.id.item_img_2_ppt, AiFileToolTypes.TOOL_IMG_TO_PPT);
    }

    private void bindTool(int viewId, int toolType) {
        View v = findViewById(viewId);
        if (v == null) return;
        v.setOnClickListener(click -> {
            Class<?> cls = AiFileToolTypes.isImageFlow(toolType) ? AiFileIntroImageActivity.class : AiFileIntroFileActivity.class;
            startActivity(new Intent(this, cls).putExtra(AiFileToolTypes.EXTRA_TOOL_TYPE, toolType));
        });
    }

    @Override
    protected void setupObservers() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWhiteStatusBar();
    }
}
