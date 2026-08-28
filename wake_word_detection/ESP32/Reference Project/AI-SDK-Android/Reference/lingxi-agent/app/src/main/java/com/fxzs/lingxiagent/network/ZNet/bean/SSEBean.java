package com.fxzs.lingxiagent.network.ZNet.bean;

public class SSEBean {


    private Send send;
    private Receive receive;
    public void setSend(Send send) {
        this.send = send;
    }
    public Send getSend() {
        return send;
    }

    public void setReceive(Receive receive) {
        this.receive = receive;
    }
    public Receive getReceive() {
        return receive;
    }

}
