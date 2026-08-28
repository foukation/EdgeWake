package com.fxzs.lingxiagent.model.drawing.dto;

import java.io.Serializable;

/**
 * AI绘画分类DTO
 */
public class DrawingCategoryDto implements Serializable {
    
    private Long id;           // 分类ID
    private String name;       // 分类名称
    private String code;       // 分类代码
    private Integer sort;      // 排序
    private Integer status;    // 状态：0-禁用，1-启用
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
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
}