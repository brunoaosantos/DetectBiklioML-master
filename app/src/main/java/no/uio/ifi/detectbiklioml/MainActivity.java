package no.uio.ifi.detectbiklioml;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    private static Bundle bundle = new Bundle();
    private Intent backgroundServiceIntent;
    private List<Trip> tripsList = new ArrayList<>();
    private TripAdapter mAdapter;
    public ToggleButton toggleBtnTrip;
    public RecyclerView recyclerView;
    public static final String CHANNEL_ID = "MainActivityNotificationChannel";

    private int samplingInterval = 5000;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);

        toggleBtnTrip = findViewById(R.id.tglBtnTrip);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setHasFixedSize(true);

        createNotificationChannel();

        mAdapter = new TripAdapter(tripsList);
        recyclerView.setAdapter(mAdapter);

        backgroundServiceIntent = new Intent(this, TripService.class);

        TripRepository.fetch(this);

        Classifier.loadModelAsync();
        Classifier.setContext(this);

        // Get required permissions
        try {
            PackageInfo info = getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            ArrayList<String> permissionsToRequest = new ArrayList<>();
            if (info.requestedPermissions != null) {
                for (String permission : info.requestedPermissions) {
                    // check if we need to ask for these permissions
                    if (ContextCompat.checkSelfPermission(this, permission)
                            != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(permission);
                    }
                }
            }

            // request permissions
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(new String[0]), 32);
            } else {
                // all permissions have been already granted.
                startService();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        bundle.putBoolean("ToggleButtonState", toggleBtnTrip.isChecked());
    }

    @Override
    public void onResume() {
        super.onResume();
        toggleBtnTrip.setChecked(bundle.getBoolean("ToggleButtonState", false));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int grantResult : grantResults) {
            if (grantResult != PermissionChecker.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "Missing permission grant");
                finishAndRemoveTask();
                return;
            }
        }
        startService();
    }

    /**
     * Updates the recycler view with a list of Trips
     *
     * @param newTripsList list of Trip objects
     */
    public void refreshTripList(List<Trip> newTripsList) {
        tripsList = newTripsList;
        mAdapter.setTripsList(tripsList);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Click handler for "Start trip" toggle button
     * @param view
     */
    public void toggleStartTrip(View view) {
        if (((ToggleButton) view).isChecked()) {
            showNotification("Trip started!");
            Intent intent = new Intent("TRIP_STARTED");
            intent.putExtra("samplingInterval", samplingInterval);
            sendBroadcast(intent);
            updateServiceNotification("A trip is being recorded");
        } else {
            sendBroadcast(new Intent("TRIP_ENDED"));
            showNotification("Trip ended!");
            updateServiceNotification("Service running");
            TripRepository.fetch(this);
        }
    }

    private void startService() {
        Log.d(TAG, "Starting trip service...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(backgroundServiceIntent);
        } else {
            startService(backgroundServiceIntent);
        }
    }

    /**
     * Shows a notification
     */
    private void showNotification(String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bike)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }

    /**
     * Updates an existing notification
     * @param content
     */
    private void updateServiceNotification(String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, TripService.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bike)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(TripService.NOTIFICATION_ID, builder.build());
    }

    /**
     * Creates the notification channel used by the app
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "DetectBiklioMLNotificationChannel", importance);
            channel.setDescription("DetectBiklioML Notification Channel");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    public void updateSamplingInterval(View view) {
        if (!toggleBtnTrip.isChecked()) {
            Button samplingButton = (Button)findViewById(R.id.sampling);
            if (samplingInterval == 5000) {
                samplingInterval = 10000;
                samplingButton.setText("10");
            }
            else if (samplingInterval == 10000) {
                samplingInterval = 30000;
                samplingButton.setText("30");
            }
            else if (samplingInterval == 30000) {
                samplingInterval = 60000;
                samplingButton.setText("60");
            }
            else {
                samplingInterval = 5000;
                samplingButton.setText("5");
            }
        }
    }
}

