package com.sap.sailing.gwt.ui.shared;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.Position;

public class WindLinesDTO implements IsSerializable {
    
    private Map<Position, SortedMap<Long, List<Position>>> windLinesMap;

    public Map<Position, SortedMap<Long, List<Position>>> getWindLinesMap() {
        return windLinesMap;
    }

    public void setWindLinesMap(Map<Position, SortedMap<Long, List<Position>>> windLinesMap) {
        this.windLinesMap = windLinesMap;
    }
    
    public void addWindLine(Position position, Long timePoint, List<Position> windLine) {
        if (windLinesMap == null) {
            windLinesMap = new HashMap<Position, SortedMap<Long, List<Position>>>();
        }
        if (!windLinesMap.containsKey(position)) {
            windLinesMap.put(position, new TreeMap<Long, List<Position>>());
        }
        windLinesMap.get(position).put(timePoint, windLine);
    }
    
    public List<Position> getWindLine(Position position, Long timePoint) {
        if (windLinesMap != null) {
            if (windLinesMap.get(position) != null) {
                return windLinesMap.get(position).get(timePoint);
            }
        }
        return null;
    }
}
