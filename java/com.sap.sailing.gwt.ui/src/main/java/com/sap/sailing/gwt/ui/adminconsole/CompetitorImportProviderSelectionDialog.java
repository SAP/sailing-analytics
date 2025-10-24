package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.common.CompetitorDescriptor;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.CompetitorProviderDTO;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.async.ParallelExecutionCallback;
import com.sap.sse.gwt.client.async.ParallelExecutionHolder;
import com.sap.sse.gwt.client.controls.busyindicator.BusyDisplay;
import com.sap.sse.gwt.client.controls.busyindicator.BusyIndicator;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

/**
 * Defines the dialog for displaying competitor provider names and list box of pair "event, regatta" names where we have
 * competitors available for importing. Also defines which dialog would be created for matching imported competitors
 * using the factory {@link MatchImportedCompetitorsDialogFactory}
 * 
 * @author Alexander Tatarinovich
 *
 */
public class CompetitorImportProviderSelectionDialog extends DataEntryDialog<CompetitorImportSelectionDialogResult> {

    private final ListBox competitorListBox;
    private final ListBox competitorProviderListBox;
    private final BusyIndicator busyIndicator;

    private final SailingServiceAsync sailingService;
    private final StringMessages stringMessages;
    private final ErrorReporter errorReporter;

    /**
     * a unique and human-readable string key for a eventNameRegattaName pair
     */
    private final LinkedHashMap<String, Pair<String, String>> eventRegattaNamesByCompetitorListItem;

    public CompetitorImportProviderSelectionDialog(MatchImportedCompetitorsDialogFactory matchCompetitorsDialogFactory,
            BusyDisplay busyDisplay, Iterable<String> competitorProviderNames, SailingServiceAsync sailingService,
            StringMessages stringMessages, ErrorReporter errorReporter) {
        super(stringMessages.importCompetitors(), /* message */ null, stringMessages.ok(), stringMessages.cancel(), /* validator */ null,
                new Callback(matchCompetitorsDialogFactory, sailingService, busyDisplay, errorReporter,
                        stringMessages));
        this.sailingService = sailingService;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        competitorProviderListBox = createListBox(/* isMultipleSelect */ false);
        competitorListBox = createListBox(/* isMultipleSelect */ false);
        competitorListBox.setVisible(false);
        busyIndicator = new SimpleBusyIndicator();
        eventRegattaNamesByCompetitorListItem = new LinkedHashMap<>();
        competitorProviderListBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                int selectedIndex = competitorProviderListBox.getSelectedIndex();
                if (selectedIndex > 0) {
                    String selectedProviderName = competitorProviderListBox.getItemText(selectedIndex);
                    competitorImportProviderChanged(selectedProviderName);
                } else {
                    competitorImportProviderChanged(null);
                }
            }
        });
        addProviderNamesToListBox(competitorProviderNames, stringMessages);
    }

    private void addProviderNamesToListBox(Iterable<String> competitorProviderNames, StringMessages stringMessages) {
        List<String> sortedProviderNames = getSortedProviderNames(competitorProviderNames);
        competitorProviderListBox.addItem(stringMessages.selectCompetitorImportProvider());
        for (String providerName : sortedProviderNames) {
            competitorProviderListBox.addItem(providerName);
        }
    }

    private List<String> getSortedProviderNames(Iterable<String> competitorProviderNames) {
        List<String> sortedProviderNames = new ArrayList<>();
        for (String providerName : competitorProviderNames) {
            sortedProviderNames.add(providerName);
        }
        Collections.sort(sortedProviderNames);
        return sortedProviderNames;
    }

    private void competitorImportProviderChanged(String selectedProviderName) {
        if (selectedProviderName != null) {
            busyIndicator.setBusy(true);
            competitorProviderListBox.setEnabled(false);
            sailingService.getCompetitorProviderDTOByName(selectedProviderName,
                    new AsyncCallback<CompetitorProviderDTO>() {
                        @Override
                        public void onSuccess(CompetitorProviderDTO result) {
                            updateCompetitorListBox(result);
                            competitorListBox.setVisible(true);
                            busyIndicator.setBusy(false);
                            competitorProviderListBox.setEnabled(true);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            updateCompetitorListBox(null);
                            competitorListBox.setVisible(false);
                            busyIndicator.setBusy(false);
                            competitorProviderListBox.setEnabled(true);
                            errorReporter.reportError(
                                    stringMessages.errorLoadingCompetitorImportProviders(caught.getMessage()));
                        }
                    });
        } else {
            competitorListBox.setVisible(false);
            updateCompetitorListBox(null);
        }
    }

    private void updateCompetitorListBox(CompetitorProviderDTO competitorProvider) {
        eventRegattaNamesByCompetitorListItem.clear();
        competitorListBox.clear();
        if (competitorProvider != null) {
            competitorListBox.addItem(stringMessages.pleaseSelectAScoringResult());
            List<Pair<String, String>> eventAndRegattaNames = getEventAndRegattaNamesWhichHasCompetitors(competitorProvider);
            for (Pair<String, String> pair : eventAndRegattaNames) {
                String eventName = pair.getA();
                String ragattaName = pair.getB();
                String competitorImportSourceName = eventName + ", " + ragattaName;
                eventRegattaNamesByCompetitorListItem.put(competitorImportSourceName, pair);
                competitorListBox.addItem(competitorImportSourceName);
            }
        }
    }

    private List<Pair<String, String>> getEventAndRegattaNamesWhichHasCompetitors(CompetitorProviderDTO competitorProvider) {
        List<Pair<String, String>> eventAndRegattaNames = new ArrayList<>();
        for (Entry<String, Set<String>> entry : competitorProvider.getHasCompetitorsForRegattasInEvent().entrySet()) {
            if (entry.getValue() == null) {
                eventAndRegattaNames.add(new Pair<>(entry.getKey(), ""));
            } else {
                for (String ragattaName : entry.getValue()) {
                    eventAndRegattaNames.add(new Pair<>(entry.getKey(), ragattaName));
                }
            }
        }
        return eventAndRegattaNames;
    }

    private static class Callback implements DialogCallback<CompetitorImportSelectionDialogResult> {
        private final BusyDisplay busyDisplay;
        private final SailingServiceAsync sailingService;
        private final ErrorReporter errorReporter;
        private final StringMessages stringMessages;
        private final MatchImportedCompetitorsDialogFactory matchCompetitorsDialogFactory;

        public Callback(MatchImportedCompetitorsDialogFactory matchCompetitorsDialogFactory,
                SailingServiceAsync sailingService, BusyDisplay busyDisplay, ErrorReporter errorReporter,
                StringMessages stringMessages) {
            this.sailingService = sailingService;
            this.busyDisplay = busyDisplay;
            this.errorReporter = errorReporter;
            this.stringMessages = stringMessages;
            this.matchCompetitorsDialogFactory = matchCompetitorsDialogFactory;
        }

        @Override
        public void cancel() {
            // don't do anything; dialog was canceled
        }

        @Override
        public void ok(final CompetitorImportSelectionDialogResult competitorImportDialogResult) {
            if (competitorImportDialogResult != null) {
                final String competitorProviderName = competitorImportDialogResult.getProviderName();
                final String eventName = competitorImportDialogResult.getEventName();
                final String regattaName = competitorImportDialogResult.getRegattaName();
                busyDisplay.setBusy(true);
                @SuppressWarnings("unchecked")
                final Pair<List<CompetitorDescriptor>, String>[] competitorDescriptorsAndHint = (Pair<List<CompetitorDescriptor>, String>[]) new Pair<?, ?>[1];
                @SuppressWarnings("unchecked")
                final Iterable<CompetitorDTO>[] competitors = (Iterable<CompetitorDTO>[]) new Iterable<?>[1];
                final ParallelExecutionCallback<Pair<List<CompetitorDescriptor>, String>> getCompetitorDescriptorsCallback =
                        new ParallelExecutionCallback<Pair<List<CompetitorDescriptor>, String>>() {
                    @Override
                    public void onSuccess(Pair<List<CompetitorDescriptor>, String> myCompetitorDescriptors) {
                        competitorDescriptorsAndHint[0] = myCompetitorDescriptors;
                        super.onSuccess(myCompetitorDescriptors);
                    }
                };
                final ParallelExecutionCallback<Iterable<CompetitorDTO>> getCompetitorsCallback =
                        new ParallelExecutionCallback<Iterable<CompetitorDTO>>() {
                            @Override
                            public void onSuccess(Iterable<CompetitorDTO> myCompetitors) {
                                competitors[0] = myCompetitors;
                                super.onSuccess(myCompetitors);
                            }
                };
                // the following parallel execution holder fires its handleSuccess method after both callbacks have succeeded
                new ParallelExecutionHolder(getCompetitorDescriptorsCallback, getCompetitorsCallback) {
                    @Override
                    protected void handleSuccess() {
                        busyDisplay.setBusy(false);
                        matchCompetitorsDialogFactory.createMatchImportedCompetitorsDialog(competitorDescriptorsAndHint[0], competitors[0]).show();
                    }
                    
                    @Override
                    protected void handleFailure(Throwable t) {
                        busyDisplay.setBusy(false);
                        errorReporter.reportError(
                                stringMessages.errorLoadingCompetitorImportDescriptors(t.getMessage()));
                    }
                };
                // trigger both calls, allowing for parallel execution, synchronizing with the parallel execution holder above:
                sailingService.getCompetitorDescriptorsAndHint(competitorProviderName, eventName, regattaName,
                        LocaleInfo.getCurrentLocale().getLocaleName(), new MarkedAsyncCallback<>(getCompetitorDescriptorsCallback));
                sailingService.getCompetitors(false, false, new MarkedAsyncCallback<>(getCompetitorsCallback));
            }
        }
    }

    /**
     * Factory for creating specific dialog {@link MatchImportedCompetitorsDialog} where we will be match imported
     * competitors.
     * 
     * @param competitorDescriptors
     *            imported competitor descriptors {@link CompetitorDescriptor}
     * @param competitors
     *            existing competitors from {@link CompetitorAndBoatStore}
     * @author Alexander Tatarinovich
     *
     */
    public interface MatchImportedCompetitorsDialogFactory {
        MatchImportedCompetitorsDialog createMatchImportedCompetitorsDialog(
                Pair<List<CompetitorDescriptor>, String> competitorDescriptorsAndHint, Iterable<CompetitorDTO> competitors);
    }

    @Override
    protected Widget getAdditionalWidget() {
        VerticalPanel vPanel = new VerticalPanel();
        vPanel.add(busyIndicator);
        vPanel.add(competitorProviderListBox);
        vPanel.add(competitorListBox);
        return vPanel;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return competitorProviderListBox;
    }

    @Override
    protected CompetitorImportSelectionDialogResult getResult() {
        CompetitorImportSelectionDialogResult competitorImportDialogResult = null;
        int selectedProviderIndex = competitorProviderListBox.getSelectedIndex();
        if (selectedProviderIndex > 0) {
            String selectedProviderName = competitorProviderListBox.getItemText(selectedProviderIndex);
            int selectedDocumentIndex = competitorListBox.getSelectedIndex();
            if (selectedDocumentIndex > 0) {
                Pair<String, String> pair = eventRegattaNamesByCompetitorListItem
                        .get(competitorListBox.getValue(selectedDocumentIndex));
                String eventName = pair.getA();
                String regattaName = pair.getB();
                competitorImportDialogResult = new CompetitorImportSelectionDialogResult(selectedProviderName,
                        eventName, regattaName);
            }
        }
        return competitorImportDialogResult;
    }
}
