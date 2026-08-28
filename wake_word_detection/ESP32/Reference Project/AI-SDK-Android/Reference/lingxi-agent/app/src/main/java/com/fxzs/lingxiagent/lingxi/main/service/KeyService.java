package com.fxzs.lingxiagent.lingxi.main.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.media.VolumeProviderCompat;

import com.fxzs.lingxiagent.MainActivity;
import com.fxzs.lingxiagent.R;

import timber.log.Timber;

public class KeyService extends Service {
	private MediaSessionCompat mediaSession;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private long lastPressTime = 0;
	private static final long LONG_PRESS_THRESHOLD = 1000;

	@Override
	public void onCreate() {
		super.onCreate();
		initMediaSession();
		startForeground(1, buildNotification());
	}

	private void initMediaSession() {
		mediaSession = new MediaSessionCompat(this, "VolumeWakeService");
		mediaSession.setCallback(new MediaSessionCompat.Callback() {});

		VolumeProviderCompat provider = new VolumeProviderCompat(
				VolumeProviderCompat.VOLUME_CONTROL_RELATIVE,
				100,
				50) {
			@Override
			public void onAdjustVolume(int direction) {
				long now = System.currentTimeMillis();
				Timber.tag("VolumeService").d(String.valueOf(now - lastPressTime));
				if (now - lastPressTime < LONG_PRESS_THRESHOLD) {
					Intent launch = new Intent(getApplicationContext(), MainActivity.class);
					launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(launch);
				}
				lastPressTime = now;
				Timber.tag("VolumeService").d("Volume direction: %s", direction);
			}
		};

		mediaSession.setPlaybackToRemote(provider);
		PlaybackStateCompat state = new PlaybackStateCompat.Builder()
			.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE |
					PlaybackStateCompat.ACTION_PLAY |
					PlaybackStateCompat.ACTION_PAUSE)
			.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
			.build();
		mediaSession.setPlaybackState(state);
		mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
		mediaSession.setActive(true);
	}

	private Notification buildNotification() {
		NotificationChannel channel = new NotificationChannel(
				"vol_wakeup", "音量唤醒服务", NotificationManager.IMPORTANCE_LOW);
		NotificationManager mgr = getSystemService(NotificationManager.class);
		mgr.createNotificationChannel(channel);

		return new NotificationCompat.Builder(this, "vol_wakeup")
				.setContentTitle("音量唤醒服务正在运行")
				.setContentText("请通过长按音量键唤醒App")
				.setSmallIcon(R.mipmap.ic_launcher)
				.setOngoing(true)
				.build();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		if (mediaSession != null) {
			mediaSession.release();
		}
		super.onDestroy();
	}
}
