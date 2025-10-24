package com.sap.sailing.gwt.ui.leaderboardedit;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.CompositeCell;
import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.IncrementalOrFullLeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardEntryDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.settings.client.leaderboard.EditableLeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.EditableLeaderboardSettingsDialogComponent;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardSettings;
import com.sap.sailing.gwt.ui.actions.GetLeaderboardByNameAction;
import com.sap.sailing.gwt.ui.client.Collator;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionModel;
import com.sap.sailing.gwt.ui.client.FlagImageRenderer;
import com.sap.sailing.gwt.ui.client.FlagImageResolverImpl;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.filter.CompetitorRaceRankFilter;
import com.sap.sailing.gwt.ui.leaderboard.ClassicLeaderboardStyle;
import com.sap.sailing.gwt.ui.leaderboard.CompetitorColumnBase;
import com.sap.sailing.gwt.ui.leaderboard.CompetitorFetcher;
import com.sap.sailing.gwt.ui.leaderboard.ExpandableSortableColumn;
import com.sap.sailing.gwt.ui.leaderboard.ExplicitRaceColumnSelection;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardPanel;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardSortableColumnWithMinMax;
import com.sap.sailing.gwt.ui.leaderboardedit.EditScoreDialog.ScoreCorrectionUpdate;
import com.sap.sse.common.InvertibleComparator;
import com.sap.sse.common.SortingOrder;
import com.sap.sse.common.Util;
import com.sap.sse.common.filter.BinaryOperator;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.common.impl.InvertibleComparatorAdapter;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.async.AsyncAction;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.AbstractSortableColumnWithMinMax;
import com.sap.sse.gwt.client.celltable.BaseCelltable;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.formfactor.DeviceDetector;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.shared.components.SettingsDialog;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.gwt.client.useragent.UserAgentDetails;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.WithSecurity;

/**
 * An editable version of the {@link ClassicLeaderboardPanel} which allows a user to enter carried / accumulated
 * points and fix individual race scores.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class EditableLeaderboardPanel extends LeaderboardPanel<EditableLeaderboardSettings> {

    //private static final Logger logger = Logger.getLogger(EditableLeaderboardPanel.class.getName());
    private static EditableLeaderboardResources resources = GWT.create(EditableLeaderboardResources.class);

    final DateBox lastScoreCorrectionTimeBox;
    final TextBox lastScoreCorrectionCommentBox;

    final private CellTable<CompetitorDTO> suppressedCompetitorsTable;
    final private ListDataProvider<CompetitorDTO> suppressedCompetitorsShown;

    private CheckBox showUncorrectedTotalPointsCheckbox;
    
    private ListBox raceListBox;
    private String raceListSelection;
    private boolean showCarryColumn;
    
    private final SailingServiceWriteAsync sailingServiceWrite; 
    
    private class SettingsClickHandler implements ClickHandler {
        private final StringMessages stringMessages;

        private SettingsClickHandler(StringMessages stringMessages) {
            this.stringMessages = stringMessages;
        }

        @Override
        public void onClick(ClickEvent event) {
            new SettingsDialog<EditableLeaderboardSettings>(EditableLeaderboardPanel.this, stringMessages).show();
        }
    }

    private class EditableCarryColumn extends CarryColumn {
        public EditableCarryColumn() {
            super(new CompositeCell<LeaderboardRowDTO>(getCellListForEditableCarryColumn()));
            this.setCellStyleNames("accumulatedCell");
        }
    }

    private class EditableCompetitorColumn extends CompetitorColumn {
        public EditableCompetitorColumn(List<HasCell<LeaderboardRowDTO, ?>> cells,
                CompetitorColumnBase<LeaderboardRowDTO> base) {
            super(new CompositeCell<LeaderboardRowDTO>(cells), base);
            this.setCellStyleNames("competitorNameCell");
        }

        @Override
        public void render(Context context, LeaderboardRowDTO object, SafeHtmlBuilder sb) {
            super.defaultRender(context, object, sb);
        }
    }

    /**
     * Shows the country flag and sail ID, if present
     * 
     * @author Axel Uhl (d043530)
     * 
     */
    private class SuppressedShortInfoColumn extends LeaderboardSortableColumnWithMinMax<CompetitorDTO, String> {
        protected SuppressedShortInfoColumn() {
            super(new TextCell(), SortingOrder.ASCENDING, EditableLeaderboardPanel.this);
            this.setCellStyleNames("competitorInfoCell");
        }

        /**
         * Depending on the {@link LeaderboardDTO LeaderboardDTO's}
         * {@link AbstractLeaderboardDTO#canBoatsOfCompetitorsChangePerRace canBoatsOfCompetitorsChangePerRace} flag
         * there is a different precedence of showing either the sail ID (no changing boats) or the competitor short
         * name (changing boats). This method handles the resolution of the text to show for a competitor in this
         * column.
         */
        private String getRealTextToShow(CompetitorDTO competitor) {
            boolean preferSailId = !getLeaderboard().canBoatsOfCompetitorsChangePerRace;
            return competitor.getShortInfo(preferSailId);
        }

        @Override
        public InvertibleComparator<CompetitorDTO> getComparator() {
            return new InvertibleComparatorAdapter<CompetitorDTO>() {
                @Override
                public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                    return Collator.getInstance().compare(getRealTextToShow(o1), getRealTextToShow(o2));
                }
            };
        }

        @Override
        public Header<String> getHeader() {
            return new TextHeader(stringMessages.competitor());
        }

        @Override
        public void render(Context context, CompetitorDTO object, SafeHtmlBuilder sb) {
            final String twoLetterIsoCountryCode = object.getTwoLetterIsoCountryCode();
            final ImageResource flagImageResource;
            if (twoLetterIsoCountryCode==null || twoLetterIsoCountryCode.isEmpty()) {
                flagImageResource = FlagImageResolverImpl.get().getEmptyFlagImageResource();
            } else {
                flagImageResource = FlagImageResolverImpl.get().getFlagImageResource(twoLetterIsoCountryCode);
            }
            if (flagImageResource != null) {
                sb.append(FlagImageRenderer.image(flagImageResource.getSafeUri().asString()));
                sb.appendHtmlConstant("&nbsp;");
            }
            sb.appendEscaped(getRealTextToShow(object));
        }

        @Override
        public String getValue(CompetitorDTO object) {
            return object.getShortInfo();
        }
    }

    protected class SuppressedCompetitorColumn
            extends LeaderboardSortableColumnWithMinMax<CompetitorDTO, CompetitorDTO> {
        private final CompetitorColumnBase<CompetitorDTO> base;

        protected SuppressedCompetitorColumn(CompetitorColumnBase<CompetitorDTO> base) {
            super(base.getCell(getLeaderboard()), SortingOrder.ASCENDING, EditableLeaderboardPanel.this);
            this.setCellStyleNames("suppressedCompetitorCell");
            this.base = base;
        }

        @Override
        public InvertibleComparator<CompetitorDTO> getComparator() {
            return base.getComparator();
        }

        @Override
        public SafeHtmlHeader getHeader() {
            return base.getHeader();
        }

        @Override
        public CompetitorDTO getValue(CompetitorDTO competitor) {
            return competitor;
        }

        @Override
        public void render(Context context, CompetitorDTO competitor, SafeHtmlBuilder sb) {
            base.render(context, competitor, sb);
        }
    }

    private FieldUpdater<LeaderboardRowDTO, String> createCompetitorColumnFieldUpdater(final EditTextCell cell) {
        return new FieldUpdater<LeaderboardRowDTO, String>() {
            @Override
            public void update(final int rowIndex, final LeaderboardRowDTO row, final String value) {
                getSailingService().updateCompetitorDisplayNameInLeaderboard(getLeaderboardName(),
                        row.competitor.getIdAsString(), value == null || value.length() == 0 ? null : value.trim(),
                        new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable t) {
                                EditableLeaderboardPanel.this.getErrorReporter()
                                        .reportError(stringMessages.errorUpdatingDisplayNames(row.competitor.getName(),
                                                getLeaderboardName(), t.getMessage()));
                            }

                            @Override
                            public void onSuccess(Void v) {
                                if (getLeaderboard().competitorDisplayNames == null) {
                                    getLeaderboard().competitorDisplayNames = new HashMap<>();
                                }
                                getLeaderboard().competitorDisplayNames.put(row.competitor,
                                        value == null || value.trim().length() == 0 ? null : value.trim());
                                cell.setViewData(row, null); // ensure that getValue() is called again
                                EditableLeaderboardPanel.this.getData().getList()
                                        .set(EditableLeaderboardPanel.this.getData().getList().indexOf(row), row);
                            }
                        });
            }
        };
    }

    private class CompositeCellRememberingRenderingContextAndObject extends CompositeCell<LeaderboardRowDTO> {
        private final List<RowUpdateWhiteboardProducerThatAlsoHasCell<LeaderboardRowDTO, ?>> cells;

        public CompositeCellRememberingRenderingContextAndObject(
                List<RowUpdateWhiteboardProducerThatAlsoHasCell<LeaderboardRowDTO, ?>> hasCells) {
            super(new ArrayList<HasCell<LeaderboardRowDTO, ?>>(hasCells));
            cells = hasCells;
        }

        @Override
        public void render(Context context, LeaderboardRowDTO value, SafeHtmlBuilder sb) {
            tellCellsWhatIsBeingRendered(value);
            super.render(context, value, sb);
        }

        private void tellCellsWhatIsBeingRendered(LeaderboardRowDTO value) {
            for (RowUpdateWhiteboardProducerThatAlsoHasCell<LeaderboardRowDTO, ?> cell : cells) {
                cell.setCurrentlyRendering(value);
            }
        }

        @Override
        protected <X> void render(Context context, LeaderboardRowDTO value, SafeHtmlBuilder sb,
                HasCell<LeaderboardRowDTO, X> hasCell) {
            tellCellsWhatIsBeingRendered(value);
            super.render(context, value, sb, hasCell);
        }
    }

    private class EditableRaceColumn extends RaceColumn<LeaderboardRowDTO>
            implements RowUpdateWhiteboardOwner<LeaderboardRowDTO> {
        private RowUpdateWhiteboard<LeaderboardRowDTO> currentRowUpdateWhiteboard;

        public EditableRaceColumn(RaceColumnDTO race,
                List<RowUpdateWhiteboardProducerThatAlsoHasCell<LeaderboardRowDTO, ?>> cellList) {
            super(race, /* expandable */ false, // we don't want leg expansion when editing scores
                    new CompositeCellRememberingRenderingContextAndObject(cellList), SortingOrder.NONE,
                    RACE_COLUMN_HEADER_STYLE, RACE_COLUMN_STYLE);
            this.setCellStyleNames("raceCell");
            for (RowUpdateWhiteboardProducer<LeaderboardRowDTO> rowUpdateWhiteboardProducer : cellList) {
                rowUpdateWhiteboardProducer.setWhiteboardOwner(this);
            }
            // the field updater for the composite is invoked after any component has updated its field
            setFieldUpdater(new FieldUpdater<LeaderboardRowDTO, LeaderboardRowDTO>() {
                @Override
                public void update(int rowIndex, LeaderboardRowDTO row, LeaderboardRowDTO value) {
                    currentRowUpdateWhiteboard.setIndexOfRowToUpdate(rowIndex);
                    currentRowUpdateWhiteboard = null; // show that it has been consumed and updated
                }
            });
        }

        @Override
        public void render(Context context, LeaderboardRowDTO object, SafeHtmlBuilder html) {
            defaultRender(context, object, html);
        }

        @Override
        public LeaderboardRowDTO getValue(LeaderboardRowDTO object) {
            return object;
        }

        @Override
        public void whiteboardProduced(RowUpdateWhiteboard<LeaderboardRowDTO> whiteboard) {
            currentRowUpdateWhiteboard = whiteboard;
        }
    }

    private class MaxPointsDropDownCellProvider
            extends AbstractRowUpdateWhiteboardProducerThatHasCell<LeaderboardRowDTO, String> {
        private final SelectionCell dropDownCell;
        private final String raceColumnName;

        public MaxPointsDropDownCellProvider(String raceColumnName) {
            this.raceColumnName = raceColumnName;
            List<String> selectionCellContents = new ArrayList<String>();
            selectionCellContents.add(""); // represents "no" max points reason
            for (MaxPointsReason maxPointReason : MaxPointsReason.values()) {
                selectionCellContents.add(maxPointReason.name());
            }
            dropDownCell = new SelectionCell(selectionCellContents);
        }

        @Override
        public SelectionCell getCell() {
            return dropDownCell;
        }

        @Override
        public FieldUpdater<LeaderboardRowDTO, String> getFieldUpdater() {
            return new FieldUpdater<LeaderboardRowDTO, String>() {
                @Override
                public void update(final int rowIndex, final LeaderboardRowDTO row, final String value) {
                    final RowUpdateWhiteboard<LeaderboardRowDTO> whiteboard = new RowUpdateWhiteboard<LeaderboardRowDTO>(
                            EditableLeaderboardPanel.this.getData());
                    getWhiteboardOwner().whiteboardProduced(whiteboard);
                    addBusyTask();
                    getSailingService().updateLeaderboardMaxPointsReason(getLeaderboardName(),
                            row.competitor.getIdAsString(), raceColumnName,
                            value == null || value.trim().length() == 0 ? null : MaxPointsReason.valueOf(value.trim()),
                            getLeaderboardDisplayDate(), new AsyncCallback<Util.Triple<Double, Double, Boolean>>() {
                                @Override
                                public void onFailure(Throwable t) {
                                    removeBusyTask();
                                    getErrorReporter().reportError(stringMessages.errorUpdatingMaxPointsReason(
                                            row.competitor.getName(), getLeaderboardName(), t.getMessage()));
                                }

                                @Override
                                public void onSuccess(
                                        Util.Triple<Double, Double, Boolean> newTotalAndNetPointsAndIsCorrected) {
                                    removeBusyTask();
                                    row.fieldsByRaceColumnName.get(raceColumnName).reasonForMaxPoints = value == null
                                            || value.length() == 0 ? null : MaxPointsReason.valueOf(value.trim());
                                    row.fieldsByRaceColumnName.get(
                                            raceColumnName).totalPoints = newTotalAndNetPointsAndIsCorrected.getA();
                                    row.fieldsByRaceColumnName
                                            .get(raceColumnName).netPoints = newTotalAndNetPointsAndIsCorrected.getB();
                                    row.fieldsByRaceColumnName.get(
                                            raceColumnName).totalPointsCorrected = newTotalAndNetPointsAndIsCorrected
                                                    .getC();
                                    getCell().setViewData(row, null); // ensure that getValue() is called again
                                    whiteboard.setObjectWithWhichToUpdateRow(row);
                                }
                            });
                }
            };
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            LeaderboardEntryDTO leaderboardEntryDTO = object.fieldsByRaceColumnName.get(raceColumnName);
            MaxPointsReason reasonForMaxPoints = null;
            if (leaderboardEntryDTO != null) {
                reasonForMaxPoints = leaderboardEntryDTO.reasonForMaxPoints;
            }
            return reasonForMaxPoints == null ? "" : reasonForMaxPoints.name();
        }
    }
    
    private class ReasonForMaxPointsTextViewProvider
            extends AbstractRowUpdateWhiteboardProducerThatHasCell<LeaderboardRowDTO, String> {
        private final String raceColumnName;

        protected ReasonForMaxPointsTextViewProvider(String raceColumnName) {
            this.raceColumnName = raceColumnName;
        }

        @Override
        public Cell<String> getCell() {
            return new TextCell();
        }

        @Override
        public FieldUpdater<LeaderboardRowDTO, String> getFieldUpdater() {
            return null;
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            LeaderboardEntryDTO leaderboardEntryDTO = object.fieldsByRaceColumnName.get(raceColumnName);
            MaxPointsReason reasonForMaxPoints = null;
            if (leaderboardEntryDTO != null) {
                reasonForMaxPoints = leaderboardEntryDTO.reasonForMaxPoints;
            }
            return reasonForMaxPoints == null || reasonForMaxPoints == MaxPointsReason.NONE ? ""
                    : " " + reasonForMaxPoints.name();
        }
    }

    private class UncorrectedTotalPointsViewProvider
            extends AbstractRowUpdateWhiteboardProducerThatHasCell<LeaderboardRowDTO, String> {
        private final String raceColumnName;
        
        protected UncorrectedTotalPointsViewProvider(String raceColumnName) {
            this.raceColumnName = raceColumnName;
        }

        @Override
        public Cell<String> getCell() {
            return new TextCell();
        }

        @Override
        public FieldUpdater<LeaderboardRowDTO, String> getFieldUpdater() {
            return null;
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            LeaderboardEntryDTO leaderboardEntryDTO = object.fieldsByRaceColumnName.get(raceColumnName);
            String result = "";
            if (leaderboardEntryDTO != null && leaderboardEntryDTO.totalPointsUncorrected != null) {
                result=" ("+scoreFormat.format(leaderboardEntryDTO.totalPointsUncorrected)+")";
            }
            return result;
        }
    }

    private class IncrementalScoreCorrectionViewProvider
            extends AbstractRowUpdateWhiteboardProducerThatHasCell<LeaderboardRowDTO, String> {
        private final String raceColumnName;
        
        protected IncrementalScoreCorrectionViewProvider(String raceColumnName) {
            this.raceColumnName = raceColumnName;
        }

        @Override
        public Cell<String> getCell() {
            return new TextCell();
        }

        @Override
        public FieldUpdater<LeaderboardRowDTO, String> getFieldUpdater() {
            return null;
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            final LeaderboardEntryDTO leaderboardEntryDTO = object.fieldsByRaceColumnName.get(raceColumnName);
            String result = "";
            if (leaderboardEntryDTO != null && leaderboardEntryDTO.incrementalScoreCorrectionInPoints != null) {
                final char sign = Math.signum(leaderboardEntryDTO.incrementalScoreCorrectionInPoints) >= 0 ? '+' : '-';
                result=" ["+sign+scoreFormat.format(leaderboardEntryDTO.incrementalScoreCorrectionInPoints)+"]";
            }
            return result;
        }
    }

    private class TotalPointsEditCellProvider
            extends AbstractRowUpdateWhiteboardProducerThatHasCell<LeaderboardRowDTO, String> {
        private final EditTextCell totalPointsEditCell;
        private final String raceColumnName;

        protected TotalPointsEditCellProvider(String raceColumnName) {
            this.raceColumnName = raceColumnName;
            totalPointsEditCell = new EditTextCell(new SafeHtmlRenderer<String>() {
                @Override
                public void render(String totalPointsAsString, SafeHtmlBuilder html) {
                    if (totalPointsAsString != null) {
                        final boolean totalPointsCorrected = isTotalPointsCorrected();
                        if (totalPointsCorrected) {
                            html.appendHtmlConstant("<b>");
                        }
                        html.appendEscaped(totalPointsAsString);
                        if (totalPointsCorrected) {
                            html.appendHtmlConstant("</b>");
                        }
                    }
                }

                @Override
                public SafeHtml render(String totalPointsAsString) {
                    final boolean totalPointsCorrected = isTotalPointsCorrected();
                    String prefix;
                    String suffix;
                    if (totalPointsCorrected) {
                        prefix = "<b class='bold'>";
                        suffix = "</b>";
                    } else {
                        prefix = "";
                        suffix = "";
                    }
                    return SafeHtmlUtils.fromSafeConstant(
                            prefix + SafeHtmlUtils.fromString(totalPointsAsString).asString() + suffix);
                }
            });
        }

        private boolean isTotalPointsCorrected() {
            LeaderboardEntryDTO leaderboardEntryDTO = getCurrentlyRendering().fieldsByRaceColumnName
                    .get(raceColumnName);
            return leaderboardEntryDTO != null && leaderboardEntryDTO.totalPointsCorrected;
        }

        @Override
        public EditTextCell getCell() {
            return totalPointsEditCell;
        }

        @Override
        public FieldUpdater<LeaderboardRowDTO, String> getFieldUpdater() {
            return new FieldUpdater<LeaderboardRowDTO, String>() {
                @Override
                public void update(final int rowIndex, final LeaderboardRowDTO row, final String value) {
                    final RowUpdateWhiteboard<LeaderboardRowDTO> whiteboard = new RowUpdateWhiteboard<LeaderboardRowDTO>(
                            EditableLeaderboardPanel.this.getData());
                    getWhiteboardOwner().whiteboardProduced(whiteboard);
                    addBusyTask();
                    getSailingService().updateLeaderboardScoreCorrection(getLeaderboardName(),
                            row.competitor.getIdAsString(), raceColumnName,
                            value == null || value.trim().length() == 0 ? null
                                    : value.trim().equals("-") ? null : Double.valueOf(value.trim()),
                            getLeaderboardDisplayDate(), new AsyncCallback<Util.Triple<Double, Double, Boolean>>() {
                                @Override
                                public void onFailure(Throwable t) {
                                    removeBusyTask();
                                    getErrorReporter().reportError(
                                            stringMessages.errorUpdatingLeaderboardScore(row.competitor.getName(),
                                                    getLeaderboardName(), raceColumnName, t.getMessage()));
                                }

                                @Override
                                public void onSuccess(
                                        Util.Triple<Double, Double, Boolean> newTotalAndTotalPointsAndIsCorrected) {
                                    removeBusyTask();
                                    final LeaderboardEntryDTO leaderboardEntryDTO = row.fieldsByRaceColumnName
                                            .get(raceColumnName);
                                    leaderboardEntryDTO.totalPoints = value == null || value.length() == 0
                                            ? newTotalAndTotalPointsAndIsCorrected.getA()
                                            : Double.valueOf(value.trim());
                                    leaderboardEntryDTO.netPoints = newTotalAndTotalPointsAndIsCorrected.getB();
                                    leaderboardEntryDTO.totalPointsCorrected = newTotalAndTotalPointsAndIsCorrected
                                            .getC();
                                    getCell().setViewData(row, null); // ensure that getValue() is called again
                                    whiteboard.setObjectWithWhichToUpdateRow(row);
                                }
                            });
                }
            };
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            LeaderboardEntryDTO leaderboardEntryDTO = object.fieldsByRaceColumnName.get(raceColumnName);
            String result = "-";
            if (leaderboardEntryDTO != null && leaderboardEntryDTO.totalPoints != null) {
                result = scoreFormat.format(leaderboardEntryDTO.totalPoints);
            }
            return result;
        }
    }
    
    private class MaxPointsReasonAndTotalPointsEditButtonCell
            extends AbstractRowUpdateWhiteboardProducerThatHasCell<LeaderboardRowDTO, String> {
        private final String raceColumnName;
        private final StringMessages stringMessages;
        private final MaxPointsDropDownCellProvider maxPointsDropDownCellProvider;
        private final TotalPointsEditCellProvider totalPointsEditCellProvider;

        /**
         * @param maxPointsDropDownCellProvider
         *            will have its cell's view data reset after a successful update to force call to getValue
         * @param totalPointsEditCellProvider
         *            will have its cell's view data reset after a successful update to force call to getValue
         */
        public MaxPointsReasonAndTotalPointsEditButtonCell(StringMessages stringMessages, String raceColumnName,
                MaxPointsDropDownCellProvider maxPointsDropDownCellProvider,
                TotalPointsEditCellProvider totalPointsEditCellProvider) {
            this.stringMessages = stringMessages;
            this.raceColumnName = raceColumnName;
            this.maxPointsDropDownCellProvider = maxPointsDropDownCellProvider;
            this.totalPointsEditCellProvider = totalPointsEditCellProvider;
        }

        @Override
        public ButtonCell getCell() {
            return new EditButtonCell(stringMessages.edit());
        }

        @Override
        public FieldUpdater<LeaderboardRowDTO, String> getFieldUpdater() {
            return new FieldUpdater<LeaderboardRowDTO, String>() {
                @Override
                public void update(int index, final LeaderboardRowDTO row, final String value) {
                    final RowUpdateWhiteboard<LeaderboardRowDTO> whiteboard = new RowUpdateWhiteboard<LeaderboardRowDTO>(
                            EditableLeaderboardPanel.this.getData());
                    getWhiteboardOwner().whiteboardProduced(whiteboard);
                    new EditScoreDialog(stringMessages, row.competitor.getName(), raceColumnName,
                            row.fieldsByRaceColumnName.get(raceColumnName).reasonForMaxPoints,
                            row.fieldsByRaceColumnName.get(raceColumnName).totalPoints,
                            row.fieldsByRaceColumnName.get(raceColumnName).incrementalScoreCorrectionInPoints,
                            new DialogCallback<ScoreCorrectionUpdate>() {
                                @Override
                                public void ok(final ScoreCorrectionUpdate editedObject) {
                                    addBusyTask();
                                    getSailingService().updateLeaderboardScoreCorrection(getLeaderboardName(),
                                            row.competitor.getIdAsString(), raceColumnName, editedObject.getCorrectedScore(),
                                            getLeaderboardDisplayDate(),
                                            new AsyncCallback<Util.Triple<Double, Double, Boolean>>() {
                                                @Override
                                                public void onFailure(Throwable t) {
                                                    removeBusyTask();
                                                    getErrorReporter()
                                                            .reportError(stringMessages.errorUpdatingLeaderboardScore(
                                                                    row.competitor.getName(), getLeaderboardName(),
                                                                    raceColumnName, t.getMessage()));
                                                }

                                                @Override
                                                public void onSuccess(Util.Triple<Double, Double, Boolean> newTotalAndNetPointsAndIsCorrected) {
                                                    getSailingService().updateLeaderboardIncrementalScoreCorrection(getLeaderboardName(),
                                                            row.competitor.getIdAsString(), raceColumnName, editedObject.getIncrementalScoreCorrectionInPoints(),
                                                            getLeaderboardDisplayDate(),
                                                            new AsyncCallback<Util.Triple<Double, Double, Boolean>>() {
                                                                @Override
                                                                public void onFailure(Throwable t) {
                                                                    removeBusyTask();
                                                                    getErrorReporter().reportError(stringMessages
                                                                            .errorUpdatingLeaderboardScore(
                                                                                    row.competitor.getName(),
                                                                                    getLeaderboardName(),
                                                                                    raceColumnName, t.getMessage()));
                                                                }
                                                                
                                                                @Override
                                                                public void onSuccess(Util.Triple<Double, Double, Boolean> newTotalAndNetPointsAndIsCorrected) {
                                                                    getSailingService().updateLeaderboardMaxPointsReason(
                                                                            getLeaderboardName(), row.competitor.getIdAsString(),
                                                                            raceColumnName, editedObject.getMaxPointsReason(),
                                                                            getLeaderboardDisplayDate(),
                                                                            new AsyncCallback<Util.Triple<Double, Double, Boolean>>() {
                                                                                @Override
                                                                                public void onFailure(Throwable t) {
                                                                                    removeBusyTask();
                                                                                    getErrorReporter().reportError(stringMessages
                                                                                            .errorUpdatingLeaderboardScore(
                                                                                                    row.competitor.getName(),
                                                                                                    getLeaderboardName(),
                                                                                                    raceColumnName, t.getMessage()));
                                                                                }
                
                                                                                @Override
                                                                                public void onSuccess(
                                                                                        Util.Triple<Double, Double, Boolean> newTotalAndNetPointsAndIsCorrected) {
                                                                                    removeBusyTask();
                                                                                    final LeaderboardEntryDTO leaderboardEntryDTO = row.fieldsByRaceColumnName
                                                                                            .get(raceColumnName);
                                                                                    leaderboardEntryDTO.reasonForMaxPoints = editedObject
                                                                                            .getMaxPointsReason();
                                                                                    leaderboardEntryDTO.totalPoints = newTotalAndNetPointsAndIsCorrected
                                                                                            .getA();
                                                                                    leaderboardEntryDTO.netPoints = newTotalAndNetPointsAndIsCorrected
                                                                                            .getB();
                                                                                    leaderboardEntryDTO.totalPointsCorrected = newTotalAndNetPointsAndIsCorrected
                                                                                            .getC();
                                                                                    leaderboardEntryDTO.incrementalScoreCorrectionInPoints = editedObject
                                                                                            .getIncrementalScoreCorrectionInPoints();
                                                                                    maxPointsDropDownCellProvider.getCell()
                                                                                            .setViewData(row, null);
                                                                                    totalPointsEditCellProvider.getCell()
                                                                                            .setViewData(row, null);
                                                                                    whiteboard.setObjectWithWhichToUpdateRow(row);
                                                                                }
                                                                            });
                                                                }
                                                });
                                            }
                                    });
                                }

                                @Override
                                public void cancel() {
                                }
                            }).show();
                }
            };
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            return stringMessages.edit();
        }
    }

    public EditableLeaderboardPanel(ComponentContext<EditableLeaderboardSettings> context,
            final SailingServiceWriteAsync sailingServiceWrite, AsyncActionsExecutor asyncActionsExecutor,
            String leaderboardName, String leaderboardGroupName, final ErrorReporter errorReporter,
            final StringMessages stringMessages, UserAgentDetails userAgent, Iterable<DetailType> availableDetailTypes, 
            EditableLeaderboardSettings settings, WithSecurity sailingCF, UserService userService) {
        super(null, context, sailingServiceWrite, asyncActionsExecutor, settings,
                new CompetitorSelectionModel(/* hasMultiSelection */true),
                leaderboardName, errorReporter, stringMessages, /* showRaceDetails */ true, new ClassicLeaderboardStyle(),
                FlagImageResolverImpl.get(), availableDetailTypes, sailingCF);
        initialize(settings);
        this.showCarryColumn = settings.getShowCarryColumn();
        this.sailingServiceWrite = sailingServiceWrite;
        this.setStyleName("editableLeaderboardPanel");
        getContentPanel().addStyleName("editableLeaderboardContentPanel");
        if (DeviceDetector.isDesktop()) {
            getContentPanel().addStyleName("desktop");
        } else {
            getContentPanel().addStyleName("mobile");
            getLeaderboardTable().setWidth("auto");
        }
        getLeaderboardTable().setStyleName("editableLeaderboardTable");
        suppressedCompetitorsShown = new ListDataProvider<>(new ArrayList<>());
        suppressedCompetitorsTable = createSuppressedCompetitorsTable();
        final ImageResource importIcon = resources.importIcon();
        final Anchor importAnchor = new Anchor(AbstractImagePrototype.create(importIcon).getSafeHtml());
        getRefreshAndSettingsPanel().insert(importAnchor, 0);
        importAnchor.setTitle(stringMessages.importOfficialResults());
        importAnchor.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                sailingServiceWrite.getScoreCorrectionProviderNames(new AsyncCallback<Iterable<String>>() {
                    @Override
                    public void onSuccess(Iterable<String> providerNames) {
                        ResultSelectionAndApplyDialog dialog = new ResultSelectionAndApplyDialog(
                                EditableLeaderboardPanel.this, providerNames, getSailingService(), stringMessages,
                                getErrorReporter(), userService);
                        dialog.show();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter
                                .reportError(stringMessages.errorGettingScoreCorrectionProvider(caught.getMessage()));
                    }
                });
            }
        });
        final FlexTable scoreCorrectionInfoGrid = new FlexTable();
        scoreCorrectionInfoGrid.setCellPadding(3);
        scoreCorrectionInfoGrid.setWidget(0,  0, new Label(stringMessages.lastScoreCorrectionsTime() + ":"));
        lastScoreCorrectionTimeBox = new DateBox();
        scoreCorrectionInfoGrid.setWidget(0,  1, lastScoreCorrectionTimeBox);
        scoreCorrectionInfoGrid.setWidget(1,  0, new Label(stringMessages.lastScoreCorrectionsComment() + ":"));
        lastScoreCorrectionCommentBox = new TextBox();
        lastScoreCorrectionCommentBox.setWidth("100%");
        scoreCorrectionInfoGrid.setWidget(1,  1, lastScoreCorrectionCommentBox);
        scoreCorrectionInfoGrid.getFlexCellFormatter().setColSpan(1, 1, 2);
        final Button saveScoreCorrectionInfoBtn = new Button(stringMessages.save());
        scoreCorrectionInfoGrid.setWidget(1,  2, saveScoreCorrectionInfoBtn);
        saveScoreCorrectionInfoBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final String lastScoreCorrectionComment = lastScoreCorrectionCommentBox.getText();
                final Date lastScoreCorrectionTime = lastScoreCorrectionTimeBox.getValue();
                sailingServiceWrite.updateLeaderboardScoreCorrectionMetadata(getLeaderboardName(),
                        lastScoreCorrectionTime, lastScoreCorrectionComment, new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void noarg) {
                        updateScoreCorrectionInformation(lastScoreCorrectionComment, lastScoreCorrectionTime);
                        Notification.notify(stringMessages.successfullyUpdatedScores(), NotificationType.SUCCESS);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                                errorReporter.reportError(
                                        stringMessages.errorUpdatingLeaderboardScoreCorrection(caught.getMessage()));
                    }
                });
            }
        });
        final Button setScoreCorrectionDefaultTimeBtn = new Button(stringMessages.setTimeToNow());
        setScoreCorrectionDefaultTimeBtn.addStyleName("inlineButton");
        scoreCorrectionInfoGrid.setWidget(0, 2, setScoreCorrectionDefaultTimeBtn);
        showUncorrectedTotalPointsCheckbox = new CheckBox(stringMessages.showUncorrectedTotalPoints());
        showUncorrectedTotalPointsCheckbox.setTitle(stringMessages.showUncorrectedTotalPointsDescription());
        showUncorrectedTotalPointsCheckbox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                loadCompleteLeaderboard(/* showProgress */ true);
            }
        });
        if (DeviceDetector.isDesktop()) {
            scoreCorrectionInfoGrid.getFlexCellFormatter().setColSpan(0, 5, 2);
            scoreCorrectionInfoGrid.setWidget(0, 5, showUncorrectedTotalPointsCheckbox);
        } else {
            scoreCorrectionInfoGrid.getFlexCellFormatter().setColSpan(3, 1, 2);
            scoreCorrectionInfoGrid.setWidget(3, 1, showUncorrectedTotalPointsCheckbox);
        }
        setScoreCorrectionDefaultTimeBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                lastScoreCorrectionTimeBox.setValue(new Date());
            }
        });
        getContentPanel().insert(scoreCorrectionInfoGrid, 0);
        final Label suppressedCompetitorsLabel = new Label(stringMessages.suppressedCompetitors() + ":");
        suppressedCompetitorsLabel.addStyleName("suppressedCompetitorsLabel");
        getContentPanel().add(suppressedCompetitorsLabel);
        getContentPanel().add(suppressedCompetitorsTable);
        // add a dedicated settings button that allows users to remove columns if needed; the settings
        // button has disappeared as the LeaderboardPanel switched to the use of the more general Component
        // framework with its own handling of component and settings visibility
        final Anchor settingsAnchor = new Anchor(AbstractImagePrototype.create(getSettingsIcon()).getSafeHtml());
        settingsAnchor.setTitle(stringMessages.settings());
        settingsAnchor.addClickHandler(new SettingsClickHandler(stringMessages));
        getRefreshAndSettingsPanel().add(settingsAnchor);
        raceListBox = new ListBox();
        raceListBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                handleRaceChangeEvent(raceListBox);
            }
        });
        if (DeviceDetector.isDesktop()) {
            scoreCorrectionInfoGrid.setWidget(1, 4, new Label(stringMessages.raceSelection() + ":"));
            scoreCorrectionInfoGrid.setWidget(1, 5, raceListBox);
        } else {
            scoreCorrectionInfoGrid.getFlexCellFormatter().setColSpan(4, 1, 2);
            scoreCorrectionInfoGrid.setWidget(4, 0, new Label(stringMessages.raceSelection() + ":"));
            scoreCorrectionInfoGrid.setWidget(4, 1, raceListBox);
        }
        raceListSelection = Window.Location.getParameter("selectedRace");
    }
    
    /**
     * Updates the table based on the selection box of selected race.
     */
    private void updateRaceListSelectionBox() {
        List<String> items = currentSettings.getNamesOfRaceColumnsToShow();
        if (items != null) {
            // get map of currently visible race columns mapped to race name
            final Map<String, Column<LeaderboardRowDTO, ?>> columnMap = new HashMap<>();
            for (int i = 0; i < getLeaderboardTable().getColumnCount(); i++) {
                final Column<LeaderboardRowDTO, ?> currentColumn = getLeaderboardTable().getColumn(i);
                if (currentColumn instanceof LeaderboardPanel.RaceColumn) {
                    final RaceColumnDTO raceInLeaderboard = ((RaceColumn<?>) currentColumn).getRace();
                    if (raceInLeaderboard != null) {
                        columnMap.put(raceInLeaderboard.getName(), currentColumn);
                    }
                }
            }
            if (!columnMap.containsKey(raceListSelection)) {
                // if race name in raceListSelection is not available, set to showAll ('')
                raceListSelection = "";
            }
            // reset list box
            raceListBox.clear();
            raceListBox.addItem(stringMessages.showAll(), "");
            for (int i=0; i<items.size(); i++) {
                final String raceName = items.get(i);
                if (raceListSelection != null && raceListSelection.length() > 0
                        && !raceListSelection.equals(raceName)) {
                    // remove other columns from table if a specific race is selected in list box
                    removeColumn(columnMap.get(raceName));
                }
                raceListBox.addItem(raceName);
                if (raceListSelection != null && raceListSelection.equals(raceName)) {
                    // select option if raceListSelection is equals to raceName
                    raceListBox.setSelectedIndex(i + 1);
                }
            }
        }
    }
    
    /**
     * Handles the change of a race selection. Will set the raceListSelection by selection of list box 
     * and then trigger an update of settings which will reload the table with only selected column.
     */
    private void handleRaceChangeEvent(ListBox raceListBox) {
        raceListSelection = raceListBox.getSelectedValue();
        updateSettings(currentSettings);
    }

    private CellTable<CompetitorDTO> createSuppressedCompetitorsTable() {
        final Resources tableResources = GWT.create(AdminConsoleTableResources.class);
        CellTable<CompetitorDTO> result = new BaseCelltable<>(10000, tableResources);
        suppressedCompetitorsShown.addDataDisplay(result);
        final SuppressedShortInfoColumn suppressedSailIDColumn = new SuppressedShortInfoColumn();
        suppressedSailIDColumn.setSortable(true);
        result.addColumn(suppressedSailIDColumn, suppressedSailIDColumn.getHeader());
        final SuppressedCompetitorColumn suppressedCompetitorColumn = new SuppressedCompetitorColumn(
                new CompetitorColumnBase<CompetitorDTO>(this, stringMessages, new CompetitorFetcher<CompetitorDTO>() {
                    @Override
                    public CompetitorDTO getCompetitor(CompetitorDTO t) {
                        return t;
                    }
                }));
        result.addColumn(suppressedCompetitorColumn, suppressedCompetitorColumn.getHeader());
        suppressedCompetitorColumn.setSortable(true);
        final Column<CompetitorDTO, String> unsuppressButtonColumn = new Column<CompetitorDTO, String>(
                new SuppressButtonCell()) {
            @Override
            public String getValue(CompetitorDTO object) {
                return stringMessages.unsuppress();
            }
        };
        unsuppressButtonColumn.setFieldUpdater(new FieldUpdater<CompetitorDTO, String>() {
            @Override
            public void update(int index, final CompetitorDTO object, String value) {
                getSailingService().suppressCompetitorInLeaderboard(getLeaderboardName(), object.getIdAsString(),
                        /* suppressed */ false, new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                getErrorReporter()
                                        .reportError(stringMessages.errorTryingToUnsupressCompetitor(object.getName()));
                            }

                            @Override
                            public void onSuccess(Void result) {
                                Notification.notify("Successfully unsuppressed competitor " + object.getName(),
                                        NotificationType.INFO);
                                // force a reload of the entire editable leaderboard to hide the now suppressed
                                // competitor
                                loadCompleteLeaderboard(/* showProgress */ true);
                            }
                        });
            }
        });
        result.addColumn(unsuppressButtonColumn);
        final ListHandler<CompetitorDTO> sortHandler = new ListHandler<>(suppressedCompetitorsShown.getList());
        sortHandler.setComparator(suppressedSailIDColumn, suppressedSailIDColumn.getComparator());
        sortHandler.setComparator(suppressedCompetitorColumn, suppressedCompetitorColumn.getComparator());
        result.addColumnSortHandler(sortHandler);
        result.setStyleName("suppressedCompetitorsTable");
        return result;
    }

    private void updateScoreCorrectionInformation(String lastScoreCorrectionComment, Date lastScoreCorrectionTime) {
        getLeaderboard().setComment(lastScoreCorrectionComment);
        getLeaderboard().setTimePointOfLastCorrectionsValidity(lastScoreCorrectionTime);
        updateLeaderboard(getLeaderboard());
    }

    /**
     * Always ensures that there is a carry column displayed because in the editable version
     * of the leaderboard the carried / accumulated values must always be editable and therefore
     * the column must always be shown.
     */
    @Override
    protected int updateCarryColumn(LeaderboardDTO leaderboard, int zeroBasedIndexOfCarryColumn) {
        if (Boolean.TRUE.equals(currentSettings.getShowCarryColumn())) {
            ensureCarryColumn(zeroBasedIndexOfCarryColumn);
            return zeroBasedIndexOfCarryColumn + 1;
        } else {
            ensureNoCarryColumn(zeroBasedIndexOfCarryColumn);
        }
        return zeroBasedIndexOfCarryColumn;
    }

    @Override
    protected CarryColumn createCarryColumn() {
        return new EditableCarryColumn();
    }

    @Override
    protected CompetitorColumn createCompetitorColumn() {
        return new EditableCompetitorColumn(getCellListForEditableCompetitorColumn(),
                new CompetitorColumnBase<LeaderboardRowDTO>(this, stringMessages,
                        new CompetitorFetcher<LeaderboardRowDTO>() {
                    @Override
                    public CompetitorDTO getCompetitor(LeaderboardRowDTO t) {
                        return t.competitor;
                    }
                }));
    }

    private List<HasCell<LeaderboardRowDTO, ?>> getCellListForEditableCompetitorColumn() {
        List<HasCell<LeaderboardRowDTO, ?>> result = new ArrayList<HasCell<LeaderboardRowDTO, ?>>();
        final class OptionalBoldRenderer implements SafeHtmlRenderer<String> {
            private LeaderboardRowDTO currentRow;

            @Override
            public SafeHtml render(String object) {
                SafeHtmlBuilder builder = new SafeHtmlBuilder();
                render(object, builder);
                return builder.toSafeHtml();
            }

            private boolean isDisplayNameSet() {
                return currentRow != null && getLeaderboard().isDisplayNameSet(currentRow.competitor);
            }

            @Override
            public void render(String value, SafeHtmlBuilder builder) {
                if (isDisplayNameSet()) {
                    builder.appendHtmlConstant("<b>");
                }
                builder.appendEscaped(value);
                builder.appendHtmlConstant("&nbsp;");
                if (isDisplayNameSet()) {
                    builder.appendHtmlConstant("</b>");
                }
            }

            public void setCurrentRow(LeaderboardRowDTO currentRow) {
                this.currentRow = currentRow;
            }
        }
        final OptionalBoldRenderer renderer = new OptionalBoldRenderer();
        final EditTextCell cellForCompetitorName = new EditTextCell(renderer) {
            @Override
            public void render(Context context, String value, SafeHtmlBuilder sb) {
                renderer.setCurrentRow((LeaderboardRowDTO) context.getKey());
                super.render(context, value, sb);    
            }
        };
        result.add(new HasCell<LeaderboardRowDTO, String>() {
            @Override
            public EditTextCell getCell() {
                return cellForCompetitorName;
            }

            @Override
            public FieldUpdater<LeaderboardRowDTO, String> getFieldUpdater() {
                return createCompetitorColumnFieldUpdater(getCell());
            }

            @Override
            public String getValue(LeaderboardRowDTO row) {
                return getLeaderboard().getDisplayName(row.competitor);
            }
        });
        result.add(new HasCell<LeaderboardRowDTO, String>() {
            private final ButtonCell cell = new SuppressButtonCell();
            @Override
            public Cell<String> getCell() {
                return cell;
            }

            @Override
            public FieldUpdater<LeaderboardRowDTO, String> getFieldUpdater() {
                return new FieldUpdater<LeaderboardRowDTO, String>() {
                    @Override
                    public void update(int index, final LeaderboardRowDTO row, String value) {
                        getSailingService().suppressCompetitorInLeaderboard(getLeaderboardName(),
                                row.competitor.getIdAsString(), /* suppressed */ true, new AsyncCallback<Void>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        getErrorReporter().reportError("Error trying to suppress competitor "
                                                + row.competitor.getName() + " in leaderboard " + getLeaderboardName());
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        // force a reload of the entire editable leaderboard to hide the now suppressed
                                        // competitor
                                        loadCompleteLeaderboard(/* showProgress */ true);
                                    }
                                });
                    }
                };
            }

            @Override
            public String getValue(LeaderboardRowDTO object) {
                return stringMessages.suppress();
            }
        });
        result.add(new HasCell<LeaderboardRowDTO, String>() {
            private final ButtonCell cell = new EditButtonCell(stringMessages.edit());
            @Override
            public Cell<String> getCell() {
                return cell;
            }

            @Override
            public FieldUpdater<LeaderboardRowDTO, String> getFieldUpdater() {
                return new FieldUpdater<LeaderboardRowDTO, String>() {
                    @Override
                    public void update(int index, final LeaderboardRowDTO row, String valueToUpdate) {
                        new EditCompetitorNameDialog(stringMessages, row.competitor.getName(),
                                new DialogCallback<String>() {
                                    @Override
                                    public void ok(final String value) {
                                        getSailingService().updateCompetitorDisplayNameInLeaderboard(
                                            getLeaderboardName(), row.competitor.getIdAsString(),
                                            value == null || value.length() == 0 ? null : value.trim(),
                                            new AsyncCallback<Void>() {
                                                @Override
                                                public void onFailure(Throwable t) {
                                                    EditableLeaderboardPanel.this.getErrorReporter()
                                                            .reportError(stringMessages.errorUpdatingDisplayNames(
                                                                    row.competitor.getName(), getLeaderboardName(),
                                                                    t.getMessage()));
                                                }

                                                @Override
                                                public void onSuccess(Void v) {
                                                    if (getLeaderboard().competitorDisplayNames == null) {
                                                        getLeaderboard().competitorDisplayNames = new HashMap<>();
                                                    }
                                                    getLeaderboard().competitorDisplayNames.put(row.competitor,
                                                            value == null || value.trim().length() == 0 ? null
                                                                    : value.trim());
                                                    // ensure that getValue() is called again
                                                    cellForCompetitorName.setViewData(row, null); 
                                                    EditableLeaderboardPanel.this.getData().getList()
                                                            .set(EditableLeaderboardPanel.this.getData().getList()
                                                                    .indexOf(row), row);
                                                }
                                            });
                                    }

                                    @Override
                                    public void cancel() {
                                    }
                                }).show();
                    }
                };
            }

            @Override
            public String getValue(LeaderboardRowDTO object) {
                return stringMessages.edit();
            }
        });
        return result;
    }
    
    private List<HasCell<LeaderboardRowDTO, ?>> getCellListForEditableCarryColumn() {
        List<HasCell<LeaderboardRowDTO, ?>> result = new ArrayList<HasCell<LeaderboardRowDTO, ?>>();
        final EditTextCell carryTextCell = new EditTextCell();
        final FieldUpdater<LeaderboardRowDTO, String> fieldUpdater = new FieldUpdater<LeaderboardRowDTO, String>() {
            @Override
            public void update(final int rowIndex, final LeaderboardRowDTO row, final String value) {
                updateCarriedPoints(rowIndex, row,
                        value == null || value.length() == 0 ? null : Double.valueOf(value.trim()));
            }
        };
        result.add(new HasCell<LeaderboardRowDTO, String>() {
            @Override
            public Cell<String> getCell() {
                return carryTextCell;
            }

            @Override
            public FieldUpdater<LeaderboardRowDTO, String> getFieldUpdater() {
                return fieldUpdater;
            }

            @Override
            public String getValue(LeaderboardRowDTO object) {
                return object.carriedPoints == null ? "" : scoreFormat.format(object.carriedPoints) + " ";
            }
        });
        result.add(new HasCell<LeaderboardRowDTO, String>() {
            private final ButtonCell cell = new EditButtonCell(stringMessages.edit());
            @Override
            public Cell<String> getCell() {
                return cell;
            }

            @Override
            public FieldUpdater<LeaderboardRowDTO, String> getFieldUpdater() {
                return new FieldUpdater<LeaderboardRowDTO, String>() {
                    @Override
                    public void update(int index, final LeaderboardRowDTO row, String value) {
                        new EditCarryValueDialog(stringMessages, row.competitor.getName(), row.carriedPoints,
                                new DialogCallback<Double>() {
                                    @Override
                                    public void ok(final Double value) {
                                        updateCarriedPoints(
                                                EditableLeaderboardPanel.this.getData().getList().indexOf(row), row,
                                                value);
                                    }
                            @Override
                            public void cancel() {
                            }
                        }).show();
                    }
                };
            }

            @Override
            public String getValue(LeaderboardRowDTO object) {
                return stringMessages.edit();
            }
        });
        return result;
    }
    
    private void updateCarriedPoints(final int rowIndex, final LeaderboardRowDTO row, final Double value) {
        getSailingService().updateLeaderboardCarryValue(getLeaderboardName(), row.competitor.getIdAsString(), value,
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable t) {
                        EditableLeaderboardPanel.this.getErrorReporter()
                                .reportError(stringMessages.errorUpdatingCarriedPoints(row.competitor.getName(),
                                        getLeaderboardName(), t.getMessage()));
                    }

            @Override
            public void onSuccess(Void v) {
                row.carriedPoints = value;
                EditableLeaderboardPanel.this.getData().getList().set(rowIndex, row);
            }
        });
    }

    @Override
    protected RaceColumn<?> createRaceColumn(RaceColumnDTO race) {
        return new EditableRaceColumn(race, getRaceColumnCellList(race));
    }

    private List<RowUpdateWhiteboardProducerThatAlsoHasCell<LeaderboardRowDTO, ?>> getRaceColumnCellList(
            RaceColumnDTO race) {
        List<RowUpdateWhiteboardProducerThatAlsoHasCell<LeaderboardRowDTO, ?>> list = 
                new ArrayList<RowUpdateWhiteboardProducerThatAlsoHasCell<LeaderboardRowDTO, ?>>();
        final MaxPointsDropDownCellProvider maxPointsDropDownCellProvider = new MaxPointsDropDownCellProvider(
                race.getRaceColumnName());
        final TotalPointsEditCellProvider totalPointsEditCellProvider = new TotalPointsEditCellProvider(
                race.getRaceColumnName());
        list.add(totalPointsEditCellProvider);
        final ReasonForMaxPointsTextViewProvider maxPointsReasonViewProvider = new ReasonForMaxPointsTextViewProvider(
                race.getRaceColumnName());
        list.add(maxPointsReasonViewProvider);
        final IncrementalScoreCorrectionViewProvider incrementalScoreCorrectionViewProvider = new IncrementalScoreCorrectionViewProvider(
                race.getRaceColumnName());
        list.add(incrementalScoreCorrectionViewProvider);
        final UncorrectedTotalPointsViewProvider uncorrectedViewProvider = new UncorrectedTotalPointsViewProvider(
                race.getRaceColumnName());
        list.add(uncorrectedViewProvider);
        list.add(new MaxPointsReasonAndTotalPointsEditButtonCell(stringMessages, race.getRaceColumnName(),
                maxPointsDropDownCellProvider, totalPointsEditCellProvider));
        return list;
    }
    
    /**
     * Computing the uncorrected scores can be expensive for large leaderboards; disable by default but allow user to
     * enable
     */
    @Override
    protected boolean isFillTotalPointsUncorrected() {
        return showUncorrectedTotalPointsCheckbox==null?false:showUncorrectedTotalPointsCheckbox.getValue();
    }

    @Override
    public void updateLeaderboard(LeaderboardDTO leaderboard) {
        super.updateLeaderboard(leaderboard);
        if (leaderboard != null) {
            String lastScoreCorrectionComment = leaderboard.getComment();
            Date lastScoreCorrectionTime = leaderboard.getTimePointOfLastCorrectionsValidity();
            lastScoreCorrectionCommentBox.setText(lastScoreCorrectionComment != null ? lastScoreCorrectionComment : "");
            lastScoreCorrectionTimeBox.setValue(lastScoreCorrectionTime != null ? lastScoreCorrectionTime : new Date());
            suppressedCompetitorsShown.getList().clear();
            for (CompetitorDTO suppressedCompetitor : leaderboard.getSuppressedCompetitors()) {
                suppressedCompetitorsShown.getList().add(suppressedCompetitor);
            }
            updateRaceListSelectionBox();
        }
    }

    /**
     * When editing, scrolling is not really an issue because usually the editable version is not used in live mode. But
     * when clicking the "Edit" button in a previously de-selected row, we still want the score to get the selection and
     * the focus. So turn off any focus/blur magic for this subclass.
     */
    @Override
    protected void blurFocusedElementAfterSelectionChange() {
    }   

    public SailingServiceWriteAsync getSailingService() {
        return sailingServiceWrite;
    }
    
    /**
     * Special edit button with edit icon and title to save space in data table.
     */
    final class EditButtonCell extends ButtonCell {
        
        private final String title;
        
        public EditButtonCell(String title) {
            super(SimpleSafeHtmlRenderer.getInstance());
            this.title = title;
        }
        
        @Override
        public void render(Context context, SafeHtml data, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<button class=\"editButton\" type=\"button\" tabindex=\"-1\" title=\"");
            sb.appendEscaped(title);
            sb.appendHtmlConstant("\"><span>");
            if (data != null) {
              sb.append(data);
            }
            sb.appendHtmlConstant("</span></button>");
        }
        
    }
    
    /**
     * Special edit button with edit icon and title to save space in data table.
     */
    final class SuppressButtonCell extends ButtonCell {
        
        public SuppressButtonCell() {
            super(SimpleSafeHtmlRenderer.getInstance());
        }
        
        @Override
        public void render(Context context, SafeHtml data, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<button class=\"suppressButton\" type=\"button\" tabindex=\"-1\">");
            if (data != null) {
              sb.append(data);
            }
            sb.appendHtmlConstant("</button>");
        }
        
    }

    protected void initialize(EditableLeaderboardSettings settings) {
        setDefaultRaceColumnSelection(settings);

        if (timer.isInitialized()) {
            loadCompleteLeaderboard(/* showProgress */ false);
        }
        updateSettings(settings);
        style.afterConstructorHook(this);
    }

    @Override
    public EditableLeaderboardSettings getSettings() {
        Iterable<RaceColumnDTO> selectedRaceColumns = raceColumnSelection
                .getSelectedRaceColumnsOrderedAsInLeaderboard(leaderboard);
        List<String> namesOfRaceColumnsToShow = new ArrayList<>();
        for (RaceColumnDTO raceColumn : selectedRaceColumns) {
            namesOfRaceColumnsToShow.add(raceColumn.getName());
        }
        final EditableLeaderboardSettings leaderboardSettings = new EditableLeaderboardSettings(selectedManeuverDetails,
                selectedLegDetails, selectedRaceDetails, selectedOverallDetailColumns, namesOfRaceColumnsToShow,
                raceColumnSelection.getNumberOfLastRaceColumnsToShow(), timer.getRefreshInterval(),
                raceColumnSelection.getType(), isShowAddedScores(), isShowCompetitorShortName(),
                isShowCompetitorFullName(), isShowCompetitorBoatInfo(), isShowCompetitorNationality, showCarryColumn, 
                new SecurityChildSettingsContext(leaderboard, paywallResolver));
        return leaderboardSettings;
    }

    @Override
    protected void setDefaultRaceColumnSelection(EditableLeaderboardSettings settings) {
        showCarryColumn = settings.getShowCarryColumn();
        switch (settings.getActiveRaceColumnSelectionStrategy()) {
        case EXPLICIT:
            raceColumnSelection = new ExplicitRaceColumnSelection();
            break;
        case LAST_N:
            setRaceColumnSelectionToLastNStrategy(settings.getNumberOfLastRacesToShow());
            break;
        }
    }

    @Override
    protected boolean canShowCompetitorBoatInfo() {
        return !this.leaderboard.canBoatsOfCompetitorsChangePerRace && !leaderboard.type.isMetaLeaderboard();
    }

    @Override
    public String getCompetitorColor(CompetitorDTO competitor) {
        return null; // Not used in multi-race leaderboard
    }

    @Override
    public int getLegCount(LeaderboardDTO leaderboardDTO, String raceColumnName) {
        return leaderboardDTO.getLegCount(raceColumnName, null);
    }

    @Override
    protected EditableLeaderboardSettings overrideDefaultsForNamesOfRaceColumns(
            EditableLeaderboardSettings currentSettings, LeaderboardDTO result) {
        return currentSettings.withNamesOfRaceColumnsToShowDefaults(result.getNamesOfRaceColumns(),
                new SecurityChildSettingsContext(leaderboard, paywallResolver));
    }

    @Override
    protected void applyTop30FilterIfCompetitorSizeGreaterEqual40(LeaderboardDTO leaderboard) {
        int maxRaceRank = 30;
        if (leaderboard.competitors.size() >= 40) {
            CompetitorRaceRankFilter raceRankFilter = new CompetitorRaceRankFilter();
            raceRankFilter.setLeaderboardFetcher(this);
            raceRankFilter.setQuickRankProvider(this.competitorFilterPanel.getQuickRankProvider());
            raceRankFilter.setOperator(new BinaryOperator<Integer>(BinaryOperator.Operators.LessThanEquals));
            raceRankFilter.setValue(maxRaceRank);
            FilterSet<CompetitorDTO, Filter<CompetitorDTO>> activeFilterSet = competitorSelectionProvider
                    .getOrCreateCompetitorsFilterSet(stringMessages.topNCompetitorsByRaceRank(maxRaceRank));
            activeFilterSet.addFilter(raceRankFilter);
            competitorSelectionProvider.setCompetitorsFilterSet(activeFilterSet);
        }
    }

    /**
     * Extracts the rows to display of the <code>leaderboard</code>. These are all {@link AbstractLeaderboardDTO#rows
     * rows} in case {@link #preSelectedRace} is <code>null</code>, or only the rows of the competitors who scored in
     * the race identified by {@link #preSelectedRace} otherwise.
     */
    @Override
    public Map<CompetitorDTO, LeaderboardRowDTO> getRowsToDisplay() {
        final Map<CompetitorDTO, LeaderboardRowDTO> result = new HashMap<>();
        Iterable<CompetitorDTO> allFilteredCompetitors = competitorSelectionProvider.getFilteredCompetitors();
        for (CompetitorDTO competitor : leaderboard.rows.keySet()) {
            if (Util.contains(allFilteredCompetitors, competitor)) {
                result.put(competitor, leaderboard.rows.get(competitor));
            }
        }
        return result;
    }

    @Override
    public SettingsDialogComponent<EditableLeaderboardSettings> getSettingsDialogComponent(
            EditableLeaderboardSettings useTheseSettings) {
        return new EditableLeaderboardSettingsDialogComponent(useTheseSettings, leaderboard.getNamesOfRaceColumns(),
                stringMessages, availableDetailTypes, canShowCompetitorBoatInfo(), paywallResolver, leaderboard);
    }

    @Override
    protected void openSettingsDialog() {
        SettingsDialog<EditableLeaderboardSettings> settingsDialog = new SettingsDialog<EditableLeaderboardSettings>(
                this, stringMessages);
        settingsDialog.ensureDebugId("LeaderboardSettingsDialog");
        settingsDialog.show();
    }

    @Override
    protected void applyRaceSelection(final LeaderboardSettings ns) {
        EditableLeaderboardSettings newSettings = (EditableLeaderboardSettings) ns;
        showCarryColumn = newSettings.getShowCarryColumn();
        Iterable<String> oldNamesOfRaceColumnsToShow = null;
        if (newSettings.getNamesOfRaceColumnsToShow() == null) {
            oldNamesOfRaceColumnsToShow = raceColumnSelection.getSelectedRaceColumnNames();
        }
        switch (newSettings.getActiveRaceColumnSelectionStrategy()) {
        case EXPLICIT:
            setDefaultRaceColumnSelection(newSettings);
            if (newSettings.getNamesOfRaceColumnsToShow() != null) {
                raceColumnSelection.requestClear();
                for (String nameOfRaceColumnToShow : newSettings.getNamesOfRaceColumnsToShow()) {
                    RaceColumnDTO raceColumnToShow = getRaceByColumnName(nameOfRaceColumnToShow);
                    if (raceColumnToShow != null) {
                        raceColumnSelection.requestRaceColumnSelection(raceColumnToShow);
                    }
                }
            } else {
                // apply the old column selections again
                for (String oldNameOfRaceColumnToShow : oldNamesOfRaceColumnsToShow) {
                    final RaceColumnDTO raceColumnByName = getLeaderboard()
                            .getRaceColumnByName(oldNameOfRaceColumnToShow);
                    if (raceColumnByName != null) {
                        raceColumnSelection.requestRaceColumnSelection(raceColumnByName);
                    }
                }
            }
            break;
        case LAST_N:
            setRaceColumnSelectionToLastNStrategy(newSettings.getNumberOfLastRacesToShow());
            break;
        }
    }

    private RaceColumnDTO getRaceByColumnName(String columnName) {
        if (getLeaderboard() != null) {
            for (RaceColumnDTO race : getLeaderboard().getRaceList()) {
                if (columnName.equals(race.getRaceColumnName())) {
                    return race;
                }
            }
        }
        return null;
    }

    @Override
    protected void processAutoExpands(AbstractSortableColumnWithMinMax<?, ?> c, RaceColumn<?> lastRaceColumn) {
        // Toggle or the last race column if that was requested
        if (isAutoExpandLastRaceColumn() && c == lastRaceColumn) {
            ExpandableSortableColumn<?> expandableSortableColumn = (ExpandableSortableColumn<?>) c;
            if (!expandableSortableColumn.isExpanded()) {
                expandableSortableColumn.changeExpansionState(/* expand */ true);
                autoExpandPerformedOnce = true;
            }
        }
    }

    @Override
    protected void updateLeaderboardAndRun(Runnable callWhenExpansionDataIsLoaded) {
        final LeaderboardDTO previousLeaderboard = getLeaderboard();
        getSailingService().getLeaderboardByName(getLeaderboardName(),
                timer.getPlayMode() == PlayModes.Live ? null : getLeaderboardDisplayDate(),
                /* namesOfRacesForWhichToLoadLegDetails */getNamesOfExpandedRaceColumns(), shallAddOverallDetails(),
                previousLeaderboard.getId(), /* fillTotalPointsUncorrected */ false,
                new MarkedAsyncCallback<IncrementalOrFullLeaderboardDTO>(
                        new AsyncCallback<IncrementalOrFullLeaderboardDTO>() {
                            @Override
                            public void onSuccess(IncrementalOrFullLeaderboardDTO result) {
                                updateLeaderboard(result.getLeaderboardDTO(previousLeaderboard));
                                callWhenExpansionDataIsLoaded.run();
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                getErrorReporter().reportError(
                                        stringMessages.errorTryingToObtainLeaderboardContents(caught.getMessage()),
                                        true /* silentMode */);
                            }
                        }));
    }

    @Override
    protected AsyncAction<LeaderboardDTO> getRetrieverAction() {
        final Date date = getLeaderboardDisplayDate();
        // need to call super here otherwise it might take the instantiated types method implementation, 
        // but fields there might not be initialized if we are within a constructor call.
        SailingServiceAsync sailingService = super.getSailingService(); 
        return new GetLeaderboardByNameAction(sailingService, getLeaderboardName(),
                useNullAsTimePoint() ? null : date,
                /* namesOfRaceColumnsForWhichToLoadLegDetails */getNamesOfExpandedRaceColumns(),
                shallAddOverallDetails(), /* previousLeaderboard */
                getLeaderboard(), isFillTotalPointsUncorrected(), timer, getErrorReporter(), stringMessages);
    }
}
