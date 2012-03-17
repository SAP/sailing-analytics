package com.sap.sailing.gwt.ui.shared;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class RaceMapDataDTO implements IsSerializable {
    public Map<CompetitorDTO, List<GPSFixDTO>> boatPositions;
    public List<MarkDTO> markPositions;
    public List<QuickRankDTO> quickRanks;
}
