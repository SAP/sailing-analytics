package com.sap.sailing.simulator.windfield.impl;


import com.sap.sailing.domain.base.SpeedWithBearing;
import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sailing.simulator.Boundary;
import com.sap.sailing.simulator.TimedPosition;
import com.sap.sailing.simulator.windfield.WindControlParameters;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;

public class WindFieldGeneratorCombined extends WindFieldGeneratorImpl implements WindFieldGenerator {
    
    private static final long serialVersionUID = 2283750025355590301L;
    private WindFieldGeneratorBlastImpl blastGen;
    private WindFieldGeneratorOscillationImpl oscillationGen;
    
    public WindFieldGeneratorCombined(Boundary boundary, WindControlParameters windParameters) {
        super(boundary, windParameters);
        blastGen = new WindFieldGeneratorBlastImpl(boundary, windParameters);
        oscillationGen = new WindFieldGeneratorOscillationImpl(boundary, windParameters);
    }

    @Override
    public void setBoundary(Boundary boundary) {
        
        super.setBoundary(boundary);
        blastGen.setBoundary(boundary);
        oscillationGen.setBoundary(boundary);
        
    }
 
    @Override
    public void generate(TimePoint start, TimePoint end, TimePoint step) {
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
