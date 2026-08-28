package com.fxzs.lingxiagent.view.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardHotelModel;
import com.fxzs.smartassist.util.ZUtil.SizeUtils;

import java.util.ArrayList;
import java.util.List;

public class ChatHotelAdapter extends RecyclerView.Adapter<ChatHotelAdapter.ChatHotelViewHolder> {

    private Context mContext;
    private List<ChatCardHotelModel> mData = new ArrayList<>();

    public ChatHotelAdapter(Context mContext) {
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ChatHotelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(mContext).inflate(R.layout.trip_list_hotel_item, parent, false);
        return new ChatHotelViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatHotelViewHolder holder, int position) {
        ChatCardHotelModel hotelModel = mData.get(position);
        holder.tvName.setText(hotelModel.getHotelName());
        holder.tvAddress.setText(hotelModel.getZone());
        holder.tvScore.setText(String.valueOf(hotelModel.getRating()));
        if (hotelModel.getBusiness() != null) {
            holder.tvTag.setText(hotelModel.getBusiness().getKeytag());
        }
        holder.tvTitle.setText(hotelModel.getTag());
        RequestOptions options = new RequestOptions()
                .transform(new CenterCrop(), new RoundedCorners(SizeUtils.dpToPx(10)));
        Glide.with(mContext)
                .load(hotelModel.getCoverImage())
                .apply(options)
                .into(holder.ivImg);
        holder.tvMore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMoreClick(hotelModel);
            }
        });
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(hotelModel);
            }
        });
    }

    OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        listener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(ChatCardHotelModel hotelModel);

        void onMoreClick(ChatCardHotelModel hotelModel);
    }

    public void setNewData(List<ChatCardHotelModel> data) {
        mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
    }

    public void addData(List<ChatCardHotelModel> data) {
        mData.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    class ChatHotelViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvAddress, tvTag, tvScore, tvName, tvMore, tvTitle;
        private final ImageView ivImg;

        public ChatHotelViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.category_title);
            tvMore = itemView.findViewById(R.id.more_button);
            tvName = itemView.findViewById(R.id.trip_item_title);
            tvScore = itemView.findViewById(R.id.trip_item_score);
            tvTag = itemView.findViewById(R.id.trip_item_tag_one);
            tvAddress = itemView.findViewById(R.id.trip_item_address);
            ivImg = itemView.findViewById(R.id.trip_item_img);
        }
    }

}
