package com.fxzs.lingxiagent.util;

import com.fxzs.lingxiagent.model.ppt.dto.OutlineItem;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PPT大纲解析工具类
 * 用于解析流式返回的大纲数据
 */
public class PptOutlineParser {
    
    private static final String TAG = "PptOutlineParser";
    private static final Gson gson = new Gson();
    
    /**
     * 解析流式大纲数据
     * @param streamData 流式数据字符串
     * @return 解析后的大纲内容
     */
    public static String parseStreamData(String streamData) {
        if (streamData == null || streamData.isEmpty()) {
            return "";
        }
        
        StringBuilder contentBuilder = new StringBuilder();
        String[] lines = streamData.split("\n");
        
        for (String line : lines) {
            if (line.startsWith("data:")) {
                String jsonData = line.substring(5).trim();
                if (!jsonData.isEmpty() && !jsonData.equals("[DONE]")) {
                    try {
                        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                        String content = extractContentFromJson(jsonObject);
                        if (content != null && !content.isEmpty()) {
                            contentBuilder.append(content);
                        }
                    } catch (Exception e) {
                        // 忽略解析错误，继续处理下一行
                    }
                }
            }
        }
        
        return contentBuilder.toString();
    }
    
    /**
     * 从JSON对象中提取内容
     */
    private static String extractContentFromJson(JsonObject jsonObject) {
        try {
            if (jsonObject.has("result")) {
                JsonObject result = jsonObject.getAsJsonObject("result");
                if (result.has("output")) {
                    JsonObject output = result.getAsJsonObject("output");
                    if (output.has("content")) {
                        return output.get("content").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return "";
    }
    
    /**
     * 将大纲文本解析为OutlineItem列表
     * @param outlineText 大纲文本
     * @return OutlineItem列表
     */
    public static List<OutlineItem> parseOutlineText(String outlineText) {
        List<OutlineItem> outlineItems = new ArrayList<>();
        
        if (outlineText == null || outlineText.isEmpty()) {
            return outlineItems;
        }
        
        // 尝试解析XML格式的大纲
        List<OutlineItem> xmlItems = parseXmlOutline(outlineText);
        if (!xmlItems.isEmpty()) {
            return xmlItems;
        }
        
        // 如果不是XML格式，尝试解析普通文本格式
        return parseTextOutline(outlineText);
    }
    
    /**
     * 解析XML格式的大纲
     */
    private static List<OutlineItem> parseXmlOutline(String outlineText) {
        List<OutlineItem> items = new ArrayList<>();
        
        // 查找 <ppt_outline> 标签内的内容
        Pattern outlinePattern = Pattern.compile("<ppt_outline>(.*?)</ppt_outline>", Pattern.DOTALL);
        Matcher outlineMatcher = outlinePattern.matcher(outlineText);
        
        if (outlineMatcher.find()) {
            String outlineContent = outlineMatcher.group(1);
            
            // 解析标题和内容
            Pattern titlePattern = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL);
            Pattern contentPattern = Pattern.compile("<content>(.*?)</content>", Pattern.DOTALL);
            
            Matcher titleMatcher = titlePattern.matcher(outlineContent);
            Matcher contentMatcher = contentPattern.matcher(outlineContent);
            
            String title = "";
            String content = "";
            
            if (titleMatcher.find()) {
                title = titleMatcher.group(1).trim();
            }
            
            if (contentMatcher.find()) {
                content = contentMatcher.group(1).trim();
            }
            
            // 解析内容中的章节
            if (!content.isEmpty()) {
                items.addAll(parseContentSections(content));
            }
            
            // 如果没有解析到章节，创建一个默认项
            if (items.isEmpty() && !title.isEmpty()) {
                OutlineItem item = new OutlineItem(title, content);
                items.add(item);
            }
        }
        
        return items;
    }
    
    /**
     * 解析内容中的章节
     */
    private static List<OutlineItem> parseContentSections(String content) {
        List<OutlineItem> items = new ArrayList<>();
        
        // 按行分割内容
        String[] lines = content.split("\n");
        OutlineItem currentItem = null;
        StringBuilder currentContent = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            
            // 检查是否是章节标题（以数字开头或特殊标记）
            if (isChapterTitle(line)) {
                // 保存前一个项目
                if (currentItem != null) {
                    currentItem.setContent(currentContent.toString().trim());
                    items.add(currentItem);
                }
                
                // 创建新项目
                currentItem = new OutlineItem();
                currentItem.setTitle(cleanTitle(line));
                currentContent = new StringBuilder();
            } else if (currentItem != null) {
                // 添加到当前项目的内容
                if (currentContent.length() > 0) {
                    currentContent.append("\n");
                }
                currentContent.append(line);
            }
        }
        
        // 添加最后一个项目
        if (currentItem != null) {
            currentItem.setContent(currentContent.toString().trim());
            items.add(currentItem);
        }
        
        return items;
    }
    
    /**
     * 解析普通文本格式的大纲
     */
    private static List<OutlineItem> parseTextOutline(String outlineText) {
        List<OutlineItem> items = new ArrayList<>();
        String[] lines = outlineText.split("\n");
        
        OutlineItem currentItem = null;
        StringBuilder currentContent = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            
            if (isChapterTitle(line)) {
                // 保存前一个项目
                if (currentItem != null) {
                    currentItem.setContent(currentContent.toString().trim());
                    items.add(currentItem);
                }
                
                // 创建新项目
                currentItem = new OutlineItem();
                currentItem.setTitle(cleanTitle(line));
                currentContent = new StringBuilder();
            } else if (currentItem != null) {
                // 添加到当前项目的内容
                if (currentContent.length() > 0) {
                    currentContent.append("\n");
                }
                currentContent.append(line);
            } else {
                // 如果还没有当前项目，创建一个默认项目
                currentItem = new OutlineItem();
                currentItem.setTitle("PPT大纲");
                currentContent = new StringBuilder(line);
            }
        }
        
        // 添加最后一个项目
        if (currentItem != null) {
            currentItem.setContent(currentContent.toString().trim());
            items.add(currentItem);
        }
        
        return items;
    }
    
    /**
     * 判断是否是章节标题
     */
    private static boolean isChapterTitle(String line) {
        // 匹配数字开头的标题：1. 标题、一、标题等
        Pattern pattern = Pattern.compile("^(\\d+[.、]|[一二三四五六七八九十]+[、.]|第[一二三四五六七八九十]+章|Chapter\\s+\\d+)");
        return pattern.matcher(line).find();
    }
    
    /**
     * 清理标题文本
     */
    private static String cleanTitle(String title) {
        // 移除数字前缀和特殊字符
        return title.replaceAll("^(\\d+[.、]|[一二三四五六七八九十]+[、.]|第[一二三四五六七八九十]+章[、.]?|Chapter\\s+\\d+[.:]?)\\s*", "").trim();
    }
    
    /**
     * 创建默认大纲（当解析失败时使用）
     */
    public static List<OutlineItem> createDefaultOutline(String topic) {
        List<OutlineItem> items = new ArrayList<>();
        
        // 封面页
        OutlineItem coverItem = new OutlineItem("封面页", topic);
        items.add(coverItem);
        
        // 目录页
        OutlineItem catalogItem = new OutlineItem("目录", "展示PPT的整体结构和主要内容");
        items.add(catalogItem);
        
        // 主要内容
        OutlineItem contentItem = new OutlineItem("主要内容", "详细阐述" + topic + "的核心要点");
        items.add(contentItem);
        
        // 总结页
        OutlineItem summaryItem = new OutlineItem("总结", "总结主要观点和结论");
        items.add(summaryItem);
        
        // 谢谢页
        OutlineItem thanksItem = new OutlineItem("谢谢", "感谢观看");
        items.add(thanksItem);
        
        return items;
    }
}