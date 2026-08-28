package com.fxzs.lingxiagent.util.ZUtil;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean;
import com.fxzs.lingxiagent.util.ZUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtils {

    public static File uriToFile(Uri uri, Context context, ChatFileBean bean) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) return null;

            // 获取文件名和扩展名
            String fileName = getFileName(uri, context);
            ZUtils.print("fileName = "+fileName);
            if (fileName == null) {
                // 如果无法获取文件名，使用 MIME 类型推断扩展名
                String mimeType = contentResolver.getType(uri);
                String extension = getExtensionFromMimeType(mimeType);
                ZUtils.print("fileName = "+fileName+" extension = "+extension);
                bean.setFileType(extension);
                fileName = "temp_file_" + System.currentTimeMillis() + (extension != null ? "." + extension : "");
            }else {
                if(fileName.contains(".")){
                    String extension = fileName.split("\\.")[1];
                    bean.setFileType(extension);
                    ZUtils.print("fileName = "+fileName+" extension = "+extension);
                }
            }
            bean.setName(fileName);

            // 创建临时文件
            File directory = context.getCacheDir(); // 或使用 getFilesDir()
            File file = new File(directory, fileName);

            // 复制 Uri 内容到 File
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // 关闭流
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            String fileSize = getFileSize(file.length());
            bean.setFileSizeLong(file.length());
            bean.setFileSize(fileSize+"");
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getFileSize(long length) {
      String name = "";
      if(length/(1024*1024*1024)> 1){
          name = length/(1024*1024*1024)+"GB";
      }else if(length/(1024*1024)> 1){
          name = length/(1024*1024)+"MB";
      }else if(length/(1024)> 1){
          name = length/(1024)+"KB";
      }else {
          name = length+"B";
      }
      return name;
    }

    // 获取文件名（包括扩展名）
    private static String getFileName(Uri uri, Context context) {
        String fileName = null;
        ContentResolver contentResolver = context.getContentResolver();

        // 尝试通过 ContentResolver 查询文件名
        try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        }

        // 如果查询失败，尝试从 Uri 的路径中提取
        if (fileName == null && "file".equalsIgnoreCase(uri.getScheme())) {
            fileName = new File(uri.getPath()).getName();
        }

        return fileName;
    }

    // 获取文件名（包括扩展名）
    public static String getMimeTypeByFile(File file) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getName());
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return type;
    }

    // 根据 MIME 类型推断文件扩展名
    static String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return null;
        switch (mimeType.toLowerCase()) {
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "application/pdf":
                return "pdf";
            case "text/plain":
                return "txt";
            case "application/msword":
                return "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return "docx";
            case "application/vnd.ms-excel":
                return "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                return "xlsx";
            case "video/mp4":
                return "mp4";
            case "audio/mpeg":
                return "mp3";
            // 添加更多 MIME 类型和扩展名的映射
            default:
                return null; // 未知类型，无扩展名
        }
    }

    // 删除缓存目录下的单个文件
    public static boolean deleteCacheFile(String fileName) {
        if(TextUtils.isEmpty(fileName)) {
            return false;
        }
        File file = new File(fileName);
        ZUtils.print("deleteCacheFile = "+fileName + " file.exists() " + file.exists());
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}
