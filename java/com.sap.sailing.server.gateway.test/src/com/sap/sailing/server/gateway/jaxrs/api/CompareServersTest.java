package com.sap.sailing.server.gateway.jaxrs.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util.Pair;

public class CompareServersTest {
    private CompareServersResource resource;
    
    @BeforeEach
    public void setUp() {
        resource = new CompareServersResource();
    }
    
    @Test
    public void testSimpleRemoveDuplicatesWithTwoEmptyObjects() {
        final JSONObject a = new JSONObject();
        final JSONObject b = new JSONObject();
        final Pair<Object, Object> result = resource.removeUnnecessaryAndDuplicateFields(a, b);
        assertNull(result.getA());
        assertNull(result.getB());
    }

    @Test
    public void testSimpleRemoveDuplicatesWithNameDifference() {
        final JSONObject a = new JSONObject();
        a.put("name", "a");
        final JSONObject b = new JSONObject();
        b.put("name", "b");
        final Pair<Object, Object> result = resource.removeUnnecessaryAndDuplicateFields(a, b);
        assertSame(a, result.getA());
        assertSame(b, result.getB());
        assertEquals("a", a.get("name"));
        assertEquals("b", b.get("name"));
    }

    @Test
    public void testSimpleRemoveDuplicatesWithEqualNames() {
        final JSONObject a = new JSONObject();
        a.put("name", "a");
        final JSONObject b = new JSONObject();
        b.put("name", "a");
        final Pair<Object, Object> result = resource.removeUnnecessaryAndDuplicateFields(a, b);
        assertNull(result.getA());
        assertNull(result.getB());
    }

    @Test
    public void testComparisonOfDiffsIfOrderingInArrayIsEqual() {
        final JSONObject a = new JSONObject();
        final JSONArray aArr = new JSONArray();
        a.put("array", aArr);
        aArr.add("123");
        aArr.add("234");
        final JSONObject b = new JSONObject();
        final JSONArray bArr = new JSONArray();
        b.put("array", bArr);
        bArr.add("123");
        bArr.add("234");
        final Pair<Object, Object> result = resource.removeUnnecessaryAndDuplicateFields(a, b);
        assertNull(result.getA());
        assertNull(result.getB());
    }
    
    @Test
    public void testComparisonOfDiffsIfOrderingInArrayIsChanged() {
        final JSONObject a = new JSONObject();
        final JSONArray aArr = new JSONArray();
        a.put("array", aArr);
        aArr.add("234");
        aArr.add("123");
        final JSONObject b = new JSONObject();
        final JSONArray bArr = new JSONArray();
        b.put("array", bArr);
        bArr.add("123");
        bArr.add("234");
        assertTrue(resource.equalsWithArrayOrderIgnored(a, b));
        final Pair<Object, Object> result = resource.removeUnnecessaryAndDuplicateFields(a, b);
        assertNull(result.getA());
        assertNull(result.getB());
    }

    /**
     * A white-box test that asserts that different names should always lead to the object comparison and field
     * removal to be skipped, regardless of where in the order of keys "name" is found.
     */
    @Test
    public void testDifferentNameNotFirstField() {
        final JSONObject a1 = new JSONObject();
        a1.put("abc", "def");
        a1.put("isTracked", "jkl");
        a1.put("name", "a1");
        final JSONObject b1 = new JSONObject();
        b1.put("name", "b1");
        b1.put("abc", "ghi");
        b1.put("isTracked", "jkl");
        assertNotEquals(a1, b1);
        final JSONObject a2 = new JSONObject();
        a2.put("name", "a1");
        a2.put("abc", "def");
        a2.put("isTracked", "jkl");
        assertEquals(a1, a2);
        final JSONObject b2 = new JSONObject();
        b2.put("name", "b1");
        b2.put("abc", "ghi");
        b2.put("isTracked", "jkl");
        assertEquals(b1, b2);
        assertNotEquals(a2, b2);
        resource.removeUnnecessaryAndDuplicateFields(a1, b1);
        resource.removeUnnecessaryAndDuplicateFields(a2, b2);
        assertEquals(a1, a2);
        assertEquals(b1, b2);
    }

    /**
     * Tests that match-making in arrays happens by "name" field for JSONObjects in array, regardless
     * of order. Extra fields that are not to be compared are intended to make life difficult. There should
     * not be a difference compared to those fields not present, meaning that objects should be removed
     * from the hierarchy if they only differ by those fields ignored.
     */
    @Test
    public void testEliminationBasedOnOrderForNamelessObjectsWithExtraFields() {
        final JSONArray a = new JSONArray();
        final JSONArray b = new JSONArray();
        final JSONObject a1 = new JSONObject();
        a1.put("name", "a1");
        a1.put("abc", "def");
        a1.put("isTracked", "jkl");
        final JSONObject a2 = new JSONObject();
        a2.put("abc", "def");
        a2.put("isTracked", "mno");
        a2.put("name", "a2");
        a.add(a1);
        a.add(a2);
        final JSONObject b1 = new JSONObject();
        b1.put("abc", "ghi");
        b1.put("name", "a1");
        b1.put("isTracked", "jkl");
        final JSONObject b2 = new JSONObject();
        b2.put("abc", "ghi");
        b2.put("name", "a2");
        b2.put("isTracked", "pqr"); // diff, expected to remain
        b.add(b2); // flip order compared to a; assume matching happens by "name" field
        b.add(b1);
        resource.removeUnnecessaryAndDuplicateFields(a, b);
        assertEquals(1, a.size()); // expecting a1 to have been removed because the non-ignored name and isTracked fields equal those of b1
        assertEquals(1, b.size());
        assertTrue(a.contains(a2));
        assertTrue(b.contains(b2));
    }

    @Test
    public void testEliminationBasedOnOrderForNamelessObjectsWithoutExtraFields() {
        final JSONArray a = new JSONArray();
        final JSONArray b = new JSONArray();
        final JSONObject a1 = new JSONObject();
        a1.put("name", "a1");
        a1.put("isTracked", "jkl");
        final JSONObject a2 = new JSONObject();
        a2.put("isTracked", "mno");
        a2.put("name", "a2");
        a.add(a1);
        a.add(a2);
        final JSONObject b1 = new JSONObject();
        b1.put("isTracked", "jkl");
        b1.put("name", "a1");
        final JSONObject b2 = new JSONObject();
        b2.put("name", "a2");
        b2.put("isTracked", "pqr"); // diff, expected to remain
        b.add(b2); // flip order compared to a; assume matching happens by "name" field
        b.add(b1);
        resource.removeUnnecessaryAndDuplicateFields(a, b);
        assertEquals(1, a.size()); // expecting a1 to have been removed because the non-ignored name and isTracked fields equal those of b1
        assertEquals(1, b.size());
        assertTrue(a.contains(a2));
        assertTrue(b.contains(b2));
    }

    /**
     * Tests positional comparison for unnamed JSONObjects within JSONArrays
     */
    @Test
    public void testAnotherStrangeArrayRecursion() {
        final JSONArray a = new JSONArray();
        final JSONArray b = new JSONArray();
        final JSONObject a1 = new JSONObject();
        a1.put("abc", "def");
        a1.put("ghi", "jkl");
        a1.put("isTracked", "jkl");
        a1.put("hasWindData", "123");
        a.add(a1);
        final JSONObject a2 = new JSONObject();
        a2.put("abc", "def");
        a2.put("ghi", "jkl");
        a2.put("isTracked", "vwx");
        a2.put("hasWindData", "234");
        a.add(a2);
        final JSONObject a3 = new JSONObject();
        a3.put("abc", "xxx");
        a3.put("ghi", "xxx");
        a3.put("isTracked", "zzz");
        a3.put("hasWindData", "678");
        a.add(a3);
        final JSONObject b1 = new JSONObject();
        b1.put("mno", "pqr");
        b1.put("isTracked", "stu");
        b.add(b1);
        final JSONObject b2 = new JSONObject();
        b2.put("isTracked", "jkl");
        b2.put("hasWindData", "234");
        b.add(b2);
        final JSONObject b3 = new JSONObject();
        b3.put("abc", "yyy");
        b3.put("ghi", "yyy");
        b3.put("isTracked", "zzz");
        b3.put("hasWindData", "678");
        b.add(b3);
        final JSONObject b4 = new JSONObject();
        b4.put("isTracked", "vwx");
        b4.put("hasWindData", "123");
        b.add(b4);
        resource.removeUnnecessaryAndDuplicateFields(a, b);
        assertTrue(a2.containsKey("isTracked")); // because we have "vwx" in a2, and "jkl" in b2
        assertTrue(b2.containsKey("isTracked")); // because we have "vwx" in a2, and "jkl" in b2
        assertFalse(a2.containsKey("hasWindData")); // because it's "234" in both, a2 and b2
        assertFalse(b2.containsKey("hasWindData")); // because it's "234" in both, a2 and b2
        assertFalse(a.contains(a3)); // because a3 equals b3 in the two fields to be compared (isTracked, hasWindData) despite other fields differing
        assertFalse(b.contains(b3));
        assertTrue(b.contains(b4));
    }
    
    @Test
    public void testComparingLeaderboardGroupOutputWithEventOrderChangeOnly() throws IOException, ParseException {
        final JSONObject oldLg = (JSONObject) new JSONParser().parse(new InputStreamReader(getClass().getResourceAsStream("/LeaderboardGroup1_old.json")));
        final JSONObject newLg = (JSONObject) new JSONParser().parse(new InputStreamReader(getClass().getResourceAsStream("/LeaderboardGroup1_new.json")));
        final Pair<Object, Object> result = resource.removeUnnecessaryAndDuplicateFields(oldLg, newLg);
        assertNull(result.getA());
        assertNull(result.getB());
    }

    @Test
    public void testComparingLeaderboardGroupOutputWithRaceHavingLostWindAndGPS() throws IOException, ParseException {
        final JSONObject oldLg = (JSONObject) new JSONParser().parse(new InputStreamReader(getClass().getResourceAsStream("/LeaderboardGroup2_old.json")));
        final JSONObject newLg = (JSONObject) new JSONParser().parse(new InputStreamReader(getClass().getResourceAsStream("/LeaderboardGroup2_new.json")));
        final Pair<Object, Object> result = resource.removeUnnecessaryAndDuplicateFields(oldLg, newLg);
        assertNotNull(result.getA());
        assertNotNull(result.getB());
        final JSONObject veldenLeaderboard1 = (JSONObject) ((JSONArray) ((JSONObject) oldLg).get("leaderboards")).get(0);
        final JSONObject veldenLeaderboard2 = (JSONObject) ((JSONArray) ((JSONObject) newLg).get("leaderboards")).get(0);
        assertEquals("Austrian League 2021 - Velden (1)", veldenLeaderboard1.get("name"));
        assertEquals("Austrian League 2021 - Velden (1)", veldenLeaderboard2.get("name"));
        final JSONObject race1Fleet_1 = (JSONObject) ((JSONArray) ((JSONObject) ((JSONArray) veldenLeaderboard1.get("series")).get(0)).get("fleets")).get(0);
        final JSONObject race1Fleet_2 = (JSONObject) ((JSONArray) ((JSONObject) ((JSONArray) veldenLeaderboard2.get("series")).get(0)).get("fleets")).get(0);
        final JSONObject f2_1 = (JSONObject) ((JSONArray) race1Fleet_1.get("races")).get(0);
        final JSONObject f2_2 = (JSONObject) ((JSONArray) race1Fleet_2.get("races")).get(0);
        assertEquals("F2", f2_1.get("name"));
        assertEquals("F2", f2_2.get("name"));
        assertEquals(true, f2_1.get("hasGpsData"));
        assertEquals(false, f2_2.get("hasGpsData"));
        assertEquals(true, f2_1.get("hasWindData"));
        assertEquals(false, f2_2.get("hasWindData"));
    }
}
