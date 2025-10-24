package com.sap.sailing.geocoding.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.Placemark;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.PlacemarkImpl;
import com.sap.sailing.geocoding.ReverseGeocoder;
import com.sap.sailing.geocoding.impl.ReverseGeocoderImpl;

public class ReverseGeocoderTest {
    private ReverseGeocoder geocoder;
    private static final Placemark KIEL = new PlacemarkImpl("Kiel", "DE", new DegreePosition(54.32132926107913, 10.1348876953125), 232758);
    private static final Position KIEL_POSITION = new DegreePosition(54.32217976191047, 10.133570443980922);
    
    @BeforeEach
    public void setUp() {
        geocoder = new ReverseGeocoderImpl(); // ensure we don't see any caching effects across test case executions
    }
    
    @Test
    public void getByNameTest() throws IOException, ParseException {
        final Placemark kiel = geocoder.getPlacemark("Kiel", new Placemark.ByPopulation().reversed());
        assertNotNull(kiel);
        assertTrue(kiel.getPopulation() > 200000);
        assertEquals("DE", kiel.getCountryCode());
    }
    
    @Test
    public void getPlacemarkSimpleTest() {
        //Simple Test in Kiel center to check the connection and the parsing from JSONObject to Placemark
        try {
            Placemark kielReversed = geocoder.getPlacemarkNearest(KIEL_POSITION);
            Assertions.assertEquals(KIEL.getName(), kielReversed.getName());
            Assertions.assertEquals(KIEL.getCountryCode(), kielReversed.getCountryCode());
            Assertions.assertEquals(KIEL.getPosition().getLatDeg(), kielReversed.getPosition().getLatDeg(), 0.0001);
            Assertions.assertEquals(KIEL.getPosition().getLngDeg(), kielReversed.getPosition().getLngDeg(), 0.0001);
        } catch (IOException e) {
            Assertions.fail(e.getMessage());
        } catch (ParseException e) {
            Assertions.fail(e.getMessage());
        }
    }
    
    @Test
    public void getPlacemarkNearSimpleTest() {
        try {
            List<Placemark> placemarks = geocoder.getPlacemarksNear(KIEL_POSITION, 20);
            assertNotNull(placemarks);
            Assertions.assertFalse(placemarks.isEmpty());
        } catch (IOException e) {
            Assertions.fail(e.getMessage());
        } catch (ParseException e) {
            Assertions.fail(e.getMessage());
        }
    }
    
    @Test
    public void getPlacemarkBestTest() {
        final Position abroad = new DegreePosition(54.429758, 10.289335);
        final Placemark firstByDistance = new PlacemarkImpl("Wendtorf", "DE", new DegreePosition(54.41212, 10.28952), 1139);
        final Placemark firstByDistanceVariation = new PlacemarkImpl("Wendtorfer Strand", "DE", new DegreePosition(54.41212, 10.28952), 1139);
        try {
            Placemark p = geocoder.getPlacemarkLast(abroad, 20, new Placemark.ByPopulation());
            Assertions.assertEquals(KIEL.getName(), p.getName());
            Assertions.assertEquals(KIEL.getCountryCode(), p.getCountryCode());
            Assertions.assertEquals(KIEL.getPosition().getLatDeg(), p.getPosition().getLatDeg(), 0.0001);
            Assertions.assertEquals(KIEL.getPosition().getLngDeg(), p.getPosition().getLngDeg(), 0.0001);
            
            p = geocoder.getPlacemarkFirst(abroad, 20, new Placemark.ByDistance(abroad));
            Assertions.assertTrue(firstByDistance.getName().equals(p.getName()) || firstByDistanceVariation.getName().equals(p.getName()),
                    ""+p.getName()+" neither equals "+firstByDistance.getName()+" nor "+firstByDistanceVariation.getName());
            Assertions.assertEquals(firstByDistance.getCountryCode(), p.getCountryCode());
            Assertions.assertEquals(firstByDistance.getPosition().getLatDeg(), p.getPosition().getLatDeg(), 0.005);
            Assertions.assertEquals(firstByDistance.getPosition().getLngDeg(), p.getPosition().getLngDeg(), 0.012);
            
            p = geocoder.getPlacemarkLast(abroad, 20, new Placemark.ByPopulationDistanceRatio(abroad));
            Assertions.assertEquals(KIEL.getName(), p.getName());
            Assertions.assertEquals(KIEL.getCountryCode(), p.getCountryCode());
            Assertions.assertEquals(KIEL.getPosition().getLatDeg(), p.getPosition().getLatDeg(), 0.005);
            Assertions.assertEquals(KIEL.getPosition().getLngDeg(), p.getPosition().getLngDeg(), 0.005);
        } catch (IOException e) {
            Assertions.fail(e.getMessage());
        } catch (ParseException e) {
            Assertions.fail(e.getMessage());
        }
    }
    
    @Test
    public void getPlacemarkNearWithOffshorePosition() {
        Position offshore = new DegreePosition(75.16330024622059, -0.087890625);
        long radius = 300; 
        try {
            List<Placemark> placemarks = geocoder.getPlacemarksNear(offshore, radius);
            Assertions.assertNull(placemarks);
        } catch (IOException e) {
            Assertions.fail(e.getMessage());
        } catch (ParseException e) {
            Assertions.fail(e.getMessage());
        }
    }
}
