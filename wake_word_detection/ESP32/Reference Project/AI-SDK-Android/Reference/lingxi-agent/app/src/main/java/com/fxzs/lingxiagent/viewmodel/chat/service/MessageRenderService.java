package com.fxzs.lingxiagent.viewmodel.chat.service;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.lingxi.lingxi_conversation.PlanProgressType;
import com.fxzs.lingxiagent.lingxi.service_api.data.MusicData;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardHotelModel;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardOrderEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardPlanEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardPlandEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardTrainEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.model.deepresearch.dto.DeepResearchBean;
import com.fxzs.lingxiagent.model.honor.dto.HtmlInfo;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息/卡片渲染与历史记录追加服务。
 */
public class MessageRenderService {

    public interface ChatListProvider {
        List<ChatMessage> getMessages();

        void postMessages(List<ChatMessage> list);
    }

    private final ChatListProvider provider;

    public MessageRenderService(@NonNull ChatListProvider provider) {
        this.provider = provider;
    }

    public ChatMessage addAIImages(ArrayList<String> imageList) {
        List<ChatMessage> list = provider.getMessages();
        if (list == null) list = new ArrayList<>();
        else if (!list.isEmpty()) list.remove(list.size() - 1); // 移除占位
        ChatMessage aiMsg = new ChatMessage(imageList, ChatAdapter.TYPE_ASSISTANT_IMG);
        list.add(aiMsg);
        provider.postMessages(list);
        return aiMsg;
    }

    public ChatMessage addAICard(HtmlInfo cardInfo) {
        List<ChatMessage> list = provider.getMessages();
        if (list == null) list = new ArrayList<>();
        ChatMessage aiMsg = new ChatMessage(cardInfo, ChatAdapter.TYPE_ASSISTANT_CARD);
        list.add(aiMsg);
        provider.postMessages(list);
        return aiMsg;
    }

    public ChatMessage addAIFoodCard(com.fxzs.lingxiagent.lingxi.service_api.data.FoodList foodList) {
        List<ChatMessage> list = provider.getMessages();
        if (list == null) list = new ArrayList<>();
        ChatMessage aiMsg = new ChatMessage(foodList, ChatAdapter.TYPE_ASSISTANT_FOOD_CARD);
        list.add(aiMsg);
        provider.postMessages(list);
        return aiMsg;
    }

    public ChatMessage addAIHotelCard(List<ChatCardHotelModel> hotelModels) {
        List<ChatMessage> list = provider.getMessages();
        if (list == null) list = new ArrayList<>();
        ChatMessage aiMsg = new ChatMessage(ChatAdapter.TYPE_ASSISTANT_HOTEL_CARD);
        aiMsg.setHotelModels(hotelModels);
        list.add(aiMsg);
        provider.postMessages(list);
        return aiMsg;
    }

    public ChatMessage addAIPlandCard(List<ChatCardPlandEntity> plandEntities) {
        List<ChatMessage> list = provider.getMessages();
        if (list == null) list = new ArrayList<>();
        ChatMessage aiMsg = new ChatMessage(ChatAdapter.TYPE_ASSISTANT_PLANE_CARD);
        aiMsg.setPlandEntities(plandEntities);
        list.add(aiMsg);
        provider.postMessages(list);
        return aiMsg;
    }

    public ChatMessage addAIPlanCard(PlanProgressType planProgressType, ChatCardPlanEntity.ContentBean planEntities, String reqUrl) {
        if (planProgressType == PlanProgressType.Loading) {
            List<ChatMessage> list = provider.getMessages();
            if (list == null) list = new ArrayList<>();
            ChatMessage aiMsg = new ChatMessage(planProgressType.getAlias(), reqUrl, planEntities, ChatAdapter.TYPE_ASSISTANT_PLAN_CARD);
            list.add(aiMsg);
            provider.postMessages(list);
            return aiMsg;
        } else {
            List<ChatMessage> list = provider.getMessages();
            if (list == null || list.isEmpty()) return null;
            ChatMessage aiMsg = list.get(list.size() - 1);
            if (!TextUtils.isEmpty(aiMsg.getPlanProgressType()) &&
                    TextUtils.equals(aiMsg.getPlanProgressType(), PlanProgressType.Loading.getAlias())) {
                aiMsg.setPlanContent(planEntities);
                aiMsg.setPlanReqUrl(reqUrl);
                aiMsg.setPlanProgressType(planProgressType.getAlias());
                provider.postMessages(list);
            }
            return aiMsg;
        }
    }

    public ChatMessage addAITrainCard(List<ChatCardTrainEntity> trainEntities) {
        List<ChatMessage> list = provider.getMessages();
        if (list == null) list = new ArrayList<>();
        ChatMessage aiMsg = new ChatMessage(ChatAdapter.TYPE_ASSISTANT_TRAIN_CARD);
        aiMsg.setTrainEntities(trainEntities);
        list.add(aiMsg);
        provider.postMessages(list);
        return aiMsg;
    }

    public ChatMessage addH5CardMsg(String id, String con) {
        List<ChatMessage> list = provider.getMessages();
        if (list == null) list = new ArrayList<>();
        ChatMessage uaiMsg = new ChatMessage(con, id, ChatAdapter.TYPE_ASSISTANT_H5_CARD);
        list.add(uaiMsg);
        provider.postMessages(list);
        return uaiMsg;
    }

    public ChatMessage addAIOrderCard(ChatCardOrderEntity orderEntities) {
        List<ChatMessage> list = provider.getMessages();
        if (list == null) list = new ArrayList<>();
        ChatMessage aiMsg = new ChatMessage(ChatAdapter.TYPE_ASSISTANT_ORDER_CARD);
        aiMsg.setOrderEntity(orderEntities);
        list.add(aiMsg);
        provider.postMessages(list);
        return aiMsg;
    }

    public void addGuiPermissionCard(int type) {
        List<ChatMessage> list = provider.getMessages();
        if (list == null) list = new ArrayList<>();
        else if (!list.isEmpty()) list.remove(list.size() - 1);
        ChatMessage aiMsg = new ChatMessage(type);
        list.add(aiMsg);
        provider.postMessages(list);
    }

    public ChatMessage addDeepResearchCard(DeepResearchBean deepResearchBean) {
        List<ChatMessage> list = provider.getMessages();
        if (list == null) list = new ArrayList<>();
        ChatMessage deepResearchMsg = new ChatMessage(deepResearchBean, ChatAdapter.TYPE_DEEP_RESEARCH);
        list.add(deepResearchMsg);
        provider.postMessages(list);
        return deepResearchMsg;
    }

    public ChatMessage addDeepResearchCompleteCard(DeepResearchBean deepResearchBean) {
        List<ChatMessage> list = provider.getMessages();
        if (list == null) list = new ArrayList<>();
        ChatMessage deepResearchMsg = new ChatMessage(deepResearchBean, ChatAdapter.TYPE_DEEP_RESEARCH_COMPLETE);
        list.add(deepResearchMsg);
        provider.postMessages(list);
        return deepResearchMsg;
    }

    public ChatMessage addMusicCard(MusicData musicData) {
        List<ChatMessage> list = provider.getMessages();
        if (list == null) list = new ArrayList<>();
        else if (!list.isEmpty()) list.remove(list.size() - 1);
        ChatMessage musicMsg = new ChatMessage(musicData,ChatAdapter.TYPE_MUSIC);
        list.add(musicMsg);
        provider.postMessages(list);
        return musicMsg;
    }

    public ChatMessage addNetworkErrorCard(){
        List<ChatMessage> list = provider.getMessages();
        if (list == null) list = new ArrayList<>();
        ChatMessage deepResearchMsg = new ChatMessage(ChatAdapter.TYPE_NETWORK_ERROR);
        list.add(deepResearchMsg);
        provider.postMessages(list);
        return deepResearchMsg;
    }

}

