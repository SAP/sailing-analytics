package com.sap.sailing.domain.racelog.tracking;

import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sse.common.Duration;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.Util.Triple;


/**
 * Store abstraction for persistence of Fixes (e.g. GPSFix).
 */
public interface SensorFixStore {
    
    /**
     * Loads fixes for a device in a given time range in ascending order.
     * 
     * @param consumer
     *            will be called for each loaded fix. Must not be <code>null</code>.
     * @param deviceIdentifier
     *            the device to load the fixes for. Must not be <code>null</code>.
     * @param start
     *            the lower bound of the time range to load. If <code>null</code>, fixes are loaded from
     *            {@link TimePoint#BeginningOfTime}.
     * @param end
     *            the upper bound of the time range to load. If <code>null</code>, fixes are loaded to
     *            {@link TimePoint#EndOfTime}.
     * @param toIsInclusive
     *            true if fixes exactly at the {@code end} bounds of the time range should be loaded, false otherwise.
     *            Fixes exactly on the {@code start} bounds are always loaded.
     */
    <FixT extends Timed> void loadFixes(Consumer<FixT> consumer, DeviceIdentifier deviceIdentifier, TimePoint start, TimePoint end,
            boolean toIsInclusive) throws NoCorrespondingServiceRegisteredException,
    TransformationException;
    
    /**
     * Loads fixes for a device in a given time range in ascending order.
     * 
     * @param consumer will be called for each loaded fix. Must not be <code>null</code>.
     * @param deviceIdentifier the device to load the fixes for. Must not be <code>null</code>.
     * @param start the lower bound of the time range to load. If <code>null</code>, fixes are loaded from {@link TimePoint#BeginningOfTime}.
     * @param end the upper bound of the time range to load. If <code>null</code>, fixes are loaded to {@link TimePoint#EndOfTime}.
     * @param inclusive true if fixes exactly at the bounds of the time range should be loaded, false otherwise.
     * @param progressReporter not allowed to be null, can be used to get reports of the approximate loading progress
     */
    <FixT extends Timed> void loadFixes(Consumer<FixT> consumer, DeviceIdentifier deviceIdentifier, TimePoint start,
            TimePoint end, boolean inclusive, BooleanSupplier isPreemptiveStopped,
            Consumer<Double> progressReporter)
            throws NoCorrespondingServiceRegisteredException,
    TransformationException;

    /**
     * Saves a single fix for the given device and informs all registered listeners about the new fix.
     * 
     * @param device
     *            the device to store the fix for. Must not be <code>null</code>.
     * @param fix
     *            The fix to store. Must not be <code>null</code>.
     */
    <FixT extends Timed> void storeFix(DeviceIdentifier device, FixT fix);

    /**
     * Saves a batch of fixes for the given device and informs all registered listeners about the new fix.
     * 
     * @param device
     *            the device to store the fix for. Must not be <code>null</code>.
     * @param fixes
     *            The fixes to store. Must not be <code>null</code>.
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
     * @return An {@link Iterable} with {@link RegattaAndRaceIdentifier}s in their first component is returned that will
     *         contain races with new maneuvers which were not available at the last time the given device stored a fix
     *         in case the {@code returnManeuverUpdate} parameter was set to {@code true}, and all races with their live
     *         delays to which the fix was mapped in case {@code returnLiveDelay} was set to {@code true}. The
     *         {@link Iterable} returned can be empty but is never {@code null}. It can also contain multiple
     *         identifiers if the device mapping is currently ambiguous.
     */
    <FixT extends Timed> Iterable<Triple<RegattaAndRaceIdentifier, Boolean, Duration>> storeFixes(
            DeviceIdentifier device, Iterable<FixT> fixes, boolean returnManeuverUpdate, boolean returnLiveDelay);

    /**
     * Listeners are notified, whenever a {@link GPSFix} submitted by the {@code device}
     * is stored through the {@link #storeFix(DeviceIdentifier, GPSFix)} method.
     */
    void addListener(FixReceivedListener<? extends Timed> listener, DeviceIdentifier device);

    /**
     * Remove the registrations of the listener for all devices.
     */
    void removeListener(FixReceivedListener<? extends Timed> listener);
    
    /**
     * Remove the registrations of the listener for the given device.
     */
    void removeListener(FixReceivedListener<? extends Timed> listener, DeviceIdentifier device);
    
    TimeRange getTimeRangeCoveredByFixes(DeviceIdentifier device) throws TransformationException,
    NoCorrespondingServiceRegisteredException;
    
    long getNumberOfFixes(DeviceIdentifier device) throws TransformationException, NoCorrespondingServiceRegisteredException;
    
    /**
     * Obtains the fixes that were received last for each of the devices specified. For devices that have not delivered
     * fixes yet, no mapping is created in the resulting map. Note that due to the possibility of out-of-order delivery
     * the fixes returned may not be the fixes with the latest time stamp for that device.
     */
    <FixT extends Timed> Map<DeviceIdentifier, FixT> getFixLastReceived(Iterable<DeviceIdentifier> forDevices)
            throws TransformationException, NoCorrespondingServiceRegisteredException;

    /**
     * Loads the oldest fix for the given device in the specified {@link TimeRange}.
     * 
     * @return true if a fix was loaded, false otherwise
     */
    <FixT extends Timed> boolean loadOldestFix(Consumer<FixT> consumer, DeviceIdentifier device,
            TimeRange timeRangetoLoad)
            throws NoCorrespondingServiceRegisteredException, TransformationException;

    /**
     * Loads the youngest fix for the given device in the specified {@link TimeRange}.
     * 
     * @return true if a fix was loaded, false otherwise
     */
    <FixT extends Timed> boolean loadYoungestFix(Consumer<FixT> consumer, DeviceIdentifier device,
            TimeRange timeRangetoLoad)
            throws NoCorrespondingServiceRegisteredException, TransformationException;
}
