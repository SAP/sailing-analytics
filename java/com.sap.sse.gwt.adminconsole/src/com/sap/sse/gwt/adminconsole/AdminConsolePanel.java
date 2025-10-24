package com.sap.sse.gwt.adminconsole;

import static com.google.gwt.safehtml.shared.SafeHtmlUtils.htmlEscape;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.gwt.client.AbstractEntryPoint;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.ServerInfoDTO;
import com.sap.sse.gwt.client.panels.AbstractTabLayoutPanel;
import com.sap.sse.gwt.client.panels.HorizontalTabLayoutPanel;
import com.sap.sse.gwt.client.panels.VerticalTabLayoutPanel;
import com.sap.sse.gwt.shared.ClientConfiguration;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.UserStatusEventHandler;
import com.sap.sse.security.ui.loginpanel.LoginPanelCss;

/**
 * A panel that can be used to implement an administration console. Widgets can be arranged in vertical and horizontal
 * tabs ("L-shape"). The top-level element is the vertical tab panel. Widgets may either be added directly as the
 * content of one vertical tab, or a horizontal tab panel can be added as the content widget of a vertical tab, in turn
 * holding widgets in horizontal tabs.
 * <p>
 * 
 * After constructing an instance of this class, there are three ways for adding widgets:
 * <ul>
 * <li>{@link #addToVerticalTabPanel(RefreshableAdminConsolePanel, String, HasPermissions)} adds a widget as a content
 * element of a vertical tab</li>
 * <li>{@link #addVerticalTab(String, String, HasPermissions)} creates a horizontal tab panel and adds it as a content
 * element of a vertical tab</li>
 * <li>{@link #addToTabPanel(TabLayoutPanel, RefreshableAdminConsolePanel, String, HasPermissions)} adds a widget as a
 * content element of a horizontal tab</li>
 * </ul>
 * 
 * Widgets to be added need to be wrapped as {@link RefreshableAdminConsolePanel} holding the widget and receiving the
 * refresh call when the widget is shown because the user has selected the tab. If the component doesn't require any
 * refresh logic, an instance of {@link DefaultRefreshableAdminConsolePanel} can be used to wrap the widget.
 * <p>
 * 
 * After the widgets have been added, {@link #initUI()} must be called to assemble all tabs for the current user's
 * roles. The {@link #initUI()} method must be called each time more widgets have been added dynamically.
 * <p>
 * 
 * For each widget added, a {@link HasPermissions set of permissions} needs to be specified, any of which is sufficient to
 * get to see the widget. When the user changes or has his/her permissions updated the set of tabs visible will be
 * adjusted according to the new roles available for the logged-in user.
 * <p>
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class AdminConsolePanel<T extends Place & AdminConsolePlace> extends HeaderPanel {
    private final UserService userService;
    
    /**
     * The administration console's UI depends on the user's roles. When the roles change then so shall the display of
     * tabs. Required {@link HasPermissions}s tell when they are to be made available based on the user's actual
     * permissions. This map keeps track of the dependencies and allows the UI to adjust to role changes.<p>
     * 
     * The values are the permissions, at least one of which is required from the user to be able to see the widget.
     */
    private final LinkedHashSet<Triple<VerticalOrHorizontalTabLayoutPanel, Widget, String>> roleSpecificTabs;
    
    private final Map<Widget, Set<BooleanSupplier>> permissionsAnyOfWhichIsRequiredToSeeWidget;
    
    private final Map<Widget, T> widgetPlacesMap;
    
    private final Map<String, String> verticalTabNameToTitleMap;
    
    private final Map<Widget, String> panelAndDebugId;
    
    private final Map<Class<?>, String> placeAndDebugId;

    private final SelectionHandler<Integer> tabSelectionHandler;
    
    /**
     * The top-level vertical tab panel
     */
    private final VerticalTabLayoutPanel topLevelTabPanel;
    
    private final VerticalOrHorizontalTabLayoutPanel topLevelTabPanelWrapper;
    
    /**
     * Keys are the results of calling {@link RefreshableAdminConsolePanel#getWidget()} on their associated values. This
     * allows the panel to find the refresh target when a widget has been selected in a tab panel.
     */
    private final Map<Widget, RefreshableAdminConsolePanel<? extends Widget>> panelsByWidget;

    private final PlaceController placeController;

    private T currentPlace;

    /**
     * Generic selection handler that forwards selected tabs to a refresher that ensures that data gets reloaded. If
     * you add a new tab then make sure to have a look at #refreshDataFor(Widget widget) to ensure that upon
     * selection your tab gets the data refreshed.
     */
    private class TabSelectionHandler implements SelectionHandler<Integer> {
        @Override
        public void onSelection(SelectionEvent<Integer> event) {

            Object source = event.getSource();
            if (source != null) {
                if (source instanceof HorizontalTabLayoutPanel) {
                    final HorizontalTabLayoutPanel tabPanel = ((HorizontalTabLayoutPanel) source);
                    final Widget selectedPanel = tabPanel.getWidget(event.getSelectedItem());
                    if (selectedPanel instanceof PanelSupplierScollPanel) {
                        PanelSupplierScollPanel supplierScollPanel = (PanelSupplierScollPanel) selectedPanel;
                        supplierScollPanel.activate(panelSupplierScollPanel -> {
                                goToWidgetsPlace(panelSupplierScollPanel);
                            });
                    } else {
                        goToWidgetsPlace(selectedPanel);
                    }

               } else if (source instanceof VerticalTabLayoutPanel) {
                    final VerticalTabLayoutPanel verticalTabLayoutPanel = (VerticalTabLayoutPanel) source;
                    Widget widgetAssociatedToVerticalTab = verticalTabLayoutPanel.getWidget(verticalTabLayoutPanel.getSelectedIndex());
                    if (widgetAssociatedToVerticalTab instanceof HorizontalTabLayoutPanel) {
                        HorizontalTabLayoutPanel selectedTabLayoutPanel = (HorizontalTabLayoutPanel) widgetAssociatedToVerticalTab;
                        final int selectedIndex = selectedTabLayoutPanel.getSelectedIndex();
                        if (selectedIndex >= 0) {
                            widgetAssociatedToVerticalTab = selectedTabLayoutPanel.getWidget(selectedIndex);
                        }
                    }
                    if (widgetAssociatedToVerticalTab instanceof PanelSupplierScollPanel) {
                        PanelSupplierScollPanel dummyScrollPanel = (PanelSupplierScollPanel) widgetAssociatedToVerticalTab;
                        dummyScrollPanel.activate(panelSupplierScollPanel -> 
                        {
                            goToWidgetsPlace(panelSupplierScollPanel);
                        });
                    } else {
                        goToWidgetsPlace(widgetAssociatedToVerticalTab);
                    }
                }
            }
        }
        
        private void goToWidgetsPlace(Widget widget) {
            Place gotoPlace = null;
            if (widgetPlacesMap.containsKey(widget)) {
                gotoPlace = widgetPlacesMap.get(widget);
            }
            if (currentPlace == null || (gotoPlace != null && !currentPlace.isSamePlace(gotoPlace))) {
                placeController.goTo(gotoPlace);
            }
        }
    }
    
    /**
     * If the <code>widgetMaybeWrappedByScrollPanel</code> is a scroll panel, returns the content widget,
     * otherwise <code>widgetMaybeWrappedByScrollPanel</code> is returned.
     */
    private static Widget unwrapScrollPanel(Widget widgetMaybeWrappedByScrollPanel) {
        final Widget target;
        if (widgetMaybeWrappedByScrollPanel instanceof ScrollPanel) {
            target = ((ScrollPanel) widgetMaybeWrappedByScrollPanel).getWidget();
        } else {
            target = widgetMaybeWrappedByScrollPanel;
        }
        return target;
    }
    
    public AdminConsolePanel(UserService userService,
            ServerInfoDTO serverInfo, String releaseNotesAnchorLabel,
            String releaseNotesURL, Anchor footerAnchor, ErrorReporter errorReporter, LoginPanelCss loginPanelCss,
            StringMessages stringMessages, PlaceController placeController) {
        this.placeController = placeController;
        this.permissionsAnyOfWhichIsRequiredToSeeWidget = new HashMap<>();
        this.userService = userService;
        roleSpecificTabs = new LinkedHashSet<>();
        this.panelsByWidget = new HashMap<>();
        this.widgetPlacesMap = new HashMap<>();
        this.verticalTabNameToTitleMap = new HashMap<>();
        this.panelAndDebugId = new HashMap<>();
        this.placeAndDebugId = new HashMap<>();
        getUserService().addUserStatusEventHandler(new UserStatusEventHandler() {
            @Override
            public void onUserStatusChange(UserDTO user, boolean preAuthenticated) {
                updateTabDisplayForCurrentUser(user, true);
            }
        });
        tabSelectionHandler = new TabSelectionHandler();
        topLevelTabPanel = new VerticalTabLayoutPanel(2.5, Unit.EM);
        topLevelTabPanel.addSelectionHandler(tabSelectionHandler);
        topLevelTabPanel.ensureDebugId("AdministrationTabs");
        topLevelTabPanelWrapper = new VerticalOrHorizontalTabLayoutPanel() {
            
            @Override
            public void add(Widget child, String text, boolean asHtml, boolean fireEvents) {
                child.setTitle(text);
                topLevelTabPanel.add(child, text, asHtml, fireEvents);
                topLevelTabPanel.forceLayout();
            }  

            @Override
            public boolean remove(Widget child) {
                return topLevelTabPanel.remove(child);
            }

            @Override
            public boolean remove(Widget child, boolean fireEvents) {
                return topLevelTabPanel.remove(topLevelTabPanel.getWidgetIndex(child), fireEvents);
            }

            @Override
            public Widget getPanel() {
                return topLevelTabPanel;
            }

            @Override
            public void selectTab(int index) {
                topLevelTabPanel.selectTab(index);      
            }

            @Override
            public void selectTab(int index, boolean fireEvent) {
                topLevelTabPanel.selectTab(index, fireEvent);
            }

            @Override
            public int getWidgetIndex(Widget child) {
                return topLevelTabPanel.getWidgetIndex(child);
            }
        };
        final DockPanel informationPanel = new DockPanel();
        informationPanel.setWidth("100%");
        informationPanel.setSpacing(10);
        Widget persistentInformationWidget = errorReporter.getPersistentInformationWidget();
        persistentInformationWidget.addStyleName("footerInfoPanel");
        informationPanel.add(persistentInformationWidget, DockPanel.CENTER);
        SystemInformationPanel sysinfoPanel = new SystemInformationPanel(serverInfo, errorReporter, stringMessages);
        sysinfoPanel.addStyleName("systemInformationPanel");
        sysinfoPanel.ensureDebugId("SystemInformation");
        if (ClientConfiguration.getInstance().isBrandingActive()) {
            final Anchor releaseNotesLink = new Anchor(htmlEscape(releaseNotesAnchorLabel), releaseNotesURL);
            releaseNotesLink.addStyleName("releaseNotesAnchor");
            sysinfoPanel.add(releaseNotesLink);
            informationPanel.add(releaseNotesLink, DockPanel.EAST);
        }
        if (footerAnchor != null) {
            informationPanel.add(footerAnchor, DockPanel.EAST);
        }
        informationPanel.add(sysinfoPanel, DockPanel.EAST);
        informationPanel.setCellHorizontalAlignment(sysinfoPanel, HasHorizontalAlignment.ALIGN_RIGHT);
        this.setFooterWidget(informationPanel);
        topLevelTabPanel.setSize("100%", "100%");
        this.setContentWidget(topLevelTabPanel);
    }

    /**
     * Invoke this method after having added all panels using
     * {@link #addToTabPanel(TabLayoutPanel, RefreshableAdminConsolePanel, String, AdminConsoleFeatures)} or
     * {@link #addToVerticalTabPanel(RefreshableAdminConsolePanel, String, AdminConsoleFeatures)} or
     * {@link #addVerticalTab(String, String, AdminConsoleFeatures)}. Tabs can also dynamically be added after calling
     * this method, but then this method needs to be invoked again to ensure that all all tabs are properly displayed
     * for the current panel's state.
     */
   public void initUI(final T place) {
        updateTabDisplayForCurrentUser(getUserService().getCurrentUser(), false);
        if (place == null) {
            if (topLevelTabPanel.getWidgetCount() > 0) {
                topLevelTabPanel.selectTab(0);
                // activate 1st initial widget this way because selection logic will not be called in this case
                Widget currentSelectedWidget = topLevelTabPanel.getWidget(0);
                if (currentSelectedWidget instanceof PanelSupplierScollPanel) {
                    PanelSupplierScollPanel supplierScollPanel = (PanelSupplierScollPanel) currentSelectedWidget;
                    supplierScollPanel.activate(t -> {});
                }
            }
        } else {
            selectTabByPlace(place, false);
        }
    }

    private UserService getUserService() {
        return userService;
    }

    public static interface VerticalOrHorizontalTabLayoutPanel {
        
        void add(Widget child, String text, boolean asHtml, boolean fireEvents);

        boolean remove(Widget child);
        
        boolean remove(Widget child, boolean fireEvents);
        
        Widget getPanel();
        
        void selectTab(int index);
        
        void selectTab(int index, boolean fireEvent);

        int getWidgetIndex(Widget child);
    }

    /**
     * Adds a new horizontal tab panel to the top-level vertical tab panel.
     * 
     * @return the horizontal tab panel that was created and added to the top-level vertical tab panel; the panel returned can be specified
     * as argument to {@link #addToTabPanel(TabLayoutPanel, Widget, String, AdminConsoleFeatures)}.
     */
    public HorizontalTabLayoutPanel addVerticalTab(String tabTitle, String tabDebugId, T place, WildcardPermission... requiresAnyOfThesePermissions) {
        final HorizontalTabLayoutPanel newTabPanel = new HorizontalTabLayoutPanel(2.5, Unit.EM);
        AbstractEntryPoint.setTabPanelSize(newTabPanel, "100%", "100%");
        newTabPanel.addSelectionHandler(tabSelectionHandler);
        newTabPanel.ensureDebugId(tabDebugId);
        panelAndDebugId.put(newTabPanel, tabDebugId);
        rememberWidgetLocationAndPermissions(topLevelTabPanelWrapper, newTabPanel, tabTitle, anyPermissionCheck(requiresAnyOfThesePermissions), place);
        return newTabPanel;
    }

    public HorizontalTabLayoutPanel addVerticalTab(String tabTitle, String tabDebugId, WildcardPermission... requiresAnyOfThesePermissions) {
        verticalTabNameToTitleMap.put(tabDebugId, tabTitle);
        return addVerticalTab(tabTitle, tabDebugId, null, requiresAnyOfThesePermissions);
    }
    
    /**
     * Adds an administration panel as an entry to the top-level vertical panel, without an intermediary horizontal tab panel.
     * This is useful for panels that form a top-level category of its own but don't require multiple panels to represent this
     * top-level category.
     */
    public void addToVerticalTabPanel(final RefreshableAdminConsolePanel<? extends Widget> panelToAdd, String tabTitle, T place,
            WildcardPermission... requiresAnyOfThesePermissions) {
        addToTabPanel(topLevelTabPanelWrapper, panelToAdd, tabTitle, anyPermissionCheck(requiresAnyOfThesePermissions), place);
    }

    private ScrollPanel wrapInScrollPanel(Widget panelToAdd) {
        ScrollPanel scrollPanel = new ScrollPanel();
        scrollPanel.add(panelToAdd);
        panelToAdd.setSize("100%", "100%");
        return scrollPanel;
    }

    private ScrollPanel wrapInDummyScrollPanel(AdminConsolePanelSupplier<? extends Widget> supplierToAdd) {
        ScrollPanel scrollPanel = new PanelSupplierScollPanel(supplierToAdd);
        return scrollPanel;
    }

    public void addToTabPanel(final HorizontalTabLayoutPanel tabPanel, RefreshableAdminConsolePanel<? extends Widget> panelToAdd,
            String tabTitle, T place) {
        this.addToTabPanel(tabPanel, panelToAdd, tabTitle, place, new WildcardPermission(WildcardPermission.WILDCARD_TOKEN));
    }
    
    public BooleanSupplier anyPermissionCheck(WildcardPermission... requiresAnyOfThesePermissions) {
        return () -> {
            boolean permitted = false;
            for (WildcardPermission requiredPermission : requiresAnyOfThesePermissions) {
                // TODO for permissions with no wildcards in part 3 (object ID) we could request the ownership from the server...
                if (userService.hasCurrentUserAnyPermission(requiredPermission, /* ownership */ null)) {
                    permitted = true;
                    break;
                }
            }
            return permitted;
        };
    }

    public void addToTabPanel(final HorizontalTabLayoutPanel tabPanel, RefreshableAdminConsolePanel<? extends Widget> panelToAdd, String tabTitle, T place, WildcardPermission... requiresAnyOfThesePermissions) {
        addToTabPanel(tabPanel, panelToAdd, tabTitle, place, anyPermissionCheck(requiresAnyOfThesePermissions));       
    }
    
    public void addToTabPanel(final HorizontalTabLayoutPanel tabPanel, RefreshableAdminConsolePanel<? extends Widget> panelToAdd, String tabTitle, BooleanSupplier permissionCheck, T place) {
        VerticalOrHorizontalTabLayoutPanel wrapper = new VerticalOrHorizontalTabLayoutPanel() {
            @Override
            public void add(Widget child, String text, boolean asHtml, boolean fireEvents) {
                child.setTitle(text);
                tabPanel.add(child, text, asHtml, fireEvents);
                tabPanel.forceLayout();
            }

            @Override
            public boolean remove(Widget child) {
                return tabPanel.remove(child);
            }
            
            @Override
            public boolean remove(Widget child, boolean fireEvents) {
                return tabPanel.remove(tabPanel.getWidgetIndex(child), fireEvents);
            }

            @Override
            public Widget getPanel() {
                return tabPanel;
            }

            @Override
            public void selectTab(int index) {
               tabPanel.selectTab(index);
            }

            @Override
            public void selectTab(int index, boolean fireEvent) {
                tabPanel.selectTab(index, fireEvent);
            }

            @Override
            public int getWidgetIndex(Widget child) {
                return tabPanel.getWidgetIndex(child);
            }

        };
        addToTabPanel(wrapper, panelToAdd, tabTitle, permissionCheck, place);
    }
    
    public void addToTabPanel(final HorizontalTabLayoutPanel tabPanel, RefreshableAdminConsolePanel<? extends Widget> panelToAdd, String tabTitle, T place, BooleanSupplier permissionCheck) {
        VerticalOrHorizontalTabLayoutPanel wrapper = new VerticalOrHorizontalTabLayoutPanel() {
            @Override
            public void add(Widget child, String text, boolean asHtml, boolean fireEvents) {
                child.setTitle(text);
                tabPanel.add(child, text, asHtml, fireEvents);
                tabPanel.forceLayout();
            }

            @Override
            public boolean remove(Widget child) {
                return tabPanel.remove(child);
            }
            
            @Override
            public boolean remove(Widget child, boolean fireEvents) {
                return tabPanel.remove(tabPanel.getWidgetIndex(child), fireEvents);
            }

            @Override
            public Widget getPanel() {
                return tabPanel;
            }

            @Override
            public void selectTab(int index) {
               tabPanel.selectTab(index);
            }

            @Override
            public void selectTab(int index, boolean fireEvent) {
                tabPanel.selectTab(index, fireEvent);
            }

            @Override
            public int getWidgetIndex(Widget child) {
                return tabPanel.getWidgetIndex(child);
            }

        };
        addToTabPanel(wrapper, panelToAdd, tabTitle, permissionCheck, place);
        String debugId = panelAndDebugId.get(tabPanel);
        if (debugId != null) {
            placeAndDebugId.put(place.getClass(), debugId);
        }
    }
    
    /**
     * Remembers in which tab panel the <code>panelToAdd</code> is to be displayed and for which feature; additionally,
     * adds a hook so that when the <code>panelToAdd</code>'s widget is selected then the
     * {@link RefreshableAdminConsolePanel#refreshAfterBecomingVisible()} method can be called.
     */
    private void addToTabPanel(VerticalOrHorizontalTabLayoutPanel tabPanel, RefreshableAdminConsolePanel<? extends Widget>
    panelToAdd, String tabTitle, BooleanSupplier permissionCheck, T place) {
        if (panelToAdd.getAdminConsolePanelSupplier() != null) {
            panelToAdd.getAdminConsolePanelSupplier().setTitle(tabTitle);
            ScrollPanel wrapped = wrapInDummyScrollPanel(panelToAdd.getAdminConsolePanelSupplier());
            rememberWidgetLocationAndPermissions(tabPanel, wrapped, tabTitle, permissionCheck, place);
            panelsByWidget.put(wrapped, panelToAdd);
        } else {
            panelToAdd.getWidget().setTitle(tabTitle);
            ScrollPanel wrapped = wrapInScrollPanel(panelToAdd.getWidget());
            rememberWidgetLocationAndPermissions(tabPanel, wrapped, tabTitle, permissionCheck, place);
            panelsByWidget.put(panelToAdd.getWidget(), panelToAdd);
        }
    }

    /**
     * Remembers the tab panel in which the <code>widgetToAdd</code> is to be displayed and which permissions are
     * sufficient to see the widget. For the <code>tabPanel</code>, all permissions provided here are added to the tab
     * panel's permissions so that the user will see the tab panel as soon as the user may see any of the widgets inside
     * that panel
     * 
     * @param requiresAnyOfThesePermissions
     *            zero or more permissions; if no permissions are provided, the user will always be able to see the
     *            widget. Otherwise, if any of these permissions implies any of the permissions the user has, the user
     *            will be shown the widget.
     */
    private void rememberWidgetLocationAndPermissions(VerticalOrHorizontalTabLayoutPanel tabPanel, Widget widgetToAdd,
            String tabTitle, BooleanSupplier permissionCheck, T place) {
        roleSpecificTabs.add(new Triple<VerticalOrHorizontalTabLayoutPanel, Widget, String>(tabPanel, widgetToAdd, tabTitle));
        final Set<BooleanSupplier> permissionChecksAsSet = new HashSet<>(Arrays.asList(permissionCheck));
        permissionsAnyOfWhichIsRequiredToSeeWidget.put(widgetToAdd, permissionChecksAsSet);
        Util.addToValueSet(permissionsAnyOfWhichIsRequiredToSeeWidget, tabPanel.getPanel(), permissionCheck);
        if (place != null) { // for horizontal tabs
            widgetPlacesMap.put(widgetToAdd, place);
        }
    }

    /**
     * After initialization or whenever the user changes, the tab display is adjusted based on which roles are required
     * to see which tabs. See {@link #roleSpecificTabs}. A selection event is fired when the tab currently selected
     * was removed and another tab was therefore selected.
     */
    private void updateTabDisplayForCurrentUser(UserDTO user, boolean fireEvents) {
        final Widget selectedPanel = getSelectedTab(null);
        for (Triple<VerticalOrHorizontalTabLayoutPanel, Widget, String> e : roleSpecificTabs) {
            final Widget widgetToAddOrRemove = e.getB();
            if (user != null && userHasPermissionsToSeeWidget(user, e.getB())) {
                if (e.getA().getWidgetIndex(widgetToAddOrRemove) == -1) {
                    e.getA().add(widgetToAddOrRemove, e.getC(), /* asHtml */false, fireEvents);
                }
            } else {
                e.getA().remove(widgetToAddOrRemove, /* fireEvents */ false);
            }
        }
        getSelectedTab(selectedPanel);
    }

    /**
     * If the top-level selected tab is a horizontal tab panel, its selected panel is returned; otherwise, the selected
     * top-level panel is returned. If no top-level panel exists or is selected, {@code null} is returned.
     * 
     * @param reselectCurrentSelectionIfNotSameAsThis
     *            if not {@code null}, the selected panel is {@link AbstractTabLayoutPanel#selectTab(Widget) selected}
     *            again in case it isn't the same as {@code reselectCurrentSelectionIfNotSameAsThis}, firing a selection
     *            event
     */
    private Widget getSelectedTab(Widget reselectCurrentSelectionIfNotSameAsThis) {
        final Widget topLevelSelectedTab;
        Widget selectedTabInHorizontalTabPanel = null;
        if (topLevelTabPanel.getSelectedIndex() != -1) {
            topLevelSelectedTab = unwrapScrollPanel(topLevelTabPanel.getWidget(topLevelTabPanel.getSelectedIndex()));
            if (topLevelSelectedTab instanceof AbstractTabLayoutPanel) {
                AbstractTabLayoutPanel p = (AbstractTabLayoutPanel) topLevelSelectedTab;
                if (p.getSelectedIndex() != -1) {
                    selectedTabInHorizontalTabPanel = unwrapScrollPanel(p.getWidget(p.getSelectedIndex()));
                    if (reselectCurrentSelectionIfNotSameAsThis != null && selectedTabInHorizontalTabPanel != reselectCurrentSelectionIfNotSameAsThis) {
                        SelectionEvent.fire(p, p.getSelectedIndex());
                    }
                } else {
                    selectedTabInHorizontalTabPanel = null;
                }
            } else {
                if (reselectCurrentSelectionIfNotSameAsThis != null && topLevelSelectedTab != reselectCurrentSelectionIfNotSameAsThis) {
                    SelectionEvent.fire(topLevelTabPanel, topLevelTabPanel.getSelectedIndex());
                }
            }
        } else {
            topLevelSelectedTab = null;
        }
        return selectedTabInHorizontalTabPanel != null ? selectedTabInHorizontalTabPanel : topLevelSelectedTab;
    }

    public void selectTabByPlace(T place) {
        selectTabByPlace(place, true);
    }

    public void selectTabByPlace(T place, boolean fireEvent) {
        String verticalTabTitle = null;
        String placeVerticalTabName = placeAndDebugId.get(place.getClass());
        if (placeVerticalTabName != null && verticalTabNameToTitleMap.containsKey(placeVerticalTabName)) {
            verticalTabTitle = verticalTabNameToTitleMap.get(placeVerticalTabName);
        }
        for (Triple<VerticalOrHorizontalTabLayoutPanel, Widget, String> panelWidgetName : roleSpecificTabs) {
            VerticalOrHorizontalTabLayoutPanel panel = panelWidgetName.getA();
            Widget currentWidget = panelWidgetName.getB();
            if (isWidgetForPlace(place, panelWidgetName, verticalTabTitle)) {
                int index = panel.getWidgetIndex(currentWidget);
                currentPlace = place;
                panel.selectTab(index, fireEvent);      
                if (currentWidget instanceof PanelSupplierScollPanel) {
                    PanelSupplierScollPanel supplierScollPanel = (PanelSupplierScollPanel) currentWidget;
                    supplierScollPanel.activate(t -> {filterAndSelect(place, unwrapScrollPanel(t)); refreshDataFor(t);});
                } else {
                    filterAndSelect(place, unwrapScrollPanel(currentWidget));
                    refreshDataFor(currentWidget);
                }
            }
        }
    }

    private void refreshDataFor(final Widget target) {
        final RefreshableAdminConsolePanel<? extends Widget> refreshTarget;
        if (target instanceof PanelSupplierScollPanel) {
            refreshTarget = panelsByWidget.get(target);
        } else {
            refreshTarget = panelsByWidget.get(unwrapScrollPanel(target));
        }
        if (refreshTarget != null) {
            refreshTarget.refreshAfterBecomingVisible();
        }
    }

    private boolean isWidgetForPlace(final T place, final Triple<VerticalOrHorizontalTabLayoutPanel, Widget, String> panelWidgetName, final String verticalTabTitle) {
        Widget currentWidget = panelWidgetName.getB();
        boolean isHorizontalMenu = widgetPlacesMap.containsKey(currentWidget) && place.isSamePlace(widgetPlacesMap.get(currentWidget));
        if (isHorizontalMenu) {
            return true;
        }
        VerticalOrHorizontalTabLayoutPanel panel = panelWidgetName.getA();
        String verticalPanelName = panelWidgetName.getC();
        boolean isVerticalTab = panel == topLevelTabPanelWrapper && verticalTabTitle != null && verticalTabTitle.equals(verticalPanelName);
        return isVerticalTab;
    }

    private void filterAndSelect(final T place, final Widget widget) {
        if (widget instanceof FilterablePanelProvider && place instanceof FilterableAdminConsolePlace && ((FilterableAdminConsolePlace)place).getFilterAndSelectParameters() != null) {
            FilterablePanelProvider<?> filterablePanelProvider = (FilterablePanelProvider<?>) widget;
            FilterableAdminConsolePlace filterablePlace = (FilterableAdminConsolePlace) place;
            filterablePanelProvider.getFilterablePanel().filterAndSelect(filterablePlace.getFilterAndSelectParameters());
        }
    }

    /**
     * A user is defined to have permission to see a widget if the widget's required permissions imply any of the permissions the
     * user has. This may at first seem the wrong way around. However, the problem is that a widget cannot express a general permission
     * that is implied by any detailed permissions. Wildcard permissions don't work this way. Instead, a wildcard permission implies
     * detailed permissions. This way, if the widget requires, say, "event:*:*" (or "event" for short), this permission implies
     * all more detailed event permissions such as "event:write:9456192873". Therefore, the permissions provided for widgets
     * must imply a permission the user has in order for the user to see the tab. More detailed permissions checks can then be
     * applied at a more detailed level of the UI and, of course, in the back end. Additionally, if any of the user's permissions
     * implies the required permission (e.g., the user having "*" as the administrator's permission), permission to see the widget
     * is also implied.
     */
    private boolean userHasPermissionsToSeeWidget(UserDTO user, Widget widget) {
        final Set<BooleanSupplier> permissionsRequired = permissionsAnyOfWhichIsRequiredToSeeWidget.get(widget);
        boolean hasPermission;
        if (permissionsRequired.isEmpty()) {
            hasPermission = true;
        } else {
            hasPermission = false;
            for (BooleanSupplier requiredPermission : permissionsRequired) {
                if (requiredPermission.getAsBoolean()) {
                    hasPermission = true;
                    break;
                }
            }
        }
        return hasPermission;
    }
}
