package com.fxzs.lingxiagent.view.common;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.fxzs.lingxiagent.model.common.ObservableField;

import timber.log.Timber;

/**
 * 数据绑定工具类
 * 提供View和ViewModel之间的双向绑定方法
 */
public class DataBindingUtils {
    
    /**
     * 绑定EditText和ObservableField的双向绑定
     * @param editText EditText控件
     * @param observableField 可观察字段
     * @param lifecycleOwner 生命周期所有者
     */
    public static void bindEditText(EditText editText, ObservableField<String> observableField, LifecycleOwner lifecycleOwner) {
        // ViewModel -> View
        observableField.observe(lifecycleOwner, new Observer<String>() {
            @Override
            public void onChanged(String value) {
                if (value != null && !value.equals(editText.getText().toString())) {
                    editText.setText(value);
                }
            }
        });
        
        // View -> ViewModel
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                if (!text.equals(observableField.getValue())) {
                    observableField.set(text);
                }
            }
        });
    }

    public static void requestNextFocus(TextView editText, View nextView){
        editText.setOnEditorActionListener((v, actionId, event) -> {

            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                    (event != null &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {

                nextView.requestFocus();
                return true;
            }

            return false;
        });
    }

    /**
     * 绑定CheckBox和ObservableField的双向绑定
     * @param checkBox CheckBox控件
     * @param observableField 可观察字段
     * @param lifecycleOwner 生命周期所有者
     */
    public static void bindCheckBox(CheckBox checkBox, ObservableField<Boolean> observableField, LifecycleOwner lifecycleOwner) {
        // ViewModel -> View
        observableField.observe(lifecycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean checked) {
                if (checked != null && checkBox.isChecked() != checked) {
                    checkBox.setChecked(checked);
                }
            }
        });
        
        // View -> ViewModel
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (observableField.getValue() == null || observableField.getValue() != isChecked) {
                    observableField.set(isChecked);
                }
            }
        });
    }
    
    /**
     * 绑定TextView和ObservableField的单向绑定
     * @param textView TextView控件
     * @param observableField 可观察字段
     * @param lifecycleOwner 生命周期所有者
     */
    public static void bindTextView(TextView textView, ObservableField<String> observableField, LifecycleOwner lifecycleOwner) {
        observableField.observe(lifecycleOwner, new Observer<String>() {
            @Override
            public void onChanged(String value) {
                textView.setText(value != null ? value : "");
            }
        });
    }
    
    /**
     * 绑定TextView和ObservableField的单向绑定（带默认值）
     * @param textView TextView控件
     * @param observableField 可观察字段
     * @param defaultValue 默认值
     * @param lifecycleOwner 生命周期所有者
     */
    public static void bindTextView(TextView textView, ObservableField<String> observableField, String defaultValue, LifecycleOwner lifecycleOwner) {
        observableField.observe(lifecycleOwner, new Observer<String>() {
            @Override
            public void onChanged(String value) {
                textView.setText(value != null ? value : defaultValue);
            }
        });
    }
    
    /**
     * 绑定布尔值到View的可见性
     * @param view 视图
     * @param observableField 可观察字段
     * @param lifecycleOwner 生命周期所有者
     */
    public static void bindVisibility(android.view.View view, ObservableField<Boolean> observableField, LifecycleOwner lifecycleOwner) {
        observableField.observe(lifecycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean visible) {
                view.setVisibility(visible != null && visible ? android.view.View.VISIBLE : android.view.View.GONE);
            }
        });
    }
    
    /**
     * 绑定布尔值到View的可见性（支持反转）
     * @param view 视图
     * @param observableField 可观察字段
     * @param lifecycleOwner 生命周期所有者
     * @param invert 是否反转逻辑
     */
    public static void bindVisibility(android.view.View view, ObservableField<Boolean> observableField, LifecycleOwner lifecycleOwner, boolean invert) {
        observableField.observe(lifecycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean visible) {
                boolean shouldShow = visible != null && visible;
                if (invert) {
                    shouldShow = !shouldShow;
                }
                view.setVisibility(shouldShow ? android.view.View.VISIBLE : android.view.View.GONE);
            }
        });
    }
    
    /**
     * 绑定布尔值到View的启用状态
     * @param view 视图
     * @param observableField 可观察字段
     * @param lifecycleOwner 生命周期所有者
     */
    public static void bindEnabled(android.view.View view, ObservableField<Boolean> observableField, LifecycleOwner lifecycleOwner) {
        observableField.observe(lifecycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                view.setEnabled(enabled != null && enabled);
            }
        });
    }
    
    /**
     * 绑定LiveData<Boolean>到View的可见性
     * @param view 视图
     * @param liveData LiveData字段
     * @param lifecycleOwner 生命周期所有者
     */
    public static void bindVisibilityLiveData(android.view.View view, LiveData<Boolean> liveData, LifecycleOwner lifecycleOwner) {
        liveData.observe(lifecycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean visible) {
                view.setVisibility(visible != null && visible ? android.view.View.VISIBLE : android.view.View.GONE);
            }
        });
    }
    
    /**
     * 绑定LiveData<Boolean>到View的启用状态
     * @param view 视图
     * @param liveData LiveData字段
     * @param lifecycleOwner 生命周期所有者
     */
    public static void bindEnabledLiveData(android.view.View view, LiveData<Boolean> liveData, LifecycleOwner lifecycleOwner) {
        liveData.observe(lifecycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                view.setEnabled(enabled != null && enabled);
            }
        });
    }
    
    /**
     * 绑定进度值到ProgressBar
     * @param progressBar 进度条控件
     * @param observableField 可观察字段
     * @param lifecycleOwner 生命周期所有者
     */
    public static void bindProgress(android.widget.ProgressBar progressBar, ObservableField<Integer> observableField, LifecycleOwner lifecycleOwner) {
        observableField.observe(lifecycleOwner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer progress) {
                if (progress != null) {
                    progressBar.setProgress(progress);
                }
            }
        });
    }
    
    /**
     * 绑定TextView和LiveData的单向绑定（带转换器）
     * @param textView TextView控件
     * @param liveData LiveData数据源
     * @param lifecycleOwner 生命周期所有者
     * @param converter 数据转换器，将T类型转换为String
     * @param <T> 数据类型
     */
    public static <T> void bindTextView(TextView textView, LiveData<T> liveData, LifecycleOwner lifecycleOwner, DataConverter<T, String> converter) {
        liveData.observe(lifecycleOwner, new Observer<T>() {
            @Override
            public void onChanged(T value) {
                String displayText = converter.convert(value);
                // 如果转换结果为null，则设置为空字符串
                Timber.tag("DataBindingUtils").d( "### displayText: " + displayText);
                Timber.tag("DataBindingUtils").d( "### 当前线程: " + Thread.currentThread().getName() + ", textView.isAttachedToWindow():" + textView.isAttachedToWindow()
                + ", textView.getWindowVisibility():" + textView.getWindowVisibility() + ", textView.getVisibility():" + textView.getVisibility());
                Timber.tag("DataBindingUtils").d( "### 是否主线程: " + (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()));
                
                // 确保UI更新在主线程执行
                if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                    // 已在主线程，直接更新
                    textView.setText(displayText != null ? displayText : "");
                    textView.invalidate();
                    Timber.tag("DataBindingUtils").d( "### 主线程直接更新UI");
                } else {
                    // 切换到主线程更新UI
                    textView.post(() -> {
                        textView.setText(displayText != null ? displayText : "");
                        Timber.tag("DataBindingUtils").d( "### 通过post更新UI");
                    });
                }
            }
        });
    }
    
    /**
     * 绑定Button的启用状态
     * @param button Button控件
     * @param observableField 可观察字段
     * @param lifecycleOwner 生命周期所有者
     */
    public static void bindButtonEnabled(android.widget.Button button, ObservableField<Boolean> observableField, LifecycleOwner lifecycleOwner) {
        observableField.observe(lifecycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean enabled) {
                button.setEnabled(enabled != null && enabled);
            }
        });
    }
    
    /**
     * 数据转换器接口
     * @param <T> 源数据类型
     * @param <R> 目标数据类型
     */
    public interface DataConverter<T, R> {
        R convert(T input);
    }
}