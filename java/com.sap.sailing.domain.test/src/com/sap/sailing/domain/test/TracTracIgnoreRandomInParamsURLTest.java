package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.tractracadapter.TracTracRaceTracker;
import com.sap.sailing.domain.tractracadapter.impl.TracTracRaceTrackerImpl;

/**
 * See bug 3174: the addition of the "random" element in the params_url around summer 2015 has killed our
 * comparison of {@link TracTracRaceTracker#getID()} objects. Although two URLs differ in this "random" parameter,
 * they still refer to the same race.<p>
 * 
 * This test asserts that two TracTrac race tracker IDs differing only in the "random" URL parameter are still considered
 * equal.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class TracTracIgnoreRandomInParamsURLTest {
    @Test
    public void testEqualRaceTrackerIDsThatOnlyDifferInRandomParam() throws MalformedURLException, URISyntaxException {
        Object id1 = TracTracRaceTrackerImpl.createID(new URL("http://event.tractrac.com/events/event_20150707_erEuropean/clientparams.php?event=event_20150707_erEuropean&race=777b1420-07c1-0133-0cec-60a44ce903c3&random=769909699"),
                new URI("tcp://event.tractrac.com:4400"), new URI("tcp://event.tractrac.com:4401"));
        Object id2 = TracTracRaceTrackerImpl.createID(new URL("http://event.tractrac.com/events/event_20150707_erEuropean/clientparams.php?event=event_20150707_erEuropean&race=777b1420-07c1-0133-0cec-60a44ce903c3&random=1794448899"),
                new URI("tcp://event.tractrac.com:4400"), new URI("tcp://event.tractrac.com:4401"));
        assertEquals(id1, id2);
    }
    
    @Test
    public void testStripper() throws MalformedURLException {
        assertEquals("http://club.tractrac.com/events/event_20180129_Australian/clientparams.php?event=event_20180129_Australian&race=3b53e970-edca-0135-8676-10bf48d758ce",
                TracTracRaceTrackerImpl.getParamURLStrippedOfRandomParam(new URL("http://club.tractrac.com/events/event_20180129_Australian/clientparams.php?event=event_20180129_Australian&race=3b53e970-edca-0135-8676-10bf48d758ce&random=2000192849")).toString());
    }

    @Test
    public void testEqualRaceTrackerIDsThatOnlyDifferInRandomParamAndNotLastParameter() throws MalformedURLException, URISyntaxException {
        Object id1 = TracTracRaceTrackerImpl.createID(new URL("http://event.tractrac.com/events/event_20150707_erEuropean/clientparams.php?event=event_20150707_erEuropean&race=777b1420-07c1-0133-0cec-60a44ce903c3&random=769909699&a=b"),
                new URI("tcp://event.tractrac.com:4400"), new URI("tcp://event.tractrac.com:4401"));
        Object id2 = TracTracRaceTrackerImpl.createID(new URL("http://event.tractrac.com/events/event_20150707_erEuropean/clientparams.php?event=event_20150707_erEuropean&race=777b1420-07c1-0133-0cec-60a44ce903c3&random=1794448899&a=b"),
                new URI("tcp://event.tractrac.com:4400"), new URI("tcp://event.tractrac.com:4401"));
        assertEquals(id1, id2);
    }

    @Test
    public void testEqualRaceTrackerIDsThatOnlyDifferInRandomParamWithFragment() throws MalformedURLException, URISyntaxException {
        Object id1 = TracTracRaceTrackerImpl.createID(new URL("http://event.tractrac.com/events/event_20150707_erEuropean/clientparams.php?event=event_20150707_erEuropean&race=777b1420-07c1-0133-0cec-60a44ce903c3&random=769909699&a=b#HumbaHumba"),
                new URI("tcp://event.tractrac.com:4400"), new URI("tcp://event.tractrac.com:4401"));
        Object id2 = TracTracRaceTrackerImpl.createID(new URL("http://event.tractrac.com/events/event_20150707_erEuropean/clientparams.php?event=event_20150707_erEuropean&race=777b1420-07c1-0133-0cec-60a44ce903c3&random=1794448899&a=b#HumbaHumba"),
                new URI("tcp://event.tractrac.com:4400"), new URI("tcp://event.tractrac.com:4401"));
        assertEquals(id1, id2);
    }

    @Test
    public void testEqualRaceTrackerIDsThatOnlyDifferInRandomParamAndFragment() throws MalformedURLException, URISyntaxException {
        Object id1 = TracTracRaceTrackerImpl.createID(new URL("http://event.tractrac.com/events/event_20150707_erEuropean/clientparams.php?event=event_20150707_erEuropean&race=777b1420-07c1-0133-0cec-60a44ce903c3&random=769909699&a=b#HumbaTrala"),
                new URI("tcp://event.tractrac.com:4400"), new URI("tcp://event.tractrac.com:4401"));
        Object id2 = TracTracRaceTrackerImpl.createID(new URL("http://event.tractrac.com/events/event_20150707_erEuropean/clientparams.php?event=event_20150707_erEuropean&race=777b1420-07c1-0133-0cec-60a44ce903c3&random=1794448899&a=b#HumbaHumba"),
                new URI("tcp://event.tractrac.com:4400"), new URI("tcp://event.tractrac.com:4401"));
        assertFalse(id1.equals(id2));
    }

    @Test
    public void testEqualRaceTrackerIDsThatDifferInRaceIDAndRandomParam() throws MalformedURLException, URISyntaxException {
        Object id1 = TracTracRaceTrackerImpl.createID(new URL("http://event.tractrac.com/events/event_20150707_erEuropean/clientparams.php?event=event_20150707_erEuropean&race=777b1420-07c1-0132-0cec-60a44ce903c3&random=769909699"),
                new URI("tcp://event.tractrac.com:4400"), new URI("tcp://event.tractrac.com:4401"));
        Object id2 = TracTracRaceTrackerImpl.createID(new URL("http://event.tractrac.com/events/event_20150707_erEuropean/clientparams.php?event=event_20150707_erEuropean&race=777b1420-07c1-0133-0cec-60a44ce903c3&random=1794448899"),
                new URI("tcp://event.tractrac.com:4400"), new URI("tcp://event.tractrac.com:4401"));
        assertFalse(id1.equals(id2));
    }

    @Test
    public void testEqualRaceTrackerIDsThatDifferInLiveURI() throws MalformedURLException, URISyntaxException {
        Object id1 = TracTracRaceTrackerImpl.createID(new URL("http://event.tractrac.com/events/event_20150707_erEuropean/clientparams.php?event=event_20150707_erEuropean&race=777b1420-07c1-0132-0cec-60a44ce903c3&random=769909699"),
                new URI("tcp://event.tractrac.com:4400"), new URI("tcp://event.tractrac.com:4401"));
        Object id2 = TracTracRaceTrackerImpl.createID(new URL("http://event.tractrac.com/events/event_20150707_erEuropean/clientparams.php?event=event_20150707_erEuropean&race=777b1420-07c1-0132-0cec-60a44ce903c3&random=769909699"),
                new URI("tcp://event.tractrac.com:4412"), new URI("tcp://event.tractrac.com:4413"));
        assertTrue(id1.equals(id2)); // with the fix for bug 5308 we now assume that the live and stored URI are insignificant for the tracker ID
    }
}
