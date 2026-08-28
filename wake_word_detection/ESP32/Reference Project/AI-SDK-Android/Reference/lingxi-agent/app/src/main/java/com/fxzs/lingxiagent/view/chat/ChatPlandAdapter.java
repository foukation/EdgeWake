package com.fxzs.lingxiagent.view.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardPlandEntity;

import java.util.ArrayList;
import java.util.List;

public class ChatPlandAdapter extends RecyclerView.Adapter<ChatPlandAdapter.ViewHolder> {

    private Context mContext;
    private List<ChatCardPlandEntity> mData = new ArrayList<>();

    public ChatPlandAdapter(Context mContext) {
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(mContext).inflate(R.layout.trip_list_pland_item, parent, false);
        return new ViewHolder(inflate);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatCardPlandEntity chatCardPlandEntity = mData.get(position);
        if (chatCardPlandEntity == null) {
            holder.mRlEmptyLayout.setVisibility(View.VISIBLE);
            holder.mRlConLayout.setVisibility(View.GONE);
            return;
        }
        holder.mRlEmptyLayout.setVisibility(View.GONE);
        holder.mRlConLayout.setVisibility(View.VISIBLE);
        holder.mTvTitle.setText(chatCardPlandEntity.getTag());
        holder.mTvStartTime.setText(chatCardPlandEntity.getDepTime());
        holder.mTvEndTime.setText(chatCardPlandEntity.getArrTime());
        holder.mTvStartCity.setText(chatCardPlandEntity.getDepCity() + chatCardPlandEntity.getDepAirport() + chatCardPlandEntity.getDepTerminal());
        holder.mTvEndCity.setText(chatCardPlandEntity.getArrCity() + chatCardPlandEntity.getArrAirport() + chatCardPlandEntity.getArrTerminal());
        holder.mTvPlandNumber.setText(String.format("%s | %s", chatCardPlandEntity.getFlightCompany(), chatCardPlandEntity.getAirCraftKind()));
        holder.mTvMoney.setText(String.valueOf(chatCardPlandEntity.getPrice()));
        holder.mTvAllTime.setText(chatCardPlandEntity.getFlightNo());
        Glide.with(mContext)
                .load(chatCardPlandEntity.getFlightlogo())
                .into(holder.mIvPlandIcon);
        Glide.with(mContext)
                .load(chatCardPlandEntity.getIcon())
                .into(holder.mIvTitle);
        holder.mTvBook.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(chatCardPlandEntity);
            }
        });
    }


    OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        listener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(ChatCardPlandEntity entity);
    }

    public void setNewData(List<ChatCardPlandEntity> data) {
        mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView mTvEmptyToast, mTvBook, mTvMoney, mTvPlandNumber, mTvEndCity, mTvStartCity, mTvAllTime, mTvEndTime, mTvStartTime, mTvTitle;
        private final ImageView mIvTitle, mIvPlandIcon;
        private final RelativeLayout mRlConLayout, mRlEmptyLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTvTitle = itemView.findViewById(R.id.start_text);
            mTvStartTime = itemView.findViewById(R.id.traffic_airplane_starttime);
            mTvEndTime = itemView.findViewById(R.id.traffic_airplane_endtime);
            mTvAllTime = itemView.findViewById(R.id.traffic_airplane_alltime);
            mTvStartCity = itemView.findViewById(R.id.traffic_airplane_startcity);
            mTvEndCity = itemView.findViewById(R.id.traffic_airplane_endcity);
            mTvPlandNumber = itemView.findViewById(R.id.id_tv_pland_number);
            mTvMoney = itemView.findViewById(R.id.traffic_money);
            mTvBook = itemView.findViewById(R.id.traffic_book);
            mTvEmptyToast = itemView.findViewById(R.id.id_tv_pland_empty_toast);
            mIvPlandIcon = itemView.findViewById(R.id.pland_airplane_icon);
            mIvTitle = itemView.findViewById(R.id.id_iv_title);
            mRlEmptyLayout = itemView.findViewById(R.id.traffic_data_empty_layout);
            mRlConLayout = itemView.findViewById(R.id.traffic_data_layout);

        }
    }

}
