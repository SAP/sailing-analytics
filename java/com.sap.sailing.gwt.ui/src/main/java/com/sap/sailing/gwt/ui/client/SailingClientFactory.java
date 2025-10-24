package com.sap.sailing.gwt.ui.client;

import com.sap.sailing.gwt.common.communication.routing.ProvidesLeaderboardRouting;
import com.sap.sailing.gwt.home.desktop.places.user.profile.WithSailingService;
import com.sap.sse.gwt.client.mvp.ClientFactory;
import com.sap.sse.gwt.client.mvp.ErrorView;
import com.sap.sse.security.ui.client.WithSecurity;

/**
 * A client factory that can deliver the sailing-specific services.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface SailingClientFactory extends ClientFactory, WithSecurity, WithSailingService {
    SailingServiceAsync getSailingService(ProvidesLeaderboardRouting routingProvider);

    MediaServiceAsync getMediaService();

    MediaServiceWriteAsync getMediaServiceWrite();

    ErrorView createErrorView(String errorMessage, Throwable errorReason);
}
