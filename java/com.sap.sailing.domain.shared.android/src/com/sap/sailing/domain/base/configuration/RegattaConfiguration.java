package com.sap.sailing.domain.base.configuration;

import java.io.Serializable;

import com.sap.sailing.domain.base.configuration.procedures.ESSConfiguration;
import com.sap.sailing.domain.base.configuration.procedures.GateStartConfiguration;
import com.sap.sailing.domain.base.configuration.procedures.RRS26Configuration;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.racelog.state.racingprocedure.RacingProcedure;

/**
 * Interface holding configuration options for a Regatta, including configurations for 
 * all types of a {@link RacingProcedure}s.
 */
public interface RegattaConfiguration extends Serializable, Cloneable {
    
    /**
     * Gets the default {@link CourseDesignerMode} for races configured.
     */
    CourseDesignerMode getDefaultCourseDesignerMode();
    
    /**
     * Gets the default {@link RacingProcedureType} for races configured.
     */
    RacingProcedureType getDefaultRacingProcedureType();
    
    /**
     * Gets the configuration for RRS26 races.
     * @return <code>null</code> if there is no configuration.
     */
    RRS26Configuration getRRS26Configuration();
    
    /**
     * Gets the configuration for Gate Start races.
     * @return <code>null</code> if there is no configuration.
     */
    GateStartConfiguration getGateStartConfiguration();
    
    /**
     * Gets the configuration for ESS races.
     * @return <code>null</code> if there is no configuration.
     */
    ESSConfiguration getESSConfiguration();
    
    /**
     * Gets the configuration for Basic races.
     * @return <code>null</code> if there is no configuration.
     */
    RacingProcedureConfiguration getBasicConfiguration();
    
    /**
     * Copy me.
     */
    RegattaConfiguration clone();

    /**
     * Creates a copied {@link RegattaConfiguration} with all 
     * non-<code>null</code> fields taken from the passed {@link RegattaConfiguration}.
     */
    RegattaConfiguration merge(RegattaConfiguration update);

}
