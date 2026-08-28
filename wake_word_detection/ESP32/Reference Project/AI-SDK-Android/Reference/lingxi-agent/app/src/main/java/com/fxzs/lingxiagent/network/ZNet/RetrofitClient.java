package com.fxzs.lingxiagent.network.ZNet;

import android.text.TextUtils;

import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.model.deepresearch.api.DeepResearchApiService;
import com.fxzs.lingxiagent.model.honor.api.HonorApiService;
import com.fxzs.lingxiagent.model.network.UnityHeaderInterceptor;
import com.fxzs.lingxiagent.model.scene.api.SceneApiService;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class RetrofitClient {


    private static String TAG = "RetrofitClient";
    private static final RetrofitClient instance = new RetrofitClient();

    private final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(Constants.BASE_URL) //基础url,其他部分在GetRequestInterface里
            .client(httpClient())

            .addConverterFactory(GsonConverterFactory.create(new GsonBuilder()
                    .setLenient()
                    .create()))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    public static RetrofitClient getInstance() {
        return instance;
    }

    private OkHttpClient httpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
//                Timber.tag(TAG).e( message);
                ZUtils.print(message);
            }
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request;

                String token = SharedPreferencesUtil.getToken();
                //TODO 上线删除 306de8b9b5f5437880ad99e54e1e0994
//                token = "306de8b9b5f5437880ad99e54e1e0994";
                if(!TextUtils.isEmpty(token)){
                 request = chain.request()
                        .newBuilder()
                        .addHeader("Authorization","Bearer "+token)
                        .build();
                } else {
                request = chain.request()
                        .newBuilder()
                        .build();
                }
                ZUtils.print("request = "+request.toString());
                ZUtils.print("request headers = "+request.headers().toString());


                Response response = chain.proceed(request);

                // 解析响应体
                if (response.isSuccessful()) {
                    try {
                        // 获取响应体的字符串（注意：只能读取一次）
                        String responseBodyString = response.body().string();
                        ZUtils.print("response body = " + responseBodyString);

                        // 使用 Gson 解析为 ApiResponse
                        Gson gson = new GsonBuilder().setLenient().create();
                        Type type = new TypeToken<ApiResponse<Object>>() {}.getType();
                        ApiResponse<Object> apiResponse = gson.fromJson(responseBodyString, type);

                        // 判断 code
                        if (apiResponse.getCode() == 0) {
                            ZUtils.print("Request succeeded with code: " + apiResponse.getCode());
                        } else {
                            ZUtils.print("Request failed with code: " + apiResponse.getCode() + ", msg: " + apiResponse.getMsg());
                            // 可根据 code 进行特定处理，例如抛出异常或修改响应

//                            MyApp.getContext().startActivity(new Intent( MyApp.getContext(), LoginActivity.class));
//                            EventBus.getDefault().post(new LoginEvent());
                        }

                        // 重新构建响应体，因为 body 已被消费
                        ResponseBody newResponseBody = ResponseBody.create(
                                response.body().contentType(),
                                responseBodyString
                        );
                        response = response.newBuilder().body(newResponseBody).build();
                    } catch (Exception e) {
                        ZUtils.print("Error parsing response: " + e.getMessage());
                    }
                } else {
                    ZUtils.print("Request failed with HTTP code: " + response.code());
                }


                return response;
            }
        };
        return new OkHttpClient.Builder()
//                .addInterceptor(new AccessTokenInterceptor())
                .addInterceptor(loggingInterceptor)
                .addInterceptor(new UnityHeaderInterceptor())
                .addInterceptor(interceptor)
                .connectTimeout(20, TimeUnit.SECONDS)
                .build();
    }


    private static OkHttpClient httpClientSSE() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
//                Timber.tag(TAG).e( message);
                ZUtils.print(message);
            }
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request;

                String token = SharedPreferencesUtil.getToken();
                if(!TextUtils.isEmpty(token)){
                    request = chain.request()
                            .newBuilder()
                            .header("Authorization","Bearer "+token)
//                            .header("Accept", "application/json") // 明确指定 SSE
//                            .header("Connection", "keep-alive") // 确保长连接
                            .header("Accept", "text/event-stream") // 明确指定 SSE
                            .build();
                } else {
                    request = chain.request()
                            .newBuilder()
                            .build();
                }
                ZUtils.print("httpClientSSE request = "+request.toString());
                ZUtils.print("httpClientSSE request headers = "+request.headers().toString());
                Timber.tag(TAG).d("httpClientSSE request = "+request.toString() + "httpClientSSE request headers = "+request.headers().toString());


                Response response = chain.proceed(request);
                ZUtils.print("httpClientSSE response = "+response.toString());


                return response;
            }
        };
        return new OkHttpClient.Builder()
//                .addInterceptor(new AccessTokenInterceptor())
//                .addInterceptor(loggingInterceptor)
                .addInterceptor(interceptor)
                .addInterceptor(new UnityHeaderInterceptor())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.MINUTES)
                .build();
    }

    private static OkHttpClient DeepResearchHttpClientSSE() {
        // 创建一个不验证证书链的信任管理器
        final TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        SSLContext sslContext = null;
        try{

            // 安装所有信任的信任管理器
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());


        }catch (Exception e){
            e.printStackTrace();
        }
        // 创建一个不验证主机名的SSLSocketFactory
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
//                Timber.tag(TAG).e( message);
                ZUtils.print(message);
            }
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request;

                String token = SharedPreferencesUtil.getToken();
                if(!TextUtils.isEmpty(token)){
                    request = chain.request()
                            .newBuilder()
                            .header("Authorization","Bearer "+token)
//                            .header("Accept", "application/json") // 明确指定 SSE
//                            .header("Connection", "keep-alive") // 确保长连接
                            .header("Accept", "text/event-stream") // 明确指定 SSE
                            .build();
                } else {
                    request = chain.request()
                            .newBuilder()
                            .build();
                }
                ZUtils.print("httpClientSSE request = "+request.toString());
                ZUtils.print("httpClientSSE request headers = "+request.headers().toString());
                Timber.tag(TAG).d("httpClientSSE request = "+request.toString() + "httpClientSSE request headers = "+request.headers().toString());


                Response response = chain.proceed(request);
                ZUtils.print("httpClientSSE response = "+response.toString());


                return response;
            }
        };
        return new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0])
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true; // 不验证主机名
                    }
                })
//                .addInterceptor(new AccessTokenInterceptor())
//                .addInterceptor(loggingInterceptor)
                .addInterceptor(interceptor)
                .connectTimeout(20, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.MINUTES)
                .build();
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    public static SseApi createSseApi() {
//        OkHttpClient client = new OkHttpClient.Builder()
//
////                .addInterceptor(loggingInterceptor)
//                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(httpClientSSE())
                .addConverterFactory(GsonConverterFactory.create()) // 添加 Gson 转换器
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        return retrofit.create(SseApi.class);
    }

    public static HonorApiService createHonorApi() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL_HONOR_CONTROL)
                .client(httpClientSSE())
                .addConverterFactory(GsonConverterFactory.create()) // 添加 Gson 转换器
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        return retrofit.create(HonorApiService.class);
    }

    public static SceneApiService createSceneApi() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL_SCENE)
                .client(httpClientSSE())
                .addConverterFactory(GsonConverterFactory.create()) // 添加 Gson 转换器
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        return retrofit.create(SceneApiService.class);
    }
    public static DeepResearchApiService createDeepResearchApi() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL_DEEP_RESEARCH)
                .client(httpClientSSE())
                .addConverterFactory(GsonConverterFactory.create()) // 添加 Gson 转换器
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        return retrofit.create(DeepResearchApiService.class);
    }


}
