package com.sap.sailing.simulator.windfield.impl;


import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.simulator.Grid;
import com.sap.sailing.simulator.TimedPosition;
import com.sap.sailing.simulator.windfield.WindControlParameters;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sse.common.Duration;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

public class WindFieldGeneratorCombined extends WindFieldGeneratorImpl implements WindFieldGenerator {
    
    private static final long serialVersionUID = 2283750025355590301L;
    private WindFieldGeneratorBlastImpl blastGen;
    private WindFieldGeneratorOscillationImpl oscillationGen;
    
    public WindFieldGeneratorCombined(Grid boundary, WindControlParameters windParameters) {
        super(boundary, windParameters);
        blastGen = new WindFieldGeneratorBlastImpl(boundary, windParameters);
        oscillationGen = new WindFieldGeneratorOscillationImpl(boundary, windParameters);
    }

    @Override
    public void setBoundary(Grid boundary) {
        
        super.setBoundary(boundary);
        blastGen.setBoundary(boundary);
        oscillationGen.setBoundary(boundary);
        
    }
 
    @Override
    public void generate(TimePoint start, TimePoint end, Duration step) {
        super.generate(start, end, step);
   
        blastGen.setPositionGrid(positions);
        //Setting these to zero as they will be added to the oscillation wind
        blastGen.generate(start, end, step, 0, 0);
        
        oscillationGen.setPositionGrid(positions);
        oscillationGen.generate(start, end, step);
    }
    
    @Override
    public Wind getWind(TimedPosition timedPosition) {
        Wind blastWind = blastGen.getWind(timedPosition);
        Wind oscillationWind = oscillationGen.getWind(timedPosition);
        
        SpeedWithBearing speedWithBearing = new KnotSpeedWithBearingImpl(blastWind.getKnots() + oscillationWind.getKnots(),
                blastWind.getBearing().add(oscillationWind.getBearing()));
        
        return new WindImpl(timedPosition.getPosition(), timedPosition.getTimePoint(),
                speedWithBearing);

    }
}
