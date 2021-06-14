package com.sap.sailing.gwt.home.desktop.places.user.profile;

import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.ClientFactoryWithDispatchAndErrorAndUserService;
import com.sap.sailing.gwt.ui.client.SailingClientFactory;
import com.sap.sse.security.ui.authentication.WithAuthenticationManager;
import com.sap.sse.security.ui.client.subscription.WithSubscriptionService;

public interface UserProfileClientFactory extends SailingClientFactory, WithAuthenticationManager,
        ClientFactoryWithDispatchAndErrorAndUserService, WithSubscriptionService {
}
