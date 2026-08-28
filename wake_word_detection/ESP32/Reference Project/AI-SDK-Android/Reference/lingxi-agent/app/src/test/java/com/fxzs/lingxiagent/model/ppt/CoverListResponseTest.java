package com.fxzs.lingxiagent.model.ppt;

import com.fxzs.lingxiagent.model.ppt.dto.CoverListResponse;
import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * CoverListResponse JSON解析测试
 * 验证嵌套data结构的正确解析
 */
public class CoverListResponseTest {

    private final Gson gson = new Gson();

    @Test
    public void testCoverListResponseParsing() {
        // 模拟接口返回的JSON数据
        String jsonResponse = "{\n" +
                "  \"flag\": true,\n" +
                "  \"code\": 0,\n" +
                "  \"data\": {\n" +
                "    \"total\": 119,\n" +
                "    \"records\": [\n" +
                "      {\n" +
                "        \"templateIndexId\": \"202407179097C2D\",\n" +
                "        \"pageCount\": 5,\n" +
                "        \"payType\": \"not_free\",\n" +
                "        \"color\": \"蓝色\",\n" +
                "        \"industry\": \"教育培训\",\n" +
                "        \"style\": \"卡通\",\n" +
                "        \"type\": \"system_template\",\n" +
                "        \"detailImage\": \"{\\\"titleCoverImageLarge\\\":\\\"https://example.com/image1.jpeg\\\",\\\"titleCoverImage\\\":\\\"https://example.com/image2.jpeg\\\"}\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"pageNum\": 1\n" +
                "  }\n" +
                "}";

        // 解析JSON
        CoverListResponse response = gson.fromJson(jsonResponse, CoverListResponse.class);

        // 验证解析结果
        assertNotNull("Response should not be null", response);
        assertTrue("Flag should be true", response.isFlag());
        assertEquals("Code should be 0", 0, response.getCode());
        
        // 验证嵌套的data
        CoverListResponse.CoverListData data = response.getData();
        assertNotNull("Data should not be null", data);
        assertEquals("Total should be 119", 119, data.getTotal());
        assertEquals("PageNum should be 1", 1, data.getPageNum());
        
        // 验证records
        assertNotNull("Records should not be null", data.getRecords());
        assertEquals("Should have 1 record", 1, data.getRecords().size());
        
        // 验证第一个模板
        CoverListResponse.CoverTemplate template = data.getRecords().get(0);
        assertNotNull("Template should not be null", template);
        assertEquals("Template ID should match", "202407179097C2D", template.getTemplateIndexId());
        assertEquals("Page count should be 5", 5, template.getPageCount());
        assertEquals("Pay type should be not_free", "not_free", template.getPayType());
        assertEquals("Color should be 蓝色", "蓝色", template.getColor());
        assertEquals("Industry should be 教育培训", "教育培训", template.getIndustry());
        assertEquals("Style should be 卡通", "卡通", template.getStyle());
        assertEquals("Type should be system_template", "system_template", template.getType());
        assertNotNull("Detail image should not be null", template.getDetailImage());
        
        // 验证便捷方法
        assertEquals("Convenience method getRecords should work", 1, response.getRecords().size());
        assertEquals("Convenience method getTotal should work", 119, response.getTotal());
        
        // 验证免费模板检查
        assertFalse("Template should not be free", template.isFree());
    }

    @Test
    public void testEmptyResponse() {
        String jsonResponse = "{\n" +
                "  \"flag\": true,\n" +
                "  \"code\": 0,\n" +
                "  \"data\": {\n" +
                "    \"total\": 0,\n" +
                "    \"records\": [],\n" +
                "    \"pageNum\": 1\n" +
                "  }\n" +
                "}";

        CoverListResponse response = gson.fromJson(jsonResponse, CoverListResponse.class);

        assertNotNull("Response should not be null", response);
        assertTrue("Flag should be true", response.isFlag());
        assertEquals("Code should be 0", 0, response.getCode());
        
        CoverListResponse.CoverListData data = response.getData();
        assertNotNull("Data should not be null", data);
        assertEquals("Total should be 0", 0, data.getTotal());
        assertNotNull("Records should not be null", data.getRecords());
        assertEquals("Records should be empty", 0, data.getRecords().size());
        
        // 验证便捷方法
        assertNotNull("Convenience method getRecords should return empty list", response.getRecords());
        assertEquals("Convenience method getRecords should return empty list", 0, response.getRecords().size());
        assertEquals("Convenience method getTotal should work", 0, response.getTotal());
    }

    @Test
    public void testFreeTemplate() {
        String jsonResponse = "{\n" +
                "  \"flag\": true,\n" +
                "  \"code\": 0,\n" +
                "  \"data\": {\n" +
                "    \"total\": 1,\n" +
                "    \"records\": [\n" +
                "      {\n" +
                "        \"templateIndexId\": \"FREE001\",\n" +
                "        \"pageCount\": 3,\n" +
                "        \"payType\": \"free\",\n" +
                "        \"color\": \"绿色\",\n" +
                "        \"industry\": \"通用\",\n" +
                "        \"style\": \"简约\",\n" +
                "        \"type\": \"system_template\",\n" +
                "        \"detailImage\": \"{}\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"pageNum\": 1\n" +
                "  }\n" +
                "}";

        CoverListResponse response = gson.fromJson(jsonResponse, CoverListResponse.class);
        CoverListResponse.CoverTemplate template = response.getRecords().get(0);
        
        assertTrue("Template should be free", template.isFree());
        assertEquals("Pay type should be free", "free", template.getPayType());
    }
}
