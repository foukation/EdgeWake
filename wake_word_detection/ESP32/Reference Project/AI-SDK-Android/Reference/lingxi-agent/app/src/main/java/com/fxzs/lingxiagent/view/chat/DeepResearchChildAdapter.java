package com.fxzs.lingxiagent.view.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;

public class DeepResearchChildAdapter extends RecyclerView.Adapter<DeepResearchChildAdapter.DeepResearchChildViewHolder> {
    @NonNull
    @Override
    public DeepResearchChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.deep_research_card_child, parent, false);
        return new DeepResearchChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeepResearchChildViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }


    static class DeepResearchChildViewHolder extends RecyclerView.ViewHolder {

        public DeepResearchChildViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }



}
