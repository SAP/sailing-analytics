#!/usr/bin/env python3
"""
MST Maneuver Graph Visualization Script

This script visualizes the Minimum Spanning Tree (MST) maneuver graph structure
exported from the Java MstGraphExporter. It shows:
- Tree structure of MstGraphLevel nodes
- Each node as a box with 4 compartments (TACK, JIBE, HEAD_UP, BEAR_AWAY)
- Classification confidence and wind direction for each compartment
- Edges between compartments with transition probabilities
- Best path highlighting

Usage:
    python mst_graph_visualizer.py <input_json_file> [output_file]
    
Dependencies:
    pip install matplotlib networkx
"""

import json
import sys
import math
from collections import defaultdict

import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import matplotlib.colors as mcolors

# Maneuver type colors
TYPE_COLORS = {
    'TACK': '#4CAF50',       # Green
    'JIBE': '#2196F3',       # Blue
    'HEAD_UP': '#FF9800',    # Orange
    'BEAR_AWAY': '#9C27B0',  # Purple
}

TYPE_ORDER = ['TACK', 'JIBE', 'HEAD_UP', 'BEAR_AWAY']


class MstGraphVisualizer:
    def __init__(self, data):
        self.data = data
        self.nodes = {n['id']: n for n in data['nodes']}
        self.edges = data['edges']
        self.best_paths = data.get('bestPaths', {})
        
        # Layout parameters
        self.node_width = 4.0
        self.node_height = 1.2
        self.compartment_width = self.node_width / 4
        self.horizontal_spacing = 1.5
        self.vertical_spacing = 2.5
        
        # Calculate layout
        self.node_positions = {}
        self.calculate_layout()
    
    def calculate_layout(self):
        """Calculate x,y positions for each node based on tree structure."""
        # Group nodes by depth
        depth_groups = defaultdict(list)
        for node in self.data['nodes']:
            depth_groups[node['depth']].append(node['id'])
        
        max_depth = max(depth_groups.keys()) if depth_groups else 0
        
        # Assign positions - breadth-first layout
        for depth in range(max_depth + 1):
            nodes_at_depth = depth_groups[depth]
            n = len(nodes_at_depth)
            for i, node_id in enumerate(nodes_at_depth):
                # Center nodes horizontally at each level
                x = (i - (n - 1) / 2) * (self.node_width + self.horizontal_spacing)
                y = -depth * (self.node_height + self.vertical_spacing)
                self.node_positions[node_id] = (x, y)
    
    def draw_node(self, ax, node_id):
        """Draw a single node as a box with 4 compartments."""
        node = self.nodes[node_id]
        x, y = self.node_positions[node_id]
        
        # Draw outer box
        outer_rect = FancyBboxPatch(
            (x - self.node_width/2, y - self.node_height/2),
            self.node_width, self.node_height,
            boxstyle="round,pad=0.02,rounding_size=0.1",
            facecolor='white',
            edgecolor='black',
            linewidth=1.5
        )
        ax.add_patch(outer_rect)
        
        # Check if this node has a best classification
        best_type = self.best_paths.get(str(node_id))
        
        # Draw compartments
        compartments = {c['type']: c for c in node['compartments']}
        for i, type_name in enumerate(TYPE_ORDER):
            comp = compartments.get(type_name)
            if comp is None:
                continue
            
            cx = x - self.node_width/2 + i * self.compartment_width
            cy = y - self.node_height/2
            
            # Highlight best type
            is_best = (best_type == type_name)
            
            # Draw compartment background
            color = TYPE_COLORS.get(type_name, 'gray')
            alpha = 0.8 if is_best else 0.3
            comp_rect = patches.Rectangle(
                (cx, cy),
                self.compartment_width, self.node_height,
                facecolor=color,
                alpha=alpha,
                edgecolor='black' if is_best else 'gray',
                linewidth=2 if is_best else 0.5
            )
            ax.add_patch(comp_rect)
            
            # Draw text - type abbreviation
            type_abbrev = type_name[:1]  # T, J, H, B
            ax.text(cx + self.compartment_width/2, cy + self.node_height * 0.75,
                   type_abbrev, ha='center', va='center', fontsize=8, fontweight='bold')
            
            # Draw confidence
            conf_text = f"{comp['confidence']:.2f}"
            ax.text(cx + self.compartment_width/2, cy + self.node_height * 0.5,
                   conf_text, ha='center', va='center', fontsize=6)
            
            # Draw wind direction
            wind_est = comp.get('windEstimate', comp.get('windRangeFrom', 0))
            wind_width = comp.get('windRangeWidth', 0)
            if wind_width < 1:
                wind_text = f"{wind_est:.0f}°"
            else:
                wind_text = f"{wind_est:.0f}±{wind_width/2:.0f}°"
            ax.text(cx + self.compartment_width/2, cy + self.node_height * 0.25,
                   wind_text, ha='center', va='center', fontsize=5)
        
        # Draw timestamp label below node
        timestamp = node.get('timestamp', '')
        # Extract just time portion
        if ' ' in timestamp:
            time_part = timestamp.split(' ')[1][:8]  # HH:MM:SS
        else:
            time_part = timestamp
        ax.text(x, y - self.node_height/2 - 0.15, time_part,
               ha='center', va='top', fontsize=5, color='gray')
    
    def draw_edge(self, ax, edge):
        """Draw an edge between two compartments."""
        from_id = edge['from']
        to_id = edge['to']
        from_type = edge['fromType']
        to_type = edge['toType']
        trans_prob = edge['transitionProbability']
        is_best = edge.get('isBestPath', False)
        
        # Get compartment positions
        from_x, from_y = self.node_positions[from_id]
        to_x, to_y = self.node_positions[to_id]
        
        from_type_idx = TYPE_ORDER.index(from_type)
        to_type_idx = TYPE_ORDER.index(to_type)
        
        # Calculate start and end points at compartment centers
        start_x = from_x - self.node_width/2 + (from_type_idx + 0.5) * self.compartment_width
        start_y = from_y - self.node_height/2
        
        end_x = to_x - self.node_width/2 + (to_type_idx + 0.5) * self.compartment_width
        end_y = to_y + self.node_height/2
        
        # Color based on transition probability and best path status
        if is_best:
            color = 'red'
            linewidth = 2.0
            alpha = 0.9
        else:
            # Color from green (high prob) to gray (low prob)
            prob_normalized = min(1.0, trans_prob * 100)  # Scale up small probabilities
            color = plt.cm.RdYlGn(prob_normalized)
            linewidth = 0.5 + prob_normalized * 1.5
            alpha = 0.2 + prob_normalized * 0.4
        
        # Draw the edge
        ax.annotate('',
                   xy=(end_x, end_y), xytext=(start_x, start_y),
                   arrowprops=dict(
                       arrowstyle='-|>',
                       color=color,
                       lw=linewidth,
                       alpha=alpha,
                       shrinkA=2, shrinkB=2,
                       connectionstyle="arc3,rad=0.1" if abs(from_type_idx - to_type_idx) > 0 else "arc3,rad=0"
                   ))
        
        # Optionally add probability label for best path edges
        if is_best:
            mid_x = (start_x + end_x) / 2
            mid_y = (start_y + end_y) / 2
            ax.text(mid_x + 0.3, mid_y, f"{trans_prob:.2e}",
                   fontsize=4, color='red', alpha=0.8)
    
    def visualize(self, output_file=None, max_nodes=50, show_edges=True, 
                  edge_filter_threshold=0.0001):
        """
        Create the visualization.
        
        Args:
            output_file: Path to save the figure (or None to display)
            max_nodes: Maximum number of nodes to display (for large graphs)
            show_edges: Whether to draw edges
            edge_filter_threshold: Only show edges with probability above this
        """
        # Limit nodes if graph is too large
        nodes_to_draw = list(self.nodes.keys())[:max_nodes]
        
        # Calculate figure size based on layout
        x_coords = [self.node_positions[n][0] for n in nodes_to_draw]
        y_coords = [self.node_positions[n][1] for n in nodes_to_draw]
        
        width = max(x_coords) - min(x_coords) + self.node_width * 2
        height = max(y_coords) - min(y_coords) + self.node_height * 3
        
        # Scale figure
        scale = 0.8
        fig_width = max(12, width * scale)
        fig_height = max(8, height * scale)
        
        fig, ax = plt.subplots(figsize=(fig_width, fig_height))
        
        # Draw edges first (so they're behind nodes)
        if show_edges:
            # Filter edges to only those involving displayed nodes
            relevant_edges = [e for e in self.edges 
                            if e['from'] in nodes_to_draw 
                            and e['to'] in nodes_to_draw
                            and (e['transitionProbability'] > edge_filter_threshold 
                                 or e.get('isBestPath', False))]
            
            for edge in relevant_edges:
                self.draw_edge(ax, edge)
        
        # Draw nodes
        for node_id in nodes_to_draw:
            self.draw_node(ax, node_id)
        
        # Add legend
        legend_elements = [
            patches.Patch(facecolor=TYPE_COLORS['TACK'], alpha=0.6, label='TACK'),
            patches.Patch(facecolor=TYPE_COLORS['JIBE'], alpha=0.6, label='JIBE'),
            patches.Patch(facecolor=TYPE_COLORS['HEAD_UP'], alpha=0.6, label='HEAD_UP'),
            patches.Patch(facecolor=TYPE_COLORS['BEAR_AWAY'], alpha=0.6, label='BEAR_AWAY'),
            patches.Patch(facecolor='red', alpha=0.6, label='Best Path'),
        ]
        ax.legend(handles=legend_elements, loc='upper right', fontsize=8)
        
        # Set axis limits
        margin = 2
        ax.set_xlim(min(x_coords) - margin, max(x_coords) + margin)
        ax.set_ylim(min(y_coords) - margin, max(y_coords) + margin)
        
        ax.set_aspect('equal')
        ax.axis('off')
        
        plt.title(f'MST Maneuver Graph ({len(nodes_to_draw)} nodes)', fontsize=14)
        plt.tight_layout()
        
        if output_file:
            plt.savefig(output_file, dpi=150, bbox_inches='tight', 
                       facecolor='white', edgecolor='none')
            print(f"Saved visualization to {output_file}")
        else:
            plt.show()
        
        plt.close()
    
    def visualize_subtree(self, root_id, depth_limit=5, output_file=None):
        """Visualize a subtree starting from a specific node."""
        # Collect nodes in subtree
        subtree_nodes = set()
        def collect_subtree(node_id, current_depth):
            if current_depth > depth_limit:
                return
            subtree_nodes.add(node_id)
            node = self.nodes[node_id]
            # Find children by looking at edges
            for edge in self.edges:
                if edge['from'] == node_id:
                    collect_subtree(edge['to'], current_depth + 1)
        
        collect_subtree(root_id, 0)
        
        # Create filtered data
        filtered_data = {
            'nodes': [n for n in self.data['nodes'] if n['id'] in subtree_nodes],
            'edges': [e for e in self.edges if e['from'] in subtree_nodes and e['to'] in subtree_nodes],
            'bestPaths': {k: v for k, v in self.best_paths.items() if int(k) in subtree_nodes}
        }
        
        # Create new visualizer for subtree
        subtree_viz = MstGraphVisualizer(filtered_data)
        subtree_viz.visualize(output_file=output_file)


def main():
    if len(sys.argv) < 2:
        print("Usage: python mst_graph_visualizer.py <input_json_file> [output_file]")
        print("\nOptions:")
        print("  input_json_file  - JSON file exported from MstGraphExporter")
        print("  output_file      - Optional output image file (PNG, PDF, SVG)")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else None
    
    print(f"Loading graph from {input_file}...")
    with open(input_file, 'r') as f:
        data = json.load(f)
    
    print(f"Loaded {len(data['nodes'])} nodes, {len(data['edges'])} edges")
    
    visualizer = MstGraphVisualizer(data)
    
    # For large graphs, show only first N nodes
    num_nodes = len(data['nodes'])
    if num_nodes > 100:
        print(f"Graph has {num_nodes} nodes, showing first 50 for clarity")
        visualizer.visualize(output_file=output_file, max_nodes=50)
    else:
        visualizer.visualize(output_file=output_file, max_nodes=num_nodes)


if __name__ == '__main__':
    main()
