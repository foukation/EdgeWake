package com.fxzs.lingxiagent.util.ZUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

import io.noties.prism4j.GrammarLocator;
import io.noties.prism4j.Prism4j;
import io.noties.prism4j.annotations.PrismBundle;

/**
 * Prism4j语法定位器，支持多种编程语言的语法高亮
 */
@PrismBundle(
        include = {
            "clike", "java", "javascript", "python", "c", "cpp", "json", 
            "markup", "css"
        },
        grammarLocatorClassName = ".MyGrammarLocator"
)
public class GrammarLocatorDef implements GrammarLocator {
    
    private static GrammarLocatorDef instance;
    
    public static GrammarLocatorDef getInstance() {
        if (instance == null) {
            instance = new GrammarLocatorDef();
        }
        return instance;
    }
    
    @Nullable
    @Override
    public Prism4j.Grammar grammar(@NonNull Prism4j prism4j, @NonNull String language) {
        // 直接使用手动映射
        return getManualGrammar(prism4j, language);
    }
    
    /**
     * 手动语法映射作为备用方案
     */
    @Nullable
    private Prism4j.Grammar getManualGrammar(@NonNull Prism4j prism4j, @NonNull String language) {
        try {
            switch (language.toLowerCase()) {
                case "java":
                    return prism4j.grammar("java");
                case "javascript":
                case "js":
                    return prism4j.grammar("javascript");
                case "python":
                case "py":
                    return prism4j.grammar("python");
                case "c":
                    return prism4j.grammar("c");
                case "cpp":
                case "c++":
                    return prism4j.grammar("cpp");
                case "json":
                    return prism4j.grammar("json");
                case "xml":
                case "html":
                    return prism4j.grammar("markup");
                case "css":
                    return prism4j.grammar("css");
                case "bash":
                case "shell":
                case "sh":
                    return prism4j.grammar("bash");
                case "sql":
                    return prism4j.grammar("sql");
                case "kotlin":
                case "kt":
                    return prism4j.grammar("kotlin");
                case "swift":
                    return prism4j.grammar("swift");
                case "typescript":
                case "ts":
                    return prism4j.grammar("typescript");
                default:
                    // 默认使用C-like语法
                    return prism4j.grammar("clike");
            }
        } catch (Exception e) {
            // 如果特定语法失败，使用默认的clike
            try {
                return prism4j.grammar("clike");
            } catch (Exception fallback) {
                // 如果所有都失败，返回null（将显示无高亮的代码）
                return null;
            }
        }
    }

    @NonNull
    @Override
    public Set<String> languages() {
        Set<String> languages = new HashSet<>();
        languages.add("java");
        languages.add("javascript");
        languages.add("python");
        languages.add("c");
        languages.add("cpp");
        languages.add("json");
        languages.add("markup");
        languages.add("css");
        languages.add("clike");
        return languages;
    }
}