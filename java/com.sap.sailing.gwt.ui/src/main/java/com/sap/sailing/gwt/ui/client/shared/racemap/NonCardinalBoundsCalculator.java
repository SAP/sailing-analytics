package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.sap.sailing.domain.common.NonCardinalBounds;

public interface NonCardinalBoundsCalculator {
    NonCardinalBounds calculateNewBounds(RaceMap forMap);
}
