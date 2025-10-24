package com.sap.sailing.gwt.home.shared.partials.regattacompetition;

import static com.sap.sse.common.impl.MillisecondsTimePoint.now;
import static com.sap.sse.gwt.client.DateAndTimeFormatterUtil.shortTimeFormatter;
import static com.sap.sse.gwt.client.DateAndTimeFormatterUtil.weekdayMonthAbbrDayDateFormatter;

import java.util.Date;

import com.google.gwt.dom.client.Element;
import com.google.gwt.text.client.DateTimeFormatRenderer;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.DateUtil;
import com.sap.sailing.gwt.home.communication.race.SimpleRaceMetadataDTO;
import com.sap.sailing.gwt.home.desktop.partials.raceviewerlaunchpad.RaceviewerLaunchPadController;
import com.sap.sailing.gwt.home.shared.partials.regattacompetition.RegattaCompetitionView.RegattaCompetitionRaceView;
import com.sap.sailing.gwt.ui.client.StringMessages;

public abstract class AbstractRegattaCompetitionFleetRace extends Widget implements RegattaCompetitionRaceView {
    
    private static final StringMessages I18N = StringMessages.INSTANCE;
    private final RaceviewerLaunchPadController<SimpleRaceMetadataDTO> launchPadController;
    protected final Element mainElement;
    private final SimpleRaceMetadataDTO race;

    protected AbstractRegattaCompetitionFleetRace(final SimpleRaceMetadataDTO race, String fleetName,
            RegattaCompetitionPresenter presenter) {
        this.race = race;
        this.launchPadController = new RaceviewerLaunchPadController<>(presenter::getRaceViewerURL,
                r -> presenter.getMapAndWindChartUrl(r.getLeaderboardName(), r.getRaceName(), fleetName),
                presenter.getPaywallResolver());
        this.mainElement = getMainUiElement();
        setupRaceState(race);
        getRaceNameUiElement().setInnerText(race.getRaceName());
        setupRaceStart(race.getStart());
        setElement(mainElement);
        if (race.hasValidTrackingData()) {
            sinkEvents(Event.ONCLICK);
        }
    }
    
    @Override
    public void onBrowserEvent(Event event) {
        // Only handle click events if valid tracking data is available
        if (!race.hasValidTrackingData() || event.getTypeInt() != Event.ONCLICK) {
            return; 
        }

        // If rendered as direct link button, open link in new tab directly instead of showing the menu popup
        if (launchPadController.renderAsDirectLink(race)) {
            Window.open(launchPadController.getDirectLinkUrl(race), "_blank", "");
            return;
        }
        
        launchPadController.showLaunchPad(race, this.getElement());
    }
    
    private void setupRaceState(SimpleRaceMetadataDTO race) {
        final boolean isUntrackedRace = !race.hasValidTrackingData();
        if (race.isRunning()) {
            mainElement.addClassName(getRaceLiveStyleName());
            getRaceStateUiElement().setInnerText(isUntrackedRace ? I18N.live() : I18N.actionWatch());
        } else if (race.isFinished()) {
            getRaceStateUiElement().setInnerText(isUntrackedRace ? I18N.raceIsFinished() : I18N.tracking());
        } else {
            mainElement.addClassName(getRacePlannedStyleName());
            if (race.isScheduled()) {
                getRaceStateUiElement().setInnerText(I18N.raceIsPlanned());
            } else {
                getRaceStateUiElement().setInnerText(race.getViewState().getLabel());
            }
        }
        setStyleName(mainElement, getRaceUntrackedStyleName(), isUntrackedRace);
    }
    
    private void setupRaceStart(Date startDate) {
        if (startDate != null) {
            boolean showTime = DateUtil.isSameDayOfMonth(now().asDate(), startDate);
            DateTimeFormatRenderer renderer = showTime ? shortTimeFormatter : weekdayMonthAbbrDayDateFormatter;
            getRaceDateUiElement().setInnerText(renderer.render(startDate));
        }
    }

    protected abstract Element getMainUiElement();
    
    protected abstract Element getRaceNameUiElement();
    
    protected abstract Element getRaceStateUiElement();
    
    protected abstract Element getRaceDateUiElement();
    
    protected abstract String getRaceLiveStyleName();
    
    protected abstract String getRacePlannedStyleName();
    
    protected abstract String getRaceUntrackedStyleName();
    
}
