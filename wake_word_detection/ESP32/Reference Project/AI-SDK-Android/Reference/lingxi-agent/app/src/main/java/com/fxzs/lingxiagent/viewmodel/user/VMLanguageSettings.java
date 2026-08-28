package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.cmdc.ai.assist.constraint.LanguageModel;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepository;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepositoryImpl;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.util.GlobalSettings;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class VMLanguageSettings extends BaseViewModel {

    private final MutableLiveData<Boolean> languageChanged = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> languageList = new MutableLiveData<>();
    // Repository
    private final ChatRepository chatRepository;
    // 语言映射表
    private Map<String, String> languageMap = new LinkedHashMap<>();
    private String TAG = VMLanguageSettings.class.getSimpleName();

    public VMLanguageSettings(@NonNull Application application) {
        super(application);
        chatRepository = new ChatRepositoryImpl();
        // 加载语言列表
        loadLanguageList();
    }

    public MutableLiveData<Boolean> getLanguageChanged() {
        return languageChanged;
    }

    public MutableLiveData<Map<String, String>> getLanguageList() {
        return languageList;
    }

    public void selectLanguage(String languageCode) {
        // 保存语言设置
        SharedPreferencesUtil.saveLanguageCode(languageCode);

        // 获取语言名称
        String languageName = languageMap.get(languageCode);
        if (languageName != null) {
            // 保存到全局设置
            GlobalSettings.getInstance().setSelectedLanguage(languageCode, languageName);
            SharedPreferencesUtil.saveLanguage(languageName);
        }

        // 通知语言已更改
        languageChanged.setValue(true);
    }

    public String getCurrentLanguage() {
        // 获取当前语言设置
        String languageCode = GlobalSettings.getInstance().getSelectedLanguageCode();
        if (languageCode == null) {
            languageCode = SharedPreferencesUtil.getLanguage();
        }
        return languageCode;
    }

    public Map<String, String> getLanguageMap() {
        return languageMap;
    }

    private void loadLanguageList() {
        setLoading(false);
//        chatRepository.getEngineModelType(new ChatRepository.Callback<Map<String, String>>() {
//            @Override
//            public void onSuccess(Map<String, String> data) {
//                setLoading(false);
//                if (data != null && !data.isEmpty()) {
//                    /*languageMap = data;
//                    languageList.setValue(data);*/
//                    Timber.tag(TAG).i(data.toString());
//                }
//                // 如果接口返回为空，使用默认值 todo
//                // 使用默认值
//                loadDefaultLanguageList();
//            }
//
//            @Override
//            public void onError(String error) {
//                setLoading(false);
//                setError("加载语言列表失败: " + error);
//                // 使用默认值
//                loadDefaultLanguageList();
//            }
//        });
        loadDefaultLanguageList();
    }

    private void loadDefaultLanguageList() {
        Map<String, String> defaultMap = new LinkedHashMap<>();
        defaultMap.put(String.valueOf(LanguageModel.CHINESE_MANDARIN_BASIC_PUNCTUATION.getPid()), LanguageModel.CHINESE_MANDARIN_BASIC_PUNCTUATION.getModel());
        defaultMap.put(String.valueOf(LanguageModel.CHINESE_MANDARIN_ENHANCED_PUNCTUATION.getPid()), LanguageModel.CHINESE_MANDARIN_ENHANCED_PUNCTUATION.getModel());
//        defaultMap.put(String.valueOf(LanguageModel.CHINESE_DIALECTS_BASIC_PUNCTUATION.getPid()), LanguageModel.CHINESE_DIALECTS_BASIC_PUNCTUATION.getModel());
        defaultMap.put(String.valueOf(LanguageModel.ENGLISH_NO_PUNCTUATION.getPid()), LanguageModel.ENGLISH_NO_PUNCTUATION.getModel());
        defaultMap.put(String.valueOf(LanguageModel.ENGLISH_ENHANCED_PUNCTUATION.getPid()), LanguageModel.ENGLISH_ENHANCED_PUNCTUATION.getModel());
        languageMap = defaultMap;
        languageList.setValue(defaultMap);
    }

    public String getLanguageDescription(String languageCode, String languageName) {
        // 根据语言代码返回描述
        if (languageCode.equals(String.valueOf(LanguageModel.CHINESE_MANDARIN_BASIC_PUNCTUATION.getPid())))
            return LanguageModel.CHINESE_MANDARIN_BASIC_PUNCTUATION.getDes();
        if (languageCode.equals(String.valueOf(LanguageModel.CHINESE_MANDARIN_ENHANCED_PUNCTUATION.getPid())))
            return LanguageModel.CHINESE_MANDARIN_ENHANCED_PUNCTUATION.getDes();
        if (languageCode.equals(String.valueOf(LanguageModel.CHINESE_DIALECTS_BASIC_PUNCTUATION.getPid())))
            return LanguageModel.CHINESE_DIALECTS_BASIC_PUNCTUATION.getDes();
        if (languageCode.equals(String.valueOf(LanguageModel.ENGLISH_NO_PUNCTUATION.getPid())))
            return LanguageModel.ENGLISH_NO_PUNCTUATION.getDes();
        if (languageCode.equals(String.valueOf(LanguageModel.ENGLISH_ENHANCED_PUNCTUATION.getPid())))
            return LanguageModel.ENGLISH_ENHANCED_PUNCTUATION.getDes();

        return languageName + "识别";

    }

    private void loadDefaultLanguageList_() {
        Map<String, String> defaultMap = new LinkedHashMap<>();
        defaultMap.put("zh_CN", "普通话");
        defaultMap.put("en", "英语");
        defaultMap.put("zh_HK", "粤语");
        defaultMap.put("zh_CN_en", "普方英");
        languageMap = defaultMap;
        languageList.setValue(defaultMap);
    }

    public String getLanguageDescription_(String languageCode, String languageName) {
        // 根据语言代码返回描述
        switch (languageCode) {
            case "zh_CN":
                return "中文普通话识别";
            case "en":
                return "英语识别";
            case "zh_HK":
            case "zh_TW":
                return "广东话识别";
            case "zh_CN_en":
                return "普通话、英语混合识别";
            default:
                // 如果没有特定描述，使用语言名称
                return languageName + "识别";
        }
    }
}