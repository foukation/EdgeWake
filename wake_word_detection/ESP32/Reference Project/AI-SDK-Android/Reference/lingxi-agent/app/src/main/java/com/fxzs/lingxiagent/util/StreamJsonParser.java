package com.fxzs.lingxiagent.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import timber.log.Timber;

public class StreamJsonParser {
    private static final String TAG = "StreamJsonParser";
    
    /**
     * 尝试修复不完整的JSON字符串
     * 参考JS的tryFixJSON实现
     */
    public static String tryFixJSON(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return jsonStr;
        }
        
        Stack<Character> stack = new Stack<>();
        boolean inString = false;
        boolean escape = false;
        
        // 分析JSON结构
        for (int i = 0; i < jsonStr.length(); i++) {
            char ch = jsonStr.charAt(i);
            
            if (escape) {
                escape = false;
                continue;
            }
            
            if (ch == '\\') {
                escape = true;
                continue;
            }
            
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (ch == '{' || ch == '[') {
                    stack.push(ch);
                } else if (ch == '}' && !stack.isEmpty() && stack.peek() == '{') {
                    stack.pop();
                } else if (ch == ']' && !stack.isEmpty() && stack.peek() == '[') {
                    stack.pop();
                }
            }
        }
        
        // 补全缺失的括号
        StringBuilder fixed = new StringBuilder(jsonStr);
        while (!stack.isEmpty()) {
            char last = stack.pop();
            fixed.append(last == '{' ? '}' : ']');
        }
        
        return fixed.toString();
    }
    
    /**
     * 解析SSE数据行
     * 返回解析出的内容或null
     */
    public static String parseSSEDataLine(String line) {
        if (line == null) {
            return null;
        }
        
        // 处理两种格式：
        // 1. 标准SSE格式: "data: {content}"
        // 2. 直接的data:开头格式: "data:{content}"
        String data = null;
        if (line.startsWith("data: ")) {
            data = line.substring(6).trim();
        } else if (line.startsWith("data:")) {
            data = line.substring(5).trim();
        } else {
            return null;
        }
        
        // 跳过特殊标记
        if (data.equals("[DONE]") || data.isEmpty()) {
            return null;
        }
        
        // 尝试解析JSON格式的chunk
        try {
            JSONObject chunk = new JSONObject(data);
            
            // 处理特定格式: {"code":0,"data":"content","msg":""}
            if (chunk.has("code") && chunk.has("data") && chunk.has("msg")) {
                int code = chunk.getInt("code");
                if (code == 0) {
                    return chunk.getString("data");
                }
            }
            
            // 其他格式兼容
            if (chunk.has("content")) {
                return chunk.getString("content");
            } else if (chunk.has("text")) {
                return chunk.getString("text");
            } else if (chunk.has("delta")) {
                // 处理OpenAI格式的响应
                JSONObject delta = chunk.getJSONObject("delta");
                if (delta.has("content")) {
                    return delta.getString("content");
                }
            }
        } catch (JSONException e) {
            // 不是JSON格式，直接返回原始数据
            Timber.tag(TAG).d( "非JSON格式数据: " + data);
        }
        
        return data;
    }
    
    /**
     * 解析会议摘要的结构化内容
     * 支持流式累积和部分解析
     */
    public static class MeetingSummaryParser {
        private StringBuilder contentBuffer = new StringBuilder();
        private String currentSection = "";
        private List<String> currentList = new ArrayList<>();
        
        /**
         * 添加新的内容片段
         */
        public void addContent(String chunk) {
            if (chunk != null) {
                contentBuffer.append(chunk);
            }
        }
        
        /**
         * 获取当前累积的完整内容
         */
        public String getFullContent() {
            return contentBuffer.toString();
        }
        
        /**
         * 尝试解析当前内容为结构化数据
         * 返回：[summary, keyPoints[], topics[], actionItems[], participants[]]
         */
        public ParsedSummary parseStructuredContent() {
            String content = contentBuffer.toString();
            ParsedSummary result = new ParsedSummary();
            
            // 尝试解析JSON格式
            try {
                // 先尝试修复可能不完整的JSON
                String fixedJson = tryFixJSON(content);
                JSONObject json = new JSONObject(fixedJson);
                
                result.summary = json.optString("summary", "");
                result.keyPoints = parseJsonArray(json, "keyPoints");
                result.topics = parseJsonArray(json, "topics");
                result.actionItems = parseJsonArray(json, "actionItems");
                result.participants = parseJsonArray(json, "participants");
                
                return result;
            } catch (JSONException e) {
                // 不是JSON格式，尝试解析文本格式
                Timber.tag(TAG).d( "尝试解析文本格式内容");
            }
            
            // 解析文本格式的会议摘要
            if (content.contains("会议摘要") || content.contains("会议待办")) {
                String[] sections = content.split("会议待办");
                
                if (sections.length > 0) {
                    // 第一部分是摘要
                    String summaryPart = sections[0].replace("会议摘要", "").trim();
                    result.summary = summaryPart;
                    
                    // 解析待办事项
                    if (sections.length > 1) {
                        String todosPart = sections[1].trim();
                        result.actionItems = parseNumberedList(todosPart);
                    }
                }
            } else if (content.contains("## ") || content.contains("### ")) {
                // Markdown格式解析
                result = parseMarkdownFormat(content);
            } else {
                // 没有结构化标记，将整个内容作为摘要
                result.summary = content;
            }
            
            return result;
        }
        
        /**
         * 解析编号列表（如：1. xxx, 2. xxx）
         */
        private List<String> parseNumberedList(String text) {
            List<String> items = new ArrayList<>();
            String[] lines = text.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                // 匹配数字开头的行项目
                if (line.matches("^\\d+\\.\\s+.*")) {
                    String item = line.replaceFirst("^\\d+\\.\\s+", "").trim();
                    if (!item.isEmpty()) {
                        items.add(item);
                    }
                } else if (line.matches("^[-*]\\s+.*")) {
                    // 也支持 - 或 * 开头的列表
                    String item = line.replaceFirst("^[-*]\\s+", "").trim();
                    if (!item.isEmpty()) {
                        items.add(item);
                    }
                }
            }
            
            return items;
        }
        
        /**
         * 解析Markdown格式的内容
         */
        private ParsedSummary parseMarkdownFormat(String content) {
            ParsedSummary result = new ParsedSummary();
            
            // 移除<meeting_record>标签
            content = content.replaceAll("</?meeting_record>", "").trim();
            
            String[] lines = content.split("\n");
            String currentSection = "";
            StringBuilder sectionContent = new StringBuilder();
            boolean inMeetingContent = false;
            
            for (String line : lines) {
                line = line.trim();
                
                // 检测会议记录的开始
                if (line.contains("产品需求评审会议") || line.contains("会议记录")) {
                    inMeetingContent = true;
                }
                
                if (line.startsWith("##") || line.startsWith("###") || line.startsWith("####")) {
                    // 保存上一个section的内容
                    if (!currentSection.isEmpty() && sectionContent.length() > 0) {
                        processSection(result, currentSection, sectionContent.toString());
                        sectionContent = new StringBuilder();
                    }
                    
                    // 开始新的section
                    currentSection = line.replaceAll("^#+\\s*", "").trim();
                } else if (!line.isEmpty() && !line.equals("-")) {
                    if (sectionContent.length() > 0) {
                        sectionContent.append("\n");
                    }
                    sectionContent.append(line);
                }
            }
            
            // 处理最后一个section
            if (!currentSection.isEmpty() && sectionContent.length() > 0) {
                processSection(result, currentSection, sectionContent.toString());
            }
            
            // 如果没有解析到结构化内容，将整个内容作为摘要
            if (result.summary.isEmpty() && inMeetingContent) {
                result.summary = content;
            }
            
            return result;
        }
        
        /**
         * 处理Markdown section内容
         */
        private void processSection(ParsedSummary result, String sectionTitle, String content) {
            String titleLower = sectionTitle.toLowerCase();
            
            if (titleLower.contains("摘要") || titleLower.contains("summary") || titleLower.contains("结论")) {
                result.summary = content.trim();
            } else if (titleLower.contains("要点") || titleLower.contains("points") || titleLower.contains("关键")) {
                result.keyPoints = parseNumberedList(content);
            } else if (titleLower.contains("主题") || titleLower.contains("topic") || titleLower.contains("议题")) {
                result.topics = parseNumberedList(content);
                // 特殊处理：如果内容包含议题1、议题2等，解析它们
                if (content.contains("议题")) {
                    result.topics = parseTopicsFromContent(content);
                }
            } else if (titleLower.contains("待办") || titleLower.contains("action") || titleLower.contains("任务") || titleLower.contains("后续")) {
                result.actionItems = parseNumberedList(content);
                // 特殊处理：解析人员任务分配
                if (content.contains("：")) {
                    result.actionItems = parseTaskAssignments(content);
                }
            } else if (titleLower.contains("参与") || titleLower.contains("participant") || titleLower.contains("人员") || titleLower.contains("与会")) {
                // 解析与会人员
                result.participants = parseParticipants(content);
            } else if (titleLower.contains("基本信息")) {
                // 从基本信息中提取参与人员
                String[] lines = content.split("\n");
                for (String line : lines) {
                    if (line.contains("与会人员") || line.contains("参与人员")) {
                        result.participants = parseParticipants(line);
                    }
                }
            }
        }
        
        /**
         * 解析议题内容
         */
        private List<String> parseTopicsFromContent(String content) {
            List<String> topics = new ArrayList<>();
            String[] lines = content.split("\n");
            StringBuilder currentTopic = new StringBuilder();
            String currentTopicTitle = "";
            
            for (String line : lines) {
                if (line.matches(".*议题\\s*\\d+.*[:：].*")) {
                    // 保存上一个议题
                    if (!currentTopicTitle.isEmpty()) {
                        topics.add(currentTopicTitle + (currentTopic.length() > 0 ? ": " + currentTopic.toString() : ""));
                    }
                    // 开始新议题
                    currentTopicTitle = line.trim();
                    currentTopic = new StringBuilder();
                } else if (!line.trim().isEmpty() && !currentTopicTitle.isEmpty()) {
                    if (currentTopic.length() > 0) {
                        currentTopic.append(" ");
                    }
                    currentTopic.append(line.trim());
                }
            }
            
            // 保存最后一个议题
            if (!currentTopicTitle.isEmpty()) {
                topics.add(currentTopicTitle + (currentTopic.length() > 0 ? ": " + currentTopic.toString() : ""));
            }
            
            return topics.isEmpty() ? parseNumberedList(content) : topics;
        }
        
        /**
         * 解析任务分配
         */
        private List<String> parseTaskAssignments(String content) {
            List<String> tasks = new ArrayList<>();
            String[] lines = content.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                if (line.contains("：") && (line.contains("前") || line.contains("完成"))) {
                    // 格式如：张三：本周五前完成需求文档撰写
                    tasks.add(line);
                } else if (line.startsWith("-") || line.matches("^\\d+\\..*")) {
                    String task = line.replaceFirst("^[-\\d]+\\.?\\s*", "").trim();
                    if (!task.isEmpty()) {
                        tasks.add(task);
                    }
                }
            }
            
            return tasks;
        }
        
        /**
         * 解析参与人员
         */
        private List<String> parseParticipants(String content) {
            List<String> participants = new ArrayList<>();
            
            // 移除标签前缀
            content = content.replaceAll(".*与会人员.*[:：]", "").trim();
            content = content.replaceAll(".*参与人员.*[:：]", "").trim();
            
            // 按常见分隔符分割
            String[] parts = content.split("[、，,；;]");
            for (String part : parts) {
                part = part.trim();
                if (!part.isEmpty() && !part.equals("-")) {
                    // 提取人名（可能包含角色说明）
                    if (part.contains("（") && part.contains("）")) {
                        // 格式如：张三（产品经理）
                        participants.add(part);
                    } else if (part.matches("[\u4e00-\u9fa5]{2,4}")) {
                        // 纯中文名字
                        participants.add(part);
                    } else if (!part.isEmpty()) {
                        participants.add(part);
                    }
                }
            }
            
            return participants;
        }
        
        /**
         * 从JSON对象中解析数组
         */
        private List<String> parseJsonArray(JSONObject json, String key) {
            List<String> result = new ArrayList<>();
            try {
                if (json.has(key)) {
                    JSONArray array = json.getJSONArray(key);
                    for (int i = 0; i < array.length(); i++) {
                        result.add(array.getString(i));
                    }
                }
            } catch (JSONException e) {
                Timber.tag(TAG).e( "解析JSON数组失败: " + key +  e);
            }
            return result;
        }
    }
    
    /**
     * 解析后的摘要结构
     */
    public static class ParsedSummary {
        public String summary = "";
        public List<String> keyPoints = new ArrayList<>();
        public List<String> topics = new ArrayList<>();
        public List<String> actionItems = new ArrayList<>();
        public List<String> participants = new ArrayList<>();
    }
}