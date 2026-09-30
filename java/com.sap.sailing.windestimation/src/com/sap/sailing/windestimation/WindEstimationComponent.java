package com.sap.sailing.windestimation;

import java.util.List;

import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

/**
 * Stand-alone wind estimation which is capable of delivering a list of wind fixes by processing the given input.
 * 
 * @author Vladislav Chumak (D069712)
 *
 * @param <InputType>
 *            The type of the input, from which this wind estimation implementation infers the desired wind track.
 */
public interface WindEstimationComponent<InputType> {

    /**
     * Estimates a list of wind fixes each with assigned confidence by analyzing the provided input.
     * 
     * @param input
     *            The input, from which this wind estimation implementation infers the desired wind track
     * @return A wind track composed of a list of wind fixes, where each wind fix is assigned to a time point and
     *         position, and contains its estimation confidence.
     */
    List<WindWithConfidence<Pair<Position, TimePoint>>> estimateWindTrack(InputType input);

}
