package com.fxzs.lingxiagent.view.drawing;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class TestJumpActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Button button = new Button(this);
        button.setText("跳转到AspectRatioSelectionActivity");
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, AspectRatioSelectionActivity.class);
            startActivity(intent);
        });
        
        setContentView(button);
    }
}