package peoplesfeelingscode.com.samplemetronomerebuild;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.File;

public class MyService extends Service {
    final int ONGOING_NOTIFICATION_ID = 4345;
    final int MAX_STREAMS = 16;

    IBinder mBinder;

    Notification notification;
    Context context;

    double rate;
    boolean loopRunning;
    long startTime;
    long lastTick;
    int count = 0;
    long interval;

    int soundId;
    SoundPool sounds;

    @Override
    public void onCreate() {
        super.onCreate();

        mBinder = new MyBinder();
        loopRunning = false;
        context = getApplicationContext();

        lastTick = System.currentTimeMillis();

        createSoundPool();
        loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, context));

        setUpForeground();

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

    void start() {
        startForeground(ONGOING_NOTIFICATION_ID, notification);
        loopRunning = true;
        loop();

        Log.d("*************", "service - start");
    }

    void stop() {
        stopForeground(true);
        loopRunning = false;

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

    void loop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (Storage.fileNeedsToBeLoaded) {
                    loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, context));
                }
                lastTick = System.currentTimeMillis();
                sounds.play(soundId, 1, 1, 1, 0, 1f);

                while (loopRunning) {
                    if (Storage.fileNeedsToBeLoaded) {
                        loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, context));
                    }
                    if (System.currentTimeMillis() > lastTick + interval) {
                        count++;
                        lastTick = startTime + count * interval;

                        sounds.play(soundId, 1, 1, 1, 0, 1f);
                    }
                }
            }
        }).start();
    }

    void loadFile(String fileName) {
        soundId = sounds.load(Storage.path + File.separator + fileName, 1);
        Storage.fileNeedsToBeLoaded = false;
    }

    void setInterval(double fta) {
        if (lastTick != 0L) {
            lastTick = System.currentTimeMillis();
        }

        if (loopRunning) {
            startTime = lastTick;
        } else {
            startTime = System.currentTimeMillis();
            lastTick = startTime;
        }
        count = 0;

        double bpm = Storage.ftaToBpm(fta);
        double beat = Dry.MILLIS_IN_MINUTE / bpm;
        int intervalMillis = (int) (beat / rate);

        interval = intervalMillis;
    }

    class MyBinder extends Binder {
        MyService getService() {
            return MyService.this;
        }
    }
}
