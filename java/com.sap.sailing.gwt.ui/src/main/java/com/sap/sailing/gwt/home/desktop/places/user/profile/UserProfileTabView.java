package com.sap.sailing.gwt.home.desktop.places.user.profile;

import com.sap.sailing.gwt.common.client.controls.tabbar.TabView;
import com.sap.sailing.gwt.home.shared.places.user.profile.AbstractUserProfilePlace;
import com.sap.sse.security.ui.authentication.app.NeedsAuthenticationContext;

public interface UserProfileTabView<PLACE extends AbstractUserProfilePlace> extends
        TabView<PLACE, UserProfileView.Presenter>, NeedsAuthenticationContext {
}
