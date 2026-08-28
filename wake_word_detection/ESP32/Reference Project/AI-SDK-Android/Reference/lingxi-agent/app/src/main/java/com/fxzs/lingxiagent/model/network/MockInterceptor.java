package com.fxzs.lingxiagent.model.network;

import static com.fxzs.lingxiagent.util.ZUtils.loadJSONFromAssets;

import android.content.Context;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class MockInterceptor implements Interceptor {
	private final Context context;

	public MockInterceptor(Context context) {
		this.context = context;
	}

	@NonNull
	@Override
	public Response intercept(Chain chain) {
		String json = loadJSONFromAssets(context, "mock_ppt_history.json");
		Timber.tag("MockInterceptor").d(json);

		return new Response.Builder()
				.code(200)
				.message(json)
				.request(chain.request())
				.protocol(Protocol.HTTP_1_1)
				.body(ResponseBody.create(
						MediaType.parse("application/json"),
						json.getBytes(StandardCharsets.UTF_8)))
				.build();
	}
}