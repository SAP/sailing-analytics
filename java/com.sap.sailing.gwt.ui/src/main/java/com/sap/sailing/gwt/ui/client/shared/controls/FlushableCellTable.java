package com.sap.sailing.gwt.ui.client.shared.controls;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;

/**
 * This {@link FlushableCellTable} provides the {@link FlushableCellTable#flush()}-method for the
 * {@link SelectionCheckboxColumn}. So the {@link SelectionCheckboxColumn} can ensure that the selection state is
 * displayed correct.
 * 
 * @author D064976
 * @param <T>
 */
public class FlushableCellTable<T> extends CellTable<T> implements Flushable {
    public FlushableCellTable() {
        super();
        setTableBuilder(new AriaCellTableBuilder<T>(this));
    }
    public FlushableCellTable(final int pageSize) {
        super(pageSize);
        setTableBuilder(new AriaCellTableBuilder<T>(this));
    }
    public FlushableCellTable(ProvidesKey<T> keyProvider) {
        super(keyProvider);
        setTableBuilder(new AriaCellTableBuilder<T>(this));
    }
    public FlushableCellTable(int pageSize, Resources resources) {
        super(pageSize, resources);
        setTableBuilder(new AriaCellTableBuilder<T>(this));
    }
    public FlushableCellTable(int pageSize, ProvidesKey<T> keyProvider) {
        super(pageSize, keyProvider);
        setTableBuilder(new AriaCellTableBuilder<T>(this));
    }
    public FlushableCellTable(final int pageSize, Resources resources, ProvidesKey<T> keyPrivider) {
        super(pageSize, resources, keyPrivider);
        setTableBuilder(new AriaCellTableBuilder<T>(this));
    }
    public FlushableCellTable(final int pageSize, Resources resources, ProvidesKey<T> keyProvider, Widget loadingIndicator) {
        super(pageSize, resources, keyProvider, loadingIndicator);
        setTableBuilder(new AriaCellTableBuilder<T>(this));
    }
    public FlushableCellTable(final int pageSize, Resources resources, ProvidesKey<T> keyProvider, Widget loadingIndicator, boolean enableColGroup, boolean attachLoadingPanel) {
        super(pageSize,resources,keyProvider,loadingIndicator,enableColGroup, attachLoadingPanel);
        setTableBuilder(new AriaCellTableBuilder<T>(this));
    }


}