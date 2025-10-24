package com.sap.sailing.datamining.shared;

import com.sap.sse.common.Duration;
import com.sap.sse.common.settings.SerializableSettings;

public class TackTypeSegmentsDataMiningSettings extends SerializableSettings {
    private static final long serialVersionUID = 3239182650750480678L;
    private final Duration minimumTackTypeSegmentDuration;
    private final Duration minimumDurationBetweenAdjacentTackTypeSegments;

    public TackTypeSegmentsDataMiningSettings(Duration minimumTackTypeSegmentDuration, Duration minimumDurationBetweenAdjacentTackTypeSegments) {
        super();
        this.minimumTackTypeSegmentDuration = minimumTackTypeSegmentDuration;
        this.minimumDurationBetweenAdjacentTackTypeSegments = minimumDurationBetweenAdjacentTackTypeSegments;
    }

    public Duration getMinimumTackTypeSegmentDuration() {
        return minimumTackTypeSegmentDuration;
    }
    public Duration getMinimumDurationBetweenAdjacentTackTypeSegments() {
        return minimumDurationBetweenAdjacentTackTypeSegments;
    }

    public static TackTypeSegmentsDataMiningSettings createDefaultSettings() {
        return new TackTypeSegmentsDataMiningSettings(
                /* minimumTackTypeSegmentDuration */ null,
                /* minimumDurationBetweenAdjacentTackTypeSegments */ null);
    }
}
