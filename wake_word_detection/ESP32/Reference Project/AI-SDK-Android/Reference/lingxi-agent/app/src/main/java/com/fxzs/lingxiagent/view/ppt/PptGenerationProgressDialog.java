package com.fxzs.lingxiagent.view.ppt;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.fxzs.lingxiagent.R;

public class PptGenerationProgressDialog extends DialogFragment {
    
    private TextView titleText;
    private ImageView step1Icon;
    private TextView step1Text;
    private ImageView step2Icon;
    private TextView step2Text;
    private ProgressBar progressBar;
    private TextView progressText;
    private ImageButton closeButton;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private int currentProgress = 0;
    private boolean isGenerating = true;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.FullScreenDialog);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_ppt_generation_progress, container, false);
        
        initViews(view);
        startProgress();
        
        return view;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
    
    private void initViews(View view) {
        titleText = view.findViewById(R.id.title_text);
        step1Icon = view.findViewById(R.id.step1_icon);
        step1Text = view.findViewById(R.id.step1_text);
        step2Icon = view.findViewById(R.id.step2_icon);
        step2Text = view.findViewById(R.id.step2_text);
        progressBar = view.findViewById(R.id.progress_bar);
        progressText = view.findViewById(R.id.progress_text);
        closeButton = view.findViewById(R.id.close_button);
        
        closeButton.setOnClickListener(v -> {
            isGenerating = false;
            dismiss();
        });
    }
    
    private void startProgress() {
        updateProgress();
    }
    
    private void updateProgress() {
        if (!isGenerating || !isAdded()) {
            return;
        }
        
        currentProgress += 2;
        
        if (currentProgress <= 50) {
            // Step 1: 渲染PPT页面
            step1Icon.setImageResource(R.drawable.ic_progress_active);
            step1Text.setTextColor(Color.parseColor("#4CAF50"));
        } else if (currentProgress <= 100) {
            // Step 1 completed
            step1Icon.setImageResource(R.drawable.ic_check_circle);
            step1Text.setTextColor(Color.parseColor("#4CAF50"));
            
            // Step 2: 生成PPT
            step2Icon.setImageResource(R.drawable.ic_progress_active);
            step2Text.setTextColor(Color.parseColor("#4CAF50"));
        }
        
        progressBar.setProgress(currentProgress);
        progressText.setText(currentProgress + "%");
        
        if (currentProgress < 92) {
            handler.postDelayed(this::updateProgress, 100);
        } else if (currentProgress == 92) {
            // Pause at 92% as shown in the design
            handler.postDelayed(() -> {
                currentProgress = 92;
                updateProgress();
            }, 2000);
        } else if (currentProgress < 100) {
            handler.postDelayed(this::updateProgress, 200);
        } else {
            // Complete
            step2Icon.setImageResource(R.drawable.ic_check_circle);
            handler.postDelayed(this::dismiss, 500);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isGenerating = false;
        handler.removeCallbacksAndMessages(null);
    }
}