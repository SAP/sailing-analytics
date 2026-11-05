package com.sap.sailing.domain.common;

import com.sap.sse.common.ObjectAlreadyExistsException;
import com.sap.sse.common.ObjectAlreadyExistsException.ObjectType;

/**
 * Object type identifiers shared by conflict producers and clients handling {@link ObjectAlreadyExistsException}.
 */
public enum ConflictObjectType implements ObjectType {
    LEADERBOARD,
    LEADERBOARD_GROUP
}
