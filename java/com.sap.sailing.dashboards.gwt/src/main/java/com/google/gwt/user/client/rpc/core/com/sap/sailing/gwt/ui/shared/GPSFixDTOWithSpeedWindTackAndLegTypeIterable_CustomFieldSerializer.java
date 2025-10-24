package com.google.gwt.user.client.rpc.core.com.sap.sailing.gwt.ui.shared;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegType;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegTypeIterable;
import com.sap.sailing.gwt.ui.shared.SpeedWithBearingDTO;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * Serializes the {@link GPSFixDTOWithSpeedWindTackAndLegType} objects one by one, writing only primitive values to the
 * stream, thus avoiding that the {@link SerializationStreamWriter} has to hold on to all those DTOs for
 * back-references, so that the DTOs can be short-lived as the iterable produces them. Upon de-serializing, the full
 * set of objects will be constructed from the data in the stream. The client will need it anyhow.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class GPSFixDTOWithSpeedWindTackAndLegTypeIterable_CustomFieldSerializer extends CustomFieldSerializer<GPSFixDTOWithSpeedWindTackAndLegTypeIterable> {
    private static final int MASK_HAS_DEGREES_BOAT_TO_THE_WIND = 1 << 0;
    private static final int MASK_HAS_DETAIL_VALUE             = 1 << 1;
    private static final int MASK_HAS_LEG_TYPE                 = 1 << 2;
    private static final int MASK_HAS_SPEED_WITH_BEARING       = 1 << 3;
    private static final int MASK_HAS_TACK                     = 1 << 4;
    private static final int MASK_HAS_POSITION                 = 1 << 5;
    private static final int MASK_HAS_TIMEPOINT                = 1 << 6;
    private static final int MASK_HAS_TRUE_HEADING             = 1 << 7;
    
    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, GPSFixDTOWithSpeedWindTackAndLegTypeIterable instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }
    
    public static void serialize(SerializationStreamWriter streamWriter, GPSFixDTOWithSpeedWindTackAndLegTypeIterable instance)
            throws SerializationException {
        for (final GPSFixDTOWithSpeedWindTackAndLegType fix : instance) {
            streamWriter.writeInt(mask(fix));
            if (fix.degreesBoatToTheWind != null) {
                streamWriter.writeDouble(fix.degreesBoatToTheWind);
            }
            if (fix.detailValue != null) {
                streamWriter.writeDouble(fix.detailValue);
            }
            streamWriter.writeBoolean(fix.extrapolated);
            if (fix.legType != null) {
                streamWriter.writeInt(fix.legType.ordinal());
            }
            if (fix.speedWithBearing != null) {
                streamWriter.writeDouble(fix.speedWithBearing.bearingInDegrees);
                streamWriter.writeDouble(fix.speedWithBearing.speedInKnots);
            }
            if (fix.tack != null) {
                streamWriter.writeInt(fix.tack.ordinal());
            }
            if (fix.position != null) {
                streamWriter.writeDouble(fix.position.getLatDeg());
                streamWriter.writeDouble(fix.position.getLngDeg());
            }
            if (fix.timepoint != null) {
                streamWriter.writeLong(fix.timepoint.getTime());
            }
            if (fix.optionalTrueHeading != null) {
                streamWriter.writeDouble(fix.optionalTrueHeading.getDegrees());
            }
        }
        streamWriter.writeInt(-1); // encodes a "mask" that marks the end of the iterable
    }

    public static GPSFixDTOWithSpeedWindTackAndLegTypeIterable instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        final List<GPSFixDTOWithSpeedWindTackAndLegType> list = new LinkedList<>();
        int mask;
        while ((mask = streamReader.readInt()) != -1) {
            final Double degreesBoatToTheWind;
            if ((mask & MASK_HAS_DEGREES_BOAT_TO_THE_WIND) != 0) {
                degreesBoatToTheWind = streamReader.readDouble();
            } else {
                degreesBoatToTheWind = null;
            }
            final Double detailValue;
            if ((mask & MASK_HAS_DETAIL_VALUE) != 0) {
                detailValue = streamReader.readDouble();
            } else {
                detailValue = null;
            }
            final boolean extrapolated = streamReader.readBoolean();
            final LegType legType;
            if ((mask & MASK_HAS_LEG_TYPE) != 0) {
                legType = LegType.values()[streamReader.readInt()];
            } else {
                legType = null;
            }
            final SpeedWithBearingDTO speedWithBearing;
            if ((mask & MASK_HAS_SPEED_WITH_BEARING) != 0) {
                final double bearingInDegrees = streamReader.readDouble();
                final double speedInKnots = streamReader.readDouble();
                speedWithBearing = new SpeedWithBearingDTO(speedInKnots, bearingInDegrees);
            } else {
                speedWithBearing = null;
            }
            final Tack tack;
            if ((mask & MASK_HAS_TACK) != 0) {
                tack = Tack.values()[streamReader.readInt()];
            } else {
                tack = null;
            }
            final Position position;
            if ((mask & MASK_HAS_POSITION) != 0) {
                final double latDeg = streamReader.readDouble();
                final double lngDeg = streamReader.readDouble();
                position = new DegreePosition(latDeg, lngDeg);
            } else {
                position = null;
            }
            final Date timepoint;
            if ((mask & MASK_HAS_TIMEPOINT) != 0) {
                timepoint = new Date(streamReader.readLong());
            } else {
                timepoint = null;
            }
            final Bearing optionalTrueHeading;
            if ((mask & MASK_HAS_TRUE_HEADING) != 0) {
                optionalTrueHeading = new DegreeBearingImpl(streamReader.readDouble());
            } else {
                optionalTrueHeading = null;
            }
            list.add(new GPSFixDTOWithSpeedWindTackAndLegType(timepoint, position, speedWithBearing, optionalTrueHeading, degreesBoatToTheWind, tack, legType, extrapolated, detailValue));
        }
        return new GPSFixDTOWithSpeedWindTackAndLegTypeIterable(list);
    }

    private static int mask(GPSFixDTOWithSpeedWindTackAndLegType fix) {
        return 0
                | (fix.degreesBoatToTheWind == null ? 0 : MASK_HAS_DEGREES_BOAT_TO_THE_WIND)
                | (fix.detailValue          == null ? 0 : MASK_HAS_DETAIL_VALUE)
                | (fix.legType              == null ? 0 : MASK_HAS_LEG_TYPE)
                | (fix.speedWithBearing     == null ? 0 : MASK_HAS_SPEED_WITH_BEARING)
                | (fix.tack                 == null ? 0 : MASK_HAS_TACK)
                | (fix.position             == null ? 0 : MASK_HAS_POSITION)
                | (fix.timepoint            == null ? 0 : MASK_HAS_TIMEPOINT)
                | (fix.optionalTrueHeading  == null ? 0 : MASK_HAS_TRUE_HEADING)
                ;
    }
    
    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, GPSFixDTOWithSpeedWindTackAndLegTypeIterable instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, GPSFixDTOWithSpeedWindTackAndLegTypeIterable instance) {
        // Done by instantiateInstance
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }
    
    @Override
    public GPSFixDTOWithSpeedWindTackAndLegTypeIterable instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }
}
