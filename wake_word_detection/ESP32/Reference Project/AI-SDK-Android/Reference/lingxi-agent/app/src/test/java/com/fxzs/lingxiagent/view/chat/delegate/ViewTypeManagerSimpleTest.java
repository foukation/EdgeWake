package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * ViewTypeManager 简单单元测试
 * 不依赖 Mockito，使用简单的测试实现
 */
public class ViewTypeManagerSimpleTest {
    
    private ViewTypeManager viewTypeManager;
    private TestViewTypeDelegate delegate1;
    private TestViewTypeDelegate delegate2;
    
    private static final int VIEW_TYPE_1 = 1;
    private static final int VIEW_TYPE_2 = 2;
    private static final int UNREGISTERED_VIEW_TYPE = 999;
    
    @Before
    public void setUp() {
        viewTypeManager = new ViewTypeManager();
        delegate1 = new TestViewTypeDelegate(VIEW_TYPE_1);
        delegate2 = new TestViewTypeDelegate(VIEW_TYPE_2);
    }
    
    @Test
    public void testRegisterDelegate_Success() {
        viewTypeManager.registerDelegate(delegate1);
        
        assertTrue("Should have delegate for VIEW_TYPE_1", 
                  viewTypeManager.hasDelegate(VIEW_TYPE_1));
        assertEquals("Should return correct delegate", 
                    delegate1, viewTypeManager.getDelegate(VIEW_TYPE_1));
        assertEquals("Delegate count should be 1", 1, viewTypeManager.getDelegateCount());
    }
    
    @Test
    public void testRegisterDelegate_MultipleSuccess() {
        viewTypeManager.registerDelegate(delegate1);
        viewTypeManager.registerDelegate(delegate2);
        
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
        viewTypeManager.registerDelegate(delegate1);
        
        // 尝试注册相同视图类型的另一个委托
        TestViewTypeDelegate anotherDelegate = new TestViewTypeDelegate(VIEW_TYPE_1);
        viewTypeManager.registerDelegate(anotherDelegate);
    }
    
    @Test
    public void testGetDelegate_Success() {
        viewTypeManager.registerDelegate(delegate1);
        
        ViewTypeDelegate result = viewTypeManager.getDelegate(VIEW_TYPE_1);
        
        assertNotNull("Delegate should not be null", result);
        assertEquals("Should return correct delegate", delegate1, result);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetDelegate_NotRegistered() {
        viewTypeManager.getDelegate(UNREGISTERED_VIEW_TYPE);
    }
    
    @Test
    public void testGetDelegate_WithDefaultDelegate() {
        TestViewTypeDelegate defaultDelegate = new TestViewTypeDelegate(-1);
        viewTypeManager.setDefaultDelegate(defaultDelegate);
        
        ViewTypeDelegate result = viewTypeManager.getDelegate(UNREGISTERED_VIEW_TYPE);
        
        assertNotNull("Should return default delegate", result);
        assertEquals("Should return default delegate", defaultDelegate, result);
    }
    
    @Test
    public void testGetDelegateSafely_Success() {
        viewTypeManager.registerDelegate(delegate1);
        
        ViewTypeDelegate result = viewTypeManager.getDelegateSafely(VIEW_TYPE_1);
        
        assertNotNull("Delegate should not be null", result);
        assertEquals("Should return correct delegate", delegate1, result);
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
        
        viewTypeManager.registerDelegate(delegate1);
        
        assertTrue("Should have delegate after registration", 
                  viewTypeManager.hasDelegate(VIEW_TYPE_1));
        assertFalse("Should not have unregistered delegate", 
                   viewTypeManager.hasDelegate(VIEW_TYPE_2));
    }
    
    @Test
    public void testGetSupportedViewTypes() {
        assertTrue("Should be empty initially", 
                  viewTypeManager.getSupportedViewTypes().isEmpty());
        
        viewTypeManager.registerDelegate(delegate1);
        viewTypeManager.registerDelegate(delegate2);
        
        Set<Integer> supportedTypes = viewTypeManager.getSupportedViewTypes();
        assertEquals("Should have 2 supported types", 2, supportedTypes.size());
        assertTrue("Should contain VIEW_TYPE_1", supportedTypes.contains(VIEW_TYPE_1));
        assertTrue("Should contain VIEW_TYPE_2", supportedTypes.contains(VIEW_TYPE_2));
    }
    
    @Test
    public void testGetDelegateCount() {
        assertEquals("Should be 0 initially", 0, viewTypeManager.getDelegateCount());
        
        viewTypeManager.registerDelegate(delegate1);
        assertEquals("Should be 1 after first registration", 1, viewTypeManager.getDelegateCount());
        
        viewTypeManager.registerDelegate(delegate2);
        assertEquals("Should be 2 after second registration", 2, viewTypeManager.getDelegateCount());
    }
    
    @Test
    public void testSetDefaultDelegate() {
        assertNull("Default delegate should be null initially", 
                  viewTypeManager.getDefaultDelegate());
        
        TestViewTypeDelegate defaultDelegate = new TestViewTypeDelegate(-1);
        viewTypeManager.setDefaultDelegate(defaultDelegate);
        
        assertEquals("Should return set default delegate", 
                    defaultDelegate, viewTypeManager.getDefaultDelegate());
    }
    
    @Test
    public void testSetDefaultDelegate_Null() {
        TestViewTypeDelegate defaultDelegate = new TestViewTypeDelegate(-1);
        viewTypeManager.setDefaultDelegate(defaultDelegate);
        viewTypeManager.setDefaultDelegate(null);
        
        assertNull("Default delegate should be null after clearing", 
                  viewTypeManager.getDefaultDelegate());
    }
    
    @Test
    public void testUnregisterDelegate_Success() {
        viewTypeManager.registerDelegate(delegate1);
        assertTrue("Should have delegate before unregistration", 
                  viewTypeManager.hasDelegate(VIEW_TYPE_1));
        
        ViewTypeDelegate removed = viewTypeManager.unregisterDelegate(VIEW_TYPE_1);
        
        assertEquals("Should return removed delegate", delegate1, removed);
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
        viewTypeManager.registerDelegate(delegate1);
        viewTypeManager.registerDelegate(delegate2);
        assertEquals("Should have 2 delegates", 2, viewTypeManager.getDelegateCount());
        
        viewTypeManager.clearAllDelegates();
        
        assertEquals("Should have 0 delegates after clearing", 0, viewTypeManager.getDelegateCount());
        assertTrue("Supported types should be empty", 
                  viewTypeManager.getSupportedViewTypes().isEmpty());
    }
    
    @Test
    public void testValidateDelegates_AllValid() {
        viewTypeManager.registerDelegate(delegate1);
        viewTypeManager.registerDelegate(delegate2);
        
        boolean result = viewTypeManager.validateDelegates();
        
        assertTrue("All delegates should be valid", result);
    }
    
    @Test
    public void testGetDebugInfo() {
        viewTypeManager.registerDelegate(delegate1);
        TestViewTypeDelegate defaultDelegate = new TestViewTypeDelegate(-1);
        viewTypeManager.setDefaultDelegate(defaultDelegate);
        
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
     * 测试用的简单 ViewTypeDelegate 实现
     */
    private static class TestViewTypeDelegate implements ViewTypeDelegate {
        private final int viewType;
        
        public TestViewTypeDelegate(int viewType) {
            this.viewType = viewType;
        }
        
        @Override
        public int getViewType() {
            return viewType;
        }
        
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, LayoutInflater inflater) {
            return new TestViewHolder(new View(parent.getContext()));
        }
        
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, ChatMessage message, 
                                    int position, ChatAdapterContext context) {
            // 简单的测试实现
        }
    }
    
    /**
     * 测试用的简单 ViewHolder
     */
    private static class TestViewHolder extends RecyclerView.ViewHolder {
        public TestViewHolder(View itemView) {
            super(itemView);
        }
    }
}