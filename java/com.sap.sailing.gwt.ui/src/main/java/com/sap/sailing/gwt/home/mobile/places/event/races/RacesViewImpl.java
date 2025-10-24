package com.sap.sailing.gwt.home.mobile.places.event.races;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.EventAndLeaderboardReferenceWithStateDTO;
import com.sap.sailing.gwt.home.communication.event.GetCompetitionFormatRacesAction;
import com.sap.sailing.gwt.home.communication.event.RaceCompetitionFormatSeriesDTO;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorDTO;
import com.sap.sailing.gwt.home.communication.eventview.RegattaMetadataDTO;
import com.sap.sailing.gwt.home.communication.race.SimpleRaceMetadataDTO;
import com.sap.sailing.gwt.home.mobile.partials.quickfinder.Quickfinder;
import com.sap.sailing.gwt.home.mobile.partials.regattacompetition.RegattaCompetition;
import com.sap.sailing.gwt.home.mobile.places.QuickfinderPresenter;
import com.sap.sailing.gwt.home.mobile.places.event.AbstractEventView;
import com.sap.sailing.gwt.home.shared.partials.filter.FilterPresenter;
import com.sap.sailing.gwt.home.shared.partials.filter.FilterValueChangeHandler;
import com.sap.sailing.gwt.home.shared.partials.filter.FilterValueProvider;
import com.sap.sailing.gwt.home.shared.partials.filter.FilterWidget;
import com.sap.sailing.gwt.home.shared.partials.filter.RacesByCompetitorTextBoxFilter;
import com.sap.sailing.gwt.home.shared.partials.placeholder.InfoPlaceholder;
import com.sap.sailing.gwt.home.shared.partials.regattacompetition.RegattaCompetitionPresenter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.dispatch.shared.commands.ListResult;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

public class RacesViewImpl extends AbstractEventView<RacesView.Presenter> implements RacesView {

    private static RacesViewImplUiBinder uiBinder = GWT.create(RacesViewImplUiBinder.class);

    interface RacesViewImplUiBinder extends UiBinder<Widget, RacesViewImpl> {
    }
    
    @UiField RacesByCompetitorTextBoxFilter competitorFilterUi;
    @UiField RegattaCompetition regattaCompetitionUi;

    public RacesViewImpl(final RacesView.Presenter presenter) {
        super(presenter, presenter.isMultiRegattaEvent(), true, presenter.getRegatta() != null);
        setViewContent(uiBinder.createAndBindUi(this));
        if(presenter.getRegatta() != null) {
            RegattaCompetitionPresenter competitionPresenter = new MobileRegattaCompetitionPresenter();
            RacesViewImplFilterPresenter filterPresenter = new RacesViewImplFilterPresenter(competitorFilterUi,
                    competitionPresenter, competitionPresenter);
            refreshManager.add(filterPresenter.getRefreshableWidgetWrapper(competitionPresenter),
                    new GetCompetitionFormatRacesAction(getEventId(), getRegattaId()));
        } else {
            setViewContent(new InfoPlaceholder(StringMessages.INSTANCE.noDataForEvent()));
        }
    }
    
    @Override
    protected void setQuickFinderValues(Quickfinder quickfinder, Map<String, Set<RegattaMetadataDTO>> regattasByLeaderboardGroupName) {
        QuickfinderPresenter.getForRegattaRaces(quickfinder, currentPresenter, regattasByLeaderboardGroupName);
    }
    
    @Override
    protected void setQuickFinderValues(Quickfinder quickfinder, String seriesName, Collection<EventAndLeaderboardReferenceWithStateDTO> eventsOfSeries) {
        QuickfinderPresenter.getForSeriesEventRaces(quickfinder, seriesName, currentPresenter, eventsOfSeries);
    }
    
    private class MobileRegattaCompetitionPresenter extends RegattaCompetitionPresenter {
        public MobileRegattaCompetitionPresenter() {
            super(regattaCompetitionUi, new PaywallResolverImpl(currentPresenter.getUserService(),
                    currentPresenter.getSubscriptionServiceFactory()));
        }
        
        @Override
        protected String getRaceViewerURL(SimpleRaceMetadataDTO raceMetadata, String mode) {
            return currentPresenter.getRaceViewerURL(raceMetadata, mode);
        }
        
        @Override
        protected String getMapAndWindChartUrl(String leaderboardName, String raceName, String fleetName) {
            return currentPresenter.getMapAndWindChartUrl(leaderboardName, raceName, fleetName);
        }

        @Override
        public void setData(ListResult<RaceCompetitionFormatSeriesDTO> data) {
            super.setData(data);
            final Consumer<? super String> scrollCommand = regattaCompetitionUi::scrollToSeries;
            Scheduler.get().scheduleDeferred(() -> currentPresenter.getPreferredSeriesName().ifPresent(scrollCommand));
        }
    }
    
    private class RacesViewImplFilterPresenter extends FilterPresenter<SimpleRaceMetadataDTO, SimpleCompetitorDTO> {

        private final List<FilterValueProvider<SimpleCompetitorDTO>> valueProviders;
        private final List<FilterValueChangeHandler<SimpleRaceMetadataDTO>> valueChangeHandlers;

        public RacesViewImplFilterPresenter(FilterWidget<SimpleRaceMetadataDTO, SimpleCompetitorDTO> filterWidget,
                FilterValueProvider<SimpleCompetitorDTO> valueProvider,
                FilterValueChangeHandler<SimpleRaceMetadataDTO> valueChangeHandler) {
            super(filterWidget);
            this.valueProviders = Arrays.asList(valueProvider);
            this.valueChangeHandlers = Arrays.asList(valueChangeHandler);
            super.addHandler(valueChangeHandler);
        }

        @Override
        protected List<FilterValueProvider<SimpleCompetitorDTO>> getCurrentValueProviders() {
            return valueProviders;
        }

        @Override
        protected List<FilterValueChangeHandler<SimpleRaceMetadataDTO>> getCurrentValueChangeHandlers() {
            return valueChangeHandlers;
        }
    }
    
}
