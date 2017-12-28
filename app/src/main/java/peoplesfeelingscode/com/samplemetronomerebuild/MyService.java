package peoplesfeelingscode.com.samplemetronomerebuild;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service {
    final int ONGOING_NOTIFICATION_ID = 4345;

    IBinder mBinder;
    boolean loopRunning;

    Notification notification;

    @Override
    public void onCreate() {
        super.onCreate();

        mBinder = new MyBinder();
        loopRunning = false;

        setUpForeground();

        Log.d("**************", "service oncreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("**************", "service onbind");

        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d("**************", "service ondestroy");
    }

    void start() {
        startForeground(ONGOING_NOTIFICATION_ID, notification);
        loopRunning = true;

        Log.d("*************", "service - start");
    }

    void stop() {
        stopForeground(true);
        loopRunning = false;

        Log.d("*************", "service - stop");
    }

    void setUpForeground() {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, ActivityMain.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, intent, 0);

        notification =
                new Notification.Builder(context)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentTitle(getText(R.string.notifTitle))
                        .setContentText(getText(R.string.notifText))
                        .setSmallIcon(R.drawable.notif_ic_18)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.notifTicker))
                        .build();
    }

    class MyBinder extends Binder {
        MyService getService() {
            return MyService.this;
        }
    }
}
