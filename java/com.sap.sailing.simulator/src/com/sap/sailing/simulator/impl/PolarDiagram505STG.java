package com.sap.sailing.simulator.impl;

import java.util.NavigableMap;
import java.util.TreeMap;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;

public class PolarDiagram505STG extends PolarDiagramBase {

    private static final long serialVersionUID = 1271146273652108149L;

    // this constructor creates an instance with a hard-coded set of values
    public PolarDiagram505STG() {

        boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("505", /* typicallyStartsUpwind */true);

        speedTable = new TreeMap<Speed, NavigableMap<Bearing, Speed>>();
        NavigableMap<Bearing, Speed> tableRow;

        double cutAngle = 35.0;

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), new KnotSpeedImpl(1));
        tableRow.put(new DegreeBearingImpl(90), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(180), Speed.NULL);
        speedTable.put(Speed.NULL, tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), new KnotSpeedImpl(1));
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(5.25));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(2.5));
        speedTable.put(new KnotSpeedImpl(6), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), new KnotSpeedImpl(1));
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(5.57));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(3.35));
        speedTable.put(new KnotSpeedImpl(8), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), new KnotSpeedImpl(1));
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(5.88));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(4.25));
        speedTable.put(new KnotSpeedImpl(10), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), new KnotSpeedImpl(1));
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(6.30));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(5));
        speedTable.put(new KnotSpeedImpl(12), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), new KnotSpeedImpl(1));
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(6.83));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(5.75));
        speedTable.put(new KnotSpeedImpl(14), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), new KnotSpeedImpl(1));
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(7.35));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(6.5));
        speedTable.put(new KnotSpeedImpl(16), tableRow);

        tableRow = new TreeMap<Bearing, Speed>(PolarDiagramBase.bearingComparator);
        tableRow.put(new DegreeBearingImpl(0), Speed.NULL);
        tableRow.put(new DegreeBearingImpl(cutAngle), new KnotSpeedImpl(1));
        tableRow.put(new DegreeBearingImpl(90), new KnotSpeedImpl(7.35));
        tableRow.put(new DegreeBearingImpl(180), new KnotSpeedImpl(7.5));
        speedTable.put(new KnotSpeedImpl(20), tableRow);

        beatAngles = new TreeMap<Speed, Bearing>();
        beatAngles.put(new KnotSpeedImpl(0), new DegreeBearingImpl(44.0));
        beatAngles.put(new KnotSpeedImpl(6), new DegreeBearingImpl(44.0));
        beatAngles.put(new KnotSpeedImpl(8), new DegreeBearingImpl(46.5));
        beatAngles.put(new KnotSpeedImpl(10), new DegreeBearingImpl(50.0));
        beatAngles.put(new KnotSpeedImpl(12), new DegreeBearingImpl(52.0));
        beatAngles.put(new KnotSpeedImpl(14), new DegreeBearingImpl(54.0));
        beatAngles.put(new KnotSpeedImpl(16), new DegreeBearingImpl(55.0));
        beatAngles.put(new KnotSpeedImpl(20), new DegreeBearingImpl(56.0));

        double beatScale = 1.0;
        beatSOG = new TreeMap<Speed, Speed>();
        beatSOG.put(new KnotSpeedImpl(0), new KnotSpeedImpl(0));
        beatSOG.put(new KnotSpeedImpl(6), new KnotSpeedImpl(5.00 * beatScale));
        beatSOG.put(new KnotSpeedImpl(8), new KnotSpeedImpl(5.30 * beatScale));
        beatSOG.put(new KnotSpeedImpl(10), new KnotSpeedImpl(5.60 * beatScale));
        beatSOG.put(new KnotSpeedImpl(12), new KnotSpeedImpl(6.00 * beatScale));
        beatSOG.put(new KnotSpeedImpl(14), new KnotSpeedImpl(6.50 * beatScale));
        beatSOG.put(new KnotSpeedImpl(16), new KnotSpeedImpl(7.00 * beatScale));
        beatSOG.put(new KnotSpeedImpl(20), new KnotSpeedImpl(7.00 * beatScale));

        jibeAngles = new TreeMap<Speed, Bearing>();
        jibeAngles.put(new KnotSpeedImpl(0), new DegreeBearingImpl(141.0));
        jibeAngles.put(new KnotSpeedImpl(6), new DegreeBearingImpl(141.0));
        jibeAngles.put(new KnotSpeedImpl(8), new DegreeBearingImpl(141.0));
        jibeAngles.put(new KnotSpeedImpl(10), new DegreeBearingImpl(139.5));
        jibeAngles.put(new KnotSpeedImpl(12), new DegreeBearingImpl(137.5));
        jibeAngles.put(new KnotSpeedImpl(14), new DegreeBearingImpl(139.5));
        jibeAngles.put(new KnotSpeedImpl(16), new DegreeBearingImpl(142.5));
        jibeAngles.put(new KnotSpeedImpl(20), new DegreeBearingImpl(150.0));

        jibeSOG = new TreeMap<Speed, Speed>();
        jibeSOG.put(new KnotSpeedImpl(0), new KnotSpeedImpl(0));
        jibeSOG.put(new KnotSpeedImpl(6), new KnotSpeedImpl(5.00));
        jibeSOG.put(new KnotSpeedImpl(8), new KnotSpeedImpl(6.70));
        jibeSOG.put(new KnotSpeedImpl(10), new KnotSpeedImpl(8.50));
        jibeSOG.put(new KnotSpeedImpl(12), new KnotSpeedImpl(10.00));
        jibeSOG.put(new KnotSpeedImpl(14), new KnotSpeedImpl(11.50));
        jibeSOG.put(new KnotSpeedImpl(16), new KnotSpeedImpl(13.00));
        jibeSOG.put(new KnotSpeedImpl(20), new KnotSpeedImpl(15.00));

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
