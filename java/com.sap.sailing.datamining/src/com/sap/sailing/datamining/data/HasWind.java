package com.sap.sailing.datamining.data;

import java.util.Locale;

import com.sap.sailing.datamining.Activator;
import com.sap.sailing.domain.common.Wind;
import com.sap.sse.common.Positioned;
import com.sap.sse.common.Timed;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.shared.impl.dto.ClusterDTO;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public interface HasWind extends Positioned, Timed {
    Wind getWind();
    
    @Dimension(messageKey="WindStrengthInBeaufort", ordinal=11)
    default ClusterDTO getWindStrengthAsBeaufortCluster(Locale locale, ResourceBundleStringMessages stringMessages) {
        Wind wind = getWind();
        final Cluster<?> cluster = Activator.getClusterGroups().getWindStrengthInBeaufortClusterGroup().getClusterFor(wind);
        return new ClusterDTO(cluster.toString(), ()->cluster.asLocalizedString(locale, stringMessages));
    }
}
