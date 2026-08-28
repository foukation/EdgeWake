package com.fxzs.lingxiagent.util;

import android.app.Activity;
import android.os.Bundle;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.fxzs.lingxiagent.model.ppt.dto.PptProject;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * PPT生命周期管理器
 * 负责管理Activity生命周期中的状态保存和恢复
 */
public class PptLifecycleManager implements LifecycleObserver {
    
    private static final String TAG = "PptLifecycleManager";
    
    // 状态保存键
    private static final String KEY_CURRENT_INPUT = "current_input";
    private static final String KEY_EDITING_STATE = "editing_state";
    private static final String KEY_SCROLL_POSITION = "scroll_position";
    private static final String KEY_SELECTED_TEMPLATE = "selected_template";
    private static final String KEY_CURRENT_SLIDE_INDEX = "current_slide_index";
    private static final String KEY_ZOOM_LEVEL = "zoom_level";
    private static final String KEY_UI_STATE = "ui_state";
    
    private final Activity activity;
    private final PptStateManager stateManager;
    private final Map<String, Object> activityState = new HashMap<>();
    
    public PptLifecycleManager(Activity activity) {
        this.activity = activity;
        this.stateManager = PptStateManager.getInstance(activity);
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        Timber.tag(TAG).d( "Activity onCreate: " + activity.getClass().getSimpleName());
        
        // 从Intent恢复状态
        if (activity.getIntent() != null && activity.getIntent().getExtras() != null) {
            stateManager.restoreFromBundle(activity.getIntent().getExtras());
        }
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        Timber.tag(TAG).d( "Activity onStart: " + activity.getClass().getSimpleName());
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        Timber.tag(TAG).d( "Activity onResume: " + activity.getClass().getSimpleName());
        
        // 恢复Activity特定状态
        restoreActivitySpecificState();
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        Timber.tag(TAG).d( "Activity onPause: " + activity.getClass().getSimpleName());
        
        // 保存Activity特定状态
        saveActivitySpecificState();
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        Timber.tag(TAG).d( "Activity onStop: " + activity.getClass().getSimpleName());
        
        // 保存当前项目到历史记录
        PptProject currentProject = stateManager.getCurrentProject();
        if (currentProject != null) {
            stateManager.saveToHistory(currentProject);
        }
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        Timber.tag(TAG).d( "Activity onDestroy: " + activity.getClass().getSimpleName());
        
        // 清理资源
        activityState.clear();
    }
    
    /**
     * 保存Activity实例状态
     */
    public void saveInstanceState(Bundle outState) {
        if (outState == null) return;
        
        // 保存通用状态
        PptProject currentProject = stateManager.getCurrentProject();
        if (currentProject != null) {
            outState.putString(PptStateManager.EXTRA_PROJECT_DATA, 
                new com.google.gson.Gson().toJson(currentProject));
        }
        
        // 保存Activity特定状态
        for (Map.Entry<String, Object> entry : activityState.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                outState.putString(key, (String) value);
            } else if (value instanceof Integer) {
                outState.putInt(key, (Integer) value);
            } else if (value instanceof Float) {
                outState.putFloat(key, (Float) value);
            } else if (value instanceof Boolean) {
                outState.putBoolean(key, (Boolean) value);
            }
        }
        
        Timber.tag(TAG).d( "保存实例状态: " + activity.getClass().getSimpleName());
    }
    
    /**
     * 恢复Activity实例状态
     */
    public void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        
        // 恢复项目状态
        stateManager.restoreFromBundle(savedInstanceState);
        
        // 恢复Activity特定状态
        for (String key : savedInstanceState.keySet()) {
            Object value = savedInstanceState.get(key);
            if (value != null) {
                activityState.put(key, value);
            }
        }
        
        Timber.tag(TAG).d( "恢复实例状态: " + activity.getClass().getSimpleName());
    }
    
    /**
     * 保存当前输入内容
     */
    public void saveCurrentInput(String input) {
        activityState.put(KEY_CURRENT_INPUT, input);
        stateManager.saveSessionData(KEY_CURRENT_INPUT, input);
        Timber.tag(TAG).d( "保存当前输入: " + (input != null ? input.substring(0, Math.min(20, input.length())) : "null"));
    }
    
    /**
     * 获取保存的输入内容
     */
    public String getSavedInput() {
        String input = (String) activityState.get(KEY_CURRENT_INPUT);
        if (input == null) {
            input = stateManager.getSessionData(KEY_CURRENT_INPUT, String.class);
        }
        return input;
    }
    
    /**
     * 保存编辑状态
     */
    public void saveEditingState(boolean isEditing, int editingPosition) {
        Map<String, Object> editingState = new HashMap<>();
        editingState.put("isEditing", isEditing);
        editingState.put("position", editingPosition);
        
        activityState.put(KEY_EDITING_STATE, editingState);
        stateManager.saveSessionData(KEY_EDITING_STATE, editingState);
        
        Timber.tag(TAG).d( "保存编辑状态: isEditing=" + isEditing + ", position=" + editingPosition);
    }
    
    /**
     * 获取编辑状态
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getEditingState() {
        Map<String, Object> state = (Map<String, Object>) activityState.get(KEY_EDITING_STATE);
        if (state == null) {
            state = stateManager.getSessionData(KEY_EDITING_STATE, Map.class);
        }
        return state != null ? state : new HashMap<>();
    }
    
    /**
     * 保存滚动位置
     */
    public void saveScrollPosition(int scrollY) {
        activityState.put(KEY_SCROLL_POSITION, scrollY);
        Timber.tag(TAG).d( "保存滚动位置: " + scrollY);
    }
    
    /**
     * 获取滚动位置
     */
    public int getScrollPosition() {
        Integer position = (Integer) activityState.get(KEY_SCROLL_POSITION);
        return position != null ? position : 0;
    }
    
    /**
     * 保存选中的模板
     */
    public void saveSelectedTemplate(String templateId) {
        activityState.put(KEY_SELECTED_TEMPLATE, templateId);
        stateManager.saveSessionData(KEY_SELECTED_TEMPLATE, templateId);
        Timber.tag(TAG).d( "保存选中模板: " + templateId);
    }
    
    /**
     * 获取选中的模板
     */
    public String getSelectedTemplate() {
        String templateId = (String) activityState.get(KEY_SELECTED_TEMPLATE);
        if (templateId == null) {
            templateId = stateManager.getSessionData(KEY_SELECTED_TEMPLATE, String.class);
        }
        return templateId;
    }
    
    /**
     * 保存当前幻灯片索引
     */
    public void saveCurrentSlideIndex(int index) {
        activityState.put(KEY_CURRENT_SLIDE_INDEX, index);
        Timber.tag(TAG).d( "保存当前幻灯片索引: " + index);
    }
    
    /**
     * 获取当前幻灯片索引
     */
    public int getCurrentSlideIndex() {
        Integer index = (Integer) activityState.get(KEY_CURRENT_SLIDE_INDEX);
        return index != null ? index : 0;
    }
    
    /**
     * 保存缩放级别
     */
    public void saveZoomLevel(float zoomLevel) {
        activityState.put(KEY_ZOOM_LEVEL, zoomLevel);
        Timber.tag(TAG).d( "保存缩放级别: " + zoomLevel);
    }
    
    /**
     * 获取缩放级别
     */
    public float getZoomLevel() {
        Float zoomLevel = (Float) activityState.get(KEY_ZOOM_LEVEL);
        return zoomLevel != null ? zoomLevel : 1.0f;
    }
    
    /**
     * 保存UI状态
     */
    public void saveUIState(String key, Object value) {
        @SuppressWarnings("unchecked")
        Map<String, Object> uiState = (Map<String, Object>) activityState.get(KEY_UI_STATE);
        if (uiState == null) {
            uiState = new HashMap<>();
            activityState.put(KEY_UI_STATE, uiState);
        }
        uiState.put(key, value);
        Timber.tag(TAG).d( "保存UI状态: " + key + " = " + value);
    }
    
    /**
     * 获取UI状态
     */
    @SuppressWarnings("unchecked")
    public <T> T getUIState(String key, Class<T> type) {
        Map<String, Object> uiState = (Map<String, Object>) activityState.get(KEY_UI_STATE);
        if (uiState != null) {
            Object value = uiState.get(key);
            if (value != null && type.isInstance(value)) {
                return type.cast(value);
            }
        }
        return null;
    }
    
    /**
     * 保存Activity特定状态
     */
    private void saveActivitySpecificState() {
        String activityName = activity.getClass().getSimpleName();
        
        // 根据Activity类型保存特定状态
        switch (activityName) {
            case "PptTopicInputActivity":
                saveTopicInputState();
                break;
            case "PptOutlineEditActivity":
                saveOutlineEditState();
                break;
            case "PptTemplateSelectionActivity":
                saveTemplateSelectionState();
                break;
            case "PptPreviewActivity":
                savePreviewState();
                break;
        }
    }
    
    /**
     * 恢复Activity特定状态
     */
    private void restoreActivitySpecificState() {
        String activityName = activity.getClass().getSimpleName();
        
        // 根据Activity类型恢复特定状态
        switch (activityName) {
            case "PptTopicInputActivity":
                restoreTopicInputState();
                break;
            case "PptOutlineEditActivity":
                restoreOutlineEditState();
                break;
            case "PptTemplateSelectionActivity":
                restoreTemplateSelectionState();
                break;
            case "PptPreviewActivity":
                restorePreviewState();
                break;
        }
    }
    
    private void saveTopicInputState() {
        // 主题输入页面特定状态保存逻辑
        Timber.tag(TAG).d( "保存主题输入页面状态");
    }
    
    private void restoreTopicInputState() {
        // 主题输入页面特定状态恢复逻辑
        Timber.tag(TAG).d( "恢复主题输入页面状态");
    }
    
    private void saveOutlineEditState() {
        // 大纲编辑页面特定状态保存逻辑
        Timber.tag(TAG).d( "保存大纲编辑页面状态");
    }
    
    private void restoreOutlineEditState() {
        // 大纲编辑页面特定状态恢复逻辑
        Timber.tag(TAG).d( "恢复大纲编辑页面状态");
    }
    
    private void saveTemplateSelectionState() {
        // 模板选择页面特定状态保存逻辑
        Timber.tag(TAG).d( "保存模板选择页面状态");
    }
    
    private void restoreTemplateSelectionState() {
        // 模板选择页面特定状态恢复逻辑
        Timber.tag(TAG).d( "恢复模板选择页面状态");
    }
    
    private void savePreviewState() {
        // 预览页面特定状态保存逻辑
        Timber.tag(TAG).d( "保存预览页面状态");
    }
    
    private void restorePreviewState() {
        // 预览页面特定状态恢复逻辑
        Timber.tag(TAG).d( "恢复预览页面状态");
    }
}