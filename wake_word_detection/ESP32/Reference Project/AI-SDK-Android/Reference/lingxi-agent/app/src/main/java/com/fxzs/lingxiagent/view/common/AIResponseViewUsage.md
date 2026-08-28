# AIResponseView 使用指南

## 概述
AIResponseView 是一个优化后的AI响应视图组件，现已支持：
- Markdown格式渲染
- 流式输出模式
- 思维链展示
- 语音播报
- 一键复制

## 新增功能

### 1. Markdown 渲染
组件默认开启Markdown渲染，支持以下格式：
- 标题（# ## ###）
- 粗体（**text**）、斜体（*text*）
- 代码块（```code```）
- 列表（- item）
- 表格
- 链接
- 图片

### 2. 流式输出模式
支持像ChatGPT一样的实时流式输出效果。

## 使用示例

### 基础使用
```java
AIResponseView aiResponseView = findViewById(R.id.ai_response_view);

// 设置简单响应
aiResponseView.setAsSimpleResponse("标题", "这是一段响应内容");

// 设置带思维过程的响应
aiResponseView.setAsThinkingResponse("响应内容", 3, "思考过程文本");
```

### Markdown 渲染
```java
// Markdown内容会自动渲染
String markdownContent = "# 标题\n\n**粗体文本**\n\n- 列表项1\n- 列表项2";
aiResponseView.setContent(markdownContent);

// 可以控制是否启用Markdown
aiResponseView.setMarkdownEnabled(false); // 禁用Markdown渲染
```

### 流式输出
```java
// 开始流式输出
aiResponseView.startStreamMode();

// 逐步添加内容
aiResponseView.appendStreamContent("正在");
aiResponseView.appendStreamContent("生成");
aiResponseView.appendStreamContent("响应...");

// 结束流式输出
aiResponseView.endStreamMode();

// 或者使用便捷方法
aiResponseView.setContentStreaming("部分内容", false); // 继续流式
aiResponseView.setContentStreaming("完整内容", true);  // 结束流式
```

### 完整示例
```java
public class ChatActivity extends AppCompatActivity {
    private AIResponseView aiResponseView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        aiResponseView = findViewById(R.id.ai_response_view);
        
        // 设置刷新监听
        aiResponseView.setOnRefreshClickListener(() -> {
            regenerateResponse();
        });
        
        // 模拟AI响应
        simulateAIResponse();
    }
    
    private void simulateAIResponse() {
        // 开始流式输出
        aiResponseView.startStreamMode();
        
        String fullResponse = "## 解决方案\n\n" +
            "基于您的问题，我建议：\n\n" +
            "1. **第一步**：分析需求\n" +
            "2. **第二步**：设计方案\n" +
            "3. **第三步**：实施计划\n\n" +
            "```java\n" +
            "// 示例代码\n" +
            "public void example() {\n" +
            "    System.out.println(\"Hello\");\n" +
            "}\n" +
            "```";
        
        // 模拟逐字输出
        streamResponse(fullResponse, 0);
    }
    
    private void streamResponse(String text, int index) {
        if (index >= text.length()) {
            aiResponseView.endStreamMode();
            return;
        }
        
        int chunkSize = Math.min(10, text.length() - index);
        String chunk = text.substring(index, index + chunkSize);
        aiResponseView.appendStreamContent(chunk);
        
        new Handler().postDelayed(() -> {
            streamResponse(text, index + chunkSize);
        }, 50);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        aiResponseView.release();
    }
}
```

## 注意事项

1. **性能优化**：流式输出时有50ms的节流，避免频繁更新UI
2. **内存管理**：Activity销毁时记得调用`release()`方法
3. **Markdown渲染**：复杂的Markdown可能需要时间渲染，建议在流式结束后再渲染
4. **线程安全**：所有UI更新都在主线程执行

## 技术实现

- **Markdown渲染**：使用Markwon库（4.6.2）
- **流式输出**：使用Handler延迟更新，StringBuilder缓存
- **图片加载**：集成Glide支持Markdown中的图片
- **表格支持**：使用TablePlugin扩展