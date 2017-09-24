package com.sap.sse.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.util.graph.CycleClusters;
import com.sap.sse.util.graph.DirectedEdge;
import com.sap.sse.util.graph.DirectedGraph;
import com.sap.sse.util.graph.impl.DirectedEdgeImpl;
import com.sap.sse.util.graph.impl.DirectedGraphImpl;
import com.sap.sse.util.topologicalordering.TopologicalComparator;

public class TopologicalComparatorTest {
    private final Set<String> nodes = new HashSet<>();
    private final Set<DirectedEdge<String>> edges = new HashSet<>();
    private DirectedGraph<String> graph;
    
    private void addLessThan(String lesser, String greater) {
        nodes.add(lesser);
        nodes.add(greater);
        edges.add(new DirectedEdgeImpl<String>(lesser, greater));
    }
    
    private void singleRoot(String root) {
        nodes.add(root);
    }
    
    private void createGraph() {
        graph = new DirectedGraphImpl<>(nodes, edges);
    }
    
    @Test
    public void testSimpleSort() {
        addLessThan("A", "B"); addLessThan("B", "C"); addLessThan("C", "D");
        addLessThan("E", "F"); addLessThan("F", "G"); addLessThan("G", "H");
        singleRoot("I");
        singleRoot("J");
        createGraph();
        TopologicalComparator<String> comparator = new TopologicalComparator<>(graph);
        assertMutualEquality(comparator, "A", "E", "I", "J");
        assertMutualEquality(comparator, "B", "F");
        assertMutualEquality(comparator, "C", "G");
        assertMutualEquality(comparator, "D", "H");
        assertChain(comparator, "A", "B", "C", "D");
        assertChain(comparator, "E", "F", "G", "H");
    }
    
    @Test
    public void testSimpleSortWithTrivialCycle() {
        addLessThan("A", "B"); addLessThan("B", "C"); addLessThan("C", "A");
        createGraph();
        assertEquals("Cycle not detected properly", 1, Util.size(graph.getCycles()));
        assertEquals("Cycle cluster not detected properly", 1, Util.size(graph.getCycleClusters().getClusters()));
        TopologicalComparator<String> comparator = new TopologicalComparator<>(graph);
        assertMutualEquality(comparator, "A", "B", "C");
    }
    
    @Test
    public void testSimpleSortWithCycle() {
        addLessThan("A", "B"); addLessThan("B", "C"); addLessThan("C", "D"); addLessThan("D", "B"); addLessThan("D", "E");
        createGraph();
        TopologicalComparator<String> comparator = new TopologicalComparator<>(graph);
        assertMutualEquality(comparator, "B", "C", "D");
        assertChain(comparator, "A", "B", "E");
        assertChain(comparator, "A", "C", "E");
        assertChain(comparator, "A", "D", "E");
    }
    
    @Test
    public void testSimpleSortWithTwoCycles() {
        addLessThan("A", "B"); addLessThan("B", "C"); addLessThan("C", "D"); addLessThan("D", "B"); addLessThan("D", "E");
                                                      addLessThan("C", "F"); addLessThan("F", "B");
        createGraph();
        TopologicalComparator<String> comparator = new TopologicalComparator<>(graph);
        assertMutualEquality(comparator, "B", "C", "D", "F");
        assertChain(comparator, "A", "B", "E");
        assertChain(comparator, "A", "C", "E");
        assertChain(comparator, "A", "D", "E");
        assertChain(comparator, "A", "F", "E");
    }
    
    @Test
    public void testDedicatedThreeNestedCircleGraph() {
        addLessThan("A", "B");
        addLessThan("B", "C");
        addLessThan("C", "D");
        addLessThan("D", "E");
        addLessThan("E", "F");
        addLessThan("E", "A");
        addLessThan("F", "G");
        addLessThan("G", "H");
        addLessThan("H", "D");
        addLessThan("C", "I");
        addLessThan("I", "G");
        createGraph();
        assertOnCycle("A", "B", "C", "D", "E", "F", "G", "H", "I");
        final Pair<DirectedGraph<String>, CycleClusters<String>> dag = graph.graphWithCombinedCycleNodes();
        assertEquals(0, Util.size(dag.getA().getCycles()));
    }
    
    private void assertOnCycle(String... nodes) {
        for (final String node : nodes) {
            assertTrue("Expected "+node+" on cycle but wasn't", StreamSupport.stream(graph.getCycles().spliterator(), /* parallel */ false).anyMatch(c->c.contains(node)));
        }
    }

    @Test
    public void randomTest() {
        final Random random = new Random();
        final int NUMBER_OF_NODES = 100;
        final int NUMBER_OF_EDGES = 150;
        final String[] nodes = new String[NUMBER_OF_NODES];
        for (int i=0; i<NUMBER_OF_NODES; i++) {
            nodes[i] = ""+i;
        }
        final Set<DirectedEdge<String>> edges = new HashSet<>(NUMBER_OF_EDGES);
        for (int i=0; i<NUMBER_OF_EDGES; i++) {
            final int from = random.nextInt(NUMBER_OF_NODES);
            int to;
            do {
                to = random.nextInt(NUMBER_OF_NODES);
            } while (to == from);
            edges.add(new DirectedEdgeImpl<>(nodes[from], nodes[to]));
        }
        graph = new DirectedGraphImpl<String>(new HashSet<>(Arrays.asList(nodes)), edges);
        TopologicalComparator<String> comparator = new TopologicalComparator<>(graph);
        for (final DirectedEdge<String> edge : edges) {
            if (!graph.areOnSameCycleCluster(edge.getFrom(), edge.getTo())) {
                assertEquals(-1, comparator.compare(edge.getFrom(), edge.getTo()));
                assertEquals(1, comparator.compare(edge.getTo(), edge.getFrom()));
            }
        }
    }
    
    private void assertMutualEquality(TopologicalComparator<String> comparator, String... strings) {
        for (int i=0; i<strings.length; i++) {
            for (int j=0; j<strings.length; j++) {
                assertEquals(strings[i]+" at position "+i+" and "+strings[j]+" at position "+j+" compared incorrectly",
                        0, comparator.compare(strings[i], strings[j]));
            }
        }
    }
    
    private void assertChain(TopologicalComparator<String> comparator, String... strings) {
        for (int i=0; i<strings.length; i++) {
            for (int j=0; j<strings.length; j++) {
                assertEquals(strings[i]+" at position "+i+" and "+strings[j]+" at position "+j+" compared incorrectly",
                        Integer.compare(i, j), comparator.compare(strings[i], strings[j]));
            }
        }
    }
}
