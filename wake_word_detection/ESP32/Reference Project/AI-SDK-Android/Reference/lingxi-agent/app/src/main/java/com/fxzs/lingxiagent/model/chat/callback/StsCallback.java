
package com.fxzs.lingxiagent.model.chat.callback;

import androidx.annotation.Nullable;

import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;

public interface StsCallback {
     void progress(long percent);
     void callback(String path);
     void error( @Nullable CosXmlClientException clientException,
                 @Nullable CosXmlServiceException serviceException);
}
