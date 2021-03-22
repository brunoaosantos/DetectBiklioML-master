package no.uio.ifi.detectbiklioml;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Objects;

class TripRepository {

   private static FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build();

    // fetches trips from firestore and updates UI.
    static void fetch(final Context context) {
        final ArrayList<Trip> tripList = new ArrayList<>();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(settings);

        // gets trips from firebase ordered descending by startDate
        db
            .collection(Util.firebaseCollectionName)
            .orderBy("startDate", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                        Trip trip = document.toObject(Trip.class);
                        trip.setId(document.getId());
                        if (!trip.isInProgress()) {
                            tripList.add(trip);
                        }
                    }
                    ((MainActivity) context).refreshTripList(tripList);
                } else {
                    Toast.makeText(context,
                            "Error getting trips", Toast.LENGTH_LONG).show();
                }
            });
    }

    /**
     * Saves a trip on Firebase Firestore
     * @param trip
     */
    static void save(Trip trip) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(settings);
        DocumentReference documentReference = db.collection(Util.firebaseCollectionName).document();
        documentReference.set(trip);
    }

    /**
     * Deletes a trip from Firebase Firestore
     */
    static void delete(String tripId) {
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseFirestore.collection(Util.firebaseCollectionName).document(tripId).delete();
    }


}
