package com.sap.sailing.gwt.home.mobile.places.event.overview;

import java.util.Collection;
import java.util.LinkedHashSet;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.user.client.ui.Panel;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.home.communication.event.GetRegattaStatisticsAction;
import com.sap.sailing.gwt.home.communication.event.eventoverview.GetEventOverviewStageAction;
import com.sap.sailing.gwt.home.communication.event.news.GetEventOverviewNewsAction;
import com.sap.sailing.gwt.home.communication.event.statistics.GetEventStatisticsAction;
import com.sap.sailing.gwt.home.mobile.partials.eventdescription.EventDescription;
import com.sap.sailing.gwt.home.mobile.partials.impressions.Impressions;
import com.sap.sailing.gwt.home.mobile.partials.statisticsBox.MobileStatisticsBoxView;
import com.sap.sailing.gwt.home.mobile.partials.updatesBox.UpdatesBox;
import com.sap.sailing.gwt.home.mobile.places.event.AbstractEventView;
import com.sap.sailing.gwt.home.mobile.places.event.EventViewBase;
import com.sap.sailing.gwt.home.shared.partials.statistics.EventStatisticsBox;
import com.sap.sailing.gwt.ui.shared.SailingImageDTO;

public abstract class AbstractEventOverview extends AbstractEventView<EventViewBase.Presenter> {
    
    private EventOverviewStage overviewStageUi;
    private UpdatesBox updatesBoxUi;
    private Impressions impressionsUi;
    private EventStatisticsBox statisticsBoxUi;

    public AbstractEventOverview(EventViewBase.Presenter presenter, boolean showRegattaName, boolean enableLogoNavigation) {
        super(presenter, showRegattaName, enableLogoNavigation);
    }

    protected void setupOverviewStage(Panel container) {
        overviewStageUi = new EventOverviewStage(currentPresenter);
        refreshManager.add(overviewStageUi, new GetEventOverviewStageAction(getEventId(), true));
        container.add(overviewStageUi);
    }
    
    protected void setupUpdateBox(Panel container) {
        updatesBoxUi = new UpdatesBox(currentPresenter, refreshManager);
        if (currentPresenter.getEventDTO().getState() == EventState.RUNNING) {
            refreshManager.add(updatesBoxUi, new GetEventOverviewNewsAction(getEventId(), 2));
            container.add(updatesBoxUi);
        }
    }
    
    protected void setupImpressions(Panel container) {
        impressionsUi = new Impressions();
        impressionsUi.getElement().getStyle().setDisplay(Display.NONE);
        container.add(impressionsUi);
    }
    
    protected void setupStatisticsBox(Panel container, boolean forRegattaOnly) {
        statisticsBoxUi = new EventStatisticsBox(!forRegattaOnly, new MobileStatisticsBoxView());
        refreshManager.add(statisticsBoxUi, forRegattaOnly ? new GetRegattaStatisticsAction(getEventId(),
                getRegattaId()) : new GetEventStatisticsAction(getEventId()));
        container.add(statisticsBoxUi);
    }
    
    public void setMediaForImpressions(int nrOfImages, int nrOfVideos, Collection<SailingImageDTO> images) {
        impressionsUi.getElement().getStyle().setDisplay(Display.BLOCK);
        impressionsUi.setStatistics(nrOfImages, nrOfVideos);
        impressionsUi.addImages(new LinkedHashSet<SailingImageDTO>(images), currentPresenter.getUserService());
        // TODO: desktop media navigation
        impressionsUi.setClickDestinaton(currentPresenter.getMediaPageNavigation());
    }
    
    protected void setupEventDescription(Panel container) {
        final String description = currentPresenter.getEventDTO().getDescription();
        if (description != null) {
            container.add(new EventDescription(description));
        }
    }
}
