package com.sap.sailing.domain.test.markpassing;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

public class OBMR2012VRMarkPassingTest extends AbstractMarkPassingTest {

    public OBMR2012VRMarkPassingTest() throws MalformedURLException, URISyntaxException {
        super();
    }

    @Test
    public void testRaceFr11() throws Exception {
        testRace("Fr_Fleet_1_1");
    }

    @Test
    public void testRaceFr12() throws Exception {
        testRace("Fr_Fleet_1_2");
    }

    @Test
    public void testRaceFr13() throws Exception {
        testRace("Fr_Fleet_1_3");
    }

    @Test
    public void testRaceFr21() throws Exception {
        testRace("Fr_Fleet_2_1");
    }

    @Test
    public void testRaceFr22() throws Exception {
        testRace("Fr_Fleet_2_2");
    }

    @Test
    public void testRaceFr23() throws Exception {
        testRace("Fr_Fleet_2_3");
    }

    @Test
    public void testRaceSaVm11() throws Exception {
        testRace("Sa-Vm_Fleet_1_1");
    }

    @Test
    public void testRaceSaVm12() throws Exception {
        testRace("Sa-Vm_Fleet_1_2");
    }

    @Test
    public void testRaceSaVm13() throws Exception {
        testRace("Sa-Vm_Fleet_1_3");
    }

    @Test
    public void testRaceSaVm21() throws Exception {
        testRace("Sa-Vm_Fleet_2_1");
    }

    @Test
    public void testRaceSaVm22() throws Exception {
        testRace("Sa-Vm_Fleet_2_2");
    }

    @Override
    protected String getExpectedEventName() {
        return "OBMR 2012";
    }

    @Override
    protected String getFileName() {
        return "event_20121031_OBMR-OBMR_2012_VR_";
    }
}
