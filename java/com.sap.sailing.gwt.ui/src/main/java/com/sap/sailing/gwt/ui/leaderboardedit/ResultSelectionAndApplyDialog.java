package com.sap.sailing.gwt.ui.leaderboardedit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.FuzzyBoatClassNameMatcher;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RegattaScoreCorrectionDTO;
import com.sap.sailing.gwt.ui.shared.ScoreCorrectionProviderDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.controls.busyindicator.BusyIndicator;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.security.ui.client.UserService;

public class ResultSelectionAndApplyDialog extends DataEntryDialog<Util.Triple<String, String, Util.Pair<String, Date>>> {
    private static final String USER_PREFERENCE_LAST_SCORE_CORRECTION_PROVIDER_USED = "ResultSelectionAndApplyDialog.lastScoreCorrectionProviderUsed";
    /**
     * a unique and human-readable string key for a eventNameBoatClassNameAndLastModfied pair
     */
    private final LinkedHashMap<String, Util.Pair<String, Util.Pair<String, Date>>> scoreCorrections;
    private final ListBox scoreCorrectionListBox;
    private final ListBox scoreCorrectionProviderListBox;
    private final BusyIndicator busyIndicator;
    
    private final SailingServiceAsync sailingService;
    private final StringMessages stringMessages;
    private final ErrorReporter errorReporter;

    private final BoatClassDTO boatClass;
    private final String leaderboardName;
    
    public ResultSelectionAndApplyDialog(EditableLeaderboardPanel leaderboardPanel, Iterable<String> scoreCorrectionProviderNames, 
            SailingServiceWriteAsync sailingServiceWrite, StringMessages stringMessages, ErrorReporter errorReporter, UserService userService) {
        super(stringMessages.importOfficialResults(), null, stringMessages.ok(), stringMessages.cancel(), new Validator(stringMessages),
                new Callback(sailingServiceWrite, leaderboardPanel, errorReporter, stringMessages));
        this.sailingService = sailingServiceWrite;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        this.boatClass = leaderboardPanel.getLeaderboard().getBoatClass();
        this.leaderboardName = leaderboardPanel.getLeaderboard().getName();
        this.scoreCorrections = new LinkedHashMap<String, Util.Pair<String, Util.Pair<String, Date>>>();
        scoreCorrectionProviderListBox = createListBox(/* isMultipleSelect */ false);
        scoreCorrectionListBox = createListBox(/* isMultipleSelect */ false);
        scoreCorrectionListBox.setVisible(false);
        busyIndicator = new SimpleBusyIndicator();
        scoreCorrectionProviderListBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                onScoreCorrectionProviderListBoxSelectionChanged(userService);
            }
        });
        List<String> sortedProviderNames = new ArrayList<String>();
        for(String providerName: scoreCorrectionProviderNames) {
            sortedProviderNames.add(providerName);
        }
        final Map<String, Integer> indexOfProviderInList = new HashMap<>();
        Collections.sort(sortedProviderNames);
        scoreCorrectionProviderListBox.addItem(stringMessages.selectResultImportProvider());
        for(String providerName: sortedProviderNames) {
            indexOfProviderInList.put(providerName, scoreCorrectionProviderListBox.getItemCount());
            scoreCorrectionProviderListBox.addItem(providerName);
        }
        userService.getPreference(USER_PREFERENCE_LAST_SCORE_CORRECTION_PROVIDER_USED, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(stringMessages.errorFetchingUserPreference(USER_PREFERENCE_LAST_SCORE_CORRECTION_PROVIDER_USED, caught.getMessage()),
                        NotificationType.WARNING);
            }
            
            @Override
            public void onSuccess(String result) {
                if (result != null && indexOfProviderInList.containsKey(result)) {
                    scoreCorrectionProviderListBox.setSelectedIndex(indexOfProviderInList.get(result));
                    onScoreCorrectionProviderListBoxSelectionChanged(userService);
                }
            }
        });
    }

    private void scoreCorrectionProviderChanged(String selectedProviderName) {
        if (selectedProviderName != null) {
            busyIndicator.setBusy(true);
            scoreCorrectionProviderListBox.setEnabled(false);
            sailingService.getScoreCorrectionsOfProvider(selectedProviderName, new AsyncCallback<ScoreCorrectionProviderDTO>() {
                @Override
                public void onSuccess(ScoreCorrectionProviderDTO result) {
                    updateScoreCorrections(result);
                    scoreCorrectionListBox.setVisible(true);
                    busyIndicator.setBusy(false);
                    scoreCorrectionProviderListBox.setEnabled(true);
                }

                @Override
                public void onFailure(Throwable caught) {
                    updateScoreCorrections(null);
                    scoreCorrectionListBox.setVisible(false);
                    busyIndicator.setBusy(false);
                    scoreCorrectionProviderListBox.setEnabled(true);
                    errorReporter.reportError(stringMessages.errorLoadingScoreCorrectionProviders(caught.getMessage()));
                }
            });
        } else {
            scoreCorrectionListBox.setVisible(false);
            updateScoreCorrections(null);
        }
    }
    
    private void updateScoreCorrections(ScoreCorrectionProviderDTO scp) {
        scoreCorrections.clear();
        scoreCorrectionListBox.clear();
        if (scp != null) {
            scoreCorrectionListBox.addItem(stringMessages.pleaseSelectAScoringResult());
            List<Util.Pair<String, Util.Pair<String, Date>>> eventNameBoatClassNameAndLastModified = new ArrayList<Util.Pair<String, Util.Pair<String, Date>>>();
            for (Entry<String, Set<Util.Pair<String, Date>>> entry : scp.getHasResultsForBoatClassFromDateByEventName().entrySet()) {
                for (Util.Pair<String, Date> se : entry.getValue()) {
                    eventNameBoatClassNameAndLastModified.add(new Util.Pair<String, Util.Pair<String, Date>>(entry.getKey(), se));
                }
            }
            final Map<Util.Pair<String, Util.Pair<String, Date>>, Double> matchQualities = sortOfficialResultsByRelevance(eventNameBoatClassNameAndLastModified);
            final Map<Util.Pair<String, Util.Pair<String, Date>>, Integer> indexOfResultInScoreCorrectionListBox = new HashMap<>();
            for (Util.Pair<String, Util.Pair<String, Date>> pair : eventNameBoatClassNameAndLastModified) {
                String eventName = pair.getA();
                Util.Pair<String, Date> boatClassAndLastModified = pair.getB();
                String scoreCorrectionName = eventName + ", " + boatClassAndLastModified.getA() + ", " + boatClassAndLastModified.getB();
                indexOfResultInScoreCorrectionListBox.put(pair, scoreCorrectionListBox.getItemCount());
                scoreCorrections.put(scoreCorrectionName, pair);
                scoreCorrectionListBox.addItem(scoreCorrectionName);
            }
            if (!eventNameBoatClassNameAndLastModified.isEmpty()
                    && matchQualities.containsKey(eventNameBoatClassNameAndLastModified.get(0))
                    && matchQualities.get(eventNameBoatClassNameAndLastModified.get(0)) > 0.5) {
                // pre-select the result that seems most likely, if the likelihood exceeds 0.5:
                final Integer indexInList = indexOfResultInScoreCorrectionListBox.get(eventNameBoatClassNameAndLastModified.get(0));
                if (indexInList != null) {
                    scoreCorrectionListBox.setSelectedIndex(indexInList);
                    validateAndUpdate();
                    getOkButton().setFocus(true);
                }
            }
        }
    }

    private Map<Pair<String, Pair<String, Date>>, Double> sortOfficialResultsByRelevance(List<Util.Pair<String, Util.Pair<String, Date>>> eventNameBoatClassNameCapturedWhen) {
        return new FuzzyBoatClassNameMatcher().sortOfficialResultsByRelevance(boatClass, eventNameBoatClassNameCapturedWhen, leaderboardName);
    }

    private static class Validator implements DataEntryDialog.Validator<Util.Triple<String, String, Util.Pair<String, Date>>> {
        private final StringMessages stringMessages;

        public Validator(StringMessages stringMessages) {
            this.stringMessages = stringMessages;
        }

        @Override
        public String getErrorMessage(Util.Triple<String, String, Util.Pair<String, Date>> valueToValidate) {
            String errorMessage = null;
            if (valueToValidate == null) {
                errorMessage = stringMessages.pleaseSelectAScoringResult();
            }
            return errorMessage;
        }
    }
    
    private static class Callback implements DialogCallback<Util.Triple<String, String, Util.Pair<String, Date>>> {
        private final EditableLeaderboardPanel leaderboardPanel;
        private final SailingServiceWriteAsync sailingServiceWrite;
        private final StringMessages stringMessages;
        private final ErrorReporter errorReporter;
        
        public Callback(SailingServiceWriteAsync sailingServiceWrite, EditableLeaderboardPanel leaderboardPanel, ErrorReporter errorReporter,
                StringMessages stringMessages) {
            this.sailingServiceWrite = sailingServiceWrite;
            this.leaderboardPanel = leaderboardPanel;
            this.stringMessages = stringMessages;
            this.errorReporter = errorReporter;
        }

        @Override
        public void cancel() {
            // don't do anything; dialog was canceled
        }

        @Override
        public void ok(Util.Triple<String, String, Util.Pair<String, Date>> providerNameAndEventNameBoatClassNameCapturedWhen) {
            final String scoreCorrectionProviderName = providerNameAndEventNameBoatClassNameCapturedWhen.getA();
            final String eventName = providerNameAndEventNameBoatClassNameCapturedWhen.getB();
            final String boatClassName = providerNameAndEventNameBoatClassNameCapturedWhen.getC().getA();
            final Date timePointWhenResultPublished = providerNameAndEventNameBoatClassNameCapturedWhen.getC().getB();
            leaderboardPanel.addBusyTask();
            sailingServiceWrite.getScoreCorrections(scoreCorrectionProviderName, eventName, boatClassName, timePointWhenResultPublished,
                    new AsyncCallback<RegattaScoreCorrectionDTO>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            leaderboardPanel.removeBusyTask();
                            errorReporter.reportError(stringMessages.errorObtainingScoreCorrections(scoreCorrectionProviderName,
                                    eventName, boatClassName, timePointWhenResultPublished.toString(), caught.getMessage()));
                        }

                        @Override
                        public void onSuccess(RegattaScoreCorrectionDTO result) {
                            leaderboardPanel.removeBusyTask();
                            new MatchAndApplyScoreCorrectionsDialog(leaderboardPanel, stringMessages,
                                    sailingServiceWrite, errorReporter, result).show();
                        }
            });
        }
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        VerticalPanel vPanel = new VerticalPanel();
        vPanel.add(busyIndicator);
        vPanel.add(scoreCorrectionProviderListBox);
        vPanel.add(scoreCorrectionListBox);
        return vPanel;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return scoreCorrectionProviderListBox;
    }

    @Override
    protected Util.Triple<String, String, Util.Pair<String, Date>> getResult() {
         Util.Triple<String, String, Util.Pair<String, Date>> result = null; 
         int selectedProviderIndex = scoreCorrectionProviderListBox.getSelectedIndex();
         if (selectedProviderIndex > 0) {
             String selectedProviderName = scoreCorrectionProviderListBox.getItemText(selectedProviderIndex);
             int selectedScoreCorrectionIndex = scoreCorrectionListBox.getSelectedIndex();
             if (selectedScoreCorrectionIndex > 0) {
                 Util.Pair<String, Util.Pair<String, Date>> pair = scoreCorrections.get(scoreCorrectionListBox.getValue(selectedScoreCorrectionIndex));
                 result = new Util.Triple<String, String, Util.Pair<String, Date>>(selectedProviderName, pair.getA(), pair.getB());
             }
         }
         return result;
    }

    private void onScoreCorrectionProviderListBoxSelectionChanged(UserService userService) {
        int selectedIndex = scoreCorrectionProviderListBox.getSelectedIndex();
        if (selectedIndex > 0) {
            String selectedProviderName = scoreCorrectionProviderListBox.getItemText(selectedIndex);
            scoreCorrectionProviderChanged(selectedProviderName);
            userService.setPreference(USER_PREFERENCE_LAST_SCORE_CORRECTION_PROVIDER_USED, selectedProviderName, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    Notification.notify(stringMessages.errorSettingUserPreference(USER_PREFERENCE_LAST_SCORE_CORRECTION_PROVIDER_USED, caught.getMessage()),
                            NotificationType.WARNING);
                }

                @Override
                public void onSuccess(Void result) {
                }
            });
        } else {
            scoreCorrectionProviderChanged(null);
        }
    }
}
