package com.fxzs.lingxiagent.viewmodel.chat.service;

import android.content.Context;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.lingxi.translate.TranslationManager;

import timber.log.Timber;

/**
 * TranslationInteractor
 * 职责：
 * - 封装 TranslationManager 生命周期管理（启动/释放）
 * - 文本翻译：textTranslate 直接桥接 Kotlin 回调
 * - 实时翻译：startRealTime/stopRealTime，供 VMChat 在按键流程中调用
 */
public class TranslationInteractor {

    public interface Callback {
        void onMidAsr(@NonNull String mid);
        void onFinalAsr(@NonNull String fin);
        void onTranslation(@NonNull String text);
        void onComplete();
    }

    private TranslationManager translationManager;
    private final Context appContext;

    public interface ErrorCallback {
        void onBenefitError(@NonNull String msg);
    }

    public TranslationInteractor(Context appContext) {
        this.appContext = appContext;
    }

    public void textTranslate(String content, String fromLang, String toLang, kotlin.jvm.functions.Function1<com.cmdc.ai.assist.constraint.TranslateResponse, kotlin.Unit> onSuccess, ErrorCallback errorCallback) {
        TranslationManager tm = new TranslationManager(appContext, errorCallback::onBenefitError);
        tm.textTranslate(content, fromLang, toLang, onSuccess);
    }

    public void startRealTime(String lang1, String lang2, TranslationManager.TranslationCallback callback, ErrorCallback errorCallback) {
        translationManager = new TranslationManager(appContext, callback, errorCallback::onBenefitError);
        translationManager.translation(lang1, lang2);
    }

    public void stopRealTime() {
        if (translationManager != null) {
            translationManager.release();
        }
    }
}

