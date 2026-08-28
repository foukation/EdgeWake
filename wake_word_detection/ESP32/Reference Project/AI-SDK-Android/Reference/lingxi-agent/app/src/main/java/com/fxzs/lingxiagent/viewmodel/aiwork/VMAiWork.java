package com.fxzs.lingxiagent.viewmodel.aiwork;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.view.aiwork.AiWorkAdapter;
import com.fxzs.lingxiagent.viewmodel.history.HistoryViewModelFactory;
import com.fxzs.lingxiagent.viewmodel.history.VMHistory;
import com.fxzs.lingxiagent.viewmodel.user.VMUserProfile;

import timber.log.Timber;

public class VMAiWork extends BaseViewModel {

    private VMHistory vmHistory;
    VMUserProfile vmUserProfile;
    Activity mActivity;

    public VMAiWork(@NonNull Application application) {
        super(application);
        // 初始化vmHistory，确保分页功能正常工作
        vmHistory = new VMHistory();
        vmUserProfile = new VMUserProfile(application);
//        request = new HttpRequest();
//        repository = DrawingRepositoryImpl.getInstance();

    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // 清理资源
    }

    public VMHistory getVmHistory() {
        return vmHistory;
    }

    public VMUserProfile getVmUserProfile() {
        return vmUserProfile;
    }

    /**
     * 加载历史记录
     */
    public void loadHistory(int tabIndex/*,boolean isRefresh*/) {
//        Integer tabIndex = currentTabIndex.getValue();
//        if (tabIndex == null) return;

//        if (isRefresh) {
//            isRefreshing.setValue(true);
//        } else {
//            setLoading(true);
//        }

//        Timber.tag("VMHistory").d( "loadHistory for tab: " + tabIndex + ", isRefresh: " + isRefresh);
        vmHistory.getHistoryItems().getValue().clear();
        switch (tabIndex) {
            case AiWorkAdapter.TYPE_DRAWING:
                Timber.tag("VMHistory").d( "加载绘画历史");
                vmHistory.getCurrentTabIndex().setValue(VMHistory.TAB_DRAWING);
                vmHistory.loadDrawingHistory(true);
                break;
            case AiWorkAdapter.TYPE_MEETING:
                Timber.tag("VMHistory").d( "加载会议历史");
                vmHistory.getCurrentTabIndex().setValue(VMHistory.TAB_MEETING);
                vmHistory.loadMeetingHistory(true);
                break;
            case AiWorkAdapter.TYPE_PPT:
//                Timber.tag("VMHistory").d( "加载PPT/翻译历史（暂未实现）");
                // TODO: 实现PPT和翻译历史记录
//                handleEmptyResult(isRefresh);
                vmHistory.getCurrentTabIndex().setValue(VMHistory.TAB_PPT);
                vmHistory.loadPptHistory(true);

                break;

            case AiWorkAdapter.TYPE_TRANSLATE:
                vmHistory.getCurrentTabIndex().setValue(VMHistory.TAB_TRANSLATE);
                loadHistoryTranslate("1");
                break;
            default:
               Timber.tag("VMHistory").w("未知的Tab类型: %s", tabIndex);
//                handleEmptyResult(isRefresh);
                break;
        }
    }

    public void loadHistoryTranslate(String index/*,boolean isRefresh*/) {

        vmHistory.loadTranslateHistory(true,index);
        vmHistory.getCurrentTabIndex().setValue(VMHistory.TAB_TRANSLATE);
    }


    public void setActivity(Activity activity) {
        mActivity = activity;

        // 如果vmHistory已经在构造函数中初始化，不需要重复初始化
        // 这样可以保持分页状态
        if (vmHistory == null) {
            HistoryViewModelFactory factory = new HistoryViewModelFactory();
            vmHistory = new ViewModelProvider((ViewModelStoreOwner) activity, factory).get(VMHistory.class);
        }
        
        if (vmUserProfile == null) {
            vmUserProfile = new ViewModelProvider((ViewModelStoreOwner) activity).get(VMUserProfile.class);
        }
    }
    public void loadUserProfile(){
        vmUserProfile.loadUserProfile();
    }

}