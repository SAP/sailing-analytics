package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.ScalableInteger;
import com.sap.sailing.domain.common.confidence.impl.ScalableDouble;
import com.sap.sse.util.kmeans.Cluster;
import com.sap.sse.util.kmeans.KMeansClusterer;
import com.sap.sse.util.kmeans.KMeansClustererWithEquidistantInitialization;
import com.sap.sse.util.kmeans.KMeansMappingClusterer;

public class KMeansTest {
    @Test
    public void simpleIntegerTestWithEquidistantInitialization() {
        KMeansClustererWithEquidistantInitialization<Integer, Integer, ScalableInteger> clusterer = new KMeansClustererWithEquidistantInitialization<>(4,
                Arrays.asList(new ScalableInteger(1), new ScalableInteger(1), new ScalableInteger(11), new ScalableInteger(11), new ScalableInteger(21), new ScalableInteger(21), new ScalableInteger(31), new ScalableInteger(31)));
        Set<Cluster<ScalableInteger, Integer, Integer, ScalableInteger>> clusters = clusterer.getClusters();
        assertEquals(4, clusters.size());
        Set<Integer> clusterCentroids = new HashSet<>();
        clusters.stream().map((c)->c.getCentroid()).forEach((e)->clusterCentroids.add(e));
        assertEquals(new HashSet<Integer>(Arrays.asList(1, 11, 21, 31)), clusterCentroids);
    }

    @Test
    public void doubleTestWithRandomInitialization() {
        Random random = new Random(1234);
        List<ScalableDouble> elements = new ArrayList<>();
        for (int i=0; i<100000; i++) {
            elements.add(new ScalableDouble(random.nextDouble()));
        }
        KMeansClusterer<Double, Double, ScalableDouble> clusterer = new KMeansClusterer<>(4, elements);
        Set<Cluster<ScalableDouble, Double, Double, ScalableDouble>> clusters = clusterer.getClusters();
        for (Cluster<ScalableDouble, Double, Double, ScalableDouble> cluster : clusters) {
            for (ScalableDouble element : cluster) {
                final Double elementVal = element.divide(1);
                final double actualDistanceFromMean = Math.abs(cluster.getMean() - elementVal);
                for (Cluster<ScalableDouble, Double, Double, ScalableDouble> otherCluster : clusters) {
                    if (otherCluster != cluster) {
                        Double otherClusterMean = otherCluster.getMean();
                        final double distanceToOtherClusterMean = Math.abs(otherClusterMean - elementVal);
                        // assert that all elements are in the cluster where they are closest to the cluster's mean
                        assertTrue(distanceToOtherClusterMean >= actualDistanceFromMean);
                    }
                }
            }
        }
    }
    
    @Test
    public void testMappingClusterer() {
        List<String> strings = new ArrayList<>();
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<100000; i++) {
            for (int j=random.nextInt(100); j>=0; j--) {
                sb.append((char) ('A'+random.nextInt(26)));
            }
            strings.add(sb.toString());
            sb.delete(0, sb.length());
        }
        KMeansMappingClusterer<String, Integer, Integer, ScalableInteger> clusterer = new KMeansMappingClusterer<>(5, strings, (s)->new ScalableInteger(s.length()),
                Arrays.asList(5, 25, 50, 75, 100).iterator());
        Set<Cluster<String, Integer, Integer, ScalableInteger>> clusters = clusterer.getClusters();
        assertEquals(5, clusters.size());
        for (Cluster<String, Integer, Integer, ScalableInteger> cluster : clusters) {
            for (String element : cluster) {
                final int elementLength = element.length();
                double actualDistanceFromMean = Math.abs(cluster.getMean() - elementLength);
                for (Cluster<String, Integer, Integer, ScalableInteger> otherCluster : clusters) {
                    if (otherCluster != cluster) {
                        final int otherClusterMean = otherCluster.getMean();
                        final double distanceToOtherClusterMean = Math.abs(otherClusterMean - elementLength);
                        // assert that all elements are in the cluster where they are closest to the cluster's mean
                        assertTrue(distanceToOtherClusterMean >= actualDistanceFromMean);
                    }
                }
            }
        }
    }
}
