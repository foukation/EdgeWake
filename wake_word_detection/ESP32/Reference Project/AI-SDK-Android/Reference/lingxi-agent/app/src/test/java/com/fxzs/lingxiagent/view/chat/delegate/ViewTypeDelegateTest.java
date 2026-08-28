package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * ViewTypeDelegate 接口测试
 * 测试接口的默认方法实现和契约
 */
public class ViewTypeDelegateTest {
    
    @Mock
    private ViewGroup mockParent;
    
    @Mock
    private LayoutInflater mockInflater;
    
    @Mock
    private RecyclerView.ViewHolder mockHolder;
    
    @Mock
    private ChatMessage mockMessage;
    
    @Mock
    private ChatAdapterContext mockContext;
    
    private TestViewTypeDelegate delegate;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        delegate = new TestViewTypeDelegate();
    }
    
    @Test
    public void testDefaultPayloadBinding() {
        // 测试默认的 payload 绑定实现
        List<Object> payloads = Arrays.asList("test_payload");
        
        delegate.onBindViewHolder(mockHolder, mockMessage, 0, payloads, mockContext);
        
        // 验证调用了标准绑定方法
        assertTrue("Standard binding should be called", delegate.standardBindingCalled);
    }
    
    @Test
    public void testDefaultLifecycleMethods() {
        // 测试默认的生命周期方法不会抛出异常
        assertDoesNotThrow(() -> {
            delegate.onViewRecycled(mockHolder);
            delegate.onViewAttachedToWindow(mockHolder);
            delegate.onViewDetachedFromWindow(mockHolder);
        });
    }
    
    @Test
    public void testViewTypeContract() {
        // 测试视图类型契约
        assertEquals("View type should match", 999, delegate.getViewType());
    }
    
    @Test
    public void testCreateViewHolderContract() {
        // 测试 ViewHolder 创建契约
        RecyclerView.ViewHolder result = delegate.onCreateViewHolder(mockParent, mockInflater);
        
        assertNotNull("ViewHolder should not be null", result);
        assertEquals("Should return mock holder", mockHolder, result);
    }
    
    @Test
    public void testBindViewHolderContract() {
        // 测试数据绑定契约
        delegate.onBindViewHolder(mockHolder, mockMessage, 0, mockContext);
        
        assertTrue("Binding should be called", delegate.standardBindingCalled);
        assertEquals("Message should be passed", mockMessage, delegate.lastMessage);
        assertEquals("Position should be passed", 0, delegate.lastPosition);
        assertEquals("Context should be passed", mockContext, delegate.lastContext);
    }
    
    private void assertDoesNotThrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }
    
    /**
     * 测试用的 ViewTypeDelegate 实现
     */
    private static class TestViewTypeDelegate implements ViewTypeDelegate {
        boolean standardBindingCalled = false;
        ChatMessage lastMessage;
        int lastPosition;
        ChatAdapterContext lastContext;
        
        @Override
        public int getViewType() {
            return 999; // 测试用的类型
        }
        
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, LayoutInflater inflater) {
            // 返回模拟的 ViewHolder
            return mock(RecyclerView.ViewHolder.class);
        }
        
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, ChatMessage message, 
                                    int position, ChatAdapterContext context) {
            standardBindingCalled = true;
            lastMessage = message;
            lastPosition = position;
            lastContext = context;
        }
    }
}