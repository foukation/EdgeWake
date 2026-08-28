package com.fxzs.lingxiagent.model.deepresearch.repository;

import io.reactivex.disposables.Disposable;

public interface DeepResearchRepository {

    Disposable sendStreamRequest(String inputString, String req_id, DeepResearchStreamHandler handler);
}
