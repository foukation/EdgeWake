package com.fxzs.lingxiagent.viewmodel.translate;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.lingxi.translate.model.DialogResult;
import com.fxzs.lingxiagent.model.common.BaseViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话模式ViewModel
 */
public class VMDialogMode extends BaseViewModel {

    private static final String TAG = "VMDialogMode";

    private MutableLiveData<List<DialogResult>> dialogResults = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<String> currentMidResultA = new MutableLiveData<>();
    private MutableLiveData<String> currentMidResultB = new MutableLiveData<>();
    private MutableLiveData<String> languageA = new MutableLiveData<>("en");
    private MutableLiveData<String> languageB = new MutableLiveData<>("zh");
    private final MutableLiveData<Long> translationIdLive = new MutableLiveData<>(0L);

    public VMDialogMode(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<DialogResult>> getDialogResults() {
        return dialogResults;
    }

    public LiveData<String> getCurrentMidResultA() {
        return currentMidResultA;
    }

    public LiveData<String> getCurrentMidResultB() {
        return currentMidResultB;
    }

    public LiveData<String> getLanguageA() {
        return languageA;
    }

    public LiveData<String> getLanguageB() {
        return languageB;
    }

    public void setLanguageA(String language) {
        languageA.setValue(language);
    }

    public void setLanguageB(String language) {
        languageB.setValue(language);
    }

    public void updateMidResultA(String midResult) {
        currentMidResultA.setValue(midResult);
    }

    public void updateMidResultB(String midResult) {
        currentMidResultB.setValue(midResult);
    }

    public void addDialogResult(String translationResult, boolean isFromA) {
        List<DialogResult> currentResults = dialogResults.getValue();
        if (currentResults == null) {
            currentResults = new ArrayList<>();
        }

        String currentMid = isFromA ? currentMidResultA.getValue() : currentMidResultB.getValue();
        if (currentMid != null) {
            DialogResult result = new DialogResult();
            result.setOriginalText(currentMid);
            result.setTranslatedText(translationResult);
            result.setTimestamp(System.currentTimeMillis());
            result.setFromLanguageA(isFromA);

            currentResults.add(result);
            dialogResults.setValue(currentResults);

            // 清空对应的中间结果
            if (isFromA) {
                currentMidResultA.setValue("");
            } else {
                currentMidResultB.setValue("");
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public LiveData<Long> getTranslationIdLive() { return translationIdLive; }

    /**
     * 进入对话模式页面时添加同传记录（对话模式 type=2）
     * 将网络调用放在 ViewModel 中以符合 MVVM
     */
    public void addDialogTranslationRecord() {
        setLoading(true);
        try {
            String name = new java.text.SimpleDateFormat("yyyyMMdd日HH:mm", java.util.Locale.getDefault())
                    .format(new java.util.Date()) + "同传";
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("name", name);
            body.put("type", 2);

            com.fxzs.lingxiagent.lingxi.translate.api.TranslationRecordApiService api =
                    com.fxzs.lingxiagent.network.RetrofitClient.getInstance()
                            .create(com.fxzs.lingxiagent.lingxi.translate.api.TranslationRecordApiService.class);

            retrofit2.Call<okhttp3.ResponseBody> call = api.addTranslationRecord(body);
            call.enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                    setLoading(false);
                    if (response.isSuccessful()) {
                        try {
                            okhttp3.ResponseBody rb = response.body();
                            if (rb != null) {
                                String json = rb.string();
                                org.json.JSONObject obj = new org.json.JSONObject(json);
                                org.json.JSONObject data = obj.optJSONObject("data");
                                long id = (data != null) ? data.optLong("id", 0L) : 0L;
                                if (id > 0) {
                                    translationIdLive.postValue(id);
                                    com.fxzs.lingxiagent.util.SharedPreferencesUtil.saveTranslationId(id);
                                }
                            }
                        } catch (Exception ignore) {}
//                        setSuccess("同传记录已创建");
                    } else {
                        setError("同传记录创建失败 code=" + response.code());
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                    setError("同传记录创建异常: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            setError("同传记录创建异常: " + e.getMessage());
        }
    }

    /**
     * 保存最新一条对话消息到服务端（批量接口，但我们只传一条）
     */
    public void saveLatestDialogMessage(com.fxzs.lingxiagent.lingxi.translate.model.DialogRecord record) {
        long translationId = translationIdLive.getValue() != null ? translationIdLive.getValue() : 0L;
        if (translationId <= 0) {
            translationId = com.fxzs.lingxiagent.util.SharedPreferencesUtil.getTranslationId();
        }
        if (translationId <= 0) {
            setError("未获取到translationId，无法保存消息");
            return;
        }

        try {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("sourceText", record.getSourceText());
            item.put("targetText", record.getTargetText());
            item.put("startTime", record.getStartTime());
            item.put("endTime", record.getEndTime());
            item.put("translationId", translationId);
            item.put("speakerId", record.getSpeakerId());
            item.put("source", record.getSource());
            item.put("target", record.getTarget());

            java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
            list.add(item);

            com.fxzs.lingxiagent.lingxi.translate.api.TranslationRecordApiService api =
                    com.fxzs.lingxiagent.network.RetrofitClient.getInstance()
                        .create(com.fxzs.lingxiagent.lingxi.translate.api.TranslationRecordApiService.class);

            retrofit2.Call<okhttp3.ResponseBody> call2 = api.batchSaveTranslationMessage(list);
            call2.enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                    if (!response.isSuccessful()) {
                        setError("消息保存失败 code=" + response.code());
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                    setError("消息保存异常: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            setError("消息保存异常: " + e.getMessage());
        }
    }

}