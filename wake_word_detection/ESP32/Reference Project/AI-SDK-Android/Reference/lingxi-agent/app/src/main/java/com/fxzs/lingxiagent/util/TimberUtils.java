package com.fxzs.lingxiagent.util;
import timber.log.Timber;

public class TimberUtils {

	public static void logLong(String tag, String message) {
		if (message == null || message.isEmpty()) return;

		final int maxLength = 4000;
		int length = message.length();
		for (int i = 0; i < length; i += maxLength) {
			int end = Math.min(length, i + maxLength);
			Timber.tag(tag).d(message.substring(i, end));
		}
	}
}