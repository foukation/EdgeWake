package com.fxzs.lingxiagent.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.fxzs.lingxiagent.model.ppt.dto.PptProject;
import com.fxzs.lingxiagent.model.ppt.dto.PptStatus;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * PPT状态管理器测试
 */
@RunWith(RobolectricTestRunner.class)
public class PptStateManagerTest {

    private PptStateManager stateManager;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        stateManager = PptStateManager.getInstance(context);
    }

    @Test
    public void testCreateNewProject() {
        String topic = "测试主题";
        PptProject project = stateManager.createNewProject(topic);
        
        assertNotNull("项目不应为空", project);
        assertEquals("主题应匹配", topic, project.getTopic());
        assertEquals("标题应匹配", topic, project.getTitle());
        assertEquals("状态应为大纲生成中", PptStatus.OUTLINE_GENERATING, project.getStatus());
        assertNotNull("项目ID不应为空", project.getId());
    }

    @Test
    public void testUpdateProjectStatus() {
        String topic = "测试主题";
        PptProject project = stateManager.createNewProject(topic);
        
        stateManager.updateProjectStatus(PptStatus.OUTLINE_READY);
        
        assertEquals("状态应已更新", PptStatus.OUTLINE_READY, project.getStatus());
    }

    @Test
    public void testCreateNavigationBundle() {
        String topic = "测试主题";
        PptProject project = stateManager.createNewProject(topic);
        
        Bundle bundle = stateManager.createNavigationBundle(
            PptStateManager.SOURCE_TOPIC_INPUT,
            PptStateManager.SOURCE_OUTLINE_EDIT
        );
        
        assertNotNull("Bundle不应为空", bundle);
        assertEquals("PPT ID应匹配", project.getId(), bundle.getString(PptStateManager.EXTRA_PPT_ID));
        assertEquals("主题应匹配", topic, bundle.getString(PptStateManager.EXTRA_TOPIC));
        assertEquals("导航源应匹配", PptStateManager.SOURCE_TOPIC_INPUT, 
            bundle.getString(PptStateManager.EXTRA_NAVIGATION_SOURCE));
    }

    @Test
    public void testRestoreFromBundle() {
        String topic = "测试主题";
        PptProject originalProject = stateManager.createNewProject(topic);
        
        Bundle bundle = stateManager.createNavigationBundle(
            PptStateManager.SOURCE_TOPIC_INPUT,
            PptStateManager.SOURCE_OUTLINE_EDIT
        );
        
        // 重置状态管理器
        stateManager.reset();
        assertNull("当前项目应为空", stateManager.getCurrentProject());
        
        // 从Bundle恢复
        stateManager.restoreFromBundle(bundle);
        
        PptProject restoredProject = stateManager.getCurrentProject();
        assertNotNull("恢复的项目不应为空", restoredProject);
        assertEquals("项目ID应匹配", originalProject.getId(), restoredProject.getId());
        assertEquals("主题应匹配", topic, restoredProject.getTopic());
    }

    @Test
    public void testSessionData() {
        String key = "test_key";
        String value = "test_value";
        
        stateManager.saveSessionData(key, value);
        String retrievedValue = stateManager.getSessionData(key, String.class);
        
        assertEquals("会话数据应匹配", value, retrievedValue);
        
        stateManager.clearSessionData();
        String clearedValue = stateManager.getSessionData(key, String.class);
        
        assertNull("清除后会话数据应为空", clearedValue);
    }

    @Test
    public void testProjectHistory() {
        String topic1 = "测试主题1";
        String topic2 = "测试主题2";
        
        PptProject project1 = stateManager.createNewProject(topic1);
        PptProject project2 = stateManager.createNewProject(topic2);
        
        stateManager.saveToHistory(project1);
        stateManager.saveToHistory(project2);
        
        var history = stateManager.getProjectHistory();
        
        assertNotNull("历史记录不应为空", history);
        assertTrue("历史记录应包含项目", history.size() >= 2);
        assertEquals("最新项目应在前面", project2.getId(), history.get(0).getId());
    }
}