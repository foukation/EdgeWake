package com.fxzs.lingxiagent.lingxi.translate.util;

import android.text.TextUtils;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections; // 新增：用于创建线程安全的有序Map

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

/**
 * 语言工具类 - 支持45种语言
 * 优化点：修复语言顺序不一致问题，保证和接口返回顺序一致
 */
public class LanguageUtils {
    // 单例实例（volatile 保证可见性）
    private static volatile LanguageUtils INSTANCE;

    // 基础语言映射（本地默认，有序）
    private static final Map<String, String> DEFAULT_LANGUAGE_MAP = new LinkedHashMap<>();

    // 核心修改1：替换缓存容器为「有序 + 线程安全」的 LinkedHashMap
    // 使用 Collections.synchronizedMap 包装 LinkedHashMap，既保序又线程安全
    private final Map<String, String> sourceLanguageCache = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, String> targetLanguageCache = Collections.synchronizedMap(new LinkedHashMap<>());

    // SP 存储 KEY
    private static final String KEY_TARGET_LANGUAGES = "target_languages";
    private static final String KEY_SOURCE_LANGUAGES = "source_languages";
    // 复用 Gson 实例
    private static final Gson GSON = new Gson();

    // 静态代码块初始化默认语言列表（仅执行一次）
    static {
        DEFAULT_LANGUAGE_MAP.put("zh", "中文");
        DEFAULT_LANGUAGE_MAP.put("en", "英语");
       // DEFAULT_LANGUAGE_MAP.put("yue", "粤语");
        DEFAULT_LANGUAGE_MAP.put("jp", "日语");
        DEFAULT_LANGUAGE_MAP.put("kor", "韩语");
//        DEFAULT_LANGUAGE_MAP.put("fra", "法语");
//        DEFAULT_LANGUAGE_MAP.put("spa", "西班牙语");
//        DEFAULT_LANGUAGE_MAP.put("th", "泰语");
//        DEFAULT_LANGUAGE_MAP.put("ara", "阿拉伯语");
//        DEFAULT_LANGUAGE_MAP.put("ru", "俄语");
//        DEFAULT_LANGUAGE_MAP.put("pt", "葡萄牙语");
//        DEFAULT_LANGUAGE_MAP.put("de", "德语");
//        DEFAULT_LANGUAGE_MAP.put("it", "意大利语");
//        DEFAULT_LANGUAGE_MAP.put("el", "希腊语");
//        DEFAULT_LANGUAGE_MAP.put("nl", "荷兰语");
//        DEFAULT_LANGUAGE_MAP.put("pl", "波兰语");
//        DEFAULT_LANGUAGE_MAP.put("bul", "保加利亚语");
//        DEFAULT_LANGUAGE_MAP.put("dan", "丹麦语");
//        DEFAULT_LANGUAGE_MAP.put("fin", "芬兰语");
//        DEFAULT_LANGUAGE_MAP.put("cs", "捷克语");
//        DEFAULT_LANGUAGE_MAP.put("rom", "罗马尼亚语");
//        DEFAULT_LANGUAGE_MAP.put("swe", "瑞典语");
//        DEFAULT_LANGUAGE_MAP.put("hu", "匈牙利语");
//        DEFAULT_LANGUAGE_MAP.put("vie", "越南语");
//        DEFAULT_LANGUAGE_MAP.put("id", "印度尼西亚语");
//        DEFAULT_LANGUAGE_MAP.put("cat", "加泰罗尼亚语");
//        DEFAULT_LANGUAGE_MAP.put("heb", "希伯来语");
//        DEFAULT_LANGUAGE_MAP.put("hi", "印地语");
//        DEFAULT_LANGUAGE_MAP.put("may", "马来语");
//        DEFAULT_LANGUAGE_MAP.put("nor", "挪威语");
//        DEFAULT_LANGUAGE_MAP.put("ice", "冰岛语");
//        DEFAULT_LANGUAGE_MAP.put("fil", "菲律宾语");
//        DEFAULT_LANGUAGE_MAP.put("hkm", "高棉语");
//        DEFAULT_LANGUAGE_MAP.put("hrv", "克罗地亚语");
//        DEFAULT_LANGUAGE_MAP.put("lav", "拉脱维亚语");
//        DEFAULT_LANGUAGE_MAP.put("ben", "孟加拉语");
//        DEFAULT_LANGUAGE_MAP.put("nep", "尼泊尔语");
//        DEFAULT_LANGUAGE_MAP.put("afr", "南非荷兰语");
//        DEFAULT_LANGUAGE_MAP.put("sk", "斯洛伐克语");
//        DEFAULT_LANGUAGE_MAP.put("sin", "僧伽罗语");
//        DEFAULT_LANGUAGE_MAP.put("srp", "塞尔维亚语");
//        DEFAULT_LANGUAGE_MAP.put("swa", "斯瓦希里语");
//        DEFAULT_LANGUAGE_MAP.put("tr", "土耳其语");
//        DEFAULT_LANGUAGE_MAP.put("ukr", "乌克兰语");
//        DEFAULT_LANGUAGE_MAP.put("arm", "亚美尼亚语");
    }

    // 私有构造方法
    private LanguageUtils() {
        loadCacheFromSP();
    }

    // 双重检查锁单例
    public static LanguageUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (LanguageUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LanguageUtils();
                }
            }
        }
        return INSTANCE;
    }

    // ------------------------------ 对外方法（不变） ------------------------------
    public String getTargetLanguageName(String languageCode) {
        return getInstance().getTargetLanguageNameInternal(languageCode);
    }

    public String getSourceLanguageName(String languageCode) {
        return getInstance().getSourceLanguageNameInternal(languageCode);
    }

    public String getTargetLanguageCode(String languageName) {
        return getLanguageCodeFromMap(languageName, targetLanguageCache);
    }

    public String getSourceLanguageCode(String languageName) {
        return getLanguageCodeFromMap(languageName, sourceLanguageCache);
    }

    public String getSourceLanguagesName(String code) {
        return getLanguageNameFromMap(code, sourceLanguageCache);
    }

    public String getTargetLanguagesName(String code) {
        return getLanguageNameFromMap(code, targetLanguageCache);
    }

    public List<String> getTargetLanguagesNames() {
        return new ArrayList<>(targetLanguageCache.values());
    }

    public List<String> getTargetLanguageCodes() {
        return new ArrayList<>(targetLanguageCache.keySet());
    }

    public List<String> getSourceLanguagesNames() {
        return new ArrayList<>(sourceLanguageCache.values());
    }

    public List<String> getSourceLanguageCodes() {
        return new ArrayList<>(sourceLanguageCache.keySet());
    }

    public void requestAllLanguages() {
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.getAllLanguages(new Observer<ApiResponse<LanguageListDto>>() {
            @Override
            public void onSubscribe(Disposable d) {}

            @Override
            public void onNext(ApiResponse<LanguageListDto> response) {
                Timber.tag("Languages").i("获取语言列表成功: %s", response);
                if (response != null && response.getData() != null) {
                    // 保存到 SP + 更新内存缓存
                    saveLanguagesToSP(KEY_TARGET_LANGUAGES, response.getData().getTargetLanguages());
                    saveLanguagesToSP(KEY_SOURCE_LANGUAGES, response.getData().getSourceLanguages());
                    // 更新内存缓存（覆盖旧数据，保持接口返回顺序）
                    updateLanguageCache(sourceLanguageCache, response.getData().getSourceLanguages());
                    updateLanguageCache(targetLanguageCache, response.getData().getTargetLanguages());
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.tag("Languages").e(e, "获取语言列表失败");
            }

            @Override
            public void onComplete() {}
        });
    }

    // ------------------------------ 内部核心方法（关键修改） ------------------------------
    /**
     * 从 SP 加载缓存数据到内存
     */
    private void loadCacheFromSP() {
        // 加载源语言
        List<LanguageListDto.LanguageInfo> sourceList = getLanguagesFromSP(KEY_SOURCE_LANGUAGES);
        updateLanguageCache(sourceLanguageCache, sourceList);
        // 加载目标语言
        List<LanguageListDto.LanguageInfo> targetList = getLanguagesFromSP(KEY_TARGET_LANGUAGES);
        updateLanguageCache(targetLanguageCache, targetList);

        // 兜底：如果 SP 无数据，使用默认语言列表（此时 LinkedHashMap 保证顺序）
        if (sourceLanguageCache.isEmpty()) {
            sourceLanguageCache.putAll(DEFAULT_LANGUAGE_MAP);
        }
        if (targetLanguageCache.isEmpty()) {
            targetLanguageCache.putAll(DEFAULT_LANGUAGE_MAP);
        }
    }

    /**
     * 更新语言缓存（核心：遍历 List 时严格按接口返回顺序插入，保证顺序不变）
     */
    private void updateLanguageCache(Map<String, String> cacheMap, List<LanguageListDto.LanguageInfo> infoList) {
        if (infoList == null || infoList.isEmpty()) {
            return;
        }
        // 清空旧缓存
        cacheMap.clear();
        // 核心：按 infoList 的原始顺序遍历插入，LinkedHashMap 会保留这个顺序
        for (LanguageListDto.LanguageInfo info : infoList) {
            if (info != null && !TextUtils.isEmpty(info.getCode()) && !TextUtils.isEmpty(info.getName())) {
                cacheMap.put(info.getCode(), info.getName());
            }
        }
    }

    /**
     * 从 SP 读取语言列表（统一兜底，避免返回 null）
     */
    private List<LanguageListDto.LanguageInfo> getLanguagesFromSP(String key) {
        String json = SharedPreferencesUtil.getString(key, null);
        if (TextUtils.isEmpty(json)) {
            return new ArrayList<>(); // 返回空列表，避免 NPE
        }
        try {
            // Gson 解析 List 时会保留原始顺序
            return GSON.fromJson(json, new TypeToken<List<LanguageListDto.LanguageInfo>>() {}.getType());
        } catch (Exception e) {
            Timber.e(e, "解析语言列表失败: %s", key);
            return new ArrayList<>();
        }
    }

    /**
     * 保存语言列表到 SP（异步操作，Gson 序列化 List 会保留顺序）
     */
    private void saveLanguagesToSP(String key, List<LanguageListDto.LanguageInfo> languages) {
        if (languages == null || languages.isEmpty()) {
            return;
        }
        try {
            // Gson 序列化 List 时，会严格按 List 的顺序存储
            String json = GSON.toJson(languages);
            SharedPreferencesUtil.saveString(key, json);
        } catch (Exception e) {
            Timber.e(e, "保存语言列表失败: %s", key);
        }
    }

    /**
     * 通用：从 Map 中根据名称找代码（按插入顺序遍历）
     */
    private String getLanguageCodeFromMap(String languageName, Map<String, String> languageMap) {
        if (TextUtils.isEmpty(languageName)) {
            return "zh";
        }
        // 按 LinkedHashMap 的插入顺序遍历，和接口返回顺序一致
        for (Map.Entry<String, String> entry : languageMap.entrySet()) {
            if (languageName.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return "zh";
    }

    private String getLanguageNameFromMap(String languageCode, Map<String, String> languageMap) {
        if (TextUtils.isEmpty(languageCode)) {
            return "中文";
        }
        return languageMap.getOrDefault(languageCode, "中文");
    }

    // ------------------------------ 内部实现 ------------------------------
    private String getTargetLanguageNameInternal(String languageCode) {
        return getLanguageNameFromMap(languageCode, targetLanguageCache);
    }

    private String getSourceLanguageNameInternal(String languageCode) {
        return getLanguageNameFromMap(languageCode, sourceLanguageCache);
    }
}