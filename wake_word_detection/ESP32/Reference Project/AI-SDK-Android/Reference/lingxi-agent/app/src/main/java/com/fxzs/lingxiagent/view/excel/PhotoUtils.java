package com.fxzs.lingxiagent.view.excel;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import java.io.File;

public class PhotoUtils {
    public static final int RESULT_CODE_PHOTO = 999;
    public static final int RESULT_CODE_CAMERA = 888;
    public static final int FILE_SELECTOR_CODE = 777;
    public static String PATH_PHOTO;

    public static void startAlbum(Activity context) {
        Intent albumIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        albumIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);//允许多选
        albumIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        albumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        context.startActivityForResult(albumIntent, RESULT_CODE_PHOTO);
    }



    /**
     * 拍照
     * @param context Activity
     */
    public static void startCamera(Activity context) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        PATH_PHOTO = getSdCardDirectory(context) + "/temp.png";
        File temp = new File(PATH_PHOTO);
        if (!temp.getParentFile().exists()) {
            temp.getParentFile().mkdirs();
        }
        if (temp.exists()) {
            temp.delete();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // 通过FileProvider创建一个content类型的Uri
            Uri uri =
                    FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", temp);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        } else {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(temp));
        }
        context.startActivityForResult(intent, RESULT_CODE_CAMERA);
    }
    public static String getSdCardDirectory(Context context){
        File sdDir = null;
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            sdDir = Environment.getExternalStorageDirectory();
//            sdDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        } else {
            sdDir = context.getCacheDir();
        }
        File cacheDir = new File(sdDir, "h5pic");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return cacheDir.getPath();
    }


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(Context context, Uri uri) {
        boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;


        Log.e("TAG","isKitKat = "+isKitKat+"getPath = getAuthority = "+uri.getAuthority());

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
//                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                String type = split[0];
                if ("primary".equals(type)) {
                    return Environment.getExternalStorageDirectory().getPath() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                String id = DocumentsContract.getDocumentId(uri);
                Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id)
                );
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
//                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                Uri contentUri;
                switch (split[0]){
                    case "image":
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        break;
                    case "video":
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        break;
                    case "audio":
                        contentUri =  MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        break;
                    default:
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        break;
                }


                String selection = "_id=?";
                String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equals(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
//            return "";
        } else if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals( uri.getAuthority());
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals( uri.getAuthority());
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals( uri.getAuthority())
                /*||  uri.getAuthority().contains("media")*/;
    }

    private static String getDataColumn(
            Context context,
            Uri uri,
            String selection,
            String[] selectionArgs
    ) {
        Cursor cursor = null;
        String column = "_data";
        String[] projection = new String[]{column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIndex);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }



    /**
     * 打开本地文件器
     */
    public static void openFileSelector(Activity context) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                // PDF
                "application/pdf",

                // Word
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",

                // Excel
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",

                // PPT
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/mspowerpoint",
                "application/powerpoint",
                "application/vnd.ms-office",

                // 文本
                "text/plain",
//                "text/csv",
                "text/markdown",

                // 兜底
                "application/octet-stream"
        });
        context.startActivityForResult(intent, FILE_SELECTOR_CODE);
    }
}
