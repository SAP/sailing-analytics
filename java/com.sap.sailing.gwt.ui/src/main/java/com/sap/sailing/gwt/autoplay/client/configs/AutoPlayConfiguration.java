package com.sap.sailing.gwt.autoplay.client.configs;

import com.google.gwt.http.client.UrlBuilder;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.shared.perspective.PerspectiveCompositeSettings;
import com.sap.sse.gwt.settings.SettingsToUrlSerializer;
import com.sap.sse.gwt.settings.UrlBuilderUtil;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.subscription.SubscriptionServiceFactory;

public abstract class AutoPlayConfiguration {
    public abstract void startRootNode(AutoPlayClientFactory cf, AutoPlayContextDefinition context,
            PerspectiveCompositeSettings<?> settings, EventDTO initialEventData);

    /**
     * This method should be pure/standalone, it is not allowed to make any guesses on fields written by startRootNode,
     * as it can be called before that
     */
    public void openSettingsDialog(EventDTO selectedEvent, StrippedLeaderboardDTO leaderboard,
            OnSettingsCallback holder, PerspectiveCompositeSettings<?> settings, AutoPlayContextDefinition apcd,
            UserService userService, SubscriptionServiceFactory subscriptionServiceFactory,
            PaywallResolver leaderboarPaywallResolver) {
        Notification.notify("This configuration does not have settings", NotificationType.WARNING);
    }

    /**
     * This method should be pure/standalone, it is not allowed to make any guesses on fields written by startRootNode,
     * as it can be called before that
     */
    public void loadSettingsDefault(AutoPlayContextDefinition apcd,
            StrippedLeaderboardDTO leaderboard, UserService userService, SubscriptionServiceFactory subscriptionServiceFactory, OnSettingsCallback holder) {
        holder.newSettings(null, getUrlWithSettings(apcd, null));
    }
    
    private String getUrlWithSettings(AutoPlayContextDefinition apcd, PerspectiveCompositeSettings<?> settings) {
        UrlBuilder urlBuilder = UrlBuilderUtil.createUrlBuilderFromCurrentLocationWithCleanParameters();
        SettingsToUrlSerializer urlSerializer = new SettingsToUrlSerializer();
        if (settings != null) {
            urlSerializer.serializeSettingsMapToUrlBuilder(settings, urlBuilder);
        }
        urlSerializer.serializeToUrlBuilder(apcd, urlBuilder);
        return urlBuilder.buildString();
    }

    public interface OnSettingsCallback {
        void newSettings(PerspectiveCompositeSettings<?> newSettings, String urlWithSettings);
    }
}
