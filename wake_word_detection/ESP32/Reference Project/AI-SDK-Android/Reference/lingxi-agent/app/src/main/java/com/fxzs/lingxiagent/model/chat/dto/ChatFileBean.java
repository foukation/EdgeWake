package com.fxzs.lingxiagent.model.chat.dto;

import android.net.Uri;

import java.io.Serializable;

public class ChatFileBean implements Serializable {

    String name;
    String fileType;//PDF等
    String fileSize;//文件大小
    long fileSizeLong;//文件大小long b
    String path;
    String fileUriString;
//    Uri uri;
    String FileAnalyse;//文件类型需要传

    long percent;
    boolean isImage = false;



    public ChatFileBean(String name) {
        this.name = name;
    }
    public ChatFileBean(String name, boolean isImage) {
        this.name = name;
        this.isImage = isImage;
    }

    public ChatFileBean(String name, String path, boolean isImage) {
        this.name = name;
        this.path = path;
        this.isImage = isImage;
    }
    public ChatFileBean(String name, boolean isImage, String fileUriString) {
        this.name = name;
        this.fileUriString = fileUriString;
        this.isImage = isImage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isImage() {
        return isImage;
    }

    public void setImage(boolean image) {
        isImage = image;
    }


    public long getPercent() {
        return percent;
    }

    public void setPercent(long percent) {
        this.percent = percent;
    }
    // 获取 Uri
    public Uri getFileUri() {
        return fileUriString != null ? Uri.parse(fileUriString) : null;
    }

    // 设置 Uri
    public void setFileUri(Uri fileUri) {
        this.fileUriString = fileUri != null ? fileUri.toString() : null;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileUriString() {
        return fileUriString;
    }

    public void setFileUriString(String fileUriString) {
        this.fileUriString = fileUriString;
    }

    public long getFileSizeLong() {
        return fileSizeLong;
    }

    public void setFileSizeLong(long fileSizeLong) {
        this.fileSizeLong = fileSizeLong;
    }

    public String getFileAnalyse() {
        return FileAnalyse;
    }

    public void setFileAnalyse(String fileAnalyse) {
        FileAnalyse = fileAnalyse;
    }
    
    // 新增辅助方法
    
    /**
     * 检查是否为网络文件
     * @return 如果是网络文件返回true
     */
    public boolean isNetworkFile() {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }
    
    /**
     * 获取显示名称
     * @return 显示名称，如果name为空则返回"未知文件"
     */
    public String getDisplayName() {
        return name != null && !name.isEmpty() ? name : "未知文件";
    }
    
    /**
     * 获取有效路径
     * @return 有效的文件路径或URI字符串
     */
    public String getEffectivePath() {
        if (path != null && !path.isEmpty()) {
            return path;
        } else if (fileUriString != null && !fileUriString.isEmpty()) {
            return fileUriString;
        }
        return "";
    }
}
