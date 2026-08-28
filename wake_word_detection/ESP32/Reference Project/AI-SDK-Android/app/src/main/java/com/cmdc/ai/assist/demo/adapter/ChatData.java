package com.cmdc.ai.assist.demo.adapter;

import java.io.Serializable;

public class ChatData implements Serializable {
    public boolean isSend;
    public String text;
    public String mediaUrl;
    public String albumName;
    public String pic;
    public boolean isGenerating = false;
    public int percent = 0;
    public boolean isMusicPlay = false;

    public ChatData(boolean isSend, String text) {
        this.isSend = isSend;
        this.text = text;
    }
}
