package com.sap.sse.security.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.WildcardPermissionEncoder;

public class WildcardPermissionEncoderTest {
    private WildcardPermissionEncoder encoder = new WildcardPermissionEncoder();
    
    private void test(String s) {
        final String encoded = encoder.encodeAsPermissionPart(s);
        assertEquals(s, encoder.decodePermissionPart(encoded));
        final WildcardPermission permission = new WildcardPermission("TYPE:MODE:"+encoded);
        assertEquals(s, encoder.decodePermissionPart(permission.getParts().get(2).iterator().next()));
    }
    
    private void testPartList(String... s) {
        final String encoded = encoder.encodeStringList(s);
        assertArrayEquals(s, encoder.decodeStringList(encoded));
        final WildcardPermission permission = new WildcardPermission("TYPE:MODE:"+encoded);
        assertArrayEquals(s, encoder.decodeStringList(permission.getParts().get(2).iterator().next()));
    }
    
    @Test
    public void testSimpleList() {
        testPartList("a", "b", "c");
    }
    
    @Test
    public void testListWithBlanks() {
        testPartList("a ", " b", " c ");
    }
    
    @Test
    public void testListWithPartSeparator() {
        testPartList("a"+WildcardPermission.PART_DIVIDER_TOKEN, WildcardPermission.PART_DIVIDER_TOKEN+"b",
                WildcardPermission.PART_DIVIDER_TOKEN+"c"+WildcardPermission.PART_DIVIDER_TOKEN);
    }
    
    @Test
    public void testListWithPartAndSubpartSeparators() {
        testPartList("a"+WildcardPermission.PART_DIVIDER_TOKEN,
                WildcardPermission.PART_DIVIDER_TOKEN+"b"+WildcardPermission.SUBPART_DIVIDER_TOKEN,
                WildcardPermission.SUBPART_DIVIDER_TOKEN+WildcardPermission.PART_DIVIDER_TOKEN+"c"+
                        WildcardPermission.PART_DIVIDER_TOKEN+WildcardPermission.SUBPART_DIVIDER_TOKEN);
    }
    
    @Test
    public void testSimpleString() {
        test("abc");
    }

    @Test
    public void testPartSeparator() {
        test("ab::c:");
    }

    @Test
    public void testSubpartSeparator() {
        test("a,,bc,");
    }

    @Test
    public void testPartAndSubpartSeparator() {
        test("a,:,bc,:");
    }

    @Test
    public void testEscape() {
        test("___a,__:,bc,:_");
    }

    @Test
    public void testMixedCaseString() {
        test("abcABC");
    }

    @Test
    public void testLeadingBlank() {
        test(" abcABC");
    }

    @Test
    public void testTrailingBlank() {
        test("abcABC ");
    }

    @Test
    public void testLeadingBlanks() {
        test("  abcABC");
    }

    @Test
    public void testTrailingBlanks() {
        test("abcABC  ");
    }

    @Test
    public void testLeadingAndTrailingTabAndBlank() {
        test("\t abcABC \t");
    }
}
