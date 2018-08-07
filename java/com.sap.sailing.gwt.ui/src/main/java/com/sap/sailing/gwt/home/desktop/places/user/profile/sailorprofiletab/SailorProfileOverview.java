package com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab;

import java.util.List;

import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.domain.SailorProfileEntry;

public interface SailorProfileOverview extends SailorProfileView {

    void setProfileList(List<SailorProfileEntry> entries);

}
