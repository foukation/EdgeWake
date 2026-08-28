package com.fxzs.lingxiagent.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.fxzs.lingxiagent.BuildConfig;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.common.log.FileLoggingTree;
import com.fxzs.lingxiagent.lingxi.common.log.NoLogTree;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.lingxi.nexuspilot.NexusPilotManager;

import timber.log.Timber;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2025/12/4 下午5:27
 */
public class ServerSwitchActivity extends AppCompatActivity {

    private Spinner spinnerServerList,spinnerHonorServerList,spinnerGuiServerList;
    private ImageView ivBack;
    private TextView  btnSave,btnReset,tvLog,btnSaveHonorServer,btnSaveGuiServer;
    private boolean isOpenLog;

    private final String[] serverArray = {
            "正式服务器 ① - "+Constants.BASE_HOST_URL,
            "测试服务器 ② - "+Constants.BASE_HOST_TEST_URL,
    };

    private final String[] serverUrlArray = {
            Constants.BASE_HOST_URL,
            Constants.BASE_HOST_TEST_URL,
    };

    private final String[] serverHonorArray = {
            "荣耀出行测试服务器 ② - "+Constants.BASE_URL_TEST_HONOR,
            "荣耀出行正式服务器 ③ - "+Constants.BASE_URL_SZ_HONOR,
    };

    private final String[] serverUrlHonorArray = {
            Constants.BASE_URL_TEST_HONOR,
            Constants.BASE_URL_SZ_HONOR
    };

    private final String[] serverGuiArray = {
            "GUI测试服务器",
            "GUI正式服务器",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_switch);

        spinnerServerList = findViewById(R.id.spinnerServerList);
        spinnerHonorServerList = findViewById(R.id.spinnerHonorServerList);
        spinnerGuiServerList = findViewById(R.id.spinnerGuiServerList);
        btnSave = findViewById(R.id.btnSaveServer);
        btnSaveHonorServer = findViewById(R.id.btnSaveHonorServer);
        btnSaveGuiServer = findViewById(R.id.btnSaveGuiServer);
        btnReset = findViewById(R.id.btnReset);
        ivBack = findViewById(R.id.iv_back);
        tvLog = findViewById(R.id.tv_log);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, serverArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerServerList.setAdapter(adapter);

        ArrayAdapter<String> honorAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, serverHonorArray);
        honorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHonorServerList.setAdapter(honorAdapter);

        ArrayAdapter<String> guiAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, serverGuiArray);
        guiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGuiServerList.setAdapter(guiAdapter);
        // 初始化默认选项
        initSelectedServer();
        initSelectedHonorServer();
        initSelectedGuiServer();
        // 监听选择
        spinnerServerList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 保存与切换
        btnSave.setOnClickListener(v -> {
            String selectedUrl = serverUrlArray[spinnerServerList.getSelectedItemPosition()];

            // 保存到本地
            saveServerUrl(selectedUrl);

            // 立即切换 Retrofit 地址
//            RetrofitClient.getInstance().setBaseUrl(selectedUrl);

            GlobalToast.show(this,"服务器切换成功,请重启应用" + selectedUrl, GlobalToast.Type.SUCCESS );
        });

        // 保存与切换
        btnSaveHonorServer.setOnClickListener(v -> {
            String selectedUrl = serverUrlHonorArray[spinnerHonorServerList.getSelectedItemPosition()];
            // 保存到本地
            saveServerHonorUrl(selectedUrl);
            GlobalToast.show(this,"荣耀服务器切换成功,请重启应用" + selectedUrl, GlobalToast.Type.SUCCESS );
        });

        btnSaveGuiServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int result = -2;
                String selectedUrl = serverGuiArray[spinnerGuiServerList.getSelectedItemPosition()];
                // 保存到本地
                SharedPreferencesUtil.saveString("GUI_ENV", selectedUrl);
                if (selectedUrl.equals("GUI测试服务器")) {
                    result = NexusPilotManager.INSTANCE.setEnvironment("dev");
                }else{
                    result = NexusPilotManager.INSTANCE.setEnvironment("release");
                }
                Timber.tag("ServerSwitchActivity").i("result = " + result +  " selectedUrl = " + selectedUrl);
                GlobalToast.show(ServerSwitchActivity.this,"GUI服务器切换成功", GlobalToast.Type.SUCCESS );
            }
        });

        ivBack.setOnClickListener( v ->{finish();});

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferencesUtil.saveServer(Constants.BASE_HOST_URL);
                initSelectedServer();
                SharedPreferencesUtil.saveHonorServer(Constants.BASE_URL_HONOR_CONTROL);
                initSelectedHonorServer();
                SharedPreferencesUtil.saveString("GUI_ENV", "GUI正式服务器");
                initSelectedGuiServer();
                NexusPilotManager.INSTANCE.setEnvironment("release");
                if (BuildConfig.DEBUG) {
                    isOpenLog = true;
                    Timber.plant(new FileLoggingTree(ServerSwitchActivity.this));
                    SharedPreferencesUtil.saveLogOpen(true);
                }else {
                    isOpenLog = false;
                    Timber.uprootAll();
                    Timber.plant(new NoLogTree());
                    SharedPreferencesUtil.saveLogOpen(false);
                }
                initLog();
                GlobalToast.show(ServerSwitchActivity.this,"重置成功,请重启应用\n" , GlobalToast.Type.SUCCESS );
            }
        });

        initLog();
    }

    private void initLog(){
        if (SharedPreferencesUtil.getLogOpen()) {
            isOpenLog = true;
        }
        switchLog();
        tvLog.setOnClickListener(v -> {
            isOpenLog = !isOpenLog;
            switchLog();
        });
    }

    private void switchLog(){
        if (isOpenLog) {
            Timber.DebugTree tree = new Timber.DebugTree();
            Timber.plant(tree);
            Timber.plant(new FileLoggingTree(this));
            SharedPreferencesUtil.saveLogOpen(true);
            tvLog.setText("输入日志：打开");
            tvLog.setTextColor(ResourcesCompat.getColor(getResources(),R.color.white,null));
            tvLog.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.bg_btn_logout_blue,null));
        }else {
            Timber.uprootAll();
            Timber.plant(new NoLogTree());
            SharedPreferencesUtil.saveLogOpen(false);
            tvLog.setText("输入日志：关闭");
            tvLog.setTextColor(ResourcesCompat.getColor(getResources(),R.color.color_1E1E1E,null));
            tvLog.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.bg_ai_response_card,null));

        }
    }

    private void initSelectedServer() {
        String savedUrl = SharedPreferencesUtil.getServerUrl();

        // 自动匹配预设
        for (int i = 0; i < serverUrlArray.length; i++) {
            if (serverUrlArray[i].contains(savedUrl)) {
                spinnerServerList.setSelection(i);
                return;
            }
        }

        // 否则进入自定义
        spinnerServerList.setSelection(serverUrlArray.length - 1);
    }

    private void initSelectedHonorServer() {
        String savedUrl = SharedPreferencesUtil.getServerHonorUrl();
        // 自动匹配预设
        for (int i = 0; i < serverUrlHonorArray.length; i++) {
            if (serverUrlHonorArray[i].contains(savedUrl)) {
                spinnerHonorServerList.setSelection(i);
                return;
            }
        }

        // 否则进入自定义
        spinnerHonorServerList.setSelection(serverUrlHonorArray.length - 1);
    }

    private void initSelectedGuiServer() {
        String savedUrl = SharedPreferencesUtil.getString("GUI_ENV", "GUI正式服务器");
        // 自动匹配预设
        for (int i = 0; i < serverGuiArray.length; i++) {
            if (serverGuiArray[i].contains(savedUrl)) {
                spinnerGuiServerList.setSelection(i);
                return;
            }
        }
        // 否则进入自定义
        spinnerGuiServerList.setSelection(serverGuiArray.length - 1);
    }

    private void saveServerUrl(String url) {
        SharedPreferencesUtil.saveServer(url);
    }

    private void saveServerHonorUrl(String url) {
        SharedPreferencesUtil.saveHonorServer(url);
    }
}

