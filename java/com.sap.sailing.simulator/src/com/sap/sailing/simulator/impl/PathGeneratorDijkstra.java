package com.sap.sailing.simulator.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sap.sailing.simulator.Grid;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.TimedPosition;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PathGeneratorDijkstra extends PathGeneratorBase {

    public PathGeneratorDijkstra(SimulationParameters params) {
        this.parameters = params;
    }

    // @Override
    @Override
    public Path getPath() {
        this.algorithmStartTime = MillisecondsTimePoint.now();

        // retrieve simulation parameters
        Grid boundary = new RectangularGrid(this.parameters.getCourse().get(0), this.parameters.getCourse().get(1));// simulationParameters.getBoundaries();
        WindFieldGenerator windField = this.parameters.getWindField();
        PolarDiagram polarDiagram = this.parameters.getBoatPolarDiagram();
        Position start = this.parameters.getCourse().get(0);
        Position end = this.parameters.getCourse().get(1);
        TimePoint startTime = windField.getStartTime();// new MillisecondsTimePoint(0);

        // the solution path
        LinkedList<TimedPositionWithSpeed> lst = new LinkedList<TimedPositionWithSpeed>();

        // initiate grid
        int gridv = 10; // number of vertical grid steps
        int gridh = 100; // number of horizontal grid
        // steps
        Position[][] sailGrid = boundary.generatePositions(gridh, gridv, 0, 0);

        // create adjacency graph including start and end
        Map<Position, List<Position>> graph = new HashMap<Position, List<Position>>();
        graph.put(start, Arrays.asList(sailGrid[1]));
        for (int i = 1; i < gridv - 2; i++) {
            for (Position p : sailGrid[i]) {
                graph.put(p, Arrays.asList(sailGrid[i + 1]));
            }
        }
        for (Position p : sailGrid[gridv - 2]) {
            graph.put(p, Arrays.asList(end));
        }

        /*
         * //create backwards adjacency graph, required to reconstruct the optimal path Map<Position, List<Position>>
         * backGraph = new HashMap<Position, List<Position>>(); backGraph.put(end, Arrays.asList(sailGrid[gridv-2]));
         * for(int i = gridv-2; i > 1; i--) { for(Position p: sailGrid[i]) { backGraph.put(p,
         * Arrays.asList(sailGrid[i-1])); } } for(Position p : sailGrid[1]) { backGraph.put(p, Arrays.asList(start)); }
         */

        // create tentative distance matrix
        // additional to tentative distances, the matrix also contains the root of each position
        // that can be </null> if unavailable
        Map<Position, Util.Pair<Long, Position>> tentativeDistances = new HashMap<Position, Util.Pair<Long, Position>>();
        for (Position p : graph.keySet()) {
            tentativeDistances.put(p, new Util.Pair<Long, Position>(Long.MAX_VALUE, null));
        }
        tentativeDistances.put(start, new Util.Pair<Long, Position>(startTime.asMillis(), null));
        tentativeDistances.put(end, new Util.Pair<Long, Position>(Long.MAX_VALUE, null));

        // create set of unvisited nodes
        List<Position> unvisited = new ArrayList<Position>(graph.keySet());
        unvisited.add(end);

        // set the initial node as current
        Position currentPosition = start;
        TimePoint currentTime = startTime;
        // Bearing previousBearing = null;

        // search loop
        // ends when the end is visited
        while ((currentPosition != end)&&(!this.isTimedOut())) {
            // set the polar diagram to the wind at the current position and time
            TimedPosition currentTimedPosition = new TimedPositionImpl(currentTime, currentPosition);
            SpeedWithBearing currentWind = windField.getWind(currentTimedPosition);
            polarDiagram.setWind(currentWind);

            // compute the tentative distance to all the unvisited neighbours of the current node
            // and replace it in the matrix if is smaller than the previous one
            List<Position> unvisitedNeighbours = new LinkedList<Position>(graph.get(currentPosition));
            unvisitedNeighbours.retainAll(unvisited);
            for (Position p : unvisitedNeighbours) {
                Bearing bearingToP = currentPosition.getBearingGreatCircle(p);
                Distance distanceToP = currentPosition.getDistance(p);
                Speed speedToP = polarDiagram.getSpeedAtBearing(bearingToP);
                // multiplied by 1000 to have milliseconds
                Long timeToP = (long) (1000 * (distanceToP.getMeters() / speedToP.getMetersPerSecond()));
                /*
                 * if (previousBearing != null) { Bearing windBearingFrom = currentWind.getBearing().reverse(); if(
                 * (PolarDiagram49.bearingComparator.compare(bearingToP, windBearingFrom) > 0) &&
                 * (PolarDiagram49.bearingComparator.compare(previousBearing, windBearingFrom) < 0) ) timeToP = timeToP
                 * + 4000; if( (PolarDiagram49.bearingComparator.compare(bearingToP, windBearingFrom) < 0) &&
                 * (PolarDiagram49.bearingComparator.compare(previousBearing, windBearingFrom) > 0) ) timeToP = timeToP
                 * + 4000; }
                 */

                Long tentativeDistanceToP = currentTime.asMillis() + timeToP;
                if (tentativeDistanceToP < tentativeDistances.get(p).getA()) {
                    tentativeDistances.put(p, new Util.Pair<Long, Position>(tentativeDistanceToP, currentPosition));
                }
            }

            // mark current node as visited
            unvisited.remove(currentPosition);

            // select the unvisited node with the smallest tentative distance
            // and set it as current
            Long minTentativeDistance = Long.MAX_VALUE;
            for (Position p : unvisited) {
                if (tentativeDistances.get(p).getA() < minTentativeDistance) {
                    currentPosition = p;
                    minTentativeDistance = tentativeDistances.get(p).getA();
                    // previousBearing = tentativeDistances.get(p).getB().getBearingGreatCircle(currentPosition);
                    currentTime = new MillisecondsTimePoint(minTentativeDistance);
                }

            }
        }
        // I need to add the end point to the distances matrix
        // tentativeDistances.put(end,currentTime.asMillis());

        // at this point currentPosition = end
        // currentTime = total duration of the course

        // reconstruct the optimal path by going from start to end
        /*
         * while(currentPosition != start) { TimedPositionWithSpeed currentTimedPositionWithSpeed = new
         * TimedPositionWithSpeedImpl(currentTime, currentPosition, null ); lst.addFirst(currentTimedPositionWithSpeed);
         * System.out.println(boundary.getGridIndex(currentTimedPositionWithSpeed.getPosition())); List<Position>
         * currentPredecessors = backGraph.get(currentPosition); Long minTime = Long.MAX_VALUE; for(Position p :
         * currentPredecessors) { if(tentativeDistances.get(p) < minTime) { minTime = tentativeDistances.get(p);
         * currentPosition = p; currentTime = new MillisecondsTimePoint(minTime); } } } //I need to add the first point
         * to the path lst.addFirst(new TimedPositionWithSpeedImpl(startTime, start, null));
         */
        while ((currentPosition != null)&&(!this.isTimedOut())) {
            currentTime = new MillisecondsTimePoint(tentativeDistances.get(currentPosition).getA());
            SpeedWithBearing windAtPoint = windField.getWind(new TimedPositionImpl(currentTime, currentPosition));
            TimedPositionWithSpeed current = new TimedPositionWithSpeedImpl(currentTime, currentPosition, windAtPoint);
            lst.addFirst(current);
            currentPosition = tentativeDistances.get(currentPosition).getB();
        }

        return new PathImpl(lst, windField, this.algorithmTimedOut);

    }

}
