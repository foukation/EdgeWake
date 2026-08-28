package com.fxzs.lingxiagent.util.ZUtil;

import android.graphics.Paint;
import android.os.Build;
import android.text.style.LineHeightSpan;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class TopMarginSpan implements LineHeightSpan {

	private final int marginTop;

	public TopMarginSpan(int marginTopPx) {
		this.marginTop = marginTopPx;
	}

	@Override
	public void chooseHeight(@NonNull CharSequence text,
	                         int start, int end,
	                         int spanstartv, int v,
	                         @NonNull Paint.FontMetricsInt fm) {
		// 这里不要只判断 start == 0，因为加粗可能出现在中间
		// 所以对所有带这个 span 的行都加 margin
		fm.ascent -= marginTop;
		fm.top -= marginTop;
	}
}