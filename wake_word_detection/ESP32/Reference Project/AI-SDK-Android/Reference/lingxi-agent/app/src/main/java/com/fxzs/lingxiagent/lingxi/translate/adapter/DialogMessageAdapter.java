package com.fxzs.lingxiagent.lingxi.translate.adapter;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.translate.model.DialogMessage;

import java.util.List;

/**
 * 对话模式消息列表适配器
 */
public class DialogMessageAdapter extends RecyclerView.Adapter<DialogMessageAdapter.MessageViewHolder> {

    private List<DialogMessage> messages;
    private Context context;

    public DialogMessageAdapter(Context context, List<DialogMessage> messages) {
        this.context = context;
        this.messages = messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_dialog_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        DialogMessage message = messages.get(position);

        holder.tvMessageText.setText(message.getText());

        // 样式调整：译文为蓝色，原文为深色
        if (message.getMessageType() == DialogMessage.TYPE_TRANSLATION) {
            holder.tvMessageText.setBackground(null);
            holder.tvMessageText.setTextColor(ContextCompat.getColor(context, R.color.primary_blue));
//            holder.tvSpeakerLabel.setText("对方说：");
            ViewCompat.setBackgroundTintList(holder.dot1, ColorStateList.valueOf(Color.parseColor("#1C77FF")));
            ViewCompat.setBackgroundTintList(holder.dot2, ColorStateList.valueOf(Color.parseColor("#1C77FF")));
            ViewCompat.setBackgroundTintList(holder.dot3, ColorStateList.valueOf(Color.parseColor("#1C77FF")));

        } else {
            holder.tvMessageText.setBackground(null);
            holder.tvMessageText.setTextColor(ContextCompat.getColor(context, R.color.text_555));
//            holder.tvSpeakerLabel.setText("我说：");
            ViewCompat.setBackgroundTintList(holder.dot1, ColorStateList.valueOf(Color.parseColor("#555555")));
            ViewCompat.setBackgroundTintList(holder.dot2, ColorStateList.valueOf(Color.parseColor("#555555")));
            ViewCompat.setBackgroundTintList(holder.dot3, ColorStateList.valueOf(Color.parseColor("#555555")));
        }

        // 识别中动效：三个蓝点大小循环变化
        if (message.isRecognizing()) {
            holder.showDots();
        } else {
            holder.hideDots();
        }
        
        // 字号调整已移除，使用布局文件中定义的固定字号
    }

    @Override
    public void onViewRecycled(@NonNull MessageViewHolder holder) {
        super.onViewRecycled(holder);
        holder.hideDots();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<DialogMessage> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    public void addMessage(DialogMessage message) {
        // 检查是否已存在相同 seId 的消息
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getSeId().equals(message.getSeId())) {
                // 更新现有消息
                messages.set(i, message);
                notifyItemChanged(i);
                return;
            }
        }
        // 添加新消息
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSpeakerLabel;
        TextView tvMessageText;
        View dotsContainer;
        View dot1, dot2, dot3;
        ObjectAnimator a1x, a1y, a2x, a2y, a3x, a3y;
        final TimeInterpolator interp = new AccelerateDecelerateInterpolator();

        MessageViewHolder(View itemView) {
            super(itemView);
            tvSpeakerLabel = itemView.findViewById(R.id.tv_speaker_label);
            tvMessageText = itemView.findViewById(R.id.tv_message_text);
            dotsContainer = itemView.findViewById(R.id.layout_dots);
            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
        }

        void showDots() {
            if (dotsContainer == null) return;
            dotsContainer.setVisibility(View.VISIBLE);
            startPulse(dot1, 0);
            startPulse(dot2, 150);
            startPulse(dot3, 300);
        }

        void hideDots() {
            if (dotsContainer == null) return;
            dotsContainer.setVisibility(View.GONE);
            cancel(dot1); cancel(dot2); cancel(dot3);
        }

        private void cancel(View v) {
            if (v == null) return;
            v.clearAnimation();
            v.animate().cancel();
            v.setScaleX(1f); v.setScaleY(1f);
        }

        private void startPulse(View v, long delay) {
            if (v == null) return;
            cancel(v);
            ObjectAnimator sx = ObjectAnimator.ofFloat(v, View.SCALE_X, 1f, 1.6f, 1f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(v, View.SCALE_Y, 1f, 1.6f, 1f);
            sx.setDuration(600); sy.setDuration(600);
            sx.setStartDelay(delay); sy.setStartDelay(delay);
            sx.setRepeatCount(ObjectAnimator.INFINITE);
            sy.setRepeatCount(ObjectAnimator.INFINITE);
            sx.setInterpolator(interp); sy.setInterpolator(interp);
            sx.start(); sy.start();
        }
    }
}