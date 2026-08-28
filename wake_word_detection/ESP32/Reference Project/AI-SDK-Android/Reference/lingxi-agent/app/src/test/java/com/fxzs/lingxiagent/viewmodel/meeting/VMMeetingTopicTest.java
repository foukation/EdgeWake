package com.fxzs.lingxiagent.viewmodel.meeting;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class VMMeetingTopicTest {
    
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    
    @Mock
    private Application mockApplication;
    
    private VMMeetingTopic viewModel;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new VMMeetingTopic(mockApplication);
    }
    
    @Test
    public void testInitialState() {
        // 测试初始状态
        assertNotNull(viewModel.getTopicsContent());
        assertEquals("", viewModel.getTopicsContent().get());
        assertFalse(viewModel.getLoading().getValue());
    }
    
    @Test
    public void testGenerateTopicsWithValidContent() {
        // 测试使用有效内容生成话题
        String transcription = "今天我们讨论了项目进展，包括开发计划和测试策略。会议中决定了下一阶段的工作重点。";
        
        viewModel.generateTopics(transcription);
        
        // 验证话题内容已生成
        String topicsContent = viewModel.getTopicsContent().get();
        assertNotNull(topicsContent);
        assertTrue(topicsContent.contains("🏷️ 会议话题"));
        assertTrue(topicsContent.contains("📌 检测到的关键内容"));
        
        // 验证成功状态
        assertEquals("话题提取完成", viewModel.getSuccess().getValue());
    }
    
    @Test
    public void testGenerateTopicsWithEmptyContent() {
        // 测试使用空内容生成话题
        viewModel.generateTopics("");
        
        // 验证错误状态
        assertEquals("转写内容为空，无法提取话题", viewModel.getError().getValue());
    }
    
    @Test
    public void testGenerateTopicsWithNullContent() {
        // 测试使用null内容生成话题
        viewModel.generateTopics(null);
        
        // 验证错误状态
        assertEquals("转写内容为空，无法提取话题", viewModel.getError().getValue());
    }
    
    @Test
    public void testWordCountInTopics() {
        // 测试话题中的词汇统计
        String transcription = "这是 一个 测试 内容 包含 五个 词汇";
        
        viewModel.generateTopics(transcription);
        
        String topicsContent = viewModel.getTopicsContent().get();
        assertTrue(topicsContent.contains("包含 7 个词汇")); // "这是 一个 测试 内容 包含 五个 词汇" = 7个词
    }
}