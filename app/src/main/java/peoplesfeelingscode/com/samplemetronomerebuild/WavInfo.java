package peoplesfeelingscode.com.samplemetronomerebuild;

public class WavInfo {

    int format; // 1 means Linear PCM
    int channels; // 1, 2, etc
    int rate;
    int depth; // 16, 24, etc
    int dataSize;
    String chunkId; // RIFF, RIFX

    public WavInfo(String chunkId, int format, int channels, int rate, int depth, int dataSize ) {
        this.chunkId = chunkId;
        this.format = format;
        this.channels = channels;
        this.rate = rate;
        this.depth = depth;
        this.dataSize = dataSize;
    }
}
