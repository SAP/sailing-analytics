package com.sap.see.pairinglist.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Constructs randomized number sequences of equal length, for each element in the sequence using a random number
 * between 0 and a given limit, say {@code c}. The sequences are then ordered numerically, such that two sequences are
 * equal if all their elements are equal, and with s1 being less than s2 if the first element where s1 and s2 differ is
 * less for s1. Then, by enumerating the sorted list of sequences, the number of changed elements between the previous
 * and the next element is counted. This number is then related to the total number of sequence elements produced in the
 * process. In addition to the {@code c} parameter the process is parameterized by the number of sequences to generate
 * ({@code i}) and the length of each sequence ({@code f}).
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class CommonPrefixStatsTest {
    private static final Logger logger = Logger.getLogger(CommonPrefixStatsTest.class.getName());
    
    private final Random random = new Random();
    
    private final Comparator<int[]> sequenceComparator = (s1, s2)->{
        for (int i=0; i<s1.length; i++) {
            if (s1[i]>s2[i]) {
                return 1;
            } else if (s1[i]<s2[i]) {
                return -1;
            }
        }
        return 0;
    };
    
    /**
     * Returns an array of length {@code sequenceLength} whose elements are each randomly
     * picked from the inclusive interval 0..maxNumberPlusOne-1.
     */
    private int[] generateSeedSequence(int maxNumberPlusOne, int sequenceLength) {
        final int[] result = new int[sequenceLength];
        for (int i=0; i<sequenceLength; i++) {
            result[i] = random.nextInt(maxNumberPlusOne);
        }
        return result;
    }
    
    private List<int[]> generateSeedSequences(int howMany, int maxNumberPlusOne, int sequenceLength) {
        final List<int[]> result = new ArrayList<>(howMany);
        for (int i=0; i<howMany; i++) {
            result.add(generateSeedSequence(maxNumberPlusOne, sequenceLength));
        }
        return result;
    }
    
    private List<int[]> generateSortedSeedSequences(int howMany, int maxNumberPlusOne, int sequenceLength) {
        final List<int[]> result = generateSeedSequences(howMany, maxNumberPlusOne, sequenceLength);
        Collections.sort(result, sequenceComparator);
        return result;
    }
    
    private int countChanges(List<int[]> sequences) {
        int count;
        if (sequences.isEmpty()) {
            count = 0;
        } else {
            final Iterator<int[]> iterator = sequences.iterator();
            int[] previous = iterator.next();
            count = previous.length; // the first sequence is all new
            while (iterator.hasNext()) {
                final int[] next = iterator.next();
                count += countChanges(previous, next);
                previous = next;
            }
        }
        return count;
    }
    
    private int countChanges(int[] previous, int[] next) {
        assert previous.length == next.length;
        int changesCount = previous.length;
        while (previous[previous.length-changesCount] == next[next.length-changesCount]) {
            changesCount--;
        }
        return changesCount;
    }

    private int totalSeeds(List<int[]> sequences) {
        return sequences.isEmpty() ? 0 : sequences.size() * sequences.iterator().next().length;
    }
    
    public double computeSavingRatio(int howMany, int maxNumberPlusOne, int sequenceLength) {
        final List<int[]> sequences = generateSortedSeedSequences(howMany, maxNumberPlusOne, sequenceLength);
        final int changesInSortedSequences = countChanges(sequences);
        final int totalSeeds = totalSeeds(sequences);
        final double savingRatio = 1.-((double) changesInSortedSequences) / (double) totalSeeds;
        return savingRatio;
    }
    
    @Test
    public void testOneMillionSequencesForFifteenFlightsWithEighteenCompetitors() {
        final int runs = 3;
        double savingRatioSum = 0.0;
        for (int i=0; i<runs; i++) {
            savingRatioSum += computeSavingRatio(1000000, 18, 15);
        }
        final double savingRatioAverage = savingRatioSum / (double) runs;
        logger.info("Saving Ratio average: "+savingRatioAverage);
        assertTrue(savingRatioAverage>0.2);
    }
}
