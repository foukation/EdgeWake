package com.fxzs.lingxiagent.util.ZUtil;

import android.content.Context;

import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.model.chat.api.getStsConfig;
import com.fxzs.lingxiagent.model.chat.callback.StsCallback;
import com.fxzs.lingxiagent.model.chat.dto.Credentials;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.HttpRequest;
import com.fxzs.lingxiagent.util.ZUtils;
import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.CosXmlServiceConfig;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;
import com.tencent.cos.xml.listener.CosXmlProgressListener;
import com.tencent.cos.xml.listener.CosXmlResultListener;
import com.tencent.cos.xml.model.CosXmlRequest;
import com.tencent.cos.xml.model.CosXmlResult;
import com.tencent.cos.xml.model.object.PutObjectRequest;
import com.tencent.cos.xml.transfer.COSXMLUploadTask;
import com.tencent.cos.xml.transfer.TransferConfig;
import com.tencent.cos.xml.transfer.TransferManager;
import com.tencent.qcloud.core.auth.SessionQCloudCredentials;

import java.io.File;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class SessionUpload {

    private static final String TAG = "SessionUpload";

    // 获取临时密钥（业务层控制获取的方式）
    String tmpSecretId = "SECRETID"; // 临时密钥 SecretId
    String tmpSecretKey = "SECRETKEY"; // 临时密钥 SecretKey
    String sessionToken = "SESSIONTOKEN"; // 临时密钥 Token
    long expiredTime = 1556183496L;//临时密钥有效截止时间戳，单位是秒
    // 建议返回服务器时间作为签名的开始时间，避免由于用户手机本地时间偏差过大导致请求过期
    long startTime = 1556182000L; //临时密钥有效起始时间，单位是秒
    SessionQCloudCredentials sessionQCloudCredentials = new SessionQCloudCredentials(tmpSecretId, tmpSecretKey,
            sessionToken, startTime, expiredTime);

    // 存储桶所在地域简称，例如广州地区是 ap-guangzhou
    public static String mRegion = "ap-guangzhou";
    // 创建 CosXmlServiceConfig 对象，根据需要修改默认的配置参数
//    CosXmlServiceConfig serviceConfig = new CosXmlServiceConfig.Builder()
//            .setRegion(region)
//            .isHttps(true) // 使用 HTTPS 请求, 默认为 HTTP 请求
//            .builder();



    static getStsConfig mGetStsConfig;
    static COSXMLUploadTask uploadTask;

    /**入口*/
    public static void upload(Context context, String filePath, StsCallback callback)  {
        upload(context, filePath, null, callback);
    }

    /**入口：支持指定 fileType，可为 null*/
    public static void upload(Context context, String filePath, @Nullable String fileType, StsCallback callback)  {
        // 假设需要上传的文件本地路径为 filePath
//        String filePath = "/storage/emulated/0/recording_1745066407619.mp3
//        recording_1745065980687.mp3";
        ZUtils.print("filePath = "+filePath);

// 1、从服务端请求上传和签名信息
        File file = new File(filePath);
//        String fileMType = FileUtils.getMimeTypeByFile(file);
//        String fileType = FileUtils.getExtensionFromMimeType(fileMType);
        String resolvedFileType = fileType;
        if (resolvedFileType == null || resolvedFileType.isEmpty()) {
            resolvedFileType = "jpg";
            if(filePath.contains(".")){
                String[] keyArray =filePath.split("\\.");
                resolvedFileType = keyArray[keyArray.length-1];
                ZUtils.print(" fileType = "+resolvedFileType);
            }
        }

// getKeyAndCredentials 方法见下一个代码块
//        JSONObject keyAndCredentials = getKeyAndCredentials(file.getName());

        getStsConfig(new StsCallback(){

            @Override
            public void progress(long percent) {

            }

            @Override
            public void callback(String url) {
               String[] keyArray = filePath.split("/");

                int keyLength = keyArray.length;
                String key = keyArray[keyLength-1];
//                Utils.print("key == "+key);
                String region = mRegion;
                String bucket = "ppt-1320245968";
                // String cosKey = key;
                String cosKey = mGetStsConfig.getObjectKey();
                long startTime = mGetStsConfig.getStartTime();

// 取临时密钥示例1：如果通过 qcloud-cos-sts-sdk 获取临时密钥字段的格式。在返回值里取临时密钥信息，上传的文件路径信息
                long expiredTime = mGetStsConfig.getExpiredTime();
//                JSONObject credentials = keyAndCredentials.getJSONObject("credentials");
                Credentials credentials = mGetStsConfig.getCredentials();
                String tmpSecretId = credentials.getTmpSecretId();
                String tmpSecretKey = credentials.getTmpSecretKey();
                String sessionToken = credentials.getSessionToken();


// 2、初始化 COS SDK: CosXmlService 和 TransferManager
// 创建 CosXmlServiceConfig 对象，根据需要修改默认的配置参数
                CosXmlServiceConfig serviceConfig = new CosXmlServiceConfig.Builder()
                        .setRegion(region)
                        .isHttps(true) // 使用 HTTPS 请求, 默认为 HTTP 请求
                        .builder();
// 初始化一个 CosXmlService 的实例，可以不设置临时密钥回调
                CosXmlService cosXmlService = new CosXmlService(context, serviceConfig);
// 初始化 TransferConfig，这里使用默认配置，如果需要定制，请参考 SDK 接口文档
                TransferConfig transferConfig = new TransferConfig.Builder().build();
// 初始化 TransferManager
                TransferManager transferManager = new TransferManager(cosXmlService, transferConfig);

// 3、进行上传
                PutObjectRequest putRequest = new PutObjectRequest(bucket, cosKey, filePath);
                SessionQCloudCredentials sessionQCloudCredentials = new SessionQCloudCredentials(tmpSecretId, tmpSecretKey,
                        sessionToken, startTime, expiredTime);
                putRequest.setCredential(sessionQCloudCredentials);
                uploadTask = transferManager.upload(putRequest, null);
// 设置上传进度回调
                uploadTask.setCosXmlProgressListener(new CosXmlProgressListener() {
                    @Override
                    public void onProgress(long complete, long target) {
                        // todo Do something to update progress...
                        ZUtils.print("onProgress = complete = "+complete + "  target = "+target);
                        long percent = (complete/target)*100;
                        callback.progress(percent);
                    }
                });
// 设置返回结果回调
                uploadTask.setCosXmlResultListener(new CosXmlResultListener() {
                    @Override
                    public void onSuccess(CosXmlRequest request, CosXmlResult result) {
                        COSXMLUploadTask.COSXMLUploadTaskResult uploadResult =
                                (COSXMLUploadTask.COSXMLUploadTaskResult) result;
                        ZUtils.print("onSuccess = "+result.accessUrl);

                        //TODO 拿到url，处理自己的逻辑
                        callback.callback(result.accessUrl);
                        clearUploadTask();
                    }

                    // 如果您使用 kotlin 语言来调用，请注意回调方法中的异常是可空的，否则不会回调 onFail 方法
                    // 即：clientException 的类型为 CosXmlClientException?，serviceException 的类型为 CosXmlServiceException?
                    @Override
                    public void onFail(CosXmlRequest request,
                                       @Nullable CosXmlClientException clientException,
                                       @Nullable CosXmlServiceException serviceException) {

                        callback.error(clientException,serviceException);
                        if (clientException != null) {
                            clientException.printStackTrace();
                        } else {
                            serviceException.printStackTrace();
                        }
                        clearUploadTask();
                    }
                });
            }

            @Override
            public void error(@Nullable CosXmlClientException clientException, @Nullable CosXmlServiceException serviceException) {

            }
        }, resolvedFileType);
    }


    public static void getStsConfig(StsCallback callback, String fileType){
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.getStsConfig(new Observer<ApiResponse<getStsConfig>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(ApiResponse<getStsConfig> getStsConfigApiResponse) {

                if(getStsConfigApiResponse!=null&&getStsConfigApiResponse.getData()!=null){
                    mGetStsConfig = getStsConfigApiResponse.getData();
                    callback.callback("");
                }

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        }, fileType);
    }

    public static COSXMLUploadTask getUploadTask() {
        return uploadTask;
    }

    /** 取消进行中的上传并释放静态引用，避免 listener 链持有 ViewModel/Activity */
    public static void cancelUpload() {
        if (uploadTask != null) {
            uploadTask.cancel();
            clearUploadTask();
        }
    }

    private static void clearUploadTask() {
        if (uploadTask != null) {
            uploadTask.setCosXmlProgressListener(null);
            uploadTask.setCosXmlResultListener(null);
            uploadTask = null;
        }
    }

    //    public static void addAiMeetingRecord(String fileUrl){
//        HttpRequest httpRequest = new HttpRequest();
////        {
////            "name": "20250418日23:22分的录音",
////                "fileUrl": "https://ppt-1320245968.cos.ap-guangzhou.myqcloud.com/mouse/20250418/202504181744989726053.wav",
////                "type": 3
////        }
//        httpRequest.addAiMeetingRecord(fileUrl,new Observer<ApiResponse<String>>() {
//            @Override
//            public void onSubscribe(Disposable d) {
//
//            }
//
//            @Override
//            public void onNext(ApiResponse<String> data) {
//
//                if(data!=null&&data.getData()!=null){
//                    String meetingId = data.getData();
//                    soundRecordRecognition(fileUrl,meetingId);
//                }
//
//            }
//
//            @Override
//            public void onError(Throwable e) {
//
//            }
//
//            @Override
//            public void onComplete() {
//
//            }
//        });
//    }
//
//    public static void soundRecordRecognition(String fileUrl,String meetingId){
//        HttpRequest httpRequest = new HttpRequest();
//        httpRequest.soundRecordRecognition(fileUrl,meetingId,new Observer<ApiResponse<String>>() {
//            @Override
//            public void onSubscribe(Disposable d) {
//
//            }
//
//            @Override
//            public void onNext(ApiResponse<String> data) {
//
//                if(data!=null&&data.getData()!=null){
//                    Utils.print("成功上传会议");
//                }
//
//            }
//
//            @Override
//            public void onError(Throwable e) {
//
//            }
//
//            @Override
//            public void onComplete() {
//
//            }
//        });
//    }

    @Nullable
    public static String getUrlFileSuffix(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            String pure = url;
            int q = pure.indexOf('?');
            if (q >= 0) {
                pure = pure.substring(0, q);
            }
            int slash = pure.lastIndexOf('/');
            String name = slash >= 0 ? pure.substring(slash + 1) : pure;
            int dot = name.lastIndexOf('.');
            if (dot > 0 && dot < name.length() - 1) {
                return name.substring(dot + 1);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
