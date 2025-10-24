package com.sap.sailing.simulator.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.simulator.BoatClassProperties;
import com.sap.sailing.simulator.RaceProperties;
import com.sap.sailing.simulator.impl.BoatClassPropertiesImpl;
import com.sap.sailing.simulator.impl.ConfigurationManager;
import com.sap.sailing.simulator.impl.RacePropertiesImpl;

public class ConfigurationManagerTest {

    private final List<BoatClassProperties> _boatClassesInfo = new ArrayList<BoatClassProperties>();
    private final List<RaceProperties> _racesInfo = new ArrayList<RaceProperties>();

    @BeforeEach
    public void initialize() {
        this._boatClassesInfo.add(new BoatClassPropertiesImpl("49er Static", new MeterDistance(4.995), "PolarDiagram49.csv", 0));
        this._boatClassesInfo.add(new BoatClassPropertiesImpl("49er Bethwaite", new MeterDistance(4.876), "PolarDiagram49Bethwaite.csv", 1));
        this._boatClassesInfo.add(new BoatClassPropertiesImpl("49er ORC", new MeterDistance(4.995), "PolarDiagram49ORC.csv", 2));
        this._boatClassesInfo.add(new BoatClassPropertiesImpl("49er STG", new MeterDistance(4.876), "PolarDiagram49STG.csv", 3));
        this._boatClassesInfo.add(new BoatClassPropertiesImpl("505 STG", new MeterDistance(5.05), "PolarDiagram505STG.csv", 4));

        this._racesInfo
                .add(new RacePropertiesImpl(
                        "Internationale Deutche Meisterschaft - 49er Race4",
                        "49er STG",
                        "http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=d1f521fa-ec52-11e0-a523-406186cbf87c",
                        0));
        this._racesInfo
                .add(new RacePropertiesImpl (
                        "Internationale Deutche Meisterschaft - 49er Race5",
                        "49er STG",
                        "http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=eb06795a-ec52-11e0-a523-406186cbf87c",
                        1));
        this._racesInfo
                .add(new RacePropertiesImpl (
                        "Internationale Deutche Meisterschaft - Star Race4",
                        "49er STG",
                        "http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=6bb0829e-ec44-11e0-a523-406186cbf87c",
                        2));
        this._racesInfo
                .add(new RacePropertiesImpl (
                        "Kieler Woche 2012 - 49er Yellow - Race 1",
                        "49er STG",
                        "http://germanmaster.traclive.dk/events/event_20120615_KielerWoch/clientparams.php?event=event_20120615_KielerWoch&race=0b5969cc-b789-11e1-a845-406186cbf87c",
                        3));
    }

    @Test
    public void test_getPolarDiagramFileLocation() {
        Assertions.assertEquals("PolarDiagram49.csv", ConfigurationManager.INSTANCE
                .getPolarDiagramFileLocation(0));
        Assertions.assertEquals("PolarDiagram49Bethwaite.csv", ConfigurationManager.INSTANCE
                .getPolarDiagramFileLocation(1));
        Assertions.assertEquals("PolarDiagram49ORC.csv", ConfigurationManager.INSTANCE
                .getPolarDiagramFileLocation(2));
        Assertions.assertEquals("PolarDiagram49STG.csv", ConfigurationManager.INSTANCE
                .getPolarDiagramFileLocation(3));
        Assertions.assertEquals("PolarDiagram505STG.csv", ConfigurationManager.INSTANCE
                .getPolarDiagramFileLocation(4));
    }

    @Test
    public void test_getBoatClassesInfoCount() {
        Assertions.assertEquals(5, ConfigurationManager.INSTANCE.getBoatClassesInfoCount());
    }

    @Test
    public void test_getBoatClassesInfo() {
        int index = 0;
        for (final BoatClassProperties tuple : ConfigurationManager.INSTANCE.getBoatClassesInfo()) {
            Assertions.assertEquals(this._boatClassesInfo.get(index).getName(), tuple.getName());
            Assertions.assertEquals(this._boatClassesInfo.get(index).getLength(), tuple.getLength());
            Assertions.assertEquals(this._boatClassesInfo.get(index).getPolar(), tuple.getPolar());
            index++;
        }
    }

    @Test
    public void test_getRacesInfo() {
        int index = 0;
        for (final RaceProperties tuple : ConfigurationManager.INSTANCE.getRacesInfo()) {
            Assertions.assertEquals(this._racesInfo.get(index).getName(), tuple.getName());
            Assertions.assertEquals(this._racesInfo.get(index).getBoatClass(), tuple.getBoatClass());
            Assertions.assertEquals(this._racesInfo.get(index).getURL(), tuple.getURL());
            index++;
        }
    }
}
