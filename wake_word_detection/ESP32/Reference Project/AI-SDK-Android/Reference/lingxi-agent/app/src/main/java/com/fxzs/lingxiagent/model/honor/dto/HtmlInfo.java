package com.fxzs.lingxiagent.model.honor.dto;

import android.os.Parcel;
import android.os.Parcelable;

public class HtmlInfo implements Parcelable {
    private String url;
    private String params;
    private int mode;
    private int height;
    private int width;

    public HtmlInfo(String url, String params, int mode, int height, int width) {
        this.url = url;
        this.params = params;
        this.mode = mode;
        this.height = height;
        this.width = width;
    }

    protected HtmlInfo(Parcel in) {
        url = in.readString();
        params = in.readString();
        mode = in.readInt();
        height = in.readInt();
        width = in.readInt();
    }

    public static final Creator<HtmlInfo> CREATOR = new Creator<HtmlInfo>() {
        @Override
        public HtmlInfo createFromParcel(Parcel in) {
            return new HtmlInfo(in);
        }

        @Override
        public HtmlInfo[] newArray(int size) {
            return new HtmlInfo[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeInt(mode);
        dest.writeInt(height);
        dest.writeInt(width);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters and setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getParams() { return params; }
    public void setParams(String params) {
        this.params = params;
    }

    public int getMode() { return mode; }
    public void setMode(int mode) { this.mode = mode; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
}

