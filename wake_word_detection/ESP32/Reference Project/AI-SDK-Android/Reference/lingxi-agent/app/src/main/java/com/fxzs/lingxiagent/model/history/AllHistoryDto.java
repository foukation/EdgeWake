package com.fxzs.lingxiagent.model.history;

import com.fxzs.lingxiagent.model.meeting.dto.MeetingDto;
import com.fxzs.lingxiagent.model.ppt.dto.PptSessionDto;

import java.util.List;

public class AllHistoryDto {
    private List<Record> list;
    private int total;

    public List<Record> getList() { return list; }
    public void setList(List<Record> list) { this.list = list; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public static class Record {
        private String type; // "ppt" | "meeting" | "image" | "translation"
        private long createTimeMs;
        private PptSessionDto ppt;
        private MeetingDto meeting;
        private AllRecordImage image;
        private com.fxzs.lingxiagent.network.ZNet.bean.TranslationRecordListBean translation;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public long getCreateTimeMs() { return createTimeMs; }
        public void setCreateTimeMs(long createTimeMs) { this.createTimeMs = createTimeMs; }
        public PptSessionDto getPpt() { return ppt; }
        public void setPpt(PptSessionDto ppt) { this.ppt = ppt; }
        public MeetingDto getMeeting() { return meeting; }
        public void setMeeting(MeetingDto meeting) { this.meeting = meeting; }
        public AllRecordImage getImage() { return image; }
        public void setImage(AllRecordImage image) { this.image = image; }
        public com.fxzs.lingxiagent.network.ZNet.bean.TranslationRecordListBean getTranslation() { return translation; }
        public void setTranslation(com.fxzs.lingxiagent.network.ZNet.bean.TranslationRecordListBean translation) { this.translation = translation; }
    }
}

