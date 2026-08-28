package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.user.dto.FeedbackDto;
import com.fxzs.lingxiagent.model.user.repository.UserRepository;
import com.fxzs.lingxiagent.model.user.repository.UserRepositoryImpl;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class VMFeedbackHistory extends BaseViewModel {
    
    private static final String TAG = "VMFeedbackHistory";
    
    // 双向绑定字段
    private final ObservableField<Boolean> hasData = new ObservableField<>(false);
    
    // 业务状态
    private final MutableLiveData<List<FeedbackDto>> feedbackList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<FeedbackDto> selectedFeedback = new MutableLiveData<>();
    
    // Repository
    private final UserRepository userRepository;
    
    // 分页参数
    private int currentPage = 1;
    private static final int PAGE_SIZE = 20;
    
    public VMFeedbackHistory(@NonNull Application application) {
        super(application);
        userRepository = new UserRepositoryImpl();
        // 加载反馈历史
        loadFeedbackHistory();
    }
    
    // Getters
    public ObservableField<Boolean> getHasData() {
        return hasData;
    }
    
    public MutableLiveData<List<FeedbackDto>> getFeedbackList() {
        return feedbackList;
    }
    
    public MutableLiveData<FeedbackDto> getSelectedFeedback() {
        return selectedFeedback;
    }
    
    // 业务方法
    public void loadFeedbackHistory() {
        setLoading(true);
        
        Timber.tag(TAG).d( "Loading feedback history, page: " + currentPage);
        
        userRepository.getFeedbackHistory(currentPage, PAGE_SIZE, new UserRepository.Callback<List<FeedbackDto>>() {
            @Override
            public void onSuccess(List<FeedbackDto> result) {
                Timber.tag(TAG).d( "Feedback history loaded successfully, count: " + result.size());
                feedbackList.postValue(result);
                hasData.postValue(!result.isEmpty());
                setLoading(false);
            }
            
            @Override
            public void onError(String error) {
                Timber.tag(TAG).e( "Failed to load feedback history: " + error);
                setError("加载失败: " + error);
                setLoading(false);
                // 如果加载失败，使用空列表
                feedbackList.postValue(new ArrayList<>());
                hasData.postValue(false);
            }
        });
    }
    
    public void selectFeedback(FeedbackDto feedback) {
        selectedFeedback.setValue(feedback);
    }
    
    public void refreshData() {
        currentPage = 1;
        loadFeedbackHistory();
    }
    
    public void loadMoreData() {
        currentPage++;
        loadFeedbackHistory();
    }
}