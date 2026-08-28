package com.fxzs.lingxiagent.model.chat.dto;

public class ChatCardPlanEntity {

    /**
     * requestId : 1757295673614
     * code : 200
     * msg : 任务已完成
     * taskstatus : 1
     * content : {"h5Url":"https://test.honor.tscfn.cn/h5/1757295673614_travel_planner_11.html","route":"北京 到 上海","title":"上海一日游","moreUrl":"https://test.honor.tscfn.cn/h5/1757295673614_travel_planner_11.html","subtitle":"探索上海多元魅力","date_range":"2025/09/09","header_background_image_url":"https://dimg04.c-ctrip.com/images/1lo7412000isn0jrbD500_Q50.jpg"}
     */

    private String requestId;
    private int code;
    private String msg;
    private int taskstatus;
    private ContentBean content;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getTaskstatus() {
        return taskstatus;
    }

    public void setTaskstatus(int taskstatus) {
        this.taskstatus = taskstatus;
    }

    public ContentBean getContent() {
        return content;
    }

    public void setContent(ContentBean content) {
        this.content = content;
    }

    public static class ContentBean {
        /**
         * h5Url : https://test.honor.tscfn.cn/h5/1757295673614_travel_planner_11.html
         * route : 北京 到 上海
         * title : 上海一日游
         * moreUrl : https://test.honor.tscfn.cn/h5/1757295673614_travel_planner_11.html
         * subtitle : 探索上海多元魅力
         * date_range : 2025/09/09
         * header_background_image_url : https://dimg04.c-ctrip.com/images/1lo7412000isn0jrbD500_Q50.jpg
         */

        private String h5Url;
        private String route;
        private String title;
        private String moreUrl;
        private String subtitle;
        private String date_range;
        private String header_background_image_url;

        public String getH5Url() {
            return h5Url;
        }

        public void setH5Url(String h5Url) {
            this.h5Url = h5Url;
        }

        public String getRoute() {
            return route;
        }

        public void setRoute(String route) {
            this.route = route;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getMoreUrl() {
            return moreUrl;
        }

        public void setMoreUrl(String moreUrl) {
            this.moreUrl = moreUrl;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public String getDate_range() {
            return date_range;
        }

        public void setDate_range(String date_range) {
            this.date_range = date_range;
        }

        public String getHeader_background_image_url() {
            return header_background_image_url;
        }

        public void setHeader_background_image_url(String header_background_image_url) {
            this.header_background_image_url = header_background_image_url;
        }
    }
}
