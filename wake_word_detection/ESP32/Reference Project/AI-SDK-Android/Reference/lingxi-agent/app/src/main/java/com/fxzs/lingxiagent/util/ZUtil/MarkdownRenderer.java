package com.fxzs.lingxiagent.util.ZUtil;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.LruCache;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.noties.markwon.Markwon;
import timber.log.Timber;

public class MarkdownRenderer {
    private static final String TAG = "MarkdownRenderer";
    private static final int PARAGRAPH_THRESHOLD = 1000; // 超过1000字分批解析
    private static final int CACHE_SIZE = 20; // 减少缓存大小，避免内存占用过多
    private static final int MAX_CACHE_MEMORY = 5 * 1024 * 1024; // 最大缓存5MB
    private static final int PARSE_DELAY_MS = 50; // 每段解析后暂停50ms
    
    // 使用弱引用避免内存泄漏
    private WeakReference<Context> contextRef;
    private final Markwon markwon;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final LruCache<String, Spanned> markdownCache;
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    
    public interface RenderCallback {
        void onComplete();
        void onProgress(int progress);
        default void onError(Exception e) {}
    }
    
    public MarkdownRenderer(Context context) {
        Timber.tag(TAG).d( "MarkdownRenderer: Constructor called");
        
        // 监控初始化时的内存
        MemoryMonitor.logMemoryUsage(context, "MarkdownRenderer Constructor");
        
        this.contextRef = new WeakReference<>(context);
        this.markwon = MarkdownUtils.createMarkwon(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.markdownCache = new LruCache<String, Spanned>(CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Spanned value) {
                // 估算Spanned的内存占用
                return value.length() * 4; // 假设每个字符平均占用4字节
            }
        };
    }
    
    /**
     * 渲染大型Markdown文档，支持分批解析
     */
    public void renderLargeMarkdown(String markdown, TextView targetView, RenderCallback callback) {
        Timber.tag(TAG).d( "renderLargeMarkdown called with text length: " + (markdown != null ? markdown.length() : 0));
        
        if (markdown == null || markdown.isEmpty()) {
            Timber.tag(TAG).d( "renderLargeMarkdown: markdown is null or empty");
            if (callback != null) callback.onComplete();
            return;
        }
        
        // 重置取消状态
        isCancelled.set(false);
        
        if (markdown.length() <= PARAGRAPH_THRESHOLD) {
            // 文本较短，直接渲染
            Timber.tag(TAG).d( "renderLargeMarkdown: Short text, using cache render");
            renderWithCache(markdown, targetView);
            if (callback != null) callback.onComplete();
            return;
        }
        
        // 分批解析
        Timber.tag(TAG).d( "renderLargeMarkdown: Starting batch processing");
        executor.execute(() -> {
            try {
                Timber.tag(TAG).d( "renderLargeMarkdown: Executor thread started");
                
                // 监控渲染前的内存
                Context context = contextRef.get();
                if (context != null) {
                    MemoryMonitor.logMemoryUsage(context, "Before Markdown Render");
                }
                
                // 检查并清理HTML内容
                String cleanMarkdown = markdown;
                if (HtmlSanitizer.containsHtml(markdown)) {
                    Timber.tag(TAG).d( "renderLargeMarkdown: HTML content detected, sanitizing");
                    cleanMarkdown = HtmlSanitizer.sanitize(markdown);
                }
                
                // 检查是否已被取消
                if (isCancelled.get()) {
                    Timber.tag(TAG).d( "renderLargeMarkdown: Cancelled before rendering");
                    return;
                }
                
                // 直接使用markwon的渲染，避免复杂的分段处理导致的问题
                Timber.tag(TAG).d( "renderLargeMarkdown: Starting Markwon conversion");
                Spanned finalContent = markwon.toMarkdown(cleanMarkdown);
                Timber.tag(TAG).d( "renderLargeMarkdown: Markwon conversion completed");
                
                // 再次检查是否已被取消
                if (isCancelled.get()) {
                    Timber.tag(TAG).d( "renderLargeMarkdown: Cancelled after rendering");
                    return;
                }
                
                // 监控渲染后的内存
                if (context != null) {
                    MemoryMonitor.logMemoryUsage(context, "After Markdown Render");
                }
                
                // 在主线程中更新UI
                mainHandler.post(() -> {
                    Timber.tag(TAG).d( "renderLargeMarkdown: Main thread UI update started");
                    
                    if (isCancelled.get()) {
                        Timber.tag(TAG).d( "renderLargeMarkdown: Cancelled, skipping UI update");
                        return;
                    }
                    
                    try {
                        targetView.setText(finalContent);
                        targetView.setMovementMethod(LinkMovementMethod.getInstance());
                        Timber.tag(TAG).d( "renderLargeMarkdown: UI update completed successfully");
                    } catch (Exception e) {
                        Timber.tag(TAG).e( "renderLargeMarkdown: Error updating UI"+ e);
                    }
                    
                    if (callback != null) {
                        callback.onComplete();
                    }
                });
                
            } catch (Exception e) {
                Timber.tag(TAG).e( "renderLargeMarkdown: Exception in executor thread"+ e);
                Timber.tag(TAG).e( "renderLargeMarkdown: Exception type: " + e.getClass().getName());
                Timber.tag(TAG).e( "renderLargeMarkdown: Exception message: " + e.getMessage());
                
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                    // 降级处理
                    try {
                        Timber.tag(TAG).d( "renderLargeMarkdown: Attempting HTML fallback");
                        Spanned htmlSpanned = HtmlSanitizer.fromHtml(markdown);
                        targetView.setText(htmlSpanned);
                        Timber.tag(TAG).d( "renderLargeMarkdown: HTML fallback successful");
                    } catch (Exception htmlError) {
                        Timber.tag(TAG).e( "renderLargeMarkdown: HTML fallback failed"+ htmlError);
                        targetView.setText(markdown);
                    }
                });
            }
        });
    }
    
    /**
     * 普通渲染方法，支持缓存
     */
    public void renderWithCache(String markdown, TextView targetView) {
        Timber.tag(TAG).d( "renderWithCache called with text length: " + (markdown != null ? markdown.length() : 0));
        
        if (markdown == null || markdown.isEmpty()) {
            Timber.tag(TAG).d( "renderWithCache: markdown is null or empty");
            targetView.setText("");
            return;
        }
        
        // 检查并清理HTML内容
        if (HtmlSanitizer.containsHtml(markdown)) {
            markdown = HtmlSanitizer.sanitize(markdown);
        }
        
        String cacheKey = generateCacheKey(markdown);
        Spanned cached = markdownCache.get(cacheKey);
        
        if (cached != null) {
            Timber.tag(TAG).d( "renderWithCache: Cache hit for key: " + cacheKey);
            targetView.setText(cached);
            targetView.setMovementMethod(LinkMovementMethod.getInstance());
            return;
        }
        
        Timber.tag(TAG).d( "renderWithCache: Cache miss, parsing markdown");
        
        try {
            // 解析并缓存
            Timber.tag(TAG).d( "renderWithCache: Starting Markwon conversion");
            Spanned spannable = markwon.toMarkdown(markdown);
            Timber.tag(TAG).d( "renderWithCache: Markwon conversion completed, caching result");
            
            markdownCache.put(cacheKey, spannable);
            Timber.tag(TAG).d( "renderWithCache: Cache size after put: " + markdownCache.size());
            
            targetView.setText(spannable);
            targetView.setMovementMethod(LinkMovementMethod.getInstance());
            Timber.tag(TAG).d( "renderWithCache: Successfully rendered and cached");
            
            // 如果缓存过大，主动清理
            if (markdownCache.size() > CACHE_SIZE * 0.8) {
                Timber.tag(TAG).w("renderWithCache: Cache is getting full, considering cleanup");
                Context context = contextRef.get();
                if (context != null) {
                    MemoryMonitor.forceGC("Cache nearly full");
                }
            }
        } catch (Exception e) {
            // 如果Markdown解析失败，使用安全的HTML解析
            Timber.tag(TAG).e( "renderWithCache: Markdown parsing failed"+ e);
            Timber.tag(TAG).e( "renderWithCache: Exception type: " + e.getClass().getName());
            Timber.tag(TAG).e( "renderWithCache: Exception message: " + e.getMessage());
            try {
                Spanned htmlSpanned = HtmlSanitizer.fromHtml(markdown);
                targetView.setText(htmlSpanned);
            } catch (Exception htmlError) {
                // 最后的降级：显示纯文本
                targetView.setText(markdown);
            }
        }
    }
    
    /**
     * 智能段落分割，识别标题、代码块等特殊元素
     */
    private String[] splitIntoSmartParagraphs(String markdown) {
        List<String> paragraphs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String[] lines = markdown.split("\n");
        boolean inCodeBlock = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // 检测代码块开始/结束
            if (line.startsWith("```")) {
                if (!inCodeBlock) {
                    // 代码块开始 - 先保存当前段落
                    if (current.length() > 0) {
                        paragraphs.add(current.toString().trim());
                        current.setLength(0);
                    }
                    inCodeBlock = true;
                    current.append(line).append("\n");
                } else {
                    // 代码块结束
                    current.append(line).append("\n");
                    paragraphs.add(current.toString().trim());
                    current.setLength(0);
                    inCodeBlock = false;
                }
                continue;
            }
            
            // 在代码块内，直接添加
            if (inCodeBlock) {
                current.append(line).append("\n");
                continue;
            }
            
            // 检测标题
            if (line.startsWith("#")) {
                if (current.length() > 0) {
                    paragraphs.add(current.toString().trim());
                    current.setLength(0);
                }
                current.append(line).append("\n");
                continue;
            }
            
            // 检测表格行
            if (line.contains("|") && (line.startsWith("|") || line.endsWith("|"))) {
                current.append(line).append("\n");
                continue;
            }
            
            // 检测空行 - 段落分隔符
            if (line.trim().isEmpty()) {
                if (current.length() > 0) {
                    paragraphs.add(current.toString().trim());
                    current.setLength(0);
                }
                continue;
            }
            
            // 普通文本行
            current.append(line).append("\n");
        }
        
        // 添加最后一个段落
        if (current.length() > 0) {
            paragraphs.add(current.toString().trim());
        }
        
        return paragraphs.toArray(new String[0]);
    }
    
    /**
     * 优先处理标题和列表等关键内容
     */
    private List<String> prioritizeParagraphs(String[] paragraphs) {
        List<String> prioritized = new ArrayList<>();
        List<String> normal = new ArrayList<>();
        
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.startsWith("#") || 
                trimmed.startsWith("- ") || 
                trimmed.startsWith("* ") ||
                trimmed.matches("^\\d+\\.\\s.*") ||
                trimmed.contains("|") ||
                trimmed.contains("```")) { // 包含表格、代码块
                prioritized.add(para); // 优先内容
            } else {
                normal.add(para);
            }
        }
        
        prioritized.addAll(normal); // 普通内容随后
        return prioritized;
    }
    
    /**
     * 处理单个段落，支持缓存
     */
    private Spanned processParagraph(String paragraph) {
        String cacheKey = generateCacheKey(paragraph);
        Spanned cached = markdownCache.get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        // 解析并缓存
        Spanned spannable = markwon.toMarkdown(paragraph);
        markdownCache.put(cacheKey, spannable);
        return spannable;
    }
    
    /**
     * 生成缓存键
     */
    private String generateCacheKey(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(content.hashCode());
        }
    }
    
    /**
     * 取消当前渲染
     */
    public void cancel() {
        isCancelled.set(true);
    }
    
    /**
     * 清理资源
     */
    public void destroy() {
        Timber.tag(TAG).d( "destroy: Starting resource cleanup");
        
        cancel();
        if (executor != null && !executor.isShutdown()) {
            Timber.tag(TAG).d( "destroy: Shutting down executor");
            executor.shutdown();
            try {
                // 等待任务完成，最多等待5秒
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    Timber.tag(TAG).w( "destroy: Executor didn't terminate in time, forcing shutdown");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Timber.tag(TAG).e( "destroy: Interrupted while waiting for executor termination"+ e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (markdownCache != null) {
            Timber.tag(TAG).d( "destroy: Clearing markdown cache");
            markdownCache.evictAll();
            
            // 清理后强制GC
            MemoryMonitor.forceGC("MarkdownRenderer destroyed");
        }
        // 清除context引用
        if (contextRef != null) {
            Timber.tag(TAG).d( "destroy: Clearing context reference");
            contextRef.clear();
        }
        
        Timber.tag(TAG).d( "destroy: Resource cleanup completed");
    }
}