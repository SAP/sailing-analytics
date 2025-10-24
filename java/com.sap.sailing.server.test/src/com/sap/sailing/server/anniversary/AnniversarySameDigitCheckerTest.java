package com.sap.sailing.server.anniversary;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.dto.AnniversaryType;
import com.sap.sailing.server.anniversary.checker.SameDigitChecker;
import com.sap.sailing.server.interfaces.AnniversaryRaceDeterminator.AnniversaryChecker;

public class AnniversarySameDigitCheckerTest {

    @Test
    public void at9999() {
        AnniversaryChecker checker = new SameDigitChecker();
        checker.update(9999);
        Assertions.assertTrue(checker.getAnniversaries().isEmpty());
        Assertions.assertEquals(11111, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.REPEATED_DIGIT, checker.getType());
    }

    @Test
    public void at10000() {
        AnniversaryChecker checker = new SameDigitChecker();
        checker.update(10000);
        Assertions.assertEquals(11111, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.REPEATED_DIGIT, checker.getType());
    }

    @Test
    public void at11111() {
        AnniversaryChecker checker = new SameDigitChecker();
        checker.update(11111);
        Assertions.assertEquals(Arrays.asList(new Integer[] { 11111 }), checker.getAnniversaries());
        Assertions.assertEquals(22222, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.REPEATED_DIGIT, checker.getType());
    }

    //
    @Test
    public void after11112() {
        AnniversaryChecker checker = new SameDigitChecker();
        checker.update(11112);
        Assertions.assertEquals(Arrays.asList(new Integer[] { 11111 }), checker.getAnniversaries());
        Assertions.assertEquals(22222, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.REPEATED_DIGIT, checker.getType());
    }

    @Test
    public void at88888() {
        AnniversaryChecker checker = new SameDigitChecker();
        checker.update(88888);
        Assertions.assertEquals(Arrays.asList(new Integer[] { 11111, 22222, 33333, 44444, 55555, 66666, 77777, 88888 }),
                checker.getAnniversaries());
        Assertions.assertEquals(111111, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.REPEATED_DIGIT, checker.getType());
    }

    @Test
    public void after22221() {
        AnniversaryChecker checker = new SameDigitChecker();
        checker.update(22221);
        Assertions.assertEquals(Arrays.asList(new Integer[] { 11111 }), checker.getAnniversaries());
        Assertions.assertEquals(22222, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.REPEATED_DIGIT, checker.getType());
    }

    @Test
    public void after22222() {
        AnniversaryChecker checker = new SameDigitChecker();
        checker.update(22222);
        Assertions.assertEquals(Arrays.asList(new Integer[] { 11111, 22222 }), checker.getAnniversaries());
        Assertions.assertEquals(33333, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.REPEATED_DIGIT, checker.getType());
    }

    @Test
    public void afterMaxValidRaceCount() {
        AnniversaryChecker checker = new SameDigitChecker();
        checker.update(999999999);
        Assertions.assertEquals(Arrays.asList(
                new Integer[] { 11111, 22222, 33333, 44444, 55555, 66666, 77777, 88888, 111111, 222222, 333333, 444444,
                        555555, 666666, 777777, 888888, 1111111, 2222222, 3333333, 4444444, 5555555, 6666666, 7777777,
                        8888888, 11111111, 22222222, 33333333, 44444444, 55555555, 66666666, 77777777, 88888888,
                        111111111, 222222222, 333333333, 444444444, 555555555, 666666666, 777777777, 888888888 }),
                checker.getAnniversaries());
        Assertions.assertEquals(1111111111, checker.getNextAnniversary().intValue());
        Assertions.assertEquals(AnniversaryType.REPEATED_DIGIT, checker.getType());
    }

    /**
     * Tests that no runaway loop can happen, if the anniversary to check overflows internally. It is expected that the
     * resulting String cannot be converted to a number anymore if it exceeds IntegerRange.
     */
    @Test
    public void ensureInCaseOfBugNoFreeze() {
        assertThrows(NumberFormatException.class, () -> {
            AnniversaryChecker checker = new SameDigitChecker();
            checker.update(Integer.MAX_VALUE);
        });
    }

    @Test
    public void illegalRaceCountTest() {
        assertThrows(IllegalStateException.class, () -> {
            AnniversaryChecker checker = new SameDigitChecker();
            checker.update(-1);
        });
    }
}
