package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * ViewTypeManager 单元测试
 * 测试委托管理器的所有功能，包括注册、获取、验证和错误处理
 */
public class ViewTypeManagerTest {
    
    private ViewTypeManager viewTypeManager;
    
    @Mock
    private ViewTypeDelegate mockDelegate1;
    
    @Mock
    private ViewTypeDelegate mockDelegate2;
    
    @Mock
    private ViewTypeDelegate mockDefaultDelegate;
    
    @Mock
    private ViewGroup mockParent;
    
    @Mock
    private LayoutInflater mockInflater;
    
    @Mock
    private RecyclerView.ViewHolder mockViewHolder;
    
    @Mock
    private ChatMessage mockMessage;
    
    @Mock
    private ChatAdapterContext mockContext;
    
    private static final int VIEW_TYPE_1 = 1;
    private static final int VIEW_TYPE_2 = 2;
    private static final int UNREGISTERED_VIEW_TYPE = 999;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewTypeManager = new ViewTypeManager();
        
        // 设置 mock 委托的行为
        when(mockDelegate1.getViewType()).thenReturn(VIEW_TYPE_1);
        when(mockDelegate2.getViewType()).thenReturn(VIEW_TYPE_2);
        when(mockDefaultDelegate.getViewType()).thenReturn(-1); // 默认委托可以有任意类型
        
        when(mockDelegate1.onCreateViewHolder(any(), any())).thenReturn(mockViewHolder);
        when(mockDelegate2.onCreateViewHolder(any(), any())).thenReturn(mockViewHolder);
        when(mockDefaultDelegate.onCreateViewHolder(any(), any())).thenReturn(mockViewHolder);
    }
    
    @Test
    public void testRegisterDelegate_Success() {
        viewTypeManager.registerDelegate(mockDelegate1);
        
        assertTrue("Should have delegate for VIEW_TYPE_1", 
                  viewTypeManager.hasDelegate(VIEW_TYPE_1));
        assertEquals("Should return correct delegate", 
                    mockDelegate1, viewTypeManager.getDelegate(VIEW_TYPE_1));
        assertEquals("Delegate count should be 1", 1, viewTypeManager.getDelegateCount());
    }
    
    @Test
    public void testRegisterDelegate_MultipleSuccess() {
        viewTypeManager.registerDelegate(mockDelegate1);
        viewTypeManager.registerDelegate(mockDelegate2);
        
        assertTrue("Should have delegate for VIEW_TYPE_1", 
                  viewTypeManager.hasDelegate(VIEW_TYPE_1));
        assertTrue("Should have delegate for VIEW_TYPE_2", 
                  viewTypeManager.hasDelegate(VIEW_TYPE_2));
        assertEquals("Delegate count should be 2", 2, viewTypeManager.getDelegateCount());
        
        Set<Integer> supportedTypes = viewTypeManager.getSupportedViewTypes();
        assertTrue("Should contain VIEW_TYPE_1", supportedTypes.contains(VIEW_TYPE_1));
        assertTrue("Should contain VIEW_TYPE_2", supportedTypes.contains(VIEW_TYPE_2));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterDelegate_NullDelegate() {
        viewTypeManager.registerDelegate(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterDelegate_DuplicateViewType() {
        viewTypeManager.registerDelegate(mockDelegate1);
        
        // 尝试注册相同视图类型的另一个委托
        ViewTypeDelegate anotherDelegate = mock(ViewTypeDelegate.class);
        when(anotherDelegate.getViewType()).thenReturn(VIEW_TYPE_1);
        
        viewTypeManager.registerDelegate(anotherDelegate);
    }
    
    @Test
    public void testGetDelegate_Success() {
        viewTypeManager.registerDelegate(mockDelegate1);
        
        ViewTypeDelegate result = viewTypeManager.getDelegate(VIEW_TYPE_1);
        
        assertNotNull("Delegate should not be null", result);
        assertEquals("Should return correct delegate", mockDelegate1, result);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetDelegate_NotRegistered() {
        viewTypeManager.getDelegate(UNREGISTERED_VIEW_TYPE);
    }
    
    @Test
    public void testGetDelegate_WithDefaultDelegate() {
        viewTypeManager.setDefaultDelegate(mockDefaultDelegate);
        
        ViewTypeDelegate result = viewTypeManager.getDelegate(UNREGISTERED_VIEW_TYPE);
        
        assertNotNull("Should return default delegate", result);
        assertEquals("Should return default delegate", mockDefaultDelegate, result);
    }
    
    @Test
    public void testGetDelegateSafely_Success() {
        viewTypeManager.registerDelegate(mockDelegate1);
        
        ViewTypeDelegate result = viewTypeManager.getDelegateSafely(VIEW_TYPE_1);
        
        assertNotNull("Delegate should not be null", result);
        assertEquals("Should return correct delegate", mockDelegate1, result);
    }
    
    @Test
    public void testGetDelegateSafely_NotRegistered() {
        ViewTypeDelegate result = viewTypeManager.getDelegateSafely(UNREGISTERED_VIEW_TYPE);
        
        assertNull("Should return null for unregistered type", result);
    }
    
    @Test
    public void testHasDelegate() {
        assertFalse("Should not have delegate initially", 
                   viewTypeManager.hasDelegate(VIEW_TYPE_1));
        
        viewTypeManager.registerDelegate(mockDelegate1);
        
        assertTrue("Should have delegate after registration", 
                  viewTypeManager.hasDelegate(VIEW_TYPE_1));
        assertFalse("Should not have unregistered delegate", 
                   viewTypeManager.hasDelegate(VIEW_TYPE_2));
    }
    
    @Test
    public void testGetSupportedViewTypes() {
        assertTrue("Should be empty initially", 
                  viewTypeManager.getSupportedViewTypes().isEmpty());
        
        viewTypeManager.registerDelegate(mockDelegate1);
        viewTypeManager.registerDelegate(mockDelegate2);
        
        Set<Integer> supportedTypes = viewTypeManager.getSupportedViewTypes();
        assertEquals("Should have 2 supported types", 2, supportedTypes.size());
        assertTrue("Should contain VIEW_TYPE_1", supportedTypes.contains(VIEW_TYPE_1));
        assertTrue("Should contain VIEW_TYPE_2", supportedTypes.contains(VIEW_TYPE_2));
    }
    
    @Test
    public void testGetDelegateCount() {
        assertEquals("Should be 0 initially", 0, viewTypeManager.getDelegateCount());
        
        viewTypeManager.registerDelegate(mockDelegate1);
        assertEquals("Should be 1 after first registration", 1, viewTypeManager.getDelegateCount());
        
        viewTypeManager.registerDelegate(mockDelegate2);
        assertEquals("Should be 2 after second registration", 2, viewTypeManager.getDelegateCount());
    }
    
    @Test
    public void testSetDefaultDelegate() {
        assertNull("Default delegate should be null initially", 
                  viewTypeManager.getDefaultDelegate());
        
        viewTypeManager.setDefaultDelegate(mockDefaultDelegate);
        
        assertEquals("Should return set default delegate", 
                    mockDefaultDelegate, viewTypeManager.getDefaultDelegate());
    }
    
    @Test
    public void testSetDefaultDelegate_Null() {
        viewTypeManager.setDefaultDelegate(mockDefaultDelegate);
        viewTypeManager.setDefaultDelegate(null);
        
        assertNull("Default delegate should be null after clearing", 
                  viewTypeManager.getDefaultDelegate());
    }
    
    @Test
    public void testUnregisterDelegate_Success() {
        viewTypeManager.registerDelegate(mockDelegate1);
        assertTrue("Should have delegate before unregistration", 
                  viewTypeManager.hasDelegate(VIEW_TYPE_1));
        
        ViewTypeDelegate removed = viewTypeManager.unregisterDelegate(VIEW_TYPE_1);
        
        assertEquals("Should return removed delegate", mockDelegate1, removed);
        assertFalse("Should not have delegate after unregistration", 
                   viewTypeManager.hasDelegate(VIEW_TYPE_1));
        assertEquals("Delegate count should be 0", 0, viewTypeManager.getDelegateCount());
    }
    
    @Test
    public void testUnregisterDelegate_NotRegistered() {
        ViewTypeDelegate removed = viewTypeManager.unregisterDelegate(UNREGISTERED_VIEW_TYPE);
        
        assertNull("Should return null for unregistered type", removed);
    }
    
    @Test
    public void testClearAllDelegates() {
        viewTypeManager.registerDelegate(mockDelegate1);
        viewTypeManager.registerDelegate(mockDelegate2);
        assertEquals("Should have 2 delegates", 2, viewTypeManager.getDelegateCount());
        
        viewTypeManager.clearAllDelegates();
        
        assertEquals("Should have 0 delegates after clearing", 0, viewTypeManager.getDelegateCount());
        assertTrue("Supported types should be empty", 
                  viewTypeManager.getSupportedViewTypes().isEmpty());
    }
    
    @Test
    public void testValidateDelegates_AllValid() {
        viewTypeManager.registerDelegate(mockDelegate1);
        viewTypeManager.registerDelegate(mockDelegate2);
        
        boolean result = viewTypeManager.validateDelegates();
        
        assertTrue("All delegates should be valid", result);
    }
    
    @Test
    public void testValidateDelegates_InvalidDelegate() {
        // 创建一个返回错误视图类型的委托
        ViewTypeDelegate invalidDelegate = mock(ViewTypeDelegate.class);
        when(invalidDelegate.getViewType()).thenReturn(VIEW_TYPE_2); // 与注册的类型不同
        
        viewTypeManager.registerDelegate(mockDelegate1);
        // 手动插入无效委托来模拟数据不一致
        viewTypeManager.registerDelegate(invalidDelegate);
        
        // 修改委托返回的视图类型来模拟不一致
        when(invalidDelegate.getViewType()).thenReturn(999);
        
        boolean result = viewTypeManager.validateDelegates();
        
        assertFalse("Should detect invalid delegate", result);
    }
    
    @Test
    public void testGetDebugInfo() {
        viewTypeManager.registerDelegate(mockDelegate1);
        viewTypeManager.setDefaultDelegate(mockDefaultDelegate);
        
        String debugInfo = viewTypeManager.getDebugInfo();
        
        assertNotNull("Debug info should not be null", debugInfo);
        assertTrue("Should contain total delegates info", 
                  debugInfo.contains("Total delegates: 1"));
        assertTrue("Should contain default delegate info", 
                  debugInfo.contains("Default delegate:"));
        assertTrue("Should contain registered delegates info", 
                  debugInfo.contains("Registered delegates:"));
    }
    
    @Test
    public void testGetDebugInfo_Empty() {
        String debugInfo = viewTypeManager.getDebugInfo();
        
        assertNotNull("Debug info should not be null", debugInfo);
        assertTrue("Should show 0 delegates", debugInfo.contains("Total delegates: 0"));
        assertTrue("Should show null default delegate", debugInfo.contains("Default delegate: null"));
    }
    
    /**
     * 测试实际的委托功能集成
     */
    @Test
    public void testDelegateIntegration() {
        // 注册委托
        viewTypeManager.registerDelegate(mockDelegate1);
        
        // 获取委托并调用其方法
        ViewTypeDelegate delegate = viewTypeManager.getDelegate(VIEW_TYPE_1);
        RecyclerView.ViewHolder holder = delegate.onCreateViewHolder(mockParent, mockInflater);
        delegate.onBindViewHolder(holder, mockMessage, 0, mockContext);
        
        // 验证委托方法被调用
        verify(mockDelegate1).onCreateViewHolder(mockParent, mockInflater);
        verify(mockDelegate1).onBindViewHolder(holder, mockMessage, 0, mockContext);
    }
    
    /**
     * 测试错误消息的详细信息
     */
    @Test
    public void testErrorMessageDetails() {
        viewTypeManager.registerDelegate(mockDelegate1);
        
        try {
            viewTypeManager.getDelegate(UNREGISTERED_VIEW_TYPE);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            assertTrue("Error message should contain view type", 
                      message.contains(String.valueOf(UNREGISTERED_VIEW_TYPE)));
            assertTrue("Error message should contain available types", 
                      message.contains(String.valueOf(VIEW_TYPE_1)));
        }
    }
    
    /**
     * 测试重复注册的错误消息
     */
    @Test
    public void testDuplicateRegistrationErrorMessage() {
        viewTypeManager.registerDelegate(mockDelegate1);
        
        ViewTypeDelegate anotherDelegate = mock(ViewTypeDelegate.class);
        when(anotherDelegate.getViewType()).thenReturn(VIEW_TYPE_1);
        
        try {
            viewTypeManager.registerDelegate(anotherDelegate);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            assertTrue("Error message should contain view type", 
                      message.contains(String.valueOf(VIEW_TYPE_1)));
            assertTrue("Error message should mention existing delegate", 
                      message.contains("already registered"));
        }
    }
    
    /**
     * 测试线程安全性（基本测试）
     */
    @Test
    public void testBasicThreadSafety() throws InterruptedException {
        final int numThreads = 10;
        final Thread[] threads = new Thread[numThreads];
        final boolean[] results = new boolean[numThreads];
        
        // 先注册一个委托
        viewTypeManager.registerDelegate(mockDelegate1);
        
        // 创建多个线程同时访问委托
        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    ViewTypeDelegate delegate = viewTypeManager.getDelegate(VIEW_TYPE_1);
                    results[index] = (delegate == mockDelegate1);
                } catch (Exception e) {
                    results[index] = false;
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证所有线程都成功获取了正确的委托
        for (boolean result : results) {
            assertTrue("All threads should successfully get the delegate", result);
        }
    }
}