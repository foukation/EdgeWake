package com.fxzs.lingxiagent.lingxi.translate.adapter;

import android.animation.TimeInterpolator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.translate.model.TranslationItem;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * TranslationItem适配器 - 用于显示本地缓存的翻译结果
 */
public class TranslationItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_RESULT = 0;
    private static final int TYPE_MID_RESULT = 1;

    private List<TranslationItem> items;
    private String currentMidResult = "";
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private boolean showMidResultItem = false; // 是否在底部显示中间结果项，默认不显示
    private OnPlayClickListener playClickListener;

    /**
     * 播放按钮点击监听器接口
     */
    public interface OnPlayClickListener {
        void onPlayClick(TranslationItem item, int position);
    }

    /**
     * 设置播放按钮点击监听器
     */
    public void setOnPlayClickListener(OnPlayClickListener listener) {
        this.playClickListener = listener;
    }

    public void setShowMidResultItem(boolean show) {
        this.showMidResultItem = show;
        notifyDataSetChanged();
    }
    // 控制"识别中三点"的显示开关：麦克风开= true，关= false
    public void setMicActive(boolean active) {
        setShowMidResultItem(active);
    }



    public TranslationItemAdapter(List<TranslationItem> items) {
        this.items = items;
    }

    public void updateItems(List<TranslationItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void updateMidResult(String midResult) {
        if (!showMidResultItem) {
            // 不显示中间态行：直接忽略并清空，避免与最新稳定结果看起来重复
            this.currentMidResult = "";
            return;
        }
        this.currentMidResult = midResult;
        // 还原原先的刷新行为（仅在展示中间态时）
        if (items.size() > 0) {
            notifyItemChanged(items.size());
        } else {
            notifyDataSetChanged();
        }
    }

    /**
     * 添加单个新项目（优化性能）
     */
    public void addItem(TranslationItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    /**
     * 更新指定位置的项目
     */
    public void updateItem(int position, TranslationItem item) {
        if (position >= 0 && position < items.size()) {
            items.set(position, item);
            notifyItemChanged(position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (showMidResultItem && position == items.size()) {
            return TYPE_MID_RESULT;
        }
        return TYPE_RESULT;
    }

    @Override
    public int getItemCount() {
        int count = items.size();
        if (showMidResultItem) {
            count += 1; // 麦克风开启时固定显示底部三个点
        }
        return count;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_MID_RESULT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_translate_mid_result, parent, false);
            return new MidResultViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_translate_result, parent, false);
            return new ResultViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MidResultViewHolder) {
            ((MidResultViewHolder) holder).bind(currentMidResult);
        } else if (holder instanceof ResultViewHolder) {
            TranslationItem item = items.get(position);
            ((ResultViewHolder) holder).bind(item, timeFormat, playClickListener, position);
        }
    }


    /**
     * 翻译结果ViewHolder
     */
    static class ResultViewHolder extends RecyclerView.ViewHolder {
        private TextView tvOriginalText;
        private TextView tvTranslatedText;
        private TextView tvTime;

        public ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOriginalText = itemView.findViewById(R.id.tv_original_text);
            tvTranslatedText = itemView.findViewById(R.id.tv_translated_text);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        public void bind(TranslationItem item, SimpleDateFormat timeFormat, OnPlayClickListener listener, int position) {
            tvOriginalText.setText(item.getSourceText());
            tvTranslatedText.setText(item.getTargetText());

            // 最下面那行"正在识别"的中间状态不展示
            if (tvTime != null) {
                tvTime.setVisibility(View.GONE);
            }

            // 保持之前的透明度样式（不影响业务逻辑）
            if (item.isEnd()) {
                tvOriginalText.setAlpha(1.0f);
                tvTranslatedText.setAlpha(1.0f);
            } else {
                tvOriginalText.setAlpha(0.8f);
                tvTranslatedText.setAlpha(0.8f);
            }
            
            // 隐藏播放按钮（聆听模式不需要播放功能）
            View ivPlay = itemView.findViewById(R.id.iv_play);
            if (ivPlay != null) {
                ivPlay.setVisibility(View.GONE);
            }
        }

        /**
         * 播放按钮点击动画效果
         */
        private void playClickAnimation(View view) {
            // 缩放动画：先缩小再恢复
            view.animate()
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        view.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .start();
                    })
                    .start();
            
            // 旋转动画：轻微旋转
            view.animate()
                    .rotation(15f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        view.animate()
                                .rotation(0f)
                                .setDuration(100)
                                .start();
                    })
                    .start();
        }

        private String formatTime(int milliseconds) {
            if (milliseconds < 0) {
                return "00:00";
            }
            int totalSeconds = milliseconds / 1000;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }


    /**
     * 识别中结果ViewHolder
     */
    static class MidResultViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMidText;
        private View dotsContainer;
        private View dot1, dot2, dot3;
        private android.animation.ObjectAnimator a1x, a1y, a2x, a2y, a3x, a3y;
        private final TimeInterpolator interp = new AccelerateDecelerateInterpolator();

        public MidResultViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMidText = itemView.findViewById(R.id.tv_mid_text);
            dotsContainer = itemView.findViewById(R.id.layout_dots);
            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
        }

        public void bind(String midResult) {
            // 按需求：不显示中间态文本，只显示三个点动画
            if (tvMidText != null) {
                tvMidText.setText("");
                tvMidText.setVisibility(View.GONE);
            }
            showDots();
        }

        private void showDots() {
            if (dotsContainer == null) return;
            dotsContainer.setVisibility(View.VISIBLE);
            startPulse(dot1, 0);
            startPulse(dot2, 150);
            startPulse(dot3, 300);
        }

        private void startPulse(View v, long delay) {
            if (v == null) return;
            cancel(v);
            a1x = android.animation.ObjectAnimator.ofFloat(v, View.SCALE_X, 1f, 1.6f, 1f);
            a1y = android.animation.ObjectAnimator.ofFloat(v, View.SCALE_Y, 1f, 1.6f, 1f);
            a1x.setDuration(600); a1y.setDuration(600);
            a1x.setStartDelay(delay); a1y.setStartDelay(delay);
            a1x.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            a1y.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            a1x.setInterpolator(interp); a1y.setInterpolator(interp);
            a1x.start(); a1y.start();
        }

        private void cancel(View v) {
            v.clearAnimation();
            v.animate().cancel();
            v.setScaleX(1f); v.setScaleY(1f);
        }
    }
}
