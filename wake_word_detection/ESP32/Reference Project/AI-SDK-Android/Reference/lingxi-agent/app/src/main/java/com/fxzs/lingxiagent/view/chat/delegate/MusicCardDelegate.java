package com.fxzs.lingxiagent.view.chat.delegate;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.service_api.data.MusicData;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.util.audio.MediaPlayerUtils;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;

/**
 * 播放音乐委托类
 */
public class MusicCardDelegate extends CardMessageDelegate {

    private static final String TAG = "MusicCardDelegate";

    public MusicCardDelegate() {
        super(ChatAdapter.TYPE_MUSIC, R.layout.item_message_received_media);
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }

    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message,
                                           int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder musicHolder = (ChatAdapter.ChatViewHolder) holder;
        ChatAdapter chatAdapter = (ChatAdapter) context;
        MusicData musicData = message.getMusicData();
        if ( musicHolder != null && musicData != null){
            if (musicData.getPlay()){
                musicHolder.playIv.setImageResource(R.drawable.audio_mini_player_pause);
                chatAdapter.setPosition(position);
                playMedia(musicData,musicHolder);
            }else {
                musicHolder.playIv.setImageResource(R.drawable.audio_mini_player_play);
            }
            musicHolder.albumName.setText(musicData.getName());
            musicHolder.playIv.setOnClickListener(v -> {
                switchPlayStatus(chatAdapter,position,musicHolder,musicData);
            });
        }

    }

    private void switchPlayStatus( ChatAdapter chatAdapter,int position,ChatAdapter.ChatViewHolder musicHolder,MusicData musicData){
        TTSManager.Companion.getInstance().stop();
        if (chatAdapter.getPosition() == position){
            if (MediaPlayerUtils.Companion.getInstance().isPlaying()){
                chatAdapter.setMediaStatus(chatAdapter.getPosition());
            }else {
//                playMedia(musicData,musicHolder);
                musicData.setPlay(true);
                chatAdapter.notifyItemChanged(position);
            }
            return;
        }
        if (chatAdapter.getPosition() >= 0 ){
            chatAdapter.setMediaStatus(chatAdapter.getPosition());
        }
        chatAdapter.setPosition(position);
        musicData.setPlay(true);
        chatAdapter.notifyItemChanged(position);
//        playMedia(musicData,musicHolder);
    }


    private void playMedia(MusicData musicData,ChatAdapter.ChatViewHolder musicHolder){
        TTSManager.Companion.getInstance().stop();
        MediaPlayerUtils.Companion.getInstance().play(musicData.getUrl(), new MediaPlayerUtils.OnStartListener() {
            @Override
            public void onComplete() {
                musicData.setPlay(false);
                musicHolder.playIv.setImageResource(R.drawable.audio_mini_player_play);
            }

            @Override
            public void onStart() {
                musicData.setPlay(true);
                musicHolder.playIv.setImageResource(R.drawable.audio_mini_player_pause);
            }
        });
    }

    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }
}