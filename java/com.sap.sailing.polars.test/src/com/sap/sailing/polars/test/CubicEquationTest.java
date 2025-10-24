package com.sap.sailing.polars.test;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.polars.impl.CubicEquation;

public class CubicEquationTest {
    
    
    private static final double ERROR = 0.00001;

    @Test
    public void testForOneRealRoot() {
        CubicEquation eq = new CubicEquation(3, -10, 14, 27);
        double[] result = eq.solve();
        assertThat(result.length, is(1));
        
        double x1 = result[0];
        assertThat(x1, closeTo(-1, ERROR));
        
    }
    
    @Test
    public void testThreeRealAndEqualRoots() {
        CubicEquation eq = new CubicEquation(1, 6, 12, 8);
        double[] result = eq.solve();
        assertThat(result.length, is(1));
        
        double x1 = result[0];
        assertThat(x1, closeTo(-2, ERROR));
        
    }
    
    @Test
    public void testThreeRealRoots() {
        CubicEquation eq = new CubicEquation(2, -4, -22, 24);
        double[] result = eq.solve();
        assertThat(result.length, is(3));
        
        double x1 = result[0];
        assertTrue(Math.abs(x1 - 4) < ERROR || x1 + 3 < ERROR || x1 -1 < ERROR);
        
        double x2 = result[1];
        assertTrue(Math.abs(x2 - 4) < ERROR || x2 + 3 < ERROR || x2 -1 < ERROR);
        
        double x3 = result[2];
        assertTrue(Math.abs(x3 - 4) < ERROR || x3 + 3 < ERROR || x3 -1 < ERROR);    
        
    }

}
