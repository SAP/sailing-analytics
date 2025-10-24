package com.sap.sailing.domain.common.tracking.impl;

import com.sap.sailing.domain.common.Mile;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.sensordata.BravoExtendedSensorDataMetadata;
import com.sap.sailing.domain.common.sensordata.ColumnMetadata;
import com.sap.sailing.domain.common.tracking.BravoExtendedFix;
import com.sap.sailing.domain.common.tracking.BravoFix;
import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * Implementation of {@link BravoExtendedFix}. {@link BravoExtendedFix} adds more measures compared to {@link BravoFix}.
 */
public class BravoExtendedFixImpl extends BravoFixImpl implements BravoExtendedFix {
    private static final long serialVersionUID = 5622321493028301922L;

    public BravoExtendedFixImpl(DoubleVectorFix fix) {
        super(fix);
    }

    @Override
    protected ColumnMetadata resolveMetadataFromValueName(String valueName) {
        return BravoExtendedSensorDataMetadata.byColumnName(valueName);
    }

    @Override
    public Double getPortDaggerboardRake() {
        return fix.get(BravoExtendedSensorDataMetadata.DB_RAKE_PORT.getColumnIndex());
    }

    @Override
    public Double getStbdDaggerboardRake() {
        return fix.get(BravoExtendedSensorDataMetadata.DB_RAKE_STBD.getColumnIndex());
    }

    @Override
    public Double getPortRudderRake() {
        return fix.get(BravoExtendedSensorDataMetadata.RUDDER_RAKE_PORT.getColumnIndex());
    }

    @Override
    public Double getStbdRudderRake() {
        return fix.get(BravoExtendedSensorDataMetadata.RUDDER_RAKE_STBD.getColumnIndex());
    }

    @Override
    public Bearing getMastRotation() {
        final Double bearingDeg = fix.get(BravoExtendedSensorDataMetadata.MAST_ROTATION.getColumnIndex());
        return bearingDeg == null ? null : new DegreeBearingImpl(bearingDeg);
    }

    @Override
    public Bearing getLeeway() {
        final Double leeway = fix.get(BravoExtendedSensorDataMetadata.LEEWAY.getColumnIndex());
        return leeway == null ? null : new DegreeBearingImpl(leeway);
    }

    @Override
    public Double getSet() {
        return fix.get(BravoExtendedSensorDataMetadata.SET.getColumnIndex());
    }

    @Override
    public Bearing getDrift() {
        final Double drift = fix.get(BravoExtendedSensorDataMetadata.DRIFT.getColumnIndex());
        return drift == null ? null : new DegreeBearingImpl(drift);
    }

    @Override
    public Distance getDepth() {
        final Double depthInMeters = fix.get(BravoExtendedSensorDataMetadata.DEPTH.getColumnIndex());
        return depthInMeters == null ? null : new MeterDistance(depthInMeters);
    }

    @Override
    public Bearing getRudder() {
        final Double rudderAngleDeg = fix.get(BravoExtendedSensorDataMetadata.RUDDER.getColumnIndex());
        return rudderAngleDeg == null ? null : new DegreeBearingImpl(rudderAngleDeg);
    }

    @Override
    public Double getForestayLoad() {
        return fix.get(BravoExtendedSensorDataMetadata.FORESTAY_LOAD.getColumnIndex());
    }

    @Override
    public Double getForestayPressure() {
        return fix.get(BravoExtendedSensorDataMetadata.FORESTAY_PRESSURE.getColumnIndex());
    }

    @Override
    public Bearing getTackAngle() {
        final Double tackAngleDeg = fix.get(BravoExtendedSensorDataMetadata.TACK_ANGLE.getColumnIndex());
        return tackAngleDeg == null ? null : new DegreeBearingImpl(tackAngleDeg);
    }

    @Override
    public Bearing getRake() {
        final Double rakeDeg = fix.get(BravoExtendedSensorDataMetadata.RAKE_DEG.getColumnIndex());
        return rakeDeg == null ? null : new DegreeBearingImpl(rakeDeg);
    }

    @Override
    public Double getDeflectorPercentage() {
        return fix.get(BravoExtendedSensorDataMetadata.DEFLECTOR_PERCENTAGE.getColumnIndex());
    }

    @Override
    public Bearing getTargetHeel() {
        final Double targetHeelDeg = fix.get(BravoExtendedSensorDataMetadata.TARGET_HEEL.getColumnIndex());
        return targetHeelDeg == null ? null : new DegreeBearingImpl(targetHeelDeg);
    }

    @Override
    public Distance getDeflector() {
        final Double deflectorMillimeters = fix.get(BravoExtendedSensorDataMetadata.DEFLECTOR_MILLIMETERS.getColumnIndex());
        return deflectorMillimeters == null ? null : new MeterDistance(deflectorMillimeters / 1000.);
    }

    @Override
    public Double getTargetBoatspeedP() {
        return fix.get(BravoExtendedSensorDataMetadata.TARGET_BOATSPEED_P.getColumnIndex());
    }
    
    @Override
    public Double getExpeditionAWA() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_AWA.getColumnIndex());
    }
    
    @Override
    public Double getExpeditionAWS() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_AWS.getColumnIndex());
    }
    
    @Override
    public Double getExpeditionTWA() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_TWA.getColumnIndex());
    }
    @Override
    public Double getExpeditionTWS() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_TWS.getColumnIndex());
    }
    @Override
    public Double getExpeditionTWD() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_TWD.getColumnIndex());
    }

    @Override
    public Double getExpeditionBSP() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_BSP.getColumnIndex());
    }

    @Override
    public Double getExpeditionBSP_TR() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_BSP_TR.getColumnIndex());
    }

    @Override
    public Double getExpeditionSOG() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_SOG.getColumnIndex());
    }

    @Override
    public Double getExpeditionCOG() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_COG.getColumnIndex());
    }

    @Override
    public Double getExpeditionForestayLoad() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_FORESTAY.getColumnIndex());
    }

    @Override
    public Double getExpeditionRake() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_RAKE.getColumnIndex());
    }

    @Override
    public Double getExpeditionHDG() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_HDG.getColumnIndex());
    }

    @Override
    public Double getExpeditionHeel() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_HEEL.getColumnIndex());
    }

    @Override
    public Double getExpeditionTG_Heell() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_TGHEEL.getColumnIndex());
    }
    
    @Override
    public Double getExpeditionBARO() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_BARO.getColumnIndex());
    }
    
    @Override
    public Double getExpeditionLoadP() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_LOAD_P.getColumnIndex());
    }
    
    @Override
    public Double getExpeditionLoadS() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_LOAD_S.getColumnIndex());
    }
    
    @Override
    public Double getExpeditionJibCarPort() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_JIB_CAR_PORT.getColumnIndex());
    }
    
    @Override
    public Double getExpeditionJibCarStbd() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_JIB_CAR_STBD.getColumnIndex());
    }
    
    @Override
    public Double getExpeditionMastButt() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_MAST_BUTT.getColumnIndex());
    }

    @Override
    public Double getExpeditionTmToGunInSeconds() {
        final Double days = fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_TMTOGUN.getColumnIndex());
        return days == null ? null : days*24.0*3600.0;
    }

    @Override
    public Double getExpeditionTmToBurnInSeconds() {
        final Double days = fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_TMTOBURN.getColumnIndex());
        return days == null ? null : days*24.0*3600.0;
    }

    @Override
    public Double getExpeditionBelowLnInMeters() {
        final Double nauticalMiles = fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_BELOWLN.getColumnIndex());
        return nauticalMiles == null ? null : (nauticalMiles * Mile.METERS_PER_NAUTICAL_MILE);
    }
    
    @Override
    public Double getExpeditionRateOfTurn() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_RATE_OF_TURN.getColumnIndex());
    }

    @Override
    public Double getExpeditionCourse() {
        final Double expeditionHDG = getExpeditionHDG();
        final Bearing leeway = getLeeway();
        if (expeditionHDG != null && leeway != null) {
            return expeditionHDG + leeway.getDegrees();
        }
        return null;
    }

    @Override
    public Double getExpeditionKickerTension() {
        return fix.get(BravoExtendedSensorDataMetadata.EXPEDITION_KICKER_TENSION.getColumnIndex());
    }
}
