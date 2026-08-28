package com.fxzs.lingxiagent.lingxi.translate.audio;

/**
 * 轻量级语音活动检测器（VAD）
 * - 输入: PCM16 单声道 16kHz 帧（任意长度，建议200ms）
 * - 方法: dBFS能量 + 样本占空比 + 自适应噪声地板
 * - 目标: 鲁棒识别“有声/无声”，避免噪声误判；参数可调
 */
public class VoiceActivityDetector {

    // 自适应噪声地板，初值为极低噪声
    private double noiseFloorDbfs = -90.0; // dBFS（0dBFS为满幅，越小越安静）

    // 平滑系数（用于噪声地板更新）
    private static final double NOISE_SMOOTH = 0.95; // 越大更新越慢，越稳

    // 判定参数
    private static final double MIN_ABS_DBFS_THRESHOLD = -45.0; // 绝对能量阈值下限（dBFS）
    private static final double NOISE_MARGIN_DB = 12.0;        // 相对噪声地板的提升阈值（dB）
    private static final int OCCUPANCY_SAMPLE_ABS_THRESHOLD = 700; // 占空比的样本幅度阈值（0..32768）
    private static final double MIN_OCCUPANCY_RATIO = 0.010;   // 至少有1%样本显著非零

    private static final double EPS = 1e-9;

    /**
     * 返回该帧是否为“有声”。
     */
    public boolean isSpeech(byte[] pcmLe16) {
        if (pcmLe16 == null || pcmLe16.length < 2) return false;

        int samples = pcmLe16.length / 2;
        if (samples <= 0) return false;

        long crossings = 0;
        long occupancyCount = 0;
        double sumSq = 0.0;

        int prev = 0;
        for (int i = 0; i < samples; i++) {
            int lo = pcmLe16[2 * i] & 0xFF;
            int hi = pcmLe16[2 * i + 1]; // signed
            int v = (hi << 8) | lo;      // little-endian to signed 16-bit

            // RMS 累积
            sumSq += (double) v * (double) v;

            // 占空比：显著非零的样本占比
            if (Math.abs(v) >= OCCUPANCY_SAMPLE_ABS_THRESHOLD) {
                occupancyCount++;
            }

            // 零交叉（备用特征，当前不强制使用，仅保留为以后调参）
            if (i > 0) {
                if ((v ^ prev) < 0) crossings++; // 符号变化
            }
            prev = v;
        }

        double rms = Math.sqrt(sumSq / Math.max(1, samples));
        double dbfs = 20.0 * Math.log10(rms / 32768.0 + EPS); // dBFS: 0为满幅

        double occupancy = (double) occupancyCount / (double) samples; // 0..1

        // 动态阈值：噪声地板 + margin，与绝对阈值取更宽松者
        double dynamicThreshold = Math.max(noiseFloorDbfs + NOISE_MARGIN_DB, MIN_ABS_DBFS_THRESHOLD);

        boolean speechByEnergy = dbfs >= dynamicThreshold;
        boolean speechByOccupancy = occupancy >= MIN_OCCUPANCY_RATIO;

        boolean isSpeech = speechByEnergy && speechByOccupancy;

        // 噪声地板自适应更新：仅在较安静时缓慢下探
        if (!isSpeech) {
            // 仅当当前能量不高于地板 + 3dB 时，认定是噪声并更新地板
            double allowUp = noiseFloorDbfs + 3.0;
            double candidate = Math.min(dbfs, allowUp);
            noiseFloorDbfs = NOISE_SMOOTH * noiseFloorDbfs + (1.0 - NOISE_SMOOTH) * candidate;
        }

        return isSpeech;
    }

    /** 重置噪声地板（例如新会话开始时） */
    public void reset() {
        noiseFloorDbfs = -90.0;
    }
}

