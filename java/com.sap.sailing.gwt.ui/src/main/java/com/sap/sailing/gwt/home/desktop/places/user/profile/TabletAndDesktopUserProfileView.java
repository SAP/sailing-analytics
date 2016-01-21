package com.sap.sailing.gwt.home.desktop.places.user.profile;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabPanel;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabPanelPlaceSelectionEvent;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabView;
import com.sap.sailing.gwt.common.client.i18n.TextMessages;
import com.sap.sailing.gwt.home.desktop.partials.userHeader.UserHeader;
import com.sap.sailing.gwt.home.shared.app.ApplicationHistoryMapper;
import com.sap.sailing.gwt.home.shared.places.user.profile.AbstractUserProfilePlace;
import com.sap.sailing.gwt.home.shared.usermanagement.app.AuthenticationContext;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class TabletAndDesktopUserProfileView extends Composite implements UserProfileView<AbstractUserProfilePlace, UserProfileView.Presenter> {
    private static final ApplicationHistoryMapper historyMapper = GWT.<ApplicationHistoryMapper> create(ApplicationHistoryMapper.class);

    private static MyBinder uiBinder = GWT.create(MyBinder.class);

    private UserProfileView.Presenter currentPresenter;

    interface MyBinder extends UiBinder<Widget, TabletAndDesktopUserProfileView> {
    }

    @UiField StringMessages i18n;
    
    @UiField(provided = true)
    TabPanel<AbstractUserProfilePlace, UserProfileView.Presenter, UserProfileTabView<AbstractUserProfilePlace>> tabPanelUi;
    
    @UiField(provided = true)
    UserHeader headerUi;

    public TabletAndDesktopUserProfileView() {
    }

    @Override
    public void registerPresenter(final UserProfileView.Presenter currentPresenter) {
        this.currentPresenter = currentPresenter;
        tabPanelUi = new TabPanel<>(currentPresenter, historyMapper);
        
        headerUi = new UserHeader();
        
        initWidget(uiBinder.createAndBindUi(this));
    }

    @Override
    public void navigateTabsTo(AbstractUserProfilePlace place) {
        tabPanelUi.activatePlace(place);
        StringBuilder titleBuilder = new StringBuilder(TextMessages.INSTANCE.sapSailing()).append(" - ");

        titleBuilder.append(place.getLocationTitle());

        String currentTabTitle = tabPanelUi.getCurrentTabTitle();
        if (currentTabTitle != null && !currentTabTitle.isEmpty()) {
            titleBuilder.append(" - ").append(currentTabTitle);
        }
        Window.setTitle(titleBuilder.toString());
    }
    
    @Override
    public void setUserManagementContext(AuthenticationContext userManagementContext) {
        headerUi.setUserManagementContext(userManagementContext);
        tabPanelUi.getCurrentTab().setUserManagementContext(userManagementContext);
    }

    @SuppressWarnings("unchecked")
    @UiHandler("tabPanelUi")
    public void onTabSelection(TabPanelPlaceSelectionEvent e) {
        currentPresenter.handleTabPlaceSelection((TabView<?, UserProfileView.Presenter>) e.getSelectedActivity());
    }
    
    @Override
    public void showErrorInCurrentTab(IsWidget errorView) {
        tabPanelUi.overrideCurrentContentInTab(errorView);
    }
}
