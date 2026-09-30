package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.impl.ManeuverCurveBoundariesImpl;
import com.sap.sailing.server.gateway.serialization.impl.ManeuverCurveBoundariesJsonSerializer;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ManeuverCurveBoundariesJsonDeserializer implements JsonDeserializer<ManeuverCurveBoundaries> {

    @Override
    public ManeuverCurveBoundaries deserialize(JSONObject object) throws JsonDeserializationException {
        Long timePointBeforeMillis = (Long) object.get(ManeuverCurveBoundariesJsonSerializer.TIME_POINT_BEFORE);
        Double speedBeforeInKnots = (Double) object.get(ManeuverCurveBoundariesJsonSerializer.SPEED_BEFORE_IN_KNOTS);
        Double cogBefore = (Double) object.get(ManeuverCurveBoundariesJsonSerializer.COG_BEFORE_IN_TRUE_DEGREES);
        Long timePointAfterMillis = (Long) object.get(ManeuverCurveBoundariesJsonSerializer.TIME_POINT_AFTER);
        Double speedAfterInKnots = (Double) object.get(ManeuverCurveBoundariesJsonSerializer.SPEED_AFTER_IN_KNOTS);
        Double cogAfter = (Double) object.get(ManeuverCurveBoundariesJsonSerializer.COG_AFTER_IN_TRUE_DEGREES);
        Double directionChangeInDegrees = (Double) object
                .get(ManeuverCurveBoundariesJsonSerializer.DIRECTION_CHANGE_IN_DEGREES);
        Double lowestSpeedInKnots = (Double) object.get(ManeuverCurveBoundariesJsonSerializer.LOWEST_SPEED_IN_KNOTS);
        Double highestSpeedInKnots = (Double) object.get(ManeuverCurveBoundariesJsonSerializer.HIGHEST_SPEED_IN_KNOTS);
        return new ManeuverCurveBoundariesImpl(new MillisecondsTimePoint(timePointBeforeMillis),
                new MillisecondsTimePoint(timePointAfterMillis),
                new KnotSpeedWithBearingImpl(speedBeforeInKnots, new DegreeBearingImpl(cogBefore)),
                new KnotSpeedWithBearingImpl(speedAfterInKnots, new DegreeBearingImpl(cogAfter)),
                directionChangeInDegrees, new KnotSpeedImpl(lowestSpeedInKnots),
                //TODO remove null check after master merge of this branch
                highestSpeedInKnots == null ? null : new KnotSpeedImpl(highestSpeedInKnots));
    }

}
