package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.common.Wind;



public interface WindListener {
    void windDataReceived(Wind wind);
    
    default void windDataReceived(Iterable<Wind> winds) {
        for (final Wind wind : winds) {
            windDataReceived(wind);
        }
    }
    
    void windDataRemoved(Wind wind);

    void windAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage);
}
