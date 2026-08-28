package com.cmdc.ai.assist.demo.media;

import android.media.MediaPlayer;
import android.text.TextUtils;

import java.util.concurrent.LinkedBlockingDeque;

public class TtsMediaPlayer {
    private static final String TAG = "TtsMediaPlayer";
    private final LinkedBlockingDeque<String> ttsUrls = new LinkedBlockingDeque<>();
    private String currentQid = null;
    private MediaPlayer player;
    private MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            checkToPlay();
        }
    };

    public void stop() {
        currentQid = null;
        ttsUrls.clear();
        if (player != null)
            player.stop();
    }

    public void release() {
        if (player == null)
            return;
        player.setOnCompletionListener(null);
        player.release();
        player = null;
    }

    public void speak(String qid, String ttsUrl) {
        if (!TextUtils.equals(qid, currentQid)) {
            stop();
            currentQid = qid;
        }
        MediaPlayerUtils.Companion.getInstance().stop();
        ttsUrls.add(ttsUrl);
        checkToPlay();
    }

    private void checkToPlay() {
        if (player.isPlaying()) {
            return;
        }
        String url = ttsUrls.poll();
        try {
            player.reset();
            player.setDataSource(url);
            player.prepare();
            player.start();
        } catch (Exception e) {
            // ..
        }
    }


    public static TtsMediaPlayer getInstance() {
        return Holder.INSTANCE;
    }

    private TtsMediaPlayer() {
        player = new MediaPlayer();
        player.setOnCompletionListener(completionListener);
    }

    private static class Holder {
        private static final TtsMediaPlayer INSTANCE = new TtsMediaPlayer();
    }
}
