package com.fxzs.lingxiagent.util.ZUtil;

import android.content.Context;
import android.widget.TextView;

import timber.log.Timber;

/**
 * 测试代码高亮功能是否正常工作
 */
public class CodeHighlightTest {
    private static final String TAG = "CodeHighlightTest";
    
    /**
     * 测试不同语言的代码高亮
     */
    public static void testCodeHighlighting(Context context, TextView textView) {
        String testMarkdown = "# 代码高亮测试\n\n" +
                "Java代码:\n" +
                "```java\n" +
                "public class Hello {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello World!\");\n" +
                "    }\n" +
                "}\n" +
                "```\n\n" +
                "JavaScript代码:\n" +
                "```javascript\n" +
                "function hello() {\n" +
                "    console.log('Hello World!');\n" +
                "    return true;\n" +
                "}\n" +
                "```\n\n" +
                "Python代码:\n" +
                "```python\n" +
                "def hello():\n" +
                "    print('Hello World!')\n" +
                "    return True\n" +
                "```";
        
        Timber.tag(TAG).d( "开始测试代码高亮功能");
        
        try {
            // 使用MarkdownUtils渲染测试代码
            MarkdownUtils.renderSmart(context, testMarkdown, textView);
            Timber.tag(TAG).d( "代码高亮测试完成 - 成功");
        } catch (Exception e) {
            Timber.tag(TAG).e( "代码高亮测试失败", e);
        }
    }
    
    /**
     * 简单测试单个语言
     */
    public static void testSingleLanguage(Context context, TextView textView) {
        String javaCode = "```java\n" +
                "public class Test {\n" +
                "    private String message = \"Hello\";\n" +
                "    \n" +
                "    public void display() {\n" +
                "        Timber.i(message);\n" +
                "    }\n" +
                "}\n" +
                "```";
        
        Timber.tag(TAG).d( "测试Java代码高亮");
        MarkdownUtils.renderSmart(context, javaCode, textView);
    }
}