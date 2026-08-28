package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.fxzs.lingxiagent.R;
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
 * BaseViewTypeDelegate 基类测试
 * 测试基类提供的通用功能
 */
public class BaseViewTypeDelegateTest {
    
    @Mock
    private ViewGroup mockParent;
    
    @Mock
    private LayoutInflater mockInflater;
    
    @Mock
    private View mockView;
    
    @Mock
    private TextView mockTextView;
    
    @Mock
    private ChatMessage mockMessage;
    
    @Mock
    private ChatAdapterContext mockContext;
    
    private TestBaseViewTypeDelegate delegate;
    private TestViewHolder testViewHolder;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        delegate = new TestBaseViewTypeDelegate();
        testViewHolder = new TestViewHolder(mockView);
    }
    
    @Test
    public void testGetViewType() {
        assertEquals("View type should match constructor parameter", 1, delegate.getViewType());
    }
    
    @Test
    public void testOnCreateViewHolder() {
        when(mockInflater.inflate(R.layout.item_user_message, mockParent, false))
            .thenReturn(mockView);
        
        RecyclerView.ViewHolder result = delegate.onCreateViewHolder(mockParent, mockInflater);
        
        assertNotNull("ViewHolder should not be null", result);
        verify(mockInflater).inflate(R.layout.item_user_message, mockParent, false);
    }
    
    @Test
    public void testValidViewHolderBinding() {
        delegate.onBindViewHolder(testViewHolder, mockMessage, 0, mockContext);
        
        assertTrue("Internal binding should be called", delegate.internalBindingCalled);
        assertEquals("Message should be passed", mockMessage, delegate.lastMessage);
        assertEquals("Position should be passed", 0, delegate.lastPosition);
        assertEquals("Context should be passed", mockContext, delegate.lastContext);
    }
    
    @Test
    public void testInvalidViewHolderBinding() {
        RecyclerView.ViewHolder invalidHolder = mock(RecyclerView.ViewHolder.class);
        
        delegate.onBindViewHolder(invalidHolder, mockMessage, 0, mockContext);
        
        assertFalse("Internal binding should not be called for invalid holder", 
                   delegate.internalBindingCalled);
    }
    
    @Test
    public void testPayloadHandling() {
        List<Object> payloads = Arrays.asList("test_payload");
        
        // 测试默认不处理 payloads
        delegate.onBindViewHolder(testViewHolder, mockMessage, 0, payloads, mockContext);
        
        assertTrue("Should fall back to standard binding", delegate.internalBindingCalled);
    }
    
    @Test
    public void testCustomPayloadHandling() {
        delegate.shouldHandlePayloads = true;
        List<Object> payloads = Arrays.asList("test_payload");
        
        delegate.onBindViewHolder(testViewHolder, mockMessage, 0, payloads, mockContext);
        
        assertTrue("Payload handling should be called", delegate.payloadHandlingCalled);
        assertFalse("Standard binding should not be called", delegate.internalBindingCalled);
    }
    
    @Test
    public void testSetVisibility() {
        // 测试设置可见性
        delegate.setVisibility(mockView, true);
        verify(mockView).setVisibility(View.VISIBLE);
        
        delegate.setVisibility(mockView, false);
        verify(mockView).setVisibility(View.GONE);
        
        // 测试 null 安全
        delegate.setVisibility(null, true); // 不应抛出异常
    }
    
    @Test
    public void testSetText() {
        // 测试设置文本
        delegate.setText(mockTextView, "test text");
        verify(mockTextView).setText("test text");
        
        // 测试 null 文本
        delegate.setText(mockTextView, null);
        verify(mockTextView).setText("");
        
        // 测试 null TextView
        delegate.setText(null, "test"); // 不应抛出异常
    }
    
    @Test
    public void testSetOnClickListener() {
        View.OnClickListener listener = mock(View.OnClickListener.class);
        
        delegate.setOnClickListener(mockView, listener);
        verify(mockView).setOnClickListener(listener);
        
        // 测试 null 安全
        delegate.setOnClickListener(null, listener); // 不应抛出异常
    }
    
    @Test
    public void testIsMessageSelected() {
        // 测试消息选中状态
        when(mockMessage.getIsSelected()).thenReturn(true);
        assertTrue("Should return true for selected message", delegate.isMessageSelected(mockMessage));
        
        when(mockMessage.getIsSelected()).thenReturn(false);
        assertFalse("Should return false for unselected message", delegate.isMessageSelected(mockMessage));
        
        assertFalse("Should return false for null message", delegate.isMessageSelected(null));
    }
    
    @Test
    public void testGetMessageContent() {
        // 测试获取消息内容
        when(mockMessage.getMessage()).thenReturn("test content");
        assertEquals("Should return message content", "test content", delegate.getMessageContent(mockMessage));
        
        when(mockMessage.getMessage()).thenReturn(null);
        assertEquals("Should return empty string for null content", "", delegate.getMessageContent(mockMessage));
        
        assertEquals("Should return empty string for null message", "", delegate.getMessageContent(null));
    }
    
    /**
     * 测试用的 ViewHolder
     */
    private static class TestViewHolder extends RecyclerView.ViewHolder {
        public TestViewHolder(View itemView) {
            super(itemView);
        }
    }
    
    /**
     * 测试用的 BaseViewTypeDelegate 实现
     */
    private static class TestBaseViewTypeDelegate extends BaseViewTypeDelegate {
        boolean internalBindingCalled = false;
        boolean payloadHandlingCalled = false;
        boolean shouldHandlePayloads = false;
        ChatMessage lastMessage;
        int lastPosition;
        ChatAdapterContext lastContext;
        
        public TestBaseViewTypeDelegate() {
            super(1, R.layout.item_user_message);
        }
        
        @Override
        protected RecyclerView.ViewHolder createViewHolder(View view) {
            return new TestViewHolder(view);
        }
        
        @Override
        protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message, 
                                               int position, ChatAdapterContext context) {
            internalBindingCalled = true;
            lastMessage = message;
            lastPosition = position;
            lastContext = context;
        }
        
        @Override
        protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
            return TestViewHolder.class;
        }
        
        @Override
        protected boolean handlePayloads(RecyclerView.ViewHolder holder, ChatMessage message, int position, 
                                       List<Object> payloads, ChatAdapterContext context) {
            if (shouldHandlePayloads) {
                payloadHandlingCalled = true;
                return true;
            }
            return false;
        }
    }
}