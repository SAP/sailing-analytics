package com.sap.sailing.domain.racelog.tracking;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util.Triple;

/**
 * Listener to be informed about new fixes by {@link SensorFixStore}.
 *
 * @param <FixT>
 *            the type of fixes this listener can consume.
 */
public interface FixReceivedListener<FixT extends Timed> {
    /**
     * 
     * @param device
     *            the device that recorded the fix. Cannot be <code>null</code>.
     * @param fix
     *            The fix that was stored. Cannot be <code>null</code>.
     * @param returnManeuverUpdate
     *            if {@code true}, all listeners to which this fix is forwarded shall check whether the fix feeds into a
     *            competitor's track in the scope of a race where for that competitor the maneuver list has changed
     *            since the last call of this type; if so, the race identifier will be part of the result, with the
     *            {@link Boolean} component being {@code true} for that race. Otherwise, the {@link Boolean} component
     *            is {@code false} or the race is not listed in the result.
     * @param returnLiveDelay
     *            if {@code true} then all listeners to which the fix is forwarded shall check to which races the fix
     *            maps and report the live delay for all those races as the third component of the resulting
     *            {@link Triple}s.
     * @return An {@link Iterable} with {@link RegattaAndRaceIdentifier}s is returned that will contain races with new
     *         maneuvers which were not available at the last time the given device stored a fix. The {@link Iterable}
     *         returned can be empty but is never {@code null}. It can also contain multiple identifiers if the device
     *         mapping is currently ambiguous.
     */
    Iterable<Triple<RegattaAndRaceIdentifier, Boolean, Duration>> fixReceived(DeviceIdentifier device, FixT fix,
            boolean returnManeuverChanges, boolean returnLiveDelay);
}
