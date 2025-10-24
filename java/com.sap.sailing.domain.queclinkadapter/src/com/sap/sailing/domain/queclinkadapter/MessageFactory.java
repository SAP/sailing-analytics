package com.sap.sailing.domain.queclinkadapter;

import java.text.ParseException;

/**
 * Constructs a specific message based on the parameters array parsed from a text version of a message. This parameters
 * array is what you get when calling {@link Message#getParameters()}.
 * <p>
 * 
 * Each implementation has its requirements as to the number, type and position of the parameters in the array. Most are
 * fixed; a few message types have optional parameters where their presence depends, e.g., on the device's status and
 * how it was configured. In any case, we expect that the factory implementation can cope with such optional parameters
 * and detect whether or not they are present, e.g., based on the types, format, and count of all remaining parameters.
 * 
 * @author Axel Uhl (d043530)
 *
 */
@FunctionalInterface
public interface MessageFactory {
    Message createMessageWithParameters(String[] parameters) throws ParseException;
}
