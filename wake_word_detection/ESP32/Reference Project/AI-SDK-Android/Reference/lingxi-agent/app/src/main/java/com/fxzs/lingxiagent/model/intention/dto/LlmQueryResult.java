package com.fxzs.lingxiagent.model.intention.dto;

import java.util.List;

public class LlmQueryResult {
    public Boolean success;
    public String message;
    public Data data;

    public static class Data {
        public Prediction prediction;
        public String request_id;
    }

    public static class Prediction {
        public PredictionAction action;
        public PredictionMetadata metadata;
        public String session_id;
        public String text;
    }

    public static class PredictionMetadata {
        public Double processing_time_ms;
    }

    public static class PredictionAction {
        public String action_type;
        public List<Double> from;
        public String text;
        public String target;
        public String direction;
        public List<Double> to;
        public String thought;
        public String app_id;
        public String app_name;
        public String package_name;
    }
}
