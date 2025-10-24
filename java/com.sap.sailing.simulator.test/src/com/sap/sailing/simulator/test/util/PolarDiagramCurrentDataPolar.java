package com.sap.sailing.simulator.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileWriter;
import java.io.IOException;

import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.simulator.impl.PolarDiagram49STG;
import com.sap.sailing.simulator.impl.PolarDiagramBase;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class PolarDiagramCurrentDataPolar {

    public static void main(String[] args) throws IOException {

        PolarDiagramBase polarDiagram = new PolarDiagram49STG();
        polarDiagram.setCurrent(null);

        // NavigableMap<Speed, NavigableMap<Bearing, Speed>> table = polarDiagram.polarDiagramPlot(1.0, extraSpeeds);

        String outputFile = "C:\\temp\\pd-current-test-49stg.csv";
        FileWriter fw = new FileWriter(outputFile);

        // Set<Speed> validSpeeds = table.keySet();
        // validSpeeds.remove(Speed.NULL);

        String xLine = "X";
        // ) {
        SpeedWithBearing s = new KnotSpeedWithBearingImpl(12.0, new DegreeBearingImpl(180.0));
        // SpeedWithBearing s2 = new KnotSpeedWithBearingImpl(13, new DegreeBearingImpl(180.0));
        // xLine += ";" + (int)s2.getKnots();
        // }

        double baseCurBear = 90.0; 
        double maxCurSpeed = 1.0;
        for (double cS = 0.0; cS <= maxCurSpeed; cS += 0.2) {
            SpeedWithBearing c = new KnotSpeedWithBearingImpl(cS, new DegreeBearingImpl(baseCurBear));
            xLine += ";" + (int)(c.getKnots()*10);
        }
        fw.write(xLine + "\n");

        for (double d = 0; d <= 360; d += 0.1) {

            Bearing b = new DegreeBearingImpl(d);
            xLine = "";
            xLine += "" + b.getDegrees();

            for (double cS = 0.0; cS <= maxCurSpeed; cS += 0.2) {
                SpeedWithBearing c = new KnotSpeedWithBearingImpl(cS, new DegreeBearingImpl(baseCurBear));

                polarDiagram.setCurrent(c);
                polarDiagram.setWind(s);

                // for (double d=310; d<=359; d+= 1.0) {
                // for (Speed s : validSpeeds) {
                // xLine += ";" + polarDiagram.getSpeedAtBearingRaw(b).getKnots();
                xLine += ";" + polarDiagram.getSpeedAtBearingOverGround(b).getKnots();
            }
            fw.write(xLine + "\n");
        }

        fw.close();
        assertEquals(1, 1, "no test");

        System.out.println("Finished.");
    }

}
