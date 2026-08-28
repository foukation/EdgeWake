package com.cmdc.ai.assist.demo.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cmdc.ai.assist.AIAssistantManager;
import com.cmdc.ai.assist.api.ASRIntelligentDialogue;
import com.cmdc.ai.assist.constraint.AsrResult;
import com.cmdc.ai.assist.constraint.DialogueResult;
import com.cmdc.ai.assist.demo.R;
import com.cmdc.ai.assist.demo.adapter.AsrChatAdapter;
import com.cmdc.ai.assist.demo.adapter.ChatData;
import com.cmdc.ai.assist.demo.media.MediaPlayerUtils;
import com.cmdc.ai.assist.demo.media.TtsMediaPlayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import timber.log.Timber;

public class AsrRealTimeActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "AsrRealTimeActivity";

    private final static int MINI_DEMO_MODE = 100;

    // ============ 以下参数可以修改 =============
    // 鉴权信息请修改Const类
    /**
     * 默认的录音pcm文件，在assets目录下
     */
    private final static String ASSET_PCM_FILENAME = "16k.pcm";

    /**
     * 测试使用的的appid、appkey、pid，请修改成自己的
     */
    private static final String APPID = "demo_a17e63a0";
    private static final String APP_KEY = "3d6a57f3a4904b6f874563fa2484fe12";
    private static final String PID = "202";

    /**
     * 默认的识别模式，
     * AsrMode.MODE_FILE_STREAM 完整版本，输入文件流
     * AsrMode.MODE_REAL_TIME_STREAM 完整版本，输入麦克风流
     * AsrMode.MODE_SIMULATE_REAL_TIME_STREAM 完整版本，输入文件流模拟实时流
     */
    /*private final static int DEFAULT_MODE = AsrMode.MODE_REAL_TIME_STREAM;*/

    private InputStream is = null;
    private Button button;
    private RecyclerView chatRv;
    private AsrChatAdapter asrChatAdapter;
    private TextView asrTv;

    /*protected int mode = AsrMode.MODE_REAL_TIME_STREAM;
    private AsrClientImpl asrClient;*/

    private Call call;
    private ChatData currentReplyData = null;
    private ASRIntelligentDialogue realtimeAsr;
    /*private ASRIntelligentConversation asrIntelligentConversation;*/

    // 添加点击相关变量
    private static final int REQUIRED_CLICKS = 3;  // 需要的点击次数
    private static final long CLICK_TIMEOUT = 1000;  // 点击超时时间，单位毫秒
    private int clickCount = 0;  // 点击计数
    private long lastClickTime = 0;  // 上次点击时间

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.app_realtime_layout);
        initUi();
        /*asrClient = (AsrClientImpl) MagicClientManager.getClientBuilder(AsrClientBuilder.class)
                .builder();*/

        MediaPlayerUtils.Companion.getInstance().create();

        // 初始化语音识别类
        /*realtimeAsr = (ASRIntelligentDialogue) AIAssistantManager.Companion.getInstance().asrIntelligentDialogueHelp();*/
        /*asrIntelligentConversation = (ASRIntelligentConversation) AIAssistantManager.Companion.getInstance().asrIntelligentConversationHelp();*/
    }

    private void initUi() {
        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(this);

        initButtons();

        asrTv = findViewById(R.id.txtResult);
        chatRv = findViewById(R.id.chat_rv);
        chatRv.setLayoutManager(new LinearLayoutManager(this));
        asrChatAdapter = new AsrChatAdapter();
        chatRv.setAdapter(asrChatAdapter);

        //initBackgroundClick();
    }

    private void initButtons() {
        button = findViewById(R.id.asr_control);
        button.setText("开始识别");
        button.setOnClickListener(view -> {

            toggleAsrRecognition();

        });
        findViewById(R.id.open_activity_control).setVisibility(View.GONE);
        findViewById(R.id.open_activity_control).setOnClickListener(view -> {

            /*if (TextUtils.isEmpty(AIAssistantManager.Companion.getInstance().aiAssistConfig.getCurrentPcmFilePath())) {
                Toast.makeText(AsrRealTimeActivity.this, "请先开始 ASR 识别", Toast.LENGTH_SHORT).show();
                return;
            }*/

            openAsrFileActivity();

        });
    }

    private void initBackgroundClick() {
        View rootLayout = findViewById(R.id.root_layout);
        // 确保布局可点击
        rootLayout.setClickable(true);
        rootLayout.setFocusable(true);
        rootLayout.setOnClickListener(v -> handleTripleClick());
    }

    /**
     * 处理三次点击事件
     */
    private void handleTripleClick() {
        long currentTime = System.currentTimeMillis();

        // 如果距离上次点击超过timeout，重置计数
        if (currentTime - lastClickTime > CLICK_TIMEOUT) {
            clickCount = 0;
        }

        // 更新点击时间和计数
        lastClickTime = currentTime;
        clickCount++;

        // 达到指定点击次数，打开新界面
        if (clickCount >= REQUIRED_CLICKS) {
            clickCount = 0;  // 重置计数
            openAsrFileActivity();
        }
    }

    /**
     * 打开AsrFileActivity
     */
    private void openAsrFileActivity() {
        /*Intent intent = new Intent(this, AsrFileActivity.class);
        startActivity(intent);
        Timber.tag(TAG).d("Opening AsrFileActivity");*/
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == android.R.id.content) {
            Log.d(TAG, "Root view clicked");
            toggleAsrRecognition();
        }
    }

    /**
     * 切换语音识别状态
     * 开始/停止语音识别，并处理相关资源
     */
    private void toggleAsrRecognition() {
        try {

            if (realtimeAsr != null) {
                realtimeAsr.release();  // 需要添加这个方法
                realtimeAsr = null;
            }

            if (button.getText().equals("开始识别")) {

                button.setText("停止识别");
                MediaPlayerUtils.Companion.getInstance().stop();
                asrChatAdapter.stopMedia();
                TtsMediaPlayer.getInstance().stop();
                if (null != call) {
                    call.cancel();
                }
                start();
            } else {
                button.setText("开始识别");
                close();
                /*asrClient.endAsr();*/
                // 确保 WebSocket 连接关闭

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始识别
     *
     * @throws IOException Assets 文件异常
     */
    private void start() throws IOException {
        /*Log.d(TAG, "try to start " + mode);
        if (mode == MINI_DEMO_MODE || mode == AsrMode.MODE_FILE_STREAM
                || mode == AsrMode.MODE_SIMULATE_REAL_TIME_STREAM) {
            is = getAssets().open(ASSET_PCM_FILENAME);
            // pcm 文件流
        } else if (mode == AsrMode.MODE_REAL_TIME_STREAM) {
            *//*is = MicroRecordStream.getInstance();*//*
            // 麦克风
        }*/

        // 确保 realtimeAsr 被初始化
        if (realtimeAsr == null) {
            realtimeAsr = (ASRIntelligentDialogue) AIAssistantManager.Companion.getInstance().asrIntelligentDialogueHelp();
            /*realtimeAsr = new ASRIntelligentDialogue(MicroRecordStream.getInstance());*/
        }

        // 设置回调
        assert realtimeAsr != null;
        realtimeAsr.setListener(new ASRIntelligentDialogue.RealtimeAsrListener() {

            @Override
            public void onConnected() {
            }

            @Override
            public void onAsrMidResult(@NonNull AsrResult asrResult) {
                handleAsrResult(asrResult.getText(), false);
            }

            @Override
            public void onAsrFinalResult(@NonNull AsrResult asrResult) {
                handleAsrResult(asrResult.getText(), true);
            }

            /*@Override
            public void onDialogueResult(@NonNull JSONObject result) {

                runOnUiThread(() ->
                {
                    String qid = "";
                    if (result.has("qid")) {
                        qid = result.optString("qid");
                    }
                    if (result.has("is_end") && result.optInt("is_end") == 1) {
                        String assistantAnswer = result.optString("assistant_answer_content");
                        currentReplyData.isGenerating = false;
                        currentReplyData.text = assistantAnswer;
                    }

                    if (currentReplyData == null) {
                        currentReplyData = new ChatData(false, "");
                        asrChatAdapter.addData(currentReplyData);
                        scrollToBottom();
                    }

                    if (result.has("renderProcessing_percent")) {
                        int renderProcessing_percent = result.optInt("renderProcessing_percent");
                        currentReplyData.isGenerating = true;
                        currentReplyData.percent = renderProcessing_percent;
                    }

                    if (result.has("nlu")) {
                        JSONArray nlu = result.optJSONArray("nlu");
                    }

                    if (result.has("nluTag")) {
                        JSONObject domain = result.optJSONObject("nluTag");
                    }

                    if (result.has("renderMultiImageCard_url")) {
                        String renderMultiImageCard_url = result.optString("renderMultiImageCard_url");
                        currentReplyData.percent = 100;
                        currentReplyData.pic = renderMultiImageCard_url;
                    }

                    if (result.has("play")) {
                        JSONObject play = result.optJSONObject("play");
                        String mediaUrl = play.optString("mediaUrl");
                        String albumName = play.optString("albumName");
                        currentReplyData.mediaUrl = mediaUrl;
                        currentReplyData.albumName = albumName;
                    }

                    if (result.has("renderStreamCard_answer_content")) {
                        currentReplyData.text = result.optString("renderStreamCard_answer_content");
                    }

                    if (result.has("speak_ttsUrl")) {
                        String speak_ttsUrl = result.optString("speak_ttsUrl");
                        asrChatAdapter.stopMedia();
                        TtsMediaPlayer.getInstance().speak(qid, speak_ttsUrl);
                    }

                    *//*analyzeDirectives(result);*//*
                    asrChatAdapter.notifyItemChanged(asrChatAdapter.getItemCount() - 1);
                    scrollToBottom();
                });

            }*/

            @Override
            public void onDialogueResult(@NonNull DialogueResult result) {

                runOnUiThread(() ->
                {

                    Timber.tag(TAG).d("is_end ：%s", result.is_end() + "  " + result.getQid());
                    Timber.tag(TAG).d("is_end ：%s", result.is_end() + "  " + result.getAssistant_answer_content());

                    String qid = result.getQid();
                    int is_end = result.is_end();
                    String assistant_answer_content = result.getAssistant_answer_content();
                    JSONObject header = result.getHeader();
                    JSONObject payload = result.getPayload();

                    if (currentReplyData == null) {
                        currentReplyData = new ChatData(false, "");
                        asrChatAdapter.addData(currentReplyData);
                        scrollToBottom();
                    }

                    if (is_end == 1) {
                        currentReplyData.isGenerating = false;
                        currentReplyData.text = assistant_answer_content;
                    }

                    if (header == null) {
                        asrChatAdapter.notifyItemChanged(asrChatAdapter.getItemCount() - 1);
                        scrollToBottom();
                        return;
                    }

                    try {
                        String name = header.optString("name");
                        if ("RenderProcessing".equals(name)) {
                            currentReplyData.isGenerating = true;
                            currentReplyData.percent = payload.optInt("percent");
                        }
                        if ("Nlu".equals(name)) {
                            // nlu
                            JSONArray nlu = payload.optJSONArray("nlu");
                            Timber.tag(TAG).d("意图 ：%s", nlu != null ? nlu.toString() : null);
                        } else if ("NluTag".equals(name)) {
                            // nlu
                            String domian = payload.optString("domian");
                            String intent = payload.optString("intent");
                        } else if ("RenderMultiImageCard".equals(name)) {
                            // 图片
                            currentReplyData.percent = 100;
                            JSONArray images = payload.optJSONArray("images");
                            currentReplyData.pic = images.optJSONObject(0).optString("url");
                        } else if ("Play".equals(name)) {
                            // 音乐
                            currentReplyData.mediaUrl = payload.optJSONObject("audioItem")
                                    .optJSONObject("stream")
                                    .optString("url");
                            currentReplyData.albumName = payload.optJSONObject("audioItem").optString("extension");
                        } else if ("RenderStreamCard".equals(name)) {
                            // 流式
                            currentReplyData.text = payload.optString("answer");
                        } else if ("Speak".equals(name)) {
                            // tts
                            asrChatAdapter.stopMedia();
                            TtsMediaPlayer.getInstance().speak(qid, payload.optString("url"));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "analyzePayload err : " + e);
                    }
                    asrChatAdapter.notifyItemChanged(asrChatAdapter.getItemCount() - 1);
                    scrollToBottom();
                });
            }

            @Override
            public void onError(int code, @NonNull String message) {
                Timber.tag(TAG).e("ASRIntelligentDialogue Error - Code: %d, Message: %s", code, message);
                close();
                button.post(() -> button.setText("开始识别"));
            }

            @Override
            public void onComplete() {
                Timber.tag(TAG).d("onComplete");
                close();
                button.post(() -> button.setText("开始识别"));
            }
        });

        realtimeAsr.startRecognition(this.getBaseContext());

    }

    /**
     * 流程：关闭InputStream-> uploader 结束-> websocket 关闭-> activity 里UI及参数重置
     * 关闭inputStream
     */
    private void close() {
        Log.d(TAG, "close: ");
        try {
            if (is != null) {
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAsrResult(final String result, final boolean isfin) {
        Log.d(TAG, "handleAsrResult: " + result);
        runOnUiThread(() ->
        {
            //asrTv.setText(AsrStat.formatResult(result));
            asrTv.setText(result);
            if (isfin) {
                ChatData sendData = new ChatData(true, result);
                asrChatAdapter.addData(sendData);
                scrollToBottom();
                currentReplyData = null;
                button.setText("开始识别");
            }
        });

    }

    private void scrollToBottom() {
        if (chatRv != null && asrChatAdapter != null) {
            chatRv.scrollToPosition(asrChatAdapter.getItemCount() - 1);
        }
    }

    private void analyzeDirectives(JSONObject result) {
        try {
            JSONArray directives = result.optJSONArray("data");
            if (directives == null) {
                return;
            }
            String qid = result.optString("qid");
            for (int i = 0; i < directives.length(); i++) {
                JSONObject directive = directives.optJSONObject(i);
                JSONObject header = directive.optJSONObject("header");
                JSONObject payload = directive.optJSONObject("payload");
                String name = header.optString("name");
                if ("RenderProcessing".equals(name)) {
                    currentReplyData.isGenerating = true;
                    currentReplyData.percent = payload.optInt("percent");
                }
                if ("Nlu".equals(name)) {
                    // nlu
                    JSONArray nlu = payload.optJSONArray("nlu");
                    Timber.tag(TAG).d("意图 ：%s", nlu != null ? nlu.toString() : null);
                } else if ("NluTag".equals(name)) {
                    // nlu
                    String domian = payload.optString("domian");
                    String intent = payload.optString("intent");
                } else if ("RenderMultiImageCard".equals(name)) {
                    // 图片
                    currentReplyData.percent = 100;
                    JSONArray images = payload.optJSONArray("images");
                    if (null == images || images.length() == 0) {
                        continue;
                    }
                    currentReplyData.pic = images.optJSONObject(0).optString("url");
                } else if ("Play".equals(name)) {
                    // 音乐
                    currentReplyData.mediaUrl = payload.optJSONObject("audioItem")
                            .optJSONObject("stream")
                            .optString("url");
                    currentReplyData.albumName = payload.optJSONObject("audioItem").optString("extension");
                } else if ("RenderStreamCard".equals(name)) {
                    // 流式
                    currentReplyData.text = payload.optString("answer");
                } else if ("Speak".equals(name)) {
                    // tts
                    asrChatAdapter.stopMedia();
                    TtsMediaPlayer.getInstance().speak(qid, payload.optString("url"));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "analyzePayload err : " + e);
        }
    }

    private String getAnswer(String assistantAnswer) {
        try {
            JSONObject obj = new JSONObject(assistantAnswer);
            if (obj.has("content")) {
                return obj.optString("content");
            } else {
                return assistantAnswer;
            }
        } catch (Exception e) {
            return assistantAnswer;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
        /*if (asrClient != null) {
            asrClient.release();
        }*/
        TtsMediaPlayer.getInstance().release();
        MediaPlayerUtils.Companion.getInstance().release();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (realtimeAsr != null) {
            realtimeAsr.release();  // 需要添加这个方法
            realtimeAsr = null;
            button.setText("开始识别");
        }
    }
}
