package com.fxzs.lingxiagent.model.wps;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.model.common.Constants;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public class WpsAuthInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder newRequestBuilder = originalRequest.newBuilder();

        String date = getRFC1123Date();
        String contentType = "application/json";
        String contentMd5 = "";

        if ("POST".equalsIgnoreCase(originalRequest.method())) {
            RequestBody body = originalRequest.body();
            if (body != null) {
                contentType = Objects.requireNonNull(body.contentType()).toString();
                Buffer buffer = new Buffer();
                body.writeTo(buffer);
                String bodyStr = buffer.readString(StandardCharsets.UTF_8);
                contentMd5 = md5(bodyStr);
            }
        } else if ("GET".equalsIgnoreCase(originalRequest.method())) {
            String path = originalRequest.url().encodedPath();
            String query = originalRequest.url().encodedQuery();
            String uri = path + (query != null ? "?" + query : "");
            contentMd5 = md5(URLDecoder.decode(uri, "UTF-8"));
        }

        String signature = sha1(Constants.WPS_APP_SECRET + contentMd5 + contentType + date);
        String authorization = "WPS-2:" + Constants.WPS_APP_KEY + ":" + signature;

        newRequestBuilder.header("Date", date);
        newRequestBuilder.header("Content-Type", contentType);
        newRequestBuilder.header("Content-MD5", contentMd5);
        newRequestBuilder.header("Authorization", authorization);

        return chain.proceed(newRequestBuilder.build());
    }

    private String getRFC1123Date() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }

    private String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return toHexString(digest);
        } catch (Exception e) {
            return "";
        }
    }

    private String sha1(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return toHexString(digest);
        } catch (Exception e) {
            return "";
        }
    }

    private String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
