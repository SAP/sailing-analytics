package com.sap.sailing.gwt.ui.shared;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.common.dto.CompetitorDTO;

public class RaceMapDataDTO implements IsSerializable {
    public Map<CompetitorDTO, List<GPSFixDTO>> boatPositions;
    public CoursePositionsDTO coursePositions;
    public List<QuickRankDTO> quickRanks;
}
