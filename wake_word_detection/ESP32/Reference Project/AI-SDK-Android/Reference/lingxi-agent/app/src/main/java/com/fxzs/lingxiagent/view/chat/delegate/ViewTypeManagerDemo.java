package com.fxzs.lingxiagent.view.chat.delegate;

import timber.log.Timber;

/**
 * ViewTypeManager 功能演示类
 * 用于验证 ViewTypeManager 的核心功能
 */
public class ViewTypeManagerDemo {
    
    private static final String TAG = "ViewTypeManagerDemo";
    
    /**
     * 演示 ViewTypeManager 的基本功能
     */
    public static void demonstrateBasicFunctionality() {
        Timber.tag(TAG).d( "开始演示 ViewTypeManager 功能");
        
        ViewTypeManager manager = new ViewTypeManager();
        
        // 1. 测试初始状态
        Timber.tag(TAG).d( "初始委托数量: " + manager.getDelegateCount());
        Timber.tag(TAG).d( "支持的视图类型: " + manager.getSupportedViewTypes());
        
        // 2. 测试错误处理
        try {
            manager.registerDelegate(null);
        } catch (IllegalArgumentException e) {
            Timber.tag(TAG).d( "正确捕获了 null 委托异常: " + e.getMessage());
        }
        
        try {
            manager.getDelegate(999);
        } catch (IllegalArgumentException e) {
            Timber.tag(TAG).d( "正确捕获了未注册委托异常: " + e.getMessage());
        }
        
        // 3. 测试安全获取
        ViewTypeDelegate safeResult = manager.getDelegateSafely(999);
        Timber.tag(TAG).d( "安全获取未注册委托结果: " + safeResult);
        
        // 4. 测试调试信息
        String debugInfo = manager.getDebugInfo();
        Timber.tag(TAG).d( "调试信息:\n" + debugInfo);
        
        // 5. 测试验证功能
        boolean isValid = manager.validateDelegates();
        Timber.tag(TAG).d( "委托验证结果: " + isValid);
        
        Timber.tag(TAG).d( "ViewTypeManager 功能演示完成");
    }
    
    /**
     * 演示错误处理机制
     */
    public static void demonstrateErrorHandling() {
        Timber.tag(TAG).d( "开始演示错误处理机制");
        
        ViewTypeManager manager = new ViewTypeManager();
        
        // 测试各种错误情况
        String[] testCases = {
            "注册 null 委托",
            "获取未注册的委托",
            "重复注册相同类型"
        };
        
        for (String testCase : testCases) {
            Timber.tag(TAG).d( "测试用例: " + testCase);
            
            try {
                switch (testCase) {
                    case "注册 null 委托":
                        manager.registerDelegate(null);
                        break;
                    case "获取未注册的委托":
                        manager.getDelegate(999);
                        break;
                    case "重复注册相同类型":
                        // 这个测试需要实际的委托实例，暂时跳过
                        Timber.tag(TAG).d( "跳过重复注册测试（需要实际委托实例）");
                        break;
                }
            } catch (Exception e) {
                Timber.tag(TAG).d( "捕获异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
        
        Timber.tag(TAG).d( "错误处理机制演示完成");
    }
    
    /**
     * 演示管理功能
     */
    public static void demonstrateManagementFeatures() {
        Timber.tag(TAG).d( "开始演示管理功能");
        
        ViewTypeManager manager = new ViewTypeManager();
        
        // 测试清空功能
        manager.clearAllDelegates();
        Timber.tag(TAG).d( "清空后委托数量: " + manager.getDelegateCount());
        
        // 测试注销功能
        ViewTypeDelegate removed = manager.unregisterDelegate(999);
        Timber.tag(TAG).d( "注销不存在的委托结果: " + removed);
        
        // 测试默认委托设置
        manager.setDefaultDelegate(null);
        Timber.tag(TAG).d( "设置默认委托为 null");
        
        ViewTypeDelegate defaultDelegate = manager.getDefaultDelegate();
        Timber.tag(TAG).d( "获取默认委托: " + defaultDelegate);
        
        Timber.tag(TAG).d( "管理功能演示完成");
    }
}