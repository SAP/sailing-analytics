package com.sap.sailing.gwt.home.mobile.partials.sectionHeader;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.communication.event.LabelType;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.bubble.Bubble;
import com.sap.sailing.gwt.home.shared.partials.bubble.Bubble.Direction;
import com.sap.sailing.gwt.home.shared.partials.bubble.BubbleContentBoatClass;
import com.sap.sailing.gwt.home.shared.utils.CollapseAnimation;
import com.sap.sailing.gwt.home.shared.utils.LabelTypeUtil;
import com.sap.sse.gwt.client.LinkUtil;

public class SectionHeaderContent extends Composite {

    protected static final String ACCORDION_COLLAPSED_STYLE = SectionHeaderResources.INSTANCE.css().collapsed();
    private static MyBinder uiBinder = GWT.create(MyBinder.class);

    interface MyBinder extends UiBinder<Widget, SectionHeaderContent> {
    }

    @UiField SectionHeaderResources local_res;
    @UiField AnchorElement headerMainUi;
    @UiField DivElement headerLeftUi; 
    @UiField DivElement titleAndLabelContainerUi;
    @UiField HeadingElement titleUi;
    @UiField DivElement labelUi;
    @UiField DivElement imageUi;
    @UiField DivElement subtitleUi;
    @UiField HTMLPanel headerRightUi;
    @UiField DivElement infoTextUi;
    @UiField DivElement actionArrowUi;
    @UiField SimplePanel widgetContainerUi;
    @UiField HTMLPanel headerContentUi;

    private boolean expanded = false;;

    private final Collection<AccordionExpansionListener> accordionListeners = new ArrayList<>();

    public interface AccordionExpansionListener {
        public void onExpansionChange(boolean collapsed);
    }

    public SectionHeaderContent() {
        SectionHeaderResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        headerRightUi.setVisible(false);
        widgetContainerUi.setVisible(false);
    }

    public void setSectionTitle(String sectionHeaderTitle) {
        titleUi.setInnerText(sectionHeaderTitle);
    }

    public void setSubtitle(String subtitle) {
        subtitleUi.getStyle().clearDisplay();
        subtitleUi.setInnerText(subtitle);
        headerLeftUi.addClassName(local_res.css().sectionheader_itemdoublerow());
    }

    public void setLabelType(LabelType labelType) {
        if (labelType == null || !labelType.isRendered()) {
            labelUi.getStyle().setDisplay(Display.NONE);
        } else {
            labelUi.getStyle().clearDisplay();
            LabelTypeUtil.renderLabelType(labelUi, labelType);
        }
    }

    public void setImageUrl(String imageUrl) {
        imageUi.getStyle().clearDisplay();
        imageUi.getStyle().setBackgroundImage("url(" + imageUrl + ")");
        titleAndLabelContainerUi.addClassName(local_res.css().sectionheader_item_adjust_title_left());
    }

    public void setInfoText(String infoText) {
        headerRightUi.setVisible(true);
        infoTextUi.getStyle().clearDisplay();
        infoTextUi.setInnerText(infoText);
    }

    public void initCollapsibility(Element content, boolean showInitial) {
        final CollapseAnimation animation = new CollapseAnimation(content, showInitial);
        UIObject.setStyleName(actionArrowUi, ACCORDION_COLLAPSED_STYLE, !showInitial);
        LinkUtil.configureForAction(headerMainUi, new Runnable() {
            @Override
            public void run() {
                boolean collapsed = actionArrowUi.hasClassName(ACCORDION_COLLAPSED_STYLE);
                expanded = collapsed;
                UIObject.setStyleName(actionArrowUi, ACCORDION_COLLAPSED_STYLE, !collapsed);
                animation.animate(collapsed);
                accordionListeners.forEach(l -> l.onExpansionChange(!collapsed));
            }
        });
        actionArrowUi.addClassName(SectionHeaderResources.INSTANCE.css().accordion());
        titleAndLabelContainerUi.addClassName(local_res.css().sectionheader_item_adjust_title_right());
        actionArrowUi.getStyle().clearDisplay();
        headerRightUi.setVisible(true);
    }

    public void setClickAction(final PlaceNavigation<?> placeNavigation) {
        placeNavigation.configureAnchorElement(headerMainUi);
        this.adjustedActionStyles();
        titleAndLabelContainerUi.addClassName(local_res.css().sectionheader_item_adjust_title_right());
        headerRightUi.setVisible(true);
        actionArrowUi.getStyle().clearDisplay();
    }

    public void setClickAction(final String url) {
        headerMainUi.setHref(url);
        DOM.setEventListener(headerMainUi, event -> {
        });
        this.adjustedActionStyles();
    }

    private void adjustedActionStyles() {
        titleAndLabelContainerUi.addClassName(local_res.css().sectionheader_item_adjust_title_right());
        headerRightUi.setVisible(true);
        actionArrowUi.getStyle().clearDisplay();
    }

    public void setClickAction(final Runnable commandToExecute) {
        headerMainUi.setHref(Window.Location.getHref());
        headerRightUi.setVisible(true);
        actionArrowUi.getStyle().clearDisplay();
        LinkUtil.configureForAction(headerMainUi, commandToExecute);
    }

    public void appendHeaderElement(Widget widget) {
        headerRightUi.add(widget);
        actionArrowUi.getStyle().setMarginRight(.5, Unit.EM);
        titleAndLabelContainerUi.getStyle().setPaddingRight(4.1, Unit.EM);
    }

    public void setFirstHeaderElement(Widget widget) {
        actionArrowUi.removeFromParent();
        headerRightUi.add(widget);
        headerRightUi.getElement().appendChild(actionArrowUi);
        titleAndLabelContainerUi.getStyle().setPaddingRight(4.1, Unit.EM);
    }

    public void initAdditionalWidget(final Widget additionalWidget) {
        headerRightUi.setVisible(true);
        widgetContainerUi.setVisible(true);
        widgetContainerUi.setWidget(additionalWidget);
        additionalWidget.getElement().setAttribute("dir", "rtl");
        Scheduler.get().scheduleDeferred(
                () -> titleAndLabelContainerUi.getStyle().setMarginRight(widgetContainerUi.getOffsetWidth(), Unit.PX));
    }

    public void initBoatClassPopup(String boatClassName) {
        BubbleContentBoatClass content = new BubbleContentBoatClass(boatClassName);
        Bubble.DefaultPresenter presenter = new Bubble.DefaultPresenter(content, getElement(), imageUi,
                Direction.RIGHT);
        presenter.registerTarget(imageUi);
    }

    public void addAccordionListener(AccordionExpansionListener listener) {
        accordionListeners.add((AccordionExpansionListener) listener);
    }

    public boolean isExpanded() {
        return expanded;
    }

}
