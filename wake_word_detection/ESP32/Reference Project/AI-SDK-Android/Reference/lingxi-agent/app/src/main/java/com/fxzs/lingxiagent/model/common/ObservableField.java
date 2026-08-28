package com.fxzs.lingxiagent.model.common;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * 用于双向绑定的可观察字段
 * 继承自MutableLiveData，添加双向绑定支持
 */
public class ObservableField<T> extends MutableLiveData<T> {
    
    public ObservableField() {
        super();
    }
    
    public ObservableField(T value) {
        super(value);
    }
    
    /**
     * 设置值（双向绑定使用）
     * @param value 新值
     */
    public void set(T value) {
        setValue(value);
    }
    
    /**
     * 线程安全地设置值（用于后台线程）
     * @param value 新值
     */
    public void postValue(T value) {
        super.postValue(value);
    }
    
    /**
     * 获取值（双向绑定使用）
     * @return 当前值
     */
    public T get() {
        return getValue();
    }
    
    /**
     * 判断是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        T value = getValue();
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).isEmpty();
        }
        return false;
    }
    
    /**
     * 判断是否不为空
     * @return 是否不为空
     */
    public boolean isNotEmpty() {
        return !isEmpty();
    }
    
    /**
     * 转换为LiveData对象，用于支持观察者模式
     * 由于ObservableField本身继承自MutableLiveData，这个方法直接返回自身
     * @return LiveData对象
     */
    public LiveData<T> observeAsLiveData() {
        return this;
    }
}