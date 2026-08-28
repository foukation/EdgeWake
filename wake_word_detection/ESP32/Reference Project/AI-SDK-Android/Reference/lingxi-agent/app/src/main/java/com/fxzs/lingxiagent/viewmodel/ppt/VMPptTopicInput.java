package com.fxzs.lingxiagent.viewmodel.ppt;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.ppt.dto.OutlineItem;
import com.fxzs.lingxiagent.model.ppt.repository.PptRepository;
import com.fxzs.lingxiagent.util.LoadingManager;

import java.util.List;

import timber.log.Timber;

public class VMPptTopicInput extends BaseViewModel {

    private final ObservableField<String> topicText = new ObservableField<>("");
    private final ObservableField<Boolean> sendButtonEnabled = new ObservableField<>(false);
    private final MutableLiveData<List<OutlineItem>> parsedOutline = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> networkStatus = new MutableLiveData<>();
    private final MutableLiveData<String> titleOptimized = new MutableLiveData<>();

    private final PptRepository pptRepository;
    private LoadingManager loadingManager;



    public VMPptTopicInput(@NonNull Application application) {
        super(application);

        pptRepository = PptRepository.getInstance();
        pptRepository.initCache(application);
        loadingManager = new LoadingManager(application);

        // 监听主题文本变化，更新发送按钮状态
        topicText.observeForever(text -> {
            boolean isValid = text != null && !text.trim().isEmpty() && text.length() <= 40;
            sendButtonEnabled.set(isValid);

            // 清除之前的错误信息
            if (isValid) {
                errorMessage.postValue("");
            }
        });

        // 监听网络状态
        updateNetworkStatus();
    }

    /**
     * 获取示例主题列表
     */
    public LiveData<List<String>> getSampleTopics() {
        return pptRepository.getSampleTopics();
    }

    /**
     * 选择示例主题
     */
    public void selectSampleTopic(String topic) {
        if (topic != null && !topic.isEmpty()) {
            topicText.set(topic);
        }
    }

    /**
     * 提交主题并开始生成大纲
     */
    public void submitTopicAndGenerateOutline() {
        if (!validateTopic()) {
            return;
        }

        String topic = getCurrentTopic();
        Timber.tag("VMPptTopicInput").d( "提交主题并开始生成大纲: " + topic);

        // 设置加载状态
        setLoading(true);
//        setSuccess("正在优化标题...");

        // 先获取优化后的标题，然后再进行后续操作
        getOptimizedTitle(topic);
    }

    /**
     * 获取优化后的标题
     * @param originalTopic 原始主题
     */
    private void getOptimizedTitle(String originalTopic) {
        Timber.tag("VMPptTopicInput").d( "开始获取优化标题: " + originalTopic);
        
        pptRepository.getPptTitle(originalTopic, new PptRepository.PptCallback<String>() {
            @Override
            public void onSuccess(String optimizedTitle) {
                Timber.tag("VMPptTopicInput").d( "获取优化标题成功: " + optimizedTitle);
                
                // 更新状态提示
//                setSuccess("正在准备生成大纲...");
                
                // 使用优化后的标题保存到状态管理器
                com.fxzs.lingxiagent.util.PptStateManager.getInstance(getApplication())
                    .createNewProject(optimizedTitle);

                // 清空之前的大纲缓存
                pptRepository.saveCurrentOutline(new java.util.ArrayList<>());

                Timber.tag("VMPptTopicInput").d( "标题优化完成，准备跳转到大纲编辑页面");
                
                // 通知Activity可以跳转了，传递优化后的标题
                notifyTitleOptimized(optimizedTitle);
            }

            @Override
            public void onError(String error) {
                Timber.tag("VMPptTopicInput").e( "获取优化标题失败: " + error);
                
                // 即使优化失败，也继续使用原始标题
//                setSuccess("正在准备生成大纲...");
                
                // 使用原始标题保存到状态管理器
                com.fxzs.lingxiagent.util.PptStateManager.getInstance(getApplication())
                    .createNewProject(originalTopic);

                // 清空之前的大纲缓存
                pptRepository.saveCurrentOutline(new java.util.ArrayList<>());

                Timber.tag("VMPptTopicInput").d( "使用原始标题，准备跳转到大纲编辑页面");
                
                // 通知Activity可以跳转了，使用原始标题
                notifyTitleOptimized(originalTopic);
            }
        });
    }

    /**
     * 通知标题优化完成
     * @param finalTitle 最终使用的标题
     */
    private void notifyTitleOptimized(String finalTitle) {
        // 更新当前主题为优化后的标题
        topicText.set(finalTitle);
        
        // 设置完成状态
        setLoading(false);
//        setSuccess("标题优化完成");
        
        // 通知Activity标题优化完成，传递优化后的标题
        titleOptimized.postValue(finalTitle);
        
        Timber.tag("VMPptTopicInput").d( "标题优化流程完成，最终标题: " + finalTitle);
    }





    /**
     * 验证内容是否有效（不为空且不只包含空格和换行符）
     */
    private boolean isValidContent(String content) {
        if (content == null) {
            return false;
        }
        // 去除所有空白字符（包括空格、制表符、换行符等）
        String trimmedContent = content.replaceAll("\\s+", "");
        return !trimmedContent.isEmpty();
    }

    /**
     * 验证主题输入
     */
    public boolean validateTopic() {
        String topic = topicText.get();
        Timber.tag("VMPptTopicInput").d( "validateTopic called, topic: " + topic);

        // 检查网络连接
        if (!pptRepository.isNetworkAvailable()) {
            errorMessage.postValue("网络连接不可用，请检查网络设置");
            return false;
        }

        if (!isValidContent(topic)) {
            Timber.tag("VMPptTopicInput").d( "主题内容无效，验证失败");
            errorMessage.postValue("请输入有效内容");
            return false;
        }

        if (topic.length() > 40) {
            Timber.tag("VMPptTopicInput").d( "主题长度超过40个字符，验证失败");
            errorMessage.postValue("主题长度不能超过40个字符");
            return false;
        }

        Timber.tag("VMPptTopicInput").d( "主题验证通过");
        errorMessage.postValue(""); // 清除错误信息
        return true;
    }

    /**
     * 更新网络状态
     */
    private void updateNetworkStatus() {
        if (pptRepository.isNetworkAvailable()) {
            String networkType = pptRepository.getNetworkType();
            networkStatus.postValue("网络已连接 (" + networkType + ")");
        } else {
            networkStatus.postValue("网络连接不可用");
        }
    }

    /**
     * 检查网络连接状态
     */
    public boolean checkNetworkConnection() {
        updateNetworkStatus();
        return pptRepository.isNetworkAvailable();
    }

    /**
     * 显示用户友好的错误信息
     */
    public void showError(String error) {
        errorMessage.postValue(error);
    }

    /**
     * 清除错误信息
     */
    public void clearError() {
        errorMessage.postValue("");
    }

    /**
     * 获取当前主题文本
     */
    public String getCurrentTopic() {
        return topicText.get() != null ? topicText.get().trim() : "";
    }

    /**
     * 清空输入
     */
    public void clearInput() {
        topicText.set("");
    }

    /**
     * 检查字符数是否超限
     */
    public boolean isCharacterLimitExceeded() {
        String topic = topicText.get();
        return topic != null && topic.length() > 40;
    }

    /**
     * 获取字符计数
     */
    public int getCharacterCount() {
        String topic = topicText.get();
        return topic != null ? topic.length() : 0;
    }

    // Getters
    public ObservableField<String> getTopicText() {
        return topicText;
    }

    public ObservableField<Boolean> getSendButtonEnabled() {
        return sendButtonEnabled;
    }

    public LiveData<List<OutlineItem>> getParsedOutline() {
        return parsedOutline;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public MutableLiveData<String> getNetworkStatus() {
        return networkStatus;
    }

    public LiveData<String> getTitleOptimized() {
        return titleOptimized;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 不再取消流式请求，因为流式请求现在在PptOutlineEditActivity中管理
        // 清理观察者
        topicText.removeObserver(text -> {});
        // 注意：不调用 pptRepository.cleanup()，因为 Repository 是单例，
        // 在 Activity 切换时不应该被清理，这会导致 SSE 流被意外取消
        Timber.tag("VMPptTopicInput").d( "ViewModel cleared - 保持Repository活跃以支持流式传输");
    }
}