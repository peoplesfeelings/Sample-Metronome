package peoplesfeelingscode.com.samplemetronomerebuild;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

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

    final long TIMEOUTUS = 3000;

    AudioTrack at;

    IBinder mBinder;

    Notification notification;
    Context context;

    HandlerThread handlerThread;

    String ext;
    byte[] bytes;
    AudioFileInfo info;

    MediaCodec decoder;

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
            handleFileProblem("File not found. " + e.getMessage());
            return;
        }
        ext = FilenameUtils.getExtension(fileName);
        switch (ext.toLowerCase()) {
            case("flac"):

                break;
            case("wav"):
                loadWav(ins);
                break;
            case("mp3"):
                loadMp3(Storage.path + File.separator + fileName);
                break;
        }
        Storage.fileNeedsToBeLoaded = false;
    }

    void loadMp3(String fileLocation) {
        boolean reconfigure = true;

        short [] decoded = new short[0];
        int decodedIdx = 0;
        MediaFormat oformat = null;

        MediaExtractor extractor = new MediaExtractor();
        MediaFormat format;
        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        try {
            extractor.setDataSource(fileLocation);
        } catch (IOException e) {
            handleFileProblem("Error: exception thrown when trying to extract data from file " + fileLocation);
            return;
        }

        if (extractor.getTrackCount() > 1) {
            handleFileProblem("Multiple tracks in file.");
            return;
        }

        format = extractor.getTrackFormat(0);
        if (format.getString(MediaFormat.KEY_MIME).equals(MediaFormat.MIMETYPE_AUDIO_MPEG)) {
            extractor.selectTrack(0);
        } else {
            handleFileProblem("MIME type doesn't match file extension.");
            return;
        }

        try {
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_MPEG);
        } catch (IOException e) {
            handleFileProblem("Failed to instantiate MP3 MediaCodec.");
            return;
        }
        decoder.configure(format, null, null, 0);
        decoder.start();
        codecInputBuffers = decoder.getInputBuffers();
        codecOutputBuffers = decoder.getOutputBuffers();

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUTUS);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    int sampleSize = extractor.readSampleData(dstBuf, 0 );
                    long presentationTimeUs = 0;
                    if (sampleSize < 0) {
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                    }
                    decoder.queueInputBuffer(
                            inputBufIndex,
                            0 ,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                }
            }
            int res = decoder.dequeueOutputBuffer(info, TIMEOUTUS);
            if (res >= 0) {
                if (info.size > 0 && reconfigure) {
                    reconfigure = false;
                    extractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                    sawInputEOS = false;
                    decoder.stop();
                    decoder.configure(format, null, null, 0);
                    decoder.start();
                    codecInputBuffers = decoder.getInputBuffers();
                    codecOutputBuffers = decoder.getOutputBuffers();
                    continue;
                }
                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];
                if (decodedIdx + (info.size / 2) >= decoded.length) {
                    decoded = Arrays.copyOf(decoded, decodedIdx + (info.size / 2));
                }
                for (int i = 0; i < info.size; i += 2) {
                    decoded[decodedIdx++] = buf.getShort(i);
                }
                decoder.releaseOutputBuffer(outputBufIndex, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = decoder.getOutputBuffers();
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                oformat = decoder.getOutputFormat();
                Log.d("************", oformat.toString());
            }
        }

        extractor.release();
        decoder.stop();
        decoder.release();

        byte[] decodedBytes = Dry.MyShortToByte(decoded);

        Log.d("************", "decoded.length: " + decoded.length);
        Log.d("************", "decodedBytes.length: " + decodedBytes.length);

        at = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                (oformat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                AudioFormat.ENCODING_PCM_16BIT,
                decodedBytes.length,
                AudioTrack.MODE_STATIC);
        at.write(decodedBytes,0,decodedBytes.length);
        at.setPlaybackRate(oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE));

        Log.d("***********", "");
    }

    void loadWav(InputStream ins) {
        try {
            info = Storage.readHeader(ins);
            bytes = Storage.readWavPcm(info, ins);
        } catch (Exception e) {
            handleFileProblem("Error reading file. " + e.getMessage());

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
            handleFileProblem(constructFormatMessage(FORMAT, -1));
            return false;
        }
        if (!Dry.arrayContains(SUPPORTED_CHANNELS, info.channels)) {
            handleFileProblem(constructFormatMessage(CHANNELS, info.channels));
            return false;
        }
        if (!Dry.arrayContains(SUPPORTED_SAMPLE_RATES, info.rate)) {
            handleFileProblem(constructFormatMessage(RATE, info.rate));
            return false;
        }
        if (!Dry.arrayContains(SUPPORTED_BIT_DEPTHS, info.depth)) {
            handleFileProblem(constructFormatMessage(DEPTH, info.depth));
            return false;
        }
        if (info.dataSize <= 0) {
            handleFileProblem(constructFormatMessage(DATASIZE, info.dataSize));
            return false;
        }

        return true;
    }

    void handleFileProblem(String message) {
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
