package com.fxzs.lingxiagent.lingxi.common.log;

import timber.log.Timber;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2025/12/5 下午2:41
 */
public class NoLogTree extends Timber.Tree {
    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        // 什么都不做
    }
}

