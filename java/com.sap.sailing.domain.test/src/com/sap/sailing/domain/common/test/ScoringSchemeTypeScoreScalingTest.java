package com.sap.sailing.domain.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.ScoringSchemeType;

public class ScoringSchemeTypeScoreScalingTest {
    private static final double DELTA = 0.0000001;
    
    @Test
    public void testScalingThreeWithThree() {
        testScalingAndUnscaling(/* unscaled */ 3, /* factor */ 3, /* scaled */ 9, /* oneAlwaysStaysOne */ false);
    }

    @Test
    public void testScalingOneWithThree() {
        testScalingAndUnscaling(/* unscaled */ 1, /* factor */ 3, /* scaled */ 3, /* oneAlwaysStaysOne */ false);
    }

    @Test
    public void testScalingThreeWithTwo() {
        testScalingAndUnscaling(/* unscaled */ 3, /* factor */ 2, /* scaled */ 6, /* oneAlwaysStaysOne */ false);
    }

    @Test
    public void testScalingOneWithTwo() {
        testScalingAndUnscaling(/* unscaled */ 1, /* factor */ 2, /* scaled */ 2, /* oneAlwaysStaysOne */ false);
    }

    @Test
    public void testScalingWithThreeWithOneStaysOne() {
        testScalingAndUnscaling(/* unscaled */ 3, /* factor */ 3, /* scaled */ 7, /* oneAlwaysStaysOne */ true);
        testScalingAndUnscaling(/* unscaled */ 1, /* factor */ 3, /* scaled */ 1, /* oneAlwaysStaysOne */ true);
    }

    @Test
    public void testScalingWithTwoWithOneStaysOne() {
        testScalingAndUnscaling(/* unscaled */ 3, /* factor */ 2, /* scaled */ 5, /* oneAlwaysStaysOne */ true);
        testScalingAndUnscaling(/* unscaled */ 1, /* factor */ 2, /* scaled */ 1, /* oneAlwaysStaysOne */ true);
    }
    
    private void testScalingAndUnscaling(final double unscaled, final double factor, final double scaled,
            final boolean oneAlwaysStaysOne) {
        assertEquals(scaled, ScoringSchemeType.getScaledScore(factor, unscaled, oneAlwaysStaysOne), DELTA);
        assertEquals(unscaled, ScoringSchemeType.getUnscaledScore(factor, scaled, oneAlwaysStaysOne), DELTA);
    }
}
