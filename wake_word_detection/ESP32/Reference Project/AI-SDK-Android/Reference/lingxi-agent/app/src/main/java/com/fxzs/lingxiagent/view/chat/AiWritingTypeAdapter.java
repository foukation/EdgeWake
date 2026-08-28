package com.fxzs.lingxiagent.view.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.callback.OnItemClickAncor;
import com.fxzs.lingxiagent.model.chat.dto.AiWritingTypeBean;
import com.fxzs.lingxiagent.util.ZUtils;

import java.util.ArrayList;
import java.util.List;

public class AiWritingTypeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_ONE = 1;
    public static final int TYPE_TWO = 2;

    private List<AiWritingTypeBean> dataList = new ArrayList<>();

    private Context context;
    private boolean showDelete = false;
    OnItemClickAncor onItemClick;
    int type;

    public int selectedPosition = -1;
    public AiWritingTypeAdapter(Context context, List<AiWritingTypeBean> dataList,int type, OnItemClickAncor onItemClick) {
        this.context = context;
        this.dataList = dataList;
        this.type = type;
        this.onItemClick = onItemClick;
    }


    public void clearAllSubName(){
        for (AiWritingTypeBean bean : dataList) {
            bean.setSubName("");
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_writing_type, parent, false);
            return new ViewHolderType1(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        AiWritingTypeBean data = dataList.get(position);

        holder.itemView.setOnClickListener(v -> {
            if(onItemClick != null){
                onItemClick.onItemClick(position,v);
            }
            selectedPosition = position; // 更新选中位置
            notifyDataSetChanged();
        });

        ((ViewHolderType1) holder).bind(data,position);
    }




    @Override
    public int getItemCount() {
        return dataList.size();
    }

    // ViewHolder for Type 1
    class ViewHolderType1 extends RecyclerView.ViewHolder {
        ImageView iv;
        TextView tv_name;
        View root_view;
        ViewHolderType1(View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.iv);
            tv_name = itemView.findViewById(R.id.tv_name);
            root_view = itemView.findViewById(R.id.root_view);
        }

        void bind(AiWritingTypeBean data,int position) {

            tv_name.setText(data.getName());
            if (type == TYPE_ONE){
                iv.setVisibility(View.GONE);
//                holder.textView.setBackgroundColor(position == selectedPosition ? 0xFFADD8E6 : 0xFFFFFFFF); // 选中项高亮

                ZUtils.setTextColor(context, tv_name, position == selectedPosition ? R.color.white : R.color.black);
                ZUtils.setViewBg(context, root_view, position == selectedPosition ?  R.drawable.bg_black_r8 : R.drawable.bg_stoke_e0_r8 );
            }else {
                iv.setVisibility(View.VISIBLE);

                if(!TextUtils.isEmpty(data.getSubName())){

                    tv_name.setText(data.getName()+" "+data.getSubName());
                }
            }


        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }
}
