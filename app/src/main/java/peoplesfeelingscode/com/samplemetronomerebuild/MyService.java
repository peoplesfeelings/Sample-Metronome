package peoplesfeelingscode.com.samplemetronomerebuild;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import org.apache.commons.io.FilenameUtils;

public class MyService extends Service {
    final int ONGOING_NOTIFICATION_ID = 4345;

    AudioTrack at;

    IBinder mBinder;

    Notification notification;
    Context context;

    HandlerThread handlerThread;

    String ext;

    double rate;
    boolean playing;
    long startTime;
    long lastTick;
    int count = 0;
    double interval;

    @Override
    public void onCreate() {
        super.onCreate();

        mBinder = new MyBinder();
        playing = false;
        context = getApplicationContext();

        handlerThread = new HandlerThread("MyHandlerThread");

        Storage.fileNeedsToBeLoaded = true;

        setUpForeground();

        Log.d(Dry.TAG, "service oncreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Dry.TAG, "service onbind");

        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(Dry.TAG, "service ondestroy");
    }

    void start() {
        handlerThread.start();
        final Handler handler = new Handler(handlerThread.getLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (Storage.fileNeedsToBeLoaded) {
                    if (!loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, context))) {
                        return;
                    }
                }

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

        Log.d(Dry.TAG, "service - start");
    }

    void stop() {
        handlerThread.quit();
        handlerThread = new HandlerThread("MyHandlerThread");
        stopForeground(true);
        playing = false;

        Log.d(Dry.TAG, "service - stop");
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

    boolean loadFile(String fileName) {
        boolean success;

        ext = FilenameUtils.getExtension(fileName);

        switch (ext.toLowerCase()) {
//            case("flac"):
//
//                break;
            case("wav"):
                success = AudioFiles.loadWav(fileName, this);
                break;
            case("mp3"):
                success = AudioFiles.loadMp3(fileName, this);
                break;
            default:
                success = false;
        }

        if (success) {
            Storage.fileNeedsToBeLoaded = false;
        } else {
            return false;
        }

        return true;
    }

    void handleFileProblem(String message) {
        stop();
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
