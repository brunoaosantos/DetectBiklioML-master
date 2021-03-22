package no.uio.ifi.detectbiklioml;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * Receives acceleration readings from the smart phone sensor and send them to the classifier.
 */
public class AccelerationListener implements SensorEventListener {

    private static String TAG = "AccelerationListener";
    private long lastEventTimestamp = System.nanoTime();
    private static long ONE_SECOND_NANOS = TimeUnit.SECONDS.toNanos(1);

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        long timestampDifference = Math.abs(sensorEvent.timestamp - lastEventTimestamp);

        // It might happen to get readings sooner than 1s after the last one.
        // Update lastEventTimestamp with the current event timestamp if the previous one happened more than a second ago.
        if (timestampDifference >= ONE_SECOND_NANOS) {
            lastEventTimestamp = sensorEvent.timestamp;
        } else {
            // Just return if the last event was less than a second ago.
            return;
        }

        float accelerationMagnitude = calculateAccelerationMagnitude(sensorEvent);

        // save acceleration on classifier
        Classifier.getInstance().addAcceleration(accelerationMagnitude);

        Log.d(TAG, "Got acceleration update:" + accelerationMagnitude + " m/s^2");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Required by interface
    }

    // calculate acceleration magnitude from SensorEvent
    private float calculateAccelerationMagnitude(SensorEvent sensorEvent) {
        return (float) Math.sqrt(Math.pow(sensorEvent.values[0], 2) +
                       Math.pow(sensorEvent.values[1], 2) +
                       Math.pow(sensorEvent.values[2], 2));
    }
}
