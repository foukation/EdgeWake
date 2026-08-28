package com.fxzs.lingxiagent.viewmodel.meeting;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class VMStream extends BaseViewModel {
    
    // 流式数据相关字段
    private final ObservableField<String> streamContent = new ObservableField<>("");
    private final ObservableField<Boolean> isStreaming = new ObservableField<>(false);
    private final ObservableField<String> streamTitle = new ObservableField<>("实时数据流演示");
    private final ObservableField<Integer> streamProgress = new ObservableField<>(0);
    private final ObservableField<String> streamStatus = new ObservableField<>("准备就绪");
    
    // 控制按钮状态
    private final ObservableField<Boolean> startButtonEnabled = new ObservableField<>(true);
    private final ObservableField<Boolean> stopButtonEnabled = new ObservableField<>(false);
    private final ObservableField<Boolean> clearButtonEnabled = new ObservableField<>(false);
    
    // 流式传输控制
    private final AtomicBoolean streamingFlag = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    
    // 模拟数据源
    private static final String[] SAMPLE_SENTENCES = {
        "正在处理用户请求数据...",
        "分析用户行为模式...",
        "生成个性化推荐内容...",
        "优化算法参数设置...",
        "执行数据清洗操作...",
        "构建机器学习模型...",
        "验证数据完整性...",
        "同步云端数据库...",
        "更新用户偏好设置...",
        "计算推荐准确率...",
        "处理异常数据点...",
        "生成分析报告...",
        "优化系统性能...",
        "备份重要数据...",
        "执行安全检查...",
        "完成数据处理任务...",
        "准备返回结果给用户...",
        "清理临时文件...",
        "更新用户界面...",
        "加载新内容到缓存...",
        "完成数据流式传输...",
            "模拟API流式响应开始...",
        "处理用户身份验证...",
        "查询用户历史记录...",
        "分析用户兴趣偏好...",
        "生成个性化推荐列表...",
        "更新用户偏好设置...",
        "计算推荐准确率...",
        "处理异常数据点...",
        "生成分析报告...",
        "优化系统性能...",
        "备份重要数据...",
        "执行安全检查...",
        "完成数据处理任务...",
        "准备返回结果给用户...",
        "清理临时文件...",
        "更新用户界面...",
        "加载新内容到缓存...",
        "完成数据流式传输...",
            "模拟API流式响应开始...",
        "处理用户身份验证...",
        "查询用户历史记录...",
        "分析用户兴趣偏好...",
        "生成个性化推荐列表...",
        "更新用户偏好设置...",
        "计算推荐准确率...",
        "处理异常数据点...",
        "生成分析报告...",
        "优化系统性能...",
        "备份重要数据...",
        "执行安全检查...",
        "完成数据处理任务...",
        "准备返回结果给用户...",
        "清理临时文件...",
        "更新用户界面...",
        "加载新内容到缓存...",
        "完成数据流式传输...",
            "模拟API流式响应开始...",
        "处理用户身份验证...",
        "查询用户历史记录...",
        "分析用户兴趣偏好...",
        "生成个性化推荐列表...",
        "更新用户偏好设置...",
        "计算推荐准确率...",
        "处理异常数据点...",
        "生成分析报告...",
        "优化系统性能...",
        "备份重要数据...",
        "执行安全检查...",
        "完成数据处理任务...",
        "准备返回结果给用户...",
        "清理临时文件...",
        "更新用户界面...",
        "加载新内容到缓存...",
        "完成数据流式传输...",
            "模拟API流式响应开始...",
        "处理用户身份验证...",
        "查询用户历史记录...",
        "分析用户兴趣偏好...",
        "生成个性化推荐列表...",
        "更新用户偏好设置...",
        "计算推荐准确率...",
        "处理异常数据点...",
        "生成分析报告...",
        "优化系统性能...",
        "备份重要数据...",
        "执行安全检查...",
        "完成数据处理任务...",
        "准备返回结果给用户...",
        "清理临时文件...",
        "更新用户界面...",
        "加载新内容到缓存...",
        "完成数据流式传输...",
            "模拟API流式响应开始...",
        "处理用户身份验证...",
        "查询用户历史记录...",
        "分析用户兴趣偏好...",
        "生成个性化推荐列表...",
        "更新用户偏好设置...",
        "计算推荐准确率...",
        "处理异常数据点...",
        "生成分析报告...",
        "优化系统性能...",
        "备份重要数据...",
        "执行安全检查...",
        "完成数据处理任务...",
        "准备返回结果给用户...",
        "清理临时文件...",
        "更新用户界面...",
        "加载新内容到缓存...",
        "完成数据流式传输...",
            "模拟API流式响应开始...",
        "处理用户身份验证...",
        "查询用户历史记录...",
        "分析用户兴趣偏好...",
        "生成个性化推荐列表...",
        "更新用户偏好设置...",
        "计算推荐准确率...",
        "处理异常数据点...",
        "生成分析报告...",
        "优化系统性能...",
        "备份重要数据...",
        "执行安全检查...",
        "完成数据处理任务...",
        "准备返回结果给用户...",
        "清理临时文件...",
        "更新用户界面...",
        "加载新内容到缓存...",
        "完成数据流式传输...",
            "模拟API流式响应开始...",
        "处理用户身份验证...",
        "查询用户历史记录...",
        "分析用户兴趣偏好...",
        "生成个性化推荐列表...",
        "更新用户偏好设置...",
        "计算推荐准确率...",
        "处理异常数据点...",
        "生成分析报告...",
        "优化系统性能...",
        "备份重要数据...",
        "执行安全检查...",
        "完成数据处理任务...",
        "准备返回结果给用户...",
        "清理临时文件...",
        "更新用户界面...",
        "加载新内容到缓存...",
        "完成数据流式传输...",
            "模拟API流式响应开始...",
        "处理用户身份验证...",
        "查询用户历史记录...",
        "分析用户兴趣偏好...",
        "生成个性化推荐列表...",
        "应用机器学习算法优化推荐...",
    };
    
    public VMStream(@NonNull Application application) {
        super(application);
        setupValidation();
    }
    
    private void setupValidation() {
        // 监听流状态变化，更新按钮状态
        isStreaming.observeForever(streaming -> {
            if (streaming != null) {
                startButtonEnabled.set(!streaming);
                stopButtonEnabled.set(streaming);
                
                // 只有在有内容且未在流式传输时才允许清除
                String content = streamContent.get();
                boolean hasContent = content != null && !content.trim().isEmpty();
                clearButtonEnabled.set(hasContent && !streaming);
            }
        });
        
        // 监听内容变化，更新清除按钮状态
        streamContent.observeForever(content -> {
            boolean hasContent = content != null && !content.trim().isEmpty();
            boolean notStreaming = !Boolean.TRUE.equals(isStreaming.get());
            clearButtonEnabled.set(hasContent && notStreaming);
        });
    }
    
    /**
     * 开始流式数据传输
     */
    public void startStreaming() {
        if (streamingFlag.get()) {
            return;
        }
        
        streamingFlag.set(true);
        isStreaming.set(true);
        streamStatus.set("正在传输...");
        streamProgress.set(0);
        setLoading(true);
        
        executorService.execute(() -> {
            try {
                final int totalSteps = SAMPLE_SENTENCES.length;
                StringBuilder contentBuilder = new StringBuilder();
                
                for (int i = 0; i < totalSteps && streamingFlag.get(); i++) {
                    // 模拟网络延迟
                    Thread.sleep(2 + random.nextInt(5)); // 0.5-1.5秒随机延迟
                    
                    // 添加新内容
                    final String newSentence = SAMPLE_SENTENCES[i];
                    contentBuilder.append(newSentence).append("\n");
                    
                    // 计算进度
                    final int currentIndex = i;
                    final int progress = (int) ((float) (currentIndex + 1) / totalSteps * 100);
                    final String currentContent = contentBuilder.toString();
                    
                    // 在主线程更新UI
                    mainHandler.post(() -> {
                        if (streamingFlag.get()) {
                            streamContent.set(currentContent);
                            streamProgress.set(progress);
                            
                            // 模拟实时状态更新
                            String status = String.format("已处理 %d/%d 项", currentIndex + 1, totalSteps);
                            streamStatus.set(status);
                            
                            Timber.tag("VMStream").d( "流式更新: " + newSentence);
                            Timber.tag("VMStream").d( "当前进度: " + progress + "%");
                        }
                    });
                }
                
                // 流式传输完成
                if (streamingFlag.get()) {
                    mainHandler.post(() -> {
                        finishStreaming("传输完成");
                    });
                }
                
            } catch (InterruptedException e) {
                mainHandler.post(() -> {
                    streamingFlag.set(false);
                    isStreaming.set(false);
                    setLoading(false);
                    setError("流式传输被中断");
                    streamStatus.set("传输中断");
                });
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                mainHandler.post(() -> {
                    streamingFlag.set(false);
                    isStreaming.set(false);
                    setLoading(false);
                    setError("流式传输失败: " + e.getMessage());
                    streamStatus.set("传输失败");
                });
            }
        });
    }
    
    /**
     * 停止流式数据传输
     */
    public void stopStreaming() {
        if (!streamingFlag.get()) {
            return;
        }
        
        streamingFlag.set(false);
        finishStreaming("传输已停止");
    }
    
    /**
     * 清除流式内容
     */
    public void clearStream() {
        if (Boolean.TRUE.equals(isStreaming.get())) {
            setError("无法在传输过程中清除内容");
            return;
        }
        
        streamContent.set("");
        streamProgress.set(0);
        streamStatus.set("内容已清除");
        setSuccess("内容已清除");
    }
    
    /**
     * 模拟实时API流数据
     */
    public void simulateApiStream() {
        if (streamingFlag.get()) {
            setError("当前正在进行流式传输");
            return;
        }
        
        streamingFlag.set(true);
        isStreaming.set(true);
        streamStatus.set("模拟API流...");
        streamProgress.set(0);
        setLoading(true);
        
        executorService.execute(() -> {
            try {
                // 模拟分块流式响应
                String[] apiResponses = {
                    "{\"type\":\"start\",\"message\":\"开始处理请求\"}",
                    "{\"type\":\"progress\",\"data\":\"用户身份验证成功\"}",
                    "{\"type\":\"progress\",\"data\":\"正在查询数据库\"}",
                    "{\"type\":\"progress\",\"data\":\"数据检索完成，共找到156条记录\"}",
                    "{\"type\":\"progress\",\"data\":\"开始数据处理和分析\"}",
                    "{\"type\":\"progress\",\"data\":\"应用机器学习算法\"}",
                    "{\"type\":\"progress\",\"data\":\"生成个性化结果\"}",
                    "{\"type\":\"progress\",\"data\":\"结果质量评估\"}",
                    "{\"type\":\"progress\",\"data\":\"准备返回最终结果\"}",
                    "{\"type\":\"complete\",\"result\":\"处理完成，结果已准备就绪\"}"
                };
                
                StringBuilder responseBuilder = new StringBuilder();
                
                for (int i = 0; i < apiResponses.length && streamingFlag.get(); i++) {
                    // 模拟API响应时间
                    Thread.sleep(800 + random.nextInt(700)); // 0.8-1.5秒
                    
                    final String response = apiResponses[i];
                    responseBuilder.append("【API响应 ").append(i + 1).append("】")
                            .append(response).append("\n\n");
                    
                    final int currentIndex = i;
                    final int progress = (int) ((float) (currentIndex + 1) / apiResponses.length * 100);
                    final String currentResponse = responseBuilder.toString();
                    
                    mainHandler.post(() -> {
                        if (streamingFlag.get()) {
                            streamContent.set(currentResponse);
                            streamProgress.set(progress);
                            streamStatus.set("API流处理中... " + progress + "%");
                            
                            Timber.tag("VMStream").d( "API流响应: " + response);
                        }
                    });
                }
                
                if (streamingFlag.get()) {
                    mainHandler.post(() -> {
                        finishStreaming("API流处理完成");
                    });
                }
                
            } catch (InterruptedException e) {
                mainHandler.post(() -> {
                    streamingFlag.set(false);
                    isStreaming.set(false);
                    setLoading(false);
                    setError("API流处理被中断");
                    streamStatus.set("处理中断");
                });
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                mainHandler.post(() -> {
                    streamingFlag.set(false);
                    isStreaming.set(false);
                    setLoading(false);
                    setError("API流处理失败: " + e.getMessage());
                    streamStatus.set("处理失败");
                });
            }
        });
    }
    
    /**
     * 完成流式传输
     */
    private void finishStreaming(String message) {
        streamingFlag.set(false);
        isStreaming.set(false);
        setLoading(false);
        streamStatus.set(message);
        setSuccess(message);
        
        Timber.tag("VMStream").i("流式传输完成: " + message);
    }
    
    // Getters for observable fields
    public ObservableField<String> getStreamContent() {
        return streamContent;
    }
    
    public ObservableField<Boolean> getIsStreaming() {
        return isStreaming;
    }
    
    public ObservableField<String> getStreamTitle() {
        return streamTitle;
    }
    
    public ObservableField<Integer> getStreamProgress() {
        return streamProgress;
    }
    
    public ObservableField<String> getStreamStatus() {
        return streamStatus;
    }
    
    public ObservableField<Boolean> getStartButtonEnabled() {
        return startButtonEnabled;
    }
    
    public ObservableField<Boolean> getStopButtonEnabled() {
        return stopButtonEnabled;
    }
    
    public ObservableField<Boolean> getClearButtonEnabled() {
        return clearButtonEnabled;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        
        // 停止流式传输
        streamingFlag.set(false);
        
        // 关闭线程池
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        // 清理观察者
        isStreaming.removeObserver(streaming -> {});
        streamContent.removeObserver(content -> {});
        
        Timber.tag("VMStream").d( "VMStream已清理完成");
    }
}