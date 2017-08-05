package peoplesfeelingscode.com.samplemetronomerebuild;

import java.io.Serializable;

/**
 * Created by User Name on 10/3/2016.
 */

public class ObjectFile implements Serializable {
    String name;
    boolean selected;
    int pos;

    public ObjectFile(String name) {
        this.name = name;
    }
}
