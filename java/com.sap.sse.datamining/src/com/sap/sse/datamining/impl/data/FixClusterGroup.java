package com.sap.sse.datamining.impl.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.data.restricted.ClusterGroupExtended;

public class FixClusterGroup<ElementType extends Serializable> implements ClusterGroupExtended<ElementType> {
    private static final long serialVersionUID = -5080331147800848136L;
    
    private final Set<Cluster<ElementType>> clusters;
    
    public Set<Cluster<ElementType>> getClusters() {
        return clusters;
    }

    public FixClusterGroup(Collection<Cluster<ElementType>> clusters) {
        this.clusters = new HashSet<>(clusters);
    }

    @Override
    public Cluster<ElementType> getClusterFor(ElementType value) {
        for (Cluster<ElementType> cluster : clusters) {
            if (cluster.isInRange(value)) {
                return cluster;
            }
        }
        return null;
    }
    
    @Override
    public Class<ElementType> getClusterElementsType() {
        return clusters.iterator().next().getClusterElementsType();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + clusters;
    }

}
