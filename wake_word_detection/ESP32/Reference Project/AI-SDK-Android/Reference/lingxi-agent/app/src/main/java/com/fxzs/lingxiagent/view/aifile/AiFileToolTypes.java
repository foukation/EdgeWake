package com.fxzs.lingxiagent.view.aifile;

import android.graphics.Color;

import androidx.annotation.DrawableRes;

import com.fxzs.lingxiagent.R;

public final class AiFileToolTypes {
    private AiFileToolTypes() {
    }

    public static final String EXTRA_TOOL_TYPE = "extra_tool_type";

    public static final int TOOL_WORD_TO_PDF = 1;
    public static final int TOOL_PPT_TO_PDF = 2;
    public static final int TOOL_WORD_TO_IMG = 3;
    public static final int TOOL_PPT_TO_IMG = 4;
    public static final int TOOL_PDF_TO_IMG = 5;

    public static final int TOOL_PDF_TO_WORD = 6;
    public static final int TOOL_PDF_TO_PPT = 7;
    public static final int TOOL_IMG_TO_WORD = 8;
    public static final int TOOL_IMG_TO_PPT = 9;

    public static boolean isImageFlow(int toolType) {
        return toolType == TOOL_IMG_TO_WORD || toolType == TOOL_IMG_TO_PPT;
    }

    public static String getTitle(int toolType) {
        switch (toolType) {
            case TOOL_WORD_TO_PDF:
                return "Word转PDF";
            case TOOL_PPT_TO_PDF:
                return "PPT转PDF";
            case TOOL_WORD_TO_IMG:
                return "Word转图片";
            case TOOL_PPT_TO_IMG:
                return "PPT转图片";
            case TOOL_PDF_TO_IMG:
                return "PDF转图片";
            case TOOL_PDF_TO_WORD:
                return "PDF转Word";
            case TOOL_PDF_TO_PPT:
                return "PDF转PPT";
            case TOOL_IMG_TO_WORD:
                return "图片转Word";
            case TOOL_IMG_TO_PPT:
                return "图片转PPT";
            default:
                return "效率工具";
        }
    }

    public static String getSubTitle(int toolType) {
        return "固化文档内容和排版";
    }

    public static String[] getIntroBullets(int toolType) {
        if (isImageFlow(toolType)) {
            return new String[]{
                    "支持单个或批量图片内容提取，转换为目标文档",
                    "转换后的文档，支持在线预览和下载"
            };
        }
        return new String[]{
                "支持单个文件转换为目标格式",
                "转换后的文档，支持在线预览和下载"
        };
    }

    @DrawableRes
    public static int getHeaderBgRes(int toolType) {
        if (toolType == TOOL_WORD_TO_PDF || toolType == TOOL_PPT_TO_PDF) {
            return R.drawable.ai_file_bg_pink;
        }
        if (toolType == TOOL_WORD_TO_IMG || toolType == TOOL_PPT_TO_IMG || toolType == TOOL_PDF_TO_IMG) {
            return R.drawable.ai_file_bg_green;
        }
        if (toolType == TOOL_IMG_TO_PPT || toolType == TOOL_PDF_TO_PPT) {
            return R.drawable.ai_file_bg_orange;
        }
        return R.drawable.ai_file_bg_blue;
    }
    public static int getTextColor(int toolType) {
        if (toolType == TOOL_WORD_TO_PDF || toolType == TOOL_PPT_TO_PDF) {
            return Color.parseColor("#FF746D");
        }
        if (toolType == TOOL_WORD_TO_IMG || toolType == TOOL_PPT_TO_IMG || toolType == TOOL_PDF_TO_IMG) {
            return Color.parseColor("#3DDBC1");
            // return Color.parseColor("#00BFA5");
            
        }
        if (toolType == TOOL_IMG_TO_PPT || toolType == TOOL_PDF_TO_PPT) {
            // return Color.parseColor("#FFA000");
            return Color.parseColor("#FF9436");
        }
        return Color.parseColor("#2196F3");
    }


    @DrawableRes
    public static int getHeaderIconRes(int toolType) {
        switch (toolType) {
            case TOOL_WORD_TO_PDF:
                return R.drawable.ai_file_word_2_pdf;
            case TOOL_PPT_TO_PDF:
                return R.drawable.ai_file_ppt_2_pdf;
            case TOOL_WORD_TO_IMG:
                return R.drawable.ai_file_word_2_img;
            case TOOL_PPT_TO_IMG:
                return R.drawable.ai_file_ppt_2_img;
            case TOOL_PDF_TO_IMG:
                return R.drawable.ai_file_pdf_2_img;
            case TOOL_PDF_TO_WORD:
                return R.drawable.ai_file_pdf_2_word;
            case TOOL_PDF_TO_PPT:
                return R.drawable.ai_file_pdf_2_ppt;
            case TOOL_IMG_TO_WORD:
                return R.drawable.ai_file_img_2_word;
            case TOOL_IMG_TO_PPT:
                return R.drawable.ai_file_img_2_ppt;
            default:
                return R.drawable.ai_work_file;
        }
    }
}
