package peoplesfeelingscode.com.samplemetronomerebuild;

public class AudioFileInfo {

    int format; // 1 means Linear PCM
    int channels; // 1, 2, etc
    int rate;
    int depth; // 16, 24, etc
    int dataSize;

    public AudioFileInfo(int format, int channels, int rate, int depth, int dataSize ) {
        this.format = format;
        this.channels = channels;
        this.rate = rate;
        this.depth = depth;
        this.dataSize = dataSize;
    }

    public AudioFileInfo(int format, int channels, int rate, int depth) {
        this.format = format;
        this.channels = channels;
        this.rate = rate;
        this.depth = depth;
        dataSize = -1;
    }
}
