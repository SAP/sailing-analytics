package com.sap.sailing.server.gateway.deserialization.racelog.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.common.orc.impl.ORCCertificateImpl;
import com.sap.sailing.server.gateway.serialization.racelog.impl.ORCCertificateJsonSerializer;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.CountryCode;
import com.sap.sse.common.CountryCodeFactory;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.SecondsDurationImpl;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class ORCCertificateJsonDeserializer implements JsonDeserializer<ORCCertificate> {

    @Override
    public ORCCertificate deserialize(JSONObject json) throws JsonDeserializationException {
        String fileId = (String) json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_FILE_ID);
        CountryCode issuingCountry = CountryCodeFactory.INSTANCE.getFromThreeLetterIOCName(
                ((String) json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_ISSUING_COUNTRY_IOC)));
        String sailnumber = (String) json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_SAILNUMBER);
        String boatName = (String) json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_BOATNAME);
        String boatclass = (String) json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_BOATCLASS);
        final Number lengthOverAll = (Number) json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_LENGTH);
        Distance length = lengthOverAll==null?null:new MeterDistance(lengthOverAll.doubleValue());
        Duration gph = new SecondsDurationImpl(
                ((Number) json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_GPH)).doubleValue());
        final Number cdlAsNumber = (Number) json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_CDL);
        Double cdl = cdlAsNumber == null ? null : cdlAsNumber.doubleValue();
        TimePoint issueDate = new MillisecondsTimePoint((long) json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_ISSUE_DATE));
        Map<Speed, Map<Bearing, Speed>> velocityPredictionsPerTrueWindSpeedAndAngle = new HashMap<>();
        Map<Speed, Bearing> beatAngles = new HashMap<>();
        Map<Speed, Speed> beatVMGPredictionPerTrueWindSpeed = new HashMap<>();
        Map<Speed, Duration> beatAllowancePerTrueWindSpeed = new HashMap<>();
        Map<Speed, Bearing> runAngles = new HashMap<>();
        Map<Speed, Speed> runVMGPredictionPerTrueWindSpeed = new HashMap<>();
        Map<Speed, Duration> runAllowancePerTrueWindSpeed = new HashMap<>();
        Map<Speed, Speed> windwardLeewardSpeedPredictionPerTrueWindSpeed = new HashMap<>();
        Map<Speed, Speed> longDistanceSpeedPredictionPerTrueWindSpeed = new HashMap<>();
        Map<Speed, Speed> circularRandomSpeedPredictionPerTrueWindSpeed = new HashMap<>();
        Map<Speed, Speed> nonSpinnakerSpeedPredictionPerTrueWindSpeed = new HashMap<>();
        Speed[] trueWindSpeeds = convertJsonArrayOfDoublesToArrayOfObjectsOrReturnDefault(json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_TRUE_WIND_SPEEDS_IN_KNOTS),
                ORCCertificate.ALLOWANCES_TRUE_WIND_SPEEDS, speedInKnots->new KnotSpeedImpl(speedInKnots), new Speed[0]);
        Bearing[] trueWindAngles = convertJsonArrayOfDoublesToArrayOfObjectsOrReturnDefault(json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_TRUE_WIND_ANGLES_IN_TRUE_DEGREES),
                ORCCertificate.ALLOWANCES_TRUE_WIND_ANGLES, twaInTrueDegrees->new DegreeBearingImpl(twaInTrueDegrees), new Bearing[0]);
        for (Speed tws : trueWindSpeeds) {
            String twsKey = ORCCertificateJsonSerializer.speedToKnotsString(tws);
            beatAngles.put(tws, new DegreeBearingImpl(
                    ((Number) ((JSONObject) json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_BEAT_ANGLES))
                            .get(twsKey)).doubleValue()));
            addKnotSpeedFromPropertyToMap(beatVMGPredictionPerTrueWindSpeed, tws, twsKey, ORCCertificateJsonSerializer.ORC_CERTIFICATE_BEAT_VMG_PREDICTIONS, json);
            beatAllowancePerTrueWindSpeed.put(tws, new SecondsDurationImpl(
                    ((Number) ((JSONObject) json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_BEAT_ALLOWANCES))
                            .get(twsKey)).doubleValue()));
            runAngles.put(tws, new DegreeBearingImpl(
                    ((Number) ((JSONObject) json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_RUN_ANGLES))
                            .get(twsKey)).doubleValue()));
            addKnotSpeedFromPropertyToMap(runVMGPredictionPerTrueWindSpeed, tws, twsKey, ORCCertificateJsonSerializer.ORC_CERTIFICATE_RUN_VMG_PREDICTIONS, json);
            runAllowancePerTrueWindSpeed.put(tws, new SecondsDurationImpl(
                    ((Number) ((JSONObject) json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_RUN_ALLOWANCES))
                            .get(twsKey)).doubleValue()));
            addKnotSpeedFromPropertyToMap(windwardLeewardSpeedPredictionPerTrueWindSpeed, tws, twsKey, ORCCertificateJsonSerializer.ORC_CERTIFICATE_WINDWARD_LEEWARD_SPEED_PREDICTIONS, json);
            addKnotSpeedFromPropertyToMap(longDistanceSpeedPredictionPerTrueWindSpeed, tws, twsKey, ORCCertificateJsonSerializer.ORC_CERTIFICATE_LONG_DISTANCE_SPEED_PREDICTIONS, json);
            addKnotSpeedFromPropertyToMap(circularRandomSpeedPredictionPerTrueWindSpeed, tws, twsKey, ORCCertificateJsonSerializer.ORC_CERTIFICATE_CIRCULAR_RANDOM_SPEED_PREDICTIONS, json);
            addKnotSpeedFromPropertyToMap(nonSpinnakerSpeedPredictionPerTrueWindSpeed, tws, twsKey, ORCCertificateJsonSerializer.ORC_CERTIFICATE_NON_SPINNAKER_SPEED_PREDICTIONS, json);
            Map<Bearing, Speed> velocityPredictionAtCurrentTrueWindSpeedPerTrueWindAngle = new HashMap<>();
            for (Bearing twa : trueWindAngles) {
                String twaKey = ORCCertificateJsonSerializer.bearingToDegreeString(twa);
                velocityPredictionAtCurrentTrueWindSpeedPerTrueWindAngle.put(twa,
                        new KnotSpeedImpl(((Number) ((JSONObject) ((JSONObject) json
                                .get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_TWA_SPEED_PREDICTIONS)).get(twsKey))
                                        .get(twaKey)).doubleValue()));
            }
            velocityPredictionsPerTrueWindSpeedAndAngle.put(tws,
                    velocityPredictionAtCurrentTrueWindSpeedPerTrueWindAngle);
        }
        final String referenceNumber = json.get(ORCCertificateJsonSerializer.ORC_CERTIFICATE_ID).toString();
        final ORCCertificate certificate = new ORCCertificateImpl(trueWindSpeeds, trueWindAngles, referenceNumber,
                fileId, sailnumber, boatName, boatclass, length, gph, cdl, issueDate, issuingCountry,
                velocityPredictionsPerTrueWindSpeedAndAngle, beatAngles, beatVMGPredictionPerTrueWindSpeed,
                beatAllowancePerTrueWindSpeed, runAngles, runVMGPredictionPerTrueWindSpeed,
                runAllowancePerTrueWindSpeed, windwardLeewardSpeedPredictionPerTrueWindSpeed,
                longDistanceSpeedPredictionPerTrueWindSpeed, circularRandomSpeedPredictionPerTrueWindSpeed,
                nonSpinnakerSpeedPredictionPerTrueWindSpeed);
        return certificate;
    }

    /**
     * The value obtained from the {@code jsonObject} at index {@code jsonPropertyName} may be missing, usually due to an
     * {@link Double#isInfinite() infinite} speed which in turn may have been caused by {@code 0.0} values in an original
     * JSON or RMS file. In this case, we default the speed that we insert into the map to {@link Speed#NULL}.
     */
    private void addKnotSpeedFromPropertyToMap(Map<Speed, Speed> speedPredictionMap, Speed tws, String twsKey,
            String jsonPropertyName, JSONObject jsonObject) {
        final JSONObject speedPredictions = (JSONObject) jsonObject.get(jsonPropertyName);
        final Object speedPredictionObject = speedPredictions.get(twsKey);
        final Number value;
        if (speedPredictionObject instanceof Number) {
            value = (Number) speedPredictionObject;
        } else if (speedPredictionObject instanceof JSONObject) {
            value = Double.POSITIVE_INFINITY;
        } else {
            value = 0.0;
        }
        speedPredictionMap.put(tws, new KnotSpeedImpl(value == null ? Double.POSITIVE_INFINITY : value.doubleValue()));
    }

    private <T> T[] convertJsonArrayOfDoublesToArrayOfObjectsOrReturnDefault(final Object supposedJsonArray,
            final T[] defaults, final Function<Double, T> constructor, T[] array) {
        final T[] result;
        final JSONArray windAnglesJsonArray;
        if (supposedJsonArray == null || !(supposedJsonArray instanceof JSONArray) || (windAnglesJsonArray=(JSONArray) supposedJsonArray).isEmpty()) {
            result = defaults;
        } else {
            final List<T> resultList = new ArrayList<>();
            for (final Object number : windAnglesJsonArray) {
                resultList.add(constructor.apply(number==null ? null : ((Number) number).doubleValue()));
            }
            final T[] tArray = resultList.toArray(array);
            result = tArray;
        }
        return result;
    }
}
