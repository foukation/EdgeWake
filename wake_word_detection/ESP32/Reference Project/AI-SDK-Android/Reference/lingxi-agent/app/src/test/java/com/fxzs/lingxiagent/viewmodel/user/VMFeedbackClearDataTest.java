package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * VMFeedback清空表单数据功能的单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class VMFeedbackClearDataTest {
    
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    
    @Mock
    private Application mockApplication;
    
    private VMFeedback viewModel;
    
    @Before
    public void setUp() {
        viewModel = new VMFeedback(mockApplication);
    }
    
    /**
     * 测试清空表单数据功能
     */
    @Test
    public void testClearFormData() {
        // 设置一些测试数据
        viewModel.getContactInfo().set("test@example.com");
        viewModel.getFeedbackContent().set("这是一个测试反馈内容");
        
        // 添加一些图片
        viewModel.addImage("image1.jpg");
        viewModel.addImage("image2.jpg");
        
        // 验证数据已设置
        assertEquals("test@example.com", viewModel.getContactInfo().get());
        assertEquals("这是一个测试反馈内容", viewModel.getFeedbackContent().get());
        assertNotNull(viewModel.getImageList().getValue());
        assertEquals(2, viewModel.getImageList().getValue().size());
        assertEquals("2/4", viewModel.getImageCountText().get());
        
        // 执行清空操作
        viewModel.clearFormData();
        
        // 验证数据已清空
        assertEquals("", viewModel.getContactInfo().get());
        assertEquals("", viewModel.getFeedbackContent().get());
        assertNotNull(viewModel.getImageList().getValue());
        assertEquals(0, viewModel.getImageList().getValue().size());
        assertEquals("0/4", viewModel.getImageCountText().get());
        assertNull(viewModel.getSubmitResult().getValue());
    }
    
    /**
     * 测试清空空表单不会出错
     */
    @Test
    public void testClearEmptyFormData() {
        // 确保表单是空的
        assertEquals("", viewModel.getContactInfo().get());
        assertEquals("", viewModel.getFeedbackContent().get());
        assertEquals(0, viewModel.getImageList().getValue().size());
        
        // 执行清空操作，不应该出错
        viewModel.clearFormData();
        
        // 验证仍然是空的
        assertEquals("", viewModel.getContactInfo().get());
        assertEquals("", viewModel.getFeedbackContent().get());
        assertEquals(0, viewModel.getImageList().getValue().size());
        assertEquals("0/4", viewModel.getImageCountText().get());
    }
    
    /**
     * 测试清空后可以重新输入数据
     */
    @Test
    public void testCanInputDataAfterClear() {
        // 设置初始数据
        viewModel.getContactInfo().set("old@example.com");
        viewModel.getFeedbackContent().set("旧的反馈内容");
        viewModel.addImage("old_image.jpg");
        
        // 清空数据
        viewModel.clearFormData();
        
        // 重新设置数据
        viewModel.getContactInfo().set("new@example.com");
        viewModel.getFeedbackContent().set("新的反馈内容");
        viewModel.addImage("new_image.jpg");
        
        // 验证新数据
        assertEquals("new@example.com", viewModel.getContactInfo().get());
        assertEquals("新的反馈内容", viewModel.getFeedbackContent().get());
        assertEquals(1, viewModel.getImageList().getValue().size());
        assertEquals("new_image.jpg", viewModel.getImageList().getValue().get(0));
        assertEquals("1/4", viewModel.getImageCountText().get());
    }
    
    /**
     * 测试清空数据后提交状态重置
     */
    @Test
    public void testSubmitResultResetAfterClear() {
        // 模拟提交成功状态
        viewModel.getSubmitResult().setValue(true);
        assertNotNull(viewModel.getSubmitResult().getValue());
        assertTrue(viewModel.getSubmitResult().getValue());

        // 清空数据
        viewModel.clearFormData();

        // 验证提交状态已重置
        assertNull(viewModel.getSubmitResult().getValue());
    }

    /**
     * 测试成功提交标志的设置和重置
     */
    @Test
    public void testSubmittedSuccessfullyFlag() {
        // 初始状态应该是false
        assertFalse(viewModel.hasSubmittedSuccessfully());

        // 清空数据后仍然是false
        viewModel.clearFormData();
        assertFalse(viewModel.hasSubmittedSuccessfully());

        // 注意：这里我们无法直接测试提交成功的情况，
        // 因为那需要模拟网络请求，这在单元测试中比较复杂
        // 但我们可以测试clearFormData确实会重置标志
    }
}
