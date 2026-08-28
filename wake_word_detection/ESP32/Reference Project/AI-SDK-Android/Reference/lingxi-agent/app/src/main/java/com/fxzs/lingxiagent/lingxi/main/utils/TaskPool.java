package com.fxzs.lingxiagent.lingxi.main.utils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskPool {

    // 自定义线程池配置参数
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 100;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final RejectedExecutionHandler HANDLER = new ThreadPoolExecutor.AbortPolicy();

    // 单例线程池实例
    private static final ThreadPoolExecutor THREAD_POOL = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            new CustomThreadFactory(),
            HANDLER
    );

    private static class CustomThreadFactory implements ThreadFactory {
        private int count = 0;

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "TaskPool-Thread-" + count++);
        }
    }

    public static void execute(Runnable task) {
        THREAD_POOL.execute(task);
    }
}
