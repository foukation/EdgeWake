package com.fxzs.lingxiagent.view.chat.delegate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

/**
 * ViewType 委托管理器
 * 负责管理所有视图类型委托的注册、获取和验证
 * 
 * 主要功能：
 * - 委托注册和重复注册验证
 * - 委托获取和错误处理
 * - 默认委托支持
 * - 支持的视图类型查询
 */
public class ViewTypeManager {
    
    private static final String TAG = "ViewTypeManager";
    
    /**
     * 存储视图类型到委托的映射
     * Key: 视图类型常量 (如 ChatAdapter.TYPE_USER)
     * Value: 对应的委托实例
     */
    private final Map<Integer, ViewTypeDelegate> delegates = new HashMap<>();
    
    /**
     * 默认委托，用于处理未注册的视图类型
     */
    private ViewTypeDelegate defaultDelegate;
    
    /**
     * 注册视图类型委托
     * 
     * @param delegate 要注册的委托实例
     * @throws IllegalArgumentException 如果委托为 null 或视图类型已被注册
     */
    public void registerDelegate(ViewTypeDelegate delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate cannot be null");
        }
        
        int viewType = delegate.getViewType();
        
        // 检查是否已经注册了相同的视图类型
        if (delegates.containsKey(viewType)) {
            ViewTypeDelegate existingDelegate = delegates.get(viewType);
            String existingClass = existingDelegate != null ? existingDelegate.getClass().getSimpleName() : "null";
            String newClass = delegate.getClass().getSimpleName();
            
            throw new IllegalArgumentException(
                String.format("Delegate for viewType %d already registered. " +
                    "Existing: %s, New: %s", viewType, existingClass, newClass)
            );
        }
        
        delegates.put(viewType, delegate);
        Timber.tag(TAG).d( String.format("Registered delegate %s for viewType %d", 
            delegate.getClass().getSimpleName(), viewType));
    }
    
    /**
     * 获取指定视图类型的委托
     * 
     * @param viewType 视图类型常量
     * @return 对应的委托实例
     * @throws IllegalArgumentException 如果没有注册对应的委托且没有默认委托
     */
    public ViewTypeDelegate getDelegate(int viewType) {
        ViewTypeDelegate delegate = delegates.get(viewType);
        
        if (delegate != null) {
            return delegate;
        }
        
        // 如果没有找到对应的委托，尝试使用默认委托
        if (defaultDelegate != null) {
            Timber.tag(TAG).w( String.format("No delegate found for viewType %d, using default delegate", viewType));
            return defaultDelegate;
        }
        
        // 既没有对应委托也没有默认委托，抛出异常
        Timber.tag(TAG).e( String.format("No delegate registered for viewType: %d", viewType));
        throw new IllegalArgumentException(
            String.format("No delegate registered for viewType: %d. " +
                "Available viewTypes: %s", viewType, delegates.keySet())
        );
    }
    
    /**
     * 安全地获取委托，不会抛出异常
     * 
     * @param viewType 视图类型常量
     * @return 对应的委托实例，如果未找到则返回 null
     */
    public ViewTypeDelegate getDelegateSafely(int viewType) {
        try {
            return getDelegate(viewType);
        } catch (IllegalArgumentException e) {
            Timber.tag(TAG).e( "Failed to get delegate for viewType: " + viewType, e);
            return null;
        }
    }
    
    /**
     * 检查指定视图类型是否已注册委托
     * 
     * @param viewType 视图类型常量
     * @return true 如果已注册委托
     */
    public boolean hasDelegate(int viewType) {
        return delegates.containsKey(viewType);
    }
    
    /**
     * 获取所有已注册的视图类型
     * 
     * @return 包含所有已注册视图类型的集合
     */
    public Set<Integer> getSupportedViewTypes() {
        return delegates.keySet();
    }
    
    /**
     * 获取已注册委托的数量
     * 
     * @return 委托数量
     */
    public int getDelegateCount() {
        return delegates.size();
    }
    
    /**
     * 设置默认委托
     * 当请求的视图类型没有对应委托时，将使用默认委托
     * 
     * @param defaultDelegate 默认委托实例，可以为 null
     */
    public void setDefaultDelegate(ViewTypeDelegate defaultDelegate) {
        this.defaultDelegate = defaultDelegate;
        if (defaultDelegate != null) {
            Timber.tag(TAG).d( String.format("Set default delegate: %s",
                defaultDelegate.getClass().getSimpleName()));
        } else {
            Timber.tag(TAG).d( "Cleared default delegate");
        }
    }
    
    /**
     * 获取当前的默认委托
     * 
     * @return 默认委托实例，可能为 null
     */
    public ViewTypeDelegate getDefaultDelegate() {
        return defaultDelegate;
    }
    
    /**
     * 注销指定视图类型的委托
     * 
     * @param viewType 要注销的视图类型
     * @return 被注销的委托实例，如果不存在则返回 null
     */
    public ViewTypeDelegate unregisterDelegate(int viewType) {
        ViewTypeDelegate removed = delegates.remove(viewType);
        if (removed != null) {
            Timber.tag(TAG).d( String.format("Unregistered delegate %s for viewType %d", 
                removed.getClass().getSimpleName(), viewType));
        } else {
            Timber.tag(TAG).w( String.format("Attempted to unregister non-existent delegate for viewType %d", viewType));
        }
        return removed;
    }
    
    /**
     * 清除所有已注册的委托
     */
    public void clearAllDelegates() {
        int count = delegates.size();
        delegates.clear();
        Timber.tag(TAG).d( String.format("Cleared %d delegates", count));
    }
    
    /**
     * 验证所有已注册委托的完整性
     * 检查委托的视图类型是否与注册时的键值一致
     * 
     * @return true 如果所有委托都有效
     */
    public boolean validateDelegates() {
        boolean allValid = true;
        
        for (Map.Entry<Integer, ViewTypeDelegate> entry : delegates.entrySet()) {
            Integer registeredViewType = entry.getKey();
            ViewTypeDelegate delegate = entry.getValue();
            
            if (delegate == null) {
                Timber.tag(TAG).e( String.format("Null delegate found for viewType %d", registeredViewType));
                allValid = false;
                continue;
            }
            
            int delegateViewType = delegate.getViewType();
            if (!registeredViewType.equals(delegateViewType)) {
                Timber.tag(TAG).e( String.format("ViewType mismatch for delegate %s: registered=%d, actual=%d", 
                    delegate.getClass().getSimpleName(), registeredViewType, delegateViewType));
                allValid = false;
            }
        }
        
        return allValid;
    }
    
    /**
     * 获取调试信息字符串
     * 
     * @return 包含所有已注册委托信息的字符串
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("ViewTypeManager Debug Info:\n");
        sb.append(String.format("Total delegates: %d\n", delegates.size()));
        sb.append(String.format("Default delegate: %s\n", 
            defaultDelegate != null ? defaultDelegate.getClass().getSimpleName() : "null"));
        sb.append("Registered delegates:\n");
        
        for (Map.Entry<Integer, ViewTypeDelegate> entry : delegates.entrySet()) {
            ViewTypeDelegate delegate = entry.getValue();
            sb.append(String.format("  ViewType %d -> %s\n", 
                entry.getKey(), 
                delegate != null ? delegate.getClass().getSimpleName() : "null"));
        }
        
        return sb.toString();
    }
    
    /**
     * 处理 ViewHolder 回收事件
     * 调用对应委托的回收方法进行资源清理
     * 
     * @param viewType 视图类型
     * @param holder 要回收的 ViewHolder
     * @param context ChatAdapter 上下文
     */
    public void onViewRecycled(int viewType, androidx.recyclerview.widget.RecyclerView.ViewHolder holder, 
                              ChatAdapterContext context) {
        ViewTypeDelegate delegate = getDelegateSafely(viewType);
        if (delegate instanceof BaseViewTypeDelegate) {
            ((BaseViewTypeDelegate) delegate).onViewRecycled(holder, context);
        } else if (delegate != null) {
            Timber.tag(TAG).w(String.format("Delegate %s for viewType %d does not support onViewRecycled",
                delegate.getClass().getSimpleName(), viewType));
        }
    }
}