package com.sap.sailing.domain.common.orc;

import com.sap.sailing.domain.common.LegType;

public enum ORCPerformanceCurveLegTypes {
    TWA(true), WINDWARD_LEEWARD(true), WINDWARD_LEEWARD_REAL_LIVE(true), LONG_DISTANCE(false), NON_SPINNAKER(false), CIRCULAR_RANDOM(false);

    /**
     * Determines a {@link LegType} to use for, e.g., determining a leg's distance. If no ORC leg type is provided
     * ({@code null}, or the type is {@link #TWA}, {@link #WINDWARD_LEEWARD_REAL_LIVE} or {@link #WINDWARD_LEEWARD}, the
     * result will be {@code null}, telling the system to compute the real leg type by itself, leading to a windward
     * projection for upwind and downwind legs, and rhumb-line projection for reaching legs.
     * <p>
     * 
     * For all other ORC leg types, rhumb-line projection is forced by returning {@link LegType#REACHING}.
     */
    public static LegType getLegType(ORCPerformanceCurveLegTypes orcLegType) {
        final LegType result;
        if (orcLegType == null || orcLegType.isProjectToWindForUpwindAndDownwind()) {
            result = null;
        } else {
            result = LegType.REACHING;
        }
        return result;
    }
    
    private final boolean projectToWindForUpwindAndDownwind;

    private ORCPerformanceCurveLegTypes(boolean projectToWindForUpwindAndDownwind) {
        this.projectToWindForUpwindAndDownwind = projectToWindForUpwindAndDownwind;
    }

    public boolean isProjectToWindForUpwindAndDownwind() {
        return projectToWindForUpwindAndDownwind;
    }
}
