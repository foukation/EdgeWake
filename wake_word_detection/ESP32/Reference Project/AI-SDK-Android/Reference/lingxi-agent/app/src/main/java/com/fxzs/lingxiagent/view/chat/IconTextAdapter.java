package com.fxzs.lingxiagent.view.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.IconTextItem;

import java.util.List;

public class IconTextAdapter extends RecyclerView.Adapter<IconTextAdapter.ViewHolder> {

    private List<IconTextItem> mDatasList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public IconTextAdapter(List<IconTextItem> list, OnItemClickListener listener) {
        mDatasList = list;
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.simple_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IconTextItem option = mDatasList.get(position);
        holder.bind(option);

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatasList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;
        private TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.item_icon);
            textView = itemView.findViewById(R.id.item_text);
        }

        public void bind(IconTextItem option) {
            if (option.getIconResId() != -1) {
                imageView.setImageResource(option.getIconResId());
            }
            textView.setText(option.getTitle());
        }
    }
}