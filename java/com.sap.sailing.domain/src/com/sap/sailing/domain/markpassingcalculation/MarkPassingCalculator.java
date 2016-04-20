package com.sap.sailing.domain.markpassingcalculation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateChooserImpl;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateFinderImpl;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.util.impl.ThreadFactoryWithPriority;

/**
 * Calculates the {@link MarkPassing}s for a {@link DynamicTrackedRace} using an {@link CandidateFinder} and an
 * {@link CandidateChooser}. The finder evaluates the fixes and finds possible MarkPassings as {@link Candidate}s . The
 * chooser than finds the most likely sequence of {@link Candidate}s and updates the race with new {@link MarkPassing}s
 * for this sequence. Upon calling the constructor {@link #MarkPassingCalculator(DynamicTrackedRace, boolean, boolean)} this
 * happens for the current state of the race. In addition, for live races, the <code>listen</code> parameter of the
 * constructor should be true. Then a {@link MarkPassingUpdateListener} is initialized which puts new fixes into a queue
 * as {@link StorePositionUpdateStrategy}. A new thread will also be started to evaluate the new fixes (See
 * {@link CandidateFinder} and {@link CandidateChooser}). This continues until the {@link MarkPassingUpdateListener}
 * signals that the race is over (after {@link #stop()} is called.
 * 
 * @author Nicolas Klose
 * 
 */
public class MarkPassingCalculator {
    private final DynamicTrackedRace race;
    private CandidateFinder finder;
    private CandidateChooser chooser;
    private static final Logger logger = Logger.getLogger(MarkPassingCalculator.class.getName());
    private final MarkPassingUpdateListener listener;
    private final static ExecutorService executor = new ThreadPoolExecutor(/* corePoolSize */Math.max(Runtime
            .getRuntime().availableProcessors() - 1, 3),
    /* maximumPoolSize */Math.max(Runtime.getRuntime().availableProcessors() - 1, 3),
    /* keepAliveTime */60, TimeUnit.SECONDS,
    /* workQueue */new LinkedBlockingQueue<Runnable>(), new ThreadFactoryWithPriority(Thread.NORM_PRIORITY - 1, /* daemon */ true));

    private boolean suspended = false;

    public MarkPassingCalculator(DynamicTrackedRace race, boolean listen, boolean waitForInitialMarkPassingCalculation) {
        if (listen) {
            listener = new MarkPassingUpdateListener(race);
        } else {
            listener = null;
        }
        this.race = race;
        finder = new CandidateFinderImpl(race);
        chooser = new CandidateChooserImpl(race);
        Thread t = new Thread(() -> {
            for (Competitor c : race.getRace().getCompetitors()) {
                Util.Pair<Iterable<Candidate>, Iterable<Candidate>> allCandidates = finder.getAllCandidates(c);
                chooser.calculateMarkPassDeltas(c, allCandidates.getA(), allCandidates.getB());
            }
            if (listen) {
                final Thread listenerThread = new Thread(new Listen(race.getRace().getName()), "MarkPassingCalculator for race " + race.getRace().getName());
                listenerThread.setDaemon(true);
                listenerThread.start();
            }
        }, "MarkPassingCalculator for race "+race.getRace().getName()+" initialization");
        if (waitForInitialMarkPassingCalculation) {
            t.run();
        } else {
            t.start();
        }
    }

    /**
     * Waits until an object is in the queue, then drains it entirely. After that the information is sorted depending on
     * whether it is a fix, an updated waypoint or an updated fixed markpassing. Finally, if <code>suspended</code> is
     * false, new passing are computed. First new wayponts are evaluated, than new fixed markpassings. Than the
     * markpassings are computed using mark and competitor fixes.
     * 
     * @author Nicolas Klose
     * 
     */
    private class Listen implements Runnable {
        private final String raceName;
        
        public Listen(String raceName) {
            this.raceName = raceName;
        }

        @Override
        public void run() {
            try {
                logger.info("MarkPassingCalculator is listening on race "+raceName);
                boolean finished = false;
                Map<Competitor, List<GPSFix>> competitorFixes = new HashMap<>();
                Map<Mark, List<GPSFix>> markFixes = new HashMap<>();
                List<Waypoint> addedWaypoints = new ArrayList<>();
                List<Waypoint> removedWaypoints = new ArrayList<>();
                Integer smallestChangedWaypointIndex = null;
                List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings = new ArrayList<>();
                List<Pair<Competitor, Integer>> removedFixedMarkPassings = new ArrayList<>();
                List<Pair<Competitor, Integer>> suppressedMarkPassings = new ArrayList<>();
                List<Competitor> unsuppressedMarkPassings = new ArrayList<>();
                while (!finished) {
                    try {
                        logger.finer("MPC for "+raceName+" is checking the queue");
                        List<StorePositionUpdateStrategy> allNewFixInsertions = new ArrayList<>();
                        try {
                            allNewFixInsertions.add(listener.getQueue().take());
                        } catch (InterruptedException e) {
                            logger.log(Level.SEVERE, "MarkPassingCalculator for "+raceName+" threw exception " + e.getMessage()
                                    + " while waiting for new GPSFixes");
                        }
                        listener.getQueue().drainTo(allNewFixInsertions);
                        logger.finer("MPC for "+raceName+" recieved "+ allNewFixInsertions.size()+" new updates.");
                        for (StorePositionUpdateStrategy fixInsertion : allNewFixInsertions) {
                            if (listener.isEndMarker(fixInsertion)) {
                                logger.info("Stopping "+MarkPassingCalculator.this+"'s listener for race "+raceName);
                                finished = true;
                                break;
                            } else {
                                fixInsertion.storePositionUpdate(competitorFixes, markFixes, addedWaypoints, removedWaypoints,
                                        smallestChangedWaypointIndex, fixedMarkPassings, removedFixedMarkPassings,
                                        suppressedMarkPassings, unsuppressedMarkPassings);
                            }
                        }
                        if (!finished && !suspended) {
                            if (smallestChangedWaypointIndex != null) {
                                Map<Competitor, Util.Pair<List<Candidate>, List<Candidate>>> candidateDeltas = finder
                                        .updateWaypoints(addedWaypoints, removedWaypoints, smallestChangedWaypointIndex);
                                chooser.removeWaypoints(removedWaypoints);
                                chooser.addWaypoints(addedWaypoints);
                                for (Entry<Competitor, Util.Pair<List<Candidate>, List<Candidate>>> entry : candidateDeltas
                                        .entrySet()) {
                                    Util.Pair<List<Candidate>, List<Candidate>> pair = entry.getValue();
                                    chooser.calculateMarkPassDeltas(entry.getKey(), pair.getA(), pair.getB());
                                }
                            }
                            updateManuallySetMarkPassings(fixedMarkPassings, removedFixedMarkPassings, suppressedMarkPassings,
                                    unsuppressedMarkPassings);
                            computeMarkPasses(competitorFixes, markFixes);
                            competitorFixes.clear();
                            markFixes.clear();
                            addedWaypoints.clear();
                            removedWaypoints.clear();
                            smallestChangedWaypointIndex = null;
                            fixedMarkPassings.clear();
                            removedFixedMarkPassings.clear();
                            suppressedMarkPassings.clear();
                            unsuppressedMarkPassings.clear();
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error while calculating markpassings for race "+raceName+": " + e.getMessage(), e);
                    }
                }
            } finally {
                logger.info("MarkPassingCalculator Listen thread terminating for race "+raceName);
            }
        }

        private void updateManuallySetMarkPassings(List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings,
                List<Pair<Competitor, Integer>> removedMarkPassings,
                List<Pair<Competitor, Integer>> suppressedMarkPassings, List<Competitor> unsuppressedMarkPassings) {
            logger.finest("Updating manually edited MarkPassings for race "+raceName);
            for (Pair<Competitor, Integer> pair : suppressedMarkPassings) {
                chooser.suppressMarkPassings(pair.getA(), pair.getB());
            }
            for (Competitor c : unsuppressedMarkPassings) {
                chooser.stopSuppressingMarkPassings(c);
            }

            for (Pair<Competitor, Integer> pair : removedMarkPassings) {
                chooser.removeFixedPassing(pair.getA(), pair.getB());
            }
            for (Triple<Competitor, Integer, TimePoint> triple : fixedMarkPassings) {
                chooser.setFixedPassing(triple.getA(), triple.getB(), triple.getC());
            }
        }

        /**
         * The calculation has two steps. For every mark with new fixes those competitor fixes are calculated that may
         * have changed their status as a {@link Candidate} (see {@code FixesAffectedByNewMarkFixes}). These fixes,
         * along with any new competitor fixes, are passed into the executor, one task per competitor. The
         * {@link CandidateFinder} uses the fixes to calculate any new or wrong Candidates. These are passed to the
         * {@link CandidateChooser} to calculate any new {@link MarkPassing}s (see {@link ComputeMarkPassings}).
         * 
         */
        private void computeMarkPasses(Map<Competitor, List<GPSFix>> newCompetitorFixes,
                Map<Mark, List<GPSFix>> newMarkFixes) {
            logger.finer("Calculating markpassings for race "+raceName+" with " + newCompetitorFixes.size() + " new competitor Fixes and "
                    + newMarkFixes.size() + "new mark fixes.");
            Map<Competitor, Set<GPSFix>> combinedCompetitorFixes = new HashMap<>();

            for (Entry<Competitor, List<GPSFix>> competitorEntry : newCompetitorFixes.entrySet()) {
                Set<GPSFix> fixesForCompetitor = new HashSet<>();
                combinedCompetitorFixes.put(competitorEntry.getKey(), fixesForCompetitor);
                fixesForCompetitor.addAll(competitorEntry.getValue());
            }
            if (!newMarkFixes.isEmpty()) {
                // FIXME bug 2745 use new mark fixes to invalidate chooser's mark position and mutual mark/waypoint distance cache
                for (Entry<Competitor, List<GPSFix>> fixesAffectedByNewMarkFixes : finder
                        .calculateFixesAffectedByNewMarkFixes(newMarkFixes).entrySet()) {
                    Set<GPSFix> fixes = combinedCompetitorFixes.get(fixesAffectedByNewMarkFixes.getKey());
                    if (fixes == null) {
                        fixes = new HashSet<>();
                        combinedCompetitorFixes.put(fixesAffectedByNewMarkFixes.getKey(), fixes);
                    }
                    fixes.addAll(fixesAffectedByNewMarkFixes.getValue());
                }
            }
            List<Callable<Object>> tasks = new ArrayList<>();
            for (Entry<Competitor, Set<GPSFix>> c : combinedCompetitorFixes.entrySet()) {
                tasks.add(Executors.callable(new ComputeMarkPassings(c.getKey(), c.getValue())));
            }
            try {
                executor.invokeAll(tasks);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private class ComputeMarkPassings implements Runnable {
            Competitor c;
            Iterable<GPSFix> fixes;

            public ComputeMarkPassings(Competitor c, Iterable<GPSFix> fixes) {
                this.c = c;
                this.fixes = fixes;
            }

            @Override
            public void run() {
                logger.finer("Calculating MarkPassings for race "+raceName+", competitor " + c + " (" + Util.size(fixes) + " new fixes)");
                Util.Pair<Iterable<Candidate>, Iterable<Candidate>> candidateDeltas = finder.getCandidateDeltas(c, fixes);
                logger.finer("Received " + Util.size(candidateDeltas.getA()) + " new Candidates for race "+raceName+
                        " and competitor "+c+" and will remove "
                        + Util.size(candidateDeltas.getB()) + " old Candidates for " + c);
                chooser.calculateMarkPassDeltas(c, candidateDeltas.getA(), candidateDeltas.getB());
            }
        }

    }

    /**
     * Only suspends the actual calculation. Even when suspended all incoming fixes are added to the queue and sorted.
     */
    public void suspend() {
        logger.finest("Suspended MarkPassingCalculator");
        suspended = true;
    }

    /**
     * An empty object is written to the queue to ensure that any fixes that have been removed from the queue are
     * evaluated even if nothing else arrives after this is called.
     */
    public void resume() {
        logger.finest("Resumed MarkPassingCalculator");
        suspended = false;
        listener.getQueue().add(new StorePositionUpdateStrategy() {
            @Override
            public void storePositionUpdate(Map<Competitor, List<GPSFix>> competitorFixes,
                    Map<Mark, List<GPSFix>> markFixes, List<Waypoint> addedWaypoints, List<Waypoint> removedWaypoints,
                    Integer smallestChangedWaypointIndex,
                    List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings,
                    List<Pair<Competitor, Integer>> removedMarkPassings,
                    List<Pair<Competitor, Integer>> suppressedMarkPassings, List<Competitor> unSuppressedMarkPassings) {
            }
        });
    }

    public void stop() {
        listener.stop();
    }

    public void recalculateEverything() {
        finder = new CandidateFinderImpl(race);
        chooser = new CandidateChooserImpl(race);
        for (Competitor c : race.getRace().getCompetitors()) {
            Util.Pair<Iterable<Candidate>, Iterable<Candidate>> allCandidates = finder.getAllCandidates(c);
            chooser.calculateMarkPassDeltas(c, allCandidates.getA(), allCandidates.getB());
        }
    }

    public MarkPassingUpdateListener getListener() {
        return listener;
    }
}