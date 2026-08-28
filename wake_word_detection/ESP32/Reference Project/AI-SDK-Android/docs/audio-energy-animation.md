# 语音能量动画实现说明

## 目标

基于 SDK 内部 `AudioRecord` 已采集到的 PCM 音频数据，实时计算语音能量值，用于驱动录音/语音输入时的波形动画。

不要在业务层额外启动 `MediaRecorder` 或新的 `AudioRecord` 抢占麦克风。

## 推荐取值

使用 **PCM RMS 能量**，再转换成 **dBFS**，最后归一化成 `0.0 ~ 1.0` 给 UI。

不要使用 `MediaRecorder.getMaxAmplitude()` 作为主方案，因为它是峰值振幅，容易受瞬间噪声影响，动画会跳动。

## 计算流程

SDK 每次通过 `AudioRecord.read()` 获取一帧 PCM 数据后，立即计算能量：

```java
public static float calculateEnergy(byte[] pcm, int length) {
    if (pcm == null || length < 2) {
        return 0f;
    }

    int sampleCount = length / 2;
    double sumSq = 0.0;

    for (int i = 0; i + 1 < length; i += 2) {
        short sample = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
        sumSq += sample * sample;
    }

    double rms = Math.sqrt(sumSq / sampleCount);
    double dbfs = 20.0 * Math.log10(rms / 32768.0 + 1e-9);

    double normalized = (dbfs + 60.0) / 40.0;
    normalized = Math.max(0.0, Math.min(1.0, normalized));

    return (float) normalized;
}
```

含义：

- `rms`：当前音频帧的平均能量。
- `dbfs`：相对于 16-bit PCM 满幅值的分贝值，通常为负数。
- `normalized`：给 UI 使用的能量值，范围 `0.0 ~ 1.0`。

## SDK 回调建议

SDK 不要只回调音频数据，建议额外暴露能量回调：

```java
public interface AudioEnergyListener {
    void onAudioEnergy(float energy);
}
```

在 SDK 的录音线程中：

```java
int read = audioRecord.read(buffer, 0, buffer.length);
if (read > 0) {
    float energy = calculateEnergy(buffer, read);
    if (audioEnergyListener != null) {
        audioEnergyListener.onAudioEnergy(energy);
    }

    // 原有音频数据逻辑继续执行
    audioDataListener.onAudioData(buffer, read);
}
```

## UI 使用方式

业务层只消费 `energy`：

```java
audioEnergyListener = energy -> {
    mainHandler.post(() -> waveView.setEnergy(energy));
};
```

UI 侧根据 `energy` 映射波形高度：

```java
float minHeight = 4f;
float maxHeight = 32f;
float height = minHeight + energy * (maxHeight - minHeight);
```

## 平滑处理

为了避免动画抖动，UI 层建议做简单平滑：

```java
displayEnergy = displayEnergy * 0.75f + energy * 0.25f;
```

如果需要更灵敏，可以调成：

```java
displayEnergy = displayEnergy * 0.6f + energy * 0.4f;
```

## 参数建议

- 音频格式：PCM 16-bit little-endian
- 声道：单声道
- 帧长：20ms ~ 100ms 都可以
- UI 更新频率：建议 30ms ~ 100ms
- dBFS 映射区间：`-60dBFS ~ -20dBFS`

## 结论

语音能量动画应基于 SDK 当前 `AudioRecord` 采集到的 PCM 数据计算 RMS/dBFS。

不要额外启动 `MediaRecorder` 获取 `getMaxAmplitude()`，避免麦克风采集冲突，也避免峰值导致动画不稳定。
