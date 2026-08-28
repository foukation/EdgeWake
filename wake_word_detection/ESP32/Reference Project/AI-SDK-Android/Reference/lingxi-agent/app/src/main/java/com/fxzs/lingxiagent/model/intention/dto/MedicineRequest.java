package com.fxzs.lingxiagent.model.intention.dto;

import java.util.List;

public class MedicineRequest {
    public String medicine_info;

    // 闹钟信息
    public static class Alarm {
        public int id;
        public String time;
        public String label;
        public Boolean enabled;
        public List<String> repeat;
    }

    // 响应内容
    public static class MedicineResponseContent {
        public List<Alarm> alarms;
    }

    // 完整响应数据结构
    public static class MedicineResponse {
        public String state;
        public MedicineResponseContent content;
        public Double time;
    }
}
