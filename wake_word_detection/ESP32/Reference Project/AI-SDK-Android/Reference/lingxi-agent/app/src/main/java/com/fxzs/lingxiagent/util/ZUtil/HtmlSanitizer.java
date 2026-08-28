package com.fxzs.lingxiagent.util.ZUtil;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;

import java.util.regex.Pattern;

/**
 * HTML内容安全处理工具
 * 用于防止恶意HTML内容导致的崩溃
 */
public class HtmlSanitizer {
    
    // 危险的HTML标签
    private static final Pattern DANGEROUS_TAGS = Pattern.compile(
        "<(script|iframe|object|embed|applet|form|input|button|select|textarea|style|link|meta|base|frame|frameset)[^>]*>.*?</\\1>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // 危险的属性
    private static final Pattern DANGEROUS_ATTRS = Pattern.compile(
        "\\s*(on\\w+|javascript:|data:text/html)\\s*=",
        Pattern.CASE_INSENSITIVE
    );
    
    // 允许的安全标签
    private static final String[] ALLOWED_TAGS = {
        "p", "br", "div", "span", "h1", "h2", "h3", "h4", "h5", "h6",
        "strong", "b", "em", "i", "u", "strike", "del", "ins",
        "ul", "ol", "li", "dl", "dt", "dd",
        "table", "thead", "tbody", "tr", "td", "th",
        "a", "img", "blockquote", "pre", "code"
    };
    
    /**
     * 清理HTML内容，移除危险元素
     */
    public static String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        
        // 移除危险标签
        html = DANGEROUS_TAGS.matcher(html).replaceAll("");
        
        // 移除危险属性
        html = DANGEROUS_ATTRS.matcher(html).replaceAll("");
        
        // 转义特殊字符
        html = html.replace("<script", "&lt;script")
                   .replace("javascript:", "")
                   .replace("onerror=", "")
                   .replace("onclick=", "");
        
        return html;
    }
    
    /**
     * 安全地转换HTML为Spanned
     */
    public static Spanned fromHtml(String html) {
        String sanitized = sanitize(html);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(sanitized, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(sanitized);
        }
    }
    
    /**
     * 检测内容是否包含HTML
     */
    public static boolean containsHtml(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.contains("<") && text.contains(">") && 
               (text.contains("<p>") || text.contains("<div>") || 
                text.contains("<table>") || text.contains("<br"));
    }
}