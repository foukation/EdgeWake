package com.fxzs.lingxiagent.view.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.fxzs.lingxiagent.R;

/**
 * 创建者：ZyOng
 * 描述：
 * 创建时间：2025/8/25 下午4:34
 */
public class HintDialogFragment extends DialogFragment {

    private String message;

    public static HintDialogFragment newInstance(String message) {
        HintDialogFragment fragment = new HintDialogFragment();
        Bundle args = new Bundle();
        args.putString("message", message);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_hint, container, false);
        TextView tvMessage = view.findViewById(R.id.tv_content);
        TextView tvCancel = view.findViewById(R.id.btn_cancel);
        TextView tvCheck = view.findViewById(R.id.btn_check);

        if (getArguments() != null) {
            message = getArguments().getString("message");
            tvMessage.setText(message);
        }

        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        tvCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    Uri initialUri = Uri.parse(
//                            "content://com.android.externalstorage.documents/document/primary:Download"
//                    );
//                    intent.putExtra("android.provider.extra.INITIAL_URI", initialUri);
//                }
//
//                startActivity(intent);
                dismiss();
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.HintDialogStyle);
    }
}
