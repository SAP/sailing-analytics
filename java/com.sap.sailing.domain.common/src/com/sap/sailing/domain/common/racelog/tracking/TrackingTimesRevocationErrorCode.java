package com.sap.sailing.domain.common.racelog.tracking;

/**
 * Error codes for the revocation of tracking times. These are used to report errors that occur during the revocation
 * process, e.g. when there is no regatta leaderboard or when there are no automated tracking times.
 * 
 * @see com.sap.sailing.domain.racelogtracking.TrackingTimesRevocationReport
 * 
 * @author Axel Uhl (d043530)
 *
 */
public enum TrackingTimesRevocationErrorCode {
    NO_REGATTA_LEADERBOARD,
    NO_AUTOMATED_TRACKING_TIMES
}