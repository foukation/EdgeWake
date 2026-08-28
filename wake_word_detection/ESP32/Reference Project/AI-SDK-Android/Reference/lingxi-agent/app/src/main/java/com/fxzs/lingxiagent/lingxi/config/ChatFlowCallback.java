package com.fxzs.lingxiagent.lingxi.config;

import com.fxzs.lingxiagent.lingxi.lingxi_conversation.PlanProgressType;
import com.fxzs.lingxiagent.lingxi.service_api.data.FoodList;
import com.fxzs.lingxiagent.lingxi.service_api.data.MusicData;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardHotelModel;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardOrderEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardPlanEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardPlandEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardTrainEntity;
import com.fxzs.lingxiagent.model.deepresearch.dto.DeepResearchBean;
import com.fxzs.lingxiagent.model.honor.dto.HtmlInfo;

import java.util.ArrayList;
import java.util.List;

public interface ChatFlowCallback {
    void receiveChat(String content);
    void receiveCot(String content);
    void receiveH5Card(String templateId,String content);
    void receiveImages(ArrayList<String> imageList);
    void receiveHtmlCard(HtmlInfo cardInfo);
    void receiveFoodCard(FoodList foodList);
    void receiveOrderCard(ChatCardOrderEntity orderEntity);
    void receiveHotelCard(List<ChatCardHotelModel> hotelModel);
    void receivePlaneCard(List<ChatCardPlandEntity> planeEntities);
    void receivePlanCard(PlanProgressType planProgressType, ChatCardPlanEntity.ContentBean planEntities,String reqUrl);
    void receiveTrainCard(List<ChatCardTrainEntity> trainEntities);
    //void receiveAccessCard();
    void receiveGUIPermissionCard();
    void receiveDeepResearchCard(DeepResearchBean deepResearchBean);
    void receiveMusic(MusicData musicData);
    void end();
    void addGuiUserMsg(String content);
    void addGuiAiMsg(String content);
}
