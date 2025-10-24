package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.common.impl.KilometersPerHourSpeedImpl;
import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;

/**
 * Apparent wind speed, relative to the inertial system on which the wind speed was measured.
 * Speed is stored as kilometers per hour in the Igtimi database.<p>
 * 
 * Mail from Brent from June 10th 2014:<p>
 * "The problem is units.  We store everything in the database in SI units – so SOG, STW, and AWS are all in KPH instead of Knots. 
 * Simon, I think this explains the error you were seeing on the extreme 40 circuit as well.  The reason that I didn’t identify this 
 * before is that I incorrectly thought we had everything in Knots – which is what I had told Axel as well. My apologies for that error. 
 * Note that your TW calculation will still be correct because the units for SOG and AWS (input for T-calcs) are both KPH. 
 * However the output will need to be divided by 1.852 if you want to see the results in Knots."
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class AWS extends Fix {
    private static final long serialVersionUID = -4044229400122127582L;
    private final Speed apparentWindSpeed;
    
    public AWS(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        this(timePoint, sensor, new KilometersPerHourSpeedImpl(((Number) valuesPerSubindex.get(1)).doubleValue()));
    }

    public AWS(TimePoint timePoint, Sensor sensor, Speed apparentWindSpeed) {
        super(sensor, timePoint);
        this.apparentWindSpeed = apparentWindSpeed;
    }

    public Speed getApparentWindSpeed() {
        return apparentWindSpeed;
    }

    @Override
    protected String localToString() {
        return "AWS: "+getApparentWindSpeed();
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
