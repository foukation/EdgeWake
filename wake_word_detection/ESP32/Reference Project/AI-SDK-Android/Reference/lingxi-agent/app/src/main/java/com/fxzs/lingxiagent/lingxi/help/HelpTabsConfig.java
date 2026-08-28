package com.fxzs.lingxiagent.lingxi.help;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HelpTabsConfig {
    private final List<HelpTabConfig> tabs;

    public HelpTabsConfig(List<HelpTabConfig> tabs) {
        this.tabs = tabs;
    }

    public static HelpTabsConfig fromJson(JSONObject jsonObject) throws JSONException {
        JSONArray tabsJsonArray = jsonObject.getJSONArray("tabs");
        List<HelpTabConfig> tabs = new ArrayList<>();

        for (int i = 0; i < tabsJsonArray.length(); i++) {
            JSONObject tabJson = tabsJsonArray.getJSONObject(i);
            JSONArray contentJsonArray = tabJson.getJSONArray("content");
            List<String> content = new ArrayList<>();

            for (int j = 0; j < contentJsonArray.length(); j++) {
                content.add(contentJsonArray.getString(j));
            }

            tabs.add(new HelpTabConfig(tabJson.getString("title"), content));
        }

        return new HelpTabsConfig(
                tabs
        );
    }

    public List<HelpTabConfig> getTabs() {
        return tabs;
    }
}
