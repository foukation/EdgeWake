package com.fxzs.lingxiagent.model.intention.dto;

public class TripCreateRequest {
    public String title;
    public String description;
    public String command;

    public TripCreateRequest(String command) {
        this.title = "行程规划";
        this.description = "行程规划";
        this.command = command;
    }
}
