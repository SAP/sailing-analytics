package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;

public class LongestCommonSubsequenceTest {
    @Test
    public void testEqualNonEmptyStrings() {
        final String a = "abcd";
        final String b = "ab" + "cd";
        assertEquals(4, Util.getLengthOfLongestCommonSubsequence(a, b));
    }

    @Test
    public void testEqualEmptyStrings() {
        final String a = "";
        final String b = "ab".substring(0, 0);
        assertEquals(0, Util.getLengthOfLongestCommonSubsequence(a, b));
    }

    @Test
    public void testOneEmptyString() {
        final String a = "";
        final String b = "ab";
        assertEquals(0, Util.getLengthOfLongestCommonSubsequence(a, b));
    }

    @Test
    public void testFullSubsequence() {   
        final String a = "abcdefg";
        final String b = "cde";
        assertEquals(3, Util.getLengthOfLongestCommonSubsequence(a, b));
    }

    @Test
    public void testFullSubsequenceB() {
        final String a = "cde";
        final String b = "abcdefg";
        assertEquals(3, Util.getLengthOfLongestCommonSubsequence(a, b));
    }

    @Test
    public void testSingleCommonSubsequence() {
        final String a = "xycdez";
        final String b = "abcdefg";
        assertEquals(3, Util.getLengthOfLongestCommonSubsequence(a, b));
    }

    @Test
    public void testMultipleCommonSubsequences() {
        final String a = "xycdezabcdppp";
        final String b = "abcdefg";
        assertEquals(4, Util.getLengthOfLongestCommonSubsequence(a, b));
    }
    
    @Test
    public void testContenderEurope() {
        final String a = "contenderopen";
        final String b = "europeint";
        assertEquals(6, Util.getLengthOfLongestCommonSubsequence(a, b)); // "e rope n"
    }
    
    @Test
    public void testNonContiguousSubSequence() {
        final String a = "abcde";
        final String b = "123a456dc789d0";
        assertEquals(3, Util.getLengthOfLongestCommonSubsequence(a, b)); // "acd"
    }
}
