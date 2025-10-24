package com.sap.sailing.gwt.home.desktop.partials.old.multileaderboard;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
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
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.gwt.home.desktop.partials.old.EventRegattaLeaderboardResources;
import com.sap.sailing.gwt.home.desktop.partials.old.LeaderboardDelegate;
import com.sap.sailing.gwt.settings.client.leaderboard.MultiRaceLeaderboardSettings;
import com.sap.sailing.gwt.ui.client.DebugIdHelper;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.MultiLeaderboardProxyPanel;
import com.sap.sailing.gwt.ui.leaderboard.MultiRaceLeaderboardPanel;
import com.sap.sailing.gwt.ui.leaderboard.ScoringSchemeTypeFormatter;
import com.sap.sailing.gwt.ui.leaderboard.SelectedLeaderboardChangeListener;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.controls.busyindicator.BusyIndicator;
import com.sap.sse.gwt.client.controls.busyindicator.BusyStateChangeListener;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.gwt.client.shared.components.SettingsDialog;

public class OldMultiLeaderboard extends Composite implements SelectedLeaderboardChangeListener<MultiRaceLeaderboardPanel>, BusyStateChangeListener {
    private static OldMultiLeaderboardUiBinder uiBinder = GWT.create(OldMultiLeaderboardUiBinder.class);

    interface OldMultiLeaderboardUiBinder extends UiBinder<Widget, OldMultiLeaderboard> {
    }

    @UiField HTMLPanel oldMultiLeaderboardPanel;
    
    @UiField Anchor settingsAnchor;
    @UiField Anchor autoRefreshAnchor;
    @UiField Anchor fullscreenAnchor;
    @UiField DivElement lastScoringUpdateTimeDiv;
    @UiField DivElement lastScoringUpdateTextDiv;
    @UiField DivElement lastScoringCommentDiv;
    @UiField DivElement scoringSchemeDiv;
    @UiField BusyIndicator busyIndicator;
    @UiField EventRegattaLeaderboardResources local_res;

    private MultiLeaderboardProxyPanel multiLeaderboardPanel;
    private Timer autoRefreshTimer;
    private final OldMultiLeaderboardDelegate delegate;
    private MultiRaceLeaderboardPanel lastSelectedLeaderboardPanel;

    public OldMultiLeaderboard() {
        this(null);
    }
    
    public OldMultiLeaderboard(OldMultiLeaderboardDelegate delegate) {
        this.multiLeaderboardPanel = null;
        EventRegattaLeaderboardResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        settingsAnchor.setTitle(StringMessages.INSTANCE.settings());
        autoRefreshAnchor.setTitle(StringMessages.INSTANCE.refresh());
        fullscreenAnchor.setTitle(StringMessages.INSTANCE.openFullscreenView());
        this.delegate = delegate;
        this.setupFullscreenDelegate();
        lastSelectedLeaderboardPanel = null;
    }
    
    private void setupFullscreenDelegate() {
        if (delegate == null) {
            fullscreenAnchor.removeFromParent();
            return;
        }
        delegate.addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {
                if (multiLeaderboardPanel != null) {
                    multiLeaderboardPanel.removeFromParent();
                    oldMultiLeaderboardPanel.add(multiLeaderboardPanel);
                }
            }
        });
        delegate.getSettingsControl().setTitle(StringMessages.INSTANCE.settings());
        delegate.getSettingsControl().addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                OldMultiLeaderboard.this.settingsClicked(event);
            }
        }, ClickEvent.getType());
        delegate.getAutoRefreshControl().setTitle(StringMessages.INSTANCE.refresh());
        delegate.getAutoRefreshControl().addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                OldMultiLeaderboard.this.toogleAutoRefreshClicked(event);
            }
        }, ClickEvent.getType());
    }

    /**
     * This method turns on auto playing mode on leaderboard
     */
    public void turnOnAutoPlay() {
        if (autoRefreshTimer.getPlayState() != PlayStates.Playing) {
            autoRefreshTimer.setPlayMode(PlayModes.Live);
        }
        
        // Styles applied each time because of tabs switching. In this case play mode stays as Playing but styling is lost
        autoRefreshAnchor.addStyleName(local_res.css().regattaleaderboard_meta_reload_live());
        if (delegate != null) {
            delegate.getAutoRefreshControl().addStyleName(local_res.css().regattaleaderboard_meta_reload_live());
        }
    }

    @UiHandler("autoRefreshAnchor")
    void toogleAutoRefreshClicked(ClickEvent event) {
        if (autoRefreshTimer != null) {
            autoRefreshAnchor.removeStyleName(local_res.css().regattaleaderboard_meta_reload_live());
            autoRefreshAnchor.removeStyleName(local_res.css().regattaleaderboard_meta_reload_playing());
            if (delegate != null) {
                delegate.getAutoRefreshControl().removeStyleName(local_res.css().regattaleaderboard_meta_reload_live());
                delegate.getAutoRefreshControl()
                        .removeStyleName(local_res.css().regattaleaderboard_meta_reload_playing());
            }
            if (autoRefreshTimer.getPlayState() == PlayStates.Playing) {
                autoRefreshTimer.pause();
                // autoRefreshAnchor.getElement().getStyle().setBackgroundColor("#f0ab00");
                // autoRefreshAnchor.addStyleName(local_res.css().regattaleaderboard_meta_reload_playing());
                // if (delegate != null) {
                // delegate.getAutoRefreshControl().getElement().getStyle().setBackgroundColor("#f0ab00");
                // delegate.getAutoRefreshControl().addStyleName(local_res.css().regattaleaderboard_meta_reload_playing());
                // }
            } else {
                // playing the standalone leaderboard means putting it into live mode
                autoRefreshTimer.setPlayMode(PlayModes.Live);
                // autoRefreshAnchor.getElement().getStyle().setBackgroundColor("red");
                autoRefreshAnchor.addStyleName(local_res.css().regattaleaderboard_meta_reload_live());
                if (delegate != null) {
                    // delegate.getAutoRefreshControl().getElement().getStyle().setBackgroundColor("red");
                    delegate.getAutoRefreshControl()
                            .addStyleName(local_res.css().regattaleaderboard_meta_reload_live());
                }
            }
        }
    }
    
    @UiHandler("settingsAnchor")
    void settingsClicked(ClickEvent event) {
        if(multiLeaderboardPanel != null) {
            final String componentName = multiLeaderboardPanel.getLocalizedShortName();
            final String debugIdPrefix = DebugIdHelper.createDebugId(componentName);

            SettingsDialog<?> dialog = new SettingsDialog<MultiRaceLeaderboardSettings>(multiLeaderboardPanel, StringMessages.INSTANCE) {
                protected Widget getAdditionalWidget() {
                    Widget additionalWidget = super.getAdditionalWidget();
                    if (!oldMultiLeaderboardPanel.getElement().isOrHasChild(multiLeaderboardPanel.getElement())) {
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
        if(multiLeaderboardPanel != null && delegate != null) {
            multiLeaderboardPanel.removeFromParent();
            delegate.setLeaderboardPanel(multiLeaderboardPanel);
        }
    }

    public void setMultiLeaderboard(MultiLeaderboardProxyPanel multiLeaderboardPanel, final Timer timer) {
        this.autoRefreshTimer = timer;
        this.multiLeaderboardPanel = multiLeaderboardPanel;
        this.multiLeaderboardPanel.addSelectedLeaderboardChangeListener(this);

        oldMultiLeaderboardPanel.add(multiLeaderboardPanel);
    }

    public void updatedMultiLeaderboard(LeaderboardDTO leaderboard, boolean hasLiveRace) {
        if(leaderboard != null) {
            String comment = leaderboard.getComment() != null ? leaderboard.getComment() : "";
            String scoringScheme = leaderboard.scoringScheme != null ? ScoringSchemeTypeFormatter.getDescription(leaderboard.scoringScheme, StringMessages.INSTANCE) : "";
            lastScoringCommentDiv.setInnerText(comment);
            scoringSchemeDiv.setInnerText(scoringScheme);
            if (delegate != null) {
                delegate.getLastScoringCommentElement().setInnerText(comment);
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
                lastScoringUpdateTimeDiv.setInnerText("");
                lastScoringUpdateTextDiv.setInnerText("");
            }
            Visibility lastScoringUpdateTimeVisibility = !hasLiveRace ? Visibility.VISIBLE : Visibility.HIDDEN;
            lastScoringUpdateTimeDiv.getStyle().setVisibility(lastScoringUpdateTimeVisibility);
            if (delegate != null) {
                delegate.getLastScoringUpdateTimeElement().getStyle().setVisibility(lastScoringUpdateTimeVisibility);
            }
        }
    }

    @Override
    public void onBusyStateChange(boolean busyState) {
        busyIndicator.setBusy(busyState);
        if(delegate != null) {
            delegate.setBusyState(busyState);
        }
    }

    @Override
    public void onSelectedLeaderboardChanged(MultiRaceLeaderboardPanel selectedLeaderboard) {
        if(lastSelectedLeaderboardPanel != null) {
            lastSelectedLeaderboardPanel.removeBusyStateChangeListener(this);
        }
        selectedLeaderboard.addBusyStateChangeListener(this);
        lastSelectedLeaderboardPanel = selectedLeaderboard;
    }
    
    public interface OldMultiLeaderboardDelegate extends LeaderboardDelegate {
    }
}
