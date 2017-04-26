package com.sap.sailing.gwt.ui.leaderboard;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardSettingsFactory;
import com.sap.sailing.gwt.settings.client.leaderboard.MultiLeaderboardPanelLifecycle;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionModel;
import com.sap.sailing.gwt.ui.client.LeaderboardUpdateListener;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.TimeListener;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.shared.components.AbstractLazyComponent;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;

/**
 * A panel managing multiple {@link LeaderboardPanel}s (e.g. from a meta leaderboard) so that the user can switch between them. 
 * @author Frank
 */
public class MultiLeaderboardProxyPanel extends AbstractLazyComponent<LeaderboardSettings> implements TimeListener, SelectedLeaderboardChangeProvider {

    private LeaderboardPanel selectedLeaderboardPanel;
    private FlowPanel selectedLeaderboardFlowPanel;

    private final StringMessages stringMessages;
    private final ErrorReporter errorReporter;
    private final SailingServiceAsync sailingService;

    private String selectedLeaderboardName;

    private final AsyncActionsExecutor asyncActionsExecutor;
    private final boolean showRaceDetails;
    private final Timer timer;

    private VerticalPanel mainPanel;
    private final List<Util.Pair<String, String>> leaderboardNamesAndDisplayNames;

    private TabPanel leaderboardsTabPanel;
    private Label leaderboardsLabel;
    private final String metaLeaderboardName;
    private final boolean isEmbedded;

    private final Set<LeaderboardUpdateListener> leaderboardUpdateListeners;
    private final Set<SelectedLeaderboardChangeListener> selectedLeaderboardChangeListeners;
    private HashMap<String, LeaderboardSettings> contextStore;
    private LeaderboardSettings loadedSettings;

    public MultiLeaderboardProxyPanel(Component<?> parent, ComponentContext<?> context,
            SailingServiceAsync sailingService, String metaLeaderboardName,
            AsyncActionsExecutor asyncActionsExecutor,
            Timer timer, boolean isEmbedded, String preselectedLeaderboardName, RaceIdentifier preselectedRace, 
            ErrorReporter errorReporter, StringMessages stringMessages,
            boolean showRaceDetails, boolean autoExpandLastRaceColumn,
            LeaderboardSettings settings) {
        super(parent, context);

        loadedSettings = settings;

        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        this.sailingService = sailingService;
        this.metaLeaderboardName = metaLeaderboardName;
        this.asyncActionsExecutor = asyncActionsExecutor;
        this.showRaceDetails = showRaceDetails;
        this.timer = timer;
        this.isEmbedded = isEmbedded;
        this.selectedLeaderboardName = preselectedLeaderboardName;
        
        selectedLeaderboardFlowPanel = null;
        selectedLeaderboardPanel = null;
        leaderboardNamesAndDisplayNames = new ArrayList<Util.Pair<String, String>>();

        /**
         * This only stores the ContextSpecific settings, as they differ for all Leaderboards
         */
        leaderboardUpdateListeners = new HashSet<LeaderboardUpdateListener>();
        selectedLeaderboardChangeListeners = new HashSet<SelectedLeaderboardChangeListener>();

        contextStore = new HashMap<>();
    }

    @Override
    public Widget createWidget() {
        mainPanel = new VerticalPanel();
        
        if(!isEmbedded) {
            leaderboardsLabel = new Label(stringMessages.regattaLeaderboards());
            leaderboardsLabel.setVisible(false);
            leaderboardsLabel.getElement().getStyle().setFontWeight(FontWeight.BOLD);
            leaderboardsLabel.getElement().getStyle().setMargin(5, Unit.PX);
            mainPanel.add(leaderboardsLabel);
        }

        leaderboardsTabPanel = new TabPanel();
        leaderboardsTabPanel.setVisible(false);
        leaderboardsTabPanel.setAnimationEnabled(false);
        leaderboardsTabPanel.setWidth("100%");
        leaderboardsTabPanel.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                Integer tabIndex = event.getSelectedItem();
                if(tabIndex >= 0) {
                    updateSelectedLeaderboard(leaderboardNamesAndDisplayNames.get(tabIndex).getA(), tabIndex);
                }
            }
        });
        
        mainPanel.add(leaderboardsTabPanel);
        
        updateLeaderboardSelection();

        return mainPanel;
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.leaderboards();
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public LeaderboardSettings getSettings() {
        return selectedLeaderboardPanel.getSettings();
    }

    @Override
    public SettingsDialogComponent<LeaderboardSettings> getSettingsDialogComponent() {
        return selectedLeaderboardPanel.getSettingsDialogComponent();
    }

    @Override
    public void updateSettings(LeaderboardSettings newSettings) {
        // store contextspecific setting in this hashmap, so they are not lost on tab change
        contextStore.put(selectedLeaderboardPanel.getLeaderboardName(), newSettings);
        selectedLeaderboardPanel.updateSettings(newSettings);
    }

    public void addLeaderboardUpdateListener(LeaderboardUpdateListener listener) {
        this.leaderboardUpdateListeners.add(listener);
    }

    public void setLeaderboardNames(List<Util.Pair<String, String>> newLeaderboardNamesAndDisplayNames) {
        leaderboardNamesAndDisplayNames.clear();
        leaderboardNamesAndDisplayNames.addAll(newLeaderboardNamesAndDisplayNames);
        
        updateLeaderboardSelection();
    }

    private void readAndUpdateLeaderboardsOfMetaleaderboard() {
        sailingService.getLeaderboardsNamesOfMetaLeaderboard(metaLeaderboardName, new AsyncCallback<List<Util.Pair<String, String>>>() {
            
            @Override
            public void onSuccess(List<Util.Pair<String, String>> leaderboardNamesAndDisplayNames) {
                setLeaderboardNames(leaderboardNamesAndDisplayNames);
            }
            
            @Override
            public void onFailure(Throwable caught) {
                        GWT.log("Exception loading data", caught);
            }
        });
    }
    
    private void updateLeaderboardSelection() {
        if (leaderboardsTabPanel != null) {
            leaderboardsTabPanel.clear();
            int index = 0;
            int leaderboardCount = leaderboardNamesAndDisplayNames.size();
            for (Util.Pair<String, String> leaderboardNameAndDisplayName : leaderboardNamesAndDisplayNames) {
                FlowPanel tabFlowPanel = new FlowPanel();
                leaderboardsTabPanel.add(tabFlowPanel, leaderboardNameAndDisplayName.getB(), false);

                if (selectedLeaderboardName != null
                        && selectedLeaderboardName.equals(leaderboardNameAndDisplayName.getA())) {
                    leaderboardsTabPanel.selectTab(index);
                }
                index++;
            }
            // show the last leaderboard when no leaderboard is selected yet
            if (selectedLeaderboardName == null && leaderboardCount > 0) {
                leaderboardsTabPanel.selectTab(leaderboardCount - 1);
            }
            leaderboardsTabPanel.setVisible(leaderboardCount > 0);
            if (!isEmbedded) {
                leaderboardsLabel.setVisible(leaderboardCount > 0);
            }
        }
    }

    private void updateSelectedLeaderboard(String newSelectedLeaderboardName, int newTabIndex) {
        if (newSelectedLeaderboardName != null) {
            if (selectedLeaderboardPanel != null && selectedLeaderboardFlowPanel != null) {
                selectedLeaderboardPanel.removeAllListeners();
                selectedLeaderboardFlowPanel.remove(selectedLeaderboardPanel);
                selectedLeaderboardPanel = null;
                selectedLeaderboardFlowPanel = null;
            }
            
            selectedLeaderboardFlowPanel = (FlowPanel) leaderboardsTabPanel.getWidget(newTabIndex);

            LeaderboardSettings toMerge = contextStore.get(newSelectedLeaderboardName);
            if (toMerge != null) {
                toMerge = mergeContext(loadedSettings, toMerge);
            } else {
                toMerge = loadedSettings;
            }

            MultiLeaderboardPanel newSelectedLeaderboardPanel = new MultiLeaderboardPanel(this, getComponentContext(),
                    sailingService,
                    asyncActionsExecutor, toMerge, isEmbedded,
                    /* preselectedRace*/ null, new CompetitorSelectionModel(true), timer,
                    null, newSelectedLeaderboardName, errorReporter, stringMessages, 
                    showRaceDetails, /* competitorSearchTextBox */ null, /* showSelectionCheckbox */ true,  /* raceTimesInfoProvider */null, 
                    false, /* adjustTimerDelay */ true, /* autoApplyTopNFilter */ false,
                    /* showCompetitorFilterStatus */ false, /* enableSyncScroller */ false);
            selectedLeaderboardFlowPanel.add(newSelectedLeaderboardPanel);
            for (LeaderboardUpdateListener listener : leaderboardUpdateListeners) {
                newSelectedLeaderboardPanel.addLeaderboardUpdateListener(listener);
            }
            setSelectedLeaderboard(newSelectedLeaderboardPanel);
        } else {
            if (selectedLeaderboardPanel != null && selectedLeaderboardFlowPanel != null) {
                selectedLeaderboardPanel.removeAllListeners();
                selectedLeaderboardFlowPanel.remove(selectedLeaderboardPanel);
                selectedLeaderboardPanel = null;
                selectedLeaderboardFlowPanel = null;
            }
        }
        this.selectedLeaderboardName = newSelectedLeaderboardName;
    }

    private LeaderboardSettings mergeContext(LeaderboardSettings settings, LeaderboardSettings toMerge) {
        return LeaderboardSettingsFactory.getInstance().mergeLeaderboardSettings(toMerge, settings);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            readAndUpdateLeaderboardsOfMetaleaderboard();
        } else {
            updateSelectedLeaderboard(null, -1);
        }
    }

    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        if (selectedLeaderboardPanel != null) {
            selectedLeaderboardPanel.timeChanged(newTime, oldTime);
        }
    }

    @Override
    public void addSelectedLeaderboardChangeListener(SelectedLeaderboardChangeListener listener) {
        selectedLeaderboardChangeListeners.add(listener);
    }

    @Override
    public void removeSelectedLeaderboardChangeListener(SelectedLeaderboardChangeListener listener) {
        selectedLeaderboardChangeListeners.remove(listener);
    }

    @Override
    public void setSelectedLeaderboard(LeaderboardPanel selectedLeaderboard) {
        if (this.selectedLeaderboardPanel != selectedLeaderboard) {
            this.selectedLeaderboardPanel = selectedLeaderboard;
            for (SelectedLeaderboardChangeListener listener : selectedLeaderboardChangeListeners) {
                listener.onSelectedLeaderboardChanged(selectedLeaderboardPanel);
            }
        }
    }

    public void removeLeaderboardUpdateListener(LeaderboardUpdateListener leaderboardUpdateListener) {
        leaderboardUpdateListeners.remove(leaderboardUpdateListener);
    }

    @Override
    public String getId() {
        return MultiLeaderboardPanelLifecycle.MID;
    }
}
