package com.fxzs.lingxiagent.view.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.common.AIResponseView;

public class AIResponseViewDemo extends AppCompatActivity {
    
    private AIResponseView aiResponseView;
    private Button btnMarkdownDemo;
    private Button btnStreamDemo;
    private Button btnComplexDemo;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_response_demo);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        aiResponseView = findViewById(R.id.ai_response_view);
        btnMarkdownDemo = findViewById(R.id.btn_markdown_demo);
        btnStreamDemo = findViewById(R.id.btn_stream_demo);
        btnComplexDemo = findViewById(R.id.btn_complex_demo);
    }
    
    private void setupListeners() {
        btnMarkdownDemo.setOnClickListener(v -> showMarkdownDemo());
        btnStreamDemo.setOnClickListener(v -> showStreamDemo());
        btnComplexDemo.setOnClickListener(v -> showComplexDemo());
        
        aiResponseView.setOnRefreshClickListener(() -> {
            // Refresh action
            aiResponseView.setContent("刷新中...");
        });
    }
    
    private void showMarkdownDemo() {
        String markdownContent = "# AI响应示例\n\n" +
                "这是一个**Markdown**格式的响应示例。\n\n" +
                "## 特性列表\n" +
                "- 支持**粗体**文本\n" +
                "- 支持*斜体*文本\n" +
                "- 支持`代码块`\n" +
                "- 支持[链接](https://example.com)\n\n" +
                "### 代码示例\n" +
                "```java\n" +
                "public void hello() {\n" +
                "    System.out.println(\"Hello World!\");\n" +
                "}\n" +
                "```\n\n" +
                "### 表格示例\n" +
                "| 功能 | 状态 |\n" +
                "|------|------|\n" +
                "| Markdown渲染 | ✅ |\n" +
                "| 流式输出 | ✅ |\n" +
                "| 语音播报 | ✅ |";
        
        aiResponseView.setAsThinkingResponse(markdownContent, 2, 
            "正在分析您的请求...\n思考如何最好地展示Markdown功能...");
    }
    
    private void showStreamDemo() {
        // Start stream mode
        aiResponseView.startStreamMode();
        
        String fullText = "这是一个流式输出的演示。AI正在逐字生成响应内容，" +
                "就像ChatGPT一样实时显示输出。\n\n" +
                "流式输出的优势：\n" +
                "1. 用户体验更好，不需要等待完整响应\n" +
                "2. 可以实时看到AI的思考过程\n" +
                "3. 支持长文本的渐进式显示\n\n" +
                "**注意**：流式输出同样支持Markdown格式！";
        
        // Simulate streaming
        streamText(fullText, 0);
    }
    
    private void streamText(String text, int index) {
        if (index >= text.length()) {
            // End stream mode
            aiResponseView.endStreamMode();
            return;
        }
        
        // Append next chunk (5-10 characters)
        int chunkSize = 5 + (int)(Math.random() * 5);
        int endIndex = Math.min(index + chunkSize, text.length());
        String chunk = text.substring(index, endIndex);
        
        aiResponseView.appendStreamContent(chunk);
        
        // Schedule next chunk
        handler.postDelayed(() -> streamText(text, endIndex), 50 + (int)(Math.random() * 100));
    }
    
    private void showComplexDemo() {
        String thinkingText = "让我分析一下这个复杂的任务...\n\n" +
                "1. 首先，我需要理解问题的核心\n" +
                "2. 然后，制定解决方案\n" +
                "3. 最后，给出详细的实施步骤";
        
        String content = "## 项目实施计划\n\n" +
                "基于您的需求，我制定了以下实施计划：\n\n" +
                "### 第一阶段：需求分析\n" +
                "- 深入了解业务需求\n" +
                "- 技术可行性评估\n" +
                "- 资源评估\n\n" +
                "### 第二阶段：系统设计\n" +
                "```\n" +
                "├── 前端架构\n" +
                "│   ├── React/Vue选型\n" +
                "│   └── 组件设计\n" +
                "├── 后端架构\n" +
                "│   ├── 微服务设计\n" +
                "│   └── 数据库设计\n" +
                "└── 部署架构\n" +
                "    └── K8s容器化\n" +
                "```\n\n" +
                "### 第三阶段：开发实施\n" +
                "| 模块 | 工期 | 负责人 |\n" +
                "|------|------|--------|\n" +
                "| 用户模块 | 2周 | 张三 |\n" +
                "| 订单模块 | 3周 | 李四 |\n" +
                "| 支付模块 | 2周 | 王五 |";
        
        aiResponseView.setAsThinkingResponse(content, 5, thinkingText);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiResponseView != null) {
            aiResponseView.release();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}