package com.fxzs.lingxiagent.model.chat.callback;

import com.fxzs.lingxiagent.model.deepresearch.dto.DeepResearchBean;

public interface DeepResearchCallback {

    void onDeepResearch(DeepResearchBean deepResearchBean);
    void onDeepResearchError(String error);
    void onDeepResearchComplete();

}
