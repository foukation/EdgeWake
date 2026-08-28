import com.fxzs.lingxiagent.model.ppt.dto.CoverListResponse;
import com.google.gson.Gson;

/**
 * 验证CoverListResponse JSON解析的独立程序
 */
public class CoverListResponseValidator {
    
    public static void main(String[] args) {
        Gson gson = new Gson();
        
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

        try {
            // 解析JSON
            CoverListResponse response = gson.fromJson(jsonResponse, CoverListResponse.class);
            
            Timber.i("=== JSON解析验证结果 ===");
            Timber.i("Response不为空: " + (response != null));
            Timber.i("Flag: " + response.isFlag());
            Timber.i("Code: " + response.getCode());
            
            // 验证嵌套的data
            CoverListResponse.CoverListData data = response.getData();
            Timber.i("Data不为空: " + (data != null));
            if (data != null) {
                Timber.i("Total: " + data.getTotal());
                Timber.i("PageNum: " + data.getPageNum());
                Timber.i("Records不为空: " + (data.getRecords() != null));
                Timber.i("Records数量: " + (data.getRecords() != null ? data.getRecords().size() : 0));
                
                // 验证第一个模板
                if (data.getRecords() != null && !data.getRecords().isEmpty()) {
                    CoverListResponse.CoverTemplate template = data.getRecords().get(0);
                    Timber.i("\n=== 第一个模板信息 ===");
                    Timber.i("Template ID: " + template.getTemplateIndexId());
                    Timber.i("Page Count: " + template.getPageCount());
                    Timber.i("Pay Type: " + template.getPayType());
                    Timber.i("Color: " + template.getColor());
                    Timber.i("Industry: " + template.getIndustry());
                    Timber.i("Style: " + template.getStyle());
                    Timber.i("Type: " + template.getType());
                    Timber.i("Is Free: " + template.isFree());
                    Timber.i("Detail Image: " + template.getDetailImage());
                }
                
                // 验证便捷方法
                Timber.i("\n=== 便捷方法验证 ===");
                Timber.i("便捷方法getRecords数量: " + (response.getRecords() != null ? response.getRecords().size() : 0));
                Timber.i("便捷方法getTotal: " + response.getTotal());
            }
            
            Timber.i("\n✅ JSON解析验证成功！");
            
        } catch (Exception e) {
            System.err.println("❌ JSON解析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
