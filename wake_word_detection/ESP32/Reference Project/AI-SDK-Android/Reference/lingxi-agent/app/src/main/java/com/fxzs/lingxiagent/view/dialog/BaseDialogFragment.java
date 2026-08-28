package com.fxzs.lingxiagent.view.dialog;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.fxzs.lingxiagent.R;


public class BaseDialogFragment extends DialogFragment {
    View mView;
    ViewGroup mContainer;
    public int getLayout(){
        return 0;
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_base, container, false);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE,R.style.DialogFullScreen);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.rl_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        mContainer = view.findViewById(R.id.container);
        mView = LayoutInflater.from(getActivity()).inflate(getLayout(),  mContainer,false);
        mContainer.removeAllViews();
        mContainer.addView(mView);
    }

    @Override
    public void onStart() {
        super.onStart();
        initParams();//初始化弹窗的参数
    }
    private void initParams() {
        Window window = getDialog().getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            //调节灰色背景透明度[0-1]，默认0.5f
            lp.dimAmount = 0.5f;
            //是否在底部显示
            lp.gravity = Gravity.BOTTOM;
            window.setAttributes(lp);
        }
        setCancelable(true);//设置点击外部是否消失
    }
}
