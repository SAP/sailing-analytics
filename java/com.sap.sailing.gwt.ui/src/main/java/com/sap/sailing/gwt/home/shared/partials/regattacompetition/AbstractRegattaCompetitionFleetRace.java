package com.sap.sailing.gwt.home.shared.partials.regattacompetition;

import static com.sap.sailing.gwt.ui.common.client.DateAndTimeFormatterUtil.shortTimeFormatter;
import static com.sap.sailing.gwt.ui.common.client.DateAndTimeFormatterUtil.weekdayMonthAbbrDayDateFormatter;
import static com.sap.sse.common.impl.MillisecondsTimePoint.now;

import java.util.Date;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.text.client.DateTimeFormatRenderer;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.DateUtil;
import com.sap.sailing.gwt.home.communication.race.SimpleRaceMetadataDTO;
import com.sap.sailing.gwt.home.communication.race.SimpleRaceMetadataDTO.RaceTrackingState;
import com.sap.sailing.gwt.home.communication.race.SimpleRaceMetadataDTO.RaceViewState;
import com.sap.sailing.gwt.home.desktop.partials.raceviewerlaunchpad.RaceviewerLaunchPad;
import com.sap.sailing.gwt.home.shared.partials.regattacompetition.RegattaCompetitionView.RegattaCompetitionRaceView;
import com.sap.sailing.gwt.ui.client.StringMessages;

public abstract class AbstractRegattaCompetitionFleetRace extends Widget implements RegattaCompetitionRaceView {
    
    private static final StringMessages I18N = StringMessages.INSTANCE;
    protected final Element mainElement;
    private final SimpleRaceMetadataDTO race;
    private final RegattaCompetitionPresenter presenter;
    private final PopupPanel panel = new PopupPanel(true, false);

    protected AbstractRegattaCompetitionFleetRace(final SimpleRaceMetadataDTO race,
            RegattaCompetitionPresenter presenter) {
        this.race = race;
        this.presenter = presenter;
        this.mainElement = getMainUiElement();
        setupRaceState(race.getTrackingState(), race.getViewState());
        getRaceNameUiElement().setInnerText(race.getRaceName());
        setupRaceStart(race.getStart());
        setElement(mainElement);
        if (race.hasValidTrackingData()) {
            sinkEvents(Event.ONCLICK);
        }
    }
    
    @Override
    public void onBrowserEvent(Event event) {
        if (race.hasValidTrackingData() && event.getTypeInt() == Event.ONCLICK) {
            panel.setWidget(new RaceviewerLaunchPad(race) {
                @Override
                protected String getRaceViewerURL(SimpleRaceMetadataDTO data, String mode) {
                    return presenter.getRaceViewerURL(data, mode);
                }
            });
            panel.setVisible(false);
            panel.show();
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
                    Widget button = AbstractRegattaCompetitionFleetRace.this, panelContent = panel.getWidget();
                    int alignRight = button.getAbsoluteLeft() + button.getOffsetWidth() - panelContent.getOffsetWidth();
                    int left = (alignRight < 0 ? button.getAbsoluteLeft() - 1 : alignRight + 1);
                    int top = button.getAbsoluteTop() + button.getOffsetHeight() - panelContent.getOffsetHeight() + 1;
                    panel.setPopupPosition(left, top);
                    panel.setVisible(true);
                }
            });
        }
    }
    
    private void setupRaceState(RaceTrackingState trackingState, RaceViewState viewState) {
        // boolean isUntrackedRace = trackingState != RaceTrackingState.TRACKED_VALID_DATA;
        boolean isUntrackedRace = isUntrackedRace(trackingState);
        if (viewState == RaceViewState.RUNNING) {
            mainElement.addClassName(getRaceLiveStyleName());
            getRaceStateUiElement().setInnerText(isUntrackedRace ? I18N.live() : I18N.actionWatch());
        } else if (viewState == RaceViewState.FINISHED) {
            getRaceStateUiElement().setInnerText(isUntrackedRace ? I18N.raceIsFinished() : I18N.actionAnalyze());
        } else {
            mainElement.addClassName(getRacePlannedStyleName());
            if (viewState == RaceViewState.SCHEDULED) getRaceStateUiElement().setInnerText(I18N.raceIsPlanned());
            else getRaceStateUiElement().setInnerText(viewState.getLabel());
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
    
    // TODO: As long as there is no mobile race viewer, show all races as untracked
    //       This is a temporary method to be able to fulfill this requirement
    protected boolean isUntrackedRace(RaceTrackingState trackingState) {
        return trackingState != RaceTrackingState.TRACKED_VALID_DATA;
    }
}
