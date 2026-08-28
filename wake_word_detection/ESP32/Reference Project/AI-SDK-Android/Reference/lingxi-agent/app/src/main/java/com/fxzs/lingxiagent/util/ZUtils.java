package com.fxzs.lingxiagent.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.IYAApplication;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.main.utils.ScreenUtils;
import com.fxzs.lingxiagent.model.aiwork.AiWorkFilterBean;
import com.fxzs.lingxiagent.model.chat.dto.AiWritingTypeBean;
import com.fxzs.lingxiagent.model.chat.dto.OptionBean;
import com.fxzs.lingxiagent.model.chat.dto.OptionModel;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.service.MyForegroundService;
import com.fxzs.lingxiagent.view.aiwork.AiWorkAdapter;
import com.fxzs.lingxiagent.view.aiwork.AiWorkFilterAdapter;
import com.fxzs.lingxiagent.view.chat.OptionAdapter;
import com.fxzs.lingxiagent.view.chat.OptionAiMeetingAdapter;
import com.fxzs.lingxiagent.view.chat.OptionModelAdapter;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.GlobalToast;
import com.fxzs.lingxiagent.view.widget.CustomDividerItemDecoration;
import com.fxzs.smartassist.util.ZUtil.SizeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class ZUtils {
    public static void print(String msg){
        Timber.tag("MyLog").d(msg);
    }

    public static void showToast(String text) {
        if(!TextUtils.isEmpty(text)){
            Toast.makeText(IYAApplication.getInstance(), text, Toast.LENGTH_SHORT).show();
        }
    }

    public static void setTextBg(Context context, TextView tv, int res){
        tv.setBackground(context.getResources().getDrawable(res));
    }
    public static void setTextColor(Context context, TextView tv,int res){
        tv.setTextColor(context.getResources().getColor(res));
    }

    public static void setIvBg(Context context, ImageView iv, int res){
        iv.setBackground(context.getResources().getDrawable(res));
    }

    public static void setViewBg(Context context, View iv, int res){
        iv.setBackground(context.getResources().getDrawable(res));
    }

    public static void setViewBgTint(Context context, View view, int res){
        ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf(res));
    }

    public static void showSingleChoicePopup(Context context, View anchorView, boolean isShoImg,OptionAdapter.OnOptionSelectedListener callback) {
        // 加载弹窗布局
        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_single_choice, null);
        PopupWindow      popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        LinearLayout llChoice = popupView.findViewById(R.id.ll_choice);
        ShadowUtils.applyDefaultShadow(llChoice,context);
        // 初始化RecyclerView
        RecyclerView optionsRecyclerView = popupView.findViewById(R.id.optionsRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        optionsRecyclerView.setLayoutManager(layoutManager);

        // 设置选项数据和适配器
        List<OptionBean> options =new ArrayList<>();
        if(isShoImg){
            options.add(new OptionBean(R.mipmap.option_photo, "拍照"));
            options.add(new OptionBean(R.mipmap.option_picture, "图片"));
        }
        options.add(new OptionBean(R.mipmap.option_local_file, "本地文件"));

        OptionAdapter optionAdapter = new OptionAdapter(context,options, selected -> {
            callback.onOptionSelected(selected);
            popupWindow.dismiss();
//            selectedOption = selected;
//            chatMessages.add(new ChatMessage("选择了: " + selectedOption, true));
//            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
//            recyclerView.smoothScrollToPosition(chatMessages.size() - 1);
//            popupWindow.dismiss();
        });
        optionsRecyclerView.setAdapter(optionAdapter);

        // 设置弹窗背景
//        popupWindow.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.edit_text));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(false);

        // 计算弹窗显示位置（anchorView上方）
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int anchorHeight = anchorView.getHeight();
        int anchorWidth = anchorView.getWidth();
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupHeight = popupView.getMeasuredHeight();
        int popupWidth = popupView.getMeasuredWidth();
        int xOffset =  -anchorHeight -popupWidth/2+ZDpUtils.dpToPx((Activity) context, 8);;
//        int yOffset = -anchorHeight - popupHeight - 10; // 在anchorView上方10px处显示
        int yOffset = -anchorHeight - popupHeight - 10; // 在anchorView上方10px处显示

        // 显示弹窗
//        popupWindow.showAtLocation(anchorView, xOffset, yOffset);
        popupWindow.showAsDropDown(anchorView, xOffset, yOffset);
    }

    public static void showChooseModelPopup(Context context,View anchorView,
                                            List<OptionModel> options,
                                            OptionModel selectOptionModel,
                                            OptionModelAdapter.OnOptionSelectedListener listener) {
        int popupWidth = ZDpUtils.dpToPx((Activity) context, context.getResources().getDimension(R.dimen.dp_240));
        int screenSize = ScreenUtils.INSTANCE.getScreenWidth(context)- SizeUtils.dpToPx(context.getResources().getDimension(R.dimen.dp_40));
        if (popupWidth > screenSize){
            popupWidth = screenSize;
        }

        // 加载弹窗布局
        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_single_choice, null);
        PopupWindow      popupWindow = new PopupWindow(popupView,  popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        // 初始化RecyclerView
        RecyclerView optionsRecyclerView = popupView.findViewById(R.id.optionsRecyclerView);
        LinearLayout llChoice = popupView.findViewById(R.id.ll_choice);
        ShadowUtils.applyDefaultShadow(llChoice,context);

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        optionsRecyclerView.setLayoutManager(layoutManager);
        optionsRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
        });

        OptionModelAdapter optionAdapter = new OptionModelAdapter(context,options, selected -> {
            listener.onOptionSelected(selected);
            popupWindow.dismiss();
        });
        optionsRecyclerView.setAdapter(optionAdapter);
        if(selectOptionModel != null){
            for (int i = 0; i < options.size(); i++) {
                if(options.get(i).getName().equals(selectOptionModel.getName())){
                    optionAdapter.setSelectedPosition(i);
                }
            }
        }

        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(false);

        // 居中显示在 anchorView 下方
        int anchorWidth = anchorView.getWidth();
        int xOffset = (anchorWidth - popupWidth) / 2;

        // 显示弹窗
        popupWindow.showAsDropDown(anchorView, xOffset, -ZDpUtils.dpToPx((Activity) context, 4));
    }


    //翻译，写作弹窗
    public static void showAIMeetingPopup(Context context,View anchorView,
                                            List<AiWritingTypeBean> options,
                                          AiWritingTypeBean selectOptionModel,
                                          OptionAiMeetingAdapter.OnOptionSelectedListener listener) {
        // 加载弹窗布局
        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_single_choice, null);
        PopupWindow popupWindow = new PopupWindow(popupView,  ZDpUtils.dpToPx((Activity) context,234), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        LinearLayout llChoice = popupView.findViewById(R.id.ll_choice);
        ShadowUtils.applyDefaultShadow(llChoice,context);
        // 初始化RecyclerView
        RecyclerView optionsRecyclerView = popupView.findViewById(R.id.optionsRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        optionsRecyclerView.setLayoutManager(layoutManager);

        // 设置选项数据和适配器
//        List<OptionModel> options =new ArrayList<>();

        OptionAiMeetingAdapter optionAdapter = new OptionAiMeetingAdapter(context,options, selected -> {
            listener.onOptionSelected(selected);
            popupWindow.dismiss();
        });
        optionsRecyclerView.setAdapter(optionAdapter);

        if(selectOptionModel != null){
            for (int i = 0; i < options.size(); i++) {
                if(options.get(i).getName().equals(selectOptionModel.getName())){
                    optionAdapter.setSelectedPosition(i);
                }
            }
        }

        //添加自定义分割线
        DividerItemDecoration divider = new DividerItemDecoration((Activity) context,DividerItemDecoration.VERTICAL);
        divider.setDrawable(context.getDrawable(R.drawable.custom_divider));
        optionsRecyclerView.addItemDecoration(divider);
//        optionsRecyclerView.addItemDecoration(new DividerItemDecoration((Activity) context,DividerItemDecoration.VERTICAL));

        // 设置弹窗背景
//        popupWindow.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.edit_text));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(false);

        // 计算弹窗显示位置（anchorView上方）
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int anchorHeight = anchorView.getHeight();
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupHeight = popupView.getMeasuredHeight();
        int xOffset = -ZDpUtils.dpToPx((Activity) context, 8);
        int yOffset = -anchorHeight - popupHeight - ZDpUtils.dpToPx((Activity) context,12); // 在anchorView上方10px处显示

        // 显示弹窗
        popupWindow.showAsDropDown(anchorView, xOffset, yOffset);
    }

    //ai办公筛选
    public static void showAIWorkFilterPopup(Context context,View anchorView,
                                             AiWorkFilterBean selectOptionModel,
                                             AiWorkFilterAdapter.OnOptionSelectedListener listener) {
        // 加载弹窗布局
        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_single_choice, null);
        PopupWindow popupWindow = new PopupWindow(popupView,
//                ZDpUtils.dpToPx((Activity) context,234)
                ViewGroup.LayoutParams.MATCH_PARENT
                , ViewGroup.LayoutParams.WRAP_CONTENT, true);
        LinearLayout llChoice = popupView.findViewById(R.id.ll_choice);
        ShadowUtils.applyDefaultShadow(llChoice,context);
        // 初始化RecyclerView
        RecyclerView optionsRecyclerView = popupView.findViewById(R.id.optionsRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        optionsRecyclerView.setLayoutManager(layoutManager);

        List<AiWorkFilterBean> options = new ArrayList<>();
        options.add(new AiWorkFilterBean("全部", AiWorkAdapter.TYPE_ALL));
        options.add(new AiWorkFilterBean("AI会议", AiWorkAdapter.TYPE_MEETING));
        options.add(new AiWorkFilterBean("AI PPT",AiWorkAdapter.TYPE_PPT));
        options.add(new AiWorkFilterBean("AI 绘画",AiWorkAdapter.TYPE_DRAWING));
        options.add(new AiWorkFilterBean("同声传译",AiWorkAdapter.TYPE_TRANSLATE));

        // 设置选项数据和适配器
//        List<OptionModel> options =new ArrayList<>();

        AiWorkFilterAdapter optionAdapter = new AiWorkFilterAdapter(context,options, selected -> {
            listener.onOptionSelected(selected);
            popupWindow.dismiss();
        });
        optionsRecyclerView.setAdapter(optionAdapter);

        if(selectOptionModel != null){
            for (int i = 0; i < options.size(); i++) {
                if(options.get(i).getName().equals(selectOptionModel.getName())){
                    optionAdapter.setSelectedPosition(i);
                }
            }
        }

        //添加自定义分割线
//        DividerItemDecoration divider = new DividerItemDecoration((Activity) context,DividerItemDecoration.VERTICAL);
//        divider.setDrawable(context.getDrawable(R.drawable.custom_divider));
        CustomDividerItemDecoration divider = new CustomDividerItemDecoration(context, LinearLayoutManager.VERTICAL);
        optionsRecyclerView.addItemDecoration(divider);
//        optionsRecyclerView.addItemDecoration(new DividerItemDecoration((Activity) context,DividerItemDecoration.VERTICAL));

        // 设置弹窗背景
//        popupWindow.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.edit_text));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(false);

        // 计算弹窗显示位置（anchorView上方）
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int anchorHeight = anchorView.getHeight();
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupHeight = popupView.getMeasuredHeight();
        int xOffset = -ZDpUtils.dpToPx((Activity) context, 8);;
        int yOffset = ZDpUtils.dpToPx((Activity) context,15);
//        int yOffset = -anchorHeight - popupHeight - ZDpUtils.dpToPx((Activity) context,15); // 在anchorView上方10px处显示

        // 显示弹窗
        popupWindow.showAsDropDown(anchorView, xOffset, yOffset);
    }



    public static void copy(Context context,String text){

        ClipboardManager clipboardManager = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("label",text);
        clipboardManager.setPrimaryClip(clipData);
//        showToast("已复制");
        GlobalToast.show((Activity) context, "已复制", GlobalToast.Type.SUCCESS);
    }

    public static void setSystem(Activity activity) {

        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(activity, android.R.color.transparent));
//            window.setBackgroundDrawable( AppCompatResources.getDrawable(activity, R.mipmap.login_bg2));
            window.setNavigationBarColor( ContextCompat.getColor(activity,R.color.white));
            activity.getWindow().setStatusBarColor(activity.getResources().getColor(android.R.color.transparent));
        }
        boolean isTablet = activity.getResources().getBoolean(R.bool.isTablet);
        if (!isTablet){
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }
    // 显示控件（从下方滑入）
    public static void slideInDown2Up(View view) {
        view.setTranslationY(view.getHeight()); // 初始位置在视图下方
        view.setVisibility(View.VISIBLE);
        view.animate()
                .translationY(0) // 目标位置为原位置
                .setDuration(300)
                .setListener(null);
    }

    // 显示控件（从上方滑入）
    public static void slideInUp2Down(View view) {
        view.setTranslationY(-view.getHeight()); //
        view.setAlpha(0f); // 初始透明度为0
        view.setVisibility(View.VISIBLE);
        view.animate()
                .translationY(0) // 目标位置为原位置
                .alpha(1f) // 目标透明度为1
                .setDuration(50)
                .setListener(null);
    }

    // 隐藏控件（向下滑出）
    public static void slideOut(View view) {
        view.animate()
                .translationY(view.getHeight()) // 目标位置在视图下方
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        view.setVisibility(View.GONE); // 动画结束后设置可见性为GONE
                        view.setTranslationY(0);
                    }
                });
    }

    // 禁用子视图的 tooltip
    public static void disableTooltipForChildViews(View view) {
//        if (view instanceof ViewGroup) {
//            ViewGroup viewGroup = (ViewGroup) view;
//            for (int i = 0; i < viewGroup.getChildCount(); i++) {
//                View child = viewGroup.getChildAt(i);
//                child.setLongClickable(false);
//                child.setOnLongClickListener(null);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    child.setTooltipText(null);
//                }
//                // 递归处理子视图
//                disableTooltipForChildViews(child);
//            }
//        }
    }

    public static String getTopActivity(Context context){
        ActivityManager am = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        return cn.getClassName();
    }

    public static void setStatusBarWhite(Activity activity){

        // 设置状态栏颜色为白色，与背景一致，并保证内容不被遮挡
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            activity.getWindow().getDecorView().postDelayed(() -> {
                activity.getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
            }, 100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    public static String loadJSONFromAssets(Context context, String fileName) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
	    return json;
    }


    /**
     * 根据时间戳获取日期字符串
     */
    public static String getDateFromTimestamp(Long timestamp) {
        if (timestamp == null) return "";

        try {
            Date date = new Date(timestamp);
            Calendar today = Calendar.getInstance();
            Calendar targetDay = Calendar.getInstance();
            targetDay.setTime(date);

            // 判断是否是今天
            if (isSameDay(today, targetDay)) {
                return "今天";
            }

            // 判断是否是昨天
            today.add(Calendar.DAY_OF_YEAR, -1);
            if (isSameDay(today, targetDay)) {
                return "昨天";
            }

            // 其他日期
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
            return outputFormat.format(date);

        } catch (Exception e) {
            Timber.tag("HistoryBottomSheet").e( "时间戳转换失败: " + timestamp, e);
            return "";
        }
    }

    private  static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * 获取当前时间
     */
    private static String getCurFormatDate(){
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    /**
     * 返回时间是否过期
     * 返回是否过期时间
     */
    @SuppressLint("SimpleDateFormat")
    public static boolean compareFormatDate(String date) {
        TimberUtils.logLong("compareFormatDate", date);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date dateFormat1 = sdf.parse(date);

            String curDate = sdf.format(new Date());
            Date dateFormat2 = sdf.parse(curDate);

            if (dateFormat1 != null && dateFormat2 != null) {
                return dateFormat1.compareTo(dateFormat2) >= 0;
            }

        } catch (Exception e) {
            TimberUtils.logLong("compareFormatDate", e.getMessage());
            return true;
        }
        return false;
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {

            if (MyForegroundService.class.getName()
                    .equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    private static long lastStartTime = 0;

    public static synchronized void startService(Activity activity) {

        Timber.tag("TAG").d("开始服务");

        // 服务已经运行直接返回
        if (MyForegroundService.isRunning) {
            Timber.tag("TAG").d("服务已运行");
            return;
        }

        long now = System.currentTimeMillis();

        // 防止短时间重复拉起
        if (now - lastStartTime < 5000) {
            Timber.tag("TAG").d("服务启动过于频繁");
            return;
        }

        lastStartTime = now;

        Intent serviceIntent =
                new Intent(activity, MyForegroundService.class);

        ContextCompat.startForegroundService(activity, serviceIntent);
    }

    public static void stopService(Activity activity) {
        Timber.tag("TAG").d( "停止服务");
        Intent serviceIntent = new Intent(activity, MyForegroundService.class);
        activity.stopService(serviceIntent); // 停止服务
    }

    public static String generateSign(String timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    Constants.HONOR_SECRET_KEY.getBytes("UTF-8"),
                    "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] byteHMAC = mac.doFinal(timestamp.getBytes("UTF-8"));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Base64.getEncoder().encodeToString(byteHMAC);
            }
            return "";
        } catch (Exception e) {
            Timber.tag("TAG").d( "generateSign error " + e);
            return "";
        }
    }

    public static void downloadFile(Context context, String fileUrl) {
        showToast("开始下载");

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                String[] strArr = fileUrl.split("/");
                String fileName = strArr[strArr.length - 1];

                print("fileUrl = " + fileUrl);
                print("fileName = " + fileName);

                downloadWithOkHttp(context, fileUrl, fileName);
                return null;
            }
        }.execute();
    }

    private static void downloadWithOkHttp(Context context, String fileUrl, String fileName) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(fileUrl).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                postDownloadFailed(context);
                return;
            }

            InputStream inputStream = response.body().byteStream();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadWithMediaStore(context, inputStream, fileName);
            } else {
                downloadWithLegacyStorage(context, inputStream, fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            print("Exception = " + e.getMessage());
            postDownloadFailed(context);
        }
    }

    private static void downloadWithMediaStore(Context context, InputStream inputStream, String fileName) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName));
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LingXi");

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("无法创建MediaStore条目");
        }

        try (OutputStream outputStream = resolver.openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IOException("无法打开MediaStore输出流");
            }
            copyStream(inputStream, outputStream);
        }

        showDownloadFinishedNotification(context, uri, fileName);
        postDownloadSuccess(context, fileName);
    }

    private static void downloadWithLegacyStorage(Context context, InputStream inputStream, String fileName) throws IOException {
        File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LingXi");
        if (!downloadDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            downloadDir.mkdirs();
        }

        File outFile = new File(downloadDir, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
            copyStream(inputStream, outputStream);
        }

        MediaScannerConnection.scanFile(context, new String[]{outFile.getAbsolutePath()}, null, null);
        showDownloadFinishedNotification(context, outFile);
        postDownloadSuccess(context, outFile.getName());
    }

    private static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
    }

    private static void postDownloadSuccess(Context context, String fileName) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (context instanceof Activity) {
                // Android 10+ 使用 MediaStore 保存时，这里没有直接 file path
                boolean isMediaStore = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
                showDownloadSuccessDialog((Activity) context, fileName, isMediaStore);
            } else {
                showToast("下载完成: " + fileName);
            }
        });
    }

    private static void showDownloadSuccessDialog(Activity activity, String fileName, boolean isMediaStore) {
        String message;
        if (isMediaStore) {
            message = "文件已保存到:\nDownload/LingXi/" + fileName + "\n\n是否立即打开文件？";
        } else {
            message = "文件已保存到:\n" + fileName + "\n\n是否立即打开文件？";
        }

        new CommonDialog.Builder(activity)
                .setTitle("下载成功")
                .setMessage(message)
                .setConfirmText("打开")
                .setCancelText("稍后")
                .setOnClickListener(new CommonDialog.OnDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        openDownloadedFile(activity, fileName, isMediaStore);
                    }

                    @Override
                    public void onCancel() {
                        if (isMediaStore) {
                            GlobalToast.show(activity, "文件已保存到 Download/LingXi 文件夹", GlobalToast.Type.SUCCESS);
                        } else {
                            GlobalToast.show(activity, "文件已保存到 Download/LingXi/" + fileName, GlobalToast.Type.SUCCESS);
                        }
                    }
                })
                .show();
    }

    private static void openDownloadedFile(Activity activity, String fileName, boolean isMediaStore) {
        try {
            if (isMediaStore) {
                // Android 10+：从 MediaStore Downloads 中查询该文件并用 contentUri 打开
                Uri queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                String[] projection = {MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME};
                String selection = MediaStore.Downloads.DISPLAY_NAME + "=? AND " + MediaStore.Downloads.RELATIVE_PATH + " LIKE ?";
                String[] selectionArgs = {fileName, "%LingXi%"};

                android.database.Cursor cursor = activity.getContentResolver().query(
                        queryUri,
                        projection,
                        selection,
                        selectionArgs,
                        MediaStore.Downloads.DATE_ADDED + " DESC"
                );

                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID));
                            Uri contentUri = android.content.ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(contentUri, getMimeType(fileName));
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                                activity.startActivity(Intent.createChooser(intent, "打开文件"));
                                return;
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }

                GlobalToast.show(activity, "未找到可打开的应用，请在 Downloads/LingXi 文件夹中手动打开", GlobalToast.Type.NORMAL);
                return;
            }

            // Android 9-：直接通过 FileProvider 打开
            File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LingXi");
            File file = new File(downloadDir, fileName);
            if (!file.exists()) {
                GlobalToast.show(activity, "文件不存在，请在 Downloads/LingXi 文件夹中查找", GlobalToast.Type.ERROR);
                return;
            }

            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                String authority = activity.getPackageName() + ".fileprovider";
                uri = FileProvider.getUriForFile(activity, authority, file);
            } else {
                uri = Uri.fromFile(file);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, getMimeType(fileName));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(Intent.createChooser(intent, "打开文件"));
            } else {
                GlobalToast.show(activity, "没有找到可以打开该文件的应用", GlobalToast.Type.ERROR);
            }
        } catch (Exception e) {
            print("openDownloadedFile Exception = " + e.getMessage());
            GlobalToast.show(activity, "打开文件失败，请在 Downloads/LingXi 文件夹中手动打开", GlobalToast.Type.ERROR);
        }
    }

    private static void postDownloadFailed(Context context) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (context instanceof Activity) {
                GlobalToast.show((Activity) context, "下载失败", GlobalToast.Type.ERROR);
            } else {
                showToast("下载失败");
            }
        });
    }

    private static void showDownloadFinishedNotification(Context context, File file) {
        showDownloadFinishedNotification(context, null, file, file.getName());
    }

    private static void showDownloadFinishedNotification(Context context, Uri contentUri, String fileName) {
        showDownloadFinishedNotification(context, contentUri, null, fileName);
    }

    private static void showDownloadFinishedNotification(Context context, Uri contentUri, File file, String fileName) {
        final String channelId = "download_channel";
        final String channelName = "下载";

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Uri uriToOpen = null;
        if (contentUri != null) {
            uriToOpen = contentUri;
        } else if (file != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                String authority = context.getPackageName() + ".fileprovider";
                uriToOpen = FileProvider.getUriForFile(context, authority, file);
            } else {
                uriToOpen = Uri.fromFile(file);
            }
        }

        if (uriToOpen != null) {
            intent.setDataAndType(uriToOpen, getMimeType(fileName));
        } else {
            intent.setType(getMimeType(fileName));
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pi = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("下载完成")
                .setContentText(fileName)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        nm.notify((int) (System.currentTimeMillis() & 0x7fffffff), builder.build());
    }

    private static String getMimeType(String fileName) {
        if (TextUtils.isEmpty(fileName)) return "*/*";
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (lower.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        return "*/*";
    }
}

