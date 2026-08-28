package com.fxzs.lingxiagent.viewmodel.translate;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.lingxi.translate.model.TranslateResult;
import com.fxzs.lingxiagent.model.common.BaseViewModel;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * 聆听模式ViewModel
 */
public class VMListenMode extends BaseViewModel {

    private static final String TAG = "VMListenMode";

    private MutableLiveData<List<TranslateResult>> translateResults = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<String> currentMidResult = new MutableLiveData<>();
    private MutableLiveData<String> sourceLanguage = new MutableLiveData<>("zh");
    private MutableLiveData<String> targetLanguage = new MutableLiveData<>("en");

    public VMListenMode(@NonNull Application application) {
        super(application);
    }


    private final MutableLiveData<Long> translationIdLive = new MutableLiveData<>(0L);
    public LiveData<Long> getTranslationIdLive() { return translationIdLive; }

    public LiveData<List<TranslateResult>> getTranslateResults() {
        return translateResults;
    }

    public LiveData<String> getCurrentMidResult() {
        return currentMidResult;
    }

    public LiveData<String> getSourceLanguage() {
        return sourceLanguage;
    }

    public LiveData<String> getTargetLanguage() {
        return targetLanguage;
    }

    public void setSourceLanguage(String language) {
        sourceLanguage.setValue(language);
    }

    public void setTargetLanguage(String language) {
        targetLanguage.setValue(language);
    }

    public void updateMidResult(String midResult) {
        currentMidResult.setValue(midResult);
    }

    public void addTranslateResult(String translationResult) {
        List<TranslateResult> currentResults = translateResults.getValue();
        if (currentResults == null) {
            currentResults = new ArrayList<>();
        }

        String currentMid = currentMidResult.getValue();
        if (currentMid != null) {
            TranslateResult result = new TranslateResult();
            result.setOriginalText(currentMid);
            result.setTranslatedText(translationResult);
            result.setTimestamp(System.currentTimeMillis());

            currentResults.add(result);
            translateResults.setValue(currentResults);

            // 清空中间结果
            currentMidResult.setValue("");
        }
    }



    /**
     * 每次开启麦克风时创建新的同传记录（聆听模式 type=1）
     */
    public void addListenTranslationRecord() {
        try {
            String name = new java.text.SimpleDateFormat("yyyyMMdd日HH:mm", java.util.Locale.getDefault())
                    .format(new java.util.Date()) + "同传";
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("name", name);
            body.put("type", 1);

            com.fxzs.lingxiagent.lingxi.translate.api.TranslationRecordApiService api =
                    com.fxzs.lingxiagent.network.RetrofitClient.getInstance()
                            .create(com.fxzs.lingxiagent.lingxi.translate.api.TranslationRecordApiService.class);

            retrofit2.Call<okhttp3.ResponseBody> call = api.addTranslationRecord(body);
            call.enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                    if (response.isSuccessful()) {
                        try {
                            okhttp3.ResponseBody rb = response.body();
                            if (rb != null) {
                                String json = rb.string();
                                org.json.JSONObject obj = new org.json.JSONObject(json);
                                org.json.JSONObject data = obj.optJSONObject("data");
                                long id = (data != null) ? data.optLong("id", 0L) : 0L;
                                if (id > 0) {
                                    // 更新当前会话ID
                                    translationIdLive.postValue(id);
                                    com.fxzs.lingxiagent.util.SharedPreferencesUtil.saveTranslationId(id);
                                    Timber.tag("VMListenMode").d( "新会话已创建，ID: " + id);
                                }
                            }
                        } catch (Exception ignore) {}
                    } else {
                        Timber.tag("VMListenMode").e( "同传记录创建失败 code=" + response.code());
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                    Timber.tag("VMListenMode").e( "同传记录创建异常: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Timber.tag("VMListenMode").e( "同传记录创建异常: " + e.getMessage());
        }
    }

    /**
     * 批量保存聆听模式下所有消息（在用户停止录音时调用）
     */
    public void batchSaveListenMessages(java.util.List<com.fxzs.lingxiagent.lingxi.translate.model.TranslationItem> items,
                                        String sourceLang, String targetLang,
                                        String startTime, String endTime) {
        // 优先使用当前会话的translationId
        long translationId = translationIdLive.getValue() != null ? translationIdLive.getValue() : 0L;
        if (translationId <= 0) {
            translationId = com.fxzs.lingxiagent.util.SharedPreferencesUtil.getTranslationId();
        }
        if (translationId <= 0) {
            Timber.tag("VMListenMode").e( "未获取到translationId，无法保存消息");
            setError("未获取到会话ID，无法保存消息");
            return;
        }

        // 如果没有翻译内容，不需要保存
        if (items == null || items.isEmpty()) {
            Timber.tag("VMListenMode").d( "没有翻译内容需要保存");
            setSuccess("会话已结束");
            return;
        }

        try {
            java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
            for (com.fxzs.lingxiagent.lingxi.translate.model.TranslationItem it : items) {
                // 只保存有内容的翻译项
                if (it.getSourceText() != null && !it.getSourceText().trim().isEmpty()) {
                    java.util.Map<String, Object> item = new java.util.HashMap<>();
                    item.put("sourceText", it.getSourceText());
                    item.put("targetText", it.getTargetText() != null ? it.getTargetText() : "");
                    item.put("startTime", startTime);
                    item.put("endTime", endTime);
                    item.put("translationId", translationId);
                    item.put("speakerId", 1); // 聆听模式统一为1
                    item.put("source", sourceLang);
                    item.put("target", targetLang);
                    list.add(item);
                }
            }

            if (list.isEmpty()) {
                Timber.tag("VMListenMode").d( "没有有效的翻译内容需要保存");
                setSuccess("会话已结束");
                return;
            }

            setLoading(true);
            com.fxzs.lingxiagent.lingxi.translate.api.TranslationRecordApiService api =
                    com.fxzs.lingxiagent.network.RetrofitClient.getInstance()
                            .create(com.fxzs.lingxiagent.lingxi.translate.api.TranslationRecordApiService.class);

            retrofit2.Call<okhttp3.ResponseBody> call = api.batchSaveTranslationMessage(list);
            call.enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                    setLoading(false);
                    if (!response.isSuccessful()) {
                        Timber.tag("VMListenMode").e( "消息批量保存失败 code=" + response.code());
                        setError("消息保存失败 code=" + response.code());
                    } else {
                        Timber.tag("VMListenMode").d( "消息批量保存成功，共保存 " + list.size() + " 条记录");
                        setSuccess("会话记录已保存");
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                    setLoading(false);
                    Timber.tag("VMListenMode").e( "消息批量保存异常: " + t.getMessage());
                    setError("消息保存异常: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            setLoading(false);
            Timber.tag("VMListenMode").e( "消息批量保存异常: " + e.getMessage());
            setError("消息保存异常: " + e.getMessage());
        }
    }

    protected void onCleared() {
        super.onCleared();
    }
}