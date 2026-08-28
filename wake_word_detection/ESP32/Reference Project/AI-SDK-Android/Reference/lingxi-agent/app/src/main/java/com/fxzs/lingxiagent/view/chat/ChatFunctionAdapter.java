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

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.TrackerUtils;
import com.fxzs.lingxiagent.model.chat.callback.OnItemClick;
import com.fxzs.lingxiagent.model.chat.dto.ChatFunctionBean;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;

import java.util.ArrayList;
import java.util.List;

public class ChatFunctionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ONE = 1;
    private static final int TYPE_TWO = 2;

    private List<ChatFunctionBean> dataList = new ArrayList<>();

    private Context context;
    private boolean showDelete = false;
    OnItemClick onItemClick;
    int type;
    // 是否选中“深度思考”
    private boolean thinkSelected = false;

    public ChatFunctionAdapter(Context context, List<ChatFunctionBean> dataList, OnItemClick onItemClick) {
        this.context = context;
        this.dataList = dataList;
        this.onItemClick = onItemClick;
    }

    public void setThinkSelected(boolean selected) {
        this.thinkSelected = selected;
    }

    public boolean isThinkSelected() {
        return thinkSelected;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_function, parent, false);
        return new ViewHolderType1(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ChatFunctionBean data = dataList.get(position);

        holder.itemView.setOnClickListener(v -> {
            TrackerUtils.trackCapsuleClickEvent(data.getName());
            if (onItemClick != null) {
                onItemClick.onItemClick(position);
            }
        });

        ((ViewHolderType1) holder).bind(data);
        // 根据是否是“深度思考”项设置选中态
        holder.itemView.setSelected(data.getId() == Constant.ChatFunction.TYPE_THINK && thinkSelected);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    // ViewHolder for Type 1
    class ViewHolderType1 extends RecyclerView.ViewHolder {
        ImageView iv;
        TextView tv_name;
        RelativeLayout root_view;

        ViewHolderType1(View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.iv);
            tv_name = itemView.findViewById(R.id.tv_name);
        }

        void bind(ChatFunctionBean data) {
            ImageUtil.load(context, data.getIcon(), iv);
            tv_name.setText(data.getName());
        }
    }

}
