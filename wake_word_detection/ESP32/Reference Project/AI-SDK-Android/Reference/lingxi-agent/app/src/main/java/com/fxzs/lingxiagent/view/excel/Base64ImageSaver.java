package com.fxzs.lingxiagent.view.excel;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.core.app.NotificationCompat;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.ZUtils;

import java.io.OutputStream;

public class Base64ImageSaver {
    private static final String DOWNLOAD_CHANNEL_ID = "download_channel";
    private static final String DOWNLOAD_CHANNEL_NAME = "下载";

    public static void saveBase64ImageToAlbum(Context context, String base64String) {

        ZUtils.showToast("开始下载");
        // 1. 去除 data URI 前缀
        if (base64String.startsWith("data:image")) {
            int commaIndex = base64String.indexOf(",");
            if (commaIndex != -1) {
                base64String = base64String.substring(commaIndex + 1);
            }
        }

        try {
            // 2. Base64 解码
            byte[] bytes = Base64.decode(base64String, Base64.DEFAULT);

            // 3. 转为 Bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap == null) {
//                Toast.makeText(context, "图片解码失败", Toast.LENGTH_SHORT).show();
                ZUtils.showToast("图片解码失败");
                return;
            }

            // 4. 保存到相册
            String fileName = "IMG_" + System.currentTimeMillis() + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/lingxi"); // 保存到 Pictures/MyApp 文件夹

            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
//                    Toast.makeText(context, "图片保存成功", Toast.LENGTH_SHORT).show();
                    ZUtils.showToast("图片保存成功");
                    showImageDownloadFinishedNotification(context, uri, fileName);
                }
            }

            // 可选：回收 bitmap
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }

        } catch (Exception e) {
            e.printStackTrace();
//            Toast.makeText(context, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            ZUtils.showToast("保存失败: " + e.getMessage());
        }
    }

    private static void showImageDownloadFinishedNotification(Context context, Uri imageUri, String fileName) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    DOWNLOAD_CHANNEL_ID,
                    DOWNLOAD_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent openImageIntent = new Intent(Intent.ACTION_VIEW);
        openImageIntent.setDataAndType(imageUri, "image/*");
        openImageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openImageIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) (System.currentTimeMillis() & 0x7fffffff),
                openImageIntent,
                flags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("图片下载完成")
                .setContentText(fileName)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify((int) (System.currentTimeMillis() & 0x7fffffff), builder.build());
    }
}
