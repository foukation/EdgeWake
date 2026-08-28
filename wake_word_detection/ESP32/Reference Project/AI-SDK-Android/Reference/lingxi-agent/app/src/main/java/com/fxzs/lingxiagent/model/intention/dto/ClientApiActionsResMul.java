package com.fxzs.lingxiagent.model.intention.dto;

import java.io.Serializable;
import java.util.ArrayList;

public class ClientApiActionsResMul implements Serializable {

    public int code;
    public String msg;
    public AgentTaskDataMul data;

    public ClientApiActionsResMul(int code, String msg, AgentTaskDataMul data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static class AgentTaskDataMul implements Serializable {
        public int id;
        public String desc;
        public String appId;
        public int typeId;
        public String appName;
        public String queryIntent;
        public String actionList;
        public int execNum;
        public ArrayList<IntentData> intents;
        public MulIntentData mulIntent;

        public AgentTaskDataMul(int id, String desc, String appId, int typeId,
                                String appName, String queryIntent, String actionList,
                                int execNum, ArrayList<IntentData> intents,
                                MulIntentData mulIntent) {
            this.id = id;
            this.desc = desc;
            this.appId = appId;
            this.typeId = typeId;
            this.appName = appName;
            this.queryIntent = queryIntent;
            this.actionList = actionList;
            this.execNum = execNum;
            this.intents = intents;
            this.mulIntent = mulIntent;
        }
    }

    public static class MulIntentData implements Serializable {
        public String ResponseText;
        public String SessionID;
        public boolean IsSingleTurn;
        public String IntentCategory;
        public StandardSchemaData StandardSchema;
        public RecognizedJSON RecognizedJSON;
        public String Status;
        public String Error;

        public MulIntentData(String responseText, String sessionID, boolean isSingleTurn,
                             String intentCategory, StandardSchemaData standardSchema,
                             RecognizedJSON recognizedJSON, String status, String error) {
            this.ResponseText = responseText;
            this.SessionID = sessionID;
            this.IsSingleTurn = isSingleTurn;
            this.IntentCategory = intentCategory;
            this.StandardSchema = standardSchema;
            this.RecognizedJSON = recognizedJSON;
            this.Status = status;
            this.Error = error;
        }
    }

    public static class StandardSchemaData implements Serializable {
        public ArrayList<IntentsData> intents;
    }

    public static class RecognizedJSON implements Serializable {
        public ArrayList<IntentsData> intents;

    }

    public static class IntentsData implements Serializable {
        public SlotsDataContent slots;
        public String intent;
    }

    public static class SlotsDataContent implements Serializable {
        public String 出发地; // chufadi
        public String 目的地; // mudidi
        public String 出发日期; // chufariqi
        public String 车次类型; // checiLeixing
    }

    public static class IntentData implements Serializable {
        public String intent;
        public SlotsData slots;
    }

    public static class SlotsData implements Serializable {
        public String product;
        public String item;
        public String recipient;
        public String location;
        public String messageContent;
        public String destination;
        public String drinkName;
        public String shop;
        public String receiver;
        public String content;
        public String times;
        public String hotel;
        public String name;
        public String direction;
        public String device;
        public String object;
        public String application;
        public String departure;
        public String transportation;
        public String date;
        public String departure_date;
        public String arrival_city;
        public String departure_city;
        public String drink;
        public String sugar;
        public String temperature;
        public String check_in_date;
        public String check_out_date;
        public String city;
        public String hotel_name;
    }
}

