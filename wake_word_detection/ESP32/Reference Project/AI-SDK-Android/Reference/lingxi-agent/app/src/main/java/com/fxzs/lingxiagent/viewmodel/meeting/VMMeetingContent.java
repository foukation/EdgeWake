package com.fxzs.lingxiagent.viewmodel.meeting;

import android.app.Application;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.meeting.dto.MeetingDto;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepository;
import com.fxzs.lingxiagent.model.meeting.repository.MeetingRepositoryImpl;

import timber.log.Timber;

public class VMMeetingContent extends BaseViewModel {
    
    private static final String TAG = "VMMeetingContent";
    
    // 转写内容
    private final ObservableField<String> transcriptionContent = new ObservableField<>("");
    
    // 原文内容
    private String originalContent = "";

    // 会议ID
    private String meetingId;

    // Repository
    private final MeetingRepository repository;
    
    public VMMeetingContent(Application application) {
        super(application);
        this.repository = new MeetingRepositoryImpl();
    }
    
    public ObservableField<String> getTranscriptionContent() {
        return transcriptionContent;
    }
    
    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }
    
    /**
     * 设置原始转写内容
     */
    public void setOriginalContent(String content) {
        if (content == null) content = "";
        this.originalContent = content;

        // 直接显示格式化后的原文内容
        String formattedContent = formatMeetingContent(content);
        transcriptionContent.set(formattedContent);
    }
    

    
    /**
     * 格式化会议内容
     * 将后端返回的格式 "[0:0.930,0:1.780,0]  你好啊。\n" 转换为
     * "- **发言人1 00:00** \n 你好啊。\n"
     * 最后一位数字代表发言人ID：0=发言人1，1=发言人2，以此类推
     *
     * @param rawContent 后端返回的原始内容
     * @return 格式化后的内容
     */
    public String formatMeetingContent(String rawContent) {
        if (rawContent == null || rawContent.isEmpty()) {
            return "";
        }

        StringBuilder formattedContent = new StringBuilder();
        String[] lines = rawContent.split("\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            // 匹配格式 [speakerId:mm:ss(.ms),speakerId:mm:ss(.ms),speakerIndex] content
            // 兼容秒数为整数或小数，如 1:5 或 1:5.030
            String regex = "\\[(\\d+):(\\d+(?:\\.\\d+)?),(\\d+):(\\d+(?:\\.\\d+)?),(\\d+)\\]\\s+(.*)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
            java.util.regex.Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
                int startMinutesPart = Integer.parseInt(matcher.group(1));
                double startSecondsPart = Double.parseDouble(matcher.group(2));
                int speakerIndex = Integer.parseInt(matcher.group(5)); // 最后一位数字是发言人索引
                String content = matcher.group(6);

                // 转换发言人索引为发言人名称：0=发言人1，1=发言人2，以此类推
                String speakerName = "发言人" + (speakerIndex + 1);

                // 计算总秒并格式化为 mm:ss（补零）
                int totalSeconds = (int) Math.floor(startMinutesPart * 60 + startSecondsPart + 1e-6);
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;
                String timeString = String.format("%02d:%02d", minutes, seconds);

                // 格式化输出
                formattedContent.append("- **").append(speakerName).append(" ")
                        .append(timeString).append("** \n")
                        .append(content).append("\n\n");
            } else {
                // 如果不匹配格式，保留原内容
                formattedContent.append(line).append("\n");
            }
        }

        return formattedContent.toString().trim();
    }
    
    /**
     * 设置原始转写内容并自动格式化
     */
    public void setRawTranscriptionContent(String rawContent) {
        if (rawContent == null) rawContent = "";

        Timber.tag(TAG).d( "setRawTranscriptionContent - 原始内容长度: " + rawContent.length());

        // 保存原始内容
        this.originalContent = rawContent;

        // 显示格式化后的内容
        String formattedContent = formatMeetingContent(rawContent);
        Timber.tag(TAG).d( "setRawTranscriptionContent - 格式化后内容长度: " + formattedContent.length());

        transcriptionContent.set(formattedContent);
    }
    
    /**
     * 刷新会议内容
     */
    public void refreshContent() {
        if (meetingId == null || meetingId.isEmpty()) {
            setError("会议ID不能为空");
            return;
        }

        setLoading(true);

        // 获取会议详情
        repository.getMeetingDetail(meetingId).observeForever(result -> {
            if (result != null) {
                if (result.isSuccess() && result.getData() != null) {
                    MeetingDto meeting = result.getData();
                    if (meeting.getMeetingText() != null) {
                        // 更新原文内容
                        setRawTranscriptionContent(meeting.getMeetingText());
                        setSuccess("内容刷新完成");
                    } else {
                        setError("会议内容为空");
                    }
                } else {
                    setError(result.getError());
                }
            }
            setLoading(false);
        });
    }
}