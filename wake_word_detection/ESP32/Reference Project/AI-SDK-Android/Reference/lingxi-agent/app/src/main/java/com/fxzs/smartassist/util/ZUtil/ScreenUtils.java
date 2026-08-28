package com.fxzs.smartassist.util.ZUtil;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.LruCache;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ScreenUtils {

    /**
     * 获取状态栏高度
     */
    public static int getStatusBarHeight() {
        Resources resources = Resources.getSystem();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        return resources.getDimensionPixelSize(resourceId);
    }

    /**
     * 获取底部导航栏高度
     */
    public static int getNavBarHeight() {
        Resources resources = Resources.getSystem();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        return resourceId != 0 ? resources.getDimensionPixelSize(resourceId) : 0;
    }

    /**
     * 获取屏幕宽度
     */
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return -1;
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wm.getDefaultDisplay().getRealSize(point);
        } else {
            wm.getDefaultDisplay().getSize(point);
        }
        return point.x;
    }

    /**
     * 获取屏幕高度
     */
    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return -1;
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wm.getDefaultDisplay().getRealSize(point);
        } else {
            wm.getDefaultDisplay().getSize(point);
        }
        return point.y;
    }

    /**
     * 设置横屏
     */
    public static void setLandscape(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    /**
     * 设置竖屏
     */
    public static void setPortrait(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    /**
     * 截屏（除去导航栏）
     */
    public static Bitmap captureScreen(Activity activity, Bitmap.Config config) {
        return captureView(activity.getWindow().getDecorView(), config);
    }

    /**
     * 截取View的图片
     */
    public static Bitmap captureView(View view, Bitmap.Config bitmapConfig) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        view.measure(
                View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(view.getHeight(), View.MeasureSpec.EXACTLY)
        );
        view.layout(
                (int) view.getX(), (int) view.getY(),
                (int) view.getX() + view.getMeasuredWidth(),
                (int) view.getY() + view.getMeasuredHeight()
        );

        Bitmap bitmap = null;
        if (view.getDrawingCache() != null && view.getMeasuredWidth() > 0 && view.getMeasuredHeight() > 0) {
            bitmap = Bitmap.createBitmap(view.getDrawingCache(), 0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        }

        view.setDrawingCacheEnabled(false);
        view.destroyDrawingCache();

        if (bitmap == null && view.getMeasuredWidth() > 0 && view.getMeasuredHeight() > 0) {
            bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), bitmapConfig);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
        }

        return bitmap;
    }

    /**
     * 截取ScrollView
     */
    public static Bitmap captureScrollView(ScrollView scrollView) {
        int h = 0;
        for (int i = 0; i < scrollView.getChildCount(); i++) {
            View child = scrollView.getChildAt(i);
            h += child.getHeight();
            child.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }

        Bitmap bitmap = Bitmap.createBitmap(scrollView.getWidth(), h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        scrollView.draw(canvas);
        return bitmap;
    }

    /**
     * 截取LinearLayout
     */
    public static Bitmap captureLinearLayout(LinearLayout layout) {
        int h = 0;
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            h += child.getHeight();
            child.setBackgroundColor(Color.RED);
        }

        Bitmap bitmap = Bitmap.createBitmap(layout.getWidth(), h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        layout.draw(canvas);
        return bitmap;
    }

    /**
     * 截取ListView
     */
    public static Bitmap captureListView(ListView listView) {
        int itemCount = listView.getAdapter().getCount();
        int totalHeight = 0;
        ArrayList<Bitmap> bitmaps = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            View itemView = listView.getAdapter().getView(i, null, listView);
            itemView.measure(
                    View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            itemView.layout(0, 0, itemView.getMeasuredWidth(), itemView.getMeasuredHeight());
            itemView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            itemView.setDrawingCacheEnabled(true);
            itemView.buildDrawingCache();
            bitmaps.add(itemView.getDrawingCache());
            totalHeight += itemView.getMeasuredHeight();
        }

        Bitmap bigBitmap = Bitmap.createBitmap(listView.getMeasuredWidth(), totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bigBitmap);
        Paint paint = new Paint();
        float iHeight = 0;

        for (Bitmap b : bitmaps) {
            canvas.drawBitmap(b, 0, iHeight, paint);
            iHeight += b.getHeight();
            b.recycle();
        }

        return bigBitmap;
    }

    /**
     * 截取RecyclerView
     */
    public static Bitmap captureRecyclerView(RecyclerView recyclerView) {
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        if (adapter == null) return null;

        int size = adapter.getItemCount();
        int totalHeight = 0;
        Paint paint = new Paint();
        int iHeight = 0;

        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        LruCache<String, Bitmap> bitmapCache = new LruCache<>(cacheSize);

        for (int i = 0; i < size; i++) {
            RecyclerView.ViewHolder holder = adapter.createViewHolder(recyclerView, adapter.getItemViewType(i));
            adapter.onBindViewHolder(holder, i);

            View itemView = holder.itemView;
            itemView.measure(
                    View.MeasureSpec.makeMeasureSpec(recyclerView.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            itemView.layout(0, 0, itemView.getMeasuredWidth(), itemView.getMeasuredHeight());
            itemView.setBackgroundColor(Color.WHITE);
            itemView.setDrawingCacheEnabled(true);
            itemView.buildDrawingCache();

            Bitmap drawingCache = itemView.getDrawingCache();
            if (drawingCache != null) {
                bitmapCache.put(String.valueOf(i), drawingCache);
                totalHeight += itemView.getMeasuredHeight();
            }
        }

        Bitmap bigBitmap = Bitmap.createBitmap(recyclerView.getMeasuredWidth(), totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bigBitmap);
        if (recyclerView.getBackground() instanceof ColorDrawable) {
            canvas.drawColor(((ColorDrawable) recyclerView.getBackground()).getColor());
        }

        for (int i = 0; i < size; i++) {
            Bitmap bmp = bitmapCache.get(String.valueOf(i));
            if (bmp != null) {
                canvas.drawBitmap(bmp, 0f, iHeight, paint);
                iHeight += bmp.getHeight();
                bmp.recycle();
            }
        }

        return bigBitmap;
    }

    /**
     * 设置沉浸式状态栏
     */
    public static void immersiveShow(Activity activity) {
        Window window = activity.getWindow();

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        window.setStatusBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }

        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
}

