#!/usr/bin/env python3
"""
MST Maneuver Graph Visualization using Graphviz

This creates a hierarchical visualization of the MST graph that better handles
large trees. It uses DOT language to create the graph and renders it with graphviz.

Features:
- Proper tree layout (top to bottom)
- Each node shows 4 compartments as an HTML-like table with PORT attributes
- Edges connect to specific compartments
- Best path highlighted in red
- All other edges shown in green (high prob) or gray (lower prob)
- Edge labels with transition probabilities
- Color-coded compartments by maneuver type
- Legend explaining colors

Usage:
    python mst_graph_visualizer_graphviz.py <input_json_file> [output_file]
    
Dependencies:
    pip install graphviz
    
Also requires graphviz to be installed on the system:
    sudo apt-get install graphviz  # Ubuntu/Debian
    brew install graphviz          # macOS
"""

import json
import sys
from graphviz import Digraph

# Maneuver type colors (HTML hex format)
TYPE_COLORS = {
    'TACK': '#4CAF50',       # Green
    'JIBE': '#2196F3',       # Blue
    'HEAD_UP': '#FF9800',    # Orange
    'BEAR_AWAY': '#9C27B0',  # Purple
}

TYPE_ORDER = ['TACK', 'JIBE', 'HEAD_UP', 'BEAR_AWAY']
TYPE_ABBREV = {'TACK': 'T', 'JIBE': 'J', 'HEAD_UP': 'H', 'BEAR_AWAY': 'B'}


def blend_color_with_white(hex_color, factor):
    """Blend a hex color with white based on factor (0=white, 1=full color)."""
    r = int(int(hex_color[1:3], 16) * factor + 255 * (1 - factor))
    g = int(int(hex_color[3:5], 16) * factor + 255 * (1 - factor))
    b = int(int(hex_color[5:7], 16) * factor + 255 * (1 - factor))
    return f"#{min(255, r):02x}{min(255, g):02x}{min(255, b):02x}"


def format_wind(comp):
    """Format wind direction string."""
    wind_est = comp.get('windEstimate', comp.get('windRangeFrom', 0))
    wind_width = comp.get('windRangeWidth', 0)
    if wind_width < 1:
        return f"{wind_est:.0f}°"
    else:
        return f"{wind_est:.0f}±{wind_width/2:.0f}°"


def format_distance(node):
    """
    Format distance/time to parent in human-readable form.
    
    The node may contain:
    - spatialDistanceToParentMeters: actual spatial distance in meters
    - timeDiffToParentSeconds: actual time difference in seconds
    - compoundDistanceToParent: sum of predicted std deviations (legacy, for transition probability)
    
    We prefer to show actual spatial distance and time if available.
    """
    spatial_dist = node.get('spatialDistanceToParentMeters')
    time_diff = node.get('timeDiffToParentSeconds')
    
    if spatial_dist is not None and time_diff is not None:
        # Format spatial distance
        if spatial_dist >= 1000:
            dist_str = f'{spatial_dist/1000:.1f}km'
        elif spatial_dist >= 1:
            dist_str = f'{spatial_dist:.0f}m'
        else:
            dist_str = f'{spatial_dist:.1f}m'
        
        # Format time difference
        if time_diff >= 60:
            time_str = f'{time_diff/60:.1f}min'
        else:
            time_str = f'{time_diff:.0f}s'
        
        return f'{dist_str}, {time_str}'
    
    # Fallback to compound distance (legacy)
    compound_dist = node.get('compoundDistanceToParent') or node.get('distanceToParent')
    if compound_dist is None or compound_dist == 0:
        return None
    return f'σ={compound_dist:.1f}'  # Mark as std sum with σ symbol


def create_node_label_with_ports(node, best_type=None):
    """
    Create HTML-like label for a node with 4 compartments.
    Each compartment has a PORT attribute for incoming edges (from above).
    The footer row has additional ports for outgoing edges (going down).
    This prevents edges from crossing through the timestamp/distance section.
    """
    compartments = {c['type']: c for c in node['compartments']}
    
    # Extract time from timestamp
    timestamp = node.get('timestamp', '')
    if ' ' in timestamp:
        time_part = timestamp.split(' ')[1][:8]  # HH:MM:SS
    else:
        time_part = timestamp[:8] if len(timestamp) >= 8 else timestamp
    
    # Get distance/time to parent (pass whole node for full info)
    dist_str = format_distance(node)
    
    # Get competitor info
    competitor_name = node.get('competitorName', '')
    # Truncate long names for display
    if competitor_name:
        competitor_str = competitor_name[:12] + '...' if len(competitor_name) > 15 else competitor_name
    else:
        competitor_str = ''
    
    # Build HTML table with PORT attributes
    html = '<<TABLE BORDER="1" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">'
    
    # Main row with compartments - ports here are for INCOMING edges (from above)
    html += '<TR>'
    for type_name in TYPE_ORDER:
        comp = compartments.get(type_name)
        if comp is None:
            continue
        
        confidence = comp['confidence']
        is_best = (best_type == type_name)
        
        # Calculate background color - blend with white based on confidence
        base_color = TYPE_COLORS[type_name]
        if is_best:
            # Best type: full saturation with red border
            bg_color = base_color
            border_attr = f' BGCOLOR="{bg_color}" BORDER="3" COLOR="red"'
        else:
            # Other types: fade based on confidence (min 0.2 to keep some color visible)
            blend_factor = max(0.2, confidence)
            bg_color = blend_color_with_white(base_color, blend_factor)
            border_attr = f' BGCOLOR="{bg_color}"'
        
        wind_str = format_wind(comp)
        abbrev = TYPE_ABBREV[type_name]
        
        # PORT for incoming edges (named after type)
        port_name = type_name
        
        # Create cell content with PORT
        cell_content = (
            f'<B>{abbrev}</B><BR/>'
            f'<FONT POINT-SIZE="9">{confidence:.2f}</FONT><BR/>'
            f'<FONT POINT-SIZE="8">{wind_str}</FONT>'
        )
        
        html += f'<TD PORT="{port_name}"{border_attr}>{cell_content}</TD>'
    html += '</TR>'
    
    # Footer row with competitor info, timestamp and distance - spans all columns
    footer_parts = []
    if competitor_str:
        footer_parts.append(f'<FONT COLOR="purple"><B>{competitor_str}</B></FONT>')
    footer_parts.append(time_part)
    if dist_str:
        footer_parts.append(f'<FONT COLOR="blue">↑{dist_str}</FONT>')
    footer_content = '  '.join(footer_parts)
    html += f'<TR><TD COLSPAN="4" BGCOLOR="white"><FONT POINT-SIZE="9">{footer_content}</FONT></TD></TR>'
    
    # Bottom row with path vote diagnostics AND ports for OUTGOING edges
    # Each cell corresponds to one compartment position for proper horizontal alignment
    path_votes = node.get('pathVotes', {})
    html += '<TR>'
    for type_name in TYPE_ORDER:
        # Port for outgoing edges (named type_out)
        out_port_name = f'{type_name}_out'
        
        vote_info = path_votes.get(type_name, {})
        path_count = vote_info.get('pathCount', 0)
        quality_sum = vote_info.get('qualitySum', 0)
        if path_count > 0:
            # Format quality sum in scientific notation if very small
            if quality_sum < 0.001:
                qs_str = f'{quality_sum:.1e}'
            else:
                qs_str = f'{quality_sum:.3f}'
            vote_content = f'<FONT POINT-SIZE="6" COLOR="gray40">{path_count}p/{qs_str}</FONT>'
        else:
            vote_content = '<FONT POINT-SIZE="6" COLOR="gray70">-</FONT>'
        html += f'<TD PORT="{out_port_name}" BGCOLOR="white">{vote_content}</TD>'
    html += '</TR>'
    
    html += '</TABLE>>'
    
    return html

def create_legend():
    """Create a legend explaining the colors."""
    html = '<<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">'
    html += '<TR><TD COLSPAN="2" BGCOLOR="white"><B>Legend</B></TD></TR>'
    
    # Maneuver type colors
    for type_name in TYPE_ORDER:
        color = TYPE_COLORS[type_name]
        abbrev = TYPE_ABBREV[type_name]
        html += f'<TR><TD BGCOLOR="{color}">{abbrev}</TD><TD BGCOLOR="white">{type_name}</TD></TR>'
    
    # Edge colors
    html += '<TR><TD COLSPAN="2" BGCOLOR="white"><B>Edges</B></TD></TR>'
    html += '<TR><TD BGCOLOR="white"><FONT COLOR="red">━━</FONT></TD><TD BGCOLOR="white">Best Path</TD></TR>'
    html += '<TR><TD BGCOLOR="white"><FONT COLOR="darkgreen">━━</FONT></TD><TD BGCOLOR="white">High Prob (&gt;1%)</TD></TR>'
    html += '<TR><TD BGCOLOR="white"><FONT COLOR="gray50">━━</FONT></TD><TD BGCOLOR="white">Medium Prob</TD></TR>'
    html += '<TR><TD BGCOLOR="white"><FONT COLOR="gray80">━━</FONT></TD><TD BGCOLOR="white">Low Prob</TD></TR>'
    
    html += '</TABLE>>'
    return html


def visualize_mst_graph(data, output_file=None, max_nodes=100, min_edge_prob=0.0):
    """
    Create graphviz visualization of MST graph with edges connecting to specific compartments.
    
    Args:
        data: Parsed JSON data from MstGraphExporter
        output_file: Output file path (without extension; .pdf/.png will be added)
        max_nodes: Maximum number of nodes to show
        min_edge_prob: Minimum edge probability to display (0 = show all)
    """
    nodes = {n['id']: n for n in data['nodes']}
    edges = data['edges']
    best_paths = data.get('bestPaths', {})
    
    # Create directed graph
    dot = Digraph(comment='MST Maneuver Graph')
    dot.attr(rankdir='TB')  # Top to bottom
    dot.attr('node', shape='plaintext')  # Use HTML labels
    dot.attr(splines='polyline')  # Use polyline for clearer edge routing
    dot.attr(nodesep='0.5')  # Horizontal spacing between nodes
    dot.attr(ranksep='1.0')  # Vertical spacing between ranks
    
    # Limit nodes
    nodes_to_draw = list(nodes.keys())[:max_nodes]
    nodes_set = set(nodes_to_draw)
    
    # Add legend
    dot.node('legend', create_legend())
    dot.node('legend_spacer', '', shape='none', width='0', height='0')
    
    # Add nodes
    for node_id in nodes_to_draw:
        node = nodes[node_id]
        best_type = best_paths.get(str(node_id))
        label = create_node_label_with_ports(node, best_type)
        dot.node(str(node_id), label)
    
    # Process ALL edges
    for edge in edges:
        from_id = edge['from']
        to_id = edge['to']
        
        if from_id not in nodes_set or to_id not in nodes_set:
            continue
        
        is_best = edge.get('isBestPath', False)
        trans_prob = edge['transitionProbability']
        from_type = edge['fromType']
        to_type = edge['toType']
        
        # Skip very low probability edges unless they're best path
        if trans_prob < min_edge_prob and not is_best:
            continue
        
        # Style based on best path and probability
        if is_best:
            color = 'red'
            penwidth = '2.5'
            style = 'bold'
            fontcolor = 'red'
        elif trans_prob > 0.01:
            color = 'darkgreen'
            penwidth = '1.2'
            style = 'solid'
            fontcolor = 'darkgreen'
        elif trans_prob > 0.001:
            color = 'gray50'
            penwidth = '0.8'
            style = 'solid'
            fontcolor = 'gray50'
        else:
            color = 'gray80'
            penwidth = '0.5'
            style = 'dashed'
            fontcolor = 'gray70'
        
        # Format probability label
        if trans_prob >= 0.01:
            prob_str = f'{trans_prob:.2f}'
        elif trans_prob >= 0.001:
            prob_str = f'{trans_prob:.3f}'
        else:
            prob_str = f'{trans_prob:.1e}'
        
        # Build edge label with type transition and probability
        from_abbrev = TYPE_ABBREV[from_type]
        to_abbrev = TYPE_ABBREV[to_type]
        edge_label = f'{from_abbrev}→{to_abbrev}\\n{prob_str}'
        
        # Use PORT to connect to specific compartments
        # Outgoing edges use the _out ports in the footer row (below timestamp)
        # Incoming edges use the compartment ports (top of compartment cells)
        from_port = f'{from_id}:{from_type}_out:s'  # :s = south (bottom) of footer cell
        to_port = f'{to_id}:{to_type}:n'            # :n = north (top) of compartment cell
        
        dot.edge(from_port, to_port,
                label=edge_label,
                color=color,
                penwidth=penwidth,
                style=style,
                fontsize='7',
                fontcolor=fontcolor,
                arrowsize='0.6')
    
    # Render
    if output_file:
        # Determine format from extension
        if '.' in output_file:
            base, fmt = output_file.rsplit('.', 1)
        else:
            base = output_file
            fmt = 'pdf'
        
        dot.render(base, format=fmt, cleanup=True)
        print(f"Saved visualization to {base}.{fmt}")
    else:
        # Try to display
        dot.view()
    
    return dot


def create_detailed_edge_graph(data, output_file=None, max_depth=10, min_edge_prob=0.0):
    """
    Create a more detailed graph showing all compartment-to-compartment edges.
    Each node compartment becomes its own graphviz node, grouped by cluster.
    
    This is useful for small subtrees to see the full inner graph structure.
    """
    nodes = {n['id']: n for n in data['nodes']}
    edges = data['edges']
    best_paths = data.get('bestPaths', {})
    
    # Filter to first N levels
    nodes_to_draw = [n for n in data['nodes'] if n['depth'] <= max_depth]
    nodes_set = {n['id'] for n in nodes_to_draw}
    
    dot = Digraph(comment='MST Detailed Inner Graph')
    dot.attr(rankdir='TB')
    dot.attr(nodesep='0.3')
    dot.attr(ranksep='0.8')
    
    # Add legend
    dot.node('legend', create_legend())
    
    # Create subgraph for each tree node (to keep compartments together)
    for node in nodes_to_draw:
        node_id = node['id']
        best_type = best_paths.get(str(node_id))
        
        # Extract time from timestamp for label
        timestamp = node.get('timestamp', '')
        if ' ' in timestamp:
            time_part = timestamp.split(' ')[1][:8]
        else:
            time_part = str(node_id)
        
        # Get distance/time to parent (pass whole node)
        dist_str = format_distance(node)
        
        # Build cluster label with time and distance
        if dist_str:
            cluster_label = f'{time_part}  ↑{dist_str}'
        else:
            cluster_label = time_part
        
        with dot.subgraph(name=f'cluster_{node_id}') as c:
            c.attr(label=cluster_label)
            c.attr(style='rounded,filled')
            c.attr(fillcolor='white')
            
            for comp in node['compartments']:
                type_name = comp['type']
                comp_id = f"{node_id}_{type_name}"
                
                is_best = (best_type == type_name)
                base_color = TYPE_COLORS[type_name]
                confidence = comp['confidence']
                
                if is_best:
                    fillcolor = base_color
                    fontcolor = 'white'
                    penwidth = '3'
                    pencolor = 'red'
                else:
                    # Blend based on confidence
                    blend_factor = max(0.3, confidence)
                    fillcolor = blend_color_with_white(base_color, blend_factor)
                    fontcolor = 'black'
                    penwidth = '1'
                    pencolor = 'black'
                
                label = f"{TYPE_ABBREV[type_name]}\\n{confidence:.2f}\\n{format_wind(comp)}"
                
                c.node(comp_id, label,
                      shape='box',
                      style='filled',
                      fillcolor=fillcolor,
                      fontcolor=fontcolor,
                      fontsize='9',
                      penwidth=penwidth,
                      color=pencolor)
    
    # Add ALL edges between compartments
    for edge in edges:
        from_id = edge['from']
        to_id = edge['to']
        
        if from_id not in nodes_set or to_id not in nodes_set:
            continue
        
        from_comp_id = f"{from_id}_{edge['fromType']}"
        to_comp_id = f"{to_id}_{edge['toType']}"
        
        is_best = edge.get('isBestPath', False)
        trans_prob = edge['transitionProbability']
        
        # Skip very low probability edges unless best path
        if trans_prob < min_edge_prob and not is_best:
            continue
        
        if is_best:
            color = 'red'
            penwidth = '2.5'
            fontcolor = 'red'
        elif trans_prob > 0.01:
            color = 'darkgreen'
            penwidth = '1.2'
            fontcolor = 'darkgreen'
        elif trans_prob > 0.001:
            color = 'gray50'
            penwidth = '0.8'
            fontcolor = 'gray50'
        else:
            color = 'gray80'
            penwidth = '0.4'
            fontcolor = 'gray70'
        
        # Format probability label
        if trans_prob >= 0.01:
            prob_str = f'{trans_prob:.2f}'
        elif trans_prob >= 0.001:
            prob_str = f'{trans_prob:.3f}'
        else:
            prob_str = f'{trans_prob:.1e}'
        
        # Build edge label with type transition and probability
        from_abbrev = TYPE_ABBREV[edge['fromType']]
        to_abbrev = TYPE_ABBREV[edge['toType']]
        edge_label = f'{from_abbrev}→{to_abbrev}\\n{prob_str}'
        
        dot.edge(from_comp_id, to_comp_id,
                label=edge_label,
                color=color,
                penwidth=penwidth,
                fontsize='7',
                fontcolor=fontcolor,
                arrowsize='0.5')
    
    if output_file:
        if '.' in output_file:
            base, fmt = output_file.rsplit('.', 1)
        else:
            base = output_file
            fmt = 'pdf'
        dot.render(base, format=fmt, cleanup=True)
        print(f"Saved detailed visualization to {base}.{fmt}")
    
    return dot


def main():
    if len(sys.argv) < 2:
        print("Usage: python mst_graph_visualizer_graphviz.py <input_json_file> [output_file] [options]")
        print("\nOptions:")
        print("  input_json_file    - JSON file exported from MstGraphExporter")
        print("  output_file        - Output file (extension determines format: .pdf, .png, .svg)")
        print("  --detailed         - Create detailed compartment-level graph (for small graphs)")
        print("  --min-prob=<value> - Minimum edge probability to show (default: 0 = show all)")
        print("  --max-nodes=<n>    - Maximum number of nodes to display (default: 100)")
        print("\nCompartment Colors:")
        print("  T (TACK)      - Green")
        print("  J (JIBE)      - Blue")
        print("  H (HEAD_UP)   - Orange")
        print("  B (BEAR_AWAY) - Purple")
        print("\nEdge Colors:")
        print("  Red       - Best path (selected by algorithm)")
        print("  Dark Green - High probability (>1%)")
        print("  Gray      - Medium/low probability")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = None
    detailed = False
    min_prob = 0.0
    max_nodes = 100
    
    # Parse arguments
    for arg in sys.argv[2:]:
        if arg == '--detailed':
            detailed = True
        elif arg.startswith('--min-prob='):
            min_prob = float(arg.split('=')[1])
        elif arg.startswith('--max-nodes='):
            max_nodes = int(arg.split('=')[1])
        elif not arg.startswith('--'):
            output_file = arg
    
    print(f"Loading graph from {input_file}...")
    with open(input_file, 'r') as f:
        data = json.load(f)
    
    num_nodes = len(data['nodes'])
    num_edges = len(data['edges'])
    print(f"Loaded {num_nodes} nodes, {num_edges} edges")
    print(f"Minimum edge probability: {min_prob}")
    
    if detailed:
        print("Creating detailed compartment-level visualization...")
        create_detailed_edge_graph(data, output_file, max_depth=10, min_edge_prob=min_prob)
    else:
        print(f"Creating tree visualization (max {max_nodes} nodes)...")
        visualize_mst_graph(data, output_file, max_nodes=max_nodes, min_edge_prob=min_prob)


if __name__ == '__main__':
    main()
