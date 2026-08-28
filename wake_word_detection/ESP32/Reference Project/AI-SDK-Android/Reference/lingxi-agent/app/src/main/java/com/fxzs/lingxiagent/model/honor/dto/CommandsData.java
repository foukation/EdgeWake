package com.fxzs.lingxiagent.model.honor.dto;

public class CommandsData {
    private HeadData head;
    private BodyData body;

    public CommandsData(HeadData head, BodyData body) {
        this.head = head;
        this.body = body;
    }

    // Getters
    public HeadData getHead() { return head; }
    public BodyData getBody() { return body; }

    // Setters
    public void setHead(HeadData head) { this.head = head; }
    public void setBody(BodyData body) { this.body = body; }
}

