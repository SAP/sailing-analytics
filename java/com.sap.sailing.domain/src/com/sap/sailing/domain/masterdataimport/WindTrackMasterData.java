package com.sap.sailing.domain.masterdataimport;

import java.io.Serializable;

import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;
import com.sap.sailing.domain.tracking.WindTrack;

public class WindTrackMasterData implements Serializable {

    private static final long serialVersionUID = -2047209715118386803L;

    private final WindSourceType windSourceType;
    private final Object windSourceId;
    private final WindTrack windTrack;
    private final String regattaName;
    private final String raceName;
    private final Serializable raceId;

    public WindTrackMasterData(WindSourceType windSourceType, Object windSourceId, WindTrack windTrack,
            String regattaName, String raceName, Serializable raceId) {
        this.windSourceId = windSourceId;
        this.windTrack = windTrack;
        this.regattaName = regattaName;
        this.raceName = raceName;
        this.raceId = raceId;
        this.windSourceType = windSourceType;
    }

    public WindSourceType getWindSourceType() {
        return windSourceType;
    }

    public Object getWindSourceId() {
        return windSourceId;
    }

    public WindTrack getWindTrack() {
        return windTrack;
    }
    
    public String getRegattaName() {
        return regattaName;
    }

    public String getRaceName() {
        return raceName;
    }

    public Serializable getRaceId() {
        return raceId;
    }

    public WindSource getWindSource() {
        WindSource source;
        if (windSourceId != null) {
            source = new WindSourceWithAdditionalID(windSourceType, windSourceId.toString());
        } else {
            source = new WindSourceImpl(windSourceType);
        }
        return source;
    }

}
