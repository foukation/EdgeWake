package com.fxzs.lingxiagent.viewmodel.meeting;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingDto;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingSummaryDto;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepository;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepositoryImpl;
import com.fxzs.lingxiagent.model.meeting.repository.StreamProgressCallback;
import com.fxzs.lingxiagent.util.NetworkStateManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

public class VMMeetingSummary extends BaseViewModel {
    
    private static final String TAG = "VMMeetingSummary";
    
    // 流式摘要数据相关字段（参考VMStream）
    private final ObservableField<String> summaryStreamContent = new ObservableField<>("");
    private final ObservableField<Boolean> isGenerating = new ObservableField<>(false);
    private final ObservableField<Integer> summaryProgress = new ObservableField<>(0);
    private final ObservableField<String> summaryStatus = new ObservableField<>("准备生成摘要");
    
    // 控制按钮状态
    private final ObservableField<Boolean> generateButtonEnabled = new ObservableField<>(true);
    private final ObservableField<Boolean> stopButtonEnabled = new ObservableField<>(false);
    private final ObservableField<Boolean> clearButtonEnabled = new ObservableField<>(false);
    
    // 流式传输控制
    private final AtomicBoolean generatingFlag = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 强制更新控制
    private final AtomicInteger updateCounter = new AtomicInteger(0);
    
    // 兼容性字段 - 仅保留给Fragment使用
    private final ObservableField<MeetingSummaryDto> meetingSummaryResult = new ObservableField<>();
    
    // Repository
    private final MeetingRepository repository;

    // 当前使用的botKey
    private String currentBotKey = "1";
    
    // 网络状态管理
    private NetworkStateManager networkStateManager;
    private boolean wasGeneratingWhenNetworkLost = false;
    private String lastTranscriptionResult;
    private Integer lastMeetingId;
    private String lastBotKey;
    
    // 网络重连回调
    private final ObservableField<Boolean> shouldShowReconnectDialog = new ObservableField<>(false);

    public AtomicBoolean getGeneratingFlag() {
        return generatingFlag;
    }

    public VMMeetingSummary(@NonNull Application application) {
        super(application);
        this.repository = new MeetingRepositoryImpl();
        setupValidation();
        setupNetworkMonitoring();
    }
    
    private void setupValidation() {
        // 监听生成状态变化，更新按钮状态（参考VMStream）
        isGenerating.observeForever(generating -> {
            if (generating != null) {
                generateButtonEnabled.set(!generating);
                stopButtonEnabled.set(generating);
                
                // 只有在有内容且未在生成时才允许清除
                String content = summaryStreamContent.get();
                boolean hasContent = content != null && !content.trim().isEmpty();
                clearButtonEnabled.set(hasContent && !generating);
            }
        });
        
        // 监听内容变化，更新清除按钮状态
        summaryStreamContent.observeForever(content -> {
            boolean hasContent = content != null && !content.trim().isEmpty();
            boolean notGenerating = !Boolean.TRUE.equals(isGenerating.get());
            clearButtonEnabled.set(hasContent && notGenerating);
        });
    }
    
    /**
     * 设置网络状态监听
     */
    private void setupNetworkMonitoring() {
        networkStateManager = new NetworkStateManager(getApplication());
        networkStateManager.addNetworkStateListener(new NetworkStateManager.NetworkStateListener() {
            @Override
            public void onNetworkAvailable() {
                handleNetworkStateChange(true);
            }
            
            @Override
            public void onNetworkLost() {
                handleNetworkStateChange(false);
            }
            
            @Override
            public void onNetworkChanged(boolean isConnected) {
                handleNetworkStateChange(isConnected);
            }
        });
    }
    
    /**
     * 处理网络状态变化
     */
    private void handleNetworkStateChange(boolean isConnected) {
        if (!isConnected) {
            // 网络断开
            if (generatingFlag.get()) {
                // 正在生成时网络断开，记录状态
                wasGeneratingWhenNetworkLost = true;
                Timber.tag(TAG).w("网络断开，正在生成会议摘要");
                
                // 更新状态显示
                mainHandler.post(() -> {
                    summaryStatus.set("网络连接中断，正在重连...");
                    summaryStreamContent.set("网络连接中断，正在尝试重连...\n\n请检查网络连接，网络恢复后将提示重新生成。");
                });
            }
        } else {
            // 网络恢复
            if (wasGeneratingWhenNetworkLost) {
                Timber.tag(TAG).i( "网络已恢复，之前正在生成会议摘要");
                
                // 停止当前生成状态
                generatingFlag.set(false);
                isGenerating.set(false);
                setLoading(false);
                
                // 触发显示重连弹框
                mainHandler.post(() -> {
                    shouldShowReconnectDialog.set(true);
                    summaryStatus.set("网络已恢复");
                    summaryStreamContent.set("网络连接已恢复\n\n请选择是否重新生成会议摘要。");
                });
                
                wasGeneratingWhenNetworkLost = false;
            }
        }
    }
    
    /**
     * 重新生成会议摘要（网络恢复后调用）
     */
//    public void retryGenerationAfterReconnect() {
//        if (lastTranscriptionResult != null) {
//            Timber.tag(TAG).i(, "网络恢复后重新生成会议摘要");
//            generateMeetingSummaryStream(lastTranscriptionResult, lastMeetingId, lastBotKey);
//        }
//        shouldShowReconnectDialog.set(false);
//    }
    
    /**
     * 取消重新生成（网络恢复后用户选择取消）
     */
    public void cancelRetryAfterReconnect() {
        shouldShowReconnectDialog.set(false);
        summaryStreamContent.set("已取消重新生成\n\n可以点击标签重新生成摘要。");
        summaryStatus.set("已取消重新生成");
    }
    
    // 新的流式摘要字段Getters
    public ObservableField<String> getSummaryStreamContent() {
        return summaryStreamContent;
    }
    
    /**
     * 强制更新流式内容，确保观察者能收到通知
     */
    private void forceUpdateStreamContent(String content) {
        // 在主线程中更新，确保观察者能收到通知
        mainHandler.post(() -> {
            // 使用计数器强制触发更新
            int counter = updateCounter.incrementAndGet();
//            summaryStreamContent.set(content + "|" + counter);
            // 立即设置为正确内容
            summaryStreamContent.set(content);
            Timber.tag(TAG).d( "强制更新流式内容，计数器: " + counter + ", 内容长度: " + (content != null ? content.length() : 0));
        });
    }
    
    public ObservableField<Boolean> getIsGenerating() {
        return isGenerating;
    }
    
    public ObservableField<Integer> getSummaryProgress() {
        return summaryProgress;
    }
    
    public ObservableField<String> getSummaryStatus() {
        return summaryStatus;
    }
    
    public ObservableField<Boolean> getGenerateButtonEnabled() {
        return generateButtonEnabled;
    }
    
    public ObservableField<Boolean> getStopButtonEnabled() {
        return stopButtonEnabled;
    }
    
    public ObservableField<Boolean> getClearButtonEnabled() {
        return clearButtonEnabled;
    }
    
    public ObservableField<Boolean> getShouldShowReconnectDialog() {
        return shouldShowReconnectDialog;
    }
    
    // 兼容性方法
    public ObservableField<MeetingSummaryDto> getMeetingSummaryResult() {
        return meetingSummaryResult;
    }
    
    
    
    
    /**
     * 流式生成会议摘要 - 使用真实API替换mock数据
     */
    public void generateMeetingSummaryStream(String transcriptionResult, Integer meetingId) {
        generateMeetingSummaryStream(transcriptionResult, meetingId, "1");
    }

    /**
     * 强制重新生成摘要 - 不检查已有内容，直接重新生成
     * @param transcriptionResult 转写结果
     * @param meetingId 会议ID
     * @param botKey 摘要类型
     */
    public void forceRegenerateSummary(String transcriptionResult, Integer meetingId, String botKey) {
        Timber.tag(TAG).d( "强制重新生成摘要 - meetingId: " + meetingId + ", botKey: " + botKey);

        // 更新当前botKey
        this.currentBotKey = botKey;

        // 清空当前内容，显示加载状态
        summaryStreamContent.set("正在重新生成会议摘要...\n\n请稍候，AI正在重新分析会议内容并生成摘要。");
        summaryProgress.set(0);
        summaryStatus.set("正在重新生成摘要...");

        // 直接调用生成方法，跳过检查
        generateMeetingSummaryStream(transcriptionResult, meetingId, botKey);
    }
    
    /**
     * 先查询是否有会议记录，如果有直接显示，没有则生成新的摘要
     * @param transcriptionResult 转写结果
     * @param meetingId 会议ID
     * @param botKey 摘要类型：5-按章节, 4-按主题, 3-详细
     */
    public void generateMeetingSummaryWithCheck(String transcriptionResult, Integer meetingId, String botKey) {
        // 更新当前botKey
        this.currentBotKey = botKey;

        if (meetingId == null) {
            generateMeetingSummaryStream(transcriptionResult, meetingId, botKey);
            return;
        }
        
        setLoading(true);
        summaryStatus.set("正在查询会议记录...");

        // 只有在当前内容为空时才显示查询提示
        String currentContent = summaryStreamContent.get();
        if (currentContent == null || currentContent.trim().isEmpty() ||
            currentContent.contains("摘要内容已清除") ||
            currentContent.contains("等待会议内容")) {
            summaryStreamContent.set("正在加载摘要内容...");
        }
        // 如果已有内容，保持不变，不显示查询状态
        
        repository.getMeetingInfoById(meetingId)
            .observeForever(result -> {
                if (result != null && result.isSuccess()) {
                    MeetingDto meetingInfo = result.getData();
                    if (meetingInfo != null) {
                        // 调试日志：打印所有摘要相关字段
                        // Timber.tag(TAG).d( "会议信息详情 - ID: " + meetingId);
                        // Timber.tag(TAG).d( "  abstractText: " + meetingInfo.getAbstractText());
                        // Timber.tag(TAG).d( "  abstractChapterText: " + meetingInfo.getAbstractChapterText());
                        // Timber.tag(TAG).d( "  abstractDetailText: " + meetingInfo.getAbstractDetailText());
                        // Timber.tag(TAG).d( "  abstractOptimizeText: " + meetingInfo.getAbstractOptimizeText());
                        // Timber.tag(TAG).d( "  当前botKey: " + botKey);
                        
                        // 根据不同的botKey检查对应的摘要字段
                        String existingSummary = null;
                        switch (botKey) {
                            case "5": // 按章节
                                existingSummary = meetingInfo.getAbstractChapterText();
                                break;
                            case "4": // 按主题（可能是abstractOptimizeText）
                                existingSummary = meetingInfo.getAbstractOptimizeText();
                                break;
                            case "3": // 详细
                                existingSummary = meetingInfo.getAbstractDetailText();
                                break;
                            default:
                                existingSummary = meetingInfo.getAbstractText();
                                break;
                        }
                        
                         Timber.tag(TAG).d( "根据botKey=" + botKey + "，获取到的摘要: " +
                             (existingSummary != null ? "长度=" + existingSummary.length() : "null"));
                        
                        if (existingSummary != null && !existingSummary.trim().isEmpty()) {
                            // 如果已有摘要，直接显示，不显示加载过程
                            Timber.tag(TAG).i("找到已有的会议摘要（类型: " + botKey + "），直接显示");
                            final String summaryToDisplay = existingSummary;
                            mainHandler.post(() -> {
                                setLoading(false);
                                // 直接显示已有内容，不显示加载状态
                                summaryStreamContent.set(summaryToDisplay);
                                summaryProgress.set(100);
                                summaryStatus.set("已加载历史摘要");
                                // 确保生成状态为false
                                isGenerating.set(false);
                                generatingFlag.set(false);
                            });
                        } else {
                            // 没有摘要，需要生成新的，此时才显示加载状态
                            Timber.tag(TAG).i( "未找到会议摘要（类型: " + botKey + "），开始生成新摘要");
                            generateMeetingSummaryStream(transcriptionResult, meetingId, botKey);
                        }
                    } else {
                        // 没有会议信息，需要生成新的
                        Timber.tag(TAG).i( "未找到会议信息，开始生成新摘要");
                        generateMeetingSummaryStream(transcriptionResult, meetingId, botKey);
                    }
                } else {
                    // 查询失败，仍然尝试生成新摘要
                    Timber.tag(TAG).w( "查询会议记录失败，开始生成新摘要");
                    generateMeetingSummaryStream(transcriptionResult, meetingId, botKey);
                }
            });
    }
    
    /**
     * 流式生成会议摘要 - 支持不同的botKey类型
     * @param transcriptionResult 转写结果
     * @param meetingId 会议ID
     * @param botKey 摘要类型：5-按章节, 4-按主题, 3-详细
     */
    public void generateMeetingSummaryStream(String transcriptionResult, Integer meetingId, String botKey) {
        Timber.tag(TAG).d( "generateMeetingSummaryStream transcriptionResult = "+transcriptionResult+" meetingId = "+meetingId+" botKey = "+botKey);
        // 更新当前botKey
        this.currentBotKey = botKey;
        
        // 保存参数以便网络恢复后重新生成
        this.lastTranscriptionResult = transcriptionResult;
        this.lastMeetingId = meetingId;
        this.lastBotKey = botKey;

        if (transcriptionResult == null || transcriptionResult.trim().isEmpty()) {
            setError("转写内容为空，无法生成摘要");
            return;
        }
        
        if (generatingFlag.get()) {
//            setError("当前正在生成摘要");
            return;
        }
        
        // 使用final变量，以便在内部类中访问
        final Integer finalMeetingId = meetingId;
        final String finalBotKey = botKey;
        
        generatingFlag.set(true);
        isGenerating.set(true);
        summaryStatus.set("正在生成摘要...");
        summaryProgress.set(0);
        // 在流式返回前先显示加载状态
        summaryStreamContent.set("正在生成会议摘要...\n\n请稍候，AI正在分析会议内容并生成摘要。");
        setLoading(true);
        
        // Timber.tag(TAG).i(, "开始流式生成会议摘要 - meetingId: " + meetingId + 
        //     ", 内容长度: " + transcriptionResult.length());
        
        // 创建流式进度回调
        StreamProgressCallback progressCallback = new StreamProgressCallback() {
            private int totalChunks = 0;
            
            @Override
            public void onChunkReceived(String chunk, String accumulatedContent) {
                totalChunks++;

                // 在主线程更新UI
                mainHandler.post(() -> {
                    if (generatingFlag.get()) {
                        // 转换XML格式为Markdown格式
                        String formattedContent = convertXmlToMarkdown(accumulatedContent);

                        // 如果是第一个有效内容片段，清除加载状态
                        if (totalChunks == 1 && formattedContent != null && !formattedContent.trim().isEmpty()) {
                            // 第一次接收到真实内容，清除加载提示
                            Timber.tag(TAG).d( "接收到第一个内容片段，清除加载状态");
                        }

                        // 将换行符转换为可见的\n以便在日志中查看
                        String logContent = formattedContent.replace("\n", "\\n");
                        Timber.tag(TAG).i( "接收到流式片段 - 格式化后内容: " + logContent);
                        summaryStreamContent.set(formattedContent);
                        // 使用强制更新确保观察者能收到通知
//                        forceUpdateStreamContent(formattedContent);
                        
                        // 计算模拟进度（基于内容长度，最多95%）
                        int estimatedProgress = Math.min(95, (formattedContent.length() * 90) / Math.max(1000, transcriptionResult.length() * 3));
                        summaryProgress.set(estimatedProgress);

                        // 更新状态
                        String status = String.format("生成中... %d%% (已接收 %d 片段)", estimatedProgress, totalChunks);
                        summaryStatus.set(status);
                        
                        // Timber.tag(TAG).d( "实时摘要更新 - 片段 " + totalChunks + ": " + 
                        //     (chunk.length() > 50 ? chunk.substring(0, 50) + "..." : chunk));
                         Timber.tag(TAG).d( "累积内容长度: " + formattedContent.length() + ", 进度: " + estimatedProgress + "%");
                    }
                });
            }
            
            @Override
            public void onStreamComplete(int totalChunks) {
                mainHandler.post(() -> {
                    Timber.tag(TAG).i( "流式摘要生成完成 - onStreamComplete: " + generatingFlag.get());
                    if (generatingFlag.get()) {
                        // 完成时设置进度为100%
//                        summaryProgress.set(100);
                        summaryProgress.setValue(100);
                        finishGenerating("摘要生成完成，共接收 " + totalChunks + " 个片段");
                        
                        
                         Timber.tag(TAG).i( "流式摘要生成完成 - 总片段数: " + totalChunks);
                        
                        // 如果有meetingId，则更新会议记录
                        if (finalMeetingId != null) {
                            String summaryContent = summaryStreamContent.get();
                            if (summaryContent != null && !summaryContent.trim().isEmpty()) {
                                
                                repository.updateMeetingRecord(finalMeetingId, finalBotKey, summaryContent)
                                    .observeForever(updateResult -> {
                                        if (updateResult != null) {
                                            if (updateResult.isSuccess()) {
                                                Timber.tag(TAG).i( "成功更新会议记录摘要");
                                                summaryProgress.setValue(100);
                                            } else {
                                                Timber.tag(TAG).e( "更新会议记录摘要失败: " + updateResult.getError());
                                            }
                                        }
                                    });
                            }
                        }
                    }
                });
            }
            
            @Override
            public void onStreamError(String error) {
                mainHandler.post(() -> {
                    generatingFlag.set(false);
                    isGenerating.set(false);
                    setLoading(false);
                    setError("摘要生成失败: " + error);
                    summaryStatus.set("生成失败");

                    // 显示错误状态
                    summaryStreamContent.set("摘要生成失败\n\n错误信息：" + error + "\n\n请点击刷新按钮重试。");

                    Timber.tag(TAG).e( "流式摘要生成错误: " + error);
                });
            }
        };
        
        // 调用真实的流式API
        repository.generateMeetingSummaryWithProgress(transcriptionResult, meetingId, botKey, progressCallback)
            .observeForever(result -> {
                // Timber.tag(TAG).i(, "API返回摘要结果 result: " +
                //     (result != null ? result.toString() : "null"));
                if (result != null) {
                    if (result.isSuccess()) {
                        MeetingSummaryDto summaryDto = result.getData();
                        if (summaryDto != null) {
                            // Timber.tag(TAG).i(, "API返回摘要结果 - 长度: " + 
                            //     (summaryDto.getSummary() != null ? summaryDto.getSummary().length() : 0));
                            
                            // 更新最终内容（如果API返回的内容比流式内容更完整）
                            if (summaryDto.getSummary() != null && !summaryDto.getSummary().isEmpty()) {
                                String currentStreamContent = summaryStreamContent.get();
                                if (currentStreamContent == null || summaryDto.getSummary().length() > currentStreamContent.length()) {
                                    // summaryStreamContent.set(summaryDto.getSummary());
                                }
                            }
                            
                            // 更新兼容性字段给Fragment使用
                            // meetingSummaryResult.postValue(summaryDto);
                        }
                    } else {
                        // 如果还在生成中，说明回调已经处理了错误
                        if (generatingFlag.get()) {
                            mainHandler.post(() -> {
                                generatingFlag.set(false);
                                isGenerating.set(false);
                                setLoading(false);
                                setError(result.getError());
                                summaryStatus.set("生成失败");

                                // 显示API错误状态
                                String errorMsg = result.getError() != null ? result.getError() : "未知错误";
                                summaryStreamContent.set("摘要生成失败\n\nAPI错误：" + errorMsg + "\n\n请点击刷新按钮重试。");
                            });
                        }
                    }
                }
            });
    }
    
    /**
     * 当流式API不可用时的降级处理（使用传统API）
     */
    // private void fallbackToTraditionalAPI(String transcriptionResult, Integer meetingId) {
    //     Timber.tag(TAG).w((TAG, "使用传统API作为降级方案");
        
    //     repository.generateMeetingSummaryForUI(transcriptionResult, meetingId)
    //         .observeForever(result -> {
    //             if (result != null) {
    //                 if (result.isSuccess()) {
    //                     MeetingSummaryDto summaryDto = result.getData();
    //                     if (summaryDto != null && summaryDto.getSummary() != null) {
    //                         // 模拟流式效果：分段显示内容
    //                         String fullSummary = summaryDto.getSummary();
    //                         simulateStreamingEffect(fullSummary, summaryDto);
    //                     } else {
    //                         finishGenerating("摘要内容为空");
    //                     }
    //                 } else {
    //                     mainHandler.post(() -> {
    //                         generatingFlag.set(false);
    //                         isGenerating.set(false);
    //                         setLoading(false);
    //                         setError(result.getError());
    //                         summaryStatus.set("生成失败");
    //                     });
    //                 }
    //             }
    //         });
    // }
    
    /**
     * 模拟流式显示效果
     */
    private void simulateStreamingEffect(String fullSummary, MeetingSummaryDto finalDto) {
        // executorService.execute(() -> {
        //     try {
        //         // 将完整摘要分段显示，模拟流式效果
        //         String[] sentences = fullSummary.split("[。！？\n]");
        //         StringBuilder contentBuilder = new StringBuilder();
                
        //         for (int i = 0; i < sentences.length && generatingFlag.get(); i++) {
        //             String sentence = sentences[i].trim();
        //             if (!sentence.isEmpty()) {
        //                 contentBuilder.append(sentence);
        //                 if (i < sentences.length - 1) {
        //                     contentBuilder.append("。\n");
        //                 }
                        
        //                 // 计算进度
        //                 final int progress = (int) ((float) (i + 1) / sentences.length * 100);
        //                 final String currentContent = contentBuilder.toString();
                        
        //                 // 在主线程更新UI
        //                 mainHandler.post(() -> {
        //                     if (generatingFlag.get()) {
        //                         summaryStreamContent.set(currentContent);
        //                         summaryProgress.set(progress);
        //                         summaryStatus.set(String.format("生成进度: %d%%", progress));
        //                     }
        //                 });
                        
        //                 // 模拟网络延迟
        //                 Thread.sleep(100 + (int)(Math.random() * 200));
        //             }
        //         }
                
        //         // 完成时更新最终内容和兼容性字段
        //         if (generatingFlag.get()) {
        //             mainHandler.post(() -> {
        //                 summaryStreamContent.set(fullSummary);
        //                 meetingSummaryResult.postValue(finalDto);
        //                 finishGenerating("摘要生成完成");
        //             });
        //         }
                
        //     } catch (InterruptedException e) {
        //         mainHandler.post(() -> {
        //             generatingFlag.set(false);
        //             isGenerating.set(false);
        //             setLoading(false);
        //             setError("摘要生成被中断");
        //             summaryStatus.set("生成中断");
        //         });
        //         Thread.currentThread().interrupt();
        //     }
        // });
    }
    
    /**
     * 停止生成摘要
     */
    public void stopGenerating() {
        if (!generatingFlag.get()) {
            return;
        }

        generatingFlag.set(false);
        // 显示停止状态
        summaryStreamContent.set("摘要生成已停止\n\n点击刷新按钮重新生成摘要。");
        finishGenerating("摘要生成已停止");
    }
    
    /**
     * 清除摘要内容
     */
    public void clearSummary() {
        if (Boolean.TRUE.equals(isGenerating.get())) {
            setError("无法在生成过程中清除内容");
            return;
        }
        
        summaryStreamContent.set("摘要内容已清除\n\n点击标签重新生成摘要内容。");
        summaryProgress.set(0);
        summaryStatus.set("内容已清除");
    }
    
    /**
     * 将XML格式的内容转换为Markdown格式
     * @param content 原始内容
     * @return Markdown格式的内容
     */
    private String convertXmlToMarkdown(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        // 调试日志
        // Timber.tag(TAG).d( "convertXmlToMarkdown - 原始内容长度: " + content.length());
        // Timber.tag(TAG).d( "convertXmlToMarkdown - 原始内容包含<标签>: " + content.contains("<"));
        
        // 转换XML标签为Markdown格式
        String markdown = content
            // 主要标题
            .replaceAll("<会议总结>", "## 会议总结\n\n")
            .replaceAll("</会议总结>", "")
            .replaceAll("<会议主题及相关内容>", "\n## 会议主题及相关内容\n\n")
            .replaceAll("</会议主题及相关内容>", "")
            .replaceAll("<会议待办>", "\n## 会议待办\n\n")
            .replaceAll("</会议待办>", "")
            // 其他可能的标签
            .replaceAll("<会议纪要>", "## 会议纪要\n\n")
            .replaceAll("</会议纪要>", "")
            .replaceAll("<核心议题>", "\n## 核心议题\n\n")
            .replaceAll("</核心议题>", "")
            .replaceAll("<行动项>", "\n## 行动项\n\n")
            .replaceAll("</行动项>", "")
            // 子标题格式
            .replaceAll("(^|\n)主题：", "\n**主题：**")
            .replaceAll("(^|\n)核心事项：", "\n**核心事项：**")
            .replaceAll("(^|\n)时间：", "\n**时间：**")
            .replaceAll("(^|\n)地点：", "\n**地点：**")
            .replaceAll("(^|\n)参会人：", "\n**参会人：**")
            // 清理多余的空行
            .replaceAll("\n{3,}", "\n\n")
            .trim();
        
        // 调试日志
        // Timber.tag(TAG).d( "convertXmlToMarkdown - 转换后长度: " + markdown.length());
        // Timber.tag(TAG).d( "convertXmlToMarkdown - 转换后包含换行符: " + markdown.contains("\n"));
        // String preview = markdown.length() > 200 ? markdown.substring(0, 200) + "..." : markdown;
        // Timber.tag(TAG).d( "convertXmlToMarkdown - 转换后预览: " + preview.replace("\n", "\\n"));
        
        return markdown;
    }
    
    /**
     * 格式化流式数据，去除<meeting_record>标签和空白字符
     * @param rawContent 原始内容
     * @return 格式化后的内容
     */
    private String formatStreamContent(String rawContent) {
        if (rawContent == null || rawContent.isEmpty()) {
            return "";
        }
        
        // 调试日志：检查原始内容
        // Timber.tag(TAG).d( "formatStreamContent - 原始内容长度: " + rawContent.length());
        // Timber.tag(TAG).d( "formatStreamContent - 原始内容包含换行符: " + rawContent.contains("\n"));
        // String rawLogContent = rawContent.length() > 100 ? rawContent.substring(0, 100) + "..." : rawContent;
        // Timber.tag(TAG).d( "formatStreamContent - 原始内容前100字符: " + rawLogContent.replace("\n", "\\n"));
        
        // 去除<meeting_record>和</meeting_record>标签
        String formatted = rawContent
            .replaceAll("<meeting_record>", "")
            .replaceAll("</meeting_record>", "")
            .trim();
        
        // 检查是否需要转换转义的换行符
        if (formatted.contains("\\n")) {
            // Timber.tag(TAG).d( "formatStreamContent - 检测到转义的换行符，进行转换");
            formatted = formatted.replace("\\n", "\n");
        }
        
        // 调试日志：检查格式化后内容
        // Timber.tag(TAG).d( "formatStreamContent - 格式化后长度: " + formatted.length());
        // Timber.tag(TAG).d( "formatStreamContent - 格式化后包含换行符: " + formatted.contains("\n"));
        // String formattedLogContent = formatted.length() > 100 ? formatted.substring(0, 100) + "..." : formatted;
        // Timber.tag(TAG).d( "formatStreamContent - 格式化后前100字符: " + formattedLogContent.replace("\n", "\\n"));
        
        return formatted;
    }
    
    /**
     * 完成摘要生成
     */
    private void finishGenerating(String message) {
        generatingFlag.set(false);
        isGenerating.set(false);
        setLoading(false);
        summaryStatus.set(message);

        // Timber.tag(TAG).i(, "摘要生成完成: " + message);
    }
    
    /**
     * 测试流式摘要功能 - 使用真实API
     */
    public void testStreamSummary() {
        String mockTranscription = "这是一个测试会议内容，包含了项目讨论、进度汇报、问题分析等多个议题。" +
            "会议中大家就下一步工作计划达成了一致意见，确定了重要的里程碑节点。" +
            "参会人员包括产品经理、技术负责人、UI设计师等关键角色。" +
            "会议确定了下个季度的开发重点，包括用户体验优化、性能提升、新功能开发等方面。";

        Timber.tag(TAG).i( "开始测试流式摘要功能，使用真实API");
        // 显示测试状态
        summaryStreamContent.set("正在测试摘要生成功能...\n\n使用模拟会议内容进行测试。");
        generateMeetingSummaryStream(mockTranscription, 1);
    }

    /**
     * 获取当前使用的botKey
     * @return 当前botKey
     */
    public String getCurrentBotKey() {
        return currentBotKey;
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        
        // 停止摘要生成
        generatingFlag.set(false);
        
        // 关闭线程池
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        // 清理网络监听器
        if (networkStateManager != null) {
            networkStateManager.destroy();
        }
        
        // 清理观察者
        isGenerating.removeObserver(generating -> {});
        summaryStreamContent.removeObserver(content -> {});
        
        // Timber.tag(TAG).d( "VMMeetingSummary已清理完成");
    }
}