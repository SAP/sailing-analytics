package com.sap.sailing.simulator.impl;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.simulator.TimedPosition;
import com.sap.sse.common.Position;

public class PathCandidate implements Comparable<PathCandidate> {

    public PathCandidate(TimedPosition pos, boolean reached, double vrt, double hrz, int trn, String path, char sid, Wind wind, Position start) {
        this.pos = pos;   // time and position
        this.wind = wind;
        this.reached = reached;
        this.vrt = vrt;   // height of target projected onto wind
        this.hrz = hrz;   // distance from middle line
        this.trn = trn;   // number of turns
        this.path = path; // path as sequence of steps from start to pos
        this.sid = sid;   // side of wind of step reaching pos
        this.start = start;
    }

    TimedPosition pos;
    Wind wind;
    boolean reached;
    double vrt;
    double hrz;
    int trn;
    String path;
    char sid;
    Position start;

    public int getIndexOfTurnLR() {
        String tmpPath = path;
        tmpPath = tmpPath.toUpperCase();
        tmpPath = tmpPath.replace('D', 'L');
        tmpPath = tmpPath.replace('E', 'R');
    	return tmpPath.indexOf("LR");
    }

    public int getIndexOfTurnRL() {
        String tmpPath = path;
        tmpPath = tmpPath.toUpperCase();
        tmpPath = tmpPath.replace('D', 'L');
        tmpPath = tmpPath.replace('E', 'R');
    	return tmpPath.indexOf("RL");
    }

    @Override
    // sort descending by time, -#turns
    public int compareTo(PathCandidate other) {
            if (Math.abs(this.pos.getTimePoint().asMillis() - other.pos.getTimePoint().asMillis()) <= 1000) {
                if (this.trn == other.trn) {
                    return 0;
                } else {
                    return (this.trn < other.trn ? -1 : +1);
                }
            } else {
                return (this.pos.getTimePoint().asMillis() < other.pos.getTimePoint().asMillis() ? -1 : +1);
            }
    }

}
