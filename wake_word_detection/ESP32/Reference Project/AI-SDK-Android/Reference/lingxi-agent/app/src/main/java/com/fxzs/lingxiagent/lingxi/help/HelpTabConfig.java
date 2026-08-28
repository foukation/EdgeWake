package com.fxzs.lingxiagent.lingxi.help;

import java.util.List;

public class HelpTabConfig {
    private final String title;
    private final List<String> content;

    public HelpTabConfig(String title, List<String> content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getContent() {
        return content;
    }
}