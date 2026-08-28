package com.fxzs.lingxiagent.util;

import android.app.ActivityManager;
import android.content.Context;

public class TypingPerformanceUtil {

	/**
	 * step：1 最细腻，6 最省性能
	 */
	public static int calculateTypingStep(Context context) {
		ActivityManager am =
				(ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

		ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
		am.getMemoryInfo(info);

		int cores = Runtime.getRuntime().availableProcessors();
		long ramGB = info.totalMem / (1024L * 1024 * 1024);

		int step;

		if (am.isLowRamDevice()) {
			step = 6;
		} else if (cores >= 8 && ramGB >= 8) {
			step = 1;
		} else if (cores >= 8 && ramGB >= 6) {
			step = 2;
		} else if (cores >= 6 && ramGB >= 4) {
			step = 3;
		} else if (cores >= 4 && ramGB >= 3) {
			step = 4;
		} else {
			step = 5;
		}

		return step;
	}
}