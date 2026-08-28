package com.fxzs.lingxiagent.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.fxzs.lingxiagent.model.auth.dto.LoginResponse;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.network.SignVerifier;
import com.fxzs.lingxiagent.model.user.dto.UserDto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * SharedPreferences工具类
 */
public class SharedPreferencesUtil {
    private static SharedPreferences sPreferences;

    public static void init(Context context) {
        if (sPreferences == null) {
            sPreferences = context.getApplicationContext()
                    .getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
            Constants.switchServer();
            Constants.switchHonorServer();
        }
    }

    /**
     * 保存登录信息
     */
    public static void saveLoginInfo(LoginResponse loginResponse) {
        if (loginResponse == null) return;

        SharedPreferences.Editor editor = sPreferences.edit();
        String token = loginResponse.getAccessToken();
        if (!TextUtils.isEmpty(token)) {
            editor.putString(Constants.PREF_TOKEN, AesUtil.encrypt(token, SignVerifier.getClientId()));
        }
        String refreshToken = loginResponse.getRefreshToken();
        if (!TextUtils.isEmpty(refreshToken)) {
            editor.putString(Constants.PREF_REFRESH_TOKEN, AesUtil.encrypt(refreshToken, SignVerifier.getClientId()));
        }
        if (loginResponse.getExpiresTime() != null) {
            editor.putLong(Constants.PREF_EXPIRES_TIME, loginResponse.getExpiresTime());
        }
        if (loginResponse.getUserId() != null) {
            editor.putLong(Constants.PREF_USER_ID, loginResponse.getUserId());
        }

        editor.apply();
    }

    /**
     * 保存用户信息
     */
    public static void saveUserInfo(UserDto userDto) {
        if (userDto == null) return;

        SharedPreferences.Editor editor = sPreferences.edit();
        if (userDto.getAvatar() != null) {
            editor.putString(Constants.PREF_USER_AVATAR, userDto.getAvatar());
        }
        if (userDto.getNickname() != null) {
            editor.putString(Constants.PREF_USER_NAME, userDto.getNickname());
        }
        String mobile = userDto.getMobile();
        if (!TextUtils.isEmpty(mobile)) {
            editor.putString(Constants.PREF_USER_PHONE, AesUtil.encrypt(mobile, SignVerifier.getClientId()));
        }
        if (userDto.getId() != null) {
            editor.putLong(Constants.PREF_USER_ID, userDto.getId());
        }

        editor.apply();
    }

    /**
     * 获取Token
     */
    public static String getToken() {
        String token = sPreferences.getString(Constants.PREF_TOKEN, "");
        return AesUtil.decrypt(token, SignVerifier.getClientId());
    }

    /**
     * 更新intenttion Token
     */
    public static void updateIntentionToken(String token) {
        sPreferences.edit().putString(Constants.PREF_INTENTION_TOKEN, token).apply();
    }

    /**
     * 更新ClientIP
     */
    public static void updateClientIP(String publicIp) {
        sPreferences.edit().putString(Constants.PREF_CLIENT_IP, publicIp).apply();
    }

    /**
     * 获取intenttion Token
     */
    public static String getIntentionToken() {
        return sPreferences.getString(Constants.PREF_INTENTION_TOKEN, "");
    }

    /**
     * 获取ClientIP
     */
    public static String getClientIP() {
        return sPreferences.getString(Constants.PREF_CLIENT_IP, "");
    }

    /**
     * 获取AccessToken (alias for getToken)
     */
    public static String getAccessToken() {
        return getToken();
    }

    /**
     * 获取RefreshToken
     */
    public static String getRefreshToken() {
        String refreshToken = sPreferences.getString(Constants.PREF_REFRESH_TOKEN, "");
        return AesUtil.decrypt(refreshToken, SignVerifier.getClientId());
    }

    /**
     * 获取用户ID
     */
    public static long getUserId() {
        return sPreferences.getLong(Constants.PREF_USER_ID, 0L);
    }

    /**
     * 获取用户ID字符串 (支持测试账号)
     */
    public static String getUserIdStr() {
        long userId = sPreferences.getLong(Constants.PREF_USER_ID, 0L);
        return userId > 0 ? String.valueOf(userId) : "";
    }

    /**
     * 获取用户手机号
     */
    public static String getUserPhone() {
        String mobile = sPreferences.getString(Constants.PREF_USER_PHONE, "");
        return AesUtil.decrypt(mobile, SignVerifier.getClientId());
    }

    /**
     * 保存用户手机号
     */
    public static void saveUserPhone(String phoneNum) {
        String encryptedMobile = AesUtil.encrypt(phoneNum, SignVerifier.getClientId());
        sPreferences.edit().putString(Constants.PREF_USER_PHONE, encryptedMobile).apply();
    }

    /**
     * 获取用户昵称
     */
    public static String getUserName() {
        return sPreferences.getString(Constants.PREF_USER_NAME, "");
    }

    /**
     * 清除登录信息
     */
    public static void clearLoginInfo() {
        SharedPreferences.Editor editor = sPreferences.edit();
        editor.remove(Constants.PREF_TOKEN);
        editor.remove(Constants.PREF_REFRESH_TOKEN);
        editor.remove(Constants.PREF_USER_ID);
        editor.remove(Constants.PREF_USER_PHONE);
        editor.remove(Constants.PREF_USER_NAME);
        editor.remove(Constants.PREF_USER_AVATAR);
        editor.remove(Constants.PREF_EXPIRES_TIME);

        // 清除聊天相关缓存，避免新账户登录时显示旧账号的聊天记录
        editor.remove(Constants.PREF_CONVERSATION_ID);  // 清除当前对话ID
        editor.remove(Constants.PREF_TRANSLATION_ID);   // 清除翻译/同传记录ID
        editor.remove(Constants.PREF_AGENT_ID_MAP);     // 清除智能体ID映射
        editor.remove(Constants.PREF_RECENT_CONVERSATION_LIST); // 清除最近对话列表

        // 清除所有会议话题缓存（通过前缀匹配）
        clearAllMeetingTopics(editor);

        editor.apply();
    }

    /**
     * 清除所有会议话题缓存
     * 私有方法，用于在退出登录时清理所有以 "meeting_topic_" 开头的缓存
     */
    private static void clearAllMeetingTopics(SharedPreferences.Editor editor) {
        // 获取所有键值对
        Map<String, ?> allEntries = sPreferences.getAll();
        for (String key : allEntries.keySet()) {
            if (key.startsWith("meeting_topic_")) {
                editor.remove(key);
            }
        }
    }

    /**
     * 更新Token
     */
    public static void updateToken(String token) {
        String encryptedToken = AesUtil.encrypt(token, SignVerifier.getClientId());
        sPreferences.edit().putString(Constants.PREF_TOKEN, encryptedToken).apply();
    }

    /**
     * 获取Token有效期
     */
    public static long getExpires() {
        return sPreferences.getLong(Constants.PREF_EXPIRES_TIME, 0L);
    }

    /**
     * 保存选中的模型
     */
    public static void setSelectedModel(Context context, String model) {
        init(context);
        sPreferences.edit().putString("selected_model", model).apply();
    }

    /**
     * 获取选中的模型
     */
    public static String getSelectedModel(Context context) {
        init(context);
        return sPreferences.getString("selected_model", "10086");
    }

    /**
     * 获取模型显示名称
     */
    public static String getModelDisplayName(Context context) {
        String model = getSelectedModel(context);
        switch (model) {
            case "deepseek_r1":
                return "DeepSeek R1";
            case "doubao":
                return "豆包";
            case "liantong_yuanjing":
                return "联通元景";
            case "tencent_hunyuan":
                return "腾讯混元";
            default:
                return "DeepSeek R1";
        }
    }

    /**
     * 获取语言设置
     */
    public static String getLanguage(Context context) {
        init(context);
        return sPreferences.getString("app_language", "16k_zh");
    }

    /**
     * 保存语言设置
     */
    public static void saveLanguage(String language) {
        sPreferences.edit().putString("app_language", language).apply();
    }

    /**
     * 获取语言设置 code
     */
    public static String getLanguageCode() {
        return sPreferences.getString("app_languageCode", "16k_zh");
    }

    /**
     * 获取语言设置 code
     */
    public static String getLanguageCode(Context context) {
        init(context);
//        return sPreferences.getString("app_languageCode", "16k_zh");
        return sPreferences.getString("app_languageCode", "1537");
    }

    /**
     * 保存语言设置 code
     */
    public static void saveLanguageCode(String language) {
        sPreferences.edit().putString("app_languageCode", language).apply();
    }

    /**
     * 保存声音设置 (无Context参数)
     */
    public static void saveVoiceOption(String voiceOptions) {
        sPreferences.edit().putString("app_voiceOptions", voiceOptions).apply();
    }

    /**
     * 获取声音设置 (无Context参数)
     */
    public static String getVoiceOption() {
        return sPreferences.getString("app_voiceOptions", "");
    }

    /**
     * 获取语言设置 (无Context参数)
     */
    public static String getLanguage() {
        return sPreferences.getString("app_language", "16k_zh");
    }

    /**
     * 保存用户头像路径
     */
    public static void saveUserAvatar(String avatarPath) {
        sPreferences.edit().putString(Constants.PREF_USER_AVATAR, avatarPath).apply();
    }

    /**
     * 保存用户名称
     */
    public static void saveUserName(String name) {
        sPreferences.edit().putString(Constants.PREF_USER_NAME, name).apply();
    }

    /**
     * 获取用户头像路径
     */
    public static String getUserAvatar() {
        return sPreferences.getString(Constants.PREF_USER_AVATAR, "");
    }

    /**
     * 清除所有数据
     */
    public static void clearAllData() {
        clearLoginInfo();
        // 清除其他数据
        SharedPreferences.Editor editor = sPreferences.edit();
        editor.remove("selected_model");
        editor.remove("app_language");
        editor.remove("test_user_id");
        editor.apply();
    }

    /**
     * 保存字符串值
     */
    public static void saveString(String key, String value) {
        sPreferences.edit().putString(key, value).apply();
    }

    /**
     * 保存布尔值
     */
    public static void saveBoolean(String key, boolean value) {
        sPreferences.edit().putBoolean(key, value).apply();
    }

    /**
     * 获取字符串值
     */
    public static String getString(String key, String defaultValue) {
        return sPreferences.getString(key, defaultValue);
    }

    /**
     * 获取布尔值
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        return sPreferences.getBoolean(key, defaultValue);
    }

    /**
     * 保存整数值
     */
    public static void saveInt(String key, int value) {
        sPreferences.edit().putInt(key, value).apply();
    }

    /**
     * 获取整数值
     */
    public static int getInt(String key, int defaultValue) {
        return sPreferences.getInt(key, defaultValue);
    }

    /**
     * 保存会议话题到本地
     */
    public static void saveMeetingTopic(String meetingId, String topicContent) {
        String key = "meeting_topic_" + meetingId;
        saveString(key, topicContent);
    }

    /**
     * 获取本地保存的会议话题
     */
    public static String getMeetingTopic(String meetingId) {
        String key = "meeting_topic_" + meetingId;
        return getString(key, "");
    }

    /**
     * 清除会议话题缓存
     */
    public static void clearMeetingTopic(String meetingId) {
        String key = "meeting_topic_" + meetingId;
        sPreferences.edit().remove(key).apply();
    }

    //智能体缓存id-对话id映射
    public static void saveAgentMap(String model, String conversationId) {
        Map<String, String> map = new HashMap<>();
        map = getAgentMap();
        map.put(model, conversationId);

        Gson gson = new Gson();
        String jsonString = gson.toJson(map);
        saveString(Constants.PREF_AGENT_ID_MAP, jsonString);
    }

    public static String getAgentMap(String model) {
        String conversationId = "";
        Map<String, String> map = getAgentMap();
        conversationId = map.get(model);
        return conversationId;
    }

    public static Map<String, String> getAgentMap() {
        Map<String, String> map = new HashMap<>();

        String jsonString = getString(Constants.PREF_AGENT_ID_MAP, "");
        if (!TextUtils.isEmpty(jsonString)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            map = gson.fromJson(jsonString, type);
        }
        return map;
    }

    /**
     * 删除指定 conversationId 的所有记录
     * (适用于一个 conversationId 可能对应多个 model 的情况)
     */
    public static boolean removeConversationIdRecords(String conversationId) {
        Map<String, String> map = getAgentMap();
        boolean removed = false;
        // 使用迭代器安全删除
        Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (conversationId.equals(entry.getValue())) {
                iterator.remove();
                removed = true;
            }
        }

        if (removed) {
            Gson gson = new Gson();
            String jsonString = gson.toJson(map);
            saveString(Constants.PREF_AGENT_ID_MAP, jsonString);
        }
        return removed;
    }

    /**
     * 保存/获取 当前会话 translationId（对话/同传记录ID）
     */
    public static void saveTranslationId(long id) {
        sPreferences.edit().putLong(Constants.PREF_TRANSLATION_ID, id).apply();
    }

    public static long getTranslationId() {
        return sPreferences.getLong(Constants.PREF_TRANSLATION_ID, 0L);
    }

    /**
     * 保存首次安装app
     */
    public static void setAgreePrivacy(boolean first) {
        sPreferences.edit().putBoolean("first_jump", first).apply();
    }

    /**
     * 获取首次安装app
     */
    public static boolean isAgreePrivacy(){
        return sPreferences.getBoolean("first_jump", false);
    }

    //设置正式包是否输出log
    public static void saveLogOpen(boolean open) {
        sPreferences.edit().putBoolean("log_open", open).apply();
    }

    public static boolean getLogOpen() {
        return sPreferences.getBoolean("log_open", false);
    }
    public static void saveFirstOpen(boolean first) {
        sPreferences.edit().putBoolean("first", first).apply();
    }

    public static boolean getFirstOpen() {
        return sPreferences.getBoolean("first", true);
    }


    //设置鉴权失败
    public static void saveAuthStatus(boolean auth) {
        sPreferences.edit().putBoolean("is_auth", auth).apply();
    }

    public static boolean getAuthStatus() {
        return sPreferences.getBoolean("is_auth", true);
    }

    public static void saveWidgetIndex(int index) {
        sPreferences.edit().putInt("index_widget", index).apply();
    }

    public static int getWidgetIndex() {
        return sPreferences.getInt("index_widget", 0);
    }
    public static void saveWidgetData(String data) {
        sPreferences.edit().putString("data_widget", data).apply();
    }
    public static void saveWidgetCatDetailListData(String data) {
        sPreferences.edit().putString("data_widget_CatDetailList", data).apply();
    }

    public static String getWidgetCatDetailListData() {
        return sPreferences.getString("data_widget_CatDetailList", "");
    }

    public static String getWidgetIData() {
        return sPreferences.getString("data_widget", "");
    }

    public static void saveServer(String url) {
        sPreferences.edit().putString("serverUrl", url).apply();
    }

    public static String getServerUrl() {
        if (sPreferences == null) {
            return Constants.BASE_HOST_URL;
        }
        return sPreferences.getString("serverUrl", Constants.BASE_HOST_URL);
    }


    /**
     * 荣耀出行服务器地址
     *
     * @param url
     */
    public static void saveHonorServer(String url) {
        sPreferences.edit().putString("serverHonorUrl", url).apply();
    }

    public static String getServerHonorUrl() {
        if (sPreferences == null) {
            return Constants.BASE_URL_HONOR_CONTROL;
        }
        return sPreferences.getString("serverHonorUrl", Constants.BASE_URL_HONOR_CONTROL);
    }
    
    // 保存 Bean 列表
    public static void saveBeanList(Context context, String json) {
        SharedPreferences sp = context.getSharedPreferences("lingxi_widget", Context.MODE_PRIVATE);
        sp.edit().putString("key_widget_beans", json).apply();
    }

    // 获取 Bean 列表
    public static String getBeanList(Context context) {
        SharedPreferences sp = context.getSharedPreferences("lingxi_widget", Context.MODE_PRIVATE);
        return sp.getString("key_widget_beans", "");
    }

    // 保存缓存时间
    public static void saveTime(Context context) {
        SharedPreferences sp = context.getSharedPreferences("lingxi_widget", Context.MODE_PRIVATE);
        sp.edit().putLong("key_cache_time", System.currentTimeMillis()).apply();
    }

    // 判断缓存是否有效（10 分钟有效，可根据需求调整）
    public static boolean isCacheValid(Context context) {
        SharedPreferences sp = context.getSharedPreferences("lingxi_widget", Context.MODE_PRIVATE);
        long lastTime = sp.getLong("key_cache_time", 0);
        // 缓存有效期：10 分钟
        return System.currentTimeMillis() - lastTime < 10 * 60 * 1000;
    }
}