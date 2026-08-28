package com.fxzs.lingxiagent.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GlobalDataHolder {
    private static String gptModel;
    private static List<String> customModels = null;
    private static boolean checkAccessOnStart;
    private static boolean defaultEnableTts;
    private static boolean defaultEnableMultiChat;
    private static int selectedTab;
    private static boolean enableInternetAccess;
    private static int webMaxCharCount;
    private static boolean onlyLatestWebResult;
    private static boolean limitVisionSize;
    private static boolean autoSaveHistory;
    private static String video;
    private static String model;
    private static int translateFromTab;
    private static int translateToTab;

    private static SharedPreferences sp = null;

    public static void init(Context context) {
        sp = context.getSharedPreferences("lingxi_assistant", Context.MODE_PRIVATE);
        loadGptApiInfo();
        loadStartUpSetting();
        loadTtsSetting();
        loadMultiChatSetting();
        loadSelectedTab();
        loadModel();
        loadVideo();
        loadFunctionSetting();
        loadVisionSetting();
        loadHistorySetting();
    }

    public static void loadGptApiInfo() {
        customModels = new ArrayList<>(Arrays.asList(sp.getString("custom_models", "").split(";")));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            customModels.removeIf(String::isEmpty);
        }
    }

    public static void loadStartUpSetting() {
        checkAccessOnStart = sp.getBoolean("check_access_on_start", true);
    }

    public static void saveStartUpSetting(boolean checkAccess) {
        checkAccessOnStart = checkAccess;
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("check_access_on_start", checkAccessOnStart);
        editor.apply();
    }

    /**
     * 控制回复是否需要语音播报
     */
    public static void loadTtsSetting() {
        defaultEnableTts = sp.getBoolean("tts_enable", true);
    }

    public static void saveTtsSetting(boolean enable) {
        defaultEnableTts = enable;
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("tts_enable", defaultEnableTts);
        editor.apply();
    }

    public static void loadMultiChatSetting() {
        defaultEnableMultiChat = sp.getBoolean("default_enable_multi_chat", false);
    }

    public static void saveMultiChatSetting(boolean defaultEnable) {
        defaultEnableMultiChat = defaultEnable;
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("default_enable_multi_chat", defaultEnableMultiChat);
        editor.apply();
    }

    public static void loadSelectedTab() {
        selectedTab = sp.getInt("selected_tab", -1);
        translateFromTab = sp.getInt("translate_from_tab", 1);
        translateToTab = sp.getInt("translate_to_tab", 0);
    }

    public static void loadModel() {
        model = sp.getString("model", "Groq");
    }

    public static void loadVideo() {
        video = sp.getString("video", "小度");
    }

    public static void saveMode(String modelVal) {
        model = modelVal;
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("model", modelVal);
        editor.apply();
    }

    public static void saveVideo(String videoVal) {
        video = videoVal;
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("video", videoVal);
        editor.apply();
    }

    public static void saveSelectedTab(int tab) {
        selectedTab = tab;
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("selected_tab", selectedTab);
        editor.apply();
    }

    public static void saveTranslateFromTab(int tab) {
        translateFromTab = tab;
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("translate_from_tab", translateFromTab);
        editor.apply();
    }

    public static void saveTranslateToTab(int tab) {
        translateToTab = tab;
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("translate_to_tab", translateToTab);
        editor.apply();
    }

    public static void loadFunctionSetting() {
        enableInternetAccess = sp.getBoolean("enable_internet", false);
        webMaxCharCount = sp.getInt("web_max_char_count", 2000);
        onlyLatestWebResult = sp.getBoolean("only_latest_web_result", false);
    }

    public static void saveFunctionSetting(boolean enableInternet, int maxCharCount, boolean onlyLatest) {
        enableInternetAccess = enableInternet;
        webMaxCharCount = maxCharCount;
        onlyLatestWebResult = onlyLatest;
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("enable_internet", enableInternetAccess);
        editor.putInt("web_max_char_count", webMaxCharCount);
        editor.putBoolean("only_latest_web_result", onlyLatestWebResult);
        editor.apply();
    }

    public static void loadVisionSetting() {
        limitVisionSize = sp.getBoolean("limit_vision_size", false);
    }

    public static void saveVisionSetting(boolean limitSize) {
        limitVisionSize = limitSize;
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("limit_vision_size", limitVisionSize);
        editor.apply();
    }

    public static void loadHistorySetting() {
        autoSaveHistory = sp.getBoolean("auto_save_history", true);
    }

    public static void saveHistorySetting(boolean autoSave) {
        autoSaveHistory = autoSave;
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("auto_save_history", autoSaveHistory);
        editor.apply();
    }

    public static String getGptModel() { return gptModel; }

    public static String getModel() { return model; }

    public static String getVideo() { return video; }

    public static List<String> getCustomModels() { return customModels; }

    public static boolean getCheckAccessOnStart() { return checkAccessOnStart; }

    public static boolean getDefaultEnableTts() { return defaultEnableTts; }

    public static boolean getDefaultEnableMultiChat() { return defaultEnableMultiChat; }

    public static int getWebMaxCharCount() { return webMaxCharCount; }

    public static int getTranslateFromTab() { return translateFromTab; }

    public static int getTranslateToTab() { return translateToTab; }

    public static boolean getOnlyLatestWebResult() { return onlyLatestWebResult; }

    public static boolean getLimitVisionSize() { return limitVisionSize; }

    public static boolean getAutoSaveHistory() { return autoSaveHistory; }
}
