package com.fxzs.lingxiagent.viewmodel.chat.helper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

/**
 * 封装 observeForever 的生命周期管理，持有 LiveData 与 Observer，并提供安全移除。
 */
public class MeetingBinder<T> {
    private LiveData<T> liveData;
    private Observer<T> observer;

    public void bind(LiveData<T> source, Observer<T> obs) {
        unbind();
        this.liveData = source;
        this.observer = obs;
        if (this.liveData != null && this.observer != null) {
            this.liveData.observeForever(this.observer);
        }
    }

    public void unbind() {
        if (this.liveData != null && this.observer != null) {
            try { this.liveData.removeObserver(this.observer); } catch (Exception ignored) {}
        }
        this.liveData = null;
        this.observer = null;
    }
}

