package com.fxzs.lingxiagent.viewmodel.history;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.chat.dto.ConversationHistoryDto;
import com.fxzs.lingxiagent.model.chat.dto.ConversationHistoryListDto;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepository;
import com.fxzs.lingxiagent.model.chat.repository.ChatRepositoryImpl;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingImageDto;
import com.fxzs.lingxiagent.model.drawing.dto.DrawingSessionDto;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepository;
import com.fxzs.lingxiagent.model.drawing.repository.DrawingRepositoryImpl;
import com.fxzs.lingxiagent.model.history.AllHistoryDto;
import com.fxzs.lingxiagent.model.history.AllRecordImage;
import com.fxzs.lingxiagent.model.history.LatestImage;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingHistoryDto;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepository;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepositoryImpl;
import com.fxzs.lingxiagent.model.ppt.dto.PptSessionDto;
import com.fxzs.lingxiagent.model.ppt.repository.PptRepository;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.network.ZNet.bean.TranslationRecord;
import com.fxzs.lingxiagent.network.ZNet.bean.TranslationRecordListBean;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.aiwork.AiWorkAdapter;
import com.fxzs.lingxiagent.view.common.BaseViewModel;
import com.fxzs.lingxiagent.view.user.HistoryItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

/**
 * 历史记录ViewModel
 * 管理所有类型的历史记录数据
 */
public class VMHistory extends BaseViewModel {
    
    // 历史记录类型常量
    public static final int TAB_CHAT = 0;
    public static final int TAB_AGENT = 1;
    public static final int TAB_DRAWING = 2;
    public static final int TAB_MEETING = 3;
    public static final int TAB_PPT = 4;
    public static final int TAB_TRANSLATE = 5;
    public static final int TAB_EXCEL = 6;

    // Repository
    private final ChatRepository chatRepository;
    private final DrawingRepository drawingRepository;
    private final MeetingRepository meetingRepository;
    private final PptRepository pptRepository;
    
    // LiveData
    private final MutableLiveData<Integer> currentTabIndex = new MutableLiveData<>(TAB_CHAT);
    private final MutableLiveData<List<HistoryItem>> historyItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);

    // 分页参数
    public static final int PAGE_SIZE = 15;
    private int currentPage = 1;
    private boolean hasMoreData = true;
    private boolean isAll = false;//AI办公 有全部
    String currentTranslateType = "1";//现在选择的同传类型1-聆听，2-对话
    private int drawingType = 0; // 0-文生图, 1-图生图
    public boolean isInit = true;//初始化不显示空图
    // 是否已用缓存预填，控制刷新时是否清空列表
    private boolean hasPrefilledFromCache = false;

    public VMHistory() {
        chatRepository = new ChatRepositoryImpl();
        drawingRepository = DrawingRepositoryImpl.getInstance();
        meetingRepository = new MeetingRepositoryImpl();
        pptRepository = PptRepository.getInstance();
        // 不在构造函数中自动加载数据，等待Fragment调用selectTab时再加载
    }
    
    // Getters
    public MutableLiveData<Integer> getCurrentTabIndex() {
        return currentTabIndex;
    }
    
    public LiveData<List<HistoryItem>> getHistoryItems() {
        return historyItems;
    }
    
    public LiveData<Boolean> getIsRefreshing() {
        return isRefreshing;
    }
    
    public boolean hasMoreData() {
        return hasMoreData;
    }

    public boolean isAll() {
        return isAll;
    }

    public void setAll(boolean all) {
        isAll = all;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * 切换Tab
     */
    public void selectTab(int tabIndex) {
        Timber.tag("VMHistory").d( "selectTab: " + tabIndex);

        // 检查是否是相同Tab且已经有数据
        if (currentTabIndex.getValue() != null && currentTabIndex.getValue() == tabIndex) {
            List<HistoryItem> currentItems = historyItems.getValue();
            if (currentItems != null && !currentItems.isEmpty()) {
                Timber.tag("VMHistory").d( "相同Tab且已有数据，不重复加载");
                return; // 相同Tab且已有数据不重复加载
            }
        }

        currentTabIndex.setValue(tabIndex);
        // 先尝试读取缓存并展示
        List<HistoryItem> cached = getCachedHistoryForTab(tabIndex);
        if (cached != null && !cached.isEmpty()) {
            historyItems.setValue(cached);
            hasPrefilledFromCache = true;
        }
        // 再发起网络刷新，保证数据最新
        refreshHistory();
    }
    
    /**
     * 刷新历史记录
     */
    public void refreshHistory() {
        currentPage = 1;
        hasMoreData = true;
        if(isAll){
            loadAllHistory(true);
        }else {
            loadHistory(true);
        }
    }
    
    /**
     * 静默刷新历史记录（不显示下拉刷新动画）
     */
    public void silentRefreshHistory() {
        Timber.tag("VMHistory").d( "静默刷新历史记录");
        currentPage = 1;
        hasMoreData = true;
        if(isAll){
            loadAllHistory(false); // 传入false，不触发刷新动画
        }else {
            loadHistoryWithoutAnimation(true); // 使用新的方法，不显示动画
        }
    }
    
    /**
     * 加载更多历史记录
     */
    public void loadMoreHistory() {
        if (!hasMoreData || Boolean.TRUE.equals(isLoading.getValue())) {
            Timber.tag("VMHistory").d( "无法加载更多：hasMoreData=" + hasMoreData + ", isLoading=" + isLoading.getValue());
            return;
        }
        
        Timber.tag("VMHistory").d( "开始加载更多历史记录，isAll=" + isAll + ", currentPage=" + currentPage);
        
        // 显示加载指示器
        showLoadingIndicator(); 
        if(isAll){
            // 全部历史模式：加载下一页
            loadAllHistory(false);
        }else {
            // 单一类型模式：加载下一页
            loadHistory(false);
        }
    }
    
    /**
     * 加载历史记录
     */
    private void loadHistory(boolean isRefresh) {
        loadHistoryInternal(isRefresh, true);
    }
    
    /**
     * 加载历史记录（不显示刷新动画）
     */
    private void loadHistoryWithoutAnimation(boolean isRefresh) {
        loadHistoryInternal(isRefresh, false);
    }
    
    /**
     * 内部加载历史记录方法
     */
    private void loadHistoryInternal(boolean isRefresh, boolean showAnimation) {
        Integer tabIndex = currentTabIndex.getValue();
        if (tabIndex == null) return;
        
        if (isRefresh) {
            // 若已用缓存预填，则不立刻清空，避免UI闪烁；等待新数据覆盖
            if (!hasPrefilledFromCache) {
                historyItems.getValue().clear();
            }
            if (showAnimation) {
                isRefreshing.setValue(true);
            }
        } else {
            currentPage++;
            setLoading(true);
        }
        
        Timber.tag("VMHistory").d( "loadHistory for tab: " + tabIndex + ", isRefresh: " + isRefresh + ", showAnimation: " + showAnimation);

        switch (tabIndex) {
            case TAB_CHAT:
                Timber.tag("VMHistory").d( "加载对话历史");
                loadChatHistory(8, isRefresh); // modelType = 8 表示对话模型
                break;
            case TAB_AGENT:
                Timber.tag("VMHistory").d( "加载智能体历史");
                loadChatHistory(1, isRefresh); // modelType = 1 表示智能体模型
                break;
            case TAB_DRAWING:
                Timber.tag("VMHistory").d( "加载绘画历史");
                loadDrawingHistory(isRefresh);
                break;
            case TAB_MEETING:
                Timber.tag("VMHistory").d( "加载会议历史");
                loadMeetingHistory(isRefresh);
                break;
            case TAB_PPT:
                Timber.tag("VMHistory").d( "加载PPT历史");
                loadPptHistory(isRefresh);
                break;
            case TAB_TRANSLATE:
                Timber.tag("VMHistory").d( "加载翻译历史（暂未实现）");
                // TODO: 实现翻译历史记录
//                handleEmptyResult(isRefresh);
                loadTranslateHistory(isRefresh,currentTranslateType);
                break;

            case TAB_EXCEL:
                Timber.tag("VMHistory").d( "加载AI表格历史");
                loadChatHistory(9, isRefresh); // modelType = 9 表示AI表格
                break;
            default:
                Timber.tag("VMHistory").w( "未知的Tab类型: " + tabIndex);
                handleEmptyResult(isRefresh);
                break;
        }
    }

    public void loadTranslateHistory(boolean isRefresh,String type) {
        currentTranslateType = type;
        if (isRefresh) {
            isRefreshing.setValue(true);
            currentPage = 1;
            hasMoreData = true;
        } else {
            currentPage++;
            setLoading(true);
        }
        
        Timber.tag("VMHistory").d( "加载翻译历史 - 类型: " + type + ", 页码: " + currentPage + ", 是否刷新: " + isRefresh);
        
        HttpRequest request = new HttpRequest();
        // 添加分页参数
        request.getTranslationRecordList(type, currentPage, PAGE_SIZE, new Observer<ApiResponse<TranslationRecord>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<TranslationRecord> res) {
                if(currentTabIndex.getValue() != VMHistory.TAB_TRANSLATE){
                    return;
                }
                List<TranslationRecordListBean> translationRecordListBeans = res.getData().getList();
                List<HistoryItem> items = new ArrayList<>();
                if(translationRecordListBeans != null && translationRecordListBeans.size() > 0){
                    for (int i = 0; i < translationRecordListBeans.size(); i++) {
                        TranslationRecordListBean bean = translationRecordListBeans.get(i);

                        HistoryItem historyItem = new HistoryItem(HistoryItem.TYPE_ITEM, bean.getName(), "暂无内容", "");
                        if(bean.getMessageList() != null && bean.getMessageList().size() > 0){
                            TranslationRecordListBean message = bean.getMessageList().get(0);
                            historyItem.setSubtitle(message.getSourceText()+"\n"+message.getTargetText());
                        }
                        historyItem.setConversationId(Long.parseLong(bean.getId()+""));
                        historyItem.setCreateTime(bean.getCreateTime());
                        items.add(historyItem);
                    }
                }
                handleSuccessResult(items, isRefresh);
            }

            @Override
            public void onError(Throwable e) {
                handleErrorResult(e.getMessage(), isRefresh);
            }

            @Override
            public void onComplete() {

            }
        });
    }

    /**
     * 加载对话历史记录
     */
    private void loadChatHistory(int modelType, boolean isRefresh) {
        Map<String, Object> params = createBaseParams();
        
        chatRepository.getConversationHistoryList(modelType, params, new ChatRepository.Callback<ConversationHistoryListDto>() {
            @Override
            public void onSuccess(ConversationHistoryListDto data) {
                LinkedList<HistoryItem> items = convertConversationToHistoryItems(data.getList());
                items.sort((historyItem, t1) -> Long.compare(t1.getCreateTime(), historyItem.getCreateTime()));
                if(modelType == 8 && currentTabIndex.getValue() == VMHistory.TAB_CHAT){
                    handleSuccessResult(items, isRefresh);
                }else if(modelType == 1 && currentTabIndex.getValue() == VMHistory.TAB_AGENT){
                    handleSuccessResult(items, isRefresh);
                }else if(modelType == 9 && currentTabIndex.getValue() == VMHistory.TAB_EXCEL){
                    handleSuccessResult(items, isRefresh);
                }
            }
            
            @Override
            public void onError(String error) {
                handleErrorResult(error, isRefresh);
            }
        });
    }
    
    /**
     * 加载绘画历史记录
     */
    public void setDrawingType(int type) {
        if (type != 0 && type != 1) {
            type = 0;
        }
        if (this.drawingType == type) {
            return;
        }
        this.drawingType = type;
        refreshHistory();
    }

    public int getDrawingType() {
        return drawingType;
    }

    public void loadDrawingHistory(boolean isRefresh) {
        try {
            Timber.tag("VMHistory").d("开始加载绘画历史记录 - isRefresh: " + isRefresh + ", currentPage: " + currentPage + ", drawingType: " + drawingType);

            if (isRefresh) {
                isRefreshing.setValue(true);
                currentPage = 1;
                hasMoreData = true;
                // 若已用缓存预填，则不立刻清空，避免闪烁
                if (!hasPrefilledFromCache) {
                    historyItems.getValue().clear();
                }
            } else {
                setLoading(true);
            }

            Map<String, Object> params = createBaseParams();
            if (drawingType == 1) {
                params.put("type", 1);
            }
            Timber.tag("VMHistory").d("绘画历史分页参数: " + params);

            drawingRepository.getSessionList(params).observeForever(result -> {
                if (currentTabIndex.getValue() != VMHistory.TAB_DRAWING) {
                    return;
                }
                try {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        List<DrawingImageDto> drawingImages = result.getData().getRecords();
                        if (drawingImages != null) {
                            Timber.tag("VMHistory").d("绘画历史加载成功，数据数量: " + drawingImages.size());
                            List<HistoryItem> items = convertDrawingToHistoryItems(drawingImages);
                            handleSuccessResult(items, isRefresh);
                        } else {
                            handleSuccessResult(new ArrayList<>(), isRefresh);
                        }
                    } else {
                        String errorMsg = result != null ? result.getError() : "加载绘画历史失败";
                        handleErrorResult(errorMsg, isRefresh);
                    }
                } catch (Exception e) {
                    handleErrorResult("处理绘画历史数据时出错: " + e.getMessage(), isRefresh);
                }
            });
        } catch (Exception e) {
            handleErrorResult("加载绘画历史时出错: " + e.getMessage(), isRefresh);
        }
    }
    
    /**
     * 加载PPT历史记录
     */
    public void loadPptHistory(boolean isRefresh) {
        Timber.tag("VMHistory").d( "开始加载PPT历史记录");

        if (isRefresh) {
            currentPage = 1;
            hasMoreData = true;
            isRefreshing.setValue(true);
        }

        pptRepository.getPptSessionList(new PptRepository.PptCallback<List<PptSessionDto>>() {
            @Override
            public void onSuccess(List<PptSessionDto> sessions) {
                if(currentTabIndex.getValue() != VMHistory.TAB_PPT){
                    return;
                }
                Timber.tag("VMHistory").d( "PPT历史记录加载成功，数量: " + (sessions != null ? sessions.size() : 0));

                List<HistoryItem> items = new ArrayList<>();
                if (sessions != null) {
                    for (PptSessionDto session : sessions) {
                        items.add(convertPptSessionToHistoryItem(session));
                    }
                }

                if (isRefresh) {
                    historyItems.postValue(items);
                } else {
                    // 追加数据
                    List<HistoryItem> currentItems = historyItems.getValue();
                    if (currentItems != null) {
                        currentItems.addAll(items);
                        historyItems.postValue(currentItems);
                    } else {
                        historyItems.postValue(items);
                    }
                }

                // 写入缓存并重置预填标志
                Integer tabIndex = currentTabIndex.getValue();
                if (tabIndex != null) {
                    // 使用最新的列表写缓存
                    List<HistoryItem> toCache = isRefresh ? items : historyItems.getValue();
                    if (toCache != null) {
                        cacheHistoryForTab(tabIndex, toCache);
                    }
                }
                hasPrefilledFromCache = false;

                isRefreshing.postValue(false);
                hasMoreData = items.size() >= PAGE_SIZE;
            }

            @Override
            public void onError(String error) {
                Timber.tag("VMHistory").e( "PPT历史记录加载失败: " + error);
                isRefreshing.postValue(false);
//
//                if (isRefresh) {
//                    historyItems.postValue(new ArrayList<>());
//                }
            }
        });
    }

    /**
     * 将PPT会话转换为历史记录项
     */
    private HistoryItem convertPptSessionToHistoryItem(PptSessionDto session) {
        // 设置描述信息
        StringBuilder description = new StringBuilder();
        String pptUrl = null;
        String coversUrl = null;

        if (session.getPptTasks() != null && !session.getPptTasks().isEmpty()) {
            PptSessionDto.PptTask latestTask = session.getPptTasks().get(0);
            if (latestTask.getPptTitle() != null) {
                description.append("PPT: ").append(latestTask.getPptTitle());
            }
            if (latestTask.getPageCount() > 0) {
                description.append(" (").append(latestTask.getPageCount()).append("页)");
            }
            pptUrl = latestTask.getPptUrl();
        } else if (session.getPptCatalogs() != null && !session.getPptCatalogs().isEmpty()) {
            description.append("已生成大纲");
        } else {
            description.append("PPT项目");
        }
        if(session.getPptCovers() != null&& session.getPptCovers().size() > 0 ){
            coversUrl = session.getPptCovers().get(0).getCoverImage();
        }

        // 创建HistoryItem，使用PPT图标
        HistoryItem item = new HistoryItem(HistoryItem.TYPE_ITEM, session.getTitle(), description.toString(),
                                         com.fxzs.lingxiagent.R.drawable.ic_ppt);

        // 设置PPT相关数据
        item.setPptSessionId(session.getId());
        item.setPptUrl(pptUrl);
        item.setImageUrl(coversUrl);
        item.setCreateTime(session.getCreateTime());

        // 设置额外数据
        item.setExtraData("session", session);
        // 标记来源类型，便于 Adapter 选择布局
        item.setExtraData("sourceType", com.fxzs.lingxiagent.view.aiwork.AiWorkAdapter.TYPE_PPT);

        Timber.tag("VMHistory").d( "创建PPT历史记录项 - ID: " + session.getId() + ", Title: " + session.getTitle() +
                          ", PPT URL: " + pptUrl + ", SessionId: " + item.getPptSessionId());

        return item;
    }

    /**
     * 加载会议历史记录
     */
    public void loadMeetingHistory(boolean isRefresh) {
        Map<String, Object> params = createBaseParams();
        params.put("sort", 2);
        params.put("keyword", "");

        meetingRepository.getMeetingHistoryList(params).observeForever(result -> {
            if(currentTabIndex.getValue() != VMHistory.TAB_MEETING){
                return;
            }
            if (result != null && result.isSuccess() && result.getData() != null) {
                List<HistoryItem> items = convertMeetingToHistoryItems(result.getData().getList());
                handleSuccessResult(items, isRefresh);
            } else {
                handleErrorResult(result != null ? result.getError() : "加载会议历史失败", isRefresh);
            }
        });
    }

        /** 加载全部历史（混合列表）
            */
    public void loadAllHistory(boolean isRefresh){
        if (isRefresh) {
            isRefreshing.setValue(true);
            currentPage = 1;
        } else {
            currentPage++;
            setLoading(true);
        }
        HttpRequest request = new HttpRequest();
        // 与示例一致，第一页3条可改为 PAGE_SIZE
        request.getAllHistory(currentPage, PAGE_SIZE, new io.reactivex.Observer<ApiResponse<AllHistoryDto>>() {
            @Override
            public void onSubscribe(io.reactivex.disposables.Disposable d) { }

            @Override
            public void onNext(ApiResponse<AllHistoryDto> res) {
                List<HistoryItem> items = new ArrayList<>();
                try{
                    if(res != null && res.getCode() == 0 && res.getData() != null){
                        List<AllHistoryDto.Record> list = res.getData().getList();
                        if(list != null){
                            for(AllHistoryDto.Record r : list){
                                String t = r.getType();
                                long create = r.getCreateTimeMs();
                                if("ppt".equals(t) && r.getPpt() != null){
                                    HistoryItem item = convertPptSessionToHistoryItem(r.getPpt());
                                    item.setExtraData("sourceType", AiWorkAdapter.TYPE_PPT);
                                    if(create>0) item.setCreateTime(create);
                                    items.add(item);
                                }else if("meeting".equals(t) && r.getMeeting() != null){
                                    String title = r.getMeeting().getName();
                                    if(title == null || title.trim().isEmpty()) title = "会议记录";
                                    String subTitle = extractMeetingSubtitle(r.getMeeting().getContent());
                                    HistoryItem item = new HistoryItem(HistoryItem.TYPE_ITEM, title, subTitle, com.fxzs.lingxiagent.R.drawable.ic_nav_meeting);
                                    try{ item.setMeetingId(Long.parseLong(r.getMeeting().getId())); }catch(Exception ignore){}
                                    item.setMeetingType(r.getMeeting().getType());
                                    if(create>0) item.setCreateTime(create);
                                    item.setExtraData("sourceType", AiWorkAdapter.TYPE_MEETING);
                                    items.add(item);
                                }else if("image".equals(t) && r.getImage()!=null){
                                    // 映射为绘画卡片
                                    AllRecordImage img = r.getImage();
                                    LatestImage latestImage = img.getLatestImage();
                                    String imageUrl = latestImage.getPicUrl();
                                    HistoryItem item = new HistoryItem(HistoryItem.TYPE_ITEM, img.getName(), "", imageUrl);
                                     item.setSessionId(Long.valueOf(img.getId()));
                                    if(create>0) item.setCreateTime(create);
                                     item.setExtraData("width", latestImage.getWidth());
                                     item.setExtraData("height", latestImage.getHeight());
                                    item.setExtraData("sourceType", AiWorkAdapter.TYPE_DRAWING);
                                    items.add(item);
                                }else if("translation".equals(t) && r.getTranslation()!=null){
                                    // 映射为同传卡片
                                    com.fxzs.lingxiagent.network.ZNet.bean.TranslationRecordListBean tr = r.getTranslation();
                                    String title = tr.getName(); if(title==null||title.trim().isEmpty()) title = "同声传译";
                                    HistoryItem item = new HistoryItem(HistoryItem.TYPE_ITEM, title, "", com.fxzs.lingxiagent.R.drawable.ic_avatar_ai_writer);
                                    // 摘要预览：取首条消息的原文+译文
                                    if(tr.getMessageList()!=null && !tr.getMessageList().isEmpty()){
                                        com.fxzs.lingxiagent.network.ZNet.bean.TranslationRecordListBean msg = tr.getMessageList().get(0);
                                        String preview = (msg.getSourceText()!=null?msg.getSourceText():"") + "\n" + (msg.getTargetText()!=null?msg.getTargetText():"");
                                        item.setSubtitle(preview);
                                    }
                                    item.setConversationId((long) tr.getId());
                                    if(create>0) item.setCreateTime(create);
                                    item.setExtraData("sourceType", AiWorkAdapter.TYPE_TRANSLATE);
                                    items.add(item);
                                }
                                // TODO: 如需支持 image/translation，可在此补充
                            }
                        }
                    }
                }catch (Exception e){
                    Timber.tag("VMHistory").e("loadAllHistory parse error: "+e.getMessage());
                }
                handleSuccessResult(items, isRefresh);
                isRefreshing.postValue(false);
            }

            @Override
            public void onError(Throwable e) {
                handleErrorResult(e.getMessage(), isRefresh);
            }

            @Override
            public void onComplete() { }
        });
    }


    private String extractMeetingSubtitle(String content){
        if(content == null) return "";
        try{
            String[] strArr = content.split("]");
            if(strArr.length>1){
                String subTitle = strArr[1].trim();
                String[] strArr2 = subTitle.split("\\[");
                if(strArr2.length>1){
                    subTitle = strArr2[0].trim();
                }
                return subTitle;
            }
        }catch(Exception ignore){}
        return "";
    }


    /**
     * 创建基础请求参数
     */
    private Map<String, Object> createBaseParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("pageNo", currentPage);
        params.put("pageSize", PAGE_SIZE);
        
        long userId = SharedPreferencesUtil.getUserId();
        Integer tabIndex = currentTabIndex.getValue();
        if (userId > 0 && tabIndex != TAB_EXCEL) {
            params.put("userId", userId);
        }
        
        return params;
    }
    
    /**
     * 处理成功结果
     */
    private void handleSuccessResult(List<HistoryItem> newItems, boolean isRefresh) {
        List<HistoryItem> currentItems = historyItems.getValue();
        if (currentItems == null) {
            currentItems = new ArrayList<>();
        }
        
        // 移除加载指示器（如果存在）
        removeLoadingIndicator(currentItems);
        
        if (isRefresh) {
            currentItems.clear();
            // 刷新时重置分页状态
            currentPage = 1;
            hasMoreData = true;
        }
        
        // 合并新数据，避免重复的日期头
        if (!isRefresh && !currentItems.isEmpty() && !newItems.isEmpty()) {
            // 如果当前列表最后一项是日期头，且新数据第一项也是相同的日期头，则移除新数据的日期头
            HistoryItem lastCurrentItem = currentItems.get(currentItems.size() - 1);
            HistoryItem firstNewItem = newItems.get(0);
            
            if (lastCurrentItem.getType() == HistoryItem.TYPE_DATE_HEADER && 
                firstNewItem.getType() == HistoryItem.TYPE_DATE_HEADER &&
                lastCurrentItem.getTitle().equals(firstNewItem.getTitle())) {
                newItems.remove(0);
            }
        }
        
        currentItems.addAll(newItems);
        
        // 更新分页状态 - 根据接口返回的列表数据是否为空判断是否还有更多数据
        int actualDataCount = 0;
        for (HistoryItem item : newItems) {
            if (item.getType() == HistoryItem.TYPE_ITEM) {
                actualDataCount++;
            }
        }
        
        // 如果接口返回的数据列表为空（actualDataCount为0），说明没有更多数据了
        // 这比简单的数量比较更准确，因为API可能返回少于PAGE_SIZE的数据但仍有下一页
        if (!isRefresh) {
            hasMoreData = actualDataCount > 0;
        }
        // 刷新时保持hasMoreData为true，让后续请求来判断
        
        // 页码在loadMoreHistory方法中已经递增，这里不需要再次递增
        
        Timber.tag("VMHistory").d( "处理成功结果 - 新增数据项: " + actualDataCount + 
                          ", 是否还有更多: " + hasMoreData + ", 当前页: " + currentPage + 
                          ", isRefresh: " + isRefresh);
        
        historyItems.setValue(currentItems);
        // 成功后将当前tab的列表写入缓存
        Integer tabIndex = currentTabIndex.getValue();
        if (tabIndex != null) {
            cacheHistoryForTab(tabIndex, currentItems);
        }
        // 新数据已到，重置预填标志
        hasPrefilledFromCache = false;
        setLoading(false);
        isRefreshing.setValue(false);
    }

    /**
     * 根据tab读取缓存的历史列表
     */
    private List<HistoryItem> getCachedHistoryForTab(int tabIndex) {
        try {
            String key;
            if (tabIndex == TAB_CHAT) {
                key = Constants.PREF_HISTORY_CACHE_CHAT;
            } else if (tabIndex == TAB_AGENT) {
                key = Constants.PREF_HISTORY_CACHE_AGENT;
            } else if (tabIndex == TAB_DRAWING) {
                key = Constants.PREF_HISTORY_CACHE_DRAWING;
            } else if (tabIndex == TAB_MEETING) {
                key = Constants.PREF_HISTORY_CACHE_MEETING;
            } else if (tabIndex == TAB_PPT) {
                key = Constants.PREF_HISTORY_CACHE_PPT;
            }  else if (tabIndex == TAB_EXCEL) {
                key = Constants.PREF_HISTORY_CACHE_EXCEL;
            } else if (tabIndex == TAB_TRANSLATE) {
                // 翻译根据当前类型(1:聆听,2:对话)区分
                key = Constants.PREF_HISTORY_CACHE_TRANSLATE_PREFIX + (TextUtils.isEmpty(currentTranslateType) ? "1" : currentTranslateType);
            } else {
                return null;
            }
            String json = SharedPreferencesUtil.getString(key, "");
            if (TextUtils.isEmpty(json)) return null;
            java.lang.reflect.Type listType = new TypeToken<List<HistoryItem>>(){}.getType();
            List<HistoryItem> list = new Gson().fromJson(json, listType);
            normalizeHistoryItems(list);
            return list;
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 按tab缓存历史列表
     */
    private void cacheHistoryForTab(int tabIndex, List<HistoryItem> items) {
        try {
            String key;
            if (tabIndex == TAB_CHAT) {
                key = Constants.PREF_HISTORY_CACHE_CHAT;
            } else if (tabIndex == TAB_AGENT) {
                key = Constants.PREF_HISTORY_CACHE_AGENT;
            } else if (tabIndex == TAB_DRAWING) {
                key = Constants.PREF_HISTORY_CACHE_DRAWING;
            } else if (tabIndex == TAB_MEETING) {
                key = Constants.PREF_HISTORY_CACHE_MEETING;
            } else if (tabIndex == TAB_PPT) {
                key = Constants.PREF_HISTORY_CACHE_PPT;
            } else if (tabIndex == TAB_TRANSLATE) {
                key = Constants.PREF_HISTORY_CACHE_TRANSLATE_PREFIX + (TextUtils.isEmpty(currentTranslateType) ? "1" : currentTranslateType);
            } else {
                return;
            }
            String json = new Gson().toJson(items);
            SharedPreferencesUtil.saveString(key, json);
        } catch (Exception ignore) {
        }
    }

    /**
     * 规整从缓存读出的列表，修正 extraData 中数字类型（避免 Double -> Integer 的强转异常）
     */
    private void normalizeHistoryItems(List<HistoryItem> items) {
        if (items == null) return;
        for (HistoryItem it : items) {
            java.util.Map<String, Object> map = it.getExtraData();
            if (map == null) continue;
            Object w = map.get("width");
            if (w instanceof Number && !(w instanceof Integer)) {
                map.put("width", ((Number) w).intValue());
            }
            Object h = map.get("height");
            if (h instanceof Number && !(h instanceof Integer)) {
                map.put("height", ((Number) h).intValue());
            }
            Object st = map.get("sourceType");
            if (st instanceof Number && !(st instanceof Integer)) {
                map.put("sourceType", ((Number) st).intValue());
            }
        }
    }
    
    /**
     * 处理错误结果
     */
    private void handleErrorResult(String error, boolean isRefresh) {
        // 移除加载指示器（如果存在）
        if (!isRefresh) {
            List<HistoryItem> currentItems = historyItems.getValue();
            if (currentItems != null) {
                removeLoadingIndicator(currentItems);
                historyItems.setValue(currentItems);
            }
        }
        
        setError(error);
        setLoading(false);
        isRefreshing.setValue(false);
    }
    
    /**
     * 处理空结果
     */
    private void handleEmptyResult(boolean isRefresh) {
        if (isRefresh) {
            historyItems.setValue(new ArrayList<>());
        }
        setLoading(false);
        isRefreshing.setValue(false);
    }
    
    // 数据转换方法（从原Fragment中迁移）
    private LinkedList<HistoryItem> convertConversationToHistoryItems(List<ConversationHistoryDto> conversations) {
        LinkedList<HistoryItem> items = new LinkedList<>();
        if (conversations == null || conversations.isEmpty()) {
            return items;
        }

        String lastDate = "";
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM月dd日", Locale.getDefault());

        for (ConversationHistoryDto conversation : conversations) {
            // 添加日期分组头
            String date = dateFormat.format(new Date(conversation.getCreateTime()));
//            if (!date.equals(lastDate)) {
//                items.add(new HistoryItem(HistoryItem.TYPE_DATE_HEADER, date, null, 0));
//                lastDate = date;
//            }

            // 构建显示标题
            String title = conversation.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = "对话记录";
            }

            // 获取头像信息
            String avatarUrl = conversation.getIconUrl();
            HistoryItem historyItem;

            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                // 使用网络头像URL
                historyItem = new HistoryItem(HistoryItem.TYPE_ITEM, title, "暂无内容", avatarUrl);
            } else {
                // 使用默认头像
                historyItem = new HistoryItem(HistoryItem.TYPE_ITEM, title, "暂无内容", com.fxzs.lingxiagent.R.drawable.ic_app_logo);
            }
            if(conversation.getLastMessage() != null){
                ConversationHistoryDto.LastMessageDto lastMessageDto = conversation.getLastMessage();
                try {
                    String content = lastMessageDto.getContent();
                    if (content != null && !content.trim().isEmpty()) {
                        // 安全地处理 Markdown 标题，避免过度移除
                        String plainText = content.replaceAll("^#+\\s*", "").trim();
                        
                        // 如果处理后为空，使用原始内容的前50个字符
                        if (plainText.isEmpty()) {
                            plainText = content.length() > 50 ? content.substring(0, 50) + "..." : content;
                        } else if (plainText.length() > 50) {
                            plainText = plainText.substring(0, 50) + "...";
                        }
                        
                        historyItem.setSubtitle(plainText);
                    } else {
                        // content 为空时显示默认文本
                        historyItem.setSubtitle(null);
                    }
                    
                    historyItem.setCreateTime(lastMessageDto.getUpdateTime());
                } catch (Exception e) {
                    Timber.tag("VMHistory").e( "处理 lastMessage content 时出错: " + e.getMessage());
                    historyItem.setSubtitle(null);
                    // 仍然设置时间，即使content处理失败
                    if (lastMessageDto.getUpdateTime() != null) {
                        historyItem.setCreateTime(lastMessageDto.getUpdateTime());
                    }
                }
            } else {
                // lastMessage 为空时的默认显示
                historyItem.setSubtitle(null);
            }

            historyItem.setConversationId(conversation.getId());
            historyItem.setModelType(conversation.getModelType());
            historyItem.setModelId(conversation.getModelId());
            historyItem.setModelName(conversation.getModelName());
            historyItem.setModel(conversation.getModel());
            items.add(historyItem);
        }

        return items;
    }

    private List<HistoryItem> convertDrawingToHistoryItems(List<DrawingImageDto> drawingImages) {
        List<HistoryItem> items = new ArrayList<>();
        if (drawingImages == null || drawingImages.isEmpty()) {
            return items;
        }

        String lastDate = "";

        try {
            for (DrawingImageDto image : drawingImages) {
                if (image == null) continue;

                // 添加日期分组头
//                String date = getDateFromDrawingTime(image.getCreateTime());
//                if (!date.equals(lastDate)) {
//                    items.add(new HistoryItem(HistoryItem.TYPE_DATE_HEADER, date, null, 0));
//                    lastDate = date;
//                }

                // 构建显示标题
                String title = image.getPrompt();
                if (title == null || title.trim().isEmpty()) {
                    title = "AI绘画";
                }
                if (title.length() > 20) {
                    title = title.substring(0, 20) + "...";
                }

                // 创建HistoryItem，使用图片URL构造函数
                String imageUrl = null;
                if (image.getThumbnailUrl() != null && !image.getThumbnailUrl().trim().isEmpty()) {
                    imageUrl = image.getThumbnailUrl();
                } else if (image.getImageUrl() != null && !image.getImageUrl().trim().isEmpty()) {
                    imageUrl = image.getImageUrl();
                }

                HistoryItem historyItem = new HistoryItem(HistoryItem.TYPE_ITEM, title, "", imageUrl);
                historyItem.setSessionId(image.getSessionId());
                historyItem.setCreateTime(Long.parseLong(image.getCreateTime()));
                historyItem.setExtraData("width",image.getWidth());
                historyItem.setExtraData("height",image.getHeight());
                items.add(historyItem);
            }
        } catch (Exception e) {
            // 如果转换过程中出错，返回已转换的部分
            Timber.tag("VMHistory").e( "转换绘画历史数据时出错: " + e.getMessage());
        }

        return items;
    }

    /**
     * 从绘画时间字符串中提取日期
     */
    private String getDateFromDrawingTime(String timeStr) {
        SimpleDateFormat outputFormat = new SimpleDateFormat("MM月dd日", Locale.getDefault());

        if (timeStr == null || timeStr.trim().isEmpty()) {
            return outputFormat.format(new Date());
        }

        try {
            // 尝试解析不同的时间格式
            Date date = null;

            if (timeStr.contains("-") && timeStr.contains(":")) {
                // 格式：2024-01-07 16:00:00 或 2024-01-07T16:00:00
                String cleanTimeStr = timeStr.replace("T", " ");
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                date = inputFormat.parse(cleanTimeStr);
            } else if (timeStr.contains("-") && !timeStr.contains(":")) {
                // 格式：2024-01-07
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                date = inputFormat.parse(timeStr);
            } else if (timeStr.matches("\\d{13}")) {
                // 13位时间戳（毫秒）
                date = new Date(Long.parseLong(timeStr));
            } else if (timeStr.matches("\\d{10}")) {
                // 10位时间戳（秒）
                date = new Date(Long.parseLong(timeStr) * 1000);
            } else {
                // 其他格式，使用当前时间
                Timber.tag("VMHistory").w( "无法解析时间格式: " + timeStr);
                date = new Date();
            }

            return outputFormat.format(date);

        } catch (Exception e) {
            // 解析失败，使用当前日期
            Timber.tag("VMHistory").e( "解析时间失败: " + timeStr + ", 错误: " + e.getMessage());
            return outputFormat.format(new Date());
        }
    }

    private List<HistoryItem> convertMeetingToHistoryItems(List<MeetingHistoryDto> meetings) {
        List<HistoryItem> items = new ArrayList<>();
        if (meetings == null || meetings.isEmpty()) {
            return items;
        }

        String lastDate = "";

        for (MeetingHistoryDto meeting : meetings) {
            // 使用会议名称中的时间戳或当前时间作为日期
            String date = getDateFromMeetingName(meeting.getName());
//            if (!date.equals(lastDate)) {
//                items.add(new HistoryItem(HistoryItem.TYPE_DATE_HEADER, date, null, 0));
//                lastDate = date;
//            }

            // 构建显示标题
            String title = meeting.getName();
            if (title == null || title.trim().isEmpty()) {
                title = "会议记录";
            }

            String subTitle = "";
            if(meeting.getContent() != null ){
                ZUtils.print("meeting.getContent()  = "+meeting.getContent() );
               String[] strArr = meeting.getContent().split("]") ;
                ZUtils.print("meeting.getContent()1  = "+strArr.length );
               if(strArr.length>1){
                   subTitle = strArr[1].trim();
                   ZUtils.print("meeting.getContent()1  = "+subTitle );
                   String[] strArr2 = subTitle.split("\\[");
                   ZUtils.print("meeting.getContent()2  = "+strArr2 );
                   if(strArr2.length>1){
                       subTitle = strArr2[0].trim();
                   }
               }
            }
            ZUtils.print("meeting.getContent()3  = "+subTitle );
            // 创建HistoryItem
            HistoryItem historyItem = new HistoryItem(HistoryItem.TYPE_ITEM, title, subTitle, com.fxzs.lingxiagent.R.drawable.ic_nav_meeting);
            historyItem.setMeetingId(meeting.getId());
            historyItem.setMeetingType(meeting.getType());
            historyItem.setCreateTime(meeting.getCreateTime());
            items.add(historyItem);
        }

        return items;
    }

    /**
     * 从会议名称中提取日期
     */
    private String getDateFromMeetingName(String meetingName) {
        if (meetingName == null) {
            return new SimpleDateFormat("MM月dd日", Locale.getDefault()).format(new Date());
        }

        // 尝试从会议名称中提取时间戳
        try {
            // 假设会议名称格式包含时间戳，如 "会议_20231201_143000"
            if (meetingName.contains("_")) {
                String[] parts = meetingName.split("_");
                for (String part : parts) {
                    if (part.length() == 8 && part.matches("\\d{8}")) {
                        // 解析日期格式 YYYYMMDD
                        String year = part.substring(0, 4);
                        String month = part.substring(4, 6);
                        String day = part.substring(6, 8);
                        return month + "月" + day + "日";
                    }
                }
            }
        } catch (Exception e) {
            // 解析失败，使用当前日期
        }

        return new SimpleDateFormat("MM月dd日", Locale.getDefault()).format(new Date());
    }

    public void updateName(HistoryItem item,String inputText) {
        int currentTab = currentTabIndex.getValue();
        updateName(item,inputText,currentTab);
    }
    public void updateName(HistoryItem item,String inputText,int currentTab) {
        String id = "";
        if (currentTab == VMHistory.TAB_CHAT && item.getConversationId() != null) {
            // 对话tab，跳转到对话页面
            id = item.getConversationId()+"";
            int modelType = item.getModelType();
            long modeId = item.getModelId();
        } else if (currentTab == VMHistory.TAB_AGENT && item.getConversationId() != null) {
            // 智能体tab，跳转到对话页面
            id = item.getConversationId()+"";
            int modelType = item.getModelType();
            long modeId = item.getModelId();
        } else if (currentTab == VMHistory.TAB_DRAWING && item.getSessionId() != null) {
            // AI绘画tab，调用详情接口并跳转
            id = item.getSessionId()+"";
        } else if (currentTab == VMHistory.TAB_MEETING && item.getMeetingId() != null) {
            // 会议tab，跳转到会议详情页面
            id = item.getMeetingId()+"";
        }else if (currentTab == VMHistory.TAB_PPT && item.getPptSessionId() != null) {
            id = item.getPptSessionId()+"";
        }else if (currentTab == VMHistory.TAB_TRANSLATE && item.getConversationId() != null) {
            id = item.getConversationId()+"";
        }else if (currentTab == VMHistory.TAB_EXCEL && item.getConversationId() != null) {
            // ai表格tab，跳转到对话页面
            id = item.getConversationId()+"";
        }
        if(TextUtils.isEmpty(id)){
            ZUtils.showToast("id为空");
            return;
        }
        if(currentTab == VMHistory.TAB_DRAWING){//绘画
            updateSessionName(id,inputText);
        }else  if(currentTab == VMHistory.TAB_MEETING){//会议
            updateMeetingName(id,inputText);
        }else  if(currentTab == VMHistory.TAB_PPT){//PPT
            updatePPTName(id,inputText);
        }else  if(currentTab == VMHistory.TAB_TRANSLATE){//同传
            updateTranslateName(item,currentTranslateType,inputText);
        }else {
            updateConversationName(id,inputText);
        }
    }

    public void updateTranslateName(HistoryItem item,String type,String inputText) {
        HttpRequest request = new HttpRequest();
        request.updateById(item.getConversationId()+"",inputText,type,new Observer<ApiResponse<String>>(){

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<String> response) {
                if(response.getData().equals("success")){
                    setMessage("更新成功");
                    refreshHistory();
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }


    // 更新会话名称
    public void updateConversationName(String id,String name) {

        HttpRequest request = new HttpRequest();
        request.updateMyEditName(id, name, new Observer<ApiResponse<Boolean>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<Boolean> booleanApiResponse) {

//                    GlobalToast.show(getActivity(),"删除成功",GlobalToast.Type.SUCCESS);
                setMessage("更新成功");
                refreshHistory();
            }


            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }
    // 更新会话名称
    public void updateSessionName(String id,String name) {
        DrawingRepository repository = DrawingRepositoryImpl.getInstance();
        DrawingSessionDto session = new DrawingSessionDto();
            session.setId(Long.valueOf(id));
            session.setName(name);
            repository.updateSession(session).observeForever(result -> {
                if (result.isSuccess()) {
                    // 更新成功
                    setMessage("更新成功");
                    refreshHistory();
                } else {
                    setError("更新会话失败");
                }
            });

    }
    // 更新会议
    public void updateMeetingName(String id,String name) {
        MeetingRepository repository = new MeetingRepositoryImpl();
        repository.updateMeetingName(Integer.valueOf(id), name)
                .observeForever(updateResult -> {
                    if (updateResult != null) {
                        if (updateResult.isSuccess()) {
                            setMessage("更新成功");
                            refreshHistory();
                        } else {
                        }
                    }
                });

    }

    // 更新PPT
    public void updatePPTName(String id,String name) {


        HttpRequest request = new HttpRequest();

        request.updatePptSession(id,name, new Observer<ApiResponse<String>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<String> stringApiResponse) {
//                if (updateResult.isSuccess()) {
                    setMessage("更新成功");
                    refreshHistory();
//                } else {
//                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });

    }

    /**
     * 从详情页返回后，仅更新当前列表中的会话标题/最后一条消息，避免分页位置回到第一页
     */
    public void updateConversationPreview(long conversationId, String title, String lastMessage) {
        List<HistoryItem> currentItems = historyItems.getValue();
        if (currentItems == null || currentItems.isEmpty()) {
            return;
        }

        boolean updated = false;
        for (HistoryItem item : currentItems) {
            if (item == null || item.getType() != HistoryItem.TYPE_ITEM || item.getConversationId() == null) {
                continue;
            }

            if (item.getConversationId() == conversationId) {
                if (!TextUtils.isEmpty(title)) {
                    item.setTitle(title);
                }
                if (!TextUtils.isEmpty(lastMessage)) {
                    String preview = lastMessage;
                    if (preview.length() > 30) {
                        preview = preview.substring(0, 30) + "...";
                    }
                    item.setSubtitle(preview);
                }
                updated = true;
                break;
            }
        }

        if (updated) {
            historyItems.setValue(new ArrayList<>(currentItems));
        }
    }

    /**
     * 显示加载指示器
     */
    private void showLoadingIndicator() {
        List<HistoryItem> currentItems = historyItems.getValue();
        if (currentItems == null) {
            currentItems = new ArrayList<>();
        }
        
        // 检查是否已经有加载指示器
        if (!currentItems.isEmpty() && 
            currentItems.get(currentItems.size() - 1).getType() == HistoryItem.TYPE_LOADING) {
            return; // 已经有加载指示器，不重复添加
        }
        
        // 添加加载指示器
        HistoryItem loadingItem = new HistoryItem(HistoryItem.TYPE_LOADING, "", "", 0);
        loadingItem.setExtraData("sourceType", AiWorkAdapter.TYPE_LOADING);
        currentItems.add(loadingItem);
        historyItems.setValue(currentItems);
    }
    
    /**
     * 移除加载指示器
     */
    private void removeLoadingIndicator(List<HistoryItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        
        // 从列表末尾开始查找并移除加载指示器
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).getType() == HistoryItem.TYPE_LOADING) {
                items.remove(i);
                break; // 只移除一个加载指示器
            }
        }
    }
}
