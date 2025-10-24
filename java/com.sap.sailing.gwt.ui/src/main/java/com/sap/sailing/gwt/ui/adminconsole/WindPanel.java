package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.Handler;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.WindImportConstants;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.adminconsole.WindImportResult.RaceEntry;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.Displayer;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.CoursePositionsDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.fileupload.FileUploadUtil;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.BaseCelltable;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.UserStatusEventHandler;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;

/**
 * Displays a table of currently tracked races. The user can configure whether a race
 * is assumed to start with an upwind leg and exclude specific
 * wind sources from the overall (combined) wind computation, e.g., for performance reasons.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class WindPanel extends FormPanel implements FilterablePanelProvider<RaceDTO> {
    private static final String URL_SAILINGSERVER_EXPEDITION_IMPORT = "/../../sailingserver/expedition-import";
    private static final String URL_SAILINGSERVER_GRIB_IMPORT = "/../../sailingserver/grib-wind-import";
    private static final String URL_SAILINGSERVER_NMEA_IMPORT = "/../../sailingserver/nmea-wind-import";
    private static final String URL_SAILINGSERVER_ROUTECONVERTER_IMPORT = "/../../sailingserver/routeconverter-wind-import";
    private static final String URL_SAILINGSERVER_BRAVO_IMPORT = "/../../sailingserver/bravo-wind-import";

    private final SailingServiceWriteAsync sailingServiceWrite;
    private final UserService userService;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private final TextColumn<WindDTO> timeColumn;
    private final TextColumn<WindDTO> speedInKnotsColumn;
    private final TextColumn<WindDTO> windDirectionInDegColumn;
    private final TextColumn<WindDTO> positionColumn;
    private final TrackedRacesListComposite trackedRacesListComposite;
    private final RefreshableMultiSelectionModel<RaceDTO> refreshableRaceSelectionModel;
    private final WindSourcesToExcludeSelectorPanel windSourcesToExcludeSelectorPanel;
    private final CheckBox raceIsKnownToStartUpwindBox;
    private final CaptionPanel windCaptionPanel;
    private final Button addWindFixButton;
    private final VerticalPanel windFixesDisplayPanel;
    private final Label windSourceLabel;
    private final ListDataProvider<WindDTO> rawWindFixesDataProvider;
    private final CellTable<WindDTO> rawWindFixesTable;
    private final VerticalPanel windFixPanel;
    private final Predicate<RaceDTO> userPermission;
    private final Displayer<RegattaDTO> regattasDisplayer;
    
    /**
     * Composite pattern over the {@link RegattasDisplayer} interface. Calls to {@link #fillRegattas(Iterable)}
     * will be forwarded to those objects contained in this collection.
     */
    private final Set<Displayer<RegattaDTO>> containedRegattaDisplayers;
    private CaptionPanel expeditionImportPanel;
    private CaptionPanel gribImportPanel;
    private CaptionPanel nmeaImportPanel;
    private CaptionPanel bravoImportPanel;
    private CaptionPanel igtimiImportPanel;
    private CaptionPanel routeconverterImportPanel;
    private CaptionPanel expeditionAllInOneImporterPanel;
    
    public WindPanel(final Presenter presenter, final StringMessages stringMessages) {
        this.ensureDebugId("WindPanel");
        this.regattasDisplayer = result->fillRegattas(result);
        this.userPermission = race -> presenter.getUserService().hasPermission(race, UPDATE);
        this.sailingServiceWrite = presenter.getSailingService();
        this.userService = presenter.getUserService();
        this.containedRegattaDisplayers = new HashSet<>();
        this.errorReporter = presenter.getErrorReporter();
        this.stringMessages = stringMessages;
        this.windSourcesToExcludeSelectorPanel = new WindSourcesToExcludeSelectorPanel(sailingServiceWrite, stringMessages,
                errorReporter);
        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSize("100%", "100%");
        this.setWidget(mainPanel);
        trackedRacesListComposite = new TrackedRacesListComposite(null, null, presenter, stringMessages, /* multiselection */true,
                /* actionButtonsEnabled */ false);
        containedRegattaDisplayers.add(trackedRacesListComposite.getRegattasDisplayer());
        trackedRacesListComposite.ensureDebugId("TrackedRacesListComposite");
        mainPanel.add(trackedRacesListComposite);
        refreshableRaceSelectionModel = (RefreshableMultiSelectionModel<RaceDTO>) trackedRacesListComposite.getSelectionModel();
        refreshableRaceSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                updateWindDisplay();
                updateVisibilityStateForPanels();
            }
        });
        windCaptionPanel = new CaptionPanel(stringMessages.wind());
        windCaptionPanel.setVisible(false);
        mainPanel.add(windCaptionPanel);
        TabPanel tabPanel = new TabPanel();
        tabPanel.setAnimationEnabled(true);
        windCaptionPanel.add(tabPanel);
        tabPanel.setWidth("100%");
        windFixesDisplayPanel = new VerticalPanel();
        addWindFixButton = new Button(stringMessages.actionAddWindData() + "...");
        addWindFixButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                RegattaAndRaceIdentifier selectedRace = getSelectedRace();
                if (selectedRace != null) {
                    final RaceDTO race = trackedRacesListComposite.getRaceByIdentifier(selectedRace);
                    sailingServiceWrite.getCoursePositions(selectedRace, race.trackedRace.startOfTracking, new AsyncCallback<CoursePositionsDTO>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            showWindSettingDialog(race, null);
                        }

                        @Override
                        public void onSuccess(final CoursePositionsDTO result) {
                            showWindSettingDialog(race, result);
                        }
                    });
                }
            }
        });
        windFixesDisplayPanel.add(addWindFixButton);
        final VerticalPanel windSourcesPanel = new VerticalPanel();
        windSourcesPanel.setSpacing(10);
        tabPanel.add(windSourcesPanel, stringMessages.windSourcesUsed());
        tabPanel.add(windFixesDisplayPanel, stringMessages.windFixes());
        tabPanel.selectTab(0);
        raceIsKnownToStartUpwindBox = new CheckBox(stringMessages.raceIsKnownToStartUpwind());
        windSourcesPanel.add(raceIsKnownToStartUpwindBox);
        windSourcesPanel.add(windSourcesToExcludeSelectorPanel);
        raceIsKnownToStartUpwindBox.addValueChangeHandler(event -> setRaceIsKnownToStartUpwind());
        windFixPanel = new VerticalPanel();
        windSourcesPanel.add(windFixPanel);
        windSourceLabel = new Label();
        windFixesDisplayPanel.add(windSourceLabel);
        // table for the raw wind fixes
        final AccessControlledActionsColumn<WindDTO, DefaultActionsImagesBarCell> actionsColumn = create(
                new DefaultActionsImagesBarCell(stringMessages), userService,
                item -> trackedRacesListComposite.getRaceByIdentifier(getSelectedRace()));
        actionsColumn.addAction(DefaultActionsImagesBarCell.ACTION_DELETE, UPDATE, wind -> {
            List<RaceDTO> selectedRaces = new ArrayList<>(refreshableRaceSelectionModel.getSelectedSet());
            final RegattaAndRaceIdentifier raceIdentifier = selectedRaces.get(selectedRaces.size() - 1)
                    .getRaceIdentifier();
            sailingServiceWrite.removeWind(raceIdentifier, wind, new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // remove row from underlying list:
                    rawWindFixesDataProvider.getList().remove(wind);
                }

                @Override
                public void onFailure(Throwable caught) {
                    WindPanel.this.errorReporter.reportError(WindPanel.this.stringMessages.errorSettingWindForRace()
                            + " " + raceIdentifier + ": " + caught.getMessage());
                }
            });
        });
        timeColumn = new TextColumn<WindDTO>() {
            @Override
            public String getValue(WindDTO object) {
                return DateAndTimeFormatterUtil.formatDateAndTime(new Date(object.measureTimepoint));
            }
        };
        speedInKnotsColumn = new TextColumn<WindDTO>() {
            @Override
            public String getValue(WindDTO object) {
                return "" + object.trueWindSpeedInKnots;
            }
        };
        windDirectionInDegColumn = new TextColumn<WindDTO>() {
            @Override
            public String getValue(WindDTO object) {
                return "" + object.trueWindFromDeg;
            }
        };
        positionColumn = new TextColumn<WindDTO>() {
            @Override
            public String getValue(WindDTO object) {
                String result = "";
                if (object.position != null) {
                    result = "Lat: " + object.position.getLatDeg() + ", Lon: " + object.position.getLngDeg();
                }
                return result;
            }
        };
        timeColumn.setSortable(true);
        speedInKnotsColumn.setSortable(true);
        windDirectionInDegColumn.setSortable(true);
        AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
        rawWindFixesTable = new BaseCelltable<WindDTO>(/* pageSize */10000, tableRes);
        rawWindFixesTable.addColumn(timeColumn, stringMessages.time());
        rawWindFixesTable.addColumn(speedInKnotsColumn, stringMessages.speedInKnots());
        rawWindFixesTable.addColumn(windDirectionInDegColumn, stringMessages.fromDeg());
        rawWindFixesTable.addColumn(positionColumn, stringMessages.position());
        rawWindFixesTable.addColumn(actionsColumn, stringMessages.actions());
        rawWindFixesDataProvider = new ListDataProvider<WindDTO>();
        rawWindFixesDataProvider.addDataDisplay(rawWindFixesTable);
        Handler columnSortHandler = getWindTableColumnSortHandler(rawWindFixesDataProvider.getList(), timeColumn, speedInKnotsColumn, windDirectionInDegColumn);
        rawWindFixesTable.addColumnSortHandler(columnSortHandler);
        rawWindFixesTable.getColumnSortList().push(timeColumn);
        windFixesDisplayPanel.add(rawWindFixesTable);
        // Expedition import
        expeditionImportPanel = createExpeditionWindImportPanel();
        gribImportPanel = createGribWindImportPanel();
        nmeaImportPanel = createNmeaWindImportPanel();
        routeconverterImportPanel = createRouteconverterWindImportPanel();
        bravoImportPanel = createBravoWindImportPanel();
        igtimiImportPanel = igtimiImportPanel(mainPanel);
        mainPanel.add(expeditionImportPanel);
        mainPanel.add(gribImportPanel);
        mainPanel.add(nmeaImportPanel);
        mainPanel.add(routeconverterImportPanel);
        mainPanel.add(bravoImportPanel);
        mainPanel.add(igtimiImportPanel);
        // Expedition all in one import
        final Pair<CaptionPanel, ExpeditionAllInOneImportPanel> expeditionAllInOneRootAndImportPanel = createExpeditionAllInOneImportPanel(presenter);
        expeditionAllInOneImporterPanel = expeditionAllInOneRootAndImportPanel.getA();
        mainPanel.add(expeditionAllInOneImporterPanel);
        containedRegattaDisplayers.add(expeditionAllInOneRootAndImportPanel.getB().getRegattasDisplayer());
        updateVisibilityStateForPanels();
        this.userService.addUserStatusEventHandler(new UserStatusEventHandler() {
            @Override
            public void onUserStatusChange(UserDTO user, boolean preAuthenticated) {
                updateVisibilityStateForPanels();
            }
        });
    }

    private void updateVisibilityStateForPanels() {
        Set<RaceDTO> selectedRaces = refreshableRaceSelectionModel.getSelectedSet();
        if (selectedRaces.isEmpty()) {
            // they could potentially hit all races, we don't have enough information to check here
            expeditionImportPanel.setVisible(true);
            gribImportPanel.setVisible(true);
            nmeaImportPanel.setVisible(true);
            bravoImportPanel.setVisible(true);
            igtimiImportPanel.setVisible(true);
        } else {
            boolean canUpdateAll = true;
            for (RaceDTO race : refreshableRaceSelectionModel.getSelectedSet()) {
                if (!userPermission.test(race)) {
                    canUpdateAll = false;
                }
            }
            expeditionImportPanel.setVisible(canUpdateAll);
            gribImportPanel.setVisible(canUpdateAll);
            nmeaImportPanel.setVisible(canUpdateAll);
            bravoImportPanel.setVisible(canUpdateAll);
            igtimiImportPanel.setVisible(canUpdateAll);
        }

        boolean canCreateEvent = userService.hasCurrentUserPermissionToCreateObjectOfType(SecuredDomainType.EVENT);
        boolean canCreateRegatta = userService.hasCurrentUserPermissionToCreateObjectOfType(SecuredDomainType.REGATTA);
        boolean canCreateRace = userService
                .hasCurrentUserPermissionToCreateObjectOfType(SecuredDomainType.TRACKED_RACE);
        boolean canCreateLeaderboard = userService
                .hasCurrentUserPermissionToCreateObjectOfType(SecuredDomainType.LEADERBOARD);
        expeditionAllInOneImporterPanel
                .setVisible(canCreateEvent && canCreateLeaderboard && canCreateRace && canCreateRegatta);
    }

    private CaptionPanel igtimiImportPanel(VerticalPanel mainPanel) {
        return createIgtimiWindImportPanel(mainPanel);
    }

    private CaptionPanel createIgtimiWindImportPanel(VerticalPanel mainPanel) {
        CaptionPanel igtimiWindImportRootPanel = new CaptionPanel(stringMessages.igtimiWindImport());
        VerticalPanel contentPanel = new VerticalPanel();
        igtimiWindImportRootPanel.add(contentPanel);
        contentPanel.add(new Label(stringMessages.seeIgtimiTabForAccountSettings()));
        final CheckBox correctByDeclination = new CheckBox(stringMessages.declinationCheckbox());
        correctByDeclination.setValue(true); // by default this is desirable because the Igtimi connector reads uncorrected magnetic values
        final TextBox optionalBearerTokenBox = new TextBox();
        final Button importButton = new Button(stringMessages.importWindFromIgtimi());
        importButton.ensureDebugId("ImportWindFromIgtimi");
        final HTML resultReport = new HTML();
        resultReport.ensureDebugId("IgtimiImportResultReport");
        importButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final String warningMessage;
                if (refreshableRaceSelectionModel.getSelectedSet().isEmpty()) {
                    warningMessage = stringMessages.windImport_AllRacesWarning();
                } else {
                    warningMessage = stringMessages.windImport_SelectedRacesWarning(refreshableRaceSelectionModel.getSelectedSet().size());
                }
                if (Window.confirm(warningMessage)) {
                    resultReport.setText(stringMessages.loading());
                    sailingServiceWrite.importWindFromIgtimi(new ArrayList<>(refreshableRaceSelectionModel.getSelectedSet()),
                            correctByDeclination.getValue(),
                            Util.hasLength(optionalBearerTokenBox.getValue()) ? optionalBearerTokenBox.getValue().trim() : null,
                            new AsyncCallback<Map<RegattaAndRaceIdentifier, Integer>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(stringMessages.errorImportingIgtimiWind(caught.getMessage()));
                            resultReport.setText(stringMessages.errorImportingIgtimiWind(caught.getMessage()));
                        }

                        @Override
                        public void onSuccess(Map<RegattaAndRaceIdentifier, Integer> result) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("\n");
                            for (Entry<RegattaAndRaceIdentifier, Integer> e : result.entrySet()) {
                                sb.append(e.getKey().getRegattaName());
                                sb.append('/');
                                sb.append(e.getKey().getRaceName());
                                sb.append(": ");
                                sb.append(e.getValue());
                                sb.append("\n");
                            }
                            resultReport.setHTML(new SafeHtmlBuilder().appendEscapedLines(stringMessages.resultFromIgtimiWindImport(sb.toString())).toSafeHtml());
                        }
                    });
                }
            }
        });
        contentPanel.add(correctByDeclination);
        final HorizontalPanel tokenPanel = new HorizontalPanel();
        tokenPanel.setSpacing(5);
        tokenPanel.add(new Label(stringMessages.optionalBearerTokenForWindImport()));
        tokenPanel.add(optionalBearerTokenBox);
        contentPanel.add(tokenPanel);
        contentPanel.add(importButton);
        contentPanel.add(resultReport);
        return igtimiWindImportRootPanel;
    }
    
    private static class WindImportFileUploadForm {
        private final FormPanel formPanel;
        private final VerticalPanel formContentPanel;
        private final FileUpload fileUpload;
        private final Button submitButton;
        public WindImportFileUploadForm(FormPanel formPanel, VerticalPanel formContentPanel, FileUpload fileUpload, Button submitButton) {
            super();
            this.formPanel = formPanel;
            this.formContentPanel = formContentPanel;
            this.fileUpload = fileUpload;
            this.submitButton = submitButton;
        }
        public FormPanel getFormPanel() {
            return formPanel;
        }
        public FileUpload getFileUpload() {
            return fileUpload;
        }
        public Button getSubmitButton() {
            return submitButton;
        }
        public VerticalPanel getFormContentPanel() {
            return formContentPanel;
        }
    }
    /**
     * Creates a file upload form that works with {@code AbstractWindImportServlet} on the server side. In particular,
     * the race selection is managed in a hidden field named {@link #WIND_IMPORT_PARAMETER_RACES}, and the
     * {@code upload} element is the MIME multipart file upload form element. The form uses {@code POST} as its method.
     * 
     * The submit button is initially disabled. Callers can implement their own logic based, e.g., on event handlers they
     * add to the form panel or other elements that enable it.
     */
    private WindImportFileUploadForm createWindImportFileUploadForm(String relativeUploadUrl, boolean multi,
            Optional<String> submitButtonDebugId, Optional<String> uploadFormDebugId,
            Optional<String> importResultPanelDebugId) {
        /*
         * To style the "browse" button of the file upload widget
         * see http://www.shauninman.com/archive/2007/09/10/styling_file_inputs_with_css_and_the_dom  
         */
        final FormPanel form = new FormPanel();
        VerticalPanel formContentPanel = new VerticalPanel();
        form.add(formContentPanel);
        final Panel importResultPanel = new VerticalPanel();
        formContentPanel.add(importResultPanel);
        importResultPanelDebugId.map(id->{ importResultPanel.ensureDebugId(id); return null; });
        form.setMethod(FormPanel.METHOD_POST);
        form.setEncoding(FormPanel.ENCODING_MULTIPART);
        form.setAction(GWT.getHostPageBaseURL() + relativeUploadUrl);
        final Button submitButton = new Button(stringMessages.windImport_Upload(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                importResultPanel.clear();
                form.submit();
            }
        });
        submitButton.setEnabled(false);
        submitButtonDebugId.map(id->{ submitButton.ensureDebugId(id); return null; });
        final FileUpload fileUpload = new FileUpload();
        fileUpload.setName("upload");
        uploadFormDebugId.map(id->{ fileUpload.ensureDebugId(id); return null; });
        if (multi) {
            fileUpload.getElement().setAttribute("multiple", "multiple");
        }
        final Hidden hiddenRacesField = new Hidden(WindImportConstants.WIND_IMPORT_PARAMETER_RACES);
        formContentPanel.add(hiddenRacesField);
        formContentPanel.add(fileUpload);
        formContentPanel.add(submitButton);
        form.addSubmitHandler(new FormPanel.SubmitHandler() {
            public void onSubmit(SubmitEvent event) {
                if ((fileUpload.getFilename() != null) && (fileUpload.getFilename().trim().length() > 0)) {
                    Set<RaceDTO> selectedRaces = refreshableRaceSelectionModel.getSelectedSet();
                    String warningMessage;
                    if (!selectedRaces.isEmpty()) {
                        warningMessage = stringMessages.windImport_SelectedRacesWarning(selectedRaces.size());
                        JSONArray raceSelection = new JSONArray();
                        for (RaceDTO race : selectedRaces) {
                            RegattaAndRaceIdentifier raceIdentifier = race.getRaceIdentifier();
                            JSONObject raceEntry = new JSONObject();
                            raceEntry.put(WindImportConstants.WIND_IMPORT_PARAMETER_RACE_NAME, new JSONString(raceIdentifier.getRaceName()));
                            raceEntry.put(WindImportConstants.WIND_IMPORT_PARAMETER_REGATTA_NAME, new JSONString(raceIdentifier.getRegattaName()));
                            raceSelection.set(raceSelection.size(), raceEntry);
                        }
                        hiddenRacesField.setValue(raceSelection.toString());
                    } else {
                        warningMessage = stringMessages.windImport_AllRacesWarning();
                        hiddenRacesField.setValue(null);
                    }
                    if (!Window.confirm(warningMessage)) {
                        event.cancel();
                    }
                } else {
                    event.cancel();
                }
            }
        });
        form.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
            public void onSubmitComplete(SubmitCompleteEvent event) {
                importResultPanel.clear();
                String windImportResultJson = FileUploadUtil.getApplicationJsonContentFromHtml(event.getResults());
                try {
                    WindImportResult windImportResult = WindImportResult.fromJson(windImportResultJson);
                    JsArray<RaceEntry> raceEntries = windImportResult.getRaceEntries();
                    Label resultHeader = new Label(stringMessages.windImport_ResultHeader(String.valueOf(windImportResult.getFirst()), 
                            String.valueOf(windImportResult.getLast()), raceEntries.length()));
                    importResultPanel.add(resultHeader);
                    if (windImportResult.getError() != null) {
                        Label errorText = new Label(stringMessages.windImport_ResultError(windImportResult.getError()));
                        importResultPanel.add(errorText);
                    }
                    for (int i = 0; i < raceEntries.length(); i++) {
                        RaceEntry raceEntry = raceEntries.get(i);
                        Label entryText = new Label(stringMessages.windImport_ResultEntry(raceEntry.getRaceName(), raceEntry.getRegattaName(), 
                                raceEntry.getCount(), String.valueOf(raceEntry.getFirst()), String.valueOf(raceEntry.getLast())));
                        importResultPanel.add(entryText);
                    }
                } catch (Exception e) {
                    errorReporter.reportError(stringMessages.windImport_ResultError(e.getMessage()));
                }
            }
        });
        return new WindImportFileUploadForm(form, formContentPanel, fileUpload, submitButton);
    }

    private Pair<CaptionPanel, ExpeditionAllInOneImportPanel> createExpeditionAllInOneImportPanel(Presenter presenter) {
        final CaptionPanel rootPanel = new CaptionPanel(stringMessages.importFullExpeditionData());
        final ExpeditionAllInOneImportPanel expeditionAllInOneImportPanel = new ExpeditionAllInOneImportPanel(stringMessages, presenter);
        rootPanel.add(expeditionAllInOneImportPanel);
        return new Pair<>(rootPanel, expeditionAllInOneImportPanel);
    }

    private CaptionPanel createExpeditionWindImportPanel() {
        /*
         * To style the "browse" button of the file upload widget
         * see http://www.shauninman.com/archive/2007/09/10/styling_file_inputs_with_css_and_the_dom  
         */
        final WindImportFileUploadForm formAndFileUploadAndSubmitButton = createWindImportFileUploadForm(URL_SAILINGSERVER_EXPEDITION_IMPORT, /* multi */ true,
                Optional.empty(), Optional.empty(), Optional.empty());
        CaptionPanel windImportRootPanel = new CaptionPanel(stringMessages.windImport_Title());
        VerticalPanel windImportContentPanel = new VerticalPanel();
        windImportRootPanel.add(windImportContentPanel);
        final FormPanel form = formAndFileUploadAndSubmitButton.getFormPanel();
        windImportContentPanel.add(form);
        VerticalPanel formContentPanel = formAndFileUploadAndSubmitButton.getFormContentPanel();
        HorizontalPanel inputPanel = new HorizontalPanel();
        formContentPanel.add(inputPanel);
        final Panel importResultPanel = new VerticalPanel();
        windImportContentPanel.add(importResultPanel);

        final TextBox boatIdTextBox = new TextBox();
        boatIdTextBox.setName(WindImportConstants.EXPEDITON_IMPORT_PARAMETER_BOAT_ID);
        final Button submitButton = formAndFileUploadAndSubmitButton.getSubmitButton();

        final FileUpload fileUpload = formAndFileUploadAndSubmitButton.getFileUpload();
        fileUpload.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                importResultPanel.clear();
                String fileName = fileUpload.getFilename();
                boolean isValidFileName = (fileName != null) && (fileUpload.getFilename().trim().length() > 0);
                submitButton.setEnabled(isValidFileName);
                String boatId = "";
                if (isValidFileName) {
                    RegExp EXPEDITION_EXPORT_FILE_PATTERN = RegExp.compile("^.*_([0-9]+)\\.csv"); //matches typical expedition log file names like "2013Jun26_0.csv" where 0 as group[1] indicates the boat id. 
                    MatchResult match = EXPEDITION_EXPORT_FILE_PATTERN.exec(fileName);
                    if (match != null && match.getGroupCount() > 0) {
                        boatId = match.getGroup(1);
                    }
                }
                boatIdTextBox.setText(boatId);
            }
        });
        Label boatIdLabel = new Label(stringMessages.windImport_BoatId());
        inputPanel.add(boatIdLabel);
        inputPanel.add(boatIdTextBox);
        boatIdLabel.setWordWrap(false);
        inputPanel.setSpacing(5);
        inputPanel.setCellVerticalAlignment(fileUpload, HasVerticalAlignment.ALIGN_MIDDLE);
        inputPanel.setCellVerticalAlignment(boatIdLabel, HasVerticalAlignment.ALIGN_MIDDLE);
        inputPanel.setCellVerticalAlignment(boatIdTextBox, HasVerticalAlignment.ALIGN_MIDDLE);
        return windImportRootPanel;
    }

    private CaptionPanel createNmeaWindImportPanel() {
        final String importServletUrl = URL_SAILINGSERVER_NMEA_IMPORT;
        final String title = stringMessages.nmeaWindImport_Title();
        return createWindImportPanel(title, importServletUrl, Optional.empty(), Optional.empty(), Optional.empty());
    }

    private CaptionPanel createRouteconverterWindImportPanel() {
        final String importServletUrl = URL_SAILINGSERVER_ROUTECONVERTER_IMPORT;
        final String title = stringMessages.routeconverterWindImport_Title();
        return createWindImportPanel(title, importServletUrl, Optional.of("ImportWindFromRouteconverterSubmit"), Optional.of("ImportWindFromRouteconverterUpload"), Optional.of("ImportWindFromRouteconverterResults"));
    }
    
    private CaptionPanel createGribWindImportPanel() {
        return createWindImportPanel(stringMessages.gribWindImport_Title(), URL_SAILINGSERVER_GRIB_IMPORT, Optional.empty(), Optional.empty(), Optional.empty());
    }

    private CaptionPanel createBravoWindImportPanel() {
        return createWindImportPanel(stringMessages.bravoWindImport_Title(), URL_SAILINGSERVER_BRAVO_IMPORT, Optional.empty(), Optional.empty(), Optional.empty());
    }

    private CaptionPanel createWindImportPanel(final String title, final String importServletUrl,
            Optional<String> submitButtonDebugId, Optional<String> uploadFormDebugId,
            Optional<String> importResultPanelDebugId) {
        final WindImportFileUploadForm formAndFileUploadAndSubmitButton = createWindImportFileUploadForm(importServletUrl, /* multi */ true, submitButtonDebugId, uploadFormDebugId, importResultPanelDebugId);
        CaptionPanel windImportRootPanel = new CaptionPanel(title);
        VerticalPanel windImportContentPanel = new VerticalPanel();
        windImportRootPanel.add(windImportContentPanel);
        final FormPanel form = formAndFileUploadAndSubmitButton.getFormPanel();
        windImportContentPanel.add(form);
        VerticalPanel formContentPanel = formAndFileUploadAndSubmitButton.getFormContentPanel();
        HorizontalPanel inputPanel = new HorizontalPanel();
        formContentPanel.add(inputPanel);
        final Panel importResultPanel = new VerticalPanel();
        windImportContentPanel.add(importResultPanel);
        formAndFileUploadAndSubmitButton.getSubmitButton().setEnabled(true);
        final FileUpload fileUpload = formAndFileUploadAndSubmitButton.getFileUpload();
        inputPanel.setSpacing(5);
        inputPanel.setCellVerticalAlignment(fileUpload, HasVerticalAlignment.ALIGN_MIDDLE);
        return windImportRootPanel;
    }
    
    private void showWindSettingDialog(RaceDTO race, CoursePositionsDTO course) {
        AddWindFixDialog windSettingDialog = new AddWindFixDialog(race, course, stringMessages, new DialogCallback<WindDTO>() {
            @Override
            public void cancel() {
            }

            @Override
            public void ok(final WindDTO result) {
                addWindFix(result, race);
            }
        });
        windSettingDialog.show();
    }

    private void addWindFix(final WindDTO wind, final RaceDTO race) {
        final RegattaAndRaceIdentifier raceIdentifier = getSelectedRace();
        sailingServiceWrite.setWind(raceIdentifier, wind, new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                showWind(raceIdentifier, race);
            }

            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorAddingWindFixForRace(Util.toStringOrNull(raceIdentifier), caught.getMessage()));
            }
        });
    }
    
    public Displayer<RegattaDTO> getRegattasDisplayer() {
        return regattasDisplayer;
    }

    public void fillRegattas(Iterable<RegattaDTO> result) {
        for (final Displayer<RegattaDTO> containedRegattaDisplayer : containedRegattaDisplayers) {
            containedRegattaDisplayer.fill(result);
        }
    }

    public void showWind(final RegattaAndRaceIdentifier raceIdentifier, final RaceDTO race) {
        sailingServiceWrite.getWindSourcesInfo(raceIdentifier, new AsyncCallback<WindInfoForRaceDTO>() {
            @Override
            public void onSuccess(final WindInfoForRaceDTO result) {
                if (result != null) {
                    updateWindSourcesToExclude(result, raceIdentifier);
                    raceIsKnownToStartUpwindBox.setValue(result.raceIsKnownToStartUpwind);
                    final boolean userHasPermission = userPermission.test(race);
                    addWindFixButton.setVisible(userHasPermission);
                    raceIsKnownToStartUpwindBox.setEnabled(userHasPermission);
                    windSourcesToExcludeSelectorPanel.setEnabled(userHasPermission);
                    // load the raw wind fixes
                    sailingServiceWrite.getRawWindFixes(raceIdentifier, null, new AsyncCallback<WindInfoForRaceDTO>() {
                        @Override
                        public void onSuccess(WindInfoForRaceDTO result) {
                            if (result != null) {
                                udapteRawWindFixes(result);
                            } else {
                                // no wind fixes known for untracked race
                                clearWindSources();
                                clearWindFixes();
                            }
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(stringMessages.errorReadingWindFixes(caught.getMessage()));
                        }
                    });
                } else {
                    // no wind sources known for untracked race
                    clearWindSources();
                    clearWindFixes();
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Error reading wind source information: " + caught.getMessage());
            }
        });
    }

    private void updateWindSourcesToExclude(WindInfoForRaceDTO result, RegattaAndRaceIdentifier raceIdentifier) {
        windSourcesToExcludeSelectorPanel.update(raceIdentifier, result.windTrackInfoByWindSource.keySet(), result.windSourcesToExclude);
    }

    private void udapteRawWindFixes(WindInfoForRaceDTO result) {
        for (Map.Entry<WindSource, WindTrackInfoDTO> e : result.windTrackInfoByWindSource.entrySet()) {
            if (e.getKey().getType() == WindSourceType.WEB || e.getKey().getType() == WindSourceType.EXPEDITION) {
                windSourceLabel.setText(stringMessages.windSource() + ": " + e.getKey());
                if (e.getKey().getType() == WindSourceType.WEB) {
                    // only the WEB wind source is editable, hence has a "Remove" column
                    // rawWindFixesTable.removeColumn(col);
                }
                rawWindFixesDataProvider.getList().clear();
                rawWindFixesDataProvider.getList().addAll(e.getValue().windFixes);
            }
        }
    }

    private Handler getWindTableColumnSortHandler(List<WindDTO> list, TextColumn<WindDTO> timeColumn, TextColumn<WindDTO> speedInKnotsColumn, TextColumn<WindDTO> windDirectionInDegColumn) {
        ListHandler<WindDTO> result = new ListHandler<WindDTO>(list);
        result.setComparator(timeColumn, new Comparator<WindDTO>() {
            @Override
            public int compare(WindDTO o1, WindDTO o2) {
                return o1.measureTimepoint < o2.measureTimepoint ? -1 : o1.measureTimepoint == o2.measureTimepoint ? 0 : 1;
            }
        });
        result.setComparator(speedInKnotsColumn, new Comparator<WindDTO>() {
            @Override
            public int compare(WindDTO o1, WindDTO o2) {
                return o1.trueWindSpeedInKnots < o2.trueWindSpeedInKnots ? -1 : o1.trueWindSpeedInKnots == o2.trueWindSpeedInKnots ? 0 : 1;
            }
        });
        result.setComparator(windDirectionInDegColumn, new Comparator<WindDTO>() {
            @Override
            public int compare(WindDTO o1, WindDTO o2) {
                return o1.trueWindFromDeg < o2.trueWindFromDeg ? -1 : o1.trueWindFromDeg == o2.trueWindFromDeg ? 0 : 1;
            }
        });
        return result;
    }

    private void setRaceIsKnownToStartUpwind() {
        final RegattaAndRaceIdentifier selectedRace = getSelectedRace();
        sailingServiceWrite.setRaceIsKnownToStartUpwind(selectedRace, raceIsKnownToStartUpwindBox.getValue(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(WindPanel.this.stringMessages.errorWhileTryingToSetWindSourceForRace() + " " + selectedRace + ": " + caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
            }
        });
    }

    private RegattaAndRaceIdentifier getSelectedRace() {
        Set<RaceDTO> selectedRaces = refreshableRaceSelectionModel.getSelectedSet();
        if (selectedRaces.isEmpty() || selectedRaces.size() > 1) {
            return null;
        }

        return selectedRaces.iterator().next().getRaceIdentifier();
    }

    private void clearWindSources() {
        final Set<WindSource> emptySet = Collections.emptySet();
        windSourcesToExcludeSelectorPanel.update(null, emptySet, emptySet);
    }

    private void clearWindFixes() {
    }

    private void showWindFixesList(final RegattaAndRaceIdentifier raceIdentifier, final RaceDTO race) {
        List<String> windSourceTypeNames = new ArrayList<String>();
        windSourceTypeNames.add(WindSourceType.COMBINED.name());
        addWindFixButton.setVisible(userPermission.test(race));
        if (race.startOfRace != null) {
            sailingServiceWrite.getAveragedWindInfo(raceIdentifier, race.startOfRace, 30000L, 100, windSourceTypeNames,
                    /* onlyUpToNewestEvent==true means to only use data "based on facts" */ true, /* includeCombinedWindForAllLegMiddles */ false, new AsyncCallback<WindInfoForRaceDTO>() {
                @Override
                public void onFailure(Throwable caught) {
                }
    
                @Override
                public void onSuccess(WindInfoForRaceDTO result) {
                    windFixPanel.clear();
                    for (WindSourceType input : new WindSourceType[] { WindSourceType.COMBINED }) {
                        windFixPanel.add(new HTML("&nbsp;"));
                        windFixPanel.add(new Label(stringMessages.windFixListingDescription() + " " + input.name()));
                        WindTrackInfoDTO windTrackInfo = result.windTrackInfoByWindSource.get(new WindSourceImpl(input));
                        if (windTrackInfo != null && windTrackInfo.windFixes.size() >= 7) {
                            NumberFormat formatter = NumberFormat.getFormat(".##");
                            for (WindDTO windFix : windTrackInfo.windFixes.subList(0, 3)) {
                                windFixPanel.add(new Label("" + formatter.format(windFix.trueWindFromDeg) + " (deg) " + formatter.format(windFix.trueWindSpeedInKnots) + " (kt) " + formatter.format(windFix.position.getLatDeg()) + " (lat) " + formatter.format(windFix.position.getLngDeg()) + " (lng) " + new Date(windFix.measureTimepoint)));
                            }
                            // These fixes must not necessarily be the real last ones. This especially holds for long races.
                            for (WindDTO windFix : windTrackInfo.windFixes.subList(windTrackInfo.windFixes.size() - 4, windTrackInfo.windFixes.size() - 1)) {
                                windFixPanel.add(new Label("" + formatter.format(windFix.trueWindFromDeg) + " (deg) " + formatter.format(windFix.trueWindSpeedInKnots) + " (kt) " + formatter.format(windFix.position.getLatDeg()) + " (lat) " + formatter.format(windFix.position.getLngDeg()) + " (lng) " + new Date(windFix.measureTimepoint)));
                            }
                        } else {
                            windFixPanel.add(new Label(stringMessages.noWindFixesAvailable()));
                        }
                    }
                }
            });
        }
    }

    private void updateWindDisplay() {
        final RegattaAndRaceIdentifier selectedRace = getSelectedRace();
        final RaceDTO race = selectedRace != null ? trackedRacesListComposite.getRaceByIdentifier(selectedRace) : null;
        if (selectedRace != null && race != null && race.trackedRace != null) {
            windCaptionPanel.setVisible(true);
            windCaptionPanel.setCaptionText(stringMessages.wind() + ": " + race.getName());
            showWind(selectedRace, race);
            showWindFixesList(selectedRace, race);
        } else {
            windCaptionPanel.setVisible(false);
            windCaptionPanel.setCaptionText(stringMessages.wind());
            clearWindSources();
            clearWindFixes();
        }
    }

    @Override
    public AbstractFilterablePanel<RaceDTO> getFilterablePanel() {
        return trackedRacesListComposite.filterablePanelRaces;
    }
}
