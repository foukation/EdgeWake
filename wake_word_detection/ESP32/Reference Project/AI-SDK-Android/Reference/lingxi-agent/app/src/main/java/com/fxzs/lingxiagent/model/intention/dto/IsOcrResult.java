package com.fxzs.lingxiagent.model.intention.dto;

import java.util.ArrayList;

public class IsOcrResult {
    public String state;
    public OcrContent content;
    public static class OcrContent {
        public int state;
        public ArrayList<Float> coord;
    }
}

