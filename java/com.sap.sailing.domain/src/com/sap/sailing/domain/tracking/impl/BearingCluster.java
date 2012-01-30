package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.base.impl.BearingWithConfidenceImpl;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.confidence.Weigher;

/**
 * Contains a number of {@link Bearing} objects and maintains the average bearing. For a given {@link Bearing} it
 * can determine the difference to this cluster's average bearing. It can also split the cluster into two, based
 * on the two bearings farthest apart. The cluster can contain multiple occurrences of the same and also
 * multiple occurrences of mutually equal {@link Bearing} objects which is one possible way of computing a
 * weighted average.<p>
 * 
 * It is assumed that bearings added to this cluster are no further than 180 degrees apart. Violating this
 * rule will lead to unpredictable results.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class BearingCluster {
    private final BearingWithConfidenceCluster<Void> cluster;
    
    public BearingCluster() {
        cluster = new BearingWithConfidenceCluster<Void>(new Weigher<Void>() {
            @Override
            public double getConfidence(Void fix, Void request) {
                return 1;
            }
        });
    }
    
    private BearingCluster(BearingWithConfidenceCluster<Void> cluster) {
        this.cluster = cluster;
    }
    
    public BearingCluster[] splitInTwo(double minimumDegreeDifferenceBetweenTacks) {
        BearingWithConfidenceCluster<Void>[] array = cluster.splitInTwo(minimumDegreeDifferenceBetweenTacks, /* relativeTo */ null);
        BearingCluster[] result = new BearingCluster[array.length];
        int i=0;
        for (BearingWithConfidenceCluster<Void> element : array) {
            result[i++] = new BearingCluster(element);
        }
        return result;
    }
    
    public Bearing getAverage() {
        return cluster.getAverage(null).getObject();
    }

    public boolean isEmpty() {
        return cluster.isEmpty();
    }

    public int size() {
        return cluster.size();
    }

    public void add(Bearing bearing) {
        cluster.add(new BearingWithConfidenceImpl<Void>(bearing, /* confidence */ 1.0, /* relativeTo */ null));
    }
}
