package com.fxzs.lingxiagent.lingxi.help;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;

import java.util.List;

public class HelpContentAdapter extends RecyclerView.Adapter<HelpContentAdapter.ViewHolder> {
    private final List<String> items;
    private Context mContext;
    private OnItemClickListener onItemClickListener;

    public HelpContentAdapter(List<String> items, Context context) {
        this.items = items;
        this.mContext = context;
    }

    public interface OnItemClickListener {
        void onItemClick(int position, String entity);
    }

    // 新增：设置点击事件监听器的方法
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.function_help_item_content, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = items.get(position);
        int textPos = position + 1;
        // 创建 SpannableString
        SpannableString spannableString = new SpannableString(item.split(FunctionHelpConstants.FUNCTION_HELP_DETAILS_DESCRIBE)[0]);

        // 设置点击事件
        if(!TextUtils.isEmpty(item) && item.contains(FunctionHelpConstants.FUNCTION_HELP_DETAILS_DESCRIBE)) {
            holder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(position, item);
                }
            });
        }
        holder.textView.setText(spannableString);
        if(position == 0) {
            holder.rootView.setBackground(mContext.getDrawable(R.drawable.shape_top_layout_corners));
        } else if(position == items.size() -1) {
            holder.rootView.setBackground(mContext.getDrawable(R.drawable.shape_buttom_layout_corners));
            holder.imageView.setVisibility(View.GONE);
        } else {
            holder.rootView.setBackgroundColor(mContext.getColor(R.color.white));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView textView;
        public final ImageView imageView;
        public final View rootView;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.itemText);
            imageView = view.findViewById(R.id.divider_item);
            rootView = view;
        }
    }
}