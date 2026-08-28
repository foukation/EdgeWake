package com.fxzs.lingxiagent.model.repository;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MainRepositoryImpl implements MainRepository {
    
    @Override
    public CompletableFuture<String> getData() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 模拟网络请求延迟
                TimeUnit.SECONDS.sleep(1);
                return "来自Repository的数据";
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}