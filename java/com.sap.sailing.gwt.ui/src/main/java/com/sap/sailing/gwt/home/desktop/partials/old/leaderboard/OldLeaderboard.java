package com.sap.sailing.gwt.home.desktop.partials.old.leaderboard;

import java.util.Date;
import java.util.Objects;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.gwt.home.desktop.partials.old.EventRegattaLeaderboardResources;
import com.sap.sailing.gwt.home.desktop.partials.old.LeaderboardDelegate;
import com.sap.sailing.gwt.settings.client.leaderboard.MultiRaceLeaderboardSettings;
import com.sap.sailing.gwt.ui.client.DebugIdHelper;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.MultiRaceLeaderboardPanel;
import com.sap.sailing.gwt.ui.leaderboard.ScoringSchemeTypeFormatter;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.controls.busyindicator.BusyIndicator;
import com.sap.sse.gwt.client.controls.busyindicator.BusyStateChangeListener;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.gwt.client.shared.components.SettingsDialog;

public class OldLeaderboard extends Composite implements BusyStateChangeListener {
    private static OldLeaderboardUiBinder uiBinder = GWT.create(OldLeaderboardUiBinder.class);

    interface OldLeaderboardUiBinder extends UiBinder<Widget, OldLeaderboard> {
    }

    @UiField HTMLPanel oldLeaderboardPanel;

    @UiField Anchor settingsAnchor;
    @UiField Anchor autoRefreshAnchor;
    @UiField Anchor showLiveRacesAnchor;
    @UiField Anchor fullscreenAnchor;
    @UiField DivElement lastScoringUpdateTimeDiv;
    @UiField DivElement lastScoringUpdateTextDiv;
    @UiField DivElement lastScoringCommentDiv;
    @UiField DivElement scoringSchemeDiv;
    @UiField DivElement hasLiveRaceDiv;
    @UiField BusyIndicator busyIndicator;
    @UiField EventRegattaLeaderboardResources local_res;

    private final OldLeaderboardDelegate delegate;
    private MultiRaceLeaderboardPanel leaderboardPanel;
    private Timer autoRefreshTimer;
    
    public OldLeaderboard(final OldLeaderboardDelegate delegate) {
        this.leaderboardPanel = null;
        EventRegattaLeaderboardResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        settingsAnchor.setTitle(StringMessages.INSTANCE.settings());
        autoRefreshAnchor.setTitle(StringMessages.INSTANCE.refresh());
        showLiveRacesAnchor.setTitle(StringMessages.INSTANCE.showLiveNow());
        fullscreenAnchor.setTitle(StringMessages.INSTANCE.openFullscreenView());
        this.delegate = delegate;
        this.setupFullscreenDelegate();
    }
    
    private void setupFullscreenDelegate() {
        if (delegate == null) {
            fullscreenAnchor.removeFromParent();
            return;
        }
        delegate.addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {
                if (leaderboardPanel != null) {
                    leaderboardPanel.removeFromParent();
                    oldLeaderboardPanel.add(leaderboardPanel);
                }
            }
        });
        delegate.getSettingsControl().setTitle(StringMessages.INSTANCE.settings());
        delegate.getSettingsControl().addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                OldLeaderboard.this.settingsClicked(event);
            }
        }, ClickEvent.getType());
        delegate.getAutoRefreshControl().setTitle(StringMessages.INSTANCE.refresh());
        delegate.getAutoRefreshControl().addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                OldLeaderboard.this.toogleAutoRefreshClicked(event);
            }
        }, ClickEvent.getType());
    }
    
    /**
     * This method turns on auto playing mode on leaderboard
     */
    private void turnOnAutoPlay() {
        if (autoRefreshTimer.getPlayState() != PlayStates.Playing) {
            autoRefreshTimer.setPlayMode(PlayModes.Live);
        }
        // Styles applied each time because of tabs switching. In this case play mode stays as Playing but style is lost
        this.updateAutoRefreshStylesDependingOnPlayState();
    }
    
    private void updateAutoRefreshStylesDependingOnPlayState() {
        final boolean isPlaying = autoRefreshTimer != null && autoRefreshTimer.getPlayState() == PlayStates.Playing;
        final String isPlayingStyleName = local_res.css().regattaleaderboard_meta_reload_live();
        this.autoRefreshAnchor.setStyleName(isPlayingStyleName, isPlaying);
        if (Objects.nonNull(delegate)) {
            this.delegate.getAutoRefreshControl().setStyleName(isPlayingStyleName, isPlaying);
        }
    }

    @UiHandler("autoRefreshAnchor")
    void toogleAutoRefreshClicked(ClickEvent event) {
        autoRefreshAnchor.removeStyleName(local_res.css().regattaleaderboard_meta_reload_live());
        autoRefreshAnchor.removeStyleName(local_res.css().regattaleaderboard_meta_reload_playing());
        if (delegate != null) {
            delegate.getAutoRefreshControl().removeStyleName(local_res.css().regattaleaderboard_meta_reload_live());
            delegate.getAutoRefreshControl().removeStyleName(local_res.css().regattaleaderboard_meta_reload_playing());
        }
        if (autoRefreshTimer != null) {
            if (autoRefreshTimer.getPlayState() == PlayStates.Playing) {
                autoRefreshTimer.pause();
            } else {
                // playing the standalone leaderboard means putting it into live mode
                autoRefreshTimer.setPlayMode(PlayModes.Live);
                this.updateAutoRefreshStylesDependingOnPlayState();
            }
        }
    }
    
    @UiHandler("settingsAnchor")
    void settingsClicked(ClickEvent event) {
        if(leaderboardPanel != null) {
            final String componentName = leaderboardPanel.getLocalizedShortName();
            final String debugIdPrefix = DebugIdHelper.createDebugId(componentName);

            SettingsDialog<MultiRaceLeaderboardSettings> dialog = new SettingsDialog<MultiRaceLeaderboardSettings>(leaderboardPanel, StringMessages.INSTANCE) {
                protected Widget getAdditionalWidget() {
                    Widget additionalWidget = super.getAdditionalWidget();
                    if (!oldLeaderboardPanel.getElement().isOrHasChild(leaderboardPanel.getElement())) {
                        additionalWidget.getElement().getStyle().setProperty("maxWidth", Window.getClientWidth()-75, Unit.PX);
                        additionalWidget.getElement().getStyle().setOverflow(Overflow.AUTO);
                    }
                    return additionalWidget;
                }
            };
            dialog.ensureDebugId(debugIdPrefix + "SettingsDialog");
            dialog.show();
        }
    }
    
    @UiHandler("fullscreenAnchor")
    void fullscreenClicked(ClickEvent event) {
        if(leaderboardPanel != null && delegate != null) {
            leaderboardPanel.removeFromParent();
            delegate.setLeaderboardPanel(leaderboardPanel);
        }
    }

    /**
     * Provides access to the {@link OldLeaderboard leaderboard}'s control to show live races.
     * 
     * @return the {@link FocusWidget control} for showing live races.
     */
    public FocusWidget getShowLiveRacesControl() {
        return showLiveRacesAnchor;
    }

    /**
     * Provides access to the {@link OldLeaderboard leaderboard}'s control for fullscreen mode.
     * 
     * @return the {@link FocusWidget control} for fullscreen mode.
     */
    public FocusWidget getFullscreenControl() {
        return fullscreenAnchor;
    }

    public void setLeaderboard(MultiRaceLeaderboardPanel leaderboardPanel, final Timer timer) {
        this.autoRefreshTimer = timer;
        this.leaderboardPanel = leaderboardPanel;
        if (leaderboardPanel.getComponentContext() == null) {
            throw new IllegalStateException("Leaderboard Component with null Context");
        }
        this.updateAutoRefreshStylesDependingOnPlayState();
        oldLeaderboardPanel.add(leaderboardPanel);
        leaderboardPanel.addBusyStateChangeListener(this);
    }

    public void updatedLeaderboard(LeaderboardDTO leaderboard) {
        final boolean hasLiveRace = leaderboardPanel.hasLiveRace();
        if (leaderboard != null) {
            String comment = leaderboard.getComment() != null ? leaderboard.getComment() : "";
            String scoringScheme = leaderboard.scoringScheme != null ? ScoringSchemeTypeFormatter.getDescription(leaderboard.scoringScheme, StringMessages.INSTANCE) : "";
            lastScoringCommentDiv.setInnerText(comment);
            hasLiveRaceDiv.setInnerText(leaderboardPanel.getLiveRacesText());
            scoringSchemeDiv.setInnerText(scoringScheme);
            if (delegate != null) {
                delegate.getLastScoringCommentElement().setInnerText(comment);
                delegate.getHasLiveRaceElement().setInnerText(leaderboardPanel.getLiveRacesText());
                delegate.getScoringSchemeElement().setInnerText(scoringScheme);
            }
            if (leaderboard.getTimePointOfLastCorrectionsValidity() != null) {
                Date lastCorrectionDate = leaderboard.getTimePointOfLastCorrectionsValidity();
                // FIXME bug5972: make sure the two formatters use the same time zone!
                String lastUpdate = DateAndTimeFormatterUtil.defaultDateFormatterUTC.render(lastCorrectionDate) + ", "
                        + DateAndTimeFormatterUtil.longTimeFormatterUTC.render(lastCorrectionDate);
                lastScoringUpdateTimeDiv.setInnerText(lastUpdate);
                lastScoringUpdateTextDiv.setInnerText(StringMessages.INSTANCE.eventRegattaLeaderboardLastScoreUpdate());
                if (delegate != null) {
                    delegate.getLastScoringUpdateTimeElement().setInnerText(lastUpdate);
                    delegate.getLastScoringUpdateTextElement().setInnerText(StringMessages.INSTANCE.eventRegattaLeaderboardLastScoreUpdate());
                }
            } else {
                lastScoringUpdateTimeDiv.setInnerHTML("&nbsp;");
                lastScoringUpdateTextDiv.setInnerHTML("&nbsp;");
            }
            setVisible(hasLiveRaceDiv, hasLiveRace);
            setVisible(lastScoringCommentDiv, !hasLiveRace);
            setVisible(lastScoringUpdateTextDiv, !hasLiveRace);
            setVisible(lastScoringUpdateTimeDiv, !hasLiveRace);
            setVisible(scoringSchemeDiv, true);
            if (delegate != null) {
                setVisible(delegate.getHasLiveRaceElement(), hasLiveRace);
                setVisible(delegate.getLastScoringCommentElement(), !hasLiveRace);
                setVisible(delegate.getLastScoringUpdateTextElement(), !hasLiveRace);
                setVisible(delegate.getLastScoringUpdateTimeElement(), !hasLiveRace);
                setVisible(delegate.getScoringSchemeElement(), true);
            }
            if (hasLiveRace) {
                turnOnAutoPlay();
            }
        }
    }

    public void hideRefresh() {
        autoRefreshAnchor.setVisible(false);
        if (delegate != null) {
            delegate.getAutoRefreshControl().setVisible(false);
        }
        lastScoringUpdateTimeDiv.getStyle().setVisibility(Visibility.HIDDEN);
    }

    @Override
    public void onBusyStateChange(boolean busyState) {
        busyIndicator.setBusy(busyState);
        if (delegate != null) {
            delegate.setBusyState(busyState);
        }
    }

    public interface OldLeaderboardDelegate extends LeaderboardDelegate {
        Element getHasLiveRaceElement();
    }

}
