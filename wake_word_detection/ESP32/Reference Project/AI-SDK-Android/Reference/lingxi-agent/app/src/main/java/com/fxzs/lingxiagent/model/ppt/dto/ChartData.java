package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * 图表数据模型
 */
public class ChartData {
    
    /**
     * 图表类型枚举
     */
    public enum ChartType {
        @SerializedName("bar")
        BAR("bar", "柱状图"),
        
        @SerializedName("line")
        LINE("line", "折线图"),
        
        @SerializedName("pie")
        PIE("pie", "饼图"),
        
        @SerializedName("area")
        AREA("area", "面积图"),
        
        @SerializedName("scatter")
        SCATTER("scatter", "散点图");
        
        private final String value;
        private final String description;
        
        ChartType(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static ChartType fromValue(String value) {
            for (ChartType type : ChartType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return BAR; // 默认返回柱状图
        }
    }
    
    @SerializedName("type")
    private ChartType type;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("x_axis_label")
    private String xAxisLabel;
    
    @SerializedName("y_axis_label")
    private String yAxisLabel;
    
    @SerializedName("data_series")
    private List<DataSeries> dataSeries;
    
    @SerializedName("colors")
    private List<String> colors;
    
    @SerializedName("options")
    private Map<String, Object> options;
    
    public ChartData() {
    }
    
    public ChartData(ChartType type, String title) {
        this.type = type;
        this.title = title;
    }
    
    // Getters and Setters
    public ChartType getType() {
        return type;
    }
    
    public void setType(ChartType type) {
        this.type = type;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getXAxisLabel() {
        return xAxisLabel;
    }
    
    public void setXAxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;
    }
    
    public String getYAxisLabel() {
        return yAxisLabel;
    }
    
    public void setYAxisLabel(String yAxisLabel) {
        this.yAxisLabel = yAxisLabel;
    }
    
    public List<DataSeries> getDataSeries() {
        return dataSeries;
    }
    
    public void setDataSeries(List<DataSeries> dataSeries) {
        this.dataSeries = dataSeries;
    }
    
    public List<String> getColors() {
        return colors;
    }
    
    public void setColors(List<String> colors) {
        this.colors = colors;
    }
    
    public Map<String, Object> getOptions() {
        return options;
    }
    
    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }
    
    /**
     * 数据系列内部类
     */
    public static class DataSeries {
        @SerializedName("name")
        private String name;
        
        @SerializedName("data")
        private List<DataPoint> data;
        
        public DataSeries() {
        }
        
        public DataSeries(String name, List<DataPoint> data) {
            this.name = name;
            this.data = data;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<DataPoint> getData() {
            return data;
        }
        
        public void setData(List<DataPoint> data) {
            this.data = data;
        }
    }
    
    /**
     * 数据点内部类
     */
    public static class DataPoint {
        @SerializedName("x")
        private Object x; // 可以是字符串或数字
        
        @SerializedName("y")
        private Number y;
        
        @SerializedName("label")
        private String label;
        
        public DataPoint() {
        }
        
        public DataPoint(Object x, Number y) {
            this.x = x;
            this.y = y;
        }
        
        public DataPoint(Object x, Number y, String label) {
            this.x = x;
            this.y = y;
            this.label = label;
        }
        
        public Object getX() {
            return x;
        }
        
        public void setX(Object x) {
            this.x = x;
        }
        
        public Number getY() {
            return y;
        }
        
        public void setY(Number y) {
            this.y = y;
        }
        
        public String getLabel() {
            return label;
        }
        
        public void setLabel(String label) {
            this.label = label;
        }
    }
}