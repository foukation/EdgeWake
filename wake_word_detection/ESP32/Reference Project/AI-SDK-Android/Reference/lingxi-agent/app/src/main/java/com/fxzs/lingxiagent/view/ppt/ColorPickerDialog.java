package com.fxzs.lingxiagent.view.ppt;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.fxzs.lingxiagent.R;

public class ColorPickerDialog extends DialogFragment {
    
    private LinearLayout colorContainer;
    private OnColorSelectedListener listener;
    
    public interface OnColorSelectedListener {
        void onColorSelected(String color);
    }
    
    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.ColorPickerDialog);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_color_picker, container, false);
        
        initViews(view);
        setupColors();
        
        return view;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }
    
    private void initViews(View view) {
        colorContainer = view.findViewById(R.id.color_container);
        view.findViewById(R.id.close_button).setOnClickListener(v -> dismiss());
    }
    
    private void setupColors() {
        String[] colors = {
            "rainbow",
            "#FF5722", // Red
            "#FF9800", // Orange
            "#4CAF50", // Green
            "#FF6347", // Tomato
            "#2196F3", // Blue
            "#9C27B0", // Purple
            "#03A9F4", // Light Blue
            "#E91E63"  // Pink
        };
        
        for (String color : colors) {
            View colorView = createColorView(color);
            colorContainer.addView(colorView);
        }
    }
    
    private View createColorView(String color) {
        ImageView colorView = new ImageView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(48, 48);
        params.setMargins(8, 8, 8, 8);
        colorView.setLayoutParams(params);
        
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        
        if ("rainbow".equals(color)) {
            int[] gradientColors = {
                Color.parseColor("#FF5722"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#FFEB3B"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#9C27B0")
            };
            drawable.setColors(gradientColors);
            drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        } else {
            drawable.setColor(Color.parseColor(color));
        }
        
        colorView.setImageDrawable(drawable);
        colorView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onColorSelected(color);
            }
            dismiss();
        });
        
        return colorView;
    }
}