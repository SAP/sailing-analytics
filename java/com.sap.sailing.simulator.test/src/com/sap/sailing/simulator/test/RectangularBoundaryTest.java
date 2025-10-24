package com.sap.sailing.simulator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.simulator.Grid;
import com.sap.sailing.simulator.impl.RectangularGrid;

public class RectangularBoundaryTest {

	@Test 
    public void testRectangularBoundary1() {
    	Position p1 = new DegreePosition(25.661333, -90.752563);
        Position p2 = new DegreePosition(24.522137, -90.774536);

        Grid b = new RectangularGrid(p1, p2);
        Position[][] grid = b.generatePositions(20,20,0,0);
        assertEquals(400,grid.length*grid[0].length,"Number of lattice points");
    	
    }

}
