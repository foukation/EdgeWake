package com.fxzs.lingxiagent.util.ZUtil;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.BulletSpan;


public class MdSmallerBulletSpan extends BulletSpan {
	private final int radius;

	public MdSmallerBulletSpan(int gapWidth, int color, int radius) {
		super(gapWidth, color);
		this.radius = radius;
	}

	@Override
	public void drawLeadingMargin(Canvas c, Paint p, int x, int dir,
	                              int top, int baseline, int bottom,
	                              CharSequence text, int start, int end,
	                              boolean first, Layout layout) {


		if (!first) {
			// 只在首行画点，后续行保持缩进但不画点
			return;
		}

		if (((Spanned) text).getSpanStart(this) == start) {
			// 仿照父类的绘制，只是用我们自己的 radius
			Paint.Style oldStyle = p.getStyle();
			int oldColor = p.getColor();

			p.setStyle(Paint.Style.FILL);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				if (getColor() != 0) {
					p.setColor(getColor());
				}
			}

			float y = (top + bottom) / 2f;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				c.drawCircle(x + dir * getGapWidth() / 2f, y, radius, p);
			}

			p.setStyle(oldStyle);
			p.setColor(oldColor);
		}


	}
}