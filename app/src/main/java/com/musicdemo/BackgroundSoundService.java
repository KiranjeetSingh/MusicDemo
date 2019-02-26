package com.musicdemo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;

public class BackgroundSoundService extends Service {
    private AudioServiceBinder audioServiceBinder = new AudioServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return audioServiceBinder;
    }
    class AudioServiceBinder extends Binder {

        // Save local audio file uri ( local storage file. ).
        private Uri audioFileUri = null;

        // Save web audio file url.
        private String audioFileUrl = "";

        // Check if stream audio.
        private boolean streamAudio = false;

        // Media player that play audio.
        private MediaPlayer audioPlayer = null;

        // Caller activity context, used when play local audio file.
        private Context context = null;

        public Context getContext() {
            return context;
        }

        public void setContext(Context context) {
            this.context = context;
        }

        public String getAudioFileUrl() {
            return audioFileUrl;
        }

        public void setAudioFileUrl(String audioFileUrl) {
            this.audioFileUrl = audioFileUrl;
        }

        public boolean isStreamAudio() {
            return streamAudio;
        }

        public void setStreamAudio(boolean streamAudio) {
            this.streamAudio = streamAudio;
        }

        public Uri getAudioFileUri() {
            return audioFileUri;
        }

        public void setAudioFileUri(Uri audioFileUri) {
            this.audioFileUri = audioFileUri;
        }

        // Start play audio.
        public void startAudio(File file)
        {
            initAudioPlayer(file);
            if(audioPlayer!=null) {
                audioPlayer.start();
                audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Intent intent = new Intent("music_action");
                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
                    }
                });
            }
        }

        public MediaPlayer getAudioPlayer() {
            return audioPlayer;
        }

        // Stop play audio.
        public void stopAudio()
        {
            if(audioPlayer!=null) {
                audioPlayer.stop();
                destroyAudioPlayer();
            }
        }

        // Initialise audio player.
        private void initAudioPlayer(File file)
        {
            try {
                if (audioPlayer == null) {
                    audioPlayer = new MediaPlayer();
                    audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    audioPlayer.setDataSource(file.getPath());
                }

                audioPlayer.prepare();

            }catch(IOException ex)
            {
                ex.printStackTrace();
            }
        }

        // Destroy audio player.
        private void destroyAudioPlayer()
        {
            if(audioPlayer!=null)
            {
                if(audioPlayer.isPlaying())
                {
                    audioPlayer.stop();
                }

                audioPlayer.release();

                audioPlayer = null;
            }
        }


    }
}