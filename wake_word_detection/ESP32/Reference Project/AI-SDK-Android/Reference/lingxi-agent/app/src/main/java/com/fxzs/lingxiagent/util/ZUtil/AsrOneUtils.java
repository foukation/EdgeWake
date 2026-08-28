package com.fxzs.lingxiagent.util.ZUtil;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.fxzs.lingxiagent.IYAApplication;
import com.fxzs.lingxiagent.model.chat.callback.AsrCallback;
import com.fxzs.lingxiagent.model.chat.dto.AsrBean;
import com.fxzs.lingxiagent.util.GlobalSettings;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.google.gson.Gson;
import com.tencent.cloud.qcloudasrsdk.onesentence.QCloudOneSentenceRecognizer;
import com.tencent.cloud.qcloudasrsdk.onesentence.QCloudOneSentenceRecognizerListener;
import com.tencent.cloud.qcloudasrsdk.onesentence.utils.AAILogger;
import com.tencent.cloud.qcloudasrsdk.onesentence.utils.LoggerListener;

import java.util.ArrayList;
import java.util.List;

public class AsrOneUtils {

    static String appid = "1330351148";
    static String secretId = "AKIDhaP8lPrMfleMsP4oRuDJCrMsHWc0GLLq";
    static String secretKey = "XqvoUwJYjldJ4e3FE0Zu3ziEYgbJn12q";

    static String mResult = "";
    private static final AsrOneUtils instance = new AsrOneUtils();


    public static AsrOneUtils getInstance() {
        return instance;
    }

     QCloudOneSentenceRecognizer recognizer;
     List<AsrCallback> mCallbacks = new ArrayList<>();

    Activity mActivity;
    public  void init(Activity activity){
        mActivity = activity;
        recognizer = new QCloudOneSentenceRecognizer(activity, appid, secretId, secretKey);

        recognizer.setCallback(new QCloudOneSentenceRecognizerListener() {
            @Override
            public void didStartRecord() {
                ZUtils.print("AAILogger didStartRecord = ");
            }

            @Override
            public void didStopRecord() {
                ZUtils.print("AAILogger didStopRecord = ");

            }

            @Override
            public void recognizeResult(QCloudOneSentenceRecognizer recognizer, String result, Exception exception) {
                mResult = result;
                ZUtils.print("AAILogger recognizeResult = "+result);

                if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    if (mCallbacks != null && mCallbacks.size() > 0) {
                        mCallbacks.get(mCallbacks.size() -1).callback(getResult());
//                        for (AsrCallback mCallback : mCallbacks) {
//                            mCallback.callback(getResult());
//                        }
                    }
                }
//                mCallback.callback(getResult());
            }
        });
        setLog();
    }

    public  void setCallBack(AsrCallback callback){
        mCallbacks.add(callback);
    }
    public  void removeCallBack(){
        if(mCallbacks.size() > 0){
            mCallbacks.remove(mCallbacks.size() - 1);
        }
    }
    public  void recognizer(){
        if(recognizer == null){
            return;
        }
        /**
         * setDefaultParams 默认参数param
         * @param filterDirty    0 ：默认状态 不过滤脏话 1：过滤脏话
         * @param filterModal    0 ：默认状态 不过滤语气词  1：过滤部分语气词 2:严格过滤
         * @param filterPunc     0 ：默认状态 不过滤句末的句号 1：滤句末的句号
         * @param convertNumMode 1：默认状态 根据场景智能转换为阿拉伯数字；0：全部转为中文数字。
         * @param hotwordId  热词id，不使用则传null
         * @param engSerViceType  引擎模型类型，传null默认使用“16k_zh”
         */
        int filterDirty = 0;
        int filterModal = 0;
        int filterPunc = 0;
        int convertNumMode = 1;
        String hotwordId = null;
        String engSerViceType = "16k_zh";
         engSerViceType = getCurrentLanguage();
         ZUtils.print("getCurrentLanguage = "+ engSerViceType);

        recognizer.setDefaultParams(filterDirty, filterModal, filterPunc, convertNumMode,hotwordId,engSerViceType);
        try{
            recognizer.recognizeWithRecorder();
        }catch (Exception e){
            ZUtils.print("AAILogger Exception = "+e.toString());
        }
    }

    public  void stop(){
        if(recognizer != null){
            recognizer.stopRecognizeWithRecorder();
        }
    }

    public  void setLog(){
//        log组件设置
// 将log落盘到本地磁盘，needLogFile字段默认为false，接入调试期间建议设置为true，上线后此接口调用可删除。
        AAILogger.setNeedLogFile(true, IYAApplication.getInstance());
// 设置日志级别，默认为ERROR_LEVEL，接入调试期间建议设置为DEBUG_LEVEL。
        AAILogger.setLogLevel(AAILogger.DEBUG_LEVEL);
// 设置日志监听器，用于监听日志信息。
        AAILogger.setLoggerListener(new LoggerListener() {
            @Override
            public void onLogInfo(String s) {
                ZUtils.print("AAILogger = "+s);
            }
        });

    }

    public  String getResult() {
        String result = "";
        Gson gson = new Gson();


        AsrBean bean =     gson.fromJson(mResult, AsrBean.class);
        if(bean != null){
            result = bean.Response.Result;
        }
        if(TextUtils.isEmpty(result)){
            GlobalToast.show(mActivity,"未识别到文字", GlobalToast.Type.ERROR);
        }
        ZUtils.print("返回的音频 === "+result);
        return result;
    }
    public String getCurrentLanguage() {
        // 获取当前语言设置
        String languageCode = GlobalSettings.getInstance().getSelectedLanguageCode();
        if (languageCode == null) {
            languageCode = SharedPreferencesUtil.getLanguage();
        }
        return languageCode;
    }
}
