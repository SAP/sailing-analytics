package diffutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import difflib.ChangeDelta;
import difflib.Chunk;
import difflib.DeleteDelta;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.InsertDelta;
import difflib.Patch;

public class DiffTest {

    @Test
    public void testDiff_Insert() {
        final Patch<String> patch = DiffUtils.diff(Arrays.asList("hhh"), Arrays.asList("hhh", "jjj", "kkk"));
        assertNotNull(patch);
        assertEquals(1, patch.getDeltas().size());
        final Delta<String> delta = patch.getDeltas().get(0);
        assertEquals(InsertDelta.class, delta.getClass());
        List<String> emptyList = Collections.emptyList();
        assertEquals(new Chunk<String>(1, emptyList), delta.getOriginal());
        assertEquals(new Chunk<String>(1, Arrays.asList("jjj", "kkk")), delta.getRevised());
    }

    @Test
    public void testDiff_Delete() {
        final Patch<String> patch = DiffUtils.diff(Arrays.asList("ddd", "fff", "ggg"), Arrays.asList("ggg"));
        assertNotNull(patch);
        assertEquals(1, patch.getDeltas().size());
        final Delta<String> delta = patch.getDeltas().get(0);
        assertEquals(DeleteDelta.class, delta.getClass());
        assertEquals(new Chunk<String>(0, Arrays.asList("ddd", "fff")), delta.getOriginal());
        List<String> emptyList = Collections.emptyList();
        assertEquals(new Chunk<String>(0, emptyList), delta.getRevised());
    }

    @Test
    public void testDiff_Change() {
        final List<String> changeTest_from = Arrays.asList("aaa", "bbb", "ccc");
        final List<String> changeTest_to = Arrays.asList("aaa", "zzz", "ccc");

        final Patch<String> patch = DiffUtils.diff(changeTest_from, changeTest_to);
        assertNotNull(patch);
        assertEquals(1, patch.getDeltas().size());
        final Delta<String> delta = patch.getDeltas().get(0);
        assertEquals(ChangeDelta.class, delta.getClass());
        assertEquals(new Chunk<String>(1, Arrays.asList("bbb")), delta.getOriginal());
        assertEquals(new Chunk<String>(1, Arrays.asList("zzz")), delta.getRevised());
    }

    @Test
    public void testDiff_EmptyList() {
        final Patch<String> patch = DiffUtils.diff(new ArrayList<String>(), new ArrayList<String>());
        assertNotNull(patch);
        assertEquals(0, patch.getDeltas().size());
    }

    @Test
    public void testDiff_EmptyListWithNonEmpty() {
        final Patch<String> patch = DiffUtils.diff(new ArrayList<String>(), Arrays.asList("aaa"));
        assertNotNull(patch);
        assertEquals(1, patch.getDeltas().size());
        final Delta<String> delta = patch.getDeltas().get(0);
        assertEquals(InsertDelta.class, delta.getClass());
    }
}
