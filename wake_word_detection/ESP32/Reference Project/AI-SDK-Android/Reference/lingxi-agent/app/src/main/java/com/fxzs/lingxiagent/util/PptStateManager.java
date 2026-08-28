package com.fxzs.lingxiagent.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.fxzs.lingxiagent.model.ppt.dto.OutlineItem;
import com.fxzs.lingxiagent.model.ppt.dto.PptProject;
import com.fxzs.lingxiagent.model.ppt.dto.PptStatus;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import timber.log.Timber;

/**
 * PPT全局状态管理器
 * 负责管理PPT项目的状态、数据传递和持久化
 */
public class PptStateManager {
    
    private static final String TAG = "PptStateManager";
    private static final String PREFS_NAME = "ppt_state_prefs";
    private static final String KEY_CURRENT_PROJECT = "current_project";
    private static final String KEY_PROJECT_HISTORY = "project_history";
    private static final String KEY_NAVIGATION_STACK = "navigation_stack";
    
    // Intent Extra Keys
    public static final String EXTRA_PPT_ID = "ppt_id";
    public static final String EXTRA_TOPIC = "topic";
    public static final String EXTRA_TEMPLATE_ID = "template_id";
    public static final String EXTRA_IS_GENERATING = "is_generating";
    public static final String EXTRA_CURRENT_SLIDE = "current_slide";
    public static final String EXTRA_NAVIGATION_SOURCE = "navigation_source";
    public static final String EXTRA_PROJECT_DATA = "project_data";
    public static final String EXTRA_PPT_URL = "ppt_url";
    
    // Navigation Sources
    public static final String SOURCE_TOPIC_INPUT = "topic_input";
    public static final String SOURCE_OUTLINE_EDIT = "outline_edit";
    public static final String SOURCE_TEMPLATE_SELECTION = "template_selection";
    public static final String SOURCE_PREVIEW = "preview";
    
    private static PptStateManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    
    // 当前项目状态
    private PptProject currentProject;
    private final Map<String, Object> sessionData = new HashMap<>();
    private final List<String> navigationStack = new ArrayList<>();
    
    private PptStateManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadCurrentProject();
        loadNavigationStack();
    }
    
    public static synchronized PptStateManager getInstance(Context context) {
        if (instance == null) {
            instance = new PptStateManager(context);
        }
        return instance;
    }
    
    /**
     * 创建新的PPT项目
     */
    public PptProject createNewProject(String topic) {
        String projectId = UUID.randomUUID().toString();
        currentProject = new PptProject(projectId, topic, topic);
        currentProject.setStatus(PptStatus.OUTLINE_GENERATING);
        
        saveCurrentProject();
        Timber.tag(TAG).d( "创建新项目: " + projectId + ", 主题: " + topic);
        
        return currentProject;
    }
    
    /**
     * 更新当前项目状态
     */
    public void updateProjectStatus(PptStatus status) {
        if (currentProject != null) {
            currentProject.setStatus(status);
            currentProject.setUpdatedAt(new Date());
            saveCurrentProject();
            Timber.tag(TAG).d( "更新项目状态: " + status);
        }
    }
    
    /**
     * 更新项目大纲
     */
    public void updateProjectOutline(List<OutlineItem> outline) {
        if (currentProject != null) {
            currentProject.setOutline(outline);
            currentProject.setUpdatedAt(new Date());
            saveCurrentProject();
            Timber.tag(TAG).d( "更新项目大纲，项目数: " + (outline != null ? outline.size() : 0));
        }
    }
    
    /**
     * 更新项目模板
     */
    public void updateProjectTemplate(String templateId) {
        if (currentProject != null) {
            currentProject.setTemplateId(templateId);
            currentProject.setUpdatedAt(new Date());
            saveCurrentProject();
            Timber.tag(TAG).d( "更新项目模板: " + templateId);
        }
    }

    /**
     * 更新当前项目主题
     */
    public void updateCurrentTopic(String topic) {
        if (currentProject != null) {
            currentProject.setTopic(topic);
            currentProject.setTitle(topic); // 同时更新标题
            currentProject.setUpdatedAt(new Date());
            saveCurrentProject();
            Timber.tag(TAG).d( "更新项目主题: " + topic);
        }
    }
    
    /**
     * 获取当前项目
     */
    public PptProject getCurrentProject() {
        return currentProject;
    }
    
    /**
     * 设置当前项目
     */
    public void setCurrentProject(PptProject project) {
        this.currentProject = project;
        saveCurrentProject();
    }
    
    /**
     * 创建Intent Bundle用于Activity间数据传递
     */
    public Bundle createNavigationBundle(String source, String destination) {
        Bundle bundle = new Bundle();
        
        if (currentProject != null) {
            bundle.putString(EXTRA_PPT_ID, currentProject.getId());
            bundle.putString(EXTRA_TOPIC, currentProject.getTopic());
            bundle.putString(EXTRA_TEMPLATE_ID, currentProject.getTemplateId());
            bundle.putBoolean(EXTRA_IS_GENERATING, currentProject.isGenerating());
            
            // 序列化项目数据
            String projectJson = gson.toJson(currentProject);
            bundle.putString(EXTRA_PROJECT_DATA, projectJson);
        }
        
        bundle.putString(EXTRA_NAVIGATION_SOURCE, source);
        
        // 记录导航路径
        addToNavigationStack(destination);
        
        Timber.tag(TAG).d( "创建导航Bundle: " + source + " -> " + destination);
        return bundle;
    }
    
    /**
     * 从Bundle恢复项目数据
     */
    public void restoreFromBundle(Bundle bundle) {
        if (bundle == null) return;
        
        String projectJson = bundle.getString(EXTRA_PROJECT_DATA);
        if (projectJson != null) {
            try {
                PptProject project = gson.fromJson(projectJson, PptProject.class);
                if (project != null) {
                    this.currentProject = project;
                    Timber.tag(TAG).d( "从Bundle恢复项目数据: " + project.getId());
                }
            } catch (Exception e) {
                Timber.tag(TAG).e( "恢复项目数据失败", e);
            }
        }
        
        // 恢复其他数据
        String pptId = bundle.getString(EXTRA_PPT_ID);
        String topic = bundle.getString(EXTRA_TOPIC);
        if (pptId != null && currentProject == null) {
            // 如果没有完整项目数据，创建基础项目
            currentProject = new PptProject(pptId, topic, topic);
        }
    }
    
    /**
     * 保存会话数据
     */
    public void saveSessionData(String key, Object value) {
        sessionData.put(key, value);
        Timber.tag(TAG).d( "保存会话数据: " + key);
    }
    
    /**
     * 获取会话数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getSessionData(String key, Class<T> type) {
        Object value = sessionData.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * 清除会话数据
     */
    public void clearSessionData() {
        sessionData.clear();
        Timber.tag(TAG).d( "清除会话数据");
    }
    
    /**
     * 添加到导航栈
     */
    private void addToNavigationStack(String destination) {
        navigationStack.add(destination);
        if (navigationStack.size() > 10) {
            navigationStack.remove(0); // 保持栈大小
        }
        saveNavigationStack();
    }
    
    /**
     * 获取上一个页面
     */
    public String getPreviousPage() {
        if (navigationStack.size() >= 2) {
            return navigationStack.get(navigationStack.size() - 2);
        }
        return null;
    }
    
    /**
     * 清除导航栈
     */
    public void clearNavigationStack() {
        navigationStack.clear();
        saveNavigationStack();
    }
    
    /**
     * 保存当前项目到SharedPreferences
     */
    private void saveCurrentProject() {
        if (currentProject != null) {
            String projectJson = gson.toJson(currentProject);
            prefs.edit().putString(KEY_CURRENT_PROJECT, projectJson).apply();
        }
    }
    
    /**
     * 从SharedPreferences加载当前项目
     */
    private void loadCurrentProject() {
        String projectJson = prefs.getString(KEY_CURRENT_PROJECT, null);
        if (projectJson != null) {
            try {
                currentProject = gson.fromJson(projectJson, PptProject.class);
                Timber.tag(TAG).d( "加载当前项目: " + (currentProject != null ? currentProject.getId() : "null"));
            } catch (Exception e) {
                Timber.tag(TAG).e( "加载项目数据失败", e);
            }
        }
    }
    
    /**
     * 保存导航栈
     */
    private void saveNavigationStack() {
        String stackJson = gson.toJson(navigationStack);
        prefs.edit().putString(KEY_NAVIGATION_STACK, stackJson).apply();
    }
    
    /**
     * 加载导航栈
     */
    private void loadNavigationStack() {
        String stackJson = prefs.getString(KEY_NAVIGATION_STACK, null);
        if (stackJson != null) {
            try {
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> loadedStack = gson.fromJson(stackJson, listType);
                if (loadedStack != null) {
                    navigationStack.clear();
                    navigationStack.addAll(loadedStack);
                }
            } catch (Exception e) {
                Timber.tag(TAG).e( "加载导航栈失败", e);
            }
        }
    }
    
    /**
     * 保存项目历史记录
     */
    public void saveToHistory(PptProject project) {
        if (project == null) return;
        
        try {
            String historyJson = prefs.getString(KEY_PROJECT_HISTORY, "[]");
            Type listType = new TypeToken<List<PptProject>>(){}.getType();
            List<PptProject> history = gson.fromJson(historyJson, listType);
            
            if (history == null) {
                history = new ArrayList<>();
            }
            
            // 移除已存在的相同项目
            history.removeIf(p -> p.getId().equals(project.getId()));
            
            // 添加到历史记录开头
            history.add(0, project);
            
            // 保持历史记录数量限制
            if (history.size() > 20) {
                history = history.subList(0, 20);
            }
            
            String updatedHistoryJson = gson.toJson(history);
            prefs.edit().putString(KEY_PROJECT_HISTORY, updatedHistoryJson).apply();
            
            Timber.tag(TAG).d( "保存项目到历史记录: " + project.getId());
        } catch (Exception e) {
            Timber.tag(TAG).e( "保存历史记录失败", e);
        }
    }
    
    /**
     * 获取项目历史记录
     */
    public List<PptProject> getProjectHistory() {
        try {
            String historyJson = prefs.getString(KEY_PROJECT_HISTORY, "[]");
            Type listType = new TypeToken<List<PptProject>>(){}.getType();
            List<PptProject> history = gson.fromJson(historyJson, listType);
            return history != null ? history : new ArrayList<>();
        } catch (Exception e) {
            Timber.tag(TAG).e( "获取历史记录失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 清理过期数据
     */
    public void cleanup() {
        // 清理会话数据
        clearSessionData();
        
        // 清理过期的历史记录（超过30天）
        try {
            List<PptProject> history = getProjectHistory();
            long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
            
            history.removeIf(project -> {
                Date createdAt = project.getCreatedAt();
                return createdAt != null && createdAt.getTime() < thirtyDaysAgo;
            });
            
            String updatedHistoryJson = gson.toJson(history);
            prefs.edit().putString(KEY_PROJECT_HISTORY, updatedHistoryJson).apply();
            
            Timber.tag(TAG).d( "清理过期历史记录完成");
        } catch (Exception e) {
            Timber.tag(TAG).e( "清理历史记录失败", e);
        }
    }
    
    /**
     * 重置状态管理器
     */
    public void reset() {
        currentProject = null;
        clearSessionData();
        clearNavigationStack();
        prefs.edit().clear().apply();
        Timber.tag(TAG).d( "重置状态管理器");
    }
}