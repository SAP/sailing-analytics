package com.sap.sailing.simulator.windfield;



import java.io.Serializable;

import com.sap.sailing.simulator.Grid;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

public interface WindFieldGenerator extends WindField, Serializable {
    
    public WindControlParameters getWindParameters();

    public void setBoundary(Grid boundary);

    public void setPositionGrid(Position[][] positions);
    
    public void generate(TimePoint start, TimePoint end, Duration step);

    public Position[][] getPositionGrid();
    
    /**
     * The first time for which we have a timed wind from the wind field
     * @return
     */
    public TimePoint getStartTime();
    
    /**
     * Returns the time steps if the wind field is generated at these time uints
     * from the start time
     * @return
     */
    public Duration getTimeStep();
    
    /**
     * The last TimePoint for which the wind field is generated, could be null if no
     * such time is defined.
     * @return
     */
    public TimePoint getEndTime();
    
    public int[] getGridResolution();
    public void setGridResolution(int[] gridRes);
    public Position[] getGridAreaGps();
    public void setGridAreaGps(Position[] gridAreaGps);

}
