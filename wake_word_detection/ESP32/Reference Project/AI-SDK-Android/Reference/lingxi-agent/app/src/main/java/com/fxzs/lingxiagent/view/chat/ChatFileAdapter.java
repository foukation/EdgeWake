package com.fxzs.lingxiagent.view.chat;

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
import com.fxzs.lingxiagent.model.chat.callback.OnFileItemClick;
import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean;
import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;
import com.fxzs.lingxiagent.util.ZUtils;

import java.util.ArrayList;
import java.util.List;

public class ChatFileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_FILE = 1;
    public static final int TYPE_IMAGE = 3;
    public static final int TYPE_PROMPT = 2;

    private List<ChatFileBean> dataList = new ArrayList<>();

    private Context context;

    OnFileItemClick onItemClick;
    int type;
    boolean showClose;

    public int selectedPosition = 0;
    boolean isUpload;
    public ChatFileAdapter(Context context, List<ChatFileBean> dataList, int type, OnFileItemClick onItemClick) {
        this.context = context;
        this.dataList = dataList;
        this.type = type;
        this.onItemClick = onItemClick;
    }


    public void setType(int type) {
        this.type = type;
        notifyDataSetChanged();
    }

    public int getType() {
        return type;
    }

    public void setDataList(List<ChatFileBean> dataList) {
        this.dataList = dataList;
        notifyDataSetChanged();
    }
    
    public List<ChatFileBean> getDataList() {
        return dataList;
    }

    public void setShowClose(boolean showClose) {
        this.showClose = showClose;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int res = R.layout.item_chat_quick_prompt;
            if(type == TYPE_FILE){
                res = R.layout.item_chat_file;
            }else if(type == TYPE_IMAGE){
                res = R.layout.item_chat_file_image;
            }else if(type == TYPE_PROMPT){
                res = R.layout.item_chat_quick_prompt;
            }

            View view = LayoutInflater.from(parent.getContext()).inflate(res, parent, false);
            return new ViewHolderType1(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ChatFileBean data = dataList.get(position);

        holder.itemView.setOnClickListener(v -> {
            if(onItemClick != null){
                onItemClick.onItemClick(position);
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
        ImageView iv_progress;
        ImageView iv_file_type;
        TextView tv_name;
        TextView tv_info;
        View root_view;
        View iv_close;
        ViewHolderType1(View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.iv);
            tv_name = itemView.findViewById(R.id.tv_name);
            tv_info = itemView.findViewById(R.id.tv_info);
            root_view = itemView.findViewById(R.id.root_view);
            iv_progress = itemView.findViewById(R.id.iv_progress);
            iv_close = itemView.findViewById(R.id.iv_close);
            iv_file_type = itemView.findViewById(R.id.iv_file_type);
        }

        void bind(ChatFileBean data, int position) {
            ZUtils.print( "文件上传 type: " +type);


            if (type == TYPE_IMAGE){
                if(data.getFileUri() != null){
                    ImageUtil.loadUriRadius(context,data.getFileUri(),iv);
                }else {
                    ImageUtil.netRadius(context,data.getPath(),iv);
                }
                ZUtils.print( "文件上传getPercent: " + data.getPercent());
                if(data.getPercent() >= 100){
                    iv_progress.setVisibility(View.GONE);
                }else {
                    iv_progress.setVisibility(View.VISIBLE);
                }
                setCloseUI(this,position);
            }else if (type == TYPE_FILE){
                tv_name.setText(data.getName());
                tv_info.setText((data.getFileType() == null?"":data.getFileType())+" "+(data.getFileSize() == null ?"":data.getFileSize()));
                setFileTypeImag(iv_file_type,data.getFileType() );

                setCloseUI(this,position);
//                iv.setVisibility(View.GONE);
//                holder.textView.setBackgroundColor(position == selectedPosition ? 0xFFADD8E6 : 0xFFFFFFFF); // 选中项高亮

//                ZUtils.setTextColor(context, tv_name, position == selectedPosition ? R.color.white : R.color.black);
//                ZUtils.setViewBg(context, root_view, position == selectedPosition ?  R.drawable.bg_black_r8 : R.drawable.bg_stoke_e0_r8 );
            }else {
                tv_name.setText(data.getName());

                ZUtils.setTextColor(context,tv_name,isUpload?R.color.text_hint:R.color.account_text_primary);
                ZUtils.setViewBgTint(context,iv,isUpload?R.color.text_hint:R.color.black);
            }


        }
    }

    private void setFileTypeImag(ImageView ivFileType, String fileType) {
        String type = fileType.toUpperCase();
        int res = 0;
        if(type.equals("PDF")){
            res = R.drawable.icon_file_pdf;
        }else if(type.contains("PPT")){
            res = R.drawable.icon_file_ppt;
        }else if(type.equals("WORD")||type.equals("DOC")||type.equals("DOCX")){
            res = R.drawable.icon_file_word;
        }else if(type.equals("EXCEL")||type.contains("XLS")){
            res = R.drawable.icon_file_excel;
        }else if(type.equals("MARKDOWN")){
            res = R.drawable.icon_file_markdown;
        }else if(type.equals("PNG")||type.equals("JPG")||type.equals("JPEG")){
            res = R.drawable.icon_file_img;
        }
        if(res == 0){
            ivFileType.setVisibility(View.INVISIBLE);
        }else {
            ivFileType.setVisibility(View.VISIBLE);
            ZUtils.setIvBg(context,ivFileType,res);
        }
    }

    public void setCloseUI(ViewHolderType1 viewHolder,int position){
        if(showClose){
            viewHolder.iv_close.setVisibility(View.VISIBLE);
            viewHolder.iv_close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dataList.remove(position);
                    notifyItemRemoved(position);
                    if(onItemClick != null){
                        onItemClick.onClose(position);
                    }
                }
            });
        }else {
            viewHolder.iv_close.setVisibility(View.GONE);
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }


    public void setUpload(boolean upload) {
        isUpload = upload;
        notifyDataSetChanged();
    }
}
