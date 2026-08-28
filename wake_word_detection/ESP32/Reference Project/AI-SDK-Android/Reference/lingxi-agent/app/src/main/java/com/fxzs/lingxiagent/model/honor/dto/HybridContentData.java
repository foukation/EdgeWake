package com.fxzs.lingxiagent.model.honor.dto;

public class HybridContentData {
    private CommandsData commands;

    public HybridContentData(CommandsData commands) {
        this.commands = commands;
    }

    // Getters
    public CommandsData getCommands() { return commands; }

    // Setters
    public void setCommands(CommandsData commands) { this.commands = commands; }
}
