package com.fxzs.lingxiagent.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import static org.junit.Assert.*;

/**
 * 性能优化工具类的单元测试
 */
@RunWith(RobolectricTestRunner.class)
public class PerformanceOptimizerTest {

    @Test
    public void testDataCacheManager() {
        DataCacheManager cacheManager = DataCacheManager.getInstance(RuntimeEnvironment.getApplication());
        
        // 测试字符串缓存
        String testKey = "test_key";
        String testValue = "test_value";
        
        cacheManager.cacheString(testKey, testValue);
        String cachedValue = cacheManager.getCachedString(testKey);
        
        assertEquals("缓存的字符串应该与原始值相同", testValue, cachedValue);
    }
    
    @Test
    public void testMemoryManager() {
        MemoryManager memoryManager = MemoryManager.getInstance();
        
        // 测试内存信息获取
        MemoryManager.MemoryInfo memoryInfo = memoryManager.getMemoryInfo(RuntimeEnvironment.getApplication());
        
        assertNotNull("内存信息不应该为空", memoryInfo);
        assertTrue("已使用内存应该大于0", memoryInfo.getUsedMemory() > 0);
        assertTrue("最大内存应该大于已使用内存", memoryInfo.getMaxMemory() > memoryInfo.getUsedMemory());
    }
    
    @Test
    public void testNetworkOptimizer() {
        NetworkOptimizer networkOptimizer = NetworkOptimizer.getInstance(RuntimeEnvironment.getApplication());
        
        // 测试请求去重
        String requestKey = "test_request";
        
        // 第一次请求不应该被合并
        assertFalse("第一次请求不应该被合并", networkOptimizer.shouldMergeRequest(requestKey));
        
        // 立即再次请求应该被合并
        assertTrue("立即再次请求应该被合并", networkOptimizer.shouldMergeRequest(requestKey));
    }
    
    @Test
    public void testPerformanceMonitor() {
        PerformanceMonitor performanceMonitor = PerformanceMonitor.getInstance(RuntimeEnvironment.getApplication());
        
        // 测试操作计时
        String operationName = "test_operation";
        
        performanceMonitor.startOperation(operationName);
        
        // 模拟一些操作
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        long duration = performanceMonitor.endOperation(operationName);
        
        assertTrue("操作持续时间应该大于0", duration > 0);
    }
}