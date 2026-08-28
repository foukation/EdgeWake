package com.fxzs.lingxiagent.viewmodel.chat.service;

import android.app.Application;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.model.chat.callback.CreateMyCallback;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepository;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepositoryImpl;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * 聊天会话相关服务：负责创建会话、智能体会话、会议会话以及会议绑定/更新。
 * 将 VMChat 中的会话创建逻辑拆分到此，降低 VMChat 复杂度。
 */
public class ConversationService {
    private final Application application;
    private final HttpRequest request;
    private final ObservableField<Long> conversationId;

    public ConversationService(@NonNull Application application,
                               @NonNull HttpRequest request,
                               @NonNull ObservableField<Long> conversationId) {
        this.application = application;
        this.request = request;
        this.conversationId = conversationId;
    }

    public void createMy(String model, String title, CreateMyCallback callback) {
        if (conversationId.get() != 0) { // 已创建则直接回调
            if (callback != null) callback.back();
            return;
        }
        request.createMy(model, title, new Observer<ApiResponse<Integer>>() {
            @Override public void onSubscribe(Disposable d) { }
            @Override public void onNext(ApiResponse<Integer> res) {
                if (res.getCode() == 0 && res.getData() != null) {
                    long id = Long.parseLong(res.getData().toString());
                    conversationId.setValue(id);
                    conversationId.postValue(id);
                    SharedPreferencesUtil.saveString(Constants.PREF_CONVERSATION_ID, String.valueOf(id));
                    if (callback != null) callback.back();
                }
            }
            @Override public void onError(Throwable e) { }
            @Override public void onComplete() { }
        });
    }

    public void createMyWithFile(String model, String title, CreateMyCallback callback) {
        request.createMy(model, title, new Observer<ApiResponse<Integer>>() {
            @Override public void onSubscribe(Disposable d) { }
            @Override public void onNext(ApiResponse<Integer> res) {
                if (res.getCode() == 0 && res.getData() != null) {
                    long id = Long.parseLong(res.getData().toString());
                    conversationId.setValue(id);
                    if (callback != null) callback.back();
                }
            }
            @Override public void onError(Throwable e) { }
            @Override public void onComplete() { }
        });
    }

    public void createMyAgent(String model, String title, String aiMenuId, CreateMyCallback callback) {
        if (conversationId.get() != 0) { // 已创建则直接回调
            if (callback != null) callback.back();
            return;
        }
        request.createMy(model, title, aiMenuId, new Observer<ApiResponse<Integer>>() {
            @Override public void onSubscribe(Disposable d) { }
            @Override public void onNext(ApiResponse<Integer> res) {
                if (res.getCode() == 0 && res.getData() != null) {
                    long id = Long.parseLong(res.getData().toString());
                    conversationId.setValue(id);
                    SharedPreferencesUtil.saveAgentMap(model, String.valueOf(conversationId.get()));
                    if (callback != null) callback.back();
                }
            }
            @Override public void onError(Throwable e) { }
            @Override public void onComplete() { }
        });
    }

    public void createMyMeeting(String model, String title, String systemMessage, CreateMyCallback callback) {
        request.createMyMeeting(model, title, systemMessage, new Observer<ApiResponse<Integer>>() {
            @Override public void onSubscribe(Disposable d) { }
            @Override public void onNext(ApiResponse<Integer> res) {
                if (res.getCode() == 0 && res.getData() != null) {
                    long id = Long.parseLong(res.getData().toString());
                    conversationId.setValue(id);
                    if (callback != null) callback.back();
                }
            }
            @Override public void onError(Throwable e) { }
            @Override public void onComplete() { }
        });
    }

    public void bindMeetingAndConversationId(String meetingId, String conversionId, CreateMyCallback callback) {
        MeetingRepository repository = new MeetingRepositoryImpl();
        repository.bindMeetingAndConversationId(meetingId, conversionId).observeForever(updateResult -> {
            if (updateResult != null && updateResult.isSuccess()) {
                if (callback != null) callback.back();
            }
        });
    }

    public void updateMyMeeting(String id, String systemMessage, CreateMyCallback callback) {
        request.updateMyMeeting(id, systemMessage, new Observer<ApiResponse<Boolean>>() {
            @Override public void onSubscribe(Disposable d) { }
            @Override public void onNext(ApiResponse<Boolean> res) {
                if (res.getCode() == 0) {
                    if (callback != null) callback.back();
                }
            }
            @Override public void onError(Throwable e) { }
            @Override public void onComplete() { }
        });
    }
}

