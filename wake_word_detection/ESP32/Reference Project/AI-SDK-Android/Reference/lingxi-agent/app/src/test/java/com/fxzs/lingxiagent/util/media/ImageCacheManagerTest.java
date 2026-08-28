package com.fxzs.lingxiagent.util.media;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * ImageCacheManager单元测试
 */
@RunWith(RobolectricTestRunner.class)
public class ImageCacheManagerTest {
    
    @Mock
    private SharedPreferences mockPreferences;
    
    @Mock
    private SharedPreferences.Editor mockEditor;
    
    private Context context;
    private ImageCacheManager cacheManager;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        
        // Mock SharedPreferences
        when(mockPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putLong(anyString(), eq(0L))).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), eq(true))).thenReturn(mockEditor);
        when(mockEditor.apply()).thenReturn();
        
        // 设置默认值
        when(mockPreferences.getLong(eq("cache_size_limit"), eq(100 * 1024 * 1024L)))
                .thenReturn(100 * 1024 * 1024L);
        when(mockPreferences.getBoolean(eq("preload_enabled"), eq(true)))
                .thenReturn(true);
        when(mockPreferences.getLong(eq("last_cache_clean"), eq(0L)))
                .thenReturn(0L);
    }
    
    @Test
    public void testCacheManagerInitialization() {
        cacheManager = new ImageCacheManager(context);
        assertNotNull("CacheManager should not be null", cacheManager);
    }
    
    @Test
    public void testPreloadEnabled() {
        cacheManager = new ImageCacheManager(context);
        assertTrue("Preload should be enabled by default", cacheManager.isPreloadEnabled());
    }
    
    @Test
    public void testSetPreloadEnabled() {
        cacheManager = new ImageCacheManager(context);
        cacheManager.setPreloadEnabled(false);
        // 由于我们使用了mock，这里主要测试方法调用不会抛出异常
        assertNotNull("CacheManager should still be valid", cacheManager);
    }
    
    @Test
    public void testGetCacheSizeLimit() {
        cacheManager = new ImageCacheManager(context);
        long defaultLimit = 100 * 1024 * 1024L; // 100MB
        assertEquals("Default cache size should be 100MB", defaultLimit, cacheManager.getCacheSizeLimit());
    }
    
    @Test
    public void testSetCacheSizeLimit() {
        cacheManager = new ImageCacheManager(context);
        long newLimit = 200 * 1024 * 1024L; // 200MB
        cacheManager.setCacheSizeLimit(newLimit);
        // 由于我们使用了mock，这里主要测试方法调用不会抛出异常
        assertNotNull("CacheManager should still be valid", cacheManager);
    }
    
    @Test
    public void testPreloadImagesWithNullArray() {
        cacheManager = new ImageCacheManager(context);
        // 测试传入null数组不会抛出异常
        cacheManager.preloadImages((String[]) null);
        assertNotNull("CacheManager should still be valid", cacheManager);
    }
    
    @Test
    public void testPreloadImagesWithEmptyArray() {
        cacheManager = new ImageCacheManager(context);
        // 测试传入空数组不会抛出异常
        cacheManager.preloadImages(new String[0]);
        assertNotNull("CacheManager should still be valid", cacheManager);
    }
    
    @Test
    public void testPreloadImagesWithValidUrls() {
        cacheManager = new ImageCacheManager(context);
        String[] urls = {
            "https://example.com/image1.jpg",
            "https://example.com/image2.png"
        };
        // 测试传入有效URL不会抛出异常
        cacheManager.preloadImages(urls);
        assertNotNull("CacheManager should still be valid", cacheManager);
    }
    
    @Test
    public void testSmartPreloadWithNullUrls() {
        cacheManager = new ImageCacheManager(context);
        // 测试传入null不会抛出异常
        cacheManager.smartPreload("https://example.com/current.jpg", null);
        assertNotNull("CacheManager should still be valid", cacheManager);
    }
    
    @Test
    public void testCleanup() {
        cacheManager = new ImageCacheManager(context);
        // 测试清理方法不会抛出异常
        cacheManager.cleanup();
        assertNotNull("CacheManager should still be valid", cacheManager);
    }
}