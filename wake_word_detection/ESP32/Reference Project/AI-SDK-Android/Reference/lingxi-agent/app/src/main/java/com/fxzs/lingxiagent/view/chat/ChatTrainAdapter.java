package com.fxzs.lingxiagent.view.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Html;
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
import com.fxzs.lingxiagent.model.chat.dto.ChatCardTrainEntity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class ChatTrainAdapter extends RecyclerView.Adapter<ChatTrainAdapter.ViewHolder> {

    private Context mContext;
    private List<ChatCardTrainEntity> mData = new ArrayList<>();

    Gson gson = new GsonBuilder().create();

    public ChatTrainAdapter(Context mContext) {
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(mContext).inflate(R.layout.trip_list_traffic_item, parent, false);
        return new ViewHolder(inflate);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatCardTrainEntity chatCardTrainEntity = mData.get(position);
        if (chatCardTrainEntity == null) {
            holder.mRlEmptyLayout.setVisibility(View.VISIBLE);
            holder.mRlConLayout.setVisibility(View.GONE);
            return;
        }
        holder.mRlEmptyLayout.setVisibility(View.GONE);
        holder.mRlConLayout.setVisibility(View.VISIBLE);
        holder.mTvTitle.setText(chatCardTrainEntity.getTag());
        holder.mTvStartTime.setText(chatCardTrainEntity.getStart_time());
        holder.mTvEndTime.setText(chatCardTrainEntity.getEnd_time());
        holder.mTvStartCity.setText(chatCardTrainEntity.getStart_station());
        holder.mTvEndCity.setText(chatCardTrainEntity.getEnd_station());
        holder.mTvTrainNumber.setText(chatCardTrainEntity.getTrainNo());
        holder.mTvMoney.setText(String.valueOf(chatCardTrainEntity.getTicket_price()));
        holder.mTvAllTime.setText(chatCardTrainEntity.getTotaltime());
        Glide.with(mContext).load(chatCardTrainEntity.getIcon()).into(holder.mIvTitle);
        holder.mTvBook.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(chatCardTrainEntity);
            }
        });
        String ticketsStr = chatCardTrainEntity.getTickets();
        List<ChatCardTrainEntity.TicketsBean> tickets = gson.fromJson(ticketsStr, new TypeToken<List<ChatCardTrainEntity.TicketsBean>>() {
        }.getType());
        if (tickets != null && !tickets.isEmpty()) {
            String format = "%s <font color=\"#15C252\">%s</font>";
            holder.mTvTrainTwo.setText(Html.fromHtml(String.format(format, tickets.get(0).getTicket_type(), tickets.get(0).getTicket_left())));
            if (tickets.size() > 1) {
                holder.mTvTrainOne.setText(Html.fromHtml(String.format(format, tickets.get(1).getTicket_type(), tickets.get(1).getTicket_left())));
            }
            if (tickets.size() > 2) {
                holder.mTvTrainAffairs.setText(Html.fromHtml(String.format(format, tickets.get(2).getTicket_type(), tickets.get(2).getTicket_left())));
            }
        }
    }


    OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        listener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(ChatCardTrainEntity entity);
    }

    public void setNewData(List<ChatCardTrainEntity> data) {
        mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView mTvBook, mTvTrainAffairs, mTvTrainOne, mTvTrainTwo, mTvMoney, mTvTrainNumber, mTvEndCity, mTvStartCity, mTvAllTime, mTvEndTime, mTvStartTime, mTvTitle;
        private final ImageView mIvTitle;
        private final RelativeLayout mRlConLayout, mRlEmptyLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTvTitle = itemView.findViewById(R.id.start_text);
            mTvStartTime = itemView.findViewById(R.id.traffic_train_starttime);
            mTvEndTime = itemView.findViewById(R.id.traffic_train_endtime);
            mTvAllTime = itemView.findViewById(R.id.traffic_train_alltime);
            mTvStartCity = itemView.findViewById(R.id.traffic_train_startcity);
            mTvEndCity = itemView.findViewById(R.id.traffic_train_endcity);
            mTvTrainNumber = itemView.findViewById(R.id.traffic_train_num);
            mTvMoney = itemView.findViewById(R.id.traffic_money);
            mTvTrainTwo = itemView.findViewById(R.id.traffic_train_two);
            mTvTrainOne = itemView.findViewById(R.id.traffic_train_one);
            mTvTrainAffairs = itemView.findViewById(R.id.traffic_train_affairs);
            mTvBook = itemView.findViewById(R.id.traffic_book);
            mIvTitle = itemView.findViewById(R.id.id_iv_title);
            mRlEmptyLayout = itemView.findViewById(R.id.traffic_data_empty_layout);
            mRlConLayout = itemView.findViewById(R.id.traffic_data_layout);
        }
    }

}
