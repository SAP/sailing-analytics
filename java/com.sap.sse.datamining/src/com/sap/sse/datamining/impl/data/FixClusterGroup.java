package com.sap.sse.datamining.impl.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.data.ClusterGroup;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class FixClusterGroup<ElementType extends Serializable> implements ClusterGroup<ElementType> {

    private static final long serialVersionUID = 2632796094234887943L;
    private final String messageKey;
    private final Collection<Cluster<ElementType>> clusters;

    public FixClusterGroup(String messageKey, Collection<Cluster<ElementType>> clusters) {
        this.messageKey = messageKey;
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
    public String getLocalizedName(Locale locale, ResourceBundleStringMessages stringMessages) {
        return stringMessages.get(locale, messageKey);
    }
    
    @Override
    public String toString() {
        return messageKey + " " + clusters;
    }

}
