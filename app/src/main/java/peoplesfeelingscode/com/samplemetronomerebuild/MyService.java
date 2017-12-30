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
    long timeReference;
    long lastCycle;
    int cycle = 0;
    long period;

    String fileLocation;
    int soundId;
    SoundPool sounds;

    @Override
    public void onCreate() {
        super.onCreate();

        mBinder = new MyBinder();
        loopRunning = false;
        context = getApplicationContext();

        lastCycle = System.currentTimeMillis();

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
                        .setSmallIcon(R.drawable.notif_ic_18)
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
                lastCycle = System.currentTimeMillis();
                sounds.play(soundId, 1, 1, 1, 0, 1f);

                while (loopRunning) {
                    if (Storage.fileNeedsToBeLoaded) {
                        loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, context));
                    }
                    if (System.currentTimeMillis() > lastCycle + period) {
                        cycle++;
                        lastCycle = timeReference + cycle * period;

                        sounds.play(soundId, 1, 1, 1, 0, 1f);
                    }
                }
            }
        }).start();
    }

    void loadFile(String fileName) {
        fileLocation = Storage.path + File.separator + fileName;
        soundId = sounds.load(fileLocation, 1);
        Storage.fileNeedsToBeLoaded = false;
    }

    void setPeriod(double fta) {
        /*
        loopRunning bool is assigned true after setPeriod() is called in btnStartStop handler so the following
        should only be true if bpm is being changed while loop is running. this allows smooth change of tempo
        while metronome is playing
        */
        if (lastCycle != 0L && loopRunning) {
            timeReference = lastCycle;
        } else {
            timeReference = System.currentTimeMillis();
            lastCycle = timeReference;
        }
        cycle = 0;

        double bpm = Storage.ftaToBpm(fta);
        double beat = 60000 / bpm;
        int intervalMillis = (int) (beat / rate);

        period = intervalMillis;
    }

    class MyBinder extends Binder {
        MyService getService() {
            return MyService.this;
        }
    }
}
