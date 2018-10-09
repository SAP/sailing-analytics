package com.sap.sailing.gwt.home.mobile.places.user.profile.sailorprofiles;

import java.util.List;

import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileDTO;
import com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.SailingProfileOverviewPresenter;
import com.sap.sailing.gwt.home.mobile.places.user.profile.UserProfileViewBase;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.EditSailorProfilePresenter;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.view.SailorProfileView;

public interface SailorProfilesOverviewView
        extends UserProfileViewBase, SailorProfileView, SailorProfilesViewWithAuthenticationContext {
    public interface Presenter extends UserProfileViewBase.Presenter {
        EditSailorProfilePresenter getSailorProfilePresenter();

        SailingProfileOverviewPresenter getSailorProfileOverviewPresenter();
    }

    public void setProfileList(List<SailorProfileDTO> entries);
}
