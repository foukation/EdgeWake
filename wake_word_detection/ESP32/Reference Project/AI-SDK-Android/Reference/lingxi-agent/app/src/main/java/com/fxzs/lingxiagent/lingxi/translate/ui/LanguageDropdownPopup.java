package com.fxzs.lingxiagent.lingxi.translate.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.translate.model.LanguageOption;

import java.util.ArrayList;
import java.util.List;

/**
 * 语言下拉选择，无蒙层，锚定在触发视图下方
 */
public class LanguageDropdownPopup extends PopupWindow {

    public interface OnSelect {
        void onSelected(LanguageOption option);
    }

    private final View content;
    private final RecyclerView rv;
    private final TextView tvTitle;
    private final Adapter adapter;

    public LanguageDropdownPopup(@NonNull Context context) {
        super(context);
        content = LayoutInflater.from(context).inflate(R.layout.popup_language_dropdown, null);
        setContentView(content);
        // 背景必须非空才能响应 OutsideTouchable，这里用透明色且可去除黑角
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setOutsideTouchable(true);
        setFocusable(true);
        setClippingEnabled(true);
        // 不设置 elevation，避免阴影

        tvTitle = content.findViewById(R.id.tv_title);
        rv = content.findViewById(R.id.rv_languages);
        rv.setLayoutManager(new LinearLayoutManager(context));
        adapter = new Adapter();
        rv.setAdapter(adapter);

        // 默认宽高由内容决定，可在 showBelow 时与锚点同宽
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public void setTitle(String title){
        tvTitle.setText(title);
    }

    public void setData(List<LanguageOption> list){
        adapter.setData(list);
    }

    public void setOnSelect(OnSelect onSelect){
        adapter.onSelect = onSelect;
    }

    public void showBelow(View anchor){
        // 宽度与锚点一致，位置在其正下方
//        setWidth(anchor.getWidth());
        showAsDropDown(anchor, 0, 8);
    }

    private static class Adapter extends RecyclerView.Adapter<VH>{
        private final List<LanguageOption> data = new ArrayList<>();
        private OnSelect onSelect;

        void setData(List<LanguageOption> list){
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_translate_language_option, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            LanguageOption op = data.get(pos);
            h.name.setText(op.getName());
            // 不显示 code/zh
            h.desc.setVisibility(View.GONE);
            h.radio.setSelected(op.isSelected());
            h.itemView.setOnClickListener(v -> {
                for (LanguageOption o : data) o.setSelected(false);
                op.setSelected(true);
                notifyDataSetChanged();
                if (onSelect != null) onSelect.onSelected(op);
            });
        }

        @Override public int getItemCount() { return data.size(); }
    }

    private static class VH extends RecyclerView.ViewHolder{
        TextView name, desc; View radio;
        VH(@NonNull View itemView){
            super(itemView);
            name = itemView.findViewById(R.id.tv_language_name);
            desc = itemView.findViewById(R.id.tv_language_desc);
            radio = itemView.findViewById(R.id.radio_button);
        }
    }
}

