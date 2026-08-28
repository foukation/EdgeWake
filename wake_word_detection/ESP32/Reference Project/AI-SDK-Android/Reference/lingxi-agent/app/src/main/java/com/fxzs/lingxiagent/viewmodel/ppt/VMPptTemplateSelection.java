package com.fxzs.lingxiagent.viewmodel.ppt;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.ppt.dto.CoverListResponse;
import com.fxzs.lingxiagent.model.ppt.dto.OutlineItem;
import com.fxzs.lingxiagent.model.ppt.dto.PptTemplate;
import com.fxzs.lingxiagent.model.ppt.dto.TaskStatus;
import com.fxzs.lingxiagent.model.ppt.repository.PptRepository;
import com.fxzs.lingxiagent.util.CrashSafetyHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class VMPptTemplateSelection extends BaseViewModel {
    
    private final MutableLiveData<List<PptTemplate>> templateList = new MutableLiveData<>(new ArrayList<>());
    private final ObservableField<String> selectedTemplateId = new ObservableField<>("");
    private final ObservableField<String> selectedColor = new ObservableField<>("全部");
    private final ObservableField<String> selectedStyle = new ObservableField<>("推荐");
    private final ObservableField<Boolean> generateButtonEnabled = new ObservableField<>(false);

    // 存储当前选中的模板对象，用于获取coverUrl
    private PptTemplate selectedTemplate;
    private final MutableLiveData<Integer> generationProgress = new MutableLiveData<>(0);
    private final MutableLiveData<String> generationStatus = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isGenerating = new MutableLiveData<>(false);
    // 空列表事件：交给页面层弹 GlobalToast
    private final MutableLiveData<Boolean> noMoreTemplatesEvent = new MutableLiveData<>();
    
    private final PptRepository pptRepository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private String currentTaskId;
    private Runnable pollingRunnable;
    private int nextPageNum = 1;
    
    // 颜色选项
    private final String[] colorOptions = {"全部", "红色", "橙色", "绿色", "橙红色", "蓝色", "紫色", "青色", "粉色"};
    
    // 风格选项
    private final String[] styleOptions = {"推荐", "简约商务", "卡通插画", "炫酷科技", "中国风", "水彩清新", "党务政务"};
    
    public VMPptTemplateSelection(@NonNull Application application) {
        super(application);

        // 先初始化pptRepository，确保不为null
        pptRepository = PptRepository.getInstance();

        try {
            pptRepository.initCache(application);

            selectedTemplateId.observeForever(id -> {
                generateButtonEnabled.set(id != null && !id.isEmpty());
            });
        } catch (Exception e) {
            Timber.tag("VMPptTemplateSelection").e( "初始化失败"+ e);
            setError("初始化失败: " + e.getMessage());
        }
    }

    public void loadTemplates() {
        try {
            setLoading(true);

            String color = selectedColor.get();
            String style = selectedStyle.get();

            // 处理"全部"选项，转换为null
            String apiColor = ("全部".equals(color)) ? null : color;
            String apiStyle = mapStyleToApiParam(style);

            Timber.tag("VMPptTemplateSelection").d( "加载模板: color=" + apiColor + ", style=" + apiStyle);

            if (pptRepository == null) {
                Timber.tag("VMPptTemplateSelection").e( "PptRepository为空，无法加载模板");
                setLoading(false);
                setError("系统初始化失败，请重启应用");
                loadDefaultTemplates();
                return;
            }

            pptRepository.getCoverList(apiColor, apiStyle, nextPageNum, 20, new PptRepository.PptCallback<CoverListResponse>() {
            @Override
            public void onSuccess(CoverListResponse response) {
                Timber.tag("VMPptTemplateSelection").d( "API调用成功，response: " + (response != null ? "not null" : "null"));
                setLoading(false);
                if (response != null && response.getRecords() != null) {
                    Timber.tag("VMPptTemplateSelection").d( "获取到模板数量: " + response.getRecords().size());
                    // 空列表：不更新当前列表，发事件给页面并重置页码
                    if (response.getRecords().isEmpty()) {

                        noMoreTemplatesEvent.postValue(true);
                        nextPageNum = 0;
                        return;
                    }
                    List<PptTemplate> templates = new ArrayList<>();
                    for (CoverListResponse.CoverTemplate record : response.getRecords()) {
                        PptTemplate template = new PptTemplate();
                        template.setId(record.getTemplateIndexId());
                        template.setName(record.getIndustry() + " - " + record.getStyle());

                        // 从detailImage JSON中提取titleCoverImageLarge的第一张图
                        String coverImageUrl = extractCoverImageUrl(record.getDetailImage());
                        Timber.tag("VMPptTemplateSelection").d( "模板 " + template.getId() + " 封面URL: " + coverImageUrl);
                        template.setThumbnailUrl(coverImageUrl);

                        template.setCategory(record.getIndustry());
                        template.setColor(record.getColor());
                        template.setStyle(record.getStyle());
                        templates.add(template);
                    }
                    Timber.tag("VMPptTemplateSelection").d( "处理完成，模板总数: " + templates.size());
                    templateList.postValue(templates);
                } else {
                    Timber.tag("VMPptTemplateSelection").e( "响应数据为空");
                    setError("获取模板列表失败：响应数据为空");
                }
            }

            @Override
            public void onError(String error) {
                Timber.tag("VMPptTemplateSelection").e( "API调用失败: " + error);
                setLoading(false);
                setError("加载模板失败: " + error);
                // 加载失败时显示默认模板
                loadDefaultTemplates();
            }
        });
        } catch (Exception e) {
            Timber.tag("VMPptTemplateSelection").e( "loadTemplates异常"+ e);
            setLoading(false);
            setError("加载模板时发生异常: " + e.getMessage());
            loadDefaultTemplates();
        }
    }

    /**
     * 从detailImage JSON中提取titleCoverImageLarge的第一张图
     */
    private String extractCoverImageUrl(String detailImageJson) {
        return CrashSafetyHelper.safeExecute("extractCoverImageUrl", () -> {
            if (CrashSafetyHelper.isEmpty(detailImageJson)) {
                return null;
            }

            JsonObject detailImage = gson.fromJson(detailImageJson, JsonObject.class);
            if (detailImage == null || !detailImage.has("titleCoverImageLarge")) {
                return null;
            }

            String titleCoverImageLarge = detailImage.get("titleCoverImageLarge").getAsString();
            if (CrashSafetyHelper.isEmpty(titleCoverImageLarge)) {
                return null;
            }

            // 如果是数组格式，取第一个
            if (titleCoverImageLarge.startsWith("[")) {
                String[] urls = gson.fromJson(titleCoverImageLarge, String[].class);
                if (urls != null && urls.length > 0) {
                    return urls[0];
                }
            } else {
                return titleCoverImageLarge;
            }

            return null;
        }, null);
    }

    private void loadDefaultTemplates() {
        List<PptTemplate> templates = new ArrayList<>();
        templates.add(createDefaultTemplate("1", "运营年终工作总结", "商务"));
        templates.add(createDefaultTemplate("2", "工程项目进度汇报", "简约"));
        templates.add(createDefaultTemplate("3", "年度工作回顾", "创意"));
        templates.add(createDefaultTemplate("4", "企业品牌介绍", "商务"));
        templates.add(createDefaultTemplate("5", "功能简介", "清新"));
        templates.add(createDefaultTemplate("6", "名片内联官网", "扁平"));
        templates.add(createDefaultTemplate("7", "多身份切换", "插画"));
        templates.add(createDefaultTemplate("8", "多种分享模式及物料", "卡通"));
        
        templateList.postValue(templates);
    }
    
    private PptTemplate createDefaultTemplate(String id, String name, String style) {
        PptTemplate template = new PptTemplate();
        template.setId(id);
        template.setName(name);
        template.setStyle(style);
        template.setColor(selectedColor.get());
        template.setCategory("默认");
        return template;
    }
    
    public void refreshTemplates() {
        // 当前页码+1，超过15页时重置为1
        nextPageNum = nextPageNum >= 15 ? 1 : nextPageNum + 1;
        Timber.tag("VMPptTemplateSelection").d( "换一组模板，当前页码: " + nextPageNum);
        loadTemplates();
    }
    
    public void selectTemplate(String templateId) {
        selectedTemplateId.set(templateId);

        // 同时保存完整的模板对象
        List<PptTemplate> templates = templateList.getValue();
        if (templates != null) {
            for (PptTemplate template : templates) {
                if (templateId.equals(template.getId())) {
                    selectedTemplate = template;
                    Timber.tag("VMPptTemplateSelection").d( "选中模板: " + templateId + ", coverUrl: " + template.getCoverUrl());
                    break;
                }
            }
        }
    }
    
    public void setSelectedColor(String color) {
        selectedColor.set(color);
        // 颜色改变时重新加载模板，重置为第1页
        nextPageNum = 1;
        loadTemplates();
    }
    
    public void setSelectedStyle(String style) {
        selectedStyle.set(style);
        // 风格改变时重新加载模板，重置为第1页
        nextPageNum = 1;
        loadTemplates();
    }
    
    public String[] getColorOptions() {
        return colorOptions;
    }
    
    public String[] getStyleOptions() {
        return styleOptions;
    }
    
    public MutableLiveData<PptGenerationResult> generatePpt(String topic) {
        MutableLiveData<PptGenerationResult> result = new MutableLiveData<>();
        
        String templateId = selectedTemplateId.get();
        if (templateId == null || templateId.isEmpty()) {
            result.postValue(new PptGenerationResult(false, null, "请先选择模板"));
            return result;
        }
        
        setLoading(true);
        isGenerating.postValue(true);
        generationProgress.postValue(0);
        generationStatus.postValue("准备生成PPT...");
        
        // 获取当前大纲数据
        List<OutlineItem> outlineItems = pptRepository.getCurrentOutline();
        if (outlineItems.isEmpty()) {
            result.postValue(new PptGenerationResult(false, null, "大纲数据为空"));
            setLoading(false);
            isGenerating.postValue(false);
            return result;
        }
        
        // 构建PPT生成请求
        Map<String, Object> customData = buildCustomData(topic, outlineItems);

        // 获取coverUrl和sessionId
        String coverUrl = selectedTemplate != null ? selectedTemplate.getCoverUrl() : null;
        String sessionId = pptRepository.getCurrentSessionId();

        Timber.tag("VMPptTemplateSelection").d( "提交PPT生成任务 - templateId: " + templateId +
                          ", coverUrl: " + coverUrl + ", sessionId: " + sessionId);

        pptRepository.submitPptTask(templateId, coverUrl, sessionId, customData, new PptRepository.PptCallback<String>() {
            @Override
            public void onSuccess(String taskId) {
                currentTaskId = taskId;
                generationStatus.postValue("PPT生成任务已提交，开始生成...");
                startPollingTaskStatus(result);
            }
            
            @Override
            public void onError(String error) {
                setLoading(false);
                isGenerating.postValue(false);
                result.postValue(new PptGenerationResult(false, null, "提交生成任务失败: " + error));
            }
        });
        
        return result;
    }
    
    private Map<String, Object> buildCustomData(String topic, List<OutlineItem> outlineItems) {
        Map<String, Object> customData = new HashMap<>();
        customData.put("title", topic);
        customData.put("subTitle", "");
        customData.put("author", "");
        
        List<Map<String, Object>> catalogs = new ArrayList<>();
        for (OutlineItem item : outlineItems) {
            Map<String, Object> catalog = new HashMap<>();
            catalog.put("catalog", item.getTitle());
            
            List<String> subCatalogs = new ArrayList<>();
            if (item.getContent() != null && !item.getContent().isEmpty()) {
                // 将内容按行分割作为子目录
                String[] lines = item.getContent().split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        subCatalogs.add(line);
                    }
                }
            }
            if (subCatalogs.isEmpty()) {
                subCatalogs.add(item.getTitle());
            }
            catalog.put("sub_catalog", subCatalogs);
            catalogs.add(catalog);
        }
        
        customData.put("catalogs", catalogs);
        return customData;
    }
    
    private void startPollingTaskStatus(MutableLiveData<PptGenerationResult> result) {
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentTaskId != null) {
                    pptRepository.checkTaskStatus(currentTaskId, new PptRepository.PptCallback<TaskStatus>() {
                        @Override
                        public void onSuccess(TaskStatus status) {
                            updateGenerationProgress(status);
                            
                            if ("done".equals(status.getPptStatus())) {
                                // PPT生成完成
                                setLoading(false);
                                isGenerating.postValue(false);
                                generationStatus.postValue("PPT生成完成！");
                                result.postValue(new PptGenerationResult(true, status.getPptUrl(), null));
                            } else if ("build_failed".equals(status.getPptStatus())) {
                                // PPT生成失败
                                setLoading(false);
                                isGenerating.postValue(false);
                                String errorMsg = status.getErrMsg() != null ? status.getErrMsg() : "PPT生成失败";
                                result.postValue(new PptGenerationResult(false, null, errorMsg));
                            } else {
                                // 继续轮询
                                mainHandler.postDelayed(pollingRunnable, 2000);
                            }
                        }
                        
                        @Override
                        public void onError(String error) {
                            // 轮询出错，继续尝试
                            mainHandler.postDelayed(pollingRunnable, 3000);
                        }
                    });
                }
            }
        };
        
        mainHandler.postDelayed(pollingRunnable, 1000);
    }
    
    private void updateGenerationProgress(TaskStatus status) {
        if (status.getTotalPages() > 0) {
            int progress = (status.getDonePages() * 100) / status.getTotalPages();
            generationProgress.postValue(progress);
            generationStatus.postValue(String.format("正在生成PPT... (%d/%d页)", 
                status.getDonePages(), status.getTotalPages()));
        } else {
            generationStatus.postValue("正在生成PPT...");
        }
    }
    
    public void stopGeneration() {
        if (pollingRunnable != null) {
            mainHandler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
        }
        currentTaskId = null;
        setLoading(false);
        isGenerating.postValue(false);
        generationStatus.postValue("已停止生成");
    }
    
    public MutableLiveData<List<PptTemplate>> getTemplateList() {
        return templateList;
    }
    
    public ObservableField<String> getSelectedTemplateId() {
        return selectedTemplateId;
    }
    
    public ObservableField<String> getSelectedColor() {
        return selectedColor;
    }
    
    public ObservableField<String> getSelectedStyle() {
        return selectedStyle;
    }
    
    public ObservableField<Boolean> getGenerateButtonEnabled() {
        return generateButtonEnabled;
    }
    
    public MutableLiveData<Integer> getGenerationProgress() {
        return generationProgress;
    }
    
    public MutableLiveData<String> getGenerationStatus() {
        return generationStatus;
    }
    
    public MutableLiveData<Boolean> getIsGenerating() {
        return isGenerating;
    }
    
    public MutableLiveData<String> getErrorMessage() {
        return getError();
    }
    
    public MutableLiveData<String> getSuccessMessage() {
        return getSuccess();
    }

    public MutableLiveData<Boolean> getNoMoreTemplatesEvent() {
        return noMoreTemplatesEvent;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        stopGeneration();
        selectedTemplateId.removeObserver(id -> {});
    }
    
    public static class PptGenerationResult {
        private final boolean success;
        private final String pptUrl;
        private final String errorMessage;
        
        public PptGenerationResult(boolean success, String pptUrl, String errorMessage) {
            this.success = success;
            this.pptUrl = pptUrl;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() { return success; }
        public String getPptUrl() { return pptUrl; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * 将UI显示的风格名称映射到API参数
     */
    private String mapStyleToApiParam(String uiStyle) {
        if (uiStyle == null || "全部".equals(uiStyle)) {
            return null;
        }

        // 映射UI风格名称到API参数
        switch (uiStyle) {
            case "推荐":
                return null; // 推荐相当于不筛选
            case "简约商务":
                return "商务";
            case "卡通插画":
                return "卡通";
            case "炫酷科技":
                return "科技";
            case "中国风":
                return "国风";
            case "水彩清新":
                return "清新";
            case "党务政务":
                return "政务";
            case "其他":
                return "其他";
            default:
                Timber.tag("VMPptTemplateSelection").w("未知的风格类型: " + uiStyle);
                return uiStyle; // 如果没有映射，直接使用原值
        }
    }
}