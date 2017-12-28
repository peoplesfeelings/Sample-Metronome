package peoplesfeelingscode.com.samplemetronomerebuild;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service {
    IBinder mBinder;
    boolean looping;

    @Override
    public void onCreate() {
        super.onCreate();

        mBinder = new MyBinder();
        looping = false;

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

    class MyBinder extends Binder {
        MyService getService() {
            Log.d("**************", "myservice getservice");
            return MyService.this;
        }
    }
}
