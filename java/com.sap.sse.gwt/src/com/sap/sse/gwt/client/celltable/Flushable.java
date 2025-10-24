package com.sap.sse.gwt.client.celltable;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.view.client.HasData;

/**
 * This {@link Flushable} interface should be implemented by the {@link HasData display} which uses the
 * {@link SelectionCheckboxColumn}. You need this interface to ensure that the display displays the correct selection
 * state of the {@link SelectionCheckboxColumn}. The method {@link Flushable#flush()} will trigger a redraw of selected
 * lines, e.g., by calling the method {@link CellTable#flush()}.
 * 
 * @author Lukas Furmanek
 */
public interface Flushable {
    /**
     * This method will trigger a redraw of the {@link HasData display} so the {@link SelectionCheckboxColumn} is shown
     * correctly again. This is especially important when the selection comes form the program and not from the user.
     */
    void flush();
}