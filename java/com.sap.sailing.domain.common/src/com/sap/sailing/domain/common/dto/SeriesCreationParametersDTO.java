package com.sap.sailing.domain.common.dto;

import java.io.Serializable;
import java.util.List;

public class SeriesCreationParametersDTO implements Serializable {
    private static final long serialVersionUID = -3205172230707607515L;

    private List<FleetDTO> fleets;

    private boolean isMedal;

    private boolean isFleetsCanRunInParallel;

    private boolean isStartsWithZeroScore;

    private boolean firstColumnIsNonDiscardableCarryForward;

    private int[] discardingThresholds;

    private boolean hasSplitFleetContiguousScoring;

    private boolean hasCrossFleetMergedRanking;

    private Integer maximumNumberOfDiscards;

    private boolean oneAlwaysStaysOne;

    SeriesCreationParametersDTO() {}

    public SeriesCreationParametersDTO(List<FleetDTO> fleets, boolean isMedal, boolean isFleetsCanRunInParallel, boolean isStartsWithZeroScore, boolean firstColumnIsNonDiscardableCarryForward,
            int[] discardingThresholds, boolean hasSplitFleetContiguousScoring, boolean hasCrossFleetMergedRanking, Integer maximumNumberOfDiscards, boolean oneAlwaysStaysOne) {
        super();
        this.fleets = fleets;
        this.isMedal = isMedal;
        this.isFleetsCanRunInParallel = isFleetsCanRunInParallel;
        this.isStartsWithZeroScore = isStartsWithZeroScore;
        this.hasSplitFleetContiguousScoring = hasSplitFleetContiguousScoring;
        this.hasCrossFleetMergedRanking = hasCrossFleetMergedRanking;
        this.firstColumnIsNonDiscardableCarryForward = firstColumnIsNonDiscardableCarryForward;
        this.discardingThresholds = discardingThresholds;
        this.maximumNumberOfDiscards = maximumNumberOfDiscards;
        this.oneAlwaysStaysOne = oneAlwaysStaysOne;
    }

    public List<FleetDTO> getFleets() {
        return fleets;
    }

    public boolean isMedal() {
        return isMedal;
    }

    public boolean isStartsWithZero() {
        return isStartsWithZeroScore;
    }

    public boolean isFleetsCanRunInParallel() {
        return isFleetsCanRunInParallel;
    }

    public boolean hasSplitFleetContiguousScoring() {
        return hasSplitFleetContiguousScoring;
    }

    public boolean hasCrossFleetMergedRanking() {
       return hasCrossFleetMergedRanking;
    }

    public int[] getDiscardingThresholds() {
        return discardingThresholds;
    }

    public boolean isFirstColumnIsNonDiscardableCarryForward() {
        return firstColumnIsNonDiscardableCarryForward;
    }

    public Integer getMaximumNumberOfDiscards() {
        return maximumNumberOfDiscards;
    }

    public boolean isOneAlwaysStaysOne() {
        return oneAlwaysStaysOne;
    }
}
