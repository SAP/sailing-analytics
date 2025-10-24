package com.sap.sailing.server.gateway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sailing.server.gateway.windimport.bravo.FunnyDegreeConverter;

public class FunnyDegreeConverterTest {
    @Test
    public void testDegreeConverter() {
        assertEquals(41.410765, FunnyDegreeConverter.funnyLatLng(4124.645890), 0.000001);
        assertEquals(2.228978,  FunnyDegreeConverter.funnyLatLng(213.738670), 0.000001);
        assertEquals(-41.410765, FunnyDegreeConverter.funnyLatLng(-4124.645890), 0.000001);
        assertEquals(-2.228978,  FunnyDegreeConverter.funnyLatLng(-213.738670), 0.000001);
    }
}
