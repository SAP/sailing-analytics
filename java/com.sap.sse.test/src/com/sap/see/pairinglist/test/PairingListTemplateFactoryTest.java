package com.sap.see.pairinglist.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.pairinglist.PairingListTemplate;
import com.sap.sse.pairinglist.impl.PairingListTemplateFactoryImpl;

public class PairingListTemplateFactoryTest {

    public PairingListTemplateFactoryImpl factoryImpl;

    @BeforeEach
    public void testSetup() {
        factoryImpl = new PairingListTemplateFactoryImpl();
    }

    @Test
    public void testPairingListCreationForValidValues() {
        final int flights = 15;
        final int groups = 3;
        final int competitors = 18;
        int[][] plTemplate = factoryImpl
                .createPairingListTemplate(new PairingFrameProviderTest(flights, groups, competitors))
                .getPairingListTemplate();
        Assertions.assertNotNull(plTemplate);
        for (int[] group : plTemplate) {
            for (int competitorNumber : group) {
                if (competitorNumber < 0 && competitorNumber < competitors) {
                    Assertions.fail("Values of Pairing List Template must not be smaller 0, when there are no dummies!");
                }
            }
        }
        int[][] plTemplate2 = factoryImpl
                .createPairingListTemplate(new PairingFrameProviderTest(flights, groups, competitors + 1))
                .getPairingListTemplate();
        int dummyCount = 0;
        Assertions.assertNotNull(plTemplate2);
        for (int[] group : plTemplate2) {
            for (int competitorNumber : group) {
                if (competitorNumber == -1) {
                    dummyCount++;
                }
                if (competitorNumber < -1 && competitorNumber < competitors) {
                    Assertions.fail("Values of Pairing List Template must not be smaller -1, when there are dummies!");
                }
            }
        }
        if (dummyCount != (groups - 1) * flights) {
            Assertions.fail("There are not as much dummies as there should be!");
        }
    }
    
    @Test
    public void testCreatePairingListTemplate() {
        PairingListTemplateFactoryImpl factoryImpl = new PairingListTemplateFactoryImpl();
        PairingListTemplate template1 = factoryImpl.createPairingListTemplate(new PairingFrameProviderTest(15, 3, 18));
        PairingListTemplate template2 = factoryImpl.createPairingListTemplate(new PairingFrameProviderTest(15, 3, 18));

        Assertions.assertFalse(Arrays.equals(template1.getPairingListTemplate(), template2.getPairingListTemplate()));
    }
    
    /**
     * Test some generations of example pairing combinations
     */
    @Test
    public void testGeneration() {
        PairingListTemplateFactoryImpl factoryImpl = new PairingListTemplateFactoryImpl();
        PairingListTemplate example1 = factoryImpl.createPairingListTemplate(new PairingFrameProviderTest(15, 3, 18));
        PairingListTemplate example6 = factoryImpl.createPairingListTemplate(new PairingFrameProviderTest(15, 3, 14));
        PairingListTemplate example2 = factoryImpl.createPairingListTemplate(new PairingFrameProviderTest(100, 6, 18));
        PairingListTemplate example3 = factoryImpl.createPairingListTemplate(new PairingFrameProviderTest(10, 3, 30));
        PairingListTemplate example4 = factoryImpl
                .createPairingListTemplate(new PairingFrameProviderTest(15, 3, 18), 3, 0);
        PairingListTemplate example5 = factoryImpl
                .createPairingListTemplate(new PairingFrameProviderTest(15, 3, 18), 3, 0);
        assertNotNull(example1);
        assertNotNull(example2);
        assertNotNull(example3);
        assertNotNull(example4);
        assertNotNull(example5);
        assertNotNull(example6);
    }

    @Test
    public void testMultiplyFlights() {
        final int flightCount = 14;
        final int groupCount = 3;
        final int competitorCount = 18;
        final int multiplier = 2;

        PairingListTemplateFactoryImpl factoryImpl = new PairingListTemplateFactoryImpl();
        PairingListTemplate example = factoryImpl.createPairingListTemplate(
                new PairingFrameProviderTest(flightCount, groupCount, competitorCount), multiplier, 0);

        for (int groupIndex = 0; groupIndex < flightCount * groupCount; groupIndex = groupIndex
                + (groupCount * (multiplier))) {
            assertArrayEquals(example.getPairingListTemplate()[groupIndex],
                    example.getPairingListTemplate()[groupIndex + groupCount]);
        }
    }

    @Test
    public void qualityCheck() {
        PairingListTemplate template = factoryImpl.createPairingListTemplate(new PairingFrameProviderTest(15, 3, 18));
        System.out.println(Arrays.deepToString(template.getPairingListTemplate()));
        System.out.println(template.getQuality());
        if (template.getQuality() >= 0.7) {
            Assertions.fail("Quality of Pairinglist is worse than usual!");
        }

        PairingListTemplate template2 = factoryImpl.createPairingListTemplate(new PairingFrameProviderTest(10, 3, 30));
        System.out.println(Arrays.deepToString(template2.getPairingListTemplate()));
        System.out.println(template2.getQuality());
        if (template2.getQuality() >= 1.2) {
            Assertions.fail("Quality of Pairinglist is worse than usual!");
        }
    }

    /**
     * Checks if the competitors of all single flights are divided up correctly (e.g. multiple competitors, absenced
     * competitors) flights, groups and competitors can be variable.
     */
    @Test
    public void checkFlights() {
        final int flights = 15;
        final int groups = 3;
        final int competitors = 18;

        ArrayList<Integer> availableCompetitors = new ArrayList<>();
        int[][] copy = factoryImpl.createPairingListTemplate(new PairingFrameProviderTest(flights, groups, competitors))
                .getPairingListTemplate();
        for (int i = 0; i < flights; i++) {
            IntStream.range(0, competitors - 1).forEach(competitor -> {
                availableCompetitors.add(competitor);
            });
            for (int j = 0; j < groups; j++) {
                for (int k = 0; k < competitors / groups; k++) {
                    if (availableCompetitors.contains(copy[i * groups + j][k])) {
                        availableCompetitors.remove(Integer.valueOf(copy[i * groups + j][k]));
                    }
                }
            }
            if (availableCompetitors.isEmpty()) {
                continue;
            } else {
                Assertions.fail("The competitors are not divided up correctly!");
            }
        }
    }
}
