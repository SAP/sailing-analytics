package com.sap.sailing.gwt.home.shared.partials.multiselection;

import java.util.Collection;

import com.google.gwt.view.client.ProvidesKey;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionPresenter.Display;

public interface SuggestedMultiSelectionPresenter<T, D extends Display<T>> extends ProvidesKey<T> {

    void addSelection(T item);
    
    void removeSelection(T item);
    
    void clearSelection();
    
    Collection<T> getSelection();
    
    void getSuggestionItems(Iterable<String> queryTokens, int limit, final SuggestionItemsCallback<T> callback);
    
    String createSuggestionKeyString(T value);

    String createSuggestionAdditionalDisplayString(T value);
    
    void addDisplay(D display);
    
    void persist();
    
    void initSelectedItems(Iterable<T> selectedItems);
    
    interface SuggestionItemsCallback<T> {
        void setSuggestionItems(Collection<T> suggestionItems);
    }

    interface Display<T> {
        void setSelectedItems(Iterable<T> selectedItemsToSet);
    }
}
