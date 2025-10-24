package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sailing.domain.common.security.SecuredDomainType.LEADERBOARD;
import static com.sap.sailing.domain.common.security.SecuredDomainType.REGATTA;
import static com.sap.sailing.gwt.ui.adminconsole.LeaderboardRaceConfigImagesBarCell.ACTION_CERTIFICATE_ASSIGNMENT;
import static com.sap.sailing.gwt.ui.adminconsole.LeaderboardRaceConfigImagesBarCell.ACTION_EDIT;
import static com.sap.sailing.gwt.ui.adminconsole.LeaderboardRaceConfigImagesBarCell.ACTION_EDIT_COMPETITOR_TO_BOAT_MAPPINGS;
import static com.sap.sailing.gwt.ui.adminconsole.LeaderboardRaceConfigImagesBarCell.ACTION_REFRESH_RACELOG;
import static com.sap.sailing.gwt.ui.adminconsole.LeaderboardRaceConfigImagesBarCell.ACTION_REMOVE;
import static com.sap.sailing.gwt.ui.adminconsole.LeaderboardRaceConfigImagesBarCell.ACTION_SCRATCH_BOAT_SELECTION;
import static com.sap.sailing.gwt.ui.adminconsole.LeaderboardRaceConfigImagesBarCell.ACTION_SET_FINISHING_AND_FINISH_TIME;
import static com.sap.sailing.gwt.ui.adminconsole.LeaderboardRaceConfigImagesBarCell.ACTION_SET_IMPLIED_WIND;
import static com.sap.sailing.gwt.ui.adminconsole.LeaderboardRaceConfigImagesBarCell.ACTION_SET_STARTTIME;
import static com.sap.sailing.gwt.ui.adminconsole.LeaderboardRaceConfigImagesBarCell.ACTION_SHOW_RACELOG;
import static com.sap.sailing.gwt.ui.adminconsole.LeaderboardRaceConfigImagesBarCell.ACTION_UNLINK;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.READ;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.PairingListTemplateDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.common.client.help.HelpButton;
import com.sap.sailing.gwt.common.client.help.HelpButtonResources;
import com.sap.sailing.gwt.settings.client.EntryPointWithSettingsLinkFactory;
import com.sap.sailing.gwt.settings.client.leaderboard.AbstractLeaderboardPerspectiveLifecycle;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardContextDefinition;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardPerspectiveLifecycle;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardPerspectiveOwnSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardSettingsDialogComponent;
import com.sap.sailing.gwt.settings.client.leaderboard.MetaLeaderboardPerspectiveLifecycle;
import com.sap.sailing.gwt.ui.adminconsole.DisablableCheckboxCell.IsEnabled;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.EntryPointLinkFactory;
import com.sap.sailing.gwt.ui.client.Refresher;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardEntryPoint;
import com.sap.sailing.gwt.ui.leaderboard.ScoringSchemeTypeFormatter;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogSetFinishingAndFinishTimeDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogSetStartTimeAndProcedureDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.shared.components.LinkWithSettingsGenerator;
import com.sap.sse.gwt.client.shared.components.SettingsDialogForLinkSharing;
import com.sap.sse.gwt.client.shared.perspective.PerspectiveCompositeSettings;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog.DialogConfig;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

public class LeaderboardConfigPanel extends AbstractLeaderboardConfigPanel
        implements SelectedLeaderboardProvider<StrippedLeaderboardDTO>,
        TrackedRaceChangedListener {
    private static final Logger logger = Logger.getLogger(LeaderboardConfigPanel.class.getName());
    private final AnchorTemplates ANCHORTEMPLATE = GWT.create(AnchorTemplates.class);

    private final boolean showRaceDetails;

    private Button leaderboardRemoveButton;
    private Button addRaceColumnsButton;
    private Button columnMoveUpButton;
    private Button columnMoveDownButton;
    private final Refresher<CompetitorDTO> competitorsRefresher;
    private final Refresher<BoatDTO> boatsRefresher;
    
    public static class AnchorCell extends AbstractCell<SafeHtml> {
        @Override
        public void render(com.google.gwt.cell.client.Cell.Context context, SafeHtml safeHtml, SafeHtmlBuilder sb) {
            sb.append(safeHtml);
        }
    }

    interface AnchorTemplates extends SafeHtmlTemplates {
        @SafeHtmlTemplates.Template("<a target=\"_blank\" href=\"{0}\">{1}</a>")
        SafeHtml cell(SafeUri url, String displayName);
    }

    public LeaderboardConfigPanel(final Presenter presenter, StringMessages theStringConstants, final boolean showRaceDetails) {
        super(presenter, theStringConstants, /* multi-selection */ false);
        this.showRaceDetails = showRaceDetails;
        leaderboardTable.ensureDebugId("LeaderboardsCellTable");
        competitorsRefresher = presenter.getCompetitorsRefresher();
        boatsRefresher = presenter.getBoatsRefresher();
    }

    @Override
    protected void addLeaderboardControls(AccessControlledButtonPanel buttonPanel) {
        final Button createFlexibleLeaderboardBtn = buttonPanel
                .addCreateAction(stringMessages.createFlexibleLeaderboard() + " ...", this::createFlexibleLeaderboard);
        createFlexibleLeaderboardBtn.ensureDebugId("CreateFlexibleLeaderboardButton");
        final Supplier<Boolean> leaderboardCreateAndRegattaReadPermission = () -> userService
                .hasCurrentUserPermissionToCreateObjectOfType(LEADERBOARD)
                && userService.hasCurrentUserAnyPermission(REGATTA.getPermission(READ), null);
        final Button createRegattaLeaderboardBtn = buttonPanel.addAction(
                stringMessages.createRegattaLeaderboard() + " ...", leaderboardCreateAndRegattaReadPermission,
                this::createRegattaLeaderboard);
        createRegattaLeaderboardBtn.ensureDebugId("CreateRegattaLeaderboardButton");
        final Button createRegattaLeaderboardWithEliminationsBtn = buttonPanel.addAction(
                stringMessages.createRegattaLeaderboardWithEliminations() + " ...",
                leaderboardCreateAndRegattaReadPermission, this::createRegattaLeaderboardWithEliminations);
        createRegattaLeaderboardWithEliminationsBtn.ensureDebugId("CreateRegattaLeaderboardWithEliminationsButton");
        final Button createRegattaLeaderboardWithOtherTieBreakingLeaderboardBtn = buttonPanel.addAction(
                stringMessages.createRegattaLeaderboardWithOtherTieBreakingLeaderboard() + " ...",
                leaderboardCreateAndRegattaReadPermission, this::createRegattaLeaderboardWithOtherTieBreakingLeaderboard);
        createRegattaLeaderboardWithOtherTieBreakingLeaderboardBtn.ensureDebugId("CreateRegattaLeaderboardWithOtherTieBreakingLeaderboardButton");
        leaderboardRemoveButton = buttonPanel.addRemoveAction(stringMessages.remove(), leaderboardSelectionModel, true,
                () -> removeLeaderboards(leaderboardSelectionModel.getSelectedSet()));
        leaderboardRemoveButton.ensureDebugId("LeaderboardsRemoveButton");
        buttonPanel.addUnsecuredWidget(new HelpButton(HelpButtonResources.INSTANCE,
                stringMessages.videoGuide(), "https://sapsailing-documentation.s3-eu-west-1.amazonaws.com/adminconsole/Advanced+Topics/Leaderboard+Group+explained.mp4"));
    }

    @Override
    protected void addColumnsToLeaderboardTableAndSetSelectionModel(final UserService userService,
            final FlushableCellTable<StrippedLeaderboardDTO> leaderboardTable,
            AdminConsoleTableResources tableResources,
            ListDataProvider<StrippedLeaderboardDTO> listDataProvider) {
        ListHandler<StrippedLeaderboardDTO> leaderboardColumnListHandler = new ListHandler<StrippedLeaderboardDTO>(
                filteredLeaderboardList.getList());
        SelectionCheckboxColumn<StrippedLeaderboardDTO> selectionCheckboxColumn = createSortableSelectionCheckboxColumn(
                leaderboardTable, tableResources, leaderboardColumnListHandler, listDataProvider);
        AnchorCell anchorCell = new AnchorCell();
        Column<StrippedLeaderboardDTO, SafeHtml> linkColumn = new Column<StrippedLeaderboardDTO, SafeHtml>(
                anchorCell) {
            @Override
            public SafeHtml getValue(StrippedLeaderboardDTO object) {
                final String link = EntryPointWithSettingsLinkFactory.createLeaderboardLink(
                        new LeaderboardContextDefinition(object.getName(), object.displayName),
                        new LeaderboardPerspectiveOwnSettings(showRaceDetails));
                return ANCHORTEMPLATE.cell(UriUtils.fromString(link), object.getName());
            }

        };
        linkColumn.setSortable(true);
        leaderboardColumnListHandler.setComparator(linkColumn, new Comparator<StrippedLeaderboardDTO>() {
            @Override
            public int compare(StrippedLeaderboardDTO o1, StrippedLeaderboardDTO o2) {
                boolean ascending = isSortedAscending();
                if (o1.getName().equals(o2.getName())) {
                    return 0;
                }
                int val = -1;
                val = (o1 != null && o2 != null && ascending) ? (o1.getName().compareTo(o2.getName()))
                        : -(o2.getName().compareTo(o1.getName()));

                return val;
            }

            private boolean isSortedAscending() {
                ColumnSortList sortList = leaderboardTable.getColumnSortList();
                return sortList.size() > 0 & sortList.get(0).isAscending();
            }
        });
        TextColumn<StrippedLeaderboardDTO> leaderboardDisplayNameColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                return leaderboard.getDisplayName() != null ? leaderboard.getDisplayName() : "";
            }
        };
        leaderboardDisplayNameColumn.setSortable(true);
        leaderboardColumnListHandler.setComparator(leaderboardDisplayNameColumn,
                new Comparator<StrippedLeaderboardDTO>() {
                    @Override
                    public int compare(StrippedLeaderboardDTO o1, StrippedLeaderboardDTO o2) {
                        return new NaturalComparator().compare(o1.getDisplayName(), o2.getDisplayName());
                    }
                });

        TextColumn<StrippedLeaderboardDTO> leaderboardCanBoatsOfCompetitorsChangePerRaceColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                return leaderboard.canBoatsOfCompetitorsChangePerRace ? stringMessages.yes() : stringMessages.no();
            }
        };
        leaderboardCanBoatsOfCompetitorsChangePerRaceColumn.setSortable(true);
        leaderboardColumnListHandler.setComparator(leaderboardCanBoatsOfCompetitorsChangePerRaceColumn,
                (l1, l2) -> Boolean.valueOf(l1.canBoatsOfCompetitorsChangePerRace)
                        .compareTo(Boolean.valueOf(l2.canBoatsOfCompetitorsChangePerRace)));
        TextColumn<StrippedLeaderboardDTO> discardingOptionsColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                String result = "";
                if (leaderboard.discardThresholds != null) {
                    for (int discardThreshold : leaderboard.discardThresholds) {
                        result += discardThreshold + " ";
                    }
                }
                return result;
            }
        };
        discardingOptionsColumn.setSortable(true);
        leaderboardColumnListHandler.setComparator(discardingOptionsColumn, (o1, o2) -> {
            String s1 = "";
            String s2 = "";
            if (o1.discardThresholds != null) {
                for (int i1 : o1.discardThresholds) {
                    s1 += i1;
                }
            }
            if (o2.discardThresholds != null) {
                for (int i2 : o2.discardThresholds) {
                    s2 += i2;
                }
            }
            return new NaturalComparator().compare(s1, s2);
        });
        TextColumn<StrippedLeaderboardDTO> leaderboardTypeColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                return leaderboard.type.toString();
            }
        };
        leaderboardTypeColumn.setSortable(true);
        leaderboardColumnListHandler.setComparator(leaderboardTypeColumn, (o1, o2) -> o1.type.compareTo(o2.type));
        TextColumn<StrippedLeaderboardDTO> scoringSystemColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                return leaderboard.scoringScheme == null ? ""
                        : ScoringSchemeTypeFormatter.format(leaderboard.scoringScheme, stringMessages);
            }
        };
        scoringSystemColumn.setSortable(true);
        leaderboardColumnListHandler.setComparator(scoringSystemColumn, (o1, o2) -> {
            String s1 = o1.scoringScheme == null ? null : o1.scoringScheme.toString();
            String s2 = o2.scoringScheme == null ? null : o2.scoringScheme.toString();
            return new NaturalComparator().compare(s1, s2);
        });
        TextColumn<StrippedLeaderboardDTO> courseAreasColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                return Util.joinStrings(", ", Util.map(leaderboard.courseAreas, CourseAreaDTO::getName));
            }
        };
        courseAreasColumn.setSortable(true);
        leaderboardColumnListHandler.setComparator(courseAreasColumn,
                (o1, o2) -> new NaturalComparator().compare(Util.joinStrings(", ", Util.map(o1.courseAreas, CourseAreaDTO::getName)),
                        Util.joinStrings(", ", Util.map(o2.courseAreas, CourseAreaDTO::getName))));
        // Actions column:
        final HasPermissions type = SecuredDomainType.LEADERBOARD;
        final AccessControlledActionsColumn<StrippedLeaderboardDTO, LeaderboardConfigImagesBarCell> leaderboardActionColumn = create(
                new LeaderboardConfigImagesBarCell(stringMessages), userService);
        leaderboardActionColumn.addAction(LeaderboardConfigImagesBarCell.ACTION_UPDATE, UPDATE, this::editLeaderboard);
        leaderboardActionColumn.addAction(LeaderboardConfigImagesBarCell.ACTION_DELETE, DELETE, leaderboardDTO -> {
            if (Window.confirm(stringMessages.doYouReallyWantToRemoveLeaderboard(leaderboardDTO.getName()))) {
                removeLeaderboard(leaderboardDTO);
            }
        });
        leaderboardActionColumn.addAction(LeaderboardConfigImagesBarCell.ACTION_EDIT_SCORES, UPDATE, leaderboardDTO -> {
            String leaderboardEditingUrl = EntryPointWithSettingsLinkFactory
                    .createLeaderboardEditingLink(leaderboardDTO.getName());
            Window.open(leaderboardEditingUrl, "_blank", null);
        });
        leaderboardActionColumn.addAction(LeaderboardConfigImagesBarCell.ACTION_EDIT_COMPETITORS, UPDATE,
                leaderboardDTO -> {
                    EditCompetitorsDialog editCompetitorsDialog = new EditCompetitorsDialog(sailingServiceWrite, userService,
                            competitorsRefresher, boatsRefresher, leaderboardDTO.getName(),
                            leaderboardDTO.boatClassName,
                            /* createWithBoatByDefault */ !leaderboardDTO.canBoatsOfCompetitorsChangePerRace, stringMessages, errorReporter, new DialogCallback<List<CompetitorWithBoatDTO>>() {
                                @Override
                                public void cancel() {
                                }

                                @Override
                                public void ok(final List<CompetitorWithBoatDTO> result) {
                                }
                            });
                    editCompetitorsDialog.show();
                });
        leaderboardActionColumn.addAction(LeaderboardConfigImagesBarCell.ACTION_CONFIGURE_URL, READ,
                leaderboardDTO -> {
            sailingServiceWrite.getAvailableDetailTypesForLeaderboard(leaderboardDTO.getName(), null,
                    new AsyncCallback<Iterable<DetailType>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            logger.log(Level.WARNING, "Could not load detailtypes for leaderboard", caught);
                        }

                        @Override
                        public void onSuccess(Iterable<DetailType> result) {
                            openLeaderboardUrlConfigDialog(leaderboardDTO, result);
                        }
                    });
        });
        leaderboardActionColumn.addAction(LeaderboardConfigImagesBarCell.ACTION_OPEN_COACH_DASHBOARD, READ,
                leaderboardDTO -> {
                    Map<String, String> dashboardURLParameters = new HashMap<String, String>();
                    dashboardURLParameters.put("leaderboardName", leaderboardDTO.getName());
                    Window.open(EntryPointLinkFactory.createDashboardLink(dashboardURLParameters), "", null);
                });
        leaderboardActionColumn.addAction(LeaderboardConfigImagesBarCell.ACTION_SHOW_REGATTA_LOG, READ,
                leaderboardDTO -> showRegattaLog());
        leaderboardActionColumn.addAction(LeaderboardConfigImagesBarCell.ACTION_CREATE_PAIRINGLIST, UPDATE,
                this::createPairingListTemplate);
        leaderboardActionColumn.addAction(LeaderboardConfigImagesBarCell.ACTION_COPY_PAIRINGLIST_FROM_OTHER_LEADERBOARD, UPDATE,
                this::copyPairingListFromOtherLeaderboard);
        leaderboardActionColumn.addAction(LeaderboardConfigImagesBarCell.ACTION_PRINT_PAIRINGLIST, READ,
                this::openPairingListEntryPoint);
        final DialogConfig<StrippedLeaderboardDTO> config = EditOwnershipDialog.create(
                userService.getUserManagementWriteService(), type,
                leaderboardDTO -> reloadLeaderboardForTable(leaderboardDTO.getName()), stringMessages);
        leaderboardActionColumn.addAction(LeaderboardConfigImagesBarCell.ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP,
                new Consumer<StrippedLeaderboardDTO>() {
                    @Override
                    public void accept(StrippedLeaderboardDTO t) {
                        config.openOwnershipDialog(t);
                    }
        });
        final EditACLDialog.DialogConfig<StrippedLeaderboardDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type,
                leaderboardDTO -> reloadLeaderboardForTable(leaderboardDTO.getName()), stringMessages);
        leaderboardActionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                new Consumer<StrippedLeaderboardDTO>() {
                    @Override
                    public void accept(StrippedLeaderboardDTO t) {
                        configACL.openDialog(t);
                    }
        });
        leaderboardTable.addColumn(selectionCheckboxColumn, selectionCheckboxColumn.getHeader());
        leaderboardTable.addColumn(linkColumn, stringMessages.name());
        leaderboardTable.addColumn(leaderboardDisplayNameColumn, stringMessages.displayName());
        leaderboardTable.addColumn(leaderboardCanBoatsOfCompetitorsChangePerRaceColumn,
                stringMessages.canBoatsChange());
        leaderboardTable.addColumn(discardingOptionsColumn, stringMessages.discarding());
        leaderboardTable.addColumn(leaderboardTypeColumn, stringMessages.type());
        leaderboardTable.addColumn(scoringSystemColumn, stringMessages.scoringSystem());
        leaderboardTable.addColumn(courseAreasColumn, stringMessages.courseAreas());
        SecuredDTOOwnerColumn.configureOwnerColumns(leaderboardTable, leaderboardColumnListHandler, stringMessages);
        leaderboardTable.addColumn(leaderboardActionColumn, stringMessages.actions());
        leaderboardTable.addColumnSortHandler(leaderboardColumnListHandler);
        leaderboardTable.setSelectionModel(selectionCheckboxColumn.getSelectionModel(),
                selectionCheckboxColumn.getSelectionManager());
    }

    private void editLeaderboard(StrippedLeaderboardDTO leaderboardDTO) {
        final String oldLeaderboardName = leaderboardDTO.getName();
        final List<StrippedLeaderboardDTO> otherExistingLeaderboard = new ArrayList<>();
        otherExistingLeaderboard.addAll(availableLeaderboardList);
        otherExistingLeaderboard.remove(leaderboardDTO);
        if (leaderboardDTO.type.isMetaLeaderboard()) {
            Notification.notify(stringMessages.metaLeaderboardCannotBeChanged(), NotificationType.ERROR);
        } else {
            AbstractLeaderboardDialog<?> dialog;
            switch (leaderboardDTO.type) {
            case RegattaLeaderboard:
                dialog = new RegattaLeaderboardEditDialog<>(Collections.unmodifiableCollection(otherExistingLeaderboard),
                        Collections.unmodifiableCollection(allRegattas),
                        createLeaderboardDescriptor(leaderboardDTO,
                                /* scoring scheme is provided by regatta, not leaderboard */ null),
                        stringMessages, errorReporter, new DialogCallback<LeaderboardDescriptor>() {
                            @Override
                            public void cancel() {
                            }

                            @Override
                            public void ok(LeaderboardDescriptor result) {
                                updateLeaderboard(oldLeaderboardName, result);
                            }
                        });
                dialog.show();
                break;
            case RegattaLeaderboardWithOtherTieBreakingLeaderboard:
                dialog = new RegattaLeaderboardWithOtherTieBreakingLeaderboardEditDialog(Collections.unmodifiableCollection(otherExistingLeaderboard),
                        Collections.unmodifiableCollection(allRegattas),
                        new LeaderboardDescriptorWithOtherTieBreakingLeaderboard(
                                leaderboardDTO.getName(), leaderboardDTO.displayName, /* scoringScheme is provided by regatta, not leaderboard */ null,
                                leaderboardDTO.discardThresholds, leaderboardDTO.regattaName, Util.mapToArrayList(leaderboardDTO.courseAreas, CourseAreaDTO::getId),
                                leaderboardDTO.getOtherTieBreakingLeaderboardName()),
                        stringMessages, errorReporter, new DialogCallback<LeaderboardDescriptorWithOtherTieBreakingLeaderboard>() {
                            @Override
                            public void cancel() {
                            }

                            @Override
                            public void ok(LeaderboardDescriptorWithOtherTieBreakingLeaderboard result) {
                                updateLeaderboard(oldLeaderboardName, result);
                            }
                        });
                dialog.show();
                break;
            case RegattaLeaderboardWithEliminations:
                dialog = new RegattaLeaderboardWithEliminationsEditDialog(sailingServiceWrite, userService,
                        Collections.unmodifiableCollection(otherExistingLeaderboard),
                        Collections.unmodifiableCollection(allRegattas),
                        new LeaderboardDescriptorWithEliminations(
                                createLeaderboardDescriptor(leaderboardDTO,
                                        /* scoring scheme is provided by regatta, not leaderboard */ null),
                                /* eliminated competitors */ null),
                        stringMessages, errorReporter, new DialogCallback<LeaderboardDescriptorWithEliminations>() {
                            @Override
                            public void cancel() {
                            }

                            @Override
                            public void ok(LeaderboardDescriptorWithEliminations result) {
                                updateLeaderboard(oldLeaderboardName, result);
                                sailingServiceWrite.setEliminatedCompetitors(oldLeaderboardName,
                                        result.getEliminatedCompetitors(), new AsyncCallback<Void>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                errorReporter.reportError(
                                                        "Error trying to update eliminated competitors for leaderboard "
                                                                + oldLeaderboardName + ": " + caught.getMessage());
                                            }

                                            @Override
                                            public void onSuccess(Void v) {
                                                // nothing to do for now; maybe if the table once will show the number
                                                // of eliminated competitors or similar, update it
                                            }
                                        });
                            }
                        });
                dialog.show();
                break;
            case FlexibleLeaderboard:
                openUpdateFlexibleLeaderboardDialog(leaderboardDTO, otherExistingLeaderboard, leaderboardDTO.getName(),
                        createLeaderboardDescriptor(leaderboardDTO, leaderboardDTO.scoringScheme));
                break;
            default:
                Notification.notify(stringMessages.unknownLeaderboardType(leaderboardDTO.type.name()),
                        NotificationType.ERROR);
            }
        }
    }

    private LeaderboardDescriptor createLeaderboardDescriptor(StrippedLeaderboardDTO leaderboardDTO,
            ScoringSchemeType scoringScheme) {
        return new LeaderboardDescriptor(leaderboardDTO.getName(), leaderboardDTO.displayName, scoringScheme,
                leaderboardDTO.discardThresholds, leaderboardDTO.regattaName, Util.mapToArrayList(leaderboardDTO.courseAreas, CourseAreaDTO::getId));
    }

    @Override
    protected void addColumnsToRacesTable(CellTable<RaceColumnDTOAndFleetDTOWithNameBasedEquality> racesTable) {
        TextColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality> explicitFactorColumn = new TextColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality>() {
            @Override
            public String getValue(RaceColumnDTOAndFleetDTOWithNameBasedEquality object) {
                return object.getA().getExplicitFactor() == null ? "" : object.getA().getExplicitFactor().toString();
            }
        };
        Column<RaceColumnDTOAndFleetDTOWithNameBasedEquality, Boolean> isMedalRaceCheckboxColumn = new Column<RaceColumnDTOAndFleetDTOWithNameBasedEquality, Boolean>(
                new DisablableCheckboxCell(new IsEnabled() {
                    @Override
                    public boolean isEnabled() {
                        return getSelectedLeaderboard() != null
                                && !getSelectedLeaderboard().type.isRegattaLeaderboard();
                    }
                })) {
            @Override
            public Boolean getValue(RaceColumnDTOAndFleetDTOWithNameBasedEquality race) {
                return race.getA().isMedalRace();
            }
        };
        isMedalRaceCheckboxColumn
                .setFieldUpdater(new FieldUpdater<RaceColumnDTOAndFleetDTOWithNameBasedEquality, Boolean>() {
                    @Override
                    public void update(int index, RaceColumnDTOAndFleetDTOWithNameBasedEquality object, Boolean value) {
                        setIsMedalRace(getSelectedLeaderboard().getName(), object.getA(), value);
                    }
                });
        isMedalRaceCheckboxColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        TextColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality> isLinkedRaceColumn = new TextColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality>() {
            @Override
            public String getValue(RaceColumnDTOAndFleetDTOWithNameBasedEquality raceColumnAndFleetName) {
                boolean isTrackedRace = raceColumnAndFleetName.getA().isTrackedRace(raceColumnAndFleetName.getB());
                return isTrackedRace ? stringMessages.yes() : stringMessages.no();
            }
        };
        final AccessControlledActionsColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality, LeaderboardRaceConfigImagesBarCell> actionsColumn = create(
                new LeaderboardRaceConfigImagesBarCell(this, stringMessages), userService,
                item -> getSelectedLeaderboard());
        actionsColumn.addAction(ACTION_EDIT, UPDATE, this::editRaceColumnOfLeaderboard);
        actionsColumn.addAction(ACTION_UNLINK, UPDATE,
                object -> unlinkRaceColumnFromTrackedRace(object.getA().getRaceColumnName(), object.getB()));
        actionsColumn.addAction(ACTION_REMOVE, UPDATE, object -> {
            if (Window.confirm(stringMessages.reallyRemoveRace(object.getA().getRaceColumnName()))) {
                removeRaceColumn(object.getA());
            }
        });
        actionsColumn.addAction(ACTION_REFRESH_RACELOG, UPDATE,
                object -> refreshRaceLog(object.getA(), object.getB(), true));
        actionsColumn.addAction(ACTION_SET_STARTTIME, UPDATE, object -> setStartTime(object.getA(), object.getB()));
        actionsColumn.addAction(ACTION_SET_FINISHING_AND_FINISH_TIME, UPDATE,
                object -> setEndTime(object.getA(), object.getB()));
        actionsColumn.addAction(ACTION_SHOW_RACELOG, READ, object -> showRaceLog(object.getA(), object.getB()));
        actionsColumn.addAction(ACTION_EDIT_COMPETITOR_TO_BOAT_MAPPINGS, UPDATE,
                object -> editCompetitorToBoatMappings(object.getA(), object.getB()));
        actionsColumn.addAction(ACTION_CERTIFICATE_ASSIGNMENT, READ, object -> assignCertificates(object));
        actionsColumn.addAction(ACTION_SCRATCH_BOAT_SELECTION, READ, object -> selectScratchBoat(object));
        actionsColumn.addAction(ACTION_SET_IMPLIED_WIND, READ, object -> setImpliedWind(object));
        racesTable.addColumn(isMedalRaceCheckboxColumn, stringMessages.medalRace());
        racesTable.addColumn(isLinkedRaceColumn, stringMessages.islinked());
        racesTable.addColumn(explicitFactorColumn, stringMessages.factor());
        racesTable.addColumn(actionsColumn, stringMessages.actions());
        racesTable.ensureDebugId("RacesCellTable");
    }

    @Override
    protected void addSelectedLeaderboardRacesControls(Panel racesPanel) {
        addRaceColumnsButton = new Button(stringMessages.actionAddRaces() + "...");
        addRaceColumnsButton.ensureDebugId("AddRacesButton");
        addRaceColumnsButton.addClickHandler(event -> {
            if (getSelectedLeaderboard().type.isRegattaLeaderboard()) {
                Notification.notify(stringMessages.cannotAddRacesToRegattaLeaderboardButOnlyToRegatta(),
                        NotificationType.ERROR);
            } else {
                addRaceColumnsToLeaderboard();
            }
        });
        racesPanel.add(addRaceColumnsButton);
        columnMoveUpButton = new Button(stringMessages.columnMoveUp());
        columnMoveUpButton.ensureDebugId("MoveRaceUpButton");
        columnMoveUpButton.addClickHandler(event -> moveSelectedRaceColumnUp());
        racesPanel.add(columnMoveUpButton);

        columnMoveDownButton = new Button(stringMessages.columnMoveDown());
        columnMoveDownButton.ensureDebugId("MoveRaceDownButton");
        columnMoveDownButton.addClickHandler(event -> moveSelectedRaceColumnDown());
        racesPanel.add(columnMoveDownButton);
    }

    protected void openUpdateFlexibleLeaderboardDialog(final StrippedLeaderboardDTO leaderboardDTO,
            final List<StrippedLeaderboardDTO> otherExistingLeaderboard, final String oldLeaderboardName,
            final LeaderboardDescriptor descriptor) {
        sailingServiceWrite.getEvents(new MarkedAsyncCallback<List<EventDTO>>(new AsyncCallback<List<EventDTO>>() {
            @Override
            public void onSuccess(List<EventDTO> result) {
                openUpdateFlexibleLeaderboardDialog(leaderboardDTO, otherExistingLeaderboard, oldLeaderboardName,
                        descriptor, result);
            }

            @Override
            public void onFailure(Throwable caught) {
                openUpdateFlexibleLeaderboardDialog(leaderboardDTO, otherExistingLeaderboard, oldLeaderboardName,
                        descriptor, new ArrayList<EventDTO>());
            }
        }));
    }

    protected void openUpdateFlexibleLeaderboardDialog(StrippedLeaderboardDTO leaderboardDTO,
            List<StrippedLeaderboardDTO> otherExistingLeaderboard, final String oldLeaderboardName,
            LeaderboardDescriptor descriptor, List<EventDTO> existingEvents) {
        FlexibleLeaderboardEditDialog dialog = new FlexibleLeaderboardEditDialog(
                Collections.unmodifiableCollection(otherExistingLeaderboard), descriptor, userService,
                stringMessages, Collections.unmodifiableList(existingEvents),
                errorReporter, new DialogCallback<LeaderboardDescriptor>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(LeaderboardDescriptor result) {
                        updateLeaderboard(oldLeaderboardName, result);
                    }
                });
        dialog.show();
    }

    /**
     * Assembles a dialog that other parts of the application can use to let the user parameterize a leaderboard and
     * obtain the according URL for it. This keeps the "secrets" of which URL parameters have which meaning encapsulated
     * within this class.
     * <p>
     * 
     * The implementation by and large uses the {@link LeaderboardSettingsDialogComponent}'s widget and adds to it a
     * checkbox for driving the {@link #LeaderboardUrlSettings.PARAM_EMBEDDED} field.
     * 
     * @param leaderboard
     * @param availableDetailType
     * 
     * @see LeaderboardEntryPoint#getUrl(String, LeaderboardSettings, boolean)
     */
    private void openLeaderboardUrlConfigDialog(AbstractLeaderboardDTO leaderboard,
            Iterable<DetailType> availableDetailType) {
        PaywallResolver paywallResolver = new PaywallResolverImpl(userService, subscriptionServiceFactory);

        final AbstractLeaderboardPerspectiveLifecycle lifeCycle;
        if (leaderboard.type.isMetaLeaderboard()) {
            lifeCycle = new MetaLeaderboardPerspectiveLifecycle(stringMessages, leaderboard, availableDetailType, paywallResolver);
        } else {
            lifeCycle = new LeaderboardPerspectiveLifecycle(stringMessages, leaderboard, availableDetailType, paywallResolver);
        }
        final LeaderboardContextDefinition leaderboardContextSettings = new LeaderboardContextDefinition(
                leaderboard.getName(), leaderboard.getDisplayName());
        final LinkWithSettingsGenerator<PerspectiveCompositeSettings<LeaderboardPerspectiveOwnSettings>> linkWithSettingsGenerator = new LinkWithSettingsGenerator<>(
                EntryPointLinkFactory.LEADERBOARD_PATH, lifeCycle::createDefaultSettings, leaderboardContextSettings);
        SettingsDialogForLinkSharing<PerspectiveCompositeSettings<LeaderboardPerspectiveOwnSettings>> dialog = new SettingsDialogForLinkSharing<>(
                linkWithSettingsGenerator, lifeCycle, stringMessages);
        dialog.ensureDebugId("LeaderboardPageUrlConfigurationDialog");
        dialog.show();
        
    }

    private void editCompetitorToBoatMappings(final RaceColumnDTO raceColumnDTO, final FleetDTO fleetDTO) {
        final String selectedLeaderboardName = getSelectedLeaderboardName();
        final String raceColumnName = raceColumnDTO.getName();
        final String fleetName = fleetDTO.getName();
        final String raceName = LeaderboardNameConstants.DEFAULT_FLEET_NAME.equals(fleetName) ? raceColumnName
                : raceColumnName + ", " + fleetName;
        ShowCompetitorToBoatMappingsDialog dialog = new ShowCompetitorToBoatMappingsDialog(sailingServiceWrite,
                stringMessages, errorReporter, selectedLeaderboardName, raceColumnName, fleetName, raceName,
                userService);
        dialog.center();
    }

    private void setStartTime(RaceColumnDTO raceColumnDTO, FleetDTO fleetDTO) {
        new SetStartTimeDialog(sailingServiceWrite, errorReporter, getSelectedLeaderboardName(), raceColumnDTO.getName(),
                fleetDTO.getName(), stringMessages, new DialogCallback<RaceLogSetStartTimeAndProcedureDTO>() {
                    @Override
                    public void ok(RaceLogSetStartTimeAndProcedureDTO editedObject) {
                        sailingServiceWrite.setStartTimeAndProcedure(editedObject, new AsyncCallback<Boolean>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError(caught.getMessage());
                            }

                            @Override
                            public void onSuccess(Boolean result) {
                                if (!result) {
                                    Notification.notify(stringMessages.failedToSetNewStartTime(),
                                            NotificationType.ERROR);
                                }
                            }
                        });

                    }

                    @Override
                    public void cancel() {
                    }
                }).show();
    }

    private void setEndTime(RaceColumnDTO raceColumnDTO, FleetDTO fleetDTO) {
        new SetFinishingAndFinishedTimeDialog(sailingServiceWrite, errorReporter, getSelectedLeaderboardName(),
                raceColumnDTO.getName(), fleetDTO.getName(), stringMessages,
                new DialogCallback<RaceLogSetFinishingAndFinishTimeDTO>() {
                    @Override
                    public void ok(RaceLogSetFinishingAndFinishTimeDTO editedObject) {
                        sailingServiceWrite.setFinishingAndEndTime(editedObject,
                                new AsyncCallback<Pair<Boolean, Boolean>>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter.reportError(caught.getMessage());
                                    }

                                    @Override
                                    public void onSuccess(Pair<Boolean, Boolean> result) {
                                        if (!result.getA() || !result.getB()) {
                                            Notification.notify(stringMessages.failedToSetNewFinishingAndFinishTime(),
                                                    NotificationType.ERROR);
                                        }
                                    }
                                });
                    }

                    @Override
                    public void cancel() {
                    }
                }).show();
    }

    private void removeRaceColumn(final RaceColumnDTO raceColumnDTO) {
        final String selectedLeaderboardName = getSelectedLeaderboardName();
        final String raceColumnString = raceColumnDTO.getRaceColumnName();
        sailingServiceWrite.removeLeaderboardColumn(getSelectedLeaderboardName(), raceColumnString,
                new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError("Error trying to remove leaderboard race column " + raceColumnDTO
                                + " in leaderboard " + getSelectedLeaderboardName() + ": " + t.getMessage());
                    }

                    @Override
                    public void onSuccess(Void arg0) {
                        loadAndRefreshLeaderboard(selectedLeaderboardName);
                    }
                }));
    }

    /**
     * The selected row is potentially only one of several fleet-based rows of the same RaceColumn. In this case, move
     * all fleet-based rows of the same RaceColumn down.
     */
    private void moveSelectedRaceColumnDown() {
        final String selectedLeaderboardName = getSelectedLeaderboardName();
        final String selectedRaceColumnName = getSelectedRaceColumnWithFleet().getA().getRaceColumnName();
        sailingServiceWrite.moveLeaderboardColumnDown(getSelectedLeaderboardName(), selectedRaceColumnName,
                new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to move leaderboard race column "
                                + selectedRaceColumnName + " in leaderboard " + getSelectedLeaderboardName() + " down: "
                                + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        loadAndRefreshLeaderboard(selectedLeaderboardName);
                    }
                }));
    }

    /**
     * The selected row is potentially only one of several fleet-based rows of the same RaceColumn. In this case, move
     * all fleet-based rows of the same RaceColumn down.
     */
    private void moveSelectedRaceColumnUp() {
        final String selectedLeaderboardName = getSelectedLeaderboardName();
        final String selectedRaceColumnName = getSelectedRaceColumnWithFleet().getA().getRaceColumnName();
        sailingServiceWrite.moveLeaderboardColumnUp(getSelectedLeaderboardName(), selectedRaceColumnName,
                new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to move leaderboard race column "
                                + selectedRaceColumnName + " in leaderboard " + getSelectedLeaderboardName() + " up: "
                                + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        loadAndRefreshLeaderboard(selectedLeaderboardName);
                    }
                }));
    }

    @Override
    protected void leaderboardRaceColumnSelectionChanged() {
        selectedRaceInLeaderboard = getSelectedRaceColumnWithFleet();
        if (selectedRaceInLeaderboard != null) {
            columnMoveUpButton.setEnabled(true);
            columnMoveDownButton.setEnabled(true);
            selectTrackedRaceInRaceList();
        } else {
            columnMoveUpButton.setEnabled(false);
            columnMoveDownButton.setEnabled(false);
            trackedRacesListComposite.clearSelection();
        }
    }

    private void setIsMedalRace(String leaderboardName, final RaceColumnDTO raceInLeaderboard,
            final boolean isMedalRace) {
        sailingServiceWrite.updateIsMedalRace(leaderboardName, raceInLeaderboard.getRaceColumnName(), isMedalRace,
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.errorUpdatingIsMedalRace(caught.getMessage()));
                    }

                    @Override
                    public void onSuccess(Void result) {
                        getSelectedLeaderboard().setIsMedalRace(raceInLeaderboard.getRaceColumnName(), isMedalRace);
                    }
                });
    }

    private void addRaceColumnsToLeaderboard() {
        final String leaderboardName = getSelectedLeaderboardName();
        final List<RaceColumnDTO> existingRaceColumns = new ArrayList<RaceColumnDTO>();
        for (RaceColumnDTOAndFleetDTOWithNameBasedEquality pair : raceColumnTable.getDataProvider().getList()) {
            existingRaceColumns.add(pair.getA());
        }
        final RaceColumnsInLeaderboardDialog raceDialog = new RaceColumnsInLeaderboardDialog(existingRaceColumns,
                stringMessages, new DialogCallback<List<RaceColumnDTO>>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(final List<RaceColumnDTO> result) {
                        updateRaceColumnsOfLeaderboard(leaderboardName, existingRaceColumns, result);
                    }
                });
        raceDialog.ensureDebugId("RaceColumnsInLeaderboardDialog");
        raceDialog.show();
    }

    private void updateRaceColumnsOfLeaderboard(final String leaderboardName, List<RaceColumnDTO> existingRaceColumns,
            List<RaceColumnDTO> newRaceColumns) {
        final List<Util.Pair<String, Boolean>> raceColumnsToAdd = new ArrayList<Util.Pair<String, Boolean>>();

        for (RaceColumnDTO newRaceColumn : newRaceColumns) {
            if (!existingRaceColumns.contains(newRaceColumn)) {
                raceColumnsToAdd
                        .add(new Util.Pair<String, Boolean>(newRaceColumn.getName(), newRaceColumn.isMedalRace()));
            }
        }

        sailingServiceWrite.addColumnsToLeaderboard(leaderboardName, raceColumnsToAdd,
                new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to add race columns to leaderboard " + leaderboardName
                                + ": " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void v) {
                        loadAndRefreshLeaderboard(leaderboardName);
                    }
                }));
    }

    @Override
    protected void leaderboardSelectionChanged() {
        Set<StrippedLeaderboardDTO> selectedLeaderboards = leaderboardSelectionModel.getSelectedSet();
        boolean canDeleteAllSelected = true;
        for (StrippedLeaderboardDTO leaderboard : selectedLeaderboards) {
            if (!userService.hasPermission(leaderboard, DefaultActions.DELETE)) {
                canDeleteAllSelected = false;
            }
        }
        leaderboardRemoveButton.setEnabled(!selectedLeaderboards.isEmpty() && canDeleteAllSelected);
        final StrippedLeaderboardDTO selectedLeaderboard = getSelectedLeaderboard();
        if (leaderboardSelectionModel.getSelectedSet().size() == 1 && selectedLeaderboard != null) {
            raceColumnTable.getDataProvider().getList().clear();
            for (RaceColumnDTO raceColumn : selectedLeaderboard.getRaceList()) {
                for (FleetDTO fleet : raceColumn.getFleets()) {
                    raceColumnTable.getDataProvider().getList().add(new RaceColumnDTOAndFleetDTOWithNameBasedEquality(
                            raceColumn, fleet, selectedLeaderboard));
                }
            }
            selectedLeaderBoardPanel.setVisible(true);
            selectedLeaderBoardPanel.setCaptionText("Details of leaderboard '" + selectedLeaderboard.getName() + "'");
            if (!selectedLeaderboard.type.isMetaLeaderboard()) {
                trackedRacesListComposite.setRegattaFilterValue(selectedLeaderboard.regattaName);
                trackedRacesCaptionPanel.setVisible(true);
            }

            final boolean hasPermission = userService.hasPermission(selectedLeaderboard, UPDATE);
            final boolean isRegattaLeaderboard = selectedLeaderboard.type.isRegattaLeaderboard();
            addRaceColumnsButton.setVisible(hasPermission && !isRegattaLeaderboard);
            columnMoveUpButton.setVisible(hasPermission && !isRegattaLeaderboard);
            columnMoveDownButton.setVisible(hasPermission && !isRegattaLeaderboard);
        } else {
            selectedLeaderBoardPanel.setVisible(false);
            trackedRacesCaptionPanel.setVisible(false);
            selectedRaceInLeaderboard = null;
        }
    }

    private void createFlexibleLeaderboard() {
        sailingServiceWrite.getEvents(new MarkedAsyncCallback<List<EventDTO>>(new AsyncCallback<List<EventDTO>>() {
            @Override
            public void onSuccess(List<EventDTO> result) {
                createFlexibleLeaderboard(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                createFlexibleLeaderboard(new ArrayList<EventDTO>());
            }
        }));
    }

    private void createFlexibleLeaderboard(List<EventDTO> existingEvents) {
        final FlexibleLeaderboardCreateDialog dialog = new FlexibleLeaderboardCreateDialog(
                Collections.unmodifiableCollection(availableLeaderboardList), userService,
                stringMessages, Collections.unmodifiableCollection(existingEvents),
                errorReporter, new DialogCallback<LeaderboardDescriptor>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(final LeaderboardDescriptor newLeaderboard) {
                        sailingServiceWrite.createFlexibleLeaderboard(newLeaderboard.getName(),
                                newLeaderboard.getDisplayName(), newLeaderboard.getDiscardThresholds(),
                                newLeaderboard.getScoringScheme(), newLeaderboard.getCourseAreaIds(),
                                new MarkedAsyncCallback<StrippedLeaderboardDTO>(
                                        new AsyncCallback<StrippedLeaderboardDTO>() {
                                            @Override
                                            public void onFailure(Throwable t) {
                                                errorReporter.reportError(
                                                        "Error trying to create the new flexible leaderboard "
                                                                + newLeaderboard.getName() + ": " + t.getMessage());
                                            }

                                            @Override
                                            public void onSuccess(StrippedLeaderboardDTO result) {
                                                addLeaderboard(result);
                                            }
                                        }));
                    }
                });
        dialog.ensureDebugId("FlexibleLeaderboardCreateDialog");
        dialog.show();
    }

    private void createRegattaLeaderboard() {
        RegattaLeaderboardCreateDialog<LeaderboardDescriptor> dialog = new RegattaLeaderboardCreateDialog<LeaderboardDescriptor>(
                Collections.unmodifiableCollection(availableLeaderboardList),
                Collections.unmodifiableCollection(allRegattas), new LeaderboardDescriptor(), stringMessages,
                errorReporter, new DialogCallback<LeaderboardDescriptor>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(final LeaderboardDescriptor newLeaderboard) {
                        RegattaName regattaIdentifier = new RegattaName(newLeaderboard.getRegattaName());
                        sailingServiceWrite.createRegattaLeaderboard(regattaIdentifier, newLeaderboard.getDisplayName(),
                                newLeaderboard.getDiscardThresholds(),
                                new AsyncCallback<StrippedLeaderboardDTO>() {
                                    @Override
                                    public void onFailure(Throwable t) {
                                        errorReporter.reportError("Error trying to create the new regatta leaderboard "
                                                + newLeaderboard.getName() + ": " + t.getMessage());
                                    }

                                    @Override
                                    public void onSuccess(StrippedLeaderboardDTO result) {
                                        addLeaderboard(result);
                                    }
                                });
                    }
                });
        dialog.ensureDebugId("RegattaLeaderboardCreateDialog");
        dialog.show();
    }

    private void createRegattaLeaderboardWithEliminations() {
        RegattaLeaderboardWithEliminationsCreateDialog dialog = new RegattaLeaderboardWithEliminationsCreateDialog(
                sailingServiceWrite, userService, Collections.unmodifiableCollection(availableLeaderboardList),
                Collections.unmodifiableCollection(allRegattas), stringMessages, errorReporter,
                new DialogCallback<LeaderboardDescriptorWithEliminations>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(final LeaderboardDescriptorWithEliminations newLeaderboard) {
                        sailingServiceWrite.createRegattaLeaderboardWithEliminations(newLeaderboard.getName(),
                                newLeaderboard.getDisplayName(), newLeaderboard.getRegattaName(),
                                new AsyncCallback<StrippedLeaderboardDTO>() {
                                    @Override
                                    public void onFailure(Throwable t) {
                                        errorReporter.reportError("Error trying to create the new regatta leaderboard "
                                                + newLeaderboard.getName() + ": " + t.getMessage());
                                    }

                                    @Override
                                    public void onSuccess(StrippedLeaderboardDTO result) {
                                        addLeaderboard(result);
                                        sailingServiceWrite.setEliminatedCompetitors(newLeaderboard.getName(),
                                                newLeaderboard.getEliminatedCompetitors(), new AsyncCallback<Void>() {
                                                    @Override
                                                    public void onFailure(Throwable caught) {
                                                        errorReporter.reportError(
                                                                "Error trying to set the eliminated competitors for leaderboard "
                                                                        + newLeaderboard.getName() + ": "
                                                                        + caught.getMessage());
                                                    }

                                                    @Override
                                                    public void onSuccess(Void result) {
                                                        // nothing to do for now until any elimination properties are
                                                        // shown in the leaderboard table...
                                                    }
                                                });
                                    }
                                });
                    }
                });
        dialog.ensureDebugId("RegattaLeaderboardCreateDialog");
        dialog.show();
    }

    private void createRegattaLeaderboardWithOtherTieBreakingLeaderboard() {
        RegattaLeaderboardWithOtherTieBreakingLeaderboardCreateDialog dialog = new RegattaLeaderboardWithOtherTieBreakingLeaderboardCreateDialog(
                Collections.unmodifiableCollection(availableLeaderboardList),
                Collections.unmodifiableCollection(allRegattas), stringMessages, errorReporter,
                new DialogCallback<LeaderboardDescriptorWithOtherTieBreakingLeaderboard>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(final LeaderboardDescriptorWithOtherTieBreakingLeaderboard newLeaderboard) {
                        final RegattaName regattaIdentifier = new RegattaName(newLeaderboard.getRegattaName());
                        sailingServiceWrite.createRegattaLeaderboardWithOtherTieBreakingLeaderboard(
                                regattaIdentifier, newLeaderboard.getDisplayName(),
                                newLeaderboard.getDiscardThresholds(), newLeaderboard.getOtherTieBreakingLeaderboardName(),
                                new AsyncCallback<StrippedLeaderboardDTO>() {
                                    @Override
                                    public void onFailure(Throwable t) {
                                        errorReporter.reportError("Error trying to create the new regatta leaderboard "
                                                + newLeaderboard.getName() + ": " + t.getMessage());
                                    }

                                    @Override
                                    public void onSuccess(StrippedLeaderboardDTO result) {
                                        addLeaderboard(result);
                                    }
                                });
                    }
                });
        dialog.ensureDebugId("RegattaLeaderboardWithOtherTieBreakingLeaderboardCreateDialog");
        dialog.show();
    }

    private void addLeaderboard(StrippedLeaderboardDTO result) {
        filteredLeaderboardList.getList().add(result);
        availableLeaderboardList.add(result);
        presenter.getLeaderboardsRefresher().add(result);
        leaderboardSelectionModel.clear();
        leaderboardSelectionModel.setSelected(result, true);
        loadAndRefreshLeaderboard(result);
    }

    private void updateLeaderboard(final String leaderboardName, final LeaderboardDescriptor leaderboardToUpdate) {
        sailingServiceWrite.updateLeaderboard(leaderboardName, leaderboardToUpdate.getDisplayName(),
                leaderboardToUpdate.getDiscardThresholds(), leaderboardToUpdate.getCourseAreaIds(),
                new AsyncCallback<StrippedLeaderboardDTO>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError(
                                "Error trying to update leaderboard " + leaderboardName + ": " + t.getMessage());
                    }

                    @Override
                    public void onSuccess(StrippedLeaderboardDTO updatedLeaderboard) {
                        refreshLeaderboardInTable(leaderboardName, updatedLeaderboard);
                    }
                });
    }

    private void reloadLeaderboardForTable(final String leaderboardName) {
        sailingServiceWrite.getLeaderboardWithSecurity(leaderboardName,
                new AsyncCallback<StrippedLeaderboardDTO>() {
                    @Override
                    public void onSuccess(StrippedLeaderboardDTO result) {
                        refreshLeaderboardInTable(leaderboardName, result);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(
                                "Error trying to load leaderboard " + leaderboardName + ": " + caught.getMessage());
                    }
                });
    }

    private void refreshLeaderboardInTable(final String oldLeaderboardName,
            StrippedLeaderboardDTO updatedLeaderboard) {
        int indexOfLeaderboard = 0;
        for (int i = 0; i < availableLeaderboardList.size(); i++) {
            StrippedLeaderboardDTO dao = availableLeaderboardList.get(i);
            if (dao.getName().equals(oldLeaderboardName)) {
                indexOfLeaderboard = i;
                break;
            }
        }
        getLeaderboardsRefresher().addIfNotContainedElseReplace(updatedLeaderboard,
                new NameBasedStrippedLeaderboardDTOEntityIdentityComparator());
        availableLeaderboardList.set(indexOfLeaderboard, updatedLeaderboard);
        filterLeaderboardPanel.updateAll(availableLeaderboardList);
    }

    private void removeLeaderboards(final Collection<StrippedLeaderboardDTO> leaderboards) {
        if (!leaderboards.isEmpty()) {
            Set<String> leaderboardNames = new HashSet<String>();
            for (StrippedLeaderboardDTO leaderboard : leaderboards) {
                leaderboardNames.add(leaderboard.getName());
            }
            sailingServiceWrite.removeLeaderboards(leaderboardNames, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Error trying to remove the leaderboards:" + caught.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    for (StrippedLeaderboardDTO leaderboard : leaderboards) {
                        removeLeaderboardFromTable(leaderboard);
                        getLeaderboardsRefresher().remove(leaderboard);
                    }
                    getLeaderboardsRefresher().callAllFill();
                }
            });
        }
    }

    private void removeLeaderboard(final StrippedLeaderboardDTO leaderBoard) {
        sailingServiceWrite.removeLeaderboard(leaderBoard.getName(),
                new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to remove leaderboard " + leaderBoard.getName() + ": "
                                + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        removeLeaderboardFromTable(leaderBoard);
                        getLeaderboardsRefresher().remove(leaderBoard);
                        getLeaderboardsRefresher().callAllFill();
                    }
                }));
    }

    private void removeLeaderboardFromTable(final StrippedLeaderboardDTO leaderBoard) {
        filteredLeaderboardList.getList().remove(leaderBoard);
        availableLeaderboardList.remove(leaderBoard);
        leaderboardSelectionModel.setSelected(leaderBoard, false);
    }

    private void createPairingListTemplate(final StrippedLeaderboardDTO leaderboardDTO) {
        final PairingListCreationSetupDialog dialog = new PairingListCreationSetupDialog(leaderboardDTO,
                this.stringMessages, new DialogCallback<PairingListTemplateDTO>() {
                    @Override
                    public void ok(PairingListTemplateDTO editedObject) {
                        BusyDialog busyDialog = new BusyDialog();
                        busyDialog.show();
                        sailingServiceWrite.calculatePairingListTemplate(editedObject.getFlightCount(),
                                editedObject.getGroupCount(), editedObject.getCompetitorCount(),
                                editedObject.getFlightMultiplier(), editedObject.getBoatChangeFactor(),
                                new AsyncCallback<PairingListTemplateDTO>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        busyDialog.hide();
                                        errorReporter.reportError(stringMessages
                                                .errorCalculatingPairingListTemplate(caught.getMessage()));
                                    }

                                    @Override
                                    public void onSuccess(PairingListTemplateDTO result) {
                                        busyDialog.hide();
                                        result.setSelectedFlightNames(editedObject.getSelectedFlightNames());
                                        openPairingListCreationDialog(leaderboardDTO, result);
                                    }
                                });
                    }

                    @Override
                    public void cancel() {
                    }
                });
        dialog.show();
    }
    
    private void copyPairingListFromOtherLeaderboard(final StrippedLeaderboardDTO targetLeaderboardDTO) {
        final CopyPairingListDialog dialog = new CopyPairingListDialog(availableLeaderboardList,
                Collections.unmodifiableCollection(allRegattas), targetLeaderboardDTO, stringMessages,
                new DialogCallback<CopyPairingListDialog.Result>() {
                    @Override
                    public void ok(CopyPairingListDialog.Result editedObject) {
                        sailingServiceWrite.copyPairingListFromOtherLeaderboard(
                            editedObject.getSourceLeaderboardName(), targetLeaderboardDTO.getName(),
                            editedObject.getFromRaceColumnName(), editedObject.getToRaceColumnInclusiveName(),
                            new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    Notification.notify(
                                        stringMessages.errorCopyingPairings(caught.getMessage()),
                                        NotificationType.ERROR);
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    Notification.notify(
                                            stringMessages.successfullyCopiedPairings(),
                                            NotificationType.SUCCESS);
                                }
                            });
                        // TODO
                    }

                    @Override
                    public void cancel() {
                    }
                });
        dialog.show();
    }

    private void openPairingListCreationDialog(StrippedLeaderboardDTO leaderboardDTO,
            PairingListTemplateDTO template) {
        PairingListCreationDialog dialog = new PairingListCreationDialog(leaderboardDTO, stringMessages, template,
                sailingServiceWrite, errorReporter);
        dialog.show();
    }

    private void openPairingListEntryPoint(StrippedLeaderboardDTO leaderboardDTO) {
        Map<String, String> result = new HashMap<>();
        result.put("leaderboardName", leaderboardDTO.getName());
        String link = EntryPointLinkFactory.createPairingListLink(result);
        Window.open(link, "", "");
    }
    
}
