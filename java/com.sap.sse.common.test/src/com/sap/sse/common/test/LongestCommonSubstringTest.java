package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;

public class LongestCommonSubstringTest {
    @Test
    public void testEqualNonEmptyStrings() {
        final String a = "abcd";
        final String b = "ab" + "cd";
        assertEquals(4, Util.getLengthOfLongestCommonSubstring(a, b));
    }

    @Test
    public void testEqualEmptyStrings() {
        final String a = "";
        final String b = "ab".substring(0, 0);
        assertEquals(0, Util.getLengthOfLongestCommonSubstring(a, b));
    }

    @Test
    public void testOneEmptyString() {
        final String a = "";
        final String b = "ab";
        assertEquals(0, Util.getLengthOfLongestCommonSubstring(a, b));
    }

    @Test
    public void testFullSubstring() {   
        final String a = "abcdefg";
        final String b = "cde";
        assertEquals(3, Util.getLengthOfLongestCommonSubstring(a, b));
    }

    @Test
    public void testFullSubstringB() {
        final String a = "cde";
        final String b = "abcdefg";
        assertEquals(3, Util.getLengthOfLongestCommonSubstring(a, b));
    }

    @Test
    public void testSingleCommonSubstring() {
        final String a = "xycdez";
        final String b = "abcdefg";
        assertEquals(3, Util.getLengthOfLongestCommonSubstring(a, b));
    }

    @Test
    public void testMultipleCommonSubstrings() {
        final String a = "xycdezabcdppp";
        final String b = "abcdefg";
        assertEquals(4, Util.getLengthOfLongestCommonSubstring(a, b)); // "abcd"
    }
    
    @Test
    public void testContenderEurope() {
        final String a = "contenderopen";
        final String b = "europeint";
        assertEquals(4, Util.getLengthOfLongestCommonSubstring(a, b)); // "rope"
    }
    
    @Test
    public void testNonContiguousSubstring() {
        final String a = "abcde";
        final String b = "123a456dc789d0";
        assertEquals(1, Util.getLengthOfLongestCommonSubstring(a, b)); // "a" "b" "c" "d"
    }
    
    @Test
    public void testOverlappingInOrder() {
        final String a = "abcde";
        final String b = "abc123cde";
        assertEquals(3, Util.getLengthOfLongestCommonSubstring(a, b)); // "abc" "cde"
    }
    
    @Test
    public void testOverlappingReverseOrder() {
        final String a = "abcde";
        final String b = "cdefabcd";
        assertEquals(4, Util.getLengthOfLongestCommonSubstring(a, b)); // "abcd"
    }
}
