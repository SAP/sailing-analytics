package com.sap.sailing.gwt.ui.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.ui.shared.BoatClassDTO;
import com.sap.sailing.gwt.ui.shared.PositionDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldGenParamsDTO;
import com.sap.sailing.gwt.ui.shared.WindLatticeDTO;
import com.sap.sailing.gwt.ui.shared.WindLatticeGenParamsDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldGenParamsDTO.WindPattern;

public interface SimulatorServiceAsync {

	void getRaceLocations(AsyncCallback<PositionDTO[]> callback);

	void getWindLatice(WindLatticeGenParamsDTO params,
			AsyncCallback<WindLatticeDTO> callback);

	void getWindField(WindFieldGenParamsDTO params,
			AsyncCallback<WindFieldDTO> callback);

	void getWindPatterns(AsyncCallback<WindPattern[]> callback);
	
	void getBoatClasses(AsyncCallback<BoatClassDTO[]> callback);
}
