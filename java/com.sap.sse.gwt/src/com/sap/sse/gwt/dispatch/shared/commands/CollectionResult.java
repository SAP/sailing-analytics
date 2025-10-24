package com.sap.sse.gwt.dispatch.shared.commands;

import java.util.Collection;

import com.sap.sse.gwt.shared.DTO;

/**
 * Collection result that can be reused to send a collection of {@link DTO} instances
 *
 * @param <T>
 */
public interface CollectionResult<T extends DTO> extends DTO, Result {
    Collection<T> getValues();
}
