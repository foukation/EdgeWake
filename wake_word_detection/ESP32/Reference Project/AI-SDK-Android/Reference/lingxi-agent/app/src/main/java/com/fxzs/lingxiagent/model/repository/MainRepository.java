package com.fxzs.lingxiagent.model.repository;

import java.util.concurrent.CompletableFuture;

public interface MainRepository {
    CompletableFuture<String> getData();
}