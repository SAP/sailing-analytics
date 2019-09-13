package com.sap.sailing.windestimation.data.importer;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.windestimation.data.SingleDimensionBasedTwdTransition;
import com.sap.sailing.windestimation.data.WindSourceWithFixes;
import com.sap.sailing.windestimation.data.persistence.maneuver.PersistedElementsIterator;
import com.sap.sailing.windestimation.data.persistence.twdtransition.SingleDimensionBasedTwdTransitionPersistenceManager;
import com.sap.sailing.windestimation.data.persistence.twdtransition.SingleDimensionBasedTwdTransitionPersistenceManager.SingleDimensionType;
import com.sap.sailing.windestimation.data.persistence.twdtransition.WindSourcesPersistenceManager;
import com.sap.sailing.windestimation.util.LoggingUtil;
import com.sap.sse.common.TimePoint;

public class DistanceBasedTwdTransitionImporter {

    public static final int TOLERANCE_SECONDS = 5;
    public static final int SAMPLING_SECONDS = 1;
    public static final int MAX_DISTANCE_METERS = 1000000;
    public static final int MIN_DISTANCE_METERS = 10;

    public static void main(String[] args) throws UnknownHostException {
        LoggingUtil.logInfo("###################\r\nDistance based TWD transitions Import started");
        WindSourcesPersistenceManager windSourcesPersistenceManager = new WindSourcesPersistenceManager();
        SingleDimensionBasedTwdTransitionPersistenceManager distanceBasedTwdTransitionPersistenceManager = new SingleDimensionBasedTwdTransitionPersistenceManager(
                SingleDimensionType.DISTANCE);
        distanceBasedTwdTransitionPersistenceManager.dropCollection();
        long totalValuesCount = 0;
        long numberOfWindSources = windSourcesPersistenceManager.countElements();
        long windSourceNumber = 1;
        for (PersistedElementsIterator<WindSourceWithFixes> iterator = windSourcesPersistenceManager
                .getIterator(); iterator.hasNext();) {
            long percent = windSourceNumber * 100 / numberOfWindSources;
            WindSourceWithFixes windSource = iterator.next();
            LoggingUtil.logInfo("Processing wind source " + windSourceNumber++ + "/" + numberOfWindSources + " ("
                    + percent + "%) for distance dimension");
            List<SingleDimensionBasedTwdTransition> entries = new ArrayList<>();
            List<WindSourceWithFixes> otherWindSources = new ArrayList<>();
            for (PersistedElementsIterator<WindSourceWithFixes> otherWindSourcesIterator = windSourcesPersistenceManager
                    .getIteratorForEntriesIntersectingPeriodAndHigherThanDbId(
                            windSource.getWindSourceMetadata().getStartTime(),
                            windSource.getWindSourceMetadata().getEndTime(), windSource.getDbId(),
                            TOLERANCE_SECONDS); otherWindSourcesIterator.hasNext();) {
                WindSourceWithFixes otherWindSource = otherWindSourcesIterator.next();
                double distanceBetweenWindSourcesInMeters = windSource.getWindSourceMetadata().getFirstPosition()
                        .getDistance(otherWindSource.getWindSourceMetadata().getFirstPosition()).getMeters();
                if (distanceBetweenWindSourcesInMeters <= MAX_DISTANCE_METERS
                        && distanceBetweenWindSourcesInMeters >= MIN_DISTANCE_METERS) {
                    otherWindSources.add(otherWindSource);
                }
            }
            for (ListIterator<WindSourceWithFixes> otherWindSourcesIterator = otherWindSources
                    .listIterator(); otherWindSourcesIterator.hasNext();) {
                WindSourceWithFixes otherWindSource = otherWindSourcesIterator.next();
                Iterator<Wind> fixesIterator = windSource.getWindFixes().iterator();
                Iterator<Wind> otherFixesIterator = otherWindSource.getWindFixes().iterator();
                Wind currentOtherFix = otherFixesIterator.next();
                Wind currentFix = fixesIterator.next();
                Wind nextFix = null;
                Wind nextOtherFix = null;
                TimePoint timePointOfLastTransition = null;
                double bestDuration = currentFix.getTimePoint().until(currentOtherFix.getTimePoint()).abs().asSeconds();
                do {
                    while (fixesIterator.hasNext()) {
                        nextFix = fixesIterator.next();
                        double duration = currentOtherFix.getTimePoint().until(nextFix.getTimePoint()).abs()
                                .asSeconds();
                        if (bestDuration > duration) {
                            bestDuration = duration;
                            currentFix = nextFix;
                        } else {
                            break;
                        }
                    }
                    while (otherFixesIterator.hasNext()) {
                        nextOtherFix = otherFixesIterator.next();
                        double duration = currentFix.getTimePoint().until(nextOtherFix.getTimePoint()).abs()
                                .asSeconds();
                        if (bestDuration > duration) {
                            bestDuration = duration;
                            currentOtherFix = nextOtherFix;
                        } else {
                            break;
                        }
                    }
                    if (bestDuration <= TOLERANCE_SECONDS) {
                        TimePoint timePointOfNewTransition = currentFix.getTimePoint()
                                .before(currentOtherFix.getTimePoint()) ? currentFix.getTimePoint()
                                        : currentOtherFix.getTimePoint();
                        if (timePointOfLastTransition == null || timePointOfLastTransition
                                .until(timePointOfNewTransition).asSeconds() >= SAMPLING_SECONDS) {
                            timePointOfLastTransition = currentFix.getTimePoint().before(currentOtherFix.getTimePoint())
                                    ? currentOtherFix.getTimePoint()
                                    : currentFix.getTimePoint();
                            double meters = currentFix.getPosition().getDistance(currentOtherFix.getPosition())
                                    .getMeters();
                            double twdChange = currentFix.getBearing().getDifferenceTo(currentOtherFix.getBearing())
                                    .getDegrees();
                            SingleDimensionBasedTwdTransition entry = new SingleDimensionBasedTwdTransition(meters,
                                    twdChange);
                            entries.add(entry);
                        }
                    }
                    currentFix = fixesIterator.hasNext() ? nextFix : null;
                    currentOtherFix = otherFixesIterator.hasNext() ? nextOtherFix : null;
                } while (currentFix != null && currentOtherFix != null);
            }
            if (entries.isEmpty()) {
                LoggingUtil.logInfo("No distance based TWD transitions to import");
            } else {
                distanceBasedTwdTransitionPersistenceManager.add(entries);
                int totalEntries = entries.size();
                totalValuesCount += totalEntries;
                LoggingUtil.logInfo(
                        totalEntries + " distance based TWD transitions imported, " + totalValuesCount + " in total");
            }
        }
        LoggingUtil.logInfo("###################\r\nDistance based TWD transitions Import finished");
        LoggingUtil.logInfo("Totally " + totalValuesCount + " distance based TWD transitions imported");
    }

}
