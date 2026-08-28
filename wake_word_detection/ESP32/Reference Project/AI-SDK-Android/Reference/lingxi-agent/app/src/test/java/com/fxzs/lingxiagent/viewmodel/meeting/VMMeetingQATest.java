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
public class VMMeetingQATest {
    
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    
    @Mock
    private Application mockApplication;
    
    private VMMeetingQA viewModel;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new VMMeetingQA(mockApplication);
    }
    
    @Test
    public void testInitialState() {
        // 测试初始状态
        assertNotNull(viewModel.getQuestionInput());
        assertNotNull(viewModel.getQaContent());
        assertNotNull(viewModel.getMeetingContent());
        
        assertEquals("", viewModel.getQuestionInput().get());
        assertEquals("", viewModel.getQaContent().get());
        assertEquals("", viewModel.getMeetingContent().get());
        assertFalse(viewModel.getLoading().getValue());
    }
    
    @Test
    public void testAskQuestionWithValidInput() {
        // 测试有效输入的问答
        String question = "会议的主要内容是什么？";
        String meetingTranscription = "今天我们讨论了项目进展和下一步计划";
        
        viewModel.askQuestion(question, meetingTranscription);
        
        // 验证问答内容已更新
        String qaContent = viewModel.getQaContent().get();
        assertNotNull(qaContent);
        assertTrue(qaContent.contains("🙋 问题: " + question));
        assertTrue(qaContent.contains("🤖 回答:"));
        
        // 验证成功状态
        assertEquals("问答完成", viewModel.getSuccess().getValue());
    }
    
    @Test
    public void testAskQuestionWithEmptyQuestion() {
        // 测试空问题
        String meetingTranscription = "会议内容";
        
        viewModel.askQuestion("", meetingTranscription);
        
        // 验证错误状态
        assertEquals("问题或会议内容为空", viewModel.getError().getValue());
    }
    
    @Test
    public void testAskQuestionWithEmptyMeeting() {
        // 测试空会议内容
        String question = "测试问题";
        
        viewModel.askQuestion(question, "");
        
        // 验证错误状态
        assertEquals("问题或会议内容为空", viewModel.getError().getValue());
    }
    
    @Test
    public void testMultipleQuestions() {
        // 测试多个问题的对话
        String meetingTranscription = "会议讨论了项目进展";
        
        // 第一个问题
        viewModel.askQuestion("第一个问题", meetingTranscription);
        String firstQA = viewModel.getQaContent().get();
        assertTrue(firstQA.contains("第一个问题"));
        
        // 第二个问题
        viewModel.askQuestion("第二个问题", meetingTranscription);
        String secondQA = viewModel.getQaContent().get();
        assertTrue(secondQA.contains("第一个问题"));
        assertTrue(secondQA.contains("第二个问题"));
    }
    
    @Test
    public void testClearQAContent() {
        // 测试清空问答内容
        viewModel.getQuestionInput().set("测试问题");
        viewModel.getQaContent().set("测试内容");
        
        viewModel.clearQAContent();
        
        assertEquals("", viewModel.getQuestionInput().get());
        assertEquals("", viewModel.getQaContent().get());
    }
    
    @Test
    public void testKeywordBasedAnswers() {
        // 测试基于关键词的回答
        String meetingTranscription = "会议内容";
        
        // 测试"主要"关键词
        viewModel.askQuestion("主要讨论了什么？", meetingTranscription);
        String qaContent = viewModel.getQaContent().get();
        assertTrue(qaContent.contains("主要讨论了以下几个重点"));
        
        // 清空并测试"谁"关键词
        viewModel.clearQAContent();
        viewModel.askQuestion("谁负责这个项目？", meetingTranscription);
        qaContent = viewModel.getQaContent().get();
        assertTrue(qaContent.contains("相关负责人员的分工安排"));
    }
}