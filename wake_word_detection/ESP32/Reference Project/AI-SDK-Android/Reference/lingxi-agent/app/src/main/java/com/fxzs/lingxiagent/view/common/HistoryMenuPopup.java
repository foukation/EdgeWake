package com.fxzs.lingxiagent.view.common;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.view.user.HistoryItem;

/**
 * 历史记录菜单弹窗
 */
public class HistoryMenuPopup extends PopupWindow {
    
    public interface OnMenuItemClickListener {
        void onViewDetail(HistoryItem item);
        void onRename(HistoryItem item);
        void onDelete(HistoryItem item);
    }
    
    private Context context;
    private HistoryItem historyItem;
    private OnMenuItemClickListener listener;
    
    public HistoryMenuPopup(Context context, HistoryItem item, OnMenuItemClickListener listener) {
        super(context);
        this.context = context;
        this.historyItem = item;
        this.listener = listener;
        
        initPopup();
    }
    
    private void initPopup() {
        // 加载布局
        View contentView = LayoutInflater.from(context).inflate(R.layout.popup_history_menu, null);
        setContentView(contentView);
        
        // 设置弹窗属性
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        
        // 设置动画
        setAnimationStyle(R.style.PopupMenuAnimation);
        
        // 设置点击事件
        setupClickListeners(contentView);
    }
    
    private void setupClickListeners(View contentView) {
        // 查看详情
        View menuViewDetail = contentView.findViewById(R.id.menu_view_detail);
        menuViewDetail.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetail(historyItem);
            }
            dismiss();
        });
        
        // 重命名
        View menuRename = contentView.findViewById(R.id.menu_rename);
        menuRename.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRename(historyItem);
            }
            dismiss();
        });
        
        // 删除
        View menuDelete = contentView.findViewById(R.id.menu_delete);
        menuDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(historyItem);
            }
            dismiss();
        });
    }
    
    /**
     * 显示弹窗
     * @param anchor 锚点视图
     */
    public void showAsDropDown(View anchor) {
        // 测量弹窗内容的尺寸
        getContentView().measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int popupWidth = getContentView().getMeasuredWidth();
        int popupHeight = getContentView().getMeasuredHeight();

        // 获取锚点在屏幕上的位置
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);

        // 获取屏幕尺寸
        android.util.DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        // 计算默认弹窗位置（优先在下方展示）
        int xOffset = -popupWidth + anchor.getWidth() / 2; // 让弹窗右边缘对齐锚点中心
        int yOffset = 0; // 默认贴紧锚点下方

        // 判断上下空间，若下方空间不足则向上展示
        int anchorBottomOnScreen = location[1] + anchor.getHeight();
        int spaceBelow = screenHeight - anchorBottomOnScreen;
        int spaceAbove = location[1];
        if (spaceBelow < popupHeight && spaceAbove >= popupHeight) {
            // 通过负偏移将弹窗移动到锚点上方
            yOffset = -(popupHeight + anchor.getHeight());
        }

        // 检查边界，确保弹窗不会超出屏幕
        if (location[0] + xOffset < 0) {
            xOffset = -location[0] + 8; // 左边距8dp
        }
        if (location[0] + xOffset + popupWidth > screenWidth) {
            xOffset = screenWidth - location[0] - popupWidth - 8; // 右边距8dp
        }
        xOffset = location[0] - popupWidth - 8;

//        showAsDropDown(anchor, xOffset, yOffset);
        showAtLocation(anchor, Gravity.NO_GRAVITY,xOffset,anchorBottomOnScreen);
    }
}
