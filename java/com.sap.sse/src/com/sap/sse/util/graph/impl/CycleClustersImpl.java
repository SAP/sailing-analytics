package com.sap.sse.util.graph.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sap.sse.util.graph.CycleCluster;
import com.sap.sse.util.graph.CycleClusters;
import com.sap.sse.util.graph.DirectedEdge;

public class CycleClustersImpl<T> implements CycleClusters<T> {
    private final Set<CycleCluster<T>> clusters;

    public CycleClustersImpl(Set<CycleCluster<T>> clusters) {
        this.clusters = new HashSet<>(clusters);
    }
    
    @Override
    public CycleCluster<T> getCluster(T node) {
        for (final CycleCluster<T> cluster : clusters) {
            if (cluster.contains(node)) {
                return cluster;
            }
        }
        return null;
    }
    
    @Override
    public Iterable<CycleCluster<T>> getClusters() {
        return Collections.unmodifiableCollection(clusters);
    }

    @Override
    public boolean isEdgeInCycleCluster(DirectedEdge<T> edge) {
        return clusters.stream().anyMatch(c->c.contains(edge.getFrom()) && c.contains(edge.getTo()));
    }

    @Override
    public boolean areDisjoint() {
        for (final CycleCluster<T> c1 : clusters) {
            for (final CycleCluster<T> c2 : clusters) {
                if (c1 != c2) {
                    final Set<T> set = new HashSet<>(c1.getClusterNodes());
                    set.retainAll(c2.getClusterNodes());
                    if (!set.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
