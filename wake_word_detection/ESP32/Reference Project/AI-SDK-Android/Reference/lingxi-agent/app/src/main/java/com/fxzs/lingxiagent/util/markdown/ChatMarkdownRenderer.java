package com.fxzs.lingxiagent.util.markdown;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.dialog.TextSelectorView;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Code;
import org.commonmark.node.Emphasis;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;

import java.util.Arrays;
import java.util.List;

public class ChatMarkdownRenderer {
    private final Parser parser;
    private final Context context;

    // 按容器隔离的上次Markdown内容，避免多Item之间状态串扰
    private final java.util.WeakHashMap<LinearLayout, String> containerLastMarkdown = new java.util.WeakHashMap<>();

    // 异步解析相关 - 优化：使用多线程池提升并发渲染能力
    private static final java.util.concurrent.ExecutorService EXECUTOR =
            java.util.concurrent.Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                new java.util.concurrent.ThreadFactory() {
                    private final java.util.concurrent.atomic.AtomicInteger threadNumber = new java.util.concurrent.atomic.AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "MarkdownRenderer-" + threadNumber.getAndIncrement());
                        t.setPriority(Thread.NORM_PRIORITY - 1); // 降低渲染线程优先级，避免阻塞主线程
                        return t;
                    }
                }
            );

    // 优化：添加解析结果缓存，避免重复解析相同内容
    private static final android.util.LruCache<String, Node> PARSED_CACHE =
        new android.util.LruCache<String, Node>(50) {
            @Override
            protected int sizeOf(String key, Node value) {
                return 1; // 每个条目计为1
            }
        };

    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final java.util.concurrent.ConcurrentHashMap<LinearLayout, java.util.concurrent.Future<?>> pendingTasks = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<LinearLayout, Long> taskTokens = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<LinearLayout, Runnable> pendingMounts = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int CHUNK_SIZE = 2; // not used when USE_CHUNKED=false
    private static final boolean USE_CHUNKED = false;

    private View.OnLongClickListener mLongClickListener;

    public ChatMarkdownRenderer(Context context) {
        this.context = context;
        List<Extension> extensions = Arrays.asList(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
    }

    /**
     * 异步解析 Markdown：后台线程 parse，主线程分批组装 View，避免卡顿/闪烁
     */
    public void renderInto(LinearLayout container, String markdown) {
        String lastMarkdown = containerLastMarkdown.get(container);
        // 如果内容相同且已有子视图，直接跳过，避免无谓重绘
        if (markdown != null && markdown.equals(lastMarkdown) && container.getChildCount() > 0) {
            return;
        }
        containerLastMarkdown.put(container, markdown == null ? "" : markdown);

        // 取消之前的解析与挂载任务（不立刻清空容器，等首批视图就绪再替换）
        java.util.concurrent.Future<?> prev = pendingTasks.remove(container);
        if (prev != null) prev.cancel(true);
        Runnable prevMount = pendingMounts.remove(container);
        if (prevMount != null) mainHandler.removeCallbacks(prevMount);

        if (markdown == null || markdown.isEmpty()) {
            taskTokens.remove(container);
            container.removeAllViews();
            return;
        }

        final long token = System.nanoTime();
        taskTokens.put(container, token);

        // 优化：生成缓存键（使用内容哈希）
        final String cacheKey = generateCacheKey(markdown);

        // 优化：先检查缓存
        Node cachedDoc = PARSED_CACHE.get(cacheKey);
        if (cachedDoc != null) {
            // 缓存命中，直接使用缓存的解析结果
            mainHandler.post(() -> {
                Long current = taskTokens.get(container);
                if (current == null || current.longValue() != token) return;
                if (!USE_CHUNKED) {
                    mountAllAtOnce(container, cachedDoc, token);
                } else {
                    java.util.List<Node> blocks = collectTopBlocks(cachedDoc);
                    startChunkedMount(container, blocks, token);
                }
            });
            return;
        }

        // 缓存未命中，后台解析
        java.util.concurrent.Future<?> future = EXECUTOR.submit(() -> {
            Node doc = null;
            try {
                String normalized = markdown;
                normalized = ensureNewlineBeforeNumberedItems(normalized);
                doc = parser.parse(normalized);
                // 优化：缓存解析结果
                if (doc != null) {
                    PARSED_CACHE.put(cacheKey, doc);
                }
            } catch (Throwable ignore) {
                // ignore, fallback below
            }
            final Node finalDoc = doc;
            mainHandler.post(() -> {
                Long current = taskTokens.get(container);
                if (current == null || current.longValue() != token) return;
                if (finalDoc == null) {
                    swapToFallback(container, markdown);
                    return;
                }
                if (!USE_CHUNKED) {
                    // 性能让位于稳定，直接一次性离屏构建并替换，避免频繁布局导致的屏闪
                    mountAllAtOnce(container, finalDoc, token);
                } else {
                    // 收集顶级块，按块分批挂载
                    java.util.List<Node> blocks = collectTopBlocks(finalDoc);
                    startChunkedMount(container, blocks, token);
                }
            });
        });
        pendingTasks.put(container, future);
    }

    public static String ensureNewlineBeforeNumberedItems(String input) {
        if (input == null || input.trim().isEmpty()) return input;

        // 统一换行符
        input = input.replace("\r\n", "\n").replace("\r", "\n");

        // 修复：只在“非数字后”的 \d+\. 前加换行
        // (?<![0-9]) 表示前面不能是数字（避免 10 被拆成 1 和 0.）
        // (?=\\d+\\. ) 确保后面是一个完整的 “数字.” 模式
        return input.replaceAll("(?<![0-9])(?=\\d+\\. )", "\n");
    }

    /**
     * 生成缓存键
     * 使用内容哈希作为缓存键，避免存储大量重复字符串
     */
    private String generateCacheKey(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        // 使用hashCode作为缓存键，简单高效
        return String.valueOf(markdown.hashCode());
    }

    public void setOnLongClickListener(View.OnLongClickListener listener) {
        mLongClickListener = listener;
    }

    // 宽松表格预处理：
    // - 统一为每行表格行补齐前后竖线
    // - 对齐分隔行允许任意数量连字符与可选冒号
    // - 按表头列数，自动为数据行补齐缺失单元格（仅限纯文本拆分场景）
    private String preprocessTables(String markdown) {
        if (markdown == null || markdown.indexOf('|') < 0) return markdown;
        String[] lines = markdown.split("\n", -1);
        boolean inCode = false;
        int i = 0;
        while (i < lines.length) {
            String line = normalizeTableChars(lines[i]);
            String trimmed = line;
            if (trimmed.contains("```")) {
                int count = countOccurrences(trimmed, "```");
                if ((count & 1) == 1) inCode = !inCode;
            }
            if (inCode) {
                i++;
                continue;
            }

            if (looksLikeTableHeader(trimmed)) {
                // 确保表格与前文有空行隔开，提升解析兼容性
                lines[i] = "\n" + ensurePipes(trimmed);
                if (i + 1 < lines.length) {
                    String sep = normalizeTableChars(lines[i + 1]);
                    if (looksLikeSeparator(sep)) {
                        lines[i + 1] = ensurePipes(sep.trim());
                    } else {
                        // 若第二行不是标准分隔行，尝试按表头列数构造一个
                        int headerCols = countCells(ensurePipes(trimmed));
                        if (headerCols > 0) {
                            StringBuilder sbSep = new StringBuilder("|");
                            for (int k = 0; k < headerCols; k++) sbSep.append("---|");
                            lines[i + 1] = sbSep.toString();
                        }
                    }
                }
                int headerCols = countCells(lines[i]);
                int j = i + 2;
                while (j < lines.length && mayBeTableRow(lines[j])) {
                    String row = ensurePipes(normalizeTableChars(lines[j]).trim());
                    int cells = countCells(row);
                    if (headerCols > 0 && cells < headerCols) {
                        StringBuilder sb = new StringBuilder(row);
                        for (int k = cells; k < headerCols; k++) {
                            if (sb.charAt(sb.length() - 1) == '|') sb.insert(sb.length(), "  |");
                            else sb.append("  |");
                        }
                        row = sb.toString();
                    }
                    lines[j] = row;
                    j++;
                }
                i = j;
                continue;
            }
            i++;
        }
        String joined = String.join("\n", lines);
        // 在首尾各加一个换行，帮助解析器识别块边界（对表格更友好）
        return "\n" + joined + "\n";
    }

    private int countOccurrences(String s, String token) {
        int idx = 0, c = 0;
        while ((idx = s.indexOf(token, idx)) >= 0) {
            c++;
            idx += token.length();
        }
        return c;
    }

    private boolean looksLikeTableHeader(String line) {
        if (line == null) return false;
        String t = line.trim();
        return t.indexOf('|') >= 0 && !looksLikeSeparator(t);
    }

    private boolean looksLikeSeparator(String line) {
        if (line == null) return false;
        String t = line.trim();
        // 仅由 -, :, 空格或制表符组成，且至少包含一个 '-'
        String noPipes = t.replace("|", "").replace(" ", "").replace("\t", "");
        if (noPipes.isEmpty()) return false;
        boolean hasDash = false;
        for (int ii = 0; ii < noPipes.length(); ii++) {
            char ch = noPipes.charAt(ii);
            if (ch == '-') {
                hasDash = true;
                continue;
            }
            if (ch != ':') {
                return false;
            }
        }
        return hasDash;
    }

    private boolean mayBeTableRow(String line) {
        if (line == null) return false;
        String t = line.trim();
        if (t.isEmpty()) return false;
        return t.indexOf('|') >= 0 && !looksLikeSeparator(t);
    }

    private String ensurePipes(String line) {
        String t = line.trim();
        if (!t.startsWith("|")) t = "|" + t;
        if (!t.endsWith("|")) t = t + "|";
        return t;
    }

    // 将中文顿号、全角竖线等替换为标准字符，提升容错
    private String normalizeTableChars(String s) {
        if (s == null) return null;
        // Unicode 归一化（常见的全角/特殊空白）
        s = s.replace('｜', '|');      // 全角竖线 -> 半角
        s = s.replace('：', ':');      // 全角冒号 -> 半角
        s = s.replace("——", "--");   // 中文破折号 -> 连字符
        s = s.replace('\u3000', ' '); // 全角空格 -> 半角空格
        s = s.replace('\u00A0', ' '); // 不间断空格 -> 普通空格
        return s;
    }

    private int countCells(String lineWithPipes) {
        if (lineWithPipes == null) return 0;
        String t = ensurePipes(lineWithPipes);
        String[] parts = t.split("\\|", -1);
        int cnt = 0;
        for (int i2 = 1; i2 < parts.length - 1; i2++) cnt++;
        return cnt;
    }


    // 简单的增量渲染：若 new 以 old 开头，则尽量只更新最后一个块
    public void renderIncremental(LinearLayout container, String oldContent, String newContent) {
        if (oldContent == null || oldContent.isEmpty()) {
            renderInto(container, newContent);
            return;
        }
        if (newContent == null) newContent = "";
        if (!newContent.startsWith(oldContent)) {
            renderInto(container, newContent);
            return;
        }
        // 追加部分（轻量尝试，不触发解析）
        String increment = newContent.substring(oldContent.length());
        if (increment.isEmpty()) return;

        // 复杂结构或过大增量，回退全量
        if (increment.length() > 120 || containsStructureChange(increment) || hasComplexChildren(container)) {
            renderInto(container, newContent);
            return;
        }

        // 取消可能还在进行的解析任务，避免抢占
        java.util.concurrent.Future<?> prev = pendingTasks.remove(container);
        if (prev != null) prev.cancel(true);

        int childCount = container.getChildCount();
        if (childCount == 0) {
            renderInto(container, newContent);
            return;
        }
        View last = container.getChildAt(childCount - 1);
        if (last instanceof TextView) {
            ((TextView) last).append(increment);
            containerLastMarkdown.put(container, newContent);
        } else if (last instanceof CodeBlockContainerView) {
            ((CodeBlockContainerView) last).appendCodeText(increment);
            containerLastMarkdown.put(container, newContent);
        } else if (last instanceof CodeBlockView) { // 兼容旧控件
            ((CodeBlockView) last).appendCodeText(increment);
            containerLastMarkdown.put(container, newContent);
        } else {
            // 结构变化，回退到异步全量渲染
            renderInto(container, newContent);
        }
    }

    private boolean containsStructureChange(String inc) {
        // 简单判定：出现代码围栏/表格管道/图片语法等时，认为结构变化
        if (inc == null) return false;
        String s = inc;
        return s.contains("```") || s.contains("\n|") || s.contains("![](") || s.contains("\n#");
    }

    private boolean hasComplexChildren(LinearLayout container) {
        int count = container.getChildCount();
        if (count == 0) return false;
        View last = container.getChildAt(count - 1);
        // 如果最后一块是表格或图片之类，避免增量
        return (last instanceof HorizontalScrollView) || (last instanceof TableLayout) || (last instanceof ImageView);
    }


    private void mountAllAtOnce(LinearLayout container, Node finalDoc, long token) {
        // 校验 token，避免过期视图覆盖
        Long current = taskTokens.get(container);
        if (current == null || current.longValue() != token) return;
        // 离屏构建
        LinearLayout buffer = new LinearLayout(context);
        buffer.setOrientation(LinearLayout.VERTICAL);
        renderChildren(buffer, finalDoc);
        // 交换视图（一次性替换，减少闪烁）
        container.removeAllViews();
        // 暂时禁用布局请求，避免重复测量/闪烁
        if (Build.VERSION.SDK_INT >= 18) container.suppressLayout(true);
        for (int i = 0; i < buffer.getChildCount(); i++) {
            View child = buffer.getChildAt(i);
            buffer.removeViewAt(i);
            i--;
            container.addView(child);
        }
        if (Build.VERSION.SDK_INT >= 18) container.suppressLayout(false);
        container.requestLayout();
    }

    private void swapToFallback(LinearLayout container, String markdown) {
        container.removeAllViews();
        TextView tv = new TextView(context);
        tv.setText(markdown);
        tv.setTextSize(MarkdownStyle.BODY_TEXT_SIZE_SP);
        tv.setLineSpacing(0, 1.2f);
        tv.setTextColor(0xFF222222);
        container.addView(tv);
    }


    private java.util.List<Node> collectTopBlocks(Node parent) {
        java.util.ArrayList<Node> list = new java.util.ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNext()) {
            list.add(n);
        }
        return list;
    }

    private void startChunkedMount(LinearLayout container, java.util.List<Node> blocks, long token) {
        container.removeAllViews();
        final int total = blocks.size();
        final int[] index = {0};
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Long current = taskTokens.get(container);
                if (current == null || current.longValue() != token) return;
                // 抑制本批次内的多次布局，减少闪烁
                if (Build.VERSION.SDK_INT >= 18) container.suppressLayout(true);
                int count = 0;
                while (index[0] < total && count < CHUNK_SIZE) {
                    Node n = blocks.get(index[0]++);
                    try {
                        if (n instanceof org.commonmark.node.Paragraph) {
                            org.commonmark.node.Paragraph p = (org.commonmark.node.Paragraph) n;
                            if (containsSingleImage(p)) {
                                container.addView(buildImageBlock(p));
                            } else {
                                container.addView(buildParagraph(p));
                            }
                        } else if (n instanceof org.commonmark.node.Heading) {
                            container.addView(buildHeading((org.commonmark.node.Heading) n));
                        } else if (n instanceof org.commonmark.node.BlockQuote) {
                            container.addView(buildBlockQuote((org.commonmark.node.BlockQuote) n));
                        } else if (n instanceof org.commonmark.node.BulletList) {
                            container.addView(buildList((org.commonmark.node.ListBlock) n));
                        } else if (n instanceof org.commonmark.node.OrderedList) {
                            container.addView(buildList((org.commonmark.node.ListBlock) n));
                        } else if (n instanceof org.commonmark.node.FencedCodeBlock) {
                            container.addView(buildCode((org.commonmark.node.FencedCodeBlock) n));
                        } else if (n instanceof org.commonmark.node.ThematicBreak) {
                            container.addView(buildHr());
                        } else if (n instanceof TableBlock || n instanceof TableHead || n instanceof TableBody) {
                            if (n instanceof TableBlock) {
                                container.addView(buildTable((TableBlock) n));
                            }
                        }
                    } catch (Throwable t) {
                        TextView tv = new TextView(context);
                        tv.setText(extractPlainText(n));
                        tv.setTextSize(MarkdownStyle.BODY_TEXT_SIZE_SP);
                        tv.setLineSpacing(0, 1.2f);
                        tv.setTextColor(0xFF222222);
                        container.addView(tv);
                    }
                    count++;
                }
                if (Build.VERSION.SDK_INT >= 18) container.suppressLayout(false);
                container.requestLayout();
                if (index[0] < total) {
                    // 让出一帧，保证手势/滚动事件可以响应
                    try {
                        container.postOnAnimation(this);
                    } catch (Throwable ignore) {
                        mainHandler.postDelayed(this, 8);
                    }
                } else {
                    pendingMounts.remove(container);
                }
            }
        };
        pendingMounts.put(container, task);
        // 首批挂载也让出一帧，避免 removeAllViews 后立即大量 add 导致闪烁
        try {
            container.postOnAnimation(task);
        } catch (Throwable ignore) {
            mainHandler.postDelayed(task, 8);
        }
    }

    private void renderChildren(LinearLayout container, Node parent) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNext()) {
            try {
                if (n instanceof org.commonmark.node.Paragraph) {
                    org.commonmark.node.Paragraph p = (org.commonmark.node.Paragraph) n;
                    if (containsSingleImage(p)) {
                        container.addView(buildImageBlock(p));
                    } else {
                        container.addView(buildParagraph(p));
                    }
                } else if (n instanceof org.commonmark.node.Heading) {
                    container.addView(buildHeading((org.commonmark.node.Heading) n));
                } else if (n instanceof org.commonmark.node.BlockQuote) {
                    container.addView(buildBlockQuote((org.commonmark.node.BlockQuote) n));
                } else if (n instanceof org.commonmark.node.BulletList) {
                    container.addView(buildList((org.commonmark.node.ListBlock) n));
                } else if (n instanceof org.commonmark.node.OrderedList) {
                    container.addView(buildList((org.commonmark.node.ListBlock) n));
                } else if (n instanceof org.commonmark.node.FencedCodeBlock) {
                    container.addView(buildCode((org.commonmark.node.FencedCodeBlock) n));
                } else if (n instanceof org.commonmark.node.ThematicBreak) {
                    container.addView(buildHr());
                } else if (n instanceof TableBlock || n instanceof TableHead || n instanceof TableBody) {
                    if (n instanceof TableBlock) {
                        container.addView(buildTable((TableBlock) n));
                    }
                }
            } catch (Throwable t) {
                // 单块渲染失败兜底，避免整页崩溃
                TextView tv = new TextView(context);
                tv.setText(extractPlainText(n));
                tv.setTextSize(MarkdownStyle.BODY_TEXT_SIZE_SP);
                tv.setLineSpacing(0, 1.2f);
                tv.setTextColor(0xFF222222);
                container.addView(tv);
            }
        }
        StringBuilder textAll = new StringBuilder();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof TextSelectorView) {
                textAll.append(((TextSelectorView) child).getText().toString()).append("\n");
            }
        }
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof TextSelectorView) {
                child.setTag(textAll.toString());
            }
        }
    }

    private View buildParagraph(org.commonmark.node.Paragraph paragraph) {
        TextSelectorView tv = new TextSelectorView(context);
        tv.setText(spansFromInline(paragraph));
        tv.setOnLongClickListener(mLongClickListener);
        MarkdownStyle.applyBody(tv);
        return tv;
    }


    private View buildHeading(org.commonmark.node.Heading heading) {
        TextSelectorView tv = new TextSelectorView(context);
        tv.setText(spansFromInline(heading));
        int level = heading.getLevel();
        MarkdownStyle.applyHeading(tv, level);
        int top = MarkdownStyle.dp(context, MarkdownStyle.headingTopMarginDp(level));
        int bottom = MarkdownStyle.dp(context, MarkdownStyle.headingBottomMarginDp(level));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = top;
        lp.bottomMargin = bottom;
        tv.setLayoutParams(lp);
        tv.setOnLongClickListener(mLongClickListener);
        return tv;
    }

    private View buildBlockQuote(org.commonmark.node.BlockQuote blockQuote) {
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        View bar = new View(context);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MarkdownStyle.dp(context, MarkdownStyle.QUOTE_BAR_WIDTH_DP), LinearLayout.LayoutParams.MATCH_PARENT);
        p.rightMargin = dp(8);
        bar.setLayoutParams(p);
        bar.setBackgroundColor(MarkdownStyle.QUOTE_BAR_COLOR);
        ll.addView(bar);
        LinearLayout inner = new LinearLayout(context);
        inner.setOrientation(LinearLayout.VERTICAL);
        renderChildren(inner, blockQuote);
        ll.addView(inner);
        return ll;
    }

    private View buildList(org.commonmark.node.ListBlock list) {
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        // 判断列表类型（有序/无序）
        boolean isOrdered = list instanceof OrderedList;
        int startNumber = 1;
        char delimiter = '.'; // 有序列表的分隔符（如 1. 2. 或 1) 2)

        if (isOrdered) {
            OrderedList orderedList = (OrderedList) list;
            startNumber = orderedList.getStartNumber(); // 获取起始序号（默认1）
            delimiter = orderedList.getDelimiter(); // 获取分隔符（. 或 )
        }

        // 遍历列表项，生成内容
        int currentNumber = startNumber;
        for (Node n = list.getFirstChild(); n != null; n = n.getNext()) {
            if (n instanceof ListItem) {
                ListItem item = (ListItem) n;
                // 构建列表项文本（包含序号或符号）
                StringBuilder itemPrefix = new StringBuilder();
                if (isOrdered) {
                    // 有序列表：添加序号（如 "1. "）
                    itemPrefix.append(currentNumber)
                            .append(delimiter)
                            .append(" ");
                    currentNumber++;
                } else {
                    // 无序列表：添加符号（如 "• "，可替换为 "- " 等）
                    itemPrefix.append("• "); // 使用 • 作为无序列表符号
                }

                // 生成列表项内容（前缀 + 实际文本）
                CharSequence itemContent = spansFromInline(item); // 原方法：处理列表项内的富文本
                CharSequence fullContent = TextUtils.concat(itemPrefix, itemContent);

                // 创建列表项 TextView
                TextView tv = new TextView(context);
                tv.setText(fullContent);
                MarkdownStyle.applyBody(tv); // 应用原有样式

                // 如需处理嵌套列表，可递归调用 buildList 处理子列表
                // （此处简化处理，实际需遍历 item 的子节点，判断是否包含 ListBlock）

                ll.addView(tv);
            }
        }

        return ll;
    }


    private View buildCode(org.commonmark.node.FencedCodeBlock code) {
        CodeBlockContainerView v = new CodeBlockContainerView(context);
        String info = code.getInfo();
        if (info != null) {
            String lang = info.trim().split("\\s+")[0];
            v.setCodeText(code.getLiteral(), lang);
        } else {
            v.setCodeText(code.getLiteral());
        }
        return v;
    }

    private View buildHr() {
        View v = new View(context);
        v.setBackgroundColor(MarkdownStyle.HR_COLOR);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        ));
        return v;
    }

    private View buildTable(TableBlock tableBlock) {
        TableBlockContainerView wrap = new TableBlockContainerView(context);
        // 试图从当前 markdown 粗略提取表格 markdown，用于复制/全屏（可进一步精确化）
        String extracted = extractTableMarkdown(tableBlock);
        wrap.setTableMarkdown(extracted);
        wrap.renderTable(tableBlock);
        return wrap;
    }


    private boolean containsSingleImage(org.commonmark.node.Paragraph paragraph) {
        int count = 0;
        for (Node n = paragraph.getFirstChild(); n != null; n = n.getNext()) {
            if (n instanceof Image) count++;
            else if (!(n instanceof Text)) return false;
        }
        return count == 1;
    }

    private View buildImageBlock(org.commonmark.node.Paragraph paragraph) {
        // 查找图片节点
        Image image = null;
        for (Node n = paragraph.getFirstChild(); n != null; n = n.getNext()) {
            if (n instanceof Image) {
                image = (Image) n;
                break;
            }
        }
        if (image == null) return buildParagraph(paragraph);

        // 外层容器，保证上下 margin 一致
        LinearLayout wrap = new LinearLayout(context);
        wrap.setOrientation(LinearLayout.VERTICAL);

        ImageView iv = new ImageView(context);
//        iv.setAdjustViewBounds(true);
//        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setContentDescription("md_image");

        int width = context.getResources().getDisplayMetrics().widthPixels;
        int padding = dp(16);
        int targetW = width - padding * 2;

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        iv.setLayoutParams(lp);

        String url = image.getDestination();
        // 使用占位图，避免布局跳动
        RequestOptions opts = new RequestOptions()
                .placeholder(R.drawable.bg_imagine_loading)
                .error(R.drawable.bg_imagine_loading);
        Glide.with(context).load(url).apply(opts).into(iv);

        // 点击预览（简单提示，若你有预览 Activity 可在此启动）
        iv.setOnClickListener(v -> {
            // TODO: 若项目已有大图预览页，可在此跳转
            // 例如：ImagePreviewActivity.start(context, url);
        });

        wrap.addView(iv);
        return wrap;
    }


    private void addTableSection(TableLayout tl, org.commonmark.ext.gfm.tables.TableHead head, boolean isHeader) {
        for (Node r = head.getFirstChild(); r != null; r = r.getNext()) {
            if (r instanceof org.commonmark.ext.gfm.tables.TableRow) {
                TableRow row = new TableRow(context);
                for (Node c = r.getFirstChild(); c != null; c = c.getNext()) {
                    if (c instanceof TableCell) {
                        TextView tv = buildTableCell((TableCell) c, isHeader);
                        row.addView(tv);
                    }
                }
                tl.addView(row);
            }
        }
    }

    private void addTableSection(TableLayout tl, org.commonmark.ext.gfm.tables.TableBody body, boolean isHeader) {
        for (Node r = body.getFirstChild(); r != null; r = r.getNext()) {
            if (r instanceof org.commonmark.ext.gfm.tables.TableRow) {
                TableRow row = new TableRow(context);
                for (Node c = r.getFirstChild(); c != null; c = c.getNext()) {
                    if (c instanceof TableCell) {
                        TextView tv = buildTableCell((TableCell) c, false);
                        row.addView(tv);
                    }
                }
                tl.addView(row);
            }
        }
    }

    private TextView buildTableCell(TableCell cell, boolean isHeader) {
        TextView tv = new TextView(context);
        tv.setText(spansFromInline(cell));
        tv.setTextSize(MarkdownStyle.BODY_TEXT_SIZE_SP);
        tv.setLineSpacing(0, 1.15f);
        tv.setPadding(dp(8), dp(6), dp(8), dp(6));
        tv.setTextColor(isHeader ? MarkdownStyle.BODY_COLOR : MarkdownStyle.BODY_COLOR);
        if (isHeader) {
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setBackgroundColor(MarkdownStyle.TABLE_HEADER_BG);
        }
        // 对齐（防御性：alignment 可能为 null）
        org.commonmark.ext.gfm.tables.TableCell.Alignment align = cell.getAlignment();
        if (align == org.commonmark.ext.gfm.tables.TableCell.Alignment.CENTER) {
            tv.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        } else if (align == org.commonmark.ext.gfm.tables.TableCell.Alignment.RIGHT) {
            tv.setGravity(android.view.Gravity.END);
        } else {
            tv.setGravity(android.view.Gravity.START);
        }
        return tv;
    }

    private CharSequence spansFromInline(Node container) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        buildInline(ssb, container);
        return ssb;
    }

    private void buildInline(SpannableStringBuilder ssb, Node container) {
        for (Node n = container.getFirstChild(); n != null; n = n.getNext()) {
            if (n instanceof Text) {
                ssb.append(((Text) n).getLiteral());
            } else if (n instanceof Emphasis) {
                int start = ssb.length();
                buildInline(ssb, n);
                ssb.setSpan(new StyleSpan(Typeface.ITALIC), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (n instanceof StrongEmphasis) {
                int start = ssb.length();
                buildInline(ssb, n);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (n instanceof Code) {
                int start = ssb.length();
                ssb.append(((Code) n).getLiteral());
                ssb.setSpan(new BackgroundColorSpan(MarkdownStyle.INLINE_CODE_BG), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new ForegroundColorSpan(MarkdownStyle.INLINE_CODE_FG), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new TypefaceSpan("monospace"), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new RelativeSizeSpan(0.95f), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (n instanceof SoftLineBreak || n instanceof HardLineBreak) {
                ssb.append('\n');
            } else if (n instanceof Link) {
                int start = ssb.length();
                buildInline(ssb, n);
                ssb.setSpan(new ForegroundColorSpan(MarkdownStyle.LINK_COLOR), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                // 可选：下划线
                ssb.setSpan(new UnderlineSpan(), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                String linkUrl = ((Link) n).getDestination();
                // 使用 ClickableSpan 处理链接点击
                ssb.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        } catch (Exception e) {
                            // 处理无法打开链接的情况
                            ZUtils.showToast("无法打开链接");
                        }
                    }

                    @Override
                    public void updateDrawState(android.text.TextPaint ds) {
                        // 设置链接样式
                        ds.setColor(MarkdownStyle.LINK_COLOR);
                        ds.setUnderlineText(true);
                    }
                }, start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                buildInline(ssb, n);
            }
        }
    }

    private int dp(int v) {
        float d = context.getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }

    private String extractPlainText(Node n) {
        if (n == null) return "";
        StringBuilder sb = new StringBuilder();
        collectText(sb, n);
        return sb.toString();
    }

    private void collectText(StringBuilder sb, Node n) {
        for (Node c = n.getFirstChild(); c != null; c = c.getNext()) {
            if (c instanceof Text) sb.append(((Text) c).getLiteral());
            else collectText(sb, c);
        }
    }

    // 粗略从当前表格节点向外提取连续的 markdown 行（后续可替换为精准序列化）
    private String extractTableMarkdown(TableBlock tableBlock) {
        try {
            if (tableBlock == null) return "";
            StringBuilder sb = new StringBuilder();

            // 1) Header row
            TableHead head = null;
            TableBody body = null;
            for (Node n = tableBlock.getFirstChild(); n != null; n = n.getNext()) {
                if (n instanceof TableHead) head = (TableHead) n;
                else if (n instanceof TableBody) body = (TableBody) n;
            }

            java.util.List<String> headerTexts = new java.util.ArrayList<>();
            java.util.List<TableCell.Alignment> aligns = new java.util.ArrayList<>();

            if (head != null) {
                for (Node row = head.getFirstChild(); row != null; row = row.getNext()) {
                    if (!(row instanceof org.commonmark.ext.gfm.tables.TableRow)) continue;
                    // only first header row
                    for (Node c = row.getFirstChild(); c != null; c = c.getNext()) {
                        if (c instanceof TableCell) {
                            TableCell cell = (TableCell) c;
                            headerTexts.add(safeCellText(cell));
                            aligns.add(cell.getAlignment());
                        }
                    }
                    break; // only 1 header row for markdown output
                }
            }

            if (!headerTexts.isEmpty()) {
                // header line
                sb.append('|');
                for (int i = 0; i < headerTexts.size(); i++) {
                    sb.append(' ').append(headerTexts.get(i).trim()).append(' ').append('|');
                }
                sb.append('\n');

                // alignment/separator line
                sb.append('|');
                for (int i = 0; i < headerTexts.size(); i++) {
                    TableCell.Alignment a = i < aligns.size() ? aligns.get(i) : null;
                    String seg;
                    if (a == null) {
                        seg = "---";
                    } else if (a == TableCell.Alignment.CENTER) {
                        seg = ":---:";
                    } else if (a == TableCell.Alignment.RIGHT) {
                        seg = "---:";
                    } else if (a == TableCell.Alignment.LEFT) {
                        seg = ":---"; // GitHub 左对齐语法常见写法也支持 ---
                    } else {
                        seg = "---";
                    }
                    sb.append(' ').append(seg).append(' ').append('|');
                }
                sb.append('\n');
            }

            // 2) Body rows
            if (body != null) {
                for (Node row = body.getFirstChild(); row != null; row = row.getNext()) {
                    if (!(row instanceof org.commonmark.ext.gfm.tables.TableRow)) continue;
                    sb.append('|');
                    for (Node c = row.getFirstChild(); c != null; c = c.getNext()) {
                        if (c instanceof TableCell) {
                            String text = safeCellText((TableCell) c);
                            sb.append(' ').append(text.trim()).append(' ').append('|');
                        }
                    }
                    sb.append('\n');
                }
            }

            return sb.toString().trim();
        } catch (Throwable t) {
            // 任何异常均不影响表格渲染，回退到空字符串
            return "";
        }
    }

    private String safeCellText(TableCell cell) {
        // 提取单元格的纯文本，包括行内代码与换行等基础内容
        if (cell == null) return "";
        StringBuilder out = new StringBuilder();
        collectTextForCell(out, cell);
        return out.toString();
    }

    private void collectTextForCell(StringBuilder out, Node n) {
        for (Node c = n.getFirstChild(); c != null; c = c.getNext()) {
            if (c instanceof Text) {
                out.append(((Text) c).getLiteral());
            } else if (c instanceof Code) {
                out.append(((Code) c).getLiteral());
            } else if (c instanceof SoftLineBreak || c instanceof HardLineBreak) {
                out.append(' ');
            } else {
                collectTextForCell(out, c);
            }
        }
    }

    // 提供静态 dp 方法给其他容器使用
    static int dpStatic(Context ctx, int v) {
        float d = ctx.getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }

    /**
     * 清理所有资源，防止内存泄漏
     */
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
    }

}

