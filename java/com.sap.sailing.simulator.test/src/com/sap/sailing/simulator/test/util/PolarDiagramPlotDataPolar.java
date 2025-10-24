package com.sap.sailing.simulator.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileWriter;
import java.io.IOException;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeSet;

import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.impl.PolarDiagram49STG;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;

public class PolarDiagramPlotDataPolar {

    public static void main(String[] args)  throws IOException {

        PolarDiagram polarDiagram = new PolarDiagram49STG();

        Set<Speed> extraSpeeds = new TreeSet<Speed>();
        extraSpeeds.add(new KnotSpeedImpl(2.0));
        extraSpeeds.add(new KnotSpeedImpl(4.0));
        extraSpeeds.add(new KnotSpeedImpl(18.0));

        NavigableMap<Speed, NavigableMap<Bearing, Speed>> table = polarDiagram.polarDiagramPlot(1.0, extraSpeeds);

        String outputFile = "C:\\temp\\pd-test-49stg.csv";
        FileWriter fw = new FileWriter(outputFile);

        Set<Speed> validSpeeds = table.keySet();
        validSpeeds.remove(Speed.NULL);

        String xLine = "X";
        for (Speed s : validSpeeds) {
            xLine += ";" + (int)s.getKnots();
        }
        fw.write(xLine + "\n");

        for (Bearing b : table.get(validSpeeds.iterator().next()).keySet()) {
            xLine = "";
            xLine += "" + b.getDegrees();
            for (Speed s : validSpeeds) {
                xLine += ";" + table.get(s).get(b).getKnots();
            }
            fw.write(xLine + "\n");
        }

        fw.close();
        assertEquals(1, 1, "no test");

    }

}
