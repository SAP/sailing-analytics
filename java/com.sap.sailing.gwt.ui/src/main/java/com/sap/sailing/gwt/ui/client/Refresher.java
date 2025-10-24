package com.sap.sailing.gwt.ui.client;

import java.util.function.Predicate;

import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;

/**
 * A {@link Refresher} offers methods to register a {@link Displayer} of a specific DTO type, force reloads and give the
 * possibility to update the centrally managed data. The displayer will be informed asynchronously after data changes
 * have applied.
 * 
 * @param <T>
 *            the base DTO of {@link Displayer} and {@link Refresher}.
 */
public interface Refresher<T> {

    /**
     * Register the {@link Displayer} and call the fill method asynchronously after the initial load of the data is
     * done.
     * 
     * @param displayer
     *            the {@link Displayer} to add.
     */
    void addDisplayerAndCallFillOnInit(Displayer<T> displayer);

    void removeDisplayer(Displayer<T> displayer);

    /**
     * Force a reload of the data from server and call afterwards asynchronously the fill method of the given
     * {@link Displayer}.
     * 
     * @param fillOnlyDisplayer
     *            if not <code>null</code> only the fill method of the given {@link Displayer} will be called.
     */
    void reloadAndCallFillOnly(Displayer<T> fillOnlyDisplayer);

    /**
     * Force a reload of the data from server and afterwards call the fill method of all registered {@link Displayer}s
     * asynchronously.
     */
    void reloadAndCallFillAll();

    /**
     * Updates the list of DTOs and call the fill method of all registered {@link Displayer}s asynchronously, except an
     * optional origin {@link Displayer} if set.
     * 
     * @param dtos
     *            full data set of new DTOs
     * @param origin
     *            if not <code>null</code> the given {@link Displayer} will be ignored when calling the fill method.
     */
    void updateAndCallFillForAll(Iterable<T> dtos, Displayer<T> origin);

    /**
     * Call fill method of given {@link Displayer} even if it is not registered. If data was not loaded before, do an
     * initial load and call fill method of all registered {@link Displayer} asynchronously and if not registered of the
     * given {@link Displayer}, too.
     * 
     * @param displayer
     *            the {@link Displayer} on which the fill method shall be called.
     */
    void callFillAndReloadInitially(Displayer<T> displayer);
    
    /**
     * Call the fill method of all registered displayer, e.g. after removing some DTOs from list.
     */
    void callAllFill();
    
    /**
     * Adds a DTO to the DTO list to prevent loading all data after adding single object to context. This will
     * only take place if the DTO list already exists, indicating that it has been requested before.
     * 
     * @param dto the DTO to add
     */
    void add(T dto);
    
    /**
     * Removes a DTO from DTO list to prevent loading all data after deleting single object to context.
     * 
     * @param dto the DTO to add
     */
    void remove(T dto);

    /**
     * Like {@link #add(Object)}, but an existing DTO will be replaced by {@code dto} if
     * found in the list of DTOs known to this refresher, identified by {@link EntityIdentityComparator#equals(Object) equal}. This will only take place if the DTO list already exists,
     * indicating that it has been requested before.
     */
    void addIfNotContainedElseReplace(T dto, EntityIdentityComparator<T> comp);

    /**
     * {@link #remove(Object) Removes} all objects from this refresher that {@link Predicate#test(Object) are matched) by the
     * {@code filter} passed.
     */
    void removeAll(Predicate<T> filter);
}
