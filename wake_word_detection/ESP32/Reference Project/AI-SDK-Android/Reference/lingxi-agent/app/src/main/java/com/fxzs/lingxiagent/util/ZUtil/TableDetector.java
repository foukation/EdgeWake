package com.fxzs.lingxiagent.util.ZUtil;

import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * 表格检测工具类
 * 用于检测Markdown文本中是否包含表格
 */
public class TableDetector {
    private static final String TAG = "TableDetector";
    
    // Markdown表格的正则表达式模式
    private static final Pattern MARKDOWN_TABLE_PATTERN = Pattern.compile(
        "(?m)^\\s*\\|.*\\|\\s*$\\s*^\\s*\\|\\s*:?-+:?\\s*(?:\\|\\s*:?-+:?\\s*)*\\|\\s*$",
        Pattern.MULTILINE
    );
    
    // 简单的表格行模式（至少包含两个|符号的行）
    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile(
        "(?m)^\\s*\\|.*\\|.*\\|.*$"
    );
    
    // HTML表格模式
    private static final Pattern HTML_TABLE_PATTERN = Pattern.compile(
        "<table[^>]*>.*?</table>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    /**
     * 检测文本中是否包含表格
     * @param text 要检测的文本
     * @return true如果包含表格，false否则
     */
    public static boolean containsTable(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // 检测Markdown表格
        if (containsMarkdownTable(text)) {
            Timber.tag(TAG).d( "Detected Markdown table");
            return true;
        }
        
        // 检测HTML表格
        if (containsHtmlTable(text)) {
            Timber.tag(TAG).d( "Detected HTML table");
            return true;
        }
        
        return false;
    }
    
    /**
     * 检测是否包含Markdown格式的表格
     */
    public static boolean containsMarkdownTable(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // 首先使用严格的表格模式检测
        if (MARKDOWN_TABLE_PATTERN.matcher(text).find()) {
            return true;
        }
        
        // 如果严格模式没有匹配，使用宽松模式
        // 检查是否有多行包含表格分隔符
        String[] lines = text.split("\n");
        int tableRowCount = 0;
        boolean hasSeparator = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 检查是否是表格分隔行（如 |---|---|）
            if (line.matches("^\\s*\\|\\s*:?-+:?\\s*(?:\\|\\s*:?-+:?\\s*)*\\|\\s*$")) {
                hasSeparator = true;
                continue;
            }
            
            // 检查是否是表格数据行（至少包含两个|）
            if (TABLE_ROW_PATTERN.matcher(line).matches()) {
                tableRowCount++;
            }
        }
        
        // 如果有分隔符且至少有一行数据，或者有多行表格数据，则认为是表格
        return (hasSeparator && tableRowCount >= 1) || tableRowCount >= 2;
    }
    
    /**
     * 检测是否包含HTML格式的表格
     */
    public static boolean containsHtmlTable(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        return HTML_TABLE_PATTERN.matcher(text).find();
    }
    
    /**
     * 获取表格类型
     */
    public static TableType getTableType(String text) {
        if (containsHtmlTable(text)) {
            return TableType.HTML;
        } else if (containsMarkdownTable(text)) {
            return TableType.MARKDOWN;
        } else {
            return TableType.NONE;
        }
    }
    
    /**
     * 表格类型枚举
     */
    public enum TableType {
        NONE,       // 无表格
        MARKDOWN,   // Markdown表格
        HTML        // HTML表格
    }
    
    /**
     * 提取表格内容（用于调试）
     */
    public static String extractTableContent(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        StringBuilder tableContent = new StringBuilder();
        String[] lines = text.split("\n");
        boolean inTable = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                if (inTable) {
                    break; // 表格结束
                }
                continue;
            }
            
            // 检查是否是表格行
            if (line.contains("|")) {
                if (!inTable) {
                    inTable = true;
                }
                tableContent.append(line).append("\n");
            } else if (inTable) {
                break; // 表格结束
            }
        }
        
        return tableContent.toString();
    }

    /**
     * 测试方法 - 用于验证表格检测功能
     */
    public static void testTableDetection() {
        Timber.tag(TAG).d( "=== 开始表格检测测试 ===");

        // 测试用例1：标准Markdown表格
        String test1 = "| 姓名 | 年龄 | 城市 |\n|------|------|------|\n| 张三 | 25 | 北京 |\n| 李四 | 30 | 上海 |";
        boolean result1 = containsTable(test1);
        Timber.tag(TAG).d( "Test 1 (Standard table): " + result1);
        Timber.tag(TAG).d( "Test 1 content: " + test1.replace("\n", "\\n"));

        // 测试用例2：HTML表格
        String test2 = "<table><tr><td>数据1</td><td>数据2</td></tr></table>";
        boolean result2 = containsTable(test2);
        Timber.tag(TAG).d( "Test 2 (HTML table): " + result2);

        // 测试用例3：普通文本
        String test3 = "这是一段普通的文本，没有表格。";
        boolean result3 = containsTable(test3);
        Timber.tag(TAG).d( "Test 3 (Normal text): " + result3);

        // 测试用例4：简单表格（无分隔符）
        String test4 = "| 列1 | 列2 |\n| 数据1 | 数据2 |\n| 数据3 | 数据4 |";
        boolean result4 = containsTable(test4);
        Timber.tag(TAG).d( "Test 4 (Simple table): " + result4);
        Timber.tag(TAG).d( "Test 4 content: " + test4.replace("\n", "\\n"));

        Timber.tag(TAG).d( "=== 表格检测测试完成 ===");
    }
}
