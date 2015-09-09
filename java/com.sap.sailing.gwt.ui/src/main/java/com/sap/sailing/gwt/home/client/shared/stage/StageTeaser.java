package com.sap.sailing.gwt.home.client.shared.stage;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.shared.partials.countdowntimer.CountdownTimer;
import com.sap.sailing.gwt.ui.shared.start.EventStageDTO;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.gwt.client.controls.carousel.LazyLoadable;

public abstract class StageTeaser extends Composite implements LazyLoadable {
    
    @UiField DivElement bandCount;
    @UiField SpanElement subtitle;
    @UiField SpanElement title;
    @UiField(provided = true) CountdownTimer countdownTimerUi;
    @UiField HTMLPanel stageTeaserBandsPanel;
    @UiField DivElement teaserImage;

    interface StageTeaserUiBinder extends UiBinder<Widget, StageTeaser> {
    }

    private static StageTeaserUiBinder uiBinder = GWT.create(StageTeaserUiBinder.class);
    private final EventStageDTO event;

    @Override
    public void doInitializeLazyComponents() {
        String stageImageUrl = event.getStageImageURL() != null ? event.getStageImageURL() : StageResources.INSTANCE
                .defaultStageEventTeaserImage().getSafeUri().asString();
        String backgroundImage = "url(" + stageImageUrl + ")";
        teaserImage.getStyle().setBackgroundImage(backgroundImage);
    }

    protected void handleUserAction() {
    }

    public StageTeaser(EventStageDTO event) {
        this.event = event;
        StageResources.INSTANCE.css().ensureInjected();
        
        TimePoint eventStart = new MillisecondsTimePoint(event.getStartDate());
        countdownTimerUi = new CountdownTimer(eventStart.asDate());
        initWidget(uiBinder.createAndBindUi(this));
        
        if(MillisecondsTimePoint.now().after(eventStart)) {
            countdownTimerUi.removeFromParent();
        }

        addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                EventTarget eventTarget = event.getNativeEvent().getEventTarget();
                if (!Element.is(eventTarget)) {
                    return;
                }
                Element element = eventTarget.cast();
                if (bandCount.isOrHasChild(element)) {
                    return;
                }
                handleUserAction();
            }
        }, ClickEvent.getType());
    }
}
