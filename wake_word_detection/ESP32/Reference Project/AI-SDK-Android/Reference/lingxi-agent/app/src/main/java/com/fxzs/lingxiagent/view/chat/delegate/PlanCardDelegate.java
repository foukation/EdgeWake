package com.fxzs.lingxiagent.view.chat.delegate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.PlanProgressType;
import com.fxzs.lingxiagent.lingxi.main.utils.GsonUtils;
import com.fxzs.lingxiagent.model.chat.dto.ChatCardPlanEntity;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.model.chat.dto.EventBusSendHistoryNotifyMsg;
import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.chat.ChatAdapter;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;


public class PlanCardDelegate extends CardMessageDelegate {

    private static final String TAG = "PlanCardDelegate";

    CountDownTimer countDownTimer;

    public PlanCardDelegate() {
        super(ChatAdapter.TYPE_ASSISTANT_PLAN_CARD, R.layout.lingxi_card_travel_plan);
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ChatAdapter.ChatViewHolder(view);
    }

    @Override
    protected void onBindViewHolderInternal(RecyclerView.ViewHolder holder, ChatMessage message,
                                            int position, ChatAdapterContext context) {
        ChatAdapter.ChatViewHolder planHolder = (ChatAdapter.ChatViewHolder) holder;
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        Timber.tag(TAG).d( "setAssistantHotelCard: position=" + position);
        if (TextUtils.equals(PlanProgressType.Success.getAlias(), message.getPlanProgressType())) {
            planHolder.planProgressBar.setProgress(0);
            planHolder.llPlanCard.setVisibility(View.VISIBLE);
            planHolder.llPlanLoading.setVisibility(View.GONE);
            planHolder.llPlanLoadFail.setVisibility(View.GONE);
            // 获取酒店数据
            ChatCardPlanEntity.ContentBean planContent = message.getPlanContent();
            if (planContent != null) {
                ImageUtil.netRadius(context.getContext(), planContent.getHeader_background_image_url(), planHolder.imagePlanTop);
                planHolder.tvPlanTitle.setText(planContent.getTitle());
                planHolder.tvPlanWhere.setText(planContent.getRoute());
                planHolder.tvPlanTime.setText(planContent.getDate_range());
                planHolder.tvPlanAddress.setText(planContent.getSubtitle());
            }
            planHolder.tvPlanMore.setOnClickListener(v -> {
                startSystemSettings(context.getContext(), Intent.ACTION_VIEW, planContent.getH5Url());
            });
        } else if (TextUtils.equals(PlanProgressType.Failed.getAlias(), message.getPlanProgressType())) {
            planHolder.planProgressBar.setProgress(0);
            planHolder.llPlanCard.setVisibility(View.GONE);
            planHolder.llPlanLoading.setVisibility(View.GONE);
            planHolder.llPlanLoadFail.setVisibility(View.VISIBLE);
            planHolder.llPlanLoadFail.setOnClickListener(view -> reloadPlanData(holder, message, position, context, message.getPlanReqUrl()));
        } else if (TextUtils.equals(PlanProgressType.Loading.getAlias(), message.getPlanProgressType())) {
            planHolder.llPlanCard.setVisibility(View.GONE);
            planHolder.llPlanLoading.setVisibility(View.VISIBLE);
            planHolder.llPlanLoadFail.setVisibility(View.GONE);
            countDownTimer = new CountDownTimer(8000, 700) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int progress = 100 - (int) (millisUntilFinished / 100);
                    if (progress > planHolder.planProgressBar.getProgress()) {
                        setProgress(planHolder, context.getContext(), progress);
                    }
                }

                @Override
                public void onFinish() {
                    setProgress(planHolder, context.getContext(), 99);
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                }
            };
            countDownTimer.start();
        }

    }


    @SuppressLint("StringFormatMatches")
    private void setProgress(ChatAdapter.ChatViewHolder planHolder, Context context, int progress) {
        planHolder.tvPlanLoading.setText(String.format(context.getString(R.string.txt_plan_loading), progress));
        planHolder.planProgressBar.setProgress(progress);
    }

    private void reloadPlanData(RecyclerView.ViewHolder holder, ChatMessage message,
                                int position, ChatAdapterContext context, String url) {
        OkHttpClient client = new OkHttpClient.Builder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, IOException e) {
                ((Activity) (context.getContext())).runOnUiThread(() -> {
                    ZUtils.showToast("网络异常，请稍后再试");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ((Activity) (context.getContext())).runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        assert response.body() != null;
                        String responseData = null;
                        try {
                            responseData = response.body().string();
                            ChatCardPlanEntity chatCardPlanEntity = GsonUtils.fromJson(responseData, ChatCardPlanEntity.class);
                            if (chatCardPlanEntity.getCode() == 200 && chatCardPlanEntity.getContent() != null) {
                                message.setPlanContent(chatCardPlanEntity.getContent());
                                message.setPlanProgressType(PlanProgressType.Success.getAlias());
                                onBindViewHolderInternal(holder, message, position, context);
                                EventBus.getDefault().post(new EventBusSendHistoryNotifyMsg(GsonUtils.toJson(message)));
                            } else {
                                ZUtils.showToast(chatCardPlanEntity.getMsg());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected Class<? extends RecyclerView.ViewHolder> getExpectedViewHolderClass() {
        return ChatAdapter.ChatViewHolder.class;
    }

}