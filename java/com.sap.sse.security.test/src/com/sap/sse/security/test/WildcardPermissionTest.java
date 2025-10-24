package com.sap.sse.security.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sse.security.shared.WildcardPermission;

public class WildcardPermissionTest {
    @Test
    public void simpleToStringParseTest() {
        WildcardPermission p = new WildcardPermission("a:b,c:*");
        assertEquals(p, new WildcardPermission(p.toString()));
    }
}
