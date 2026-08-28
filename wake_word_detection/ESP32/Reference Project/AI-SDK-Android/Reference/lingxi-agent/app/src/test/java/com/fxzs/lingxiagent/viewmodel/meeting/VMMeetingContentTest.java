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
public class VMMeetingContentTest {
    
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    
    @Mock
    private Application mockApplication;
    
    private VMMeetingContent viewModel;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new VMMeetingContent(mockApplication);
    }
    
    @Test
    public void testInitialState() {
        // 测试初始状态
        assertNotNull(viewModel.getTranscriptionContent());
        assertEquals("", viewModel.getTranscriptionContent().get());
        assertFalse(viewModel.getLoading().getValue());
    }
    
    @Test
    public void testSetTranscriptionContent() {
        // 测试设置转写内容
        String testContent = "这是一段测试的会议转写内容";
        viewModel.getTranscriptionContent().set(testContent);
        
        assertEquals(testContent, viewModel.getTranscriptionContent().get());
    }
    
    @Test
    public void testRefreshContent() {
        // 测试刷新内容
        viewModel.refreshContent();
        
        // 验证成功状态
        assertEquals("内容刷新完成", viewModel.getSuccess().getValue());
    }
    
    @Test
    public void testTranscriptionContentObservable() {
        // 测试ObservableField功能
        String[] observedValues = new String[1];
        viewModel.getTranscriptionContent().observeForever(value -> {
            observedValues[0] = value;
        });
        
        String testContent = "观察者模式测试内容";
        viewModel.getTranscriptionContent().set(testContent);
        
        assertEquals(testContent, observedValues[0]);
    }
    
    @Test
    public void testFormatMeetingContent_SingleSpeaker() {
        // 测试单个发言人的格式转换
        String rawContent = "[0:0.930,0:1.780,0]  你好啊。\n";
        String expected = "- **发言人1 00:00** \n你好啊。";
        
        String formatted = viewModel.formatMeetingContent(rawContent);
        assertEquals(expected, formatted);
    }
    
    @Test
    public void testFormatMeetingContent_MultipleSpeakers() {
        // 测试多个发言人的格式转换
        String rawContent = "[0:0.930,0:1.780,0]  你好啊。\n" +
                          "[1:2.100,1:5.500,0]  很高兴见到你。\n" +
                          "[0:6.000,0:8.000,0]  我也是。\n";
        
        String expected = "- **发言人1 00:00** \n你好啊。\n\n" +
                         "- **发言人2 00:02** \n很高兴见到你。\n\n" +
                         "- **发言人1 00:06** \n我也是。";
        
        String formatted = viewModel.formatMeetingContent(rawContent);
        assertEquals(expected, formatted);
    }
    
    @Test
    public void testFormatMeetingContent_TimeConversion() {
        // 测试时间格式转换（秒转分钟:秒）
        String rawContent = "[0:65.500,0:67.000,0]  一分钟后的发言。\n" +
                          "[1:125.300,1:127.800,0]  两分钟后的发言。\n";
        
        String expected = "- **发言人1 01:05** \n一分钟后的发言。\n\n" +
                         "- **发言人2 02:05** \n两分钟后的发言。";
        
        String formatted = viewModel.formatMeetingContent(rawContent);
        assertEquals(expected, formatted);
    }
    
    @Test
    public void testFormatMeetingContent_EmptyContent() {
        // 测试空内容
        assertEquals("", viewModel.formatMeetingContent(""));
        assertEquals("", viewModel.formatMeetingContent(null));
    }
    
    @Test
    public void testFormatMeetingContent_InvalidFormat() {
        // 测试无效格式的处理
        String rawContent = "这是一行普通文本\n" +
                          "[0:0.930,0:1.780,0]  正常格式。\n" +
                          "又是一行普通文本\n";
        
        String formatted = viewModel.formatMeetingContent(rawContent);
        assertTrue(formatted.contains("这是一行普通文本"));
        assertTrue(formatted.contains("- **发言人1 00:00** \n正常格式。"));
        assertTrue(formatted.contains("又是一行普通文本"));
    }
    
    @Test
    public void testSetRawTranscriptionContent() {
        // 测试设置原始内容并自动格式化
        String rawContent = "[0:0.930,0:1.780,0]  你好啊。\n";
        viewModel.setRawTranscriptionContent(rawContent);
        
        // 默认显示优化版（格式化后的内容）
        String displayedContent = viewModel.getTranscriptionContent().get();
        assertTrue(displayedContent.contains("- **发言人1 00:00**"));
          }
}