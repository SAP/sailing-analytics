package com.sap.sailing.gwt.home.desktop.places.event.regatta.racestab;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.event.GetCompetitionFormatRacesAction;
import com.sap.sailing.gwt.home.communication.event.GetFinishedRacesAction;
import com.sap.sailing.gwt.home.communication.event.GetLiveRacesForRegattaAction;
import com.sap.sailing.gwt.home.communication.event.GetRegattaWithProgressAction;
import com.sap.sailing.gwt.home.communication.event.RaceListRaceDTO;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorDTO;
import com.sap.sailing.gwt.home.communication.race.SimpleRaceMetadataDTO;
import com.sap.sailing.gwt.home.communication.regatta.RegattaWithProgressDTO;
import com.sap.sailing.gwt.home.desktop.partials.liveraces.LiveRacesList;
import com.sap.sailing.gwt.home.desktop.partials.multiregattalist.MultiRegattaListItem;
import com.sap.sailing.gwt.home.desktop.partials.racelist.AbstractRaceList;
import com.sap.sailing.gwt.home.desktop.partials.racelist.RaceListColumnFactory;
import com.sap.sailing.gwt.home.desktop.partials.racelist.RaceListColumnSet;
import com.sap.sailing.gwt.home.desktop.partials.racelist.RaceListContainer;
import com.sap.sailing.gwt.home.desktop.partials.racelist.RaceListDataUtil;
import com.sap.sailing.gwt.home.desktop.partials.racelist.SortableRaceListColumn;
import com.sap.sailing.gwt.home.desktop.partials.raceoffice.RaceOfficeSection;
import com.sap.sailing.gwt.home.desktop.partials.regattacompetition.RegattaCompetition;
import com.sap.sailing.gwt.home.desktop.partials.regattanavigation.ListNavigationPanel;
import com.sap.sailing.gwt.home.desktop.partials.regattanavigation.ListNavigationPanel.ListNavigationAction;
import com.sap.sailing.gwt.home.desktop.partials.regattanavigation.ListNavigationPanel.SelectionCallback;
import com.sap.sailing.gwt.home.desktop.places.event.EventView;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.EventRegattaView.Presenter;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.RegattaTabView;
import com.sap.sailing.gwt.home.shared.partials.filter.FilterPresenter;
import com.sap.sailing.gwt.home.shared.partials.filter.FilterValueChangeHandler;
import com.sap.sailing.gwt.home.shared.partials.filter.FilterValueProvider;
import com.sap.sailing.gwt.home.shared.partials.filter.FilterWidget;
import com.sap.sailing.gwt.home.shared.partials.filter.RacesByCompetitorTextBoxFilter;
import com.sap.sailing.gwt.home.shared.partials.placeholder.InfoPlaceholder;
import com.sap.sailing.gwt.home.shared.partials.regattacompetition.RegattaCompetitionPresenter;
import com.sap.sailing.gwt.home.shared.refresh.ActionProvider.AbstractActionProvider;
import com.sap.sailing.gwt.home.shared.refresh.RefreshManager;
import com.sap.sailing.gwt.home.shared.refresh.RefreshManagerWithErrorAndBusy;
import com.sap.sailing.gwt.home.shared.refresh.RefreshableWidget;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.dispatch.shared.commands.ResultWithTTL;
import com.sap.sse.gwt.shared.DTO;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

public class RegattaRacesTabView extends Composite implements RegattaTabView<RegattaRacesPlace> {
    
    private static final StringMessages I18N = StringMessages.INSTANCE;
    
    private enum Navigation implements ListNavigationAction {
        SORT_LIST_FORMAT(I18N.listFormatLabel()), COMPETITION_FORMAT(I18N.competitionFormatLabel());
        
        private final String displayName;
        
        private Navigation(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
    }

    interface MyBinder extends UiBinder<Widget, RegattaRacesTabView> {
    }

    private static MyBinder ourUiBinder = GWT.create(MyBinder.class);

    private Presenter currentPresenter;

    @UiField SimplePanel regattaInfoContainerUi;
    @UiField(provided = true) ListNavigationPanel<Navigation> listNavigationPanelUi;
    @UiField DivElement listFormatContainerUi;
    @UiField RegattaCompetition compFormatContainerUi;
    @UiField(provided = true) LiveRacesList liveRacesListUi;
    @UiField(provided = true) RaceListContainer<RaceListRaceDTO> raceListContainerUi;
    @UiField RaceOfficeSection raceOfficeSectionUi;
    private final RacesByCompetitorTextBoxFilter competitorFilterUi = new RacesByCompetitorTextBoxFilter();
    private RegattaRacesTabViewFilterPresenter filterPresenter;
    private Navigation currentlySelectedTab = Navigation.SORT_LIST_FORMAT;
    private RefreshManager refreshManager;

    private final FlagImageResolver flagImageResolver;
    
    public RegattaRacesTabView(FlagImageResolver flagImageResolver) {
        this.flagImageResolver = flagImageResolver;
    }
    
    @Override
    public void setPresenter(Presenter currentPresenter) {
        this.currentPresenter = currentPresenter;
    }
    
    @Override
    public Presenter getPresenter() {
        return currentPresenter;
    }

    @Override
    public void start(RegattaRacesPlace myPlace, final AcceptsOneWidget contentArea) {
        if(currentPresenter.getRegattaMetadata() == null) {
            contentArea.setWidget(new InfoPlaceholder(StringMessages.INSTANCE.noDataForEvent()));
            return;
        }
        
        listNavigationPanelUi = new ListNavigationPanel<Navigation>(new RegattaRacesTabViewNavigationSelectionCallback());
        listNavigationPanelUi.setAdditionalWidget(competitorFilterUi);
        liveRacesListUi = new LiveRacesList(currentPresenter, false);
        RaceListFinishedRaces finishedRacesList = new RaceListFinishedRaces(currentPresenter);
        raceListContainerUi = new RaceListContainer<>(I18N.finishedRaces(), I18N.noFinishedRaces(), finishedRacesList);
        initWidget(ourUiBinder.createAndBindUi(RegattaRacesTabView.this));
        raceOfficeSectionUi.addLink(I18N.racesOverview(), currentPresenter.getRegattaOverviewLink());
        
        refreshManager = new RefreshManagerWithErrorAndBusy(this, contentArea, currentPresenter.getDispatch(), currentPresenter.getErrorAndBusyClientFactory());
        filterPresenter = new RegattaRacesTabViewFilterPresenter(competitorFilterUi);
        UUID eventId = currentPresenter.getEventDTO().getId();
        String regattaId = currentPresenter.getRegattaId();
        refreshManager.add(new RefreshableWidget<RegattaWithProgressDTO>() {
            @Override
            public void setData(RegattaWithProgressDTO data) {
                regattaInfoContainerUi.setWidget(new MultiRegattaListItem(data, true));
            }
        }, new GetRegattaWithProgressAction(eventId, regattaId));
        addRacesAction(liveRacesListUi.getRefreshable(), new GetLiveRacesForRegattaAction(eventId, regattaId), Navigation.SORT_LIST_FORMAT);
        filterPresenter.addProvider(Navigation.SORT_LIST_FORMAT, liveRacesListUi.getRaceList());
        filterPresenter.addHandler(Navigation.SORT_LIST_FORMAT, liveRacesListUi.getRaceList());
        addRacesAction(raceListContainerUi, new GetFinishedRacesAction(eventId, regattaId), Navigation.SORT_LIST_FORMAT);
        filterPresenter.addProvider(Navigation.SORT_LIST_FORMAT, finishedRacesList);
        filterPresenter.addHandler(Navigation.SORT_LIST_FORMAT, finishedRacesList);
        
        listNavigationPanelUi.addAction(Navigation.SORT_LIST_FORMAT, true);
        listNavigationPanelUi.addAction(Navigation.COMPETITION_FORMAT, false);
        RegattaCompetitionPresenter competitionPresenter = new DesktopRegattaCompetitionPresenter();
        addRacesAction(competitionPresenter, new GetCompetitionFormatRacesAction(eventId, regattaId),
                Navigation.COMPETITION_FORMAT);
        filterPresenter.addProvider(Navigation.COMPETITION_FORMAT, competitionPresenter);
        filterPresenter.addHandler(Navigation.COMPETITION_FORMAT, competitionPresenter);
    }
    
    private <D extends DTO, A extends SailingAction<ResultWithTTL<D>>> void addRacesAction(
            final RefreshableWidget<? super D> widget, A action, Navigation assosiatedTab) {
        refreshManager.add(filterPresenter.getRefreshableWidgetWrapper(widget),
                new RegattaRacesTabViewActionProvider<>(action, assosiatedTab));
    }
    
    @Override
    public void stop() {
    }

    @Override
    public RegattaRacesPlace placeToFire() {
        return new RegattaRacesPlace(currentPresenter.getCtx());
    }

    @Override
    public Class<RegattaRacesPlace> getPlaceClassForActivation() {
        return RegattaRacesPlace.class;
    }
    
    private class RegattaRacesTabViewNavigationSelectionCallback implements SelectionCallback<Navigation> {

        @Override
        public void onSelectAction(Navigation action) {
            RegattaRacesTabView.this.currentlySelectedTab = action;
            RegattaRacesTabView.this.refreshManager.forceReschedule();
            RegattaRacesTabView.this.filterPresenter.update();
            UIObject.setVisible(listFormatContainerUi, action == Navigation.SORT_LIST_FORMAT);
            compFormatContainerUi.setVisible(action == Navigation.COMPETITION_FORMAT);
        }
        
    }
    
    private class RegattaRacesTabViewActionProvider<A extends SailingAction<?>> extends AbstractActionProvider<A> {
        private final Navigation assosiatedTab;

        public RegattaRacesTabViewActionProvider(A action, Navigation assosiatedTab) {
            super(action);
            this.assosiatedTab = assosiatedTab;
        }
        
        @Override
        public boolean isActive() {
            return assosiatedTab == currentlySelectedTab;
        }
    }
    
    private class RaceListFinishedRaces extends AbstractRaceList<RaceListRaceDTO> {
        private final SortableRaceListColumn<RaceListRaceDTO, ?> durationColumn = RaceListColumnFactory.getDurationColumn();
        private final SortableRaceListColumn<RaceListRaceDTO, ?> windSpeedColumn = RaceListColumnFactory.getWindRangeColumn();
        private final SortableRaceListColumn<RaceListRaceDTO, ?> windSourcesCountColumn = RaceListColumnFactory.getWindSourcesCountColumn();
        private final SortableRaceListColumn<RaceListRaceDTO, ?> videoCountColumn = RaceListColumnFactory.getVideoCountColumn();
        private final SortableRaceListColumn<RaceListRaceDTO, ?> audioCountColumn = RaceListColumnFactory.getAudioCountColumn();
        private final SortableRaceListColumn<RaceListRaceDTO, ?> winnerColumn = RaceListColumnFactory.getWinnerColumn(flagImageResolver);
        private boolean hasWind;
        private boolean hasVideos;
        private boolean hasAudios;


        public RaceListFinishedRaces(EventView.Presenter presenter) {
            super(presenter, new RaceListColumnSet(1, 1), true);
        }
        
        @Override
        protected void setTableData(Collection<RaceListRaceDTO> data) {
            boolean hasFleets = RaceListDataUtil.hasFleets(data);
            this.fleetCornerColumn.setShowDetails(hasFleets);
            this.fleetNameColumn.setShowDetails(hasFleets);
            // Imagine a long running event (several days) where only one race has taken place.
            // Actually, you can't find out the date of this race, only its start time.
            // Therefore, start date and time is shown, as long as there's now grouping feature. 
            // TODO: this.startTimeColumn.setShowTimeOnly(!RaceListDataUtil.hasDifferentStartDates(data));
            this.startTimeColumn.setShowTimeOnly(false);
            this.durationColumn.setShowDetails(RaceListDataUtil.hasDurations(data));
            this.hasWind = RaceListDataUtil.hasWind(data);
            this.hasVideos = RaceListDataUtil.hasVideos(data);
            this.hasAudios = RaceListDataUtil.hasAudios(data);

            this.windSpeedColumn.setShowDetails(hasWind);
            this.windDirectionColumn.setShowDetails(hasWind);
            this.windSourcesCountColumn.setShowDetails(RaceListDataUtil.hasWindSources(data));
            this.videoCountColumn.setShowDetails(hasVideos);
            this.audioCountColumn.setShowDetails(hasAudios);
            this.winnerColumn.setShowDetails(RaceListDataUtil.hasWinner(data));
            super.setTableData(data);
        }

        @Override
        protected void initTableColumns() {
            add(fleetCornerColumn);
            add(raceNameColumn);
            add(fleetNameColumn);
            add(startTimeColumn);
            add(durationColumn);
            add(windSpeedColumn);
            add(windDirectionColumn);
            add(windSourcesCountColumn);
            add(videoCountColumn);
            add(audioCountColumn);
            add(winnerColumn);
            add(raceViewerButtonColumn);
            
            columnSet.addColumn(windSpeedColumn);
            columnSet.addColumn(windDirectionColumn);
            columnSet.addColumn(durationColumn);
            columnSet.addColumn(windSourcesCountColumn);
            columnSet.addColumn(videoCountColumn);
            columnSet.addColumn(audioCountColumn);
            columnSet.addColumn(fleetNameColumn);
        }

        @Override
        public boolean hasWind() {
            return hasWind;
        }

        @Override
        public boolean hasVideos() {
            return hasVideos;
        }

        @Override
        public boolean hasAudios() {
            return hasAudios;
        }
        
    }
    
    private class DesktopRegattaCompetitionPresenter extends RegattaCompetitionPresenter {
        public DesktopRegattaCompetitionPresenter() {
            super(compFormatContainerUi, new PaywallResolverImpl(currentPresenter.getUserService(), 
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
    }

    private class RegattaRacesTabViewFilterPresenter extends FilterPresenter<SimpleRaceMetadataDTO, SimpleCompetitorDTO> {

        private final Map<Navigation, List<FilterValueProvider<SimpleCompetitorDTO>>> providersByTab = new HashMap<>();
        private final Map<Navigation, List<FilterValueChangeHandler<SimpleRaceMetadataDTO>>> handlersByTab = new HashMap<>();
        
        private RegattaRacesTabViewFilterPresenter(FilterWidget<SimpleRaceMetadataDTO, SimpleCompetitorDTO> filterWidget) {
            super(filterWidget);
        }
        
        @Override
        protected List<FilterValueProvider<SimpleCompetitorDTO>> getCurrentValueProviders() {
            return providersByTab.get(currentlySelectedTab);
        }
        
        @Override
        protected List<FilterValueChangeHandler<SimpleRaceMetadataDTO>> getCurrentValueChangeHandlers() {
            return handlersByTab.get(currentlySelectedTab);
        }
        
        private void addProvider(Navigation assosiatedTab, FilterValueProvider<SimpleCompetitorDTO> provider) {
            List<FilterValueProvider<SimpleCompetitorDTO>> list = providersByTab.get(assosiatedTab);
            if (list == null) this.providersByTab.put(assosiatedTab, list = new ArrayList<>());
            list.add(provider);
        }

        private void addHandler(Navigation assosiatedTab, FilterValueChangeHandler<SimpleRaceMetadataDTO> handler) {
            List<FilterValueChangeHandler<SimpleRaceMetadataDTO>> list = handlersByTab.get(assosiatedTab);
            if (list == null) this.handlersByTab.put(assosiatedTab, list = new ArrayList<>());
            list.add(handler);
            super.addHandler(handler);
        }
    }
    
}
