package com.fxzs.lingxiagent.model.deepresearch.dto;

import java.util.List;

public class WebSearch {

    private String query;
    private List<SearchResult> web_search;

    // Getters and Setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<SearchResult> getWeb_search() {
        return web_search;
    }

    public void setWeb_search(List<SearchResult> web_search) {
        this.web_search = web_search;
    }

    @Override
    public String toString() {
        return "WebSearch{" +
                "query='" + query + '\'' +
                ", web_search=" + web_search +
                '}';
    }

    public class SearchResult {
        private String title;
        private String url;
        private String content;

        // Getters and Setters
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String toString() {
            return "SearchResult{" +
                    "title='" + title + '\'' +
                    ", url='" + url + '\'' +
                    ", content='" + content + '\'' +
                    '}';
        }
    }

}
