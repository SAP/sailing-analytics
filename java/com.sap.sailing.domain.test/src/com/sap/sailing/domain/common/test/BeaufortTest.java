package com.sap.sailing.domain.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.impl.MeterPerSecondSpeedImpl;
import com.sap.sse.common.Speed;

public class BeaufortTest {
    @Test
    public void testOneBeaufortLow() {
        Speed speed = new MeterPerSecondSpeedImpl(0.3);
        assertEquals(1, Math.round(speed.getBeaufort()));
    }

    @Test
    public void testOneBeaufortHigh() {
        Speed speed = new MeterPerSecondSpeedImpl(1.5);
        assertEquals(1, Math.round(speed.getBeaufort()));
    }

    @Test
    public void testTwoBeaufortLow() {
        Speed speed = new MeterPerSecondSpeedImpl(1.6);
        assertEquals(2, Math.round(speed.getBeaufort()));
    }

    @Test
    public void testTwoBeaufortHigh() {
        Speed speed = new MeterPerSecondSpeedImpl(3.3);
        assertEquals(2, Math.round(speed.getBeaufort()));
    }

    @Test
    public void testTenBeaufortLow() {
        Speed speed = new MeterPerSecondSpeedImpl(24.5);
        assertEquals(10, Math.round(speed.getBeaufort()));
    }

    @Test
    public void testTenBeaufortHigh() {
        Speed speed = new MeterPerSecondSpeedImpl(28.4);
        assertEquals(10, Math.round(speed.getBeaufort()));
    }
}
