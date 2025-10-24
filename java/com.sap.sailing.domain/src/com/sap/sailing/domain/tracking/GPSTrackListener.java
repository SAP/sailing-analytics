package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.shared.tracking.AddResult;

public interface GPSTrackListener<ItemType, FixType extends GPSFix> extends TrackListener {
    
    void gpsFixReceived(FixType fix, ItemType item, boolean firstFixInTrack, AddResult addedOrReplaced);

    void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage);
    
}
