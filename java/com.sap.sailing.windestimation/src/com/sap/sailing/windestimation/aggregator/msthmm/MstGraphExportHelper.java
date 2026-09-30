package com.sap.sailing.windestimation.aggregator.msthmm;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;

import com.sap.sailing.windestimation.aggregator.msthmm.MstManeuverGraphGenerator.MstManeuverGraphComponents;

/**
 * Example usage of MstGraphExporter for debugging and visualization.
 * 
 * <p>This class can be used to export the MST graph to JSON format, which can then
 * be visualized using the Python script mst_graph_visualizer.py.</p>
 * 
 * <p>Example usage in a test or debug context:</p>
 * <pre>
 * // After building your MST graph:
 * MstManeuverGraphComponents graphComponents = mstManeuverGraphGenerator.parseGraph();
 * 
 * // Export to JSON file:
 * MstGraphExportHelper.exportToFile(graphComponents, transitionProbabilitiesCalculator, 
 *     "/tmp/mst_graph.json");
 * 
 * // Then run from command line:
 * // python mst_graph_visualizer.py /tmp/mst_graph.json /tmp/mst_graph.png
 * </pre>
 * 
 * @author Generated for visualization purposes using Claude Opus 4.5
 */
public class MstGraphExportHelper {
    private static final Logger logger = Logger.getLogger(MstGraphExportHelper.class.getName());

    /**
     * Exports the MST graph to a JSON file.
     * 
     * @param graphComponents The MST graph components to export
     * @param transitionProbabilitiesCalculator Calculator for edge probabilities
     * @param filePath Path to write the JSON file
     * @throws IOException if writing fails
     */
    public static void exportToFile(MstManeuverGraphComponents graphComponents,
            MstGraphNodeTransitionProbabilitiesCalculator transitionProbabilitiesCalculator,
            String filePath) throws IOException {
        final MstGraphExporter exporter = new MstGraphExporter(transitionProbabilitiesCalculator);
        try (final FileWriter writer = new FileWriter(filePath)) {
            exporter.exportToJson(graphComponents, writer);
        }
        logger.info("Exported MST graph to: " + filePath);
        logger.info("Visualize with: python mst_graph_visualizer.py " + filePath + " output.png");
    }

    /**
     * Exports the MST graph to a JSON string (useful for testing/debugging).
     * 
     * @param graphComponents The MST graph components to export
     * @param transitionProbabilitiesCalculator Calculator for edge probabilities
     * @return JSON string representation of the graph
     * @throws IOException if writing fails
     */
    public static String exportToString(MstManeuverGraphComponents graphComponents,
            MstGraphNodeTransitionProbabilitiesCalculator transitionProbabilitiesCalculator) throws IOException {
        final MstGraphExporter exporter = new MstGraphExporter(transitionProbabilitiesCalculator);
        final StringWriter writer = new StringWriter();
        exporter.exportToJson(graphComponents, writer);
        return writer.toString();
    }

    /**
     * Exports the MST graph to a provided writer.
     * 
     * @param graphComponents The MST graph components to export
     * @param transitionProbabilitiesCalculator Calculator for edge probabilities
     * @param writer Writer to output JSON to
     * @throws IOException if writing fails
     */
    public static void exportToWriter(MstManeuverGraphComponents graphComponents,
            MstGraphNodeTransitionProbabilitiesCalculator transitionProbabilitiesCalculator,
            Writer writer) throws IOException {
        final MstGraphExporter exporter = new MstGraphExporter(transitionProbabilitiesCalculator);
        exporter.exportToJson(graphComponents, writer);
    }
}
