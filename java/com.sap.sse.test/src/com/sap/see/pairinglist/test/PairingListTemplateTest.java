package com.sap.see.pairinglist.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sse.pairinglist.impl.PairingListTemplateImpl;

public class PairingListTemplateTest extends PairingListTemplateImpl {

    public PairingListTemplateTest() {
        super(new PairingFrameProviderTest(15, 3, 18));
    }

    @Test
    public void testTeamAssociationCreation() {
        final int flights = 15;
        final int groups = 3;
        final int competitors = 18;
        int[][] plTemplate = this.createPairingListTemplate(flights, groups, competitors);
        int[][] associations = new int[competitors][competitors];

        associations = this.incrementAssociations(plTemplate, associations);
        for (int x = 0; x < associations.length; x++) {
            for (int y = 0; y < associations[0].length; y++) {
                if ((x == y) && (associations[x][y] != -1)) {
                    Assertions.fail("The diagonal of association matrix has to be -1.");
                }
                if (associations[x][y] > flights && (associations[x][y] < -1)) {
                    Assertions.fail("Calculation of assosciation matrix failed!");
                }
            }
        }
    }

    @Test
    public void testAverageTimeForSingleCase() {
        final int tests = 15;
        long[] a = new long[tests];

        for (int i = 0; i < tests; i++) {
            long time = System.currentTimeMillis();
            this.createPairingListTemplate(15, 3, 18);
            time = System.currentTimeMillis() - time;
            System.out.println(time);
            a[i] = time;
        }
        long sum = 0;
        for (int i = 0; i < tests; i++) {
            sum += a[i];
        }
        double average = sum / tests + 1;
        System.out.println("Average Time: " + (average / 1000) + "s");
        if (average > 10000) {
            Assertions.fail("The calculation of Pairing Lists took longer than expected!");
        }
    }

    @Test
    public void testAverageQualityForSingleCase() {
        final int count = 40;
        double[] results = new double[40];
        for (int i = 0; i < count; i++) {
            int[][] template = this.createPairingListTemplate(15, 3, 18);
            results[i] = calcStandardDev(incrementAssociations(template, new int[18][18]));
            System.out.println(results[i]);
            System.out.println(Arrays.deepToString(template));
        }
        double sum = 0;
        for (double quality : results) {
            sum += quality;
        }
        Assertions.assertEquals(0.574, sum / count, 0.01);
    }

    @Test
    public void testIncrementAndDecrementAssociations() {
        int[][] associations = new int[18][18];
        int[][] flight = this.createFlight(3, 18, associations, 5);

        associations = this.incrementAssociations(flight, associations);
        associations = this.decrementAssociations(flight, associations);
        for (int[] key : associations) {
            for (int value : key) {
                if (value > 0) {
                    Assertions.fail("Associations array should only have values 0 and -1!");
                }
            }
        }
    }

    @Test
    public void testStandardDevCalc() {
        int[][] givenPairingList = { { 0, 15, 17, 14, 16, 8 }, { 13, 1, 10, 9, 12, 11 }, { 3, 5, 2, 4, 6, 7 },
                { 0, 13, 17, 4, 12, 7 }, { 11, 15, 16, 6, 1, 5 }, { 14, 10, 8, 2, 9, 3 }, { 15, 0, 11, 2, 13, 3 },
                { 17, 10, 14, 6, 7, 1 }, { 8, 4, 16, 5, 9, 12 }, { 7, 3, 16, 1, 9, 0 }, { 12, 15, 10, 17, 5, 2 },
                { 6, 11, 4, 13, 8, 14 }, { 16, 11, 4, 17, 3, 10 }, { 12, 2, 6, 8, 0, 1 }, { 7, 13, 5, 15, 14, 9 },
                { 1, 3, 12, 15, 14, 4 }, { 9, 17, 6, 16, 13, 2 }, { 5, 0, 7, 11, 10, 8 }, { 16, 2, 7, 11, 14, 12 },
                { 3, 1, 5, 8, 17, 13 }, { 6, 9, 4, 10, 15, 0 }, { 14, 16, 13, 10, 5, 0 }, { 11, 2, 1, 9, 4, 17 },
                { 8, 12, 3, 7, 6, 15 }, { 10, 13, 8, 7, 16, 15 }, { 2, 14, 0, 4, 5, 1 }, { 6, 17, 9, 12, 11, 3 },
                { 14, 17, 0, 12, 11, 5 }, { 15, 16, 13, 1, 4, 2 }, { 8, 6, 3, 10, 7, 9 }, { 16, 6, 12, 0, 2, 10 },
                { 1, 8, 15, 14, 17, 9 }, { 5, 7, 13, 3, 4, 11 }, { 9, 7, 0, 2, 15, 11 }, { 13, 3, 14, 1, 12, 10 },
                { 17, 4, 5, 16, 8, 6 }, { 17, 4, 12, 15, 10, 7 }, { 3, 1, 11, 0, 8, 16 }, { 2, 9, 6, 5, 13, 14 },
                { 0, 9, 17, 5, 3, 15 }, { 12, 7, 1, 13, 16, 6 }, { 10, 14, 2, 8, 11, 4 }, { 7, 14, 2, 17, 3, 16 },
                { 15, 5, 10, 11, 1, 6 }, { 4, 12, 8, 9, 0, 13 } };
        Assertions.assertEquals(0.5998846486579744,
                calcStandardDev(incrementAssociations(givenPairingList, new int[18][18])), 0.0);
        int[][] testPairingList = { { 0, 1, 2, 3, 4 }, { 5, 6, 7, 8, 9 } };
        Assertions.assertEquals(0.4967673, (calcStandardDev(incrementAssociations(testPairingList, new int[10][10]))),
                0.01);
    }

    @Test
    public void assignmentTest() {
        this.createPairingListTemplate(15, 3, 18);
        System.out.println(this.calcStandardDev(this.getBoatAssignments(this.getPairingListTemplate(), new int[18][6])));
        
        this.createPairingListTemplate(10, 3, 30);
        System.out.println(this.calcStandardDev(this.getBoatAssignments(this.getPairingListTemplate(), new int[18][6])));
    }

    @Test
    public void testSort() {
        int[][] allSeeds = { { 0, 0, 0, 0, 7, 5 }, { 17, 12, 14, 16, 3, 4 }, { 0, 0, 0, 0, 0, 0 }, { 3, 4, 5, 6, 7, 8 },
                { 3, 4, 5, 6, 7, 9 } };
        allSeeds = radixSort(allSeeds, 18);
        int[][] expectedResult = { { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 7, 5 }, { 3, 4, 5, 6, 7, 8 },
                { 3, 4, 5, 6, 7, 9 }, { 17, 12, 14, 16, 3, 4 } };
        assertArrayEquals(allSeeds, expectedResult);
    }
    @Test
    public void testGetBoatChanges(){
    	int[][] givenPairingList = { { 0, 15, 17, 14, 16, 8 }, { 13, 1, 10, 9, 12, 11 }, { 3, 5, 2, 4, 6, 7 },
                { 0, 13, 17, 4, 12, 7 }, { 11, 15, 16, 6, 1, 5 }, { 14, 10, 8, 2, 9, 3 }, { 15, 0, 11, 2, 13, 3 },
                { 17, 10, 14, 6, 7, 1 }, { 8, 4, 16, 5, 9, 12 }, { 7, 3, 16, 1, 9, 0 }, { 12, 15, 10, 17, 5, 2 },
                { 6, 11, 4, 13, 8, 14 }, { 16, 11, 4, 17, 3, 10 }, { 12, 2, 6, 8, 0, 1 }, { 7, 13, 5, 15, 14, 9 },
                { 1, 3, 12, 15, 14, 4 }, { 9, 17, 6, 16, 13, 2 }, { 5, 0, 7, 11, 10, 8 }, { 16, 2, 7, 11, 14, 12 },
                { 3, 1, 5, 8, 17, 13 }, { 6, 9, 4, 10, 15, 0 }, { 14, 16, 13, 10, 5, 0 }, { 11, 2, 1, 9, 4, 17 },
                { 8, 12, 3, 7, 6, 15 }, { 10, 13, 8, 7, 16, 15 }, { 2, 14, 0, 4, 5, 1 }, { 6, 17, 9, 12, 11, 3 },
                { 14, 17, 0, 12, 11, 5 }, { 15, 16, 13, 1, 4, 2 }, { 8, 6, 3, 10, 7, 9 }, { 16, 6, 12, 0, 2, 10 },
                { 1, 8, 15, 14, 17, 9 }, { 5, 7, 13, 3, 4, 11 }, { 9, 7, 0, 2, 15, 11 }, { 13, 3, 14, 1, 12, 10 },
                { 17, 4, 5, 16, 8, 6 }, { 17, 4, 12, 15, 10, 7 }, { 3, 1, 11, 0, 8, 16 }, { 2, 9, 6, 5, 13, 14 },
                { 0, 9, 17, 5, 3, 15 }, { 12, 7, 1, 13, 16, 6 }, { 10, 14, 2, 8, 11, 4 }, { 7, 14, 2, 17, 3, 16 },
                { 15, 5, 10, 11, 1, 6 }, { 4, 12, 8, 9, 0, 13 } };
    	Assertions.assertEquals(56,this.getBoatChangesFromPairingList(givenPairingList, 15, 3, 18));
    }
}
