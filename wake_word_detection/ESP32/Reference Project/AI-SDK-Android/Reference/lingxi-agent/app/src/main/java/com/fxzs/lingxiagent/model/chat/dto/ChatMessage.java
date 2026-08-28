package com.fxzs.lingxiagent.model.chat.dto;


import com.fxzs.lingxiagent.lingxi.service_api.data.FoodList;
import com.fxzs.lingxiagent.lingxi.service_api.data.MusicData;
import com.fxzs.lingxiagent.model.deepresearch.dto.DeepResearchBean;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.honor.dto.HtmlInfo;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;

import java.util.ArrayList;
import java.util.List;

public class ChatMessage {

    private Long id;
    private String message;
    private String thinkMessage;
    private String thinkMessageTitle;
    private String avatar;
    private int avatarRes;
//    private boolean isUser;
//    private boolean isHeader;
    private boolean isEnd;
    private int status;
    private int thinkingTime; // 思考时间（秒）
    private boolean hideActionRefresh;

    private ArrayList<String> imageList;
    private HtmlInfo cardInfo;
    private FoodList foodList;
    private DeepResearchBean deepResearchBean;
    private List<ChatCardHotelModel> hotelModels;

    private List<ChatCardPlandEntity> plandEntities;

    private List<ChatCardTrainEntity> trainEntities;

    private ChatCardOrderEntity orderEntity;
    private boolean isSelected = false;

    private int progress; // 绘图进度
    private String url; // 绘图URL
    private List<ChatFileBean> chatFileBeanList; // 图片/文件

    private int msgType;//0-用户-普通消息，1-ai-文字消息，2-用户-智能体头部（固定头部），3-ai-绘画消息

    DrawingImageDto drawingImageDto;//绘画消息使用，包含prompt，宽高，url等信息
    private boolean hideThinking;//带思考的模型是否隐藏思考过程

    private String h5CardContent;
    private String h5TemplateId;

    private ChatCardPlanEntity.ContentBean planContent;
    private String planProgressType;
    private String planReqUrl;
    private String fromLang;
    private String toLang;

    private boolean isTranslationMsg = false;

    private boolean isTTSPlaying = false;
    private MusicData musicData;
    private List<String> nexusPilotList;//gui 头部数据

//    public ChatMessage(String message, boolean isHeader, String avatar) {
//        this.message = message;
//        this.isHeader = isHeader;
//        this.avatar = avatar;
//    }
    public ChatMessage(int msgType) {
        this.msgType = msgType;
    }

    public ChatMessage(String message, int msgType, String avatar) {
        this.message = message;
        this.msgType = msgType;
        this.avatar = avatar;
    }

    public ChatMessage(String message, int msgType, int avatarRes) {
        this.message = message;
        this.msgType = msgType;
        this.avatarRes = avatarRes;
    }

    public ChatMessage(String message, int msgType) {
        this.message = message;
        this.msgType = msgType;
    }

    public ChatMessage(String planProgressType, String planReqUrl, ChatCardPlanEntity.ContentBean contentBean, int msgType) {
        this.planProgressType = planProgressType;
        this.planReqUrl = planReqUrl;
        this.planContent = contentBean;
        this.msgType = msgType;
    }

    public ChatMessage(ArrayList<String> imageList, int msgType) {
        this.imageList = imageList;
        this.msgType = msgType;
    }

    public ChatMessage(FoodList foodList, int msgType) {
        this.foodList = foodList;
        this.msgType = msgType;
    }

    public ChatMessage(DeepResearchBean deepResearchBean, int msgType) {
        this.deepResearchBean = deepResearchBean;
        this.msgType = msgType;
    }

    public ChatMessage(HtmlInfo cardInfo, int msgType) {
        this.cardInfo = cardInfo;
        this.msgType = msgType;
    }

    public ChatMessage(List<ChatFileBean> list, int msgType) {
        this.chatFileBeanList = new ArrayList<>();
        this.chatFileBeanList.addAll(list);
        this.msgType = msgType;
    }

    public ChatMessage(String message, boolean isUser) {
        this.message = message;
        this.msgType = isUser ? ChatAdapter.TYPE_USER : ChatAdapter.TYPE_AI;
    }

    public ChatMessage(String message, boolean isUser, int status) {
        this.message = message;
        this.msgType = isUser ? ChatAdapter.TYPE_USER : ChatAdapter.TYPE_AI;
        this.status = status;
    }

    public ChatMessage(String h5CardContent, String h5TemplateId, int msgType) {
        this.h5CardContent = h5CardContent;
        this.h5TemplateId = h5TemplateId;
        this.msgType = msgType;
    }

    //    public ChatMessage(String message, String thinkMessage, boolean isUser) {
//        this.message = message;
//        this.thinkMessage = thinkMessage;
//        this.isUser = isUser;
//    }

    public ChatMessage(MusicData musicData, int msgType) {
        this.musicData = musicData;
        this.msgType = msgType;
    }


    public boolean isTTSPlaying() {
        return isTTSPlaying;
    }

    public void setTTSPlaying(boolean TTSPlaying) {
        isTTSPlaying = TTSPlaying;
    }

    public boolean isTranslationMsg() {
        return isTranslationMsg;
    }

    public String getFromLang() {
        return fromLang;
    }

    public void setFromLang(String fromLang) {
        this.fromLang = fromLang;
    }

    public String getToLang() {
        return toLang;
    }

    public void setToLang(String toLang) {
        this.toLang = toLang;
    }

    public void setTranslationMsg(boolean translationMsg) {
        isTranslationMsg = translationMsg;
    }

    public String getPlanProgressType() {
        return planProgressType;
    }

    public void setPlanProgressType(String planProgressType) {
        this.planProgressType = planProgressType;
    }

    public String getPlanReqUrl() {
        return planReqUrl;
    }

    public void setPlanReqUrl(String planReqUrl) {
        this.planReqUrl = planReqUrl;
    }

    public ChatCardPlanEntity.ContentBean getPlanContent() {
        return planContent;
    }

    public void setPlanContent(ChatCardPlanEntity.ContentBean planContent) {
        this.planContent = planContent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getH5CardContent() {
        return h5CardContent;
    }

    public void setH5CardContent(String h5CardContent) {
        this.h5CardContent = h5CardContent;
    }

    public String getH5TemplateId() {
        return h5TemplateId;
    }

    public void setH5TemplateId(String h5TemplateId) {
        this.h5TemplateId = h5TemplateId;
    }

    public ChatCardOrderEntity getOrderEntity() {
        return orderEntity;
    }

    public void setOrderEntity(ChatCardOrderEntity orderEntity) {
        this.orderEntity = orderEntity;
    }

    public List<ChatCardTrainEntity> getTrainEntities() {
        return trainEntities;
    }

    public void setTrainEntities(List<ChatCardTrainEntity> trainEntities) {
        this.trainEntities = trainEntities;
    }

    public List<ChatCardPlandEntity> getPlandEntities() {
        return plandEntities;
    }

    public void setPlandEntities(List<ChatCardPlandEntity> plandEntities) {
        this.plandEntities = plandEntities;
    }

    public List<ChatCardHotelModel> getHotelModels() {
        return hotelModels;
    }

    public void setHotelModels(List<ChatCardHotelModel> hotelModels) {
        this.hotelModels = hotelModels;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

//    public boolean isUser() {
//        return isUser;
//    }

    public String getThinkMessage() {
        return thinkMessage;
    }

    public void setThinkMessage(String thinkMessage) {
        this.thinkMessage = thinkMessage;
    }

    public String getThinkMessageTitle() {
        return thinkMessageTitle;
    }

    public void setThinkMessageTitle(String thinkMessageTitle) {
        this.thinkMessageTitle = thinkMessageTitle;
    }

    public boolean isEnd() {
        return isEnd;
    }

    public void setEnd(boolean end) {
        isEnd = end;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

//    public boolean isHeader() {
//        return isHeader;
//    }

//    public void setHeader(boolean header) {
//        isHeader = header;
//    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getThinkingTime() {
        return thinkingTime;
    }

    public void setThinkingTime(int thinkingTime) {
        this.thinkingTime = thinkingTime;
    }


    public boolean isHideActionRefresh() {
        return hideActionRefresh;
    }

    public void setHideActionRefresh(boolean hideActionRefresh) {
        this.hideActionRefresh = hideActionRefresh;
    }

    public int getMsgType() {
        return msgType;
    }

    public ArrayList<String> getImageList() {
        return imageList;
    }

    public HtmlInfo getCardInfo() {
        return cardInfo;
    }

    public FoodList getFoodList() {
        return foodList;
    }

    public void setDeepResearch(DeepResearchBean deepResearchBean) {
        this.deepResearchBean = deepResearchBean;
    }

    public DeepResearchBean getDeepResearch() {
        return deepResearchBean;
    }

    public boolean getIsSelected() {
        return isSelected;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public List<ChatFileBean> getChatFileBeanList() {
        return chatFileBeanList;
    }

    public void setChatFileBeanList(List<ChatFileBean> chatFileBeanList) {
        this.chatFileBeanList = chatFileBeanList;
    }

    public int getAvatarRes() {
        return avatarRes;
    }

    public void setAvatarRes(int avatarRes) {
        this.avatarRes = avatarRes;
    }

    public DrawingImageDto getDrawingImageDto() {
        return drawingImageDto;
    }

    public void setDrawingImageDto(DrawingImageDto drawingImageDto) {
        this.drawingImageDto = drawingImageDto;
    }

    public boolean isHideThinking() {
        return hideThinking;
    }

    public void setHideThinking(boolean hideThinking) {
        this.hideThinking = hideThinking;
    }

    // 便捷构造方法 - 带思维链和思考时间
//    public ChatMessage(String message, String thinkMessage, int thinkingTime, boolean isUser) {
//        this.message = message;
//        this.thinkMessage = thinkMessage;
//        this.thinkingTime = thinkingTime;
//        this.isUser = isUser;
//        this.thinkMessageTitle = "思考过程 (用时 " + thinkingTime + " 秒)";
//    }


    public MusicData getMusicData() {
        return musicData;
    }

    public void setMusicData(MusicData musicData) {
        this.musicData = musicData;
    }

    public List<String> getNexusPilotList() {
        return nexusPilotList;
    }

    public void setNexusPilotList(List<String> nexusPilotList) {
        this.nexusPilotList = nexusPilotList;
    }
}