package com.fxzs.lingxiagent.view.chat.delegate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * AIMessageDelegate 单元测试
 * 测试AI消息委托的所有功能，包括消息显示、思考状态、操作按钮和TTS功能
 */
public class AIMessageDelegateTest {
    
    private AIMessageDelegate delegate;
    
    @Mock
    private ViewGroup mockParent;
    
    @Mock
    private LayoutInflater mockInflater;
    
    @Mock
    private View mockItemView;
    
    @Mock
    private LinearLayout mockMarkdownContainer;
    
    @Mock
    private LinearLayout mockLlActions;
    
    @Mock
    private LinearLayout mockLlThinking;
    
    @Mock
    private TextView mockTvThinkingTitle;
    
    @Mock
    private TextView mockTvThinking;
    
    @Mock
    private ImageView mockIvChatPlay;
    
    @Mock
    private ImageView mockIvChatCopy;
    
    @Mock
    private ImageView mockIvChatShare;
    
    @Mock
    private ImageView mockIvChatRefresh;
    
    @Mock
    private ImageView mockIvThinkingArrow;
    
    @Mock
    private ImageView mockIvThinkingDash;
    
    @Mock
    private ChatMessage mockMessage;
    
    @Mock
    private ChatAdapterContext mockContext;
    
    @Mock
    private Context mockAndroidContext;
    
    @Mock
    private TTSManager mockTTSManager;
    
    private static final String TEST_MESSAGE = "这是AI生成的测试消息";
    private static final String TEST_THINKING_MESSAGE = "AI正在思考这个问题";
    private static final String TEST_THINKING_TITLE = "思考中...";
    private static final int TEST_POSITION = 3;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        delegate = new AIMessageDelegate();
        
        // 设置基础 mock 行为
        setupMockViews();
        setupMockMessage();
        setupMockContext();
    }
    
    private void setupMockViews() {
        when(mockItemView.findViewById(R.id.markdown_container)).thenReturn(mockMarkdownContainer);
        when(mockItemView.findViewById(R.id.ll_actions)).thenReturn(mockLlActions);
        when(mockItemView.findViewById(R.id.ll_thinking)).thenReturn(mockLlThinking);
        when(mockItemView.findViewById(R.id.tv_thinking_title)).thenReturn(mockTvThinkingTitle);
        when(mockItemView.findViewById(R.id.tv_thinking)).thenReturn(mockTvThinking);
        when(mockItemView.findViewById(R.id.iv_chat_play)).thenReturn(mockIvChatPlay);
        when(mockItemView.findViewById(R.id.iv_chat_copy)).thenReturn(mockIvChatCopy);
        when(mockItemView.findViewById(R.id.iv_chat_share)).thenReturn(mockIvChatShare);
        when(mockItemView.findViewById(R.id.iv_chat_refresh)).thenReturn(mockIvChatRefresh);
        when(mockItemView.findViewById(R.id.iv_thinking_arrow)).thenReturn(mockIvThinkingArrow);
        when(mockItemView.findViewById(R.id.iv_thinking_dash)).thenReturn(mockIvThinkingDash);
    }
    
    private void setupMockMessage() {
        when(mockMessage.getMessage()).thenReturn(TEST_MESSAGE);
        when(mockMessage.getThinkMessage()).thenReturn(TEST_THINKING_MESSAGE);
        when(mockMessage.getThinkMessageTitle()).thenReturn(TEST_THINKING_TITLE);
        when(mockMessage.getStatus()).thenReturn(Constant.ThinkState.END);
        when(mockMessage.isHideThinking()).thenReturn(false);
        when(mockMessage.isHideActionRefresh()).thenReturn(false);
        when(mockMessage.getId()).thenReturn(456L);
    }
    
    private void setupMockContext() {
        when(mockContext.getContext()).thenReturn(mockAndroidContext);
        when(mockContext.isSelectable()).thenReturn(false);
        when(mockContext.getTTSManager()).thenReturn(mockTTSManager);
        when(mockContext.getOnLongClickListener()).thenReturn(mock(View.OnLongClickListener.class));
    }
    
    @Test
    public void testGetViewType() {
        assertEquals("ViewType should be 1 (TYPE_AI)", 1, delegate.getViewType());
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
        when(mockInflater.inflate(R.layout.item_ai_message, mockParent, false))
            .thenReturn(mockItemView);
        
        RecyclerView.ViewHolder holder = delegate.onCreateViewHolder(mockParent, mockInflater);
        
        assertNotNull("ViewHolder should not be null", holder);
        assertTrue("ViewHolder should be ChatViewHolder", 
                  holder instanceof ChatAdapter.ChatViewHolder);
        verify(mockInflater).inflate(R.layout.item_ai_message, mockParent, false);
    }
    
    @Test
    public void testBindViewHolder_NormalState() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getStatus()).thenReturn(Constant.ThinkState.END);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证思考标题设置
        verify(mockTvThinkingTitle).setText(TEST_THINKING_TITLE);
        
        // 验证思考内容显示状态
        verify(mockLlThinking).setVisibility(View.VISIBLE);
        verify(mockTvThinking).setVisibility(View.VISIBLE);
        
        // 验证操作按钮显示
        verify(mockLlActions).setVisibility(View.VISIBLE);
        
        // 验证思考箭头和虚线状态
        verify(mockIvThinkingArrow).setVisibility(View.VISIBLE);
        verify(mockIvThinkingDash).setVisibility(View.GONE);
    }
    
    @Test
    public void testBindViewHolder_ThinkingState() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getStatus()).thenReturn(Constant.ThinkState.THINKING);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证思考中状态
        verify(mockTvThinkingTitle).setText("思考中");
        verify(mockIvThinkingArrow).setVisibility(View.GONE);
        verify(mockLlActions).setVisibility(View.GONE);
    }
    
    @Test
    public void testBindViewHolder_StartState() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getStatus()).thenReturn(Constant.ThinkState.START);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证开始状态
        verify(mockTvThinkingTitle).setText("正在思考中");
        verify(mockIvThinkingArrow).setVisibility(View.GONE);
        verify(mockLlActions).setVisibility(View.GONE);
    }
    
    @Test
    public void testBindViewHolder_HiddenThinking() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.isHideThinking()).thenReturn(true);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证思考内容隐藏
        verify(mockTvThinking).setVisibility(View.GONE);
    }
    
    @Test
    public void testBindViewHolder_SelectableMode() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockContext.isSelectable()).thenReturn(true);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 在选择模式下，操作按钮应该隐藏
        verify(mockLlActions).setVisibility(View.GONE);
    }
    
    @Test
    public void testBindViewHolder_EmptyMessage() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getMessage()).thenReturn(null);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证空消息处理 - 应该清空markdown容器
        verify(mockMarkdownContainer).removeAllViews();
    }
    
    @Test
    public void testBindViewHolder_EmptyThinkingMessage() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getThinkMessage()).thenReturn("");
        when(mockMessage.getStatus()).thenReturn(Constant.ThinkState.END);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 当思考消息为空时，思考区域应该隐藏
        verify(mockLlThinking).setVisibility(View.GONE);
    }
    
    @Test
    public void testGetExpectedViewHolderClass() {
        Class<? extends RecyclerView.ViewHolder> expectedClass = delegate.getExpectedViewHolderClass();
        
        assertEquals("Expected ViewHolder class should be ChatViewHolder", 
                    ChatAdapter.ChatViewHolder.class, expectedClass);
    }
    
    /**
     * 测试TTS功能设置
     */
    @Test
    public void testTTSFeaturesSetup() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证TTS相关的设置被调用
        verify(mockContext).getTTSManager();
    }
    
    /**
     * 测试操作按钮的点击监听器设置
     */
    @Test
    public void testActionButtonsSetup() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证操作按钮的点击监听器被设置
        verify(mockIvChatPlay).setOnClickListener(any(View.OnClickListener.class));
        verify(mockIvChatCopy).setOnClickListener(any(View.OnClickListener.class));
        verify(mockIvChatShare).setOnClickListener(any(View.OnClickListener.class));
        verify(mockIvChatRefresh).setOnClickListener(any(View.OnClickListener.class));
    }
    
    /**
     * 测试思考箭头点击监听器
     */
    @Test
    public void testThinkingArrowClick() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证思考箭头的点击监听器被设置
        verify(mockIvThinkingArrow).setOnClickListener(any(View.OnClickListener.class));
    }
    
    /**
     * 测试边界条件
     */
    @Test
    public void testEdgeCases() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        // 测试 null 消息
        delegate.onBindViewHolder(holder, null, TEST_POSITION, mockContext);
        // 应该不会崩溃
        
        // 测试 null 上下文
        try {
            delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, null);
            // 可能会抛出异常，这是预期的
        } catch (Exception e) {
            // 预期的异常
        }
    }
    
    /**
     * 测试不同位置的影响
     */
    @Test
    public void testPositionEffects() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        // 模拟列表中的最后一个位置
        when(mockContext.getItemCount()).thenReturn(TEST_POSITION + 1); // position是最后一个
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 在最后位置且非选择模式下，操作按钮应该可见
        verify(mockLlActions).setVisibility(View.VISIBLE);
    }
    
    /**
     * 测试刷新按钮显示逻辑
     */
    @Test
    public void testRefreshButtonVisibility() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        // 测试隐藏刷新按钮
        when(mockMessage.isHideActionRefresh()).thenReturn(true);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        verify(mockIvChatRefresh).setVisibility(View.GONE);
        
        // 重置并测试显示刷新按钮
        reset(mockIvChatRefresh);
        when(mockMessage.isHideActionRefresh()).thenReturn(false);
        when(mockContext.getItemCount()).thenReturn(TEST_POSITION + 1); // 确保是最后一个位置
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        verify(mockIvChatRefresh).setVisibility(View.VISIBLE);
    }
}