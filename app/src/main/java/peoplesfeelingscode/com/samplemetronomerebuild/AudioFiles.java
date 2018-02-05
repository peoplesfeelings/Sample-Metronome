package peoplesfeelingscode.com.samplemetronomerebuild;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static android.content.ContentValues.TAG;

public class AudioFiles {
    static final int WAV_HEADER_SIZE = 44;
    static final long TIMEOUTUS = 3000;
    static final int[] SUPPORTED_BIT_DEPTHS = { 16 };
    static final int[] SUPPORTED_CHANNELS = { 1, 2 };
    static final int[] SUPPORTED_SAMPLE_RATES = { 44100, 48000 };

    //////////////// mp3 stuff /////////////////

    static boolean loadMp3(String fileName, MyService service) {
        MediaCodec decoder;
        boolean reconfigure = true;

        short [] decodedShorts = new short[0];
        int decodedIdx = 0;
        MediaFormat oformat = null;

        MediaExtractor extractor = new MediaExtractor();
        MediaFormat format;
        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        try {
            extractor.setDataSource(Storage.path + File.separator + fileName);

            if (extractor.getTrackCount() > 1) {
                service.handleFileProblem("Multiple tracks in file.");
                return false;
            }

            format = extractor.getTrackFormat(0);
            if (format.getString(MediaFormat.KEY_MIME).equals(MediaFormat.MIMETYPE_AUDIO_MPEG)) {
                extractor.selectTrack(0);
            } else {
                service.handleFileProblem("MIME type doesn't match file extension.");
                return false;
            }

            try {
                decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_MPEG);
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
                        if (decodedIdx + (info.size / 2) >= decodedShorts.length) {
                            decodedShorts = Arrays.copyOf(decodedShorts, decodedIdx + (info.size / 2));
                        }
                        for (int i = 0; i < info.size; i += 2) {
                            decodedShorts[decodedIdx++] = buf.getShort(i);
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

                decoder.stop();
                decoder.release();

                byte[] decodedBytes = Dry.MyShortsToBytes(decodedShorts);

                Log.d("************", "decoded.length: " + decodedShorts.length);
                Log.d("************", "decodedBytes.length: " + decodedBytes.length);

                service.at = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                        (oformat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                        AudioFormat.ENCODING_PCM_16BIT,
                        decodedBytes.length,
                        AudioTrack.MODE_STATIC);
                service.at.write(decodedBytes,0,decodedBytes.length);
                service.at.setPlaybackRate(oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
            } catch (IOException e) {
                service.handleFileProblem("Failed to instantiate MP3 MediaCodec.");
                return false;
            }

            extractor.release();
        } catch (IOException e) {
            service.handleFileProblem("Error: exception thrown when trying to extract data from file " + fileName);
            return false;
        }

        return true;
    }

    //////////////// wav stuff /////////////////

    static boolean loadWav(String fileName, MyService service) {
        byte[] bytes;
        AudioFileInfo info;
        InputStream ins;

        try {
            ins = new FileInputStream(new File(Storage.path, fileName));
            try {
                info = AudioFiles.readWavHeader(ins);
                bytes = AudioFiles.readWavPcm(info, ins);

                if (info.format != 1) {
                    service.handleFileProblem("Audio file not supported. " + "Encoding must be PCM.");
                    return false;
                }
                if (!Dry.arrayContains(SUPPORTED_CHANNELS, info.channels)) {
                    service.handleFileProblem("Audio file not supported. " + "Channels found: " + info.channels + ". Supported numbers of channels: " + Dry.concatCSV(SUPPORTED_CHANNELS) + ".");
                    return false;
                }
                if (!Dry.arrayContains(SUPPORTED_SAMPLE_RATES, info.rate)) {
                    service.handleFileProblem("Audio file not supported. " + "Sample rate found: " + info.rate + ". Supported sample rates: " + Dry.concatCSV(SUPPORTED_SAMPLE_RATES) + ".");
                    return false;
                }
                if (!Dry.arrayContains(SUPPORTED_BIT_DEPTHS, info.depth)) {
                    service.handleFileProblem("Audio file not supported. " + "Bit depth found: " + info.depth + ". Supported bit depths: " + Dry.concatCSV(SUPPORTED_BIT_DEPTHS) + ".");
                    return false;
                }
                if (info.dataSize <= 0) {
                    service.handleFileProblem("Audio file not supported. " + "Audio data missing.");
                    return false;
                }

                service.at = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        info.rate,
                        (info.channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                        AudioFormat.ENCODING_PCM_16BIT,
                        bytes.length,
                        AudioTrack.MODE_STATIC);
                service.at.write(bytes,0,bytes.length);
                service.at.setPlaybackRate(info.rate);

                return true;
            } catch (Exception e) {
                service.handleFileProblem("Error reading file. " + e.getMessage());
                return false;
            }
        } catch (FileNotFoundException e) {
            service.handleFileProblem("File not found. " + e.getMessage());
            return false;
        }
    }

    static AudioFileInfo readWavHeader(InputStream wavStream) throws IOException, DecoderException {

        ByteBuffer buffer = ByteBuffer.allocate(WAV_HEADER_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        wavStream.read(buffer.array(), buffer.arrayOffset(), buffer.capacity());

        buffer.rewind();
        buffer.position(buffer.position() + 20);
        int format = buffer.getShort();
        int channels = buffer.getShort();
        int rate = buffer.getInt();
        buffer.position(buffer.position() + 6);
        int depth = buffer.getShort();
        int dataSize = 0;
        while (buffer.getInt() != 0x61746164) { // "data" marker
            Log.d(TAG, "Skipping non-data chunk");
            int size = buffer.getInt();
            wavStream.skip(size);

            buffer.rewind();
            wavStream.read(buffer.array(), buffer.arrayOffset(), 8);
            buffer.rewind();
        }
        dataSize = buffer.getInt();

        return new AudioFileInfo(format, channels, rate, depth, dataSize);
    }

    static byte[] readWavPcm(AudioFileInfo info, InputStream stream) throws IOException {
        byte[] data = new byte[info.dataSize];
        stream.read(data, 0, data.length);
        return data;
    }
}
