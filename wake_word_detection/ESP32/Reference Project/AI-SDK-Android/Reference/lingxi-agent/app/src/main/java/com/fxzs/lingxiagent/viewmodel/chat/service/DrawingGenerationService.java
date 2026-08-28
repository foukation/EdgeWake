package com.fxzs.lingxiagent.viewmodel.chat.service;

import android.os.Handler;
import android.os.Looper;

import com.fxzs.lingxiagent.model.drawing.api.GenerateImageRequest;
import com.fxzs.lingxiagent.model.drawing.dto.AspectRatioDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingStyleDto;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;
import com.fxzs.lingxiagent.model.honor.repository.BenefitCode;

/**
 * 绘画生成服务：负责组装生成请求、调度仓库生成、并通过回调回传进度/结果。
 */
public class DrawingGenerationService {

    public interface Callback {
        void onStart(DrawingImageDto preview, int initProgress, String initText);
        void onTaskIdReceived(Long taskId);
        void onProgress(int progress, String text);
        void onSuccess(DrawingImageDto image);
        void onBillError(String errorCode, String errorMsg);
        void onError(String errorMsg);
    }

    private final DrawingRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public DrawingGenerationService(DrawingRepository repository) {
        this.repository = repository;
    }

    public void startGeneration(Params p, Callback cb) {
        // 组装请求
        GenerateImageRequest req = new GenerateImageRequest();
        req.setPrompt(p.finalPrompt);
        if (p.sessionId != null && p.sessionId != 0) {
            req.setSessionId(p.sessionId);
        }
        if (p.referenceImageUrl != null && !p.referenceImageUrl.isEmpty()) {
            req.setImagUrls(new String[]{p.referenceImageUrl});
        }
        if (p.width > 0 && p.height > 0) {
            req.setWidth(p.width);
            req.setHeight(p.height);
        }
        if (p.style != null) {
            req.setStyleId(p.style.getId());
        }

        // 初始回调（预览）
        if (cb != null) {
            DrawingImageDto preview = new DrawingImageDto();
            preview.setPrompt(p.userPrompt);
            if (req.getStyleId() != null) preview.setStyleId(req.getStyleId());
            preview.setWidth(p.width);
            preview.setHeight(p.height);
            cb.onStart(preview, 0, "正在生成中...");
        }

        // 调接口生成
        repository.generateImage(req).observeForever(result -> {
            if (result.isSuccess() && result.getData() != null) {
                Long taskId = result.getData().getId();
                if (cb != null) cb.onTaskIdReceived(taskId);
            } else if(BenefitCode.isBenefitError(result.getCode())){
                if (cb != null) cb.onBillError(result.getCode(), result.getError());
            }else {
                if (cb != null) cb.onError(result.getError() != null ? result.getError() : "生成失败");
            }
        });
    }

    // 参数收集结构体
    public static class Params {
        public String userPrompt;              // 用户输入原始 prompt
        public String finalPrompt;             // 拼装后的最终 prompt
        public String referenceImageUrl;       // 参考图 URL
        public DrawingStyleDto style;          // 选中风格
        public AspectRatioDto ratio;           // 宽高比
        public int width;                      // 宽
        public int height;                     // 高
        public Long sessionId;                 // 会话ID
        public boolean continueEditMode;       // 是否继续编辑
        public String hiddenPrompt;            // 隐藏 prompt
    }
}

