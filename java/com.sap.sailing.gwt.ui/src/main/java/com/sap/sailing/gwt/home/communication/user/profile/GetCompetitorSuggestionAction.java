package com.sap.sailing.gwt.home.communication.user.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.filter.AbstractListFilter;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;

/**
 * {@link SailingAction} implementation to load competitor data to be shown in a suggestion box depending on the
 * {@link #GetCompetitorSuggestionAction(String, int) given query tokens}, where the amount of loaded entries can be
 * limited.
 * 
 * The given query string is matched against the competitor's {@link Competitor#getName() name} and
 * {@link Boat#getSailID() sail-id} (which is determined from its {@link Competitor#getBoat() boat}, if any).
 */
public class GetCompetitorSuggestionAction implements SailingAction<CompetitorSuggestionResult> {
    
    private ArrayList<String> queryTokens;
    private int limit;
    
    @GwtIncompatible
    private final AbstractListFilter<Competitor> competitorFilter = new AbstractListFilter<Competitor>() {
        @Override
        public Iterable<String> getStrings(Competitor competitor) {
            // This shall return those strings rendered into the competitor display by
            // class SuggestedMultiSelectionCompetitorItemDescription and what it shows
            final List<String> result = new ArrayList<>();
            if (competitor.hasBoat()) {
                final String sailID = ((CompetitorWithBoat) competitor).getBoat().getSailID();
                if (Util.hasLength(sailID)) {
                    result.add(sailID);
                }
            }
            result.add(competitor.getName());
            return result;
        }
    };
    
    @SuppressWarnings("unused")
    private GetCompetitorSuggestionAction() {
    }

    /**
     * Creates a {@link GetCompetitorSuggestionAction} instance with the given query string, where the loaded competitor
     * entries are limited to the provided amount.
     * 
     * @param queryTokens
     *            query tokens to load competitors for
     * @param limit
     *            maximum number of competitor entries to be loaded
     */
    public GetCompetitorSuggestionAction(Iterable<String> queryTokens, int limit) {
        this.queryTokens = (ArrayList<String>) Util.addAll(queryTokens, new ArrayList<String>());
        this.limit = limit;
    }

    @Override
    @GwtIncompatible
    public CompetitorSuggestionResult execute(SailingDispatchContext ctx) throws DispatchException {
        Iterable<Competitor> filteredCompetitors = getFilteredCompetitors(ctx);
        Collection<SimpleCompetitorWithIdDTO> result = convertToSimpleCompetitorDTOs(filteredCompetitors);
        return new CompetitorSuggestionResult(result, Math.max(0, Util.size(filteredCompetitors) - limit));
    }
    
    @GwtIncompatible
    private Iterable<Competitor> getFilteredCompetitors(SailingDispatchContext ctx) {
        Iterable<? extends Competitor> allCompetitors = ctx.getRacingEventService().getCompetitorAndBoatStore().getAllCompetitors();
        return competitorFilter.applyFilter(queryTokens, Util.addAll(allCompetitors, new ArrayList<Competitor>())); 
    }
    
    @GwtIncompatible
    private Collection<SimpleCompetitorWithIdDTO> convertToSimpleCompetitorDTOs(
            Iterable<Competitor> filteredCompetitors) {
        Collection<SimpleCompetitorWithIdDTO> result = new ArrayList<>();
        int count = 0;
        for (Competitor competitor : filteredCompetitors) {
            result.add(new SimpleCompetitorWithIdDTO(competitor));
            if(++count >= limit) break;
        }
        return result;
    }

}
