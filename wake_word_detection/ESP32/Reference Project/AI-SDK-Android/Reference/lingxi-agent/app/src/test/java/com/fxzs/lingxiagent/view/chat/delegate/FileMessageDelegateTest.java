package com.fxzs.lingxiagent.view.chat.delegate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * FileMessageDelegate 单元测试
 * 测试文件消息委托的功能，包括文件列表显示和交互
 */
public class FileMessageDelegateTest {
    
    private FileMessageDelegate delegate;
    
    @Mock
    private ViewGroup mockParent;
    
    @Mock
    private LayoutInflater mockInflater;
    
    @Mock
    private View mockItemView;
    
    @Mock
    private RecyclerView mockRvFiles;
    
    @Mock
    private TextView mockTvFileName;
    
    @Mock
    private ChatMessage mockMessage;
    
    @Mock
    private ChatAdapterContext mockContext;
    
    @Mock
    private Context mockAndroidContext;
    
    private static final int TEST_POSITION = 2;
    private static final String TEST_FILE_NAME = "test_document.pdf";
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        delegate = new FileMessageDelegate();
        
        setupMockViews();
        setupMockMessage();
        setupMockContext();
    }
    
    private void setupMockViews() {
        when(mockItemView.findViewById(R.id.rv_files)).thenReturn(mockRvFiles);
        when(mockItemView.findViewById(R.id.tv_file_name)).thenReturn(mockTvFileName);
    }
    
    private void setupMockMessage() {
        when(mockMessage.getFileName()).thenReturn(TEST_FILE_NAME);
        when(mockMessage.getFileList()).thenReturn(createTestFileList());
    }
    
    private void setupMockContext() {
        when(mockContext.getContext()).thenReturn(mockAndroidContext);
        when(mockContext.isSelectable()).thenReturn(false);
    }
    
    private List<String> createTestFileList() {
        List<String> fileList = new ArrayList<>();
        fileList.add("file1.txt");
        fileList.add("file2.pdf");
        fileList.add("file3.jpg");
        return fileList;
    }
    
    @Test
    public void testGetViewType() {
        assertEquals("ViewType should be 4 (TYPE_FILE)", 4, delegate.getViewType());
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
        when(mockInflater.inflate(R.layout.item_file_message, mockParent, false))
            .thenReturn(mockItemView);
        
        RecyclerView.ViewHolder holder = delegate.onCreateViewHolder(mockParent, mockInflater);
        
        assertNotNull("ViewHolder should not be null", holder);
        assertTrue("ViewHolder should be ChatViewHolder", 
                  holder instanceof ChatAdapter.ChatViewHolder);
        verify(mockInflater).inflate(R.layout.item_file_message, mockParent, false);
    }
    
    @Test
    public void testBindViewHolder_WithFileName() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证文件名显示
        verify(mockTvFileName).setText(TEST_FILE_NAME);
    }
    
    @Test
    public void testBindViewHolder_EmptyFileName() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getFileName()).thenReturn("");
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证空文件名处理
        verify(mockTvFileName).setText("");
    }
    
    @Test
    public void testBindViewHolder_NullFileName() {
        ChatAdapter.ChatViewHolder holder = new ChatAdapter.ChatViewHolder(mockItemView);
        
        when(mockMessage.getFileName()).thenReturn(null);
        
        delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, mockContext);
        
        // 验证null文件名不会导致崩溃
        verify(mockTvFileName).setText(isNull());
    }
    
    @Test
    public void testGetExpectedViewHolderClass() {
        Class<? extends RecyclerView.ViewHolder> expectedClass = delegate.getExpectedViewHolderClass();
        
        assertEquals("Expected ViewHolder class should be ChatViewHolder", 
                    ChatAdapter.ChatViewHolder.class, expectedClass);
    }
    
    /**
     * 测试边界条件
     */
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
        
        // 测试null上下文
        try {
            delegate.onBindViewHolder(holder, mockMessage, TEST_POSITION, null);
            // 可能会有异常，这是预期的
        } catch (Exception e) {
            // 预期的异常
        }
    }
}