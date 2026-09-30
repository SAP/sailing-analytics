package com.sap.sse.common;

import com.sap.sse.common.confidence.BearingWithConfidence;
import com.sap.sse.common.confidence.BearingWithConfidenceCluster;
import com.sap.sse.common.confidence.impl.BearingWithConfidenceImpl;

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
        cluster = new BearingWithConfidenceCluster<Void>(/* weigher */ null);
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
        BearingWithConfidence<Void> average = cluster.getAverage(null);
        return average == null ? null : average.getObject();
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
