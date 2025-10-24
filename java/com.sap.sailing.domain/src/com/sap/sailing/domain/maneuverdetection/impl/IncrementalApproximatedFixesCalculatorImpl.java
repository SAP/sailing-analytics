package com.sap.sailing.domain.maneuverdetection.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.maneuverdetection.IncrementalApproximatedFixesCalculator;
import com.sap.sailing.domain.shared.tracking.impl.TimedComparator;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.shared.util.impl.ArrayListNavigableSet;

/**
 * Incremental douglas peucker calculator which reuses the already calculated douglas peucker points within legs. It
 * groups the already calculated douglas peucker points by leg. The already calculated douglas peucker points of a leg
 * get only reused, if the leg was completely calculated. The leg will not be reused, if the leg is the first leg within
 * the time range of {@link #approximate(TimePoint, TimePoint)} parameters, and the {@code earliestStart} is different
 * from the earliest {@code earliestStart} which has been ever provided. Analogously, the leg will not be reused, if the
 * leg is the last leg within the queried time range, and the {@code latestEnd} is different from the latest
 * {@code latestEnd} which has been ever provided.
 * 
 * @author Vladislav Chumak (D069712)
 * @deprecated The set of douglas peucker fixes calculated by this calculator incrementally differs from the set
 *             calculated by a full calculation run. Therefore, this calculator cannot be used in production right now.
 *
 */
public class IncrementalApproximatedFixesCalculatorImpl extends ApproximatedFixesCalculatorImpl
        implements IncrementalApproximatedFixesCalculator {

    private volatile FixesApproximationResult lastFixesApproximationResult = null;
    private GPSFixTrack<Competitor, GPSFixMoving> track;
    private final Duration minDurationFromLastFixToPreviousMarkPassingToReusePreviousLegFixes;

    public IncrementalApproximatedFixesCalculatorImpl(TrackedRace trackedRace, Competitor competitor) {
        super(trackedRace, competitor);
        this.track = trackedRace.getTrack(competitor);
        this.minDurationFromLastFixToPreviousMarkPassingToReusePreviousLegFixes = trackedRace.getRace()
                .getBoatOfCompetitor(competitor).getBoatClass().getApproximateManeuverDuration().times(10.0);
    }

    @Override
    public Iterable<GPSFixMoving> approximate(TimePoint earliestStart, TimePoint latestEnd) {
        GPSFixMoving latestFix = track.getLastFixAtOrBefore(latestEnd);
        if (latestFix == null || !earliestStart.before(latestEnd)) {
            return Collections.emptyList();
        }
        FixesApproximationResult lastFixesApproximationResult = this.lastFixesApproximationResult;
        Iterable<GPSFixMoving> result;
        int alreadyApproximatedLegsCount = lastFixesApproximationResult == null ? 0
                : Util.size(lastFixesApproximationResult.getLegFixesList());
        if (alreadyApproximatedLegsCount < 2) {
            result = super.approximate(earliestStart, latestEnd);
            storeLastFixesApproximationResult(earliestStart, latestEnd, latestFix, result);
        } else {
            List<LegFixes> legFixesListToReuse = new ArrayList<>();
            ListIterator<LegFixes> existingLegFixesIterator = lastFixesApproximationResult.getLegFixesList()
                    .listIterator();
            boolean recalculateFixesAtBeginning;
            if (!earliestStart.equals(lastFixesApproximationResult.getEarliestStart())) {
                // discard fixes of the first leg
                recalculateFixesAtBeginning = true;
                GPSFixMoving earliestFix = track.getFirstFixAtOrAfter(earliestStart);
                int legNumberOfEarliestFix = earliestFix != null ? getLegNumberAt(earliestFix.getTimePoint()) : 0;
                // ingore first existing leg, because it might be incomplete
                existingLegFixesIterator.next();
                while (existingLegFixesIterator.hasNext()) {
                    LegFixes legFixesToReuse = existingLegFixesIterator.next();
                    if (earliestFix != null && checkIfLegBeginningFarEnoughFromEarliestFixToReuse(earliestFix,
                            legNumberOfEarliestFix, legFixesToReuse)) {
                        existingLegFixesIterator.previous();
                        if (earliestFix.getTimePoint()
                                .equals(legFixesToReuse.getFirstApproximatedFix().getTimePoint())) {
                            recalculateFixesAtBeginning = false;
                        }
                        break;
                    }
                }
            } else {
                recalculateFixesAtBeginning = false;
            }
            boolean recalculateFixesAtEnd;
            if (lastFixesApproximationResult.getLatestEnd().equals(latestEnd)
                    && (latestFix.getTimePoint().equals(lastFixesApproximationResult.getLatestFix().getTimePoint()))) {
                recalculateFixesAtEnd = false;
                // reuse existing leg fixes from current iterator cursor position completely
                while (existingLegFixesIterator.hasNext()) {
                    legFixesListToReuse.add(existingLegFixesIterator.next());
                }
            } else {
                recalculateFixesAtEnd = true;
                if (latestEnd != null) {
                    int legNumberOfLatestFix = getLegNumberAt(latestFix.getTimePoint());
                    // cut off last legs and recalculate these legs
                    while (existingLegFixesIterator.hasNext()) {
                        LegFixes legFixesToReuse = existingLegFixesIterator.next();
                        if (checkIfLegEndFarEnoughFromLatestFixToReuse(latestFix, legNumberOfLatestFix,
                                legFixesToReuse)) {
                            // ignore the last leg, because it might be incomplete. Do not ignore it, if the time point
                            // of the latest fix matches with the latest fix of the existing leg.
                            if (existingLegFixesIterator.hasNext() || latestFix.getTimePoint()
                                    .equals(legFixesToReuse.getLastApproximatedFix().getTimePoint())) {
                                legFixesListToReuse.add(legFixesToReuse);
                                if (!existingLegFixesIterator.hasNext()) {
                                    recalculateFixesAtEnd = false;
                                    break;
                                }
                            }
                        } else {
                            break;
                        }
                    }
                }
            }

            if (legFixesListToReuse.isEmpty()) {
                result = super.approximate(earliestStart, latestEnd);
                storeLastFixesApproximationResult(earliestStart, latestEnd, latestFix, result);
            } else {
                List<GPSFixMoving> resultList = new ArrayList<>();
                result = resultList;
                if (recalculateFixesAtBeginning) {
                    LegFixes firstLegFixesToReuse = legFixesListToReuse.get(0);
                    Iterable<GPSFixMoving> newApproximatedFixesBefore = super.approximate(earliestStart,
                            getTimePointToEndTheCalculation(firstLegFixesToReuse, lastFixesApproximationResult));
                    Iterator<GPSFixMoving> newApproximatedFixesBeforeIterator = newApproximatedFixesBefore.iterator();
                    if (newApproximatedFixesBeforeIterator.hasNext()) {
                        storeNewFixesBeforeExistingFixes(newApproximatedFixesBefore, earliestStart);
                        // add all new fixes to result, but discard the last one, because it is part of the next leg,
                        // which is reused
                        TimePoint timePointOfFirstFixToReuse = firstLegFixesToReuse.getFirstApproximatedFix()
                                .getTimePoint();
                        while (newApproximatedFixesBeforeIterator.hasNext()) {
                            GPSFixMoving fix = newApproximatedFixesBeforeIterator.next();
                            if (fix.getTimePoint().before(timePointOfFirstFixToReuse)) {
                                resultList.add(fix);
                            } else {
                                break;
                            }
                        }
                    }
                }
                for (LegFixes legFixes : legFixesListToReuse) {
                    Util.addAll(legFixes.getApproximatedFixes(), resultList);
                }
                if (recalculateFixesAtEnd) {
                    LegFixes lastReusedLegFixes = legFixesListToReuse.get(legFixesListToReuse.size() - 1);
                    Iterable<GPSFixMoving> newApproximatedFixesAfter = super.approximate(
                            getTimePointToStartTheCalculationFrom(lastReusedLegFixes, lastFixesApproximationResult),
                            latestEnd);
                    Iterator<GPSFixMoving> newApproximatedFixesAfterIterator = newApproximatedFixesAfter.iterator();
                    if (newApproximatedFixesAfterIterator.hasNext()) {
                        storeNewFixesAfterExistingFixes(newApproximatedFixesAfter, latestFix, latestEnd);
                        // add all new fixes to result, but discard the first one
                        TimePoint timePointOfLatestFixToReuse = lastReusedLegFixes.getLastApproximatedFix()
                                .getTimePoint();
                        while (newApproximatedFixesAfterIterator.hasNext()) {
                            GPSFixMoving gpsFixMoving = newApproximatedFixesAfterIterator.next();
                            if (gpsFixMoving.getTimePoint().after(timePointOfLatestFixToReuse)) {
                                resultList.add(gpsFixMoving);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Gets the time point, from which the calculation of new fixes shall get started, if at least one existing leg is
     * being reused. The {@code reusedLastLegFixes} must be the last reused leg and lie before the new computation
     * interval. This method determines the optimal start time point of the new computation interval, such that the time
     * point corresponds to the already determined douglas peucker fix which lies closest to the next mark passing after
     * {@code reusedLastLegFixes}. The determined start time point can lie either on {code reusedLastLegFixes}, or its
     * following existing leg contained in {@code lastFixesApproximationResult}. The next existing leg gets ignored if
     * it is composed less than of two fixes.
     */
    private TimePoint getTimePointToStartTheCalculationFrom(LegFixes reusedLastLegFixes,
            FixesApproximationResult lastFixesApproximationResult) {
        LegFixes nextExistingLeg = getExistingLegFixesByLegNumber(reusedLastLegFixes.getLegNumber() + 1,
                lastFixesApproximationResult);
        TimePoint result = reusedLastLegFixes.getLastApproximatedFix().getTimePoint();
        if (nextExistingLeg != null
                && !nextExistingLeg.getFirstApproximatedFix().equals(nextExistingLeg.getLastApproximatedFix())) {
            TimePoint timePointOfNextMarkPassing = reusedLastLegFixes.getTimePointOfNextMarkPassing();
            if (timePointOfNextMarkPassing != null) {
                Duration durationFromLastFixOfReusedLegToMarkPassing = reusedLastLegFixes.getLastApproximatedFix()
                        .getTimePoint().until(timePointOfNextMarkPassing);
                Duration durationFromMarkPassingTillNextExistingFix = timePointOfNextMarkPassing
                        .until(nextExistingLeg.getFirstApproximatedFix().getTimePoint());
                if (durationFromLastFixOfReusedLegToMarkPassing
                        .compareTo(durationFromMarkPassingTillNextExistingFix) > 0) {
                    result = nextExistingLeg.getFirstApproximatedFix().getTimePoint();
                }
            }
        }
        return result;
    }

    /**
     * Gets the time point, till which the calculation of new fixes shall run, if at least one existing leg is being
     * reused. The {@code reusedFirstLegFixes} must be the first reused leg and lie after the new computation interval.
     * This method determines the optimal end time point of the new computation interval, such that the time point
     * corresponds to the already determined douglas peucker fix which lies closest to the mark passing from which the
     * {@code reusedFirstLegFixes} starts. The determined end time point can lie either on {code reusedFirstLegFixes},
     * or its preceding existing leg contained in {@code lastFixesApproximationResult}. The preceding existing leg gets
     * ignored if it is composed less than of two fixes.
     */
    private TimePoint getTimePointToEndTheCalculation(LegFixes reusedFirstLegFixes,
            FixesApproximationResult lastFixesApproximationResult) {
        LegFixes previousExistingLeg = getExistingLegFixesByLegNumber(reusedFirstLegFixes.getLegNumber() - 1,
                lastFixesApproximationResult);
        TimePoint result = reusedFirstLegFixes.getLastApproximatedFix().getTimePoint();
        if (previousExistingLeg != null && !previousExistingLeg.getFirstApproximatedFix()
                .equals(previousExistingLeg.getLastApproximatedFix())) {
            TimePoint timePointOfPreviousMarkPassing = previousExistingLeg.getTimePointOfNextMarkPassing();
            if (timePointOfPreviousMarkPassing != null) {
                Duration durationFromMarkPassingToFirstFixOfReusedLeg = timePointOfPreviousMarkPassing
                        .until(reusedFirstLegFixes.getFirstApproximatedFix().getTimePoint());
                Duration durationFromPreviousExistingFixToMarkPassing = previousExistingLeg.getLastApproximatedFix()
                        .getTimePoint().until(timePointOfPreviousMarkPassing);
                if (durationFromMarkPassingToFirstFixOfReusedLeg
                        .compareTo(durationFromPreviousExistingFixToMarkPassing) > 0) {
                    result = previousExistingLeg.getLastApproximatedFix().getTimePoint();
                }
            }
        }
        return result;
    }

    /**
     * Gets the LegFixes within the provided {@code lastFixesApproximationResult} which number corresponds to the
     * provided {@code legNumber}
     */
    private LegFixes getExistingLegFixesByLegNumber(int legNumber,
            FixesApproximationResult lastFixesApproximationResult) {
        LegFixes matchedLegFixes = null;
        for (LegFixes legFixes : lastFixesApproximationResult.getLegFixesList()) {
            if (legFixes.getLegNumber() == legNumber) {
                matchedLegFixes = legFixes;
                break;
            }
        }
        return matchedLegFixes;
    }

    /**
     * Checks whether the provided {@code legFixesToReuse} is eligible for reuse considering the {@code latestFix} and
     * {@code legNumberOfLatestFix}. The provided {@code legFixesToReuse} is eligible for reuse, if the time point of
     * its last fix matches the time point of {@code latestFix}, or if the time point of the last fix of
     * {@code legFixes} is lying at least {@link #minDurationFromLastFixToPreviousMarkPassingToReusePreviousLegFixes}
     * before the time point of the provided {@code latestFix} and the {@code legNumberOfLatestFix} is higher than the
     * leg number of the leg represented by {@code legFixes}.
     */
    private boolean checkIfLegEndFarEnoughFromLatestFixToReuse(GPSFixMoving latestFix, int legNumberOfLatestFix,
            LegFixes legFixesToReuse) {
        GPSFixMoving lastExistingFixOfLeg = legFixesToReuse.getLastApproximatedFix();
        if (latestFix.getTimePoint().equals(lastExistingFixOfLeg.getTimePoint()) || latestFix.getTimePoint().asMillis()
                - lastExistingFixOfLeg.getTimePoint()
                        .asMillis() > minDurationFromLastFixToPreviousMarkPassingToReusePreviousLegFixes.asMillis()
                && legFixesToReuse.getLegNumber() < legNumberOfLatestFix) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether the provided {@code legFixesToReuse} is eligible for reuse considering the {@code earliestFix} and
     * {@code legNumberOfEarliestFix}. The provided {@code legFixesToReuse} is eligible for reuse, if the time point of
     * its first fix matches the time point of {@code earliestFix}, or if the time point of the first fix of
     * {@code legFixes} is lying at least {@link #minDurationFromLastFixToPreviousMarkPassingToReusePreviousLegFixes}
     * after the time point of the provided {@code earliestFix} and the {@code legNumberOfEarliestFix} is smaller than
     * the leg number of the leg represented by {@code legFixes}.
     */
    private boolean checkIfLegBeginningFarEnoughFromEarliestFixToReuse(GPSFixMoving earliestFix,
            int legNumberOfEarliestFix, LegFixes legFixesToReuse) {
        GPSFixMoving firstExistingFixOfLeg = legFixesToReuse.getFirstApproximatedFix();
        if (earliestFix.getTimePoint().equals(firstExistingFixOfLeg.getTimePoint())
                || firstExistingFixOfLeg.getTimePoint().asMillis() - earliestFix.getTimePoint()
                        .asMillis() > minDurationFromLastFixToPreviousMarkPassingToReusePreviousLegFixes.asMillis()
                        && legFixesToReuse.getLegNumber() > legNumberOfEarliestFix) {
            return true;
        }
        return false;
    }

    /**
     * Stores the fixes retrieved <b>exactly</b> after the last fix of an existing leg within
     * {@link #lastFixesApproximationResult}. For this, the time point of first fix within provided
     * {@code newApproximatedFixesAfter} must lie exactly at the time point of last fix of any existing leg. The new
     * fixes get only stored, if {@link #lastFixesApproximationResult} already exists and the time point of the last fix
     * within {@code newApproximatedFixesAfter} is after the time point of the last fix of the last existing leg.
     * 
     * @param newApproximatedFixesAfter
     *            The new fixes starting from the last fix of an existing leg which was reused
     * @param latestFix
     *            The latest raw fix of the track
     * @param latestEnd
     *            The latest start requested within {@link #approximate(TimePoint, TimePoint)}
     */
    private void storeNewFixesAfterExistingFixes(Iterable<GPSFixMoving> newApproximatedFixesAfter,
            GPSFixMoving latestFix, TimePoint latestEnd) {
        FixesApproximationResult lastFixesApproximationResult = this.lastFixesApproximationResult;
        if (lastFixesApproximationResult != null) {
            List<LegFixes> existingLegFixesList = lastFixesApproximationResult.getLegFixesList();
            // stored legFixes list cannot be empty (see storeLastFixesApproximationResult())
            LegFixes lastExistingLegFixes = existingLegFixesList.get(existingLegFixesList.size() - 1);
            Iterator<GPSFixMoving> newFixesIterator = newApproximatedFixesAfter.iterator();
            GPSFixMoving lastFix;
            do {
                lastFix = newFixesIterator.next();
            } while (newFixesIterator.hasNext());
            if (lastExistingLegFixes.getLastApproximatedFix().getTimePoint().before(lastFix.getTimePoint())) {
                List<LegFixes> newLegFixesListAfter = groupApproximatedFixesToLegFixes(newApproximatedFixesAfter);
                if (!newLegFixesListAfter.isEmpty()) {
                    // first fix is would be always the fix of the previous leg which was reused, when this method is
                    // called, if the already analysed fixes would not change. However, because we have outlier removal
                    // algorithm operating with the fixes, the fixes may change. This means, the last fix of the reused
                    // leg must lie AT, or BEFORE the first fix of new fixes set.
                    TimePoint timePointOfFirstNewFix = newLegFixesListAfter.get(0).getFirstApproximatedFix()
                            .getTimePoint();
                    List<LegFixes> newExistingLegFixesList = new ArrayList<>();
                    for (LegFixes legFixes : existingLegFixesList) {
                        if (!legFixes.getLastApproximatedFix().getTimePoint().after(timePointOfFirstNewFix)) {
                            newExistingLegFixesList.add(legFixes);
                        } else {
                            break;
                        }
                    }
                    // the reused leg fixes must not be empty in the context of this method call, but lets be sure
                    if (!newExistingLegFixesList.isEmpty()) {
                        TimePoint timePointOfLastReusedFix = newExistingLegFixesList
                                .get(newExistingLegFixesList.size() - 1).getLastApproximatedFix().getTimePoint();
                        for (LegFixes legFixes : newLegFixesListAfter) {
                            if (legFixes.getFirstApproximatedFix().getTimePoint().after(timePointOfLastReusedFix)) {
                                newExistingLegFixesList.add(legFixes);
                            }
                        }
                        this.lastFixesApproximationResult = new FixesApproximationResult(
                                lastFixesApproximationResult.getEarliestStart(), latestEnd, latestFix,
                                newExistingLegFixesList);
                    }
                }
            }
        }
    }

    /**
     * Stores the fixes retrieved <b>exactly</b> before the beginning fix of an existing leg within
     * {@link #lastFixesApproximationResult}. For this, the time point of last fix within provided
     * {@code newApproximatedFixesBefore} must lie exactly at the time point of beginning fix of any existing leg. The
     * new fixes get only stored, if {@link #lastFixesApproximationResult} already exists and the time point of the
     * first fix within {@code newApproximatedFixesBefore} is before the time point of the first fix of the first
     * existing leg.
     * 
     * @param newApproximatedFixesBefore
     *            The new fixes ending at the first fix of an existing leg which was reused
     * @param earliestStart
     *            The earliest start requested within {@link #approximate(TimePoint, TimePoint)}
     */
    private void storeNewFixesBeforeExistingFixes(Iterable<GPSFixMoving> newApproximatedFixesBefore,
            TimePoint earliestStart) {
        FixesApproximationResult lastFixesApproximationResult = this.lastFixesApproximationResult;
        if (lastFixesApproximationResult != null) {
            List<LegFixes> existingLegFixesList = lastFixesApproximationResult.getLegFixesList();
            // stored legFixes list cannot be empty (see storeLastFixesApproximationResult())
            LegFixes firstExistingLegFixes = existingLegFixesList.get(0);
            GPSFixMoving firstNewFix = newApproximatedFixesBefore.iterator().next();
            if (firstExistingLegFixes.getFirstApproximatedFix().getTimePoint().after(firstNewFix.getTimePoint())) {
                List<LegFixes> newLegFixesListBefore = groupApproximatedFixesToLegFixes(newApproximatedFixesBefore);
                if (!newLegFixesListBefore.isEmpty()) {
                    // last fix is always the fix at the beginning of the next leg, when this method is
                    // called, if the already analysed fixes would not change. However, because we have outlier removal
                    // algorithm operating with the fixes, the fixes may change. This means, the first fix of the reused
                    // leg must lie AT, or AFTER the last fix of new fixes set.
                    List<LegFixes> newExistingLegFixesList = new ArrayList<>();
                    TimePoint timePointOfLastNewFix = newLegFixesListBefore.get(newLegFixesListBefore.size() - 1)
                            .getLastApproximatedFix().getTimePoint();
                    for (LegFixes legFixes : existingLegFixesList) {
                        if (!legFixes.getFirstApproximatedFix().getTimePoint().before(timePointOfLastNewFix)) {
                            newExistingLegFixesList.add(legFixes);
                        }
                    }
                    // the reused leg fixes must not be empty in the context of this method call, but lets be sure
                    if (!newExistingLegFixesList.isEmpty()) {
                        TimePoint timePointOfFirstReusedFix = newExistingLegFixesList.get(0).getFirstApproximatedFix()
                                .getTimePoint();
                        List<LegFixes> extendedExistingNewLegFixesList = new ArrayList<>();
                        for (LegFixes legFixes : newLegFixesListBefore) {
                            if (legFixes.getLastApproximatedFix().getTimePoint().before(timePointOfFirstReusedFix)) {
                                extendedExistingNewLegFixesList.add(legFixes);
                            } else {
                                break;
                            }
                        }
                        extendedExistingNewLegFixesList.addAll(newExistingLegFixesList);
                        this.lastFixesApproximationResult = new FixesApproximationResult(earliestStart,
                                lastFixesApproximationResult.getLatestEnd(),
                                lastFixesApproximationResult.getLatestFix(), extendedExistingNewLegFixesList);
                    }
                }
            }
        }
    }

    private void storeLastFixesApproximationResult(TimePoint earliestStart, TimePoint latestEnd, GPSFixMoving latestFix,
            Iterable<GPSFixMoving> approximatedFixes) {
        FixesApproximationResult lastFixesApproximationResult = this.lastFixesApproximationResult;
        List<LegFixes> legFixesList = groupApproximatedFixesToLegFixes(approximatedFixes);
        if (!legFixesList.isEmpty() && (lastFixesApproximationResult == null
                || legFixesList.size() >= lastFixesApproximationResult.getLegFixesList().size())) {
            this.lastFixesApproximationResult = new FixesApproximationResult(earliestStart, latestEnd, latestFix,
                    legFixesList);
        }
    }

    /**
     * Groups provided {@code approximatedFixes} per leg. For this, the time point of the mark passing and the fix is
     * considered. A fix belongs to a leg, if its time point lies at or after the time point of corresponding mark
     * passing, but before the time point of the next mark passing. If a leg does not contain any fixes, it will be
     * skipped. The legs are represented by numbers calculated by {@link #getLegNumberAt(TimePoint)}.
     */
    private List<LegFixes> groupApproximatedFixesToLegFixes(Iterable<GPSFixMoving> approximatedFixes) {
        List<LegFixes> result = new ArrayList<>();
        Iterator<GPSFixMoving> approximatedFixesIterator = approximatedFixes == null ? null
                : approximatedFixes.iterator();
        NavigableSet<MarkPassing> roundings = trackedRace.getMarkPassings(competitor);
        if (approximatedFixesIterator != null && approximatedFixesIterator.hasNext()) {
            if (roundings != null) {
                NavigableSet<MarkPassing> localRoundings = null;
                trackedRace.lockForRead(roundings);
                try {
                    localRoundings = new ArrayListNavigableSet<>(roundings.size(), new TimedComparator());
                    localRoundings.addAll(roundings);
                } finally {
                    trackedRace.unlockAfterRead(roundings);
                }
                int legNumber = 0;
                List<GPSFixMoving> legFixes = new ArrayList<>();
                GPSFixMoving approximatedFix = approximatedFixesIterator.next();
                for (MarkPassing rounding : localRoundings) {
                    do {
                        if (approximatedFix.getTimePoint().before(rounding.getTimePoint())) {
                            legFixes.add(approximatedFix);
                            approximatedFix = approximatedFixesIterator.hasNext() ? approximatedFixesIterator.next()
                                    : null;
                        } else {
                            break;
                        }
                    } while (approximatedFix != null);
                    if (!legFixes.isEmpty()) {
                        result.add(new LegFixes(legNumber, rounding.getTimePoint(), legFixes));
                    }
                    ++legNumber;
                    legFixes = new ArrayList<>();
                    if (!approximatedFixesIterator.hasNext()) {
                        break;
                    }
                }
                while (approximatedFix != null) {
                    legFixes.add(approximatedFix);
                    approximatedFix = approximatedFixesIterator.hasNext() ? approximatedFixesIterator.next() : null;
                }
                if (!legFixes.isEmpty()) {
                    result.add(new LegFixes(legNumber, null, legFixes));
                }
            } else {
                result.add(new LegFixes(0, null, approximatedFixes));
            }
        }
        return result;
    }

    /**
     * Gets the internal number for a leg sailed by competitor at the provided time point. Number 0 relates to the
     * section, before the start line was crossed. The next higher integer after the number of the last leg refers to
     * the section, after the finish line was passed.
     */
    private int getLegNumberAt(TimePoint timePoint) {
        int legNumber = 0;
        NavigableSet<MarkPassing> roundings = trackedRace.getMarkPassings(competitor);
        if (roundings != null) {
            NavigableSet<MarkPassing> localRoundings = null;
            trackedRace.lockForRead(roundings);
            try {
                localRoundings = new ArrayListNavigableSet<>(roundings.size(), new TimedComparator());
                localRoundings.addAll(roundings);
            } finally {
                trackedRace.unlockAfterRead(roundings);
            }
            for (MarkPassing rounding : localRoundings) {
                if (!rounding.getTimePoint().before(timePoint)) {
                    break;
                }
                ++legNumber;
            }
        }
        return legNumber;
    }

    /**
     * Contains per leg grouped fixes calculated from a douglas peucker calculation. This result is meant to be reused
     * for incremental douglas peucker fixes calculation.
     * 
     * @author Vladislav Chumak (D069712)
     *
     */
    public static class FixesApproximationResult {

        private final TimePoint earliestStart;
        private final TimePoint latestEnd;
        private final GPSFixMoving latestFix;
        private final List<LegFixes> legFixesList;

        public FixesApproximationResult(TimePoint earliestStart, TimePoint latestEnd, GPSFixMoving latestFix,
                List<LegFixes> legFixesList) {
            this.earliestStart = earliestStart;
            this.latestEnd = latestEnd;
            this.latestFix = latestFix;
            this.legFixesList = legFixesList;
        }

        public TimePoint getEarliestStart() {
            return earliestStart;
        }

        public TimePoint getLatestEnd() {
            return latestEnd;
        }

        public GPSFixMoving getLatestFix() {
            return latestFix;
        }

        public List<LegFixes> getLegFixesList() {
            return legFixesList;
        }

    }

    /**
     * Contains fixes which correspond to a certain leg. The corresponding fixes must not be empty. The legs are
     * represented by numbers calculated by {@link #getLegNumberAt(TimePoint)}.
     * 
     * @author Vladislav Chumak (D069712)
     *
     */
    public static class LegFixes {
        private final int legNumber;
        private final Iterable<GPSFixMoving> approximatedFixes;
        private final GPSFixMoving firstApproximatedFix;
        private final GPSFixMoving lastApproximatedFix;
        private final TimePoint timePointOfNextMarkPassing;

        public LegFixes(int legNumber, TimePoint timePointOfNextMarkPassing, Iterable<GPSFixMoving> approximatedFixes) {
            this.legNumber = legNumber;
            this.approximatedFixes = approximatedFixes;
            this.timePointOfNextMarkPassing = timePointOfNextMarkPassing;
            Iterator<GPSFixMoving> iterator = approximatedFixes.iterator();
            GPSFixMoving lastFix = iterator.next();
            this.firstApproximatedFix = lastFix;
            while (iterator.hasNext()) {
                lastFix = iterator.next();
            }
            this.lastApproximatedFix = lastFix;
        }

        public int getLegNumber() {
            return legNumber;
        }

        public Iterable<GPSFixMoving> getApproximatedFixes() {
            return approximatedFixes;
        }

        public GPSFixMoving getFirstApproximatedFix() {
            return firstApproximatedFix;
        }

        public GPSFixMoving getLastApproximatedFix() {
            return lastApproximatedFix;
        }

        public TimePoint getTimePointOfNextMarkPassing() {
            return timePointOfNextMarkPassing;
        }
    }

    @Override
    public void clearState() {
        lastFixesApproximationResult = null;

    }

}
