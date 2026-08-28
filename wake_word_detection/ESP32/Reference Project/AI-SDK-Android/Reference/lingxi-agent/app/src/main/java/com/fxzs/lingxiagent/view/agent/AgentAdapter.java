package com.fxzs.lingxiagent.view.agent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.callback.OnItemClick;
import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean;
import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;

import java.util.ArrayList;
import java.util.List;

public class AgentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ONE = 1;
    private static final int TYPE_TWO = 2;

    private List<getCatDetailListBean> dataList = new ArrayList<>();

    private Context context;
    private boolean showDelete = false;
    OnItemClick onItemClick;
    int type;
    public AgentAdapter(Context context, int type, List<getCatDetailListBean> dataList, OnItemClick onItemClick) {
        this.context = context;
        this.type = type;
        this.dataList = dataList;
        this.onItemClick = onItemClick;
    }

    public void updateData(List<getCatDetailListBean> dataList){
        this.dataList = dataList;
       notifyDataSetChanged();
    }
    public void setShowDelete(boolean showDelete){
       this.showDelete = showDelete;
       notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        // 根据数据返回不同的视图类型
        getCatDetailListBean data = dataList.get(position);
        return TYPE_ONE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_agent, parent, false);
            return new ViewHolderType1(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        getCatDetailListBean data = dataList.get(position);

        holder.itemView.setOnClickListener(v -> {
            if(onItemClick != null){
                onItemClick.onItemClick(position);
            }
        });

        ((ViewHolderType1) holder).bind(data);
    }




    @Override
    public int getItemCount() {
        return dataList.size();
    }

    // ViewHolder for Type 1
    class ViewHolderType1 extends RecyclerView.ViewHolder {
        ImageView iv_cover;
        TextView tv_title;
        TextView tv_des;
        ViewHolderType1(View itemView) {
            super(itemView);
            iv_cover = itemView.findViewById(R.id.iv_cover);
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_des = itemView.findViewById(R.id.tv_des);


        }

        void bind(getCatDetailListBean data) {
            String url = data.getIcon();
            ImageUtil.netRadius(context,url,iv_cover);
            tv_title.setText(data.getModelName());
            tv_des.setText(data.getDescription());

        }
    }

}
