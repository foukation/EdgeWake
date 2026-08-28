package com.fxzs.lingxiagent.lingxi.lingxi_conversation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.lingxi.config.ChatFlowCallback;
import com.fxzs.lingxiagent.lingxi.service_api.data.FoodList;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardHotelModel;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardOrderEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardPlanEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardPlandEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardTrainEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatPlanResultEntity;
import com.fxzs.lingxiagent.model.honor.dto.CardData;
import com.fxzs.lingxiagent.util.TimberUtils;
import com.fxzs.lingxiagent.util.TypingPerformanceUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HonorQueueManager {
	static String TAG = "HonorQueueManager";

	private static final Handler handler = new Handler(Looper.getMainLooper());
	private static final AtomicBoolean isTyping = new AtomicBoolean(false);
	private static final Queue<TypingTask> typingQueue = new LinkedList<>();
	private static boolean isStopped = false;
	private static ChatFlowCallback callback;
	private static Context context;

	public HonorQueueManager(Context queueManagerContext, ChatFlowCallback callback) {
		HonorQueueManager.callback = callback;
		typingQueue.clear();
		handler.removeCallbacksAndMessages(null);
		isTyping.set(false);
		isStopped = false;
		context = queueManagerContext != null
				? queueManagerContext.getApplicationContext()
				: null;
	}

	// 定义任务结构
	public static class TypingTask {
		public String content;
		public int startIndex;
		public HonorDataType type;
		public CardData cardData;

		// 思维链
		public TypingTask(HonorDataType type, String content, int startIndex) {
			this.type = type;
			this.content = content;
			this.startIndex = startIndex;
		}

		// 富文本
		public TypingTask(HonorDataType type, String content) {
			this.type = type;
			this.content = content;
		}

		// 卡片
		public TypingTask(HonorDataType type, CardData cardData) {
			this.type = type;
			this.cardData = cardData;
		}
	}

	//  加入队列
	public void enqueueTypingTask(HonorDataType type, String content, int startIndex) {
		typingQueue.offer(new TypingTask(type, content, startIndex));
	}

	//  加入队列
	public void enqueueTypingTask(HonorDataType type, String content) {
		typingQueue.offer(new TypingTask(type, content));
	}

	//  加入队列
	public void enqueueTypingTask(HonorDataType type, CardData cardData) {
		typingQueue.offer(new TypingTask(type, cardData));
	}

	// 开始安全执行队列（10秒超时结束）
	public static void executeTypingQueueSafely(int retryCount) {
		if (retryCount > 200 || isStopped) {
			stopTypingTask();
			if (callback != null) {
				callback.end();
			}
			TimberUtils.logLong(TAG,"执行已结束");
			return;
		}

		if (typingQueue.isEmpty()) {
			handler.postDelayed(() -> executeTypingQueueSafely(retryCount + 1), 100);
		} else {
			runNextTask();
		}
	}

	// 开始安全结束队列
	public static void executeEndQueueSafely(Runnable action) {
		if (action == null) return;
		Runnable checkTask = new Runnable() {
			@Override
			public void run() {
				if (isTyping.get()) {
					handler.postDelayed(this, 100);
				} else if (!typingQueue.isEmpty()) {
					handler.postDelayed(this, 100);
				} else {
					action.run();
				}
			}
		};

		handler.post(checkTask);
	}

	//  执行下一个任务
	private static void runNextTask() {
		if (!typingQueue.isEmpty() && !isTyping.get() && !isStopped) {
			TypingTask next = typingQueue.poll();
			if (next == null) {
				isTyping.set(false);
				return;
			}

			isTyping.set(true);
			if (next.type.equals(HonorDataType.THINK)) {
				playTypingEffectThink(context, next);
			} else if (next.type.equals(HonorDataType.RICH_TEXT)) {
				playTypingEffectThink(context, next);
			} else if (next.type.equals(HonorDataType.CARD)) {
				playTypingEffectCard(next);
			} else {
				callback.receiveCot(next.content);
				isTyping.set(false);
				runNextTask();
			}
		} else {
			executeTypingQueueSafely(0);
		}
	}

	// 可清空队列（用于中断或重置）
	public static void stopTypingTask() {
		handler.removeCallbacksAndMessages(null);
		isStopped = true;
		typingQueue.clear();
		isTyping.set(false);
	}

	public static void clearCallback() {
		callback = null;
	}

	// 打印队列内容
	public static void printTypingQueue() {
		if (typingQueue.isEmpty()) {
			TimberUtils.logLong(TAG, "🟡 当前队列为空");
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("🔹 当前队列任务列表（共 ").append(typingQueue.size()).append(" 个）：\n");

		int index = 1;
		for (TypingTask task : typingQueue) {
			sb.append("  ")
					.append(index++)
					.append("️⃣ 类型: ").append(task.type)
					.append("\n");
		}
		TimberUtils.logLong(TAG, sb.toString());
	}

	// 思维链逐字显示效果-任务队列
	@SuppressLint("SetTextI18n")
	public static void playTypingEffectThink(Context context, TypingTask task) {
		if (task == null || task.content == null) return;

		final String content = task.content;
		final int contentLength = content.length();
		int startIndex = Math.max(task.startIndex, 0);

		if (startIndex >= contentLength) {
			isTyping.set(false);
			runNextTask();
			return;
		}

		final boolean isThinkType = HonorDataType.THINK.equals(task.type);
		final boolean isRichTextType = HonorDataType.RICH_TEXT.equals(task.type);

		// 阈值配置
		final int TYPE_LIMIT = 300;   // 前打字
		final int FAST_LIMIT = 800;   // 中段加速

		// Phase 3 极速刷参数
		final int FAST_STEP = 60;
		final int FAST_DELAY = 48;
		final int UI_UPDATE_EVERY = 3;

		final int baseStep = TypingPerformanceUtil.calculateTypingStep(context);
		final int delay = baseStep >= 4 ? 32 : 16;

		StringBuilder builder = new StringBuilder(
				startIndex > 0 ? content.substring(0, startIndex) : ""
		);

		int[] index = {startIndex};
		int[] fastFrame = {0};

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					if (index[0] >= contentLength) {
						isTyping.set(false);
						runNextTask();
						return;
					}

					int step;
					int delayLocal;

					if (index[0] < TYPE_LIMIT) {
						// Phase 1：前打字
						step = Math.max(1, baseStep);
						delayLocal = delay;
					} else if (index[0] < FAST_LIMIT) {
						// Phase 2：中段加速
						step = 10;
						delayLocal = delay;
					} else {
						// Phase 3：后快刷
						step = FAST_STEP;
						delayLocal = FAST_DELAY;
					}

					int next = Math.min(index[0] + step, contentLength);
					builder.append(content, index[0], next);
					index[0] = next;

					// Phase 3 降低 UI 刷新频率
					if (index[0] < FAST_LIMIT || fastFrame[0]++ % UI_UPDATE_EVERY == 0 || index[0] >= contentLength) {
						if (isThinkType) {
							callback.receiveCot(builder.toString());
						} else if (isRichTextType) {
							callback.receiveChat(builder.toString());
						}
					}
					handler.postDelayed(this, delayLocal);
				} catch (Exception e) {
					e.printStackTrace();
					isTyping.set(false);
					runNextTask();
				}
			}
		};

		handler.post(runnable);
	}

	// 富文本格式任务
	private static void playTypingEffectRichText(TypingTask task) {
		if (callback != null) {
			callback.receiveChat(task.content);
		}
		isTyping.set(false);
		runNextTask();
	}

	// 卡片任务
	private static void playTypingEffectCard(TypingTask task) {
		handler.postDelayed(() -> {
			if (callback != null) {
				callback.end();
			}
			stopTypingTask();
			showHonorServiceCard(task.cardData);
		}, 300);
	}

	@SuppressLint({"NewApi", "LocalSuppress"})
	private static void showHonorServiceCard(CardData cardData) {
		Gson gson = new GsonBuilder().create();
		String type = cardData.getType();
		if (type.equals(ServiceTemplateType.PLANE.getAlias())) {
			List<ChatCardPlandEntity> planeEntities = gson.fromJson(cardData.getContent(), new TypeToken<List<ChatCardPlandEntity>>() {
			}.getType());
			callback.receivePlaneCard(planeEntities);
		} else if (type.equals(ServiceTemplateType.TRAIN.getAlias())) {
			List<ChatCardTrainEntity> trainEntities = gson.fromJson(cardData.getContent(), new TypeToken<List<ChatCardTrainEntity>>() {
			}.getType());
			callback.receiveTrainCard(trainEntities);
		} else if (type.equals(ServiceTemplateType.HOTEL.getAlias())) {
			List<ChatCardHotelModel> hotelModel = gson.fromJson(cardData.getContent(), new TypeToken<List<ChatCardHotelModel>>() {
			}.getType());
			callback.receiveHotelCard(hotelModel);
		} else if (type.equals(ServiceTemplateType.HOME.getAlias())) {
			ChatPlanResultEntity chatPlanResultEntity = gson.fromJson(cardData.getContent(), ChatPlanResultEntity.class);
			callback.receivePlanCard(PlanProgressType.Loading, null, chatPlanResultEntity.getUrl());
			new Handler(Looper.getMainLooper()).postDelayed(() -> getPlanData(chatPlanResultEntity.getUrl()), 5000);
		} else if (type.equals(ServiceTemplateType.ORDER.getAlias())) {
			ChatCardOrderEntity chatCardOrderEntity = gson.fromJson(cardData.getContent(), ChatCardOrderEntity.class);
			callback.receiveOrderCard(chatCardOrderEntity);
		} else if (type.equals(ServiceTemplateType.FOOD.getAlias())) {
			FoodList foodList = gson.fromJson(cardData.getContent(), FoodList.class);
			if (!foodList.getList().isEmpty()) {
				callback.receiveFoodCard(foodList);
			}
		}
	}

	private static void getPlanData(String url) {
		OkHttpClient client = new OkHttpClient.Builder()
				.build();
		Request request = new Request.Builder()
				.url(url)
				.build();
		Gson gson = new GsonBuilder().create();
		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(@NonNull Call call, IOException e) {
				callback.receivePlanCard(PlanProgressType.Failed, null, url);
			}

			@Override
			public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
				if (response.isSuccessful()) {
					assert response.body() != null;
					String responseData = response.body().string();
					ChatCardPlanEntity chatCardPlanEntity = gson.fromJson(responseData, ChatCardPlanEntity.class);
					if (chatCardPlanEntity.getCode() == 200) {
						if (chatCardPlanEntity.getTaskstatus() == 1) {
							callback.receivePlanCard(PlanProgressType.Success, chatCardPlanEntity.getContent(), url);
						} else if (chatCardPlanEntity.getTaskstatus() == 0) {
							new Handler(Looper.getMainLooper()).postDelayed(() -> getPlanData(url),1000);
						} else {
							callback.receivePlanCard(PlanProgressType.Failed, null, url);
						}
					} else {
						callback.receivePlanCard(PlanProgressType.Failed, null, url);
					}
				}
			}
		});
	}
}