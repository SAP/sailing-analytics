/**
 * 
 */
package com.sap.sailing.simulator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.impl.KilometersPerHourSpeedWithBearingImpl;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.simulator.impl.RectangularGrid;
import com.sap.sailing.simulator.impl.TimedPositionWithSpeedImpl;
import com.sap.sailing.simulator.windfield.WindControlParameters;
import com.sap.sailing.simulator.windfield.impl.WindFieldGeneratorBlastImpl;
import com.sap.sailing.simulator.windfield.impl.WindFieldGeneratorCombined;
import com.sap.sailing.simulator.windfield.impl.WindFieldGeneratorImpl;
import com.sap.sailing.simulator.windfield.impl.WindFieldGeneratorOscillationImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Test for @WindFieldGenerator
 * 
 * @author Nidhi Sawhney(D054070)
 * 
 */
public class WindFieldGeneratorTest {

    private static Logger logger = Logger.getLogger(WindFieldGeneratorTest.class.getName());

    @Test
    public void testWindFieldGeneratorBasic() {
        Position start = new DegreePosition(54.32447456461419, 10.15613079071045);
        Position end = new DegreePosition(54.32877915239163, 10.156173706054688);
        List<Position> course = new LinkedList<Position>();
        course.add(start);
        course.add(end);

        WindControlParameters windParameters = new WindControlParameters(3, 180);
        RectangularGrid bd = new RectangularGrid(start, end);
        WindFieldGeneratorImpl wf = new WindFieldGeneratorBlastImpl(bd, windParameters);
        int hSteps = 10;
        int vSteps = 5;
        Position[][] positions = bd.generatePositions(hSteps, vSteps, 0, 0);
        assert (positions.length*positions[0].length == hSteps * vSteps);
        int index = 0;
        for (int i = 0; i < positions.length; ++i) {
            for (int j = 0; j < positions[0].length; ++j) {
                logger.info("P" + ++index + ":" + positions[i][j]);
            }
        }
        wf.setPositionGrid(bd.generatePositions(hSteps, vSteps, 0, 0));
        Position[][] positionGrid = wf.getPositionGrid();
        assertNotNull(positionGrid, "Position Grid is not null");
        assertEquals(vSteps, positionGrid.length, "Position Grid Number of Rows");
        assertEquals(hSteps, positionGrid[0].length, "Position Grid Number of Columns");
        for (int i = 0; i < vSteps; ++i) {
            for (int j = 0; j < hSteps; ++j) {
                logger.info("P[" + i + "][" + j + "]:" + positionGrid[i][j]);
                assertEquals(positionGrid[i][j], wf.getPosition(i, j), "Map index check");
            }
        }
    }

    @Test
    public void testWindFieldGeneratorOscillation() {
        Position start = new DegreePosition(54.32447456461419, 10.15613079071045);
        Position end = new DegreePosition(54.32877915239163, 10.156173706054688);
        List<Position> course = new LinkedList<Position>();
        course.add(start);
        course.add(end);

        WindControlParameters windParameters = new WindControlParameters(10,0);
        windParameters.leftWindSpeed = 70.0;
        windParameters.middleWindSpeed = 80.0;
        windParameters.rightWindSpeed = 90.0;
        windParameters.frequency = 0.375;
        windParameters.amplitude = 20.0;

        RectangularGrid bd = new RectangularGrid(start, end);
        WindFieldGeneratorOscillationImpl wf = new WindFieldGeneratorOscillationImpl(bd, windParameters);
      
        int hSteps = 30;
        int vSteps = 15;

        wf.setPositionGrid(bd.generatePositions(hSteps, vSteps, 0, 0));
        Position[][] positionGrid = wf.getPositionGrid();
        TimePoint startTime = new MillisecondsTimePoint(0);
        Duration timeStep = new MillisecondsDurationImpl(30 * 1000);
        wf.generate(startTime, null, timeStep);
        wf.setTimeScale(1.0);
        SpeedWithBearing speed = new KilometersPerHourSpeedWithBearingImpl(0, new DegreeBearingImpl(0));

        /*
         * Check the speed & angle at the start time
         */
        List<Wind> windList = new ArrayList<Wind>();
        for (int i = 0; i < vSteps; ++i) {
            for (int j = 0; j < hSteps; ++j) {
                Wind localWind = wf.getWind(new TimedPositionWithSpeedImpl(startTime, positionGrid[i][j], speed));
                logger.info("Wind[" + i + "][" + j + "]" + localWind.toString());
                windList.add(localWind);
            }
        }
        assertEquals(hSteps * vSteps, windList.size(), "Size of windList ");
        double epsilon = 1e-6;
        // Check the speed
        assertEquals(7, windList.get(0).getKnots(), 0, "StartTime First Wind Speed ");
        assertEquals(9, windList.get(hSteps - 1).getKnots(), 0, "StartTime Last Wind Speed in first row ");

        assertEquals(7.896551, windList.get(windList.size() / 2 - 2)
                .getKnots(), epsilon, "StartTime One before Middle Wind Speed ");
        assertEquals(7.965517, windList.get(windList.size() / 2 - 1).getKnots(), epsilon, "StartTime Middle Wind Speed ");
        assertEquals(9, windList.get(windList.size() - 1).getKnots(), 0, "StartTime Last Wind Speed ");
        // Check the angle
        assertEquals(0, windList.get(0).getBearing().getRadians(), 0, "StartTime First Wind Angle ");
        Util.Pair<Integer, Integer> pairIndex = getIndex(windList.size(), hSteps);
        logger.info(pairIndex.toString());
        assertEquals(0.3224948, windList.get(windList.size() / 2 - 2).getBearing()
                .getRadians(), epsilon, "StartTime One before Middle Wind Angle ");
        assertEquals(0.3224948, windList.get(windList.size() / 2 - 1).getBearing().getRadians(), epsilon,
                "StartTime Middle Wind Angle ");
        assertEquals(0.2468268, windList.get(windList.size() - 1).getBearing().getRadians(), epsilon, "StartTime Last Wind Angle ");

        /**
         * Check the speed at the next time
         */
        /*
         * Check the speed & angle at the next time point
         */
        windList.clear();
        for (int i = 0; i < vSteps; ++i) {
            for (int j = 0; j < hSteps; ++j) {
                Wind localWind = wf.getWind(new TimedPositionWithSpeedImpl(new MillisecondsTimePoint(startTime
                        .asMillis() + timeStep.asMillis()), positionGrid[i][j], speed));
                logger.info("Wind[" + i + "][" + j + "]" + localWind.toString());
                windList.add(localWind);
            }
        }
        assertEquals(hSteps * vSteps, windList.size(), "Size of windList ");

        // Check the speed
        assertEquals(7, windList.get(0).getKnots(), 0, "One Time Unit First Wind Speed ");
        assertEquals(7.896551, windList.get(windList.size() / 2 - 2).getKnots(), epsilon,
                "One Time Unit One before Middle Wind Speed ");
        assertEquals(7.965517, windList.get(windList.size() / 2 - 1).getKnots(), epsilon, "One Time Unit Middle Wind Speed ");
        assertEquals(9, windList.get(windList.size() - 1).getKnots(), 0, "One Time Unit Last Wind Speed ");
        // Check the angle
        assertEquals(0.0068534,windList.get(0).getBearing().getRadians(), epsilon, "One Time Unit First Wind Angle ");
        assertEquals(0.3250553,  windList.get(windList.size() / 2 - 2).getBearing()
                .getRadians(), epsilon, "One Time Unit One before Middle Wind Angle ");
        assertEquals(0.3250553, windList.get(windList.size() / 2 - 1).getBearing()
               .getRadians(), epsilon, "One Time Unit Middle Wind Angle ");
        assertEquals(0.241933,  windList.get(windList.size() - 1).getBearing().getRadians(), epsilon,
                "One Time Unit Last Wind Angle ");

    }

    //@Test
    /*public void testWindFieldGeneratorBlast() {
        Position start = new DegreePosition(54.32447456461419, 10.15613079071045);
        Position end = new DegreePosition(54.32877915239163, 10.156173706054688);
        List<Position> course = new LinkedList<Position>();
        course.add(start);
        course.add(end);

        WindControlParameters windParameters = new WindControlParameters(10, 180);
        windParameters.blastProbability = 15.0;
        windParameters.maxBlastSize = 4.0;
        windParameters.blastWindSpeed = 15.0;
        windParameters.blastWindSpeedVar = 15.0;

        RectangularBoundary bd = new RectangularBoundary(start, end, 0.1);
        WindFieldGeneratorBlastImpl wf = new WindFieldGeneratorBlastImpl(bd, windParameters);
        int hSteps = 10;
        int vSteps = 5;

        wf.setPositionGrid(bd.extractGrid(hSteps, vSteps));
        Position[][] positionGrid = wf.getPositionGrid();
        TimePoint startTime = new MillisecondsTimePoint(0);
        TimePoint timeStep = new MillisecondsTimePoint(30 * 1000);
        wf.generate(startTime, null, timeStep);

        SpeedWithBearing speed = new KilometersPerHourSpeedWithBearingImpl(0, new DegreeBearingImpl(0));

        //
        // Check the speed & angle at the start time
        //
        List<Wind> windList = new ArrayList<Wind>();
        for (int i = 0; i < vSteps; ++i) {
            for (int j = 0; j < hSteps; ++j) {
                Wind localWind = wf.getWind(new TimedPositionWithSpeedImpl(startTime, positionGrid[i][j], speed));
                logger.info("Wind[" + i + "][" + j + "]" + localWind.toString());
                windList.add(localWind);
            }
        }
        assertEquals("Size of windList ", hSteps * vSteps, windList.size());
        //double epsilon = 1e-6;
        // Check the speed
        assertEquals("StartTime First Wind Speed ", 10, windList.get(0).getKnots(), 0);
    }*/
    
    @Test
    public void testWindFieldGeneratorCombinedNoBlast() {
        Position start = new DegreePosition(54.32447456461419, 10.15613079071045);
        Position end = new DegreePosition(54.32877915239163, 10.156173706054688);
        List<Position> course = new LinkedList<Position>();
        course.add(start);
        course.add(end);

        WindControlParameters windParameters = new WindControlParameters(10, 0);
        //Set Oscillation from testWindFieldGeneratorTest
        windParameters.leftWindSpeed = 70.0;
        windParameters.middleWindSpeed = 80.0;
        windParameters.rightWindSpeed = 90.0;
        windParameters.frequency = 0.375;
        windParameters.amplitude = 20.0;
        //No Blast
        windParameters.blastProbability = 0.0;
        windParameters.maxBlastSize = 1.0;
        windParameters.blastWindSpeed = 0.0;
        windParameters.blastWindSpeedVar = 0.0;

        RectangularGrid bd = new RectangularGrid(start, end);
        WindFieldGeneratorCombined wf = new WindFieldGeneratorCombined(bd, windParameters);
        int hSteps = 30;
        int vSteps = 15;

        wf.setPositionGrid(bd.generatePositions(hSteps, vSteps, 0, 0));
        Position[][] positionGrid = wf.getPositionGrid();
        TimePoint startTime = new MillisecondsTimePoint(0);
        Duration timeStep = new MillisecondsDurationImpl(30 * 1000);
        wf.generate(startTime, null, timeStep);

        SpeedWithBearing speed = new KilometersPerHourSpeedWithBearingImpl(0, new DegreeBearingImpl(0));

        /*
         * Check the speed & angle at the start time
         */
        List<Wind> windList = new ArrayList<Wind>();
        for (int i = 0; i < vSteps; ++i) {
            for (int j = 0; j < hSteps; ++j) {
                Wind localWind = wf.getWind(new TimedPositionWithSpeedImpl(startTime, positionGrid[i][j], speed));
                //logger.info("Wind[" + i + "][" + j + "]" + localWind.toString());
                windList.add(localWind);
            }
        }
        assertEquals(hSteps * vSteps, windList.size(), "Size of windList ");
        double epsilon = 1e-6;
        // Check the speed
        assertEquals(7, windList.get(0).getKnots(), 0, "StartTime First Wind Speed ");
        assertEquals(9, windList.get(hSteps - 1).getKnots(), 0, "StartTime Last Wind Speed in first row ");

        assertEquals(7.896551, windList.get(windList.size() / 2 - 2)
                .getKnots(), epsilon, "StartTime One before Middle Wind Speed ");
        assertEquals(7.965517, windList.get(windList.size() / 2 - 1).getKnots(), epsilon, "StartTime Middle Wind Speed ");
        assertEquals(9, windList.get(windList.size() - 1).getKnots(), 0, "StartTime Last Wind Speed ");
        // Check the angle
        assertEquals(0, windList.get(0).getBearing().getRadians(), 0, "StartTime First Wind Angle ");
       
    }
    
    private Util.Pair<Integer, Integer> getIndex(int listIndex, int numCols) {
        Util.Pair<Integer, Integer> indexPair = new Util.Pair<Integer, Integer>(1 + (listIndex - 1) / numCols, 1
                + (listIndex - 1) % numCols);
        return indexPair;
    }

    @Test
    public void testIndex() {
        Util.Pair<Integer, Integer> pairIndex = getIndex(1, 30);
        assertEquals(1, (int) pairIndex.getA(), "Index 1 RowIndex ");
        assertEquals(1, (int) pairIndex.getB(), "Index 1 ColIndex ");
        pairIndex = getIndex(30, 30);
        assertEquals(1, (int) pairIndex.getA(), "Index 30 RowIndex ");
        assertEquals(30, (int) pairIndex.getB(), "Index 30 ColIndex ");
        pairIndex = getIndex(31, 30);
        assertEquals(2, (int) pairIndex.getA(), "Index 31 RowIndex ");
        assertEquals(1, (int) pairIndex.getB(), "Index 31 ColIndex ");
        pairIndex = getIndex(450, 30);
        assertEquals(15, (int) pairIndex.getA(), "Index 450 RowIndex ");
        assertEquals(30, (int) pairIndex.getB(), "Index 450 ColIndex ");
    }

}
