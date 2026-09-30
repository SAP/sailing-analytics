package com.sap.sailing.simulator.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;

public class PolarDiagramCSV extends PolarDiagramBase {

    private static final long serialVersionUID = -9219705955440602679L;

    public PolarDiagramCSV(String inputFile) throws IOException {
        ClassLoader cl = this.getClass().getClassLoader();
        InputStream csvFile = cl.getResourceAsStream(inputFile);
        InputStreamReader isr = new InputStreamReader(csvFile, Charset.forName("UTF-8"));
        BufferedReader bfr = new BufferedReader(isr);

        List<Speed> windSpeeds = new ArrayList<Speed>();
        List<Bearing> beatAngles = new ArrayList<Bearing>();
        List<Speed> beatSpeed = new ArrayList<Speed>();
        Map<Bearing, List<Speed>> speeds = new HashMap<Bearing, List<Speed>>();
        List<Speed> jibeSpeed = new ArrayList<Speed>();
        List<Bearing> jibeAngles = new ArrayList<Bearing>();

        String line = "";
        line = bfr.readLine();
        if (line != null) {
            this.boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass(line, /* typicallyStartsUpwind */true);
        }
        
        String[] elements = null;
        while (line != null) {

            line = bfr.readLine();
            if (line == null) {
                break;
            }
            
            elements = line.split(",");
            elements[0] = elements[0].replace(" ", "");
            elements[0] = elements[0].toLowerCase();

            switch (elements[0]) {
            case "windspeed":
                for (int i = 1; i < elements.length; i++) {
                    windSpeeds.add(new KnotSpeedImpl(Double.valueOf(elements[i])));
                }
                break;
            case "beatangles":
                for (int i = 1; i < elements.length; i++) {
                    beatAngles.add(new DegreeBearingImpl(Double.valueOf(elements[i])));
                }
                break;
            case "beatsog":
                for (int i = 1; i < elements.length; i++) {
                    beatSpeed.add(new KnotSpeedImpl(Double.valueOf(elements[i])));
                }
                break;
            case "jibesog":
                for (int i = 1; i < elements.length; i++) {
                    jibeSpeed.add(new KnotSpeedImpl(Double.valueOf(elements[i])));
                }
                break;
            case "jibeangles":
                for (int i = 1; i < elements.length; i++) {
                    jibeAngles.add(new DegreeBearingImpl(Double.valueOf(elements[i])));
                }
                break;
            default:
                List<Speed> sp = new ArrayList<Speed>();

                for (int i = 1; i < elements.length; i++) {
                    if (elements[i].length() > 0) {
                        sp.add(new KnotSpeedImpl(Double.valueOf(elements[i])));
                    } else {
                        sp.add(Speed.NULL);
                    }
                }
                speeds.put(new DegreeBearingImpl(Double.valueOf(elements[0])), sp);
                break;

            }
        }
        bfr.close();
        isr.close();

        NavigableMap<Speed, NavigableMap<Bearing, Speed>> mapSpeedTable = new TreeMap<Speed, NavigableMap<Bearing, Speed>>();
        NavigableMap<Speed, Bearing> mapBeatAngles = new TreeMap<Speed, Bearing>();
        NavigableMap<Speed, Bearing> mapJibeAngles = new TreeMap<Speed, Bearing>();
        NavigableMap<Speed, Speed> mapBeatSOG = new TreeMap<Speed, Speed>();
        NavigableMap<Speed, Speed> mapJibeSOG = new TreeMap<Speed, Speed>();

        Speed windSpeed = null;
        Speed boatSpeed = null;
        NavigableMap<Bearing, Speed> speedTableLine = null;

        for (int index = 0; index < windSpeeds.size(); index++) {
            windSpeed = windSpeeds.get(index);
            speedTableLine = new TreeMap<Bearing, Speed>(bearingComparator);

            for (Entry<Bearing, List<Speed>> entry : speeds.entrySet()) {
                if (index >= entry.getValue().size()) {
                    continue;
                }

                boatSpeed = entry.getValue().get(index);

                if (boatSpeed != Speed.NULL) {
                    speedTableLine.put(entry.getKey(), boatSpeed);
                }
            }

            mapSpeedTable.put(windSpeed, speedTableLine);
            mapBeatAngles.put(windSpeed, beatAngles.get(index));
            mapJibeAngles.put(windSpeed, jibeAngles.get(index));
            mapBeatSOG.put(windSpeed, beatSpeed.get(index));
            mapJibeSOG.put(windSpeed, jibeSpeed.get(index));
        }

        //setWind(new KnotSpeedWithBearingImpl(0, new DegreeBearingImpl(180)));

        super.speedTable = mapSpeedTable;
        super.beatAngles = mapBeatAngles;
        super.jibeAngles = mapJibeAngles;
        super.beatSOG = mapBeatSOG;
        super.jibeSOG = mapJibeSOG;

        for (Speed s : super.speedTable.keySet()) {

            if (super.beatAngles.containsKey(s) && !super.speedTable.get(s).containsKey(super.beatAngles.get(s))) {
                super.speedTable.get(s).put(super.beatAngles.get(s), super.beatSOG.get(s));
            }

            if (super.jibeAngles.containsKey(s) && !super.speedTable.get(s).containsKey(super.jibeAngles.get(s))) {
                super.speedTable.get(s).put(super.jibeAngles.get(s), super.jibeSOG.get(s));
            }

        }

    }

}
