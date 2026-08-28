package com.fxzs.lingxiagent.view.chat.delegate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * AgentHeadDelegate 单元测试
 * 测试智能体头部消息委托的功能，包括头像显示和提示文本设置
 */
public class AgentHeadDelegateTest {
    
    private AgentHeadDelegate delegate;
    
    @Mock
    private ViewGroup mockParent;
    
    @Mock
    private LayoutInflater mockInflater;
    
    @Mock
    private View mockItemView;
    
    @Mock
    private ImageView mockIvAgent;
    
    @Mock
    private TextView mockTvAgentHint;
    
    @Mock
    private ChatMessage mockMessage;
    
    @Mock
    private ChatAdapterContext mockContext;
    
    @Mock
    private Context mockAndroidContext;
    
    private static final int TEST_POSITION = 0;
    private static final String TEST_HINT_MESSAGE = "我是您的智能助手，有什么可以帮您的吗？";
    private static final String TEST_AVATAR_URL = "https://example.com/agent-avatar.jpg";
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        delegate = new AgentHeadDelegate();
        
        setupMockViews();
        setupMockMessage();
        setupMockContext();
    }
    
    private void setupMockViews() {
        when(mockItemView.findViewById(R.id.iv_agent)).thenReturn(mockIvAgent);
        when(mockItemView.findViewById(R.id.tv_agent_hint)).thenReturn(mockTvAgentHint);
    }
    
    private void setupMockMessage() {
        when(mockMessage.getMessage()).thenReturn(TEST_HINT_MESSAGE);
        when(mockMessage.getAvatar()).thenReturn(TEST_AVATAR_URL);
    }
    
    private void setupMockContext() {
        when(mockContext.getContext()).thenReturn(mockAndroidContext);
    }
    
    @Test
    public void testGetViewType() {
        assertEquals("ViewType should be 2 (TYPE_USER_HEAD_AGENT)", 2, delegate.getViewType());
    }
    
    @Test
    public void testCreateViewHolder() {
        RecyclerView.ViewHolder holder = delegate.createViewHolder(mockItemView);
        
        assertNotNull("ViewHolder should not be null", holder);
        assertTrue("ViewHolder should be ChatViewHolder", 
                  holder instanceof ChatAdapter.ChatViewHolder);
    }
    
    @Test
    public void testOnCreateViewHolder() {
        when(mockInflater.inflate(R.layout.item_meeting_head_message, mockParent, false))
            .thenReturn(mockItemView);
        
        RecyclerView.ViewHolder holder = delegate.onCreateViewHolder(mockParent, mockInflater);
        
        assertNotNull("ViewHolder should not be null", holder);
        assertTrue("ViewHolder should be ChatViewHolder", 
                  holder instanceof ChatAdapter.ChatViewHolder);
        verify(mockInflater).inflate(R.layout.item_meeting_head_message, mockParent, false);
    }
    
    @Test
    public void testBindViewHolder_WithHintMessage() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证提示文本设置
        verify(mockTvAgentHint).setText(TEST_HINT_MESSAGE);
    }
    
    @Test
    public void testBindViewHolder_WithAvatar() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证头像设置，包括背景清除（修复头像重叠问题）
        verify(mockIvAgent).setBackground(null);
        // 注意：由于ImageUtil.netRadius是静态方法，我们无法直接验证其调用
        // 但可以验证setBackground(null)的调用，这是我们的修复逻辑
    }
    
    @Test
    public void testBindViewHolder_NullHintMessage() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getMessage()).thenReturn(null);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证null消息处理，不应该崩溃
        // setText(null)应该被安全处理
        verify(mockTvAgentHint).setText(isNull());
    }
    
    @Test
    public void testBindViewHolder_EmptyHintMessage() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getMessage()).thenReturn("");
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证空消息处理
        verify(mockTvAgentHint).setText("");
    }
    
    @Test
    public void testBindViewHolder_NullAvatar() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getAvatar()).thenReturn(null);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 当头像为null时，不应该清除背景或加载图片
        verify(mockIvAgent, never()).setBackground(null);
    }
    
    @Test
    public void testBindViewHolder_EmptyAvatar() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getAvatar()).thenReturn("");
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 当头像为空字符串时，不应该清除背景或加载图片
        verify(mockIvAgent, never()).setBackground(null);
    }
    
    @Test
    public void testGetExpectedViewHolderClass() {
        Class<? extends RecyclerView.ViewHolder> expectedClass = delegate.getExpectedViewHolderClass();
        
        assertEquals("Expected ViewHolder class should be ChatViewHolder", 
                    ChatAdapter.ChatViewHolder.class, expectedClass);
    }
    
    @Test
    public void testEdgeCases() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        // 测试null消息
        try {
            delegate.onBindViewHolder(holder, null, TEST_POSITION, mockContext);
            // 应该能够安全处理
        } catch (Exception e) {
            fail("Should handle null message gracefully: " + e.getMessage());
        }
        
        // 测试null上下文
        try {
            delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, null);
            // 可能会有异常，这是预期的，因为需要Context来加载图片
        } catch (Exception e) {
            // 预期的异常
        }
    }
    
    @Test
    public void testBackgroundClearingLogic() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        // 模拟有效的头像URL
        when(mockMessage.getAvatar()).thenReturn(TEST_AVATAR_URL);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证背景清除逻辑（这是修复头像重叠问题的关键）
        verify(mockIvAgent).setBackground(null);
        
        // 重置mock并测试无头像的情况
        reset(mockIvAgent);
        when(mockMessage.getAvatar()).thenReturn(null);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 无头像时不应该清除背景
        verify(mockIvAgent, never()).setBackground(null);
    }
}