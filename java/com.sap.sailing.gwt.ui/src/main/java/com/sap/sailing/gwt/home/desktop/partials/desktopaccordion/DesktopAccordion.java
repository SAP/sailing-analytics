package com.sap.sailing.gwt.home.desktop.partials.desktopaccordion;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.desktop.partials.eventsrecent.EventsOverviewRecentResources;
import com.sap.sailing.gwt.home.shared.utils.CollapseAnimation;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class DesktopAccordion extends Composite {

    interface DesktopAccordionUiBinder extends UiBinder<Widget, DesktopAccordion> {
    }

    private static DesktopAccordionUiBinder uiBinder = GWT.create(DesktopAccordionUiBinder.class);

    @UiField
    SpanElement titleUi;
    @UiField
    FlowPanel contentPanelUi;
    @UiField
    DivElement contentDivUi;
    @UiField
    HTMLPanel headerDivUi;
    @UiField
    StringMessages i18n;

    private boolean isContentVisible;

    private final CollapseAnimation animation;

    public DesktopAccordion() {
        this(false);
    }

    public DesktopAccordion(boolean showInitial) {

        EventsOverviewRecentResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));

        this.titleUi.setInnerText("Title");

        headerDivUi.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onHeaderCicked();
            }
        }, ClickEvent.getType());

        isContentVisible = showInitial;
        animation = new CollapseAnimation(contentDivUi, showInitial);
        updateAccordionState();
    }

    public void setTitle(String title) {
        titleUi.setInnerText(title);
    }

    private void onHeaderCicked() {
        isContentVisible = !isContentVisible;
        updateContentVisibility();
    }

    private void updateContentVisibility() {
        animation.animate(isContentVisible);
        updateAccordionState();
    }

    public void addWidget(Widget w) {
        contentPanelUi.add(w);
    }

    private void updateAccordionState() {
        if (isContentVisible) {
            getElement().removeClassName(EventsOverviewRecentResources.INSTANCE.css().accordioncollapsed());
        } else {
            getElement().addClassName(EventsOverviewRecentResources.INSTANCE.css().accordioncollapsed());
        }
    }
}
