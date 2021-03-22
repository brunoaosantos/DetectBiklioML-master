package no.uio.ifi.detectbiklioml;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.TimeUnit;


/**
 * Class responsible for managing and collecting trip data.
 */
class TripManager {

    private static String TAG = "TripManager";
    private SensorManager sensorManager;
    private AccelerationListener accelerationListener;
    private Sensor accelerometer;
    private FusedLocationProviderClient locationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Trip currentTrip = null;
    private static final long ACCEL_SAMPLING_PERIOD = TimeUnit.SECONDS.toMicros(1);
    private static final long GPS_SAMPLING_INTERVAL = TimeUnit.SECONDS.toMillis(10); // 10 seconds in MILIseconds
    private static boolean tripInProgress = false;

    TripManager(Context context) {
        // setup accelerometer
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        accelerationListener = new AccelerationListener();

        // setup location params
        locationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(GPS_SAMPLING_INTERVAL);
        locationRequest.setMaxWaitTime(GPS_SAMPLING_INTERVAL);
        // callback to receive location updates
        locationCallback = new LocationListener();
    }

    /**
     * Starts recording a trip
     */
    void startTrip() {
        sensorManager.registerListener(accelerationListener, accelerometer, (int) ACCEL_SAMPLING_PERIOD, (int) ACCEL_SAMPLING_PERIOD);
        Log.d(TAG, "Started accelerometer");
        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d(TAG, "Started GPS");
        tripInProgress = true;
        currentTrip = new Trip();
        currentTrip.start();
    }

    /**
     * Stops recording a trip
     */
    void stopTrip() {
        // stop sensors
        sensorManager.unregisterListener(accelerationListener, accelerometer);
        Log.d(TAG, "Stopped accelerometer");
        locationProviderClient.removeLocationUpdates(locationCallback);
        Log.d(TAG, "Stopped GPS");

        // stop trip
        if (tripInProgress && currentTrip != null) {
            tripInProgress = false;
            currentTrip.finish();

            // classify trip
            long startTime = System.nanoTime();
            int transportModeId = Classifier.getInstance().classify();
            long endTime = System.nanoTime();
            long classificationTime = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
            Log.d(TAG, "Classification took " + classificationTime + " ms");

            if (Util.isBike(transportModeId)) {
                currentTrip.setBike();
            }
            currentTrip.setModeId(transportModeId);
            currentTrip.setTimeToClassify(classificationTime);
            currentTrip.setDistance(Classifier.getInstance().getFeature("distance"));
            // save trip
            TripRepository.save(currentTrip);
            currentTrip = null;
        }
    }

}
