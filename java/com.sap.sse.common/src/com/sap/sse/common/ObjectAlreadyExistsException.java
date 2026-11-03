package com.sap.sse.common;

/**
 * Rejects a creation or update because its target identifier is occupied. Throw this only before making
 * domain changes in the rejected operation. An enclosing compound action may already have made changes;
 * only callers guaranteeing that their entire action was rejected may roll back its ownership.
 */
public class ObjectAlreadyExistsException extends IllegalArgumentException {
    private static final long serialVersionUID = 2741417039591563032L;

    public ObjectAlreadyExistsException(final String objectType, final String identifier) {
        super(objectType + " with name or identifier \"" + identifier + "\" already exists");
    }
}
