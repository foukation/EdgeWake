package com.fxzs.lingxiagent.viewmodel.translate;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.lingxi.translate.model.TranslateDetailBean;
import com.fxzs.lingxiagent.lingxi.translate.model.TranslateResult;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * 聆听模式ViewModel
 */
public class VMTranslateDetail extends BaseViewModel {

    private static final String TAG = "VMListenMode";

    private MutableLiveData<List<TranslateResult>> translateResults = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<String> translateType = new MutableLiveData<>("");

    public VMTranslateDetail(@NonNull Application application) {
        super(application);
    }


    private final MutableLiveData<Long> translationIdLive = new MutableLiveData<>(0L);
    public LiveData<Long> getTranslationIdLive() { return translationIdLive; }

    public LiveData<List<TranslateResult>> getTranslateResults() {
        return translateResults;
    }

    public MutableLiveData<String> getTranslateType() {
        return translateType;
    }

    public void getList(String id, String type){
        HttpRequest request = new HttpRequest();
        request.getDetailById(id, type, new Observer<ApiResponse<TranslateDetailBean>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<TranslateDetailBean> res) {
                if (res.getData() == null) {
                    return;
                }
                List<TranslateResult> list = res.getData().getMessageList();

                translateResults.postValue(list);
                translateType.postValue(res.getData().getType()+"");

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }
//    public void  List<TranslateResult>
}