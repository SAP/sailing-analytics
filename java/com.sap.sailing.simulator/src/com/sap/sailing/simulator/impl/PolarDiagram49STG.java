package com.sap.sailing.simulator.impl;

import java.util.NavigableMap;
import java.util.TreeMap;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;

public class PolarDiagram49STG extends PolarDiagramBase {

    private static final long serialVersionUID = 4446552127317400136L;

    // this constructor creates an instance with a hard-coded set of values
    public PolarDiagram49STG() {

        boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true);

        speedTable = new TreeMap<Speed, NavigableMap<Bearing, Speed>>();
        NavigableMap<Bearing, Speed> tableRow;

        double cutAngle = 38.0;

        beatAngles = new TreeMap<Speed, Bearing>();
        beatAngles.put(new KnotSpeedImpl(0), new DegreeBearingImpl(43.0));
        beatAngles.put(new KnotSpeedImpl(6), new DegreeBearingImpl(43.0));
        beatAngles.put(new KnotSpeedImpl(8), new DegreeBearingImpl(43.0));
        beatAngles.put(new KnotSpeedImpl(10), new DegreeBearingImpl(43.0));
        beatAngles.put(new KnotSpeedImpl(12), new DegreeBearingImpl(44.0));
        beatAngles.put(new KnotSpeedImpl(14), new DegreeBearingImpl(45.0));
        beatAngles.put(new KnotSpeedImpl(16), new DegreeBearingImpl(45.6));
        beatAngles.put(new KnotSpeedImpl(20), new DegreeBearingImpl(47.0));

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(90), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(180), Speed.NULL);
        speedTable.put(Speed.NULL, tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(4.73));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(2.84));
        speedTable.put(new KnotSpeedImpl(6), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(6.56));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(3.94));
        speedTable.put(new KnotSpeedImpl(8), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(8.40));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(5.04));
        speedTable.put(new KnotSpeedImpl(10), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(8.93));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(5.36));
        speedTable.put(new KnotSpeedImpl(12), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(9.45));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(5.67));
        speedTable.put(new KnotSpeedImpl(14), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(9.77));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(5.86));
        speedTable.put(new KnotSpeedImpl(16), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(10.50));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(6.3));
        speedTable.put(new KnotSpeedImpl(20), tableRow);

        double beatScale = 1.0;
        beatSOG = new TreeMap<Speed, Speed>();
        beatSOG.put(new KnotSpeedImpl(0), new KnotSpeedImpl(0));
        beatSOG.put(new KnotSpeedImpl(6), new KnotSpeedImpl(4.50 * beatScale));
        beatSOG.put(new KnotSpeedImpl(8), new KnotSpeedImpl(6.25 * beatScale));
        beatSOG.put(new KnotSpeedImpl(10), new KnotSpeedImpl(8.00 * beatScale));
        beatSOG.put(new KnotSpeedImpl(12), new KnotSpeedImpl(8.50 * beatScale));
        beatSOG.put(new KnotSpeedImpl(14), new KnotSpeedImpl(9.00 * beatScale));
        beatSOG.put(new KnotSpeedImpl(16), new KnotSpeedImpl(9.30 * beatScale));
        beatSOG.put(new KnotSpeedImpl(20), new KnotSpeedImpl(10.00 * beatScale));

        jibeAngles = new TreeMap<Speed, Bearing>();
        jibeAngles.put(new KnotSpeedImpl(0), new DegreeBearingImpl(135.0));
        jibeAngles.put(new KnotSpeedImpl(6), new DegreeBearingImpl(135.0));
        jibeAngles.put(new KnotSpeedImpl(8), new DegreeBearingImpl(140.0));
        jibeAngles.put(new KnotSpeedImpl(10), new DegreeBearingImpl(145.0));
        jibeAngles.put(new KnotSpeedImpl(12), new DegreeBearingImpl(150.0));
        jibeAngles.put(new KnotSpeedImpl(14), new DegreeBearingImpl(155.0));
        jibeAngles.put(new KnotSpeedImpl(16), new DegreeBearingImpl(157.0));
        jibeAngles.put(new KnotSpeedImpl(20), new DegreeBearingImpl(160.0));

        jibeSOG = new TreeMap<Speed, Speed>();
        jibeSOG.put(new KnotSpeedImpl(0), new KnotSpeedImpl(0));
        jibeSOG.put(new KnotSpeedImpl(6), new KnotSpeedImpl(8.00));
        jibeSOG.put(new KnotSpeedImpl(8), new KnotSpeedImpl(11.00));
        jibeSOG.put(new KnotSpeedImpl(10), new KnotSpeedImpl(14.00));
        jibeSOG.put(new KnotSpeedImpl(12), new KnotSpeedImpl(15.00));
        jibeSOG.put(new KnotSpeedImpl(14), new KnotSpeedImpl(16.00));
        jibeSOG.put(new KnotSpeedImpl(16), new KnotSpeedImpl(17.00));
        jibeSOG.put(new KnotSpeedImpl(20), new KnotSpeedImpl(19.00));

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
