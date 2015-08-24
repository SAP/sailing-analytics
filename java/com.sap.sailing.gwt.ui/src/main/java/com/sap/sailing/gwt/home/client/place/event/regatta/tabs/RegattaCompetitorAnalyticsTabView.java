package com.sap.sailing.gwt.home.client.place.event.regatta.tabs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabView;
import com.sap.sailing.gwt.home.client.place.event.oldcompetitorcharts.OldCompetitorCharts;
import com.sap.sailing.gwt.home.client.place.event.oldcompetitorcharts.OldCompetitorChartsDelegateFullscreenViewer;
import com.sap.sailing.gwt.home.client.place.event.regatta.EventRegattaView;
import com.sap.sailing.gwt.home.client.place.event.regatta.EventRegattaView.Presenter;
import com.sap.sailing.gwt.home.client.place.event.regatta.RegattaAnalyticsDataManager;
import com.sap.sailing.gwt.home.client.shared.placeholder.Placeholder;
import com.sap.sailing.gwt.home.shared.ExperimentalFeatures;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionChangeListener;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionModel;
import com.sap.sse.common.Util;

/**
 * Created by pgtaboada on 25.11.14.
 */
public class RegattaCompetitorAnalyticsTabView extends SharedLeaderboardRegattaTabView<RegattaCompetitorAnalyticsPlace> {

    interface MyBinder extends UiBinder<HTMLPanel, RegattaCompetitorAnalyticsTabView> {
    }

    private static MyBinder ourUiBinder = GWT.create(MyBinder.class);
    private Presenter currentPresenter;

    private final int MAX_COMPETITORS_IN_CHART = 30; 

    public RegattaCompetitorAnalyticsTabView() {
        super();
    }

    @UiField(provided = true)
    protected OldCompetitorCharts competitorCharts = new OldCompetitorCharts(
            ExperimentalFeatures.SHOW_COMPETITOR_ANALYTICS_FULLSCREEN_VIEWER ? new OldCompetitorChartsDelegateFullscreenViewer() : null);

    @Override
    public Class<RegattaCompetitorAnalyticsPlace> getPlaceClassForActivation() {
        return RegattaCompetitorAnalyticsPlace.class;
    }
    
    @Override
    public TabView.State getState() {
        return currentPresenter.getCtx().getEventDTO().isHasAnalytics() ? TabView.State.VISIBLE : TabView.State.INVISIBLE;
    }

    @Override
    public void start(RegattaCompetitorAnalyticsPlace myPlace, AcceptsOneWidget contentArea) {
        contentArea.setWidget(new Placeholder());
        String regattaId = myPlace.getRegattaId();

        if (regattaId != null && !regattaId.isEmpty()) {
            String leaderboardName = regattaId;
            RegattaAnalyticsDataManager regattaAnalyticsManager = currentPresenter.getCtx().getRegattaAnalyticsManager();
            if(regattaAnalyticsManager.getLeaderboardPanel() == null) {
                createSharedLeaderboardPanel(leaderboardName, regattaAnalyticsManager);
            }

            initWidget(ourUiBinder.createAndBindUi(this));

            DetailType initialDetailType = DetailType.REGATTA_RANK;
            if (regattaAnalyticsManager.getMultiCompetitorChart() == null) {
                regattaAnalyticsManager.createMultiCompetitorChart(leaderboardName, initialDetailType);
            }
            competitorCharts.setChart(regattaAnalyticsManager.getMultiCompetitorChart(), getAvailableDetailsTypes(),
                    initialDetailType);

            regattaAnalyticsManager.showCompetitorChart(competitorCharts.getSelectedChartDetailType());
            contentArea.setWidget(this);
        }
    }

    private List<DetailType> getAvailableDetailsTypes() {
        List<DetailType> availableDetailsTypes = new ArrayList<DetailType>();
        availableDetailsTypes.add(DetailType.REGATTA_RANK);
        availableDetailsTypes.add(DetailType.REGATTA_TOTAL_POINTS_SUM);
        return availableDetailsTypes;
    }

    @Override
    public void stop() {
    }

    @Override
    public RegattaCompetitorAnalyticsPlace placeToFire() {
        return new RegattaCompetitorAnalyticsPlace(currentPresenter.getCtx());
    }

    @Override
    public void setPresenter(EventRegattaView.Presenter currentPresenter) {
        this.currentPresenter = currentPresenter;
    }

    public boolean isSmallWidth() {
        int width = Window.getClientWidth();
        return width <= 720;
    }

    @Override
    public void updatedLeaderboard(LeaderboardDTO leaderboard) {
        // adjust the competitor selection for the chart in case the leaderboard changed
        updateCompetitorSelection();
    }

    private void updateCompetitorSelection() {
        RegattaAnalyticsDataManager regattaAnalyticsManager = currentPresenter.getCtx().getRegattaAnalyticsManager();
        CompetitorSelectionModel competitorSelectionProvider = regattaAnalyticsManager.getCompetitorSelectionProvider();

        // preselect the top N competitors in case there was no selection before and there too many competitors for a chart
        int competitorsCount = Util.size(competitorSelectionProvider.getAllCompetitors());
        int selectedCompetitorsCount = Util.size(competitorSelectionProvider.getSelectedCompetitors());
        
        if(selectedCompetitorsCount == 0 && competitorsCount > MAX_COMPETITORS_IN_CHART) {
            List<CompetitorDTO> selectedCompetitors = new ArrayList<CompetitorDTO>();
            Iterator<CompetitorDTO> allCompetitorsIt = competitorSelectionProvider.getAllCompetitors().iterator();
            int counter = 0;
            while(counter < MAX_COMPETITORS_IN_CHART) {
                selectedCompetitors.add(allCompetitorsIt.next());
                counter++;
            }
            competitorSelectionProvider.setSelection(selectedCompetitors, (CompetitorSelectionChangeListener[]) null);
        }
    }

    @Override
    public void currentRaceSelected(RaceIdentifier raceIdentifier, RaceColumnDTO raceColumn) {
    }
}