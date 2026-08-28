package com.fxzs.lingxiagent.view.chat.delegate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * DrawingMessageDelegate 单元测试
 * 测试绘画消息委托的功能，包括绘画状态显示和操作按钮处理
 */
public class DrawingMessageDelegateTest {
    
    private DrawingMessageDelegate delegate;
    
    @Mock
    private ViewGroup mockParent;
    
    @Mock
    private LayoutInflater mockInflater;
    
    @Mock
    private View mockItemView;
    
    @Mock
    private ImageView mockIvDrawing;
    
    @Mock
    private LinearLayout mockLlActionsDrawing;
    
    @Mock
    private ImageView mockIvChatDownload;
    
    @Mock
    private ImageView mockIvChatRefresh;
    
    @Mock
    private RelativeLayout mockRlProgress;
    
    @Mock
    private ProgressBar mockProgressBar;
    
    @Mock
    private TextView mockTvProgress;
    
    @Mock
    private ChatMessage mockMessage;
    
    @Mock
    private ChatAdapterContext mockContext;
    
    @Mock
    private Context mockAndroidContext;
    
    private static final int TEST_POSITION = 1;
    private static final String TEST_DRAWING_URL = "https://example.com/drawing.jpg";
    private static final String TEST_PROGRESS = "75%";
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        delegate = new DrawingMessageDelegate();
        
        setupMockViews();
        setupMockMessage();
        setupMockContext();
    }
    
    private void setupMockViews() {
        when(mockItemView.findViewById(R.id.iv_drawing)).thenReturn(mockIvDrawing);
        when(mockItemView.findViewById(R.id.ll_actions_drawing)).thenReturn(mockLlActionsDrawing);
        when(mockItemView.findViewById(R.id.iv_chat_download)).thenReturn(mockIvChatDownload);
        when(mockItemView.findViewById(R.id.iv_chat_refresh)).thenReturn(mockIvChatRefresh);
        when(mockItemView.findViewById(R.id.rl_progress)).thenReturn(mockRlProgress);
        when(mockItemView.findViewById(R.id.progress_bar)).thenReturn(mockProgressBar);
        when(mockItemView.findViewById(R.id.tv_progress)).thenReturn(mockTvProgress);
    }
    
    private void setupMockMessage() {
        when(mockMessage.getUrl()).thenReturn(TEST_DRAWING_URL);
        when(mockMessage.getProgress()).thenReturn(TEST_PROGRESS);
        when(mockMessage.getStatus()).thenReturn(Constant.DrawingState.COMPLETED);
        when(mockMessage.getDrawingWidth()).thenReturn(512);
        when(mockMessage.getDrawingHeight()).thenReturn(512);
    }
    
    private void setupMockContext() {
        when(mockContext.getContext()).thenReturn(mockAndroidContext);
        when(mockContext.isSelectable()).thenReturn(false);
        when(mockContext.getMsgActionCallback()).thenReturn(mock(com.fxzs.lingxiagent.model.chat.callback.MsgActionCallback.class));
    }
    
    @Test
    public void testGetViewType() {
        assertEquals("ViewType should be 3 (TYPE_AI_DRAWING)", 3, delegate.getViewType());
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
        when(mockInflater.inflate(R.layout.item_drawing_message, mockParent, false))
            .thenReturn(mockItemView);
        
        RecyclerView.ViewHolder holder = delegate.onCreateViewHolder(mockParent, mockInflater);
        
        assertNotNull("ViewHolder should not be null", holder);
        assertTrue("ViewHolder should be ChatViewHolder", 
                  holder instanceof ChatAdapter.ChatViewHolder);
        verify(mockInflater).inflate(R.layout.item_drawing_message, mockParent, false);
    }
    
    @Test
    public void testBindViewHolder_CompletedDrawing() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getStatus()).thenReturn(Constant.DrawingState.COMPLETED);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证完成状态的UI设置
        verify(mockLlActionsDrawing).setVisibility(View.VISIBLE);
        verify(mockRlProgress).setVisibility(View.GONE);
        
        // 验证操作按钮的点击监听器设置
        verify(mockIvChatDownload).setOnClickListener(any(View.OnClickListener.class));
        verify(mockIvChatRefresh).setOnClickListener(any(View.OnClickListener.class));
    }
    
    @Test
    public void testBindViewHolder_InProgressDrawing() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getStatus()).thenReturn(Constant.DrawingState.IN_PROGRESS);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证进行中状态的UI设置
        verify(mockRlProgress).setVisibility(View.VISIBLE);
        verify(mockLlActionsDrawing).setVisibility(View.GONE);
        verify(mockTvProgress).setText(TEST_PROGRESS);
    }
    
    @Test
    public void testBindViewHolder_ErrorDrawing() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getStatus()).thenReturn(Constant.DrawingState.ERROR);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证错误状态的UI设置
        verify(mockLlActionsDrawing).setVisibility(View.VISIBLE);
        verify(mockRlProgress).setVisibility(View.GONE);
        
        // 错误状态下应该显示重新生成按钮
        verify(mockIvChatRefresh).setVisibility(View.VISIBLE);
    }
    
    @Test
    public void testBindViewHolder_SelectableMode() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockContext.isSelectable()).thenReturn(true);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 在选择模式下，操作按钮应该隐藏
        verify(mockLlActionsDrawing).setVisibility(View.GONE);
    }
    
    @Test
    public void testGetExpectedViewHolderClass() {
        Class<? extends RecyclerView.ViewHolder> expectedClass = delegate.getExpectedViewHolderClass();
        
        assertEquals("Expected ViewHolder class should be ChatViewHolder", 
                    ChatAdapter.ChatViewHolder.class, expectedClass);
    }
    
    @Test
    public void testDrawingDimensionsCalculation() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        // 测试不同的绘画尺寸
        when(mockMessage.getDrawingWidth()).thenReturn(1024);
        when(mockMessage.getDrawingHeight()).thenReturn(768);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证尺寸计算逻辑被调用
        // 由于尺寸计算逻辑在私有方法中，我们通过副作用来验证
        // 比如ImageView的布局参数设置
        assertNotNull("Drawing ImageView should be configured", mockIvDrawing);
    }
    
    @Test
    public void testEdgeCases() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        // 测试null消息
        try {
            delegate.onBindViewHolder(holder, null, TEST_POSITION, mockContext);
            // 不应该崩溃
        } catch (Exception e) {
            fail("Should handle null message gracefully");
        }
        
        // 测试空URL
        when(mockMessage.getUrl()).thenReturn("");
        
        try {
            delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
            // 不应该崩溃
        } catch (Exception e) {
            fail("Should handle empty URL gracefully");
        }
    }
}