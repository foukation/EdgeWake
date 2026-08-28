//package com.fxzs.lingxiagent.util.ZUtil;
//
//import android.content.Context;
//
//import com.fxzs.lingxiagent.IYAApplication;
//import com.tencent.aai.AAIClient;
//import com.tencent.aai.audio.data.AudioRecordDataSource;
//import com.tencent.aai.auth.LocalCredentialProvider;
//import com.tencent.aai.exception.ClientException;
//import com.tencent.aai.exception.ServerException;
//import com.tencent.aai.listener.AudioRecognizeResultListener;
//import com.tencent.aai.listener.AudioRecognizeStateListener;
//import com.tencent.aai.log.AAILogger;
//import com.tencent.aai.log.LoggerListener;
//import com.tencent.aai.model.AudioRecognizeConfiguration;
//import com.tencent.aai.model.AudioRecognizeRequest;
//import com.tencent.aai.model.AudioRecognizeResult;
//
//public class AsrUtils {
//    static int appid = 0;
//    static int projectId = 0; //此参数固定为0；
//    static String secretId = "XXX";
//    static String secretKey = "XXX";
//
//    static AAIClient aaiClient = null;
//    public static void  init(Context context){
//
//
//        /**直接鉴权**/
//        // 1. 签名鉴权类，sdk中给出了一个本地的鉴权类，您也可以自行实现CredentialProvider接口，在您的服务器上实现鉴权签名
//        aaiClient = new AAIClient(context, appid, projectId, secretId ,new LocalCredentialProvider(secretKey));
//
//        /** 使用临时密钥鉴权
//         *  (1).通过sts 获取到临时证书 （secretId secretKey token） ,此步骤应在您的服务器端实现，见https://cloud.tencent.com/document/product/598/33416
//         *  (2).通过临时密钥调用接口
//         */
//        // aaiClient = new AAIClient(MainActivity.this, appid, projectId,"临时secretId", "临时secretKey","对应的token");
//
//        // 2、初始化语音识别请求。
//        AudioRecognizeRequest.Builder builder = new AudioRecognizeRequest.Builder();
//        final AudioRecognizeRequest audioRecognizeRequest = builder
//                //设置数据源，数据源要求实现PcmAudioDataSource接口，您可以自己实现此接口来定制您的自定义数据源，例如从第三方推流中获
//                .pcmAudioDataSource(new AudioRecordDataSource(false)) // 使用SDK内置录音器作为数据源,false:不保存音频
//                .setEngineModelType("16k_zh") // 设置引擎参数("16k_zh" 通用引擎，支持中文普通话+英文)
//                .setFilterDirty(0)  // 0 ：默认状态 不过滤脏话 1：过滤脏话
//                .setFilterModal(0) // 0 ：默认状态 不过滤语气词  1：过滤部分语气词 2:严格过滤
//                .setFilterPunc(0) // 0 ：默认状态 不过滤句末的句号 1：滤句末的句号
//                .setConvert_num_mode(1) //1：默认状态 根据场景智能转换为阿拉伯数字；0：全部转为中文数字。
//                .setNeedvad(1) //0：关闭 vad，1：默认状态 开启 vad。语音时长超过一分钟需要开启,如果对实时性要求较高,并且时间较短的输入,建议关闭
//                // .setHotWordId("")//热词 id。用于调用对应的热词表，如果在调用语音识别服务时，不进行单独的热词 id 设置，自动生效默认热词；如果进行了单独的热词 id 设置，那么将生效单独设置的热词 id。
//                //.setCustomizationId("")//自学习模型 id。如果设置了该参数，那么将生效对应的自学习模型
//                .build();
//
//        // 3、初始化语音识别结果监听器。
//        final AudioRecognizeResultListener audioRecognizeResultlistener = new AudioRecognizeResultListener() {
//
//            @Override
//            public void onSliceSuccess(AudioRecognizeRequest request, AudioRecognizeResult result, int seq) {
//                //返回分片的识别结果，此为中间态结果，会被持续修正
//            }
//
//            @Override
//            public void onSegmentSuccess(AudioRecognizeRequest request, AudioRecognizeResult result, int seq) {
//                //返回语音流的识别结果，此为稳定态结果，可做为识别结果用与业务
//            }
//
//            @Override
//            public void onSuccess(AudioRecognizeRequest request, String result) {
//                //识别结束回调，返回所有的识别结果
//            }
//
//            @Override
//            public void onFailure(AudioRecognizeRequest request, final ClientException clientException, final ServerException serverException, String response) {
//                // 识别失败
//            }
//        };
//
//        // 4、自定义识别配置
//        final AudioRecognizeConfiguration audioRecognizeConfiguration = new AudioRecognizeConfiguration.Builder()
//                //分片默认40ms，可设置40-5000，如果您不了解此参数不建议更改
//                //.sliceTime(40)
//                // 是否使能静音检测，
//                .setSilentDetectTimeOut(false)
//                // 静音检测超时停止录音可设置>2000ms，setSilentDetectTimeOut为true有效，超过指定时间没有说话将关闭识别；需要大于等于sliceTime，实际时间为sliceTime的倍数，如果小于sliceTime，则按sliceTime的时间为准
//                .audioFlowSilenceTimeOut(5000)
//                // 音量回调时间，需要大于等于sliceTime，实际时间为sliceTime的倍数，如果小于sliceTime，则按sliceTime的时间为准
//                .minVolumeCallbackTime(80)
//                .build();
//
//        AudioRecognizeStateListener audioRecognizeStateListener = new AudioRecognizeStateListener() {
//            @Override
//            public void onStartRecord(AudioRecognizeRequest audioRecognizeRequest) {
//                // 开始录音
//            }
//            @Override
//            public void onStopRecord(AudioRecognizeRequest audioRecognizeRequest) {
//                // 结束录音
//            }
//
//            @Override
//            public void onVoiceVolume(AudioRecognizeRequest audioRecognizeRequest, int i) {
//                // 音量回调
//            }
//
//            @Override
//            public void onVoiceDb(float v) {
//
//            }
//
//            /**
//             * 返回音频流，
//             * 用于返回宿主层做录音缓存业务。
//             * 由于方法跑在sdk线程上，这里多用于文件操作，宿主需要新开一条线程专门用于实现业务逻辑
//             * new AudioRecordDataSource(true) 有效，否则不会回调该函数
//             * @param audioDatas
//             */
//            @Override
//            public void onNextAudioData(final short[] audioDatas, final int readBufferLength){
//            }
//
//            /**
//             * 静音检测超时回调
//             * 注意：此时任务还未中止，仍然会等待最终识别结果
//             */
//            @Override
//            public void onSilentDetectTimeOut(){
//                //触发了静音检测事件
//            }
//        };
//
//        // 5、启动语音识别
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                if (aaiClient!=null) {
//                    aaiClient.startAudioRecognize(audioRecognizeRequest,
//                            audioRecognizeResultlistener,
//                            audioRecognizeStateListener,
//                            audioRecognizeConfiguration);
//                }
//            }
//        }).start();
//        // 6、log组件设置
//        // 将log落盘到本地磁盘，needLogFile字段默认为false，接入调试期间建议设置为true，上线后此接口调用可删除。
//        AAILogger.setNeedLogFile(true, IYAApplication.getInstance());
//        // 设置日志级别，默认为ERROR_LEVEL，接入调试期间建议设置为DEBUG_LEVEL。
//        AAILogger.setLogLevel(AAILogger.DEBUG_LEVEL);
//        // 设置日志监听器，用于监听日志信息。
//        AAILogger.setLoggerListener(new LoggerListener() {
//            @Override
//            public void onLogInfo(String s) {
//
//            }
//        });
//    }
//}
