package com.sap.sailing.gwt.managementconsole.partials.header;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasVisibility;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.managementconsole.partials.mainframe.MainFrame;
import com.sap.sailing.gwt.managementconsole.resources.ManagementConsoleResources;
import com.sap.sse.gwt.client.controls.dropdown.DropdownHandler;
import com.sap.sse.security.ui.authentication.app.AuthenticationContext;
import com.sap.sse.security.ui.authentication.app.NeedsAuthenticationContext;

/**
 * Header component for management console application {@link MainFrame main frame} containing the primary navigation
 * items, which are shown tab-like on desktop resolutions and as burger menu on smaller / mobile screens.
 */
public class Header extends Composite implements NeedsAuthenticationContext {

    interface HeaderUiBinder extends UiBinder<Widget, Header> {
    }

    /**
     * A navigation menu item whose active (currently-selected) state can be toggled so the user can see which top-level
     * category is presently shown.
     */
    public interface ActivatableMenuItem extends HasVisibility {
        void setActive(boolean active);
    }

    private static HeaderUiBinder uiBinder = GWT.create(HeaderUiBinder.class);

    @UiField
    ManagementConsoleResources app_res;

    @UiField
    HeaderResources local_res;

    @UiField
    AnchorElement logoAnchor, navigationAnchor;

    @UiField
    Element actions, mobileMenu;

    @UiField
    FlowPanel desktopMenu, mobileMenuActions;

    @UiField
    Anchor userDetails, userDetailsMobile, signOutMobile;

    private final DropdownHandler dropdownHandler;

    public Header() {
        initWidget(uiBinder.createAndBindUi(this));
        local_res.style().ensureInjected();

        this.dropdownHandler = new DropdownHandler(navigationAnchor, mobileMenu) {
            @Override
            protected void dropdownStateChanged(final boolean dropdownShown) {
                setStyleName(mobileMenu, local_res.style().active(), dropdownShown);
            }
        };
    }

    @Override
    public void setAuthenticationContext(final AuthenticationContext authenticationContext) {
        setVisible(actions, authenticationContext.isLoggedIn());
    }

    public ActivatableMenuItem addMenuItem(final String text, final ClickHandler handler) {
        final Anchor desktopItem = createMenuItem(text);
        desktopItem.addClickHandler(handler);
        desktopMenu.add(desktopItem);

        final Anchor mobileItem = createMenuItem(text);
        mobileItem.addClickHandler(new CloseMenuClickHandler(handler));
        mobileMenuActions.add(mobileItem);

        return new MenuItem(desktopItem, mobileItem);
    }

    public HasVisibility initUserDetailsItem(final ClickHandler handler) {
        userDetails.addClickHandler(handler);
        userDetailsMobile.addClickHandler(new CloseMenuClickHandler(handler));
        return new MenuItem(userDetails, userDetailsMobile);
    }

    public HasVisibility initSignOutItem(final ClickHandler handler) {
        signOutMobile.addClickHandler(new CloseMenuClickHandler(handler));
        return new MenuItem(signOutMobile);
    }

    private Anchor createMenuItem(final String text) {
        final Anchor item = new Anchor(text);
        item.addStyleName(app_res.style().anchor());
        item.addStyleName(local_res.style().item());
        return item;
    }

    private class CloseMenuClickHandler implements ClickHandler {

        private final ClickHandler handler;

        private CloseMenuClickHandler(final ClickHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onClick(final ClickEvent event) {
            Header.this.dropdownHandler.setVisible(false);
            handler.onClick(event);
        }
    }

    private class MenuItem implements ActivatableMenuItem {

        private final Set<Anchor> items = new HashSet<>();

        private MenuItem(final Anchor... anchors) {
            this.items.addAll(Arrays.asList(anchors));
        }

        @Override
        public boolean isVisible() {
            return items.stream().anyMatch(HasVisibility::isVisible);
        }

        @Override
        public void setVisible(final boolean visible) {
            items.forEach(item -> item.setVisible(visible));
        }

        @Override
        public void setActive(final boolean active) {
            items.forEach(item -> item.setStyleName(local_res.style().itemActive(), active));
        }

    }

}
