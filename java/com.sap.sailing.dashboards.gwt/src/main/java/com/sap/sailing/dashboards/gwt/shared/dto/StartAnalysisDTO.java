package com.sap.sailing.dashboards.gwt.shared.dto;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;

public class StartAnalysisDTO implements IsSerializable, Comparable<StartAnalysisDTO> {

    public String raceName;
    public CompetitorDTO competitor;
    public WindAndAdvantagesInfoForStartLineDTO startAnalysisWindLineInfoDTO;
    public List<StartAnalysisCompetitorDTO> startAnalysisCompetitorDTOs;
    public long timeOfStartInMilliSeconds;
    public RegattaAndRaceIdentifier regattaAndRaceIdentifier;
    public RacingProcedureType racingProcedureType;
    public long tailLenghtInMilliseconds;

    public StartAnalysisDTO() {
    }

    @Override
    public int compareTo(StartAnalysisDTO o) {
        return raceName.compareTo(o.raceName);
    }

    public List<CompetitorDTO> getCompetitorDTOsFromStartAnaylsisCompetitorDTOs() {
        List<CompetitorDTO> competitorDTOs = new ArrayList<>();
        for (StartAnalysisCompetitorDTO startAnalysisCompetitorDTO : startAnalysisCompetitorDTOs) {
            competitorDTOs.add(startAnalysisCompetitorDTO.competitorDTO);
        }
        return competitorDTOs;
    }
}