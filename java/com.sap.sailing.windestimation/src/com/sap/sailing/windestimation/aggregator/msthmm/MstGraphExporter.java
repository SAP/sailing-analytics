package com.sap.sailing.windestimation.aggregator.msthmm;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.sap.sailing.windestimation.aggregator.graph.DijkstraShortestPathFinderImpl;
import com.sap.sailing.windestimation.aggregator.graph.DijsktraShortestPathFinder;
import com.sap.sailing.windestimation.aggregator.graph.ElementAdjacencyQualityMetric;
import com.sap.sailing.windestimation.aggregator.graph.InnerGraphSuccessorSupplier;
import com.sap.sailing.windestimation.aggregator.hmm.GraphNode;
import com.sap.sailing.windestimation.aggregator.hmm.WindCourseRange;
import com.sap.sailing.windestimation.aggregator.msthmm.MstManeuverGraphGenerator.MstManeuverGraphComponents;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverTypeForClassification;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * Exports MST graph data to JSON format for visualization.
 * The output can be consumed by a Python visualization script.
 * 
 * @author Generated for visualization purposes using Claude Opus 4.5
 */
public class MstGraphExporter {

    private final MstGraphNodeTransitionProbabilitiesCalculator transitionProbabilitiesCalculator;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public MstGraphExporter(MstGraphNodeTransitionProbabilitiesCalculator transitionProbabilitiesCalculator) {
        this.transitionProbabilitiesCalculator = transitionProbabilitiesCalculator;
    }

    /**
     * Exports the MST graph components to JSON format.
     * 
     * @param graphComponents The MST graph to export
     * @param writer The writer to output JSON to
     * @throws IOException if writing fails
     */
    public void exportToJson(MstManeuverGraphComponents graphComponents, Writer writer) throws IOException {
        // First, assign node IDs and collect best nodes per level
        final MstGraphLevel root = graphComponents.getRoot();
        final Map<MstGraphLevel, Integer> levelToId = new HashMap<>();
        final int[] nodeIdCounter = {0};
        assignNodeIds(root, levelToId, nodeIdCounter);
        
        // Collect path vote diagnostics for debugging
        final Map<MstGraphLevel, Map<ManeuverTypeForClassification, List<Double>>> pathVotes = 
                collectPathVoteDiagnostics(graphComponents);
        
        // Collect best (disambiguated) classification per node
        final Map<String, String> bestNodePerLevel = collectBestNodePerLevel(graphComponents, levelToId, pathVotes);
        
        // Derive best path edges from the best node selections
        // This ensures edges connect nodes with red frames (best classifications)
        final Set<String> bestPathEdges = collectBestPathEdges(graphComponents, bestNodePerLevel, levelToId);
        
        writer.write("{\n");
        writer.write("  \"nodes\": [\n");
        // Export all nodes starting from root
        final boolean[] firstNode = {true};
        final int[] exportNodeIdCounter = {0};
        final Map<MstGraphLevel, Integer> exportLevelToId = new HashMap<>();
        exportNode(root, writer, firstNode, exportLevelToId, exportNodeIdCounter, 0, pathVotes);
        writer.write("\n  ],\n");
        writer.write("  \"edges\": [\n");
        // Export edges between nodes
        final boolean[] firstEdge = {true};
        exportEdges(root, writer, firstEdge, exportLevelToId, bestPathEdges);
        writer.write("\n  ],\n");
        // Export best path information
        writer.write("  \"bestPaths\": {\n");
        boolean firstBest = true;
        for (Map.Entry<String, String> entry : bestNodePerLevel.entrySet()) {
            if (!firstBest) {
                writer.write(",\n");
            }
            firstBest = false;
            writer.write("    \"" + entry.getKey() + "\": \"" + entry.getValue() + "\"");
        }
        writer.write("\n  }\n");
        writer.write("}\n");
    }

    private void exportNode(MstGraphLevel level, Writer writer, boolean[] firstNode, 
            Map<MstGraphLevel, Integer> levelToId, int[] nodeIdCounter, int depth,
            Map<MstGraphLevel, Map<ManeuverTypeForClassification, List<Double>>> pathVotes) throws IOException {
        final int nodeId = nodeIdCounter[0]++;
        levelToId.put(level, nodeId);
        if (!firstNode[0]) {
            writer.write(",\n");
        }
        firstNode[0] = false;
        final ManeuverForEstimation maneuver = level.getManeuver();
        writer.write("    {\n");
        writer.write("      \"id\": " + nodeId + ",\n");
        writer.write("      \"depth\": " + depth + ",\n");
        writer.write("      \"timestamp\": \"" + DATE_FORMAT.format(maneuver.getManeuverTimePoint().asDate()) + "\",\n");
        // Competitor info
        if (maneuver.getCompetitorName() != null) {
            writer.write("      \"competitorName\": \"" + escapeJson(maneuver.getCompetitorName()) + "\",\n");
        }
        writer.write("      \"position\": {\"lat\": " + maneuver.getManeuverPosition().getLatDeg() + 
                     ", \"lon\": " + maneuver.getManeuverPosition().getLngDeg() + "},\n");
        // distanceToParent is actually the "compound distance" = sum of two predicted standard deviations
        // It's used for the Gaussian-based transition probability calculation
        writer.write("      \"compoundDistanceToParent\": " + level.getDistanceToParent() + ",\n");
        // Also export actual spatial distance and time difference to parent
        final MstGraphLevel parent = level.getParent();
        if (parent != null) {
            final ManeuverForEstimation parentManeuver = parent.getManeuver();
            final double spatialDistanceMeters = maneuver.getManeuverPosition()
                    .getDistance(parentManeuver.getManeuverPosition()).getMeters();
            final long timeDiffMillis = Math.abs(maneuver.getManeuverTimePoint().asMillis() 
                    - parentManeuver.getManeuverTimePoint().asMillis());
            writer.write("      \"spatialDistanceToParentMeters\": " + spatialDistanceMeters + ",\n");
            writer.write("      \"timeDiffToParentSeconds\": " + (timeDiffMillis / 1000.0) + ",\n");
        }
        writer.write("      \"compartments\": [\n");
        // Export each maneuver type compartment
        boolean firstCompartment = true;
        for (GraphNode<MstGraphLevel> node : level.getLevelNodes()) {
            if (!firstCompartment) {
                writer.write(",\n");
            }
            firstCompartment = false;
            final ManeuverTypeForClassification type = node.getManeuverType();
            final WindCourseRange windRange = node.getValidWindRange();
            final double confidence = node.getConfidence();
            writer.write("        {\n");
            writer.write("          \"type\": \"" + type.name() + "\",\n");
            writer.write("          \"confidence\": " + confidence + ",\n");
            writer.write("          \"windRangeFrom\": " +
                    new DegreeBearingImpl(windRange.getFromPortside()).reverse().getDegrees() + ",\n");
            writer.write("          \"windRangeWidth\": " + windRange.getAngleTowardStarboard() + ",\n");
            
            // Calculate single wind direction estimate for TACK/JIBE (width ~0)
            double windEstimate;
            if (windRange.getAngleTowardStarboard() < 1.0) {
                windEstimate = new DegreeBearingImpl(windRange.getFromPortside()).reverse().getDegrees();
            } else {
                // For HEAD_UP/BEAR_AWAY, use middle of range
                windEstimate = new DegreeBearingImpl(windRange.getFromPortside()).reverse().getDegrees()
                        + windRange.getAngleTowardStarboard() / 2.0;
                if (windEstimate >= 360) {
                    windEstimate -= 360;
                }
            }
            writer.write("          \"windEstimate\": " + windEstimate + ",\n");
            writer.write("          \"tackAfter\": \"" + node.getTackAfter() + "\"\n");
            writer.write("        }");
        }
        writer.write("\n      ],\n");
        // Add diagnostic info about path votes for this node
        writer.write("      \"pathVotes\": " + formatPathVoteDiagnostics(level, pathVotes) + "\n");
        writer.write("    }");
        // Recursively export children
        for (MstGraphLevel child : level.getChildren()) {
            exportNode(child, writer, firstNode, levelToId, nodeIdCounter, depth + 1, pathVotes);
        }
    }

    private void exportEdges(MstGraphLevel level, Writer writer, boolean[] firstEdge,
            Map<MstGraphLevel, Integer> levelToId, Set<String> bestPathEdges) throws IOException {
        final int parentId = levelToId.get(level);
        for (final MstGraphLevel child : level.getChildren()) {
            final int childId = levelToId.get(child);
            // Export edges between all compartment pairs
            for (final GraphNode<MstGraphLevel> parentNode : level.getLevelNodes()) {
                for (final GraphNode<MstGraphLevel> childNode : child.getLevelNodes()) {
                    // Calculate transition probability
                    final double transitionProb = transitionProbabilitiesCalculator.getTransitionProbability(
                            childNode, parentNode, child.getDistanceToParent());
                    final String edgeKey = parentId + "_" + parentNode.getManeuverType().ordinal() + 
                                    "_" + childId + "_" + childNode.getManeuverType().ordinal();
                    final boolean isBestPath = bestPathEdges.contains(edgeKey);
                    if (!firstEdge[0]) {
                        writer.write(",\n");
                    }
                    firstEdge[0] = false;
                    writer.write("    {\n");
                    writer.write("      \"from\": " + parentId + ",\n");
                    writer.write("      \"fromType\": \"" + parentNode.getManeuverType().name() + "\",\n");
                    writer.write("      \"to\": " + childId + ",\n");
                    writer.write("      \"toType\": \"" + childNode.getManeuverType().name() + "\",\n");
                    writer.write("      \"transitionProbability\": " + transitionProb + ",\n");
                    writer.write("      \"distance\": " + child.getDistanceToParent() + ",\n");
                    writer.write("      \"isBestPath\": " + isBestPath + "\n");
                    writer.write("    }");
                }
            }
            // Recursively export edges for children
            exportEdges(child, writer, firstEdge, levelToId, bestPathEdges);
        }
    }

    /**
     * Collects the best path edges based on the disambiguated best node selections.
     * An edge is marked as "best" if it connects two nodes where both endpoints
     * have their best (disambiguated) classification matching the edge's from/to types.
     */
    private Set<String> collectBestPathEdges(MstManeuverGraphComponents graphComponents, 
            Map<String, String> bestNodePerLevel, Map<MstGraphLevel, Integer> levelToId) {
        Set<String> bestPathEdges = new HashSet<>();
        collectBestPathEdgesRecursive(graphComponents.getRoot(), bestPathEdges, bestNodePerLevel, levelToId);
        return bestPathEdges;
    }
    
    private void collectBestPathEdgesRecursive(MstGraphLevel parent, Set<String> bestPathEdges,
            Map<String, String> bestNodePerLevel, Map<MstGraphLevel, Integer> levelToId) {
        Integer parentId = levelToId.get(parent);
        String parentBestType = bestNodePerLevel.get(String.valueOf(parentId));
        
        for (MstGraphLevel child : parent.getChildren()) {
            Integer childId = levelToId.get(child);
            String childBestType = bestNodePerLevel.get(String.valueOf(childId));
            
            // Mark the edge between the best classifications as the best path edge
            if (parentBestType != null && childBestType != null) {
                int parentTypeOrdinal = ManeuverTypeForClassification.valueOf(parentBestType).ordinal();
                int childTypeOrdinal = ManeuverTypeForClassification.valueOf(childBestType).ordinal();
                String edgeKey = parentId + "_" + parentTypeOrdinal + "_" + childId + "_" + childTypeOrdinal;
                bestPathEdges.add(edgeKey);
            }
            
            // Recurse to children
            collectBestPathEdgesRecursive(child, bestPathEdges, bestNodePerLevel, levelToId);
        }
    }

    /**
     * Collects diagnostic information about which paths voted for which classification at each node.
     * This runs Dijkstra from each leaf and records which classification was selected and with what path quality.
     */
    private Map<MstGraphLevel, Map<ManeuverTypeForClassification, List<Double>>> collectPathVoteDiagnostics(
            MstManeuverGraphComponents graphComponents) {
        final Map<MstGraphLevel, Map<ManeuverTypeForClassification, List<Double>>> result = new HashMap<>();
        
        final ElementAdjacencyQualityMetric<GraphNode<MstGraphLevel>> edgeQualityMetric = (previousNode, currentNode) -> {
            return transitionProbabilitiesCalculator.getTransitionProbability(currentNode, previousNode,
                    previousNode.getGraphLevel() == null ? 0.0 : previousNode.getGraphLevel().getDistanceToParent());
        };
        
        for (MstGraphLevel leaf : graphComponents.getLeaves()) {
            final InnerGraphSuccessorSupplier<GraphNode<MstGraphLevel>, MstGraphLevel> innerGraphSuccessorSupplier =
                    new InnerGraphSuccessorSupplier<>(graphComponents,
                            (final Supplier<String> nameSupplier) -> new GraphNode<MstGraphLevel>(
                                    null, null, new WindCourseRange(0, 360), 1.0, 0, null) {
                                @Override
                                public String toString() {
                                    return nameSupplier.get();
                                }
                            });
            
            final DijsktraShortestPathFinder<GraphNode<MstGraphLevel>> dijkstra = 
                    new DijkstraShortestPathFinderImpl<>(
                            innerGraphSuccessorSupplier.getArtificialLeaf(leaf),
                            innerGraphSuccessorSupplier.getArtificialRoot(),
                            innerGraphSuccessorSupplier, edgeQualityMetric);
            
            for (GraphNode<MstGraphLevel> node : dijkstra.getShortestPath()) {
                if (node.getGraphLevel() != null) {
                    Map<ManeuverTypeForClassification, List<Double>> votesForNode = 
                            result.computeIfAbsent(node.getGraphLevel(), k -> new HashMap<>());
                    votesForNode.computeIfAbsent(node.getManeuverType(), k -> new ArrayList<>())
                            .add(dijkstra.getPathQuality());
                }
            }
        }
        
        return result;
    }

    private Map<String, String> collectBestNodePerLevel(MstManeuverGraphComponents graphComponents,
            Map<MstGraphLevel, Integer> levelToId,
            Map<MstGraphLevel, Map<ManeuverTypeForClassification, List<Double>>> pathVotes) {
        final Map<String, String> bestNodePerLevel = new HashMap<>();
        
        // Determine best classification per node based on sum of path qualities
        for (Map.Entry<MstGraphLevel, Map<ManeuverTypeForClassification, List<Double>>> entry : pathVotes.entrySet()) {
            MstGraphLevel level = entry.getKey();
            Map<ManeuverTypeForClassification, List<Double>> votes = entry.getValue();
            
            // Find classification with highest sum of path qualities
            double maxSum = -1;
            ManeuverTypeForClassification bestType = null;
            
            for (Map.Entry<ManeuverTypeForClassification, List<Double>> typeVotes : votes.entrySet()) {
                double sum = typeVotes.getValue().stream().mapToDouble(Double::doubleValue).sum();
                if (sum > maxSum) {
                    maxSum = sum;
                    bestType = typeVotes.getKey();
                }
            }
            
            if (bestType != null) {
                Integer nodeId = levelToId.get(level);
                if (nodeId != null) {
                    bestNodePerLevel.put(String.valueOf(nodeId), bestType.name());
                }
            }
        }
        
        return bestNodePerLevel;
    }
    
    /**
     * Formats path vote diagnostics for a node as a JSON string for inclusion in the export.
     */
    private String formatPathVoteDiagnostics(MstGraphLevel level,
            Map<MstGraphLevel, Map<ManeuverTypeForClassification, List<Double>>> pathVotes) {
        Map<ManeuverTypeForClassification, List<Double>> votes = pathVotes.get(level);
        if (votes == null || votes.isEmpty()) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (ManeuverTypeForClassification type : ManeuverTypeForClassification.values()) {
            List<Double> typeVotes = votes.get(type);
            if (typeVotes != null && !typeVotes.isEmpty()) {
                if (!first) sb.append(", ");
                first = false;
                double sum = typeVotes.stream().mapToDouble(Double::doubleValue).sum();
                sb.append("\"").append(type.name()).append("\": {");
                sb.append("\"pathCount\": ").append(typeVotes.size());
                sb.append(", \"qualitySum\": ").append(sum);
                sb.append(", \"qualities\": [");
                sb.append(typeVotes.stream().map(d -> String.format("%.6e", d)).collect(Collectors.joining(", ")));
                sb.append("]}");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private void assignNodeIds(MstGraphLevel level, Map<MstGraphLevel, Integer> levelToId, int[] nodeIdCounter) {
        levelToId.put(level, nodeIdCounter[0]++);
        for (final MstGraphLevel child : level.getChildren()) {
            assignNodeIds(child, levelToId, nodeIdCounter);
        }
    }

    /**
     * Escapes special characters in a string for safe JSON encoding.
     */
    private static String escapeJson(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
