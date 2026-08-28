package com.fxzs.lingxiagent.view.meeting;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.chat.callback.RequestCallback;
import com.fxzs.lingxiagent.model.chat.dto.OptionMeetingLan;
import com.fxzs.lingxiagent.model.event.MessageEvent;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.ZDpUtils;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.ZUtil.GridSpaceItemDecoration;
import com.fxzs.lingxiagent.util.ZUtil.SessionUpload;
import com.fxzs.lingxiagent.util.ZUtils;
import com.fxzs.lingxiagent.util.audio.TTSManager;
import com.fxzs.lingxiagent.view.chat.OptionMeetingLanAdapter;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.common.DataBindingUtils;
import com.fxzs.lingxiagent.view.common.MeetingLoadingProgressDialog;
import com.fxzs.lingxiagent.viewmodel.meeting.VMAudioTranscription;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class AIMeetingSub2Fragment extends Fragment {

    RecyclerView rv;
    TextView tv_step1;
    TextView tv_step1_name;
    TextView tv_step2;
    TextView tv_step2_name;
    RelativeLayout tv_step3;
    TextView tv_step3_name;
    LinearLayout ll_step1;
    LinearLayout ll_step2;
    LinearLayout ll_local;
    private TextView tvStart;

    // ViewModel
    private VMAudioTranscription viewModel;

    // 进度对话框
    private MeetingLoadingProgressDialog progressDialog;

    // 语言选项适配器
    private OptionMeetingLanAdapter optionAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ai_meeting_sub2, container, false);

        // 初始化RecyclerView
        rv = view.findViewById(R.id.rv);
        tv_step1 = view.findViewById(R.id.tv_step1);
        tv_step1_name = view.findViewById(R.id.tv_step1_name);
        tv_step2 = view.findViewById(R.id.tv_step2);
        tv_step2_name = view.findViewById(R.id.tv_step2_name);
        tv_step3 = view.findViewById(R.id.tv_step3);
        tv_step3_name = view.findViewById(R.id.tv_step3_name);
        ll_step1 = view.findViewById(R.id.ll_step1);
        ll_step2 = view.findViewById(R.id.ll_step2);
        ll_local = view.findViewById(R.id.ll_local);
        tvStart = view.findViewById(R.id.tv_start);

        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(VMAudioTranscription.class);

        // 设置观察者
        setupObservers();

        // 添加点击事件，点击数字1返回上一步
        View.OnClickListener stepOneClickListener = v -> {
            if (ll_step2.getVisibility() == View.VISIBLE) {
                updateStepToOne();
            }
        };

        // 给数字1和文字都添加点击事件
        tv_step1.setOnClickListener(stepOneClickListener);
        tv_step1_name.setOnClickListener(stepOneClickListener);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        layoutManager.setSpanCount(2);
        rv.setLayoutManager(layoutManager);
        // 移除原有间距装饰器
//        int itemDecorationCount = rv.getItemDecorationCount();
//        for (int i = 0; i < itemDecorationCount; i++) {
//            rv.removeItemDecorationAt(0);
//        }
        // 添加新的间距
        rv.addItemDecoration(new GridSpaceItemDecoration(2,
                ZDpUtils.dpToPx(getActivity(), 8),
                ZDpUtils.dpToPx(getActivity(), 8)));

        // 初始化空的适配器
        List<OptionMeetingLan> options = new ArrayList<>();
        optionAdapter = new OptionMeetingLanAdapter(getActivity(), options, selected -> {
            // 用户选择语言时的回调
            if (selected != null && selected.getKey() != null) {
                viewModel.setSelectedLanguage(selected.getKey());
                Timber.tag("AIMeetingSub2Fragment").d( "用户选择语言: " + selected.getKey() + " (" + selected.getTitle() + ")");

                // 更新所有选项的选中状态
                for (OptionMeetingLan option : options) {
                    option.setSelect(option.getKey() != null && option.getKey().equals(selected.getKey()));
                }
                optionAdapter.notifyDataSetChanged();
            }
        });
        rv.setAdapter(optionAdapter);

        // 从API获取语言选项数据
        loadLanguageOptions();//TODO

        tvStart.setOnClickListener(v -> {
            // 添加空指针检查
            if (getActivity() == null || getContext() == null) {
                Timber.tag("AIMeetingSub2Fragment").e( "Activity或Context为空，无法执行操作");
                return;
            }

            try {
                // 点击开始按钮的逻辑
                ll_step1.setVisibility(View.GONE);
                ll_step2.setVisibility(View.VISIBLE);

                ZUtils.setTextColor(getActivity(), tv_step1_name, R.color.text_hint_color);
                ZUtils.setTextColor(getActivity(), tv_step1, R.color.text_hint_color);
                ZUtils.setTextColor(getActivity(), tv_step2_name, R.color.text_black);
                ZUtils.setTextColor(getActivity(), tv_step2, R.color.text_black);

                ZUtils.setViewBg(getActivity(), tv_step1, R.drawable.bg_step_circle);
                ZUtils.setViewBg(getActivity(), tv_step2, R.drawable.bg_step_circle_select);
            } catch (Exception e) {
                Timber.tag("AIMeetingSub2Fragment").e( "执行开始操作失败: " + e.getMessage(), e);
            }
        });

        ll_local.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 添加空指针检查
                if (getActivity() == null || getContext() == null) {
                    Timber.tag("AIMeetingSub2Fragment").e( "Activity或Context为空");
                    return;
                }
                
                // 检查并请求存储权限
                if (checkStoragePermission()) {
                    openAudioFilePicker();
                } else {
                    requestStoragePermission();
                }
            }
        });

        // 添加视频相册点击事件
        view.findViewById(R.id.ll_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() == null || getContext() == null) {
                    Timber.tag("AIMeetingSub2Fragment").e( "Activity或Context为空");
                    return;
                }

                // 检查并请求存储权限
                if (checkStoragePermission()) {
                    openVideoFilePicker();
                } else {
                    requestStoragePermission();
                }
            }
        });
        return view;
    }

    private void updateStepToOne() {

        // 显示第一步
        ll_step1.setVisibility(View.VISIBLE);
        ll_step2.setVisibility(View.GONE);

        // 更新步骤指示器状态
        ZUtils.setTextColor(getActivity(), tv_step1_name, R.color.text_black);
        ZUtils.setTextColor(getActivity(), tv_step1, R.color.text_black);
        ZUtils.setTextColor(getActivity(), tv_step2_name, R.color.text_hint_color);
        ZUtils.setTextColor(getActivity(), tv_step2, R.color.text_hint_color);

        ZUtils.setViewBg(getActivity(), tv_step1, R.drawable.bg_step_circle_select);
        ZUtils.setViewBg(getActivity(), tv_step2, R.drawable.bg_step_circle);
    }

    /**
     * 设置ViewModel观察者
     */
    private void setupObservers() {
        // 观察进度消息
        viewModel.getProgressMessage().observe(this, message -> {
            if (message != null) {
                updateProgressDialog(message);
            }
        });

        // 观察转写结果
        viewModel.getTranscriptionResult().observe(this, result -> {
            if (result != null) {
                hideProgressDialog();
                if (result.isSuccess()) {
                    // 转写成功，跳转到会议详情页面
                    jumpToMeetingDetail(result.getMeetingId(), result.getTranscriptionText());
                } else {
                    // 转写失败，显示错误信息
                    handleRecognitionError(result.getErrorMessage());
                }
            }
        });

        // 观察加载状态
        viewModel.getLoading().observe(this, loading -> {
            if (loading != null) {
                if (loading) {
                    if (progressDialog == null || !progressDialog.isShowing()) {
                        Constant.isLoadMeetingExchange = true;
                        showProgressDialog("正在上传中");
                    }
                } else {
                    hideProgressDialog();
                }
            }
        });

        // 观察错误消息
        viewModel.getError().observe(this, error -> {
            ZUtils.print("观察错误消息");
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        // 观察转写内容更新
        viewModel.getTranscriptionContent().observe(this, content -> {
            if (content != null) {
                // 可以在这里实时更新UI显示转写内容
                Timber.tag("AIMeetingSub2Fragment").d( "转写内容更新: " + content.length() + " 字符");
            }
        });
    }

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int PICK_AUDIO_REQUEST = 1002;
    private static final int PICK_VIDEO_REQUEST = 1003;

    private boolean checkStoragePermission() {
        if (getContext() == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上版本使用新的媒体权限
            return ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 13以下版本使用传统存储权限
            return ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (getActivity() == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上版本
//            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_AUDIO)) {
//                // 用户之前拒绝过权限，显示解释
//                new AlertDialog.Builder(getActivity())
//                        .setTitle("需要音频访问权限")
//                        .setMessage("为了能够选择音频文件进行转写，我们需要访问音频文件的权限。")
//                        .setPositiveButton("授权", (dialog, which) -> {
//                            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO},
//                                    PERMISSION_REQUEST_CODE);
//                        })
//                        .setNegativeButton("取消", null)
//                        .create()
//                        .show();
//            } else {
//                // 首次请求权限
//
//            }
            AppPermissionRequestManager.requestReadMediaAudioPermission(getActivity(), PERMISSION_REQUEST_CODE,"允许访问音频文件","请授权音频文件，以便对音频文件进行转写");
        } else {
            // Android 13以下版本
//            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
//                // 用户之前拒绝过权限，显示解释
//                new AlertDialog.Builder(getActivity())
//                        .setTitle("需要存储权限")
//                        .setMessage("为了能够选择音频文件进行转写，我们需要访问存储的权限。")
//                        .setPositiveButton("授权", (dialog, which) -> {
//                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                                    PERMISSION_REQUEST_CODE);
//                        })
//                        .setNegativeButton("取消", null)
//                        .create()
//                        .show();
//            } else {
//                // 首次请求权限
//                AppPermissionRequestManager.requestExternalStoragePermission(getActivity(), PERMISSION_REQUEST_CODE);
//            }
            AppPermissionRequestManager.requestExternalStoragePermission(getActivity(), PERMISSION_REQUEST_CODE,"请授权手机存储，以便导入本地文件进行转写");
        }
    }

        private void openAudioFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");  // 只显示音频文件
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(
                Intent.createChooser(intent, "选择音频文件"),
                PICK_AUDIO_REQUEST
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "请安装文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    private void openVideoFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");  // 只显示视频文件
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(
                Intent.createChooser(intent, "选择视频文件"),
                PICK_VIDEO_REQUEST
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "请安装文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限获取成功，但这里不能直接调用openAudioFilePicker()
                // 因为我们不知道用户点击的是音频还是视频按钮
                Toast.makeText(getContext(), "权限获取成功，请重新点击选择文件", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "需要存储权限才能选择文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri audioUri = data.getData();
                // 检查文件类型
                String mimeType = getMimeType(audioUri);
                String fileName = getFileName(audioUri);

                // 添加调试日志
                Timber.tag("AIMeetingSub2Fragment").d( "选择的音频文件: " + fileName + ", MIME类型: " + mimeType);

                if (isValidAudioFile(mimeType, fileName)) {
                    handleSelectedAudioFile(audioUri);
                } else {
                    Toast.makeText(getContext(), "请选择支持的音频格式文件 (支持: MP3, WAV, M4A, AAC, OGG, FLAC, AIFF)", Toast.LENGTH_LONG).show();
                    Timber.tag("AIMeetingSub2Fragment").w("不支持的音频文件格式 - 文件: " + fileName + ", MIME类型: " + mimeType);
                }
            }
        } else if (requestCode == PICK_VIDEO_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri videoUri = data.getData();
                // 检查文件类型
                String mimeType = getMimeType(videoUri);
                String fileName = getFileName(videoUri);

                // 添加调试日志
                Timber.tag("AIMeetingSub2Fragment").d( "选择的视频文件: " + fileName + ", MIME类型: " + mimeType);

                if (isValidVideoFile(mimeType, fileName)) {
                    handleSelectedVideoFile(videoUri);
                } else {
                    Toast.makeText(getContext(), "请选择支持的视频格式文件 (支持: MP4, AVI, MOV, MKV, WMV, FLV)", Toast.LENGTH_LONG).show();
                   Timber.tag("AIMeetingSub2Fragment").w( "不支持的视频文件格式 - 文件: " + fileName + ", MIME类型: " + mimeType);
                }
            }
        }
    }

    private String getMimeType(Uri uri) {
        if (getContext() == null) {
            return "";
        }
        ContentResolver cr = getContext().getContentResolver();
        return cr.getType(uri);
    }

    private boolean isValidAudioFile(String mimeType, String fileName) {
        // 首先通过MIME类型检查
        if (mimeType != null) {
            // 支持的音频格式 - 使用准确的MIME类型匹配
            if (mimeType.equals("audio/mpeg") ||      // MP3
                mimeType.equals("audio/mp3") ||       // MP3 (alternative)
                mimeType.equals("audio/wav") ||       // WAV
                mimeType.equals("audio/wave") ||      // WAV (alternative)
                mimeType.equals("audio/x-wav") ||     // WAV (x-prefix)
                mimeType.equals("audio/mp4") ||       // M4A
                mimeType.equals("audio/m4a") ||       // M4A
                mimeType.equals("audio/aac") ||       // AAC
                mimeType.equals("audio/ogg") ||       // OGG
                mimeType.equals("audio/flac") ||      // FLAC
                mimeType.equals("audio/aiff") ||      // AIFF
                mimeType.equals("audio/x-aiff")) {    // AIFF (x-prefix)
                return true;
            }
        }

        // 如果MIME类型检查失败，通过文件扩展名检查（备用方案）
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            return lowerFileName.endsWith(".mp3") ||
                   lowerFileName.endsWith(".wav") ||
                   lowerFileName.endsWith(".m4a") ||
                   lowerFileName.endsWith(".aac") ||
                   lowerFileName.endsWith(".ogg") ||
                   lowerFileName.endsWith(".flac") ||
                   lowerFileName.endsWith(".aiff") ||
                   lowerFileName.endsWith(".aif");
        }

        return false;
    }

    private boolean isValidVideoFile(String mimeType, String fileName) {
        // 首先通过MIME类型检查
        if (mimeType != null) {
            // 支持的视频格式 - 使用准确的MIME类型匹配
            if (mimeType.equals("video/mp4") ||       // MP4
                mimeType.equals("video/avi") ||       // AVI
                mimeType.equals("video/x-msvideo") || // AVI (x-prefix)
                mimeType.equals("video/quicktime") || // MOV
                mimeType.equals("video/x-matroska") ||// MKV
                mimeType.equals("video/x-ms-wmv") ||  // WMV
                mimeType.equals("video/x-flv")) {     // FLV
                return true;
            }
        }

        // 如果MIME类型检查失败，通过文件扩展名检查（备用方案）
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            return lowerFileName.endsWith(".mp4") ||
                   lowerFileName.endsWith(".avi") ||
                   lowerFileName.endsWith(".mov") ||
                   lowerFileName.endsWith(".mkv") ||
                   lowerFileName.endsWith(".wmv") ||
                   lowerFileName.endsWith(".flv");
        }

        return false;
    }

    private void handleSelectedAudioFile(Uri audioUri) {
        if (getContext() == null) {
            return;
        }

        try {
            // 获取文件大小
            Cursor cursor = getContext().getContentResolver().query(audioUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                long fileSize = cursor.getLong(sizeIndex);
                cursor.close();

                // 检查文件大小是否超过500M
                if (fileSize > 500 * 1024 * 1024) { // 500MB in bytes
                    Toast.makeText(getContext(), "音频文件大小不能超过500M", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 获取文件名
                String fileName = getFileName(audioUri);
                Timber.tag("AIMeetingSub2Fragment").d( "开始处理音频文件: " + fileName);

                // 将URI转换为本地文件路径
                String audioFilePath = copyUriToLocalFile(audioUri, fileName);
                if (audioFilePath != null) {
                    // 开始音频识别流程，参考实时录音链路
                    startAudioRecognitionProcess(audioFilePath, fileName);
                } else {
                    Toast.makeText(getContext(), "文件处理失败", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "文件读取失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private String getFileName(Uri uri) {
        if (getContext() == null) {
            return "";
        }
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                result = cursor.getString(nameIndex);
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void handleSelectedVideoFile(Uri videoUri) {
        if (getContext() == null) {
            return;
        }

        try {
            // 获取文件大小
            Cursor cursor = getContext().getContentResolver().query(videoUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                long fileSize = cursor.getLong(sizeIndex);
                cursor.close();

                // 检查文件大小是否超过1GB
                if (fileSize > 1024 * 1024 * 1024) { // 1GB in bytes
                    Toast.makeText(getContext(), "视频文件大小不能超过1GB", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 获取文件名
                String fileName = getFileName(videoUri);
                Timber.tag("AIMeetingSub2Fragment").d( "开始处理视频文件: " + fileName);

                // 将URI转换为本地文件路径
                String videoFilePath = copyUriToLocalFile(videoUri, fileName);
                if (videoFilePath != null) {
                    // 开始视频识别流程，参考实时录音链路
                    startAudioRecognitionProcess(videoFilePath, fileName);
                } else {
                    Toast.makeText(getContext(), "文件处理失败", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "视频文件读取失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * 将URI文件复制到本地临时目录
     */
    private String copyUriToLocalFile(Uri uri, String fileName) {
        if (getContext() == null) {
            return null;
        }

        try {
            // 创建临时文件目录
            File tempDir = new File(getContext().getCacheDir(), "temp_audio");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            // 创建临时文件
            File tempFile = new File(tempDir, fileName);

            // 复制文件内容
            java.io.InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            Timber.tag("AIMeetingSub2Fragment").d( "文件复制成功: " + tempFile.getAbsolutePath());
            return tempFile.getAbsolutePath();

        } catch (Exception e) {
            Timber.tag("AIMeetingSub2Fragment").e( "文件复制失败"+ e);
            return null;
        }
    }

    /**
     * 开始音频识别流程 - 使用ViewModel处理
     */
    private void startAudioRecognitionProcess(String audioFilePath, String fileName) {
        Timber.tag("AIMeetingSub2Fragment").d( "开始音频识别流程: " + audioFilePath);

        // 检查网络连接
        if (!com.fxzs.lingxiagent.util.NetworkUtils.isNetworkAvailable(getContext())) {
            Toast.makeText(getContext(), "当前无网络连接，建议连接网络后再尝试操作", Toast.LENGTH_LONG).show();
            return;
        }

        // 更新步骤进度条到第3步
        updateStepToThree();

        // 使用ViewModel开始转写流程
        viewModel.startAudioTranscription(audioFilePath, fileName);
    }

    /**
     * 跳转到会议详情页面
     */
    private void jumpToMeetingDetail(Integer meetingId, String transcriptionResult) {
        if (getActivity() != null) {
            Constant.isLoadMeetingExchange = false;
            // 生成会议标题，基于当前时间
            String meetingTitle = generateMeetingTitle();

            Intent intent = MeetingActivity.createIntent(
                getActivity(),
                meetingId != null ? meetingId.toString() : "1", // 使用真实的meetingId
                transcriptionResult, // 识别结果
                0, // tabType，会议内容
                meetingTitle // 会议标题
            );
            startActivity(intent);
            updateStepToOne();
//            getActivity().finish();
        }
    }

    /**
     * 生成会议标题
     */
    private String generateMeetingTitle() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm会议", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    /**
     * 显示进度对话框 - 使用dialog_meeting_progress.xml布局
     */
    private void showProgressDialog(String message) {
        if (getContext() == null) {
            return;
        }

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        progressDialog = new MeetingLoadingProgressDialog(getContext())
                .setMessage(message)
                .setCancelable(false);
        progressDialog.setCancel(new RequestCallback() {
            @Override
            public void callback(Object data) {


                CommonDialog.showConfirmDialog(getActivity(), "将不保存会议内容",
                        "请确认是否退出", "退出",
                        new CommonDialog.OnDialogClickListener() {
                            @Override
                            public void onConfirm() {
                                if(SessionUpload.getUploadTask() == null){
                                    return;
                                }
                                progressDialog.dismiss();
                                SessionUpload.getUploadTask().cancel();//TODO
                                viewModel.setCancel(true);
                                ll_step1.setVisibility(View.VISIBLE);
                                ll_step2.setVisibility(View.GONE);

                                // 更新步骤指示器状态
                                ZUtils.setTextColor(getActivity(), tv_step1_name, R.color.text_black);
                                ZUtils.setTextColor(getActivity(), tv_step1, R.color.text_black);
                                ZUtils.setTextColor(getActivity(), tv_step2_name, R.color.text_hint_color);
                                ZUtils.setTextColor(getActivity(), tv_step2, R.color.text_hint_color);

                                ZUtils.setViewBg(getActivity(), tv_step1, R.drawable.bg_step_circle_select);
                                ZUtils.setViewBg(getActivity(), tv_step2, R.drawable.bg_step_circle);

                                ZUtils.setTextColor(getActivity(), tv_step3_name, R.color.text_hint_color);
                                ZUtils.setViewBg(getActivity(), tv_step3, R.drawable.bg_step_circle);

                            }

                            @Override
                            public void onCancel() {
                                // 用户点击不同意，不做任何操作
                            }
                        });
            }
        });
        progressDialog.show();
    }

    /**
     * 更新进度对话框消息
     */
    private void updateProgressDialog(String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            if("UPLOAD_SUCCESS".equals(message)){
                progressDialog.setMessage("正在转写中");
            }
//            showProgressDialog("正在处理...");
        }
    }

    /**
     * 隐藏进度对话框
     */
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }



    /**
     * 处理识别错误
     */
    private void handleRecognitionError(String error) {
        Timber.tag("AIMeetingSub2Fragment").e( "识别失败: " + error);
        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
    }

    /**
     * 更新步骤进度条到第3步（提交转写）
     */
    private void updateStepToThree() {
        if (getActivity() == null) {
            return;
        }

        Timber.tag("AIMeetingSub2Fragment").d( "更新步骤进度条到第3步");

        // 更新步骤1：完成状态
        ZUtils.setTextColor(getActivity(), tv_step1_name, R.color.text_hint_color);
        ZUtils.setTextColor(getActivity(), tv_step1, R.color.text_hint_color);
        ZUtils.setViewBg(getActivity(), tv_step1, R.drawable.bg_step_circle);

        // 更新步骤2：完成状态
        ZUtils.setTextColor(getActivity(), tv_step2_name, R.color.text_hint_color);
        ZUtils.setTextColor(getActivity(), tv_step2, R.color.text_hint_color);
        ZUtils.setViewBg(getActivity(), tv_step2, R.drawable.bg_step_circle);

        // 更新步骤3：当前活跃状态
        ZUtils.setTextColor(getActivity(), tv_step3_name, R.color.text_black);
        ZUtils.setViewBg(getActivity(), tv_step3, R.drawable.bg_step_circle_select);

        // 隐藏所有步骤内容
        ll_step1.setVisibility(View.GONE);
        ll_step2.setVisibility(View.GONE);
    }

    /**
     * 从API加载语言选项数据
     */
    private void loadLanguageOptions() {
        Timber.tag("AIMeetingSub2Fragment").d( "开始加载语言选项数据");

        // 使用ViewModel获取语言选项
        viewModel.getOfflineEngineModelType().observe(this, result -> {
            if (result != null) {
                if (result.isSuccess() && result.getData() != null) {
                    Timber.tag("AIMeetingSub2Fragment").d( "语言选项加载成功，数量: " + result.getData().size());

                    // 转换API数据为OptionMeetingLan列表
                    List<OptionMeetingLan> options = new ArrayList<>();
                    java.util.Map<String, String> languageMap = result.getData();

                    for (java.util.Map.Entry<String, String> entry : languageMap.entrySet()) {
                        String key = entry.getValue();
                        String title = entry.getKey();

                        // 设置默认选中第一个选项（通常是中文）
                        boolean isSelected = key.equals(viewModel.getSelectedLanguage().getValue());

                        // 创建选项，使用key作为标识，title作为显示名称
                        OptionMeetingLan option = new OptionMeetingLan(key, title, getLanguageDescription(key), isSelected);
                        options.add(option);
                    }

                    // 更新适配器数据
                    if (optionAdapter != null) {
                        optionAdapter.updateData(options);
                    }

                } else {
                    Timber.tag("AIMeetingSub2Fragment").e( "语言选项加载失败: " + result.getError());
                    Toast.makeText(getContext(), "加载语言选项失败: " + result.getError(), Toast.LENGTH_SHORT).show();

                    // 加载失败时使用默认选项
                    loadDefaultLanguageOptions();
                }
            }
        });
    }

    /**
     * 根据语言key获取描述信息
     */
    private String getLanguageDescription(String key) {
        switch (key) {
            case "16k_zh_large":
                return "普通话大模型，识别准确";
            case "16k_zh":
                return "中文通用模型";
            case "16k_multi_lang":
                return "支持多种语言的大模型";
            case "16k_zh_dialect":
                return "普通话方言混合模型";
            case "16k_en":
                return "English recognition";
            case "16k_yue":
                return "广东话识别";
            case "16k_zh-PY":
                return "中英粤混合识别";
            case "16k_ja":
                return "日本語認識";
            case "16k_ko":
                return "한국어 인식";
            default:
                return "语音识别模型";
        }
    }

    /**
     * 加载默认语言选项（API失败时的备用方案）
     */
    private void loadDefaultLanguageOptions() {
        List<OptionMeetingLan> options = new ArrayList<>();
        options.add(new OptionMeetingLan("16k_zh_large", "普方英", "普通话大模型，识别准确度高", true));
        options.add(new OptionMeetingLan("16k_zh", "中文通用", "中文通用模型", false));
        options.add(new OptionMeetingLan("16k_en", "英语", "English recognition", false));
        options.add(new OptionMeetingLan("16k_yue", "粤语", "广东话识别", false));
        options.add(new OptionMeetingLan("16k_zh-PY", "中英粤", "中英粤混合识别", false));

        if (optionAdapter != null) {
            optionAdapter.updateData(options);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 切换界面时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清理进度对话框，防止内存泄漏
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        
        // 确保销毁时停止TTS播放
        TTSManager.Companion.getInstance().stop();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
//        Toast.makeText(getActivity(), "退出", Toast.LENGTH_SHORT).show();
    }



}
