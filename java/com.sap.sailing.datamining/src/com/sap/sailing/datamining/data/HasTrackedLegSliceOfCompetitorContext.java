package com.sap.sailing.datamining.data;

import com.sap.sse.datamining.annotations.Dimension;

/**
 * A "slice" of a competitor's tracked leg; slicing may happen in the retriever, perhaps influenced by some
 * configuration parameters, e.g., based on windward distance sailed, time spent, or rhumb line distance sailed.
 * A full leg is a special case of this.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface HasTrackedLegSliceOfCompetitorContext extends TerminalHasTrackedLegSliceOfCompetitorContext {
    /**
     * one-based; e.g., if there are ten slices, they have slice numbers 1..10
     */
    @Dimension(messageKey="SliceNumber")
    Integer getSliceNumber();
}