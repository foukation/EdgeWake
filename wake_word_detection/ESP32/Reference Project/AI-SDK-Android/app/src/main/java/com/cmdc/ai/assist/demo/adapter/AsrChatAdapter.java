package com.cmdc.ai.assist.demo.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cmdc.ai.assist.demo.R;
import com.cmdc.ai.assist.demo.media.MediaPlayerUtils;
import com.cmdc.ai.assist.demo.media.TtsMediaPlayer;

import java.util.ArrayList;
import java.util.List;

public class AsrChatAdapter extends RecyclerView.Adapter<AsrChatAdapter.ViewHolder> {
    public static final int TYPE_RIGHT = 1;
    public static final int TYPE_LEFT = 2;

    private List<ChatData> dataList = new ArrayList<>();

    public void addData(ChatData data) {
        if (data == null) {
            return;
        }
        dataList.add(data);
        notifyItemInserted(dataList.size() - 1);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_RIGHT) {
            return new RightViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.app_realtime_item_right, parent, false));
        }
        return new LeftViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_realtime_item_left, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatData chatData = dataList.get(position);
        if (holder instanceof LeftViewHolder) {
            LeftViewHolder leftViewHolder = (LeftViewHolder) holder;
            String text = chatData.text;
            if (text != null && text.contains("图片信息:"))
                text = text.replace("图片信息:", "用户输入：");
            leftViewHolder.leftTv.setText(text);
            if (TextUtils.isEmpty(chatData.pic)) {
                leftViewHolder.picIv.setVisibility(View.GONE);
            } else {
                Glide.with(leftViewHolder.picIv).load(chatData.pic).into(leftViewHolder.picIv);
                leftViewHolder.picIv.setVisibility(View.VISIBLE);
            }

            if (TextUtils.isEmpty(chatData.mediaUrl)) {
                leftViewHolder.playView.setVisibility(View.GONE);
            } else {
                if (chatData.isMusicPlay) {
                    leftViewHolder.playIv.setImageResource(R.mipmap.audio_mini_player_pause_white);
                } else {
                    leftViewHolder.playIv.setImageResource(R.mipmap.audio_mini_player_play_white);
                }
                leftViewHolder.albumTv.setText(chatData.albumName);
                leftViewHolder.playView.setVisibility(View.VISIBLE);
                leftViewHolder.playView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (chatData.isMusicPlay) {
                            chatData.isMusicPlay = false;
                            leftViewHolder.playIv.setImageResource(R.mipmap.audio_mini_player_play_white);
                            MediaPlayerUtils.Companion.getInstance().stop();
                        } else {
                            stopMedia();
                            chatData.isMusicPlay = true;
                            leftViewHolder.playIv.setImageResource(R.mipmap.audio_mini_player_pause_white);
                            MediaPlayerUtils.Companion.getInstance().play(chatData.mediaUrl);
                            TtsMediaPlayer.getInstance().stop();
                        }
                    }
                });
            }

            if (chatData.isGenerating) {
                leftViewHolder.leftTv.setText("生成中...." + chatData.percent + "%");
            }
        } else if (holder instanceof RightViewHolder) {
            ((RightViewHolder) holder).rightTv.setText(chatData.text);
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (dataList.get(position).isSend) {
            return TYPE_RIGHT;
        } else {
            return TYPE_LEFT;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class LeftViewHolder extends ViewHolder {
        private TextView leftTv;
        private ImageView picIv;
        private View playView;
        private TextView albumTv;
        private ImageView playIv;

        LeftViewHolder(View itemView) {
            super(itemView);
            leftTv = itemView.findViewById(R.id.left_tv);
            picIv = itemView.findViewById(R.id.pic_iv);
            playView = itemView.findViewById(R.id.player_view);
            albumTv = itemView.findViewById(R.id.album_name);
            playIv = itemView.findViewById(R.id.play_iv);
        }
    }

    public void stopMedia() {
        for (ChatData chatData : dataList) {
            if (null != chatData) {
                chatData.isMusicPlay = false;
            }
        }
        notifyDataSetChanged();
    }

    public class RightViewHolder extends ViewHolder {
        private TextView rightTv;

        RightViewHolder(View itemView) {
            super(itemView);
            rightTv = itemView.findViewById(R.id.right_tv);
        }
    }
}
