package com.fxzs.lingxiagent.view.chat.delegate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.view.dialog.TextSelectorView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * UserMessageDelegate 单元测试
 * 测试用户消息委托的所有功能，包括消息显示、选择状态和交互处理
 */
public class UserMessageDelegateTest {
    
    private UserMessageDelegate delegate;
    
    @Mock
    private ViewGroup mockParent;
    
    @Mock
    private LayoutInflater mockInflater;
    
    @Mock
    private View mockItemView;
    
    @Mock
    private TextSelectorView mockMessageText;
    
    @Mock
    private ImageView mockRadio;
    
    @Mock
    private ImageView mockRadioSelected;
    
    @Mock
    private ChatMessage mockMessage;
    
    @Mock
    private ChatAdapterContext mockContext;
    
    @Mock
    private Context mockAndroidContext;
    
    private static final String TEST_MESSAGE = "这是一条测试用户消息";
    private static final int TEST_POSITION = 5;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        delegate = new UserMessageDelegate();
        
        // 设置 mock 行为
        when(mockItemView.findViewById(R.id.messageText)).thenReturn(mockMessageText);
        when(mockItemView.findViewById(R.id.radio)).thenReturn(mockRadio);
        when(mockItemView.findViewById(R.id.radio_check)).thenReturn(mockRadioSelected);
        
        when(mockMessage.getMessage()).thenReturn(TEST_MESSAGE);
        when(mockMessage.getIsSelected()).thenReturn(false);
        when(mockMessage.getId()).thenReturn(123L);
        
        when(mockContext.getContext()).thenReturn(mockAndroidContext);
        when(mockContext.isSelectable()).thenReturn(false);
    }
    
    @Test
    public void testGetViewType() {
        assertEquals("ViewType should be 0 (TYPE_USER)", 0, delegate.getViewType());
    }
    
    @Test
    public void testCreateViewHolder() {
        RecyclerView.ViewHolder holder = delegate.createViewHolder(mockItemView);
        
        assertNotNull("ViewHolder should not be null", holder);
        assertTrue("ViewHolder should be UserMessageViewHolder", 
                  holder instanceof UserMessageDelegate.UserMessageViewHolder);
    }
    
    @Test
    public void testOnCreateViewHolder() {
        when(mockInflater.inflate(R.layout.item_user_message, mockParent, false))
            .thenReturn(mockItemView);
        
        RecyclerView.ViewHolder holder = delegate.onCreateViewHolder(mockParent, mockInflater);
        
        assertNotNull("ViewHolder should not be null", holder);
        assertTrue("ViewHolder should be UserMessageViewHolder", 
                  holder instanceof UserMessageDelegate.UserMessageViewHolder);
        verify(mockInflater).inflate(R.layout.item_user_message, mockParent, false);
    }
    
    @Test
    public void testBindViewHolder_NormalMode() {
        UserMessageDelegate.UserMessageViewHolder holder = 
            new UserMessageDelegate.UserMessageViewHolder(mockItemView);
        
        when(mockContext.isSelectable()).thenReturn(false);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证消息文本设置
        verify(mockMessageText).setText(TEST_MESSAGE);
        
        // 验证选择按钮隐藏
        verify(mockRadio).setVisibility(View.GONE);
        verify(mockRadioSelected).setVisibility(View.GONE);
    }
    
    @Test
    public void testBindViewHolder_SelectableMode() {
        UserMessageDelegate.UserMessageViewHolder holder = 
            new UserMessageDelegate.UserMessageViewHolder(mockItemView);
        
        when(mockContext.isSelectable()).thenReturn(true);
        when(mockMessage.getIsSelected()).thenReturn(false);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证消息文本设置
        verify(mockMessageText).setText(TEST_MESSAGE);
        
        // 验证选择按钮显示
        verify(mockRadio).setVisibility(View.VISIBLE);
        verify(mockRadioSelected).setVisibility(View.GONE);
    }
    
    @Test
    public void testBindViewHolder_SelectableModeWithSelection() {
        UserMessageDelegate.UserMessageViewHolder holder = 
            new UserMessageDelegate.UserMessageViewHolder(mockItemView);
        
        when(mockContext.isSelectable()).thenReturn(true);
        when(mockMessage.getIsSelected()).thenReturn(true);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证消息文本设置
        verify(mockMessageText).setText(TEST_MESSAGE);
        
        // 验证选择状态显示
        verify(mockRadio).setVisibility(View.VISIBLE);
        verify(mockRadioSelected).setVisibility(View.VISIBLE);
    }
    
    @Test
    public void testBindViewHolder_EmptyMessage() {
        UserMessageDelegate.UserMessageViewHolder holder = 
            new UserMessageDelegate.UserMessageViewHolder(mockItemView);
        
        when(mockMessage.getMessage()).thenReturn(null);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证空消息处理
        verify(mockMessageText).setText("");
    }
    
    @Test
    public void testBindViewHolder_LongClickListener() {
        UserMessageDelegate.UserMessageViewHolder holder = 
            new UserMessageDelegate.UserMessageViewHolder(mockItemView);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证长按监听器设置
        verify(mockMessageText).setOnLongClickListener(any(View.OnLongClickListener.class));
    }
    
    @Test
    public void testGetExpectedViewHolderClass() {
        Class<? extends RecyclerView.ViewHolder> expectedClass = delegate.getExpectedViewHolderClass();
        
        assertEquals("Expected ViewHolder class should be UserMessageViewHolder", 
                    UserMessageDelegate.UserMessageViewHolder.class, expectedClass);
    }
    
    @Test
    public void testViewHolderFieldsInitialization() {
        UserMessageDelegate.UserMessageViewHolder holder = 
            new UserMessageDelegate.UserMessageViewHolder(mockItemView);
        
        // 验证字段初始化
        assertEquals("messageText should be initialized", mockMessageText, holder.messageText);
        assertEquals("radio should be initialized", mockRadio, holder.radio);
        assertEquals("radioSelected should be initialized", mockRadioSelected, holder.radioSelected);
    }
    
    /**
     * 测试消息选择状态的切换
     */
    @Test
    public void testSelectionStateToggle() {
        UserMessageDelegate.UserMessageViewHolder holder = 
            new UserMessageDelegate.UserMessageViewHolder(mockItemView);
        
        // 测试从非选择模式到选择模式
        when(mockContext.isSelectable()).thenReturn(false);
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        verify(mockRadio).setVisibility(View.GONE);
        
        // 重置 mock
        reset(mockRadio, mockRadioSelected);
        
        // 测试选择模式
        when(mockContext.isSelectable()).thenReturn(true);
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        verify(mockRadio).setVisibility(View.VISIBLE);
    }
    
    /**
     * 测试不同消息内容的处理
     */
    @Test
    public void testDifferentMessageContents() {
        UserMessageDelegate.UserMessageViewHolder holder = 
            new UserMessageDelegate.UserMessageViewHolder(mockItemView);
        
        // 测试普通文本
        when(mockMessage.getMessage()).thenReturn("普通文本消息");
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        verify(mockMessageText).setText("普通文本消息");
        
        // 重置 mock
        reset(mockMessageText);
        
        // 测试长文本
        String longText = "这是一条很长的消息".repeat(10);
        when(mockMessage.getMessage()).thenReturn(longText);
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        verify(mockMessageText).setText(longText);
        
        // 重置 mock
        reset(mockMessageText);
        
        // 测试空字符串
        when(mockMessage.getMessage()).thenReturn("");
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        verify(mockMessageText).setText("");
    }
    
    /**
     * 测试边界条件
     */
    @Test
    public void testEdgeCases() {
        UserMessageDelegate.UserMessageViewHolder holder = 
            new UserMessageDelegate.UserMessageViewHolder(mockItemView);
        
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
        
        // 测试负数位置
        delegate.onBindViewHolder(holder, mockMessage, -1, mockContext);
        // 应该不会崩溃
    }
    
    /**
     * 测试 ViewHolder 类型验证
     */
    @Test
    public void testViewHolderTypeValidation() {
        // 创建正确类型的 ViewHolder
        UserMessageDelegate.UserMessageViewHolder correctHolder = 
            new UserMessageDelegate.UserMessageViewHolder(mockItemView);
        
        // 测试正确类型
        delegate.onBindViewHolder(correctHolder, mockMessage, TEST_POSITION, mockContext);
        // 应该正常执行
        
        // 创建错误类型的 ViewHolder
        RecyclerView.ViewHolder wrongHolder = new RecyclerView.ViewHolder(mockItemView) {};
        
        // 测试错误类型（应该被基类的类型检查捕获）
        try {
            delegate.onBindViewHolder(wrongHolder, mockMessage, TEST_POSITION, mockContext);
            // 可能会抛出异常或被安全处理
        } catch (Exception e) {
            // 预期的异常
        }
    }
}