package com.sap.sailing.domain.swisstimingadapter;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;

/**
 * Configuration parameters that can be used to connect to a SwissTiming event.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface SwissTimingConfiguration extends WithQualifiedObjectIdentifier {

    String getCreatorName();
    String getName();
    
    String getJsonURL();
    
    String getHostname();
    
    Integer getPort();

    String getUpdateURL();

    String getApiToken();

    @Override
    default QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier());
    }

    @Override
    default HasPermissions getPermissionType() {
        return SecuredDomainType.SWISS_TIMING_ACCOUNT;
    }

    default TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return getTypeRelativeObjectIdentifier(getJsonURL(), getCreatorName());
    }

    public static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(String jsonUrl, String username) {
        return username == null ? new TypeRelativeObjectIdentifier(jsonUrl)
                : new TypeRelativeObjectIdentifier(jsonUrl, username);
    }
}
