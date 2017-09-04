/*

Precise Sample Metronome
Copyright (C) 2017 People's Feelings

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

*/

package peoplesfeelingscode.com.samplemetronomerebuild;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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

    static File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Documents/Precise_Sample_Metronome/");

    static boolean fileNeedsToBeLoaded;

    static Pair[] samplePack = {
            new Pair(R.raw.guitar_hit_1, "guitar_hit_1.flac"),
            new Pair(R.raw.guitar_hit_2, "guitar_hit_2.flac"),
            new Pair(R.raw.guitar_hit_3, "guitar_hit_3.flac"),
            new Pair(R.raw.guitar_hit_4, "guitar_hit_4.flac"),
            new Pair(R.raw.guitar_hit_5, "guitar_hit_5.flac"),
            new Pair(R.raw.guitar_hit_6, "guitar_hit_6.flac"),
            new Pair(R.raw.guitar_hit_7, "guitar_hit_7.flac"),
    };

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

    //////////////// read/write stuff /////////////////

    static void writeSamplePack(Activity activity) {

        for (int i = 0; i < samplePack.length; i++) {
            InputStream ins = activity.getResources().openRawResource((int) samplePack[i].first);
            ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
            int size = 0;

            byte[] buffer = new byte[1024];
            try {
                while((size=ins.read(buffer,0,1024))>=0){
                    outputStream.write(buffer,0,size);
                }
                ins.close();
            } catch (Exception e) {
                Log.d("***********", "error");
            }
            buffer=outputStream.toByteArray();
            try {
                FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Documents/Precise_Sample_Metronome/", (String) samplePack[i].second));
                fos.write(buffer);
                fos.close();
            } catch (Exception e) {
                Log.d("***********", "error2");
            }
        }
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
