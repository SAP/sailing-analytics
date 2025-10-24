package com.sap.sailing.windestimation.aggregator.msthmm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.windestimation.aggregator.graph.DijkstraShortestPathFinderImpl;
import com.sap.sailing.windestimation.aggregator.graph.DijsktraShortestPathFinder;
import com.sap.sailing.windestimation.aggregator.graph.ElementAdjacencyQualityMetric;
import com.sap.sailing.windestimation.aggregator.graph.ElementWithQuality;
import com.sap.sailing.windestimation.aggregator.graph.GroupOutOfWhichToPickTheBestElement;
import com.sap.sailing.windestimation.aggregator.graph.InnerGraphSuccessorSupplier;
import com.sap.sse.common.Named;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

public class DijkstraTest {
    private Node startNode;
    private Node endNode;
    private Map<Node, Set<Node>> successors;
    private Map<Pair<Node, Node>, Double> edgeQuality;
    
    private static class Node implements ElementWithQuality, Named {
        private static final long serialVersionUID = 4495016215564212071L;
        private final double quality;
        private final Supplier<String> nameSupplier;
        
        public Node(String name, double quality) {
            this(()->name, quality);
        }
        
        public Node(Supplier<String> nameSupplier, double quality) {
            this.quality = quality;
            this.nameSupplier = nameSupplier;
        }
        
        @Override
        public String getName() {
            return nameSupplier.get();
        }

        @Override
        public double getQuality() {
            return quality;
        }
    }
    
    private static class CompositeTreeNode implements GroupOutOfWhichToPickTheBestElement<Node, CompositeTreeNode> {
        private final CompositeTreeNode parent;
        private final Node[] elements;
        private final Set<CompositeTreeNode> children;

        public CompositeTreeNode(CompositeTreeNode parent, Node... elements) {
            this.parent = parent;
            this.elements = elements;
            this.children = new HashSet<>();
        }
        
        @Override
        public CompositeTreeNode getParent() {
            return parent;
        }

        @Override
        public Iterable<CompositeTreeNode> getChildren() {
            return Collections.unmodifiableCollection(children);
        }

        @Override
        public Iterable<Node> getElements() {
            return Arrays.asList(elements);
        }
        
        public boolean addChild(CompositeTreeNode child) {
            return this.children.add(child);
        }
    }
    
    @BeforeEach
    public void setUp() {
        startNode = new Node("Start", 1.0);
        endNode = new Node("End", 1.0);
        successors = new HashMap<>();
        edgeQuality = new HashMap<>();
    }
    
    @Test
    public void testSimpleCompositeTree() {
        // set up overarching tree structure:
        //              root
        //               |
        //             level1
        //               /\
        //     level2Left  level2Right
        //          |           |
        //     level3Left  level3Right
        // Each of these tree nodes contains two inner graph nodes
        final CompositeTreeNode root = new CompositeTreeNode(/* parent */ null, new Node("Root1", 0.5), new Node("Root2", 0.5));
        final CompositeTreeNode level1 = new CompositeTreeNode(/* parent */ root, new Node("L1_1", 0.5), new Node("L1_2", 0.5));
        root.addChild(level1);
        final CompositeTreeNode level2Left = new CompositeTreeNode(/* parent */ level1, new Node("L2L1", 0.1), new Node("L2L2", 0.9));
        level1.addChild(level2Left);
        final CompositeTreeNode level2Right = new CompositeTreeNode(/* parent */ level1, new Node("L2R1", 0.9), new Node("L2R2", 0.1));
        level1.addChild(level2Right);
        final CompositeTreeNode level3Left = new CompositeTreeNode(/* parent */ level2Left, new Node("L3L1", 0.1), new Node("L3L2", 0.9));
        level2Left.addChild(level3Left);
        final CompositeTreeNode level3Right = new CompositeTreeNode(/* parent */ level2Right, new Node("L3R1", 0.9), new Node("L3R2", 0.1));
        level2Right.addChild(level3Right);
        // now set up the qualities of the edges connecting the inner nodes; simple model:
        // staying on the same inner node index is good (quality 0.9), changing node index is bad (quality 0.1);
        // this way, at the junction represented by level1 the path coming up from level2Left with its
        // preferred __L2 nodes should end up in L1_2, whereas the path coming up from level2Right with its
        // preferred __R1 nodes should end up in L1_1.
        setEdgeQuality(level3Left, 0, level2Left, 0, 0.9);
        setEdgeQuality(level3Left, 0, level2Left, 1, 0.1);
        setEdgeQuality(level3Left, 1, level2Left, 0, 0.1);
        setEdgeQuality(level3Left, 1, level2Left, 1, 0.9);
        setEdgeQuality(level3Right, 0, level2Right, 0, 0.9);
        setEdgeQuality(level3Right, 0, level2Right, 1, 0.1);
        setEdgeQuality(level3Right, 1, level2Right, 0, 0.1);
        setEdgeQuality(level3Right, 1, level2Right, 1, 0.9);
        setEdgeQuality(level2Left, 0, level1, 0, 0.9);
        setEdgeQuality(level2Left, 0, level1, 1, 0.1);
        setEdgeQuality(level2Left, 1, level1, 0, 0.1);
        setEdgeQuality(level2Left, 1, level1, 1, 0.9);
        setEdgeQuality(level2Right, 0, level1, 0, 0.9);
        setEdgeQuality(level2Right, 0, level1, 1, 0.1);
        setEdgeQuality(level2Right, 1, level1, 0, 0.1);
        setEdgeQuality(level2Right, 1, level1, 1, 0.9);
        setEdgeQuality(level1, 0, root, 0, 0.9);
        setEdgeQuality(level1, 0, root, 1, 0.1);
        setEdgeQuality(level1, 1, root, 0, 0.1);
        setEdgeQuality(level1, 1, root, 1, 0.9);
        // now have the inner nodes connected among each other according to the overarching tree structure...
        final InnerGraphSuccessorSupplier<Node, CompositeTreeNode> successorSupplier = new InnerGraphSuccessorSupplier<>(
                ()->root, nodeName->new Node(nodeName, 1.0));
        // the edge qualities for the additional artificial nodes will all default to 1.0
        // ...and solve the inner graph's shortest path problem, once for each artificial leaf:
        final Iterable<Node> bestPathStartingAtLevel3Left = new DijkstraShortestPathFinderImpl<Node>(
                successorSupplier.getArtificialLeaf(level3Left), successorSupplier.getArtificialRoot(),
                successorSupplier, getEdgeQualitySupplier()).getShortestPath();
        final Iterable<Node> bestPathStartingAtLevel3Right = new DijkstraShortestPathFinderImpl<Node>(
                successorSupplier.getArtificialLeaf(level3Right), successorSupplier.getArtificialRoot(),
                successorSupplier, getEdgeQualitySupplier()).getShortestPath();
        // We expect the two solutions to provide different best inner nodes for the converging level1 node.
        // For the indices, note that 0 denotes the artificial leaf node, so index 1 denotes the inner
        // level3 node selected, index 2 the inner level2 node, and index 3 the inner level1 node which is
        // what we'd like to compare:
        assertNotSame(Util.get(bestPathStartingAtLevel3Left, 3), Util.get(bestPathStartingAtLevel3Right, 3));
    }
    
    private void setEdgeQuality(CompositeTreeNode fromLevel, int fromInnerNodeZeroBased, CompositeTreeNode toLevel,
            int toInnerNodeZeroBased, double quality) {
        edgeQuality.put(new Pair<>(Util.get(fromLevel.getElements(), fromInnerNodeZeroBased),
                Util.get(toLevel.getElements(), toInnerNodeZeroBased)), quality);
    }

    @Test
    public void testBasicDijkstra() {
        edge(startNode, endNode, 1.0);
        final Iterable<Node> path = getShortestPath();
        assertEquals(startNode, Util.get(path, 0));
        assertEquals(endNode, Util.get(path, 1));
    }

    private Iterable<Node> getShortestPath() {
        final DijsktraShortestPathFinder<Node> result = new DijkstraShortestPathFinderImpl<Node>(startNode, endNode,
                n -> successors.get(n), getEdgeQualitySupplier());
        return result == null ? null : result.getShortestPath();
    }

    /**
     * Returns an {@link ElementAdjacencyQualityMetric} that etrieves the quality from {@link #edgeQuality} and defaults
     * to {@code 1.0} if no other quality definition is found in {@link #edgeQuality} for the node pair provided.
     */
    private ElementAdjacencyQualityMetric<Node> getEdgeQualitySupplier() {
        return (n1, n2) -> {
            final Double quality = edgeQuality.get(new Pair<>(n1, n2));
            return quality == null ? 1.0 : quality;
        };
    }

    @Test
    public void testDijkstraWithOneGoodAndOneBadPath() {
        final Node good = new Node("Good", 1.0);
        final Node bad = new Node("Bad", 0.5);
        edge(startNode, good, 1.0);
        edge(startNode, bad, 0.7);
        edge(good, endNode, 1.0);
        edge(bad, endNode, 0.7);
        final Iterable<Node> path = getShortestPath();
        assertEquals(startNode, Util.get(path, 0));
        assertEquals(good, Util.get(path, 1));
        assertEquals(endNode, Util.get(path, 2));
    }
    
    @Test
    public void testDijkstraWithGoodPathTurningBad() {
        final Node goodFirst = new Node("GoodFirst", 1.0);
        final Node badFirst = new Node("BadFirst", 0.5);
        edge(startNode, goodFirst, 1.0);
        edge(startNode, badFirst, 0.7);
        edge(goodFirst, endNode, 0.1);
        edge(badFirst, endNode, 1.0);
        final Iterable<Node> path = getShortestPath();
        assertEquals(startNode, Util.get(path, 0));
        assertEquals(badFirst, Util.get(path, 1));
        assertEquals(endNode, Util.get(path, 2));
    }
    
    @Test
    public void testNullForUnreachableEndNode() {
        assertNull(getShortestPath());
    }
    
    private void edge(Node from, Node to, double quality) {
        Util.addToValueSet(successors, from, to);
        edgeQuality.put(new Pair<>(from, to), quality);
    }
}
