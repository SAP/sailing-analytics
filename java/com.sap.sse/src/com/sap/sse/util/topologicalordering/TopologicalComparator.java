package com.sap.sse.util.topologicalordering;

import java.util.Comparator;
import java.util.Map;

import com.sap.sse.util.graph.DirectedGraph;

/**
 * Implements a {@link Comparator} based on partial orderings that may violate the rules of transitivity. The algorithm
 * operates on a {@link DirectedGraph directed graph} with nodes and directed edges. The graph may have more than one
 * root. It is possible for a root to not have any outbound edge. The graph may contain zero or more cycles. Shortcut
 * edges are ignored; in other words, if a node B is reachable by traversing two or more edges in their direction
 * starting at A, and there is an edge leading directly from A to B then this latter edge from A to B will be ignored. It
 * does not add any ordering information. The resulting {@link Comparator} complies with the specification by being able
 * to mutually compare all nodes in the graph, fulfilling the transitivity and symmetry criteria.
 * <p>
 * 
 * <ul>
 * <li>All nodes of a single cycle are considered equal to each other.</li>
 * <li>If node B is reachable by traversing directed edges in their direction starting at A and A and B are not part of
 * the same cycle then B is greater than A.</li>
 * <li>If two nodes A and B are not comparable by any of the above rules, the length of the longest path from a root to
 * the node is computed (R(A) and R(B)) for both nodes and then compared numerically.</li>
 * </ul>
 * 
 * The last criterion is consistent with the other rules for the following reason: The rule applies only if there is no
 * path connecting the nodes either way. An inconsistency would mean that for three nodes A, B, and C we have A&lt;B and
 * B&lt;C but then also C&lt;A. For an inconsistency to exist, at least one of the comparisons must be decided by a path
 * between the nodes because otherwise the ordering by the maximum distance from a root node will result in a
 * well-defined ordering by definition; however, not all three comparisons can have been decided by paths because then
 * all three nodes would be part of the same cycle, and all three nodes would be equal based on the first rule.
 * Furthermore, if there is a path from A to B, and a path from B to C then there is a path from A to C that results in
 * consistent comparison. Three cases therefore remain to be analyzed:
 * <ol>
 * <li>A path exists from C to A: in this case we have R(A) &gt; R(C); since no path exists between B and C, we must
 * have had R(B) &lt; R(C); and since no path exists between A and B either, we must have had R(A) &lt; R(B) and hence
 * R(A) &lt; R(C). But this is a contradiction with R(A) &gt; R(C) which is implied by the path leading from C to A, and
 * hence this case cannot occur.</li>
 * <li>A path exists from A to B: This implies R(B) &gt; R(A). B and C were compared based on R(B) &lt; R(C), and
 * so we have a consistently increasing R-value: R(A) &lt; R(B) &lt; R(C).</li>
 * <li>A path exists from B to C: Same as the case above.</li>
 * </ol>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class TopologicalComparator<T> implements Comparator<T> {
    private final DirectedGraph<T> graph;
    private final Map<T, Integer> lengthsOfLongestPathsFromRoot;
    
    public TopologicalComparator(DirectedGraph<T> graph) {
        this.graph = graph;
        this.lengthsOfLongestPathsFromRoot = graph.getLengthsOfLongestPathsFromRoot();
    }

    @Override
    public int compare(T o1, T o2) {
        final int result;
        if (o1.equals(o2) || graph.areOnSameCycleCluster(o1, o2)) {
            result = 0;
        } else {
            if (graph.hasPath(o1, o2)) {
                result = -1;
            } else if (graph.hasPath(o2, o1)) {
                result = 1;
            } else {
                result = Integer.compare(lengthsOfLongestPathsFromRoot.get(o1), lengthsOfLongestPathsFromRoot.get(o2));
            }
        }
        return result;
    }
}
