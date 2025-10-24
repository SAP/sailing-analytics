package com.sap.sailing.gwt.home.communication.anniversary;

import java.util.Collection;

import com.sap.sse.gwt.dispatch.shared.commands.ListResult;
import com.sap.sse.gwt.shared.DTO;

/**
 * This class is used to transmit current anniversary states to the front-end. It is used to highlight anniversaries
 * that will happen soon, or that did happen a few days ago prominently on the start page.
 */
public class AnniversariesDTO extends ListResult<AnniversaryDTO> implements DTO {

    /**
     * Creates an empty {@link AnniversariesDTO}.
     * 
     * @see ListResult#ListResult()
     */
    public AnniversariesDTO() {
    }

    /**
     * Creates an {@link AnniversariesDTO} containing the given {@link AnniversaryDTO anniversary entries}.
     * 
     * @param values
     *            {@link Collection} of a {@link AnniversaryDTO anniversary entries}
     * 
     * @see ListResult#ListResult(Collection)
     */
    public AnniversariesDTO(Collection<AnniversaryDTO> values) {
        super(values);
    }

}
