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
import java.util.Arrays;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import static android.content.ContentValues.TAG;

public class AudioFiles {
    static final int WAV_HEADER_SIZE = 44;
    static final long TIMEOUTUS = 3000;
    static final int[] SUPPORTED_BIT_DEPTHS = { 16 };
    static final int[] SUPPORTED_CHANNELS = { 1, 2 };
    static final int[] SUPPORTED_SAMPLE_RATES = { 44100, 48000 };

    //////////////// mp3 stuff /////////////////

    static boolean decodeWithJlayer(String fileName, MyService service) {
        Log.d(Dry.TAG, "decoding with jlayer");
        Decoder decoder;
        Bitstream bitStream;
        SampleBuffer sampleBuffer = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Header h;

        try {
            bitStream = new Bitstream(new FileInputStream(Storage.path + File.separator + fileName));
        } catch (FileNotFoundException e) {
            //
            Log.d(Dry.TAG, "Exception initializing bitstream");
            return false;
        }

        while (true) {
            decoder = new Decoder();
            try {
                h = bitStream.readFrame();
            } catch (Exception e) {
                //
                return false;
            }

            if (h != null) {
                try {
                    sampleBuffer = (SampleBuffer) decoder.decodeFrame(h, bitStream); //returns the next 2304 samples
                } catch (javazoom.jl.decoder.DecoderException e) {
                    //
                    return false;
                }
            } else {
//                bitStream.closeFrame();
                break;
            }
            bitStream.closeFrame();

            if (sampleBuffer != null) {
                try {
                    outputStream.write(Dry.shortsToBytes(sampleBuffer.getBuffer()));
                } catch (IOException e) {
                    //
                    Log.d(Dry.TAG, "Exception writing to output stream");
                    return false;
                }
            }
        }

        byte[] byteArray = outputStream.toByteArray();

        Log.d(Dry.TAG, "byte array size: " + byteArray.length);

        service.at = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                decoder.getOutputFrequency(),
                (decoder.getOutputChannels() == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                AudioFormat.ENCODING_PCM_16BIT,
                byteArray.length,
                AudioTrack.MODE_STATIC);
        service.at.write(byteArray,0,byteArray.length);
        service.at.setPlaybackRate(decoder.getOutputFrequency());

        return true;
    }

    static boolean loadMp3(String fileName, MyService service) {
        MediaCodec decoder;

//        boolean reconfigure = true;

        short [] decodedShorts = new short[0];
        int decodedIndex = 0;
        MediaFormat outputformat = null;

        MediaExtractor extractor = new MediaExtractor();
        MediaFormat sourceFormat;
        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        try {
            extractor.setDataSource(Storage.path + File.separator + fileName);
        } catch (IOException e) {
            service.handleFileProblem("Error: exception thrown when trying to extract data from file " + fileName);
            return false;
        }

        if (extractor.getTrackCount() > 1) {
            service.handleFileProblem("Multiple tracks in file.");
            return false;
        }

        sourceFormat = extractor.getTrackFormat(0);
        Log.d(Dry.TAG, "source format tostring: " + sourceFormat.toString());

        if (sourceFormat.getString(MediaFormat.KEY_MIME).equals(MediaFormat.MIMETYPE_AUDIO_MPEG)) {
            extractor.selectTrack(0);
        } else {
            service.handleFileProblem("MIME type doesn't match file extension.");
            return false;
        }

        try {
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_MPEG);
        } catch (IOException e) {
            service.handleFileProblem("Failed to instantiate MP3 MediaCodec.");
            return false;
        }

        decoder.configure(sourceFormat, null, null, 0);
        decoder.start();
        codecInputBuffers = decoder.getInputBuffers();
        codecOutputBuffers = decoder.getOutputBuffers();
        Log.d(Dry.TAG, "codecInputBuffers.length: " + codecInputBuffers.length);

        MediaCodec.BufferInfo outputBufferInfo = new MediaCodec.BufferInfo();
        boolean inputEOS = false;
        boolean outputEOS = false;

        while (!outputEOS) {
            if (!inputEOS) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUTUS);
                if (inputBufIndex >= 0) {
                    int sampleSize = extractor.readSampleData(codecInputBuffers[inputBufIndex], 0 );
                    Log.d(Dry.TAG, "sampleSize: " + sampleSize);
                    if (sampleSize < 0) {
                        inputEOS = true;
                        Log.d(Dry.TAG, "input eos true");
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
            Log.d(Dry.TAG, "outputBufferInfo.size: " + outputBufferInfo.size);

            if (outputBufferIndex >= 0) {
                if (decodedIndex + (outputBufferInfo.size / 2) >= decodedShorts.length) {
                    decodedShorts = Arrays.copyOf(decodedShorts, decodedIndex + (outputBufferInfo.size / 2));
                }
                for (int i = 0; i < outputBufferInfo.size; i += 2) {
                    decodedShorts[decodedIndex++] = codecOutputBuffers[outputBufferIndex].getShort(i);
                }
                decoder.releaseOutputBuffer(outputBufferIndex, false);
                if ((outputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputEOS = true;
                }
                outputformat = decoder.getOutputFormat();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = decoder.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                outputformat = decoder.getOutputFormat();
            }
        }

        Log.d(Dry.TAG, "output format after loop: " + outputformat.toString());

        decoder.stop();
        decoder.release();

        byte[] decodedBytes = Dry.shortsToBytes(decodedShorts);

        Log.d(Dry.TAG, "decodedShorts.length: " + decodedShorts.length);
        Log.d(Dry.TAG, "decodedBytes.length: " + decodedBytes.length);

        service.at = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                outputformat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                (outputformat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                AudioFormat.ENCODING_PCM_16BIT,
                decodedBytes.length,
                AudioTrack.MODE_STATIC);
        service.at.write(decodedBytes,0,decodedBytes.length);
        service.at.setPlaybackRate(outputformat.getInteger(MediaFormat.KEY_SAMPLE_RATE));

        extractor.release();

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
