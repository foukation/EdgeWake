package com.fxzs.lingxiagent.model.billing.model;

import java.util.List;

public class ServicePackageResponse {

    private String phone;
    private List<String> subscribedCategories;
    private List<PackageItem> packages;

    public String getPhone() {
        return phone;
    }

    public List<String> getSubscribedCategories() {
        return subscribedCategories;
    }

    public List<PackageItem> getPackages() {
        return packages;
    }

    public static class PackageItem {

        private String packageId;
        private String packageDesc;
        private String packageName;
        private String packageType;
        private String packageTypeDesc;
        private String categoryCode;
        private String categoryName;
        private String status;
        private String discountPrice;
        private String originalPrice;
        private String priceUnit;
        private String discountLabel;
        private String dailyDiscountPrice;

        // 你原来缺少的字段
        private String startTime;
        private String endTime;

        private String deviceNo;
        private String deviceModelId;

        private List<FeatureItem> features;

        public String getPackageId() {
            return packageId;
        }

        public String getPackageDesc() {
            return packageDesc;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getPackageType() {
            return packageType;
        }

        public String getPackageTypeDesc() {
            return packageTypeDesc;
        }

        public String getCategoryCode() {
            return categoryCode;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public String getStatus() {
            return status;
        }

        public String getDiscountPrice() {
            return discountPrice;
        }

        public String getOriginalPrice() {
            return originalPrice;
        }

        public String getPriceUnit() {
            return priceUnit;
        }

        public String getDiscountLabel() {
            return discountLabel;
        }

        public String getDailyDiscountPrice() {
            return dailyDiscountPrice;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getDeviceNo() {
            return deviceNo;
        }

        public String getDeviceModelId() {
            return deviceModelId;
        }

        public List<FeatureItem> getFeatures() {
            return features;
        }

        public static class FeatureItem {

            private String featureName;
            private String quotaDesc;

            public String getFeatureName() {
                return featureName;
            }

            public String getQuotaDesc() {
                return quotaDesc;
            }
        }
    }
}