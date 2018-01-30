package peoplesfeelingscode.com.samplemetronomerebuild;

import android.app.ActivityManager;
import android.content.Context;

import java.nio.ByteBuffer;

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

    static byte[] MyShortToByte(short[] buffer) {
        int N = buffer.length;
        short min = 0;
        short max = 0;
        for (int i=0; i<N; i++) {
            if (buffer[i] > max) max = buffer[i];
            if (buffer[i] < min) min = buffer[i];
        }
        short scaling = (short) (1+(max-min)/256); // 1+ ensures we stay within range and guarantee no divide by zero if sequence is pure silence ...

        ByteBuffer byteBuf = ByteBuffer.allocate(N);
        for (int i=0; i<N; i++) {
            byte b = (byte)(buffer[i]/scaling);  /*convert to byte. */
            byteBuf.put(b);
        }
        return byteBuf.array();
    }

    // cleaner, more expensive
//    static byte[] MyShortToByte(short[] buffer) {
//        int N = buffer.length;
//        float f[] = new float[N];
//        float min = 0.0f;
//        float max = 0.0f;
//        for (int i=0; i<N; i++) {
//            f[i] = (float)(buffer[i]);
//            if (f[i] > max) max = f[i];
//            if (f[i] < min) min = f[i];
//        }
//        float scaling = 1.0f+(max-min)/256.0f; // +1 ensures we stay within range and guarantee no divide by zero if sequence is pure silence ...
//
//        ByteBuffer byteBuf = ByteBuffer.allocate(N);
//        for (int i=0; i<N; i++) {
//            byte b = (byte)(f[i]/scaling);  /*convert to byte. */
//            byteBuf.put(b);
//        }
//        return byteBuf.array();
//    }
}
