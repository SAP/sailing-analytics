package com.sap.sailing.domain.common.orc;

import java.io.Serializable;
import java.util.Map;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.CountryCode;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.WithID;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.NauticalMileDistance;

/**
 * Represents semantically a real ORC certificate for a {@link Competitor}, which is used to rate different type of
 * boats for different inshore and offshore race conditions.
 * <p>
 * An ORC certificate is issued by the "Member National Authorities" of World Sailing and are available for insight at
 * https://www.orc.org/index.asp . Other information about the whole scoring system and different variants are available
 * too.
 * <p>
 * One implementing class provides all necessary functionalities to score the Competitors with a choosen
 * {@link RankingMetric}.
 * <p>
 * 
 * The {@link WithID} interface is to be implemented based on the {@link #getReferenceNumber()} which is unique across the
 * ORC world-wide certificate database. Note that the {@link #getBoatKey()} consisting of the {@link #getFileId()} and the
 * {@link #getIssuingCountry()} field values is a unique key for the boat to which the certificate belongs.
 * <p>
 * 
 * The certificate contains time allowances (or, conversely, speed predictions) for a combination of true wind speeds
 * (TWS) and true wind angles (TWA). There are defaults for the values used for the true wind speeds and the true wind
 * angles, forming a regular, fully populated matrix of allowances / speed predictions. These defaults can be found in
 * {@link #ALLOWANCES_TRUE_WIND_SPEEDS} and {@link #ALLOWANCES_TRUE_WIND_SPEEDS}.
 * <p>
 * 
 * Certificates may also specify non-default values for the slots or "bins" of the matrix that describes the allowances
 * / speed predictions. As for the default values, the allowances matrix has to be fully populated. The values in the
 * TWS and TWA arrays are expected to be increasing monotonously.
 * 
 * @author Daniel Lisunkin (i505543)
 *
 */
public interface ORCCertificate extends WithID, Serializable {
    /**
     * Equals the column heading of the allowances table of an ORC certificate. The speeds are set by the offshore
     * racing congress. The speeds occur in the array in ascending order.
     * 
     * There are references in the persistance module. If the values change, there will be an adjustment needed
     * in {@link MongoObjectFactoryImpl.speedToKnotsString}.
     */
    Speed[] ALLOWANCES_TRUE_WIND_SPEEDS = { new KnotSpeedImpl(6), new KnotSpeedImpl(8),
                new KnotSpeedImpl(10), new KnotSpeedImpl(12), new KnotSpeedImpl(14), new KnotSpeedImpl(16),
                new KnotSpeedImpl(20) };
    /**
     * Equals the line heading of the allowances table of an ORC certificate. The true wind angles are set by the
     * offshore racing congress. The angles occur in the array in ascending order.
     * 
     * There are references in the persistance module. If the values change, there will be an adjustment needed
     * in {@link MongoObjectFactoryImpl.bearingToDegreeString}.
     */
    Bearing[] ALLOWANCES_TRUE_WIND_ANGLES = { new DegreeBearingImpl(52), new DegreeBearingImpl(60),
                new DegreeBearingImpl(75), new DegreeBearingImpl(90), new DegreeBearingImpl(110),
                new DegreeBearingImpl(120), new DegreeBearingImpl(135), new DegreeBearingImpl(150) };
    
    Distance NAUTICAL_MILE = new NauticalMileDistance(1);

    /**
     * We use the {@link #getReferenceNumber() reference number ("RefNo")} as the ID of a certificate document.
     * It is unique for all of ORC's certificates issued world-wide.
     */
    @Override
    String getId();

    String getReferenceNumber();
    
    /**
     * Technically, at ORC a certificate's boat is identified by a "file ID" as returned by this method, together
     * with the {@link #getIssuingCountry() issuing country} which together makes this a unique boat key. For each
     * such boat key there is at most one valid certificate at a time.
     */
    String getFileId();
    
    /**
     * The country whose authority issued this certificate. Together with the {@link #getFileId() file ID} this makes for a
     * unique key of the boat to which this certifiate belongs.
     */
    CountryCode getIssuingCountry();
    
    /**
     * Returns the sailnumber of the {@link Competitor} which this certificate belongs to.
     * 
     * @return sailnumber as a string, which consists out of some alphanumeric characters (most time the nation or
     *         boatclass id), a blank space and some numerical digits
     */
    String getSailNumber();
    
    /**
     * Returns the boatclass of the {@link Competitor} which this certificate belongs to.
     */
    String getBoatClassName();
    
    Duration getGPH();

    /**
     * Returns the GPH value for the {@link Competitor}. The GPH value represents the overall performance of the boat.
     * The value itself is again an allowance (in seconds per nautical mile) and could be used as a ToD Factor.
     * Most of the times it is used to divide a big fleet into similar fast divisions.
     */
    double getGPHInSecondsToTheMile();
    
    /**
     * Returns the CDL (Class Division Length) value for the {@link Competitor}. This value is another (and newer) approach to rate the overall performance of different boats.
     * The different division intervals are - in contrast to the intervals of the GPH - set by the Offshore Race Committee and not by the national association for a uniform handling.
     * The higher the value, the higher the overall performance, it is measured in meters.
     */
    Double getCDL();
    
    /**
     * Returns the LOA (length over all) of the {@link Competitor} boat which this certificate belongs to.
     */
    Distance getLengthOverAll();
    
    /**
     * Returns the TimePoint when the certificate was issued by the national association. 
     */
    TimePoint getIssueDate();
    
    /**
     * Returns a Map of speed predictions (in knots) for different wind speeds to use for a {@link ORCPerformanceCurve} rating
     * in a race, where the upwind and the downwind part both conrtibute 50% to the total course.
     * 
     * @return Map with elements of type wind {@link Speed} as keys and {@link Duration}s equaling a time allowance per
     *         nautical mile as values.
     */
    Map<Speed, Speed> getWindwardLeewardSpeedPrediction();
    
    /**
     * Returns a Map of speed predictions (in knots) for different wind speeds to use for a {@link ORCPerformanceCurve} rating
     * in a race, where the course resembles a circular figure.
     * 
     * @return Map with elements of type wind {@link Speed} as keys and {@link Duration}s equaling a time allowance per
     *         nautical mile as values.
     */
    Map<Speed, Speed> getCircularRandomSpeedPredictions();
    
    /**
     * Returns a Map of speed predictions (in knots) for different wind speeds to use for a {@link ORCPerformanceCurve} rating
     * in a race, where the race is a long distance offshore/coastal race.
     * 
     * @return Map with elements of type wind {@link Speed} as keys and {@link Duration}s equaling a time allowance per
     *         nautical mile as values.
     */
    Map<Speed, Speed> getLongDistanceSpeedPredictions();
    
    /**
     * Returns a Map of speed predictions (in knots)  for different wind speeds to use for a {@link ORCPerformanceCurve} rating
     * in a race, where the competitor doesn't use any spinnaker.
     * 
     * @return Map with elements of type wind {@link Speed} as keys and {@link Duration}s equaling a time allowance per
     *         nautical mile as values.
     */
    Map<Speed, Speed> getNonSpinnakerSpeedPredictions();
    
    Map<Speed, Bearing> getBeatAngles();
    
    Map<Speed, Bearing> getRunAngles();
    
    Map<Speed, Duration> getBeatAllowances();
    
    Map<Speed, Duration> getRunAllowances();
    
    Map<Speed, Speed> getBeatVMGPredictions();
    
    Map<Speed, Speed> getRunVMGPredictions();
    
    Map<Speed, Map<Bearing, Speed>> getVelocityPredictionPerTrueWindSpeedAndAngle();

    String getBoatName();
    
    /**
     * @return the true wind speeds used for the matrix returned by
     *         {@link #getVelocityPredictionPerTrueWindSpeedAndAngle()}. The {@link Speed} values returned by this
     *         method form the key set of the map returned by {@link #getVelocityPredictionPerTrueWindSpeedAndAngle()}.
     */
    Speed[] getTrueWindSpeeds();
    
    /**
     * @return the true wind angles used for the matrix returned by
     *         {@link #getVelocityPredictionPerTrueWindSpeedAndAngle()}. The {@link Bearing} values returned by this
     *         method form the key set of all the maps returned as values in the map returned by
     *         {@link #getVelocityPredictionPerTrueWindSpeedAndAngle()}.
     */
    Bearing[] getTrueWindAngles();
}