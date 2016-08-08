package com.sap.sailing.gwt.home.shared.partials.multiselection;

import java.util.Collection;

import com.google.gwt.view.client.ProvidesKey;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionDataProvider.Display;

public interface SuggestedMultiSelectionDataProvider<T, D extends Display<T>> extends ProvidesKey<T> {
    
    void addSelection(T item);
    
    void removeSelection(T item);
    
    void clearSelection();
    
    void getSuggestionItems(Iterable<String> queryTokens, int limit, final SuggestionItemsCallback<T> callback);
    
    void addDisplay(D display);
    
    void persist();
    
    void initSelectedItems(Collection<T> selectedItems);
    
    interface SuggestionItemsCallback<T> {
        void setSuggestionItems(Collection<T> suggestionItems);
    }

    interface Display<T> {
        void setSelectedItems(Collection<T> selectedItems);
    }
}
