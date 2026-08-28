package com.fxzs.lingxiagent.util.ZUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

import io.noties.prism4j.GrammarLocator;
import io.noties.prism4j.Prism4j;

/**
 * 简化的 Prism4j 语法定位器
 * 暂时返回null，避免编译错误
 */
public class SimplePrismGrammarLocator implements GrammarLocator {
    
    private static SimplePrismGrammarLocator instance;
    
    public static SimplePrismGrammarLocator getInstance() {
        if (instance == null) {
            instance = new SimplePrismGrammarLocator();
        }
        return instance;
    }
    
    @Nullable
    @Override
    public Prism4j.Grammar grammar(@NonNull Prism4j prism4j, @NonNull String language) {
        // 暂时返回null，避免编译错误
        // TODO: 实现语法高亮功能
        return null;
    }
    
    @NonNull
    @Override
    public Set<String> languages() {
        Set<String> languages = new HashSet<>();
        // 暂时返回空集合
        return languages;
    }
}