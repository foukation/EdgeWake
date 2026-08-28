package com.fxzs.lingxiagent.viewmodel.chat.service;

import com.fxzs.lingxiagent.util.audio.TTSManager;

import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * TTSAudioService
 * 职责：
 * - 统一 TTS 文本与 URL 播放入口，支持增量/完整模式
 * - 结合 isAutoPlay 开关，避免上层重复判断与直接操作 TTSManager
 */
public class TTSAudioService {
    // 1. 匹配HTML标签
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    // 2. 匹配超链接
    private static final Pattern LINK_PATTERN = Pattern.compile(
            "https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]|" +
                    "www\\.[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
    );

    // 3. 匹配空括号对（包括中英文括号）
    private static final Pattern EMPTY_BRACKETS_PATTERN = Pattern.compile(
            "\\(\\s*\\)|（\\s*）|\\[\\s*\\]|【\\s*】"
    );

    // 4. 匹配特殊符号组合（如>]、>[等）
    private static final Pattern SPECIAL_COMBINATION_PATTERN = Pattern.compile(
            "[<>]{1,2}[\\[\\]()（）]|[\\[\\]()（）]{1,2}[<>]"
    );

    // 5. 合并连续空格
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");

    public static String filterForTTS(String originalText) {
        if (originalText == null || originalText.isEmpty()) {
            return "";
        }
        Timber.i("TTS处理 originalText: %s", originalText);
        String filtered = originalText;

        // 步骤1：移除HTML标签
        filtered = HTML_TAG_PATTERN.matcher(filtered).replaceAll(" ");

        // 步骤2：过滤超链接
        filtered = LINK_PATTERN.matcher(filtered).replaceAll(" ");

        // 步骤3：过滤空括号对（如()、（）、[]等）
        filtered = EMPTY_BRACKETS_PATTERN.matcher(filtered).replaceAll(" ");

        // 步骤4：过滤特殊符号组合
        filtered = SPECIAL_COMBINATION_PATTERN.matcher(filtered).replaceAll(" ");

        // 步骤5：合并空格并去除首尾空格
        filtered = MULTIPLE_SPACES_PATTERN.matcher(filtered).replaceAll(" ").trim();
        Timber.i("TTS处理 filtered: %s", filtered);
        return filtered;
    }

    public void playText(String content, String conversationId, boolean isComplete, Boolean isAutoPlay) {
        if (isAutoPlay == null || !isAutoPlay) return;
        String ttsContent = filterForTTS(content);
        if (isComplete) {
            TTSManager.Companion.getInstance().textToAudio(ttsContent);
        } else {
            Timber.i("playTTSContent: %s", ttsContent);
            TTSManager.Companion.getInstance().textToAudio(ttsContent, conversationId);
        }
    }

    public void playUrl(String url, String conversationId, boolean isComplete, Boolean isAutoPlay) {
        if (isAutoPlay == null || !isAutoPlay) return;
        if (isComplete) {
            TTSManager.Companion.getInstance().playTTS(url);
        } else {
            Timber.i("playTTSUrl: %s", url);
            TTSManager.Companion.getInstance().playTTS(url, conversationId);
        }
    }
}

