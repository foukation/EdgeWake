package com.fxzs.lingxiagent.view.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fxzs.lingxiagent.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ExportFileDialog {

    public interface OnExportOptionSelected {
        void onWordSelected();
        void onPdfSelected();
        void onTxtSelected();
    }

    public static void showExportDialog(Context context, OnExportOptionSelected listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_bottom_export, null);
        dialog.setContentView(view);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        TextView tvWord = view.findViewById(R.id.tv_word);
        TextView tvPdf = view.findViewById(R.id.tv_pdf);
        TextView tvTxt = view.findViewById(R.id.tv_txt);
        Button btnExport = view.findViewById(R.id.btn_export);

        final int[] selectedOption = {-1};
        resetAllOptions(tvWord, tvPdf, tvTxt);

        tvWord.setOnClickListener(v -> {
            selectedOption[0] = 0;
            setSelectedOption(tvWord, tvPdf, tvTxt, 0);
        });

        tvPdf.setOnClickListener(v -> {
            selectedOption[0] = 1;
            setSelectedOption(tvWord, tvPdf, tvTxt, 1);
        });

        tvTxt.setOnClickListener(v -> {
            selectedOption[0] = 2;
            setSelectedOption(tvWord, tvPdf, tvTxt, 2);
        });

        btnExport.setOnClickListener(v -> {
            if (selectedOption[0] == -1) {
                Toast.makeText(context, "请选择导出格式", Toast.LENGTH_SHORT);
                return;
            }

            switch (selectedOption[0]) {
                case 0:
                    listener.onWordSelected();
                    break;
                case 1:
                    listener.onPdfSelected();
                    break;
                case 2:
                    listener.onTxtSelected();
                    break;
            }
            dialog.dismiss();
        });
        view.findViewById(R.id.iv_close).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        expandDialog(dialog, view);
    }

    private static void resetAllOptions(TextView tvWord, TextView tvPdf, TextView tvTxt) {
        tvWord.setSelected(false);
        tvPdf.setSelected(false);
        tvTxt.setSelected(false);
    }

    private static void setSelectedOption(TextView tvWord, TextView tvPdf, TextView tvTxt, int option) {
        resetAllOptions(tvWord, tvPdf, tvTxt);

        switch (option) {
            case 0:
                tvWord.setSelected(true);
                break;
            case 1:
                tvPdf.setSelected(true);
                break;
            case 2:
                tvTxt.setSelected(true);
                break;
        }
    }

    private static void expandDialog(BottomSheetDialog dialog, View view) {
        View parent = (View) view.getParent();
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
    }
}