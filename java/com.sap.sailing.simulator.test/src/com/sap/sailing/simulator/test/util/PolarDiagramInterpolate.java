package com.sap.sailing.simulator.test.util;

import java.io.IOException;
import java.util.NavigableMap;

import com.sap.sailing.simulator.impl.PolarDiagram49STG;
import com.sap.sailing.simulator.impl.PolarDiagramBase;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

public class PolarDiagramInterpolate {

    public static void main(String[] args) throws IOException {

        PolarDiagramBase polarDiagram = new PolarDiagram49STG();
        
        NavigableMap<Double, Object> map = polarDiagram.extendSpeedMap();

        //
        // generate output for plotting polar diagrams with csv-polar-plot.R
        // and testing polar diagram interpolation including water currents
        //
        System.out.println("X, 12");
        for (double boatBear = 0.0; boatBear <= 360.0; boatBear += 1.0) {
            double[] values = new double[4];
            values[0] = 12.0;
            values[1] = 1.0;
            values[2] = 95.0;
            values[3] = boatBear;

            polarDiagram.setCurrent(new KnotSpeedWithBearingImpl(values[1], new DegreeBearingImpl(values[2])));
            polarDiagram.setWind(new KnotSpeedWithBearingImpl(values[0], new DegreeBearingImpl(180)));
            double result = polarDiagram.interpolate(values, 0, map);
            System.out.println("" + boatBear + ", " + result);
        }

    }

}
