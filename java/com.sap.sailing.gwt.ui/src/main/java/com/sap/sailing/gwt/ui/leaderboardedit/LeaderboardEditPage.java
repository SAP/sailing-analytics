package com.sap.sailing.gwt.ui.leaderboardedit;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.gwt.common.authentication.FixedSailingAuthentication;
import com.sap.sailing.gwt.common.authentication.SAPSailingHeaderWithAuthentication;
import com.sap.sailing.gwt.common.communication.routing.ProvidesLeaderboardRouting;
import com.sap.sailing.gwt.settings.client.leaderboard.EditableLeaderboardLifecycle;
import com.sap.sailing.gwt.settings.client.leaderboard.EditableLeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboardedit.EditableLeaderboardContextDefinition;
import com.sap.sailing.gwt.settings.client.utils.StoredSettingsLocationFactory;
import com.sap.sailing.gwt.ui.client.AbstractSailingWriteEntryPoint;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.gwt.client.shared.settings.OnSettingsLoadedCallback;
import com.sap.sse.gwt.settings.SettingsToUrlSerializer;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.authentication.decorator.AuthorizedContentDecorator;
import com.sap.sse.security.ui.authentication.decorator.WidgetFactory;
import com.sap.sse.security.ui.authentication.generic.GenericAuthentication;
import com.sap.sse.security.ui.authentication.generic.GenericAuthorizedContentDecorator;
import com.sap.sse.security.ui.authentication.generic.sapheader.SAPHeaderWithAuthentication;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;
import com.sap.sse.security.ui.settings.ComponentContextWithSettingsStorage;
import com.sap.sse.security.ui.settings.StoredSettingsLocation;

public class LeaderboardEditPage extends AbstractSailingWriteEntryPoint implements ProvidesLeaderboardRouting {
    
    private static final Logger logger = Logger.getLogger(LeaderboardEditPage.class.getName());
    public static final long DEFAULT_REFRESH_INTERVAL_MILLIS = 3000l;
    
    private String leaderboardName;
    private EditableLeaderboardContextDefinition editableLeaderboardContextDefinition;
    private SAPSailingHeaderWithAuthentication header;
    
    @Override
    protected void doOnModuleLoad() {
        super.doOnModuleLoad();
        this.editableLeaderboardContextDefinition = new SettingsToUrlSerializer()
                .deserializeFromCurrentLocation(new EditableLeaderboardContextDefinition());
        leaderboardName = editableLeaderboardContextDefinition.getLeaderboardName();
        getSailingService().getLeaderboardNames(new MarkedAsyncCallback<List<String>>(
                new GetLeaderboardNamesCallback()));
    }
    
    private class GetLeaderboardNamesCallback implements AsyncCallback<List<String>> {
        @Override
        public void onSuccess(List<String> leaderboardNames) {
            if (leaderboardNames.contains(leaderboardName)) {
                getSailingService().getAvailableDetailTypesForLeaderboard(leaderboardName, null, 
                        new GetAvailableDetailTypesForLeaderboardCallback());
            } else {
                RootPanel.get().add(new Label(getStringMessages().noSuchLeaderboard()));
            }
        }

        @Override
        public void onFailure(Throwable t) {
            reportError(getStringMessages().getLeaderboardNamesError());
            logger.log(Level.SEVERE, "Could not load detailtypes", t);
        }
    }
    
    private class GetAvailableDetailTypesForLeaderboardCallback implements AsyncCallback<Iterable<DetailType>> {
        @Override
        public void onFailure(Throwable caught) {
            reportError(getStringMessages().getAvailableDetailTypesForLeaderboardError());
            logger.log(Level.SEVERE, "Could not load detailtypes", caught);
        }

        @Override
        public void onSuccess(Iterable<DetailType> result) {
            SAPHeaderWithAuthentication header = initHeader();
            PaywallResolver paywallResolver = new PaywallResolverImpl(getUserService(), getSubscriptionServiceFactory());
            GenericAuthentication genericSailingAuthentication = new FixedSailingAuthentication(getUserService(),
                    paywallResolver, header.getAuthenticationMenuView());
            AuthorizedContentDecorator authorizedContentDecorator = new GenericAuthorizedContentDecorator(
                    genericSailingAuthentication);
            getSailingService().getLeaderboardWithSecurity(leaderboardName,
                    new GetLeaderboardWithSecurityCallback(authorizedContentDecorator, result, header));
        }

    }
    
    private class GetLeaderboardWithSecurityCallback implements AsyncCallback<StrippedLeaderboardDTO> {
        private final AuthorizedContentDecorator authorizedContentDecorator;
        private final Iterable<DetailType> getAvailableDetailTypesForLeaderboardResult;
        private final SAPHeaderWithAuthentication header;

        GetLeaderboardWithSecurityCallback(AuthorizedContentDecorator authorizedContentDecorator,
                Iterable<DetailType> getAvailableDetailTypesForLeaderboardResult,
                SAPHeaderWithAuthentication header) {
            this.authorizedContentDecorator = authorizedContentDecorator;
            this.getAvailableDetailTypesForLeaderboardResult = getAvailableDetailTypesForLeaderboardResult;
            this.header = header;
        }
        
        @Override
        public void onFailure(Throwable caught) {
            logger.log(Level.SEVERE, "Could not get leaderboard data.", caught);
            reportError(getStringMessages().getLeaderboardError());        
        }

        @Override
        public void onSuccess(
                StrippedLeaderboardDTO leaderboardWithSecurity) {
            if (leaderboardWithSecurity != null && leaderboardWithSecurity.getDisplayName() != null) {
                header.setHeaderTitle(getHeaderTitle(leaderboardWithSecurity.getDisplayName()));
            }
            final StoredSettingsLocation storageDefinition = StoredSettingsLocationFactory
                    .createStoredSettingsLocatorForEditableLeaderboard(editableLeaderboardContextDefinition);
            PaywallResolver paywallResolver = new PaywallResolverImpl(getUserService(), getSubscriptionServiceFactory());
            EditableLeaderboardLifecycle rootComponentLifeCycle = 
                    new EditableLeaderboardLifecycle(StringMessages.INSTANCE, leaderboardWithSecurity, 
                            getAvailableDetailTypesForLeaderboardResult, paywallResolver);
            ComponentContext<EditableLeaderboardSettings> context = 
                    new ComponentContextWithSettingsStorage<>(
                    rootComponentLifeCycle, getUserService(), storageDefinition);
            authorizedContentDecorator.setPermissionToCheck(
                    leaderboardWithSecurity, DefaultActions.UPDATE);
            context.getInitialSettings(new OnSettingsLoadedCallback<EditableLeaderboardSettings>() {
                @Override
                public void onSuccess(EditableLeaderboardSettings settings) {
                    authorizedContentDecorator
                    .setContentWidgetFactory(new WidgetFactory() {
                        @Override
                        public Widget get() {
                            EditableLeaderboardPanel leaderboardPanel = new EditableLeaderboardPanel(
                                    context, getSailingService(), new AsyncActionsExecutor(), leaderboardName, 
                                    /* leaderboardGroupName */ null, LeaderboardEditPage.this, getStringMessages(), 
                                    userAgent, getAvailableDetailTypesForLeaderboardResult, settings, LeaderboardEditPage.this,
                                    getUserService());
                            leaderboardPanel.ensureDebugId("EditableLeaderboardPanel");
                            return leaderboardPanel;
                        }
                    });
                    DockLayoutPanel mainPanel = new DockLayoutPanel(Unit.PX);
                    RootLayoutPanel.get().add(mainPanel);
                    mainPanel.addNorth(header, 75);
                    mainPanel.add(new ScrollPanel(authorizedContentDecorator));
                }
                
                @Override
                public void onError(Throwable caught,
                        EditableLeaderboardSettings fallbackDefaultSettings) {
                    logger.log(Level.SEVERE, "Could not get editable leaderboard settings.", caught);
                    reportError(getStringMessages().settingsGetError());                    
                }
            });
        }
        
    }

    private SAPHeaderWithAuthentication initHeader() {
        header = new SAPSailingHeaderWithAuthentication(getHeaderTitle(leaderboardName));
        header.getElement().getStyle().setWidth(100, Unit.PCT);
        return header;
    }

    @Override
    public String getLeaderboardName() {
        return leaderboardName;
    }

    private String getHeaderTitle(String leaderboardDisplayName) {
        return leaderboardDisplayName + ": " + getStringMessages().editScores();
    }

}
