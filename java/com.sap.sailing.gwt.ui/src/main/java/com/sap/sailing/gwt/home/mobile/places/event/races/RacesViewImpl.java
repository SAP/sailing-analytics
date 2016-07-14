package com.sap.sailing.gwt.home.mobile.places.event.races;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.EventReferenceDTO;
import com.sap.sailing.gwt.home.communication.event.GetCompetitionFormatRacesAction;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorDTO;
import com.sap.sailing.gwt.home.communication.eventview.RegattaMetadataDTO;
import com.sap.sailing.gwt.home.communication.race.SimpleRaceMetadataDTO;
import com.sap.sailing.gwt.home.mobile.partials.quickfinder.Quickfinder;
import com.sap.sailing.gwt.home.mobile.partials.regattacompetition.RegattaCompetition;
import com.sap.sailing.gwt.home.mobile.places.QuickfinderPresenter;
import com.sap.sailing.gwt.home.mobile.places.event.AbstractEventView;
import com.sap.sailing.gwt.home.shared.ExperimentalFeatures;
import com.sap.sailing.gwt.home.shared.partials.filter.FilterPresenter;
import com.sap.sailing.gwt.home.shared.partials.filter.FilterValueChangeHandler;
import com.sap.sailing.gwt.home.shared.partials.filter.FilterWidget;
import com.sap.sailing.gwt.home.shared.partials.filter.RacesByCompetitorTextBoxFilter;
import com.sap.sailing.gwt.home.shared.partials.regattacompetition.RegattaCompetitionPresenter;
import com.sap.sailing.gwt.ui.raceboard.RaceBoardModes;

public class RacesViewImpl extends AbstractEventView<RacesView.Presenter> implements RacesView {

    private static RacesViewImplUiBinder uiBinder = GWT.create(RacesViewImplUiBinder.class);

    interface RacesViewImplUiBinder extends UiBinder<Widget, RacesViewImpl> {
    }
    
    @UiField RacesByCompetitorTextBoxFilter competitorFilterUi;
    @UiField RegattaCompetition regattaCompetitionUi;

    public RacesViewImpl(final RacesView.Presenter presenter) {
        super(presenter, presenter.isMultiRegattaEvent(), true);
        setViewContent(uiBinder.createAndBindUi(this));
        RegattaCompetitionPresenter competitionPresenter = new MobileRegattaCompetitionPresenter();
        RacesViewImplFilterPresenter filterPresenter = new RacesViewImplFilterPresenter(competitorFilterUi, competitionPresenter);
        refreshManager.add(filterPresenter.getRefreshableWidgetWrapper(competitionPresenter), new GetCompetitionFormatRacesAction(getEventId(), getRegattaId()));
        competitorFilterUi.removeFromParent();
    }
    
    @Override
    protected void setQuickFinderValues(Quickfinder quickfinder, Map<String, Set<RegattaMetadataDTO>> regattasByLeaderboardGroupName) {
        QuickfinderPresenter.getForRegattaRaces(quickfinder, currentPresenter, regattasByLeaderboardGroupName);
    }
    
    @Override
    protected void setQuickFinderValues(Quickfinder quickfinder, String seriesName, Collection<EventReferenceDTO> eventsOfSeries) {
        QuickfinderPresenter.getForSeriesEventRaces(quickfinder, seriesName, currentPresenter, eventsOfSeries);
    }
    
    private class MobileRegattaCompetitionPresenter extends RegattaCompetitionPresenter {
        public MobileRegattaCompetitionPresenter() {
            super(regattaCompetitionUi);
        }
        
        @Override
        protected String getRaceViewerURL(SimpleRaceMetadataDTO raceMetadata, RaceBoardModes mode) {
            return ExperimentalFeatures.ENABLE_RACE_VIEWER_LINK_ON_MOBILE
                    ? currentPresenter.getRaceViewerURL(raceMetadata, mode) : null;
        }
    }
    
    private class RacesViewImplFilterPresenter extends FilterPresenter<SimpleRaceMetadataDTO, SimpleCompetitorDTO> {
        private final List<FilterValueChangeHandler<SimpleRaceMetadataDTO, SimpleCompetitorDTO>> valueChangeHandler;

        public RacesViewImplFilterPresenter(FilterWidget<SimpleRaceMetadataDTO, SimpleCompetitorDTO> filterWidget,
                FilterValueChangeHandler<SimpleRaceMetadataDTO, SimpleCompetitorDTO> valueChangeHandler) {
            super(filterWidget);
            this.valueChangeHandler = Arrays.asList(valueChangeHandler);
            super.addHandler(valueChangeHandler);
        }

        @Override
        protected List<FilterValueChangeHandler<SimpleRaceMetadataDTO, SimpleCompetitorDTO>> getCurrentValueChangeHandlers() {
            return valueChangeHandler;
        }
    }
    
}
