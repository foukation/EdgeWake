package com.fxzs.lingxiagent.view.aifile;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.AppPermissionRequestManager;
import com.fxzs.lingxiagent.view.excel.PhotoUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AiFilePickImageBottomSheetDialog extends BottomSheetDialogFragment {

    public interface Listener {
        void onCancel();
    }

    public static final int REQ_CAMERA = 9101;

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_ai_file_pick_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        View btnCamera = view.findViewById(R.id.btn_camera);
        View btnAlbum = view.findViewById(R.id.btn_album);
        View btnCancel = view.findViewById(R.id.btn_cancel);
        View parent = (View) view.getParent();
        if (parent != null) {
            parent.setBackgroundColor(Color.TRANSPARENT);
        }

        btnCamera.setOnClickListener(v -> {
            Activity act = getActivity();
            if (act == null) {
                dismiss();
                return;
            }

            if (ContextCompat.checkSelfPermission(act, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                PhotoUtils.startCamera(act);
                dismiss();
                return;
            }

            AppPermissionRequestManager.requestCameraPermission(act, REQ_CAMERA);
            dismiss();
        });

        btnAlbum.setOnClickListener(v -> {
            Activity act = getActivity();
            if (act != null) {
                PhotoUtils.startAlbum(act);
            }
            dismiss();
        });

//        btnCancel.setOnClickListener(v -> {
//            if (listener != null) listener.onCancel();
//            dismiss();
//        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Activity act = getActivity();
        if (act == null) return;

        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PhotoUtils.startCamera(act);
            }
        }
    }
}
