package com.sap.sailing.domain.base;

import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sse.metering.CPUMeter;

/**
 * The {@link #name()}s of this enumeration type's literals can be used as the keys for the {@link CPUMeter} that each
 * {@link Leaderboard} or {@link TrackedRegatta} has. This way, different types of CPU consumption may be distinguished.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public enum CPUMeteringType {
    NET_POINTS_SUM, COMPETITORS_FROM_BEST_TO_WORST, MANEUVER_DETECTION, QUICK_RANKS, CROSS_TRACK_ERROR, SIMULATOR, LEADERBOARD_COMPUTE_DTO, MARK_PASSINGS, BACKEND_POLARS;
}
