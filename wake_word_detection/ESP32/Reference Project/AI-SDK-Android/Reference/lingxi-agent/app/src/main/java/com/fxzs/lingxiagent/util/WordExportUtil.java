package com.fxzs.lingxiagent.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Word文档导出工具类（使用RTF格式）
 */
public class WordExportUtil {
    private static final String TAG = "WordExportUtil";

    /**
     * 导出AIResponse内容为Word文档（RTF格式）
     *
     * @param context 上下文
     * @param title 文档标题
     * @param content 内容（支持Markdown格式）
     * @param callback 导出结果回调
     */
    public static void exportToWord(Context context, String title, String content, ExportCallback callback) {
        new Thread(() -> {
            try {
                // 调试：记录原始内容信息
                Timber.tag(TAG).d( "=== 开始导出文档 ===");
                Timber.tag(TAG).d( "标题: " + title);
                Timber.tag(TAG).d( "原始内容长度: " + (content != null ? content.length() : 0));
                if (content != null && content.length() > 0) {
                    // 显示内容的前200个字符用于调试
                    String preview = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                    Timber.tag(TAG).d( "内容预览: " + preview);
                    
                    // 检查是否存在明显的重复模式
                    checkForObviousDuplication(content);
                }

                // 生成RTF内容
                StringBuilder rtfContent = new StringBuilder();

                // RTF文档头 - 优化编码设置以更好支持中文和特殊符号
                rtfContent.append("{\\rtf1\\ansi\\ansicpg936\\deff0\\deflang2052");
                // 字体表：添加更多字体支持
                rtfContent.append("{\\fonttbl");
                rtfContent.append("{\\f0\\fnil\\fcharset134\\fprq2 SimSun;}");
                rtfContent.append("{\\f1\\fnil\\fcharset134\\fprq2 Microsoft YaHei;}");
                rtfContent.append("{\\f2\\fnil\\fcharset134\\fprq2 Arial Unicode MS;}");
                rtfContent.append("}");
                // 颜色表
                rtfContent.append("{\\colortbl;\\red0\\green0\\blue0;\\red102\\green102\\blue102;}");
                // 文档设置：启用Unicode支持
                rtfContent.append("\\viewkind4\\uc1\\pard");

                // 添加标题
                addRTFTitle(rtfContent, title);

                // 添加生成时间
                addRTFGenerationTime(rtfContent);

                // 添加分隔线
                addRTFSeparator(rtfContent);

                // 解析并添加内容
                parseAndAddRTFContent(rtfContent, content);

                // RTF文档尾
                rtfContent.append("}");

                // 调试：显示最终生成的RTF内容
                String finalRtf = rtfContent.toString();
                Timber.tag(TAG).d( "最终RTF内容长度: " + finalRtf.length());
                if (finalRtf.length() > 500) {
                    Timber.tag(TAG).d( "RTF内容预览: " + finalRtf.substring(0, 500) + "...");
                } else {
                    Timber.tag(TAG).d( "完整RTF内容: " + finalRtf);
                }

                // 保存文档
                File file = saveRTFDocument(context, finalRtf, title);

                Timber.tag(TAG).d( "=== 文档导出完成 ===");

                // 回调成功
                if (callback != null) {
                    callback.onSuccess(file);
                }

            } catch (Exception e) {
                Timber.tag(TAG).e( "导出Word文档失败", e);
                if (callback != null) {
                    callback.onError("导出失败: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * 检查内容中是否存在明显的重复模式
     */
    private static void checkForObviousDuplication(String content) {
        if (content == null || content.length() < 50) return;

        // 检查是否存在完全重复的一半
        int halfLength = content.length() / 2;
        if (halfLength > 10) {
            String firstHalf = content.substring(0, halfLength);
            String secondHalf = content.substring(halfLength);
            if (firstHalf.equals(secondHalf)) {
                Timber.tag(TAG).w( "⚠️ 检测到内容完全重复！内容被复制了一遍");
                return;
            }
        }

        // 检查行级重复
        String[] lines = content.split("\n");
        java.util.Map<String, Integer> lineCount = new java.util.HashMap<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lineCount.put(trimmed, lineCount.getOrDefault(trimmed, 0) + 1);
            }
        }

        int duplicateLines = 0;
        for (java.util.Map.Entry<String, Integer> entry : lineCount.entrySet()) {
            if (entry.getValue() > 1) {
                duplicateLines++;
                Timber.tag(TAG).w( "⚠️ 重复行 (" + entry.getValue() + "次): " +
                    (entry.getKey().length() > 50 ? entry.getKey().substring(0, 50) + "..." : entry.getKey()));
            }
        }

        if (duplicateLines > 0) {
            Timber.tag(TAG).w("⚠️ 总共发现 " + duplicateLines + " 行重复内容");
        } else {
            Timber.tag(TAG).d( "✅ 未发现明显的重复内容");
        }
    }
    
    /**
     * 添加RTF文档标题
     */
    private static void addRTFTitle(StringBuilder rtf, String title) {
        String escapedTitle = escapeRTFText(title);

        Timber.tag(TAG).d( "标题原文: " + title);
        Timber.tag(TAG).d( "标题转义: " + escapedTitle);

        rtf.append("\\pard\\qc\\f0\\fs36\\b ");
        rtf.append(escapedTitle);
        rtf.append("\\b0\\par\\par");
    }

    /**
     * 添加RTF生成时间
     */
    private static void addRTFGenerationTime(StringBuilder rtf) {
        String currentTime = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()).format(new Date());
        String timeText = "生成时间: " + currentTime;
        String escapedTimeText = escapeRTFText(timeText);

        Timber.tag(TAG).d( "时间原文: " + timeText);
        Timber.tag(TAG).d( "时间转义: " + escapedTimeText);

        rtf.append("\\pard\\qr\\f0\\fs20\\cf2 ");
        rtf.append(escapedTimeText);
        rtf.append("\\cf1\\par\\par");
    }

    /**
     * 添加RTF分隔线
     */
    private static void addRTFSeparator(StringBuilder rtf) {
        rtf.append("\\pard\\qc\\f0\\fs20\\cf2 ");
        // 使用简单的ASCII字符作为分隔线，避免特殊字符乱码
        rtf.append("----------------------------------------");
        rtf.append("\\cf1\\par\\par");
    }

    /**
     * 转义RTF特殊字符并处理中文编码
     */
    private static String escapeRTFText(String text) {
        if (text == null) return "";

        Timber.tag(TAG).d( "escapeRTFText - 输入: " + text);

        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '\\') {
                result.append("\\\\");
            } else if (c == '{') {
                result.append("\\{");
            } else if (c == '}') {
                result.append("\\}");
            } else if (c == '\n') {
                // 保留换行符，转换为RTF的换行标记
                result.append("\\line ");
            } else if (c == '\r') {
                // 忽略回车符
                continue;
            } else if (needsUnicodeEscape(c)) {
                // 需要Unicode转义的字符，使用专门的处理方法
                result.append(getSpecialSymbolRTF(c));
            } else {
                result.append(c);
            }
        }
        
        String escaped = result.toString();
        Timber.tag(TAG).d( "escapeRTFText - 输出: " + escaped);
        return escaped;
    }

    /**
     * 判断字符是否需要Unicode转义
     */
    private static boolean needsUnicodeEscape(char c) {
        // ASCII控制字符需要转义（除了制表符和换行符）
        if (c < 32 && c != '\t' && c != '\n') return true;
        
        // 基本ASCII字符不需要转义
        if (c >= 32 && c <= 126) return false;
        
        // 扩展ASCII和所有Unicode字符都需要转义
        return c > 126;
    }

    /**
     * 获取特殊符号的RTF表示
     */
    private static String getSpecialSymbolRTF(char c) {
        // 处理一些常见的特殊符号
        switch (c) {
            case '\u2022': return "\\bullet ";  // •
            case '\u2014': return "\\emdash ";  // —
            case '\u2013': return "\\endash ";  // –
            case '\u201C': return "\\ldblquote ";  // "
            case '\u201D': return "\\rdblquote ";  // "
            case '\u2018': return "\\lquote ";  // '
            case '\u2019': return "\\rquote ";  // '
            case '\u2026': return "\\ldots ";  // …
            default:
                // 对于其他字符，使用Unicode转义
                int codePoint = (int) c;
                // RTF Unicode转义：对于大于32767的字符，需要转换为有符号16位整数
                if (codePoint > 32767) {
                    codePoint = codePoint - 65536;
                }
                return "\\u" + codePoint + "?";
        }
    }
    
    /**
     * 解析Markdown内容并添加到RTF文档
     */
    private static void parseAndAddRTFContent(StringBuilder rtf, String content) {
        if (content == null || content.trim().isEmpty()) {
            rtf.append("\\pard\\f0\\fs24 ");
            rtf.append(escapeRTFText("暂无内容"));
            rtf.append("\\par");
            return;
        }

        Timber.tag(TAG).d( "开始解析内容，原始长度: " + content.length());

        // 预处理内容：去除可能的重复段落
        String cleanedContent = preprocessContent(content);
        Timber.tag(TAG).d( "预处理后内容长度: " + cleanedContent.length());

        // 按行分割内容
        String[] lines = cleanedContent.split("\n");
        Timber.tag(TAG).d( "分割后行数: " + lines.length);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.isEmpty()) {
                // 空行
                rtf.append("\\par");
                continue;
            }

            Timber.tag(TAG).d( "处理第" + (i+1) + "行: " + (line.length() > 50 ? line.substring(0, 50) + "..." : line));

            if (line.startsWith("# ")) {
                // 一级标题
                addRTFHeading(rtf, line.substring(2), 32, true);
            } else if (line.startsWith("## ")) {
                // 二级标题
                addRTFHeading(rtf, line.substring(3), 28, true);
            } else if (line.startsWith("### ")) {
                // 三级标题
                addRTFHeading(rtf, line.substring(4), 24, true);
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                // 无序列表
                addRTFBulletPoint(rtf, line.substring(2));
            } else if (line.matches("^\\d+\\. .*")) {
                // 有序列表
                addRTFNumberedPoint(rtf, line);
            } else {
                // 普通段落
                addRTFFormattedText(rtf, line);
            }
        }
    }

    /**
     * 预处理内容，去除可能的重复段落
     */
    private static String preprocessContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        Timber.tag(TAG).d( "预处理前内容: " + content.substring(0, Math.min(200, content.length())) + "...");

        // 检查是否存在整体内容重复（比如整个内容被复制了一遍）
        String dedupedContent = removeGlobalDuplication(content);
        
        // 按行分割进行行级去重
        String[] lines = dedupedContent.split("\n");
        StringBuilder result = new StringBuilder();
        java.util.Set<String> seenLines = new java.util.HashSet<>();
        String previousLine = null;

        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 跳过空行的重复检测
            if (trimmedLine.isEmpty()) {
                result.append(line).append("\n");
                continue;
            }
            
            // 跳过与前一行完全相同的行（连续重复）
            if (previousLine != null && previousLine.equals(trimmedLine)) {
                Timber.tag(TAG).d( "发现连续重复行，跳过: " + trimmedLine);
                continue;
            }

            // 跳过在整个文档中已经出现过的非空行（全局重复）
            if (seenLines.contains(trimmedLine)) {
                Timber.tag(TAG).d( "发现全局重复行，跳过: " + trimmedLine);
                continue;
            }

            result.append(line).append("\n");
            seenLines.add(trimmedLine);
            previousLine = trimmedLine;
        }

        String finalContent = result.toString();
        Timber.tag(TAG).d( "预处理后内容: " + finalContent.substring(0, Math.min(200, finalContent.length())) + "...");
        
        return finalContent;
    }

    /**
     * 检测并移除整体内容重复
     */
    private static String removeGlobalDuplication(String content) {
        if (content == null || content.length() < 100) {
            return content;
        }

        // 检查内容是否被完整重复了（比如整个内容被复制粘贴了一遍）
        int halfLength = content.length() / 2;
        String firstHalf = content.substring(0, halfLength);
        String secondHalf = content.substring(halfLength);

        // 如果前半部分和后半部分相同，说明内容被重复了
        if (firstHalf.equals(secondHalf)) {
            Timber.tag(TAG).d( "检测到整体内容重复，移除重复部分");
            return firstHalf;
        }

        // 检查是否存在大段重复（允许一些差异）
        String[] paragraphs = content.split("\n\n");
        if (paragraphs.length >= 4) {
            int midPoint = paragraphs.length / 2;
            StringBuilder firstHalfParagraphs = new StringBuilder();
            StringBuilder secondHalfParagraphs = new StringBuilder();
            
            for (int i = 0; i < midPoint; i++) {
                firstHalfParagraphs.append(paragraphs[i]).append("\n\n");
            }
            for (int i = midPoint; i < paragraphs.length; i++) {
                secondHalfParagraphs.append(paragraphs[i]).append("\n\n");
            }
            
            String firstPart = firstHalfParagraphs.toString().trim();
            String secondPart = secondHalfParagraphs.toString().trim();
            
            // 计算相似度
            double similarity = calculateSimilarity(firstPart, secondPart);
            if (similarity > 0.8) { // 80%以上相似度认为是重复
                Timber.tag(TAG).d( "检测到段落级别重复，相似度: " + similarity + "，移除重复部分");
                return firstPart;
            }
        }

        return content;
    }

    /**
     * 计算两个字符串的相似度
     */
    private static double calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) return 0.0;
        if (str1.equals(str2)) return 1.0;
        
        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = levenshteinDistance(str1, str2);
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * 计算编辑距离
     */
    private static int levenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];
        
        for (int i = 0; i <= str1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= str2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
                }
            }
        }
        
        return dp[str1.length()][str2.length()];
    }
    
    /**
     * 添加RTF标题
     */
    private static void addRTFHeading(StringBuilder rtf, String text, int fontSize, boolean bold) {
        rtf.append("\\pard\\f0\\fs").append(fontSize);
        if (bold) {
            rtf.append("\\b ");
        }
        rtf.append(escapeRTFText(text));
        if (bold) {
            rtf.append("\\b0");
        }
        rtf.append("\\par\\par");
    }

    /**
     * 添加RTF无序列表项
     */
    private static void addRTFBulletPoint(StringBuilder rtf, String text) {
        rtf.append("\\pard\\li720\\f0\\fs24 ");
        // 使用RTF标准的bullet符号
        rtf.append("\\bullet ");
        // 直接添加格式化文本内容，避免重复的段落格式
        addRTFInlineFormattedText(rtf, text);
        rtf.append("\\par");
    }

    /**
     * 添加RTF有序列表项
     */
    private static void addRTFNumberedPoint(StringBuilder rtf, String text) {
        rtf.append("\\pard\\li720\\f0\\fs24 ");
        // 直接添加格式化文本内容，避免重复的段落格式
        addRTFInlineFormattedText(rtf, text);
        rtf.append("\\par");
    }
    
    /**
     * 添加RTF格式化文本（支持粗体等）
     */
    private static void addRTFFormattedText(StringBuilder rtf, String text) {
        if (text == null || text.trim().isEmpty()) {
            rtf.append("\\pard\\f0\\fs24 \\par");
            return;
        }

        Timber.tag(TAG).d( "addRTFFormattedText - 输入文本: " + text);
        
        rtf.append("\\pard\\f0\\fs24 ");
        addRTFInlineFormattedText(rtf, text);
        rtf.append("\\par");
        
        Timber.tag(TAG).d( "addRTFFormattedText - 处理完成");
    }

    /**
     * 添加RTF内联格式化文本（不包含段落格式控制符）
     */
    private static void addRTFInlineFormattedText(StringBuilder rtf, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        Timber.tag(TAG).d( "addRTFInlineFormattedText - 输入文本: " + text);
        
        // 处理粗体 **text**
        Pattern boldPattern = Pattern.compile("\\*\\*(.*?)\\*\\*");
        Matcher boldMatcher = boldPattern.matcher(text);

        int lastEnd = 0;
        boolean hasFormatting = false;

        while (boldMatcher.find()) {
            hasFormatting = true;

            // 添加粗体前的普通文本
            if (boldMatcher.start() > lastEnd) {
                String beforeBold = text.substring(lastEnd, boldMatcher.start());
                Timber.tag(TAG).d( "添加粗体前文本: " + beforeBold);
                rtf.append(escapeRTFText(beforeBold));
            }

            // 添加粗体文本
            String boldText = boldMatcher.group(1);
            Timber.tag(TAG).d( "添加粗体文本: " + boldText);
            rtf.append("\\b ");
            rtf.append(escapeRTFText(boldText));
            rtf.append("\\b0 ");

            lastEnd = boldMatcher.end();
        }

        // 如果没有找到任何格式化标记，直接添加普通文本
        if (!hasFormatting) {
            Timber.tag(TAG).d( "添加普通文本: " + text);
            rtf.append(escapeRTFText(text));
        } else {
            // 添加剩余的普通文本（只有在有格式化标记时才执行）
            if (lastEnd < text.length()) {
                String remainingText = text.substring(lastEnd);
                Timber.tag(TAG).d( "添加剩余文本: " + remainingText);
                rtf.append(escapeRTFText(remainingText));
            }
        }
        
        Timber.tag(TAG).d( "addRTFInlineFormattedText - 处理完成");
    }
    
    /**
     * 保存RTF文档到文件
     */
    private static File saveRTFDocument(Context context, String rtfContent, String title) throws IOException {
        // 创建文件名
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = sanitizeFileName(title) + "_" + timestamp + ".rtf";

        // 获取外部存储目录
        File documentsDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports");
        if (!documentsDir.exists()) {
            documentsDir.mkdirs();
        }

        File file = new File(documentsDir, fileName);

        // 写入RTF文件 - 直接写入字节以确保编码正确
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // RTF文档应该使用ANSI编码，但我们已经在内容中使用了Unicode转义
            fos.write(rtfContent.getBytes(StandardCharsets.UTF_8));
        }

        Timber.tag(TAG).d( "RTF文档已保存到: " + file.getAbsolutePath());
        return file;
    }
    
    /**
     * 清理文件名中的非法字符
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "document";
        }
        
        // 移除或替换非法字符
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_")
                      .replaceAll("\\s+", "_")
                      .substring(0, Math.min(fileName.length(), 50));
    }
    
    /**
     * 打开导出的文档
     */
    public static void openDocument(Context context, File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_VIEW);

            // 根据文件扩展名设置MIME类型
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".rtf")) {
                intent.setDataAndType(uri, "application/rtf");
            } else if (fileName.endsWith(".docx")) {
                intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            } else {
                intent.setDataAndType(uri, "text/plain");
            }

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(intent, "打开文档"));
        } catch (Exception e) {
            Timber.tag(TAG).e( "打开文档失败", e);
        }
    }
    
    /**
     * 测试中文编码和特殊符号
     */
    public static void testChineseEncoding() {
        String testText = "测试中文编码：会议摘要、话题讨论、智能问答";
        String escaped = escapeRTFText(testText);
        Timber.tag(TAG).d( "中文测试 - 原文: " + testText);
        Timber.tag(TAG).d( "中文测试 - 转义后: " + escaped);
        
        // 测试特殊符号
        String specialText = "特殊符号测试：\u2022 \u2014 \u2013 \u201C \u201D \u2018 \u2019 \u2026 \u2605 \u2606 \u2713 \u2717";
        String escapedSpecial = escapeRTFText(specialText);
        Timber.tag(TAG).d( "特殊符号测试 - 原文: " + specialText);
        Timber.tag(TAG).d( "特殊符号测试 - 转义后: " + escapedSpecial);
        
        // 测试具体的中文字符编码
        testSpecificChineseChars();
    }
    
    /**
     * 测试具体的中文字符编码
     */
    private static void testSpecificChineseChars() {
        // 测试一些常见中文字符的Unicode值
        char[] testChars = {'测', '试', '中', '文', '编', '码', '会', '议', '摘', '要'};
        
        for (char c : testChars) {
            int codePoint = (int) c;
            String rtfEscape = getSpecialSymbolRTF(c);
            Timber.tag(TAG).d( "字符 '" + c + "' Unicode: " + codePoint + " RTF转义: " + rtfEscape);
        }
    }

    /**
     * 测试导出功能，检查是否存在内容重复
     */
    public static void testExportWithDuplicateContent(Context context) {
        // 创建包含重复内容的测试文本
        String testContent = "## 会议话题\n\n" +
                "1. 项目进度讨论\n" +
                "1. 项目进度讨论\n" +  // 重复行
                "2. 技术方案评审\n" +
                "- 前端架构设计\n" +
                "- 前端架构设计\n" +  // 重复行
                "- 后端API接口\n" +
                "3. 下一步计划\n\n" +
                "## 总结\n" +
                "本次会议讨论了重要议题。\n" +
                "本次会议讨论了重复议题。";  // 重复行

        Timber.tag(TAG).d( "测试导出功能 - 原始内容包含重复行");
        
        exportToWord(context, "测试导出", testContent, new ExportCallback() {
            @Override
            public void onSuccess(File file) {
                Timber.tag(TAG).d( "测试导出成功: " + file.getAbsolutePath());
            }

            @Override
            public void onError(String error) {
                Timber.tag(TAG).e( "测试导出失败: " + error);
            }
        });
    }

    /**
     * 测试简单内容导出，用于调试RTF格式问题
     */
    public static void testSimpleExport(Context context) {
        String simpleContent = "话题1:询问对方在干什么及为何出来慢";
        
        Timber.tag(TAG).d( "测试简单内容导出");
        
        exportToWord(context, "简单测试", simpleContent, new ExportCallback() {
            @Override
            public void onSuccess(File file) {
                Timber.tag(TAG).d( "简单测试导出成功: " + file.getAbsolutePath());
                
                // 尝试读取文件内容进行验证
                try {
                    java.nio.file.Path path = java.nio.file.Paths.get(file.getAbsolutePath());
                    byte[] bytes = java.nio.file.Files.readAllBytes(path);
                    String fileContent = new String(bytes, StandardCharsets.UTF_8);
                    Timber.tag(TAG).d( "文件实际内容: " + fileContent);
                } catch (Exception e) {
                    Timber.tag(TAG).e( "读取文件失败: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Timber.tag(TAG).e( "简单测试导出失败: " + error);
            }
        });
    }

    /**
     * 测试修复后的中文编码
     */
    public static void testFixedChineseEncoding(Context context) {
        Timber.tag(TAG).d( "=== 测试修复后的中文编码和bullet符号 ===");
        
        // 先测试bullet点格式化
        testBulletPointFormatting();
        
        // 测试原始问题中的文本，包含bullet点
        String testContent = "### 章节划分与摘要\n" +
                "- **章节**：此为会议仅有的一段内容。\n" +
                "- **摘要**：有人询问对方在做什么，并指责其出来得慢。\n\n" +
                "### 会议结论\n" +
                "此次简短交流未达成明确结果和共识。\n\n" +
                "### 后续待办\n" +
                "- 未确定后续需要完成的任务及负责人。\n" +
                "- 需要进一步讨论具体实施方案。";
        
        exportToWord(context, "会议摘要", testContent, new ExportCallback() {
            @Override
            public void onSuccess(File file) {
                Timber.tag(TAG).d( "✅ 修复测试成功: " + file.getAbsolutePath());
                
                // 验证文件内容
                try {
                    java.nio.file.Path path = java.nio.file.Paths.get(file.getAbsolutePath());
                    byte[] bytes = java.nio.file.Files.readAllBytes(path);
                    String fileContent = new String(bytes, StandardCharsets.UTF_8);
                    
                    // 检查是否包含正确的Unicode转义
                    if (fileContent.contains("\\u") && !fileContent.contains("\\u-")) {
                        Timber.tag(TAG).d( "✅ Unicode转义格式正确");
                    } else {
                        Timber.tag(TAG).w( "⚠️ Unicode转义可能仍有问题");
                    }
                    
                    // 检查是否包含正确的bullet符号
                    if (fileContent.contains("\\bullet")) {
                        Timber.tag(TAG).d( "✅ Bullet符号格式正确");
                    } else {
                        Timber.tag(TAG).w( "⚠️ Bullet符号可能有问题");
                    }
                    
                    // 显示部分内容用于验证
                    int previewLength = Math.min(500, fileContent.length());
                    Timber.tag(TAG).d( "文件内容预览: " + fileContent.substring(0, previewLength));
                    
                } catch (Exception e) {
                    Timber.tag(TAG).e( "读取测试文件失败: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Timber.tag(TAG).e( "❌ 修复测试失败: " + error);
            }
        });
    }

    /**
     * 测试bullet点格式化
     */
    public static void testBulletPointFormatting() {
        Timber.tag(TAG).d( "=== 测试bullet点格式化 ===");
        
        // 测试不同类型的bullet点
        String[] testLines = {
            "- **章节**：此为会议仅有的一段内容。",
            "- 普通bullet点内容",
            "* 星号bullet点内容",
            "- **粗体内容**：后面跟普通文本"
        };
        
        StringBuilder rtf = new StringBuilder();
        rtf.append("{\\rtf1\\ansi\\ansicpg936\\deff0\\deflang2052");
        rtf.append("{\\fonttbl{\\f0\\fnil\\fcharset134\\fprq2 SimSun;}}");
        rtf.append("\\viewkind4\\uc1\\pard");
        
        for (String line : testLines) {
            Timber.tag(TAG).d( "处理bullet行: " + line);
            if (line.startsWith("- ") || line.startsWith("* ")) {
                addRTFBulletPoint(rtf, line.substring(2));
            }
        }
        
        rtf.append("}");
        
        String result = rtf.toString();
        Timber.tag(TAG).d( "生成的RTF内容: " + result);
        
        // 验证是否包含正确的bullet符号
        if (result.contains("\\bullet")) {
            Timber.tag(TAG).d( "✅ Bullet符号正确生成");
        } else {
            Timber.tag(TAG).w("⚠️ 未找到bullet符号");
        }
    }

    /**
     * 导出结果回调接口
     */
    public interface ExportCallback {
        void onSuccess(File file);
        void onError(String error);
    }
}
