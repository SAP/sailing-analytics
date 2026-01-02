package com.sap.sse.gwt.client.celltable;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.DefaultSelectionEventManager.EventTranslator;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sse.common.InvertibleComparator;
import com.sap.sse.common.SortingOrder;
import com.sap.sse.common.impl.InvertibleComparatorAdapter;
import com.sap.sse.gwt.client.controls.BetterCheckboxCell;

/**
 * A column to be used in a {@link CellTable} that controls and reflects a table's selection model using stylable
 * "check boxes". To make things work, clients have to also call
 * {@link CellTable#setSelectionModel(com.google.gwt.view.client.SelectionModel, com.google.gwt.view.client.CellPreviewEvent.Handler)}
 * with the result of this columns {@link #getSelectionModel} as the first and the result of the
 * {@link #getSelectionManager()} method as second argument. This will ensure that the event handling and selection
 * updates work properly.
 * <p>
 * 
 * Clients should use the column's own {@link #getSelectionModel() RefreshableMultiSelectionModel}. This will ensure that
 * the {@link SelectionCheckboxColumn} will be refreshed correctly when the selection changes or the
 * {@link ListDataProvider} has new elements. Clients should also ensure that the {@link Flushable} and the
 * {@link ListDataProvider} are not <code>null</code>; otherwise the {@link RefreshableMultiSelectionModel selection model}
 * won�t work correctly. The {@link Flushable} interface is used to ensure that the selection state is displayed
 * correctly by {@link SelectionCheckboxColumn}. To ensure this, the {@link Flushable#flush()} method is called after
 * every selection state change.
 * <p>
 * The column uses the {@link BetterCheckboxCell} cell to implement the display properties. Three CSS styles can be used
 * to parameterize this column: one for the <code>&lt;td&gt;</code> element rendering the cell, and two for the
 * <code>&lt;div&gt;</code> element representing a selected or deselected element.
 * 
 * @author Axel Uhl (D043530)
 * @author Lukas Furmanek
 * 
 * @param <T>
 */
public class SelectionCheckboxColumn<T> extends AbstractSortableColumnWithMinMax<T, Boolean> {
    private final BetterCheckboxCell cell;
    private final String checkboxColumnCellCSSClass;
    private final EventTranslator<T> selectionEventTranslator;
    private final RefreshableMultiSelectionModel<T> selectionModel;
    private final ListDataProvider<T> listDataProvider;
    private final Flushable display;

    /**
     * @param selectedCheckboxCSSClass
     *            CSS class for the <code>&lt;div&gt;</code> element representing a selected element
     * @param deselectedCheckboxCSSClass
     *            CSS class for the <code>&lt;div&gt;</code> element representing a deselected element
     * @param checkboxColumnCellCSSClass
     *            CSS class for the <code>&lt;td&gt;</code> element rendering the cell
     * @param entityIdentityComparator
     *            {@link EntityIdentityComparator} to create a {@link RefreshableMultiSelectionModel}
     * @param listDataProvider
     *            {@link ListDataProvider} to create a {@link RefreshableMultiSelectionModel}
     * @param display
     *            {@link Flushable} to redraw the selected elements on the display
     */
    public SelectionCheckboxColumn(String selectedCheckboxCSSClass, String deselectedCheckboxCSSClass,
            String checkboxColumnCellCSSClass, EntityIdentityComparator<T> entityIdentityComparator,
            ListDataProvider<T> listDataProvider, Flushable display) {
        this(new BetterCheckboxCell(selectedCheckboxCSSClass, deselectedCheckboxCSSClass), checkboxColumnCellCSSClass,
                entityIdentityComparator, listDataProvider, display);
    }
    
    private SelectionCheckboxColumn(BetterCheckboxCell checkboxCell, String checkboxColumnCellCSSClass,
            EntityIdentityComparator<T> entityIdentityComparator, ListDataProvider<T> listDataProvider,
            Flushable display) {
        super(checkboxCell, SortingOrder.DESCENDING);
        this.display = display;
        this.listDataProvider = listDataProvider;
        this.cell = checkboxCell;
        this.checkboxColumnCellCSSClass = checkboxColumnCellCSSClass;
        this.selectionEventTranslator = createSelectionEventTranslator();
        this.selectionModel = createSelectionModel(entityIdentityComparator);
        this.setSortable(false);
    }
    
    public Header<Boolean> createHeader() {
        final CheckboxCell selectAllCell = new CheckboxCell();
        final Header<Boolean> selectAllHeader = new Header<Boolean>(selectAllCell) {
            @Override
            public Boolean getValue() {
                return false;
             }
        };
        selectAllHeader.setUpdater(value -> {
            onBeforeBulkSelection(value);
            try {
                for (final T item : listDataProvider.getList()) {
                    if (selectionModel != null) {
                        selectionModel.setSelected(item, value);
                    }
                    onAfterBulkSelectionItem(item, value);
                }
            } finally {
                onAfterBulkSelection(value);
            }
        });
        selectionModel.addSelectionChangeHandler(e -> {
            if (selectionModel.getSelectedSet().isEmpty()) {
                selectAllCell.setViewData(/* key */ selectAllHeader.getValue(), false);
            } else if (selectionModel.getSelectedSet().size() == listDataProvider.getList().size()) {
                selectAllCell.setViewData(/* key */ selectAllHeader.getValue(), true);
            }
        });
        return selectAllHeader;
    }
    
    protected void onBeforeBulkSelection(boolean value) {
        // default: no-op
    }
    
    protected void onAfterBulkSelectionItem(T item, boolean value) {
        // default: no-op
    }
    
    protected void onAfterBulkSelection(boolean value) {
        // default: no-op
    }
    
    /**
     * @return a selection manager that should be used for the table to which this column is added; use
     *         {@link CellTable#setSelectionModel(com.google.gwt.view.client.SelectionModel, com.google.gwt.view.client.CellPreviewEvent.Handler)}
     *         to set the selection manager together with the selection model on the table.
     */
    public CellPreviewEvent.Handler<T> getSelectionManager() {
        return DefaultSelectionEventManager.createCustomManager(getSelectionEventTranslator());
    }
    
    protected ListDataProvider<T> getListDataProvider() {
        return listDataProvider;
    }

    public RefreshableMultiSelectionModel<T> getSelectionModel() {
        return selectionModel;
    }

    /**
     * Clients should use the {@link RefreshableMultiSelectionModel} returned by this method for the {@link CellTable}
     * to which they add this column. If they do so, the {@link Flushable#flush()} method will be
     * triggered correctly for all selection changes. Otherwise, clients or subclasses are responsible to issue the
     * necessary calls to {@link Flushable#flush()} after selection changes.
     */
    private RefreshableMultiSelectionModel<T> createSelectionModel(final EntityIdentityComparator<T> entityIdentityComparator) {
        return new RefreshableMultiSelectionModel<T>(entityIdentityComparator, listDataProvider) {
            @Override
            public void clear() {
                super.clear();
                display.flush();
            }

            @Override
            public void setSelected(T item, boolean selected) {
                boolean wasSelected = isSelected(item);
                super.setSelected(item, selected);
                if (wasSelected != selected) {
                    display.flush();
                }
            }
        };
    }
    
    /**
     * @return a selection event translator that works nicely with
     *         {@link DefaultSelectionEventManager#createCustomManager(EventTranslator)} to ensure that this selection
     *         checkbox column does what it is supposed to do. Client may want to obtain the custom event manager
     *         directly by using {@link #getSelectionManager()}.
     */
    public EventTranslator<T> getSelectionEventTranslator() {
        return selectionEventTranslator;
    }

    /**
     * The checkbox value is determined by by selection state of <code>row</code>, using the
     * {@link #getSelectionModel() selection model}.
     */
    @Override
    public Boolean getValue(T row) {
        return getSelectionModel().isSelected(row);
    }

    /**
     * The default header display is a check mark. Subclasses may redefine this.
     */
    @Override
    public Header<?> getHeader() {
        return new SafeHtmlHeader(new SafeHtmlBuilder().appendEscaped("\u2713").toSafeHtml());
    }

    @Override
    public String getCellStyleNames(Context context, T object) {
        String basicStyles = super.getCellStyleNames(context, object);
        return basicStyles == null ? checkboxColumnCellCSSClass : (basicStyles + " " + checkboxColumnCellCSSClass);
    }

    @Override
    public BetterCheckboxCell getCell() {
        return cell;
    }

    @Override
    public InvertibleComparator<T> getComparator() {
        return new InvertibleComparatorAdapter<T>() {
            @Override
            public int compare(T a, T b) {
                return getValue(a) ? getValue(b) ? 0 : 1 : getValue(b) ? -1 : 0;
            }
        };
    }

    /**
     * No reasonable min/max display for this column; does nothing.
     */
    @Override
    public void updateMinMax() {}
    
    private EventTranslator<T> createSelectionEventTranslator() {
        return new EventTranslator<T>() {
            /**
             * Don't clear the selection when the user has clicked on the checkbox column
             */
            @Override
            public boolean clearCurrentSelection(CellPreviewEvent<T> event) {
                NativeEvent nativeEvent = event.getNativeEvent();
                boolean ctrlOrMeta = nativeEvent.getCtrlKey() || nativeEvent.getMetaKey();
                return !isSelectionCheckboxColumn(event) && !ctrlOrMeta;
            }

            private boolean isSelectionCheckboxColumn(CellPreviewEvent<T> event) {
                Column<?, ?> column = getColumn(event);
                return column == SelectionCheckboxColumn.this;
            }
            
            private Column<?, ?> getColumn(CellPreviewEvent<T> event) {
                CellTable<T> table = (CellTable<T>) event.getDisplay();
                return table.getColumn(event.getContext().getColumn());
            }

            @Override
            public SelectAction translateSelectionEvent(CellPreviewEvent<T> event) {
                final SelectAction result;
                if (BrowserEvents.CLICK.equals(event.getNativeEvent().getType()) && isSelectionCheckboxColumn(event)) {
                    result = SelectAction.TOGGLE;
                } else {
                    result = SelectAction.DEFAULT;
                }
                return result;
            }
        };
    }
}