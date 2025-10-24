package com.sap.sailing.domain.tracking;

import java.io.Serializable;

import com.sap.sailing.domain.common.tracking.BravoExtendedFix;
import com.sap.sailing.domain.common.tracking.BravoFix;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.WithID;

/**
 * Specific {@link SensorFixTrack} for {@link BravoFix}es.
 *
 * @param <ItemType> the type of item mapped by this track
 */
public interface BravoFixTrack<ItemType extends WithID & Serializable> extends SensorFixTrack<ItemType, BravoFix> {
    public static final String TRACK_NAME = "BravoFixTrack";
    
    /**
     * Calculates the ride height for a specific {@link TimePoint}
     * 
     * @param timePoint
     *            the {@link TimePoint} to get the ride height for. Must not be <code>null</code>.
     * @return the ride height for the given {@link TimePoint} or <code>null</code> if the {@link TimePoint} is not in the range
     *         where we have fixes for.
     */
    Distance getRideHeight(TimePoint timePoint);

    /**
     * Calculates the heel for a specific {@link TimePoint}
     * 
     * @param timePoint
     * @return
     */
    Bearing getHeel(TimePoint timePoint);

    /**
     * Calculates the pitch for a specific {@link TimePoint}
     * 
     * @param timePoint
     * @return
     */
    Bearing getPitch(TimePoint timePoint);

    /**
     * Tells if the competitor to which this track belongs is considered to be foiling at the time point
     * given. This depends on the {@link #getRideHeight} and a threshold above which a boat is considered
     * to be foiling.
     */
    boolean isFoiling(TimePoint timePoint);
    
    /**
     * Calculates the average ride height for the given time range.
     * 
     * @param from
     *            the lower bound of the time range to get the ride height for. Must not be <code>null</code>.
     * @param to
     *            the upper bound of the time range to get the ride height for. Must not be <code>null</code>.
     * @return the average ride height for the given time range or <code>null</code> if there are no fixes in the given
     *         time range.
     */
    Distance getAverageRideHeight(TimePoint from, TimePoint to);
    
    /**
     * The time spent foiling during the time range provided
     */
    Duration getTimeSpentFoiling(TimePoint from, TimePoint to);

    /**
     * The distance sailed foiling during the time range provided
     */
    Distance getDistanceSpentFoiling(TimePoint from, TimePoint to);
    
    /**
     * Returns {@code true}, if the track contains {@link BravoExtendedFix} instances instead of simple {@link BravoFix BravoFixes}.
     * It is not guaranteed that the track exclusively contains {@link BravoExtendedFix BravoExtendedFixes} if this is {@code true}.
     * It is up to the operator to ensure, that either normal Bravo data or the extended data is imported, but it isn't enforced.
     * If both types of fixes are contained, data may seem to have "gaps" because single fixes returned do not provide the extended data.
     */
    boolean hasExtendedFixes();
    
    Double getPortDaggerboardRakeIfAvailable(TimePoint timePoint);
    Double getStbdDaggerboardRakeStbdIfAvailable(TimePoint timePoint);
    Double getPortRudderRakeIfAvailable(TimePoint timePoint);
    Double getStbdRudderRakeIfAvailable(TimePoint timePoint);
    Bearing getMastRotationIfAvailable(TimePoint timePoint);
    Bearing getLeewayIfAvailable(TimePoint timePoint);
    Double getSetIfAvailable(TimePoint timePoint);
    Bearing getDriftIfAvailable(TimePoint timePoint);
    Distance getDepthIfAvailable(TimePoint timePoint);
    Bearing getRudderIfAvailable(TimePoint timePoint);
    Double getForestayLoadIfAvailable(TimePoint timePoint);
    Double getForestayPressureIfAvailable(TimePoint timePoint);
    Bearing getTackAngleIfAvailable(TimePoint timePoint);
    Bearing getRakeIfAvailable(TimePoint timePoint);
    Double getDeflectorPercentageIfAvailable(TimePoint timePoint);
    Bearing getTargetHeelIfAvailable(TimePoint timePoint);
    Distance getDeflectorIfAvailable(TimePoint timePoint);
    Double getTargetBoatspeedPIfAvailable(TimePoint timePoint);
    Double getExpeditionAWAIfAvailable(TimePoint at);
    Double getExpeditionAWSIfAvailable(TimePoint at);
    Double getExpeditionTWAIfAvailable(TimePoint at);
    Double getExpeditionTWSIfAvailable(TimePoint at);
    Double getExpeditionTWDIfAvailable(TimePoint at);
    Double getExpeditionTargTWAIfAvailable(TimePoint at);
    Double getExpeditionBoatSpeedIfAvailable(TimePoint at);
    Double getExpeditionTargBoatSpeedIfAvailable(TimePoint at);
    Double getExpeditionSOGIfAvailable(TimePoint at);
    Double getExpeditionCOGIfAvailable(TimePoint at);
    Double getExpeditionForestayLoadIfAvailable(TimePoint at);
    Double getExpeditionRakeIfAvailable(TimePoint at);
    Double getExpeditionCourseDetailIfAvailable(TimePoint at);
    Double getExpeditionHeadingIfAvailable(TimePoint at);
    Double getExpeditionVMGIfAvailable(TimePoint at);
    Double getExpeditionVMGTargVMGDeltaIfAvailable(TimePoint at);
    Double getExpeditionRateOfTurnIfAvailable(TimePoint at);
    Double getExpeditionTargetHeelIfAvailable(TimePoint at);
    Double getExpeditionTimeToPortLaylineIfAvailable(TimePoint at);
    Double getExpeditionTimeToStbLaylineIfAvailable(TimePoint at);
    Double getExpeditionDistToPortLaylineIfAvailable(TimePoint at);
    Double getExpeditionDistToStbLaylineIfAvailable(TimePoint at);
    Double getExpeditionTimeToGunInSecondsIfAvailable(TimePoint at);
    Double getExpeditionTimeToCommitteeBoatIfAvailable(TimePoint at);
    Double getExpeditionTimeToPinIfAvailable(TimePoint at);
    Double getExpeditionTimeToBurnToLineInSecondsIfAvailable(TimePoint at);
    Double getExpeditionTimeToBurnToCommitteeBoatIfAvailable(TimePoint at);
    Double getExpeditionTimeToBurnToPinIfAvailable(TimePoint at);
    Double getExpeditionDistanceToCommitteeBoatIfAvailable(TimePoint at);
    Double getExpeditionDistanceToPinDetailIfAvailable(TimePoint at);
    Double getExpeditionDistanceBelowLineInMetersIfAvailable(TimePoint at);
    Double getExpeditionLineSquareForWindIfAvailable(TimePoint at);
    Double getExpeditionBaroIfAvailable(TimePoint at);
    Double getExpeditionLoadSIfAvailable(TimePoint at);
    Double getExpeditionLoadPIfAvailable(TimePoint at);
    Double getExpeditionJibCarPortIfAvailable(TimePoint at);
    Double getExpeditionJibCarStbdIfAvailable(TimePoint at);
    Double getExpeditionMastButtIfAvailable(TimePoint at);
    Double getExpeditionKickerTensionIfAvailable(TimePoint at);
    Double getAverageExpeditionAWAIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionAWSIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTWAIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTWSIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTWDIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTargTWAIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionBoatSpeedIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTargBoatSpeedIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionSOGIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionCOGIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionForestayLoadIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionRakeIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionCourseDetailIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionHeadingIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionVMGIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionVMGTargVMGDeltaIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionRateOfTurnIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionRudderAngleIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTargetHeelIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTimeToPortLaylineIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTimeToStbLaylineIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionDistToPortLaylineIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionDistToStbLaylineIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTimeToGunInSecondsIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTimeToCommitteeBoatIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTimeToPinIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTimeToBurnToLineInSecondsIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTimeToBurnToCommitteeBoatIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionTimeToBurnToPinIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionDistanceToCommitteeBoatIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionDistanceToPinDetailIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionDistanceBelowLineInMetersIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionLineSquareForWindIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionBaroIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionLoadSIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionLoadPIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionJibCarPortIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionJibCarStbdIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionMastButtIfAvailable(TimePoint start, TimePoint endTimePoint);
    Double getAverageExpeditionKickerTensionIfAvailable(TimePoint start, TimePoint endTimePoint);
}
