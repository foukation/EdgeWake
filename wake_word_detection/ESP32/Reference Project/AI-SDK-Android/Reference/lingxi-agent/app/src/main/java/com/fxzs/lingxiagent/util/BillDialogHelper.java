package com.fxzs.lingxiagent.util;

import android.content.Context;
import com.fxzs.lingxiagent.view.common.CommonDialog;
public class BillDialogHelper {

    public static void showBillDialog(
            Context context,
            String message
    ) {
        showBillDialog(context, message, null);
    }

    public static void showBillDialog(
            Context context,
            String message,
            Runnable onConfirmAction
    ) {

        CommonDialog.showConfirmOneBtnDialog(
                context,
                "灵犀权益包",
                message,
                "我知道了",
                new CommonDialog.OnDialogClickListener() {

                    @Override
                    public void onConfirm() {

                        if (onConfirmAction != null) {
                            onConfirmAction.run();
                        }
                    }

                    @Override
                    public void onCancel() {
                    }
                }
        );
    }
}