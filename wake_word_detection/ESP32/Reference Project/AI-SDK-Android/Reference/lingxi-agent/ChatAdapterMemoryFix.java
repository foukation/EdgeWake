// 内存泄漏修复建议

public class ChatAdapterMemoryFix {
    
    // 1. 使用WeakHashMap替代HashMap
    private final WeakHashMap<ChatViewHolder, MarkwonAdapter> viewHolderAdapterCache = new WeakHashMap<>();
    private final WeakHashMap<ChatViewHolder, String> lastRenderedContent = new WeakHashMap<>();
    private final WeakHashMap<ChatViewHolder, Spanned> lastRenderedSpanned = new WeakHashMap<>();
    
    // 2. 改进ViewHolder回收机制
    @Override
    public void onViewRecycled(@NonNull ChatViewHolder holder) {
        super.onViewRecycled(holder);
        
        // 取消待处理的更新任务
        if (holder.pendingUpdateRunnable != null && holder.markdownContainer != null) {
            holder.markdownContainer.removeCallbacks(holder.pendingUpdateRunnable);
            holder.pendingUpdateRunnable = null;
        }
        
        // 清理WebView
        if (holder.recyclerViewAi != null) {
            holder.recyclerViewAi.setAdapter(null);
        }
        
        // 清理markdown容器
        if (holder.markdownContainer != null) {
            holder.markdownContainer.removeAllViews();
            holder.markdownContainer.setTag(null);
        }
        
        // 主动清理缓存（虽然使用WeakHashMap，但主动清理更及时）
        viewHolderAdapterCache.remove(holder);
        lastRenderedContent.remove(holder);
        lastRenderedSpanned.remove(holder);
    }
    
    // 3. 改进异步更新机制，使用WeakReference
    private void updateStreamingContentSafe(int position, String newContent, boolean immediate) {
        if (position < 0 || position >= chatMessages.size()) return;
        
        ChatMessage message = chatMessages.get(position);
        message.setMessage(newContent);
        
        // 使用WeakReference避免内存泄漏
        WeakReference<ChatAdapter> adapterRef = new WeakReference<>(this);
        
        Runnable updateTask = () -> {
            ChatAdapter adapter = adapterRef.get();
            if (adapter == null) return; // Adapter已被回收
            
            // 执行更新逻辑
            adapter.notifyItemChanged(position, immediate ? "streaming_update_immediate" : "streaming_update");
        };
        
        if (immediate) {
            updateTask.run();
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(updateTask, 180);
        }
    }
    
    // 4. 改进cleanup方法
    public void cleanup() {
        Timber.tag(TAG).d( "cleanup: Starting adapter cleanup");
        
        // 取消所有markdown渲染
        cancelAllMarkdownRendering();
        
        // 清理渲染器
        for (MarkdownRenderer renderer : activeRenderers) {
            if (renderer != null) {
                renderer.destroy();
            }
        }
        activeRenderers.clear();
        
        if (markdownRenderer != null) {
            markdownRenderer.destroy();
            markdownRenderer = null;
        }
        
        if (chatMarkdownRenderer != null) {
            // 如果ChatMarkdownRenderer有cleanup方法，调用它
            chatMarkdownRenderer = null;
        }
        
        // 清理所有缓存
        viewHolderAdapterCache.clear();
        lastRenderedContent.clear();
        lastRenderedSpanned.clear();
        lastItemCounts.clear();
        
        // 清理共享池
        if (sharedPool != null) {
            sharedPool.clear();
        }
        
        Timber.tag(TAG).d( "cleanup: Adapter cleanup completed");
    }
    
    // 5. 在Activity/Fragment的onDestroy中调用cleanup
    // 在使用ChatAdapter的Activity或Fragment中：
    /*
    @Override
    protected void onDestroy() {
        if (chatAdapter != null) {
            chatAdapter.cleanup();
        }
        super.onDestroy();
    }
    */
}