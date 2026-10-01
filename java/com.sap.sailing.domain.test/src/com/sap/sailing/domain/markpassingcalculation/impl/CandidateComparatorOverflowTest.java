package com.sap.sailing.domain.markpassingcalculation.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * Reproduces, in isolation, the defect that pinned all background-executor threads on the "my" server: the
 * {@code CandidateChooserImpl.StartAndEndAwareTimeBasedCandidateComparator} compared candidate time points with
 * {@code (int) (o1.getTimePoint().asMillis() - o2.getTimePoint().asMillis())}. Epoch milliseconds are on the order
 * of 1.7e12, so for candidates more than {@link Integer#MAX_VALUE} ms (~24.8 days) apart the 64-bit difference is
 * truncated to 32 bits and its sign can flip, turning the comparator into a non-total order.
 * <p>
 * These tests depend on no domain type; they exercise the exact arithmetic against plain {@code long} epoch
 * milliseconds so the mechanism is proven deterministically and the test doubles as a regression guard.
 * {@link #BUGGY} is the original expression; {@link #FIXED} is {@link Long#compare(long, long)}.
 */
public class CandidateComparatorOverflowTest {
    /** The original, overflow-prone time comparison. */
    private static final Comparator<Long> BUGGY = (final Long a, final Long b) -> (int) (a - b);
    /** The overflow-safe replacement. */
    private static final Comparator<Long> FIXED = (final Long a, final Long b) -> Long.compare(a, b);
    /**
     * Three realistic epoch-millis time points in true chronological order {@code T_LOW < T_MID < T_HIGH}. Each
     * adjacent step is ~3e9 ms (~34.7 days), comfortably above {@link Integer#MAX_VALUE} (~24.8 days), so a single
     * step already overflows the {@code (int)} cast and flips its sign. All three fall inside a plausible multi-week
     * data range for a long-running tracked race.
     */
    private static final long T_LOW = 1_700_000_000_000L; // ~2023-11-14
    private static final long T_MID = T_LOW + 3_000_000_000L; // +~34.7 days
    private static final long T_HIGH = T_MID + 3_000_000_000L; // +~34.7 days

    /**
     * The buggy comparator reports a cycle on the true chronological order: {@code T_LOW > T_MID} and
     * {@code T_MID > T_HIGH} yet {@code T_LOW < T_HIGH}. A {@link Comparator} contract requires a total order, so no
     * value can be simultaneously greater than its successor and less than its successor's successor. The fixed
     * comparator reports the one true order.
     */
    @Test
    public void truncatingCastReportsACyclicOrder() {
        // The fixed comparator honours the true chronological order throughout.
        assertTrue(FIXED.compare(T_LOW, T_MID) < 0);
        assertTrue(FIXED.compare(T_MID, T_HIGH) < 0);
        assertTrue(FIXED.compare(T_LOW, T_HIGH) < 0);
        // The buggy comparator flips each single ~3e9 ms step: it claims the earlier point is the greater one...
        assertTrue(BUGGY.compare(T_LOW, T_MID) > 0, "a single >2^31 ms step must overflow and flip the sign");
        assertTrue(BUGGY.compare(T_MID, T_HIGH) > 0, "a single >2^31 ms step must overflow and flip the sign");
        // ...while the ~6e9 ms end-to-end gap wraps a second time and comes back positive, so T_LOW < T_HIGH again.
        // Together these three answers form the cycle T_LOW > T_MID > T_HIGH > T_LOW: not a total order.
        assertTrue(BUGGY.compare(T_LOW, T_HIGH) < 0, "the ~6e9 ms gap wraps twice and reports T_LOW < T_HIGH");
    }

    /**
     * On a larger, clustered set the broken order makes {@link TreeSet} navigation disagree with the elements actually
     * present: the first/last of the sorted view are not the true chronological extremes, so {@code tailSet}-based
     * navigation (what {@code getTimeWiseContiguousCandidates} relies on) walks into the wrong neighbourhood. The fixed
     * comparator yields the true extremes.
     */
    @Test
    public void treeSetNavigationIsConsistentOnlyWithFixedComparator() {
        final NavigableSet<Long> fixed = buildClusteredSet(FIXED);
        assertEquals(T_LOW, fixed.first(), "fixed order must expose the earliest time point as first()");
        assertEquals(T_HIGH + 3_000L, fixed.last(), "fixed order must expose the latest time point as last()");
        final NavigableSet<Long> buggy = buildClusteredSet(BUGGY);
        final boolean extremesAreTrue = buggy.first() == T_LOW && buggy.last() == T_HIGH + 3_000L;
        assertTrue(!extremesAreTrue, "broken order must misplace the chronological extremes in the sorted view");
    }

    /**
     * Models the convergence loop of {@code MostProbableCandidatesInSmallTimeRangeFilter.updateCandidates}: repeatedly
     * take the next candidate, compute the contiguous time-wise sequence around it, and remove that whole sequence from
     * the working set. With a correct order the working set shrinks every iteration and the loop terminates; with the
     * broken order the "contiguous sequence" computed via {@code tailSet} does not line up with the elements actually
     * present, so progress stalls and the set never drains. The iteration cap turns the server's silent hang into a
     * loud, bounded test failure.
     */
    @Test
    public void convergenceLoopTerminatesOnlyWithFixedComparator() {
        final DrainOutcome fixed = drain(FIXED);
        assertTrue(fixed.converged, "with Long.compare the convergence loop must terminate");
        assertTrue(fixed.coversEveryCandidate, "with Long.compare every candidate must be consumed exactly once");
        // The buggy comparator does not drain within a very high iteration cap: in production this is the thread spin
        // observed on the server. It also fails to ever reach most candidates, so even the capped run is incomplete.
        final DrainOutcome buggy = drain(BUGGY);
        assertTrue(!buggy.converged, "with the (int) cast the convergence loop fails to converge within the cap");
        assertTrue(!buggy.coversEveryCandidate, "with the (int) cast most candidates are never reached");
    }

    private NavigableSet<Long> buildClusteredSet(final Comparator<Long> comparator) {
        final NavigableSet<Long> set = new TreeSet<>(comparator);
        for (final long base : new long[] { T_LOW, T_MID, T_HIGH }) {
            for (int i = 0; i < 4; i++) {
                set.add(base + i * 1_000L);
            }
        }
        return set;
    }

    /**
     * Builds a realistically-sized candidate set: {@code clusterCount} time clusters whose bases are {@code baseStepMs}
     * apart (chosen above {@link Integer#MAX_VALUE} so the {@code (int)} cast overflows between clusters), each cluster
     * holding {@code perCluster} points one second apart. This mirrors a long-running tracked race with many maneuver
     * candidates; the single-cluster toy set of {@link #buildClusteredSet} is too small to exhibit the hang.
     */
    private NavigableSet<Long> buildLargeMultiClusterSet(final Comparator<Long> comparator) {
        final int clusterCount = 50;
        final int perCluster = 50;
        final long baseStepMs = 2_200_000_000L; // ~25.5 days, so one step already overflows the (int) cast
        final NavigableSet<Long> set = new TreeSet<>(comparator);
        for (int cluster = 0; cluster < clusterCount; cluster++) {
            final long base = T_LOW + cluster * baseStepMs;
            for (int i = 0; i < perCluster; i++) {
                set.add(base + i * 1_000L);
            }
        }
        return set;
    }

    /**
     * Runs the {@code updateCandidates}-style convergence loop over a large multi-cluster set and reports whether it
     * drained within a high iteration cap and whether it ever reached every candidate. With a correct order the loop
     * removes a whole contiguous cluster each pass and finishes in a few passes; with the broken order {@code first()}
     * and {@code tailSet} disagree with the true chronology, so the same region is revisited and the set never drains.
     *
     * @return the outcome: {@code converged} is {@code false} when the cap was hit (the production loop would spin
     *         forever), and {@code coversEveryCandidate} is {@code false} when candidates were left unreachable.
     */
    private DrainOutcome drain(final Comparator<Long> comparator) {
        final long windowMs = 5_000L; // CANDIDATE_FILTER_TIME_WINDOW
        final NavigableSet<Long> all = buildLargeMultiClusterSet(comparator);
        final NavigableSet<Long> working = new TreeSet<>(comparator);
        working.addAll(all);
        final Set<Long> everRemoved = new HashSet<>();
        final int cap = 1_000_000; // orders of magnitude above the ~50 passes a correct run needs
        int iterations = 0;
        boolean converged = true;
        while (!working.isEmpty() && converged) {
            if (++iterations > cap) {
                converged = false; // did not converge: the production loop would spin here forever
            } else {
                final long startFrom = working.first();
                final NavigableSet<Long> sequence = new TreeSet<>(comparator);
                sequence.add(startFrom);
                long current = startFrom;
                for (final long next : all.tailSet(startFrom, false)) {
                    final boolean withinWindow = Math.abs(next - current) <= windowMs;
                    if (withinWindow) {
                        sequence.add(next);
                        current = next;
                    }
                }
                everRemoved.addAll(sequence);
                working.removeAll(sequence);
            }
        }
        return new DrainOutcome(converged, everRemoved.equals(all));
    }

    /** The result of a convergence-loop simulation: did it terminate, and did it reach every candidate. */
    private static final class DrainOutcome {
        private final boolean converged;
        private final boolean coversEveryCandidate;

        private DrainOutcome(final boolean converged, final boolean coversEveryCandidate) {
            this.converged = converged;
            this.coversEveryCandidate = coversEveryCandidate;
        }
    }
}
