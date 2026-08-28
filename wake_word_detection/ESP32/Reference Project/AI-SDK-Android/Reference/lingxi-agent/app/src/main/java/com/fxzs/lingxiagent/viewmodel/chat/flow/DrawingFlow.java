package com.fxzs.lingxiagent.viewmodel.chat.flow;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.model.drawing.dto.AspectRatioDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSessionDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingStyleDto;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;
import com.fxzs.lingxiagent.viewmodel.chat.service.DrawingGenerationService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * DrawingFlow
 * 职责：
 * - 封装绘画生图的参数组装、会话创建等待（必要时）、任务轮询、进度/结果回调
 * - 将 VMChat 从网络请求、轮询细节中解耦，VMChat 专注 UI 状态与消息变更
 * 注意：
 * - 若后端返回真实进度，可替换当前"缓进度"策略（<=90%）为真实值
 * - 入参使用 DrawingGenerationService.Params，避免重复构建请求
 */
public class DrawingFlow {

    public interface Callback {
        void onInitProgress(DrawingImageDto preview, int initProgress, String initText);
        void onProgress(int p, String text);
        void onComplete(DrawingImageDto imageDto, String finalImageUrl);
        void onError(String errorMsg);
        void onBillError(String errorCode, String errorMsg);
        void onSessionCreated(DrawingSessionDto sessionDto);
    }

    public static class Params {
        public String userPrompt;
        public String finalPrompt;
        public @Nullable String referenceImageUrl; // 继续编辑/同款
        public @Nullable DrawingStyleDto style;
        public int width;
        public int height;
        public long sessionId;
        public boolean continueEditMode;
        public @Nullable String hiddenPrompt;
        public @Nullable AspectRatioDto aspectRatioDto;
    }

    private final DrawingRepository repository;
    private final DrawingGenerationService drawingService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean isCreatingSession = false;
    private boolean pendingGeneration = false;
    private Long currentSessionId;
    private Long currentTaskId;

    public DrawingFlow(DrawingRepository repository, DrawingGenerationService drawingService) {
        this.repository = repository;
        this.drawingService = drawingService;
    }

    public void startGeneration(Params p, Callback cb) {
        if (p == null) { if (cb != null) cb.onError("参数错误"); return; }
        if (p.sessionId == 0 && !isCreatingSession) {
            pendingGeneration = true;
            createNewSession(p.userPrompt, p, new Runnable() { @Override public void run() {
                // 确保在重试前已经写入 sessionId，避免重复创建
                if (pendingGeneration) { pendingGeneration = false; startGeneration(p, cb); }
            } }, cb);
            return;
        } else if (p.sessionId == 0 && isCreatingSession) {
            if (cb != null) cb.onError("会话创建中，请稍后重试");
            return;
        }

        // 组装请求（下沉到 service 层的 Params）
        DrawingGenerationService.Params svcParams = new DrawingGenerationService.Params();
        svcParams.userPrompt = p.userPrompt;
        svcParams.finalPrompt = p.finalPrompt;
        svcParams.referenceImageUrl = p.referenceImageUrl;
        svcParams.style = p.style;
        svcParams.width = p.width;
        svcParams.height = p.height;
        svcParams.sessionId = p.sessionId;
        svcParams.continueEditMode = p.continueEditMode;
        svcParams.hiddenPrompt = p.hiddenPrompt;

        drawingService.startGeneration(svcParams, new DrawingGenerationService.Callback() {
            @Override
            public void onStart(DrawingImageDto preview, int initProgress, String initText) {
                if (cb != null) cb.onInitProgress(preview, initProgress, initText);
            }

            @Override
            public void onTaskIdReceived(Long taskId) {
                currentTaskId = taskId;
                currentSessionId = p.sessionId;
                startPollingTaskStatus(cb);
            }

            @Override
            public void onProgress(int pValue, String text) {
                if (cb != null) cb.onProgress(pValue, text);
            }

            @Override
            public void onSuccess(DrawingImageDto image) { /* 由轮询得到最终成功 */ }

            @Override
            public void onError(String errorMsg) {
                if (cb != null) cb.onError(errorMsg != null ? errorMsg : "生成失败");
            }

            @Override
            public void onBillError(String errorCode, String errorMsg) {
                if (cb != null) cb.onBillError(errorCode, errorMsg != null ? errorMsg : "生成失败");
            }
        });
    }

    private void createNewSession(String sessionName, Params p, Runnable onCreated, Callback cb) {
        if (isCreatingSession) return;
        isCreatingSession = true;
        repository.createImageSession(sessionName,null).observeForever(result -> {
            isCreatingSession = false;
            if (result.isSuccess() && result.getData() != null) {
                currentSessionId = Long.parseLong(result.getData().toString());
                // 将新会话写入参数，避免后续再次创建
                if (p != null) p.sessionId = currentSessionId;
                // 构造 session dto 反馈给上层
                DrawingSessionDto session = new DrawingSessionDto();
                session.setId(currentSessionId);
                session.setName("绘画会话 " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
                if (cb != null) cb.onSessionCreated(session);
                if (onCreated != null) onCreated.run();
            } else {
                if (cb != null) cb.onError("创建会话失败，请重试");
            }
        });
    }

    private void startPollingTaskStatus(Callback cb) {
        if (currentTaskId == null) return;
        Timer pollingTimer = new Timer();
        final int[] pollCount = {0};
        final int maxPolls = 60;
        final int[] uiProgress = {0}; // 用于在轮询过程中缓慢推进进度（<=90）
        pollingTimer.schedule(new TimerTask() {
            @Override public void run() {
                pollCount[0]++;
                if (pollCount[0] > maxPolls) {
                    pollingTimer.cancel();
                    if (cb != null) cb.onError("图片生成超时，请重试");
                    return;
                }
                mainHandler.post(() -> {
                    DrawingImageDto queryDto = new DrawingImageDto();
                    queryDto.setId(currentTaskId);
                    if (currentSessionId != null) queryDto.setSessionId(currentSessionId);
                    repository.getImageDetail(queryDto).observeForever(result -> {
                        if (result.isSuccess() && result.getData() != null) {
                            DrawingImageDto image = result.getData();
                            Integer status = image.getStatus();
                            if (status != null && status == 20) {
                                pollingTimer.cancel();
                                String finalUrl = image.getImageUrl();
                                if (finalUrl != null && !finalUrl.isEmpty()) {
                                    if (cb != null) cb.onComplete(image, finalUrl);
                                } else {
                                    if (cb != null) cb.onError("图片生成失败：未返回图片地址");
                                }
                            } else if (status != null && status == 30) {
                                pollingTimer.cancel();
                                if (cb != null) cb.onError(image.getErrorMsg() != null ? image.getErrorMsg() : "图片生成失败");
                            } else if (status != null && status == 10) {
                                // 进行中：缓慢推进进度，不超过90%
                                if (cb != null) {
                                    if (uiProgress[0] < 90) uiProgress[0] = Math.min(90, uiProgress[0] + 5);
                                    cb.onProgress(uiProgress[0], "生成中 " + uiProgress[0] + "%");
                                }
                            }
                        } else {
                            if (pollCount[0] > 10 && !result.isSuccess()) {
                                pollingTimer.cancel();
                                if (cb != null) cb.onError("查询图片状态失败，请重试");
                            }
                        }
                    });
                });
            }
        }, 1000, 2000);
    }

}

