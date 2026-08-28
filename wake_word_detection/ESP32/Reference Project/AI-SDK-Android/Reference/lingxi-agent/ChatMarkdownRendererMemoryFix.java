// ChatMarkdownRenderer内存泄漏修复建议

public class ChatMarkdownRendererMemoryFix {
    
    // 1. 使用WeakHashMap避免容器泄漏
    private final java.util.WeakHashMap<LinearLayout, String> containerLastMarkdown = new java.util.WeakHashMap<>();
    
    // 2. 改进异步任务管理
    private final java.util.concurrent.ConcurrentHashMap<LinearLayout, java.util.concurrent.Future<?>> pendingTasks = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<LinearLayout, Long> taskTokens = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<LinearLayout, Runnable> pendingMounts = new java.util.concurrent.ConcurrentHashMap<>();
    
    // 3. 添加清理方法
    public void cleanup() {
        // 取消所有待处理的任务
        for (java.util.concurrent.Future<?> future : pendingTasks.values()) {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }
        pendingTasks.clear();
        
        // 清理所有待处理的挂载任务
        for (Runnable runnable : pendingMounts.values()) {
            if (runnable != null) {
                mainHandler.removeCallbacks(runnable);
            }
        }
        pendingMounts.clear();
        
        // 清理token
        taskTokens.clear();
        
        // 清理容器缓存
        containerLastMarkdown.clear();
        
        // 关闭线程池（如果是全局的，考虑引用计数）
        // EXECUTOR.shutdown(); // 注意：如果是全局共享的，不要关闭
    }
    
    // 4. 改进renderInto方法，添加容器有效性检查
    public void renderInto(LinearLayout container, String markdown) {
        // 检查容器是否还有效
        if (container == null || container.getContext() == null) {
            return;
        }
        
        // 检查Activity是否还活着
        Context context = container.getContext();
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }
        
        // 原有逻辑...
        String lastMarkdown = containerLastMarkdown.get(container);
        if (markdown != null && markdown.equals(lastMarkdown) && container.getChildCount() > 0) {
            return;
        }
        
        // 使用WeakReference避免内存泄漏
        WeakReference<LinearLayout> containerRef = new WeakReference<>(container);
        
        // 继续原有的异步处理逻辑，但使用WeakReference
        java.util.concurrent.Future<?> future = EXECUTOR.submit(() -> {
            LinearLayout safeContainer = containerRef.get();
            if (safeContainer == null) return; // 容器已被回收
            
            // 原有的解析逻辑...
        });
        
        pendingTasks.put(container, future);
    }
    
    // 5. 改进增量渲染，添加安全检查
    public void renderIncremental(LinearLayout container, String oldContent, String newContent) {
        if (container == null || container.getContext() == null) {
            return;
        }
        
        // 检查Activity状态
        Context context = container.getContext();
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }
        
        // 原有逻辑...
    }
}