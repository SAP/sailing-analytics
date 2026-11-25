package com.sap.sailing.gwt.managementconsole.app;

import static com.sap.sse.security.ui.authentication.AuthenticationPlaces.SIGN_IN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.ui.HasVisibility;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.managementconsole.partials.header.Header;
import com.sap.sailing.gwt.managementconsole.partials.header.Header.ActivatableMenuItem;
import com.sap.sailing.gwt.managementconsole.partials.mainframe.MainFrame;
import com.sap.sailing.gwt.managementconsole.places.AbstractManagementConsolePlace;
import com.sap.sailing.gwt.managementconsole.places.dashboard.DashboardPlace;
import com.sap.sailing.gwt.managementconsole.places.event.create.CreateEventPlace;
import com.sap.sailing.gwt.managementconsole.places.event.edit.EditEventPlace;
import com.sap.sailing.gwt.managementconsole.places.event.media.EventMediaPlace;
import com.sap.sailing.gwt.managementconsole.places.event.overview.EventOverviewPlace;
import com.sap.sailing.gwt.managementconsole.places.eventseries.create.CreateEventSeriesPlace;
import com.sap.sailing.gwt.managementconsole.places.eventseries.events.EventSeriesEventsPlace;
import com.sap.sailing.gwt.managementconsole.places.eventseries.overview.EventSeriesOverviewPlace;
import com.sap.sailing.gwt.managementconsole.places.regatta.create.AddRegattaPlace;
import com.sap.sailing.gwt.managementconsole.places.regatta.overview.RegattaOverviewPlace;
import com.sap.sailing.gwt.managementconsole.places.showcase.ShowcasePlace;
import com.sap.sailing.gwt.managementconsole.resources.ManagementConsoleResources;
import com.sap.sailing.gwt.ui.client.AbstractSailingWriteEntryPoint;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.ui.authentication.AuthenticationContextEvent;
import com.sap.sse.security.ui.authentication.AuthenticationRequestEvent;
import com.sap.sse.security.ui.authentication.app.AuthenticationContext;
import com.sap.sse.security.ui.client.UserService;

public class ManagementConsoleEntryPoint extends AbstractSailingWriteEntryPoint {

    private final StringMessages msg = StringMessages.INSTANCE;
    private final List<MenuItemConfig> menuItemConfigs = new ArrayList<>();

    @Override
    protected void doOnModuleLoad() {
        SharedResources.INSTANCE.mediaCss().ensureInjected();
        ManagementConsoleResources.INSTANCE.fonts().ensureInjected();
        ManagementConsoleResources.INSTANCE.icons().ensureInjected();
        ManagementConsoleResources.INSTANCE.style().ensureInjected();

        super.doOnModuleLoad();
        final EventBus eventBus = new SimpleEventBus();
        final SailingServiceWriteAsync service = getSailingService();
        final ManagementConsoleClientFactory clientFactory = new ManagementConsoleClientFactoryImpl(eventBus, service);
        final MainFrame mainFrame = new MainFrame(() -> eventBus.fireEvent(new AuthenticationRequestEvent(SIGN_IN)));
        eventBus.addHandler(AuthenticationContextEvent.TYPE, event -> {
            mainFrame.setAuthenticationContext(event.getCtx());
            menuItemConfigs.forEach(c -> c.validate(event.getCtx()));
        });
        initMenuItems(clientFactory, eventBus, mainFrame.getHeader());
        initActivitiesAndPlaces(clientFactory, eventBus, mainFrame);
    }

    private void initActivitiesAndPlaces(final ManagementConsoleClientFactory clientFactory, final EventBus eventBus,
            final MainFrame mainFrame) {
        final ManagementConsolePlaceHistoryMapper historyMapper = GWT.create(ManagementConsolePlaceHistoryMapper.class);
        final PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);
        historyHandler.register(clientFactory.getPlaceController(), eventBus, new DashboardPlace());

        final ManagementConsoleActivityMapper activityMapper = new ManagementConsoleActivityMapper(clientFactory);
        final ActivityManager activityManager = new ActivityManager(activityMapper, eventBus);

        activityManager.setDisplay(mainFrame.getContentContainer());
        RootLayoutPanel.get().add(mainFrame);

        historyHandler.handleCurrentHistory();
    }

    private void initMenuItems(final ManagementConsoleClientFactory clientFactory, final EventBus eventBus,
            final Header header) {
        addMenuItem(clientFactory, header, "(SHOWCASE)", ShowcasePlace::new);
        addMenuItem(clientFactory, header, "Dashboard", DashboardPlace::new)
                .matchesPlaces(DashboardPlace.class);

        addMenuItem(clientFactory, header, msg.series(), EventSeriesOverviewPlace::new)
                .permission(SecuredDomainType.EVENT, DefaultActions.MUTATION_ACTIONS)
                .matchesPlaces(EventSeriesOverviewPlace.class, CreateEventSeriesPlace.class,
                        EventSeriesEventsPlace.class);

        addMenuItem(clientFactory, header, msg.events(), EventOverviewPlace::new)
                .permission(SecuredDomainType.EVENT, DefaultActions.MUTATION_ACTIONS)
                .matchesPlaces(EventOverviewPlace.class, CreateEventPlace.class, EditEventPlace.class,
                        EventMediaPlace.class, AddRegattaPlace.class, RegattaOverviewPlace.class);

        // addMenuItem(clientFactory, header, msg.deviceConfiguration(), ShowcasePlace::new)
        // .permission(SecuredDomainType.RACE_MANAGER_APP_DEVICE_CONFIGURATION, DefaultActions.MUTATION_ACTIONS);

        // addMenuItem(clientFactory, header, msg.connectors(), ShowcasePlace::new)
        // .permission(SecuredDomainType.TRACTRAC_ACCOUNT, DefaultActions.values())
        // .permission(SecuredDomainType.SWISS_TIMING_ARCHIVE_ACCOUNT, DefaultActions.values())
        // .permission(SecuredDomainType.SWISS_TIMING_ACCOUNT, DefaultActions.values())
        // .permission(SecuredDomainType.LEADERBOARD, DefaultActions.UPDATE, DefaultActions.DELETE)
        // .permission(SecuredDomainType.IGTIMI_ACCOUNT, DefaultActions.values())
        // .permission(SecuredDomainType.EXPEDITION_DEVICE_CONFIGURATION, DefaultActions.values())
        // .permission(SecuredDomainType.RESULT_IMPORT_URL, DefaultActions.values())
        // .permission(SecuredDomainType.REGATTA, DefaultActions.CREATE);

        // addMenuItem(clientFactory, header, msg.courseCreation(), ShowcasePlace::new)
        // .permission(SecuredDomainType.MARK_TEMPLATE, DefaultActions.MUTATION_ACTIONS)
        // .permission(SecuredDomainType.MARK_PROPERTIES, DefaultActions.MUTATION_ACTIONS)
        // .permission(SecuredDomainType.COURSE_TEMPLATE, DefaultActions.MUTATION_ACTIONS)
        // .permission(SecuredDomainType.MARK_ROLE, DefaultActions.MUTATION_ACTIONS);

        addMenuItem(clientFactory, header, msg.advanced(), ShowcasePlace::new)
                .anyServerPermission(ServerActions.REPLICATE, ServerActions.START_REPLICATION, ServerActions.READ_REPLICATOR)
                .anyServerPermission(ServerActions.CONFIGURE_LOCAL_SERVER, DefaultActions.CHANGE_OWNERSHIP, DefaultActions.CHANGE_ACL)
                .serverPermission(ServerActions.CAN_IMPORT_MASTERDATA)
                .serverPermission(ServerActions.CONFIGURE_REMOTE_INSTANCES)
                .serverPermission(ServerActions.CONFIGURE_FILE_STORAGE)
                .permission(SecuredSecurityTypes.USER, DefaultActions.MUTATION_ACTIONS)
                .permission(SecuredSecurityTypes.ROLE_DEFINITION, DefaultActions.MUTATION_ACTIONS)
                .permission(SecuredSecurityTypes.USER_GROUP, DefaultActions.MUTATION_ACTIONS);

        initMenuItem(clientFactory, header.initUserDetailsItem(
                // TODO: Temporary! Replace with account management navigation as soon as specified
                event -> clientFactory.getAuthenticationManager().logout()));
        initMenuItem(clientFactory, header.initSignOutItem(event -> clientFactory.getAuthenticationManager().logout()));
        eventBus.addHandler(PlaceChangeEvent.TYPE,
                event -> menuItemConfigs.forEach(c -> c.updateActiveState(event.getNewPlace())));
    }

    private MenuItemConfig addMenuItem(final ManagementConsoleClientFactory clientFactory, final Header header,
            final String text, final Supplier<AbstractManagementConsolePlace> targetPlace) {

        final ClickHandler navigation = event -> clientFactory.getPlaceController().goTo(targetPlace.get());
        final ActivatableMenuItem menuItem = header.addMenuItem(text, navigation);
        final MenuItemConfig config = new MenuItemConfig(clientFactory.getUserService(), menuItem);
        this.menuItemConfigs.add(config);
        return config;
    }

    private MenuItemConfig initMenuItem(final ManagementConsoleClientFactory clientFactory, final HasVisibility item) {
        final MenuItemConfig config = new MenuItemConfig(clientFactory.getUserService(), item);
        this.menuItemConfigs.add(config);
        return config;
    }

    private class MenuItemConfig {

        private final Set<Predicate<AuthenticationContext>> permissions = new HashSet<>();
        private final Set<Class<? extends Place>> activePlaces = new HashSet<>();
        private final UserService userService;
        private final HasVisibility menuItem;
        private final ActivatableMenuItem activatableMenuItem;

        private MenuItemConfig(final UserService userService, final ActivatableMenuItem menuItem) {
            this.userService = userService;
            this.menuItem = menuItem;
            this.activatableMenuItem = menuItem;
        }

        private MenuItemConfig(final UserService userService, final HasVisibility menuItem) {
            this.userService = userService;
            this.menuItem = menuItem;
            this.activatableMenuItem = null;
        }

        private MenuItemConfig permission(final HasPermissions type, final Action... actions) {
            permission(ctx -> type.getPermission(actions));
            return this;
        }

        private MenuItemConfig serverPermission(final Action action) {
            permission(ctx -> SecuredSecurityTypes.SERVER.getPermissionForObject(action, userService.getServerInfo()));
            return this;
        }

        private MenuItemConfig permission(final Function<AuthenticationContext, WildcardPermission> permissionFactory) {
            permissions.add(ctx -> userService.hasCurrentUserAnyPermission(permissionFactory.apply(ctx), null));
            return this;
        }

        private MenuItemConfig anyServerPermission(final Action... actions) {
            permissions.add(ctx -> userService.hasAnyServerPermission(actions));
            return this;
        }

        @SafeVarargs
        private final MenuItemConfig matchesPlaces(final Class<? extends Place>... places) {
            activePlaces.addAll(Arrays.asList(places));
            return this;
        }

        private void updateActiveState(final Place currentPlace) {
            final boolean active = activatableMenuItem != null && currentPlace != null
                    && activePlaces.contains(currentPlace.getClass());
            if (activatableMenuItem != null) {
                activatableMenuItem.setActive(active);
            }
        }

        private void validate(final AuthenticationContext ctx) {
            menuItem.setVisible(
                    ctx.isLoggedIn() && (permissions.isEmpty() || permissions.stream().anyMatch(p -> p.test(ctx))));
        }

    }

}