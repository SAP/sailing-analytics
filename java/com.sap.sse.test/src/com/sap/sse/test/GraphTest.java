package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.util.graph.CycleCluster;
import com.sap.sse.util.graph.CycleClusters;
import com.sap.sse.util.graph.DirectedEdge;
import com.sap.sse.util.graph.DirectedGraph;

public class GraphTest {
    @Test
    public void testVerySimpleCycleDetection() {
        final String a = "a";
        final String b = "b";
        final Set<String> nodes = new HashSet<>();
        nodes.add(a);
        nodes.add(b);
        final Set<DirectedEdge<String>> edges = new HashSet<>();
        edges.add(DirectedEdge.create(a, b));
        edges.add(DirectedEdge.create(b, a));
        DirectedGraph<String> graph = DirectedGraph.create(nodes, edges);
        assertEquals(1, Util.size(graph.getCycleClusters().getClusters()));
        assertEquals(2, graph.getCycleClusters().getClusters().iterator().next().getClusterNodes().size());
    }

    @Test
    public void testVerySimpleCycleDetectionWithTwoCycles() {
        final String a = "a";
        final String b = "b";
        final String c = "c";
        final String d = "d";
        final Set<String> nodes = new HashSet<>();
        nodes.add(a);
        nodes.add(b);
        nodes.add(c);
        nodes.add(d);
        final Set<DirectedEdge<String>> edges = new HashSet<>();
        edges.add(DirectedEdge.create(a, b));
        edges.add(DirectedEdge.create(b, a));
        edges.add(DirectedEdge.create(c, d));
        edges.add(DirectedEdge.create(d, c));
        edges.add(DirectedEdge.create(a, c)); // connect the two cycles, but not cyclically
        DirectedGraph<String> graph = DirectedGraph.create(nodes, edges);
        assertEquals(2, Util.size(graph.getCycleClusters().getClusters()));
        for (final CycleCluster<String> cluster : graph.getCycleClusters().getClusters()) {
            assertEquals(2, cluster.getClusterNodes().size());
        }
    }

    @Test
    public void testCycleDetection() {
        final String cro = "CRO";
        final String usa = "USA";
        final String msc = "MSC";
        final String pol1 = "POL1";
        final String pol2 = "POL2";
        final String fin = "FIN";
        final String ger = "GER";
        final Set<String> nodes = new HashSet<>();
        nodes.add(cro);
        nodes.add(usa);
        nodes.add(msc);
        nodes.add(pol1);
        nodes.add(pol2);
        nodes.add(fin);
        nodes.add(ger);
        final List<DirectedEdge<String>> edgeList = new ArrayList<>();
        // CRO
        edgeList.add(DirectedEdge.create(cro, usa));
        edgeList.add(DirectedEdge.create(cro, msc));
        edgeList.add(DirectedEdge.create(cro, pol1));
        edgeList.add(DirectedEdge.create(cro, pol2));
        edgeList.add(DirectedEdge.create(cro, fin));
        edgeList.add(DirectedEdge.create(cro, ger));
        // USA
        edgeList.add(DirectedEdge.create(usa, msc));
        edgeList.add(DirectedEdge.create(usa, pol1));
        edgeList.add(DirectedEdge.create(usa, pol2));
        edgeList.add(DirectedEdge.create(usa, fin));
        edgeList.add(DirectedEdge.create(usa, ger));
        // MSC
        edgeList.add(DirectedEdge.create(msc, usa));
        edgeList.add(DirectedEdge.create(msc, pol2));
        edgeList.add(DirectedEdge.create(msc, fin));
        // POL1
        edgeList.add(DirectedEdge.create(pol1, fin));
        // POL2
        edgeList.add(DirectedEdge.create(pol2, fin));
        // FIN
        edgeList.add(DirectedEdge.create(fin, msc));
        // GER
        edgeList.add(DirectedEdge.create(ger, msc));
        edgeList.add(DirectedEdge.create(ger, pol1));
        edgeList.add(DirectedEdge.create(ger, pol2));
        edgeList.add(DirectedEdge.create(ger, fin));
        Collections.shuffle(edgeList);
        Set<DirectedEdge<String>> edges = new LinkedHashSet<>();
        edges.addAll(edgeList);
        DirectedGraph<String> graph = DirectedGraph.create(nodes, edges);
        assertEquals(1, Util.size(graph.getCycleClusters().getClusters()));
        assertEquals(6, graph.getCycleClusters().getClusters().iterator().next().getClusterNodes().size());
    }

    @Test
    public void testCycleDetectionWithDuplicateEdges() {
        final String cro = "CRO";
        final String usa = "USA";
        final String msc = "MSC";
        final String pol1 = "POL1";
        final String pol2 = "POL2";
        final String fin = "FIN";
        final String ger = "GER";
        final Set<String> nodes = new HashSet<>();
        nodes.add(cro);
        nodes.add(usa);
        nodes.add(msc);
        nodes.add(pol1);
        nodes.add(pol2);
        nodes.add(fin);
        nodes.add(ger);
        final Set<DirectedEdge<String>> edges = new HashSet<>();
        // CRO
        edges.add(DirectedEdge.create(cro, usa));
        edges.add(DirectedEdge.create(cro, msc));
        edges.add(DirectedEdge.create(cro, pol1));
        edges.add(DirectedEdge.create(cro, pol2));
        edges.add(DirectedEdge.create(cro, fin));
        edges.add(DirectedEdge.create(cro, ger));
        edges.add(DirectedEdge.create(cro, usa));
        edges.add(DirectedEdge.create(cro, msc));
        edges.add(DirectedEdge.create(cro, pol1));
        edges.add(DirectedEdge.create(cro, pol2));
        edges.add(DirectedEdge.create(cro, fin));
        edges.add(DirectedEdge.create(cro, ger));
        // USA
        edges.add(DirectedEdge.create(usa, msc));
        edges.add(DirectedEdge.create(usa, pol1));
        edges.add(DirectedEdge.create(usa, pol2));
        edges.add(DirectedEdge.create(usa, fin));
        edges.add(DirectedEdge.create(usa, ger));
        edges.add(DirectedEdge.create(usa, msc));
        edges.add(DirectedEdge.create(usa, pol1));
        edges.add(DirectedEdge.create(usa, pol2));
        edges.add(DirectedEdge.create(usa, fin));
        edges.add(DirectedEdge.create(usa, ger));
        // MSC
        edges.add(DirectedEdge.create(msc, usa));
        edges.add(DirectedEdge.create(msc, pol2));
        edges.add(DirectedEdge.create(msc, fin));
        edges.add(DirectedEdge.create(msc, usa));
        edges.add(DirectedEdge.create(msc, pol2));
        edges.add(DirectedEdge.create(msc, fin));
        // POL1
        edges.add(DirectedEdge.create(pol1, fin));
        edges.add(DirectedEdge.create(pol1, fin));
        // POL2
        edges.add(DirectedEdge.create(pol2, fin));
        edges.add(DirectedEdge.create(pol2, fin));
        // FIN
        edges.add(DirectedEdge.create(fin, msc));
        edges.add(DirectedEdge.create(fin, msc));
        // GER
        edges.add(DirectedEdge.create(ger, msc));
        edges.add(DirectedEdge.create(ger, pol1));
        edges.add(DirectedEdge.create(ger, pol2));
        edges.add(DirectedEdge.create(ger, fin));
        edges.add(DirectedEdge.create(ger, msc));
        edges.add(DirectedEdge.create(ger, pol1));
        edges.add(DirectedEdge.create(ger, pol2));
        edges.add(DirectedEdge.create(ger, fin));
        DirectedGraph<String> graph = DirectedGraph.create(nodes, edges);
        assertEquals(1, Util.size(graph.getCycleClusters().getClusters()));
        assertEquals(6, graph.getCycleClusters().getClusters().iterator().next().getClusterNodes().size());
    }
    
    @Test
    public void testCompetitorGraphWithMultipleCycles() {
        final String usa1 = "USA1";
        final String cro = "CRO";
        final String pol2 = "POL2";
        final String fin = "FIN";
        final String ger = "GER";
        final String nor = "NOR";
        final String ksss = "KSSS";
        final String pot = "POT";
        final String usa2 = "USA2";
        final String mal = "MAL";
        final Set<String> nodes = new LinkedHashSet<>();
        nodes.addAll(Arrays.asList(usa2, nor, ksss, pot, pol2, usa1, cro, mal, fin, ger));
        final Set<DirectedEdge<String>> edges = new HashSet<>();
        // USA 2 LISOT Black with boat USA 2 LISOT Black=[Norway with boat Norway, Sweden KSSS with boat Sweden KSSS,
        // Potsdamer Yacht Club with boat Potsdamer Yacht Club, Poland 2 with boat Poland 2, Sweden Malmström with boat
        // Sweden Malmström, Finland with boat Finland, Germany Worlds with boat Germany Worlds]
        edges.add(DirectedEdge.create(usa2, nor));
        edges.add(DirectedEdge.create(usa2, ksss));
        edges.add(DirectedEdge.create(usa2, pot));
        edges.add(DirectedEdge.create(usa2, pol2));
        edges.add(DirectedEdge.create(usa2, mal));
        edges.add(DirectedEdge.create(usa2, fin));
        edges.add(DirectedEdge.create(usa2, ger));
        // Norway with boat Norway=[Finland with boat Finland]
        edges.add(DirectedEdge.create(nor, fin));
        // Sweden KSSS with boat Sweden KSSS=[Norway with boat Norway, Potsdamer Yacht Club with boat Potsdamer Yacht
        // Club, Poland 2 with boat Poland 2, USA 1 Hampton Sailing Club with boat USA 1 Hampton Sailing Club, Finland
        // with boat Finland]
        edges.add(DirectedEdge.create(ksss, nor));
        edges.add(DirectedEdge.create(ksss, pot));
        edges.add(DirectedEdge.create(ksss, pol2));
        edges.add(DirectedEdge.create(ksss, usa1));
        edges.add(DirectedEdge.create(ksss, fin));
        // Potsdamer Yacht Club with boat Potsdamer Yacht Club=[Norway with boat Norway, Finland with boat Finland]
        edges.add(DirectedEdge.create(pot, nor));
        edges.add(DirectedEdge.create(pot, fin));
        // Poland 2 with boat Poland 2=[Norway with boat Norway, Sweden KSSS with boat Sweden KSSS, Potsdamer Yacht Club
        // with boat Potsdamer Yacht Club, Finland with boat Finland]
        edges.add(DirectedEdge.create(nor, ksss));
        edges.add(DirectedEdge.create(nor, pot));
        edges.add(DirectedEdge.create(nor, fin));
        // USA 1 Hampton Sailing Club with boat USA 1 Hampton Sailing Club=[USA 2 LISOT Black with boat USA 2 LISOT
        // Black, Norway with boat Norway, Sweden KSSS with boat Sweden KSSS, Potsdamer Yacht Club with boat Potsdamer
        // Yacht Club, Poland 2 with boat Poland 2, Finland with boat Finland, Germany Worlds with boat Germany Worlds]
        edges.add(DirectedEdge.create(usa1, usa2));
        edges.add(DirectedEdge.create(usa1, nor));
        edges.add(DirectedEdge.create(usa1, ksss));
        edges.add(DirectedEdge.create(usa1, pot));
        edges.add(DirectedEdge.create(usa1, pol2));
        edges.add(DirectedEdge.create(usa1, fin));
        edges.add(DirectedEdge.create(usa1, ger));
        // Croatia with boat Croatia=[USA 2 LISOT Black with boat USA 2 LISOT Black, Norway with boat Norway, Sweden
        // KSSS with boat Sweden KSSS, Potsdamer Yacht Club with boat Potsdamer Yacht Club, Poland 2 with boat Poland 2,
        // USA 1 Hampton Sailing Club with boat USA 1 Hampton Sailing Club, Finland with boat Finland, Sweden Malmström
        // with boat Sweden Malmström, Germany Worlds with boat Germany Worlds]
        edges.add(DirectedEdge.create(cro, usa2));
        edges.add(DirectedEdge.create(cro, nor));
        edges.add(DirectedEdge.create(cro, ksss));
        edges.add(DirectedEdge.create(cro, pot));
        edges.add(DirectedEdge.create(cro, pol2));
        edges.add(DirectedEdge.create(cro, usa1));
        edges.add(DirectedEdge.create(cro, fin));
        edges.add(DirectedEdge.create(cro, mal));
        edges.add(DirectedEdge.create(cro, ger));
        // Sweden Malmström with boat Sweden Malmström=[Norway with boat Norway, Sweden KSSS with boat Sweden KSSS,
        // Potsdamer Yacht Club with boat Potsdamer Yacht Club, Poland 2 with boat Poland 2, USA 1 Hampton Sailing Club
        // with boat USA 1 Hampton Sailing Club, Croatia with boat Croatia, Finland with boat Finland, Germany Worlds
        // with boat Germany Worlds]
        edges.add(DirectedEdge.create(mal, nor));
        edges.add(DirectedEdge.create(mal, ksss));
        edges.add(DirectedEdge.create(mal, pot));
        edges.add(DirectedEdge.create(mal, pol2));
        edges.add(DirectedEdge.create(mal, usa1));
        edges.add(DirectedEdge.create(mal, cro));
        edges.add(DirectedEdge.create(mal, fin));
        edges.add(DirectedEdge.create(mal, ger));
        // Finland with boat Finland=[Potsdamer Yacht Club with boat Potsdamer Yacht Club]
        edges.add(DirectedEdge.create(fin, pot));
        // Germany Worlds with boat Germany Worlds=[Norway with boat Norway, Sweden KSSS with boat Sweden KSSS,
        // Potsdamer Yacht Club with boat Potsdamer Yacht Club, Poland 2 with boat Poland 2, Finland with boat Finland]
        edges.add(DirectedEdge.create(ger, nor));
        edges.add(DirectedEdge.create(ger, ksss));
        edges.add(DirectedEdge.create(ger, pot));
        edges.add(DirectedEdge.create(ger, pol2));
        edges.add(DirectedEdge.create(ger, fin));
        // graph construction:
        final DirectedGraph<String> graph = DirectedGraph.create(nodes, edges);
        final Pair<DirectedGraph<String>, CycleClusters<String>> dag = graph.graphWithCombinedCycleNodes();
        // tests:
        assertTrue(Util.isEmpty(dag.getA().getCycleClusters().getClusters()));
    }
}
