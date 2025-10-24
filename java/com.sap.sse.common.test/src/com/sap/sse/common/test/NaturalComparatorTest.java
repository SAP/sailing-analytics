package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.util.NaturalComparator;

public class NaturalComparatorTest {

    private NaturalComparator comparator;
    private NaturalComparator caseInsensitive;

    @BeforeEach
    public void setUp() {
        comparator = new NaturalComparator();
        caseInsensitive = new NaturalComparator(false);
    }
    
    @Test 
    public void testCaseInsensitive(){
        assertEquals(0, caseInsensitive.compare("E", "e"));
    }
    
    @Test
    public void testWithAngleBracket() {
        assertTrue(caseInsensitive.compare("[4.0kn - 7.0kn[ Light breeze", "[11.0kn - 16.0kn[ Moderate breeze")<0);
    }

    @Test 
    public void testCaseInsensitiveIsDifferent(){
        assertEquals(-1, caseInsensitive.compare("E", "F"));
        assertEquals(-1, caseInsensitive.compare("E", "f"));
        assertEquals(1, caseInsensitive.compare("F", "E"));
        assertEquals(1, caseInsensitive.compare("f", "E"));
    }

    @Test
    public void testEmpty() {
        assertTrue(comparator.compare("", "a") < 0);
        assertTrue(comparator.compare("a", "") > 0);
        assertEquals(0, comparator.compare("", ""));
    }

    @Test
    public void testSimpleCharOrder() {
        assertTrue(comparator.compare("a", "b") < 0);
        assertTrue(comparator.compare("aa", "ab") < 0);
        assertTrue(comparator.compare("aaa", "aab") < 0);
        assertTrue(comparator.compare("aaa", "aaab") < 0);
        assertEquals(0, comparator.compare("abc", "abc"));
    }

    @Test
    public void testSimpleNumberOrder() {
        assertTrue(comparator.compare("0", "1") < 0);
        assertTrue(comparator.compare("00", "1") < 0);
        assertTrue(comparator.compare("2", "6") < 0);
        assertTrue(comparator.compare("9", "10") < 0);
        assertTrue(comparator.compare("9", "100") < 0);
        assertTrue(comparator.compare("198", "200") < 0);
        assertEquals(0, comparator.compare("0", "0"));
    }

    @Test
    public void testCaseOrder() {
        assertTrue(comparator.compare("A", "a") < 0);
        assertTrue(comparator.compare("Z", "a") < 0);
        assertEquals(0, comparator.compare("AbCd123", "AbCd123"));
    }

    @Test
    public void testLeadingOrder() {
        assertTrue(comparator.compare("0001", "002") < 0);
        assertTrue(comparator.compare("  0 0 1", "2") < 0);
        assertTrue(comparator.compare("002b", "2c") < 0);
        assertTrue(comparator.compare("001a", "0001a") < 0);
        assertTrue(comparator.compare("0001a", "001b") < 0);
        assertEquals(0, comparator.compare("01", "01"));
    }

    @Test
    public void testDigitVsCharOrder() {
        assertTrue(comparator.compare("0", "a") < 0);
        assertTrue(comparator.compare("0", "A") < 0);
        assertTrue(comparator.compare("9", "a") < 0);
        assertTrue(comparator.compare("9", "A") < 0);
    }

    @Test
    public void testMixedOrder() {
        assertTrue(comparator.compare("a42", "a300") < 0);
        assertTrue(comparator.compare("a1b2c3d4e5", "a1b2c9d4e5") < 0);
        assertTrue(comparator.compare("a1b2c3d4e5", "a1b2z3d4e5") < 0);

        assertTrue(comparator.compare("a1b2 c3d4e5", "a1b2c3d4e5") < 0);

        assertTrue(comparator.compare("a9b11", "a10b11") < 0);
        assertTrue(comparator.compare("a9b9", "a9b10") < 0);

        assertTrue(comparator.compare("a12", "a123") < 0);
        assertTrue(comparator.compare("123", "1234") < 0);
        assertTrue(comparator.compare("12abc", "12abcd5") < 0);

        assertTrue(comparator.compare("R1", "R10") < 0);
        assertTrue(comparator.compare("R 1", "R 10") < 0);
        assertTrue(comparator.compare("G1", "G10") < 0);
        assertTrue(comparator.compare("G 1", "G 10") < 0);
    }

    @Test
    public void testDotsOrder() {
        assertTrue(comparator.compare("0.1", "0.2") < 0);
        assertTrue(comparator.compare("0.01", "0.2") < 0);
        assertTrue(comparator.compare("0.1.5b", "0.2a") < 0);
    }
    
    @Test
    public void testSortSailIDs() {
        List<String> list = Arrays.asList("Groupama", "GER8829", "A", "FRA 56893", "Cam", "GAC Pindar", "Grün", "a/m", "d/n");
        Collections.sort(list, new NaturalComparator(/* case sensitive */ false));
        assertTrue(list.indexOf("A") < list.indexOf("Cam"));
    }

}
