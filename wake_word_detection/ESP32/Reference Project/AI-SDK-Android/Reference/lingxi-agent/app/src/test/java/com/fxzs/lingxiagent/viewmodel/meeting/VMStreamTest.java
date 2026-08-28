package com.fxzs.lingxiagent.viewmodel.meeting;

import android.app.Application;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VMStreamTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private Observer<String> mockStringObserver;

    @Mock
    private Observer<Boolean> mockBooleanObserver;

    @Mock
    private Observer<Integer> mockIntegerObserver;

    @Mock
    private Application mockApplication;

    private VMStream viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new VMStream(mockApplication);
    }

    @Test
    public void testInitialState() {
        // 验证初始状态
        assertEquals("", viewModel.getStreamContent().get());
        assertEquals(false, viewModel.getIsStreaming().get());
        assertEquals("实时数据流演示", viewModel.getStreamTitle().get());
        assertEquals(Integer.valueOf(0), viewModel.getStreamProgress().get());
        assertEquals("准备就绪", viewModel.getStreamStatus().get());
        
        // 验证按钮初始状态
        assertEquals(true, viewModel.getStartButtonEnabled().get());
        assertEquals(false, viewModel.getStopButtonEnabled().get());
        assertEquals(false, viewModel.getClearButtonEnabled().get());
    }

    @Test
    public void testStreamContentObservable() {
        // 观察流内容变化
        viewModel.getStreamContent().observeForever(mockStringObserver);
        
        // 验证初始值
        verify(mockStringObserver).onChanged("");
        
        // 模拟设置内容
        viewModel.getStreamContent().set("测试内容");
        verify(mockStringObserver).onChanged("测试内容");
        
        // 清理
        viewModel.getStreamContent().removeObserver(mockStringObserver);
    }

    @Test
    public void testStreamingStateObservable() {
        // 观察流状态变化
        viewModel.getIsStreaming().observeForever(mockBooleanObserver);
        
        // 验证初始值
        verify(mockBooleanObserver).onChanged(false);
        
        // 模拟设置流状态
        viewModel.getIsStreaming().set(true);
        verify(mockBooleanObserver).onChanged(true);
        
        // 清理
        viewModel.getIsStreaming().removeObserver(mockBooleanObserver);
    }

    @Test
    public void testProgressObservable() {
        // 观察进度变化
        viewModel.getStreamProgress().observeForever(mockIntegerObserver);
        
        // 验证初始值
        verify(mockIntegerObserver).onChanged(0);
        
        // 模拟设置进度
        viewModel.getStreamProgress().set(50);
        verify(mockIntegerObserver).onChanged(50);
        
        viewModel.getStreamProgress().set(100);
        verify(mockIntegerObserver).onChanged(100);
        
        // 清理
        viewModel.getStreamProgress().removeObserver(mockIntegerObserver);
    }

    @Test
    public void testButtonStateValidation() {
        // 验证按钮状态联动
        
        // 初始状态：开始按钮启用，停止和清除按钮禁用
        assertTrue(viewModel.getStartButtonEnabled().get());
        assertFalse(viewModel.getStopButtonEnabled().get());
        assertFalse(viewModel.getClearButtonEnabled().get());
        
        // 模拟开始流传输
        viewModel.getIsStreaming().set(true);
        
        // 流传输时：开始按钮禁用，停止按钮启用
        assertFalse(viewModel.getStartButtonEnabled().get());
        assertTrue(viewModel.getStopButtonEnabled().get());
        assertFalse(viewModel.getClearButtonEnabled().get());
        
        // 模拟添加内容并停止流传输
        viewModel.getStreamContent().set("一些内容");
        viewModel.getIsStreaming().set(false);
        
        // 停止流传输且有内容：开始按钮启用，停止按钮禁用，清除按钮启用
        assertTrue(viewModel.getStartButtonEnabled().get());
        assertFalse(viewModel.getStopButtonEnabled().get());
        assertTrue(viewModel.getClearButtonEnabled().get());
    }

    @Test
    public void testClearStream() {
        // 设置一些内容
        viewModel.getStreamContent().set("测试内容");
        viewModel.getStreamProgress().set(50);
        viewModel.getStreamStatus().set("测试状态");
        
        // 确保不在流传输状态
        viewModel.getIsStreaming().set(false);
        
        // 执行清除
        viewModel.clearStream();
        
        // 验证内容被清除
        assertEquals("", viewModel.getStreamContent().get());
        assertEquals(Integer.valueOf(0), viewModel.getStreamProgress().get());
        assertEquals("内容已清除", viewModel.getStreamStatus().get());
    }

    @Test
    public void testClearStreamWhileStreaming() {
        // 设置流传输状态
        viewModel.getIsStreaming().set(true);
        
        // 尝试清除（应该失败）
        viewModel.clearStream();
        
        // 验证错误状态被设置
        assertNotNull(viewModel.getError().getValue());
        assertEquals("无法在传输过程中清除内容", viewModel.getError().getValue());
    }

    @Test
    public void testStopStreamingWhenNotStreaming() {
        // 确保不在流传输状态
        viewModel.getIsStreaming().set(false);
        
        // 尝试停止流传输（应该不会有任何影响）
        viewModel.stopStreaming();
        
        // 验证状态没有改变
        assertFalse(viewModel.getIsStreaming().get());
    }

    @Test
    public void testStartStreamingMultipleTimes() {
        // 第一次启动
        viewModel.startStreaming();
        assertTrue(viewModel.getIsStreaming().get());
        
        // 第二次启动（应该不会有影响）
        viewModel.startStreaming();
        assertTrue(viewModel.getIsStreaming().get());
    }

    @Test
    public void testSimulateApiStreamWhenAlreadyStreaming() {
        // 设置流传输状态
        viewModel.getIsStreaming().set(true);
        
        // 尝试启动API流（应该失败）
        viewModel.simulateApiStream();
        
        // 验证错误状态被设置
        assertNotNull(viewModel.getError().getValue());
        assertEquals("当前正在进行流式传输", viewModel.getError().getValue());
    }

    @Test
    public void testObservableFieldValidation() {
        // 测试所有ObservableField的getter方法
        assertNotNull(viewModel.getStreamContent());
        assertNotNull(viewModel.getIsStreaming());
        assertNotNull(viewModel.getStreamTitle());
        assertNotNull(viewModel.getStreamProgress());
        assertNotNull(viewModel.getStreamStatus());
        assertNotNull(viewModel.getStartButtonEnabled());
        assertNotNull(viewModel.getStopButtonEnabled());
        assertNotNull(viewModel.getClearButtonEnabled());
    }

    @Test
    public void testViewModelCleanup() {
        // 设置一些观察者
        viewModel.getStreamContent().observeForever(mockStringObserver);
        viewModel.getIsStreaming().observeForever(mockBooleanObserver);
        
        // 执行清理
        viewModel.onCleared();
        
        // 验证清理完成（主要验证不会抛出异常）
        assertTrue(true);
    }

    @Test
    public void testStreamingInterruption() {
        // 启动流传输
        viewModel.startStreaming();
        assertTrue(viewModel.getIsStreaming().get());
        
        // 立即停止
        viewModel.stopStreaming();
        
        // 验证状态
        assertFalse(viewModel.getIsStreaming().get());
    }

    @Test
    public void testContentAndButtonStateIntegration() {
        // 初始状态：无内容，清除按钮禁用
        assertEquals("", viewModel.getStreamContent().get());
        assertFalse(viewModel.getClearButtonEnabled().get());
        
        // 添加内容：清除按钮启用
        viewModel.getStreamContent().set("内容");
        assertTrue(viewModel.getClearButtonEnabled().get());
        
        // 开始流传输：清除按钮禁用
        viewModel.getIsStreaming().set(true);
        assertFalse(viewModel.getClearButtonEnabled().get());
        
        // 停止流传输：清除按钮重新启用
        viewModel.getIsStreaming().set(false);
        assertTrue(viewModel.getClearButtonEnabled().get());
        
        // 清除内容：清除按钮禁用
        viewModel.getStreamContent().set("");
        assertFalse(viewModel.getClearButtonEnabled().get());
    }
}