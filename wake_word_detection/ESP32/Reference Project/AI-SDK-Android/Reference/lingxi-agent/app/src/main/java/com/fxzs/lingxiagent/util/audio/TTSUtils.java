package com.fxzs.lingxiagent.util.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.google.gson.Gson;
import com.tencent.cloud.stream.tts.FlowingSpeechSynthesizer;
import com.tencent.cloud.stream.tts.FlowingSpeechSynthesizerListener;
import com.tencent.cloud.stream.tts.FlowingSpeechSynthesizerRequest;
import com.tencent.cloud.stream.tts.SpeechSynthesizerResponse;
import com.tencent.cloud.stream.tts.core.utils.AAILogger;
import com.tencent.cloud.stream.tts.core.utils.ByteUtils;
import com.tencent.cloud.stream.tts.core.utils.Ttsutils;
import com.tencent.cloud.stream.tts.core.ws.Credential;
import com.tencent.cloud.stream.tts.core.ws.SpeechClient;

import java.nio.ByteBuffer;

class TTSUtils {

    private static final TTSUtils instance = new TTSUtils();


    public static TTSUtils getInstance() {
        return instance;
    }

    public static float volume = 0;//音量大小
    public static float speed = 1.0f;//智小柔
    public static int SAMPLE_RATE = 16000;//音频采样率
    public static int voiceType = 502001;//智小柔

    private AudioTrack audioTrack;
    private FlowingSpeechSynthesizer synthesizer = null;
    private final SpeechClient proxy = new SpeechClient();


    static String appId = "1330351148";
    static String secretId = "AKIDhaP8lPrMfleMsP4oRuDJCrMsHWc0GLLq";
    static String secretKey = "XqvoUwJYjldJ4e3FE0Zu3ziEYgbJn12q";


    static String TAG = "TTSUtils";
    static String Uid = "";
    static Context context;
    int currentId = 0;

    boolean isPlayPcmData = false;

    public void init(Context context) {
        getPlayer();
        context = context;
        Uid = SharedPreferencesUtil.getUserId() + "";
        FlowingSpeechSynthesizerRequest request = new FlowingSpeechSynthesizerRequest();
/************** 配置项含义也可参考官网文档: https://cloud.tencent.com/document/product/1073/108595 **************/
        request.setVolume(volume); // 音量大小，范围[-10，10]，对应音量大小。默认为0，代表正常音量，值越大音量越高。
        request.setSpeed(speed); // 语速，范围：[-2，6]，分别对应不同语速：-2: 代表0.6倍; -1: 代表0.8倍; 0: 代表1.0倍（默认）; 1: 代表1.2倍; 2: 代表1.5倍; 6: 代表2.5倍
        request.setCodec("pcm"); // 返回音频格式：pcm: 返回二进制pcm音频（默认）; mp3: 返回二进制mp3音频
//        request.setSampleRate(SAMPLE_RATE); // 音频采样率：24000: 24k(部分音色支持); 16000: 16k(默认); 8000: 8k
        request.setVoiceType(voiceType); // 音色ID
        request.setEnableSubtitle(true); // 是否开启时间戳功能，默认为false。
        request.setEmotionCategory("neutral");// 控制合成音频的情感，仅支持多情感音色使用
        request.setEmotionIntensity(100); // 控制合成音频情感程度，取值范围为 [50,200]，默认为 100; 只有 EmotionCategory 不为空时生效。
        request.setSessionId(Uid);//sessionId，需要保持全局唯一（推荐使用 uuid），遇到问题需要提供该值方便服务端排查
/************** 配置项含义也可参考官网文档: https://cloud.tencent.com/document/product/1073/108595 **************/

    }


    public void process() {
//        String appId = "";
//        String secretId = "";
//        String secretKey = "";
        String token = "";
        process(appId, secretId, secretKey, token);
    }

    Credential credential;
    FlowingSpeechSynthesizerRequest request;
    FlowingSpeechSynthesizerListener listener;

    public void process(String appId, String secretId, String secretKey, String token) {
//        resetMsg();
        if (audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
            audioTrack.play();
        }
        isPlayPcmData = true;
        credential = new Credential(appId, secretId, secretKey, token);
        request = new FlowingSpeechSynthesizerRequest();
        /************** 配置项含义也可参考官网文档: https://cloud.tencent.com/document/product/1073/108595 **************/
        request.setVolume(volume); // 音量大小，范围[-10，10]，对应音量大小。默认为0，代表正常音量，值越大音量越高。
        request.setSpeed(speed); // 语速，范围：[-2，6]，分别对应不同语速：-2: 代表0.6倍; -1: 代表0.8倍; 0: 代表1.0倍（默认）; 1: 代表1.2倍; 2: 代表1.5倍; 6: 代表2.5倍
        request.setCodec("pcm"); // 返回音频格式：pcm: 返回二进制pcm音频（默认）; mp3: 返回二进制mp3音频
        request.setSampleRate(SAMPLE_RATE); // 音频采样率：24000: 24k(部分音色支持); 16000: 16k(默认); 8000: 8k
        request.setVoiceType(voiceType); // 音色ID
        request.setEnableSubtitle(true); // 是否开启时间戳功能，默认为false。
        request.setEmotionCategory("neutral");// 控制合成音频的情感，仅支持多情感音色使用
        request.setEmotionIntensity(100); // 控制合成音频情感程度，取值范围为 [50,200]，默认为 100; 只有 EmotionCategory 不为空时生效。
        request.setSessionId(Uid);//sessionId，需要保持全局唯一（推荐使用 uuid），遇到问题需要提供该值方便服务端排查
        /************** 配置项含义也可参考官网文档: https://cloud.tencent.com/document/product/1073/108595 **************/

        AAILogger.d(TAG, "session_id: " + request.getSessionId());
        listener = new FlowingSpeechSynthesizerListener() {//tips：回调方法中应该避免进行耗时操作，如果有耗时操作建议进行异步处理否则会影响websocket请求处理
            byte[] audio = new byte[0];

            @Override
            public void onSynthesisStart(SpeechSynthesizerResponse response) {
                String msg = String.format("%s session_id:%s, %s", "onSynthesisStart", response.getSessionId(), new Gson().toJson(response));
                AAILogger.d(TAG, msg);
//                updateMsg(msg);
            }

            @Override
            public void onSynthesisEnd(SpeechSynthesizerResponse response) {
                String msg = String.format("%s session_id:%s, %s", "onSynthesisEnd", response.getSessionId(), new Gson().toJson(response));
                AAILogger.d(TAG, msg);
//                updateMsg(msg);

                ZUtils.print("onSynthesisEnd = " + request.getCodec());
                if ("pcm".equals(request.getCodec())) {
                    // 保存文件
                    String audioFilePath = Ttsutils.responsePcm2Wav(context, 16000, audio, request.getSessionId());
                    AAILogger.d(TAG, "audio file path: " + audioFilePath);
                    // 停止播放
                    audioTrack.stop();
                }
                if ("mp3".equals(request.getCodec())) {
                    // TODO 自行播放 或 保存文件
                }
            }

            @Override
            public void onAudioResult(ByteBuffer buffer) {
                if (isPlayPcmData) {
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    // 播放pcm
                    audioTrack.write(data, 0, data.length);
                    audio = ByteUtils.concat(audio, data);
                }
            }

            @Override
            public void onTextResult(SpeechSynthesizerResponse response) {
                AAILogger.d(TAG, String.format("%s session_id:%s, %s", "onTextResult", response.getSessionId(), new Gson().toJson(response)));
            }

            @Override
            public void onSynthesisCancel() {
//                updateMsg("onSynthesisCancel");
                ZUtils.print("onSynthesisCancel == ");
            }

            /**
             * 错误回调 当发生错误时回调该方法
             * @param response 响应
             */
            @Override
            public void onSynthesisFail(SpeechSynthesizerResponse response) {
                String msg = String.format("%s session_id:%s, %s", "onSynthesisFail", response.getSessionId(), new Gson().toJson(response));
                AAILogger.d(TAG, msg);
//                updateMsg(msg);
            }
        };

    }

    public void ttsCancel() {
        try {
            // 会直接断开websocket链接
            synthesizer.cancel();
            AAILogger.d(TAG, "synthesizer cancel latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
        } catch (Exception e) {
            String msg = "synthesizer exception: " + e;
            AAILogger.e(TAG, msg);
            //                    updateMsg(msg);
        }

    }

    public void ttsStop() {
        try {
            synthesizer.stop();
            AAILogger.d(TAG, "synthesizer stop latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
        } catch (Exception e) {
            String msg = "synthesizer exception: " + e;
            AAILogger.e(TAG, msg);
            //                    updateMsg(msg);
        }

    }

    public static int TTS_TEXT_ID_AUTO = -1;
    String tempText = "";

    public void ttsText(String text, boolean isStreamEnd) {
        ZUtils.print("ttsText == currentId " + currentId);
        tempText += text;
        ZUtils.print("ttsText ==  tempText = " + tempText);
        if (tempText.length() > 20 || isStreamEnd) {
            ttsText(tempText);
            tempText = "";
        }
//        if(id> 0 &&  currentId > TTS_TEXT_ID_AUTO ){//优先播放 手动点击
//            ttsText(text);
//            currentId = id;
//        }else if(currentId == 0 && id == TTS_TEXT_ID_AUTO ||  currentId == TTS_TEXT_ID_AUTO ){//进入界面自动播放||自动连播
//            ttsText(text);
//            currentId = id;
//        }else {
////            stopPlayer();
////            process();
////            ttsStart();
////            ttsText(text);
//        }

    }

    public void ttsText(String text) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//
//                    AAILogger.d(TAG, "synthesizer ttsText at : " + System.currentTimeMillis() + " ms");
//                    synthesizer.process(text);
//                    Thread.sleep(500);
//                } catch (Exception e) {
//                    String msg = "synthesizer exception: " + e;
//                    AAILogger.e(TAG, msg);
//                    //                    updateMsg(msg);
//                }
//            }
//        }).start();


    }

    long currentTimeMillis;

    public void ttsInit() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    synthesizer = new FlowingSpeechSynthesizer(proxy, credential, request, listener);
                    //synthesizer不可重复使用，每次合成需要重新生成新对象
                    long currentTimeMillis = System.currentTimeMillis();
                    synthesizer.start();
                    AAILogger.d(TAG, "synthesizer ttsInit : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
                } catch (Exception e) {
                    String msg = "synthesizer exception: " + e;
                    AAILogger.e(TAG, msg);
                    //                    updateMsg(msg);
                }
            }
        }).start();


    }

    public void ttsStart() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    synthesizer.start();
                    AAILogger.d(TAG, "synthesizer start latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
                } catch (Exception e) {
                    String msg = "synthesizer exception: " + e;
                    AAILogger.e(TAG, msg);
                    //                    updateMsg(msg);
                }
            }
        }).start();

    }


    public void ttsStartAndPlay() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    synthesizer = new FlowingSpeechSynthesizer(proxy, credential, request, listener);
                    //synthesizer不可重复使用，每次合成需要重新生成新对象
                    long currentTimeMillis = System.currentTimeMillis();
                    synthesizer.start();
                    AAILogger.d(TAG, "synthesizer start latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
                } catch (Exception e) {
                    String msg = "synthesizer exception: " + e;
                    AAILogger.e(TAG, msg);
                    //                    updateMsg(msg);
                }
            }
        }).start();

    }


    public void startPlayer() {
        ZUtils.print("startPlayer =  audioTrack.getState() " + audioTrack.getState());
        if (audioTrack != null  /*&& audioTrack.getState() == AudioTrack.PLAYSTATE_PAUSED*/) {
            audioTrack.play();
            isPlayPcmData = true;
        }
    }

    public void stopPlayer() {
        if (audioTrack != null && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
            audioTrack.stop();
            audioTrack.release();
        }
    }

    public void pausePlayer() {
        ZUtils.print("pausePlayer =  audioTrack.getState() " + audioTrack.getState());
        if (audioTrack != null && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
            audioTrack.pause();
        }
    }

    public void getPlayer() {

        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);
    }

    public void click2PlayTts(String msg) {
        TTSUtils.getInstance().pausePlayer();
        TTSUtils.getInstance().ttsCancel();
        TTSUtils.getInstance().process();
        TTSUtils.getInstance().ttsInit();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        TTSUtils.getInstance().ttsText(msg, true);
        TTSUtils.getInstance().startPlayer();
    }

    public void cancelAndPlay() {
//        TTSUtils.getInstance().pausePlayer();
//        TTSUtils.getInstance().ttsCancel();
//        TTSUtils.getInstance().process();
//        TTSUtils.getInstance().ttsInit();
//
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        TTSUtils.getInstance().startPlayer();
    }
}
