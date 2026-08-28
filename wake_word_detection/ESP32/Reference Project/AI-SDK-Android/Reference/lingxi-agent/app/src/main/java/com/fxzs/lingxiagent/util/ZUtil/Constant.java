package com.fxzs.lingxiagent.util.ZUtil;

public class Constant {
    public static String BASE_URL = "https://china-mobile.jmkjsh.com";

    public static final String INTENT_TYPE = "INTENT_TYPE";
    public static final String INTENT_DATA = "INTENT_DATA";
    public static final String INTENT_DATA1 = "INTENT_DATA1";
    public static final String INTENT_DATA2 = "INTENT_DATA2";
    public static final String INTENT_DATA_GUI_QUERY = "INTENT_DATA_GUI_QUERY";
    public static final String INTENT_ID = "INTENT_ID";


    public  class KEY{
        public static final  String IMG_PATH = "IMG_PATH";
        public static final  String PIC_WIDTH = "PIC_WIDTH";
        public static final  String PIC_HEIGHT = "PIC_HEIGHT";

    }

    public  class ChatFunction{
        public static final  int TYPE_LIFE = 997;//生活
        public static final  int TYPE_PHONE = 998;//通话
        public static final  int TYPE_WORK = 999;//办公
        public static final  int TYPE_PLAY = 1000;//娱乐
        public static final  int TYPE_AI_WRITE = 1001;//AI写作
        public static final  int TYPE_AI_TRANSLATE  = 1002;//AI翻译
        public static final  int TYPE_PPT = 1003;//PPT
        public static final  int TYPE_AI_PIC = 1004;//AI绘画
        public static final  int TYPE_AI_MEETING = 1054;//AI会议
        public static final  int TYPE_VOICE = 1006;//同声传译

        public static final  int TYPE_TRAVEL = 1007;//出行规划
        public static final  int TYPE_PART = 1008;//同城聚餐
        public static final  int TYPE_THINK = 1009;//深度思考
        public static final  int TYPE_GUI = 1010;//GUI
    }

    public class ThinkState{
        public static final  int START = 1;//初始状态（正在思考）
        public static final  int THINKING = 2;//思考中。。。
        public static final  int END = 3;//思考结束（思考过程 (用时 3 秒)）

    }
    public class ShareReqCardTypeParams{
        public static final  String HOTEL_CARD = "hotelCard";
        public static final  String HOME_CARD = "homeCard";
        public static final  String TRAIN_CARD = "trainCard";
        public static final  String PLANE_CARD = "planeCard";
        public static final  String FOOD_CARD = "foodCard";
        public static final  String ORDER_CARD = "orderCard";
        public static final  String H5_CARD = "h5Card";
        public static final  String IMG_CARD = "imgCard";
        public static final  String THINK_COT = "thinkCot";

    }

    public static String meetingId = "";
    public static String transcription = "";
    public static boolean isLoadMeetingExchange = false;

    public static boolean isUseLingXiTranslation = false;//是否使用灵犀翻译

    public static String lastSuccess = "";

}
