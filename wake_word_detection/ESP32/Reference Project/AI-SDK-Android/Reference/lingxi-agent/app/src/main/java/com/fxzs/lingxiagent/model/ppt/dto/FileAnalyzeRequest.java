package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 文档解析请求DTO
 */
public class FileAnalyzeRequest {
    
    @SerializedName("fileUrl")
    private String fileUrl;
    
    @SerializedName("fileType")
    private String fileType; // PDF、DOC、DOCX、PPT、PPTX、MD、TXT、XLS、XLSX、CSV、PNG、JPG、JPEG、BMP、GIF、WEBP、HEIC、EPS、ICNS、IM、PCX、PPM、TIFF、XBM、HEIF、JP2
    
    public FileAnalyzeRequest() {
    }
    
    public FileAnalyzeRequest(String fileUrl, String fileType) {
        this.fileUrl = fileUrl;
        this.fileType = fileType;
    }
    
    // Getters and Setters
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    /**
     * 根据文件URL自动推断文件类型
     */
    public void autoDetectFileType() {
        if (fileUrl != null && fileType == null) {
            String url = fileUrl.toLowerCase();
            if (url.endsWith(".pdf")) {
                fileType = "PDF";
            } else if (url.endsWith(".doc")) {
                fileType = "DOC";
            } else if (url.endsWith(".docx")) {
                fileType = "DOCX";
            } else if (url.endsWith(".ppt")) {
                fileType = "PPT";
            } else if (url.endsWith(".pptx")) {
                fileType = "PPTX";
            } else if (url.endsWith(".md")) {
                fileType = "MD";
            } else if (url.endsWith(".txt")) {
                fileType = "TXT";
            } else if (url.endsWith(".xls")) {
                fileType = "XLS";
            } else if (url.endsWith(".xlsx")) {
                fileType = "XLSX";
            } else if (url.endsWith(".csv")) {
                fileType = "CSV";
            } else if (url.endsWith(".png")) {
                fileType = "PNG";
            } else if (url.endsWith(".jpg") || url.endsWith(".jpeg")) {
                fileType = "JPG";
            } else if (url.endsWith(".bmp")) {
                fileType = "BMP";
            } else if (url.endsWith(".gif")) {
                fileType = "GIF";
            } else if (url.endsWith(".webp")) {
                fileType = "WEBP";
            }
        }
    }
}