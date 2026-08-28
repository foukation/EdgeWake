package com.fxzs.lingxiagent.view.aifile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;

import java.util.List;

public class AiFilePageAdapter extends RecyclerView.Adapter<AiFilePageAdapter.VH> {

    public interface Listener {
        void onItemClick(int position);
    }

    private final List<AiFilePageItem> items;
    private Listener listener;

    public AiFilePageAdapter(List<AiFilePageItem> items) {
        this.items = items;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_file_page, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AiFilePageItem item = items.get(position);
        holder.tvPage.setText(String.valueOf(item.pageIndex));
        if (item.thumbnail != null) {
            holder.ivThumb.setImageBitmap(item.thumbnail);
        } else {
            holder.ivThumb.setImageDrawable(null);
        }

        holder.fl_check_mark.setVisibility(item.selected ? View.VISIBLE : View.INVISIBLE);
        holder.vBorder.setBackgroundResource(item.selected ? R.drawable.bg_ai_file_page_border_checked : R.drawable.bg_ai_file_page_border_unchecked);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        View fl_check_mark;
        View vBorder;
        TextView tvPage;

        VH(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_thumb);
            fl_check_mark = itemView.findViewById(R.id.fl_check_mark);
            vBorder = itemView.findViewById(R.id.v_border);
            tvPage = itemView.findViewById(R.id.tv_page);
        }
    }
}
