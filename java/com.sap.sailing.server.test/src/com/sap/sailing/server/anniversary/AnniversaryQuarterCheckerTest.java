package com.sap.sailing.server.anniversary;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.dto.AnniversaryType;
import com.sap.sailing.server.anniversary.checker.QuarterChecker;
import com.sap.sailing.server.interfaces.AnniversaryRaceDeterminator.AnniversaryChecker;

public class AnniversaryQuarterCheckerTest {

    @Test
    public void at9999() {
        AnniversaryChecker checker = new QuarterChecker();
        checker.update(9999);
        Assertions.assertTrue(checker.getAnniversaries().isEmpty());
        Assertions.assertEquals(10000, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.QUARTER, checker.getType());
    }

    @Test
    public void at10000() {
        AnniversaryChecker checker = new QuarterChecker();
        checker.update(10000);
        Assertions.assertEquals(Arrays.asList(new Integer[] { 10000 }), checker.getAnniversaries());
        Assertions.assertEquals(25000, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.QUARTER, checker.getType());
    }

    @Test
    public void after10001() {
        AnniversaryChecker checker = new QuarterChecker();
        checker.update(10001);
        Assertions.assertEquals(Arrays.asList(new Integer[] { 10000 }), checker.getAnniversaries());
        Assertions.assertEquals(25000, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.QUARTER, checker.getType());
    }

    @Test
    public void after11111() {
        AnniversaryChecker checker = new QuarterChecker();
        checker.update(11111);
        Assertions.assertEquals(Arrays.asList(new Integer[] { 10000 }), checker.getAnniversaries());
        Assertions.assertEquals(25000, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.QUARTER, checker.getType());
    }

    @Test
    public void after24999() {
        AnniversaryChecker checker = new QuarterChecker();
        checker.update(24999);
        Assertions.assertEquals(Arrays.asList(new Integer[] { 10000 }), checker.getAnniversaries());
        Assertions.assertEquals(25000, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.QUARTER, checker.getType());
    }

    @Test
    public void after25000() {
        AnniversaryChecker checker = new QuarterChecker();
        checker.update(25000);
        Assertions.assertEquals(Arrays.asList(new Integer[] { 10000, 25000 }), checker.getAnniversaries());
        Assertions.assertEquals(50000, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.QUARTER, checker.getType());
    }

    @Test
    public void after25001() {
        AnniversaryChecker checker = new QuarterChecker();
        checker.update(25001);
        Assertions.assertEquals(Arrays.asList(new Integer[] { 10000, 25000 }), checker.getAnniversaries());
        Assertions.assertEquals(50000, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.QUARTER, checker.getType());
    }

    @Test
    public void afterMaxValidRaceCount() {
        AnniversaryChecker checker = new QuarterChecker();
        checker.update(999999999);
        Assertions.assertEquals(Arrays.asList(
                new Integer[] { 10000, 25000, 50000, 75000, 100000, 250000, 500000, 750000, 1000000, 2500000, 5000000,
                        7500000, 10000000, 25000000, 50000000, 75000000, 100000000, 250000000, 500000000, 750000000 }),
                checker.getAnniversaries());
        Assertions.assertEquals(1000000000, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.QUARTER, checker.getType());
    }

    /**
     * Tests that no runaway loop can happen, if the anniversary to check overflows internally. The used 1000000000 is
     * the first number, that will lead to a overflow, after *10 multiplier is applied, testing for the next anniversary
     */
    @Test
    public void ensureInCaseOfBugNoFreeze() {
        assertThrows(IllegalStateException.class, () -> {
            AnniversaryChecker checker = new QuarterChecker();
            checker.update(1000000000);
        });
    }

    @Test
    public void illegalRaceCountTest() {
        assertThrows(IllegalStateException.class, () -> {
            AnniversaryChecker checker = new QuarterChecker();
            checker.update(-1);
        });
    }
}
