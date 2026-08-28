package com.fxzs.lingxiagent.util.ZUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.callback.SuperShareCallback;
import com.fxzs.lingxiagent.util.AdapterCaptureHelper;
import com.fxzs.lingxiagent.util.ZDpUtils;

import java.util.ArrayList;
import java.util.List;

public class SuperLongPicUtil {

    private Context mContext;
    private LinearLayout mLongPicLayout;
    private SuperShareCallback mShareCallback;
    private List<Integer> mSelectPositions = new ArrayList<>();
    private RecyclerView mChatRecyclerView;
    private ImageView mExitIcon;
    private ImageView mLongPicView;

    public SuperLongPicUtil(Context context, LinearLayout layout, RecyclerView view) {
        mContext = context;
        mLongPicLayout = layout;
        mChatRecyclerView = view;

        initView();
    }

    public void setCallback(SuperShareCallback callback) {
        mShareCallback = callback;
    }

    private void initView() {
        mExitIcon = mLongPicLayout.findViewById(R.id.longpic_exit_icon);
        mLongPicView = mLongPicLayout.findViewById(R.id.longpic_image);

        mExitIcon.setOnClickListener(v -> {
            mLongPicLayout.setVisibility(View.GONE);
            if (mShareCallback != null) {
                mShareCallback.closeBottomLayout();
            }
        });
    }

    public void createLongImage() {
        if (mShareCallback != null) {
            mSelectPositions = mShareCallback.getSelectPositions();
        }

        List<View> views = new ArrayList<>();
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.share_longpic_layout, null, false);
        int widthSpec = View.MeasureSpec.makeMeasureSpec(mContext.getResources().getDisplayMetrics().widthPixels, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        View topView = view.findViewById(R.id.share_longpic_top);
        View bottomView = view.findViewById(R.id.share_longpic_bottom);
        views.add(topView);

        RecyclerView.Adapter adapter = mChatRecyclerView.getAdapter();
        for (int i = 0; i < mSelectPositions.size(); i++) {
            int position = mSelectPositions.get(i);
            int viewType = adapter.getItemViewType(position);
            RecyclerView.ViewHolder holder = adapter.createViewHolder(mChatRecyclerView, viewType);
            adapter.bindViewHolder(holder, position);

            View itemView = holder.itemView;
            widthSpec = View.MeasureSpec.makeMeasureSpec(mChatRecyclerView.getWidth()
                    - ZDpUtils.dpToPx2(mContext, 34), View.MeasureSpec.EXACTLY);
            heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            itemView.measure(widthSpec, heightSpec);
            itemView.layout(0, 0, itemView.getMeasuredWidth(), itemView.getMeasuredHeight());

            views.add(itemView);
        }
        views.add(bottomView);

        AdapterCaptureHelper.captureFromAdapter(mContext, views, new AdapterCaptureHelper.CaptureCallback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                if (mShareCallback != null) {
                    mShareCallback.onShareLongPic();
                }
                mLongPicLayout.setVisibility(View.VISIBLE);
                mLongPicView.setImageBitmap(bitmap);
            }

            @Override
            public void onFailed(String message) {

            }
        });
    }

    public void saveLongPic() {
        mLongPicLayout.setVisibility(View.GONE);
        AdapterCaptureHelper.saveBitmap(mContext,
                ((BitmapDrawable) mLongPicView.getDrawable()).getBitmap(),
                "long_pic.png");
        Toast.makeText(mContext, "保存图片成功", Toast.LENGTH_SHORT);
    }

}
