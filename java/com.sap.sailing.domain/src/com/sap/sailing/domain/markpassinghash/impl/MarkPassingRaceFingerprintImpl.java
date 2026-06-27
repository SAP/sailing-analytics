package com.sap.sailing.domain.markpassinghash.impl;

import java.util.logging.Logger;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.MarkPassingDataFinder;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprint;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;

public class MarkPassingRaceFingerprintImpl implements MarkPassingRaceFingerprint {
    private static final Logger logger = Logger.getLogger(MarkPassingRaceFingerprintImpl.class.getName());

    private final int calculatorVersion;
    private final int competitorHash;
    private final TimePoint startOfTracking;
    private final TimePoint startTimeReceived;
    private final TimePoint endOfTracking;
    private final TimePoint startTimeFromRaceLog;
    private final TimePoint finishTimeFromRaceLog;
    private final int fixedAndSuppressedMarkPassingsFromRaceLogHash;
    private final int waypointsHash;
    private final int numberOfGPSFixes;
    private final int gpsFixesHash;

    private static enum JSON_FIELDS {
        COMPETITOR_HASH, START_OF_TRACKING_AS_MILLIS, END_OF_TRACKING_AS_MILLIS, START_TIME_RECEIVED_AS_MILLIS,
        START_TIME_FROM_RACE_LOG_AS_MILLIS, FINISH_TIME_FROM_RACE_LOG_AS_MILLIS, WAYPOINTS_HASH, NUMBEROFGPSFIXES,
        GPSFIXES_HASH, RACE_ID, CALCULATOR_VERSION, FIXED_AND_SUPPRESSED_MARK_PASSINGS_FROM_RACE_LOG_HASH
    };

    public MarkPassingRaceFingerprintImpl(TrackedRace trackedRace) {
        this.calculatorVersion = MarkPassingCalculator.CALCULATOR_VERSION;
        this.competitorHash = calculateHashForCompetitors(trackedRace);
        this.startOfTracking = trackedRace.getStartOfTracking();
        this.endOfTracking = trackedRace.getEndOfTracking();
        this.startTimeReceived = trackedRace.getStartTimeReceived();
        final Pair<TimePoint, TimePoint> startAndFinishedTimeFromRaceLogs = trackedRace
                .getStartAndFinishedTimeFromRaceLogs();
        this.startTimeFromRaceLog = startAndFinishedTimeFromRaceLogs == null ? null
                : startAndFinishedTimeFromRaceLogs.getA();
        this.finishTimeFromRaceLog = startAndFinishedTimeFromRaceLogs == null ? null
                : startAndFinishedTimeFromRaceLogs.getB();
        this.waypointsHash = calculateHashForWaypoints(trackedRace);
        this.numberOfGPSFixes = calculateHashForNumberOfGPSFixes(trackedRace);
        this.gpsFixesHash = calculateHashForGPSFixes(trackedRace);
        this.fixedAndSuppressedMarkPassingsFromRaceLogHash = calculateFixedAndSuppressedMarkPassingsFromRaceLogHash(trackedRace);
    }

    private int calculateFixedAndSuppressedMarkPassingsFromRaceLogHash(TrackedRace trackedRace) {
        int result = 1023;
        for (final RaceLog raceLog : trackedRace.getAttachedRaceLogs()) {
            for (final Triple<Competitor, Integer, TimePoint> triple : new MarkPassingDataFinder(raceLog).analyze()) {
                result ^= triple.getA().getId().hashCode();
                result ^= triple.getB();
                if (triple.getC() != null) {
                    result ^= triple.getC().hashCode();
                }
            }
        }
        return result;
    }

    public MarkPassingRaceFingerprintImpl(JSONObject json) {
        final Number calculatorVersionNumber = (Number) json.get(JSON_FIELDS.CALCULATOR_VERSION.name());
        if (calculatorVersionNumber == null) {
            this.calculatorVersion = -1;
            logger.warning("Mark passing fingerprint JSON document did not contain CALCULATOR_VERSION field: "+json);
        } else {
            this.calculatorVersion = calculatorVersionNumber.intValue();
        }
        this.competitorHash = ((Number) json.get(JSON_FIELDS.COMPETITOR_HASH.name())).intValue();
        this.startOfTracking = TimePoint.of((Long) json.get(JSON_FIELDS.START_OF_TRACKING_AS_MILLIS.name()));
        this.endOfTracking = TimePoint.of((Long) json.get(JSON_FIELDS.END_OF_TRACKING_AS_MILLIS.name()));
        this.startTimeReceived = TimePoint.of((Long) json.get(JSON_FIELDS.START_TIME_RECEIVED_AS_MILLIS.name()));
        this.startTimeFromRaceLog = TimePoint
                .of((Long) json.get(JSON_FIELDS.START_TIME_FROM_RACE_LOG_AS_MILLIS.name()));
        this.finishTimeFromRaceLog = TimePoint
                .of((Long) json.get(JSON_FIELDS.FINISH_TIME_FROM_RACE_LOG_AS_MILLIS.name()));
        this.waypointsHash = ((Number) json.get(JSON_FIELDS.WAYPOINTS_HASH.name())).intValue();
        this.numberOfGPSFixes = ((Number) json.get(JSON_FIELDS.NUMBEROFGPSFIXES.name())).intValue();
        this.gpsFixesHash = ((Number) json.get(JSON_FIELDS.GPSFIXES_HASH.name())).intValue();
        this.fixedAndSuppressedMarkPassingsFromRaceLogHash = ((Number) json.get(JSON_FIELDS.FIXED_AND_SUPPRESSED_MARK_PASSINGS_FROM_RACE_LOG_HASH.name())).intValue();
    }

    @Override
    public JSONObject toJson() {
        JSONObject result = new JSONObject();
        result.put(JSON_FIELDS.CALCULATOR_VERSION.name(), calculatorVersion);
        result.put(JSON_FIELDS.COMPETITOR_HASH.name(), competitorHash);
        result.put(JSON_FIELDS.START_OF_TRACKING_AS_MILLIS.name(),
                startOfTracking == null ? null : startOfTracking.asMillis());
        result.put(JSON_FIELDS.END_OF_TRACKING_AS_MILLIS.name(),
                endOfTracking == null ? null : endOfTracking.asMillis());
        result.put(JSON_FIELDS.START_TIME_RECEIVED_AS_MILLIS.name(),
                startTimeReceived == null ? null : startTimeReceived.asMillis());
        result.put(JSON_FIELDS.START_TIME_FROM_RACE_LOG_AS_MILLIS.name(),
                startTimeFromRaceLog == null ? null : startTimeFromRaceLog.asMillis());
        result.put(JSON_FIELDS.FINISH_TIME_FROM_RACE_LOG_AS_MILLIS.name(),
                finishTimeFromRaceLog == null ? null : finishTimeFromRaceLog.asMillis());
        result.put(JSON_FIELDS.WAYPOINTS_HASH.name(), waypointsHash);
        result.put(JSON_FIELDS.NUMBEROFGPSFIXES.name(), numberOfGPSFixes);
        result.put(JSON_FIELDS.GPSFIXES_HASH.name(), gpsFixesHash);
        result.put(JSON_FIELDS.FIXED_AND_SUPPRESSED_MARK_PASSINGS_FROM_RACE_LOG_HASH.name(), fixedAndSuppressedMarkPassingsFromRaceLogHash);
        return result;
    }

    @Override
    public boolean matches(TrackedRace trackedRace) {
        final boolean result;
        if (!Util.equalsWithNull(calculatorVersion, MarkPassingCalculator.CALCULATOR_VERSION)) {
            result = false;
        } else if (!Util.equalsWithNull(startOfTracking, trackedRace.getStartOfTracking())) {
            result = false;
        } else if (!Util.equalsWithNull(endOfTracking, trackedRace.getEndOfTracking())) {
            result = false;
        } else if (!Util.equalsWithNull(startTimeReceived, trackedRace.getStartTimeReceived())) {
            result = false;
        } else {
            final Pair<TimePoint, TimePoint> startAndFinishedTimeFromRaceLogs = trackedRace
                    .getStartAndFinishedTimeFromRaceLogs();
            if (!Util.equalsWithNull(startTimeFromRaceLog,
                    startAndFinishedTimeFromRaceLogs == null ? null : startAndFinishedTimeFromRaceLogs.getA())) {
                result = false;
            } else if (!Util.equalsWithNull(finishTimeFromRaceLog,
                    startAndFinishedTimeFromRaceLogs == null ? null : startAndFinishedTimeFromRaceLogs.getB())) {
                result = false;
            } else if (waypointsHash != calculateHashForWaypoints(trackedRace)) {
                result = false;
            } else if (competitorHash != calculateHashForCompetitors(trackedRace)) {
                result = false;
            } else if (numberOfGPSFixes != calculateHashForNumberOfGPSFixes(trackedRace)) {
                result = false;
            } else if (fixedAndSuppressedMarkPassingsFromRaceLogHash != calculateFixedAndSuppressedMarkPassingsFromRaceLogHash(trackedRace)) {
                result = false;
            } else if (gpsFixesHash != calculateHashForGPSFixes(trackedRace)) {
                result = false;
            } else {
                result = true;
            }
        }
        return result;
    }

    private int calculateHashForCompetitors(TrackedRace trackedRace) {
        int hashForCompetitors = 1023;
        for (Competitor c : trackedRace.getRace().getCompetitors()) {
            hashForCompetitors = hashForCompetitors ^ c.getId().hashCode();
        }
        return hashForCompetitors;
    }

    private int calculateHashForNumberOfGPSFixes(TrackedRace trackedRace) {
        int count = 0;
        for (Mark m : trackedRace.getMarks()) {
            count += trackedRace.getTrack(m).size();
        }
        for (final Competitor competitor : trackedRace.getRace().getCompetitors()) {
            count += trackedRace.getTrack(competitor).size();
        }
        return count;
    }

    private int calculateHashForGPSFixes(TrackedRace trackedRace) {
        int res = 511;
        for (Mark m : trackedRace.getMarks()) {
            final GPSFixTrack<Mark, GPSFix> markTrack = trackedRace.getTrack(m);
            markTrack.lockForRead();
            try {
                for (GPSFix gf : markTrack.getRawFixes()) {
                    res = res ^ gf.getTimePoint().hashCode();
                    res = res ^ gf.getPosition().hashCode();
                }
            } finally {
                markTrack.unlockAfterRead();
            }
        }
        for (final Competitor competitor : trackedRace.getRace().getCompetitors()) {
            final GPSFixTrack<Competitor, GPSFixMoving> competitorTrack = trackedRace.getTrack(competitor);
            competitorTrack.lockForRead();
            try {
                for (GPSFixMoving gfm : competitorTrack.getRawFixes()) {
                    res = res ^ gfm.getTimePoint().hashCode();
                    res = res ^ gfm.getPosition().hashCode();
                    res = res ^ gfm.getSpeed().getBearing().hashCode();
                    res = res ^ Double.hashCode(gfm.getSpeed().getKnots());
                }
            } finally {
                competitorTrack.unlockAfterRead();
            }
        }
        return res;
    }

    private int calculateHashForWaypoints(TrackedRace trackedRace) {
        Iterable<Waypoint> waypoints = trackedRace.getRace().getCourse().getWaypoints();
        int res = 0;
        for (Waypoint p : waypoints) {
            Iterable<Mark> marks = p.getMarks();
            for (Mark m : marks) {
                res = res ^ m.getId().hashCode();
            }
            res = res ^ p.getPassingInstructions().name().hashCode();
            res = (res << 5) - res; // we want to detect changes in order
        }
        return res;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + calculatorVersion;
        result = prime * result + competitorHash;
        result = prime * result + ((endOfTracking == null) ? 0 : endOfTracking.hashCode());
        result = prime * result + ((finishTimeFromRaceLog == null) ? 0 : finishTimeFromRaceLog.hashCode());
        result = prime * result + gpsFixesHash;
        result = prime * result + numberOfGPSFixes;
        result = prime * result + ((startOfTracking == null) ? 0 : startOfTracking.hashCode());
        result = prime * result + ((startTimeFromRaceLog == null) ? 0 : startTimeFromRaceLog.hashCode());
        result = prime * result + ((startTimeReceived == null) ? 0 : startTimeReceived.hashCode());
        result = prime * result + waypointsHash;
        result = prime * result + fixedAndSuppressedMarkPassingsFromRaceLogHash;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MarkPassingRaceFingerprintImpl other = (MarkPassingRaceFingerprintImpl) obj;
        if (calculatorVersion != other.calculatorVersion)
            return false;
        if (competitorHash != other.competitorHash)
            return false;
        if (endOfTracking == null) {
            if (other.endOfTracking != null)
                return false;
        } else if (!endOfTracking.equals(other.endOfTracking))
            return false;
        if (finishTimeFromRaceLog == null) {
            if (other.finishTimeFromRaceLog != null)
                return false;
        } else if (!finishTimeFromRaceLog.equals(other.finishTimeFromRaceLog))
            return false;
        if (gpsFixesHash != other.gpsFixesHash)
            return false;
        if (numberOfGPSFixes != other.numberOfGPSFixes)
            return false;
        if (startOfTracking == null) {
            if (other.startOfTracking != null)
                return false;
        } else if (!startOfTracking.equals(other.startOfTracking))
            return false;
        if (startTimeFromRaceLog == null) {
            if (other.startTimeFromRaceLog != null)
                return false;
        } else if (!startTimeFromRaceLog.equals(other.startTimeFromRaceLog))
            return false;
        if (startTimeReceived == null) {
            if (other.startTimeReceived != null)
                return false;
        } else if (!startTimeReceived.equals(other.startTimeReceived))
            return false;
        if (waypointsHash != other.waypointsHash)
            return false;
        if (fixedAndSuppressedMarkPassingsFromRaceLogHash != other.fixedAndSuppressedMarkPassingsFromRaceLogHash)
            return false;
        return true;
    }
}