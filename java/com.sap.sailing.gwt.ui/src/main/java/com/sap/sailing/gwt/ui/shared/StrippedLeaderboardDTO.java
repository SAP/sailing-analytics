package com.sap.sailing.gwt.ui.shared;

import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;

public class StrippedLeaderboardDTO extends AbstractLeaderboardDTO {
    private static final long serialVersionUID = 3285029720177137625L;

    public void removeRace(String raceColumnName) {
        getRaceList().remove(getRaceColumnByName(raceColumnName));
    }
}
