package com.sap.sailing.gwt.home.mobile.partials.stage;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.EventLinkAndMetadataDTO;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;
import com.sap.sse.gwt.client.controls.carousel.WidgetCarousel;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class Stage extends Composite {
    
    interface StageUiBinder extends UiBinder<Widget, Stage> {
    }
    
    private static StageUiBinder uiBinder = GWT.create(StageUiBinder.class);

    @UiField DivElement stageContainer;
    @UiField WidgetCarousel widgetCarousel;

    private StageTeaser stageTeaser;
    private final MobilePlacesNavigator placeNavigator;

    public Stage(MobilePlacesNavigator placeNavigator, boolean showDots) {
        this.placeNavigator = placeNavigator;
        StageResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        widgetCarousel.setShowArrows(false);
        if (!showDots) stageContainer.getStyle().setHeight(100, Unit.PCT);
        widgetCarousel.setShowDots(showDots);
    }

    public void setFeaturedEvents(List<? extends EventLinkAndMetadataDTO> list, TakedownNoticeService takedownNoticeService) {
        final List<Widget> stageWidgets = new ArrayList<>();
        for (EventLinkAndMetadataDTO event : list) {
            switch (event.getStageType()) {
            case POPULAR:
                stageTeaser = new PopularEventStageTeaser(event, placeNavigator, takedownNoticeService);
                break;
            case RUNNING:
                stageTeaser = new LiveEventStageTeaser(event, placeNavigator, takedownNoticeService);
                break;
            case UPCOMING_SOON:
                stageTeaser = new UpcomingEventStageTeaser(event, placeNavigator, takedownNoticeService);
                break;
            }
            stageWidgets.add(stageTeaser);
        }
        widgetCarousel.setWidgets(stageWidgets);
    }
}
