package peoplesfeelingscode.com.samplemetronomerebuild;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.InputStream;

public class MyService extends Service {
    final int ONGOING_NOTIFICATION_ID = 4345;
    final int MAX_STREAMS = 16;

    AudioTrack at;

    IBinder mBinder;

    Notification notification;
    Context context;

    HandlerThread handlerThread;

    double rate;
    boolean playing;
    long startTime;
    long lastTick;
    int count = 0;
    double interval;

    int soundId;
    SoundPool sounds;

    @Override
    public void onCreate() {
        super.onCreate();

        mBinder = new MyBinder();
        playing = false;
        context = getApplicationContext();

        handlerThread = new HandlerThread("MyHandlerThread");

        createSoundPool();
        loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, context));

        setUpForeground();

        setUpTestAudioTrack();

        Log.d("**************", "service oncreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("**************", "service onbind");

        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d("**************", "service ondestroy");
    }

    void setUpTestAudioTrack() {
        InputStream ins = getResources().openRawResource((int) R.raw.test);
        byte[] bytes;
        WavInfo info;
        try {
            info = Storage.readHeader(ins);
            bytes = Storage.readWavPcm(info, ins);
        } catch (Exception e) {
            Log.d("*************", "except");
            return;
        }
        at = new AudioTrack(AudioManager.STREAM_NOTIFICATION, info.rate, info.channels, AudioFormat.ENCODING_PCM_16BIT, bytes.length, AudioTrack.MODE_STATIC);
        at.write(bytes,0,bytes.length);
        at.setPlaybackRate(info.rate);
    }

    void start() {
        handlerThread.start();
        final Handler handler = new Handler(handlerThread.getLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (Storage.fileNeedsToBeLoaded) {
                    loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, context));
                }

//                sounds.play(soundId, 1, 1, 1, 0, 1f);
                if (at.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    at.stop();
                }
                at.play();

                if (startTime == -1) {
                    startTime = SystemClock.uptimeMillis();
                    lastTick = startTime;
                    count = 0;
                } else {
                    count++;
                    lastTick = startTime + (long) (count * interval);
                }

                handler.postAtTime(this, (long) (lastTick + interval));
            }
        });

        startForeground(ONGOING_NOTIFICATION_ID, notification);
        playing = true;

        Log.d("*************", "service - start");
    }

    void stop() {
        handlerThread.quit();
        handlerThread = new HandlerThread("MyHandlerThread");
        stopForeground(true);
        playing = false;

        Log.d("*************", "service - stop");
    }

    void setUpForeground() {
        Intent intent = new Intent(context, ActivityMain.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, intent, 0);

        notification =
                new Notification.Builder(context)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentTitle(getText(R.string.notifTitle))
                        .setContentText(getText(R.string.notifText))
                        .setSmallIcon(R.drawable.notif_icon)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.notifTicker))
                        .build();
    }

    void createSoundPool() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createNewSoundPool();
        } else {
            createOldSoundPool();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    void createNewSoundPool(){
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        sounds = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(MAX_STREAMS)
                .build();
    }

    @SuppressWarnings("deprecation")
    void createOldSoundPool(){
        sounds = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC,0);
    }

    void loadFile(String fileName) {
        soundId = sounds.load(Storage.path + File.separator + fileName, 1);
        Storage.fileNeedsToBeLoaded = false;
    }

    void setInterval(double fta) {
        if (playing) {
            startTime = lastTick;
            count = 0;
        } else {
            startTime = -1;
        }

        double bpm = Storage.ftaToBpm(fta);
        double beatDuration = Dry.MILLIS_IN_MINUTE / bpm;
        interval = (beatDuration / rate);
    }

    class MyBinder extends Binder {
        MyService getService() {
            return MyService.this;
        }
    }
}
