package com.fxzs.lingxiagent.viewmodel.ppt;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import com.fxzs.lingxiagent.model.ppt.dto.OutlineItem;

import java.util.ArrayList;
import java.util.List;

/**
 * VMPptOutlineEdit的单元测试
 * 测试大纲编辑功能的核心逻辑
 */
public class VMPptOutlineEditTest {

    @Test
    public void testOutlineItemCreation() {
        // 测试OutlineItem的创建
        OutlineItem item = new OutlineItem("测试标题", "测试内容", 1);

        assertNotNull("OutlineItem不应为空", item);
        assertEquals("标题应该匹配", "测试标题", item.getTitle());
        assertEquals("内容应该匹配", "测试内容", item.getContent());
        assertEquals("级别应该匹配", 1, item.getLevel());
    }

    @Test
    public void testOutlineItemWithSubItems() {
        // 测试带子项目的OutlineItem
        OutlineItem mainItem = new OutlineItem("主项目", "主内容", 1);

        List<OutlineItem> subItems = new ArrayList<>();
        subItems.add(new OutlineItem("子项目1", "子内容1", 2));
        subItems.add(new OutlineItem("子项目2", "子内容2", 2));

        mainItem.setSubItems(subItems);

        assertNotNull("子项目列表不应为空", mainItem.getSubItems());
        assertEquals("应该有两个子项目", 2, mainItem.getSubItems().size());
        assertEquals("第一个子项目标题", "子项目1", mainItem.getSubItems().get(0).getTitle());
        assertEquals("第二个子项目标题", "子项目2", mainItem.getSubItems().get(1).getTitle());
    }

    @Test
    public void testOutlineItemListOperations() {
        // 测试列表操作
        List<OutlineItem> items = new ArrayList<>();

        // 添加项目
        OutlineItem item1 = new OutlineItem("项目1", "内容1", 1);
        OutlineItem item2 = new OutlineItem("项目2", "内容2", 1);
        items.add(item1);
        items.add(item2);

        assertEquals("应该有两个项目", 2, items.size());

        // 插入项目
        OutlineItem insertItem = new OutlineItem("插入项目", "插入内容", 1);
        items.add(1, insertItem);

        assertEquals("应该有三个项目", 3, items.size());
        assertEquals("中间项目应该是插入的", "插入项目", items.get(1).getTitle());

        // 删除项目
        items.remove(0);
        assertEquals("应该剩余两个项目", 2, items.size());
        assertEquals("第一个项目应该是插入的", "插入项目", items.get(0).getTitle());
    }

    @Test
    public void testOutlineItemEditing() {
        // 测试项目编辑
        OutlineItem item = new OutlineItem("原标题", "原内容", 1);

        // 修改标题和内容
        item.setTitle("新标题");
        item.setContent("新内容");

        assertEquals("标题应该已更新", "新标题", item.getTitle());
        assertEquals("内容应该已更新", "新内容", item.getContent());
    }
}
