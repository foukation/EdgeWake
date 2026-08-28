package com.fxzs.lingxiagent.model.common;

import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

/**
 * 应用常量类
 */
public class Constants {
    // Unified Certification Center
    public static final String APP_ID = "02009401";
    public static final String APP_KEY = "F725672A4D107AB2";
    // Key for data encryption
    public static final String KEY_ALIAS = "F13x3TyMoUToL3gh";
    /**
     * 校验签名 pad
     */
    public static final String X_SECRET_PAD="Yaj3PkJ8Xb2YQQIq";
    public static final String CLIENT_ID = "lingxi_android";
    public static final String CLIENT_PAD_ID = "lingxi_pad";
    public static final String PROJECT_CODE = "lingxi";

    /**
     * 正式环境 统一端口地址
     */
    public static final String BASE_HOST_URL = "ivs.chinamobiledevice.com:11443/lingxi/";
    /**
     * 测试环境 统一端口地址
     */
    public static final String BASE_HOST_TEST_URL = "z5f3vhk2.cxzfdm.com:30101/lingxi/";

    // 统一控制切换测试和线上地址
    public static String BASE_URL_CONTROL = BASE_HOST_URL;

    // API基础URL
    public static String BASE_URL = "https://"+BASE_URL_CONTROL;

    //    public static final String BASE_URL = "https://yd-out.jmkjsh.com/";
//    public static final String BASE_URL_V1 = "http://36.213.71.163:11453/";
//    public static final String BASE_URL_V2 = "http://36.213.71.163:11470/";
//    public static final String BASE_URL_V3 = "http://36.213.71.163:11507/";
//    public static final String BASE_URL_V4 = "http://36.213.71.163:11508/";
  //   public static String BASE_URL_HONOR = "https://honor.tscfn.cn/";
    //苏州
    public static String BASE_URL_SZ_HONOR = "https://ivs.chinamobiledevice.com:11443/lingxi/app-api/agent/api/";
    //荣耀测试
    public static String BASE_URL_TEST_HONOR = "https://test.honor.tscfn.cn/";

    //控制出行荣耀接口BASE_URL_HONOR
    public static String BASE_URL_HONOR_CONTROL = "https://ivs.chinamobiledevice.com:11443/lingxi/app-api/agent/api/";

    public static final String BASE_URL_SCENE = "https://ivs.chinamobiledevice.com:11443/lingxi/app-api/agent/";
    public static final String BASE_URL_AGREEMENT = "https://ivs.chinamobiledevice.com:11443/lingxi/h5/h5/static/";
    public static final String TOKEN_SHA256_KEY = "c08be96456977eb5dce9468f0b461069e24fb1f51a74cf32a08a6e382031f067";
    //测试地址
    //    public static final String BASE_URL_DEEP_RESEARCH = "http://36.213.71.163:11440";
//    生产地址
    // 深度研究api调用
    public static final String BASE_URL_DEEP_RESEARCH = "https://ivs.chinamobiledevice.com:11443/lingxi/app-api/agent/";
    // 深度研究报考详情页展示
    public static final String BASE_URL_DEEP_RESEARCH_DETAIL = "https://h5.lfyai.com:18088";
    //    public static final String BASE_WS_URL = "wss://yd-out.jmkjsh.com/";
    public static final String BASE_WS_URL = "wss://"+BASE_URL_CONTROL;
    // PPT生成专用URL
    public static final String BASE_URL_PPT = BASE_URL;
    public static final String HONOR_MEET = BASE_URL_HONOR_CONTROL + "honor-agent/v1/medical-advice";   // 荣耀同城聚会
    public static final String HONOR_TRIP = BASE_URL_HONOR_CONTROL + "honor-agent/v2/travel-planning";  // 荣耀同城出行
    // 中国移动认证服务条款
    public static final String CM_CONTACT_URL = "https://wap.cmpassport.com/uni-access/contactCm.html";

    // 第三方信息共享清单
    public static final String PERSONAL_INFO_LIST = BASE_URL_AGREEMENT + "agreement_share.html";

    // 灵犀APP用户协议
    public static final String USER_AGREEMENT = BASE_URL_AGREEMENT + "agreement_user.html";

    // 灵犀APP隐私政策（摘要版）
    public static final String PRIVACY_POLICY_SUMMARY = BASE_URL_AGREEMENT + "agreement_privacy.html";

    // 收集个人信息明示清单
    public static final String PARTY_INFO_SHARE_LIST = BASE_URL_AGREEMENT + "agreement_user_info.html";

    // 灵犀APP隐私政策（详细版）
    public static final String PRIVACY_POLICY_DETAILED = BASE_URL_AGREEMENT + "agreement_privacy_detail.html";

    // 注销协议
    public static final String UNSUBSCRIBE_AGREEMENT_URL = BASE_URL_AGREEMENT + "delete_account.html";

    // ppt预览（正式）
    public static final String PPT_PREVIEW =  "https://ivs.chinamobiledevice.com:11443/lingxi-h5/#/previewApp?";
    // AI表格（测试）
    public static final String AI_EXCEL_HOME_TEST =  "https://honor.tscfn.cn/h5/lingxi-agent-web-table/index.html#/tabulation?token=";
    // AI表格（正式）
    public static final String AI_EXCEL_HOME =  "https://ivs.chinamobiledevice.com:11443/lingxi-table/#/tabulation?token=";
    public static final String AI_EXCEL_DETAIL =  "https://excel.jmkjsh.com/#/tabulation?token=xxxx&id=xxxx";
    public static final String HONOR_ACCESS_KEY = "2fe3e88fc9a7f943c5c2cdb8f4a6199c";
    public static final String HONOR_SECRET_KEY= "ecea44d8f0143567a8a45555ac2c6dcfbb4ab231d8185c623f5735c5947d24af";

    // SharedPreferences相关
    public static final String PREF_NAME = "lingxi_pref";
    public static final String PREF_TOKEN = "user_token";
    public static final String PREF_REFRESH_TOKEN = "refresh_token";
    public static final String PREF_EXPIRES_TIME = "expires_time";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_PHONE = "user_phone";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_AVATAR = "user_avatar";
    public static final String PREF_CLIENT_IP = "client_ip";
    public static final String PREF_INTENTION_TOKEN = "intention_token";
    public static final String PREF_CONVERSATION_ID = "conversation_id";//主页最近一次对话id
    public static final String PREF_TRANSLATION_ID = "translation_id";// 对话/同传记录的ID
    public static final String PREF_RECENT_CONVERSATION_LIST = "recent_conversation_list";//主页最近对话列表

    public static final String PREF_AGENT_ID_MAP = "agent_id_map";//智能体缓存id-对话id映射
    // 历史列表缓存（各Tab）
    public static final String PREF_HISTORY_CACHE_CHAT = "history_cache_chat";
    public static final String PREF_HISTORY_CACHE_AGENT = "history_cache_agent";
    public static final String PREF_HISTORY_CACHE_DRAWING = "history_cache_drawing";
    public static final String PREF_HISTORY_CACHE_MEETING = "history_cache_meeting";
    public static final String PREF_HISTORY_CACHE_PPT = "history_cache_ppt";
    public static final String PREF_HISTORY_CACHE_EXCEL = "history_cache_excel";
    public static final String PREF_HISTORY_CACHE_TRANSLATE_PREFIX = "history_cache_translate_"; // 需拼接类型: 1/2
    // 详情离线缓存（按ID存储）
    public static final String PREF_CHAT_HISTORY_BY_ID_PREFIX = "chat_history_by_id_"; // + conversationId
    public static final String PREF_DRAWING_SESSION_BY_ID_PREFIX = "drawing_session_by_id_"; // + sessionId
    public static final String PREF_AGENT_HEAD_BY_MODEL_PREFIX = "agent_head_by_model_"; // + modelId
    public static final String AGENT_TRAVEL = "出行规划";
    public static final String AGENT_GUI = "自动执行";
    public static final String AGENT_TRIP = "同城聚餐";
    public static final String AGENT_MGVIDOE = "咪咕视频";
    public static final String AGENT_FINANCE = "金融领域专业分析报告";
    public static final String AGENT_COMMUNICATION = "通信助手";
    public static final String AGENT_DEEP_RESEARCH = "深度研究";
    // 网络请求超时时间（秒）
    public static final int CONNECT_TIMEOUT = 30;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;

    // 验证码发送场景
    public static final int SCENE_LOGIN = 1;         // 登录
    public static final int SCENE_CHANGE_PHONE = 2;  // 更换手机号
    public static final int SCENE_RESET_PWD = 3;     // 重置密码
    public static final int SCENE_REGISTER = 4;      // 注册用户
    public static final int SCENE_DELETE_USER = 5;   // 注销用户
    public static final int SCENE_CHANGE_PWD = 7;   // 修改密码

    // 验证码倒计时（秒）
    public static final int SMS_COUNTDOWN = 60;
    // 验证码长度
    public static final int VERIFICATION_CODE_LEN = 6;
    // 密码最小长度
    public static final int PASSWORD_MIN_LEN = 8;
    // 密码最大长度
    public static final int PASSWORD_MAX_LEN = 20;
    public static final int ERROR_CODE_LOGIN_FAIL_PWD2 = 1004003009;
    public static final int ERROR_CODE_LOGIN_FAIL_PWD = 1004003000;
    public static final int ERROR_CODE_LOGIN_FAIL_VCODE = 1002014004;
    public static final int ERROR_CODE_USER_FAIL_VCODE = 1004003007;
    public static final int ERROR_CODE_USER_EXIST_VCODE = 1004001006;//注册用户已经存在

    // 请求Header
    public static final String HEADER_CLIENT_ID = "client-id";

    public static final String HEADER_BILL_CLIENT_ID = "clientId";
    public static final String HEADER_PROJECT_CODE = "project-code";
    public static final String HEADER_VERSION = "version";
    public static final String HEADER_TIME = "ts";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_BEARER = "Bearer ";
    public static final String X_CLIENT_IP = "X-Client-Ip";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json; charset=utf-8";
    // SharedPreferences Keys（用于VMLogin）
    public static final String KEY_TOKEN = PREF_TOKEN;
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";
    public static final String KEY_USER_PHONE = PREF_USER_PHONE;
    public static final String KEY_USER_ID = PREF_USER_ID;
    public static final String KEY_IS_AUTO = "KEY_IS_AUTO";
    public static final String LING_XI_MODEL = "10086";
    public static final String DEEPRESEARCH_QUERY = "deep_research_query";
    public static final String DEEPRESEARCH_REQ_ID = "deep_research_req_id";

    public static final String SP_TYPE_PERMISSIONS_PHONE_STATE = "SP_TYPE_PERMISSIONS_PHONE_STATE";
    public static final String SP_TYPE_PERMISSIONS_LOCATION = "SP_TYPE_PERMISSIONS_LOCATION";
    public static final String SP_TYPE_PERMISSIONS_AUDIO = "SP_TYPE_PERMISSIONS_AUDIO";
    public static final String SP_TYPE_PERMISSIONS_EXTERNAL_STORAGE = "SP_TYPE_PERMISSIONS_EXTERNAL_STORAGE";
    public static final String SP_TYPE_PERMISSIONS_EXTERNAL_STORAGE_WRITE = "SP_TYPE_PERMISSIONS_EXTERNAL_STORAGE_WRITE";
    public static final String SP_TYPE_PERMISSIONS_CAMERA = "SP_TYPE_PERMISSIONS_CAMERA";
    public static final String SP_TYPE_PERMISSIONS_READ_MEDIA_AUDIO = "SP_TYPE_PERMISSIONS_READ_MEDIA_AUDIO";
    public static final String SP_TYPE_PERMISSIONS_BLUETOOTH_CONNECT = "SP_TYPE_PERMISSIONS_BLUETOOTH_CONNECT";
    public static final String SP_TYPE_PERMISSIONS_READ_CONTACTS = "SP_TYPE_PERMISSIONS_READ_CONTACTS";
    public static final String SP_TYPE_PERMISSIONS_CALL_PHONE = "SP_TYPE_PERMISSIONS_CALL_PHONE";
    public static final String SP_TYPE_PERMISSIONS_SEND_SMS = "SP_TYPE_PERMISSIONS_SEND_SMS";
    public static final String SP_TYPE_PERMISSIONS_POST_NOTIFICATIONS = "SP_TYPE_PERMISSIONS_POST_NOTIFICATIONS";
    public static final String SP_TYPE_PERMISSIONS_EXCEL_FILE = "SP_TYPE_PERMISSIONS_EXCEL_FILE";

    // WPS Convert/OCR API
    public static final String WPS_BASE_URL = "https://solution.wps.cn";
//    public static final String WPS_APP_KEY = "SX20260128KXOVXP"; // TODO: Fill in your AppKey
//    public static final String WPS_APP_SECRET = "HhOPveVLUEedmvLBfcWQObcZUdftzYNr"; // TODO: Fill in your AppSecret
    public static final String WPS_APP_KEY = "SX20260304PCAZEA"; // TODO: Fill in your AppKey
    public static final String WPS_APP_SECRET = "fFxFixtcznzFLAdECXWFjgxvkJMlugTI"; // TODO: Fill in your AppSecret

    // public static final String WPS_APP_KEY = "SX20260310LIJSYF"; // TODO: Fill in your AppKey
    // public static final String WPS_APP_SECRET = "dWOErwSgywxlMWLRfwGOoeGaMgxBPtbj"; // TODO: Fill in your AppSecret

    public static final int AUTH_ERROR = 1401;

    // ==================== 性能优化配置 ====================

    /**
     * 消息数量限制 - 低端设备（< 3GB RAM）
     * 限制内存中保留的消息数量，避免大数据量导致卡顿
     */
    public static final int MAX_MESSAGES_LOW_END = 50;

    /**
     * 消息数量限制 - 中端设备（3-5GB RAM）
     */
    public static final int MAX_MESSAGES_MID_RANGE = 100;

    /**
     * 消息数量限制 - 高端设备（> 5GB RAM）
     */
    public static final int MAX_MESSAGES_HIGH_END = 150;

    /**
     * RecyclerView 缓存池大小 - 低端设备
     */
    public static final int RECYCLER_CACHE_SIZE_LOW_END = 2;

    /**
     * RecyclerView 缓存池大小 - 中端设备
     */
    public static final int RECYCLER_CACHE_SIZE_MID_RANGE = 4;

    /**
     * RecyclerView 缓存池大小 - 高端设备
     */
    public static final int RECYCLER_CACHE_SIZE_HIGH_END = 6;

    /**
     * RecyclerView ViewHolder 缓存数量 - 低端设备
     */
    public static final int RECYCLER_VIEW_CACHE_SIZE_LOW_END = 3;

    /**
     * RecyclerView ViewHolder 缓存数量 - 中端设备
     */
    public static final int RECYCLER_VIEW_CACHE_SIZE_MID_RANGE = 5;

    /**
     * RecyclerView ViewHolder 缓存数量 - 高端设备
     */
    public static final int RECYCLER_VIEW_CACHE_SIZE_HIGH_END = 8;
    /**
     * 对话内容可输入750个字符
     */
    public static final int DIALOG_INPUT_NUMBER = 600;

    public static final String REFRESH_STATUS = "isRefresh";
    public static final String WAKE_CONTENT = "formFloatContent";


    /**
     * 服务器响应体签名
     */
    public static String X_AI_SIGN="";

    public static void switchServer(){
        BASE_URL_CONTROL =  SharedPreferencesUtil.getServerUrl();
        BASE_URL = "https://"+BASE_URL_CONTROL;
    }

    /**
     * 切换荣耀服务器地址
     */
    public static void switchHonorServer(){
        BASE_URL_HONOR_CONTROL =  SharedPreferencesUtil.getServerHonorUrl();
    }

}