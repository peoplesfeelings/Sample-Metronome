package peoplesfeelingscode.com.samplemetronomerebuild;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by User Name on 6/18/2017.
 */

public class Storage {

    static final double SENSITIVITY_FACTOR = 9.0;

    static final String SHARED_PREF_FILE_NAME = "pf-sm-file";
    static final boolean DEFAULT_SHARED_PREF_BOOL = false;
    static final int DEFAULT_SHARED_PREF_INT = -1;
    static final int DEFAULT_SHARED_PREF_FLOAT = -1;
    static final int DEFAULT_SHARED_PREF_DOUBLE = -1;
    static final String DEFAULT_SHARED_PREF_STRING = "";

    static final String SHARED_PREF_HAS_RUN_KEY = "app has run before";
    static final String SHARED_PREF_FTA_KEY = "full texture angle";
    static final String SHARED_PREF_RATE_KEY = "rate";
    static final String SHARED_PREF_SELECTED_FILE_KEY = "selected file";

    static final String[] SUPPORTED_FILE_EXTENSIONS = { "3gp", "mp4", "m4a", "aac", "flac", "mp3", "mkv", "wav", "ogg"};

    static final double DEFAULT_BPM = 120.0;
    static final int DEFAULT_RATE = 1;

    static File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Documents/Sample_Metronome/");

    //////////////// write stuff /////////////////

    static void makeDirectoryIfNeeded() {
        try {
            if (!path.exists()) {
                if (path.mkdirs()){
                } else {
                    //
                }
            } else {
                //
            }
        } catch (Exception e) {
            throw e;
        }
    }

    //////////////// read stuff /////////////////

    static ArrayList<ObjectFile> getFileList() {
        makeDirectoryIfNeeded();
        File[] fileArray = path.listFiles();
        ArrayList<ObjectFile> fileList = new ArrayList<ObjectFile>();
        for (int i = 0; i < fileArray.length; i++) {
            for (String ext: SUPPORTED_FILE_EXTENSIONS) {
                if (fileArray[i].getName().endsWith(ext)) {
                    fileList.add(new ObjectFile(fileArray[i].getName()));
                }
            }
        }

        Collections.sort(fileList, new Comparator<ObjectFile>() {
            @Override
            public int compare(ObjectFile o1, ObjectFile o2) {
                return o1.name.toLowerCase().compareTo(o2.name.toLowerCase());
            }
        });

        return fileList;
    }

    //////////////// shared pref ////////////////////////

    //    int

    static int getSharedPrefInt(String key, Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPrefs.getInt(key, DEFAULT_SHARED_PREF_INT);
    }

    static void setSharedPrefInt(int value, String key, Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    //    float

    static void setSharedPrefFloat(float value, String key, Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putFloat(key, value);
        editor.commit();
    }

    static float getSharedPrefFloat(String key, Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPrefs.getFloat(key, DEFAULT_SHARED_PREF_FLOAT);
    }

    //    double

    static void setSharedPrefDouble(SharedPreferences.Editor editor, double value, String key, Context context) {
        editor.putLong(key, Double.doubleToRawLongBits(value));
        editor.commit();
    }

    static double getSharedPrefDouble(SharedPreferences sharedPrefs, String key, Context context) {
        return Double.longBitsToDouble(sharedPrefs.getLong(key, Double.doubleToLongBits(DEFAULT_SHARED_PREF_DOUBLE)));
    }

    //    bool

    static boolean getSharedPrefBool(String key, Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPrefs.getBoolean(key, DEFAULT_SHARED_PREF_BOOL);
    }

    static void setSharedPref(boolean b, String key, Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(key, b);
        editor.commit();
    }

    //    string

    static String getSharedPrefString(String key, Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPrefs.getString(key, DEFAULT_SHARED_PREF_STRING);
    }

    static void setSharedPrefString(String s, String key, Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(key, s);
        editor.commit();
    }

    //////////////// bpm /  fta conversion ////////////////////////

    static double bpmToFta (double bpm) {
        return bpm / SENSITIVITY_FACTOR;
    }

    static double ftaToBpm (double fta) {
        return fta * SENSITIVITY_FACTOR;
    }
}
