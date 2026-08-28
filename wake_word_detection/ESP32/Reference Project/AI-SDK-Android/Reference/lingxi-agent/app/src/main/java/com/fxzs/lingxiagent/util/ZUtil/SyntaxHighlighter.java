package com.fxzs.lingxiagent.util.ZUtil;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码语法高亮工具类
 * 支持多种编程语言的基础语法高亮
 */
public class SyntaxHighlighter {
    
    private final Context context;
    
    // 颜色定义
    private final int keywordColor;
    private final int stringColor;
    private final int commentColor;
    private final int numberColor;
    private final int operatorColor;
    
    public SyntaxHighlighter(Context context) {
        this.context = context;
        
        // 初始化颜色 - 浅色主题
        keywordColor = ContextCompat.getColor(context, R.color.code_keyword);
        stringColor = ContextCompat.getColor(context, R.color.code_string);
        commentColor = ContextCompat.getColor(context, R.color.code_comment);
        numberColor = ContextCompat.getColor(context, R.color.code_number);
        operatorColor = Color.parseColor("#D73A49"); // 操作符颜色 - 浅色主题
    }
    
    /**
     * 对代码进行语法高亮
     */
    public SpannableStringBuilder highlight(String code, String language) {
        if (code == null || code.isEmpty()) {
            return new SpannableStringBuilder("");
        }
        
        SpannableStringBuilder spannable = new SpannableStringBuilder(code);
        
        // 根据语言类型进行高亮
        switch (language.toLowerCase()) {
            case "java":
            case "kotlin":
                highlightJava(spannable);
                break;
            case "python":
                highlightPython(spannable);
                break;
            case "javascript":
            case "js":
            case "typescript":
            case "ts":
                highlightJavaScript(spannable);
                break;
            case "xml":
            case "html":
                highlightXml(spannable);
                break;
            case "json":
                highlightJson(spannable);
                break;
            case "sql":
                highlightSql(spannable);
                break;
            case "css":
                highlightCss(spannable);
                break;
            default:
                highlightGeneric(spannable);
                break;
        }
        
        return spannable;
    }
    
    /**
     * Java/Kotlin 语法高亮
     */
    private void highlightJava(SpannableStringBuilder spannable) {
        // Java关键字
        String[] keywords = {
            "public", "private", "protected", "static", "final", "abstract", "class", "interface",
            "extends", "implements", "import", "package", "if", "else", "for", "while", "do",
            "switch", "case", "default", "break", "continue", "return", "try", "catch", "finally",
            "throw", "throws", "new", "this", "super", "null", "true", "false", "void", "int",
            "long", "double", "float", "boolean", "char", "byte", "short", "String"
        };
        
        highlightKeywords(spannable, keywords);
        highlightStrings(spannable);
        highlightComments(spannable);
        highlightNumbers(spannable);
    }
    
    /**
     * Python 语法高亮
     */
    private void highlightPython(SpannableStringBuilder spannable) {
        String[] keywords = {
            "def", "class", "if", "elif", "else", "for", "while", "try", "except", "finally",
            "import", "from", "as", "return", "yield", "lambda", "with", "pass", "break",
            "continue", "and", "or", "not", "in", "is", "None", "True", "False", "self"
        };
        
        highlightKeywords(spannable, keywords);
        highlightStrings(spannable);
        highlightPythonComments(spannable);
        highlightNumbers(spannable);
    }
    
    /**
     * JavaScript 语法高亮
     */
    private void highlightJavaScript(SpannableStringBuilder spannable) {
        String[] keywords = {
            "function", "var", "let", "const", "if", "else", "for", "while", "do", "switch",
            "case", "default", "break", "continue", "return", "try", "catch", "finally",
            "throw", "new", "this", "typeof", "instanceof", "true", "false", "null",
            "undefined", "class", "extends", "import", "export", "async", "await"
        };
        
        highlightKeywords(spannable, keywords);
        highlightStrings(spannable);
        highlightComments(spannable);
        highlightNumbers(spannable);
    }
    
    /**
     * 通用语法高亮
     */
    private void highlightGeneric(SpannableStringBuilder spannable) {
        highlightStrings(spannable);
        highlightComments(spannable);
        highlightNumbers(spannable);
    }
    
    /**
     * 高亮关键字
     */
    private void highlightKeywords(SpannableStringBuilder spannable, String[] keywords) {
        for (String keyword : keywords) {
            Pattern pattern = Pattern.compile("\\b" + keyword + "\\b");
            Matcher matcher = pattern.matcher(spannable);
            while (matcher.find()) {
                spannable.setSpan(
                    new ForegroundColorSpan(keywordColor),
                    matcher.start(),
                    matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                spannable.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    matcher.start(),
                    matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
    }
    
    /**
     * 高亮字符串
     */
    private void highlightStrings(SpannableStringBuilder spannable) {
        // 双引号字符串
        Pattern doubleQuotePattern = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
        Matcher matcher = doubleQuotePattern.matcher(spannable);
        while (matcher.find()) {
            spannable.setSpan(
                new ForegroundColorSpan(stringColor),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        // 单引号字符串
        Pattern singleQuotePattern = Pattern.compile("'([^'\\\\]|\\\\.)*'");
        matcher = singleQuotePattern.matcher(spannable);
        while (matcher.find()) {
            spannable.setSpan(
                new ForegroundColorSpan(stringColor),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    
    /**
     * 高亮注释
     */
    private void highlightComments(SpannableStringBuilder spannable) {
        // 单行注释 //
        Pattern singleLinePattern = Pattern.compile("//.*$", Pattern.MULTILINE);
        Matcher matcher = singleLinePattern.matcher(spannable);
        while (matcher.find()) {
            spannable.setSpan(
                new ForegroundColorSpan(commentColor),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        // 多行注释 /* */
        Pattern multiLinePattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
        matcher = multiLinePattern.matcher(spannable);
        while (matcher.find()) {
            spannable.setSpan(
                new ForegroundColorSpan(commentColor),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    
    /**
     * Python注释高亮
     */
    private void highlightPythonComments(SpannableStringBuilder spannable) {
        Pattern pattern = Pattern.compile("#.*$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(spannable);
        while (matcher.find()) {
            spannable.setSpan(
                new ForegroundColorSpan(commentColor),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    
    /**
     * 高亮数字
     */
    private void highlightNumbers(SpannableStringBuilder spannable) {
        Pattern pattern = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");
        Matcher matcher = pattern.matcher(spannable);
        while (matcher.find()) {
            spannable.setSpan(
                new ForegroundColorSpan(numberColor),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    
    /**
     * XML/HTML 语法高亮
     */
    private void highlightXml(SpannableStringBuilder spannable) {
        // XML标签
        Pattern tagPattern = Pattern.compile("<[^>]+>");
        Matcher matcher = tagPattern.matcher(spannable);
        while (matcher.find()) {
            spannable.setSpan(
                new ForegroundColorSpan(keywordColor),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        highlightStrings(spannable);
        highlightComments(spannable);
    }
    
    /**
     * JSON 语法高亮
     */
    private void highlightJson(SpannableStringBuilder spannable) {
        // JSON键
        Pattern keyPattern = Pattern.compile("\"[^\"]+\"\\s*:");
        Matcher matcher = keyPattern.matcher(spannable);
        while (matcher.find()) {
            spannable.setSpan(
                new ForegroundColorSpan(keywordColor),
                matcher.start(),
                matcher.end() - 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        highlightStrings(spannable);
        highlightNumbers(spannable);
    }
    
    /**
     * SQL 语法高亮
     */
    private void highlightSql(SpannableStringBuilder spannable) {
        String[] keywords = {
            "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP",
            "ALTER", "TABLE", "INDEX", "VIEW", "DATABASE", "SCHEMA", "GRANT", "REVOKE",
            "COMMIT", "ROLLBACK", "TRANSACTION", "JOIN", "INNER", "LEFT", "RIGHT", "FULL",
            "ON", "AS", "AND", "OR", "NOT", "NULL", "TRUE", "FALSE", "DISTINCT", "ORDER",
            "BY", "GROUP", "HAVING", "LIMIT", "OFFSET"
        };
        
        highlightKeywords(spannable, keywords);
        highlightStrings(spannable);
        highlightComments(spannable);
        highlightNumbers(spannable);
    }
    
    /**
     * CSS 语法高亮
     */
    private void highlightCss(SpannableStringBuilder spannable) {
        // CSS属性
        Pattern propertyPattern = Pattern.compile("\\b[a-zA-Z-]+\\s*:");
        Matcher matcher = propertyPattern.matcher(spannable);
        while (matcher.find()) {
            spannable.setSpan(
                new ForegroundColorSpan(keywordColor),
                matcher.start(),
                matcher.end() - 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        highlightStrings(spannable);
        highlightComments(spannable);
        highlightNumbers(spannable);
    }
}
