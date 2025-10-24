package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.TimePoint;

/**
 * For YachtBot and WindBot devices, this lists the number of satellites the device currently uses to determine its
 * position. For the YachtBot smartphone app, restrictions may apply, as per the following e-mail by Brent Russell,
 * Igtimi CTO, sent at 2013-12-01T21:16:00Z:
 * 
 * <pre>
 * You're correct, we're not reading or propagating it at the moment from mobile devices, although it works fine with YachtBot hardware. 
 * The mobile platform is what we're giving away for free, so we're not trying to make it a direct feature match for YachtBot :)
 * 
 * Both mobile and YachtBot only Tx GPS coordinates when there is a valid fix, so in practice I think you should never see a zeroed
 * SatQ or SatC that is of any true meaning anyway.  Best just to ignore both these types (or throw away if zero) and not request it
 * with the ::data call.
 * 
 * We do have some plans to change the way fix quality information is transmitted.  Mostly it's so that changes only are transmitted,
 * and we'll encourage people to use hdop or gdop rather than satellite count.  There's some changes we need to make under the hood
 * first though, so not a near future thing.
 * </pre>
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class GpsQualitySatCount extends Fix {
    private static final long serialVersionUID = -5507100027164944068L;
    private final int satCount;
    
    public GpsQualitySatCount(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        this(timePoint, sensor, ((Number) valuesPerSubindex.get(1)).intValue());
    }

    public GpsQualitySatCount(TimePoint timePoint, Sensor sensor, int satCount) {
        super(sensor, timePoint);
        this.satCount = satCount;
    }

    public int getSatCount() {
        return satCount;
    }

    @Override
    protected String localToString() {
        return ""+getSatCount()+" satellites";
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
