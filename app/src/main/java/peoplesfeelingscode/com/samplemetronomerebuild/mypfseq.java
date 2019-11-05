package peoplesfeelingscode.com.samplemetronomerebuild;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import peoplesfeelingscode.com.pfseq.PFSeq;

public class mypfseq extends PFSeq {
    @Override
    public Notification getNotification() {
        Notification notification;
        Context context = getApplicationContext();
        Intent intent = new Intent(context, ActivityMain.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, intent, 0);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            String channelId = "the sample id";
            CharSequence name = getString(R.string.notif_channel_name);// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(channelId, name, importance);

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(mChannel);

            notification =
                    new Notification.Builder(context, channelId)
                            .setContentTitle(getText(R.string.notifTitle))
                            .setContentText(getText(R.string.notifText))
                            .setSmallIcon(R.drawable.notif_icon)
                            .setContentIntent(pendingIntent)
                            .setTicker(getText(R.string.notifTicker))
                            .build();
        } else {
            notification =
                    new Notification.Builder(context)
                            .setPriority(Notification.PRIORITY_HIGH)
                            .setContentTitle(getText(R.string.notifTitle))
                            .setContentText(getText(R.string.notifText))
                            .setSmallIcon(R.drawable.notif_icon)
                            .setContentIntent(pendingIntent)
                            .setTicker(getText(R.string.notifTicker))
                            .build();
        }
        return notification;
    }
}
