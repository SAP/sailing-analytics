package com.sap.sailing.gwt.ui.leaderboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.SortingOrder;
import com.sap.sse.gwt.client.celltable.AbstractSortableColumnWithMinMax;

/**
 * A column that is sortable and offers an expand/collapse button in its column header.
 *  
 * @author Axel Uhl (D043530)
 *
 */
public abstract class ExpandableSortableColumn<C> extends LeaderboardSortableColumnWithMinMax<LeaderboardRowDTO, C> {
    private boolean enableExpansion;
    
    /**
     * Set when the {@link #changeExpansionState(boolean)} method begins with its work. Three things can happen:
     * <ol>
     * <li>the column is to be collapsed. This can happen synchronously, and the flag is cleared again before the method
     * returns</li>
     * <li>the column is to be expanded and all information required for this is already available; again, this type
     * of request can be handled synchronously, and the flag is cleared before the method returns</li>
     * <li>the column is to be expanded but not all information required is available yet; it has to be requested using
     * an asynchronous call; the method returns with the flag still set, and the callback will clear it when done</li>
     * </ol>
     */
    private boolean togglingInProcess;
    
    /**
     * If {@link #togglingInProcess} is {@code true} and a call to {@link #changeExpansionState(boolean)} is received,
     * instead of running the method, the request is "queued" only by appending the desired expansion state. When the
     * asynchronous task completes the running toggle action, the first request is picked from the list and
     * {@link #changeExpansionState(boolean)} is called. This way, overlapping asynchronous requests triggered by calls
     * to {@link #changeExpansionState(boolean)} are avoided.
     */
    private final LinkedList<Boolean> queuedToggleRequests;
    
    private boolean suppressSortingOnce;
    private final LeaderboardPanel<?> leaderboardPanel;
    private final Map<DetailType, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> detailColumnsMap;
    private final List<DetailType> detailSelection;
    
    /**
     * Holds the child columns that represent expanded information for this column. If <code>null</code>,
     * the expanded set of columns hasn't been requested yet or cannot be computed because this column
     * is not {@link #isExpansionEnabled() expandable}. Once computed, the child columns remain in this
     * collection and are dynamically inserted to and removed from the {@link CellTable} to the right
     * of this column.
     */
    protected List<AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> directChildren;
    
    /**
     * Tells if this race column is currently displayed in expanded form which includes a visualization
     * of the race's legs.
     */
    private boolean expanded;

    public ExpandableSortableColumn(LeaderboardPanel<?> leaderboardPanel, boolean enableExpansion, Cell<C> cell,
            SortingOrder preferredSortingOrder, StringMessages stringConstants, String detailHeaderStyle, String detailColumnStyle,
            List<DetailType> detailSelection, DisplayedLeaderboardRowsProvider displayedLeaderboardRowsProvider) {
        super(cell, preferredSortingOrder, displayedLeaderboardRowsProvider);
        this.queuedToggleRequests = new LinkedList<>();
        this.enableExpansion = enableExpansion;
        this.leaderboardPanel = leaderboardPanel;
        this.detailSelection = detailSelection;
        this.detailColumnsMap = getDetailColumnMap(leaderboardPanel, stringConstants, detailHeaderStyle, detailColumnStyle);
    }
    
    /**
     * By default, an expandable sortable column has no detail columns. Subclasses that want to offer detail columns must
     * override this method.
     */
    protected Map<DetailType, AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> getDetailColumnMap(
            LeaderboardPanel<?> leaderboardPanel, StringMessages stringConstants, String detailHeaderStyle,
            String detailColumnStyle) {
        return Collections.emptyMap();
    }

    protected LeaderboardPanel<?> getLeaderboardPanel() {
        return leaderboardPanel;
    }
    
    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void suppressSortingOnce() {
        suppressSortingOnce = true;
    }
    
    /**
     * Fetches the cached {@link #directChildren}. Usually, no children are to be returned if the column is not
     * {@link #isExpanded() expanded}. If <code>null</code>, the child columns are determined by calling
     * {@link #createExpansionColumns} and cached in {@link #directChildren}. RaceName of Columns like
     * ManeuverCountRaceColumn is not known at this point because it will be later set in the constructor of
     * TextRaceColumn
     * 
     * @param filterByPermissions
     *            if {@code true}, returns only columns for {@link DetailType}s for which the user has permissions to
     *            view
     */
    protected Iterable<AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> getDirectChildren() {
        List<AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> result;
        if (isExpanded()) {
            result = new ArrayList<AbstractSortableColumnWithMinMax<LeaderboardRowDTO,?>>();
            for (DetailType detailColumnType : detailSelection) {
                AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> selectedColumn = detailColumnsMap.get(detailColumnType);
                if (selectedColumn != null) {
                    result.add(selectedColumn);
                }
            }
        } else {
            result = Collections.emptyList();
        }
        return result;
    }
    
    protected AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> createExpansionColumn(DetailType detailColumnType) {
        throw new RuntimeException("Detail column type "+detailColumnType+" not supported by column of type "+getClass().getName());
    }
    
    /**
     * @return the list of details supported by {@link #createExpansionColumn}
     */
    protected List<DetailType> getSupportedDetails() {
        return Collections.emptyList();
    }

    /**
     * Determines the direct and transitive child columns that due to the current expansion state should be visible.
     * Note that for columns not currently visible or currently being expanded (see
     * {@link #changeExpansionState(boolean)}), the column collection returned does not necessarily contain only columns
     * really part of the {@link CellTable} used to display this column.
     */
    private Collection<AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> getAllVisibleChildren() {
        List<AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?>> transitiveChildren = new ArrayList<>();
        if (isExpanded()) {
            for (AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> childColumn : getDirectChildren()) {
                transitiveChildren.add(childColumn);
                if (childColumn instanceof ExpandableSortableColumn<?>) {
                    @SuppressWarnings("unchecked")
                    ExpandableSortableColumn<C> expandableChild = (ExpandableSortableColumn<C>) childColumn;
                    transitiveChildren.addAll(expandableChild.getAllVisibleChildren());
                }
            }
        }
        return transitiveChildren;
    }
    
    /**
     * Asks this column to add / remove the columns used for its expanded view to its right in the leaderboard panel's
     * table. For non-expandable columns, this default implementation simply does nothing. For expandable columns, the
     * {@link #getAllChildred child columns} are obtained and added to the right of this column.
     * <p>
     * 
     * Precondition: this column must currently be contained in the {@link CellTable} showing the
     * {@link #leaderboardPanel leaderboard}.
     * <p>
     * 
     * When the method starts to toggle the expansion state, it sets the {@link #togglingInProcess} flag. When the
     * method returns the {@link #isExpanded()} method yields the new expansion state. However, when the column contents
     * need to be loaded asynchronously first, the expansion will not have affected the table structure yet and is still
     * "pending." This state is indicated by the {@link #isTogglingInProcess()} method returning {@code true} after this
     * method has returned. Only after the asynchronous data loading has happened and the table has actually been
     * updated to reflect the expanded state, the {@link #isTogglingInProcess()} will then again return {@code false}.
     * 
     * @param expand
     *            if {@code true} the column will be expanded if not already expanded, if {@code false} the column will
     *            be collapsed if not already collapsed
     */
    public void changeExpansionState(boolean expand) {
        if (isExpansionEnabled()) {
            if (isTogglingInProcess()) {
                queuedToggleRequests.add(expand);
            } else {
                final CellTable<LeaderboardRowDTO> table = getLeaderboardPanel().getLeaderboardTable();
                if (table == null) {
                    GWT.log("WARNING: leaderboard table is null");
                } else {
                    getLeaderboardPanel().addBusyTask();
                    setTogglingInProcess(true);
                    if (!expand) { // collapse
                        if (isExpanded()) { // but only if currently expanded
                            // don't filter by permissions; if we need to collapse columns after the user lost permissions or was logged out,
                            // columns currently still shown wouldn't be returned from getAllVisibleChildren otherwise
                            for (AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> column : getAllVisibleChildren()) {
                                getLeaderboardPanel().removeColumn(column); // removes only the children currently displayed
                            }
                            // important: toggle expanded state after asking for all visible children
                            setExpanded(!isExpanded());
                        }
                        finishCurrentToggling();
                    } else { // expand
                        if (!isExpanded()) { // but only if not expanded yet
                            // important: toggle expanded state before asking for all visible children
                            setExpanded(!isExpanded());
                            ensureExpansionDataIsLoaded(new Runnable() {
                                public void run() {
                                    // column may have been collapsed meanwhile, so that the columns must not be added to the table right now
                                    if (isExpanded()) {
                                        int insertIndex = table.getColumnIndex(ExpandableSortableColumn.this);
                                        // The check "insertIndex != -1" is necessary, because the child-columns might be deleted
                                        // asynchronously while toggling the columns.
                                        if (insertIndex != -1) {
                                            insertIndex++;
                                            // for expanding we filter by permissions in order to only show what the user is allowed to see
                                            for (AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> column : getAllVisibleChildren()) {
                                                column.updateMinMax();
                                                if (table.getColumnIndex(column) < 0) {
                                                    getLeaderboardPanel().insertColumn(insertIndex++, column);
                                                }
                                            }
                                            getLeaderboardPanel().getLeaderboardTable().redraw();
                                        }
                                    }
                                    finishCurrentToggling();
                                }
                            });
                        } else {
                            finishCurrentToggling();
                        }
                    }
                }
            }
        }
    }
    /**
     * Finishes the current toggling by removing the
     * {@link ClassicLeaderboardPanel#removeBusyTask() busy task} from the {@link #getLeaderboardPanel() leaderboard panel} and setting the
     * {@link #setTogglingInProcess(boolean) togglingInProcess flag} to <code>false</code>.
     * Afterwards, {@link #queuedToggleRequests queued toggling requests} are executed, if any.
     * Make sure that this method is called only once, and only after {@link ClassicLeaderboardPanel#addBusyTask()} has been called.
     */
    private void finishCurrentToggling() {
        getLeaderboardPanel().removeBusyTask();
        setTogglingInProcess(false);
        if (!queuedToggleRequests.isEmpty()) {
            changeExpansionState(queuedToggleRequests.remove(0));
        }
    }
    
    /**
     * Called to ensure that all data necessary to display the expanded data of this column is actually loaded.
     * If this is not yet the case, an asynchronous call to the server may be required that subclasses have to
     * implement. When the data has successfully been loaded, <code>callWhenExpansionDataIsLoaded</code>'s
     * {@link Runnable#run() run} method must be called to create.<p>
     * 
     * This default implementation assumes that all data necessary is already loaded and therefore immediately
     * calls <code>callWhenExpansionDataIsLoaded.run()</code>.
     */
    protected void ensureExpansionDataIsLoaded(Runnable callWhenExpansionDataIsLoaded) {
        callWhenExpansionDataIsLoaded.run();
    }

    @Override
    public boolean isSortable() {
        boolean result;
        if (suppressSortingOnce) {
            result = false;
            suppressSortingOnce = false;
        } else {
            result = super.isSortable();
        }
        return result;
    }

    public boolean isExpansionEnabled() {
        return enableExpansion;
    }
    
    public boolean isTogglingInProcess() {
        return togglingInProcess;
    }
    
    private void setTogglingInProcess(boolean togglingInProcess) {
        this.togglingInProcess = togglingInProcess;
    }

    public void setEnableExpansion(boolean enableExpansion) {
        this.enableExpansion = enableExpansion;
    }

    protected void defaultRender(Context context, LeaderboardRowDTO object, SafeHtmlBuilder html) {
        super.render(context, object, html);
    }
    
    @Override
    public abstract SortableExpandableColumnHeader getHeader();

}
