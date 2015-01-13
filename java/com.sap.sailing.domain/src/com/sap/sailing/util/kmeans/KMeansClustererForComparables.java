package com.sap.sailing.util.kmeans;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sap.sailing.domain.common.scalablevalue.ComparableScalableValueWithDistance;
import com.sap.sailing.domain.common.scalablevalue.ScalableValueWithDistance;

public class KMeansClustererForComparables<ValueType, AveragesTo extends Comparable<AveragesTo>, T extends ComparableScalableValueWithDistance<ValueType, AveragesTo>>
        extends KMeansClusterer<ValueType, AveragesTo, T> {

    public KMeansClustererForComparables(int numberOfClusters, Iterable<T> elements) {
        super(numberOfClusters, elements, getEquiDistantSeeds(numberOfClusters, elements));
    }

    /**
     * Based on the elements being mutually comparable, min and max are determined and elements in between are calculated
     * so that <code>numberOfClusters-1</code> equal distances are created.
     */
    private static <ValueType, AveragesTo extends Comparable<AveragesTo>, T extends ComparableScalableValueWithDistance<ValueType, AveragesTo>> Iterator<AveragesTo> getEquiDistantSeeds(
            int numberOfClusters, Iterable<T> elements) {
        AveragesTo min = null;
        T minElement = null;
        AveragesTo max = null;
        T maxElement = null;
        for (T t : elements) {
            final AveragesTo tVal = t.divide(1);
            if (min == null || min.compareTo(tVal) > 0) {
                minElement = t;
                min = tVal;
            }
            if (max == null || max.compareTo(tVal) < 0) {
                maxElement = t;
                max = tVal;
            }
        }
        ScalableValueWithDistance<ValueType, AveragesTo> step = maxElement.add(minElement.multiply(-1)).multiply(1./(double) (numberOfClusters-1));
        List<AveragesTo> seeds = new ArrayList<>();
        ScalableValueWithDistance<ValueType, AveragesTo> a = minElement;
        for (int i = 0; i <numberOfClusters; i++) {
            seeds.add(a.divide(1));
            a = a.add(step);
        }
        return seeds.iterator();
    }
}
