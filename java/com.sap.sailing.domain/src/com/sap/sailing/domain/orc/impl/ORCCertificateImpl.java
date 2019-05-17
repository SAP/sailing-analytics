package com.sap.sailing.domain.orc.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.orc.ORCCertificate;
import com.sap.sailing.domain.orc.ORCPerformanceCurve;
import com.sap.sailing.domain.orc.ORCPerformanceCurveCourse;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * For a {@link Competitor} 
 * https://orc.org/index.asp?id=23
 * 
 * @author Daniel Lisunkin (I505543)
 *
 */

// TODO Improve public API in regards of semantic connection with real ORC Certificate (paper)
// TODO COMMENTS!

public class ORCCertificateImpl implements ORCCertificate {

    /*
     * Equals the column heading of the allowances table of an ORC certificate. The speeds are set by the offshore racing congress.
     */
    public static final Speed[] ALLOWANCES_TRUE_WIND_SPEEDS = {new KnotSpeedImpl( 6),
                                                               new KnotSpeedImpl( 8),
                                                               new KnotSpeedImpl(10),
                                                               new KnotSpeedImpl(12),
                                                               new KnotSpeedImpl(14),
                                                               new KnotSpeedImpl(16),
                                                               new KnotSpeedImpl(20) };
    
    /*
     * 
     */
    public static final Bearing[] ALLOWANCES_TRUE_WIND_ANGLES = {new DegreeBearingImpl( 52),
                                                                 new DegreeBearingImpl( 60),
                                                                 new DegreeBearingImpl( 75),
                                                                 new DegreeBearingImpl( 90),
                                                                 new DegreeBearingImpl(110),
                                                                 new DegreeBearingImpl(120),
                                                                 new DegreeBearingImpl(135),
                                                                 new DegreeBearingImpl(150) };
    
    /*
     * 
     */
    private static final Map<Speed, Map<Bearing, Double>> perCentOfAllowancesForLongDistancePC;
    static {
        Map<Speed, Map<Bearing, Double>> result = new HashMap<>();
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[0], new HashMap<Bearing, Double>() {{
            put(new DegreeBearingImpl(0)      , 0.45);
            put(ALLOWANCES_TRUE_WIND_ANGLES[1], 0.0);
            put(ALLOWANCES_TRUE_WIND_ANGLES[3], 0.0);
            put(ALLOWANCES_TRUE_WIND_ANGLES[5], 0.0);
            put(ALLOWANCES_TRUE_WIND_ANGLES[7], 0.0);
            put(new DegreeBearingImpl(180)    , 0.55);
        }});
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[1], new HashMap<Bearing, Double>() {{
            put(new DegreeBearingImpl(0)      , 0.40);
            put(ALLOWANCES_TRUE_WIND_ANGLES[1], 0.05);
            put(ALLOWANCES_TRUE_WIND_ANGLES[3], 0.05);
            put(ALLOWANCES_TRUE_WIND_ANGLES[5], 0.05);
            put(ALLOWANCES_TRUE_WIND_ANGLES[7], 0.05);
            put(new DegreeBearingImpl(180)    , 0.40);
        }});
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[2], new HashMap<Bearing, Double>() {{
            put(new DegreeBearingImpl(0)      , 0.35);
            put(ALLOWANCES_TRUE_WIND_ANGLES[1], 0.10);
            put(ALLOWANCES_TRUE_WIND_ANGLES[3], 0.075);
            put(ALLOWANCES_TRUE_WIND_ANGLES[5], 0.10);
            put(ALLOWANCES_TRUE_WIND_ANGLES[7], 0.10);
            put(new DegreeBearingImpl(180)    , 0.275);
        }});
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[3], new HashMap<Bearing, Double>() {{
            put(new DegreeBearingImpl(0)      , 0.30);
            put(ALLOWANCES_TRUE_WIND_ANGLES[1], 0.15);
            put(ALLOWANCES_TRUE_WIND_ANGLES[3], 0.10);
            put(ALLOWANCES_TRUE_WIND_ANGLES[5], 0.15);
            put(ALLOWANCES_TRUE_WIND_ANGLES[7], 0.15);
            put(new DegreeBearingImpl(180)    , 0.15);
        }});
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[4], new HashMap<Bearing, Double>() {{
            put(new DegreeBearingImpl(0)      , 0.25);
            put(ALLOWANCES_TRUE_WIND_ANGLES[1], 0.175);
            put(ALLOWANCES_TRUE_WIND_ANGLES[3], 0.125);
            put(ALLOWANCES_TRUE_WIND_ANGLES[5], 0.175);
            put(ALLOWANCES_TRUE_WIND_ANGLES[7], 0.15);
            put(new DegreeBearingImpl(180)    , 0.125);
        }});
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[5], new HashMap<Bearing, Double>() {{
            put(new DegreeBearingImpl(0)      , 0.20);
            put(ALLOWANCES_TRUE_WIND_ANGLES[1], 0.20);
            put(ALLOWANCES_TRUE_WIND_ANGLES[3], 0.15);
            put(ALLOWANCES_TRUE_WIND_ANGLES[5], 0.20);
            put(ALLOWANCES_TRUE_WIND_ANGLES[7], 0.15);
            put(new DegreeBearingImpl(180)    , 0.10);
        }});
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[6], new HashMap<Bearing, Double>() {{
            put(new DegreeBearingImpl(0)      , 0.10);
            put(ALLOWANCES_TRUE_WIND_ANGLES[1], 0.25);
            put(ALLOWANCES_TRUE_WIND_ANGLES[3], 0.20);
            put(ALLOWANCES_TRUE_WIND_ANGLES[5], 0.25);
            put(ALLOWANCES_TRUE_WIND_ANGLES[7], 0.10);
            put(new DegreeBearingImpl(180)    , 0.10);
        }});
        perCentOfAllowancesForLongDistancePC = Collections.unmodifiableMap(result);
    }
    
    /*
     * 
     */
    
    private final String sailnumber;
    private final String boatclass;
    private final Distance lengthOverAll;
    private final Duration gph;
    private final Map<Speed, Map<Bearing, Duration>> timeAllowancesPerTrueWindSpeedAndAngle;
    private final Map<Speed, Bearing> beatAngles;
    private final Map<Speed, Bearing> gybeAngles;


    public ORCCertificateImpl(String sailnumber, String boatclass, Distance length, Duration gph,
            Map<Speed, Map<Bearing, Duration>> timeAllowancesPerTrueWindSpeedAndAngle, Map<Speed, Bearing> beatAngles,
            Map<Speed, Bearing> gybeAngles) {
        super();
        this.sailnumber = sailnumber;
        this.boatclass = boatclass;
        this.lengthOverAll = length;
        this.gph = gph;
        this.timeAllowancesPerTrueWindSpeedAndAngle = Collections.unmodifiableMap(timeAllowancesPerTrueWindSpeedAndAngle);
        this.beatAngles = Collections.unmodifiableMap(beatAngles);
        this.gybeAngles = Collections.unmodifiableMap(gybeAngles);
    }

    @Override
    public ORCPerformanceCurve getPerformanceCurve(ORCPerformanceCurveCourse course) {
        return new ORCPerformanceCurveImpl(timeAllowancesPerTrueWindSpeedAndAngle, beatAngles, gybeAngles, course);
    }

    @Override
    public double getGPH() {
        return gph.asSeconds();
    }

    @Override
    public Map<Speed, Duration> getWindwardLeewardAllowances() {
        Map<Speed, Duration> result = new HashMap<>();
        for (Speed tws : ALLOWANCES_TRUE_WIND_SPEEDS) {
            //gets the Allowance for Beat&Run at the given TWS divided by 2
            Duration allowance = timeAllowancesPerTrueWindSpeedAndAngle.get(tws).get(beatAngles.get(tws))
                    .plus(timeAllowancesPerTrueWindSpeedAndAngle.get(tws).get(gybeAngles.get(tws))).divide(2);
            result.put(tws, allowance);
        }
        return result;
    }

    @Override
    public Map<Speed, Duration> getCircularRandomAllowances() {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<Speed, Duration> getLongDistanceAllowancesAlternative() {
        Map<Speed, Duration> result = new HashMap<>();
        for (Entry<Speed, Map<Bearing, Double>> twsEntry : perCentOfAllowancesForLongDistancePC.entrySet()) {
            double allowanceInSec = 0.0;
            for (Entry<Bearing, Double> twaEntry : twsEntry.getValue().entrySet()) {
                if(twaEntry.getKey().equals(new DegreeBearingImpl(0))) {
                    allowanceInSec += twaEntry.getValue() * timeAllowancesPerTrueWindSpeedAndAngle.get(twsEntry.getKey()).get(beatAngles.get(twsEntry.getKey())).asSeconds();
                }
                else if (twaEntry.getKey().equals(new DegreeBearingImpl(180))) {
                    allowanceInSec += twaEntry.getValue() * timeAllowancesPerTrueWindSpeedAndAngle.get(twsEntry.getKey()).get(gybeAngles.get(twsEntry.getKey())).asSeconds();
                }
                else {
                    allowanceInSec += twaEntry.getValue() * timeAllowancesPerTrueWindSpeedAndAngle.get(twsEntry.getKey()).get(twaEntry.getKey()).asSeconds();
                }
            }
            result.put(twsEntry.getKey(), Duration.ONE_SECOND.times(allowanceInSec));
        }
        return result;
    }
    
    @Override
    public Map<Speed, Duration> getLongDistanceAllowances() {
        Map<Speed, Duration> result = new HashMap<>();
        
        Duration allowance6knt = timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[0]).get(beatAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[0])).times(45);
        allowance6knt = allowance6knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[0]).get(gybeAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[0])).times(55));
        allowance6knt = allowance6knt.divide(100);
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[0], allowance6knt);
        
        Duration allowance8knt = timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[1]).get(beatAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[1])).times(40);
        allowance8knt = allowance8knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[1]).get(ALLOWANCES_TRUE_WIND_ANGLES[1]).times(5));
        allowance8knt = allowance8knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[1]).get(ALLOWANCES_TRUE_WIND_ANGLES[3]).times(5));
        allowance8knt = allowance8knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[1]).get(ALLOWANCES_TRUE_WIND_ANGLES[5]).times(5));
        allowance8knt = allowance8knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[1]).get(ALLOWANCES_TRUE_WIND_ANGLES[7]).times(5));
        allowance8knt = allowance8knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[1]).get(gybeAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[1])).times(40));
        allowance8knt = allowance8knt.divide(100);
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[1], allowance8knt);
        
        Duration allowance10knt = timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[2]).get(beatAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[2])).times(35);
        allowance10knt = allowance10knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[2]).get(ALLOWANCES_TRUE_WIND_ANGLES[1]).times(10));
        allowance10knt = allowance10knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[2]).get(ALLOWANCES_TRUE_WIND_ANGLES[3]).times(7.5));
        allowance10knt = allowance10knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[2]).get(ALLOWANCES_TRUE_WIND_ANGLES[5]).times(10));
        allowance10knt = allowance10knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[2]).get(ALLOWANCES_TRUE_WIND_ANGLES[7]).times(10));
        allowance10knt = allowance10knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[2]).get(gybeAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[2])).times(27.5));
        allowance10knt = allowance10knt.divide(100);
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[2], allowance10knt);
        
        Duration allowance12knt = timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[3]).get(beatAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[3])).times(30);
        allowance12knt = allowance12knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[3]).get(ALLOWANCES_TRUE_WIND_ANGLES[1]).times(15));
        allowance12knt = allowance12knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[3]).get(ALLOWANCES_TRUE_WIND_ANGLES[3]).times(10));
        allowance12knt = allowance12knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[3]).get(ALLOWANCES_TRUE_WIND_ANGLES[5]).times(15));
        allowance12knt = allowance12knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[3]).get(ALLOWANCES_TRUE_WIND_ANGLES[7]).times(15));
        allowance12knt = allowance12knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[3]).get(gybeAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[3])).times(15));
        allowance12knt = allowance12knt.divide(100);
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[3], allowance12knt);
        
        Duration allowance14knt = timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[4]).get(beatAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[4])).times(25);
        allowance14knt = allowance14knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[4]).get(ALLOWANCES_TRUE_WIND_ANGLES[1]).times(17.5));
        allowance14knt = allowance14knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[4]).get(ALLOWANCES_TRUE_WIND_ANGLES[3]).times(12.5));
        allowance14knt = allowance14knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[4]).get(ALLOWANCES_TRUE_WIND_ANGLES[5]).times(17.5));
        allowance14knt = allowance14knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[4]).get(ALLOWANCES_TRUE_WIND_ANGLES[7]).times(15));
        allowance14knt = allowance14knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[4]).get(gybeAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[4])).times(12.5));
        allowance14knt = allowance14knt.divide(100);
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[4], allowance14knt);
        
        Duration allowance16knt = timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[5]).get(beatAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[5])).times(20);
        allowance16knt = allowance16knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[5]).get(ALLOWANCES_TRUE_WIND_ANGLES[1]).times(20));
        allowance16knt = allowance16knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[5]).get(ALLOWANCES_TRUE_WIND_ANGLES[3]).times(15));
        allowance16knt = allowance16knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[5]).get(ALLOWANCES_TRUE_WIND_ANGLES[5]).times(20));
        allowance16knt = allowance16knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[5]).get(ALLOWANCES_TRUE_WIND_ANGLES[7]).times(15));
        allowance16knt = allowance16knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[5]).get(gybeAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[5])).times(10));
        allowance16knt = allowance16knt.divide(100);
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[5], allowance16knt);
        
        Duration allowance20knt = timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[6]).get(beatAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[6])).times(25);
        allowance20knt = allowance20knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[6]).get(ALLOWANCES_TRUE_WIND_ANGLES[1]).times(17.5));
        allowance20knt = allowance20knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[6]).get(ALLOWANCES_TRUE_WIND_ANGLES[3]).times(12.5));
        allowance20knt = allowance20knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[6]).get(ALLOWANCES_TRUE_WIND_ANGLES[5]).times(17.5));
        allowance20knt = allowance20knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[6]).get(ALLOWANCES_TRUE_WIND_ANGLES[7]).times(15));
        allowance20knt = allowance20knt.plus(timeAllowancesPerTrueWindSpeedAndAngle.get(ALLOWANCES_TRUE_WIND_SPEEDS[6]).get(gybeAngles.get(ALLOWANCES_TRUE_WIND_SPEEDS[6])).times(12.5));
        allowance20knt = allowance20knt.divide(100);
        result.put(ALLOWANCES_TRUE_WIND_SPEEDS[6], allowance20knt);
        
        return result;
    }

    @Override
    public Map<Speed, Duration> getNonSpinnakerAllowances() {
        // TODO Auto-generated method stub
        return null;
    }
}
