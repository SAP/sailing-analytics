package com.sap.sailing.simulator;

import java.util.List;
import java.util.Map;


public interface SailingSimulator {

    void setSimulationParameters(SimulationParameters params, SimulatorUISelection selection);

    SimulationParameters getSimulationParameters();

    Path getLegGPSTrack(SimulatorUISelection selection);

    Map<String, Path> getAllPaths();

    Map<String, Path> getAllPathsForLeg(SimulatorUISelection selection);

    Map<String, Path> getAllPathsEvenTimed(long millisecondsStep, SimulatorUISelection selection);

    Path getRaceCourse();

    List<String> getLegsNames(int selectedRaceIndex);

    List<String> getRacesNames();

    Path getLeg(int selectedCompetitorIndex, int selectedLegIndex);

    List<String> getComeptitorsNames(int selectedRaceIndex);
}
