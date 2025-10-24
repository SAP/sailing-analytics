package com.sap.sailing.gwt.home.mobile.places.series;

import java.util.Collection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.EventAndLeaderboardReferenceWithStateDTO;
import com.sap.sailing.gwt.home.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.home.communication.event.GetSeriesStatisticsAction;
import com.sap.sailing.gwt.home.communication.event.minileaderboard.GetMiniOverallLeaderbordAction;
import com.sap.sailing.gwt.home.communication.fakeseries.EventSeriesViewDTO;
import com.sap.sailing.gwt.home.mobile.partials.minileaderboard.MinileaderboardBox;
import com.sap.sailing.gwt.home.mobile.partials.quickfinder.Quickfinder;
import com.sap.sailing.gwt.home.mobile.partials.recents.EventsOverviewRecentYearEvent;
import com.sap.sailing.gwt.home.mobile.partials.seriesheader.SeriesHeader;
import com.sap.sailing.gwt.home.mobile.partials.statisticsBox.MobileStatisticsBoxView;
import com.sap.sailing.gwt.home.mobile.places.QuickfinderPresenter;
import com.sap.sailing.gwt.home.shared.partials.statistics.EventStatisticsBox;
import com.sap.sailing.gwt.home.shared.refresh.LifecycleRefreshManager;
import com.sap.sailing.gwt.home.shared.refresh.RefreshManager;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class SeriesViewImpl extends Composite implements SeriesView {
    private static final StringMessages MSG = StringMessages.INSTANCE;
    private static StartPageViewUiBinder uiBinder = GWT.create(StartPageViewUiBinder.class);

    interface StartPageViewUiBinder extends UiBinder<Widget, SeriesViewImpl> {
    }

    @UiField(provided = true) SeriesHeader eventHeaderUi;
    @UiField Quickfinder quickFinderUi;
    @UiField(provided = true) MinileaderboardBox leaderboardUi;
    @UiField FlowPanel eventsUi;
    @UiField(provided = true) EventStatisticsBox statisticsBoxUi;

    private final Presenter currentPresenter;
    private final RefreshManager refreshManager;

    public SeriesViewImpl(SeriesView.Presenter presenter, FlagImageResolver flagImageResolver) {
        this.currentPresenter = presenter;
        this.refreshManager = new LifecycleRefreshManager(this, currentPresenter.getDispatch());
        EventSeriesViewDTO series = currentPresenter.getSeriesDTO();
        eventHeaderUi = new SeriesHeader(currentPresenter.getCtx(), series, currentPresenter.getUserService());
        this.setupStatisticsBox(series);
        leaderboardUi = new MinileaderboardBox(true, flagImageResolver);
        initWidget(uiBinder.createAndBindUi(this));
        this.setupListContent(series);
        this.setupEventListContent(series);
    }
    
    private void setupEventListContent(EventSeriesViewDTO series) {
        boolean first = true;
        for(EventMetadataDTO eventOfSeries : series.getEventsDescending()) {
            if(eventOfSeries.getState() == EventState.PLANNED) {
                continue;
            }
            boolean teaser = first || eventOfSeries.getState() == EventState.UPCOMING || eventOfSeries.getState() == EventState.RUNNING;
            EventsOverviewRecentYearEvent eventTeaser = new EventsOverviewRecentYearEvent(currentPresenter.getEventNavigation(eventOfSeries.getId().toString()), eventOfSeries, eventOfSeries.getState().getStateMarker(), teaser );
            eventsUi.add(eventTeaser);
            first = false;
        }
    }
    
    @Override
    public void setQuickFinderValues(String seriesName,
            Collection<EventAndLeaderboardReferenceWithStateDTO> eventsOfSeries) {
        QuickfinderPresenter.getForSeriesLeaderboards(quickFinderUi, seriesName, currentPresenter, eventsOfSeries);
    }

    private void setupListContent(EventSeriesViewDTO event) {
        leaderboardUi.setAction(MSG.showAll(), currentPresenter.getMiniOverallLeaderboardNavigation());
        refreshManager.add(leaderboardUi, new GetMiniOverallLeaderbordAction(event.getLeaderboardGroupUUID(), event.getLeaderboardId(), 3));
    }
    
    private void setupStatisticsBox(EventSeriesViewDTO series) {
        statisticsBoxUi = new EventStatisticsBox(true, new MobileStatisticsBoxView());
        refreshManager.add(statisticsBoxUi, new GetSeriesStatisticsAction(series.getLeaderboardGroupUUID()));
    }
}
