package com.sap.sse.common.confidence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.DoublePair;
import com.sap.sse.common.Util;
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
public class BearingWithConfidenceCluster<RelativeTo> {
    private final List<BearingWithConfidence<RelativeTo>> bearings;
    private final Weigher<RelativeTo> weigher;
    private final ConfidenceBasedAverager<DoublePair, Bearing, RelativeTo> averager;
    
    /**
     * @param weigher
     *            if <code>null</code>, the <code>relativeTo</code> parameter of {@link #getAverage(Object)} will be ignored,
     *            and only the confidences provided by the {@link BearingWithConfidence} objects will be taken into
     *            account. Otherwise, the <code>weigher</code> will be used to determine the confidence relative to
     *            the <code>relativeTo</code> argument of {@link #getAverage(Object)}.
     */
    public BearingWithConfidenceCluster(Weigher<RelativeTo> weigher) {
        bearings = new ArrayList<BearingWithConfidence<RelativeTo>>();
        this.weigher = weigher;
        averager = ConfidenceFactory.INSTANCE.createAverager(weigher);
    }
    
    /**
     * Finds the two bearings in the cluster that are farthest apart (at least <code>minimumDegreeDifferenceBetweenTacks</code>).
     * Then, the remaining bearings in this cluster are associated with the one of the two extreme bearings to which they are
     * closer. The two resulting clusters are returned.
     * 
     * @param minimumDegreeDifferenceBetweenTacks
     *            tells the minimum degree difference that must exist between the two extreme bearings before they are
     *            considered to represent boats on different tacks. If more than one bearing exists in this cluster
     *            but no two bearings are at least <code>minimumDegreeDifferenceBetweenTacks</code> degrees apart from
     *            each other, only fir first of the two clusters returned will contain bearings while the second one
     *            remains empty.
     * @return two bearing clusters; both empty if this cluster is empty; only the second one empty if this cluster
     *         contains only one bearing. Otherwise, the two bearings farthest apart (greatest absolute
     *         {@link Bearing#getDifferenceTo(Bearing) difference}) are guaranteed to be in different clusters, and
     *         all other bearings contained in this cluster will be contained in the cluster that contains the extreme
     *         bearing to which it's closer.
     */
    public BearingWithConfidenceCluster<RelativeTo>[] splitInTwo(double minimumDegreeDifferenceBetweenTacks, RelativeTo relativeTo) {
        BearingWithConfidenceCluster<RelativeTo>[] result = createBearingClusterArraySizeTwo();
        result[0] = createEmptyCluster();
        result[1] = createEmptyCluster();
        if (bearings.size() >= 2) {
            Util.Pair<BearingWithConfidence<RelativeTo>, BearingWithConfidence<RelativeTo>> extremeBearings = getExtremeBearings(minimumDegreeDifferenceBetweenTacks);
            if (extremeBearings != null) {
                result[0].add(extremeBearings.getA());
                result[1].add(extremeBearings.getB());
            }
            for (BearingWithConfidence<RelativeTo> bearing : bearings) {
                if (extremeBearings == null || (bearing != extremeBearings.getA() && bearing != extremeBearings.getB())) {
                    if (extremeBearings == null
                            || result[0].getDifferenceFromAverage(bearing.getObject(), relativeTo) <= result[1]
                                    .getDifferenceFromAverage(bearing.getObject(), relativeTo)) {
                        result[0].add(bearing);
                    } else {
                        result[1].add(bearing);
                    }
                }
            }
        } else if (!bearings.isEmpty()) {
            // add the only bearing to the first of the two resulting clusters
            result[0].add(bearings.get(0));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    protected BearingWithConfidenceCluster<RelativeTo>[] createBearingClusterArraySizeTwo() {
        return (BearingWithConfidenceCluster<RelativeTo>[]) new BearingWithConfidenceCluster<?>[2];
    }
    
    /**
     * To avoid that a bearing with low confidence decides about the clustering, the difference between two bearings
     * is scaled by their confidences. This scaled distance is then maximized for those bearings at least
     * <code>minimumDegreeDifferenceBetweenTacks</code> degrees apart.
     */
    private Util.Pair<BearingWithConfidence<RelativeTo>, BearingWithConfidence<RelativeTo>> getExtremeBearings(double minimumDegreeDifferenceBetweenTacks) {
        assert bearings.size() >= 2;
        double maxAbsDegDiff = 0;
        Util.Pair<BearingWithConfidence<RelativeTo>, BearingWithConfidence<RelativeTo>> result = null;
        for (int i=0; i<bearings.size(); i++) {
            for (int j=i+1; j<bearings.size(); j++) {
                final double confidenceScaledDifference = getConfidenceScaledDifference(bearings.get(i), bearings.get(j));
                if (Math.abs(bearings.get(i).getObject().getDifferenceTo(bearings.get(j).getObject()).getDegrees()) >= minimumDegreeDifferenceBetweenTacks
                        && confidenceScaledDifference > maxAbsDegDiff) {
                    result = new Util.Pair<BearingWithConfidence<RelativeTo>, BearingWithConfidence<RelativeTo>>(bearings.get(i), bearings.get(j));
                    maxAbsDegDiff = confidenceScaledDifference;
                    assert Math.abs(bearings.get(i).getObject().getDifferenceTo(bearings.get(j).getObject()).getDegrees()) <= 180.;
                }
            }
        }
        return result;
    }

    private double getConfidenceScaledDifference(BearingWithConfidence<RelativeTo> bearingWithConfidence1,
            BearingWithConfidence<RelativeTo> bearingWithConfidence2) {
        return Math.abs(bearingWithConfidence1.getObject().getDifferenceTo(bearingWithConfidence2.getObject()).getDegrees()) *
                bearingWithConfidence1.getConfidence() * bearingWithConfidence2.getConfidence();
    }

    public boolean isEmpty() {
        return bearings.isEmpty();
    }
    
    public int size() {
        return bearings.size();
    }
    
    /**
     * Adds the <code>bearing</code> if its {@link BearingWithConfidence#getConfidence() confidence} is greater than 0.0
     */
    public void add(BearingWithConfidence<RelativeTo> bearing) {
        if (bearing.getConfidence() > 0) {
            bearings.add(bearing);
        }
    }
    
    /**
     * If the cluster contains no bearings, <code>null</code> is returned. Otherwise, the average angle is computed
     * by adding up the sin and cos values of the individual bearings, then computing the atan2 of the ratio. If the
     * combined confidence of the bearings in the cluster is 0.0, the result will contain <code>null</code> as
     * {@link BearingWithConfidence#getObject() object}.
     * 
     * TODO bug5576 comment 40: we could analyze the cluster's variance and let greater variances reduce the confidence
     */
    public BearingWithConfidence<RelativeTo> getAverage(RelativeTo relativeTo) {
        HasConfidence<DoublePair, Bearing, RelativeTo> average = averager.getAverage(getBearings(), relativeTo);
        return average == null ? null : new BearingWithConfidenceImpl<RelativeTo>(average.getObject(), average.getConfidence(), average.getRelativeTo());
    }
    
    /**
     * Absolute difference to {@link #getAverage() this cluster's average bearing} in degrees. If there is no bearing stored in
     * this cluster yet, or the combined confidence of the values in the cluster is 0.0,  0.0 is returned.
     * 
     * @return a value <code>&gt;=0.0</code>
     */
    private double getDifferenceFromAverage(Bearing bearing, RelativeTo relativeTo) {
        Bearing averageBearing = getAverage(relativeTo).getObject();
        return averageBearing == null ? 0.0 : Math.abs(averageBearing.getDifferenceTo(bearing).getDegrees());
    }
    
    protected Iterable<BearingWithConfidence<RelativeTo>> getBearings() {
        return Collections.unmodifiableCollection(bearings);
    }
    
    @Override
    public String toString() {
        return bearings.toString();
    }
    
    private BearingWithConfidenceCluster<RelativeTo> createEmptyCluster() {
        return new BearingWithConfidenceCluster<RelativeTo>(weigher);
    }

    public void clear() {
        bearings.clear();
    }
}
