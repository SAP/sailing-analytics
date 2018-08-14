package com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab;

import java.util.Collection;

import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileEntry;

public interface SailorProfileOverview extends SailorProfileView {

    void setProfileList(Collection<SailorProfileEntry> entries);

}
