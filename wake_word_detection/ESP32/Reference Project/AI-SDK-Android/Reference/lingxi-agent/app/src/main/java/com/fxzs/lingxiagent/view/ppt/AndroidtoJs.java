package com.fxzs.lingxiagent.view.ppt;

import android.content.Context;
import android.webkit.JavascriptInterface;

import timber.log.Timber;

public class AndroidtoJs {

    public interface JSCallback {
        void callback(String name);
    }

    private final Context context;
    private final JSCallback callback;

    public AndroidtoJs(Context context, JSCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @JavascriptInterface
    public void saveFile(String msg) {
        Timber.tag("PptPreview").d("JS调用Android saveFile, msg=" + msg);
        Timber.tag("PptPreview").e( "AndroidtoJs ");
        if (callback != null) {
            callback.callback("saveFile");
        }
    }
}
