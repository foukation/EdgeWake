package com.fxzs.lingxiagent.view.chat.delegate;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * ViewTypeManager 基础单元测试
 * 测试核心功能，不依赖 Android 组件
 */
public class ViewTypeManagerBasicTest {
    
    private ViewTypeManager viewTypeManager;
    
    private static final int VIEW_TYPE_1 = 1;
    private static final int VIEW_TYPE_2 = 2;
    private static final int UNREGISTERED_VIEW_TYPE = 999;
    
    @Before
    public void setUp() {
        viewTypeManager = new ViewTypeManager();
    }
    
    @Test
    public void testInitialState() {
        assertEquals("Initial delegate count should be 0", 0, viewTypeManager.getDelegateCount());
        assertTrue("Initial supported types should be empty", 
                  viewTypeManager.getSupportedViewTypes().isEmpty());
        assertNull("Initial default delegate should be null", 
                  viewTypeManager.getDefaultDelegate());
        assertFalse("Should not have any delegates initially", 
                   viewTypeManager.hasDelegate(VIEW_TYPE_1));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterDelegate_NullDelegate() {
        viewTypeManager.registerDelegate(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetDelegate_NotRegistered() {
        viewTypeManager.getDelegate(UNREGISTERED_VIEW_TYPE);
    }
    
    @Test
    public void testGetDelegateSafely_NotRegistered() {
        ViewTypeDelegate result = viewTypeManager.getDelegateSafely(UNREGISTERED_VIEW_TYPE);
        assertNull("Should return null for unregistered type", result);
    }
    
    @Test
    public void testSetDefaultDelegate() {
        assertNull("Default delegate should be null initially", 
                  viewTypeManager.getDefaultDelegate());
        
        // We can't create a real delegate without Android dependencies,
        // but we can test the null case
        viewTypeManager.setDefaultDelegate(null);
        assertNull("Default delegate should remain null", 
                  viewTypeManager.getDefaultDelegate());
    }
    
    @Test
    public void testUnregisterDelegate_NotRegistered() {
        ViewTypeDelegate removed = viewTypeManager.unregisterDelegate(UNREGISTERED_VIEW_TYPE);
        assertNull("Should return null for unregistered type", removed);
    }
    
    @Test
    public void testClearAllDelegates_Empty() {
        viewTypeManager.clearAllDelegates();
        assertEquals("Should have 0 delegates after clearing empty manager", 
                    0, viewTypeManager.getDelegateCount());
    }
    
    @Test
    public void testValidateDelegates_Empty() {
        boolean result = viewTypeManager.validateDelegates();
        assertTrue("Empty delegate manager should be valid", result);
    }
    
    @Test
    public void testGetDebugInfo_Empty() {
        String debugInfo = viewTypeManager.getDebugInfo();
        
        assertNotNull("Debug info should not be null", debugInfo);
        assertTrue("Should show 0 delegates", debugInfo.contains("Total delegates: 0"));
        assertTrue("Should show null default delegate", debugInfo.contains("Default delegate: null"));
        assertTrue("Should contain debug header", debugInfo.contains("ViewTypeManager Debug Info"));
    }
    
    @Test
    public void testGetSupportedViewTypes_Empty() {
        Set<Integer> supportedTypes = viewTypeManager.getSupportedViewTypes();
        assertNotNull("Supported types should not be null", supportedTypes);
        assertTrue("Should be empty initially", supportedTypes.isEmpty());
    }
    
    @Test
    public void testHasDelegate_False() {
        assertFalse("Should not have delegate for any type initially", 
                   viewTypeManager.hasDelegate(VIEW_TYPE_1));
        assertFalse("Should not have delegate for any type initially", 
                   viewTypeManager.hasDelegate(VIEW_TYPE_2));
        assertFalse("Should not have delegate for any type initially", 
                   viewTypeManager.hasDelegate(UNREGISTERED_VIEW_TYPE));
    }
}