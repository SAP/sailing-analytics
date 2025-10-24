package com.sap.sailing.domain.orc;

import org.apache.commons.math.ArgumentOutsideDomainException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveCourse;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;

/**
 * For a {@link Competitor} and the {@link ORCPerformanceCurveCourse} which the competitor sailed until the creation of
 * an instance of an implementing class, this instance represents a so called "Polar Curve". This Curve is
 * specified by the so called "Implied Wind" a {@link Speed} on the x-Axis and the allowance in s/NM respectively a
 * {@link Duration} on the y-Axis. It represents a simplified polar curve for the given boat and the given part of the
 * course. For a given windspeed the performance curve returns the allowance for the boat or in simpler words: how long
 * should the boat need for a nautical mile when sailing 100% performance.
 * 
 * @author Daniel Lisunkin (i505543)
 *
 */
public interface ORCPerformanceCurve {

    /**
     * The implied wind is a size defined by ORC. It represents the average wind the {@link Competitor} had to complete
     * the course in the given time. The higher the overall performance of the Competitor, the higher the value. If the
     * Competitor has the highest value, he is defined as the winner of the race.
     * 
     * @param durationToCompleteCourse
     *            equals the duration the boat needed to conquer the {@link #getCourse() course}
     * 
     * @throws ArgumentOutsideDomainException
     * @throws MaxIterationsExceededException
     * @throws FunctionEvaluationException
     */
    Speed getImpliedWind(Duration durationToCompleteCourse) throws MaxIterationsExceededException, FunctionEvaluationException;
    
    /**
     * When calculating the performance of a {@link Competitor} by the usage of a performance curve, there is no classic
     * calculated time. The calculated time can only be provided in relation to another boat, because the implied wind
     * of this boat is mapped on the performance curve of the reference boat. To create meaningful data for a
     * leaderboard all competitors should compare their performance to the same reference boat. This reference boat is
     * often called "Scratchboat" and is often the fastest boat (by GPH) in the field.
     * 
     * @param referenceBoat
     *            the "scratch boat" (in ORC terms) against whose performance curve to apply the implied wind calculated
     *            with {@code this} performance curve for the duration sailed ({@code durationToCompleteCourse}). This
     *            "converts" this performance curve's implied wind to a "calculated time" that is in the "time system"
     *            or "terms" of the {@code referenceBoat}.
     */
    Duration getCalculatedTime(ORCPerformanceCurve referenceBoat, Duration durationToCompleteCourse) throws MaxIterationsExceededException, FunctionEvaluationException;
 
    /**
     * The duration that the boat represented by this performance curve is predicted to take to sail the
     * {@link #getCourse() course} at the given {@code trueWindSpeed}.
     */
    Duration getAllowancePerCourse(Speed trueWindSpeed) throws ArgumentOutsideDomainException;
    
    /**
     * @return the {@link ORCPerformanceCurve}, which is the sailed part of the whole course this PerformanceCurve was
     *         created for
     */
    ORCPerformanceCurveCourse getCourse();
}
