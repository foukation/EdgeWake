package com.fxzs.lingxiagent.util;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import timber.log.Timber;

public class AdapterCaptureHelper {

    public interface CaptureCallback {
        void onSuccess(Bitmap bitmap);
        void onFailed(String message);
    }

    /**
     * 通过Adapter生成长图
     * @param context 上下文
     * @param itemViews item view
     * @param callback 回调接口
     */
    public static void captureFromAdapter(Context context,
                                          List<View> itemViews,
                                          CaptureCallback callback) {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {
                try {
                    return captureAdapter(context, itemViews);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    callback.onSuccess(bitmap);
                } else {
                    callback.onFailed("Failed to capture from adapter");
                }
            }
        }.execute();
    }

    private static Bitmap captureAdapter(Context context, List<View> itemViews) {
        if (itemViews == null || itemViews.size() == 0) {
            return null;
        }

        int totalHeight = 0;
        for (View view : itemViews) {
            totalHeight += view.getMeasuredHeight() + ZDpUtils.dpToPx2(context, 16);
        }
        totalHeight += ZDpUtils.dpToPx2(context, 50);

        int targetWidth = context.getResources().getDisplayMetrics().widthPixels;

        Bitmap bitmap = Bitmap.createBitmap(targetWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        int currentHeight = 0;
        for (int i = 0; i < itemViews.size(); i++) {
            if (i == itemViews.size() -1) {
                currentHeight += ZDpUtils.dpToPx2(context, 50);
            }
            canvas.save();
            if (i != 0 && (i != itemViews.size() -1)) {
                canvas.translate(ZDpUtils.dpToPx2(context, 34) / 2, currentHeight);
            } else {
                canvas.translate(0, currentHeight);
            }
            itemViews.get(i).draw(canvas);
            canvas.restore();

            currentHeight += itemViews.get(i).getMeasuredHeight() + ZDpUtils.dpToPx2(context, 16);
        }

        return bitmap;
    }

    public static void saveBitmap(Context context, Bitmap bitmap, String fileName) {
        if (bitmap == null) {
            Toast.makeText(context, "图片无效", Toast.LENGTH_SHORT);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageViaMediaStore(context, bitmap, fileName);
            return;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "请授予存储权限", Toast.LENGTH_SHORT);
            return;
        }

        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File appDir = new File(picturesDir, context.getPackageName());
        if (!appDir.exists() && !appDir.mkdirs()) {
            Toast.makeText(context, "无法创建目录", Toast.LENGTH_SHORT);
            return;
        }

        File imageFile = new File(appDir, fileName);
        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                MediaScannerConnection.scanFile(
                        context,
                        new String[]{imageFile.getAbsolutePath()},
                        new String[]{"image/png"},
                        null
                );
                Toast.makeText(context, "图片已保存至相册", Toast.LENGTH_SHORT);
            }
        } catch (IOException e) {
            Toast.makeText(context, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static void saveImageViaMediaStore(Context context, Bitmap bitmap, String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES +
                File.separator + context.getPackageName());

        try {
            Uri uri = context.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                    if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        Toast.makeText(context, "图片已保存至相册", Toast.LENGTH_SHORT);
                    }
                }
            }
        } catch (IOException e) {
            Timber.tag("SaveImage").e( "保存失败"+e);
            Toast.makeText(context, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT);
        }
    }
}