//package com.cmdc.ai.assist.demo;
//
//import android.Manifest;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.cmdc.ai.assist.AIAssistantManager;
//import com.cmdc.ai.assist.aiModel.MicroRecordStream;
//import com.google.gson.Gson;
//import com.xiaodu.magictool.MagicClientManager;
//import com.xiaodu.magictool.bean.AsrFrameBody;
//import com.xiaodu.magictool.bean.AsrResult;
//import com.xiaodu.magictool.speech.AsrClientBuilder;
//import com.xiaodu.magictool.speech.AsrClientImpl;
//import com.xiaodu.magictool.speech.realtime.AsrListener;
//import com.xiaodu.magictool.speech.realtime.AsrMode;
//import com.xiaodu.magictool.utils.DeviceIdUtils;
//import com.xiaodu.magictool.utils.Md5Util;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//
//import okhttp3.Call;
//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//import okhttp3.ResponseBody;
//
//public class AsrFileActivity extends AppCompatActivity {
//
//    private static final String TAG = "AsrRealTimeActivity";
//
//    private final static int MINI_DEMO_MODE = 100;
//
//    // ============ 以下参数可以修改 =============
//    // 鉴权信息请修改Const类
//    /**
//     * 默认的录音pcm文件，在assets目录下
//     */
//    /*private final static String ASSET_PCM_FILENAME = "16k.pcm";*/
//    private final static String ASSET_PCM_FILENAME = "audio_20250221_164251_362e2f94-04e7-477d-aeea-b25924a62234.pcm";
//
//    /**
//     * 测试使用的的appid、appkey、pid，请修改成自己的
//     */
//    private static final String APPID = "demo_a17e63a0";
//    private static final String APP_KEY = "3d6a57f3a4904b6f874563fa2484fe12";
//    private static final String PID = "202";
//
//    /**
//     * 默认的识别模式，
//     * AsrMode.MODE_FILE_STREAM 完整版本，输入文件流
//     * AsrMode.MODE_REAL_TIME_STREAM 完整版本，输入麦克风流
//     * AsrMode.MODE_SIMULATE_REAL_TIME_STREAM 完整版本，输入文件流模拟实时流
//     */
//    private final static int DEFAULT_MODE = AsrMode.MODE_REAL_TIME_STREAM;
//
//    private InputStream is = null;
//    private Button button;
//    private RecyclerView chatRv;
//    private AsrChatAdapter asrChatAdapter;
//    private TextView asrTv;
//
//    /*protected int mode = AsrMode.MODE_REAL_TIME_STREAM;*/
//
//    /*protected int mode = AsrMode.MODE_FILE_STREAM;*/
//    protected int mode = AsrMode.MODE_REAL_TIME_STREAM;
//
//    private AsrClientImpl asrClient;
//
//    private OkHttpClient httpClient;
//    private Call call;
//    private ChatData currentReplyData = null;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().hide();
//        }
//        setContentView(R.layout.app_realtime_layout);
//        initUi();
//        initPermission();
//        asrClient = (AsrClientImpl) MagicClientManager.getClientBuilder(AsrClientBuilder.class)
//                .builder();
//
//        httpClient = new OkHttpClient.Builder()
//                .connectTimeout(20, TimeUnit.SECONDS)
//                .readTimeout(80, TimeUnit.SECONDS)
//                .writeTimeout(80, TimeUnit.SECONDS)
//                .build();
//
//        MediaPlayerUtils.Companion.getInstance().create();
//    }
//
//    private void initUi() {
//        initButtons();
//        asrTv = findViewById(R.id.txtResult);
//        chatRv = findViewById(R.id.chat_rv);
//        chatRv.setLayoutManager(new LinearLayoutManager(this));
//        asrChatAdapter = new AsrChatAdapter();
//        chatRv.setAdapter(asrChatAdapter);
//    }
//
//    private void initButtons() {
//        findViewById(R.id.open_activity_control).setVisibility(View.GONE);
//        button = findViewById(R.id.asr_control);
//        button.setText("开始识别");
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                try {
//                    if (button.getText().equals("开始识别")) {
//                        button.setText("停止识别");
//                        MediaPlayerUtils.Companion.getInstance().stop();
//                        asrChatAdapter.stopMedia();
//                        TtsMediaPlayer.getInstance().stop();
//                        if (null != call) {
//                            call.cancel();
//                        }
//                        start();
//                    } else {
//                        button.setText("开始识别");
//                        close();
//                        asrClient.endAsr();
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }
//
//    /**
//     * 开始识别
//     *
//     * @throws IOException Assets 文件异常
//     */
//    private void start() throws IOException {
//        Log.d(TAG, "try to start " + mode);
//        if (mode == MINI_DEMO_MODE || mode == AsrMode.MODE_FILE_STREAM
//                || mode == AsrMode.MODE_SIMULATE_REAL_TIME_STREAM) {
//            /*is = getAssets().open(ASSET_PCM_FILENAME);*/
//            /*is = getInputStreamFromFile(AIAssistantManager.Companion.getInstance().aiAssistConfig.getCurrentPcmFilePath());*/
//            // pcm 文件流
//        } else if (mode == AsrMode.MODE_REAL_TIME_STREAM) {
//            is = MicroRecordStream.getInstance();
//            // 麦克风
//        }
//        AsrFrameBody asrFrameBody = new AsrFrameBody();
//        asrFrameBody.data = new AsrFrameBody.Data();
//        asrFrameBody.data.appid = APPID;
//        asrFrameBody.data.pid = PID;
//        asrFrameBody.data.appkey = APP_KEY;
//        asrFrameBody.data.cuid = DeviceIdUtils.getShortDid();
//        asrClient.beginAsr(is, new Gson().toJson(asrFrameBody), mode, new AsrListener() {
//            @Override
//            public void onResult(AsrResult asrResult) {
//                // 异步返回结果，更新UI需要
//                handleAsrResult(asrResult);
//            }
//
//            @Override
//            public void onClose(String msg) {
//                close();
//                button.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        button.setText("开始识别");
//                    }
//                });
//            }
//
//            @Override
//            public void onFail(int code, String msg) {
//                close();
//                button.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        button.setText("开始识别");
//                    }
//                });
//            }
//        });
//
//    }
//
//    /**
//     * 流程：关闭InputStream-> uploader 结束-> websocket 关闭-> activity 里UI及参数重置
//     * 关闭inputStream
//     */
//    private void close() {
//        Log.d(TAG, "close: ");
//        try {
//            if (is != null) {
//                is.close();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void handleAsrResult(final AsrResult result) {
//        Log.d(TAG, "handleAsrResult: " + result);
//        runOnUiThread(() ->
//        {
//            //asrTv.setText(AsrStat.formatResult(result));
//            asrTv.setText(result.getResult());
//            if (result.isFin()) {
//                ChatData sendData = new ChatData(true, result.getResult());
//                asrChatAdapter.addData(sendData);
//                sendChat(result.getResult());
//                scrollToBottom();
//            }
//        });
//
//    }
//
//    private void scrollToBottom() {
//        if (chatRv != null && asrChatAdapter != null) {
//            chatRv.scrollToPosition(asrChatAdapter.getItemCount() - 1);
//        }
//    }
//
//    private void sendChat(String query) {
//        currentReplyData = null;
//        String appId = "gNCcuuUkSB63XXEK8Q1ljjPSBrxAqyPY";
//        String secret = "GDZlYoLrLZ1jQCBq8W7jKS6aD0rxdB74";
//        long time = System.currentTimeMillis();
//        String sign = Md5Util.stringToMD5(appId + secret + time) + ":" + time;
//
//
//        Request request = new Request.Builder()
//                .url("https://openapi-iot.baidu.com/sandbox/inside-rc/chat")
//                .header("Client-Id", appId)
//                .header("Authorization", sign)
//                .post(RequestBody.create(MediaType.parse("text/event-stream;charset=utf-8"),
//                        "{\"qid\":\"" + UUID.randomUUID().toString() + "\",\"cuid\":\"2c03a20b-902\",\"version\":\"1\",\"client_context\":[{\"header\":{\"name\":\"GpsState\",\"namespace\":\"ai.dueros.device_interface.location\"},\"payload\":{\"enable\":true,\"geoCoordinateSystem\":\"bd09ll\",\"latitude\":31.180068,\"longitude\":121.605782}}],\"event\":{\"header\":{\"name\":\"\",\"namespace\":\"\",\"messageId\":\"\",\"dialogRequestId\":\"\"},\"payload\":null},\"stream\":true,\"messages\":[{\"role\":\"user\",\"content\":\"" + query + "\",\"Nlu\":\"\",\"mediaInfo\":{}}]}"))
//                .build();
//        call = httpClient.newCall(request);
//
//        call.enqueue(new okhttp3.Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                Log.i(TAG, "sendQuery result onError e = " + e.getMessage());
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                Log.i(TAG, "sendQuery result response");
//                try {
//                    ResponseBody responseBody = response.body();
//                    boolean isNewData = true;
//                    if (responseBody != null) {
//                        final InputStream inputStream = responseBody.byteStream();
//                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//                        String line;
//                        while ((line = reader.readLine()) != null) {
//                            Log.i(TAG, "line = " + line);
//                            if (!TextUtils.isEmpty(line) && line.startsWith("data:")) {
//                                String data = line.substring(6).trim();
//                                JSONObject result = new JSONObject(data);
//                                String assistantAnswer = result.optString("assistant_answer");
//
//                                runOnUiThread(() ->
//                                {
//                                    if (currentReplyData == null) {
//                                        currentReplyData = new ChatData(false, "");
//                                        asrChatAdapter.addData(currentReplyData);
//                                        scrollToBottom();
//                                    }
//                                    analyzeDirectives(result);
//                                    int isEnd = result.optInt("is_end");
//                                    if (isEnd == 1) {
//                                        currentReplyData.isGenerating = false;
//                                        currentReplyData.text = getAnswer(assistantAnswer);
//                                    }
//                                    asrChatAdapter.notifyItemChanged(asrChatAdapter.getItemCount() - 1);
//                                    scrollToBottom();
//                                });
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }
//
//    private void analyzeDirectives(JSONObject result) {
//        try {
//            JSONArray directives = result.optJSONArray("data");
//            if (directives == null) {
//                return;
//            }
//            String qid = result.optString("qid");
//            for (int i = 0; i < directives.length(); i++) {
//                JSONObject directive = directives.optJSONObject(i);
//                JSONObject header = directive.optJSONObject("header");
//                JSONObject payload = directive.optJSONObject("payload");
//                String name = header.optString("name");
//                if ("RenderProcessing".equals(name)) {
//                    currentReplyData.isGenerating = true;
//                    currentReplyData.percent = payload.optInt("percent");
//                }
//                if ("RenderMultiImageCard".equals(name)) {
//                    // 图片
//                    currentReplyData.percent = 100;
//                    JSONArray images = payload.optJSONArray("images");
//                    if (null == images || images.length() == 0) {
//                        continue;
//                    }
//                    currentReplyData.pic = images.optJSONObject(0).optString("url");
//                } else if ("Play".equals(name)) {
//                    // 音乐
//                    currentReplyData.mediaUrl = payload.optJSONObject("audioItem")
//                            .optJSONObject("stream")
//                            .optString("url");
//                    currentReplyData.albumName = payload.optJSONObject("audioItem").optString("extension");
//                } else if ("RenderStreamCard".equals(name)) {
//                    // 流式
//                    currentReplyData.text = payload.optString("answer");
//                } else if ("Speak".equals(name)) {
//                    // tts
//                    asrChatAdapter.stopMedia();
//                    TtsMediaPlayer.getInstance().speak(qid, payload.optString("url"));
//                }
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "analyzePayload err : " + e);
//        }
//    }
//
//    private String getAnswer(String assistantAnswer) {
//        try {
//            JSONObject obj = new JSONObject(assistantAnswer);
//            if (obj.has("content")) {
//                return obj.optString("content");
//            } else {
//                return assistantAnswer;
//            }
//        } catch (Exception e) {
//            return assistantAnswer;
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        close();
//        if (asrClient != null) {
//            asrClient.release();
//        }
//        TtsMediaPlayer.getInstance().release();
//        MediaPlayerUtils.Companion.getInstance().release();
//    }
//
//
//    /**
//     * android 6.0 以上需要动态申请权限
//     */
//    private void initPermission() {
//        String[] permissions = {
//                Manifest.permission.RECORD_AUDIO,
//                Manifest.permission.ACCESS_NETWORK_STATE,
//                Manifest.permission.INTERNET
//        };
//        ArrayList<String> toApplyList = new ArrayList<>();
//
//        for (String perm : permissions) {
//            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
//                toApplyList.add(perm);
//                // 进入到这里代表没有权限.
//            }
//        }
//        String[] tmpList = new String[toApplyList.size()];
//        if (!toApplyList.isEmpty()) {
//            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        // 此处为android 6.0以上动态授权的回调，用户自行实现。
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }
//
//    private InputStream getInputStreamFromFile(String filePath) {
//        try {
//            File file = new File(filePath);
//            if (file.exists()) {
//                return new FileInputStream(file);
//            } else {
//                throw new IOException("File does not exist: " + filePath);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null; // 或者抛出异常，根据你的需求
//        }
//    }
//}
