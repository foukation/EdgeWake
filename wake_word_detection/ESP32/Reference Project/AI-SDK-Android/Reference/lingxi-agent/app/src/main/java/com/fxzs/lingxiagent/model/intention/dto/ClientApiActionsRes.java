package com.fxzs.lingxiagent.model.intention.dto;

import java.util.ArrayList;

public class ClientApiActionsRes {
    public int code;
    public String msg;
    public String status;
    public AgentTaskData data;

    public static class AgentTaskData {
        public int id;
        public String desc;
        public String appId;
        public int typeId;
        public String appName;
        public String queryIntent;
        public String actionList;
        public int execNum;
        public ArrayList<ClientApiActionsResMul.IntentData> intents;
    }
}

