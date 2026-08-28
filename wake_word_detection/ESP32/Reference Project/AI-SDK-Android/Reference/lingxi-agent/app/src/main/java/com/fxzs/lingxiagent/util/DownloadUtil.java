package com.fxzs.lingxiagent.util;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 文件下载工具类，支持实时返回下载进度
 */
public class DownloadUtil {

    private Context mContext;
    private DownloadListener mListener;
    private DownloadTask mDownloadTask;

    // 下载回调接口
    public interface DownloadListener {
        void onProgress(int progress); // 下载进度
        void onSuccess(File file);     // 下载成功
        void onFailure(String errorMsg); // 下载失败
    }

    public DownloadUtil(Context context) {
        this.mContext = context;
    }

    /**
     * 开始下载文件
     * @param url 下载链接
     * @param listener 下载监听器
     */
    public void download(String url, DownloadListener listener) {
        this.mListener = listener;
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
        }
        mDownloadTask = new DownloadTask();
        mDownloadTask.execute(url);
    }

    /**
     * 取消下载
     */
    public void cancelDownload() {
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }
    }

    /**
     * 异步下载任务
     */
    private class DownloadTask extends AsyncTask<String, Integer, File> {
        private String errorMsg;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // 可以在这里显示进度对话框
        }

        @Override
        protected File doInBackground(String... params) {
            String urlStr = params[0];
            InputStream is = null;
            FileOutputStream fos = null;
            HttpURLConnection connection = null;

            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                connection.setRequestMethod("GET");
                connection.connect();

                // 检查响应码
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    errorMsg = "服务器响应错误: " + connection.getResponseCode();
                    return null;
                }

                // 获取文件总大小
                long totalLength = connection.getContentLength();
                if (totalLength <= 0) {
                    errorMsg = "无法获取文件大小";
                    return null;
                }

                // 创建下载目录
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File appDir = new File(dir, mContext.getPackageName());
                if (!appDir.exists() && !appDir.mkdirs()) {
                    errorMsg = "无法创建下载目录";
                    return null;
                }

                // 获取文件名
                String fileName = getFileNameFromUrl(urlStr);
                File file = new File(appDir, fileName);

                // 开始下载
                is = connection.getInputStream();
                fos = new FileOutputStream(file);
                byte[] buffer = new byte[1024 * 8];
                int len;
                long downloadedLength = 0;

                while ((len = is.read(buffer)) != -1) {
                    // 检查是否取消下载
                    if (isCancelled()) {
                        if (file.exists() && file.delete()) {
                            // 删除不完整文件
                        }
                        return null;
                    }

                    fos.write(buffer, 0, len);
                    downloadedLength += len;
                    // 计算进度并发布
                    int progress = (int) (downloadedLength * 100 / totalLength);
                    publishProgress(progress);
                }

                fos.flush();
                return file;

            } catch (Exception e) {
                errorMsg = "下载失败: " + e.getMessage();
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (is != null) is.close();
                    if (fos != null) fos.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            // 通知进度更新
            if (mListener != null) {
                mListener.onProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            if (file != null && file.exists()) {
                // 下载成功
                if (mListener != null) {
                    mListener.onSuccess(file);
                }
                Toast.makeText(mContext, "下载完成", Toast.LENGTH_SHORT).show();
            } else {
                // 下载失败
                if (mListener != null) {
                    mListener.onFailure(errorMsg);
                }
                Toast.makeText(mContext, errorMsg, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (mListener != null) {
                mListener.onFailure("下载已取消");
            }
            Toast.makeText(mContext, "下载已取消", Toast.LENGTH_SHORT).show();
        }

        /**
         * 从URL中获取文件名
         */
        private String getFileNameFromUrl(String url) {
            try {
                String fileName = url.substring(url.lastIndexOf("/") + 1);
                // 处理URL中可能包含的参数
                if (fileName.contains("?")) {
                    fileName = fileName.substring(0, fileName.indexOf("?"));
                }
                return fileName;
            } catch (Exception e) {
                return "download_" + System.currentTimeMillis();
            }
        }
    }


}
