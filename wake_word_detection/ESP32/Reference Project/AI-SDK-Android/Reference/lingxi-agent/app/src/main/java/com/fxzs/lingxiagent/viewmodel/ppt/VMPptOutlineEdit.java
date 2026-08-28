package com.fxzs.lingxiagent.viewmodel.ppt;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.honor.repository.BenefitCode;
import com.fxzs.lingxiagent.model.ppt.callback.PptStreamCallback;
import com.fxzs.lingxiagent.model.ppt.dto.OutlineItem;
import com.fxzs.lingxiagent.model.ppt.dto.OutlineRequest;
import com.fxzs.lingxiagent.model.ppt.repository.PptRepository;
import com.fxzs.lingxiagent.util.BillDialogHelper;
import com.fxzs.lingxiagent.util.PptOutlineParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class VMPptOutlineEdit extends BaseViewModel {
    
    private final MutableLiveData<List<OutlineItem>> outlineItems = new MutableLiveData<>(new ArrayList<>());
    private final ObservableField<Boolean> isGenerating = new ObservableField<>(false);
    private final MutableLiveData<Boolean> canSelectTemplate = new MutableLiveData<>(false);
    private final MutableLiveData<String> currentTopic = new MutableLiveData<>();
    private final MutableLiveData<String> streamingContent = new MutableLiveData<>("");
    private final MutableLiveData<String> thinkingProcess = new MutableLiveData<>("");

    private final PptRepository pptRepository;
    private String currentPptId;

    // 流式数据累积
    private StringBuilder streamDataBuffer = new StringBuilder();
    private StringBuilder thinkingBuffer = new StringBuilder();
    // 解析节流，避免每个token都全量解析导致主线程卡顿
    private static final long STREAM_PARSE_INTERVAL_MS = 300;
    private long lastStreamParseTimeMs = 0L;

    private Context context;
    
    public VMPptOutlineEdit(@NonNull Application application) {
        super(application);
        context = application;
        pptRepository = PptRepository.getInstance();
        pptRepository.initCache(application);
    }
    
    /**
     * 加载大纲数据
     */
    public void loadOutline(String pptId) {
        this.currentPptId = pptId;
        
        // 首先尝试从缓存加载
        List<OutlineItem> cachedOutline = pptRepository.getCurrentOutline();
        if (!cachedOutline.isEmpty()) {
            // 创建深拷贝，避免引用共享问题
            List<OutlineItem> copiedOutline = deepCopyOutline(cachedOutline);
            
            // 添加调试日志，检查数据完整性
            Timber.tag("VMPptOutlineEdit").d( "从缓存加载大纲，原始数据项目数: " + cachedOutline.size());
            for (int i = 0; i < copiedOutline.size(); i++) {
                OutlineItem item = copiedOutline.get(i);
                String title = item != null ? item.getTitle() : "null";
                List<OutlineItem> subItems = item != null ? item.getSubItems() : null;
                int subItemsSize = subItems != null ? subItems.size() : 0;
                Timber.tag("VMPptOutlineEdit").d( "缓存数据[" + i + "]: title=" + title + ", subItems.size()=" + subItemsSize);
            }
            
            outlineItems.setValue(copiedOutline);
            canSelectTemplate.setValue(true);
            return;
        }
        
        // 如果没有缓存，创建默认大纲
        createDefaultOutline();
    }
    
    /**
     * 深拷贝大纲列表，避免引用共享问题
     */
    private List<OutlineItem> deepCopyOutline(List<OutlineItem> original) {
        if (original == null) {
            return new ArrayList<>();
        }
        
        List<OutlineItem> copied = new ArrayList<>();
        for (OutlineItem item : original) {
            if (item != null) {
                // 创建新的OutlineItem对象
                OutlineItem copiedItem = new OutlineItem();
                copiedItem.setId(item.getId());
                copiedItem.setTitle(item.getTitle());
                copiedItem.setContent(item.getContent());
                copiedItem.setOrder(item.getOrder());
                copiedItem.setLevel(item.getLevel());
                copiedItem.setParentId(item.getParentId());
                copiedItem.setExpanded(item.isExpanded());
                copiedItem.setEditing(item.isEditing());
                copiedItem.setSelected(item.isSelected());
                
                // 深拷贝subItems
                if (item.getSubItems() != null) {
                    List<OutlineItem> copiedSubItems = new ArrayList<>();
                    for (OutlineItem subItem : item.getSubItems()) {
                        if (subItem != null) {
                            OutlineItem copiedSubItem = new OutlineItem();
                            copiedSubItem.setId(subItem.getId());
                            copiedSubItem.setTitle(subItem.getTitle());
                            copiedSubItem.setContent(subItem.getContent());
                            copiedSubItem.setOrder(subItem.getOrder());
                            copiedSubItem.setLevel(subItem.getLevel());
                            copiedSubItem.setParentId(subItem.getParentId());
                            copiedSubItem.setExpanded(subItem.isExpanded());
                            copiedSubItem.setEditing(subItem.isEditing());
                            copiedSubItem.setSelected(subItem.isSelected());
                            copiedSubItems.add(copiedSubItem);
                        }
                    }
                    copiedItem.setSubItems(copiedSubItems);
                }
                
                copied.add(copiedItem);
            }
        }
        
        return copied;
    }
    
    /**
     * 创建默认大纲
     */
    private void createDefaultOutline() {
        String topic = currentTopic.getValue();
        if (topic == null || topic.isEmpty()) {
            topic = "PPT演示";
        }
        
        List<OutlineItem> defaultItems = PptOutlineParser.createDefaultOutline(topic);
        outlineItems.setValue(defaultItems);
        
        // 缓存默认大纲
        pptRepository.saveCurrentOutline(defaultItems);
        canSelectTemplate.setValue(true);
    }
    
    /**
     * 重新生成大纲（使用流式方法）
     */
    public void regenerateOutline() {
        String topic = currentTopic.getValue();
        Timber.tag("VMPptOutlineEdit").d( "regenerateOutline called, currentTopic.getValue(): " + topic);

        if (topic == null || topic.isEmpty()) {
            Timber.tag("VMPptOutlineEdit").e( "主题信息丢失，currentTopic为: " + topic);
            setError("主题信息丢失，无法重新生成大纲");
            return;
        }

        // 重新开始流式生成
        startReceivingStreamData(topic, context);
    }
    
    /**
     * 更新大纲项
     */
    public void updateOutlineItem(int position, OutlineItem item) {
        List<OutlineItem> items = outlineItems.getValue();
        if (items != null && position >= 0 && position < items.size()) {
            items.set(position, item);
            outlineItems.setValue(items);
            
            // 缓存更新后的大纲
            pptRepository.saveCurrentOutline(items);
        }
    }

    /**
     * 开始接收流式大纲数据
     */
    public void startReceivingStreamData(String topic, Context context) {
        Timber.tag("VMPptOutlineEdit").d( "startReceivingStreamData called with topic: " + topic);

        // 确保主题被正确设置
        if (topic != null && !topic.trim().isEmpty()) {
            setCurrentTopic(topic);
            // 同时更新状态管理器中的项目主题
            // 注意：PptStateManager没有updateCurrentTopic方法，主题在createNewProject时已设置
        } else {
            Timber.tag("VMPptOutlineEdit").e( "主题为空，无法开始生成");
            setError("主题信息缺失，无法生成大纲");
            return;
        }

        isGenerating.set(true);
        canSelectTemplate.setValue(false);

        // 清空之前的数据
        streamDataBuffer.setLength(0);
        thinkingBuffer.setLength(0);
        lastStreamParseTimeMs = 0L;
        streamingContent.setValue("");
        thinkingProcess.setValue("");
        outlineItems.setValue(new ArrayList<>());

        OutlineRequest request = new OutlineRequest(topic);
        Timber.tag("VMPptOutlineEdit").d( "创建OutlineRequest: " + request.getTheme());

        pptRepository.generateOutlineStream(request, new PptStreamCallback() {
            @Override
            public void onStart() {
                Timber.tag("VMPptOutlineEdit").d( "开始接收流式大纲数据");
            }

            @Override
            public void onReceive(String data) {
                // 降低日志与解析频率，避免高频token触发卡顿/ANR
                long now = System.currentTimeMillis();
                boolean shouldParseNow = now - lastStreamParseTimeMs >= STREAM_PARSE_INTERVAL_MS;

                if (shouldParseNow) {
                    lastStreamParseTimeMs = now;
                    // 解析SSE数据，区分思考过程和最终结果
                    parseStreamData(data, context);
                } else {
                    // 仅累积数据，不立即解析
                    try {
                        JSONObject jsonData = new JSONObject(data);
                        if (jsonData.has("result")) {
                            JSONObject result = jsonData.getJSONObject("result");
                            if (result.has("output")) {
                                JSONObject output = result.getJSONObject("output");
                                String content = output.optString("content", "");
                                if (!content.isEmpty()) {
                                    streamDataBuffer.append(content);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        streamDataBuffer.append(data);
                    }
                }
            }

            @Override
            public void onComplete() {
                Timber.tag("VMPptOutlineEdit").d( "流式数据接收完成");

                isGenerating.set(false);

                // 检查是否已经通过流式解析获得了大纲数据
                List<OutlineItem> currentOutlineItems = outlineItems.getValue();
                if (currentOutlineItems != null && !currentOutlineItems.isEmpty()) {
                    // 已经有大纲数据，直接使用（不重新解析，避免覆盖）
                    Timber.tag("VMPptOutlineEdit").d( "使用流式解析的大纲数据，项目数: " + currentOutlineItems.size());

                    // 缓存大纲数据
                    pptRepository.saveCurrentOutline(currentOutlineItems);
                    canSelectTemplate.setValue(true);
                    setSuccess("大纲生成成功");
                } else {
                    // 如果流式解析没有获得数据，尝试最终解析
                    Timber.tag("VMPptOutlineEdit").d( "流式解析未获得数据，尝试最终解析");
                    String finalContent = streamDataBuffer.toString();
                    if (!finalContent.isEmpty()) {

                        String parsedContent = PptOutlineParser.parseStreamData(finalContent);
                        List<OutlineItem> newOutlineItems = PptOutlineParser.parseOutlineText(parsedContent);

                        if (newOutlineItems.isEmpty()) {
                            // 如果解析失败，创建默认大纲
                            newOutlineItems = PptOutlineParser.createDefaultOutline(topic);
                        }

                        outlineItems.setValue(newOutlineItems);
                        pptRepository.saveCurrentOutline(newOutlineItems);
                        canSelectTemplate.setValue(true);
                        setSuccess("大纲生成成功");
                    } else {
                        // 如果没有接收到有效数据，创建默认大纲
                        List<OutlineItem> defaultItems = PptOutlineParser.createDefaultOutline(topic);
                        outlineItems.setValue(defaultItems);
                        pptRepository.saveCurrentOutline(defaultItems);
                        canSelectTemplate.setValue(true);
                        setError("未接收到有效数据，已生成默认大纲");
                    }
                }
            }

            @Override
            public void onError(String error) {
                Timber.tag("VMPptOutlineEdit").e( "流式数据接收失败: " + error);

                isGenerating.set(false);

                // 生成失败时，创建默认大纲
                List<OutlineItem> defaultItems = PptOutlineParser.createDefaultOutline(topic);
                outlineItems.setValue(defaultItems);
                pptRepository.saveCurrentOutline(defaultItems);
                canSelectTemplate.setValue(true);

                setError("大纲生成失败: " + error + "，已生成默认大纲");
            }
        });
    }

    /**
     * 解析流式数据（基于Vue版本的逻辑）
     */
    private void parseStreamData(String data, Context context) {
        try {
            JSONObject jsonData = new JSONObject(data);

            // 检查是否有result字段
            if (jsonData.has("result")) {
                JSONObject result = jsonData.getJSONObject("result");
                if (result.has("output")) {
                    JSONObject output = result.getJSONObject("output");

                    String content = output.optString("content", "");
                    JSONObject metadata = output.optJSONObject("metadata");
                    int errorCode = metadata != null ? metadata.optInt("errorCode", -1) : -1;

                    if (BenefitCode.isBenefitError(String.valueOf(errorCode))) {
                        BillDialogHelper.showBillDialog(context, content, () -> ((Activity) context).finish());
                    }

                    if (!content.isEmpty()) {
                        // 累积所有内容
                        streamDataBuffer.append(content);
                        String fullContent = streamDataBuffer.toString();

                        // 按照Vue版本的逻辑解析内容
                        parseContentByVueLogic(fullContent);
                    }
                }
            }
        } catch (JSONException e) {
            Timber.tag("VMPptOutlineEdit").e( "解析流式数据失败: " + e.getMessage());
            // 如果JSON解析失败，直接作为文本内容处理
            streamDataBuffer.append(data);
            parseContentByVueLogic(streamDataBuffer.toString());
        }
    }

    /**
     * 按照Vue版本的逻辑解析内容（修复大纲保留问题）
     */
    private void parseContentByVueLogic(String fullContent) {
        // 按照 <ppt_outline> 标签分割内容
        String[] splitData = fullContent.split("<ppt_outline>");

        if (splitData.length > 1) {
            // 有大纲内容
            String[] outlineData = splitData[1].split("</ppt_outline>");

            // 先解析大纲JSON数据
            try {
                String jsonContent = outlineData[0].replaceAll("\\s", "");
                String fixedJson = tryFixJSON(jsonContent);
                
                Timber.tag("VMPptOutlineEdit").d("尝试解析JSON，原始长度: " + jsonContent.length() + ", 修复后长度: " + fixedJson.length());
                
                JSONObject outlineJson = new JSONObject(fixedJson);

                if (outlineJson.has("catalogs")) {
                    JSONArray catalogs = outlineJson.getJSONArray("catalogs");
                    List<OutlineItem> newOutlineItems = parseOutlineFromCatalogs(catalogs);

                    // 验证解析出的数据是否完整有效
                    if (isOutlineDataValid(newOutlineItems)) {
                        // 只有在数据完整有效时才更新大纲数据
                        outlineItems.setValue(newOutlineItems);
                        Timber.tag("VMPptOutlineEdit").d( "解析并保留大纲项目: " + newOutlineItems.size() + "个");
                        
                        // 记录每个项目的标题，用于调试
                        for (int i = 0; i < newOutlineItems.size(); i++) {
                            OutlineItem item = newOutlineItems.get(i);
                            if (item != null) {
                                Timber.tag("VMPptOutlineEdit").d("解析的项目[" + i + "]: title=" + item.getTitle() + ", hashCode=" + System.identityHashCode(item));
                            }
                        }
                    } else {
                        Timber.tag("VMPptOutlineEdit").w( "解析出的大纲数据不完整，保留之前的数据。解析出的项目数: " + newOutlineItems.size());
                        
                        // 检查之前的数据是否有效，如果无效则清空
                        List<OutlineItem> currentItems = outlineItems.getValue();
                        if (currentItems != null && !isOutlineDataValid(currentItems)) {
                            Timber.tag("VMPptOutlineEdit").w( "之前的数据也无效，清空数据");
                            outlineItems.setValue(new ArrayList<>());
                        }
                    }
                } else {
                    Timber.tag("VMPptOutlineEdit").w( "JSON中没有catalogs字段");
                }
            } catch (JSONException e) {
                Timber.tag("VMPptOutlineEdit").e( "解析大纲JSON失败: " + e.getMessage() + "，保留之前的数据");
                
                // 解析失败时，检查之前的数据是否有效
                List<OutlineItem> currentItems = outlineItems.getValue();
                if (currentItems != null && !isOutlineDataValid(currentItems)) {
                    Timber.tag("VMPptOutlineEdit").w( "之前的数据无效，清空数据以避免显示重复内容");
                    outlineItems.setValue(new ArrayList<>());
                }
            }

            // 如果有解释内容，显示在解释区域
            if (outlineData.length > 1) {
                String explanationContent = outlineData[1];
                if (!explanationContent.trim().isEmpty()) {
                    streamingContent.setValue(explanationContent);
                    Timber.tag("VMPptOutlineEdit").d( "解析到解释内容: " + explanationContent);
                }
            }

            // 思考过程结束，但保留显示（不清空）
            Timber.tag("VMPptOutlineEdit").d( "大纲生成完成，保留思考过程显示");
        } else {
            // 思考过程内容
            String thinkingContent = splitData[0];
            if (thinkingContent.contains("<ppt_outline")) {
                thinkingContent = thinkingContent.split("<ppt_outline")[0];
            }

            if (!thinkingContent.isEmpty()) {
                thinkingProcess.setValue(thinkingContent);
                Timber.tag("VMPptOutlineEdit").d( "解析到思考过程: " + thinkingContent);
            }
        }
    }

    /**
     * 修复不完整的JSON字符串（基于Vue版本的逻辑）
     * 改进：处理不完整的字符串（Unterminated string错误）
     */
    private String tryFixJSON(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return "{}";
        }

        java.util.Stack<Character> stack = new java.util.Stack<>();
        boolean inString = false;
        boolean escape = false;
        int lastStringStart = -1;

        char[] chars = jsonStr.toCharArray();
        
        // 分析结构
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            
            if (escape) {
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '"') {
                if (!inString) {
                    // 字符串开始
                    inString = true;
                    lastStringStart = i;
                } else {
                    // 字符串结束
                    inString = false;
                    lastStringStart = -1;
                }
                continue;
            }

            if (!inString) {
                if (c == '{' || c == '[') {
                    stack.push(c);
                }
                if (c == '}' && !stack.isEmpty() && stack.peek() == '{') {
                    stack.pop();
                }
                if (c == ']' && !stack.isEmpty() && stack.peek() == '[') {
                    stack.pop();
                }
            }
        }

        // 处理不完整的字符串：如果字符串未结束，补全引号
        StringBuilder fixed = new StringBuilder(jsonStr);
        if (inString && lastStringStart >= 0) {
            // 字符串未结束，补全引号
            fixed.append('"');
            Timber.tag("VMPptOutlineEdit").d("检测到未完成的字符串，已补全引号");
        }

        // 补全缺失括号
        while (!stack.isEmpty()) {
            char last = stack.pop();
            fixed.append(last == '{' ? '}' : ']');
        }

        return fixed.toString();
    }

    /**
     * 从catalogs数组解析大纲项目（按照Vue版本的逻辑）
     */
    private List<OutlineItem> parseOutlineFromCatalogs(JSONArray catalogs) {
        List<OutlineItem> items = new ArrayList<>();

        if (catalogs == null) {
            Timber.tag("VMPptOutlineEdit").e( "catalogs为null，返回空列表");
            return items;
        }

        try {
            Timber.tag("VMPptOutlineEdit").d( "开始解析catalogs，数量: " + catalogs.length());

            for (int i = 0; i < catalogs.length(); i++) {
                try {
                    JSONObject catalog = catalogs.getJSONObject(i);
                    if (catalog == null) {
                        Timber.tag("VMPptOutlineEdit").w( "catalog[" + i + "]为null，跳过");
                        continue;
                    }
                    
                    Timber.tag("VMPptOutlineEdit").d( "解析catalog[" + i + "]: " + catalog.toString());

                    String title = catalog.optString("catalog", "");
                    // 验证标题是否有效（非空且不是空白字符串）
                    if (title == null || title.trim().isEmpty()) {
                        Timber.tag("VMPptOutlineEdit").w( "catalog[" + i + "]的标题为空，跳过");
                        continue;
                    }

                    // 创建主标题项目
                    OutlineItem mainItem = new OutlineItem(title.trim(), "", 1);
                    Timber.tag("VMPptOutlineEdit").d( "创建主标题: " + title.trim());

                    // 处理子标题
                    JSONArray subCatalogArray = catalog.optJSONArray("sub_catalog");
                    if (subCatalogArray != null && subCatalogArray.length() > 0) {
                        List<OutlineItem> subItems = new ArrayList<>();
                        Timber.tag("VMPptOutlineEdit").d( "子标题数量: " + subCatalogArray.length());

                        for (int j = 0; j < subCatalogArray.length(); j++) {
                            try {
                                String subTitle = subCatalogArray.optString(j, "");
                                // 验证子标题是否有效
                                if (subTitle != null && !subTitle.trim().isEmpty()) {
                                    OutlineItem subItem = new OutlineItem(subTitle.trim(), "", 2);
                                    subItem.setParentId(mainItem.getId()); // 设置父级关系
                                    subItems.add(subItem);
                                    Timber.tag("VMPptOutlineEdit").d( "添加子标题: " + subTitle.trim());
                                } else {
                                    Timber.tag("VMPptOutlineEdit").w( "子标题[" + j + "]为空，跳过");
                                }
                            } catch (Exception e) {
                                Timber.tag("VMPptOutlineEdit").e( "解析子标题[" + j + "]失败: " + e.getMessage());
                                // 继续处理下一个子标题
                            }
                        }

                        mainItem.setSubItems(subItems);
                        mainItem.setExpanded(true); // 默认展开
                    }

                    items.add(mainItem);
                } catch (JSONException e) {
                    Timber.tag("VMPptOutlineEdit").e( "解析catalog[" + i + "]失败: " + e.getMessage());
                    // 继续处理下一个catalog，不中断整个解析过程
                }
            }

            Timber.tag("VMPptOutlineEdit").d( "解析完成，总项目数: " + items.size());
        } catch (Exception e
//                JSONException e
        ) {
            Timber.tag("VMPptOutlineEdit").e( "解析catalogs失败: " + e.getMessage(), e);
        }

        return items;
    }

    /**
     * 验证大纲数据是否完整有效
     * @param items 要验证的大纲项目列表
     * @return true表示数据完整有效，false表示数据不完整或无效
     */
    private boolean isOutlineDataValid(List<OutlineItem> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        // 检查是否有重复的标题（可能是解析错误导致的）
        java.util.Set<String> titles = new java.util.HashSet<>();
        for (OutlineItem item : items) {
            if (item == null) {
                Timber.tag("VMPptOutlineEdit").w( "发现null的OutlineItem，数据无效");
                return false;
            }

            String title = item.getTitle();
            if (title == null || title.trim().isEmpty()) {
                Timber.tag("VMPptOutlineEdit").w( "发现空标题的OutlineItem，数据无效");
                return false;
            }

            // 检查是否有重复的主标题（可能是解析错误导致所有项目都变成相同的值）
            if (titles.contains(title)) {
                Timber.tag("VMPptOutlineEdit").w( "发现重复的主标题: " + title + "，可能是解析错误");
                // 如果所有主标题都相同（只有一个唯一标题但有多项），认为数据无效
                if (titles.size() == 1 && items.size() > 1) {
                    Timber.tag("VMPptOutlineEdit").e( "所有主标题都相同，数据无效！项目数: " + items.size() + ", 唯一标题数: " + titles.size());
                    return false;
                }
            }
            titles.add(title);

            // 检查子项
            List<OutlineItem> subItems = item.getSubItems();
            if (subItems != null) {
                java.util.Set<String> subTitles = new java.util.HashSet<>();
                for (OutlineItem subItem : subItems) {
                    if (subItem == null) {
                        continue;
                    }
                    String subTitle = subItem.getTitle();
                    if (subTitle != null && !subTitle.trim().isEmpty()) {
                        // 检查是否有重复的子标题
                        if (subTitles.contains(subTitle)) {
                            Timber.tag("VMPptOutlineEdit").w( "发现重复的子标题: " + subTitle);
                        }
                        subTitles.add(subTitle);
                    }
                }
            }
        }

        return true;
    }

    /**
     * 添加新的大纲项
     */
    public void addOutlineItem(String title, String content) {
        List<OutlineItem> items = outlineItems.getValue();
        if (items == null) {
            items = new ArrayList<>();
        }

        OutlineItem newItem = new OutlineItem(title, content);

        // 为新的主标题添加一个空白子项目
        List<OutlineItem> subItems = new ArrayList<>();
        OutlineItem emptySubItem = new OutlineItem("", "", 2);
        emptySubItem.setParentId(newItem.getId());
        subItems.add(emptySubItem);
        newItem.setSubItems(subItems);

        items.add(newItem);
        outlineItems.setValue(items);

        // 缓存更新后的大纲
        pptRepository.saveCurrentOutline(items);
    }
    
    /**
     * 删除大纲项
     */
    public void removeOutlineItem(int position) {
        List<OutlineItem> items = outlineItems.getValue();
        if (items != null && position >= 0 && position < items.size()) {
            items.remove(position);
            outlineItems.setValue(items);
            
            // 缓存更新后的大纲
            pptRepository.saveCurrentOutline(items);
        }
    }
    
    /**
     * 移动大纲项位置
     */
    public void moveOutlineItem(int fromPosition, int toPosition) {
        List<OutlineItem> items = outlineItems.getValue();
        if (items != null && fromPosition >= 0 && fromPosition < items.size()
            && toPosition >= 0 && toPosition < items.size()) {

            OutlineItem item = items.remove(fromPosition);
            items.add(toPosition, item);
            outlineItems.setValue(items);

            // 缓存更新后的大纲
            pptRepository.saveCurrentOutline(items);
        }
    }



    /**
     * 在指定位置插入大纲项
     */
    public void insertOutlineItem(int position, OutlineItem newItem) {
        List<OutlineItem> items = outlineItems.getValue();
        if (items == null) {
            items = new ArrayList<>();
        }

        if (position >= 0 && position <= items.size()) {
            items.add(position, newItem);
            outlineItems.setValue(items);

            // 缓存更新后的大纲
            pptRepository.saveCurrentOutline(items);
        }
    }
    /**
     * 在指定位置插入子项
     */
    public void insertSubOutlineItem(int position, OutlineItem newItem) {
        List<OutlineItem> items = outlineItems.getValue();
        if (items == null) {
            items = new ArrayList<>();
        }

        if (position >= 0 && position <= items.size()) {
            items.add(position, newItem);
            outlineItems.setValue(items);

            // 缓存更新后的大纲
            pptRepository.saveCurrentOutline(items);
        }
    }
    
    /**
     * 停止生成
     */
    public void stopGenerating() {
        isGenerating.set(false);
        setLoading(false);
    }
    
    /**
     * 设置当前主题
     */
    public void setCurrentTopic(String topic) {
        Timber.tag("VMPptOutlineEdit").d( "setCurrentTopic called with: " + topic);
        currentTopic.setValue(topic);
        Timber.tag("VMPptOutlineEdit").d( "currentTopic.getValue() after set: " + currentTopic.getValue());
    }

    /**
     * 获取当前主题
     */
    public String getCurrentTopicValue() {
        String topic = currentTopic.getValue();
        Timber.tag("VMPptOutlineEdit").d( "获取当前主题: " + topic);
        return topic != null ? topic : "";
    }

    /**
     * 准备跳转到模板选择页面的数据
     */
    public android.os.Bundle prepareTemplateSelectionData() {
        android.os.Bundle bundle = new android.os.Bundle();

        String topic = getCurrentTopicValue();
        if (!topic.isEmpty()) {
            bundle.putString(com.fxzs.lingxiagent.util.PptStateManager.EXTRA_TOPIC, topic);
            Timber.tag("VMPptOutlineEdit").d( "准备模板选择数据 - 主题: " + topic);
        }

        // 确保大纲数据已保存
        List<OutlineItem> currentOutline = outlineItems.getValue();
        if (currentOutline != null && !currentOutline.isEmpty()) {
            pptRepository.saveCurrentOutline(currentOutline);
            Timber.tag("VMPptOutlineEdit").d( "保存大纲数据: " + currentOutline.size() + "项");
        }

        return bundle;
    }
    
    /**
     * 检查是否可以选择模板
     */
    public boolean canProceedToTemplateSelection() {
        List<OutlineItem> items = outlineItems.getValue();
        return items != null && !items.isEmpty();
    }
    
    /**
     * 获取大纲摘要
     */
    public String getOutlineSummary() {
        List<OutlineItem> items = outlineItems.getValue();
        if (items == null || items.isEmpty()) {
            return "暂无大纲内容";
        }
        
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < Math.min(items.size(), 3); i++) {
            OutlineItem item = items.get(i);
            if (i > 0) summary.append("、");
            summary.append(item.getTitle());
        }
        
        if (items.size() > 3) {
            summary.append("等").append(items.size()).append("个部分");
        }
        
        return summary.toString();
    }
    
    public MutableLiveData<List<OutlineItem>> getOutlineItems() {
        return outlineItems;
    }
    
    public ObservableField<Boolean> getIsGenerating() {
        return isGenerating;
    }
    
    public MutableLiveData<String> getErrorMessage() {
        return getError();
    }
    
    public MutableLiveData<String> getSuccessMessage() {
        return getSuccess();
    }

    public LiveData<String> getStreamingContent() {
        return streamingContent;
    }

    public LiveData<String> getThinkingProcess() {
        return thinkingProcess;
    }

    public LiveData<String> getCurrentTopic() {
        return currentTopic;
    }

    /**
     * 取消大纲生成
     */
    public void cancelOutlineGeneration() {
        Timber.tag("VMPptOutlineEdit").d( "cancelOutlineGeneration called");
        pptRepository.cancelOutlineStream();
        isGenerating.set(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Timber.tag("VMPptOutlineEdit").d( "ViewModel onCleared called - 取消大纲生成");
        cancelOutlineGeneration();
    }
}