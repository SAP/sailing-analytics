package com.sap.sailing.domain.orc.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.common.orc.impl.ORCCertificateImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.CountryCodeFactory;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.SecondsDurationImpl;

/**
 * Represents a file in format {@code .json} which is a simple ASCII file format. The {@code .json} file contains an
 * array of maps, each map representing one measurement certificate. The maps have the specific measurement values
 * accessible with a {@link String} for each boat.
 * <p>
 * 
 * The result of successfully parsing a {@code .json} file is a map keyed by the sailing number, with values being
 * equal-sized maps from the key names to the {@link String} values.
 * 
 * @author Daniel Lisunkin (i505543)
 *
 */
public class ORCCertificatesCollectionJSON extends AbstractORCCertificatesCollection {
    private static final String GYBE_ANGLE = "GybeAngle";
    private static final String BEAT_ANGLE = "BeatAngle";
    private static final String ALLOWANCES = "Allowances";
    private static final String ISSUE_DATE = "IssueDate";
    private static final String CDL2 = "CDL";
    private static final String GPH2 = "GPH";
    private static final String CLASS = "Class";
    private static final String YACHT_NAME = "YachtName";
    private static final String LOA = "LOA";
    private static final String SAIL_NO = "SailNo";
    private static final String BIN = "BIN";
    private static final String NAT_AUTH = "NatAuth";
    private static final String REFERENCE_NUMBER = "RefNo";
    private static final String RUN = "Run";
    private static final String BEAT = "Beat";
    private static final String WINDWARD_LEEWARD = "WL";
    private static final String LONG_DISTANCE = "OC";
    private static final String CIRCULAR_RANDOM = "CR";
    private static final String NON_SPINNAKER = "NS";
    private static final String WIND_SPEEDS = "WindSpeeds";
    private static final String WIND_ANGLES = "WindAngles";
    
    /**
     * Pattern to recognize a true wind angle in the allowances section. Group 1 contains
     * the decimal number that represents the true wind angle in degrees.
     */
    private static final Pattern twaPattern = Pattern.compile("R(\\d+(\\.\\d+)?)");

    /**
     * Keys are canonicalized through {@link #canonicalizeId(String)}).
     */
    private Map<String, JSONObject> certificateJsonObjectsByCertificateId;

    /**
     * Receives an {@link InputStream} from different possible sources (web, local file, ...) and does parse the
     * {@code .json} file.
     */
    public ORCCertificatesCollectionJSON(Iterable<JSONObject> data) {
        this.certificateJsonObjectsByCertificateId = new HashMap<>();
        for (final JSONObject o : data) {
            if (this.certificateJsonObjectsByCertificateId.put(canonicalizeId(getId(o)), o) != null) {
                throw new IllegalArgumentException("Certificate ID in collection not unique: " + getId(o));
            }
        }
    }

    /**
     * Returns an {@link ORCCertificateImpl} object for a given {@link String} key. If there is no map value for the
     * given key, the method returns {@link null}.
     * 
     * @param certificateId
     *            internally, the ID is "canonicalized" by removing all whitespace from it, so, e.g., "FIN11826L11826"
     *            is considered equivalent to "FIN11826 L11826". This helps iron out some subtle differences in how RMS
     *            files, JSON files and XML-based search results represent certificate IDs.
     */
    @Override
    public ORCCertificate getCertificateById(String certificateId) {
        String refNo = null;
        String natAuth = null;
        String fileId = null;
        String boatName = null;
        String boatclass = null;
        Distance length = null;
        Duration gph = null;
        Double cdl = null;
        TimePoint issueDate = null;
        final Map<Speed, Bearing> beatAngles = new HashMap<>();
        final Map<Speed, Bearing> gybeAngles = new HashMap<>();
        final Map<Speed, Map<Bearing, Speed>> velocityPredictionPerTrueWindSpeedAndAngle = new HashMap<>();
        final Map<Bearing, Map<Speed, Duration>> allowanceDurationsPerTrueWindAngleAndSpeed = new HashMap<>();  // per nautical mile
        final Map<String, Map<Speed, Duration>> predefinedAllowanceDurationsPerTrueWindSpeed = new HashMap<>(); // per nautical mile
        final Map<Double, Speed> trueWindSpeedMap = new TreeMap<Double,Speed>(); // Using TreeMap implementation to save the TrueWindSpeed
                                                                           // so that it first sorts the entries as well as avoids duplicates
        final Map<Double, Bearing> trueWindAngleMap = new TreeMap<Double,Bearing>(); // Same like trueWindSpeedMap
        final JSONObject object = certificateJsonObjectsByCertificateId.get(canonicalizeId(certificateId));
        final ORCCertificate result;
        if (object == null) {
            result = null;
        } else {
            String sailNumber = null;
            for (Entry<Object, Object> entry : object.entrySet()) {
                switch ((String) entry.getKey()) {
                case REFERENCE_NUMBER:
                    refNo = entry.getValue() == null ? null : entry.getValue().toString();
                    break;
                case NAT_AUTH:
                    natAuth = entry.getValue() == null ? null : entry.getValue().toString();
                    break;
                case BIN:
                    fileId = entry.getValue() == null ? null : entry.getValue().toString();
                    break;
                case SAIL_NO:
                    sailNumber = entry.getValue() == null ? null : entry.getValue().toString();
                    break;
                case LOA:
                    length = entry.getValue() == null ? null : new MeterDistance(((Number) entry.getValue()).doubleValue());
                    break;
                case YACHT_NAME:
                    boatName = entry.getValue() == null ? null : entry.getValue().toString();
                    break;
                case CLASS:
                    boatclass = (String) entry.getValue();
                    break;
                case GPH2:
                    gph = entry.getValue() == null ? null : new SecondsDurationImpl(((Number) entry.getValue()).doubleValue());
                    break;
                case CDL2:
                    cdl = entry.getValue() == null ? null : ((Number) entry.getValue()).doubleValue();
                    break;
                case ISSUE_DATE:
                    Date date = DatatypeConverter.parseDateTime((String) entry.getValue()).getTime();
                    issueDate = new MillisecondsTimePoint(date);
                    break;
                case ALLOWANCES:
                    final JSONObject allowances = (JSONObject) entry.getValue();
                    // calculating the TWA and TWS before calculating the other dependent values in allowances object
                    final Object windSpeedsObject = allowances.get(WIND_SPEEDS);
                    if (windSpeedsObject != null) {
                        final JSONArray twsArray = (JSONArray) windSpeedsObject;
                        for (final Object twsObject : twsArray) {
                            Double twsValue = ((Number) twsObject).doubleValue();
                            trueWindSpeedMap.put(twsValue, new KnotSpeedImpl(twsValue));
                        }
                    } else {
                        for (Speed speed : ORCCertificate.ALLOWANCES_TRUE_WIND_SPEEDS) {
                            trueWindSpeedMap.put(speed.getKnots(), speed);
                        }
                    }
                    final Double [] trueWindSpeedArray = new Double[trueWindSpeedMap.size()];
                    trueWindSpeedMap.keySet().toArray(trueWindSpeedArray);
                    for (final Object aKey : allowances.keySet()) {
                        String keyString = (String) aKey;
                        final Matcher matcher = twaPattern.matcher(keyString);
                        if (matcher.lookingAt()) {
                            final JSONArray array = (JSONArray) allowances.get(keyString);
                            final Map<Speed, Duration> twsMap = new HashMap<>();
                            for (int i = 0; i < array.size(); i++) {
                                final Double allowanceValue = ((Number) array.get(i)).doubleValue();
                                twsMap.put(trueWindSpeedMap.get(trueWindSpeedArray[i]), new SecondsDurationImpl(allowanceValue));
                            }
                            final Double trueWindAngleValue = Double.parseDouble(matcher.group(1));
                            trueWindAngleMap.put(trueWindAngleValue, new DegreeBearingImpl(trueWindAngleValue));
                            allowanceDurationsPerTrueWindAngleAndSpeed.put(trueWindAngleMap.get(trueWindAngleValue), twsMap);
                        }
                    }
                    // Now read the various allowances, assuming we find values for the TWS "bins" identified above
                    for (final Object aKey : allowances.keySet()) {
                        final JSONArray twaArray = (JSONArray) allowances.get(aKey);
                        if (((String) aKey).equals(BEAT_ANGLE)) {
                            for (int i = 0; i < twaArray.size(); i++) {
                                beatAngles.put(new KnotSpeedImpl(trueWindSpeedArray[i]),
                                        new DegreeBearingImpl(((Number) twaArray.get(i)).doubleValue()));
                            }
                            continue;
                        }
                        if (((String) aKey).equals(GYBE_ANGLE)) {
                            for (int i = 0; i < twaArray.size(); i++) {
                                gybeAngles.put(new KnotSpeedImpl(trueWindSpeedArray[i]),
                                        new DegreeBearingImpl(((Number) twaArray.get(i)).doubleValue()));
                            }
                            continue;
                        }
                        Map<Speed, Duration> twsMap = new HashMap<>();
                        // ignore true wind angles key
                        if (!aKey.equals(WIND_SPEEDS) && !aKey.equals(WIND_ANGLES) &&
                                !twaPattern.matcher((String) aKey).lookingAt()) {
                            for (int i = 0; i < twaArray.size(); i++) {
                                twsMap.put(new KnotSpeedImpl(trueWindSpeedArray[i]),
                                        new SecondsDurationImpl(((Number) twaArray.get(i)).doubleValue()));
                            }
                        }
                        switch ((String) aKey) {
                        case BEAT:
                        case RUN:
                            predefinedAllowanceDurationsPerTrueWindSpeed.put((String) aKey, twsMap);
                            break;
                        default:
                            predefinedAllowanceDurationsPerTrueWindSpeed.put((String) aKey, twsMap);
                            break;
                        }
                    }
                    break;
                default:
                    break;
                }
            }
            trueWindSpeedMap.values().forEach(tws -> velocityPredictionPerTrueWindSpeedAndAngle.put(tws, new HashMap<>()));
            for (final Bearing keyTWA : allowanceDurationsPerTrueWindAngleAndSpeed.keySet()) {
                for (final Speed keyTWS : allowanceDurationsPerTrueWindAngleAndSpeed.get(keyTWA).keySet()) {
                    velocityPredictionPerTrueWindSpeedAndAngle.get(keyTWS).put(keyTWA, ORCCertificate.NAUTICAL_MILE
                            .inTime(allowanceDurationsPerTrueWindAngleAndSpeed.get(keyTWA).get(keyTWS)));
                }
            }
            final Map<Speed, Speed> beatVMGPredictionPerTrueWindSpeed = new HashMap<>();
            final Map<Speed, Duration> beatAllowancePerTrueWindSpeed = new HashMap<>();
            final Map<Speed, Speed> runVMGPredictionPerTrueWindSpeed = new HashMap<>();
            final Map<Speed, Duration> runAllowancePerTrueWindSpeed = new HashMap<>();
            final Map<Speed, Speed> windwardLeewardSpeedPredictionPerTrueWindSpeed = new HashMap<>();
            final Map<Speed, Speed> longDistanceSpeedPredictionPerTrueWindSpeed = new HashMap<>();
            final Map<Speed, Speed> circularRandomSpeedPredictionPerTrueWindSpeed = new HashMap<>();
            final Map<Speed, Speed> nonSpinnakerSpeedPredictionPerTrueWindSpeed = new HashMap<>();
            for (final Speed tws : velocityPredictionPerTrueWindSpeedAndAngle.keySet()) {
                beatVMGPredictionPerTrueWindSpeed.put(tws, getSpeedPredictionFromTimeAllowance(predefinedAllowanceDurationsPerTrueWindSpeed, BEAT, tws));
                beatAllowancePerTrueWindSpeed.put(tws, predefinedAllowanceDurationsPerTrueWindSpeed.get(BEAT).get(tws));
                runVMGPredictionPerTrueWindSpeed.put(tws, getSpeedPredictionFromTimeAllowance(predefinedAllowanceDurationsPerTrueWindSpeed, RUN, tws));
                runAllowancePerTrueWindSpeed.put(tws, predefinedAllowanceDurationsPerTrueWindSpeed.get(RUN).get(tws));
                windwardLeewardSpeedPredictionPerTrueWindSpeed.put(tws, getSpeedPredictionFromTimeAllowance(predefinedAllowanceDurationsPerTrueWindSpeed, WINDWARD_LEEWARD, tws));
                final Speed longDistanceSpeedPrediction = getSpeedPredictionFromTimeAllowance(predefinedAllowanceDurationsPerTrueWindSpeed, LONG_DISTANCE, tws);
                if (longDistanceSpeedPrediction != null) {
                    longDistanceSpeedPredictionPerTrueWindSpeed.put(tws, longDistanceSpeedPrediction);
                }
                final Speed circularRandomSpeedPrediction = getSpeedPredictionFromTimeAllowance(predefinedAllowanceDurationsPerTrueWindSpeed, CIRCULAR_RANDOM, tws);
                if (circularRandomSpeedPrediction != null) {
                    circularRandomSpeedPredictionPerTrueWindSpeed.put(tws, circularRandomSpeedPrediction);
                }
                final Speed nonSpinnakerSpeedPrediction = getSpeedPredictionFromTimeAllowance(predefinedAllowanceDurationsPerTrueWindSpeed, NON_SPINNAKER, tws);
                if (nonSpinnakerSpeedPrediction != null) {
                    nonSpinnakerSpeedPredictionPerTrueWindSpeed.put(tws, nonSpinnakerSpeedPrediction);
                }
            }
            final Bearing[] dynamicAllowancesTrueWindAngles = trueWindAngleMap.values().toArray(new Bearing[trueWindAngleMap.values().size()]);
            final Speed[] dynamicAllowancesTrueWindSpeeds = trueWindSpeedMap.values().toArray(new Speed[trueWindSpeedMap.values().size()]);
            // replace array by the default arrays if they match; this will save the space for the dynamically-parsed arrays
            result = new ORCCertificateImpl(
                    Arrays.equals(ORCCertificate.ALLOWANCES_TRUE_WIND_SPEEDS, dynamicAllowancesTrueWindSpeeds)
                            ? ORCCertificate.ALLOWANCES_TRUE_WIND_SPEEDS
                            : dynamicAllowancesTrueWindSpeeds,
                    Arrays.equals(ORCCertificate.ALLOWANCES_TRUE_WIND_ANGLES, dynamicAllowancesTrueWindAngles)
                            ? ORCCertificate.ALLOWANCES_TRUE_WIND_ANGLES
                            : dynamicAllowancesTrueWindAngles,
                    refNo, fileId, sailNumber, boatName, boatclass, length, gph, cdl,
                    issueDate, CountryCodeFactory.INSTANCE.getFromThreeLetterIOCName(natAuth),
                    velocityPredictionPerTrueWindSpeedAndAngle, beatAngles, beatVMGPredictionPerTrueWindSpeed,
                    beatAllowancePerTrueWindSpeed, gybeAngles, runVMGPredictionPerTrueWindSpeed,
                    runAllowancePerTrueWindSpeed, windwardLeewardSpeedPredictionPerTrueWindSpeed,
                    longDistanceSpeedPredictionPerTrueWindSpeed, circularRandomSpeedPredictionPerTrueWindSpeed,
                    nonSpinnakerSpeedPredictionPerTrueWindSpeed);
        }
        return result;
    }

    private Speed getSpeedPredictionFromTimeAllowance(
            Map<String, Map<Speed, Duration>> predefinedAllowanceDurationsPerTrueWindSpeed, String predictionCategory,
            Speed tws) {
        final Map<Speed, Duration> timeAllowances = predefinedAllowanceDurationsPerTrueWindSpeed.get(predictionCategory);
        final Duration timeAllowance;
        final Speed speedPrediction;
        if (timeAllowances != null && (timeAllowance = timeAllowances.get(tws)) != null) {
            speedPrediction = ORCCertificate.NAUTICAL_MILE.inTime(timeAllowance);
        } else {
            speedPrediction = null;
        }
        return speedPrediction;
    }

    private String getId(JSONObject certificateAsJson) {
        return certificateAsJson.get(REFERENCE_NUMBER).toString();
    }

    @Override
    public Iterable<String> getCertificateIds() {
        return Collections.unmodifiableCollection(certificateJsonObjectsByCertificateId.keySet());
    }
}
