package no.uio.ifi.detectbiklioml;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

/**
 * Receives location readings from the smart phone and send them to the classifier.
 */
public class LocationListener extends LocationCallback {

    private static String TAG = "LocationListener";

    @Override
    public void onLocationResult(LocationResult locationResult) {
        for (Location location : locationResult.getLocations()) {
            Log.d(TAG, "Got location update:" + location);
            // save data on classifier
            Classifier.getInstance().addLocation(location);
        }
    }

}
