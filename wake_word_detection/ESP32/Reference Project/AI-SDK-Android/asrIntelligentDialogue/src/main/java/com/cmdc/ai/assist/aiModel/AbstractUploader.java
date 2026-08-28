package com.cmdc.ai.assist.aiModel;

import android.util.Log;

import com.cmdc.ai.assist.api.IRecordStream;

import java.io.InputStream;

import okhttp3.WebSocket;
import okio.ByteString;
import timber.log.Timber;

/**
 * 上传类
 * STEP 2. 连接成功后发送数据
 * STEP 2.1 发送发送开始参数帧
 * STEP 2.2 实时发送音频数据帧
 * STEP 2.4 发送结束帧
 */
public abstract class AbstractUploader {

    protected static final String TAG = RealTimeUploader.class.getSimpleName();

    /**
     * 输入的音频流
     */
    protected IRecordStream inputStream;

    protected volatile boolean isClosed = false;

    protected DealSotaOne.Context ctx;

    AbstractUploader(IRecordStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * STEP 2.2 实时发送音频数据帧
     *
     * @param webSocket WebSocket 类
     */
    protected abstract void sendAudioFrames(WebSocket webSocket, DealSotaOne.Context ctx);

    protected void sendBytes(WebSocket webSocket, byte[] buffer) {
        sendBytes(webSocket, buffer, buffer.length);
    }

    /**
     * 发送二进制帧
     *
     * @param webSocket WebSocket类
     * @param buffer    二进制
     * @param size
     * @return
     */
    protected void sendBytes(WebSocket webSocket, byte[] buffer, int size) {
        if (size > 0) {
            ByteString bytesToSend = ByteString.of(buffer, 0, size);
            if (!webSocket.send(bytesToSend)) {
                ctx.setRet(1);
            }
        } else if (size == 0) {
            Timber.tag(TAG).d("read size is 0");
        }
    }

}
