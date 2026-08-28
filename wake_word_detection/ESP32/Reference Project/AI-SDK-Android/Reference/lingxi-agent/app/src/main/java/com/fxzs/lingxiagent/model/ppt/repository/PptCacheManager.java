package com.fxzs.lingxiagent.model.ppt.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.fxzs.lingxiagent.model.ppt.dto.CoverListResponse;
import com.fxzs.lingxiagent.model.ppt.dto.OutlineItem;
import com.fxzs.lingxiagent.model.ppt.dto.PptProject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * PPT本地缓存管理器
 * 负责PPT相关数据的本地存储和读取
 */
public class PptCacheManager {
    
    private static final String PREF_PPT_CACHE = "ppt_cache";
    private static final String KEY_SAMPLE_TOPICS = "sample_topics";
    private static final String KEY_COVER_TEMPLATES = "cover_templates";
    private static final String KEY_MY_PROJECTS = "my_projects";
    private static final String KEY_CURRENT_OUTLINE = "current_outline";
    private static final String KEY_CURRENT_TASK_ID = "current_task_id";
    private static final String KEY_SELECTED_TEMPLATE_ID = "selected_template_id";
    
    private static PptCacheManager instance;
    private final SharedPreferences preferences;
    private final Gson gson;
    
    private PptCacheManager(Context context) {
        preferences = context.getSharedPreferences(PREF_PPT_CACHE, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    public static synchronized PptCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new PptCacheManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 缓存示例主题列表
     */
    public void cacheSampleTopics(List<String> topics) {
        String json = gson.toJson(topics);
        preferences.edit().putString(KEY_SAMPLE_TOPICS, json).apply();
    }
    
    /**
     * 获取缓存的示例主题列表
     */
    public List<String> getCachedSampleTopics() {
        String json = preferences.getString(KEY_SAMPLE_TOPICS, null);
        if (json != null) {
            Type type = new TypeToken<List<String>>(){}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }
    
    /**
     * 缓存封面模板列表
     */
    public void cacheCoverTemplates(List<CoverListResponse.CoverTemplate> templates) {
        String json = gson.toJson(templates);
        preferences.edit().putString(KEY_COVER_TEMPLATES, json).apply();
    }
    
    /**
     * 获取缓存的封面模板列表
     */
    public List<CoverListResponse.CoverTemplate> getCachedCoverTemplates() {
        String json = preferences.getString(KEY_COVER_TEMPLATES, null);
        if (json != null) {
            Type type = new TypeToken<List<CoverListResponse.CoverTemplate>>(){}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }
    
    /**
     * 缓存我的PPT项目列表
     */
    public void cacheMyProjects(List<PptProject> projects) {
        String json = gson.toJson(projects);
        preferences.edit().putString(KEY_MY_PROJECTS, json).apply();
    }
    
    /**
     * 获取缓存的我的PPT项目列表
     */
    public List<PptProject> getCachedMyProjects() {
        String json = preferences.getString(KEY_MY_PROJECTS, null);
        if (json != null) {
            Type type = new TypeToken<List<PptProject>>(){}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }
    
    /**
     * 缓存当前编辑的大纲
     */
    public void cacheCurrentOutline(List<OutlineItem> outline) {
        String json = gson.toJson(outline);
        preferences.edit().putString(KEY_CURRENT_OUTLINE, json).apply();
    }
    
    /**
     * 获取缓存的当前大纲
     */
    public List<OutlineItem> getCachedCurrentOutline() {
        String json = preferences.getString(KEY_CURRENT_OUTLINE, null);
        if (json != null) {
            Type type = new TypeToken<List<OutlineItem>>(){}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }
    
    /**
     * 缓存当前任务ID
     */
    public void cacheCurrentTaskId(String taskId) {
        preferences.edit().putString(KEY_CURRENT_TASK_ID, taskId).apply();
    }
    
    /**
     * 获取缓存的当前任务ID
     */
    public String getCachedCurrentTaskId() {
        return preferences.getString(KEY_CURRENT_TASK_ID, null);
    }
    
    /**
     * 缓存选中的模板ID
     */
    public void cacheSelectedTemplateId(String templateId) {
        preferences.edit().putString(KEY_SELECTED_TEMPLATE_ID, templateId).apply();
    }
    
    /**
     * 获取缓存的选中模板ID
     */
    public String getCachedSelectedTemplateId() {
        return preferences.getString(KEY_SELECTED_TEMPLATE_ID, null);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        preferences.edit().clear().apply();
    }
    
    /**
     * 清除当前会话缓存（保留长期缓存如示例主题、模板等）
     */
    public void clearSessionCache() {
        preferences.edit()
                .remove(KEY_CURRENT_OUTLINE)
                .remove(KEY_CURRENT_TASK_ID)
                .remove(KEY_SELECTED_TEMPLATE_ID)
                .apply();
    }
    
    /**
     * 检查缓存是否过期
     */
    public boolean isCacheExpired(String key, long maxAgeMillis) {
        long cacheTime = preferences.getLong(key + "_timestamp", 0);
        return System.currentTimeMillis() - cacheTime > maxAgeMillis;
    }
    
    /**
     * 更新缓存时间戳
     */
    public void updateCacheTimestamp(String key) {
        preferences.edit().putLong(key + "_timestamp", System.currentTimeMillis()).apply();
    }
    
    /**
     * 缓存是否存在
     */
    public boolean hasCachedData(String key) {
        return preferences.contains(key);
    }
}