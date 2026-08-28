package com.cmdc.ai.assist.aiModel;

import com.baidu.voicesearch.opus.OpusTools;
import com.cmdc.ai.assist.api.IRecordStream;

import org.java_websocket.client.WebSocketClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.WebSocket;
import okio.ByteString;
import timber.log.Timber;


/**
 * WebSocket的上行逻辑
 * 实时流，由于音频数据生成的时候已经有间隔，发送时不需要再有间隔
 */
public class CompressUploader {

    private static final int BYTES_PER_MS = 16000 * 2 / 1000;
    private static final int FRAME_MS = 160;
    private static final int BYTES_PER_FRAME = (BYTES_PER_MS * FRAME_MS) / 8;

    protected IRecordStream inputStream;
    protected volatile boolean isClosed = false;
    private static final String TAG = CompressUploader.class.getSimpleName();
    private long opusEncoder;

    public CompressUploader(IRecordStream inputStream) {
        this.inputStream = inputStream;
    }


    /**
     * STEP 2.2 实时发送音频数据帧
     * <p>
     * 发送二进制，积累到160ms，即5120个字节再发送
     *
     * @param webSocket WebSocket类
     * @throws IOException
     */
    public void sendAudioFrames(WebSocketClient webSocket) {
        Timber.tag(TAG).d("sendAudioFrames!");
        int bytesPerFrame = BYTES_PER_FRAME; // 一个帧 160ms的音频数据
        byte[] bufferOpus = new byte[bytesPerFrame];
        byte[] buffer = new byte[80 * 8];
        int readSize = -1;
        int totalSize = 0;
        int encodeSize = -1;
        opusEncoder = OpusTools.createEncoder(16000, 1, 10);

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        do {
            if (isClosed) {
                Timber.tag(TAG).d("websocket is closed before all data sent");
                break;
            }
            try {
                readSize = inputStream.read(buffer);
                // Log.d(TAG, "readSize =" + readSize);
                if (readSize > 0) {
                    byte[] opusByte = new byte[buffer.length / 8];
                    encodeSize = OpusTools.encode(opusEncoder, OpusTools.toShortArray(buffer), 0, opusByte);
                    // Log.d("record", "storeAudioData: encodeSize->" + encodeSize);
                    if (encodeSize > 0) {
                        bufferOpus = new byte[encodeSize];
                        System.arraycopy(opusByte, 0, bufferOpus, 0, encodeSize);
                    }
                }
            } catch (Exception e) {
                Timber.tag(TAG).d("inputstream is closed:" + e.getClass().getSimpleName() + ":" + e.getMessage());
            }
            if (readSize > 0) {
                // 发送二进制，积累到20ms，即640个字节再发送
                outputBuffer.write(bufferOpus, 0, encodeSize);
                if (outputBuffer.size() >= bytesPerFrame) {
                    byte[] buf = outputBuffer.toByteArray();
                    sendBytes(webSocket, buf);
                    outputBuffer.reset();
                    outputBuffer.write(buf, bytesPerFrame, buf.length - bytesPerFrame);
                    totalSize += bytesPerFrame;
                    Timber.tag(TAG).d("should wait to send next DATA Frame: " + 160
                            + "ms | send binary bytes size :" + bytesPerFrame + " | total size: " + totalSize);
                }
            } else if (readSize == -1) {
                byte[] buf = outputBuffer.toByteArray();
                if (buf.length > 0) {
                    sendBytes(webSocket, buf);
                    totalSize += buf.length;
                    Timber.tag(TAG).d("last pack send size " + buf.length + " | total size :" + totalSize);
                }
            }
        } while (readSize >= 0);
        OpusTools.destroyEncoder(opusEncoder);
    }

    protected void sendBytes(WebSocketClient webSocket, byte[] buffer) {
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
    protected void sendBytes(WebSocketClient webSocket, byte[] buffer, int size) {
        if (size > 0) {
            ByteString bytesToSend = ByteString.of(buffer, 0, size);
            webSocket.send(bytesToSend.toByteArray());
        } else if (size == 0) {
            Timber.tag(TAG).d("read size is 0");
        }
    }

}
