package com.fxzs.lingxiagent.view.common;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.audio.OnPlayerListener;
import com.fxzs.lingxiagent.util.audio.TTSManager;

import java.util.Locale;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.image.glide.GlideImagesPlugin;
import timber.log.Timber;

public class AIResponseView extends LinearLayout {
    private static final String TAG = "AIResponseView";
    
    private LinearLayout llThinkingSection;
    private LinearLayout llThinkingHeader;
    private TextView tvThinkingProcess;
    private ImageView ivArrow;
    private TextView tvThinkingText;
    private TextView tvTitle;
    private TextView tvContent;
    private ImageButton btnCopy;
    private ImageButton btnSpeak;
    private ImageButton btnRefresh;
    private View divider;

    private TextToSpeech textToSpeech;
    private OnRefreshClickListener onRefreshClickListener;
    
    // Markdown support
    private Markwon markwon;
    private boolean isMarkdownEnabled = true;
    
    // Stream mode support
    private Handler streamHandler;
    private StringBuilder streamBuffer;
    private boolean isStreamMode = false;
    private static final int STREAM_UPDATE_DELAY = 50; // 50ms update interval
//    private final Map<RecyclerView.ViewHolder, Boolean> ttsStateMap = new WeakHashMap<>();
    boolean isCurrentlyPlaying = false;
    public interface OnRefreshClickListener {
        void onRefreshClick();
    }

    public AIResponseView(Context context) {
        super(context);
        init(context);
    }

    public AIResponseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AIResponseView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        Timber.tag(TAG).d( "AIResponseView.init() 开始");
        LayoutInflater.from(context).inflate(R.layout.view_ai_response, this, true);
        
        llThinkingSection = findViewById(R.id.ll_thinking_section);
        llThinkingHeader = findViewById(R.id.ll_thinking_header);
        tvThinkingProcess = findViewById(R.id.tv_thinking_process);
        ivArrow = findViewById(R.id.iv_arrow);
        tvThinkingText = findViewById(R.id.tv_thinking_text);
        tvTitle = findViewById(R.id.tv_title);
        tvContent = findViewById(R.id.tv_content);
        btnCopy = findViewById(R.id.btn_copy);
        btnSpeak = findViewById(R.id.btn_speak);
        btnRefresh = findViewById(R.id.btn_refresh);
        divider = findViewById(R.id.divider);

        Timber.tag(TAG).d( "视图绑定结果:");
        Timber.tag(TAG).d( "  - llThinkingSection: " + (llThinkingSection != null ? "找到" : "未找到"));
        Timber.tag(TAG).d( "  - llThinkingHeader: " + (llThinkingHeader != null ? "找到" : "未找到"));
        Timber.tag(TAG).d( "  - tvThinkingProcess: " + (tvThinkingProcess != null ? "找到" : "未找到"));
        Timber.tag(TAG).d( "  - ivArrow: " + (ivArrow != null ? "找到" : "未找到"));
        Timber.tag(TAG).d( "  - tvThinkingText: " + (tvThinkingText != null ? "找到" : "未找到"));
        
        setupListeners();
        initTextToSpeech();
        initMarkdown(context);
        initStreamMode();
        Timber.tag(TAG).d( "AIResponseView.init() 完成");
    }

    private void setupListeners() {
        btnCopy.setOnClickListener(v -> copyToClipboard());
        btnSpeak.setOnClickListener(v -> handleTTSPlayClick());
        btnRefresh.setOnClickListener(v -> {
            if (onRefreshClickListener != null) {
                onRefreshClickListener.onRefreshClick();
            }
        });
        
        // Add click listener for thinking header to toggle expand/collapse
        llThinkingHeader.setOnClickListener(v -> toggleThinkingText());
    }

    private void initTextToSpeech() {
        // 保留原生TTS作为备用，但优先使用TTSManager
        textToSpeech = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.CHINESE);
            }
        });
    }
    
    private void initMarkdown(Context context) {
        // 调试日志
        // android.util.Timber.tag(TAG).d( "初始化Markwon配置");
        
        markwon = Markwon.builder(context)
                .usePlugin(ImagesPlugin.create())
//                .usePlugin(MarkwonInlineParserPlugin.create())
//                .usePlugin(JLatexMathPlugin.create(30, builder -> {
//                    builder.inlinesEnabled(true);
//                    builder.errorHandler((latex, error) -> {
//                        Log.d("==================", latex + ",,,," + error.getMessage());
//                        return null;
//                    });
//
//                    builder.executorService(Executors.newCachedThreadPool());
//                }))
                .usePlugin(GlideImagesPlugin.create(context))
                .usePlugin(TablePlugin.create(context))
                // 自定义插件：将单个换行符转换为硬换行
                .usePlugin(new io.noties.markwon.AbstractMarkwonPlugin() {
                    @Override
                    public String processMarkdown(@NonNull String markdown) {
                        // android.util.Timber.tag(TAG).d( "处理Markdown前: " + markdown.substring(0, Math.min(100, markdown.length())).replace("\n", "\\n"));
                        // 在每个单独的换行符后添加两个空格，使其成为硬换行
                        // 但要避免处理已经是双换行的情况
                        String processed = markdown
                            .replaceAll("(?<!\n)\n(?!\n)", "  \n");
                        // android.util.Timber.tag(TAG).d( "处理Markdown后: " + processed.substring(0, Math.min(100, processed.length())).replace("\n", "\\n"));
                        return processed;
                    }
                })
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configure(Registry registry) {
                        // 自定义 CorePlugin 的 SpanFactory，设置字体大小
                        registry.require(CorePlugin.class, corePlugin -> {
                            corePlugin.addOnTextAddedListener((markdown, text, start) -> {
                                // 设置字体大小（例如 20sp）
                                markdown.setSpans(start,new AbsoluteSizeSpan(15, true));
//                                markdown.setSpan(new AbsoluteSizeSpan(20, true), start, start + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            });
                        });
                    }
                })
                .usePlugin(TablePlugin.create(context))
                .build();
    }
    
    private void initStreamMode() {
        streamHandler = new Handler(Looper.getMainLooper());
        streamBuffer = new StringBuilder();
    }

    private void copyToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        String textToCopy;
        
        // If in stream mode, use the stream buffer content
        if (isStreamMode) {
            textToCopy = streamBuffer.toString();
        } else {
            // For markdown content, we need to get the raw text
            textToCopy = tvContent.getText().toString();
        }
        
        ClipData clip = ClipData.newPlainText("AI Response", textToCopy);
        clipboard.setPrimaryClip(clip);
        GlobalToast.show((Activity) getContext(), "已复制到剪贴板", GlobalToast.Type.SUCCESS );
    }

    private void speakContent() {
        String text = tvContent.getText().toString();
        if (!text.isEmpty()) {
            // 优先使用项目统一的TTSManager
            try {
                TTSManager.Companion.getInstance().textToAudio(text);
                Timber.tag(TAG).d( "使用TTSManager播放语音: " + text.substring(0, Math.min(50, text.length())));
            } catch (Exception e) {
                Timber.tag(TAG).w( "TTSManager播放失败，使用原生TTS作为备用"+e);
                // 如果TTSManager失败，使用原生TTS作为备用
                if (textToSpeech != null) {
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        }
    }
    
    private void toggleThinkingText() {
        if (tvThinkingText.getVisibility() == VISIBLE) {
            // Collapse
            Timber.tag(TAG).d( "收起思维链");
            tvThinkingText.setVisibility(GONE);
            ivArrow.animate().rotation(0).setDuration(200).start();
        } else {
            // Expand
            Timber.tag(TAG).d( "展开思维链");
            tvThinkingText.setVisibility(VISIBLE);
            ivArrow.animate().rotation(180).setDuration(200).start();
        }
    }

    // Public methods
    public void setContent(String content) {
        // Timber.tag("AIResponseView").d( "setContent被调用 - 内容长度: " + (content != null ? content.length() : 0));
        // if (content != null) {
        //     Timber.tag("AIResponseView").d( "内容包含换行符: " + content.contains("\n"));
        //     String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
        //     Timber.tag("AIResponseView").d( "内容预览: " + preview.replace("\n", "\\n"));
        //     
        //     // 检查是否包含XML标签
        //     Timber.tag("AIResponseView").d( "内容包含<标签>: " + content.contains("<") + ", 包含</标签>: " + content.contains(">"));
        //     
        //     // 打印前200个字符的详细内容，显示每个字符
        //     if (content.length() > 0) {
        //         StringBuilder charDebug = new StringBuilder();
        //         int debugLen = Math.min(content.length(), 200);
        //         for (int i = 0; i < debugLen; i++) {
        //             char c = content.charAt(i);
        //             if (c == '\n') {
        //                 charDebug.append("[\\n]");
        //             } else if (c == '\r') {
        //                 charDebug.append("[\\r]");
        //             } else if (c == ' ') {
        //                 charDebug.append("[空格]");
        //             } else {
        //                 charDebug.append(c);
        //             }
        //         }
        //         Timber.tag("AIResponseView").d( "字符调试: " + charDebug.toString());
        //     }
        // }
        // Timber.tag("AIResponseView").d( "Markdown启用: " + isMarkdownEnabled + ", markwon对象: " + (markwon != null));
        
        if (isMarkdownEnabled && markwon != null) {
            // Timber.tag("AIResponseView").d( "使用Markwon渲染内容");
            markwon.setMarkdown(tvContent, content);
            
            // // 渲染后检查TextView的实际内容
            // Timber.tag("AIResponseView").d( "渲染后TextView文本长度: " + tvContent.getText().length());
            // Timber.tag("AIResponseView").d( "渲染后TextView内容类型: " + tvContent.getText().getClass().getSimpleName());
            // 
            // // 检查TextView的布局参数
            // Timber.tag("AIResponseView").d( "TextView宽度: " + tvContent.getWidth() + ", 高度: " + tvContent.getHeight());
            // Timber.tag("AIResponseView").d( "TextView行数: " + tvContent.getLineCount());
            // Timber.tag("AIResponseView").d( "TextView最大行数: " + tvContent.getMaxLines());
            // 
            // // 调试：检查渲染后的实际文本内容
            // String renderedText = tvContent.getText().toString();
            // if (renderedText.length() > 0) {
            //     // 检查前100个字符，看换行符是否还存在
            //     String preview = renderedText.length() > 100 ? renderedText.substring(0, 100) + "..." : renderedText;
            //     Timber.tag("AIResponseView").d( "渲染后文本预览: " + preview.replace("\n", "\\n"));
            //     
            //     // 检查第一行的内容
            //     if (tvContent.getLineCount() > 0) {
            //         int lineEnd = tvContent.getLayout().getLineEnd(0);
            //         String firstLine = renderedText.substring(0, Math.min(lineEnd, renderedText.length()));
            //         Timber.tag("AIResponseView").d( "第一行内容: " + firstLine);
            //         Timber.tag("AIResponseView").d( "第一行长度: " + firstLine.length());
            //     }
            // }
        } else {
            // Timber.tag("AIResponseView").d( "使用setText设置纯文本");
            tvContent.setText(content);
        }
    }

    public void setTitle(String title) {
        if (title != null && !title.isEmpty()) {
            tvTitle.setText(title);
            tvTitle.setVisibility(VISIBLE);
        } else {
            tvTitle.setVisibility(GONE);
        }
    }

    public void showThinkingProcess(int seconds) {
        Timber.tag(TAG).d( "showThinkingProcess() 被调用，秒数: " + seconds);
        Timber.tag(TAG).d( "  - llThinkingSection 当前可见性: " + (llThinkingSection.getVisibility() == VISIBLE ? "VISIBLE" : "GONE"));
        llThinkingSection.setVisibility(VISIBLE);
        tvThinkingProcess.setText("思考过程 (用时 " + seconds + " 秒)");
        Timber.tag(TAG).d( "  - llThinkingSection 设置后可见性: " + (llThinkingSection.getVisibility() == VISIBLE ? "VISIBLE" : "GONE"));
        Timber.tag(TAG).d( "  - tvThinkingProcess 文本: " + tvThinkingProcess.getText());
    }
    
    public void setThinkingText(String thinkingText) {
        Timber.tag(TAG).d( "setThinkingText() 被调用");
        Timber.tag(TAG).d( "  - thinkingText 长度: " + (thinkingText != null ? thinkingText.length() : "null"));
        if (thinkingText != null && !thinkingText.isEmpty()) {
            // Support markdown in thinking text too
            if (isMarkdownEnabled && markwon != null) {
                markwon.setMarkdown(tvThinkingText, thinkingText);
            } else {
                tvThinkingText.setText(thinkingText);
            }
            // Make thinking header clickable
            llThinkingHeader.setClickable(true);
            // 设置初始状态为收起
            tvThinkingText.setVisibility(GONE);
            ivArrow.setRotation(0);
            Timber.tag(TAG).d( "  - 设置思维文本成功，初始状态为收起");
        } else {
            tvThinkingText.setText("");
            tvThinkingText.setVisibility(GONE);
            llThinkingHeader.setClickable(false);
            Timber.tag(TAG).d( "  - 思维文本为空，禁用点击");
        }
    }

    public void hideThinkingProcess() {
        llThinkingSection.setVisibility(GONE);
    }

    public void showCopyButton(boolean show) {
        btnCopy.setVisibility(show ? VISIBLE : GONE);
    }

    public void showSpeakButton(boolean show) {
        btnSpeak.setVisibility(show ? VISIBLE : GONE);
    }

    public void showDivider(boolean show) {
        divider.setVisibility(show ? VISIBLE : GONE);
    }

    public void setOnRefreshClickListener(OnRefreshClickListener listener) {
        this.onRefreshClickListener = listener;
    }

    public void release() {
        // 停止TTSManager播放
        try {
            TTSManager.Companion.getInstance().stop();
        } catch (Exception e) {
            Timber.tag(TAG).w( "停止TTSManager失败"+ e);
        }
        
        // 停止原生TTS
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        
        if (streamHandler != null) {
            streamHandler.removeCallbacksAndMessages(null);
        }
    }

    // Response types based on Figma design
    public void setAsThinkingResponse(String content, int thinkingTime) {
        showThinkingProcess(thinkingTime);
        setContent(content);
        showCopyButton(true);
        showSpeakButton(true);
    }
    
    public void setAsThinkingResponse(String content, int thinkingTime, String thinkingText) {
        Timber.tag(TAG).d( "setAsThinkingResponse() 被调用 (带思维文本)");
        Timber.tag(TAG).d( "  - content: " + content.substring(0, Math.min(50, content.length())) + "...");
        Timber.tag(TAG).d( "  - thinkingTime: " + thinkingTime);
        Timber.tag(TAG).d( "  - thinkingText: " + (thinkingText != null ? thinkingText.substring(0, Math.min(50, thinkingText.length())) + "..." : "null"));
        showThinkingProcess(thinkingTime);
        setThinkingText(thinkingText);
        setContent(content);
        showCopyButton(true);
        showSpeakButton(true);
        Timber.tag(TAG).d( "setAsThinkingResponse() 执行完成");
    }

    public void setAsTopicResponse(String title, String content) {
        setTitle(title);
        setContent(content);
        showCopyButton(true);
        showSpeakButton(true);
    }

    public void setAsMeetingSummary(String content, String todos) {
        setTitle("会议摘要");
        String fullContent = content + "\n\n会议待办\n" + todos;
        setContent(fullContent);
        showCopyButton(true);
        showSpeakButton(true);
    }

    public void setAsSimpleResponse(String title, String content) {
        setTitle(title);
        setContent(content);
        showCopyButton(true);
        showSpeakButton(true);
    }
    
    // Markdown control methods
    public void setMarkdownEnabled(boolean enabled) {
        this.isMarkdownEnabled = enabled;
    }
    
    public boolean isMarkdownEnabled() {
        return isMarkdownEnabled;
    }
    
    // Stream mode methods
    public void startStreamMode() {
        isStreamMode = true;
        streamBuffer.setLength(0);
        tvContent.setText("");
        Timber.tag(TAG).d( "Stream mode started");
    }
    
    public void appendStreamContent(String chunk) {
        if (!isStreamMode) {
            Timber.tag(TAG).w("appendStreamContent called but stream mode is not active");
            return;
        }
        
        streamBuffer.append(chunk);
        
        // Update UI on main thread with throttling
        streamHandler.removeCallbacksAndMessages(null);
        streamHandler.postDelayed(() -> {
            String content = streamBuffer.toString();
            if (isMarkdownEnabled && markwon != null) {
                markwon.setMarkdown(tvContent, content);
            } else {
                tvContent.setText(content);
            }
        }, STREAM_UPDATE_DELAY);
    }
    
    public void endStreamMode() {
        if (!isStreamMode) {
            return;
        }
        
        isStreamMode = false;
        streamHandler.removeCallbacksAndMessages(null);
        
        // Final update
        String finalContent = streamBuffer.toString();
        if (isMarkdownEnabled && markwon != null) {
            markwon.setMarkdown(tvContent, finalContent);
        } else {
            tvContent.setText(finalContent);
        }
        
        Timber.tag(TAG).d( "Stream mode ended");
    }
    
    public boolean isInStreamMode() {
        return isStreamMode;
    }
    
    // Enhanced setContent method for stream mode
    public void setContentStreaming(String content, boolean isComplete) {
        if (!isStreamMode) {
            startStreamMode();
        }
        
        if (isComplete) {
            streamBuffer.setLength(0);
            streamBuffer.append(content);
            endStreamMode();
        } else {
            appendStreamContent(content);
        }
    }

    /**
     * 获取当前显示的内容
     * @return 内容文本
     */
    public String getContent() {
        if (tvContent != null) {
            return tvContent.getText().toString();
        }
        return "";
    }

    public void setRefreshBtnVisible(int visible){
        btnRefresh.setVisibility(visible);
    }


    /**
     * 处理 TTS 播放按钮点击
     */
    private void handleTTSPlayClick() {

        if (isCurrentlyPlaying == true) {
            // 当前正在播放，停止播放
            stopTTS();
        } else {
            // 开始播放
            String textToSpeak = tvContent.getText().toString();
            if (textToSpeak != null && !textToSpeak.trim().isEmpty()) {
                startTTS(textToSpeak);
            }
        }
    }

    /**
     * 开始 TTS 播放
     */
    private void startTTS(String text) {
        // 停止其他正在播放的 TTS
        stopAllTTS();

        // 显示加载提示
        Toast loadingToast = GlobalToast.show((Activity) getContext(),
                "正在生成语音朗读", GlobalToast.Type.LOADING);

        // 使用弱引用避免内存泄漏
//        WeakReference<ChatAdapter.ChatViewHolder> holderRef = new WeakReference<>(holder);
//        currentTTSHolderRef = new WeakReference<>(holder);
        startPlayAnimation();
        // 设置 TTS 监听器
        TTSManager.getInstance().setOnPlayerListener(new OnPlayerListener() {
            @Override
            public void playerStart() {
                if (loadingToast == null){
                    return;
                }
                loadingToast.cancel();
//                ChatAdapter.ChatViewHolder currentHolder = holderRef.get();
                if (btnSpeak != null) {
                    // 设置播放状态
                    isCurrentlyPlaying =  true;

                    Timber.tag(TAG).d( "TTS playback started");
                }
            }

            @Override
            public void playerStop() {
                     // 重置播放状态
                    isCurrentlyPlaying = false;

                    // 重置播放按钮
                    resetPlayButton();

                    Timber.tag(TAG).d( "TTS playback stopped");

                // 清理当前播放的引用
//                if (currentTTSHolderRef != null) {
//                    currentTTSHolderRef.clear();
//                    currentTTSHolderRef = null;
//                }
            }
        });
        Spanned markdown = markwon.toMarkdown(text);
        // 开始 TTS 播放
        TTSManager.getInstance().textForceToAudio(markdown.toString());
    }

    /**
     * 停止 TTS 播放
     */
    private void stopTTS() {
        isCurrentlyPlaying = false;
        TTSManager.getInstance().stop();
        resetPlayButton();

        Timber.tag(TAG).d( "TTS playback stopped manually");
    }

    /**
     * 停止所有 TTS 播放
     */
    private void stopAllTTS() {
                stopTTS();

        TTSManager.getInstance().stop();
    }

    /**
     * 开始播放动画
     */
    protected void startPlayAnimation() {
        if ( btnSpeak != null) {
            btnSpeak.setImageResource(R.drawable.chat_tts_speaking_anim);
            Drawable drawable = btnSpeak.getDrawable();
            if (drawable instanceof AnimationDrawable) {
                ((AnimationDrawable) drawable).start();
            }
        }
    }

    /**
     * 重置播放按钮状态
     */
    protected void resetPlayButton() {
        if (btnSpeak != null) {
            // 停止动画
            Drawable drawable = btnSpeak.getDrawable();
            if (drawable instanceof AnimationDrawable) {
                ((AnimationDrawable) drawable).stop();
            }

            // 重置为播放图标
            btnSpeak.setImageResource(R.mipmap.chat_play);
        }
    }
}