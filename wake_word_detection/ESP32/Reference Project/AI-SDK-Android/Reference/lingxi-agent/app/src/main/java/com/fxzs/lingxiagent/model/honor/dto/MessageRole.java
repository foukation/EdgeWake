package com.fxzs.lingxiagent.model.honor.dto;

public enum MessageRole {
    USER("user"),
    ASSISTANT("assistant");

    private final String alias;

    MessageRole(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }
}

