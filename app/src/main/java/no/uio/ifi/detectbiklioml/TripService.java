package no.uio.ifi.detectbiklioml;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Objects;

/**
 * Service to keep the application running in background while a trip is being recorded.
 */
public class TripService extends Service {

    public static final int NOTIFICATION_ID = 200;
    public static final String CHANNEL_ID = "TripServiceNotificationChannel";
    private static final String TAG = "TripService";
    private static boolean serviceStarted = false;
    private TripManager tripManager;
    private BroadcastReceiver tripBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "TRIP_STARTED":
                        tripManager.startTrip();
                        break;
                    case "TRIP_ENDED":
                        tripManager.stopTrip();
                        break;
                }
            }
        };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter tripUpdatesIntentFilter = new IntentFilter();
        tripUpdatesIntentFilter.addAction("TRIP_STARTED");
        tripUpdatesIntentFilter.addAction("TRIP_ENDED");

        registerReceiver(tripBroadcastReceiver, tripUpdatesIntentFilter);
        Log.i(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        if (!serviceStarted) {
            Notification notification = createServiceNotification();
            tripManager = new TripManager(this);
            startForeground(NOTIFICATION_ID, notification);
        }
        serviceStarted = true;

        return START_STICKY;
    }

    /**
     * Creates service notification (required by Android for foreground services)
     */
    private Notification createServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationChannel serviceChannel = new NotificationChannel(
                            CHANNEL_ID,
                            "Foreground Service Channel",
                            NotificationManager.IMPORTANCE_DEFAULT
                            );

                    Objects.requireNonNull(getSystemService(NotificationManager.class))
                            .createNotificationChannel(serviceChannel);
        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service running")
                .setSmallIcon(R.drawable.ic_bike)
                .setContentIntent(pendingIntent)
                .build();
    }

}
