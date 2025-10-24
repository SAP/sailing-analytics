package com.sap.sailing.gwt.home.mobile.places.event;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.windfinder.SpotDTO;
import com.sap.sailing.gwt.home.communication.event.EventAndLeaderboardReferenceWithStateDTO;
import com.sap.sailing.gwt.home.communication.eventview.EventViewDTO;
import com.sap.sailing.gwt.home.communication.eventview.RegattaMetadataDTO;
import com.sap.sailing.gwt.home.mobile.partials.eventheader.EventHeader;
import com.sap.sailing.gwt.home.mobile.partials.quickfinder.Quickfinder;
import com.sap.sailing.gwt.home.mobile.partials.section.MobileSection;
import com.sap.sailing.gwt.home.mobile.partials.sectionHeader.SectionHeaderContent;
import com.sap.sailing.gwt.home.mobile.partials.simpleinfoblock.SimpleInfoBlock;
import com.sap.sailing.gwt.home.mobile.places.QuickfinderPresenter;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.windfinder.WindfinderControl;
import com.sap.sailing.gwt.home.shared.places.event.EventContext;
import com.sap.sailing.gwt.home.shared.refresh.RefreshManager;
import com.sap.sailing.gwt.home.shared.refresh.RefreshManagerWithErrorAndBusy;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public abstract class AbstractEventView<P extends EventViewBase.Presenter> extends Composite implements EventViewBase {
    
    private static AbstractEventViewUiBinder uiBinder = GWT.create(AbstractEventViewUiBinder.class);

    interface AbstractEventViewUiBinder extends UiBinder<Widget, AbstractEventViewLayout> {
    }
    
    static class AbstractEventViewLayout {
        @UiField(provided = true) EventHeader eventHeaderUi;
        @UiField Quickfinder quickFinderUi;
        @UiField SimpleInfoBlock sailorInfoUi;
        @UiField SimpleInfoBlock seriesNavigationoUi;
        @UiField DivElement windfinderWrapperUi;
        @UiField(provided = true) WindfinderControl windfinderUi;
        @UiField SimplePanel viewContentUi;
        
        private AbstractEventViewLayout(EventContext eventContext, EventViewDTO event, String regattaName, PlaceNavigation<?> logoNavigation, TakedownNoticeService takedownNoticeService) {
            this.eventHeaderUi = new EventHeader(eventContext, event, regattaName, logoNavigation, takedownNoticeService);
            this.windfinderUi = new WindfinderControl(SpotDTO::getCurrentlyMostAppropriateUrl);
        }
    }

    protected final P currentPresenter;
    protected final RefreshManager refreshManager;
    private final AbstractEventViewLayout layout;
    
    private final SimplePanel contentRoot = new SimplePanel();

    public AbstractEventView(P presenter, boolean showRegattaName, boolean enableLogoNavigation) {
        this(presenter, showRegattaName, enableLogoNavigation, true);
    }
    
    public AbstractEventView(P presenter, boolean showRegattaName, boolean enableLogoNavigation,
            boolean supportsRefresh) {
        this.currentPresenter = presenter;
        String regattaName = showRegattaName ? currentPresenter.getRegatta().getDisplayName() : null;
        PlaceNavigation<?> logoNavigation = enableLogoNavigation ? currentPresenter.getEventNavigation() : null;
        this.layout = new AbstractEventViewLayout(currentPresenter.getCtx(), currentPresenter.getEventDTO(), regattaName, logoNavigation, currentPresenter.getUserService());
        initWidget(uiBinder.createAndBindUi(this.layout));
        if (supportsRefresh) {
            this.refreshManager = new RefreshManagerWithErrorAndBusy(contentRoot, layout.viewContentUi,
                    currentPresenter.getDispatch(), currentPresenter.getErrorAndBusyClientFactory());
        } else {
            this.refreshManager = null;
            layout.viewContentUi.setWidget(contentRoot);
        }
    }
    
    protected void setViewContent(Widget contentWidget) {
        contentRoot.setWidget(contentWidget);
    }
    
    protected UUID getEventId() {
        return currentPresenter.getEventDTO().getId();
    }
    
    protected String getRegattaId() {
        return currentPresenter.getRegattaId();
    }
    
    protected boolean isMultiRegattaEvent() {
        return currentPresenter.isMultiRegattaEvent();
    }
    
    protected void setQuickFinderValues(Quickfinder quickfinder,
            Map<String, Set<RegattaMetadataDTO>> regattasByLeaderboardGroupName) {
        QuickfinderPresenter.getForRegattaLeaderboards(quickfinder, currentPresenter, regattasByLeaderboardGroupName);
    }
    
    protected void setQuickFinderValues(Quickfinder quickfinder, String seriesName,
            Collection<EventAndLeaderboardReferenceWithStateDTO> eventsOfSeries) {
        QuickfinderPresenter.getForSeriesLeaderboards(quickfinder, seriesName, currentPresenter, eventsOfSeries);
    }
    
    protected void initRacesNavigation(Panel container) {
        final MobileSection mobileSection = new MobileSection();
        final SectionHeaderContent header = new SectionHeaderContent();
        final RegattaMetadataDTO regatta = currentPresenter.getRegatta();
        header.setSectionTitle(StringMessages.INSTANCE.racesCount(regatta == null ? 0 : regatta.getRaceCount()));
        header.setInfoText(StringMessages.INSTANCE.showAll());
        header.setClickAction(currentPresenter.getRegattaRacesNavigation(getRegattaId()));
        mobileSection.addHeader(header);
        mobileSection.setEdgeToEdgeContent(true);
        container.add(mobileSection);
    }
    
    @Override
    public void setQuickFinderValues(Map<String, Set<RegattaMetadataDTO>> regattasByLeaderboardGroupName) {
        setQuickFinderValues(layout.quickFinderUi, regattasByLeaderboardGroupName);
    }

    @Override
    public final void setQuickFinderValues(String seriesName,
            Collection<EventAndLeaderboardReferenceWithStateDTO> eventsOfSeries) {
        setQuickFinderValues(layout.quickFinderUi, seriesName, eventsOfSeries);
    }
    
    @Override
    public final void hideQuickfinder() {
        layout.quickFinderUi.removeFromParent();
    }
    
    @Override
    public void setSailorInfos(String description, String buttonLabel, String url) {
        layout.sailorInfoUi.setDescription(SafeHtmlUtils.fromString(description.replace("\n", " ")));
        layout.sailorInfoUi.setAction(buttonLabel, url);
    }
    
    @Override
    public void setWindfinderNavigations(Iterable<SpotDTO> spotData) {
        layout.windfinderWrapperUi.getStyle().clearDisplay();
        layout.windfinderUi.setSpotData(spotData);
    }

    @Override
    public void setSeriesNavigation(String buttonLabel, PlaceNavigation<?> placeNavigation) {
        layout.seriesNavigationoUi.setAction(buttonLabel, placeNavigation);
    }
    
}
