package com.fxzs.lingxiagent.network.ZNet;


import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
public abstract class Request {
    final Retrofit retrofit;

    public Request() {
        this.retrofit = RetrofitClient.getInstance().getRetrofit();
    }


    public  <T> void toSubscribe(Observable<T> o, Observer<T> s) {
        o.subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s);
    }
}
