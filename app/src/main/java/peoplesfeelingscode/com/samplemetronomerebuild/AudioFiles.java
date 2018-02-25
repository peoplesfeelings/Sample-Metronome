package peoplesfeelingscode.com.samplemetronomerebuild;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.content.ContentValues.TAG;

public class AudioFiles {
    static final int WAV_HEADER_SIZE = 44;
    static final long TIMEOUTUS = 3000;
    static final int[] SUPPORTED_BIT_DEPTHS = { 16 };
    static final int[] SUPPORTED_CHANNELS = { 1, 2 };
    static final int[] SUPPORTED_SAMPLE_RATES = { 44100, 48000 };

    //////////////// flac stuff /////////////////

    static boolean loadFlac(String fileName, MyService service) {
        MediaCodec decoder;
        MediaFormat outputformat = null;
        MediaExtractor extractor = new MediaExtractor();
        MediaFormat sourceFormat;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            extractor.setDataSource(Storage.path + File.separator + fileName);
        } catch (IOException e) {
            service.handleFileProblem("Error: exception thrown when trying to extract data from file " + fileName);
            return false;
        }

        sourceFormat = extractor.getTrackFormat(0);
        extractor.selectTrack(0);

        try {
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_FLAC);
        } catch (IOException e) {
            service.handleFileProblem("Failed to instantiate FLAC MediaCodec.");
            return false;
        }

        decoder.configure(sourceFormat, null, null, 0);
        decoder.start();

        MediaCodec.BufferInfo outputBufferInfo = new MediaCodec.BufferInfo();
        boolean inputEOS = false;
        boolean outputEOS = false;

        while (!outputEOS) {
            if (!inputEOS) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUTUS);
                if (inputBufIndex >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufIndex);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0 );
                    if (sampleSize < 0) {
                        inputEOS = true;
                        decoder.queueInputBuffer(
                                inputBufIndex,
                                0 ,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(
                                inputBufIndex,
                                0 ,
                                sampleSize,
                                presentationTimeUs,
                                0);
                        extractor.advance();
                    }
                }
            }

            int outputBufferIndex = decoder.dequeueOutputBuffer(outputBufferInfo, TIMEOUTUS);

            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = decoder.getOutputBuffer(outputBufferIndex);

                for (int i = 0; i < outputBufferInfo.size; i++) {
                    byteArrayOutputStream.write(outputBuffer.get(i));
                }

                decoder.releaseOutputBuffer(outputBufferIndex, false);

                if ((outputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputEOS = true;
                }

                outputformat = decoder.getOutputFormat();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                outputformat = decoder.getOutputFormat();
            }
        }

        extractor.release();
        decoder.stop();
        decoder.release();

        byte[] decodedBytes = byteArrayOutputStream.toByteArray();

        service.at = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                outputformat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                (outputformat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                AudioFormat.ENCODING_PCM_16BIT,
                decodedBytes.length,
                AudioTrack.MODE_STATIC);
        service.at.write(decodedBytes,0,decodedBytes.length);
        service.at.setPlaybackRate(outputformat.getInteger(MediaFormat.KEY_SAMPLE_RATE));

        return true;
    }

    //////////////// mp3 stuff /////////////////

    static boolean loadMp3(String fileName, MyService service) {
        MediaCodec decoder;
        MediaFormat outputformat = null;
        MediaExtractor extractor = new MediaExtractor();
        MediaFormat sourceFormat;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            extractor.setDataSource(Storage.path + File.separator + fileName);
        } catch (IOException e) {
            service.handleFileProblem("Error: exception thrown when trying to extract data from file " + fileName);
            return false;
        }

        sourceFormat = extractor.getTrackFormat(0);
        extractor.selectTrack(0);

        try {
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_MPEG);
        } catch (IOException e) {
            service.handleFileProblem("Failed to instantiate MP3 MediaCodec.");
            return false;
        }

        decoder.configure(sourceFormat, null, null, 0);
        decoder.start();

        MediaCodec.BufferInfo outputBufferInfo = new MediaCodec.BufferInfo();
        boolean inputEOS = false;
        boolean outputEOS = false;

        while (!outputEOS) {
            if (!inputEOS) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUTUS);
                if (inputBufIndex >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufIndex);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0 );
                    if (sampleSize < 0) {
                        inputEOS = true;
                        decoder.queueInputBuffer(
                                inputBufIndex,
                                0 ,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(
                                inputBufIndex,
                                0 ,
                                sampleSize,
                                presentationTimeUs,
                                0);
                        extractor.advance();
                    }
                }
            }

            int outputBufferIndex = decoder.dequeueOutputBuffer(outputBufferInfo, TIMEOUTUS);

            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = decoder.getOutputBuffer(outputBufferIndex);

                for (int i = 0; i < outputBufferInfo.size; i++) {
                    byteArrayOutputStream.write(outputBuffer.get(i));
                }

                decoder.releaseOutputBuffer(outputBufferIndex, false);

                if ((outputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputEOS = true;
                }

                outputformat = decoder.getOutputFormat();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                outputformat = decoder.getOutputFormat();
            }
        }

        extractor.release();
        decoder.stop();
        decoder.release();

        byte[] decodedBytes = byteArrayOutputStream.toByteArray();

        service.at = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                outputformat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                (outputformat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                AudioFormat.ENCODING_PCM_16BIT,
                decodedBytes.length,
                AudioTrack.MODE_STATIC);
        service.at.write(decodedBytes,0,decodedBytes.length);
        service.at.setPlaybackRate(outputformat.getInteger(MediaFormat.KEY_SAMPLE_RATE));

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
