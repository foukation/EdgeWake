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
 * UserMessageDelegate 集成测试
 * 测试 UserMessageDelegate 与 ViewTypeManager 和其他组件的集成
 */
public class UserMessageDelegateIntegrationTest {
    
    private ViewTypeManager viewTypeManager;
    private UserMessageDelegate userMessageDelegate;
    
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
    
    private static final int TYPE_USER = 0;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        viewTypeManager = new ViewTypeManager();
        userMessageDelegate = new UserMessageDelegate();
        
        // 设置 mock 行为
        when(mockItemView.findViewById(R.id.messageText)).thenReturn(mockMessageText);
        when(mockItemView.findViewById(R.id.radio)).thenReturn(mockRadio);
        when(mockItemView.findViewById(R.id.radio_check)).thenReturn(mockRadioSelected);
        
        when(mockInflater.inflate(R.layout.item_user_message, mockParent, false))
            .thenReturn(mockItemView);
        
        when(mockMessage.getMessage()).thenReturn("测试消息");
        when(mockMessage.getIsSelected()).thenReturn(false);
        when(mockMessage.getMsgType()).thenReturn(TYPE_USER);
        
        when(mockContext.getContext()).thenReturn(mockAndroidContext);
        when(mockContext.isSelectable()).thenReturn(false);
    }
    
    @Test
    public void testRegisterUserMessageDelegate() {
        // 注册用户消息委托
        viewTypeManager.registerDelegate(userMessageDelegate);
        
        // 验证注册成功
        assertTrue("Should have delegate for TYPE_USER", 
                  viewTypeManager.hasDelegate(TYPE_USER));
        assertEquals("Should return UserMessageDelegate", 
                    userMessageDelegate, viewTypeManager.getDelegate(TYPE_USER));
    }
    
    @Test
    public void testViewTypeManagerIntegration() {
        // 注册委托
        viewTypeManager.registerDelegate(userMessageDelegate);
        
        // 通过管理器获取委托
        ViewTypeDelegate delegate = viewTypeManager.getDelegate(TYPE_USER);
        
        // 验证委托类型
        assertNotNull("Delegate should not be null", delegate);
        assertTrue("Delegate should be UserMessageDelegate", 
                  delegate instanceof UserMessageDelegate);
        assertEquals("ViewType should match", TYPE_USER, delegate.getViewType());
    }
    
    @Test
    public void testCompleteWorkflow() {
        // 1. 注册委托到管理器
        viewTypeManager.registerDelegate(userMessageDelegate);
        
        // 2. 通过管理器获取委托
        ViewTypeDelegate delegate = viewTypeManager.getDelegate(TYPE_USER);
        
        // 3. 创建 ViewHolder
        RecyclerView.ViewHolder holder = delegate.onCreateViewHolder(mockParent, mockInflater);
        
        // 4. 绑定数据
        delegate.onBindViewHolder(holder, mockMessage, 0, mockContext);
        
        // 验证整个流程
        assertNotNull("ViewHolder should be created", holder);
        assertTrue("ViewHolder should be correct type", 
                  holder instanceof UserMessageDelegate.UserMessageViewHolder);
        
        // 验证绑定操作
        verify(mockMessageText).setText("测试消息");
        verify(mockRadio).setVisibility(View.GONE);
        verify(mockRadioSelected).setVisibility(View.GONE);
    }
    
    @Test
    public void testMultipleDelegateRegistration() {
        // 注册用户消息委托
        viewTypeManager.registerDelegate(userMessageDelegate);
        
        // 创建并注册错误消息委托作为默认委托
        ErrorMessageDelegate errorDelegate = new ErrorMessageDelegate();
        viewTypeManager.setDefaultDelegate(errorDelegate);
        
        // 验证两个委托都可以正常工作
        ViewTypeDelegate userDelegate = viewTypeManager.getDelegate(TYPE_USER);
        ViewTypeDelegate defaultDelegate = viewTypeManager.getDelegate(999); // 未注册的类型
        
        assertTrue("User delegate should be UserMessageDelegate", 
                  userDelegate instanceof UserMessageDelegate);
        assertTrue("Default delegate should be ErrorMessageDelegate", 
                  defaultDelegate instanceof ErrorMessageDelegate);
    }
    
    @Test
    public void testDelegateWithDifferentMessageStates() {
        viewTypeManager.registerDelegate(userMessageDelegate);
        ViewTypeDelegate delegate = viewTypeManager.getDelegate(TYPE_USER);
        
        RecyclerView.ViewHolder holder = delegate.onCreateViewHolder(mockParent, mockInflater);
        
        // 测试普通状态
        when(mockContext.isSelectable()).thenReturn(false);
        delegate.onBindViewHolder(holder, mockMessage, 0, mockContext);
        verify(mockRadio).setVisibility(View.GONE);
        
        // 重置 mock
        reset(mockRadio, mockRadioSelected);
        
        // 测试选择状态
        when(mockContext.isSelectable()).thenReturn(true);
        when(mockMessage.getIsSelected()).thenReturn(true);
        delegate.onBindViewHolder(holder, mockMessage, 0, mockContext);
        verify(mockRadio).setVisibility(View.VISIBLE);
        verify(mockRadioSelected).setVisibility(View.VISIBLE);
    }
    
    @Test
    public void testDelegateErrorHandling() {
        viewTypeManager.registerDelegate(userMessageDelegate);
        ViewTypeDelegate delegate = viewTypeManager.getDelegate(TYPE_USER);
        
        RecyclerView.ViewHolder holder = delegate.onCreateViewHolder(mockParent, mockInflater);
        
        // 测试 null 消息处理
        delegate.onBindViewHolder(holder, null, 0, mockContext);
        // 应该不会崩溃
        
        // 测试空消息处理
        when(mockMessage.getMessage()).thenReturn(null);
        delegate.onBindViewHolder(holder, mockMessage, 0, mockContext);
        verify(mockMessageText).setText("");
    }
    
    @Test
    public void testViewHolderRecycling() {
        viewTypeManager.registerDelegate(userMessageDelegate);
        ViewTypeDelegate delegate = viewTypeManager.getDelegate(TYPE_USER);
        
        // 创建多个 ViewHolder
        RecyclerView.ViewHolder holder1 = delegate.onCreateViewHolder(mockParent, mockInflater);
        RecyclerView.ViewHolder holder2 = delegate.onCreateViewHolder(mockParent, mockInflater);
        
        // 验证 ViewHolder 类型一致
        assertEquals("ViewHolders should be same type", 
                    holder1.getClass(), holder2.getClass());
        assertTrue("Both should be UserMessageViewHolder", 
                  holder1 instanceof UserMessageDelegate.UserMessageViewHolder);
        assertTrue("Both should be UserMessageViewHolder", 
                  holder2 instanceof UserMessageDelegate.UserMessageViewHolder);
    }
    
    @Test
    public void testDelegateValidation() {
        // 验证委托的基本属性
        assertEquals("ViewType should be TYPE_USER", TYPE_USER, userMessageDelegate.getViewType());
        
        // 验证委托可以被注册
        viewTypeManager.registerDelegate(userMessageDelegate);
        assertTrue("Delegate should be registered", 
                  viewTypeManager.hasDelegate(TYPE_USER));
        
        // 验证委托验证通过
        assertTrue("All delegates should be valid", 
                  viewTypeManager.validateDelegates());
    }
    
    @Test
    public void testPerformanceConsiderations() {
        viewTypeManager.registerDelegate(userMessageDelegate);
        ViewTypeDelegate delegate = viewTypeManager.getDelegate(TYPE_USER);
        
        // 测试多次创建 ViewHolder 的性能
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            RecyclerView.ViewHolder holder = delegate.onCreateViewHolder(mockParent, mockInflater);
            assertNotNull("ViewHolder should be created", holder);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证性能在合理范围内（100次创建应该在1秒内完成）
        assertTrue("ViewHolder creation should be fast", duration < 1000);
    }
    
    @Test
    public void testMemoryLeakPrevention() {
        viewTypeManager.registerDelegate(userMessageDelegate);
        ViewTypeDelegate delegate = viewTypeManager.getDelegate(TYPE_USER);
        
        RecyclerView.ViewHolder holder = delegate.onCreateViewHolder(mockParent, mockInflater);
        
        // 绑定数据
        delegate.onBindViewHolder(holder, mockMessage, 0, mockContext);
        
        // 模拟 ViewHolder 回收
        delegate.onViewRecycled(holder);
        
        // 验证没有内存泄漏（这里主要是确保方法调用不会抛出异常）
        // 实际的内存泄漏检测需要更复杂的工具
        assertNotNull("Holder should still exist after recycling", holder);
    }
}