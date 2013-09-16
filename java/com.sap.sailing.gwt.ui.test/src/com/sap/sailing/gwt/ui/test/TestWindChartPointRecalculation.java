package com.sap.sailing.gwt.ui.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.IsEqual.*;

import org.junit.Test;
import org.moxieapps.gwt.highcharts.client.Point;

import com.sap.sailing.gwt.ui.client.shared.charts.WindChartPointRecalculator;

public class TestWindChartPointRecalculation {

    @Test
    public void testRecalculaion() {
        Point notToBeRecalculated = new Point(10, 100);
        assertThat(WindChartPointRecalculator.recalculateDirectionPoint(90.0, 110.0, notToBeRecalculated).getY(), equalTo(notToBeRecalculated.getY()));
        assertThat(WindChartPointRecalculator.recalculateDirectionPoint(null, 110.0, notToBeRecalculated).getY(), equalTo(notToBeRecalculated.getY()));
        assertThat(WindChartPointRecalculator.recalculateDirectionPoint(90.0, null, notToBeRecalculated).getY(), equalTo(notToBeRecalculated.getY()));
        assertThat(WindChartPointRecalculator.recalculateDirectionPoint(100.27458, 110.0, notToBeRecalculated).getY(), equalTo(notToBeRecalculated.getY()));
        assertThat(WindChartPointRecalculator.recalculateDirectionPoint(90.0, 100.75639, notToBeRecalculated).getY(), equalTo(notToBeRecalculated.getY()));
        
        //Test for special case, which caused a wrong recalculation, because of the wrong use of Math.abs()
        notToBeRecalculated = new Point(10, 179.8537);
        assertThat(WindChartPointRecalculator.recalculateDirectionPoint(180.12356, 190.0, notToBeRecalculated).getY(), equalTo(notToBeRecalculated.getY()));
        assertThat(WindChartPointRecalculator.recalculateDirectionPoint(170.0, 179.5372, notToBeRecalculated).getY(), equalTo(notToBeRecalculated.getY()));
        
        Point toBeMovedDown = new Point(10, 356);
        assertThat(WindChartPointRecalculator.recalculateDirectionPoint(4.0, 23.0, toBeMovedDown).getY(), equalTo(new Point(10, 356.0 - 360.0).getY()));
        
        Point toBeMovedUp = new Point(10, 5);
        assertThat(WindChartPointRecalculator.recalculateDirectionPoint(356.0, 359.83364, toBeMovedUp).getY(), equalTo(new Point(10, 5.0 + 360.0).getY()));
    }
    
    @Test
    public void testContinuousWrapAround() {
        //Test multiple wrap around between 350� and 30�
        List<Double> pointYValues = Arrays.asList(356.74, 10.54, 30.192, 20.625, 5.647, 350.526);
        List<Double> expectedPointYValues = Arrays.asList(356.74, 10.54 + 360, 30.192 + 360, 20.625 + 360, 5.647 + 360, 350.526);
        assertThat(recalculatePoints(pointYValues), equalTo(expectedPointYValues));
        
        //Test multiple round trips from 0� to 360�
        pointYValues = Arrays.asList(5.34, 358.97, 10.42, 50.0, 110.65, 140.54, 210.85, 280.38, 330.5, 358.97,
                                     7.24, 48.0, 112.65, 135.54, 215.85, 278.38, 333.5, 356.97,
                                     8.24, 48.245, 112.57, 133.54, 215.637, 278.38, 333.5, 356.97);
        expectedPointYValues = Arrays.asList(5.34, 358.97 - 360, 10.42, 50.0, 110.65, 140.54, 210.85 - 360, 280.38 - 360, 330.5 - 360, 358.97 - 360,
                                             7.24, 48.0, 112.65, 135.54, 215.85 - 360, 278.38 - 360, 333.5 - 360, 356.97 - 360,
                                             8.24, 48.245, 112.57, 133.54, 215.637 - 360, 278.38 - 360, 333.5 - 360, 356.97 - 360);

        assertThat(recalculatePoints(pointYValues), equalTo(expectedPointYValues));
    }

    private List<Double> recalculatePoints(List<Double> pointYValues) {
        List<Double> recalculatedPointYValues = new ArrayList<Double>();
        Double min = null;
        Double max = null;
        for (Double yValue : pointYValues) {
            Point p = new Point(10, yValue);
            p = WindChartPointRecalculator.recalculateDirectionPoint(min, max, p);
            Double newYValue = p.getY().doubleValue();
            recalculatedPointYValues.add(newYValue);
            
            if (min == null || newYValue < min) {
                min = newYValue;
            }
            if (max == null || newYValue > max) {
                max = newYValue;
            }
        }
        return recalculatedPointYValues;
    }

}
