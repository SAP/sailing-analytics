package com.sap.sse.common.scalablevalue;

import java.io.Serializable;
import java.util.Iterator;

/**
 * In a sequence of {@link ComparableScalableValueWithDistance} objects, tells the contiguous sub-sequence with the
 * greatest and the contiguous sub-sequence with the least sum, according to the
 * {@link ComparableScalableValueWithDistance#add(ScalableValue) add} and the
 * {@link ComparableScalableValueWithDistance#compareTo(Object) compareTo} methods.
 * <p>
 * 
 * The sequence is mutable. In particular, elements can be added at any position, also before the start or after the
 * end, and elements can be removed at least from the beginning of the sequence. Updating the sub-sequences with minimal
 * and maximal sums happens with complexity O(1) when adding to the end of the sequence, so with constant effort
 * regardless the size of the sequence. When inserting into or removing from the sequence at arbitrary positions,
 * constant effort can no longer be guaranteed as changes may need to get propagated onwards to following elements.
 * <p>
 * 
 * See also <a href="https://en.wikipedia.org/wiki/Maximum_subarray_problem">here</a> for a description of the
 * algorithm.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface KadaneExtremeSubsequenceFinder<ValueType, AveragesTo extends Comparable<AveragesTo>, T extends ComparableScalableValueWithDistance<ValueType, AveragesTo>>
        extends Serializable, Iterable<T> {
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }
    
    void add(int index, T t);

    default void add(T t) {
        add(size(), t);
    }

    void remove(int index);

    void remove(T t);
    
    void removeFirst(int i);

    ScalableValueWithDistance<ValueType, AveragesTo> getMinSum();

    ScalableValueWithDistance<ValueType, AveragesTo> getMaxSum();

    Iterator<T> getSubSequenceWithMaxSum();
    
    Iterator<T> getSubSequenceWithMinSum();
    
    int getStartIndexOfMaxSumSequence();

    /**
     * @return the index into {@link #sequence} holding the last element of the contiguous sub-sequence that has the
     *         maximal sum; note that pointing <em>to</em> and not <em>after</em> the last element of that sequence is
     *         slightly different from how indices may be handled in some other from/to collection operations.
     */
    int getEndIndexOfMaxSumSequence();

    int getStartIndexOfMinSumSequence();

    /**
     * @return the index into {@link #sequence} holding the last element of the contiguous sub-sequence that has the
     *         minimal sum; note that pointing <em>to</em> and not <em>after</em> the last element of that sequence is
     *         slightly different from how indices may be handled in some other from/to collection operations.
     */
    int getEndIndexOfMinSumSequence();

    /**
     * @return statistics: average number of propagation steps when a change affected a minimum sum sub-sequence
     */
    int getAverageMinChangePropagationSteps();

    /**
     * @return statistics: average number of propagation steps when a change affected a maximum sum sub-sequence
     */
    int getAverageMaxChangePropagationSteps();
    
    /**
     * Resets the statistics on change propagation steps as returned by {@link #getAverageMinChangePropagationSteps()}
     * and {@link #getAverageMaxChangePropagationSteps()}.
     */
    void resetStats();
}
