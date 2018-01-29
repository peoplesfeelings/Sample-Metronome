package peoplesfeelingscode.com.samplemetronomerebuild;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static peoplesfeelingscode.com.samplemetronomerebuild.MyService.attribute.CHANNELS;
import static peoplesfeelingscode.com.samplemetronomerebuild.MyService.attribute.DATASIZE;
import static peoplesfeelingscode.com.samplemetronomerebuild.MyService.attribute.DEPTH;
import static peoplesfeelingscode.com.samplemetronomerebuild.MyService.attribute.FORMAT;
import static peoplesfeelingscode.com.samplemetronomerebuild.MyService.attribute.RATE;

public class MyService extends Service {
    final int ONGOING_NOTIFICATION_ID = 4345;
    final int[] SUPPORTED_BIT_DEPTHS = { 16 };
    final int[] SUPPORTED_CHANNELS = { 1, 2 };
    final int[] SUPPORTED_SAMPLE_RATES = { 44100, 48000 };

    AudioTrack at;

    IBinder mBinder;

    Notification notification;
    Context context;

    HandlerThread handlerThread;

    FilenameUtils filenameUtils;
    String fileLocation;
    String ext;
    byte[] bytes;
    AudioFileInfo info;

    double rate;
    boolean playing;
    long startTime;
    long lastTick;
    int count = 0;
    double interval;

    enum attribute { FORMAT, CHANNELS, RATE, DEPTH, DATASIZE};

    @Override
    public void onCreate() {
        super.onCreate();

        mBinder = new MyBinder();
        playing = false;
        context = getApplicationContext();

        handlerThread = new HandlerThread("MyHandlerThread");

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
        handlerThread.start();
        final Handler handler = new Handler(handlerThread.getLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (Storage.fileNeedsToBeLoaded) {
                    loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, context));
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

    void loadFile(String fileName) {
        InputStream ins;
        try {
            ins = new FileInputStream(new File(Storage.path, fileName));
        } catch (FileNotFoundException e) {
            //
            return;
        }
        ext = filenameUtils.getExtension(fileName);
        switch (ext.toLowerCase()) {
            case("flac"):

                break;
            case("wav"):
                loadWav(ins);
                break;
            case("mp3"):

                break;
        }
        Log.d("*************", "ext: " + ext);
        Storage.fileNeedsToBeLoaded = false;
    }

    void loadWav(InputStream ins) {
        try {
            info = Storage.readHeader(ins);
            bytes = Storage.readWavPcm(info, ins);
        } catch (Exception e) {
            handleIncompatibleFile("Error reading file. " + e.getMessage());

            Log.d("*************", "except");
            return;
        }

        if (fileSupported(info)) {
            at = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    info.rate,
                    (info.channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                    AudioFormat.ENCODING_PCM_16BIT,
                    bytes.length,
                    AudioTrack.MODE_STATIC);
            at.write(bytes,0,bytes.length);
            at.setPlaybackRate(info.rate);
        }
    }

    boolean fileSupported(AudioFileInfo info) {
        if (info.format != 1) {
            handleIncompatibleFile(constructFormatMessage(FORMAT, -1));
            return false;
        }
        if (!Dry.arrayContains(SUPPORTED_CHANNELS, info.channels)) {
            handleIncompatibleFile(constructFormatMessage(CHANNELS, info.channels));
            return false;
        }
        if (!Dry.arrayContains(SUPPORTED_SAMPLE_RATES, info.rate)) {
            handleIncompatibleFile(constructFormatMessage(RATE, info.rate));
            return false;
        }
        if (!Dry.arrayContains(SUPPORTED_BIT_DEPTHS, info.depth)) {
            handleIncompatibleFile(constructFormatMessage(DEPTH, info.depth));
            return false;
        }
        if (info.dataSize <= 0) {
            handleIncompatibleFile(constructFormatMessage(DATASIZE, info.dataSize));
            return false;
        }

        return true;
    }

    void handleIncompatibleFile(String message) {
        stop();
    }

    String constructFormatMessage(attribute attr, int value) {
        String msg = "Audio file not supported. ";

        switch (attr) {
            case FORMAT:
                msg += "Encoding must be PCM.";
                break;
            case CHANNELS:
                msg += "Channels found: " + value + ". Supported numbers of channels: " + Dry.concatCSV(SUPPORTED_CHANNELS) + ".";
                break;
            case RATE:
                msg += "Sample rate found: " + value + ". Supported sample rates: " + Dry.concatCSV(SUPPORTED_SAMPLE_RATES) + ".";
                break;
            case DEPTH:
                msg += "Bit depth found: " + value + ". Supported bit depths: " + Dry.concatCSV(SUPPORTED_BIT_DEPTHS) + ".";
                break;
            case DATASIZE:
                msg += "Audio data missing.";
                break;
        }

        return msg;
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
