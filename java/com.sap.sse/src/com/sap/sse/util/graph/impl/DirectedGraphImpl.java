package com.sap.sse.util.graph.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.util.graph.CycleCluster;
import com.sap.sse.util.graph.CycleClusters;
import com.sap.sse.util.graph.DirectedEdge;
import com.sap.sse.util.graph.DirectedGraph;

public class DirectedGraphImpl<T> implements DirectedGraph<T> {
    private final Set<T> nodes;
    private final Set<DirectedEdge<T>> edges;
    private final Map<T, Set<T>> immediateSuccessors;
    private final Set<T> roots;
    
    private final CycleClusters<T> cycleClusters;
    
    public DirectedGraphImpl(Set<T> nodes, Set<DirectedEdge<T>> edges) {
        this.nodes = nodes;
        this.edges = edges;
        final Map<T, Set<T>> succ = new HashMap<>();
        final Map<T, Set<T>> pred = new HashMap<>();
        for (final T node : nodes) {
            succ.put(node, new HashSet<>());
            pred.put(node, new HashSet<>());
        }
        for (final DirectedEdge<T> e : edges) {
            succ.get(e.getFrom()).add(e.getTo());
            pred.get(e.getTo()).add(e.getFrom());
        }
        final Map<T, Set<T>> succWithUnmodifiableSets = new HashMap<>();
        final Map<T, Set<T>> predWithUnmodifiableSets = new HashMap<>();
        final Set<T> modifiableRoots = new HashSet<>();
        for (final T node : nodes) {
            succWithUnmodifiableSets.put(node, Collections.unmodifiableSet(succ.get(node)));
            final Set<T> preds = pred.get(node);
            predWithUnmodifiableSets.put(node, Collections.unmodifiableSet(preds));
            if (preds.isEmpty()) {
                modifiableRoots.add(node);
            }
        }
        this.immediateSuccessors = Collections.unmodifiableMap(succWithUnmodifiableSets);
        this.roots = Collections.unmodifiableSet(modifiableRoots);
        cycleClusters = findStronglyConnectedComponents();
        assert cycleClusters.areDisjoint();
    }
    
    private interface DFSStep<T> {
        int applyAndReturnNextDFSNumber(LinkedList<T> oReps, LinkedList<T> oNodes, Map<T, T> representatives,
                Map<T, Integer> depthFirstSearchPosition, Set<T> remainingRoots, LinkedList<DFSStep<T>> worklist, int nodeCounter, Set<T> oNodesForFastContains);
    }
    
    private class Backtrack implements DFSStep<T> {
        private final T backtrackFromNode;

        public Backtrack(T backtrackFromNode) {
            this.backtrackFromNode = backtrackFromNode;
        }

        @Override
        public int applyAndReturnNextDFSNumber(LinkedList<T> oReps, LinkedList<T> oNodes, Map<T, T> representatives,
                Map<T, Integer> depthFirstSearchPosition, Set<T> remainingRoots, LinkedList<DFSStep<T>> worklist, int nodeCounter, Set<T> oNodesForFastContains) {
            // the second condition avoids creating single-element components; we don't want
            // to call each single element a "strongly-connected component"
            if (backtrackFromNode == oReps.getLast()) {
                oReps.removeLast();
                T lastNodeFromoNodes;
                do {
                    lastNodeFromoNodes = oNodes.removeLast();
                    oNodesForFastContains.remove(lastNodeFromoNodes);
                    representatives.put(lastNodeFromoNodes, backtrackFromNode);
                } while (lastNodeFromoNodes != backtrackFromNode);
            }
            return nodeCounter;
        }
        
        @Override
        public String toString() {
            return "backtrack("+backtrackFromNode+")";
        }
    }
    
    private class TraverseEdge implements DFSStep<T> {
        private final T toNode;
        
        public TraverseEdge(T toNode) {
            this.toNode = toNode;
        }
        
        @Override
        public int applyAndReturnNextDFSNumber(LinkedList<T> oReps, LinkedList<T> oNodes, Map<T, T> representatives,
                Map<T, Integer> depthFirstSearchPosition, Set<T> remainingRoots, LinkedList<DFSStep<T>> worklist,
                int nodeCounter, Set<T> oNodesForFastContains) {
            if (depthFirstSearchPosition.containsKey(toNode)) {
                if (oNodesForFastContains.contains(toNode)) {
                    while (depthFirstSearchPosition.get(toNode) < depthFirstSearchPosition.get(oReps.getLast())) {
                        oReps.removeLast();
                    }
                }
            } else {
                oReps.add(toNode);
                oNodes.add(toNode);
                oNodesForFastContains.add(toNode);
                remainingRoots.remove(toNode);
                depthFirstSearchPosition.put(toNode, nodeCounter++);
                worklist.add(new DFS(toNode));
            }
            return nodeCounter;
        }
    }
    
    private class Root implements DFSStep<T> {
        private final T rootNode;

        public Root(T rootNode) {
            super();
            this.rootNode = rootNode;
        }

        @Override
        public int applyAndReturnNextDFSNumber(LinkedList<T> oReps, LinkedList<T> oNodes, Map<T, T> representatives,
                Map<T, Integer> depthFirstSearchPosition, Set<T> remainingRoots, LinkedList<DFSStep<T>> worklist, int nodeCounter, Set<T> oNodesForFastContains) {
            remainingRoots.remove(rootNode);
            depthFirstSearchPosition.put(rootNode, nodeCounter++);
            oReps.add(rootNode);
            oNodes.add(rootNode);
            oNodesForFastContains.add(rootNode);
            worklist.add(new DFS(rootNode));
            return nodeCounter;
        }
        
        @Override
        public String toString() {
            return "root("+rootNode+")";
        }
    }
    
    private class DFS implements DFSStep<T> {
        private final T to;
        
        public DFS(T to) {
            this.to = to;
        }

        @Override
        public int applyAndReturnNextDFSNumber(LinkedList<T> oReps, LinkedList<T> oNodes, Map<T, T> representatives,
                Map<T, Integer> depthFirstSearchPosition, Set<T> remainingRoots, LinkedList<DFSStep<T>> worklist, int nodeCounter, Set<T> oNodesForFastContains) {
            worklist.add(new Backtrack(to));
            for (final T successor : immediateSuccessors.get(to)) {
                worklist.add(new TraverseEdge(successor));
            }
            return nodeCounter;
        }
        
        @Override
        public String toString() {
            return "DFS("+to+")";
        }
    }
    
    /**
     * A strongly connected component is a set of nodes such that all nodes in the set can be reached from any other
     * node in the set by traversing existing graph edges, and where no other node exists outside that set that, when
     * added, would still make a strongly connected component.<p>
     * 
     * See also http://algo2.iti.kit.edu/documents/AlgorithmenII_WS11/folien.pdf (pages 119 and following) for a
     * description of the algorithm implemented here.
     * 
     * @return cycle clusters; each {@link CycleCluster} is one strongly-connected component
     */
    private CycleClusters<T> findStronglyConnectedComponents() {
        final Set<T> remainingRoots = new HashSet<>(nodes);
        final LinkedList<DFSStep<T>> worklist = new LinkedList<>();
        final LinkedList<T> oReps = new LinkedList<>();
        final LinkedList<T> oNodes = new LinkedList<>();
        final Set<T> oNodesForFastContains = new HashSet<>();
        int nodeCounter = 0;
        final Map<T, T> representativesForNodes = new HashMap<>();
        
        final Map<T, Integer> depthFirstSearchPosition = new HashMap<>();
        
        if (!nodes.isEmpty()) {
            final T nextRoot = remainingRoots.iterator().next();
            remainingRoots.remove(nextRoot);
            worklist.add(new Root(nextRoot));
            while (!Util.isEmpty(worklist)) {
                // depth-first search by using last element in worklist and replacing it by paths extended by its successors;
                // insert "instructions" (DFSStep objects) in reverse order of intended execution order
                final DFSStep<T> nextStep = worklist.removeLast();
                nodeCounter = nextStep.applyAndReturnNextDFSNumber(oReps, oNodes, representativesForNodes,
                        depthFirstSearchPosition, remainingRoots, worklist, nodeCounter, oNodesForFastContains);
                if (worklist.isEmpty() && !remainingRoots.isEmpty()) {
                    // there are nodes remaining which must be on cycles because they
                    // haven't been reached from any of the root nodes. Add path to first
                    // node not yet visited and continue:
                    final T nextRoot2 = remainingRoots.iterator().next();
                    remainingRoots.remove(nextRoot2);
                    worklist.add(new Root(nextRoot2));
                }
            }
        }
        final Set<CycleCluster<T>> cycleClusters = new HashSet<>();
        final Map<T, Set<T>> cycleClusterNodes = new HashMap<>();
        for (final Entry<T, T> e : representativesForNodes.entrySet()) {
            Util.addToValueSet(cycleClusterNodes, e.getValue(), e.getKey());
        }
        for (final Entry<T, Set<T>> e : cycleClusterNodes.entrySet()) {
            if (e.getValue().size() > 1) {
                cycleClusters.add(new CycleClusterImpl<>(e.getKey(), e.getValue()));
            }
        }
        return new CycleClustersImpl<>(cycleClusters);
    }

    @Override
    public Set<T> getNodes() {
        return nodes;
    }

    @Override
    public Set<DirectedEdge<T>> getEdges() {
        return edges;
    }

    @Override
    public Set<T> getRoots() {
        return roots;
    }

    @Override
    public CycleClusters<T> getCycleClusters() {
        return cycleClusters;
    }

    @Override
    public CycleCluster<T> getCycleCluster(T node) {
        return cycleClusters.getCluster(node);
    }
    
    @Override
    public boolean areOnSameCycleCluster(T a, T b) {
        final CycleCluster<T> aCluster = cycleClusters.getCluster(a);
        final CycleCluster<T> bCluster = cycleClusters.getCluster(b);
        return aCluster != null && aCluster == bCluster;
    }

    @Override
    public boolean hasPath(T from, T to) {
        final Set<T> visited = new HashSet<>();
        final LinkedList<T> toVisit = new LinkedList<>();
        toVisit.add(from);
        visited.add(from);
        while (!toVisit.isEmpty()) {
            final T next = toVisit.removeFirst();
            if (next.equals(to)) {
                return true;
            } else {
                for (final T successor : immediateSuccessors.get(next)) {
                    if (!visited.contains(successor)) {
                        toVisit.add(successor);
                        visited.add(successor);
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Map<T, Integer> getLengthsOfLongestPathsFromRoot() {
        // construct a new graph with joined cycles replaced by combined "cycle nodes" and edges combined accordingly
        // then count in a breadth-first search starting from the roots
        Pair<DirectedGraph<T>, CycleClusters<T>> dag = graphWithCombinedCycleNodes();
        Map<T, Integer> preResult = dag.getA().getLengthsOfLongestPathsFromRootForDag();
        // now map back the results of the cycle representatives to all nodes that were part of the cycle:
        Map<T, Integer> result = new HashMap<>(preResult);
        for (final CycleCluster<T> cycleCluster : dag.getB().getClusters()) {
            final Integer rootDistance = result.get(cycleCluster.getRepresentative());
            for (final T representedCycleNode : cycleCluster.getClusterNodes()) {
                result.put(representedCycleNode, rootDistance);
            }
        }
        return result;
    }

    @Override
    public Pair<DirectedGraph<T>, CycleClusters<T>> graphWithCombinedCycleNodes() {
        assert cycleClusters != null;
        final Set<T> newNodes = new HashSet<>(nodes);
        for (final CycleCluster<T> cluster : cycleClusters.getClusters()) {
            newNodes.removeAll(cluster.getClusterNodes());
            newNodes.add(cluster.getRepresentative()); // add the representative nodes
        }
        final Set<DirectedEdge<T>> newEdges = new HashSet<>();
        for (final DirectedEdge<T> edge : edges) {
            if (!cycleClusters.isEdgeInCycleCluster(edge)) {
                newEdges.add(replaceCycleNodesByRepresentatives(edge, cycleClusters));
            }
        }
        return new Pair<>(new DirectedGraphImpl<T>(newNodes, newEdges), cycleClusters);
    }

    private DirectedEdge<T> replaceCycleNodesByRepresentatives(DirectedEdge<T> edge, CycleClusters<T> cycleClusters) {
        boolean replaced = false;
        T from = null;
        T to = null;
        final CycleCluster<T> fromCluster;
        final CycleCluster<T> toCluster;
        if (from == null && (fromCluster=cycleClusters.getCluster(edge.getFrom())) != null && !fromCluster.getRepresentative().equals(edge.getFrom())) {
            from = fromCluster.getRepresentative();
            replaced = true;
        } else {
            from = edge.getFrom();
        }
        if (to == null && (toCluster=cycleClusters.getCluster(edge.getTo())) != null && !toCluster.getRepresentative().equals(edge.getTo())) {
            to = toCluster.getRepresentative();
            replaced = true;
        } else {
            to = edge.getTo();
        }
        return replaced ? new DirectedEdgeImpl<>(from, to) : edge;
    }

    @Override
    public Map<T, Integer> getLengthsOfLongestPathsFromRootForDag() {
        if (!Util.isEmpty(cycleClusters.getClusters())) {
            throw new IllegalStateException("Can't call this method on a graph with cycles; use graphWithCombinedCycleNodes() first");
        }
        final Map<T, Integer> result = new HashMap<>();
        Set<T> workingSet = new HashSet<>(roots);
        int longestDistanceFromRoot = 0;
        while (!workingSet.isEmpty()) {
            final Set<T> nextWorkingSet = new HashSet<>();
            for (final T node : workingSet) {
                if (!result.containsKey(node) || result.get(node) < longestDistanceFromRoot) {
                    result.put(node, longestDistanceFromRoot);
                    nextWorkingSet.addAll(immediateSuccessors.get(node));
                }
            }
            longestDistanceFromRoot++;
            workingSet = nextWorkingSet;
        }
        return result;
    }

}
