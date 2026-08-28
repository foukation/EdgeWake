package com.fxzs.lingxiagent.lingxi.translate.util;

import java.util.List;

public class LanguageListDto {
    private int code;
    private String message;
    private List<LanguageInfo> sourceLanguages; // 源语言列表
    private List<LanguageInfo> targetLanguages; // 目标语言列表

    // Getter 和 Setter 方法
    public List<LanguageInfo> getSourceLanguages() {
        return sourceLanguages;
    }

    public void setSourceLanguages(List<LanguageInfo> sourceLanguages) {
        this.sourceLanguages = sourceLanguages;
    }

    public List<LanguageInfo> getTargetLanguages() {
        return targetLanguages;
    }

    public void setTargetLanguages(List<LanguageInfo> targetLanguages) {
        this.targetLanguages = targetLanguages;
    }

    // Getter 和 Setter 方法
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 语言信息内部类（对应单个语言对象）
     */
    public static class LanguageInfo {
        private String code; // 语言编码（如"zh"、"en"、"auto"）
        private String name; // 语言名称（如"中文（普通话）"、"自动检测"）

        // Getter 和 Setter 方法
        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}