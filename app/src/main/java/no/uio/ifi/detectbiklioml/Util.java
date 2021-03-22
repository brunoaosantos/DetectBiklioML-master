package no.uio.ifi.detectbiklioml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class
 */
class Util {

    private static final Integer[] BIKE_MODE_IDS = {1, 16, 17, 35};
    static final String firebaseCollectionName = "trips";

    static boolean isBike(int transportModeId) {
        return Arrays.asList(BIKE_MODE_IDS).contains(transportModeId);
    }

    // calculates the mean value of a list of floats.
    static float getMean(List<Float>  values) {
        if (values.isEmpty()) return 0f;

        float sum = 0;
        for (float value : values) {
            sum += value;
        }

        return sum / (float) values.size();
    }

    // calculates the standard deviation of a list of floats.
    static float getStandardDeviation(List<Float> values) {
        if (values.isEmpty()) return 0f;

        float mean = getMean(values);
        float squareSum = 0;
        for (float value : values) {
            squareSum += Math.pow(value - mean, 2);
        }

        return (float) Math.sqrt((squareSum / (float) values.size()));
    }

    // calculates the minimum value of a list of floats.
    static float getMin(List<Float> values) {
        if (values.isEmpty()) return 0f;

        float minValue = Float.MAX_VALUE;
        for (float value : values) {
            if (value < minValue) {
                minValue = value;
            }
        }

        return minValue;
    }

    // calculates the maximum value of a list of floats.
    static float getMax(List<Float> values) {
        if (values.isEmpty()) return 0f;

        float maxValue = Float.MIN_VALUE;
        for (float value : values) {
            if (value > maxValue) {
                maxValue = value;
            }
        }

        return maxValue;
    }

    // removes values above limit and returns a copy of the values list
    static List<Float> removeValuesAbove(List<Float> values, float limit) {
        List<Float> copy = new ArrayList<>(values);
        for (float value : values) {
            if (value > limit) {
                copy.remove(copy.indexOf(value));
            }
        }

        return copy;
    }

    // removes values below limit and returns a copy of the values list
    static List<Float> removeValuesBelow(List<Float> values, float limit) {
        List<Float> copy = new ArrayList<>(values);
        for (float value : values) {
            if (value < limit) {
                copy.remove(copy.indexOf(value));
            }
        }

        return copy;
    }

    // removes values outside a lower and upper limit nd returns a copy of the values list
    static List<Float> removeValuesNotInInterval(List<Float> values, float lowerLimit, float upperLimit) {
        List<Float> copy = new ArrayList<>(values);
        for (float value : values) {
            if (value < lowerLimit || value > upperLimit) {
                copy.remove(copy.indexOf(value));
            }
        }

        return copy;
    }
}
