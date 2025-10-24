package com.sap.sailing.domain.trackimport;

import java.io.Serializable;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceBoatSensorDataMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorSensorDataMappingEvent;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sse.common.TimePoint;

/**
 * Definition of importers used by SensorDataImportServlet to do the import of a specific file type.
 */
public interface DoubleVectorFixImporter extends BaseDoubleVectorFixImporter {

    public static final String EXPEDITION_EXTENDED_TYPE = "EXPEDITION_EXTENDED";
    
    /**
     * Creates the {@link RegattaLogEvent} for the DeviceMapping, mapping to a {@link Competitor}
     */
    RegattaLogDeviceCompetitorSensorDataMappingEvent createEvent(TimePoint createdAt, TimePoint logicalTimePoint,
            AbstractLogEventAuthor author, Serializable id, Competitor mappedTo, DeviceIdentifier device,
            TimePoint from, TimePoint to);
    
    /**
     * Creates the {@link RegattaLogEvent} for the DeviceMapping, mapping to a {@link Boat}
     */
    RegattaLogDeviceBoatSensorDataMappingEvent createEvent(TimePoint createdAt, TimePoint logicalTimePoint,
            AbstractLogEventAuthor author, Serializable id, Boat mappedTo, DeviceIdentifier device,
            TimePoint from, TimePoint to);
}
