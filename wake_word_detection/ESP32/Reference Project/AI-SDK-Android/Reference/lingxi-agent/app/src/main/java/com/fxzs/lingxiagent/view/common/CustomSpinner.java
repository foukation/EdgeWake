package com.fxzs.lingxiagent.view.common;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.smartassist.model.chat.dto.SpinnerPopItem;
import com.fxzs.smartassist.util.ZUtil.SizeUtils;

import java.util.ArrayList;
import java.util.List;

public class CustomSpinner extends RelativeLayout {
    private PopupWindow mPopupWindow;
    private RelativeLayout mSpinnerLayout;
    private ImageView mSpinnerImg;
    private TextView mSpinnerText;
    private List<SpinnerPopItem> mSpinnerPopItems = new ArrayList<>();
    private static final int POPUP_HEIGHT = 242;
    private static final int POPUP_WIDTH = 152;
    private static final int POPUP_X_DISTANCE = 10;
    private static final int POPUP_Y_DISTANCE = 37;
    private int mSelectItemPosition = -1;

    // 构造方法
    public CustomSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // 加载自定义属性
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Custom_Tab_Spinner);
        String spinnerText = typedArray.getString(R.styleable.Custom_Tab_Spinner_spinner_text);
        int spinnerColor = typedArray.getColor(R.styleable.Custom_Tab_Spinner_spinner_textcolor, getResources().getColor(R.color.color_1E1E1E));
        Drawable spinnerImg = typedArray.getDrawable(R.styleable.Custom_Tab_Spinner_spinner_img);

        // 解析条目数据
        int entriesResId = typedArray.getResourceId(
                R.styleable.Custom_Tab_Spinner_spinner_entries,
                0
        );

        if (entriesResId != 0) {
            String[] entries = context.getResources().getStringArray(entriesResId);

            for (String entry : entries) {
                SpinnerPopItem item = new SpinnerPopItem(entry);
                mSpinnerPopItems.add(item);
            }
        }
        if(mSpinnerPopItems.isEmpty()) {
            String[] optionTitles = getContext().getResources()
                    .getStringArray(R.array.fy_select);
            for (String optionTitle : optionTitles) {
                SpinnerPopItem item = new SpinnerPopItem(optionTitle);
                mSpinnerPopItems.add(item);
            }
        }
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.custom_spinner_layout, this, true);
        mSpinnerLayout = findViewById(R.id.custom_spinner_layout);
        // 设置点击事件
        mSpinnerLayout.setOnClickListener(v -> showDropDown());

        mSpinnerText = findViewById(R.id.custom_spinner_text);
        if(mSelectItemPosition != -1) {
            mSpinnerText.setText(mSpinnerPopItems.get(mSelectItemPosition).getName());
        } else {
            mSpinnerText.setText(spinnerText);
        }
        mSpinnerText.setTextColor(spinnerColor);
        mSpinnerImg = findViewById(R.id.custom_spinner_img);
        mSpinnerImg.setImageDrawable(spinnerImg);

        typedArray.recycle();
    }

    // 显示下拉菜单
    private void showDropDown() {
        View popupView = LayoutInflater.from(getContext())
                .inflate(R.layout.popup_spinner_options, null);

        int viewWidth = SizeUtils.dpToPx(POPUP_WIDTH);
        int viewHeight =  SizeUtils.dpToPx(POPUP_HEIGHT);
        mPopupWindow = new PopupWindow(
                popupView,
                viewWidth,
                viewHeight,
                true
        );
        mPopupWindow.setOutsideTouchable(true);

        RecyclerView rvOptions = popupView.findViewById(R.id.spinner_option);
        rvOptions.setLayoutManager(new LinearLayoutManager(getContext()));

        SpinnerPopAdapter spinnerPopAdapter = new SpinnerPopAdapter(mSpinnerPopItems,
                position -> {
                    mPopupWindow.dismiss();
                    handleOptionClick(position);
                });
        spinnerPopAdapter.setSelectPosition(mSelectItemPosition);
        rvOptions.setAdapter(spinnerPopAdapter);
        rvOptions.scrollToPosition(mSelectItemPosition);
        int xOffset =  SizeUtils.dpToPx( POPUP_X_DISTANCE);
        int yOffset =  SizeUtils.dpToPx(POPUP_HEIGHT + POPUP_Y_DISTANCE);
        mPopupWindow.showAsDropDown(mSpinnerLayout, -xOffset, -yOffset);
    }

    private OnItemSelectListener mListener;
    public interface OnItemSelectListener {
        void onItemSelect(int position);
    }

    public void setItemSelectListener(OnItemSelectListener listener) {
        mListener = listener;
    }

    public void setSelectPosition(int position) {
        handleOptionClick(position);
    }

    public int getSelectedItemPosition(){
        return mSelectItemPosition;
    }

    private void handleOptionClick(int position) {
        mSelectItemPosition = position;
        mSpinnerText.setText(mSpinnerPopItems.get(position).getName());
        if(mListener != null) {
            mListener.onItemSelect(position);
        }
    }

    // 关闭下拉菜单
    private void dismissDropDown() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    public String getSelectedItem(){
        if(mSpinnerPopItems.size() > mSelectItemPosition) {
            return mSpinnerPopItems.get(mSelectItemPosition).getName();
        }
        return "";
    }
}

class SpinnerPopAdapter extends RecyclerView.Adapter<SpinnerPopAdapter.ViewHolder> {

    private List<SpinnerPopItem> mDatasList;
    private OnItemClickListener mListener;
    private int mSelectPosition;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public SpinnerPopAdapter(List<SpinnerPopItem> list, OnItemClickListener listener) {
        mDatasList = list;
        mListener = listener;
    }

    public void setSelectPosition(int position) {
        mSelectPosition = position;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.spinner_pop_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SpinnerPopItem option = mDatasList.get(position);
        holder.bind(option, mSelectPosition == position);

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatasList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.spinner_item_icon);
            textView = itemView.findViewById(R.id.spinner_item_text);
        }

        public void bind(SpinnerPopItem option, boolean select) {
            if(select) {
                imageView.setVisibility(VISIBLE);
            } else {
                imageView.setVisibility(GONE);
            }
            textView.setText(option.getName());
        }
    }
}
