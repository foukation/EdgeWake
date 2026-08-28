package com.fxzs.lingxiagent.view.aiwork;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;
import com.fxzs.lingxiagent.model.chat.dto.IconTextItem;
import com.fxzs.lingxiagent.util.ShadowUtils;
import com.fxzs.lingxiagent.util.ZDpUtils;
import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.view.chat.IconTextAdapter;
import com.fxzs.lingxiagent.view.common.HistoryMenuPopup;
import com.fxzs.lingxiagent.view.user.HistoryAdapter;
import com.fxzs.lingxiagent.view.user.HistoryItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import timber.log.Timber;


public class AiWorkAdapter extends RecyclerView.Adapter<AiWorkAdapter.ChatViewHolder> {
    private static final String TAG = "AiWorkAdapter";
    public static final int TYPE_LOADING = 101;//全部
    public static final int TYPE_BOTTOM_HINT = 102;//已滑到底部
    public static final int TYPE_ALL = 100;//全部
    public static final int TYPE_MEETING = 0;//会议
    public static final int TYPE_PPT = 1;//ppt
    public static final int TYPE_TRANSLATE = 2;//同声传译
    public static final int TYPE_DRAWING = 3;//ai绘画
    public static final int TYPE_CHAT = 4;//对话
    public static final int TYPE_AGENT = 5;//智能体


    private List<HistoryItem> list;
    Context context;
    private static final int POPUP_HEIGHT = 160;
    private static final int POPUP_WIDTH = 128;
    private static final int POPUP_DISTANCE = 16;

    int type = TYPE_MEETING;
    private boolean showBottomHint = false;

    private HistoryAdapter.OnMoreActionListener onMoreActionListener;
    private HistoryAdapter.OnItemClickListener onItemClickListener;
    private HistoryMenuPopup menuPopup;

    public AiWorkAdapter(Context context, List<HistoryItem> datas) {
        Timber.tag(TAG).d("AiWorkAdapter: Constructor called");
        this.context = context;
        this.list = (datas != null) ? new ArrayList<>(datas) : new ArrayList<>();

    }

    public AiWorkAdapter(Context context) {
        Timber.tag(TAG).d("AiWorkAdapter: Constructor called");
        this.context = context;
        this.list = new ArrayList<>();
    }

    public void setItems(List<HistoryItem> items) {
        // 防御性拷贝，避免外部对同一实例的原地修改引发预取期不一致
        this.list = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
        notifyDataSetChanged();
    }


    public void setType(int type) {
        this.type = type;
        notifyDataSetChanged();
    }

    public int getType() {
        return type;
    }

    public void setShowBottomHint(boolean show) {
        if (this.showBottomHint != show) {
            this.showBottomHint = show;
            notifyDataSetChanged();
        }
    }

    public interface OnMoreActionListener {
        void onMoreAction(View anchor, HistoryItem item, int actionType);
    }

    public void setOnMoreActionListener(HistoryAdapter.OnMoreActionListener listener) {
        this.onMoreActionListener = listener;
    }

    public void setOnItemClickListener(HistoryAdapter.OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int res = 0;
        if (viewType == TYPE_MEETING) {
            res = R.layout.item_work_meeting;
        } else if (viewType == TYPE_PPT) {
            res = R.layout.item_work_ppt;
        } else if (viewType == TYPE_DRAWING) {
            res = R.layout.item_work_drawing;
        } else if (viewType == TYPE_TRANSLATE) {
            res = R.layout.item_work_translate;
        } else if (viewType == TYPE_AGENT) {
            res = R.layout.item_work_agent;
        } else if (viewType == TYPE_CHAT) {
            res = R.layout.item_work_chat;
        } else if (viewType == TYPE_LOADING) {
            res = R.layout.item_history_loading;
        } else if (viewType == TYPE_BOTTOM_HINT) {
            res = R.layout.item_history_bottom_hint;
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(res, parent, false);
        return new ChatViewHolder(view);
    }

//    @Override
//    public void onBindViewHolder(ChatViewHolder holder, int position, List<Object> payloads) {
//
//        Timber.tag(TAG).d( "onBindViewHolder: position=" + position);
//        HistoryItem data = list.get(position);

    /// /        setUI(holder, message, position);
//
//        ((ChatViewHolder) holder).bind(data);
//    }
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        if (position < 0 || position >= getItemCount()) {
            return;
        }
        int viewType = getItemViewType(position);
        if (viewType == TYPE_BOTTOM_HINT) {
            // 底部提示项不需要绑定数据
            return;
        }
        HistoryItem data = list.get(position);
        ((ChatViewHolder) holder).bind(holder, data);
    }

    @Override
    public int getItemCount() {
        int count = list == null ? 0 : list.size();
        if (showBottomHint && count > 0) {
            count += 1; // 添加底部提示项
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        int dataSize = list == null ? 0 : list.size();
        // 如果是底部提示位置
        if (showBottomHint && position == dataSize) {
            return TYPE_BOTTOM_HINT;
        }
        if (position < 0 || list == null || position >= list.size()) {
            return TYPE_LOADING;
        }
        if (type == TYPE_ALL) {
            // 在"全部"模式下，依据每条数据的 extraData 中的源类型动态选择布局
            try {
                Object t = list.get(position).getExtraData("sourceType");
                if (t instanceof Integer) {
                    return (Integer) t;
                }
            } catch (Exception ignore) {
            }
            return TYPE_MEETING; // 兜底
        } else {
            try {
                Object t = list.get(position).getExtraData("sourceType");
                if (t instanceof Integer) {
                    return (Integer) t;
                }
            } catch (Exception ignore) {
            }
            return type;
        }

    }

    public class ChatViewHolder extends RecyclerView.ViewHolder {
        public TextView tv_title;
        public TextView tv_content;
        public TextView tv_date;
        public ImageView iv_content;
        public ImageView iv_avatar;
        public View ll_card;
        public View iv_more;
        public View rl_container;


        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_content = itemView.findViewById(R.id.tv_content);
            tv_date = itemView.findViewById(R.id.tv_date);
            ll_card = itemView.findViewById(R.id.ll_card);
            iv_content = itemView.findViewById(R.id.iv_content);
            rl_container = itemView.findViewById(R.id.rl_container);
            iv_more = itemView.findViewById(R.id.iv_more);
            iv_avatar = itemView.findViewById(R.id.iv_avatar);

        }


        void bind(ChatViewHolder holder, HistoryItem data) {
            int viewType = holder.getItemViewType();
            if (viewType == TYPE_LOADING) {
                return;
            }
//            ShadowUtils.applyDefaultShadow(ll_card,context);
            ShadowUtils.applyShadow(
                    ll_card,
                    context,
                    4, // lower elevation to reduce bottom shadow thickness
                    ContextCompat.getColor(context, R.color.color_606F8B),
                    16, // match card corner radius to background for consistent shadow shape
                    false,
                    Color.TRANSPARENT,
                    0,
                    Color.WHITE
            );
            tv_title.setText(data.getTitle());
            if (data.getCreateTime() == 0) {
                tv_date.setVisibility(View.GONE);
            } else {
                tv_date.setVisibility(View.VISIBLE);
                tv_date.setText(ZUtils.getDateFromTimestamp(data.getCreateTime()));
            }
            if (tv_content != null) {
                ZUtils.print("data.getTitle() = " + data.getTitle());
                ZUtils.print("data.getSubtitle() = " + data.getSubtitle());
                tv_content.setVisibility(View.GONE);
                if (data.getSubtitle() != null) {
                    if (!data.getSubtitle().startsWith("{") && !data.getSubtitle().startsWith("[")) {
                        tv_content.setText(data.getSubtitle());
                        tv_content.setVisibility(View.VISIBLE);
                    }
                }
                if (viewType == TYPE_AGENT || viewType == TYPE_CHAT) {
                    try {
                        Gson gson = new Gson();
                        Type type = new TypeToken<ChatMessage>() {
                        }.getType();
                        ChatMessage aiMsg = gson.fromJson(data.getSubtitle(), type);
                        if (!TextUtils.isEmpty(aiMsg.getMessage())) {
                            tv_content.setText(aiMsg.getMessage());
                        } else {
                            tv_content.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        ZUtils.print("Exception = " + e);
                    }
                }
            }

            if (viewType == TYPE_DRAWING) {
                setDrawingUI(this, data);
            } else if (viewType == TYPE_PPT) {
                setPPTUI(this, data);
            } else if (viewType == TYPE_AGENT) {
                setAgentUI(this, data);
            }
            // 设置点击事件
            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    // 根据不同的数据类型判断是否可以点击
                    boolean canClick = (data.getSessionId() != null) ||
                            (data.getConversationId() != null) ||
                            (data.getMeetingId() != null) ||
                            (data.getPptSessionId() != null);
                    if (canClick) {
                        onItemClickListener.onItemClick(data);
                    }
                }
            });
            iv_more.setOnClickListener(v -> {
//                Timber.tag("HistoryAdapter").d( "ivMore clicked, isDrawingTab=" + isDrawingTab + ", moreActionListener=" + (moreActionListener != null));
                if (onMoreActionListener != null) {
                    // 使用自定义弹窗
                     menuPopup =
                            new HistoryMenuPopup(
                                    itemView.getContext(),
                                    data,
                                    new HistoryMenuPopup.OnMenuItemClickListener() {
                                        @Override
                                        public void onViewDetail(HistoryItem item) {
                                            onMoreActionListener.onMoreAction(iv_more, item, 0);
                                        }

                                        @Override
                                        public void onRename(HistoryItem item) {
                                            onMoreActionListener.onMoreAction(iv_more, item, 1);
                                        }

                                        @Override
                                        public void onDelete(HistoryItem item) {
                                            onMoreActionListener.onMoreAction(iv_more, item, 2);
                                        }
                                    }
                            );
                    menuPopup.showAsDropDown(iv_more);
                }
            });

        }
    }

    private void setAgentUI(ChatViewHolder chatViewHolder, HistoryItem data) {
        ImageUtil.netRadius(context, data.getImageUrl(), chatViewHolder.iv_avatar);
    }

    private void setPPTUI(ChatViewHolder chatViewHolder, HistoryItem data) {
        ImageUtil.netRadius(context, data.getImageUrl(), chatViewHolder.iv_content);
    }

    private void setDrawingUI(ChatViewHolder holder, HistoryItem data) {

        if (holder.rl_container != null) {
            if (TextUtils.isEmpty(data.getImageUrl())) {
                holder.rl_container.setVisibility(View.GONE);
            } else {
                holder.rl_container.setVisibility(View.VISIBLE);
                if (holder.getItemViewType() == TYPE_DRAWING) {
                    Map<String, Object> map = data.getExtraData();
                    int width = 0;
                    int height = 0;
                    if (map != null) {
                        if (map.get("width") != null) {
                            width = ((int) map.get("width"));
                        }
                        if (map.get("height") != null) {
                            height = ((int) map.get("height"));
                        }
                        ZUtils.print("width = " + width + " height = " + height);
                    }


//                        if (width!= 0 && height != 0) {

                    // 获取屏幕宽度，减去 padding/margin
                    int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
                    int maxWidth = screenWidth - ZDpUtils.dpToPx((Activity) context, 32); // 假设 16dp 两边边距
                    float aspectRatio = (float) height / width;
//                            int targetWidth = Math.min(width, maxWidth); // 限制最大宽度
                    int targetWidth = ZDpUtils.dpToPx((Activity) context, 180);
                    int targetHeight = (int) (targetWidth * aspectRatio); // 根据宽高比计算高度
                    ZUtils.print("targetWidth = " + targetWidth);
                    ZUtils.print("aspectRatio = " + aspectRatio);
                    ZUtils.print("targetHeight = " + targetHeight);

                    // 设置 rl_container 的尺寸
                    ViewGroup.LayoutParams params = holder.rl_container.getLayoutParams();
                    ZUtils.print(" params.width  = " + params.width);
                    ZUtils.print(" params.height  = " + params.height);
                    params.width = targetWidth;
                    params.height = targetHeight;
                    holder.rl_container.setLayoutParams(params);

                    // 确保 iv_content 跟随容器尺寸
                    ViewGroup.LayoutParams imageParams = holder.iv_content.getLayoutParams();
                    imageParams.width = targetWidth;
                    imageParams.height = targetHeight;
                    holder.iv_content.setLayoutParams(imageParams);


                    ImageUtil.netRadiusXY(context, data.getImageUrl(), holder.iv_content, targetWidth, targetHeight);
                }
            }
        }
    }


    private void showPopup(View anchor, String content) {
        Context context = anchor.getContext();
        View popupView = LayoutInflater.from(context)
                .inflate(R.layout.popup_refresh_options, null);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setOutsideTouchable(true);

        RecyclerView rvOptions = popupView.findViewById(R.id.rvOptions);
        rvOptions.setLayoutManager(new LinearLayoutManager(context));

        String[] optionTitles = context.getResources()
                .getStringArray(R.array.refresh_options);
        List<IconTextItem> options = new ArrayList<>();
        options.add(new IconTextItem(R.mipmap.refresh_retry, optionTitles[0]));
        options.add(new IconTextItem(R.mipmap.refresh_simplify, optionTitles[1]));
        options.add(new IconTextItem(R.mipmap.refresh_details, optionTitles[2]));
        options.add(new IconTextItem(R.mipmap.refresh_conversation, optionTitles[3]));

        rvOptions.setAdapter(new IconTextAdapter(options,
                position -> {
                    popupWindow.dismiss();
//                    handleOptionClick(position, content);
                }));

        int xOffset = ZDpUtils.dpToPx2(context, POPUP_DISTANCE + POPUP_WIDTH);
        int yOffset = ZDpUtils.dpToPx2(context, POPUP_HEIGHT);
        popupWindow.showAsDropDown(anchor, -xOffset, -yOffset);
    }


    public void cancelPopu(){
        if (menuPopup != null){
            menuPopup.dismiss();
        }
    }

}