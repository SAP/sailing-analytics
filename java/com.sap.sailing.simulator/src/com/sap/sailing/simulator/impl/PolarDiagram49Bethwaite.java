package com.sap.sailing.simulator.impl;

import java.util.NavigableMap;
import java.util.TreeMap;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;

public class PolarDiagram49Bethwaite extends PolarDiagramBase {

    private static final long serialVersionUID = 7725011817087318654L;

    // this constructor creates an instance with a hard-coded set of values
    public PolarDiagram49Bethwaite() {

        boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true);
        beatAngles = new TreeMap<Speed, Bearing>();
        beatAngles.put(new KnotSpeedImpl(0), new DegreeBearingImpl(43.0));
        beatAngles.put(new KnotSpeedImpl(6), new DegreeBearingImpl(50.24));
        beatAngles.put(new KnotSpeedImpl(9), new DegreeBearingImpl(49.40));
        beatAngles.put(new KnotSpeedImpl(12), new DegreeBearingImpl(47.35));
        beatAngles.put(new KnotSpeedImpl(25), new DegreeBearingImpl(48.01));

        double beatScale = 1.0;
        beatSOG = new TreeMap<Speed, Speed>();
        beatSOG.put(new KnotSpeedImpl(0), new KnotSpeedImpl(0));
        beatSOG.put(new KnotSpeedImpl(6), new KnotSpeedImpl(7.53 * beatScale));
        beatSOG.put(new KnotSpeedImpl(9), new KnotSpeedImpl(9.39 * beatScale));
        beatSOG.put(new KnotSpeedImpl(12), new KnotSpeedImpl(11.96 * beatScale));
        beatSOG.put(new KnotSpeedImpl(25), new KnotSpeedImpl(13.08 * beatScale));

        jibeAngles = new TreeMap<Speed, Bearing>();
        jibeAngles.put(new KnotSpeedImpl(0), new DegreeBearingImpl(135.0));
        jibeAngles.put(new KnotSpeedImpl(6), new DegreeBearingImpl(141.7));
        jibeAngles.put(new KnotSpeedImpl(9), new DegreeBearingImpl(144.7));
        jibeAngles.put(new KnotSpeedImpl(12), new DegreeBearingImpl(150.8));
        jibeAngles.put(new KnotSpeedImpl(25), new DegreeBearingImpl(155.8));

        jibeSOG = new TreeMap<Speed, Speed>();
        jibeSOG.put(new KnotSpeedImpl(0), new KnotSpeedImpl(0));
        jibeSOG.put(new KnotSpeedImpl(6), new KnotSpeedImpl(12.63));
        jibeSOG.put(new KnotSpeedImpl(9), new KnotSpeedImpl(14.35));
        jibeSOG.put(new KnotSpeedImpl(12), new KnotSpeedImpl(17.82));
        jibeSOG.put(new KnotSpeedImpl(25), new KnotSpeedImpl(23.25));

        speedTable = new TreeMap<Speed, NavigableMap<Bearing, Speed>>();
        NavigableMap<Bearing, Speed> tableRow;

        double cutAngle = 30.0;
        //double factor90 = 1.5;
        //double factor180 = 0.6;

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(90), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(180), Speed.NULL);
        speedTable.put(Speed.NULL, tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(45), new KnotSpeedImpl(6.55));
        tableRow.put(new DegreeBearingImpl(50.24), new KnotSpeedImpl(7.53));
        tableRow.put(new DegreeBearingImpl(70.47), new KnotSpeedImpl(9.97));
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(11.39));
        tableRow.put(new DegreeBearingImpl(101.83), new KnotSpeedImpl(11.07));
        tableRow.put(new DegreeBearingImpl(108.60), new KnotSpeedImpl(10.60));
        tableRow.put(new DegreeBearingImpl(117.54), new KnotSpeedImpl(9.71));
        tableRow.put(new DegreeBearingImpl(136.10), new KnotSpeedImpl(12.21));
        tableRow.put(new DegreeBearingImpl(141.70), new KnotSpeedImpl(12.63));
        tableRow.put(new DegreeBearingImpl(145.85), new KnotSpeedImpl(11.13));
        tableRow.put(new DegreeBearingImpl(147.91), new KnotSpeedImpl(6.88));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(5.56));
        //tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(factor90*beatSOG.get(new KnotSpeedImpl(6)).getKnots()));
        //tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(factor180*tableRow.get(new DegreeBearingImpl(90)).getKnots()));
        speedTable.put(new KnotSpeedImpl(6), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(45.00), new KnotSpeedImpl(8.38));
        tableRow.put(new DegreeBearingImpl(49.40), new KnotSpeedImpl(9.39));
        tableRow.put(new DegreeBearingImpl(70.58), new KnotSpeedImpl(11.98));
        tableRow.put(new DegreeBearingImpl(90.00), new KnotSpeedImpl(13.43));
        tableRow.put(new DegreeBearingImpl(101.39), new KnotSpeedImpl(13.37));
        tableRow.put(new DegreeBearingImpl(111.92), new KnotSpeedImpl(12.77));
        tableRow.put(new DegreeBearingImpl(121.60), new KnotSpeedImpl(11.85));
        tableRow.put(new DegreeBearingImpl(138.80), new KnotSpeedImpl(13.91));
        tableRow.put(new DegreeBearingImpl(144.70), new KnotSpeedImpl(14.35));
        tableRow.put(new DegreeBearingImpl(150.86), new KnotSpeedImpl(12.45));
        tableRow.put(new DegreeBearingImpl(155.00), new KnotSpeedImpl(7.87));
        tableRow.put(new DegreeBearingImpl(180.00), new KnotSpeedImpl(6.48));
        //tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(factor90*beatSOG.get(new KnotSpeedImpl(8)).getKnots()));
        //tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(factor180*tableRow.get(new DegreeBearingImpl(90)).getKnots()));
        speedTable.put(new KnotSpeedImpl(9), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(45.00), new KnotSpeedImpl(11.39));
        tableRow.put(new DegreeBearingImpl(47.35), new KnotSpeedImpl(11.96));
        tableRow.put(new DegreeBearingImpl(70.21), new KnotSpeedImpl(14.22));
        tableRow.put(new DegreeBearingImpl(90.00), new KnotSpeedImpl(15.14));
        tableRow.put(new DegreeBearingImpl(101.21), new KnotSpeedImpl(15.25));
        tableRow.put(new DegreeBearingImpl(113.94), new KnotSpeedImpl(14.94));
        tableRow.put(new DegreeBearingImpl(127.26), new KnotSpeedImpl(13.61));
        tableRow.put(new DegreeBearingImpl(144.52), new KnotSpeedImpl(16.83));
        tableRow.put(new DegreeBearingImpl(150.77), new KnotSpeedImpl(17.82));
        tableRow.put(new DegreeBearingImpl(155.60), new KnotSpeedImpl(15.35));
        tableRow.put(new DegreeBearingImpl(157.65), new KnotSpeedImpl(9.01));
        tableRow.put(new DegreeBearingImpl(180.00), new KnotSpeedImpl(7.55));
        //tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(factor90*beatSOG.get(new KnotSpeedImpl(10)).getKnots()));
        //tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(factor180*tableRow.get(new DegreeBearingImpl(90)).getKnots()));
        speedTable.put(new KnotSpeedImpl(12), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(45.00), new KnotSpeedImpl(12.11));
        tableRow.put(new DegreeBearingImpl(48.01), new KnotSpeedImpl(13.08));
        tableRow.put(new DegreeBearingImpl(70.71), new KnotSpeedImpl(15.69));
        tableRow.put(new DegreeBearingImpl(90.00), new KnotSpeedImpl(17.50));
        tableRow.put(new DegreeBearingImpl(101.14), new KnotSpeedImpl(18.45));
        tableRow.put(new DegreeBearingImpl(121.46), new KnotSpeedImpl(19.16));
        tableRow.put(new DegreeBearingImpl(140.00), new KnotSpeedImpl(18.50));
        tableRow.put(new DegreeBearingImpl(150.64), new KnotSpeedImpl(22.10));
        tableRow.put(new DegreeBearingImpl(155.78), new KnotSpeedImpl(23.25));
        tableRow.put(new DegreeBearingImpl(159.51), new KnotSpeedImpl(22.09));
        tableRow.put(new DegreeBearingImpl(163.83), new KnotSpeedImpl(16.29));
        tableRow.put(new DegreeBearingImpl(180.00), new KnotSpeedImpl(14.72));
        //tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(factor90*beatSOG.get(new KnotSpeedImpl(12)).getKnots()));
        //tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(factor180*tableRow.get(new DegreeBearingImpl(90)).getKnots()));
        speedTable.put(new KnotSpeedImpl(25), tableRow);

        /*tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(factor90*beatSOG.get(new KnotSpeedImpl(14)).getKnots()));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(factor180*tableRow.get(new DegreeBearingImpl(90)).getKnots()));
        speedTable.put(new KnotSpeedImpl(14), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(factor90*beatSOG.get(new KnotSpeedImpl(16)).getKnots()));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(factor180*tableRow.get(new DegreeBearingImpl(90)).getKnots()));
        speedTable.put(new KnotSpeedImpl(16), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(factor90*beatSOG.get(new KnotSpeedImpl(20)).getKnots()));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(factor180*tableRow.get(new DegreeBearingImpl(90)).getKnots()));
        speedTable.put(new KnotSpeedImpl(20), tableRow);*/

        for (Speed s : speedTable.keySet()) {

            if (beatAngles.containsKey(s) && !speedTable.get(s).containsKey(beatAngles.get(s))) {
                speedTable.get(s).put(beatAngles.get(s), beatSOG.get(s));
            }

            if (jibeAngles.containsKey(s) && !speedTable.get(s).containsKey(jibeAngles.get(s))) {
                speedTable.get(s).put(jibeAngles.get(s), jibeSOG.get(s));
            }

        }

    }

}
