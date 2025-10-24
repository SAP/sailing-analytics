package com.sap.sailing.domain.test.markpassing;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

public class FlorianopolisMarkPassingTest extends AbstractMarkPassingTest {

    public FlorianopolisMarkPassingTest() throws MalformedURLException, URISyntaxException {
        super();
    }

    @Test
    public void testRace3() throws Exception {
        testRace("3");
    }

    @Test
    public void testRace4() throws Exception {
        testRace("4");
    }

    @Test
    public void testRace1() throws Exception {
        testRace("1");
    }

    @Test
    public void testRace2() throws Exception {
        testRace("2");
    }

    @Test
    public void testRace5() throws Exception {
        testRace("5");
    }

    @Test
    public void testRace6() throws Exception {
        testRace("6");
    }

    @Test
    public void testRace7() throws Exception {
        testRace("7");
    }

    @Test
    public void testRace8() throws Exception {
        testRace("8");
    }

    @Test
    public void testRace10() throws Exception {
        testRace("10");
    }

    @Test
    public void testRace11() throws Exception {
        testRace("11");
    }

    @Test
    public void testRace12() throws Exception {
        testRace("12");
    }

    @Test
    public void testRace13() throws Exception {
        testRace("13");
    }

    @Test
    public void testRace17() throws Exception {
        testRace("17");
    }

    @Test
    public void testRace19() throws Exception {
        testRace("19");
    }

    @Test
    public void testRace22() throws Exception {
        testRace("22");
    }

    @Test
    public void testRace24() throws Exception {
        testRace("24");
    }

    /*
     * Alinghi acting wierd
     * 
     * @Test public void testRace14() throws IOException, InterruptedException, URISyntaxException, ParseException,
     * SubscriberInitializationException, CreateModelException { testRace("14"); }
     * 
     * @Test public void testRace23() throws IOException, InterruptedException, URISyntaxException { testRace(23); }
     */

    /*
     * Wrong start from TracTrac fails start test.
     * 
     * @Test public void testRace18() throws IOException, InterruptedException, URISyntaxException { testRace("18"); }
     */

    /*
     * Fails in Hudson...
     * 
     * @Test public void testRace16() throws IOException, InterruptedException, URISyntaxException, ParseException,
     * SubscriberInitializationException, CreateModelException { testRace("16"); }
     */

    /*
     * All mark data seems to be missing
     * 
     * @Test public void testRace9() throws IOException, InterruptedException, URISyntaxException { testRace(9); }
     * 
     * @Test public void testRace20() throws IOException, InterruptedException, URISyntaxException { testRace(20); }
     */

    /*
     * Missing static mark info leads to skipping of all finishes.
     * 
     * @Test public void testRace25() throws IOException, InterruptedException, URISyntaxException { testRace(25); }
     * 
     * @Test public void testRace21() throws IOException, InterruptedException, URISyntaxException { testRace(21); }
     * 
     * @Test public void testRace15() throws IOException, InterruptedException, URISyntaxException { testRace(15); }
     * 
     * @Test public void testRace26() throws IOException, InterruptedException, URISyntaxException { testRace(26); }
     */
    @Override
    protected String getExpectedEventName() {
        return "ESS Florianopolis 2013";
    }

    @Override
    protected String getFileName() {
        return "event_20131112_ESSFlorian-Race_";
    }
}
