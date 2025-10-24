package com.sap.sailing.polars.regression.test;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.polars.regression.IncrementalLeastSquares;
import com.sap.sailing.polars.regression.impl.IncrementalAnyOrderLeastSquaresImpl;

public class IncrementalLeastSquaresTest {
    
    
    private static final double ERROR = 0.01;

    @Test
    public void testIncrementalRegression() throws NotEnoughDataHasBeenAddedException {
        IncrementalLeastSquares regression = new IncrementalAnyOrderLeastSquaresImpl(3);
        long startTime = System.nanoTime();
        Random random = new Random(1);
        for (int i = 0; i < 100000; i++) {
            double x = random.nextDouble() * 40;
            regression.addData(x, function(x) +  (random.nextDouble() - 0.5));
        }
        System.out.println("Adding Data " + (System.nanoTime() - startTime) + "ns");
        startTime = System.nanoTime();
        double[] coeffs = regression.getOrCreatePolynomialFunction().getCoefficients();
        System.out.println("First calculation " + (System.nanoTime() - startTime) + "ns");
        
        startTime = System.nanoTime();
        coeffs = regression.getOrCreatePolynomialFunction().getCoefficients();
        System.out.println("No calculation " + (System.nanoTime() - startTime) + "ns");
        double x = random.nextDouble() * 40;
        regression.addData(x, function(x) +  (random.nextDouble() - 0.5));
        startTime = System.nanoTime() ;
        coeffs = regression.getOrCreatePolynomialFunction().getCoefficients();
        System.out.println("Second calculation " + (System.nanoTime()  - startTime) + "ns");
        
        assertThat(coeffs[0], closeTo(-2.1309, ERROR));
        assertThat(coeffs[1], closeTo(1.5213, ERROR));
        assertThat(coeffs[2], closeTo(-0.0373, ERROR));
        assertThat(coeffs[3], closeTo(0.0003, ERROR));
    }
    
    private double function(double x) {
        return 0.0003*x*x*x-0.0373*x*x+1.5213*x -2.1309;
    }
    
    @Test
    public void testIncrementalRegressionWithWebsiteExampleData() throws NotEnoughDataHasBeenAddedException {
        IncrementalLeastSquares regression = new IncrementalAnyOrderLeastSquaresImpl(4);
        regression.addData(-1, -1);
        regression.addData(0, 3);
        regression.addData(1, 2.5);
        regression.addData(2, 5);
        regression.addData(3, 4);
        regression.addData(5, 2);
        regression.addData(7, 5);
        regression.addData(9, 4);
        double[] coeffs = regression.getOrCreatePolynomialFunction().getCoefficients();
        assertThat(coeffs[0], closeTo(2.686, ERROR));
        assertThat(coeffs[1], closeTo(2.301, ERROR));
        assertThat(coeffs[2], closeTo(-1.233, ERROR));
        assertThat(coeffs[3], closeTo(0.2169 , ERROR));
        assertThat(coeffs[4], closeTo(-0.01182 , ERROR));
    }
    
    @Test
    public void testIncrementalRegressionWithRealData() throws NotEnoughDataHasBeenAddedException {
        
        IncrementalLeastSquares leastSquares = new IncrementalAnyOrderLeastSquaresImpl(3);
        fillRegression(leastSquares);
        double[] coeffs = leastSquares.getOrCreatePolynomialFunction().getCoefficients();
        
        assertThat(coeffs[0], closeTo(6.319899225, ERROR));
        assertThat(coeffs[1], closeTo(-0.427890096, ERROR));
        assertThat(coeffs[2], closeTo(0.057763706, ERROR));
        assertThat(coeffs[3], closeTo(-0.001574209 , ERROR));
    }
    
    @Test
    public void testIncrementalRegressionWithRealDataNoInterceptWithSymbollicInversion() throws NotEnoughDataHasBeenAddedException {
        
        IncrementalLeastSquares leastSquares = new IncrementalAnyOrderLeastSquaresImpl(3, false);
        fillRegression(leastSquares);
        double[] coeffs = leastSquares.getOrCreatePolynomialFunction().getCoefficients();
        
        assertThat(coeffs[0], closeTo(0, ERROR));
        assertThat(coeffs[1], closeTo(0.9317759507, ERROR));
        assertThat(coeffs[2], closeTo(-0.0364029315, ERROR));
        assertThat(coeffs[3], closeTo(0.0005326266, ERROR));
        
        leastSquares.addData(0, 0);
        long startTime = System.nanoTime();
        leastSquares.getOrCreatePolynomialFunction().getCoefficients();
        System.out.println("With symbollic inversion no intercept" + (System.nanoTime()  - startTime) + "ns");
    }
    
    @Test
    public void testIncrementalRegressionWithRealDataNoInterceptWithoutSymbollicInversion() throws NotEnoughDataHasBeenAddedException {
        
        IncrementalLeastSquares leastSquares = new IncrementalAnyOrderLeastSquaresImpl(3, false, false);
        fillRegression(leastSquares);
        double[] coeffs = leastSquares.getOrCreatePolynomialFunction().getCoefficients();
        
        assertThat(coeffs[0], closeTo(0, ERROR));
        assertThat(coeffs[1], closeTo(0.9317759507, ERROR));
        assertThat(coeffs[2], closeTo(-0.0364029315, ERROR));
        assertThat(coeffs[3], closeTo(0.0005326266, ERROR));
        
        leastSquares.addData(0, 0);
        long startTime = System.nanoTime();
        leastSquares.getOrCreatePolynomialFunction().getCoefficients();
        System.out.println("With numeric inversion no intercept " + (System.nanoTime()  - startTime) + "ns");
    }
    
    @Test
    public void testIncrementalRegressionWithRealDataWithInterceptWithSymbollicInversion() throws NotEnoughDataHasBeenAddedException {
        
        IncrementalLeastSquares leastSquares = new IncrementalAnyOrderLeastSquaresImpl(3, true, true);
        fillRegression(leastSquares);
        double[] coeffs = leastSquares.getOrCreatePolynomialFunction().getCoefficients();
        
        assertThat(coeffs[0], closeTo(6.319899225, ERROR));
        assertThat(coeffs[1], closeTo(-0.427890096, ERROR));
        assertThat(coeffs[2], closeTo(0.057763706, ERROR));
        assertThat(coeffs[3], closeTo(-0.001574209, ERROR));
        
        leastSquares.addData(0, 0);
        long startTime = System.nanoTime();
        leastSquares.getOrCreatePolynomialFunction().getCoefficients();
        System.out.println("With symbollic inversion and intercept " + (System.nanoTime()  - startTime) + "ns");
    }
    
    @Test
    public void testIncrementalRegressionWithRealDataWithInterceptWithoutSymbollicInversion() throws NotEnoughDataHasBeenAddedException {
        
        IncrementalLeastSquares leastSquares = new IncrementalAnyOrderLeastSquaresImpl(3, true, false);
        fillRegression(leastSquares);
        double[] coeffs = leastSquares.getOrCreatePolynomialFunction().getCoefficients();
        
        assertThat(coeffs[0], closeTo(6.319899225, ERROR));
        assertThat(coeffs[1], closeTo(-0.427890096, ERROR));
        assertThat(coeffs[2], closeTo(0.057763706, ERROR));
        assertThat(coeffs[3], closeTo(-0.001574209, ERROR));
        
        leastSquares.addData(0, 0);
        long startTime = System.nanoTime();
        leastSquares.getOrCreatePolynomialFunction().getCoefficients();
        System.out.println("With numeric inversion and intercept " + (System.nanoTime()  - startTime) + "ns");
    }
  
    private void fillRegression(IncrementalLeastSquares leastSquares) {
        File file = new File("resources/5O5Worlds14_Winners_Upwind4550.csv");
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {

            br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] row = line.split(cvsSplitBy);
                leastSquares.addData(Double.parseDouble(row[0]), Double.parseDouble(row[1]));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }





}
