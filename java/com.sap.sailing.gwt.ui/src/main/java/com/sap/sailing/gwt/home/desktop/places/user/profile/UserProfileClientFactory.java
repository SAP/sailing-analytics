package com.sap.sailing.gwt.home.desktop.places.user.profile;

import com.sap.sailing.gwt.home.shared.usermanagement.WithAuthenticationManager;
import com.sap.sailing.gwt.ui.client.SailingClientFactory;
import com.sap.sailing.gwt.ui.client.refresh.ErrorAndBusyClientFactory;

public interface UserProfileClientFactory extends SailingClientFactory, ErrorAndBusyClientFactory, WithAuthenticationManager {
}
