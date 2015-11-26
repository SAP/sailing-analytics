package com.sap.sailing.gwt.home.desktop.partials.old;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.shared.partials.fullscreen.FullscreenContainer;
import com.sap.sse.common.Color;
import com.sap.sse.gwt.client.controls.busyindicator.BusyIndicator;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;

public abstract class AbstractLeaderboardFullscreenViewer<T extends Widget> extends FullscreenContainer<T> implements
        LeaderboardDelegate<T> {

    private final Image autoRefreshControl = new Image("images/home/reload.svg");
    private final Image settingsControl = new Image("images/home/settings.svg");

    private final Label lastScoringUpdateTime = new Label();
    private final Label lastScoringUpdateText = new Label();
    protected final Label lastScoringComment = new Label();
    protected final Label scoringScheme = new Label();
    protected final BusyIndicator busyIndicator = new SimpleBusyIndicator();

    public AbstractLeaderboardFullscreenViewer() {
        showLogo();
        showBorder();
        addToolbarInfo(createPanel(lastScoringUpdateText, lastScoringUpdateTime));
        addToolbarBusyIndicator(busyIndicator);
        addToolbarAction(autoRefreshControl);
        addToolbarAction(settingsControl);
    }

    @Override
    protected void onShow() {
        Element leaderboardPanel = getContentWidget().getElement();
        leaderboardPanel.getStyle().setBackgroundColor(Color.WHITE.getAsHtml());
        leaderboardPanel.getFirstChildElement().getStyle().setMarginTop(-10, Unit.PX);
        leaderboardPanel.setTabIndex(0);
        leaderboardPanel.focus();
    }

    @Override
    protected void onClose() {
        Element leaderboardPanel = getContentWidget().getElement();
        leaderboardPanel.getStyle().clearBackgroundColor();
        leaderboardPanel.getFirstChildElement().getStyle().clearMarginTop();
        leaderboardPanel.blur();
        leaderboardPanel.removeAttribute("tabIndex");
    }

    protected Widget createPanel(Widget... contentWidgets) {
        FlowPanel panel = new FlowPanel();
        for (Widget widget : contentWidgets)
            panel.add(widget);
        return panel;
    }

    @Override
    public void setLeaderboardPanel(T leaderboardPanel) {
        showContent(leaderboardPanel);
    }

    @Override
    public Widget getAutoRefreshControl() {
        return autoRefreshControl;
    }

    @Override
    public Widget getSettingsControl() {
        return settingsControl;
    }

    @Override
    public Element getLastScoringUpdateTimeElement() {
        return lastScoringUpdateTime.getElement();
    }

    @Override
    public Element getLastScoringUpdateTextElement() {
        return lastScoringUpdateText.getElement();
    }

    @Override
    public Element getLastScoringCommentElement() {
        return lastScoringComment.getElement();
    }

    @Override
    public Element getScoringSchemeElement() {
        return scoringScheme.getElement();
    }

    @Override
    public Element getBusyIndicatorElement() {
        return busyIndicator.getElement();
    }
}
