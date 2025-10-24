package com.sap.sse.security.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.apache.shiro.authz.Permission;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.security.UsernamePasswordRealm;
import com.sap.sse.security.impl.PermissionConverter;
import com.sap.sse.security.shared.WildcardPermission;

public class PermissionConverterTest {
    @Test
    public void testPermissionParts() {
        List<Set<String>> parts = new PermissionConverter().getPermissionParts(new WildcardPermission("LEADERBOARD:EDIT:KW2017 Laser Int."));
        assertTrue(parts.get(0).contains("LEADERBOARD"));
        assertTrue(parts.get(1).contains("EDIT"));
        assertTrue(parts.get(2).contains("KW2017 Laser Int."));
    }

    @Test
    public void testSimpleObjectId() {
        Iterable<String> parts = new PermissionConverter().getObjectIdsAsString(new WildcardPermission("LEADERBOARD:EDIT:KW2017 Laser Int."));
        assertEquals(1, Util.size(parts));
        assertEquals("LEADERBOARD/KW2017 Laser Int.", parts.iterator().next());
    }

    @Test
    public void testTwoObjectIds() {
        Iterable<String> parts = new PermissionConverter().getObjectIdsAsString(new WildcardPermission("LEADERBOARD:EDIT:KW2017 Laser Int.,SWC 2017 Miami N/17"));
        assertEquals(2, Util.size(parts));
        assertEquals("LEADERBOARD/KW2017 Laser Int.", Util.get(parts, 0));
        assertEquals("LEADERBOARD/SWC 2017 Miami N\\/17", Util.get(parts, 1));
    }

    @Test
    public void testTwoObjectIdsOneWithLeadingBlank() {
        Iterable<String> parts = new PermissionConverter().getObjectIdsAsString(new WildcardPermission("LEADERBOARD:EDIT:KW2017 Laser Int., SWC 2017 Miami N/17"));
        assertEquals(2, Util.size(parts));
        assertEquals("LEADERBOARD/KW2017 Laser Int.", Util.get(parts, 0));
        assertEquals("LEADERBOARD/ SWC 2017 Miami N\\/17", Util.get(parts, 1));
    }
    
    @Test
    public void testGetWildcardPermission() {
        com.sap.sse.security.shared.WildcardPermission wp = new PermissionConverter().getWildcardPermission(new org.apache.shiro.authz.permission.WildcardPermission("LEADERBOARD:EDIT:KW2017 Laser Int.", /* case sensitive */ true));
        assertEquals("LEADERBOARD:EDIT:KW2017 Laser Int.", wp.toString());
    }

    @Test
    public void testGetWildcardPermissionWithMultipleObjectsInThirdPart() {
        com.sap.sse.security.shared.WildcardPermission wp = new PermissionConverter().getWildcardPermission(new org.apache.shiro.authz.permission.WildcardPermission("LEADERBOARD:EDIT:KW2017 Laser Int.,KW2024 [ILCA7]", /* case sensitive */ true));
        assertEquals("LEADERBOARD:EDIT:KW2017 Laser Int.,KW2024 [ILCA7]", wp.toString());
    }
    
    @Test
    public void testMixedCaseWildcardPermissionWithDedicatedRealm() {
        final String typeName = "LEADERBOARD";
        final String mode = "EDIT";
        final String objectID = "KW2017 Laser Int.";
        final String permissionString = typeName+":"+mode+":"+objectID;
        Permission permission = new UsernamePasswordRealm().getPermissionResolver().resolvePermission(permissionString);
        assertEquals(typeName+":"+mode+":"+objectID, permission.toString());
    }
}
