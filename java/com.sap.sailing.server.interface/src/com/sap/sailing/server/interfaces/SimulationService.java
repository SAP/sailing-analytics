package com.sap.sailing.server.interfaces;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.common.LegIdentifier;
import com.sap.sailing.domain.common.PathType;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.SimulationResults;

public interface SimulationService {
    long getSimulationResultsVersion(LegIdentifier legIdentifier);
    
    SimulationResults getSimulationResults(LegIdentifier legIdentifier);

    Map<PathType, Path> getAllPathsEvenTimed(SimulationParameters simuPars, long millisecondsStep)
            throws InterruptedException, ExecutionException;

    Iterable<BoatClass> getBoatClassesWithPolarData();

    /**
     * @param boatClass
     *            must be one that is in the result set of {@link #getBoatClassesWithPolarData()}. Otherwise,
     *            {@code null} will be returned.
     */
    PolarDiagram getPolarDiagram(BoatClass boatClass);

    /**
     * Obtains a boat class from the {@link RacingEventService} by name, using {@link RacingEventService#getBaseDomainFactory()} and its
     * {@link SharedDomainFactory#getBoatClass(String)} method.
     */
    BoatClass getBoatClass(String name);
}
