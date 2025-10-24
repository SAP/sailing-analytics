package com.sap.sailing.domain.abstractlog.race.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.AbstractLog;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.shared.tracking.impl.TimeRangeCache;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.scalablevalue.ScalableValue;

/**
 * Wrapper for a {@link RaceLog} which will ignore all calls trying to add an {@link RaceLogEvent}. All other
 * functionality redirects to the wrapper {@link RaceLog}.
 */
public class NoAddingRaceLogWrapper implements RaceLog {
    private static final long serialVersionUID = -5145744179072564993L;

    private static final Logger logger = Logger.getLogger(NoAddingRaceLogWrapper.class.getSimpleName());

    private final RaceLog innerRaceLog;

    public NoAddingRaceLogWrapper(RaceLog innerRaceLog) {
        this.innerRaceLog = innerRaceLog;
    }

    @Override
    public boolean add(RaceLogEvent event) {
        logger.log(Level.FINEST, "add() called; event will not be added.");
        return false;
    }

    @Override
    public Iterable<RaceLogEvent> add(RaceLogEvent event, UUID clientId) {
        logger.log(Level.FINEST, "add() called; event will not be added.");
        return Collections.emptyList();
    }

    @Override
    public void addListener(RaceLogEventVisitor listener) {
        innerRaceLog.addListener(listener);
    }

    @Override
    public void removeListener(RaceLogEventVisitor listener) {
        innerRaceLog.removeListener(listener);
    }

    @Override
    public void addAllListeners(Iterable<RaceLogEventVisitor> listeners) {
        innerRaceLog.addAllListeners(listeners);
    }

    @Override
    public HashSet<RaceLogEventVisitor> removeAllListeners() {
        return innerRaceLog.removeAllListeners();
    }

    @Override
    public Iterable<RaceLogEventVisitor> getAllListeners() {
        return innerRaceLog.getAllListeners();
    }

    @Override
    public void lockForRead() {
        innerRaceLog.lockForRead();
    }

    @Override
    public void unlockAfterRead() {
        innerRaceLog.unlockAfterRead();
    }

    @Override
    public Iterable<RaceLogEvent> getFixes() {
        return innerRaceLog.getFixes();
    }

    @Override
    public Iterable<RaceLogEvent> getRawFixes() {
        return innerRaceLog.getRawFixes();
    }

    @Override
    public RaceLogEvent getLastFixAtOrBefore(TimePoint timePoint) {
        return innerRaceLog.getLastFixAtOrBefore(timePoint);
    }

    @Override
    public RaceLogEvent getLastFixBefore(TimePoint timePoint) {
        return innerRaceLog.getLastFixBefore(timePoint);
    }

    @Override
    public RaceLogEvent getLastRawFixAtOrBefore(TimePoint timePoint) {
        return innerRaceLog.getLastRawFixAtOrBefore(timePoint);
    }

    @Override
    public RaceLogEvent getFirstFixAtOrAfter(TimePoint timePoint) {
        return innerRaceLog.getFirstFixAtOrAfter(timePoint);
    }

    @Override
    public RaceLogEvent getFirstRawFixAtOrAfter(TimePoint timePoint) {
        return innerRaceLog.getFirstRawFixAtOrAfter(timePoint);
    }

    @Override
    public RaceLogEvent getLastRawFixBefore(TimePoint timePoint) {
        return innerRaceLog.getLastRawFixBefore(timePoint);
    }

    @Override
    public RaceLogEvent getFirstRawFixAfter(TimePoint timePoint) {
        return innerRaceLog.getFirstRawFixAfter(timePoint);
    }

    @Override
    public RaceLogEvent getFirstFixAfter(TimePoint timePoint) {
        return innerRaceLog.getFirstFixAfter(timePoint);
    }

    @Override
    public RaceLogEvent getFirstRawFix() {
        return innerRaceLog.getFirstRawFix();
    }

    @Override
    public RaceLogEvent getLastRawFix() {
        return innerRaceLog.getLastRawFix();
    }

    @Override
    public Iterator<RaceLogEvent> getFixesIterator(TimePoint startingAt, boolean inclusive) {
        return innerRaceLog.getFixesIterator(startingAt, inclusive);
    }

    @Override
    public Iterator<RaceLogEvent> getFixesIterator(TimePoint startingAt, boolean startingAtInclusive,
            TimePoint endingAt, boolean endingAtInclusive) {
        return innerRaceLog.getFixesIterator(startingAt, startingAtInclusive, endingAt, endingAtInclusive);
    }

    @Override
    public Iterator<RaceLogEvent> getRawFixesIterator(TimePoint startingAt, boolean inclusive) {
        return innerRaceLog.getRawFixesIterator(startingAt, inclusive);
    }

    @Override
    public Iterator<RaceLogEvent> getRawFixesIterator(TimePoint startingAt, boolean startingAtInclusive,
            TimePoint endingAt, boolean endingAtInclusive) {
        return innerRaceLog.getRawFixesIterator(startingAt, startingAtInclusive, endingAt, endingAtInclusive);
    }

    @Override
    public Serializable getId() {
        return innerRaceLog;
    }

    @Override
    public int getCurrentPassId() {
        return innerRaceLog.getCurrentPassId();
    }

    @Override
    public boolean isEmpty() {
        return innerRaceLog.isEmpty();
    }

    @Override
    public Iterable<RaceLogEvent> getRawFixesDescending() {
        return innerRaceLog.getRawFixesDescending();
    }

    @Override
    public Iterable<RaceLogEvent> getFixesDescending() {
        return innerRaceLog.getFixesDescending();
    }

    @Override
    public Iterable<RaceLogEvent> getRawFixes(UUID clientId) {
        return innerRaceLog.getRawFixes(clientId);
    }

    @Override
    public boolean load(RaceLogEvent event) {
        return innerRaceLog.load(event);
    }

    @Override
    public Iterator<RaceLogEvent> getFixesDescendingIterator(TimePoint startingAt, boolean inclusive) {
        return innerRaceLog.getFixesDescendingIterator(startingAt, inclusive);
    }

    @Override
    public Iterator<RaceLogEvent> getRawFixesDescendingIterator(TimePoint startingAt, boolean inclusive) {
        return innerRaceLog.getRawFixesDescendingIterator(startingAt, inclusive);
    }

    @Override
    public Iterable<RaceLogEvent> getEventsToDeliver(UUID clientId) {
        return innerRaceLog.getEventsToDeliver(clientId);
    }

    @Override
    public Iterable<RaceLogEvent> getFixes(TimePoint from, boolean fromInclusive, TimePoint to, boolean toInclusive) {
        return innerRaceLog.getFixes(from, fromInclusive, to, toInclusive);
    }

    @Override
    public NavigableSet<RaceLogEvent> getUnrevokedEvents() {
        return innerRaceLog.getUnrevokedEvents();
    }

    @Override
    public Duration getAverageIntervalBetweenFixes() {
        return innerRaceLog.getAverageIntervalBetweenFixes();
    }

    @Override
    public Duration getAverageIntervalBetweenRawFixes() {
        return innerRaceLog.getAverageIntervalBetweenRawFixes();
    }

    @Override
    public NavigableSet<RaceLogEvent> getUnrevokedEventsDescending() {
        return innerRaceLog.getUnrevokedEventsDescending();
    }
    
    public RaceLogEvent getEventById(Serializable id) {
        return innerRaceLog.getEventById(id);
    }

    @Override
    public void merge(AbstractLog<RaceLogEvent, RaceLogEventVisitor> other) {
        innerRaceLog.merge(other);
    }

    @Override
    public void revokeEvent(AbstractLogEventAuthor author, RaceLogEvent toRevoke, String reason)
            throws NotRevokableException {
        innerRaceLog.revokeEvent(author, toRevoke, reason);
    }

    @Override
    public void revokeEvent(AbstractLogEventAuthor author, RaceLogEvent toRevoke) throws NotRevokableException {
        innerRaceLog.revokeEvent(author, toRevoke);
    }

    @Override
    public <InternalType, ValueType> ValueType getInterpolatedValue(TimePoint timePoint,
            Function<RaceLogEvent, ScalableValue<InternalType, ValueType>> converter) {
        return innerRaceLog.getInterpolatedValue(timePoint, converter);
    }
    
    @Override
    public int size() {
        return innerRaceLog.size();
    }

    @Override
    public <T> T getValueSum(TimePoint from, TimePoint to, T nullElement,
            com.sap.sailing.domain.shared.tracking.Track.Adder<T> adder, TimeRangeCache<T> cache,
            com.sap.sailing.domain.shared.tracking.Track.TimeRangeValueCalculator<T> valueCalculator) {
        return innerRaceLog.getValueSum(from, to, nullElement, adder, cache, valueCalculator);
    }
}
