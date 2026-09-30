package com.sap.sailing.gwt.ui.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.ui.shared.BoatClassDTOsAndNotificationMessage;
import com.sap.sailing.gwt.ui.shared.PolarDiagramDTOAndNotificationMessage;
import com.sap.sailing.gwt.ui.shared.Request1TurnerDTO;
import com.sap.sailing.gwt.ui.shared.RequestTotalTimeDTO;
import com.sap.sailing.gwt.ui.shared.Response1TurnerDTO;
import com.sap.sailing.gwt.ui.shared.ResponseTotalTimeDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorResultsDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorUISelectionDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldGenParamsDTO;
import com.sap.sailing.gwt.ui.shared.WindLatticeDTO;
import com.sap.sailing.gwt.ui.shared.WindLatticeGenParamsDTO;
import com.sap.sailing.gwt.ui.shared.WindPatternDTO;
import com.sap.sailing.gwt.ui.simulator.windpattern.WindPatternDisplay;
import com.sap.sse.common.Position;

public interface SimulatorServiceAsync {

    void getRaceLocations(AsyncCallback<Position[]> callback);

    void getWindLatice(WindLatticeGenParamsDTO params, AsyncCallback<WindLatticeDTO> callback);

    void getWindField(WindFieldGenParamsDTO params, WindPatternDisplay display, AsyncCallback<WindFieldDTO> callback);

    void getWindPatterns(char mode, AsyncCallback<List<WindPatternDTO>> callback);

    void getWindPatternDisplay(WindPatternDTO pattern, AsyncCallback<WindPatternDisplay> callback);

    void getBoatClasses(AsyncCallback<BoatClassDTOsAndNotificationMessage> callback);

    void getPolarDiagram(Double bearingStep, int boatClassIndex, AsyncCallback<PolarDiagramDTOAndNotificationMessage> callback);

    void getTotalTime(RequestTotalTimeDTO requestData, AsyncCallback<ResponseTotalTimeDTO> asyncCallback);

    void get1Turner(final Request1TurnerDTO requestData, AsyncCallback<Response1TurnerDTO> asyncCallback);

    void getLegsNames(int selectedRaceIndex, AsyncCallback<List<String>> asyncCallback);

    void getRacesNames(AsyncCallback<List<String>> asyncCallback);

    void getSimulatorResults(char mode, char rcDirection, WindFieldGenParamsDTO params, WindPatternDisplay pattern, boolean withWindField, SimulatorUISelectionDTO selection,
            AsyncCallback<SimulatorResultsDTO> callback);

    void getCompetitorsNames(int selectedRaceIndex, AsyncCallback<List<String>> asyncCallback);
    
    void getGoogleMapsLoaderAuthenticationParams(AsyncCallback<String> asyncCallback);
}
