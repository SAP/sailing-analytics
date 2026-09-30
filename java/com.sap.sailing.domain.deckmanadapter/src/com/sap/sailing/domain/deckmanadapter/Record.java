package com.sap.sailing.domain.deckmanadapter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.deckmanadapter.impl.FieldType;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.Positioned;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class Record implements Timed, Positioned {
    private static final Logger logger = Logger.getLogger(Record.class.getName());

    private static final long serialVersionUID = -7939775022865795801L;

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    
    private final TimePoint timePoint;
    
    private final Position position; 
    
    private final GPSFixMoving gpsFix;
    
    private final Wind wind;
    
    private Map<FieldType, String> fieldsAsString;
    
    public Record(Map<FieldType, String> fieldsAsString) throws ParseException {
        this.fieldsAsString = fieldsAsString;
        final Date date;
        synchronized (dateFormat) {
            date = dateFormat.parse(this.fieldsAsString.get(FieldType.date));
        }
        timePoint = new MillisecondsTimePoint(date);
        position = new DegreePosition(Double.valueOf(this.fieldsAsString.get(FieldType.latitude)),
                Double.valueOf(this.fieldsAsString.get(FieldType.longitude)));
        final SpeedWithBearing speed = new KnotSpeedWithBearingImpl(Double.valueOf(this.fieldsAsString.get(FieldType.sog)),
                new DegreeBearingImpl(Double.valueOf(this.fieldsAsString.get(FieldType.cog))));
        Bearing optionalTrueHeading;
        try {
            optionalTrueHeading = this.fieldsAsString.get(FieldType.heading) == null ? null :
                new DegreeBearingImpl(Double.valueOf(this.fieldsAsString.get(FieldType.heading))).add(
                        DeclinationService.INSTANCE.getDeclination(timePoint, position, /* timeout in ms */ 1000).getBearingCorrectedTo(timePoint));
        } catch (NumberFormatException | IOException | ParseException e) {
            logger.log(Level.WARNING, "Problem obtaining magnetic declination for converting Deckman magnetic heading value to true", e);
            optionalTrueHeading = null;
        }
        wind = new WindImpl(position, timePoint, new KnotSpeedWithBearingImpl(Double.valueOf(this.fieldsAsString
                .get(FieldType.tws)), new DegreeBearingImpl(Double.valueOf(this.fieldsAsString.get(FieldType.twd)))));
        gpsFix = new GPSFixMovingImpl(position, timePoint, speed, optionalTrueHeading);
    }
    
    public String getField(FieldType fieldType) {
        return fieldsAsString.get(fieldType);
    }

    public Map<FieldType, String> getFieldsAsString() {
        return fieldsAsString;
    }

    public void setFieldsAsString(Map<FieldType, String> fieldsAsString) {
        this.fieldsAsString = fieldsAsString;
    }

    public TimePoint getTimePoint() {
        return timePoint;
    }

    public GPSFixMoving getGpsFix() {
        return gpsFix;
    }

    public Wind getWind() {
        return wind;
    }

    @Override
    public Position getPosition() {
        return position;
    }
}
