package com.fxzs.lingxiagent.model.chat.dto;

import java.util.ArrayList;
import java.util.List;

public class ModelTypeResponse extends ArrayList<ModelTypeResponse.ModelItem> {
    
    public List<ModelItem> getList() {
        return this;
    }
    
    public static class ModelItem {
        private Long id;
        private String keyId;
        private String name;
        private String model;
        private String platform;
        private Integer sort;
        private Integer status;
        private Double temperature;
        private Integer maxTokens;
        private Integer maxContexts;
        private String createTime;
        
        // 用于UI显示的描述
        private String modelDesc;
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getKeyId() {
            return keyId;
        }
        
        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public String getPlatform() {
            return platform;
        }
        
        public void setPlatform(String platform) {
            this.platform = platform;
        }
        
        public Integer getSort() {
            return sort;
        }
        
        public void setSort(Integer sort) {
            this.sort = sort;
        }
        
        public Integer getStatus() {
            return status;
        }
        
        public void setStatus(Integer status) {
            this.status = status;
        }
        
        public Double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }
        
        public Integer getMaxTokens() {
            return maxTokens;
        }
        
        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }
        
        public Integer getMaxContexts() {
            return maxContexts;
        }
        
        public void setMaxContexts(Integer maxContexts) {
            this.maxContexts = maxContexts;
        }
        
        public String getCreateTime() {
            return createTime;
        }
        
        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }
        
        // 兼容旧代码的方法
        public String getModelName() {
            return name;
        }
        
        public String getModelCode() {
            return model;
        }
        
        public String getModelDesc() {
            if (modelDesc != null) {
                return modelDesc;
            }
            
            // 根据模型名称生成默认描述
            switch (name) {
                case "灵犀":
                    return "灵犀智能助手";
                case "腾讯混元":
                    return "适合大部分任务";
                case "九天大模型":
                    return "强大的通用能力";
                case "豆包大模型":
                    return "全能思考";
                case "DeepSeek-r1":
                    return "深度思考推理";
                default:
                    return "智能AI助手";
            }
        }
        
        public void setModelDesc(String modelDesc) {
            this.modelDesc = modelDesc;
        }
    }
}