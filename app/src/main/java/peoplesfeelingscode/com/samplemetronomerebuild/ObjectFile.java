package peoplesfeelingscode.com.samplemetronomerebuild;

import java.io.Serializable;

public class ObjectFile implements Serializable {
    String name;
    boolean selected;
    int pos;

    public ObjectFile(String name) {
        this.name = name;
    }
}
