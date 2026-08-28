package com.fxzs.lingxiagent.model.deepresearch.dto;

import java.util.List;

public class DeepResearchBean {

    private String query;
    private List<DeepResearchItem> list;
    private String req_id;
    private int TaskStatus;//1：待创建 2: 已创建 3:报告执行中 4:报告已完成
    private String step;//1:thinking 2: think complete 3: web_search 4:reporting 5:report complete
    private int report_count;
    private String model;
    private String report;
    private String reportContent;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<DeepResearchItem> getList() {
        return list;
    }

    public void setList(List<DeepResearchItem> list) {
        this.list = list;
    }

    public String getReq_id() {
        return req_id;
    }

    public void setReq_id(String req_id) {
        this.req_id = req_id;
    }

    public int getTaskStatus() {
        return TaskStatus;
    }

    public void setTaskStatus(int taskStatus) {
        TaskStatus = taskStatus;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public int getReport_count() {
        return report_count;
    }

    public void setReport_count(int report_count) {
        this.report_count = report_count;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public String getReportContent() {
        return reportContent;
    }

    public void setReportContent(String reportContent) {
        this.reportContent = reportContent;
    }

    @Override
    public String toString() {
        return "DeepResearchBean{" +
                "query='" + query + '\'' +
                ", list=" + list +
                ", req_id='" + req_id + '\'' +
                ", TaskStatus=" + TaskStatus +
                ", step='" + step + '\'' +
                ", report_count=" + report_count +
                ", model='" + model + '\'' +
                '}';
    }
}
