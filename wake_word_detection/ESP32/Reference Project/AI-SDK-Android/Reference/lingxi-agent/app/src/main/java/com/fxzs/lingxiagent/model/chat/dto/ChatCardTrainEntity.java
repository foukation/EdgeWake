package com.fxzs.lingxiagent.model.chat.dto;

public class ChatCardTrainEntity {

    /**
     * moreUrl : https://m.ctrip.com/webapp/train/list?allianceid=&sid=&ticketType=0&dStation=北京&aStation=深圳&dDate=2025-08-14
     * icon : https://e3f49eaa46b57.cdn.sohucs.com/2025/5/27/11/41/MTAwMTIyXzE3NDgzMTcyNzYxNTY=.png
     * tag : 价格最优
     * id : 152596
     * processed_at : 2025-08-07T00:42:56
     * trainNo : K105
     * start_station : 北京西
     * start_city : 北京
     * end_station : 深圳东
     * end_city : 深圳
     * totaltime : 29小时9分钟
     * start_date : 2025-08-07
     * start_time : 23:31
     * end_time : 4:40
     * dayDiff : 2
     * tickets : [{"ticket_left":"99张","ticket_type":"硬座","ticket_price":"254.5"},{"ticket_left":"0张","ticket_type":"硬卧上","ticket_price":"434.5"},{"ticket_left":"0张","ticket_type":"硬卧中","ticket_price":"449.5"},{"ticket_left":"0张","ticket_type":"硬卧下","ticket_price":"464.5"},{"ticket_left":"4张","ticket_type":"软卧上","ticket_price":"687.5"},{"ticket_left":"4张","ticket_type":"软卧下","ticket_price":"717.5"},{"ticket_left":"99张","ticket_type":"无座","ticket_price":"254.5"}]
     * h5Url : https://m.ctrip.com/webapp/train-main/trainXPage?departStation=北京西&arriveStation=深圳东&departDate=2025-08-14&trainNo=K105&allianceid=6470339&sid=210062357
     * ticket_price : 254.5
     */

    private String moreUrl;
    private String icon;
    private String tag;
    private int id;
    private String processed_at;
    private String trainNo;
    private String start_station;
    private String start_city;
    private String end_station;
    private String end_city;
    private String totaltime;
    private String start_date;
    private String start_time;
    private String end_time;
    private int dayDiff;
    private String tickets;
    private String h5Url;
    private String ticket_price;

    public String getMoreUrl() {
        return moreUrl;
    }

    public void setMoreUrl(String moreUrl) {
        this.moreUrl = moreUrl;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProcessed_at() {
        return processed_at;
    }

    public void setProcessed_at(String processed_at) {
        this.processed_at = processed_at;
    }

    public String getTrainNo() {
        return trainNo;
    }

    public void setTrainNo(String trainNo) {
        this.trainNo = trainNo;
    }

    public String getStart_station() {
        return start_station;
    }

    public void setStart_station(String start_station) {
        this.start_station = start_station;
    }

    public String getStart_city() {
        return start_city;
    }

    public void setStart_city(String start_city) {
        this.start_city = start_city;
    }

    public String getEnd_station() {
        return end_station;
    }

    public void setEnd_station(String end_station) {
        this.end_station = end_station;
    }

    public String getEnd_city() {
        return end_city;
    }

    public void setEnd_city(String end_city) {
        this.end_city = end_city;
    }

    public String getTotaltime() {
        return totaltime;
    }

    public void setTotaltime(String totaltime) {
        this.totaltime = totaltime;
    }

    public String getStartDate() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }

    public String getStart_time() {
        return start_time;
    }

    public void setStart_time(String start_time) {
        this.start_time = start_time;
    }

    public String getEnd_time() {
        return end_time;
    }

    public void setEnd_time(String end_time) {
        this.end_time = end_time;
    }

    public int getDayDiff() {
        return dayDiff;
    }

    public void setDayDiff(int dayDiff) {
        this.dayDiff = dayDiff;
    }

    public String getTickets() {
        return tickets;
    }

    public void setTickets(String tickets) {
        this.tickets = tickets;
    }

    public String getH5Url() {
        return h5Url;
    }

    public void setH5Url(String h5Url) {
        this.h5Url = h5Url;
    }

    public String getTicket_price() {
        return ticket_price;
    }

    public void setTicket_price(String ticket_price) {
        this.ticket_price = ticket_price;
    }

    public class TicketsBean {

        /**
         * ticket_left : 99张
         * ticket_type : 硬座
         * ticket_price : 254.5
         */

        private String ticket_left;
        private String ticket_type;
        private String ticket_price;

        public String getTicket_left() {
            return ticket_left;
        }

        public void setTicket_left(String ticket_left) {
            this.ticket_left = ticket_left;
        }

        public String getTicket_type() {
            return ticket_type;
        }

        public void setTicket_type(String ticket_type) {
            this.ticket_type = ticket_type;
        }

        public String getTicket_price() {
            return ticket_price;
        }

        public void setTicket_price(String ticket_price) {
            this.ticket_price = ticket_price;
        }
    }
}
