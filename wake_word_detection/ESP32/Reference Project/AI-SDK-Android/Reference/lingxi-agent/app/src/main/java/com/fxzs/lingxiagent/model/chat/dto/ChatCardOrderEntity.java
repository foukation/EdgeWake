package com.fxzs.lingxiagent.model.chat.dto;

public class ChatCardOrderEntity {

    /**
     * buttonName : 去查看
     * deepLinkUrl : https://m.ctrip.com/webapp/myctrip/orders/allorders?filterValidOrder=false\u0026status=All\u0026timeIndex=-1\u0026selectPageName=FlightOrderListPage\u0026biz=Flight%2CPreSale%2CFlightX%2CFlightCor
     * desc :
     * title : 订单信息\t
     */

    private String buttonName;
    private String deepLinkUrl;
    private String desc;
    private String title;

    public String getButtonName() {
        return buttonName;
    }

    public void setButtonName(String buttonName) {
        this.buttonName = buttonName;
    }

    public String getDeepLinkUrl() {
        return deepLinkUrl;
    }

    public void setDeepLinkUrl(String deepLinkUrl) {
        this.deepLinkUrl = deepLinkUrl;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
