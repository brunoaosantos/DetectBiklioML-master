package no.uio.ifi.detectbiklioml;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
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

        // Asks for writing permission
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

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
            sendBroadcast(new Intent("TRIP_STARTED"));
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

    /**
     * Asks for writing permission
     */
    public void checkPermission(String permission){
        String[] PERMISSIONS_STORAGE = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        int check = ContextCompat.checkSelfPermission(this, permission);

        if (check != PackageManager.PERMISSION_GRANTED) {
            //if the app is not allowed to write in the external storage it will ask the user to give permission
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    1);
        }
    }

}

