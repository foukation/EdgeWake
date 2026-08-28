package com.fxzs.lingxiagent.view.user;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.user.dto.FAQItem;

import java.util.ArrayList;
import java.util.List;

public class HelpFAQAdapter extends RecyclerView.Adapter<HelpFAQAdapter.FAQViewHolder> {
    
    private final Context context;
    private final List<FAQItem> faqList;
    private OnItemClickListener clickListener;
    
    public interface OnItemClickListener {
        void onItemClick(FAQItem faq, int position);
    }
    
    public HelpFAQAdapter(Context context) {
        this.context = context;
        this.faqList = new ArrayList<>();
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }
    
    public void setData(List<FAQItem> faqs) {
        faqList.clear();
        if (faqs != null) {
            faqList.addAll(faqs);
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public FAQViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_help_faq, parent, false);
        return new FAQViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FAQViewHolder holder, int position) {
        FAQItem faq = faqList.get(position);
        
        holder.tvTitle.setText(faq.getTitle());
        
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(faq, position);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return faqList.size();
    }
    
    static class FAQViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ImageView ivArrow;
        
        FAQViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            ivArrow = itemView.findViewById(R.id.iv_arrow);
        }
    }
}