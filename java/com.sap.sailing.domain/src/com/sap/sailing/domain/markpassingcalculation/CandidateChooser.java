package com.sap.sailing.domain.markpassingcalculation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;

/**
 * The standard implementation of {@link AbstractCandidateChooser}. First two proxy-candidates are created, It makes
 * creates {@link Edge}s out of the {@link Candidate}s for each competitor, creating a DAG. The shortest path from A
 * shortest path-algorithm is then used to find the most likely sequence of {@link MarkPassing}s. An estimation of the
 * time between two {@link Candidate}s and the distance between their {@link Waypoint}s is used to further increase the
 * edges correctness.
 * 
 * @author Nicolas Klose
 * 
 */
public class CandidateChooser implements AbstractCandidateChooser {

    private static final Logger logger = Logger.getLogger(CandidateChooser.class.getName());

    private LinkedHashMap<Competitor, LinkedHashMap<Waypoint, MarkPassing>> currentMarkPasses = new LinkedHashMap<>();
    private LinkedHashMap<Competitor, Set<Edge>> allEdges = new LinkedHashMap<>();
    private LinkedHashMap<Competitor, Set<Candidate>> candidates = new LinkedHashMap<>();
    private TimePoint raceStartTime;
    private Candidate start;
    private Candidate end;
    private final DynamicTrackedRace race;
    private final double penaltyForSkipping = 1 - Edge.getPenaltyForSkipping();
    private static double strictness = 1000;

    public CandidateChooser(DynamicTrackedRace race) {
        logger.setLevel(Level.INFO);
        this.race = race;
        raceStartTime = race.getStartOfRace();
        start = new Candidate(0, raceStartTime, 1, null, true, true, "Proxy");
        end = new Candidate(race.getRace().getCourse().getIndexOfWaypoint(race.getRace().getCourse().getLastWaypoint()) + 2, null, 1, null, true, true, "Proxy");
        candidates = new LinkedHashMap<>();
        for (Competitor c : race.getRace().getCompetitors()) {
            candidates.put(c, new TreeSet<Candidate>());
            currentMarkPasses.put(c, new LinkedHashMap<Waypoint, MarkPassing>());
            allEdges.put(c, new TreeSet<Edge>());
            addCandidates(Arrays.asList(start, end), c);
        }
    }

    @Override
    public LinkedHashMap<Competitor, LinkedHashMap<Waypoint, MarkPassing>> getAllPasses() {
        return currentMarkPasses;
    }

    @Override
    public void calculateMarkPassDeltas(Competitor c, Pair<List<Candidate>, List<Candidate>> candidateDeltas) {
        if (race.getStartOfRace() != raceStartTime) {
            raceStartTime = race.getStartOfRace();
            for (Competitor com : allEdges.keySet()) {
                removeCandidates(Arrays.asList(start), com);
            }
            start = new Candidate(0, raceStartTime, 1, null, true, true, "Proxy");
            for (Competitor com : allEdges.keySet()) {
                addCandidates(Arrays.asList(start), com);
            }
        }
        removeCandidates(candidateDeltas.getB(), c);
        addCandidates(candidateDeltas.getA(), c);
        findShortestPath(c);
    }

    private void createNewEdges(Competitor co, List<Candidate> newCans) {
        for (Candidate newCan : newCans) {
            for (Candidate oldCan : candidates.get(co)) {
                Candidate early = newCan;
                Candidate late = oldCan;
                if (oldCan.getID() < newCan.getID()) {
                    early = oldCan;
                    late = newCan;
                }
                if (raceStartTime != null) {
                    if (late == end && early != end) {
                        allEdges.get(co).add(new Edge(early, late, 1));
                    } else if (!(early.getID() == late.getID()) && !late.getTimePoint().before(early.getTimePoint()) && estimatedDistance(co, early, late) > penaltyForSkipping) {
                        Edge e = new Edge(early, late, estimatedDistance(co, early, late));
                        allEdges.get(co).add(e);
                    }
                } else {
                    /*
                     * if (early == start && late.getID() == 1 && numberOfCloseStarts(late.getTimePoint()) >
                     * penaltyForSkipping) { allEdges.get(co).add(new Edge(early, late,
                     * numberOfCloseStarts(late.getTimePoint()))); } else
                     */if ((late == end || early == start) && early != late) {
                        allEdges.get(co).add(new Edge(early, late, 1));
                    } else if (!(early.getID() == late.getID()) && late.getTimePoint().after(early.getTimePoint()) && estimatedDistance(co, early, late) > penaltyForSkipping) {
                        allEdges.get(co).add(new Edge(early, late, estimatedDistance(co, early, late)));
                    }
                }
            }
        }
    }

    private void findShortestPath(Competitor co) {
        boolean changed = false;
        ArrayList<Edge> all = new ArrayList<>();
        for (Edge e : allEdges.get(co)) {
            all.add(e);
        }
        LinkedHashMap<Candidate, Pair<Candidate, Double>> candidateWithParent = new LinkedHashMap<>();
        candidateWithParent.put(start, new Pair<Candidate, Double>(null, 0.0));
        Pair<Edge, Double> currentMostLikelyEdge = null;
        while (!candidateWithParent.containsKey(end)) {
            currentMostLikelyEdge = null;
            for (Edge e : all) {
                if (candidateWithParent.containsKey(e.getStart())) {
                    Double cost = candidateWithParent.get(e.getStart()).getB() + e.getProbability();
                    if (currentMostLikelyEdge == null) {
                        currentMostLikelyEdge = new Pair<Edge, Double>(e, cost);
                    } else if (cost < currentMostLikelyEdge.getB()) {
                        currentMostLikelyEdge = new Pair<Edge, Double>(e, cost);
                    }
                }
            }
            if (!candidateWithParent.containsKey(currentMostLikelyEdge.getA().getEnd())
                    || candidateWithParent.get(currentMostLikelyEdge.getA().getEnd()).getB() > currentMostLikelyEdge.getB()) {
                candidateWithParent.put(currentMostLikelyEdge.getA().getEnd(), new Pair<Candidate, Double>(currentMostLikelyEdge.getA().getStart(), currentMostLikelyEdge.getB()));
            }
            all.remove(currentMostLikelyEdge.getA());
        }
        Candidate marker = candidateWithParent.get(end).getA();
        while (!(marker == start)) {
            if (currentMarkPasses.get(co).get(marker.getWaypoint()) == null || currentMarkPasses.get(co).get(marker.getWaypoint()).getTimePoint() != marker.getTimePoint()) {
                currentMarkPasses.get(co).put(marker.getWaypoint(), new MarkPassingImpl(marker.getTimePoint(), marker.getWaypoint(), co));
                changed = true;
            }
            marker = candidateWithParent.get(marker).getA();
        }
        if (changed) {
            logger.info("New MarkPasses for " + co);
            List<MarkPassing> markPassDeltas = new ArrayList<>();
            for (MarkPassing m : currentMarkPasses.get(co).values()) {
                markPassDeltas.add(m);
            }
            race.updateMarkPassings(co, markPassDeltas);
        }
    }

    @SuppressWarnings("unused")
    private void reEvaluateStartingEdges() {
        for (Competitor c : candidates.keySet()) {
            ArrayList<Edge> newEdges = new ArrayList<>();
            ArrayList<Edge> edgesToRemove = new ArrayList<>();
            for (Edge e : allEdges.get(c)) {
                if (e.getStart().getID() == 0 && e.getEnd().getID() == 1) {
                    newEdges.add(new Edge(e.getStart(), e.getEnd(), numberOfCloseStarts(e.getEnd().getTimePoint())));
                    edgesToRemove.add(e);
                }
            }
            for (Edge e : edgesToRemove) {
                allEdges.get(c).remove(e);
            }
            for (Edge e : newEdges) {
                allEdges.get(c).add(e);
            }
        }
    }

    private double numberOfCloseStarts(TimePoint t) {
        double numberOfCompetitors = 0;
        double totalTimeDifference = 0;
        for (Competitor c : candidates.keySet()) {
            double closestCandidate = -1;
            for (Candidate ca : candidates.get(c)) {
                if (ca.getID() == 1) {
                    if (closestCandidate == -1) {
                        closestCandidate = Math.abs(ca.getTimePoint().asMillis() - t.asMillis());
                    } else if (closestCandidate > Math.abs(ca.getTimePoint().asMillis() - t.asMillis())) {
                        closestCandidate = Math.abs(ca.getTimePoint().asMillis() - t.asMillis());
                    }
                }
            }
            if (closestCandidate != -1) {
                numberOfCompetitors++;
                totalTimeDifference = totalTimeDifference + closestCandidate;
            }
        }
        return 1 / (0.00001 * (totalTimeDifference / numberOfCompetitors) + 1);
    }

    private double estimatedDistance(Competitor c, Candidate c1, Candidate c2) {
        Distance totalEstimatedDistance = new MeterDistance(0);
        Waypoint current;
        if (c1.getID() == 0) {
            current = race.getRace().getCourse().getFirstWaypoint();
        } else {
            current = c1.getWaypoint();
        }
        while (current != c2.getWaypoint()) {
            TrackedLeg leg = race.getTrackedLegStartingAt(current);
            totalEstimatedDistance = totalEstimatedDistance.add(estimatedDistanceOnLeg(leg,
                    c1.getTimePoint().plus(1 / 2 * c2.getTimePoint().minus(c1.getTimePoint().asMillis()).asMillis())));
            current = leg.getLeg().getTo();
        }
        Distance actualDistance = race.getTrack(c).getDistanceTraveled(c1.getTimePoint(), c2.getTimePoint());
        double differenceInMeters = totalEstimatedDistance.getMeters() - actualDistance.getMeters();
        double sigmaSquared = 1 / (2 * Math.PI);
        double factor = 1 / (Math.sqrt(sigmaSquared * 2 * Math.PI));
        double exponent = -(Math.pow(differenceInMeters / strictness, 2) / (2 * sigmaSquared));
        double result = factor * Math.pow(Math.E, exponent);
        return result;
    }

    private Distance estimatedDistanceOnLeg(TrackedLeg leg, TimePoint t) {
        try {
            if (leg.getLegType(t) == LegType.DOWNWIND) {
                return leg.getGreatCircleDistance(t).scale(1.2);
            }
            if (leg.getLegType(t) == LegType.UPWIND) {
                return leg.getGreatCircleDistance(t).scale(Math.sqrt(2));
            }
        } catch (NoWindException e) {
            Logger.getLogger(CandidateChooser.class.getName()).log(Level.SEVERE,
                    "CandidateChooser threw exception " + e.getMessage() + " while estimating the Time between two Candidates.");
        }
        return leg.getGreatCircleDistance(t).scale(1.3);
    }

    private void addCandidates(List<Candidate> newCandidates, Competitor co) {
        for (Candidate c : newCandidates) {
            if (!candidates.get(co).contains(c)) {
                candidates.get(co).add(c);
            }
            /*
             * if (c.getID() == 1 && race.getStartOfRace() == null) { reEvaluateStartingEdges(); }
             */
        }
        createNewEdges(co, newCandidates);
    }

    private void removeCandidates(List<Candidate> wrongCandidates, Competitor co) {
        for (Candidate c : wrongCandidates) {
            candidates.get(co).remove(c);
            List<Edge> toRemove = new ArrayList<>();
            for (Edge e : allEdges.get(co)) {
                if (e.getStart().equals(c) || e.getEnd().equals(c)) {
                    toRemove.add(e);
                }
            }
            for (Edge e : toRemove) {
                allEdges.get(co).remove(e);
            }
        }
    }
}
