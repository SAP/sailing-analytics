package com.sap.sailing.simulator.test;

import java.sql.Date;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.sap.sailing.domain.common.Duration;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MillisecondsDurationImpl;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.impl.PathGeneratorTreeGrowWind3;
import com.sap.sailing.simulator.impl.PolarDiagram49STG;
import com.sap.sailing.simulator.impl.RectangularBoundary;
import com.sap.sailing.simulator.impl.SimulationParametersImpl;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;
import com.sap.sailing.simulator.windfield.WindControlParameters;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sailing.simulator.windfield.impl.WindFieldGeneratorOscillationImpl;

public class TreeGrowTest {

    @Test
    public void testSailingSimulatorALL() {

        Position start = new DegreePosition(54.001917,10.82222);
        //Position end = new DegreePosition(54.023806,10.822048);
        SpeedWithBearing bearNorth =  new KnotSpeedWithBearingImpl(6.0, new DegreeBearingImpl(33.0));
        Position end = bearNorth.travelTo(start, new MillisecondsTimePoint(0), new MillisecondsTimePoint(10*60*1000));
        //System.out.println(start.getDistance(end).getKilometers());

        List<Position> course = new LinkedList<Position>();
        course.add(start);
        course.add(end);
        PolarDiagram pd = new PolarDiagram49STG();//PolarDiagram49.CreateStandard49();
        RectangularBoundary bd = new RectangularBoundary(start, end, 0.1);
        Position[][] positions = bd.extractGrid(10, 10, 0, 0);
        //RectangularBoundary new_bd = new RectangularBoundary(start, end, 0.1);
        //Speed knotSpeed = new KnotSpeedImpl(8);
        WindControlParameters windParameters = new WindControlParameters(12, start.getBearingGreatCircle(end).reverse().getDegrees());
        WindFieldGenerator wf = new WindFieldGeneratorOscillationImpl(bd, windParameters);
        wf.setPositionGrid(positions);
        Date startDate = new Date(0);
        TimePoint startTime = new MillisecondsTimePoint(startDate.getTime());
        Duration timeStep = new MillisecondsDurationImpl(20000);
        wf.generate(startTime, null, timeStep);
        SimulationParameters param = new SimulationParametersImpl(course, pd, wf, SailingSimulatorConstants.ModeFreestyle, true, true);

        /*param.setProperty("Heuristic.targetTolerance[double]", 0.05);
        param.setProperty("Heuristic.timeResolution[long]", 30000.0);
        param.setProperty("Djikstra.gridv[int]", 10.0);
        param.setProperty("Djikstra.gridh[int]", 100.0);*/

        PathGeneratorTreeGrowWind3 treeGrow = new PathGeneratorTreeGrowWind3(param);

        Path path = treeGrow.getPath();

        //System.out.println("tree-grow path points: "+path.getPathPoints().size());
        Assert.assertNotNull(path.getPathPoints());
        
        //for(TimedPositionWithSpeed pos : path.getPathPoints()) {
        //    System.out.println(""+pos.getPosition().getLatDeg()+", "+pos.getPosition().getLngDeg());
        //}
    }

}
