package com.sap.sse.datamining.impl.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.data.ClusterGroup;

public class TestPercentageClusterFormatter {

    private ClusterGroup<Double> percentageClusterGroup;
    private PercentageClusterFormatter formatter;
    
    @BeforeEach
    public void initialize() {
        percentageClusterGroup = new LinearDoubleClusterGroup(0.0, 1.0, 0.1, true);
        formatter = new PercentageClusterFormatter();
    }

    @Test
    public void testFormat() {
        Cluster<Double> cluster = percentageClusterGroup.getClusterFor(0.05);
        String formattedCluster = formatter.format(cluster);
        assertThat(formattedCluster, is("[0% - 10%["));
    }

}
