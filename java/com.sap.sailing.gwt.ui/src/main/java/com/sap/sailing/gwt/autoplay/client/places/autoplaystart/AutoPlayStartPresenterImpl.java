package com.sap.sailing.gwt.autoplay.client.places.autoplaystart;

import java.util.List;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.configs.AutoPlayConfiguration;
import com.sap.sailing.gwt.autoplay.client.configs.AutoPlayConfiguration.OnSettingsCallback;
import com.sap.sailing.gwt.autoplay.client.configs.AutoPlayContextDefinition;
import com.sap.sailing.gwt.autoplay.client.configs.AutoPlayContextDefinitionImpl;
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayFailureEvent;
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayHeaderEvent;
import com.sap.sailing.gwt.autoplay.client.utils.AutoplayHelper;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.event.LocaleChangeEvent;
import com.sap.sse.gwt.client.event.LocaleChangeEventHandler;
import com.sap.sse.gwt.client.shared.perspective.PerspectiveCompositeSettings;
import com.sap.sse.gwt.settings.SettingsToUrlSerializer;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.subscription.SubscriptionServiceFactory;

public class AutoPlayStartPresenterImpl extends AbstractActivity implements AutoPlayStartView.Presenter {
    public static final String LOAD_EVENTS_DATA_CATEGORY = "loadEventsData";
    private final AutoPlayClientFactory clientFactory;

    private AutoPlayStartView view;
    private EventBus eventBus;

    public AutoPlayStartPresenterImpl(AutoPlayStartPlace place, AutoPlayClientFactory clientFactory,
            AutoPlayStartView view) {
        this.clientFactory = clientFactory;
        this.view = view;
    }

    @Override
    public void start(final AcceptsOneWidget panel, EventBus eventBus) {
        // check if config can be restored!
        SettingsToUrlSerializer serializer = new SettingsToUrlSerializer();
        AutoPlayContextDefinitionImpl apcd = new AutoPlayContextDefinitionImpl();
        serializer.deserializeFromCurrentLocation(apcd);
        if (apcd.getEventId() != null) {
            view.showLoading();
            clientFactory.getSailingService().getEventById(apcd.getEventId(), true, new AsyncCallback<EventDTO>() {
                @Override
                public void onSuccess(final EventDTO event) {
                    StrippedLeaderboardDTO leaderBoard = AutoplayHelper.getSelectedLeaderboard(event,
                            apcd.getLeaderboardName());
                    apcd.getType().getConfig().loadSettingsDefault(apcd, leaderBoard,
                            clientFactory.getUserService(), clientFactory.getSubscriptionServiceFactory(),
                            new OnSettingsCallback() {
                                @Override
                                public void newSettings(PerspectiveCompositeSettings<?> newSettings,
                                        String urlWithSettings) {
                                    if (newSettings != null) {
                                        serializer.deserializeSettingsMapFromCurrentLocation(newSettings);
                                    }
                                    startRootNode(apcd, newSettings, event);
                                }
                            });
                }
                @Override
                public void onFailure(Throwable caught) {
                    clientFactory.getEventBus().fireEvent(
                            new AutoPlayFailureEvent(caught, "Error loading Event with id " + apcd.getEventId()));
                }
            });
            panel.setWidget(new Label(StringMessages.INSTANCE.loading()));
            eventBus.fireEvent(new AutoPlayHeaderEvent(StringMessages.INSTANCE.loading(), ""));
        } else {
            this.eventBus = eventBus;
            clientFactory.getSailingService()
                    .getEvents(new MarkedAsyncCallback<List<EventDTO>>(new AsyncCallback<List<EventDTO>>() {

                        @Override
                        public void onSuccess(List<EventDTO> result) {
                            panel.setWidget(view.asWidget());

                            view.asWidget().ensureDebugId("AutoPlayStartView");
                            view.setEvents(result);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                        }
                    }, LOAD_EVENTS_DATA_CATEGORY));

            eventBus.addHandler(LocaleChangeEvent.TYPE, new LocaleChangeEventHandler() {
                @Override
                public void onLocaleChange(final LocaleChangeEvent event) {
                    UrlBuilder urlBuilder = Window.Location.createUrlBuilder();
                    urlBuilder.removeParameter("locale");
                    urlBuilder.setParameter("locale", event.getNewLocaleID());
                    Window.Location.replace(urlBuilder.buildString());
                }
            });
            view.setCurrentPresenter(this);
            eventBus.fireEvent(new AutoPlayHeaderEvent(StringMessages.INSTANCE.autoplayConfiguration(), ""));
        }
    }

    @Override
    public void handleLocaleChange(String selectedLocale) {
        LocaleChangeEvent localeChangeEvent = new LocaleChangeEvent(selectedLocale);
        eventBus.fireEvent(localeChangeEvent);
    }

    @Override
    public void startRootNode(AutoPlayContextDefinition ctxDef, PerspectiveCompositeSettings<?> settings,
            EventDTO initialEventData) {
        AutoPlayConfiguration autoPlayConfiguration = ctxDef.getType().getConfig();
        autoPlayConfiguration.startRootNode(clientFactory, ctxDef, settings, initialEventData);
    }

    @Override
    public UserService getUserService() {
        return clientFactory.getUserService();
    }

    @Override
    public SubscriptionServiceFactory getSubscriptionServiceFactory() {
        return clientFactory.getSubscriptionServiceFactory();
    }
}
