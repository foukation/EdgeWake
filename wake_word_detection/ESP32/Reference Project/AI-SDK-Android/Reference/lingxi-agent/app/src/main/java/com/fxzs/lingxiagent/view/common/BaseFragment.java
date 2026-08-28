package com.fxzs.lingxiagent.view.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fxzs.lingxiagent.model.common.BaseViewModel;

public abstract class BaseFragment<VM extends BaseViewModel> extends Fragment {
    
    protected VM viewModel;
    private View rootView;
    
    protected abstract int getLayoutResource();
    protected abstract Class<VM> getViewModelClass();
    protected abstract void initializeViews(View view);
    protected abstract void setupDataBinding();
    protected abstract void setupObservers();
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(getViewModelClass());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(getLayoutResource(), container, false);
        return rootView;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupDataBinding();
        setupObservers();
        
        observeCommonStates();
    }
    
    private void observeCommonStates() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), this::handleLoadingState);
        viewModel.getError().observe(getViewLifecycleOwner(), this::handleErrorState);
        viewModel.getSuccess().observe(getViewLifecycleOwner(), this::handleSuccessState);
    }
    
    protected void handleLoadingState(boolean loading) {
        // 子类可以重写此方法来自定义加载状态的显示
    }
    
    protected void handleErrorState(String error) {
        if (error != null && !error.isEmpty()) {
            showToast(error);
        }
    }
    
    protected void handleSuccessState(String success) {
        if (success != null && !success.isEmpty()) {
            showToast(success);
        }
    }
    
    protected void showToast(String message) {
        if (getActivity() != null) {
            GlobalToast.show(getActivity(), message, GlobalToast.Type.NORMAL);
        }
    }
    
    protected <T extends View> T findViewById(int id) {
        return rootView.findViewById(id);
    }
}