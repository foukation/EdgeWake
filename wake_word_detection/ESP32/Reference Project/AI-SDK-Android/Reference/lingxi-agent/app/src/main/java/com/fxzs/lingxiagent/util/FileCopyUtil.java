package com.fxzs.lingxiagent.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/**
 * 本地文件复制工具类，支持进度回调
 */
public class FileCopyUtil {
    private Context mContext;
    private CopyListener mListener;
    private CopyTask mCopyTask;
    private long delayMillis = 50; // 每次进度更新后的延时毫秒数，可调整

    // 复制回调接口
    public interface CopyListener {
        void onProgress(int progress); // 复制进度(0-100)
        void onSuccess(File destFile); // 复制成功
        void onFailure(String errorMsg); // 复制失败
    }

    public FileCopyUtil(Context context) {
        this.mContext = context;
    }

    /**
     * 设置延时时间（用于控制进度条更新速度）
     * @param delayMillis 延时毫秒数，建议50-200之间
     */
    public void setDelayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    /**
     * 开始复制文件
     * @param sourcePath 源文件路径
     * @param destDir 目标目录
     * @param listener 复制监听器
     */
    public void copyFile(String sourcePath, String destDir, CopyListener listener) {
        this.mListener = listener;
        if (mCopyTask != null) {
            mCopyTask.cancel(true);
        }
        mCopyTask = new CopyTask();
        mCopyTask.execute(sourcePath, destDir);
    }

    /**
     * 取消复制
     */
    public void cancelCopy() {
        if (mCopyTask != null) {
            mCopyTask.cancel(true);
            mCopyTask = null;
        }
    }

    /**
     * 异步复制任务
     */
    private class CopyTask extends AsyncTask<String, Integer, File> {
        private String errorMsg;

        @Override
        protected File doInBackground(String... params) {
            String sourcePath = params[0];
            String destDirPath = params[1];

            // 验证源文件
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                errorMsg = "源文件不存在";
                return null;
            }
            if (!sourceFile.isFile()) {
                errorMsg = "源路径不是文件";
                return null;
            }
            if (!sourceFile.canRead()) {
                errorMsg = "没有读取源文件的权限";
                return null;
            }

            // 创建目标目录
            File destDir = new File(destDirPath);
            if (!destDir.exists()) {
                if (!destDir.mkdirs()) {
                    errorMsg = "无法创建目标目录";
                    return null;
                }
            }
            if (!destDir.isDirectory()) {
                errorMsg = "目标路径不是目录";
                return null;
            }

            // 创建目标文件
            File destFile = new File(destDir, sourceFile.getName());
            // 如果目标文件已存在，先删除
            if (destFile.exists() && !destFile.delete()) {
                errorMsg = "无法覆盖已存在的目标文件";
                return null;
            }

            // 开始复制文件
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new FileInputStream(sourceFile);
                out = new FileOutputStream(destFile);

                byte[] buffer = new byte[1024 * 8];
                int length;
                long totalLength = sourceFile.length();
                long copiedLength = 0;
                int lastProgress = 0;

                while ((length = in.read(buffer)) > 0) {
                    // 检查是否取消复制
                    if (isCancelled()) {
                        // 如果取消了，删除不完整的目标文件
                        if (destFile.exists() && !destFile.delete()) {
                            errorMsg = "取消复制时删除临时文件失败";
                        }
                        return null;
                    }

                    out.write(buffer, 0, length);
                    copiedLength += length;

                    // 计算进度并发布
                    if (totalLength > 0) {
                        int progress = (int) (copiedLength * 100 / totalLength);
                        // 只有进度发生变化时才更新，避免频繁刷新
                        if (progress > lastProgress) {
                            lastProgress = progress;
                            publishProgress(progress);

                            // 添加延时，放慢进度展示速度
                            SystemClock.sleep(delayMillis);
                        }
                    }
                }

                out.flush();
                return destFile;

            } catch (IOException e) {
                errorMsg = "复制失败: " + e.getMessage();
                // 复制失败时删除目标文件
                if (destFile.exists() && !destFile.delete()) {
                    errorMsg += "，且清理临时文件失败";
                }
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (mListener != null) {
                mListener.onProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(File result) {
            super.onPostExecute(result);
            if (result != null && result.exists()) {
                if (mListener != null) {
                    mListener.onSuccess(result);
                }
                Toast.makeText(mContext, "文件复制成功", Toast.LENGTH_SHORT).show();
            } else {
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
                mListener.onFailure("复制已取消");
            }
            Toast.makeText(mContext, "复制已取消", Toast.LENGTH_SHORT).show();
        }
    }
}
