package com.fxzs.lingxiagent.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.ZDpUtils;
import com.fxzs.lingxiagent.util.ZUtils;

import timber.log.Timber;

public class TextSelectorView extends AppCompatTextView {
    private Paint selectionPaint;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private boolean isSelecting = false;
    private OnSelectionClickListener listener;
    private int startHandleResId = R.mipmap.selection_handle_start;
    private int endHandleResId = R.mipmap.selection_handle_end;
    private int handleWidth = ZDpUtils.dpToPx2(getContext(), 6);
    private int handleHeight = ZDpUtils.dpToPx2(getContext(), 24);
    private boolean isInSelectionMode = false;
    private boolean isSelectionEnabled = false;
    private WindowManager windowManager;
    private HandleManager handleManager;

    public interface OnSelectionClickListener {
        void onSelectVoice(String text);
    }

    public TextSelectorView(Context context) {
        super(context);
        init();
    }

    public TextSelectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        selectionPaint = new Paint();
        selectionPaint.setColor(Color.argb(100, 64, 128, 255));
        setTextIsSelectable(false);
        setWillNotDraw(false);

        windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        handleManager = new HandleManager();
        setupScrollListener();
        setLongClickable(false);
        setTextIsSelectable(false);
        setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) { return false; }
            @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
            @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) { return false; }
            @Override public void onDestroyActionMode(ActionMode mode) {}
        });


    }

    private void setupScrollListener() {
        getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (handleManager != null && handleManager.isHandlesShowing()) {
                handleManager.updateHandlePositions();
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (hasSelectable()) {
            Layout layout = getLayout();
            int startLine = layout.getLineForOffset(selectionStart);
            int endLine = layout.getLineForOffset(selectionEnd);

            for (int i = startLine; i <= endLine; i++) {
                float left = i == startLine ?
                        layout.getPrimaryHorizontal(selectionStart) + getPaddingLeft() : getPaddingLeft();
                float right = i == endLine ?
                        layout.getPrimaryHorizontal(selectionEnd) + getPaddingLeft() : getWidth() - getPaddingRight();

                float top = layout.getLineTop(i) + getPaddingTop();
                float bottom = layout.getLineBottom(i) + getPaddingTop();

                canvas.drawRect(left, top, right, bottom, selectionPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isSelectionEnabled) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (handleManager.checkHandleTouch(event)) {
                    disableParentScroll(true);
                    return true;
                }

                startNewSelection(event.getX(), event.getY());
                return true;

            case MotionEvent.ACTION_MOVE:
                if (handleManager.isDragging()) {
                    handleManager.updateDraggingHandle(event);
                    updateSelectionFromHandles();
                } else if (isSelecting) {
                    updateSelectionEnd(event.getX(), event.getY());
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean wasDragging = handleManager.isDragging();
                handleManager.endDrag();
                isSelecting = false;
                disableParentScroll(false);

                if (hasSelectable()) {
                    if (wasDragging) {
                        post(() -> handleManager.updateHandlePositions());
                    }
                    showMenu();
                }
                return true;
        }

        return isInSelectionMode || super.onTouchEvent(event);
    }

    private void startNewSelection(float x, float y) {
        clearSelection();
        selectionStart = getOffset(x, y);
        selectionEnd = selectionStart;
        isSelecting = true;

        handleManager.showHandles();
        handleManager.updateHandlePositions();

        disableParentScroll(true);
    }

    private void updateSelectionEnd(float x, float y) {
        selectionEnd = getOffset(x, y);
        adjustSelectionOrder();
        handleManager.updateHandlePositions();
        invalidate();
    }

    private void updateSelectionFromHandles() {
        int[] location = new int[2];
        getLocationOnScreen(location);

        if (handleManager.startHandle.isDragging) {
            // 计算相对于TextView的坐标
            float relativeX = handleManager.startHandle.handleParams.x - location[0] + handleWidth/2 - getPaddingLeft();
            float relativeY = handleManager.startHandle.handleParams.y - location[1] + handleHeight - getPaddingTop();
            selectionStart = getOffset(relativeX, relativeY);
        }

        if (handleManager.endHandle.isDragging) {
            float relativeX = handleManager.endHandle.handleParams.x - location[0] + handleWidth/2 - getPaddingLeft();
            float relativeY = handleManager.endHandle.handleParams.y - location[1] + handleHeight - getPaddingTop();
            selectionEnd = getOffset(relativeX, relativeY);
        }

        adjustSelectionOrder();
        invalidate();

        post(() -> {
            if (!handleManager.isDragging()) {
                handleManager.updateHandlePositions();
            }
        });
    }

    private int getOffset(float x, float y) {
        Layout layout = getLayout();
        x -= getPaddingLeft();
        y -= getPaddingTop();
        int line = layout.getLineForVertical((int)y);
        return Math.max(0, Math.min(layout.getOffsetForHorizontal(line, x), getText().length()));
    }

    private void adjustSelectionOrder() {
        if (selectionStart > selectionEnd) {
            int temp = selectionStart;
            selectionStart = selectionEnd;
            selectionEnd = temp;
        }
    }

    private boolean hasSelectable() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }

    public void setSelectionEnabled(boolean enabled) {
        this.isSelectionEnabled = enabled;
        if (enabled) {
            selectAllText();
        } else {
            clearSelection();
        }
    }

    private void selectAllText() {
        if (getText().length() == 0) return;

        selectionStart = 0;
        selectionEnd = getText().length();
        isSelecting = false;

        handleManager.showHandles();
        handleManager.updateHandlePositions();

        post(() -> {
            showMenu();
        });

        invalidate();
    }

    public void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
        handleManager.hideHandles();
        invalidate();
    }

    private void disableParentScroll(boolean disable) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disable);
            isInSelectionMode = disable;
        }
    }

    private void showMenu() {
        Dialog menuDialog = new Dialog(getContext());
        menuDialog.setContentView(R.layout.text_select_dialog);

        TextView ivCopy = menuDialog.findViewById(R.id.tv_copy);

        Layout layout = getLayout();
        int middle = (selectionStart + selectionEnd) / 2;
        int line = layout.getLineForOffset(middle);
        float x = layout.getPrimaryHorizontal(middle) + getPaddingLeft();
        float y = layout.getLineTop(line) + getPaddingTop();

        int[] location = new int[2];
        getLocationOnScreen(location);

        Window window = menuDialog.getWindow();
        if (window != null) {
            // 设置窗口背景为透明
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // 去除窗口阴影和边框
            window.setDimAmount(0f); // 去除周围变暗效果
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = (int)(x + location[0] - ivCopy.getWidth()/2);
            params.y = (int)(y + location[1] - ivCopy.getHeight());
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ivCopy.setOnClickListener(v -> {
            ZUtils.copy(getContext(), getSelectedText());
            clearSelection();
            menuDialog.dismiss();
        });

        menuDialog.findViewById(R.id.tv_voice).setOnClickListener(v -> {
            listener.onSelectVoice(getSelectedText());
            clearSelection();
            menuDialog.dismiss();
        });

        menuDialog.show();

        menuDialog.setCanceledOnTouchOutside(true);
    }

    public String getSelectedText() {
        if (!hasSelectable()) return "";
        return getText().subSequence(selectionStart, selectionEnd).toString();
    }

    public void setOnSelectionClickListener(OnSelectionClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (hasSelectable() && !handleManager.isHandlesShowing()) {
            handleManager.showHandles();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handleManager.hideHandles();
        try {
            clearFocus();
            // 关闭浮动菜单，防止 PopupWindow 泄漏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cancelLongPress();
                destroyDrawingCache();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class HandleManager {
        SelectionHandleView startHandle;
        SelectionHandleView endHandle;
        private boolean handlesShowing = false;

        void showHandles() {
            if (!isSelectionEnabled || !isAttachedToWindow()) return;

            if (startHandle == null) {
                startHandle = new SelectionHandleView(getContext(), true);
            }
            if (endHandle == null) {
                endHandle = new SelectionHandleView(getContext(), false);
            }

            updateHandlePositions();
            handlesShowing = true;
        }

        void hideHandles() {
            if (startHandle != null) {
                startHandle.removeFromWindow();
                startHandle = null;
            }
            if (endHandle != null) {
                endHandle.removeFromWindow();
                endHandle = null;
            }
            handlesShowing = false;
        }

        boolean isHandlesShowing() {
            return handlesShowing;
        }

        boolean checkHandleTouch(MotionEvent event) {
            float x = event.getRawX();
            float y = event.getRawY();

            if (startHandle != null && startHandle.isInTouchArea(x, y)) {
                startHandle.startDrag();
                return true;
            }

            if (endHandle != null && endHandle.isInTouchArea(x, y)) {
                endHandle.startDrag();
                return true;
            }

            setSelectionEnabled(false);

            return false;
        }

        void updateDraggingHandle(MotionEvent event) {
            if (startHandle != null && startHandle.isDragging) {
                startHandle.updatePosition(
                        event.getRawX() - handleWidth/2,
                        event.getRawY() - handleHeight
                );
            } else if (endHandle != null && endHandle.isDragging) {
                endHandle.updatePosition(
                        event.getRawX() - handleWidth/2,
                        event.getRawY() - handleHeight
                );
            }
        }

        boolean isDragging() {
            return (startHandle != null && startHandle.isDragging) ||
                    (endHandle != null && endHandle.isDragging);
        }

        void endDrag() {
            if (startHandle != null) startHandle.endDrag();
            if (endHandle != null) endHandle.endDrag();
        }

        void updateHandlePositions() {
            if (!handlesShowing || getLayout() == null) return;

            int[] location = new int[2];
            getLocationOnScreen(location);

            updateSingleHandlePosition(startHandle, selectionStart, location);
            updateSingleHandlePosition(endHandle, selectionEnd, location);
        }

        private void updateSingleHandlePosition(SelectionHandleView handle, int offset, int[] parentLocation) {
            if (handle == null) return;

            Layout layout = getLayout();
            int line = layout.getLineForOffset(offset);
            float x = layout.getPrimaryHorizontal(offset);
            float y = layout.getLineBottom(line);

            x += getPaddingLeft();
            y += getPaddingTop();

            // 转换为全局坐标
            float globalX = x + parentLocation[0] - handleWidth/2;
            float globalY = y + parentLocation[1] - handleHeight;

            // 如果正在拖动则不强制更新位置
            if (!handle.isDragging) {
                handle.updatePosition(globalX, globalY);
            }
        }
    }

    private class SelectionHandleView extends View {
        boolean isDragging = false;
        boolean isStartHandle;
        WindowManager.LayoutParams handleParams;
        Rect touchArea = new Rect();

        public SelectionHandleView(Context context, boolean isStartHandle) {
            super(context);
            this.isStartHandle = isStartHandle;
            setBackgroundResource(isStartHandle ? startHandleResId : endHandleResId);

            handleParams = new WindowManager.LayoutParams(
                    handleWidth,
                    handleHeight,
                    WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );
            handleParams.gravity = Gravity.TOP | Gravity.START;

            windowManager.addView(this, handleParams);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(handleWidth, handleHeight);
        }

        void updatePosition(float x, float y) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int screenWidth = metrics.widthPixels;
            int screenHeight = metrics.heightPixels;

            x = Math.max(0, Math.min(x, screenWidth - handleWidth));
            y = Math.max(0, Math.min(y, screenHeight - handleHeight));

            handleParams.x = (int)x;
            handleParams.y = (int)y;

            try {
                windowManager.updateViewLayout(this, handleParams);
            } catch (Exception e) {
                Timber.tag("TextSelectorView").e( "Update handle position failed"+e);
            }

            updateTouchArea();
        }

        private void updateTouchArea() {
            int touchPadding = ZDpUtils.dpToPx2(getContext(), 25);
            touchArea.set(
                    (int)(handleParams.x - handleWidth - touchPadding),
                    (int)(handleParams.y - handleHeight - touchPadding/2),
                    (int)(handleParams.x + handleWidth  + touchPadding),
                    (int)(handleParams.y + handleHeight + touchPadding/2)
            );
        }

        boolean isInTouchArea(float x, float y) {
            return touchArea.contains((int)x, (int)y);
        }

        void startDrag() {
            isDragging = true;
        }

        void endDrag() {
            isDragging = false;
        }

        void removeFromWindow() {
            try {
                windowManager.removeView(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        // 拦截“粘贴”命令
        if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) {
            return false;
        }
        return super.onTextContextMenuItem(id);
    }
}