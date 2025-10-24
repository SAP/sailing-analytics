package com.sap.sailing.domain.common.orc.impl;

import java.util.Collections;
import java.util.Map;

import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.CountryCode;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

/**
 * Holds an ORC boat certificate including various of the metrics found in it. Note that multiple different certificates
 * for the same boat may exist, and they can be distinguished in particular using their {@link #getIssueDate() issuing
 * date}.
 * 
 * @author Daniel Lisunkin (i505543)
 *
 */

public class ORCCertificateImpl implements ORCCertificate {
    private static final long serialVersionUID = 8725162998514202782L;
    
    private final String referenceNumber;

    private final String sailNumber;
    private final String boatName;
    private final String boatClassName;
    private final Distance lengthOverAll;
    private final Duration gph;
    private final Double cdl;
    private final TimePoint issueDate;
    private final String fileId;
    private final CountryCode issuingCountry;
    
    /**
     * The "core" of the certificate for polar curve scoring; the values in the map tell how fast the boat
     * described by this certificate is expected to sail for the true wind speed (TWS) provided by the key, and the true
     * wind angle (TWA) provided by the value map's key. This is the reciprocal of the duration it takes the boat to
     * sail a certain distance.
     * <p>
     * 
     * The keys are the speeds as provided by {@link #allowancesTrueWindSpeeds}; the keys of the value maps are the
     * angles as provided by {@link #allowancesTrueWindAngles}.
     */
    private final Map<Speed, Map<Bearing, Speed>> velocityPredictionPerTrueWindSpeedAndAngle;

    /**
     * The beat angles for the true wind speeds; key set is equal to that of
     * {@link #velocityPredictionPerTrueWindSpeedAndAngle}.
     */
    private final Map<Speed, Bearing> beatAngles;
    
    /**
     * The VMG the boat represented by this certificate sails at when sailing upwind, meaning
     * {@code Math.abs(TWA) <= beatAngles.get(TWS)}.
     */
    private final Map<Speed, Speed> beatVMGPredictionPerTrueWindSpeed;

    /**
     * The duration in seconds per nautical mile the boat represented by this certificate is allowed to sail at 100%
     * performance and a given windspeed, when sailing upwind. Key set is equal to that of
     * {@link #velocityPredictionPerTrueWindSpeedAndAngle}.
     */
    private final Map<Speed, Duration> beatAllowancePerTrueWindSpeed;

    /**
     * The run angles for the true wind speeds; note that the gybing angle is then {@code 180-2*runAngle}. The key set
     * is equal to that of {@link #durationPerNauticalMileAtTrueWindAngleAndSpeed}. In the certificates this is referred
     * to as "gybe angles" but this seems inconsistent as it really describes the TWAs sailed at on a downwind leg, not
     * the gybe angles.
     */
    private final Map<Speed, Bearing> runAngles;

    /**
     * The VMG the boat represented by this certificate sails at when sailing upwind, meaning
     * {@code Math.abs(TWA) <= runAngles.get(TWS)}.
     */
    private final Map<Speed, Speed> runVMGPredictionPerTrueWindSpeed;
    
    /**
     * The duration in seconds per nautical mile the boat represented by this certificate is allowed to sail at 100%
     * performance and a given windspeed, when sailing downwind. Key set is equal to that of
     * {@link #velocityPredictionPerTrueWindSpeedAndAngle}.
     */
    private final Map<Speed, Duration> runAllowancePerTrueWindSpeed;
    
    private final Map<Speed, Speed> windwardLeewardSpeedPredictionPerTrueWindSpeed;
    
    private final Map<Speed, Speed> longDistanceSpeedPredictionPerTrueWindSpeed;
    
    private final Map<Speed, Speed> circularRandomSpeedPredictionPerTrueWindSpeed;
    
    private final Map<Speed, Speed> nonSpinnakerSpeedPredictionPerTrueWindSpeed;
    private final Speed[] allowancesTrueWindSpeeds;
    private final Bearing[] allowancesTrueWindAngles;
    
    /**
     * Can be used to specify non-default TWS/TWA values for the time allowances / speed predictions.
     * 
     * @param allowancesTrueWindSpeeds
     *            a monotonously-increasing sequence of true wind speed (TWS) values for the matrix of time allowances /
     *            velocity predictions for this certificate. See also
     *            {@link ORCCertificate#ALLOWANCES_TRUE_WIND_SPEEDS}.
     * @param allowancesTrueWindAngles
     *            a monotonously-increasing sequence of true wind angle (TWA) values for the matrix of time allowances /
     *            velocity predictions for this certificate. See also
     *            {@link ORCCertificate#ALLOWANCES_TRUE_WIND_ANGLES}.
     */
    public ORCCertificateImpl(Speed[] allowancesTrueWindSpeeds, Bearing[] allowancesTrueWindAngles,
            String referenceNumber, String fileId, String sailnumber, String boatName,
            String boatClassName, Distance length, Duration gph, Double cdl,
            TimePoint issueDate, CountryCode issuingCountry,
            Map<Speed, Map<Bearing, Speed>> velocityPredictionsPerTrueWindSpeedAndAngle, Map<Speed, Bearing> beatAngles,
            Map<Speed, Speed> beatVMGPredictionPerTrueWindSpeed, Map<Speed, Duration> beatAllowancePerTrueWindSpeed,
            Map<Speed, Bearing> runAngles,
            Map<Speed, Speed> runVMGPredictionPerTrueWindSpeed,
            Map<Speed, Duration> runAllowancePerTrueWindSpeed,
            Map<Speed, Speed> windwardLeewardSpeedPredictionsPerTrueWindSpeed,
            Map<Speed, Speed> longDistanceSpeedPredictionsPerTrueWindSpeed,
            Map<Speed, Speed> circularRandomSpeedPredictionsPerTrueWindSpeed,
            Map<Speed, Speed> nonSpinnakerSpeedPredictionsPerTrueWindSpeed) {
        this.allowancesTrueWindAngles = allowancesTrueWindAngles;
        this.allowancesTrueWindSpeeds = allowancesTrueWindSpeeds;
        this.referenceNumber = referenceNumber;
        this.fileId = fileId;
        this.issuingCountry = issuingCountry;
        this.sailNumber = sailnumber;
        this.boatName = boatName;
        this.boatClassName = boatClassName;
        this.lengthOverAll = length;
        this.gph = gph;
        this.cdl = cdl;
        this.issueDate = issueDate;
        this.velocityPredictionPerTrueWindSpeedAndAngle = Collections
                .unmodifiableMap(velocityPredictionsPerTrueWindSpeedAndAngle);
        this.beatAngles = Collections.unmodifiableMap(beatAngles);
        this.beatVMGPredictionPerTrueWindSpeed = Collections.unmodifiableMap(beatVMGPredictionPerTrueWindSpeed);
        this.beatAllowancePerTrueWindSpeed = Collections.unmodifiableMap(beatAllowancePerTrueWindSpeed);
        this.runAngles = Collections.unmodifiableMap(runAngles);
        this.runVMGPredictionPerTrueWindSpeed = Collections.unmodifiableMap(runVMGPredictionPerTrueWindSpeed);
        this.runAllowancePerTrueWindSpeed = Collections.unmodifiableMap(runAllowancePerTrueWindSpeed);
        this.windwardLeewardSpeedPredictionPerTrueWindSpeed = Collections.unmodifiableMap(windwardLeewardSpeedPredictionsPerTrueWindSpeed);
        this.longDistanceSpeedPredictionPerTrueWindSpeed = Collections.unmodifiableMap(longDistanceSpeedPredictionsPerTrueWindSpeed);
        this.circularRandomSpeedPredictionPerTrueWindSpeed = Collections.unmodifiableMap(circularRandomSpeedPredictionsPerTrueWindSpeed);
        this.nonSpinnakerSpeedPredictionPerTrueWindSpeed = Collections.unmodifiableMap(nonSpinnakerSpeedPredictionsPerTrueWindSpeed);
    }
    
    /**
     * Uses the default TWS/TWA values from {@link ORCCertificate#ALLOWANCES_TRUE_WIND_SPEEDS} and
     * {@link ORCCertificate#ALLOWANCES_TRUE_WIND_ANGLES}, respectively, for the matrix of time allowances
     * or, conversely, the speed predictions.
     */
    public ORCCertificateImpl(String referenceNumber,
            String fileId, String sailnumber, String boatName,
            String boatClassName, Distance length,
            Duration gph, Double cdl,
            TimePoint issueDate, 
            CountryCode issuingCountry, Map<Speed, Map<Bearing, Speed>> velocityPredictionsPerTrueWindSpeedAndAngle,
            Map<Speed, Bearing> beatAngles, Map<Speed, Speed> beatVMGPredictionPerTrueWindSpeed,
            Map<Speed, Duration> beatAllowancePerTrueWindSpeed,
            Map<Speed, Bearing> runAngles,
            Map<Speed, Speed> runVMGPredictionPerTrueWindSpeed,
            Map<Speed, Duration> runAllowancePerTrueWindSpeed,
            Map<Speed, Speed> windwardLeewardSpeedPredictionsPerTrueWindSpeed,
            Map<Speed, Speed> longDistanceSpeedPredictionsPerTrueWindSpeed,
            Map<Speed, Speed> circularRandomSpeedPredictionsPerTrueWindSpeed,
            Map<Speed, Speed> nonSpinnakerSpeedPredictionsPerTrueWindSpeed) {
        this(ALLOWANCES_TRUE_WIND_SPEEDS, ALLOWANCES_TRUE_WIND_ANGLES, referenceNumber, fileId, sailnumber, boatName,
                boatClassName, length, gph, cdl, issueDate, issuingCountry, velocityPredictionsPerTrueWindSpeedAndAngle,
                beatAngles, beatVMGPredictionPerTrueWindSpeed, beatAllowancePerTrueWindSpeed, runAngles,
                runVMGPredictionPerTrueWindSpeed, runAllowancePerTrueWindSpeed,
                windwardLeewardSpeedPredictionsPerTrueWindSpeed, longDistanceSpeedPredictionsPerTrueWindSpeed,
                circularRandomSpeedPredictionsPerTrueWindSpeed, nonSpinnakerSpeedPredictionsPerTrueWindSpeed);
    }

    @Override
    public String getId() {
        return getReferenceNumber();
    }
    
    @Override
    public String getReferenceNumber() {
        return referenceNumber;
    }

    @Override
    public String getFileId() {
        return fileId;
    }

    @Override
    public CountryCode getIssuingCountry() {
        return issuingCountry;
    }

    @Override
    public String getBoatName() {
        return boatName;
    }

    @Override
    public Duration getGPH() {
        return gph;
    }
    
    @Override
    public double getGPHInSecondsToTheMile() {
        return gph==null ? 0 : gph.asSeconds();
    }

    // Please do not try to calculate these combined allowances and instead use the provided values from the certificate.
    // The values from the certificate are calculated with more accurate base data (via the velocity prediction program)
    // and rounded afterwards. Those values aren't provided to the public, so there is no possibility to get the same
    // level of accuracy.
    @Override
    public Map<Speed, Speed> getWindwardLeewardSpeedPrediction() {
        return windwardLeewardSpeedPredictionPerTrueWindSpeed;
    }

    @Override
    public Map<Speed, Speed> getCircularRandomSpeedPredictions() {
        return circularRandomSpeedPredictionPerTrueWindSpeed;
    }

    @Override
    public Map<Speed, Speed> getLongDistanceSpeedPredictions() {
        return longDistanceSpeedPredictionPerTrueWindSpeed;
    }

    @Override
    public Map<Speed, Speed> getNonSpinnakerSpeedPredictions() {
        return nonSpinnakerSpeedPredictionPerTrueWindSpeed;
    }

    @Override
    public String getSailNumber() {
        return sailNumber;
    }

    @Override
    public String getBoatClassName() {
        return boatClassName;
    }

    @Override
    public Distance getLengthOverAll() {
        return lengthOverAll;
    }

    @Override
    public Double getCDL() {
        return cdl;
    }

    @Override
    public Map<Speed, Bearing> getBeatAngles() {
        return beatAngles;
    }

    @Override
    public Map<Speed, Bearing> getRunAngles() {
        return runAngles;
    }

    @Override
    public Map<Speed, Duration> getBeatAllowances() {
        return beatAllowancePerTrueWindSpeed;
    }

    @Override
    public Map<Speed, Duration> getRunAllowances() {
        return runAllowancePerTrueWindSpeed;
    }

    @Override
    public Map<Speed, Map<Bearing, Speed>> getVelocityPredictionPerTrueWindSpeedAndAngle() {
        return velocityPredictionPerTrueWindSpeedAndAngle;
    }

    @Override
    public Map<Speed, Speed> getBeatVMGPredictions() {
        return beatVMGPredictionPerTrueWindSpeed;
    }

    @Override
    public Map<Speed, Speed> getRunVMGPredictions() {
        return runVMGPredictionPerTrueWindSpeed;
    }
    
    @Override
    public TimePoint getIssueDate() {
        return issueDate;
    }
    
    @Override
    public String toString() {
        return "Certificate with ID \""+getId()+"\" for "+getSailNumber()+" / "+getBoatName() + " - Issued on: " + (getIssueDate()==null?"n/a":getIssueDate().asDate())+" with GPH "+
                Util.padPositiveValue(getGPHInSecondsToTheMile(), 1, 1, /* round */ true);
    }

    @Override
    public Speed[] getTrueWindSpeeds() {
        return allowancesTrueWindSpeeds;
    }

    @Override
    public Bearing[] getTrueWindAngles() {
        return allowancesTrueWindAngles;
    }
}
