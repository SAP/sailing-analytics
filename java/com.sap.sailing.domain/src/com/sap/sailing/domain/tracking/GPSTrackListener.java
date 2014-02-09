package com.sap.sailing.domain.tracking;

import java.io.Serializable;

public interface GPSTrackListener<ItemType, FixType extends GPSFix> extends Serializable, TrackListener<FixType> {
    void gpsFixReceived(FixType fix, ItemType item);

    void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage);
}
