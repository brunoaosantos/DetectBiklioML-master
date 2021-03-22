package no.uio.ifi.detectbiklioml;

import java.util.Date;

/**
 * Class representing a trip.
 */
class Trip {

    private String id;
    private Date startDate;
    private Date endDate;
    private String TAG = "Trip";
    private boolean bike;
    private boolean inProgress = false;
    private long timeToClassify;
    private float distance;
    private int modeId;

    void start() {
        this.startDate = new Date();
        inProgress = true;
    }

    void finish() {
        this.endDate = new Date();
        inProgress = false;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setBike() {
        bike = true;
    }

    public boolean isBike() {
        return bike;
    }

    public boolean isInProgress() {
        return inProgress || endDate == null;
    }

    public long getTimeToClassify() {
        return timeToClassify;
    }

    public void setTimeToClassify(long timeToClassify) {
        this.timeToClassify = timeToClassify;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    // TODO: remove, for debugging purposes only
    public int getModeId() {
        return modeId;
    }

    public void setModeId(int modeId) {
        this.modeId = modeId;
    }
}
