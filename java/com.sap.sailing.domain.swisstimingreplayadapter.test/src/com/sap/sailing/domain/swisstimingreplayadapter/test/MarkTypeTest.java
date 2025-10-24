package com.sap.sailing.domain.swisstimingreplayadapter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.swisstimingreplayadapter.impl.MarkType;

public class MarkTypeTest {
    
    @Test
    public void testNone() {
        byte bitCode = (byte) 0;
        MarkType markType = new MarkType(bitCode);
        assertFalse(markType.isVisible());
        assertFalse(markType.isCouple());
        assertFalse(markType.isStart());
        assertTrue(markType.isFinishOrCourse());
        assertFalse(markType.isFinish());
        assertTrue(markType.isStartOrCourse());
        assertFalse(markType.isTurn());
        assertTrue(markType.isStraight());
        assertFalse(markType.isBoat());
        assertFalse(markType.isBuoy());
        assertTrue(markType.isPin());
        assertFalse(markType.isMeasure());
    }
    
    @Test
    public void testVisibleAndCouple() {
        byte bitCode = (byte) (MarkType.IS_VISIBLE | MarkType.IS_COUPLE);
        MarkType markType = new MarkType(bitCode);
        assertTrue(markType.isVisible());
        assertTrue(markType.isCouple());
        assertFalse(markType.isStart());
        assertTrue(markType.isFinishOrCourse());
        assertFalse(markType.isFinish());
        assertTrue(markType.isStartOrCourse());
        assertFalse(markType.isTurn());
        assertTrue(markType.isStraight());
        assertFalse(markType.isBoat());
        assertFalse(markType.isBuoy());
        assertTrue(markType.isPin());
        assertFalse(markType.isMeasure());
    }
    
    @Test
    public void testFinishAndStart() {
        byte bitCode = (byte) (MarkType.IS_FINISH| MarkType.IS_START);
        MarkType markType = new MarkType(bitCode);
        assertFalse(markType.isVisible());
        assertFalse(markType.isCouple());
        assertTrue(markType.isStart());
        assertFalse(markType.isFinishOrCourse());
        assertTrue(markType.isFinish());
        assertFalse(markType.isStartOrCourse());
        assertFalse(markType.isTurn());
        assertTrue(markType.isStraight());
        assertFalse(markType.isBoat());
        assertFalse(markType.isBuoy());
        assertTrue(markType.isPin());
        assertFalse(markType.isMeasure());
    }
    
    @Test
    public void testBuoyBoatAndMeasure() {
        byte bitCode = (byte) (MarkType.IS_BUOY| MarkType.IS_BOAT | MarkType.IS_MEASURE);
        MarkType markType = new MarkType(bitCode);
        assertFalse(markType.isVisible());
        assertFalse(markType.isCouple());
        assertFalse(markType.isStart());
        assertTrue(markType.isFinishOrCourse());
        assertFalse(markType.isFinish());
        assertTrue(markType.isStartOrCourse());
        assertFalse(markType.isTurn());
        assertTrue(markType.isStraight());
        assertTrue(markType.isBoat());
        assertTrue(markType.isBuoy());
        assertFalse(markType.isPin());
        assertTrue(markType.isMeasure());
    }
    
    @Test
    public void testVisibleAndCoupleToString() {
        byte bitCode = (byte) (MarkType.IS_VISIBLE | MarkType.IS_COUPLE);
        MarkType markType = new MarkType(bitCode);
        assertEquals("[VISIBLE, COUPLE, turn, finish, start, measure, boat, buoy]", markType.toString());
    }
    
    @Test
    public void testBuoyToString() {
        byte bitCode = (byte) (MarkType.IS_BUOY);
        MarkType markType = new MarkType(bitCode);
        assertEquals("[visible, couple, turn, finish, start, measure, boat, BUOY]", markType.toString());
    }
    
    @Test
    public void testAllToString() {
        byte bitCode = (byte) (0xFF);
        MarkType markType = new MarkType(bitCode);
        assertEquals("[VISIBLE, COUPLE, TURN, FINISH, START, MEASURE, BOAT, BUOY]", markType.toString());
    }
    

}
