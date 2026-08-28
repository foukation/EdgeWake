package com.fxzs.lingxiagent.model.billing;

import com.fxzs.lingxiagent.model.billing.callback.BillingCallback;
import com.fxzs.lingxiagent.model.billing.repository.BillingRepository;

public class BillingManager {

    private static volatile BillingManager instance;

    private final BillingRepository repository;

    private BillingManager() {
        repository = new BillingRepository();
    }

    public static BillingManager getInstance() {

        if (instance == null) {

            synchronized (BillingManager.class) {

                if (instance == null) {
                    instance = new BillingManager();
                }
            }
        }

        return instance;
    }

    /**
     * 开始执行完整服务流程
     */
    public void start(BillingCallback callback, boolean needPackageInfo) {
        repository.start(callback, needPackageInfo);
    }

    /**
     * 清除缓存token和设备
     */
    public void clearCache() {
        repository.clearCache();
    }
}