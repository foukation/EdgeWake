package com.fxzs.lingxiagent.view.drawing;

import android.content.Intent;
import android.content.res.Configuration;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.main.utils.ScreenUtils;
import com.fxzs.lingxiagent.util.markdown.MarkdownStyle;
import com.fxzs.lingxiagent.view.chat.HistoryContainActivity;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.chat.VMChat;
import com.fxzs.lingxiagent.viewmodel.history.VMHistory;

import timber.log.Timber;


public class DrawingSelectActivity extends BaseActivity {

    DrawingNewFragment drawingNewFragment;


    @Override
    protected int getLayoutResource() {
        return R.layout.act_drawing_select;
    }

    @Override
    protected Class getViewModelClass() {
        return VMChat.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {
        init();
    }



    public void init() {

        findViewById(R.id.back).setOnClickListener(view -> finish());

        findViewById(R.id.iv_ai_draw_text_to_pic).setOnClickListener(view -> {
            startActivity(new Intent(DrawingSelectActivity.this, DrawingContainActivity.class));
        });

//        findViewById(R.id.btn_create_ai_draw_text_to_pic).setOnClickListener(view -> {
//            startActivity(new Intent(DrawingSelectActivity.this, DrawingContainActivity.class));
//        });

        findViewById(R.id.iv_ai_draw_transform).setOnClickListener(view -> {
            startActivity(new Intent(DrawingSelectActivity.this, DrawingTransformActivity.class));
        });
        findViewById(R.id.tv_history).setOnClickListener(view -> {
            Intent intent = new Intent(DrawingSelectActivity.this, HistoryContainActivity.class);
            intent.putExtra("default_tab",VMHistory.TAB_DRAWING);
            intent.putExtra("is_tab_hide",true);
            startActivity(intent);
        });


    }
    @Override
    protected void setupObservers() {
        // 监听 ViewModel 的 LiveData

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Timber.tag("SuperChatContainActivity").d( "onActivityResult called - requestCode: " + requestCode + ", resultCode: " + resultCode);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        Timber.tag("DrawingSelectActivity").d("screen width=%d, height=%d", displayMetrics.widthPixels, displayMetrics.heightPixels);

//        if (isTablet()) {
//            int orientation = getResources().getConfiguration().orientation;
//            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                int bottom = getResources().getDimensionPixelSize(R.dimen.dp_10);
//                android.view.View btn = findViewById(R.id.btn_create_ai_draw_text_to_pic);
//                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) btn.getLayoutParams();
//                lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
//                lp.bottomMargin = bottom;
//                btn.setLayoutParams(lp);
//            }
//        }
    }
}
