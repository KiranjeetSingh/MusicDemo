package com.musicdemo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity" ;
    private Button btnDownload,btnPlay,btnStop;
    private final int MY_PERMISSIONS_REQUEST_STORAGE = 99;
    private BackgroundSoundService.AudioServiceBinder audioServiceBinder = null;
    File outputFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnDownload = (Button)findViewById(R.id.btnDownload);
        btnPlay = (Button)findViewById(R.id.btnPlay);
        btnStop = (Button)findViewById(R.id.btnStop);
        btnDownload.setOnClickListener(this);
        btnPlay.setOnClickListener(this);
        btnStop.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btnDownload:
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST_STORAGE);
                    } else {
                        // No explanation needed; request the permission
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST_STORAGE);
                    }
                }
                else {
                    new DownloadingTask().execute();
                }
                break;

            case R.id.btnPlay:
                if(audioServiceBinder != null) {
                    audioServiceBinder.stopAudio();
                    audioServiceBinder.startAudio(outputFile);
                    btnPlay.setEnabled(false);
                    btnStop.setEnabled(true);
                }
                break;

            case R.id.btnStop:
                if(audioServiceBinder != null) {
                    audioServiceBinder.stopAudio();
                    btnPlay.setEnabled(true);
                    btnStop.setEnabled(false);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new DownloadingTask().execute();
                } else {
                    Toast.makeText(MainActivity.this,"Permission Denied",Toast.LENGTH_SHORT).show();
                }
                return;
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private class DownloadingTask extends AsyncTask<Void, Void, Void> {

        File apkStorage = null;

        String downloadUrl = "https://ia802508.us.archive.org/5/items/testmp3testfile/mpthreetest.mp3";
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            btnDownload.setEnabled(false);
            btnPlay.setEnabled(false);
            btnStop.setEnabled(false);
            btnDownload.setText(R.string.downloadStarted);//Set Button Text when download started
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                if (outputFile != null) {
                    btnPlay.setEnabled(true);
                    btnStop.setEnabled(true);
                    btnDownload.setText(R.string.downloadCompleted);//If Download completed then change button text
                    bindAudioService();
                } else {
                    btnDownload.setText(R.string.downloadFailed);//If download failed change button text
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            btnDownload.setEnabled(true);
                            btnPlay.setEnabled(false);
                            btnStop.setEnabled(false);
                            btnDownload.setText(R.string.downloadAgain);//Change button text again after 3sec
                        }
                    }, 3000);

                    Log.e(TAG, "Download Failed");

                }
            } catch (Exception e) {
                e.printStackTrace();

                //Change button text if exception occurs
                btnDownload.setText(R.string.downloadFailed);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        btnDownload.setEnabled(true);
                        btnPlay.setEnabled(false);
                        btnStop.setEnabled(false);
                        btnDownload.setText(R.string.downloadAgain);
                    }
                }, 3000);
                Log.e(TAG, "Download Failed with Exception - " + e.getLocalizedMessage());

            }


            super.onPostExecute(result);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                URL url = new URL(downloadUrl);//Create Download URl
                HttpURLConnection c = (HttpURLConnection) url.openConnection();//Open Url Connection
                c.setRequestMethod("GET");//Set Request Method to "GET" since we are grtting data
                c.connect();//connect the URL Connection

                //If Connection response is not OK then show Logs
                if (c.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Server returned HTTP " + c.getResponseCode()
                            + " " + c.getResponseMessage());

                }
                apkStorage = new File(Environment.getExternalStorageDirectory() + "/" + "MusicDemo");
                //If File is not present create directory
                if (!apkStorage.exists()) {
                    apkStorage.mkdir();
                    Log.e(TAG, "Directory Created.");
                }

                outputFile = new File(apkStorage, "demo.mp3");//Create Output file in Main File

                //Create New File if not present
                if (!outputFile.exists()) {
                    outputFile.createNewFile();
                    Log.e(TAG, "File Created");
                }

                FileOutputStream fos = new FileOutputStream(outputFile);//Get OutputStream for NewFile Location

                InputStream is = c.getInputStream();//Get InputStream for connection

                byte[] buffer = new byte[1024];//Set buffer type
                int len1 = 0;//init length
                while ((len1 = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len1);//Write new file
                }

                //Close all connection after doing task
                fos.close();
                is.close();

            } catch (Exception e) {

                //Read exception if something went wrong
                e.printStackTrace();
                outputFile = null;
                Log.e(TAG, "Download Error Exception " + e.getMessage());
            }

            return null;
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // Cast and assign background service's onBind method returned iBander object.
            audioServiceBinder = (BackgroundSoundService.AudioServiceBinder) iBinder;
            audioServiceBinder.setContext(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private void bindAudioService(){
        if(audioServiceBinder == null) {
            Intent intent = new Intent(MainActivity.this, BackgroundSoundService.class);

            // Below code will invoke serviceConnection's onServiceConnected method.
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    // Unbound background audio service with caller activity.
    private void unBoundAudioService()
    {
        if(audioServiceBinder != null) {
            unbindService(serviceConnection);
        }
    }
    @Override
    protected void onDestroy() {
        // Unbound background audio service when activity is destroyed.
        unBoundAudioService();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("music_action"));
        super.onDestroy();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            btnPlay.setEnabled(true);
            btnStop.setEnabled(false);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("music_action"));
    }

}
