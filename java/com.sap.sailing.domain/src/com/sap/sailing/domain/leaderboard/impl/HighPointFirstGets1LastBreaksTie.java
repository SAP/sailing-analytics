package com.sap.sailing.domain.leaderboard.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.NumberOfCompetitorsInLeaderboardFetcher;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.util.graph.DirectedEdge;
import com.sap.sse.util.graph.DirectedGraph;
import com.sap.sse.util.topologicalordering.TopologicalComparator;

/**
 * The special thing about this scoring scheme is its tie-breaking rule. If two competitors have equal score,
 * instead of doing what sometimes is referred to as a "count-back" (sorting races from best to worst score,
 * then comparing to same sort order of other competitor and looking for first difference) this scheme
 * uses a graph whose edges consist of the "won over in same race" relation where competitors matched
 * directly. A topological order will be tried to compute, but cycles may exist (A wins against B, B wins against
 * C, C wins against A). In this case, competitors on "strongly-connected components" (node sets in the graph
 * in which each node is reachable from each other node in that set) will be ordered by their maximum distance
 * from a root node.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class HighPointFirstGets1LastBreaksTie extends HighPointFirstGetsFixedScore {
    private static final long serialVersionUID = 1L;

    public HighPointFirstGets1LastBreaksTie() {
        super(/* score for race winner */ 1.0, /* minimum score from rank */ 0.0);
    }

    @Override
    public ScoringSchemeType getType() {
        return ScoringSchemeType.HIGH_POINT_FIRST_GETS_ONE;
    }

    @Override
    public Double getPenaltyScore(RaceColumn raceColumn, Competitor competitor, MaxPointsReason maxPointsReason, Integer numberOfCompetitorsInRace,
            NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsInLeaderboardFetcher, TimePoint timePoint, Leaderboard leaderboard, Supplier<Double> uncorrectedScoreProvider) {
        return -1.0;
    }

    @Override
    public int compareByBetterScore(Competitor o1, List<Util.Pair<RaceColumn, Double>> o1Scores, Competitor o2,
            List<Util.Pair<RaceColumn, Double>> o2Scores, Iterable<RaceColumn> raceColumnsToConsider, boolean nullScoresAreBetter, TimePoint timePoint, Leaderboard leaderboard, Map<Competitor, Set<RaceColumn>> discardedRaceColumnsPerCompetitor, BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        // Construct a graph in which all competitors with equal points are represented as nodes;
        // edges represent a RaceColumn/Fleet where the "from" node in the edge has won over the "to"
        // node in the edge. This graph will be sorted such that competitors precede those over which
        // they won, except if there is a cycle in the graph in which case competitors on the cycle
        // will be ranked equal by this method, leaving the decision to other criteria
        final Double o1NetPoints = leaderboard.getNetPoints(o1, raceColumnsToConsider, timePoint);
        final Double o2NetPoints = leaderboard.getNetPoints(o2, raceColumnsToConsider, timePoint);
        assert Math.abs(o1NetPoints-o2NetPoints) < 0.00001;
        final Set<Competitor> nodesInGraph = new HashSet<>();
        nodesInGraph.add(o1);
        nodesInGraph.add(o2);
        for (final Competitor c : leaderboard.getCompetitors()) {
            if (leaderboard.getNetPoints(c, raceColumnsToConsider, timePoint).equals(o1NetPoints)) {
                nodesInGraph.add(c);
            }
        }
        final Set<DirectedEdge<Competitor>> edges = constructEdges(nodesInGraph, leaderboard, raceColumnsToConsider, timePoint, nullScoresAreBetter);
        final DirectedGraph<Competitor> graph = DirectedGraph.create(nodesInGraph, edges);
        final TopologicalComparator<Competitor> comparator = new TopologicalComparator<>(graph);
        final int resultBasedOnGraphSort = comparator.compare(o1, o2);
        // if o1 and o2 competed more than once in one race with contradicting outcomes (o1 better than o2 in at
        // least one race, and o2 better than o1 in at least one race) and are not part of a bigger cycle,
        // decide based on direct match score, breaking tie by last direct match between them:
        final int result;
        if (resultBasedOnGraphSort == 0 && graph.areOnSameCycleCluster(o1, o2) && graph.getCycleCluster(o1).getClusterNodes().size() == 2) {
            result = compareByDirectComparison(o1Scores, o1, o2Scores, o2, nullScoresAreBetter, timePoint);
        } else {
            result = resultBasedOnGraphSort;
        }
        return result;
    }

    private int compareByDirectComparison(List<Pair<RaceColumn, Double>> o1Scores, Competitor o1,
            List<Pair<RaceColumn, Double>> o2Scores, Competitor o2, boolean nullScoresAreBetter, TimePoint timePoint) {
        int lastDirectComparison = 0;
        final Comparator<Double> pureScoreComparator = getScoreComparator(nullScoresAreBetter);
        Iterator<Pair<RaceColumn, Double>> o1Iter = o1Scores.iterator();
        double o1DirectComparisonScore = 0.0;
        double o2DirectComparisonScore = 0.0;
        while (o1Iter.hasNext()) {
            final Pair<RaceColumn, Double> o1Result = o1Iter.next();
            Iterator<Pair<RaceColumn, Double>> o2Iter = o2Scores.iterator();
            while (o2Iter.hasNext()) {
                final Pair<RaceColumn, Double> o2Result = o2Iter.next();
                if (o2Result.getA() == o1Result.getA() &&
                        o2Result.getA().getFleetOfCompetitor(o2) == o1Result.getA().getFleetOfCompetitor(o1)) {
                    o1DirectComparisonScore += o1Result.getB();
                    o2DirectComparisonScore += o2Result.getB();
                    lastDirectComparison = pureScoreComparator.compare(o1Result.getB(), o2Result.getB());
                    break;
                }
            }
        }
        return o1DirectComparisonScore == o2DirectComparisonScore ? lastDirectComparison : pureScoreComparator.compare(o1DirectComparisonScore, o2DirectComparisonScore);
    }

    /**
     * Finds all races ({@link RaceColumn}/{@link Fleet} combinations) where more than one competitor from
     * {@code nodesInGraph} competed, and adds an edge to the result for each pair of competitors in that same
     * race, leading from the better to the worse competitor.
     */
    private Set<DirectedEdge<Competitor>> constructEdges(Set<Competitor> nodesInGraph, Leaderboard leaderboard,
            Iterable<RaceColumn> raceColumnsToConsider, TimePoint timePoint, boolean nullScoresAreBetter) {
        final Comparator<Double> pureScoreComparator = getScoreComparator(nullScoresAreBetter);
        final Set<DirectedEdge<Competitor>> edges = new HashSet<>();
        for (final RaceColumn raceColumn : raceColumnsToConsider) {
            for (final Fleet fleet : raceColumn.getFleets()) {
                final Iterable<Competitor> competitorsInRace = leaderboard.getCompetitors(raceColumn, fleet);
                final Set<Competitor> equalRankedCompetitorsInRace = new HashSet<>();
                Util.addAll(competitorsInRace, equalRankedCompetitorsInRace);
                equalRankedCompetitorsInRace.retainAll(nodesInGraph);
                final List<Competitor> competitorsInRaceOrderedByScoreInRace = new ArrayList<>(equalRankedCompetitorsInRace);
                Collections.sort(competitorsInRaceOrderedByScoreInRace, (c1, c2) -> {
                    return pureScoreComparator.compare(leaderboard.getTotalPoints(c1, raceColumn, timePoint),
                            leaderboard.getTotalPoints(c2, raceColumn, timePoint));
                });
                for (int i=0; i<competitorsInRaceOrderedByScoreInRace.size(); i++) {
                    for (int j=i+1; j<competitorsInRaceOrderedByScoreInRace.size(); j++) {
                        // add an edge only if the score comparison result is non-zero
                        if (pureScoreComparator.compare(leaderboard.getTotalPoints(competitorsInRaceOrderedByScoreInRace.get(i), raceColumn, timePoint),
                                                        leaderboard.getTotalPoints(competitorsInRaceOrderedByScoreInRace.get(j), raceColumn, timePoint)) < 0) {
                            edges.add(DirectedEdge.create(competitorsInRaceOrderedByScoreInRace.get(i), competitorsInRaceOrderedByScoreInRace.get(j)));
                        }
                    }
                }
            }
        }
        return edges;
    }

    @Override
    public int compareByLastRace(List<Pair<RaceColumn, Double>> o1ScoresIncludingDiscarded,
            List<Pair<RaceColumn, Double>> o2ScoresIncludingDiscarded, boolean nullScoresAreBetter, Competitor o1,
            Competitor o2, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return 0;
    }
}
