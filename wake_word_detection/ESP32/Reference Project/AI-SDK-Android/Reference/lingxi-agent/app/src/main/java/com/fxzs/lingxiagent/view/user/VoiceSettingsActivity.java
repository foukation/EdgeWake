package com.fxzs.lingxiagent.view.user;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.VoiceSettingBean;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.viewmodel.user.VMVoiceSettings;

import timber.log.Timber;

public class VoiceSettingsActivity extends BaseActivity<VMVoiceSettings> {

    private ImageView ivBack;
    private RecyclerView rv;
    private View ll_control;
    private View iv_more;
    private boolean isShowControl = false;
    private TextView name;
    private TextView tv_voice_speed;
    private TextView tv_voice_volume;
    private SeekBar spd;
    private SeekBar vol;
    private TextView tv_finish;
    private String TAG = VoiceSettingsActivity.class.getSimpleName();

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_voice_settings;
    }

    @Override
    protected Class<VMVoiceSettings> getViewModelClass() {
        return VMVoiceSettings.class;
    }

    @Override
    protected void initializeViews() {
        // 初始化控件
        ivBack = findViewById(R.id.iv_back);
        rv = findViewById(R.id.rv);
        iv_more = findViewById(R.id.iv_more);
        ll_control = findViewById(R.id.ll_control);
        name = findViewById(R.id.name);
        tv_voice_speed = findViewById(R.id.tv_voice_speed);
        tv_voice_volume = findViewById(R.id.tv_voice_volume);
        spd = findViewById(R.id.spd);
        vol = findViewById(R.id.vol);
        tv_finish = findViewById(R.id.tv_finish);
        tv_finish.setOnClickListener(v -> {
            viewModel.saveVoice();
            finish();
        });
        initSeekBars();

        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());
        iv_more.setOnClickListener(v -> {
            toggleControlPanel();
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);

//        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
//        });
    }

    private void initSeekBars() {
        spd.setMax(15);
        vol.setMax(10);
        spd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tv_voice_speed.setText(String.valueOf(progress));
                    viewModel.getCurrentVoiceOption().setSpd(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        vol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tv_voice_volume.setText(String.valueOf(progress));
                    viewModel.getCurrentVoiceOption().setVol(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }

    @Override
    protected void setupDataBinding() {
        // 声音设置不需要数据绑定
    }

    @Override
    protected void setupObservers() {
        // 观察声音列表加载
        viewModel.getVoiceList().observe(this, voiceList -> {
            if (voiceList != null && !voiceList.isEmpty()) {
                // 更新UI显示
                VoiceSettingAdapter voiceSettingAdapter = new VoiceSettingAdapter(this, voiceList, option -> {
                    Timber.tag(TAG).i("Selected option: %s", option.getName());
                    viewModel.selectVoice(option);
                    if (ll_control.getVisibility() == View.VISIBLE) toggleControlPanel();
                });
                rv.setAdapter(voiceSettingAdapter);
                VoiceSettingBean voiceSettingBean = viewModel.getCurrentVoiceOption();
                int index = -1;
                for (int i = 0; i < voiceList.size(); i++) {
                    if (voiceList.get(i).getPer() == voiceSettingBean.getPer()) {
                        index = i;
                        break;
                    }
                }
                voiceSettingAdapter.setSelectedPosition(index);
            }
        });

        // 观察声音变化
        viewModel.getVoiceChanged().observe(this, changed -> {
            if (changed != null && changed) {
                // 设置结果并返回
                VoiceSettingBean voiceSettingBean = viewModel.getCurrentVoiceOption();
                name.setText(voiceSettingBean.getName());
                spd.setProgress(voiceSettingBean.getSpd());
                tv_voice_speed.setText(String.valueOf(voiceSettingBean.getSpd()));
                vol.setProgress(voiceSettingBean.getVol());
                tv_voice_volume.setText(String.valueOf(voiceSettingBean.getVol()));
                setResult(RESULT_OK);
            }
        });

        // 观察加载状态
        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                // 显示加载中
            } else {
                // 隐藏加载中
            }
        });
    }

    private void toggleControlPanel() {
        isShowControl = !isShowControl;
        ll_control.setVisibility(isShowControl ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 设置状态栏颜色为白色，与背景一致，并保证内容不被遮挡
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            getWindow().getDecorView().postDelayed(() -> {
                getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            }, 100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }
}