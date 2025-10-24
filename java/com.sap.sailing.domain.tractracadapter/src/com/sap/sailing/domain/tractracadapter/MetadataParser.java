package com.sap.sailing.domain.tractracadapter;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.tractracadapter.impl.RaceCourseReceiver;
import com.sap.sse.common.Color;
import com.sap.sse.common.Named;
import com.tractrac.model.lib.api.event.IRaceCompetitor;
import com.tractrac.model.lib.api.map.IMapItem;
import com.tractrac.model.lib.api.map.IPositionedItem;

/**
 * TracTrac objects can be augmented by what TracTrac calls a "DataSheet." These optional data sheets can provide
 * meta data about the object to which they belong. For example, a course's meta data can provide the passing
 * sides for each waypoint in the list. The race can provide a definition of side lines in its meta data. And a
 * control point can define its shape and color.<p>
 * 
 * This class encapsulates the patterns and procedures by which the additional data is extracted from the
 * respective TracTrac data. It can be used for both, clientparams.php or TTCM based data.
 * 
 * @see ClientParamsPHP
 * @see RaceCourseReceiver
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface MetadataParser {
    public interface ControlPointMetaData extends Named {
        MarkType getType();
        Color getColor();
        String getShape();
        String getPattern();
        Serializable getId();
    }

    public interface BoatMetaData extends Named {
        String getId();
        String getColor();
        UUID getUuid();
    }

    Map<Integer, PassingInstruction> parsePassingInstructionData(String routeMetadataString, int numberOfWaypoints);

    /**
     * Returns as many metadata objects as there are marks in the control point (two for a gate, one otherwise)
     */
    ControlPointMetaData parseControlPointMetadata(IPositionedItem positionedItem);

    /**
     * Parses the race metadata for sideline information
     * The sidelines of a race (course) are encoded like this:
     * <pre>
     *  SIDELINE1=(TR-A) 3
     *  SIDELINE2=(TR-A) Start
     * </pre>
     * Each sideline is defined right now through a simple gate, but this might change in the future.
     * 
     * @return keys are the sideline names, such as "SIDELINE1", values are the control points that form the sideline
     */
    Map<String, Iterable<IPositionedItem>> parseSidelinesFromRaceMetadata(String raceMetadataString,
            Iterable<? extends IMapItem> controlPoints);

    /**
     * Parses the boat name, boad id and the boat color for a competitor (entry) of a race.
     * @param competitor
     * @return the boat metadata or null if no metadata is availalble
     */
    BoatMetaData parseCompetitorBoat(IRaceCompetitor competitor);
}
