package com.fxzs.lingxiagent.view.chat.delegate;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.util.audio.OnPlayerListener;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;
import com.fxzs.lingxiagent.view.common.GlobalToast;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import timber.log.Timber;

/**
 * 支持 TTS 功能的委托基类
 * 提供统一的 TTS 功能支持，包括：
 * - TTS 播放控制和状态管理
 * - ViewHolder 的弱引用管理，防止内存泄漏
 * - 播放动画的生命周期管理
 * - TTS 资源的自动清理
 * <p>
 * 需要 TTS 功能的委托类可以继承此基类
 */
public abstract class TTSAwareDelegate extends BaseViewTypeDelegate {

    private static final String TAG = "TTSAwareDelegate";

    // 使用 WeakHashMap 缓存 ViewHolder 的 TTS 状态，防止内存泄漏
    private final Map<RecyclerView.ViewHolder, Boolean> ttsStateMap = new WeakHashMap<>();

    // 当前正在播放 TTS 的 ViewHolder 弱引用
    private WeakReference<RecyclerView.ViewHolder> currentTTSHolderRef;
   // private Markwon markwon;

    public TTSAwareDelegate(int viewType, int layoutRes) {
        super(viewType, layoutRes);
    }

    /**
     * TTS 相关的 ViewHolder 绑定逻辑
     * 子类需要调用此方法来设置 TTS 功能
     */
    protected void setupTTSFeatures(RecyclerView.ViewHolder holder, ChatMessage message,
                                    ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder chatHolder = (ChatAdapter.ChatViewHolder) holder;
        //markwon = MarkdownUtils.createMarkwon(context.getContext());
        ChatAdapter chatAdapter = (ChatAdapter) context;
        if (chatHolder.iv_chat_play != null) {
            // 初始化播放按钮状态
            resetPlayButton(chatHolder);

            // 设置播放按钮点击监听器
            chatHolder.iv_chat_play.setOnClickListener(view -> {
                chatAdapter.setMediaStatus(chatAdapter.getPosition());
                handleTTSPlayClick(chatHolder, message, context);
            });
        }
    }

    /**
     * 处理 TTS 播放按钮点击
     */
    private void handleTTSPlayClick(ChatAdapter.ChatViewHolder holder, ChatMessage message,
                                    ChatAdapterContext context) {
        ZUtils.print("message.getToLang() == "+message.getToLang());
        if (message.getToLang() != null && !TTSManager.getInstance().getTargetLanguageList().contains(message.getToLang())){
            GlobalToast.show((Activity) context.getContext(),
                    "暂不支持语音朗读", GlobalToast.Type.ERROR);
            return;
        }
         Boolean isCurrentlyPlaying = ttsStateMap.get(holder);

        if (Boolean.TRUE.equals(isCurrentlyPlaying) || Boolean.TRUE.equals(message.isTTSPlaying())) {
            // 当前正在播放，停止播放
            stopTTS(holder,message);
        } else {
            // 开始播放
            String textToSpeak = extractTextForTTS(message);
            if (textToSpeak != null && !textToSpeak.trim().isEmpty()) {
                startTTS(holder, textToSpeak, context,message);
            }
        }
    }

    /**
     * 开始 TTS 播放
     */
    private void startTTS(ChatAdapter.ChatViewHolder holder, String text, ChatAdapterContext context,ChatMessage message) {
        // 停止其他正在播放的 TTS
        stopAllTTS();

        // 显示加载提示
        Toast loadingToast = GlobalToast.show((Activity) context.getContext(),
                "正在生成语音朗读", GlobalToast.Type.LOADING);

        // 使用弱引用避免内存泄漏
        WeakReference<ChatAdapter.ChatViewHolder> holderRef = new WeakReference<>(holder);
        currentTTSHolderRef = new WeakReference<>(holder);
        startPlayAnimation(holder);
        // 设置 TTS 监听器
        TTSManager.getInstance().setOnPlayerListener(new OnPlayerListener() {
            @Override
            public void playerStart() {
                if (loadingToast == null){
                    return;
                }
//                if(message != null){
//                    message.setTTSPlaying(true);
//                }
                loadingToast.cancel();
                ChatAdapter.ChatViewHolder currentHolder = holderRef.get();
                if (currentHolder != null && currentHolder.iv_chat_play != null) {
                    // 设置播放状态
                    ttsStateMap.put(currentHolder, true);

                    Timber.tag(TAG).d( "TTS playback started");
                }
            }

            @Override
            public void playerStop() {
//                if(message != null){
//                    message.setTTSPlaying(false);
//                }
                ChatAdapter.ChatViewHolder currentHolder = holderRef.get();
                if (currentHolder != null) {
                    // 重置播放状态
                    ttsStateMap.put(currentHolder, false);

                    // 重置播放按钮
                    resetPlayButton(currentHolder);

                    Timber.tag(TAG).d( "TTS playback stopped");
                }

                // 清理当前播放的引用
                if (currentTTSHolderRef != null) {
                    currentTTSHolderRef.clear();
                    currentTTSHolderRef = null;
                }
            }
        });
        //Spanned markdown = markwon.toMarkdown(text);
        // 开始 TTS 播放
        TTSManager.getInstance().textForceToAudio(text);
    }

    /**
     * 停止 TTS 播放
     */
    private void stopTTS(ChatAdapter.ChatViewHolder holder,ChatMessage message) {
//        if(message != null){
//            message.setTTSPlaying(false);
//        }
        ttsStateMap.put(holder, false);
        TTSManager.getInstance().stop();
        resetPlayButton(holder);

        Timber.tag(TAG).d( "TTS playback stopped manually");
    }

    /**
     * 停止所有 TTS 播放
     */
    private void stopAllTTS() {
        if (currentTTSHolderRef != null) {
            RecyclerView.ViewHolder currentHolder = currentTTSHolderRef.get();
            if (currentHolder instanceof ChatAdapter.ChatViewHolder) {
                stopTTS((ChatAdapter.ChatViewHolder) currentHolder,null);
            }
        }

        TTSManager.getInstance().stop();
    }

    /**
     * 开始播放动画
     */
    protected void startPlayAnimation(ChatAdapter.ChatViewHolder holder) {
        if (holder != null && holder.iv_chat_play != null) {
            holder.iv_chat_play.setImageResource(R.drawable.chat_tts_speaking_anim);
            Drawable drawable = holder.iv_chat_play.getDrawable();
            if (drawable instanceof AnimationDrawable) {
                ((AnimationDrawable) drawable).start();
            }
        }
    }

    /**
     * 重置播放按钮状态
     */
    protected void resetPlayButton(ChatAdapter.ChatViewHolder holder) {
        if (holder.iv_chat_play != null) {
            // 停止动画
            Drawable drawable = holder.iv_chat_play.getDrawable();
            if (drawable instanceof AnimationDrawable) {
                ((AnimationDrawable) drawable).stop();
            }

            // 重置为播放图标
            holder.iv_chat_play.setImageResource(R.mipmap.chat_play);
        }
    }

    /**
     * 清理 TTS 相关资源
     * 在 ViewHolder 回收时调用
     */
    protected void cleanupTTSResources(RecyclerView.ViewHolder holder) {
        if (holder instanceof ChatAdapter.ChatViewHolder) {
            ChatAdapter.ChatViewHolder chatHolder = (ChatAdapter.ChatViewHolder) holder;

            // 如果当前 ViewHolder 正在播放，停止播放
            Boolean isPlaying = ttsStateMap.get(holder);
            if (Boolean.TRUE.equals(isPlaying)) {
                stopTTS(chatHolder,null);
            }

            // 清理状态缓存
            ttsStateMap.remove(holder);

            // 重置播放按钮
            resetPlayButton(chatHolder);

            Timber.tag(TAG).d( "TTS resources cleaned up for ViewHolder");
        }
    }

    /**
     * 从消息中提取用于 TTS 的文本
     * 子类可以重写此方法来自定义提取逻辑
     */
    protected String extractTextForTTS(ChatMessage message) {
        return message != null ? message.getMessage() : null;
    }

    /**
     * 检查当前 ViewHolder 是否正在播放 TTS
     */
    protected boolean isTTSPlaying(RecyclerView.ViewHolder holder) {
        return Boolean.TRUE.equals(ttsStateMap.get(holder));
    }

    /**
     * 获取 TTS 管理器实例
     */
    protected TTSManager getTTSManager() {
        return TTSManager.getInstance();
    }

    /**
     * ViewHolder 回收时的清理逻辑
     * 子类可以重写此方法添加额外的清理逻辑
     */
    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder, ChatAdapterContext context) {
        super.onViewRecycled(holder, context);

        // 清理 TTS 相关资源
        cleanupTTSResources(holder);
    }
}