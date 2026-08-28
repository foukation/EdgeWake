package com.fxzs.lingxiagent.model.intention.dto;

import java.io.Serializable;

public class TripCreateRes implements Serializable {
    public int code;
    public String msg;
    public TripCreateData data;

    public static class TripCreateData implements Serializable{
        public int taskId;
    }
}

