package com.sap.sse.common;

import java.io.Serializable;

/**
 * Rejects a creation or update because its target identifier is occupied. Throw this only before making
 * domain changes in the rejected operation. An enclosing compound action may already have made changes;
 * only callers guaranteeing that their entire action was rejected may roll back its ownership.
 */
public class ObjectAlreadyExistsException extends IllegalArgumentException {
    private static final long serialVersionUID = 2741417039591563032L;

    public interface ObjectType extends Serializable {
        /** Enum constant name used in the technical exception message, supplied by {@link Enum#name()}. */
        String name();
    }

    // Must be non-final so GWT includes concrete object types in the RPC serialization policy.
    private ObjectType objectType;
    private final String identifier;

    public ObjectAlreadyExistsException(final ObjectType objectType, final String identifier) {
        super(objectType.name() + " with name or identifier \"" + identifier + "\" already exists");
        this.objectType = objectType;
        this.identifier = identifier;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public String getIdentifier() {
        return identifier;
    }
}
