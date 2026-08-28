package com.fxzs.lingxiagent.model.ppt.dto;

import java.util.List;

/**
 * PPT会话数据传输对象
 */
public class PptSessionDto {
    
    private int id;
    private String title;
    private long createTime;
    private long updateTime;
    private String userId;
    private List<PptCover> pptCovers;
    private List<PptCatalog> pptCatalogs;
    private List<PptTask> pptTasks;
    
    // 构造函数
    public PptSessionDto() {}
    
    public PptSessionDto(int id, String title, long createTime, long updateTime, String userId) {
        this.id = id;
        this.title = title;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.userId = userId;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    public long getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public List<PptCover> getPptCovers() {
        return pptCovers;
    }
    
    public void setPptCovers(List<PptCover> pptCovers) {
        this.pptCovers = pptCovers;
    }
    
    public List<PptCatalog> getPptCatalogs() {
        return pptCatalogs;
    }
    
    public void setPptCatalogs(List<PptCatalog> pptCatalogs) {
        this.pptCatalogs = pptCatalogs;
    }
    
    public List<PptTask> getPptTasks() {
        return pptTasks;
    }
    
    public void setPptTasks(List<PptTask> pptTasks) {
        this.pptTasks = pptTasks;
    }
    
    /**
     * PPT封面数据
     */
    public static class PptCover {
        // 根据实际需要添加字段
        private int id;
        private String title;
        private String requestId;
        private String coverImage;
        private String coverId;
        private long createTime;
        private long updateTime;
        private String userId;
        private int sessionId;
        public void setId(int id) {
            this.id = id;
        }
        public int getId() {
            return id;
        }

        public void setTitle(String title) {
            this.title = title;
        }
        public String getTitle() {
            return title;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
        public String getRequestId() {
            return requestId;
        }

        public void setCoverImage(String coverImage) {
            this.coverImage = coverImage;
        }
        public String getCoverImage() {
            return coverImage;
        }

        public void setCoverId(String coverId) {
            this.coverId = coverId;
        }
        public String getCoverId() {
            return coverId;
        }

        public void setCreateTime(long createTime) {
            this.createTime = createTime;
        }
        public long getCreateTime() {
            return createTime;
        }

        public void setUpdateTime(long updateTime) {
            this.updateTime = updateTime;
        }
        public long getUpdateTime() {
            return updateTime;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
        public String getUserId() {
            return userId;
        }

        public void setSessionId(int sessionId) {
            this.sessionId = sessionId;
        }
        public int getSessionId() {
            return sessionId;
        }
    }
    
    /**
     * PPT目录数据
     */
    public static class PptCatalog {
        private int id;
        private String title;
        private String requestId;
        private String catalogs;
        private long createTime;
        private long updateTime;
        private String userId;
        private int sessionId;
        
        // Getters and Setters
        public int getId() {
            return id;
        }
        
        public void setId(int id) {
            this.id = id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getRequestId() {
            return requestId;
        }
        
        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
        
        public String getCatalogs() {
            return catalogs;
        }
        
        public void setCatalogs(String catalogs) {
            this.catalogs = catalogs;
        }
        
        public long getCreateTime() {
            return createTime;
        }
        
        public void setCreateTime(long createTime) {
            this.createTime = createTime;
        }
        
        public long getUpdateTime() {
            return updateTime;
        }
        
        public void setUpdateTime(long updateTime) {
            this.updateTime = updateTime;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public int getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(int sessionId) {
            this.sessionId = sessionId;
        }
    }
    
    /**
     * PPT任务数据
     */
    public static class PptTask {
        private int id;
        private String title;
        private String requestId;
        private String coverId;
        private String customData;
        private String taskId;
        private String pptUrl;
        private String pptTitle;
        private int pageCount;
        private long createTime;
        private long updateTime;
        private String userId;
        private int sessionId;
        private String type;
        
        // Getters and Setters
        public int getId() {
            return id;
        }
        
        public void setId(int id) {
            this.id = id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getRequestId() {
            return requestId;
        }
        
        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
        
        public String getCoverId() {
            return coverId;
        }
        
        public void setCoverId(String coverId) {
            this.coverId = coverId;
        }
        
        public String getCustomData() {
            return customData;
        }
        
        public void setCustomData(String customData) {
            this.customData = customData;
        }
        
        public String getTaskId() {
            return taskId;
        }
        
        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }
        
        public String getPptUrl() {
            return pptUrl;
        }
        
        public void setPptUrl(String pptUrl) {
            this.pptUrl = pptUrl;
        }
        
        public String getPptTitle() {
            return pptTitle;
        }
        
        public void setPptTitle(String pptTitle) {
            this.pptTitle = pptTitle;
        }
        
        public int getPageCount() {
            return pageCount;
        }
        
        public void setPageCount(int pageCount) {
            this.pageCount = pageCount;
        }
        
        public long getCreateTime() {
            return createTime;
        }
        
        public void setCreateTime(long createTime) {
            this.createTime = createTime;
        }
        
        public long getUpdateTime() {
            return updateTime;
        }
        
        public void setUpdateTime(long updateTime) {
            this.updateTime = updateTime;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public int getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(int sessionId) {
            this.sessionId = sessionId;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
    }
}
