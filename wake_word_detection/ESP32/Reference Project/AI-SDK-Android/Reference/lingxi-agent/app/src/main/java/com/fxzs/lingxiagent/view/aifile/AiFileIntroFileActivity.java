package com.fxzs.lingxiagent.view.aifile;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.model.common.Constants;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.view.common.BaseActivity;
import com.fxzs.lingxiagent.view.common.CommonDialog;
import com.fxzs.lingxiagent.view.excel.PhotoUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

import timber.log.Timber;

public class AiFileIntroFileActivity extends BaseActivity<AiFileToolViewModel> {

    private static final String TAG = "AiFileIntroFile";

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_ai_file_intro;
    }

    @Override
    protected Class<AiFileToolViewModel> getViewModelClass() {
        return AiFileToolViewModel.class;
    }

    @Override
    protected void setupDataBinding() {

    }

    @Override
    protected void initializeViews() {
        View back = findViewById(R.id.back);
        TextView headerTitle = findViewById(R.id.tv_header_title);
        if (headerTitle != null) {
            headerTitle.setText(AiFileToolTypes.getTitle(getToolType()));
        }
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        View headerBg = findViewById(R.id.v_header_bg);
        if (headerBg != null) {
            headerBg.setBackgroundResource(AiFileToolTypes.getHeaderBgRes(getToolType()));
        }

        TextView title = findViewById(R.id.tv_tool_title);
        if (title != null) {
            title.setText(AiFileToolTypes.getTitle(getToolType()));
            title.setTextColor(AiFileToolTypes.getTextColor(getToolType()));
        }

        TextView subTitle = findViewById(R.id.tv_tool_subtitle);
        if (subTitle != null) {
            subTitle.setText(AiFileToolTypes.getSubTitle(getToolType()));
            subTitle.setTextColor(AiFileToolTypes.getTextColor(getToolType()));
        }

        ImageView icon = findViewById(R.id.iv_tool_icon);
        if (icon != null) {
            icon.setImageResource(AiFileToolTypes.getHeaderIconRes(getToolType()));
        }

        TextView bullet1 = findViewById(R.id.tv_bullet_1);
        TextView bullet2 = findViewById(R.id.tv_bullet_2);
        String[] bullets = AiFileToolTypes.getIntroBullets(getToolType());
        if (bullet1 != null && bullets.length > 0) {
            bullet1.setText(bullets[0]);
        }
        if (bullet2 != null && bullets.length > 1) {
            bullet2.setText(bullets[1]);
        }

        View btn = findViewById(R.id.btn_action);
        if (btn instanceof TextView) {
            ((TextView) btn).setText("上传文件");
        }
        if (btn != null) {
            btn.setOnClickListener(v -> openFileSelectorWithPermission());
        }
    }

    private void openFileSelectorWithPermission() {
        boolean aBoolean = SharedPreferencesUtil.getBoolean(Constants.SP_TYPE_PERMISSIONS_EXCEL_FILE, false);
        if (!aBoolean) {
            AppPermissionRequestManager.basePermissionStyle(true, this, "允许访问文件权限", "请授权文件权限，以便对文件进行上传", new CommonDialog.OnDialogClickListener() {
                @Override
                public void onConfirm() {
                    PhotoUtils.openFileSelector(AiFileIntroFileActivity.this);
                    SharedPreferencesUtil.saveBoolean(Constants.SP_TYPE_PERMISSIONS_EXCEL_FILE, true);
                }

                @Override
                public void onCancel() {
                    // no-op
                }
            });
        } else {
            PhotoUtils.openFileSelector(this);
        }
    }

    @Override
    protected void setupObservers() {

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWhiteStatusBar();
    }

    private int getToolType() {
        return getIntent().getIntExtra(AiFileToolTypes.EXTRA_TOOL_TYPE, AiFileToolTypes.TOOL_WORD_TO_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == PhotoUtils.FILE_SELECTOR_CODE && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;

            Timber.tag(TAG).d("FILE_SELECTOR_CODE result: uri=%s", uri);

            String path = null;

            // SAF/文档选择器通常返回 content://，在 Android 10+ 分区存储下即使能解析出 /storage/emulated/0/... 也可能无权限读取。
            // 为避免后续上传（COS 计算 MD5）出现 EACCES，这里对 content:// 一律复制到 cache 后再走文件路径。
            if (uri != null && ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                String copied = copyUriToCache(uri);
                if (copied != null) {
                    path = copied;
                } else {
                    path = uri.toString();
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    path = PhotoUtils.getPath(this, uri);
                }

                // 兜底：解析失败或文件不存在时，尝试复制
                if (path == null || path.startsWith("content://") || !new File(path).exists()) {
                    String copied = copyUriToCache(uri);
                    if (copied != null) {
                        path = copied;
                    } else if (path == null) {
                        path = uri.toString();
                    }
                }
            }

            Timber.tag(TAG).d("FILE_SELECTOR_CODE resolved path=%s", path);

            if (!isFileFormatAllowed(uri)) {
                return;
            }

            startActivity(new Intent(this, AiFilePickFileActivity.class)
                    .putExtra(AiFileToolTypes.EXTRA_TOOL_TYPE, getToolType())
                    .putExtra(AiFilePickFileActivity.EXTRA_FILE_URI, uri.toString())
                    .putExtra(AiFilePickFileActivity.EXTRA_FILE_PATH, path));
        }
    }

    private String copyUriToCache(Uri uri) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            String name = queryDisplayName(uri);
            if (name == null || name.trim().isEmpty()) {
                name = "aifile_" + System.currentTimeMillis();
            }

            File dir = new File(getCacheDir(), "aifile");
            if (!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }
            File dst = new File(dir, name);

            in = getContentResolver().openInputStream(uri);
            if (in == null) return null;

            out = new FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();

            Timber.tag(TAG).d("copyUriToCache success: %s (%s bytes)", dst.getAbsolutePath(), dst.length());
            return dst.getAbsolutePath();
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "copyUriToCache failed, uri=%s", uri);
            return null;
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {
            }
            try {
                if (out != null) out.close();
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isFileFormatAllowed(Uri uri) {
        int toolType = getToolType();

        String mime = null;
        try {
            mime = getContentResolver().getType(uri);
        } catch (Exception ignored) {
        }
        if (mime != null) {
            mime = mime.toLowerCase(Locale.ROOT);
        }

        String name = queryDisplayName(uri);
        String ext = null;
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) {
                ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
        }

        boolean ok;
        String tip;
        switch (toolType) {
            case AiFileToolTypes.TOOL_WORD_TO_PDF:
            case AiFileToolTypes.TOOL_WORD_TO_IMG:
                ok = isWord(mime, ext);
                tip = "请选择 Word 文件（.doc/.docx）";
                break;
            case AiFileToolTypes.TOOL_PPT_TO_PDF:
            case AiFileToolTypes.TOOL_PPT_TO_IMG:
                ok = isPpt(mime, ext);
                tip = "请选择 PPT 文件（.ppt/.pptx）";
                break;
            case AiFileToolTypes.TOOL_PDF_TO_IMG:
            case AiFileToolTypes.TOOL_PDF_TO_PPT:
            case AiFileToolTypes.TOOL_PDF_TO_WORD:
                ok = isPdf(mime, ext);
                tip = "请选择 PDF 文件（.pdf）";
                break;
            default:
                ok = true;
                tip = null;
                break;
        }

        if (!ok) {
            showToast(tip);
            return false;
        }
        return true;
    }

    private static boolean isWord(String mime, String ext) {
        if ("doc".equals(ext) || "docx".equals(ext)) return true;
        if (mime == null) return false;
        return mime.contains("msword") || mime.contains("wordprocessingml");
    }

    private static boolean isPpt(String mime, String ext) {
        if ("ppt".equals(ext) || "pptx".equals(ext)) return true;
        if (mime == null) return false;
        return mime.contains("ms-powerpoint") || mime.contains("presentation");
    }

    private static boolean isPdf(String mime, String ext) {
        if ("pdf".equals(ext)) return true;
        if (mime == null) return false;
        return mime.contains("application/pdf") || mime.endsWith("/pdf");
    }

    private String queryDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    return cursor.getString(idx);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}
