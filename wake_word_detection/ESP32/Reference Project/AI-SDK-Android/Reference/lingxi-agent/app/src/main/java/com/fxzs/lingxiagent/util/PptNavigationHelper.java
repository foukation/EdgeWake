package com.fxzs.lingxiagent.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.fxzs.lingxiagent.model.ppt.dto.PptProject;
import com.fxzs.lingxiagent.view.ppt.PptOutlineEditActivity;
import com.fxzs.lingxiagent.view.ppt.PptPreviewActivity;
import com.fxzs.lingxiagent.view.ppt.PptTopicInputActivity;

import timber.log.Timber;

/**
 * PPT导航辅助类
 * 负责Activity间的导航和数据传递
 */
public class PptNavigationHelper {
    
    private static final String TAG = "PptNavigationHelper";
    
    /**
     * 导航到主题输入页面
     */
    public static void navigateToTopicInput(Context context) {
        Intent intent = new Intent(context, PptTopicInputActivity.class);
        
        // 清除之前的状态
        PptStateManager stateManager = PptStateManager.getInstance(context);
        stateManager.clearSessionData();
        
        Bundle bundle = stateManager.createNavigationBundle(
            PptStateManager.SOURCE_TOPIC_INPUT, 
            PptStateManager.SOURCE_TOPIC_INPUT
        );
        intent.putExtras(bundle);
        
        context.startActivity(intent);
        Timber.tag(TAG).d( "导航到主题输入页面");
    }
    
    /**
     * 从主题输入导航到大纲编辑页面
     */
    public static void navigateToOutlineEdit(Context context, String topic) {
        PptStateManager stateManager = PptStateManager.getInstance(context);
        
        // 创建新项目或更新现有项目
        PptProject project = stateManager.getCurrentProject();
        if (project == null) {
            project = stateManager.createNewProject(topic);
        } else {
            project.setTopic(topic);
            stateManager.setCurrentProject(project);
        }

        // 设置项目为大纲生成状态
        project.setStatus(com.fxzs.lingxiagent.model.ppt.dto.PptStatus.OUTLINE_GENERATING);
        stateManager.setCurrentProject(project);
        
        Intent intent = new Intent(context, PptOutlineEditActivity.class);
        Bundle bundle = stateManager.createNavigationBundle(
            PptStateManager.SOURCE_TOPIC_INPUT,
            PptStateManager.SOURCE_OUTLINE_EDIT
        );
        
        // 添加特定参数
        bundle.putString(PptStateManager.EXTRA_TOPIC, topic);
        bundle.putBoolean(PptStateManager.EXTRA_IS_GENERATING, true);
        
        intent.putExtras(bundle);
        context.startActivity(intent);
        
        // 如果是Activity，关闭当前页面
//        if (context instanceof Activity) {
//            ((Activity) context).finish();
//        }
        
        Timber.tag(TAG).d( "导航到大纲编辑页面，主题: " + topic);
    }
    
    /**
     * 从大纲编辑导航到模板选择页面
     */
    public static void navigateToTemplateSelection(Context context) {
        PptStateManager stateManager = PptStateManager.getInstance(context);
        PptProject project = stateManager.getCurrentProject();
        
        if (project == null) {
            Timber.tag(TAG).e( "无法导航到模板选择页面：当前项目为空");
            return;
        }
        
        // 更新项目状态
        stateManager.updateProjectStatus(com.fxzs.lingxiagent.model.ppt.dto.PptStatus.OUTLINE_READY);
        
        Intent intent = new Intent(context, com.fxzs.lingxiagent.view.ppt.PptTemplateSelectionActivity.class);
        Bundle bundle = stateManager.createNavigationBundle(
            PptStateManager.SOURCE_OUTLINE_EDIT,
            PptStateManager.SOURCE_TEMPLATE_SELECTION
        );
        
        intent.putExtras(bundle);
        context.startActivity(intent);
        
        Timber.tag(TAG).d( "导航到模板选择页面，项目ID: " + project.getId());
    }
    
    /**
     * 从模板选择导航到预览页面
     */
    public static void navigateToPreview(Context context, String templateId) {
        PptStateManager stateManager = PptStateManager.getInstance(context);
        PptProject project = stateManager.getCurrentProject();
        
        if (project == null) {
            Timber.tag(TAG).e( "无法导航到预览页面：当前项目为空");
            return;
        }
        
        // 更新项目模板和状态
        stateManager.updateProjectTemplate(templateId);
        stateManager.updateProjectStatus(com.fxzs.lingxiagent.model.ppt.dto.PptStatus.PPT_GENERATING);
        
        Intent intent = new Intent(context, PptPreviewActivity.class);
        Bundle bundle = stateManager.createNavigationBundle(
            PptStateManager.SOURCE_TEMPLATE_SELECTION,
            PptStateManager.SOURCE_PREVIEW
        );
        
        // 添加模板ID
        bundle.putString(PptStateManager.EXTRA_TEMPLATE_ID, templateId);
        
        intent.putExtras(bundle);
        context.startActivity(intent);
        
        Timber.tag(TAG).d( "导航到预览页面，模板ID: " + templateId);
    }
    
    /**
     * 导航到指定幻灯片的预览页面
     */
    public static void navigateToSlidePreview(Context context, int slideIndex) {
        PptStateManager stateManager = PptStateManager.getInstance(context);
        PptProject project = stateManager.getCurrentProject();
        
        if (project == null) {
            Timber.tag(TAG).e( "无法导航到幻灯片预览：当前项目为空");
            return;
        }
        
        Intent intent = new Intent(context, PptPreviewActivity.class);
        Bundle bundle = stateManager.createNavigationBundle(
            PptStateManager.SOURCE_PREVIEW,
            PptStateManager.SOURCE_PREVIEW
        );
        
        // 添加幻灯片索引
        bundle.putInt(PptStateManager.EXTRA_CURRENT_SLIDE, slideIndex);
        
        intent.putExtras(bundle);
        context.startActivity(intent);
        
        Timber.tag(TAG).d( "导航到幻灯片预览，索引: " + slideIndex);
    }
    
    /**
     * 返回到上一个页面
     */
    public static boolean navigateBack(Context context) {
        PptStateManager stateManager = PptStateManager.getInstance(context);
        String previousPage = stateManager.getPreviousPage();
        
        if (previousPage == null) {
            Timber.tag(TAG).d( "没有上一个页面，使用默认返回");
            return false;
        }
        
        Intent intent = null;
        switch (previousPage) {
            case PptStateManager.SOURCE_TOPIC_INPUT:
                intent = new Intent(context, PptTopicInputActivity.class);
                break;
            case PptStateManager.SOURCE_OUTLINE_EDIT:
                intent = new Intent(context, PptOutlineEditActivity.class);
                break;
            case PptStateManager.SOURCE_TEMPLATE_SELECTION:
                intent = new Intent(context, com.fxzs.lingxiagent.view.ppt.PptTemplateSelectionActivity.class);
                break;
            case PptStateManager.SOURCE_PREVIEW:
                intent = new Intent(context, PptPreviewActivity.class);
                break;
        }
        
        if (intent != null) {
            Bundle bundle = stateManager.createNavigationBundle(
                "back_navigation",
                previousPage
            );
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
            
            Timber.tag(TAG).d( "返回到上一个页面: " + previousPage);
            return true;
        }
        
        return false;
    }
    
    /**
     * 重新开始PPT创建流程
     */
    public static void restartPptCreation(Context context) {
        PptStateManager stateManager = PptStateManager.getInstance(context);
        
        // 保存当前项目到历史记录
        PptProject currentProject = stateManager.getCurrentProject();
        if (currentProject != null) {
            stateManager.saveToHistory(currentProject);
        }
        
        // 重置状态
        stateManager.reset();
        
        // 导航到主题输入页面
        navigateToTopicInput(context);
        
        Timber.tag(TAG).d( "重新开始PPT创建流程");
    }
    
    /**
     * 从历史记录恢复项目
     */
    public static void restoreFromHistory(Context context, PptProject project) {
        PptStateManager stateManager = PptStateManager.getInstance(context);
        stateManager.setCurrentProject(project);
        
        // 根据项目状态导航到相应页面
        Intent intent = null;
        switch (project.getStatus()) {
            case OUTLINE_GENERATING:
            case OUTLINE_READY:
                intent = new Intent(context, PptOutlineEditActivity.class);
                break;
            case PPT_GENERATING:
            case PPT_READY:
                intent = new Intent(context, PptPreviewActivity.class);
                break;
            case FAILED:
                intent = new Intent(context, PptTopicInputActivity.class);
                break;
        }
        
        if (intent != null) {
            Bundle bundle = stateManager.createNavigationBundle(
                "history_restore",
                getSourceFromStatus(project.getStatus())
            );
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            
            Timber.tag(TAG).d( "从历史记录恢复项目: " + project.getId());
        }
    }
    
    /**
     * 根据项目状态获取对应的源标识
     */
    private static String getSourceFromStatus(com.fxzs.lingxiagent.model.ppt.dto.PptStatus status) {
        switch (status) {
            case OUTLINE_GENERATING:
            case OUTLINE_READY:
                return PptStateManager.SOURCE_OUTLINE_EDIT;
            case PPT_GENERATING:
            case PPT_READY:
                return PptStateManager.SOURCE_PREVIEW;
            case FAILED:
            default:
                return PptStateManager.SOURCE_TOPIC_INPUT;
        }
    }
    
    /**
     * 处理Activity的onNewIntent
     */
    public static void handleNewIntent(Activity activity, Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            PptStateManager stateManager = PptStateManager.getInstance(activity);
            stateManager.restoreFromBundle(intent.getExtras());
            
            Timber.tag(TAG).d( "处理新Intent，恢复状态数据");
        }
    }
    
    /**
     * 保存Activity状态
     */
    public static void saveActivityState(Activity activity, Bundle outState) {
        PptStateManager stateManager = PptStateManager.getInstance(activity);
        PptProject currentProject = stateManager.getCurrentProject();
        
        if (currentProject != null) {
            outState.putString(PptStateManager.EXTRA_PROJECT_DATA, 
                new com.google.gson.Gson().toJson(currentProject));
        }
        
        Timber.tag(TAG).d( "保存Activity状态");
    }
    
    /**
     * 恢复Activity状态
     */
    public static void restoreActivityState(Activity activity, Bundle savedInstanceState) {
//        if (savedInstanceState != null) {
//            PptStateManager stateManager = PptStateManager.getInstance(activity);
//            stateManager.restoreFromBundle(savedInstanceState);
//
//            Timber.tag(TAG).d( "恢复Activity状态");
//        }
    }
}