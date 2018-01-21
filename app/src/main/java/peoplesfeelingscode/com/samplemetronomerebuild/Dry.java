package peoplesfeelingscode.com.samplemetronomerebuild;

import android.app.ActivityManager;
import android.content.Context;

public class Dry {
    static int MILLIS_IN_SECOND = 1000;
    static int MILLIS_IN_MINUTE = MILLIS_IN_SECOND * 60;

    static boolean serviceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
