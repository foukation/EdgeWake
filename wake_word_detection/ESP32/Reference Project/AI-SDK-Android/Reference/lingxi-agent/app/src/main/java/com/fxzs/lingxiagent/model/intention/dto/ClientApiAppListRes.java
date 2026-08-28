package com.fxzs.lingxiagent.model.intention.dto;

import java.util.ArrayList;

public class ClientApiAppListRes {
    public int code;
    public String msg;
    public AppListData data;

    public static class AppListData {
        public int count;
        public int pageIndex;
        public int pageSize;
        public ArrayList<AppData> list;
    }

    public static class AppData {
        public int id;
        public String name;
        public String enName;
        public String packageName;
        public String homeActivity;
    }
}
