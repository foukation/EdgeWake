package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.chat.dto.ModelTypeResponse;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepository;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepositoryImpl;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.util.GlobalSettings;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VMUserSettings extends BaseViewModel {
    
    // 双向绑定字段
    private final ObservableField<String> selectedModel = new ObservableField<>("");
    private final ObservableField<String> selectedLanguage = new ObservableField<>("");
    
    // 业务状态
    private final MutableLiveData<Integer> navigationTarget = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showModelDialog = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showLanguageDialog = new MutableLiveData<>();

    // Repository
    private final ChatRepository chatRepository;
    
    // 导航目标常量
    public static final int NAV_SECURITY = 1;
    
    // 可选模型列表
    private List<ModelTypeResponse.ModelItem> modelList = new ArrayList<>();
    private String[] availableModels = {};
    // 可选语言列表
    private Map<String, String> languageMap = new LinkedHashMap<>();
    private String[] availableLanguages = {};
    
    public VMUserSettings(@NonNull Application application) {
        super(application);
        chatRepository = new ChatRepositoryImpl();
        // 加载设置
        loadSettings();
        // 加载模型列表
        loadModelList();
        // 加载语言列表
        loadLanguageList();
    }
    
    // Getters
    public ObservableField<String> getSelectedModel() {
        return selectedModel;
    }
    
    public ObservableField<String> getSelectedLanguage() {
        return selectedLanguage;
    }
    
    public MutableLiveData<Integer> getNavigationTarget() {
        return navigationTarget;
    }
    
    public MutableLiveData<Boolean> getShowModelDialog() {
        return showModelDialog;
    }
    
    public MutableLiveData<Boolean> getShowLanguageDialog() {
        return showLanguageDialog;
    }
    
    public String[] getAvailableModels() {
        return availableModels;
    }
    
    public String[] getAvailableLanguages() {
        return availableLanguages;
    }
    
    // 业务方法
    public void showModelSelector() {
        showModelDialog.setValue(true);
    }
    
    public void showLanguageSelector() {
        showLanguageDialog.setValue(true);
    }
    
    public void selectModel(String model) {
        selectedModel.set(model);
        // 查找对应的模型代码并保存
        for (ModelTypeResponse.ModelItem item : modelList) {
            if (item.getModelName().equals(model)) {
                // 保存到全局设置
                GlobalSettings.getInstance().setSelectedModel(getApplication(), item.getModelCode(), item.getModelName());
                break;
            }
        }
        setSuccess("已切换到 " + model + " 模型");
    }
    
    public void selectLanguage(String language) {
        selectedLanguage.set(language);
        // 查找对应的语言代码并保存
        for (Map.Entry<String, String> entry : languageMap.entrySet()) {
            if (entry.getValue().equals(language)) {
                // 保存到全局设置
                GlobalSettings.getInstance().setSelectedLanguage(entry.getKey(), entry.getValue());
                break;
            }
        }
        setSuccess("语音识别语言已设置为 " + language);
    }
    
    public void navigateToSecurity() {
        navigationTarget.setValue(NAV_SECURITY);
    }
    
    public void clearNavigationTarget() {
        navigationTarget.setValue(null);
    }

    // 刷新显示数据
    public void refreshDisplayData() {
        loadSettings();
    }
    
    // 私有方法
    private void loadSettings() {
        GlobalSettings settings = GlobalSettings.getInstance();
        
        // 加载模型设置
        String modelName = settings.getSelectedModelName();
        if (modelName != null) {
            selectedModel.set(modelName);
        } else {
            // 如果全局设置为空，从 SharedPreferences 加载
            modelName = SharedPreferencesUtil.getModelDisplayName(getApplication());
            selectedModel.set(modelName);
        }
        
        // 加载语言设置
        String languageName = settings.getSelectedLanguageName();
        if (languageName != null) {
            selectedLanguage.set(languageName);
        } else {
            // 默认值
            selectedLanguage.set("普通话");
        }
    }
    
    private void saveSettings() {
        // 模拟保存设置到本地存储
        // TODO: 实际保存到SharedPreferences或数据库
    }
    
    private String getLanguageDisplayName(String language) {
        switch (language) {
            case "zh_CN":
                return "中文简体";
            case "zh_TW":
                return "中文繁體";
            case "en":
                return "English";
            default:
                return "中文简体";
        }
    }
    
    private void loadModelList() {
        setLoading(true);
        chatRepository.getModelTypeList(8, new ChatRepository.Callback<ModelTypeResponse>() {
            @Override
            public void onSuccess(ModelTypeResponse data) {
                setLoading(false);
                if (data != null && data.getList() != null) {
                    modelList = data.getList();
                    // 更新可用模型数组
                    availableModels = new String[modelList.size()];
                    for (int i = 0; i < modelList.size(); i++) {
                        availableModels[i] = modelList.get(i).getModelName();
                    }
                    // 更新当前选中的模型显示名称
                    updateSelectedModelDisplay();
                }
            }
            
            @Override
            public void onError(String error) {
                setLoading(false);
                // 如果加载失败，使用默认模型列表
                availableModels = new String[]{"DeepSeek R1", "豆包", "联通元景", "腾讯混元"};
            }
        });
    }
    
    private void updateSelectedModelDisplay() {
        String currentModelCode = GlobalSettings.getInstance().getSelectedModelCode();
        if (currentModelCode == null) {
            currentModelCode = SharedPreferencesUtil.getSelectedModel(getApplication());
        }
        for (ModelTypeResponse.ModelItem item : modelList) {
            if (item.getModelCode().equals(currentModelCode)) {
                selectedModel.set(item.getModelName());
                // 更新全局设置中的模型名称
                GlobalSettings.getInstance().setSelectedModel(getApplication(), item.getModelCode(), item.getModelName());
                break;
            }
        }
    }
    
    private void loadLanguageList() {
        // 更新当前选中的语言显示名称
        languageMap = GlobalSettings.getInstance().getLanguageList();
        updateSelectedLanguageDisplay();
//        chatRepository.getEngineModelType(new ChatRepository.Callback<Map<String, String>>() {
//            @Override
//            public void onSuccess(Map<String, String> data) {
//                if (data != null && !data.isEmpty()) {
//                    languageMap = data;
//                    // 更新可用语言数组
//                    availableLanguages = new String[languageMap.size()];
//                    int i = 0;
//                    for (String languageName : languageMap.values()) {
//                        availableLanguages[i++] = languageName;
//                    }
//                    // 更新当前选中的语言显示名称
//                    updateSelectedLanguageDisplay();
//                }
//            }
//
//            @Override
//            public void onError(String error) {
//                // 如果加载失败，使用默认语言列表
//                availableLanguages = new String[]{"普方英", "普通话", "英语", "粤语"};
//            }
//        });
    }
    
    private void updateSelectedLanguageDisplay() {
        String currentLanguageCode = GlobalSettings.getInstance().getSelectedLanguageCode();
        if (currentLanguageCode == null) {
            currentLanguageCode = SharedPreferencesUtil.getLanguageCode(getApplication());
        }
        String languageName = languageMap.get(currentLanguageCode);
        if (languageName != null) {
            selectedLanguage.set(languageName);
            // 更新全局设置中的语言名称
            GlobalSettings.getInstance().updateLanguageName(languageName);
        }
    }
}