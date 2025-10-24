package com.sap.sailing.gwt.home.mobile.places.series.minileaderboard;

import java.util.Collection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.EventAndLeaderboardReferenceWithStateDTO;
import com.sap.sailing.gwt.home.communication.event.minileaderboard.GetMiniOverallLeaderbordAction;
import com.sap.sailing.gwt.home.mobile.partials.minileaderboard.MinileaderboardBox;
import com.sap.sailing.gwt.home.mobile.partials.quickfinder.Quickfinder;
import com.sap.sailing.gwt.home.mobile.partials.seriesheader.SeriesHeader;
import com.sap.sailing.gwt.home.mobile.places.QuickfinderPresenter;
import com.sap.sailing.gwt.home.shared.refresh.LifecycleRefreshManager;
import com.sap.sailing.gwt.home.shared.refresh.RefreshManager;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class SeriesMiniOverallLeaderboardViewImpl extends Composite implements SeriesMiniOverallLeaderboardView {
    private static final StringMessages MSG = StringMessages.INSTANCE;
    private static MyBinder uiBinder = GWT.create(MyBinder.class);

    interface MyBinder extends UiBinder<Widget, SeriesMiniOverallLeaderboardViewImpl> {
    }

    private Presenter currentPresenter;
    @UiField(provided = true)
    SeriesHeader eventHeaderUi;
    @UiField Quickfinder quickFinderUi;
    @UiField(provided = true)
    MinileaderboardBox minileaderboardUi;

    public SeriesMiniOverallLeaderboardViewImpl(Presenter presenter, FlagImageResolver flagImageResolver) {
        this.currentPresenter = presenter;
        minileaderboardUi = new MinileaderboardBox(true, flagImageResolver);
        eventHeaderUi = new SeriesHeader(presenter.getCtx(), presenter.getSeriesDTO(), presenter.getSeriesNavigation(), presenter.getUserService());
        initWidget(uiBinder.createAndBindUi(this));
        RefreshManager refreshManager = new LifecycleRefreshManager(this, currentPresenter.getDispatch());
        refreshManager.add(minileaderboardUi, new GetMiniOverallLeaderbordAction(presenter.getCtx().getLeaderboardGroupId(),
                presenter.getSeriesDTO().getLeaderboardId()));
        minileaderboardUi.setAction(MSG.details(), presenter.getOverallLeaderboardNavigation());
    }
    
    @Override
    public void setQuickFinderValues(String seriesName,
            Collection<EventAndLeaderboardReferenceWithStateDTO> eventsOfSeries) {
        QuickfinderPresenter.getForSeriesLeaderboards(quickFinderUi, seriesName, currentPresenter, eventsOfSeries);
    }
}
