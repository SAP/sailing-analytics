package com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.details;

import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileDTO;
import com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.SailorProfileView;

public interface SailorProfileDetailsView extends SailorProfileView {

    void setEntry(SailorProfileDTO entry);

}
