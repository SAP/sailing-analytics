package com.sap.sailing.domain.igtimiadapter;

import com.sap.sailing.domain.igtimiadapter.impl.DataAccessWindowImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;

/**
 * When a device is shared to you, all that happens is that a DAW (Data Access Window) is being created for the time
 * interval that the device owner wants you to have access to the data from the device, and that
 * permissions---particularly for the {@link DefaultActions#READ} action---are granted to other users, either through
 * access control lists (ACLs) or by explicitly assigning permissions to users or indirectly through roles.
 * <p>
 * 
 * So you’re not actually sharing a device per-se, rather you are granting access to data that it creates. DAW’s allow
 * granting access to data from a device for any interval in time; past, present or future.
 * <p>
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public interface DataAccessWindow extends HasId, HasStartAndEndTime, WithQualifiedObjectIdentifier {
    String getDeviceSerialNumber();

    static DataAccessWindow create(long id, TimePoint startTime, TimePoint endTime, String deviceSerialNumber) {
        return new DataAccessWindowImpl(id, startTime, endTime, deviceSerialNumber);
    }
    
    static TypeRelativeObjectIdentifier createTypeRelativeObjectIdentifier(String deviceSerialNumber, TimePoint startTime, TimePoint endTime) {
        return new TypeRelativeObjectIdentifier(deviceSerialNumber,
                ""+(startTime==null?"null":startTime.asMillis()),
                ""+(endTime==null?"null":endTime.asMillis()));
    }
}
