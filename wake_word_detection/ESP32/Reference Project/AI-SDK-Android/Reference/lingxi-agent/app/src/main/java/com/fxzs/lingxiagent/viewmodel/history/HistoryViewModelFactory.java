package com.fxzs.lingxiagent.viewmodel.history;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * HistoryViewModel工厂类
 */
public class HistoryViewModelFactory implements ViewModelProvider.Factory {
    
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(VMHistory.class)) {
            return (T) new VMHistory();
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
