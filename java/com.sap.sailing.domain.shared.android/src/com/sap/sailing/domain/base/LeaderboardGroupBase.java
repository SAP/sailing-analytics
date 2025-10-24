package com.sap.sailing.domain.base;

import com.sap.sse.common.NamedWithUUID;
import com.sap.sse.common.Renamable;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;

public interface LeaderboardGroupBase extends Renamable, NamedWithUUID, WithQualifiedObjectIdentifier, WithDescription {
    void setDescriptiom(String description);
    boolean hasOverallLeaderboard();
    String getOverallLeaderboardName();
    String getDisplayName();
    void setDisplayName(String displayName);
}
