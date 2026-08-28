package com.fxzs.lingxiagent.model.intention.dto;

import java.io.Serializable;
import java.util.ArrayList;

public class TripContentRes implements Serializable {
    public int code;
    public String msg;
    public TripListData data;

    public static class TripListData implements Serializable {
        public int count;
        public int pageIndex;
        public int pageSize;
        public ArrayList<TripItemContent> list;
    }

    public static class TripItemContent implements Serializable {
        public int id;
        public String title;
        public String description;
        public String command;
        public int status;
        public int sub_status1;
        public int sub_status2;
        public String createdAt;
        public TripResultContent result;
        public TripAllResultContent all_result;
    }

    public static class TripResultContent implements Serializable {
        public String file_url;
    }

    public static class TripAllResultContent implements Serializable {
        public PoiSearchResultContent poi_search_result;
        public String status_summary_info;
    }

    public static class PoiSearchResultContent implements Serializable {
        public String top1_poi_recommend_reason;
        public String title;
        public String sub_title;
        public ArrayList<PoiItem> poi_list;
    }

    public static class PoiItem implements Serializable {
        public String name;
        public String address;
        public String opentime_week;
        public String rating;
        public ArrayList<String> poi_photos;
        public String type;
        public String recommend_reason;
        public String location;
    }
}

