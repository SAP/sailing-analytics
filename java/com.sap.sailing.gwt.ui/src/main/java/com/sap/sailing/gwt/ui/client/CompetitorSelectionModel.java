package com.sap.sailing.gwt.ui.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.filter.FilterSet;

public class CompetitorSelectionModel implements CompetitorSelectionProvider {
    protected final Set<CompetitorDTO> allCompetitors;
    
    /**
     * Keys are {@link CompetitorWithBoatDTO#getIdAsString()} of their values
     */
    private final LinkedHashMap<String, CompetitorDTO> selectedCompetitors;
    
    private final Set<CompetitorSelectionChangeListener> listeners;
    
    private final boolean hasMultiSelection;
    
    private FilterSet<CompetitorDTO, Filter<CompetitorDTO>> competitorsFilterSet;

    protected final CompetitorColorProvider competitorColorProvider;

    public CompetitorSelectionModel(boolean hasMultiSelection) {
        this(hasMultiSelection, new CompetitorColorProviderImpl(), null);
    }

    public CompetitorSelectionModel(boolean hasMultiSelection, CompetitorColorProvider competitorColorProvider) {
        this(hasMultiSelection, competitorColorProvider, null);
    }

    private CompetitorSelectionModel(boolean hasMultiSelection, CompetitorColorProvider competitorColorProvider, FilterSet<CompetitorDTO, Filter<CompetitorDTO>> competitorsFilterSet) {
        super();
        this.hasMultiSelection = hasMultiSelection;
        this.competitorColorProvider = competitorColorProvider;
        this.competitorsFilterSet = competitorsFilterSet;
        this.allCompetitors = new LinkedHashSet<>();
        this.selectedCompetitors = new LinkedHashMap<>();
        this.listeners = new HashSet<CompetitorSelectionChangeListener>();
    }
    
    @Override
    public CompetitorDTO getSelectedCompetitor(String competitorIdAsString) {
        return selectedCompetitors.get(competitorIdAsString);
    }
    
    @Override
    public Iterable<String> getSelectedCompetitorIdsAsStrings() {
        return selectedCompetitors.keySet();
    }

    /**
     * Adds a competitor to the {@link #getAllCompetitors() set of all competitors}. If the competitor was not yet
     * contained, it will be deselected.
     */
    public void add(CompetitorWithBoatDTO competitor) {
        add(competitor, true);
    }
    
    private void add(CompetitorDTO competitor, boolean notifyListeners) {
        if (competitor.getColor() != null) {
            competitorColorProvider.addBlockedColor(competitor.getColor());
        }
        boolean changed = allCompetitors.add(competitor);
        if (notifyListeners && changed) {
            fireListChanged(getAllCompetitors());
        }
    }

    @Override
    public boolean hasMultiSelection() {
        return hasMultiSelection;
    }
    
    /**
     * Removes all competitors. Afterwards, this model is empty. Deselection events are issued for all competitors that
     * were selected so far.
     */
    public void clear() {
        Iterator<CompetitorDTO> selIter = getSelectedCompetitors().iterator();
        while (selIter.hasNext()) {
            CompetitorDTO selected = selIter.next();
            setSelected(selected, false);
            selIter = getSelectedCompetitors().iterator();
        }
        assert selectedCompetitors.isEmpty();
        allCompetitors.clear();
        fireListChanged(getAllCompetitors());
    }
    
    public void addAll(Iterable<CompetitorDTO> competitors) {
        boolean changed = false;
        for (CompetitorDTO competitor : competitors) {
            add(competitor, false);
            changed = true;
        }
        if (changed) {
            fireListChanged(getAllCompetitors());
        }
    }

    /**
     * Removes the competitor from the {@link #getAllCompetitors() set of all competitor}. If the competitor was previously
     * contained and selected, it is deselected first.
     */
    public void remove(CompetitorDTO competitor) {
        if (isSelected(competitor)) {
            setSelected(competitor, false);
        }
        if (competitor.getColor() != null) {
            competitorColorProvider.removeBlockedColor(competitor.getColor());
        }
        boolean changed = allCompetitors.remove(competitor);
        if (changed) {
            fireListChanged(getAllCompetitors());
        }
    }
    
    @Override
    public Iterable<CompetitorDTO> getSelectedCompetitors() {
        return Collections.unmodifiableCollection(selectedCompetitors.values());
    }

    @Override
    public Iterable<CompetitorDTO> getSelectedFilteredCompetitors() {
        return Util.filter(getFilteredCompetitors(), c->selectedCompetitors.containsKey(c.getIdAsString()));
    }

    @Override
    public Iterable<CompetitorDTO> getAllCompetitors() {
        return Collections.unmodifiableCollection(allCompetitors);
    }
    
    @Override
    public Iterable<CompetitorDTO> getFilteredCompetitors() {
        final Iterable<CompetitorDTO> result;
        if (competitorsFilterSet == null || competitorsFilterSet.getFilters().isEmpty()) {
            result = allCompetitors;
        } else {
            result = Util.filter(allCompetitors, competitorDTO -> competitorsFilterSet.getFilters().stream().allMatch(filter->filter.matches(competitorDTO)));
        }
        return result;
    }
    
    public void setSelected(CompetitorDTO competitor, boolean selected, CompetitorSelectionChangeListener... listenersNotToNotify) {
        if (selected) {
            if (allCompetitors.contains(competitor) && !selectedCompetitors.containsKey(competitor.getIdAsString())) {
                selectedCompetitors.put(competitor.getIdAsString(), competitor);
                fireAddedToSelection(competitor, listenersNotToNotify);
            }
        } else {
            if (selectedCompetitors.containsKey(competitor.getIdAsString())) {
                selectedCompetitors.remove(competitor.getIdAsString());
                fireRemovedFromSelection(competitor, listenersNotToNotify);
            }
        }
    }
    
    private void fireAddedToSelection(CompetitorDTO competitor, CompetitorSelectionChangeListener... listenersNotToNotify) {
        for (CompetitorSelectionChangeListener listener : listeners) {
            if (listenersNotToNotify == null || !Arrays.asList(listenersNotToNotify).contains(listener)) {
                listener.addedToSelection(competitor);
            }
        }
    }
    
    private void fireRemovedFromSelection(CompetitorDTO competitor, CompetitorSelectionChangeListener... listenersNotToNotify) {
        for (CompetitorSelectionChangeListener listener : listeners) {
            if (listenersNotToNotify == null || !Arrays.asList(listenersNotToNotify).contains(listener)) {
                listener.removedFromSelection(competitor);
            }
        }
    }
    
    private void fireListChanged(Iterable<CompetitorDTO> competitors, CompetitorSelectionChangeListener... listenersNotToNotify) {
        for (CompetitorSelectionChangeListener listener : listeners) {
            if (listenersNotToNotify == null || !Arrays.asList(listenersNotToNotify).contains(listener)) {
                listener.competitorsListChanged(competitors);
            }
        }
    }

    @Override
    public boolean isSelected(CompetitorDTO competitor) {
        return selectedCompetitors.containsKey(competitor.getIdAsString());
    }

    @Override
    public void addCompetitorSelectionChangeListener(CompetitorSelectionChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeCompetitorSelectionChangeListener(CompetitorSelectionChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void setSelection(Iterable<CompetitorDTO> newSelection, CompetitorSelectionChangeListener... listenersNotToNotify) {
        Set<CompetitorDTO> competitorsToRemoveFromSelection = new HashSet<>(selectedCompetitors.values());
        for (CompetitorDTO newSelectedCompetitor : newSelection) {
            setSelected(newSelectedCompetitor, true, listenersNotToNotify);
            competitorsToRemoveFromSelection.remove(newSelectedCompetitor);
        }
        for (CompetitorDTO competitorToRemoveFromSelection : competitorsToRemoveFromSelection) {
            setSelected(competitorToRemoveFromSelection, false, listenersNotToNotify);
        }
    }

    @Override
    public void setCompetitors(Iterable<CompetitorDTO> newCompetitors, CompetitorSelectionChangeListener... listenersNotToNotify) {
        boolean changed = false;
        Map<String, CompetitorDTO> oldCompetitorsToRemove = new HashMap<>();
        for (final CompetitorDTO c : allCompetitors) {
            oldCompetitorsToRemove.put(c.getIdAsString(), c);
        }
        for (CompetitorDTO newCompetitor : newCompetitors) {
            // Search by equality, including all CompetitorDTO fields; if equal object is found, leave it in place;
            // non-equal objects may still represent the same competitor entity; yet, replace the object to obtain the
            // new, changed state; make sure the selection keeps unchanged regarding the selected competitor "entities";
            // see also bug 3413 for more background.
            if (allCompetitors.contains(newCompetitor)) {
                oldCompetitorsToRemove.remove(newCompetitor.getIdAsString()); // the old competitor object remains in allCompetitors; it is equal but doesn't have to be identical to newCompetitor
            } else {
                boolean selected = isSelected(newCompetitor);
                if (selected) {
                    remove(oldCompetitorsToRemove.get(newCompetitor.getIdAsString()));
                    oldCompetitorsToRemove.remove(newCompetitor.getIdAsString());
                }
                add(newCompetitor, false); // due to the equals definition of CompetitorDTOImpl that compares most fields, state changes lead to a replacement here
                if (selected) {
                    // restore selection for replaced element
                    setSelected(newCompetitor, selected, listenersNotToNotify);
                }
                changed = true;
            }
        }
        for (CompetitorDTO oldCompetitorToRemove : oldCompetitorsToRemove.values()) {
            remove(oldCompetitorToRemove);
            changed = true;
        }
        
        if (changed) {
            fireListChanged(getAllCompetitors(), listenersNotToNotify);
        }
    }

    @Override
    public Color getColor(CompetitorDTO competitor) {
        return allCompetitors.contains(competitor) ? competitorColorProvider.getColor(competitor) : null;
    }

    @Override
    public FilterSet<CompetitorDTO, Filter<CompetitorDTO>> getCompetitorsFilterSet() {
        return competitorsFilterSet;
    }
    
    @Override 
    public FilterSet<CompetitorDTO, Filter<CompetitorDTO>> getOrCreateCompetitorsFilterSet(String nameToAssignToNewFilterSet) {
        if (competitorsFilterSet == null) {
            competitorsFilterSet = new FilterSet<>(nameToAssignToNewFilterSet);
        }
        return getCompetitorsFilterSet();
    }

    @Override
    public void setCompetitorsFilterSet(FilterSet<CompetitorDTO, Filter<CompetitorDTO>> competitorsFilterSet) {
        FilterSet<CompetitorDTO, ? extends Filter<CompetitorDTO>> oldFilterSet = this.competitorsFilterSet;
        this.competitorsFilterSet = competitorsFilterSet;
        if (!Util.equalsWithNull(competitorsFilterSet, oldFilterSet)) {
            for (CompetitorSelectionChangeListener listener : listeners) {
                listener.filterChanged(oldFilterSet, competitorsFilterSet);
            }
        }
        for (CompetitorSelectionChangeListener listener : listeners) {
            listener.filteredCompetitorsListChanged(getFilteredCompetitors());
        }
    }

    @Override
    public boolean hasActiveFilters() {
        return (competitorsFilterSet != null && !competitorsFilterSet.getFilters().isEmpty() 
                && Util.size(getFilteredCompetitors()) != allCompetitors.size());
    }

    @Override
    public void clearAllFilters() {
        if (hasActiveFilters()) {
            Iterator<CompetitorDTO> selIter = getSelectedCompetitors().iterator();
            while (selIter.hasNext()) {
                CompetitorDTO selected = selIter.next();
                setSelected(selected, false);
                selIter = getSelectedCompetitors().iterator();
            }
            competitorsFilterSet = null;
            fireListChanged(getAllCompetitors());
        }
    }

    @Override
    public int getFilteredCompetitorsListSize() {
        return Util.size(getFilteredCompetitors());
    }
}
