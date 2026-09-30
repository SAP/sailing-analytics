# MST Maneuver Graph Visualization

This directory contains tools for visualizing the MST (Minimum Spanning Tree) 
maneuver graph used in wind estimation.

## Overview

The MST maneuver graph is a tree structure where:
- Each tree node (`MstGraphLevel`) represents a detected maneuver with its timestamp and position
- Each tree node has 4 "compartments" - one for each possible maneuver classification:
  - **TACK** (green): Tacking maneuver
  - **JIBE** (blue): Jibing/gybing maneuver  
  - **HEAD_UP** (orange): Heading up toward the wind
  - **BEAR_AWAY** (purple): Bearing away from the wind
- Each compartment shows:
  - Classification confidence (probability this maneuver is of this type)
  - Estimated wind direction range
- Edges connect compartments between adjacent tree levels with transition probabilities
- The "best path" through the inner graph is highlighted - this determines the final maneuver classifications

## Files

### Java (Exporter)

- `MstGraphExporter.java` - Exports the MST graph to JSON format
- `MstGraphExportHelper.java` - Helper class with convenience methods

### Python (Visualizers)

- `mst_graph_visualizer.py` - Basic matplotlib visualization
- `mst_graph_visualizer_graphviz.py` - Advanced graphviz visualization (recommended)

## Usage

### Step 1: Export the graph from Java

In your Java code (e.g., in a test or debug session):

```java
import com.sap.sailing.windestimation.aggregator.msthmm.MstGraphExportHelper;

// After building your MST graph:
MstManeuverGraphComponents graphComponents = mstManeuverGraphGenerator.parseGraph();

// Export to JSON file:
MstGraphExportHelper.exportToFile(graphComponents, transitionProbabilitiesCalculator, 
    "/tmp/mst_graph.json");
```

### Step 2: Visualize with Python

Install dependencies:
```bash
pip install matplotlib graphviz
```

For graphviz, also install the system package:
```bash
# Ubuntu/Debian
sudo apt-get install graphviz

# macOS
brew install graphviz
```

Run the visualizer:
```bash
# Basic tree visualization
python mst_graph_visualizer_graphviz.py /tmp/mst_graph.json output.pdf

# Detailed compartment-level view (for small graphs)
python mst_graph_visualizer_graphviz.py /tmp/mst_graph.json output.pdf --detailed

# Interactive matplotlib visualization
python mst_graph_visualizer.py /tmp/mst_graph.json
```

## Understanding the Visualization

### Node Structure

Each node box contains 4 compartments (T, J, H, B):
```
+------+------+------+------+
|  T   |  J   |  H   |  B   |
| 0.68 | 0.05 | 0.13 | 0.13 |
| 224° |  44° | 308°±51° | 89°±51° |
+------+------+------+------+
         15:30:41
```

- First row: Maneuver type abbreviation
- Second row: Classification confidence
- Third row: Wind direction estimate (or range for HEAD_UP/BEAR_AWAY)
- Below: Timestamp

### Edge Colors

- **Red**: Best path edges (selected by Dijkstra)
- **Green**: High transition probability
- **Gray**: Lower transition probability

### Best Path Highlighting

The best path represents the most likely sequence of maneuver classifications
considering both:
1. Individual classification confidences
2. Transition probabilities (how likely is it for the wind to change this much given the time/distance between maneuvers)

## JSON Format

The exported JSON has this structure:

```json
{
  "nodes": [
    {
      "id": 0,
      "depth": 0,
      "timestamp": "2011-06-23 15:30:40.500",
      "position": {"lat": 54.493, "lon": 10.197},
      "distanceToParent": 0.0,
      "compartments": [
        {
          "type": "TACK",
          "confidence": 0.17,
          "windRangeFrom": 224.18,
          "windRangeWidth": 0.0,
          "windEstimate": 224.18,
          "tackAfter": "PORT"
        },
        // ... JIBE, HEAD_UP, BEAR_AWAY
      ]
    }
  ],
  "edges": [
    {
      "from": 0,
      "fromType": "TACK",
      "to": 1,
      "toType": "TACK",
      "transitionProbability": 0.023,
      "distance": 12.5,
      "isBestPath": true
    }
  ],
  "bestPaths": {
    "0": "JIBE",
    "1": "TACK"
  }
}
```

## Troubleshooting

### Large graphs are slow to render

For graphs with >100 nodes, the visualization limits the display to the first N nodes.
Use the `--detailed` flag only for small subtrees.

### Graphviz not found

Make sure graphviz is installed at the system level, not just the Python package:
```bash
which dot  # Should return path to dot executable
```

### Edges not showing

Edges with very low transition probabilities are filtered out by default.
Use `show_low_prob_edges=True` in Python to see all edges.
