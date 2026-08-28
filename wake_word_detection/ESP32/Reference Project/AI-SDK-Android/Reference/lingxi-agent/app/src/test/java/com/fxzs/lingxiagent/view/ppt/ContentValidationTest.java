package com.fxzs.lingxiagent.view.ppt;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * PPT内容验证测试类
 * 测试只包含空格和换行符的内容验证逻辑
 */
public class ContentValidationTest {

    /**
     * 验证内容是否有效（不为空且不只包含空格和换行符）
     */
    private boolean isValidContent(String content) {
        if (content == null) {
            return false;
        }
        // 去除所有空白字符（包括空格、制表符、换行符等）
        String trimmedContent = content.replaceAll("\\s+", "");
        return !trimmedContent.isEmpty();
    }

    @Test
    public void testValidContent() {
        // 有效内容测试
        assertTrue("正常文本应该有效", isValidContent("标题1"));
        assertTrue("包含数字的文本应该有效", isValidContent("第1章"));
        assertTrue("包含特殊字符的文本应该有效", isValidContent("项目：概述"));
        assertTrue("中英文混合应该有效", isValidContent("Title标题"));
        assertTrue("带有正常空格的文本应该有效", isValidContent("项目 概述"));
    }

    @Test
    public void testInvalidContent() {
        // 无效内容测试
        assertFalse("null应该无效", isValidContent(null));
        assertFalse("空字符串应该无效", isValidContent(""));
        assertFalse("只包含空格应该无效", isValidContent("   "));
        assertFalse("只包含换行应该无效", isValidContent("\n\n"));
        assertFalse("只包含制表符应该无效", isValidContent("\t\t"));
        assertFalse("混合空白字符应该无效", isValidContent("  \n\t  \n  "));
        assertFalse("回车换行组合应该无效", isValidContent("\r\n\r\n"));
    }

    @Test
    public void testEdgeCases() {
        // 边界情况测试
        assertTrue("单个字符应该有效", isValidContent("a"));
        assertTrue("单个中文字符应该有效", isValidContent("标"));
        assertTrue("单个数字应该有效", isValidContent("1"));
        assertTrue("单个标点符号应该有效", isValidContent("."));
        
        // 包含空白字符但有内容的情况
        assertTrue("前后有空格但中间有内容应该有效", isValidContent("  标题  "));
        assertTrue("换行中包含内容应该有效", isValidContent("\n标题\n"));
        assertTrue("混合空白字符中包含内容应该有效", isValidContent("\t\n  标题  \n\t"));
    }
}