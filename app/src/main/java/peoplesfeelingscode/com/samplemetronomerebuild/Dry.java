package peoplesfeelingscode.com.samplemetronomerebuild;

import android.app.ActivityManager;
import android.content.Context;

public class Dry {
    static int ALLOWED_DEVIATION_FROM_START = 100; // millis. has to be greater than ALLOWED_DEVIATION_FOR_TICK
    static int ALLOWED_DEVIATION_FOR_TICK = 100; // millis
    static int MAX_BPM = 1000;
    static int MILLIS_IN_SECOND = 1000;
    static int MILLIS_IN_MINUTE = MILLIS_IN_SECOND * 60;
    static String TAG = "-------*********-------";

    static boolean serviceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    static boolean arrayContains(int[] arr, int val) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == val) return true;
        }

        return false;
    }

    static String concatCSV (int[] values) {
        String str = "";

        for (int i = 0; i < values.length; i++) {
            str += values[i];
            str += i == values.length - 1 ? ", " : "";
        }

        return str;
    }
}
