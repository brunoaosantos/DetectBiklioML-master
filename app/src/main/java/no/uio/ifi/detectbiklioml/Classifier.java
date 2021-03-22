package no.uio.ifi.detectbiklioml;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.ProbabilityDistribution;
import org.jpmml.evaluator.TargetField;
import org.jpmml.model.SerializationUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class holds the raw acceleration and location values and is responsible for calculation the
 * model features and classifying the trip.
 */
class Classifier {

    private static String TAG = "Classifier";
    private List<Float> rawAccelerations;
    private List<Location> rawLocations;
    private Map<String, Float> features;
    @SuppressLint("StaticFieldLeak")
    private static Classifier instance;
    private static String modelName = "randomForest.pmml.ser";
    private static Evaluator evaluator;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static boolean modelLoaded = false;

    private Classifier() {
        loadModelAsync();
        initializeValues();
    }

    static Classifier getInstance() {
        if (instance == null) {
            instance = new Classifier();
        }
        return instance;
    }

    static void setContext(Context ctx) {
        context = ctx;
    }

    void addLocation(Location location) {
        rawLocations.add(location);
    }

    void addAcceleration(float acceleration) {
        rawAccelerations.add(acceleration);
    }

    /**
     * Uses the ML model to evaluate the mode of transport based on the features values.
     */
    int classify() {
        if (!modelLoaded) {
            loadModel();
        }

        calculateFeatures();
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

        for (InputField requiredModelFeature: evaluator.getInputFields()) {
            FieldName fieldName = requiredModelFeature.getName();
            FieldValue fieldValue = requiredModelFeature.prepare(features.get(fieldName.toString()));
            arguments.put(fieldName, fieldValue);
        }

        Map<FieldName, ?> results = evaluator.evaluate(arguments);
        ProbabilityDistribution pd = (ProbabilityDistribution) results.get(evaluator.getTargetFields().get(0).getName());
        int transportModeId = (int) pd.getResult();

        return transportModeId;
    }

    static void loadModelAsync() {
        if (modelLoaded) {
            return;
        }

        // load model asynchronously
        try {
            AsyncTask.execute(() -> {
                try {
                    createEvaluator();
                    modelLoaded = true;
                    Log.d(TAG, "Model loaded successfully");
                } catch (Exception e) {
                    Log.d(TAG, "Failed loading model");
                    e.printStackTrace();
                }
            });
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    float getFeature(String name) {
        if (features.containsKey(name)) {
            return features.get(name);
        }
        return 0f;
    }

    private static void loadModel() {
        if (modelLoaded) {
            return;
        }

        try {
            createEvaluator();
            modelLoaded = true;
            Log.d(TAG, "Model loaded successfully");
        } catch (Exception e) {
            Log.d(TAG, "Failed loading model");
            e.printStackTrace();
        }
    }

    /**
     * Creates the evaluator that will be used in the prediction.
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static void createEvaluator() {
        AssetManager assetManager = context.getAssets();

        // open and load the serialized model
        try(InputStream is = assetManager.open(modelName)){
            PMML pmml = SerializationUtil.deserializePMML(is);

            ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
            ModelEvaluator<?> modelEvaluator = modelEvaluatorFactory.newModelEvaluator(pmml);
            for (TargetField field : modelEvaluator.getTargetFields()) {
              Log.d(TAG, field.getName() + " " + field.getDataType());
            }
            // evaluator self check
            modelEvaluator.verify();

            evaluator = modelEvaluator;
        } catch (Exception e) {
            Log.d(TAG, "Failed loading model");
        }
    }

    // calculate all the features based on the raw values collected
    private void calculateFeatures() {
        // used to calculate accelsBelowFilter feature
        final float accelerationFilter = 0.3f;
        float distance = 0f;
        List<Float> speeds = new ArrayList<>();
        List<Float> accuracies = new ArrayList<>();
        List<Float> gpsTimeDiffs = new ArrayList<>();
        long prevLocationTime = 0;
        long locationTimeDiff = 0;

        if (rawLocations.size() > 0) {
            Location currentLocation = rawLocations.get(0);
            for (Location location : rawLocations) {
                // build list of speeds
                float speed = location.getSpeed();
                speeds.add(speed * 3.6f);

                // calculate GPS time diffs
                if (prevLocationTime > 0) {
                    locationTimeDiff = (location.getTime() - prevLocationTime);
                    gpsTimeDiffs.add((float) locationTimeDiff / (float) 1000);
                }
                prevLocationTime = location.getTime();

                // build list of location accuracies
                float locationAccuracy = location.getAccuracy();
                accuracies.add(locationAccuracy);

                // calculate distance
                distance += currentLocation.distanceTo(location);
                currentLocation = location;
            }
        }

        features.put("distance", distance);

        features.put("avgAccel", Util.getMean(rawAccelerations));
        features.put("minAccel", Util.getMin(rawAccelerations));
        features.put("maxAccel", Util.getMax(rawAccelerations));
        features.put("stdDevAccel", Util.getStandardDeviation(rawAccelerations));

        List<Float> filteredAccelerations = Util.removeValuesBelow(rawAccelerations, accelerationFilter);
        features.put("avgFilteredAccel", Util.getMean(filteredAccelerations));

        filteredAccelerations = Util.removeValuesAbove(rawAccelerations, accelerationFilter);
        features.put("accelsBelowFilter", (float) (filteredAccelerations.size() * 100) / (float) rawAccelerations.size());

        filteredAccelerations = Util.removeValuesNotInInterval(rawAccelerations, 0.3f, 0.6f);
        features.put("accelBetw_03_06", (float) (filteredAccelerations.size() * 100) / (float) rawAccelerations.size());

        filteredAccelerations = Util.removeValuesNotInInterval(rawAccelerations, 0.6f, 1f);
        features.put("accelBetw_06_1", (float) (filteredAccelerations.size() * 100) / (float) rawAccelerations.size());

        filteredAccelerations = Util.removeValuesNotInInterval(rawAccelerations, 1f, 3f);
        features.put("accelBetw_1_3", (float) (filteredAccelerations.size() * 100) / (float) rawAccelerations.size());

        filteredAccelerations = Util.removeValuesNotInInterval(rawAccelerations, 3f, 6f);
        features.put("accelBetw_3_6", (float) (filteredAccelerations.size() * 100) / (float) rawAccelerations.size());

        features.put("avgSpeed", Util.getMean(speeds));
        features.put("minSpeed", Util.getMin(speeds));
        features.put("maxSpeed", Util.getMax(speeds));
        features.put("stdDevSpeed", Util.getStandardDeviation(speeds));

        features.put("avgAcc", Util.getMean(accuracies));
        features.put("minAcc", Util.getMin(accuracies));
        features.put("maxAcc", Util.getMax(accuracies));
        features.put("stdDevAcc", Util.getStandardDeviation(accuracies));

        features.put("gpsTimeMean", Util.getMean(gpsTimeDiffs));
        rawLocations.clear();
        rawAccelerations.clear();
    }

    private void initializeValues()     {
        rawAccelerations = new ArrayList<>();
        rawLocations = new ArrayList<>();
        features = new HashMap<String, Float>() {{
            put("avgAccel", (float) 0);
            put("minAccel", (float) 0);
            put("maxAccel", (float) 0);
            put("stdDevAccel", (float) 0);
            put("avgFilteredAccel", (float) 0); // average of acceleration magnitude after removing the values below acceleration filter
            put("accelsBelowFilter", (float) 0); // percentage of accelerations with values below the acceleration filter
            put("accelBetw_03_06", (float) 0);
            put("accelBetw_06_1", (float) 0);
            put("accelBetw_1_3", (float) 0);
            put("accelBetw_3_6", (float) 0);
            put("accelAbove_6", (float) 0);
            put("avgSpeed", (float) 0);
            put("minSpeed", (float) 0);
            put("maxSpeed", (float) 0);
            put("stdDevSpeed", (float) 0);
            put("avgAcc", (float) 0); // accuracy associated with the precision of the location coordinates
            put("minAcc", (float) 0);
            put("maxAcc", (float) 0);
            put("stdDevAcc", (float) 0);
            put("gpsTimeMean", (float) 0); // mean time between 2 consecutive locations received (units in seconds)
            put("OS", (float) 0);
            put("distance", (float) 0); // in meters
            put("estimatedSpeed", (float) 1);
        }};
    }

}
