package com.sap.sailing.geocoding.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.Placemark;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.PlacemarkImpl;

public class PlacemarkTest {
    @Test
    public void placemarkEqualsTest() {
        Placemark p1 = new PlacemarkImpl("Kiel", "DE", "Germany", new DegreePosition(55, 10), "P");
        Placemark p2 = new PlacemarkImpl("Kiel", "DE", "Germany", new DegreePosition(55, 10), "P");
        Assertions.assertEquals(p1, p2);
    }
}
