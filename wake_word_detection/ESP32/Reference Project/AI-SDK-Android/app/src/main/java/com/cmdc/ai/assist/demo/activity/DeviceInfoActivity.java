package com.cmdc.ai.assist.demo.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.cmdc.ai.assist.constraint.DeviceInfoResponse;
import com.cmdc.ai.assist.demo.R;
import com.cmdc.ai.assist.demo.utils.DeviceInfo;

import org.jetbrains.annotations.NotNull;

public class DeviceInfoActivity extends BaseActivity implements View.OnClickListener {

    private TextView txtDeviceNoTypeResult;
    private TextView txtDeviceNoResult;
    private TextView txtProductIdNoResult;
    private TextView txtProductKeyResult;

    private TextView txtDeviceInfoResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.app_device_info_layout);
        initView();
        initialization();
    }

    void initView() {
        txtDeviceNoTypeResult = findViewById(R.id.txtDeviceNoTypeResult);
        txtDeviceNoResult = findViewById(R.id.txtDeviceNoResult);
        txtProductIdNoResult = findViewById(R.id.txtProductIdNoResult);
        txtProductKeyResult = findViewById(R.id.txtProductKeyResult);
        txtDeviceInfoResult = findViewById(R.id.txtDeviceInfoResult);
    }

    void initialization() {

        String deviceNoType = DeviceInfo.getDeviceNoType(this.getApplicationContext());
        String deviceNo = DeviceInfo.getDeviceNo(this.getApplicationContext());
        String productId = DeviceInfo.getProductId(this.getApplicationContext());
        String productKey = DeviceInfo.getProductKey(this.getApplicationContext());

        /*deviceNoType = "IMEI";
        deviceNo = "1008600299862537075000008";
        productId = "1845748518784040962";
        productKey = "mfyrHFRPdsFRQPAU";*/

        if (TextUtils.isEmpty(deviceNoType))
            txtDeviceNoTypeResult.setText("未获取到有效信息");
        else
            txtDeviceNoTypeResult.setText(deviceNoType);

        if (TextUtils.isEmpty(deviceNo))
            txtDeviceNoResult.setText("未获取到有效信息");
        else
            txtDeviceNoResult.setText(deviceNo);

        if (TextUtils.isEmpty(productId))
            txtProductIdNoResult.setText("未获取到有效信息");
        else
            txtProductIdNoResult.setText(productId);

        if (TextUtils.isEmpty(productKey))
            txtProductKeyResult.setText("未获取到有效信息");
        else
            txtProductKeyResult.setText(productKey);

        if (deviceNoType == null)
            deviceNoType = "";
        if (deviceNo == null)
            deviceNo = "";
        if (productId == null)
            productId = "";
        if (productKey == null)
            productKey = "";

        DeviceInfo deviceInfo = new DeviceInfo(deviceNoType, deviceNo, productId, productKey);
        deviceInfo.obtainDeviceInformation(this.getApplicationContext(), new DeviceInfo.DeviceInfoCallback() {
            @Override
            public void onSuccess(@NotNull DeviceInfoResponse deviceInfoResponse) {
                runOnUiThread(() -> txtDeviceInfoResult.setText(deviceInfoResponse.toString()));
            }

            @Override
            public void onError(@NotNull String error) {
                runOnUiThread(() -> txtDeviceInfoResult.setText(error));
            }
        });
    }

    @Override
    public void onClick(View view) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
