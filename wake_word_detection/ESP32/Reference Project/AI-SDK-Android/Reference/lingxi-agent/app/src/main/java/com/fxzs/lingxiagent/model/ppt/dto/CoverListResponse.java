package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 封面列表响应DTO
 * 处理嵌套的data结构：data.data.{total, records}
 */
public class CoverListResponse {

    @SerializedName("flag")
    private boolean flag;

    @SerializedName("code")
    private int code;

    @SerializedName("data")
    private CoverListData data;

    public CoverListResponse() {
    }

    // Getters and Setters
    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public CoverListData getData() {
        return data;
    }

    public void setData(CoverListData data) {
        this.data = data;
    }

    // 便捷方法，直接获取records
    public List<CoverTemplate> getRecords() {
        return data != null ? data.getRecords() : null;
    }

    // 便捷方法，直接获取total
    public int getTotal() {
        return data != null ? data.getTotal() : 0;
    }

    /**
     * 内部数据类，对应接口返回的data.data部分
     */
    public static class CoverListData {
        @SerializedName("total")
        private int total;

        @SerializedName("records")
        private List<CoverTemplate> records;

        @SerializedName("pageNum")
        private int pageNum;

        public CoverListData() {
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public List<CoverTemplate> getRecords() {
            return records;
        }

        public void setRecords(List<CoverTemplate> records) {
            this.records = records;
        }

        public int getPageNum() {
            return pageNum;
        }

        public void setPageNum(int pageNum) {
            this.pageNum = pageNum;
        }
    }
    
    /**
     * 封面模板内部类
     */
    public static class CoverTemplate {
        @SerializedName("templateIndexId")
        private String templateIndexId;
        
        @SerializedName("pageCount")
        private int pageCount;
        
        @SerializedName("payType")
        private String payType;
        
        @SerializedName("color")
        private String color;
        
        @SerializedName("industry")
        private String industry;
        
        @SerializedName("style")
        private String style;
        
        @SerializedName("type")
        private String type;
        
        @SerializedName("detailImage")
        private String detailImage; // JSON字符串，包含各种封面图片URL
        
        public CoverTemplate() {
        }
        
        // Getters and Setters
        public String getTemplateIndexId() {
            return templateIndexId;
        }
        
        public void setTemplateIndexId(String templateIndexId) {
            this.templateIndexId = templateIndexId;
        }
        
        public int getPageCount() {
            return pageCount;
        }
        
        public void setPageCount(int pageCount) {
            this.pageCount = pageCount;
        }
        
        public String getPayType() {
            return payType;
        }
        
        public void setPayType(String payType) {
            this.payType = payType;
        }
        
        public String getColor() {
            return color;
        }
        
        public void setColor(String color) {
            this.color = color;
        }
        
        public String getIndustry() {
            return industry;
        }
        
        public void setIndustry(String industry) {
            this.industry = industry;
        }
        
        public String getStyle() {
            return style;
        }
        
        public void setStyle(String style) {
            this.style = style;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getDetailImage() {
            return detailImage;
        }
        
        public void setDetailImage(String detailImage) {
            this.detailImage = detailImage;
        }
        
        /**
         * 检查是否为免费模板
         */
        public boolean isFree() {
            return !"not_free".equals(payType);
        }
    }
}