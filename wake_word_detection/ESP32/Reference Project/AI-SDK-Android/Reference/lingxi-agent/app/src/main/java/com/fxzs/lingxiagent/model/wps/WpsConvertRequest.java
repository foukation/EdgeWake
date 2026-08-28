package com.fxzs.lingxiagent.model.wps;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WpsConvertRequest {

    @SerializedName("url")
    public String url;

    @SerializedName("filename")
    public String filename;

    @SerializedName("ranges")
    public String pages;

    @SerializedName("from_page")
    public Integer fromPage;

    @SerializedName("to_page")
    public Integer toPage;

    @SerializedName("page_num_begin")
    public Integer pageNumBegin;

    @SerializedName("page_num_end")
    public Integer pageNumEnd;

    @SerializedName("to_format")
    public String toFormat;

    @SerializedName("img_urls")
    public List<String> imgUrls;

}
