package com.sap.sailing.gwt.autoplay.client.places.autoplaystart;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.autoplay.client.configs.AutoPlayConfiguration.OnSettingsCallback;
import com.sap.sailing.gwt.autoplay.client.configs.AutoPlayContextDefinitionImpl;
import com.sap.sailing.gwt.autoplay.client.configs.AutoPlayType;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.GWTLocaleUtil;
import com.sap.sse.gwt.client.shared.perspective.PerspectiveCompositeSettings;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

public class AutoPlayStartViewImpl extends Composite implements AutoPlayStartView {
    private static StartPageViewUiBinder uiBinder = GWT.create(StartPageViewUiBinder.class);

    interface StartPageViewUiBinder extends UiBinder<Widget, AutoPlayStartViewImpl> {
    }

    @UiField(provided = true)
    ListBox localeSelectionBox;
    @UiField(provided = true)
    ListBox eventSelectionBox;
    @UiField(provided = true)
    ListBox configurationSelectionBox;
    @UiField(provided = true)
    ListBox leaderboardSelectionBox;
    @UiField
    Button startAutoPlayButton;
    @UiField
    Button settingsButton;
    @UiField
    DivElement leaderboardSelectionUi;
    @UiField
    Anchor configStarter;

    private final List<EventDTO> events;

    private AutoPlayType selectedAutoPlayType = null;
    private EventDTO selectedEvent;
    private StrippedLeaderboardDTO selectedLeaderboard;
    private Presenter currentPresenter;
    private AutoPlayContextDefinitionImpl apcd;
    private PerspectiveCompositeSettings<?> settings;
    private String configuratedUrl;

    public AutoPlayStartViewImpl() {
        super();
        this.events = new ArrayList<EventDTO>();
        eventSelectionBox = new ListBox();
        eventSelectionBox.setMultipleSelect(false);
        eventSelectionBox.ensureDebugId("eventSelectionBox");
        leaderboardSelectionBox = new ListBox();
        leaderboardSelectionBox.setMultipleSelect(false);
        leaderboardSelectionBox.ensureDebugId("leaderboardSelectionBox");
        localeSelectionBox = new ListBox();
        localeSelectionBox.setMultipleSelect(false);
        configurationSelectionBox = new ListBox();
        configurationSelectionBox.addItem("--", "");
        configurationSelectionBox.ensureDebugId("configurationSelectionBox");

        for (AutoPlayType apt : AutoPlayType.values()) {
            configurationSelectionBox.addItem(apt.getName(), apt.name());
        }
        LocaleInfo currentLocale = LocaleInfo.getCurrentLocale();
        int i = 0;
        for (String localeName : GWTLocaleUtil.getAvailableLocales()) {
            String displayName = GWTLocaleUtil.getDecoratedLanguageDisplayNameWithDefaultLocaleSupport(localeName);
            localeSelectionBox.addItem(displayName, localeName);
            if (currentLocale.getLocaleName().equals(localeName)) {
                localeSelectionBox.setSelectedIndex(i);
            }
            i++;
        }
        initWidget(uiBinder.createAndBindUi(this));
        this.ensureDebugId("AutoPlayStartView");

        configStarter.getElement().getStyle().setDisplay(Display.BLOCK);
        configStarter.ensureDebugId("startURL");

        validate();
    }

    @Override
    public void setCurrentPresenter(Presenter currentPresenter) {
        this.currentPresenter = currentPresenter;
    }

    @Override
    public void setEvents(List<EventDTO> events) {
        this.events.clear();
        this.events.addAll(events);
        eventSelectionBox.addItem(StringMessages.INSTANCE.pleaseSelectAnEvent());
        for (EventDTO event : Util.sortNamedCollection(events)) {
            eventSelectionBox.addItem(event.getName());
        }
    }

    @UiHandler("configurationSelectionBox")
    void onConfigChange(ChangeEvent event) {
        String selectedConfigType = configurationSelectionBox.getSelectedValue();
        this.selectedAutoPlayType = AutoPlayType.valueOf(selectedConfigType);
        validate();
    }

    @UiHandler("eventSelectionBox")
    void onEventSelectionChange(ChangeEvent event) {
        this.selectedEvent = getSelectedEvent();
        if (this.selectedEvent != null) {
            leaderboardSelectionBox.clear();
            leaderboardSelectionBox.addItem(StringMessages.INSTANCE.selectALeaderboard());
            for (LeaderboardGroupDTO leaderboardGroup : selectedEvent.getLeaderboardGroups()) {
                for (StrippedLeaderboardDTO leaderboard : leaderboardGroup.getLeaderboards()) {
                    leaderboardSelectionBox.addItem(leaderboard.getName());
                }
            }
        }
        validate();
    }

    @UiHandler("leaderboardSelectionBox")
    void onLeaderboardSelectionChange(ChangeEvent event) {
        this.selectedLeaderboard = getSelectedLeaderboard();
        validate();
    }

    @UiHandler("settingsButton")
    void onOpenSettings(ClickEvent event) {
        PaywallResolver paywallResolver = new PaywallResolverImpl(currentPresenter.getUserService(),
                currentPresenter.getSubscriptionServiceFactory());
        selectedAutoPlayType.getConfig().openSettingsDialog(selectedEvent, selectedLeaderboard,
                new OnSettingsCallback() {
                    @Override
                    public void newSettings(PerspectiveCompositeSettings<?> newSettings, String urlWithSettings) {
                        settings = newSettings;
                        updateURL(urlWithSettings);
                    }
                }, settings, apcd, currentPresenter.getUserService(), currentPresenter.getSubscriptionServiceFactory(),
                paywallResolver);
    }

    private boolean validate() {
        boolean readyToGo = true;
        if (readyToGo && selectedAutoPlayType == null) {
            readyToGo = false;
            eventSelectionBox.setEnabled(false);
            eventSelectionBox.getElement().getStyle().setOpacity(0.2);
        } else {
            eventSelectionBox.setEnabled(true);
            eventSelectionBox.getElement().getStyle().setOpacity(1);
        }

        EventDTO selectedEvent = getSelectedEvent();
        if (selectedEvent == null) {
            readyToGo = false;
            leaderboardSelectionBox.setEnabled(false);
            leaderboardSelectionBox.getElement().getStyle().setOpacity(0.2);
        } else {
            leaderboardSelectionBox.setEnabled(true);
            leaderboardSelectionBox.getElement().getStyle().setOpacity(1);
        }

        String selectedLeaderboardName = getSelectedLeaderboardName();
        if (readyToGo && selectedLeaderboardName == null)
            readyToGo = false;

        startAutoPlayButton.setEnabled(readyToGo);
        settingsButton.setEnabled(readyToGo);
        if (!readyToGo) {
            configuratedUrl = null;
            startAutoPlayButton.addStyleName(SharedResources.INSTANCE.mainCss().buttoninactive());
            settingsButton.addStyleName(SharedResources.INSTANCE.mainCss().buttoninactive());
            configStarter.setText(StringMessages.INSTANCE.invalidSelection());
            configStarter.setEnabled(false);
        } else {
            configStarter.setEnabled(true);
            configStarter.setText(StringMessages.INSTANCE.loading());
            settingsButton.removeStyleName(SharedResources.INSTANCE.mainCss().buttoninactive());
            startAutoPlayButton.removeStyleName(SharedResources.INSTANCE.mainCss().buttoninactive());
            apcd = new AutoPlayContextDefinitionImpl(selectedAutoPlayType, selectedEvent.id, selectedLeaderboardName);
            apcd.getType().getConfig().loadSettingsDefault(apcd, selectedLeaderboard, currentPresenter.getUserService(),
                    currentPresenter.getSubscriptionServiceFactory(),
                    new OnSettingsCallback() {
                        @Override
                        public void newSettings(PerspectiveCompositeSettings<?> newSettings, String urlWithSettings) {
                            settings = newSettings;
                            updateURL(urlWithSettings);
                        }
                    });
        }

        return readyToGo;
    }

    private void updateURL(String urlWithSettings) {
        configuratedUrl = urlWithSettings;
        configStarter.setText(urlWithSettings);
        configStarter.setHref(urlWithSettings);
    }

    @UiHandler("localeSelectionBox")
    void onLocaleSelectionChange(ChangeEvent event) {
        currentPresenter.handleLocaleChange(getSelectedLocale());
    }

    @UiHandler("startAutoPlayButton")
    void startAutoPlayClicked(ClickEvent event) {
        settingsButton.setEnabled(false);
        startAutoPlayButton.addStyleName(SharedResources.INSTANCE.mainCss().buttoninactive());
        settingsButton.addStyleName(SharedResources.INSTANCE.mainCss().buttoninactive());
        Window.Location.assign(configuratedUrl);
    }

    private String getSelectedLocale() {
        String result = null;
        int selectedIndex = localeSelectionBox.getSelectedIndex();
        if (selectedIndex >= 0) {
            String selectedLocale = localeSelectionBox.getValue(selectedIndex);
            for (String localeName : GWTLocaleUtil.getAvailableLocales()) {
                if (selectedLocale.equals(localeName)) {
                    result = localeName;
                    break;
                }
            }
        }
        return result;
    }

    private String getSelectedLeaderboardName() {
        String result = null;
        int selectedIndex = leaderboardSelectionBox.getSelectedIndex();
        if (selectedIndex > 0) {
            result = leaderboardSelectionBox.getItemText(selectedIndex);
        }
        return result;
    }

    private StrippedLeaderboardDTO getSelectedLeaderboard() {
        EventDTO selectedEvent = getSelectedEvent();
        String selectedLeaderboardName = getSelectedLeaderboardName();
        for (LeaderboardGroupDTO leaderboardGroup : selectedEvent.getLeaderboardGroups()) {
            for (StrippedLeaderboardDTO leaderboard : leaderboardGroup.getLeaderboards()) {
                if (leaderboard.getName().equals(selectedLeaderboardName)) {
                    return leaderboard;
                }
            }
        }
        return null;
    }

    private EventDTO getSelectedEvent() {
        EventDTO result = null;
        int selectedIndex = eventSelectionBox.getSelectedIndex();
        if (events != null && selectedIndex > 0) {
            String selectedItemText = eventSelectionBox.getValue(selectedIndex);
            for (EventDTO event : events) {
                if (event.getName().equals(selectedItemText)) {
                    result = event;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public void showLoading() {
        startAutoPlayButton.setEnabled(false);
    }
}
