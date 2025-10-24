package com.sap.sailing.simulator.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileWriter;
import java.io.IOException;
import java.util.NavigableMap;
import java.util.Set;

import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.impl.PolarDiagram49STG;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;

public class PolarDiagramPlotDataXY {

    public static void main(String[] args)  throws IOException {

        PolarDiagram polarDiagram = new PolarDiagram49STG();

        NavigableMap<Speed, NavigableMap<Bearing, Speed>> table = polarDiagram.polarDiagramPlot(1.0, null);

        String outputFile = "C:\\temp\\pd-test-XY.csv";
        FileWriter fw = new FileWriter(outputFile);

        Set<Speed> validSpeeds = table.keySet();
        validSpeeds.remove(Speed.NULL);
        String xLine = "wspeed;bangle;bspeed;polarX;polarY";
        fw.write(xLine + "\n");

        for (Speed s : validSpeeds) {    
            xLine = "";
            for (Bearing b : table.get(validSpeeds.iterator().next()).keySet()) {
                xLine += "" + s.getKnots();
                xLine += ";" + b.getDegrees();
                xLine += ";" + table.get(s).get(b).getKnots();
                xLine += ";" + table.get(s).get(b).getKnots() * Math.sin(b.getRadians());
                xLine += ";" + table.get(s).get(b).getKnots() * Math.cos(b.getRadians()) + "\n";
            }
            fw.write(xLine + "\n");
        }

        fw.close();
        assertEquals(1, 1, "no test");

    }

}
