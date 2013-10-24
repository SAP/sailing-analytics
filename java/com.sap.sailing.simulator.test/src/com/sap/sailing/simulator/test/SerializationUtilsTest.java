package com.sap.sailing.simulator.test;

import junit.framework.Assert;

import org.junit.Test;

import com.sap.sailing.simulator.impl.SimulatorUtils;

public class SerializationUtilsTest {

    @Test
    public void test_getRaceIt() {

        Assert.assertEquals(
                "d1f521fa-ec52-11e0-a523-406186cbf87c",
                SimulatorUtils
                        .getRaceID("http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=d1f521fa-ec52-11e0-a523-406186cbf87c"));
        Assert.assertEquals(
                "eb06795a-ec52-11e0-a523-406186cbf87c",
                SimulatorUtils
                        .getRaceID("http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=eb06795a-ec52-11e0-a523-406186cbf87c"));
        Assert.assertEquals(
                "6bb0829e-ec44-11e0-a523-406186cbf87c",
                SimulatorUtils
                        .getRaceID("http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=6bb0829e-ec44-11e0-a523-406186cbf87c"));
        Assert.assertEquals(
                "0b5969cc-b789-11e1-a845-406186cbf87c",
                SimulatorUtils
                        .getRaceID("http://germanmaster.traclive.dk/events/event_20120615_KielerWoch/clientparams.php?event=event_20120615_KielerWoch&race=0b5969cc-b789-11e1-a845-406186cbf87c"));

    }
}
