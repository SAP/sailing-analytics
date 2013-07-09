package com.sap.sailing.simulator.impl;

import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.simulator.TimedPosition;

public class PathCandidate implements Comparable<PathCandidate> {

    public PathCandidate(TimedPosition pos, double vrt, double hrz, int trn, String path, char sid, Wind wind) {
        this.pos = pos;   // time and position
        this.vrt = vrt;   // height of target projected onto wind
        this.hrz = hrz;   // distance from middle line
        this.trn = trn;   // number of turns
        this.path = path; // path as sequence of steps from start to pos
        this.sid = sid;   // side of wind of step reaching pos
    }

    TimedPosition pos;
    double vrt;
    double hrz;
    int trn;
    String path;
    char sid;

    @Override
    // sort descending by length, width, height
    public int compareTo(PathCandidate other) {
        if (this.path.length() == other.path.length()) {
            if (this.vrt == other.vrt) {
                if (Math.abs(this.hrz) == Math.abs(other.hrz)) {
                    return 0;
                } else {
                    return (Math.abs(this.hrz) < Math.abs(other.hrz) ? -1 : +1);
                }
            } else {
                return (this.vrt > other.vrt ? -1 : +1);
            }
        } else {
            return (this.path.length() < other.path.length() ? -1 : +1);            
        }
    }

}
